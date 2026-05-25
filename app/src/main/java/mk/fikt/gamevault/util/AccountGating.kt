package mk.fikt.gamevault.util

import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mk.fikt.gamevault.R
import mk.fikt.gamevault.di.AppContainer
import mk.fikt.gamevault.ui.auth.AuthActivity

/**
 * Anonymous (guest) users can browse the app but cannot perform write actions.
 * Returns true when the current user is signed in anonymously OR not signed in at all.
 */
fun isAnonymousUser(): Boolean {
    val user = AppContainer.authRepository.currentUser() ?: return true
    return user.isAnonymous
}

/**
 * Runs [block] if the user has a real account. If the user is anonymous, shows a dialog
 * inviting them to sign in / sign up. On confirm, signs the anon user out and starts
 * AuthActivity.
 */
fun Fragment.requireAccount(block: () -> Unit) {
    if (!isAnonymousUser()) {
        block()
        return
    }
    val ctx = requireContext()
    MaterialAlertDialogBuilder(ctx)
        .setTitle(R.string.account_required_title)
        .setMessage(R.string.account_required_message)
        .setNegativeButton(R.string.common_cancel, null)
        .setPositiveButton(R.string.account_required_sign_in) { _, _ ->
            AppContainer.authRepository.signOut()
            runCatching { AppContainer.googleSignInHelper.signOut() }
            val intent = Intent(ctx, AuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            activity?.finish()
        }
        .show()
}
