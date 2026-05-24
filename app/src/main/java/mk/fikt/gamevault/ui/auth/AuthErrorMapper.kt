package mk.fikt.gamevault.ui.auth

import androidx.annotation.StringRes
import mk.fikt.gamevault.R
import mk.fikt.gamevault.data.auth.AuthError

@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.USER_NOT_FOUND -> R.string.auth_error_user_not_found
    AuthError.WRONG_PASSWORD -> R.string.auth_error_wrong_password
    AuthError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}
