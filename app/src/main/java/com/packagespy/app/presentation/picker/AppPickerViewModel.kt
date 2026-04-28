package com.packagespy.app.presentation.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packagespy.app.domain.model.InstalledAppSummary
import com.packagespy.app.domain.repository.AppRiskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPickerUiState(
    val isLoading: Boolean = true,
    val showSystem: Boolean = false,
    val query: String = "",
    val results: List<InstalledAppSummary> = emptyList(),
)

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val repository: AppRiskRepository,
) : ViewModel() {

    private val showSystem = MutableStateFlow(false)
    private val query = MutableStateFlow("")
    private val all = MutableStateFlow<List<InstalledAppSummary>>(emptyList())
    private val isLoading = MutableStateFlow(true)

    val state: StateFlow<AppPickerUiState> =
        combine(showSystem, query, all, isLoading) { showSys, q, list, loading ->
            val visible = list.filter { showSys || !it.isSystemApp }
            val filtered = if (q.isBlank()) {
                visible
            } else {
                val needle = q.trim().lowercase()
                visible.filter {
                    it.appName.lowercase().contains(needle) ||
                        it.packageName.lowercase().contains(needle)
                }
            }
            AppPickerUiState(
                isLoading = loading,
                showSystem = showSys,
                query = q,
                results = filtered,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPickerUiState(),
        )

    init {
        // Pre-load both lists once so toggling system filter is instant.
        viewModelScope.launch {
            isLoading.value = true
            all.value = repository.listInstalled(includeSystem = true)
            isLoading.value = false
        }
    }

    fun setShowSystem(value: Boolean) { showSystem.value = value }
    fun setQuery(value: String) { query.value = value }
}
