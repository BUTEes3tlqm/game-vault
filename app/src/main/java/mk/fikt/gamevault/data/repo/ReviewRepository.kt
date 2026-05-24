package mk.fikt.gamevault.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mk.fikt.gamevault.data.local.ReviewDao
import mk.fikt.gamevault.data.local.ReviewEntity
import mk.fikt.gamevault.data.remote.FirestoreSchema.COLLECTION_REVIEWS
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_AUTHOR_NAME
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_AUTHOR_UID
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_CREATED_AT
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_GAME_ID
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_GAME_TITLE
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_ID
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_RATING
import mk.fikt.gamevault.data.remote.FirestoreSchema.FIELD_TEXT

class ReviewRepository(
    private val reviewDao: ReviewDao,
    private val firebaseAvailable: Boolean,
    private val scope: CoroutineScope,
) {

    private val firestore: FirebaseFirestore? by lazy {
        if (!firebaseAvailable) null
        else runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    init {
        startRemoteSync()
    }

    fun observeRecent(limit: Int = 200): Flow<List<ReviewEntity>> =
        reviewDao.observeRecent(limit)

    fun observeByAuthor(uid: String): Flow<List<ReviewEntity>> =
        reviewDao.observeByAuthor(uid)

    fun observeByGameTitle(title: String): Flow<List<ReviewEntity>> =
        reviewDao.observeByGameTitle(title)

    suspend fun postReview(review: ReviewEntity) {
        reviewDao.upsert(review)
        val fs = firestore ?: return
        runCatching {
            fs.collection(COLLECTION_REVIEWS)
                .document(review.id)
                .set(review.toFirestoreMap())
                .await()
            reviewDao.upsert(review.copy(syncedToFirestore = true))
        }
    }

    suspend fun deleteReview(id: String) {
        reviewDao.deleteById(id)
        val fs = firestore ?: return
        runCatching { fs.collection(COLLECTION_REVIEWS).document(id).delete().await() }
    }

    private fun startRemoteSync() {
        val fs = firestore ?: return
        fs.collection(COLLECTION_REVIEWS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val reviews = snap.documents.mapNotNull { it.toReviewEntity() }
                if (reviews.isNotEmpty()) {
                    scope.launch { reviewDao.upsertAll(reviews) }
                }
            }
    }

    private fun ReviewEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_GAME_TITLE to gameTitle,
        FIELD_GAME_ID to gameId,
        FIELD_AUTHOR_UID to authorUid,
        FIELD_AUTHOR_NAME to authorName,
        FIELD_RATING to rating,
        FIELD_TEXT to text,
        FIELD_CREATED_AT to createdAt,
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toReviewEntity(): ReviewEntity? {
        val title = getString(FIELD_GAME_TITLE) ?: return null
        val authorUid = getString(FIELD_AUTHOR_UID) ?: return null
        val authorName = getString(FIELD_AUTHOR_NAME) ?: ""
        val text = getString(FIELD_TEXT) ?: ""
        val rating = getDouble(FIELD_RATING)?.toFloat() ?: 0f
        val createdAt = getLong(FIELD_CREATED_AT) ?: getTimestamp(FIELD_CREATED_AT)?.toDate()?.time
            ?: System.currentTimeMillis()
        return ReviewEntity(
            id = id,
            gameTitle = title,
            gameId = getString(FIELD_GAME_ID),
            authorUid = authorUid,
            authorName = authorName,
            rating = rating,
            text = text,
            createdAt = createdAt,
            syncedToFirestore = true,
        )
    }
}
