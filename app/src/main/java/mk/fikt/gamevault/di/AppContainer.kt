package mk.fikt.gamevault.di

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mk.fikt.gamevault.data.analytics.AnalyticsLogger
import mk.fikt.gamevault.data.auth.AuthRepository
import mk.fikt.gamevault.data.auth.GoogleSignInHelper
import mk.fikt.gamevault.data.local.AppDatabase
import mk.fikt.gamevault.data.repo.GameRepository
import mk.fikt.gamevault.data.repo.ReviewRepository
import mk.fikt.gamevault.data.repo.UserProfileRepository
import mk.fikt.gamevault.util.Prefs

/**
 * Service Locator. Initialized once in [mk.fikt.gamevault.GameVaultApplication.onCreate].
 * Lazily exposes singletons used across the app.
 */
object AppContainer {

    private const val TAG = "AppContainer"

    private lateinit var appContext: Context

    @Volatile private var initialized = false
    var firebaseAvailable: Boolean = false
        private set

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val authRepository: AuthRepository by lazy { AuthRepository(firebaseAvailable) }
    val googleSignInHelper: GoogleSignInHelper by lazy { GoogleSignInHelper(appContext) }
    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(
            database.userProfileDao(),
            database.gameDao(),
            database.reviewDao(),
            authRepository,
            firebaseAvailable,
            applicationScope,
        )
    }
    val gameRepository: GameRepository by lazy {
        GameRepository(
            database.gameDao(),
            authRepository,
            firebaseAvailable,
            applicationScope,
        ).also { repo ->
            repo.onCountChanged = { count ->
                userProfileRepository.updateGameCount(count)
            }
        }
    }
    val reviewRepository: ReviewRepository by lazy {
        ReviewRepository(database.reviewDao(), firebaseAvailable, applicationScope)
    }
    val analytics: AnalyticsLogger by lazy { AnalyticsLogger(firebaseAvailable) }
    val prefs: Prefs by lazy { Prefs(appContext) }

    fun appContextOrNull(): Context? = if (initialized) appContext else null

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            firebaseAvailable = try {
                FirebaseApp.initializeApp(appContext) != null
            } catch (e: Throwable) {
                Log.w(TAG, "Firebase not configured (google-services.json missing?): ${e.message}")
                false
            }
            initialized = true
        }
    }
}
