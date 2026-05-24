package mk.fikt.gamevault.data.auth

sealed class AuthOutcome {
    data class Success(val user: AuthUser) : AuthOutcome()
    data class Failure(val error: AuthError) : AuthOutcome()
    data object NotConfigured : AuthOutcome()
}

enum class AuthError {
    INVALID_EMAIL,
    WEAK_PASSWORD,
    USER_NOT_FOUND,
    WRONG_PASSWORD,
    EMAIL_IN_USE,
    NETWORK,
    UNKNOWN,
}

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean,
)
