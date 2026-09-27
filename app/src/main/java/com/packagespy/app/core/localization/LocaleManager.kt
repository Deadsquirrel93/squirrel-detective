package com.packagespy.app.core.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext context: Context
) {

    init {
        // One-time cleanup of the legacy SharedPreferences file that used to force a
        // language onto every install. Per-app locale state now lives with AppCompat.
        context.deleteSharedPreferences("nutspy_locale")
    }

    /** The currently applied language, or `null` if the app follows the system language. */
    fun currentLanguage(): String? =
        normalizeLanguageTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    /** Sets the app language, or clears the override (follow system) when [code] is `null`. */
    fun setLanguage(code: String?) {
        AppCompatDelegate.setApplicationLocales(
            code?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList()
        )
    }

    companion object {
        const val ENGLISH = "en"
        const val RUSSIAN = "ru"
    }
}
