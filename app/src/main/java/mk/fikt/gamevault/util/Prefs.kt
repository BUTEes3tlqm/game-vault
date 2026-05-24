package mk.fikt.gamevault.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat

class Prefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) {
            prefs.edit { putInt(KEY_THEME, value) }
            AppCompatDelegate.setDefaultNightMode(value)
        }

    /** "" = follow system, otherwise BCP 47 tag like "en" or "mk" */
    var languageTag: String
        get() = prefs.getString(KEY_LANGUAGE, "").orEmpty()
        set(value) {
            prefs.edit { putString(KEY_LANGUAGE, value) }
            val locales = if (value.isBlank()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(value)
            AppCompatDelegate.setApplicationLocales(locales)
        }

    fun applyOnStartup() {
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    companion object {
        private const val NAME = "gv_prefs"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language_tag"
    }
}
