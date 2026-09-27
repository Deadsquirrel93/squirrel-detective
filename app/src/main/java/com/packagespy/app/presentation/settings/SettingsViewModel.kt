package com.packagespy.app.presentation.settings

import androidx.lifecycle.ViewModel
import com.packagespy.app.core.localization.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localeManager: LocaleManager,
) : ViewModel() {

    private val _languageCode = MutableStateFlow(localeManager.currentLanguage())
    val languageCode: StateFlow<String?> = _languageCode.asStateFlow()

    fun refresh() {
        _languageCode.value = localeManager.currentLanguage()
    }

    fun setLanguage(code: String?) {
        localeManager.setLanguage(code)
        _languageCode.value = code
    }
}
