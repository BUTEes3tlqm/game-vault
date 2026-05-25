package mk.fikt.gamevault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import mk.fikt.gamevault.data.model.GameStatus

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE ownerUid = :uid ORDER BY dateUpdated DESC")
    fun observeAll(uid: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE ownerUid = :uid AND status = :status ORDER BY dateUpdated DESC")
    fun observeByStatus(uid: String, status: GameStatus): Flow<List<GameEntity>>

    @Query("""
        SELECT * FROM games
        WHERE ownerUid = :uid
          AND (title LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%')
        ORDER BY dateUpdated DESC
    """)
    fun search(uid: String, query: String): Flow<List<GameEntity>>

    @Query("""
        SELECT * FROM games
        WHERE ownerUid = :uid
          AND (:status IS NULL OR status = :status)
          AND (:query = '' OR title LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%')
        ORDER BY dateUpdated DESC
    """)
    fun observeFiltered(uid: String, status: String?, query: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeById(id: String): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: String): GameEntity?

    @Query("SELECT COUNT(*) FROM games WHERE ownerUid = :uid")
    fun observeCount(uid: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM games WHERE ownerUid = :uid")
    suspend fun countNow(uid: String): Int

    @Query("SELECT COUNT(*) FROM games WHERE ownerUid = :uid AND status = :status")
    fun observeCountByStatus(uid: String, status: GameStatus): Flow<Int>

    @Query("SELECT IFNULL(SUM(hoursPlayed), 0.0) FROM games WHERE ownerUid = :uid")
    fun observeTotalHours(uid: String): Flow<Double>

    @Query("SELECT status, COUNT(*) as count FROM games WHERE ownerUid = :uid GROUP BY status")
    fun observeStatusCounts(uid: String): Flow<List<StatusCountRow>>

    @Query("""
        SELECT genre, COUNT(*) as count FROM games
        WHERE ownerUid = :uid AND genre IS NOT NULL AND genre != ''
        GROUP BY genre ORDER BY count DESC LIMIT 6
    """)
    fun observeGenreCounts(uid: String): Flow<List<GenreCountRow>>

    @Query("SELECT * FROM games WHERE syncedToFirestore = 0 AND ownerUid = :uid")
    suspend fun getUnsynced(uid: String): List<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<GameEntity>)

    @Update
    suspend fun update(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM games WHERE ownerUid = :uid")
    suspend fun deleteAllForOwner(uid: String)
}
