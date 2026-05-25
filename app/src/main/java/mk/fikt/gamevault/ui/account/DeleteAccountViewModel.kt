package mk.fikt.gamevault.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mk.fikt.gamevault.data.auth.AuthOutcome
import mk.fikt.gamevault.data.auth.AuthRepository
import mk.fikt.gamevault.data.repo.UserProfileRepository
import mk.fikt.gamevault.di.AppContainer

class DeleteAccountViewModel(
    private val authRepo: AuthRepository,
    private val userRepo: UserProfileRepository,
) : ViewModel() {

    enum class ReauthKind { EMAIL_PASSWORD, GOOGLE }

    sealed class State {
        data object Idle : State()
        data object Deleting : State()
        data class RequiresReauth(val kind: ReauthKind) : State()
        data class Error(val message: String? = null) : State()
    }

    sealed class Event {
        data object Done : Event()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    /** Provider info needed by the fragment to render reauth UI. */
    val isAnonymous: Boolean get() = authRepo.currentUser()?.isAnonymous == true
    val userEmail: String? get() = authRepo.currentUser()?.email

    fun deleteAccount() {
        if (_state.value is State.Deleting) return
        viewModelScope.launch {
            _state.value = State.Deleting
            val purged = runCatching { userRepo.purgeOwnData() }.getOrDefault(false)
            if (!purged) {
                _state.value = State.Error()
                return@launch
            }
            performAuthDelete()
        }
    }

    fun submitPassword(password: String) {
        if (password.isBlank()) return
        viewModelScope.launch {
            _state.value = State.Deleting
            val outcome = authRepo.reauthenticateEmail(password)
            if (outcome !is AuthOutcome.Success) {
                _state.value = State.Error()
                return@launch
            }
            performAuthDelete()
        }
    }

    fun submitGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _state.value = State.Deleting
            val outcome = authRepo.reauthenticateGoogle(idToken)
            if (outcome !is AuthOutcome.Success) {
                _state.value = State.Error()
                return@launch
            }
            performAuthDelete()
        }
    }

    fun reauthCancelled() {
        _state.value = State.Idle
    }

    fun dismissError() {
        if (_state.value is State.Error) _state.value = State.Idle
    }

    private suspend fun performAuthDelete() {
        when (val r = authRepo.deleteAccount()) {
            AuthRepository.DeleteOutcome.Success -> _events.send(Event.Done)
            AuthRepository.DeleteOutcome.RequiresRecentLogin -> {
                val user = authRepo.currentUser()
                val kind = when {
                    user == null || user.isAnonymous -> {
                        // Anon shouldn't require reauth; treat as fatal error.
                        _state.value = State.Error()
                        return
                    }
                    !user.email.isNullOrBlank() -> ReauthKind.EMAIL_PASSWORD
                    else -> ReauthKind.GOOGLE
                }
                _state.value = State.RequiresReauth(kind)
            }
            AuthRepository.DeleteOutcome.NotConfigured -> _state.value = State.Error()
            is AuthRepository.DeleteOutcome.Failure -> _state.value = State.Error()
        }
    }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DeleteAccountViewModel(
                AppContainer.authRepository,
                AppContainer.userProfileRepository,
            ) as T
        }
    }
}
