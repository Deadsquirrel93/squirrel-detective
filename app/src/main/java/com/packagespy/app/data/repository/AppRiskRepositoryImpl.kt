package com.packagespy.app.data.repository

import com.packagespy.app.data.local.AppSnapshotDao
import com.packagespy.app.data.local.TrustedAppDao
import com.packagespy.app.data.local.TrustedAppEntity
import com.packagespy.app.data.local.acknowledgedRows
import com.packagespy.app.data.local.baselineRowsToWrite
import com.packagespy.app.data.local.toSnapshot
import com.packagespy.app.data.scanner.PackageScanner
import com.packagespy.app.domain.model.AppDiff
import com.packagespy.app.domain.model.AppRiskInfo
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
    private val trustedAppDao: TrustedAppDao,
    private val diffUseCase: ComputeDiffUseCase,
) : AppRiskRepository {

    private val cache = MutableStateFlow<List<AppRiskInfo>>(emptyList())
    private val progress = MutableStateFlow<ScanProgress?>(null)
    private val scanMutex = Mutex()
    private var lastDiff: List<AppDiff> = emptyList()
    private var pendingPackages: Set<String> = emptySet()

    override fun observeApps() = cache.asStateFlow()

    override fun observeScanProgress() = progress.asStateFlow()

    override suspend fun rescan(includeSystem: Boolean): List<AppRiskInfo> = scanMutex.withLock {
        withContext(Dispatchers.Default) {
            val previous = dao.getAll().map { it.toSnapshot() }
            val trusted = trustedAppDao.getAllPackageNames().toSet()

            progress.value = ScanProgress(current = 0, total = 0)
            try {
                val current = scanner.scanAll(includeSystem, trusted) { done, total ->
                    progress.value = ScanProgress(current = done, total = total)
                }
                val diffs = diffUseCase.diff(previous, current)
                val pending = diffs.map { it.packageName }.toSet()
                val now = System.currentTimeMillis()
                dao.writeBaseline(
                    rows = baselineRowsToWrite(current, pending, now),
                    keep = current.map { it.packageName },
                )
                lastDiff = diffs
                pendingPackages = pending
                cache.value = current
                current
            } finally {
                progress.value = null
            }
        }
    }

    override suspend fun diffSinceLastScan(): List<AppDiff> = lastDiff

    override suspend fun acknowledgeChanges(packageNames: Set<String>) = scanMutex.withLock {
        val toAck = packageNames intersect pendingPackages
        if (toAck.isEmpty()) return@withLock
        withContext(Dispatchers.Default) {
            val now = System.currentTimeMillis()
            dao.upsertAll(acknowledgedRows(cache.value, toAck, now))
        }
        pendingPackages -= toAck
    }

    override suspend fun listInstalled(includeSystem: Boolean): List<InstalledAppSummary> =
        withContext(Dispatchers.Default) {
            scanner.listInstalled(includeSystem)
        }

    override suspend fun scanSingle(packageName: String): AppRiskInfo? =
        withContext(Dispatchers.Default) {
            val trusted = trustedAppDao.getAllPackageNames().toSet()
            scanner.scanSingle(packageName, trusted)
        }

    override suspend fun scanApk(uriString: String): AppRiskInfo? =
        withContext(Dispatchers.IO) {
            scanner.scanApk(uriString)
        }

    override suspend fun setTrusted(packageName: String, trusted: Boolean) = scanMutex.withLock {
        withContext(Dispatchers.Default) {
            if (trusted) {
                trustedAppDao.insert(TrustedAppEntity(packageName, System.currentTimeMillis()))
            } else {
                trustedAppDao.delete(packageName)
            }
            val trustedSet = trustedAppDao.getAllPackageNames().toSet()
            val refreshed = scanner.scanSingle(packageName, trustedSet) ?: return@withContext
            cache.value = cache.value.map { existing ->
                if (existing.packageName == packageName) refreshed else existing
            }
        }
    }
}
