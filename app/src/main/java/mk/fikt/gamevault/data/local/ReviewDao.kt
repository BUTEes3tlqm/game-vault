package mk.fikt.gamevault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE authorUid = :uid ORDER BY createdAt DESC")
    fun observeByAuthor(uid: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE gameTitle = :title ORDER BY createdAt DESC")
    fun observeByGameTitle(title: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE id = :id")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE syncedToFirestore = 0")
    suspend fun getUnsynced(): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reviews: List<ReviewEntity>)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reviews WHERE authorUid = :uid")
    suspend fun deleteAllByAuthor(uid: String)
}
