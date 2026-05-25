package mk.fikt.gamevault.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mk.fikt.gamevault.data.auth.AuthRepository
import mk.fikt.gamevault.data.auth.AuthUser
import mk.fikt.gamevault.data.local.GameDao
import mk.fikt.gamevault.data.local.ReviewDao
import mk.fikt.gamevault.data.local.UserProfileDao
import mk.fikt.gamevault.data.local.UserProfileEntity
import mk.fikt.gamevault.data.remote.FirestoreSchema.COLLECTION_REVIEWS
import mk.fikt.gamevault.data.remote.FirestoreSchema.COLLECTION_USERS
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_AUTHOR_UID
import mk.fikt.gamevault.data.remote.FirestoreSchema.SUBCOLLECTION_GAMES
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_DISPLAY_NAME
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_DISPLAY_NAME_LOWER
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_EMAIL
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_FCM_TOKEN
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_GAME_COUNT
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_IS_ANONYMOUS
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_JOINED_AT
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_PHOTO_URL

class UserProfileRepository(
    private val dao: UserProfileDao,
    private val gameDao: GameDao,
    private val reviewDao: ReviewDao,
    private val authRepository: AuthRepository,
    private val firebaseAvailable: Boolean,
    private val scope: CoroutineScope,
) {

    private val firestore: FirebaseFirestore? by lazy {
        if (!firebaseAvailable) null
        else runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    @Volatile private var othersListener: ListenerRegistration? = null

    /** Local cached profile of current user (or any uid). */
    fun observe(uid: String): Flow<UserProfileEntity?> = dao.observe(uid)

    /** Search/browse other users from Room (kept in sync from Firestore). */
    fun observeOthers(query: String): Flow<List<UserProfileEntity>> {
        val myUid = authRepository.currentUser()?.uid ?: return flowOf(emptyList())
        ensureOthersSyncStarted()
        return dao.observeAllOthers(myUid, query.trim())
    }

    /** Called on every successful sign-in to mirror auth user → Firestore + Room. */
    suspend fun syncOnSignIn(user: AuthUser) {
        val existing = dao.get(user.uid)
        val entity = UserProfileEntity(
            uid = user.uid,
            displayName = user.displayName ?: existing?.displayName,
            email = user.email ?: existing?.email,
            photoUrl = user.photoUrl ?: existing?.photoUrl,
            isAnonymous = user.isAnonymous,
            joinedAt = existing?.joinedAt ?: System.currentTimeMillis(),
            fcmToken = existing?.fcmToken,
            gameCount = existing?.gameCount ?: 0,
        )
        dao.upsert(entity)
        pushToFirestore(entity)
    }

    /** Updates own displayName + photoUrl. Returns true on success. */
    suspend fun updateOwnProfile(displayName: String?, photoUrl: String?): Boolean {
        val uid = authRepository.currentUser()?.uid ?: return false
        val current = dao.get(uid) ?: return false
        val updated = current.copy(
            displayName = displayName?.takeIf { it.isNotBlank() },
            photoUrl = photoUrl?.takeIf { it.isNotBlank() },
        )
        dao.upsert(updated)
        pushToFirestore(updated)
        return true
    }

    /** Called when game count changes for current user; mirrors to Firestore. */
    suspend fun updateGameCount(count: Int) {
        val uid = authRepository.currentUser()?.uid ?: return
        val current = dao.get(uid) ?: return
        if (current.gameCount == count) return
        val updated = current.copy(gameCount = count)
        dao.upsert(updated)
        val fs = firestore ?: return
        runCatching {
            fs.collection(COLLECTION_USERS).document(uid)
                .set(mapOf(FIELD_GAME_COUNT to count), SetOptions.merge())
                .await()
        }
    }

    /** Stream a single foreign user's profile by snapshot listener. */
    fun observeRemote(uid: String): Flow<UserProfileEntity?> {
        val fs = firestore ?: return flowOf(null)
        return callbackFlow {
            val reg = fs.collection(COLLECTION_USERS).document(uid)
                .addSnapshotListener { snap, _ ->
                    trySend(snap?.toEntity(uid))
                }
            awaitClose { reg.remove() }
        }
    }

    private fun pushToFirestore(entity: UserProfileEntity) {
        val fs = firestore ?: return
        val data = mutableMapOf<String, Any?>(
            FIELD_DISPLAY_NAME to entity.displayName,
            FIELD_DISPLAY_NAME_LOWER to entity.displayName?.lowercase(),
            FIELD_EMAIL to entity.email,
            FIELD_PHOTO_URL to entity.photoUrl,
            FIELD_IS_ANONYMOUS to entity.isAnonymous,
            FIELD_JOINED_AT to entity.joinedAt,
            FIELD_GAME_COUNT to entity.gameCount,
        )
        entity.fcmToken?.let { data[FIELD_FCM_TOKEN] = it }
        runCatching {
            fs.collection(COLLECTION_USERS).document(entity.uid)
                .set(data, SetOptions.merge())
        }
    }

    private fun ensureOthersSyncStarted() {
        if (othersListener != null) return
        val fs = firestore ?: return
        synchronized(this) {
            if (othersListener != null) return
            othersListener = fs.collection(COLLECTION_USERS)
                .orderBy(FIELD_DISPLAY_NAME_LOWER, Query.Direction.ASCENDING)
                .limit(200)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener
                    val profiles = snap.documents.mapNotNull { it.toEntity(it.id) }
                    if (profiles.isNotEmpty()) {
                        scope.launch { dao.upsertAll(profiles) }
                    }
                }
        }
    }

    /**
     * Deletes the current user's data from Firestore (reviews, games subcollection, profile doc)
     * and wipes their rows from Room. Does NOT delete the auth account — that's [AuthRepository.deleteAccount].
     * Returns true if all deletes succeeded.
     */
    suspend fun purgeOwnData(): Boolean {
        val uid = authRepository.currentUser()?.uid ?: return false
        val fs = firestore
        var ok = true
        if (fs != null) {
            // 1) Reviews authored by uid (global collection).
            ok = ok && deleteWhere(fs.collection(COLLECTION_REVIEWS)
                .whereEqualTo(FIELD_AUTHOR_UID, uid))
            // 2) Games subcollection.
            ok = ok && deleteAll(fs.collection(COLLECTION_USERS).document(uid)
                .collection(SUBCOLLECTION_GAMES))
            // 3) User profile doc.
            ok = ok && runCatching {
                fs.collection(COLLECTION_USERS).document(uid).delete().await()
            }.isSuccess
        }
        // Local wipe regardless of Firestore outcome — we'll be signed out either way on success.
        runCatching { reviewDao.deleteAllByAuthor(uid) }
        runCatching { gameDao.deleteAllForOwner(uid) }
        runCatching { dao.delete(uid) }
        return ok
    }

    private suspend fun deleteWhere(query: com.google.firebase.firestore.Query): Boolean =
        runCatching {
            val snap = query.get().await()
            snap.documents.forEach { it.reference.delete().await() }
        }.isSuccess

    private suspend fun deleteAll(coll: com.google.firebase.firestore.CollectionReference): Boolean =
        runCatching {
            val snap = coll.get().await()
            snap.documents.forEach { it.reference.delete().await() }
        }.isSuccess

    private fun DocumentSnapshot.toEntity(uid: String): UserProfileEntity? {
        if (!exists()) return null
        return UserProfileEntity(
            uid = uid,
            displayName = getString(FIELD_DISPLAY_NAME),
            email = getString(FIELD_EMAIL),
            photoUrl = getString(FIELD_PHOTO_URL),
            isAnonymous = getBoolean(FIELD_IS_ANONYMOUS) ?: false,
            joinedAt = getLong(FIELD_JOINED_AT) ?: System.currentTimeMillis(),
            fcmToken = getString(FIELD_FCM_TOKEN),
            gameCount = (getLong(FIELD_GAME_COUNT) ?: 0L).toInt(),
        )
    }
}
