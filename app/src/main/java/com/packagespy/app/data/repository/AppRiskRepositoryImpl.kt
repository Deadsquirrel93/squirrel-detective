package com.packagespy.app.data.repository

import com.packagespy.app.data.local.AppSnapshotDao
import com.packagespy.app.data.local.toEntity
import com.packagespy.app.data.local.toSnapshot
import com.packagespy.app.data.scanner.PackageScanner
import com.packagespy.app.domain.model.AppDiff
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.AppSnapshot
import com.packagespy.app.domain.model.InstalledAppSummary
import com.packagespy.app.domain.model.ScanProgress
import com.packagespy.app.domain.repository.AppRiskRepository
import com.packagespy.app.domain.usecase.ComputeDiffUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRiskRepositoryImpl @Inject constructor(
    private val scanner: PackageScanner,
    private val dao: AppSnapshotDao,
    private val diffUseCase: ComputeDiffUseCase,
) : AppRiskRepository {

    private val cache = MutableStateFlow<List<AppRiskInfo>>(emptyList())
    private val progress = MutableStateFlow<ScanProgress?>(null)
    private val scanMutex = Mutex()
    private var lastSnapshot: List<AppSnapshot> = emptyList()

    override fun observeApps() = cache.asStateFlow()

    override fun observeScanProgress() = progress.asStateFlow()

    override suspend fun rescan(includeSystem: Boolean): List<AppRiskInfo> = scanMutex.withLock {
        withContext(Dispatchers.Default) {
            val previous = dao.getAll().map { it.toSnapshot() }
            lastSnapshot = previous

            progress.value = ScanProgress(current = 0, total = 0)
            try {
                val current = scanner.scanAll(includeSystem) { done, total ->
                    progress.value = ScanProgress(current = done, total = total)
                }
                val now = System.currentTimeMillis()
                dao.replaceAll(current.map { it.toEntity(now) })
                cache.value = current
                current
            } finally {
                progress.value = null
            }
        }
    }

    override suspend fun diffSinceLastScan(): List<AppDiff> = withContext(Dispatchers.Default) {
        diffUseCase.diff(lastSnapshot, cache.value)
    }

    override suspend fun listInstalled(includeSystem: Boolean): List<InstalledAppSummary> =
        withContext(Dispatchers.Default) {
            scanner.listInstalled(includeSystem)
        }

    override suspend fun scanSingle(packageName: String): AppRiskInfo? =
        withContext(Dispatchers.Default) {
            scanner.scanSingle(packageName)
        }
}
