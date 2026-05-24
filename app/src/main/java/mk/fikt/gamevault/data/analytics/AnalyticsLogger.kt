package mk.fikt.gamevault.data.analytics

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsLogger(private val firebaseAvailable: Boolean) {

    private val analytics: FirebaseAnalytics? by lazy {
        if (!firebaseAvailable) null
        else runCatching { FirebaseAnalytics.getInstance(mk.fikt.gamevault.di.AppContainer.appContextOrNull()!!) }.getOrNull()
    }

    fun logSignIn(method: String) = log("login", bundleOf(FirebaseAnalytics.Param.METHOD to method))
    fun logSignUp(method: String) = log("sign_up", bundleOf(FirebaseAnalytics.Param.METHOD to method))
    fun logGameAdded(status: String) = log("game_added", bundleOf("status" to status))
    fun logGameStatusChanged(status: String) = log("game_status_changed", bundleOf("status" to status))
    fun logReviewPosted(rating: Float) = log("review_posted", bundleOf("rating" to rating.toDouble()))
    fun logLanguageChanged(locale: String) = log("language_changed", bundleOf("locale" to locale))
    fun logThemeChanged(mode: String) = log("theme_changed", bundleOf("mode" to mode))

    fun setUserId(uid: String?) {
        analytics?.setUserId(uid)
    }

    private fun log(name: String, params: Bundle) {
        analytics?.logEvent(name, params)
    }
}
