package mk.fikt.gamevault.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mk.fikt.gamevault.data.local.UserProfileEntity
import mk.fikt.gamevault.data.repo.UserProfileRepository
import mk.fikt.gamevault.di.AppContainer

class EditProfileViewModel(
    private val repo: UserProfileRepository,
    private val currentUid: String?,
) : ViewModel() {

    sealed class SaveState {
        data object Idle : SaveState()
        data object Saving : SaveState()
        data object Saved : SaveState()
        data object Error : SaveState()
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    val profile: StateFlow<UserProfileEntity?> = (currentUid?.let { repo.observe(it) }
        ?: kotlinx.coroutines.flow.flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(displayName: String?, photoUrl: String?) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val ok = runCatching { repo.updateOwnProfile(displayName, photoUrl) }
                .getOrDefault(false)
            _saveState.value = if (ok) SaveState.Saved else SaveState.Error
        }
    }

    fun resetState() { _saveState.value = SaveState.Idle }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EditProfileViewModel(
                AppContainer.userProfileRepository,
                AppContainer.authRepository.currentUser()?.uid,
            ) as T
        }
    }
}
