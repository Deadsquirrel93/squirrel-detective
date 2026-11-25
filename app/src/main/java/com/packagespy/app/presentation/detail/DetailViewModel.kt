package com.packagespy.app.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.repository.AppRiskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Loaded(val app: AppRiskInfo) : DetailUiState
    data class NotFound(val isArchive: Boolean = false) : DetailUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AppRiskRepository,
) : ViewModel() {

    private val packageName: String? = savedStateHandle["packageName"]
    private val apkUri: String? = savedStateHandle["apkUri"]

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()
    private var hasPaused: Boolean = false

    init {
        refresh()
    }

    fun onPaused() {
        hasPaused = true
    }

    /**
     * Re-scans installed apps after returning from system settings (where the
     * user may have revoked a permission or turned off an Accessibility service).
     * Skipped on first entry (already loaded in init) and for APK archive URIs.
     */
    fun refreshOnResume() {
        if (!hasPaused || apkUri != null) return
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val loaded = when {
                apkUri != null -> repository.scanApk(apkUri)
                packageName != null -> repository.scanSingle(packageName)
                else -> null
            }
            _state.value = loaded
                ?.let { DetailUiState.Loaded(it) }
                ?: DetailUiState.NotFound(isArchive = apkUri != null)
        }
    }

    fun toggleTrusted() {
        val current = (_state.value as? DetailUiState.Loaded)?.app ?: return
        if (current.isArchive) return
        viewModelScope.launch {
            repository.setTrusted(current.packageName, !current.isUserTrusted)
            _state.value = repository.scanSingle(current.packageName)
                ?.let { DetailUiState.Loaded(it) }
                ?: DetailUiState.NotFound(isArchive = false)
        }
    }
}
