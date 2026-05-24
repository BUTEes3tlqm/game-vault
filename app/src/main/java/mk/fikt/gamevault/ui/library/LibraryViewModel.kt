package mk.fikt.gamevault.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import mk.fikt.gamevault.data.local.GameEntity
import mk.fikt.gamevault.data.model.GameStatus
import mk.fikt.gamevault.data.repo.GameRepository
import mk.fikt.gamevault.di.AppContainer

data class LibraryFilter(
    val status: GameStatus? = null,
    val query: String = "",
)

class LibraryViewModel(private val repo: GameRepository) : ViewModel() {

    private val _filter = MutableStateFlow(LibraryFilter())
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: StateFlow<List<GameEntity>> = _filter
        .flatMapLatest { f -> repo.observeFiltered(f.status, f.query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> = repo.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setStatusFilter(status: GameStatus?) {
        _filter.value = _filter.value.copy(status = status)
    }

    fun setQuery(query: String) {
        _filter.value = _filter.value.copy(query = query)
    }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(AppContainer.gameRepository) as T
        }
    }
}
