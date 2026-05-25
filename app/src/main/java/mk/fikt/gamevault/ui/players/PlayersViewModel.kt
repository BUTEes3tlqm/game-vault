package mk.fikt.gamevault.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import mk.fikt.gamevault.data.local.UserProfileEntity
import mk.fikt.gamevault.data.repo.UserProfileRepository
import mk.fikt.gamevault.di.AppContainer

class PlayersViewModel(
    private val repo: UserProfileRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val players: StateFlow<List<UserProfileEntity>> = query
        .flatMapLatest { repo.observeOthers(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { query.value = q }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PlayersViewModel(AppContainer.userProfileRepository) as T
        }
    }
}
