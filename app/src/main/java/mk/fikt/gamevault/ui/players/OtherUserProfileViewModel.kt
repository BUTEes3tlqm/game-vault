package mk.fikt.gamevault.ui.players

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
import mk.fikt.gamevault.data.local.UserProfileEntity
import mk.fikt.gamevault.data.repo.GameRepository
import mk.fikt.gamevault.data.repo.ReviewRepository
import mk.fikt.gamevault.data.repo.UserProfileRepository
import mk.fikt.gamevault.di.AppContainer

class OtherUserProfileViewModel(
    private val userRepo: UserProfileRepository,
    private val gameRepo: GameRepository,
    private val reviewRepo: ReviewRepository,
) : ViewModel() {

    private val uidFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val profile: StateFlow<UserProfileEntity?> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null) else userRepo.observeRemote(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: StateFlow<List<GameEntity>> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else gameRepo.observeGamesFor(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val reviews: StateFlow<List<ReviewEntity>> = uidFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else reviewRepo.observeByAuthor(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(uid: String?) { uidFlow.value = uid }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return OtherUserProfileViewModel(
                AppContainer.userProfileRepository,
                AppContainer.gameRepository,
                AppContainer.reviewRepository,
            ) as T
        }
    }
}
