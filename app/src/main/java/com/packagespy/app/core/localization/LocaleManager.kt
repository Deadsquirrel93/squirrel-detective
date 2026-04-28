package com.packagespy.app.core.localization

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nutspy_locale", Context.MODE_PRIVATE)

    private val _languageCode = MutableStateFlow(DEFAULT_LANGUAGE)
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    init {
        val stored = prefs.getString(KEY_LANGUAGE_CODE, null)
        applyLanguage(stored ?: DEFAULT_LANGUAGE, persist = stored == null)
    }

    fun setLanguage(languageCode: String) {
        applyLanguage(languageCode, persist = true)
    }

    private fun applyLanguage(languageCode: String, persist: Boolean) {
        _languageCode.value = languageCode
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        if (persist) {
            prefs.edit().putString(KEY_LANGUAGE_CODE, languageCode).apply()
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "ru"
        const val ENGLISH = "en"
        const val RUSSIAN = "ru"

        private const val KEY_LANGUAGE_CODE = "language_code"
    }
}
