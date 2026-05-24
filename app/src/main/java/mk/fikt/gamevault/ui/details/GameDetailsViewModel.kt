package mk.fikt.gamevault.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.data.local.ReviewEntity
import mk.fikt.gamevault.data.repo.GameRepository
import mk.fikt.gamevault.data.repo.ReviewRepository
import mk.fikt.gamevault.di.AppContainer

class GameDetailsViewModel(
    private val gameRepo: GameRepository,
    private val reviewRepo: ReviewRepository,
) : ViewModel() {

    private val gameIdFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val game: StateFlow<GameEntity?> = gameIdFlow
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(null) else gameRepo.observeById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val reviewsForGame: StateFlow<List<ReviewEntity>> = game
        .flatMapLatest { g ->
            if (g == null) flowOf(emptyList())
            else reviewRepo.observeByGameTitle(g.title)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(id: String?) { gameIdFlow.value = id }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GameDetailsViewModel(
                AppContainer.gameRepository,
                AppContainer.reviewRepository,
            ) as T
        }
    }
}
