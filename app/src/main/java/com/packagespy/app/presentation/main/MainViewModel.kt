package com.packagespy.app.presentation.main

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packagespy.app.domain.model.AppDiff
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ScanProgress
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.repository.AppRiskRepository
import com.packagespy.app.domain.usecase.RiskScorer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RiskFilter { ALL, RED, YELLOW, GREEN }

enum class SortMode { BY_RISK, RECENTLY_UPDATED }

enum class CapabilityFilter {
    ANY,
    CAMERA,
    MICROPHONE,
    LOCATION,
    BACKGROUND_LOCATION,
    SMS,
    CONTACTS,
    ACCESSIBILITY,
    NOTIFICATIONS,
    OVERLAY,
    INSTALL_APK,
    ALL_PACKAGES,
}

data class MainUiState(
    val isLoading: Boolean = true,
    val isRescanning: Boolean = false,
    val scanProgress: ScanProgress? = null,
    val query: String = "",
    val filter: RiskFilter = RiskFilter.ALL,
    val capability: CapabilityFilter = CapabilityFilter.ANY,
    val sortMode: SortMode = SortMode.BY_RISK,
    val showSafe: Boolean = false,
    val risky: List<AppRiskInfo> = emptyList(),
    val safe: List<AppRiskInfo> = emptyList(),
    val recentChanges: List<AppDiff> = emptyList(),
)

internal fun matchesCapability(app: AppRiskInfo, capability: CapabilityFilter): Boolean {
    if (capability == CapabilityFilter.ANY) return true
    val granted = app.grantedPermissions
    val declared = app.permissions
    val reasonIds = app.reasons.mapTo(HashSet()) { it.id }
    return when (capability) {
        CapabilityFilter.ANY -> true
        CapabilityFilter.CAMERA -> Manifest.permission.CAMERA in granted
        CapabilityFilter.MICROPHONE -> Manifest.permission.RECORD_AUDIO in granted
        CapabilityFilter.LOCATION ->
            Manifest.permission.ACCESS_FINE_LOCATION in granted ||
                Manifest.permission.ACCESS_COARSE_LOCATION in granted ||
                RiskScorer.ACCESS_BACKGROUND_LOCATION in granted
        CapabilityFilter.BACKGROUND_LOCATION -> RiskScorer.ACCESS_BACKGROUND_LOCATION in granted
        CapabilityFilter.SMS ->
            Manifest.permission.READ_SMS in granted ||
                Manifest.permission.RECEIVE_SMS in granted ||
                Manifest.permission.SEND_SMS in granted ||
                Manifest.permission.RECEIVE_MMS in granted ||
                Manifest.permission.RECEIVE_WAP_PUSH in granted
        CapabilityFilter.CONTACTS -> Manifest.permission.READ_CONTACTS in granted
        CapabilityFilter.ACCESSIBILITY ->
            ThreatId.BIND_ACCESSIBILITY in reasonIds || ThreatId.ACCESSIBILITY_ENABLED in reasonIds
        CapabilityFilter.NOTIFICATIONS ->
            ThreatId.NOTIFICATION_LISTENER in reasonIds || ThreatId.NOTIFICATION_LISTENER_ENABLED in reasonIds
        CapabilityFilter.OVERLAY -> RiskScorer.SYSTEM_ALERT_WINDOW in declared
        CapabilityFilter.INSTALL_APK -> RiskScorer.REQUEST_INSTALL_PACKAGES in declared
        CapabilityFilter.ALL_PACKAGES -> RiskScorer.QUERY_ALL_PACKAGES in declared
    }
}

internal fun matchesQuery(app: AppRiskInfo, query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    return app.appName.contains(trimmed, ignoreCase = true) ||
        app.packageName.contains(trimmed, ignoreCase = true) ||
        app.permissions.any { it.contains(trimmed, ignoreCase = true) }
}

internal fun sortApps(apps: List<AppRiskInfo>, sortMode: SortMode): List<AppRiskInfo> = when (sortMode) {
    SortMode.BY_RISK -> apps.sortedWith(
        compareBy<AppRiskInfo> { it.riskLevel.sortOrder }
            .thenBy { it.appName.lowercase() },
    )
    SortMode.RECENTLY_UPDATED -> apps.sortedWith(
        compareByDescending<AppRiskInfo> { it.lastUpdateTime }
            .thenBy { it.riskLevel.sortOrder }
            .thenBy { it.appName.lowercase() },
    )
}

internal fun filterAndPartitionApps(
    apps: List<AppRiskInfo>,
    query: String,
    riskFilter: RiskFilter,
    capability: CapabilityFilter,
    sortMode: SortMode,
): Pair<List<AppRiskInfo>, List<AppRiskInfo>> {
    val matching = apps.filter { app ->
        matchesQuery(app, query) && matchesCapability(app, capability)
    }
    val sorted = sortApps(matching, sortMode)
    val includeSafeInMainList = query.isNotBlank() || capability != CapabilityFilter.ANY
    if (includeSafeInMainList) {
        val filtered = when (riskFilter) {
            RiskFilter.ALL -> sorted
            RiskFilter.RED -> sorted.filter { it.riskLevel == RiskLevel.RED }
            RiskFilter.YELLOW -> sorted.filter { it.riskLevel == RiskLevel.YELLOW }
            RiskFilter.GREEN -> sorted.filter { it.riskLevel == RiskLevel.GREEN }
        }
        return filtered to emptyList()
    }
    val (risky, safe) = sorted.partition { it.riskLevel != RiskLevel.SAFE }
    val filtered = when (riskFilter) {
        RiskFilter.ALL -> risky
        RiskFilter.RED -> risky.filter { it.riskLevel == RiskLevel.RED }
        RiskFilter.YELLOW -> risky.filter { it.riskLevel == RiskLevel.YELLOW }
        RiskFilter.GREEN -> risky.filter { it.riskLevel == RiskLevel.GREEN }
    }
    return filtered to safe
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRiskRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(RiskFilter.ALL)
    private val capability = MutableStateFlow(CapabilityFilter.ANY)
    private val sortMode = MutableStateFlow(SortMode.BY_RISK)
    private val showSafe = MutableStateFlow(false)
    private val isRescanning = MutableStateFlow(false)
    private val recentChanges = MutableStateFlow<List<AppDiff>>(emptyList())
    private var lastIncludeSystem: Boolean = false
    private var initialScanStarted: Boolean = false

    private data class QueryParams(
        val query: String,
        val filter: RiskFilter,
        val capability: CapabilityFilter,
        val sortMode: SortMode,
        val showSafe: Boolean,
    )

    private val params = combine(
        query,
        filter,
        capability,
        sortMode,
        showSafe,
    ) { q, f, cap, sort, safe ->
        QueryParams(q, f, cap, sort, safe)
    }

    val state: StateFlow<MainUiState> =
        combine(
            repository.observeApps(),
            params,
            combine(isRescanning, repository.observeScanProgress()) { r, p -> r to p },
            recentChanges,
        ) { apps, p, scanState, changes ->
            val (rescanning, scanProgress) = scanState
            val (filtered, safe) = filterAndPartitionApps(
                apps = apps,
                query = p.query,
                riskFilter = p.filter,
                capability = p.capability,
                sortMode = p.sortMode,
            )
            MainUiState(
                isLoading = apps.isEmpty() && rescanning,
                isRescanning = rescanning,
                scanProgress = scanProgress,
                query = p.query,
                filter = p.filter,
                capability = p.capability,
                sortMode = p.sortMode,
                showSafe = p.showSafe,
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

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: RiskFilter) { filter.value = value }
    fun setCapability(value: CapabilityFilter) {
        capability.value = if (capability.value == value) CapabilityFilter.ANY else value
    }
    fun setSortMode(value: SortMode) { sortMode.value = value }
    fun toggleSafe() { showSafe.value = !showSafe.value }

    /** Called when the user expands the changes banner; moves the baseline for those packages. */
    fun onChangesExpanded() {
        val packages = recentChanges.value.map { it.packageName }.toSet()
        if (packages.isEmpty()) return
        viewModelScope.launch {
            repository.acknowledgeChanges(packages)
        }
    }
}
