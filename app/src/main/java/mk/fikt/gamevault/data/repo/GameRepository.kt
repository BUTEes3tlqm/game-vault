package mk.fikt.gamevault.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mk.fikt.gamevault.data.auth.AuthRepository
import mk.fikt.gamevault.data.local.GameDao
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.data.local.GenreCountRow
import mk.fikt.gamevault.data.local.StatusCountRow
import mk.fikt.gamevault.data.model.GamePlatform
import mk.fikt.gamevault.data.model.GameStatus
import mk.fikt.gamevault.data.remote.FirestoreSchema.COLLECTION_USERS
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_DATE_ADDED
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_DATE_UPDATED
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_GENRE
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_HOURS_PLAYED
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_ID
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_NOTES
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_PERSONAL_RATING
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_PLATFORM
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_PROGRESS_PERCENT
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_RELEASE_YEAR
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_STATUS
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_TITLE
import mk.fikt.gamevault.data.remote.FirestoreSchema.SUBCOLLECTION_GAMES

class GameRepository(
    private val gameDao: GameDao,
    private val authRepository: AuthRepository,
    private val firebaseAvailable: Boolean,
    private val scope: CoroutineScope,
) {

    private val firestore: FirebaseFirestore? by lazy {
        if (!firebaseAvailable) null
        else runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    /** Callback invoked after upsert/delete so other repos can mirror gameCount. */
    var onCountChanged: (suspend (Int) -> Unit)? = null

    @Volatile private var ownSyncListener: ListenerRegistration? = null
    @Volatile private var ownSyncedUid: String? = null

    private val currentUid: String? get() = authRepository.currentUser()?.uid

    fun observeFiltered(status: GameStatus?, query: String): Flow<List<GameEntity>> {
        val uid = currentUid ?: return flowOf(emptyList())
        ensureOwnRemoteSync(uid)
        return gameDao.observeFiltered(uid, status?.name, query.trim())
    }

    fun observeAll(): Flow<List<GameEntity>> {
        val uid = currentUid ?: return flowOf(emptyList())
        ensureOwnRemoteSync(uid)
        return gameDao.observeAll(uid)
    }

    fun observeCount(): Flow<Int> {
        val uid = currentUid ?: return flowOf(0)
        return gameDao.observeCount(uid)
    }

    fun observeTotalHours(): Flow<Double> {
        val uid = currentUid ?: return flowOf(0.0)
        return gameDao.observeTotalHours(uid)
    }

    fun observeCompletedCount(): Flow<Int> {
        val uid = currentUid ?: return flowOf(0)
        return gameDao.observeCountByStatus(uid, GameStatus.COMPLETED)
    }

    fun observeStatusCounts(): Flow<List<StatusCountRow>> {
        val uid = currentUid ?: return flowOf(emptyList())
        return gameDao.observeStatusCounts(uid)
    }

    fun observeGenreCounts(): Flow<List<GenreCountRow>> {
        val uid = currentUid ?: return flowOf(emptyList())
        return gameDao.observeGenreCounts(uid)
    }

    fun observeById(id: String): Flow<GameEntity?> = gameDao.observeById(id)

    suspend fun getById(id: String): GameEntity? = gameDao.getById(id)

    suspend fun upsert(game: GameEntity) {
        val uid = currentUid.orEmpty()
        val toSave = game.copy(
            ownerUid = game.ownerUid.ifBlank { uid },
            dateUpdated = System.currentTimeMillis(),
        )
        gameDao.upsert(toSave)
        pushGameToFirestore(toSave)
        if (uid.isNotBlank()) {
            onCountChanged?.invoke(gameDao.countNow(uid))
        }
    }

    suspend fun delete(id: String) {
        val game = gameDao.getById(id)
        gameDao.deleteById(id)
        val owner = game?.ownerUid.orEmpty()
        if (owner.isNotBlank()) {
            removeGameFromFirestore(owner, id)
            if (owner == currentUid) {
                onCountChanged?.invoke(gameDao.countNow(owner))
            }
        }
    }

    /** Read-only stream of another user's library via Firestore snapshot listener. */
    fun observeGamesFor(uid: String): Flow<List<GameEntity>> {
        val fs = firestore ?: return flowOf(emptyList())
        return callbackFlow {
            val reg = fs.collection(COLLECTION_USERS).document(uid)
                .collection(SUBCOLLECTION_GAMES)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val games = snap.documents.mapNotNull { it.toGameEntity(uid) }
                        .sortedByDescending { it.dateUpdated }
                    trySend(games)
                }
            awaitClose { reg.remove() }
        }
    }

    private fun pushGameToFirestore(game: GameEntity) {
        val fs = firestore ?: return
        val owner = game.ownerUid.takeIf { it.isNotBlank() } ?: return
        runCatching {
            fs.collection(COLLECTION_USERS).document(owner)
                .collection(SUBCOLLECTION_GAMES).document(game.id)
                .set(game.toFirestoreMap())
        }
        scope.launch {
            runCatching {
                gameDao.upsert(game.copy(syncedToFirestore = true))
            }
        }
    }

    private fun removeGameFromFirestore(owner: String, id: String) {
        val fs = firestore ?: return
        runCatching {
            fs.collection(COLLECTION_USERS).document(owner)
                .collection(SUBCOLLECTION_GAMES).document(id)
                .delete()
        }
    }

    private fun ensureOwnRemoteSync(uid: String) {
        if (ownSyncedUid == uid && ownSyncListener != null) return
        val fs = firestore ?: return
        synchronized(this) {
            if (ownSyncedUid == uid && ownSyncListener != null) return
            ownSyncListener?.remove()
            ownSyncedUid = uid
            ownSyncListener = fs.collection(COLLECTION_USERS).document(uid)
                .collection(SUBCOLLECTION_GAMES)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener
                    val games = snap.documents.mapNotNull { it.toGameEntity(uid) }
                    if (games.isNotEmpty()) {
                        scope.launch { mergeIntoRoom(games) }
                    }
                }
        }
        scope.launch { backfillUnsyncedToFirestore(uid) }
    }

    private suspend fun backfillUnsyncedToFirestore(uid: String) {
        val unsynced = runCatching { gameDao.getUnsynced(uid) }.getOrNull().orEmpty()
        if (unsynced.isEmpty()) return
        unsynced.forEach { pushGameToFirestore(it) }
        runCatching {
            onCountChanged?.invoke(gameDao.countNow(uid))
        }
    }

    private suspend fun mergeIntoRoom(remote: List<GameEntity>) {
        val merged = remote.map { incoming ->
            val existing = gameDao.getById(incoming.id)
            // Preserve local-only fields (coverUri lives on owner's device)
            incoming.copy(coverUri = existing?.coverUri)
        }
        gameDao.upsertAll(merged)
    }

    private fun GameEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_TITLE to title,
        FIELD_PLATFORM to platform.name,
        FIELD_RELEASE_YEAR to releaseYear,
        FIELD_GENRE to genre,
        FIELD_STATUS to status.name,
        FIELD_HOURS_PLAYED to hoursPlayed,
        FIELD_PERSONAL_RATING to personalRating,
        FIELD_PROGRESS_PERCENT to progressPercent,
        FIELD_NOTES to notes,
        FIELD_DATE_ADDED to dateAdded,
        FIELD_DATE_UPDATED to dateUpdated,
    )

    private fun DocumentSnapshot.toGameEntity(uid: String): GameEntity? {
        val title = getString(FIELD_TITLE) ?: return null
        val platform = getString(FIELD_PLATFORM)?.let {
            runCatching { GamePlatform.valueOf(it) }.getOrDefault(GamePlatform.OTHER)
        } ?: GamePlatform.OTHER
        val status = getString(FIELD_STATUS)?.let {
            runCatching { GameStatus.valueOf(it) }.getOrDefault(GameStatus.BACKLOG)
        } ?: GameStatus.BACKLOG
        return GameEntity(
            id = id,
            ownerUid = uid,
            title = title,
            platform = platform,
            releaseYear = getLong(FIELD_RELEASE_YEAR)?.toInt(),
            genre = getString(FIELD_GENRE),
            coverUri = null, // covers are local-only
            status = status,
            hoursPlayed = getDouble(FIELD_HOURS_PLAYED) ?: 0.0,
            personalRating = getDouble(FIELD_PERSONAL_RATING)?.toFloat() ?: 0f,
            progressPercent = (getLong(FIELD_PROGRESS_PERCENT) ?: 0L).toInt(),
            notes = getString(FIELD_NOTES),
            dateAdded = getLong(FIELD_DATE_ADDED) ?: System.currentTimeMillis(),
            dateUpdated = getLong(FIELD_DATE_UPDATED) ?: System.currentTimeMillis(),
            syncedToFirestore = true,
        )
    }
}
