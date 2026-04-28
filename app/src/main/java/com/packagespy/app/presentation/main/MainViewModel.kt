package com.packagespy.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packagespy.app.domain.model.AppDiff
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ScanProgress
import com.packagespy.app.domain.repository.AppRiskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RiskFilter { ALL, RED, YELLOW, GREEN }

data class MainUiState(
    val isLoading: Boolean = true,
    val isRescanning: Boolean = false,
    val scanProgress: ScanProgress? = null,
    val filter: RiskFilter = RiskFilter.ALL,
    val showSafe: Boolean = false,
    val risky: List<AppRiskInfo> = emptyList(),
    val safe: List<AppRiskInfo> = emptyList(),
    val recentChanges: List<AppDiff> = emptyList(),
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRiskRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(RiskFilter.ALL)
    private val showSafe = MutableStateFlow(false)
    private val isRescanning = MutableStateFlow(false)
    private val recentChanges = MutableStateFlow<List<AppDiff>>(emptyList())
    private var lastIncludeSystem: Boolean = false
    private var initialScanStarted: Boolean = false

    val state: StateFlow<MainUiState> =
        combine(
            repository.observeApps(),
            filter,
            showSafe,
            combine(isRescanning, repository.observeScanProgress()) { r, p -> r to p },
            recentChanges,
        ) { apps, currentFilter, showSafeFlag, scanState, changes ->
            val (rescanning, scanProgress) = scanState
            val sorted = apps.sortedWith(
                compareBy({ it.riskLevel.sortOrder }, { it.appName.lowercase() })
            )
            val (risky, safe) = sorted.partition { it.riskLevel != RiskLevel.SAFE }
            val filtered = when (currentFilter) {
                RiskFilter.ALL -> risky
                RiskFilter.RED -> risky.filter { it.riskLevel == RiskLevel.RED }
                RiskFilter.YELLOW -> risky.filter { it.riskLevel == RiskLevel.YELLOW }
                RiskFilter.GREEN -> risky.filter { it.riskLevel == RiskLevel.GREEN }
            }
            MainUiState(
                isLoading = apps.isEmpty() && rescanning,
                isRescanning = rescanning,
                scanProgress = scanProgress,
                filter = currentFilter,
                showSafe = showSafeFlag,
                risky = filtered,
                safe = safe,
                recentChanges = changes,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(),
        )

    /** Called once by the screen when it enters with a chosen scan mode. */
    fun ensureInitialScan(includeSystem: Boolean) {
        if (initialScanStarted) return
        initialScanStarted = true
        rescan(includeSystem)
    }

    fun rescan(includeSystem: Boolean = lastIncludeSystem) {
        lastIncludeSystem = includeSystem
        viewModelScope.launch {
            isRescanning.value = true
            try {
                repository.rescan(includeSystem)
                recentChanges.value = repository.diffSinceLastScan()
            } finally {
                isRescanning.value = false
            }
        }
    }

    fun setFilter(value: RiskFilter) { filter.value = value }
    fun toggleSafe() { showSafe.value = !showSafe.value }
}
