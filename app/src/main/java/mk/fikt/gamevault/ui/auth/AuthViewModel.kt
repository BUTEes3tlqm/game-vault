package mk.fikt.gamevault.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mk.fikt.gamevault.data.auth.AuthOutcome
import mk.fikt.gamevault.data.auth.AuthRepository
import mk.fikt.gamevault.di.AppContainer

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    sealed class UiState {
        data object Idle : UiState()
        data object Loading : UiState()
        data class Error(val outcome: AuthOutcome) : UiState()
        data object Success : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    val isFirebaseConfigured: Boolean get() = repo.isConfigured

    fun signIn(email: String, password: String) = launch(method = "password") {
        repo.signInWithEmail(email.trim(), password)
    }

    fun signUp(email: String, password: String, displayName: String?) = launch(method = "password", isSignUp = true) {
        repo.signUpWithEmail(email.trim(), password, displayName?.trim()?.takeIf { it.isNotBlank() })
    }

    fun signInAnonymously() = launch(method = "anonymous") {
        repo.signInAnonymously()
    }

    fun signInWithGoogleIdToken(idToken: String) = launch(method = "google") {
        repo.signInWithGoogleIdToken(idToken)
    }

    fun reset() {
        _state.value = UiState.Idle
    }

    private fun launch(method: String, isSignUp: Boolean = false, block: suspend () -> AuthOutcome) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val outcome = block()
            if (outcome is AuthOutcome.Success) {
                if (isSignUp) AppContainer.analytics.logSignUp(method)
                else AppContainer.analytics.logSignIn(method)
                AppContainer.analytics.setUserId(outcome.user.uid)
            }
            _state.value = when (outcome) {
                is AuthOutcome.Success -> UiState.Success
                else -> UiState.Error(outcome)
            }
        }
    }

    companion object Factory : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(mk.fikt.gamevault.di.AppContainer.authRepository) as T
        }
    }
}
