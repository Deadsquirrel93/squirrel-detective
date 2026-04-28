package com.packagespy.app.presentation.settings

import androidx.lifecycle.ViewModel
import com.packagespy.app.core.localization.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localeManager: LocaleManager,
) : ViewModel() {

    val languageCode: StateFlow<String> = localeManager.languageCode

    fun setLanguage(code: String) {
        localeManager.setLanguage(code)
    }
}
