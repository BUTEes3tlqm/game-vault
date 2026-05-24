package mk.fikt.gamevault.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Wraps FirebaseAuth. When [firebaseAvailable] is false (google-services.json missing or
 * Firebase init failed), every method returns [AuthOutcome.NotConfigured] so the UI can
 * show a friendly message instead of crashing.
 */
class AuthRepository(private val firebaseAvailable: Boolean) {

    private val auth: FirebaseAuth?
        get() = if (firebaseAvailable) FirebaseAuth.getInstance() else null

    val isConfigured: Boolean get() = firebaseAvailable

    val currentUserFlow: Flow<AuthUser?> = if (!firebaseAvailable) {
        flowOf(null)
    } else {
        callbackFlow {
            val a = FirebaseAuth.getInstance()
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
            a.addAuthStateListener(listener)
            trySend(a.currentUser?.toAuthUser())
            awaitClose { a.removeAuthStateListener(listener) }
        }
    }

    fun currentUser(): AuthUser? = auth?.currentUser?.toAuthUser()

    suspend fun signInWithEmail(email: String, password: String): AuthOutcome {
        val a = auth ?: return AuthOutcome.NotConfigured
        return runCatching { a.signInWithEmailAndPassword(email, password).await() }
            .toOutcome()
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String?): AuthOutcome {
        val a = auth ?: return AuthOutcome.NotConfigured
        return runCatching {
            val res = a.createUserWithEmailAndPassword(email, password).await()
            if (!displayName.isNullOrBlank()) {
                res.user?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                )?.await()
            }
            res
        }.toOutcome()
    }

    suspend fun signInAnonymously(): AuthOutcome {
        val a = auth ?: return AuthOutcome.NotConfigured
        return runCatching { a.signInAnonymously().await() }.toOutcome()
    }

    suspend fun signInWithGoogleIdToken(idToken: String): AuthOutcome {
        val a = auth ?: return AuthOutcome.NotConfigured
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return runCatching { a.signInWithCredential(credential).await() }.toOutcome()
    }

    fun signOut() {
        auth?.signOut()
    }

    private fun Result<com.google.firebase.auth.AuthResult>.toOutcome(): AuthOutcome =
        fold(
            onSuccess = { res ->
                res.user?.toAuthUser()?.let { AuthOutcome.Success(it) }
                    ?: AuthOutcome.Failure(AuthError.UNKNOWN)
            },
            onFailure = { e -> AuthOutcome.Failure(e.toAuthError()) },
        )

    private fun Throwable.toAuthError(): AuthError = when (this) {
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException ->
            if (message?.contains("email", ignoreCase = true) == true) AuthError.INVALID_EMAIL
            else AuthError.WRONG_PASSWORD
        is FirebaseAuthInvalidUserException -> AuthError.USER_NOT_FOUND
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
        is FirebaseNetworkException -> AuthError.NETWORK
        else -> AuthError.UNKNOWN
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl?.toString(),
    isAnonymous = isAnonymous,
)
