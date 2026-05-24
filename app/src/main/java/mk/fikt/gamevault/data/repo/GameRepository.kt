package mk.fikt.gamevault.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mk.fikt.gamevault.data.auth.AuthRepository
import mk.fikt.gamevault.data.local.GameDao
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.data.local.GenreCountRow
import mk.fikt.gamevault.data.local.StatusCountRow
import mk.fikt.gamevault.data.model.GameStatus

class GameRepository(
    private val gameDao: GameDao,
    private val authRepository: AuthRepository,
) {

    private val currentUid: String? get() = authRepository.currentUser()?.uid

    fun observeFiltered(status: GameStatus?, query: String): Flow<List<GameEntity>> {
        val uid = currentUid ?: return flowOf(emptyList())
        return gameDao.observeFiltered(uid, status?.name, query.trim())
    }

    fun observeAll(): Flow<List<GameEntity>> {
        val uid = currentUid ?: return flowOf(emptyList())
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
        gameDao.upsert(
            game.copy(
                ownerUid = game.ownerUid.ifBlank { currentUid.orEmpty() },
                dateUpdated = System.currentTimeMillis(),
            )
        )
    }

    suspend fun delete(id: String) = gameDao.deleteById(id)
}
