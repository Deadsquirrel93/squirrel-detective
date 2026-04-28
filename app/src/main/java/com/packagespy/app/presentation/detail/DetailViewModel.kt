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

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AppRiskRepository
) : ViewModel() {

    private val packageName: String = checkNotNull(savedStateHandle["packageName"])

    private val _app = MutableStateFlow<AppRiskInfo?>(null)
    val app: StateFlow<AppRiskInfo?> = _app.asStateFlow()

    init {
        viewModelScope.launch {
            _app.value = repository.getApp(packageName)
        }
    }
}
