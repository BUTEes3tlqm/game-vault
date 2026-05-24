package mk.fikt.gamevault.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import mk.fikt.gamevault.data.local.GenreCountRow
import mk.fikt.gamevault.data.local.StatusCountRow
import mk.fikt.gamevault.data.model.GameStatus
import mk.fikt.gamevault.data.repo.GameRepository
import mk.fikt.gamevault.di.AppContainer

class StatisticsViewModel(private val repo: GameRepository) : ViewModel() {

    data class UiState(
        val totalGames: Int = 0,
        val completed: Int = 0,
        val totalHours: Double = 0.0,
        val statusCounts: Map<GameStatus, Int> = emptyMap(),
        val topGenres: List<GenreCountRow> = emptyList(),
    ) {
        val isEmpty: Boolean get() = totalGames == 0
    }

    val state: StateFlow<UiState> = combine(
        repo.observeCount(),
        repo.observeCompletedCount(),
        repo.observeTotalHours(),
        repo.observeStatusCounts(),
        repo.observeGenreCounts(),
    ) { total, completed, hours, statusRows, genres ->
        UiState(
            totalGames = total,
            completed = completed,
            totalHours = hours,
            statusCounts = statusRows.toStatusMap(),
            topGenres = genres,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun List<StatusCountRow>.toStatusMap(): Map<GameStatus, Int> =
        associate { GameStatus.fromName(it.status) to it.count }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(AppContainer.gameRepository) as T
        }
    }
}
