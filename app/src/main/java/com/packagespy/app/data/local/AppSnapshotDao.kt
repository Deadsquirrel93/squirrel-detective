package com.packagespy.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AppSnapshotDao {

    @Query("SELECT * FROM app_snapshots")
    suspend fun getAll(): List<AppSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AppSnapshotEntity>)

    @Query("SELECT packageName FROM app_snapshots")
    suspend fun getAllPackageNames(): List<String>

    @Query("DELETE FROM app_snapshots WHERE packageName IN (:packages)")
    suspend fun deleteByPackages(packages: List<String>)

    /**
     * Upserts [rows] (the packages whose baseline moves this scan) and drops
     * every stored row for a package that's no longer installed, keeping
     * [keep] (every currently scanned package, including ones pending
     * acknowledgement whose row isn't in [rows]).
     *
     * Deletes in chunks to stay below SQLite's 999 bind-parameter limit on API 26–29.
     */
    @Transaction
    suspend fun writeBaseline(rows: List<AppSnapshotEntity>, keep: List<String>) {
        upsertAll(rows)
        val keepSet = keep.toSet()
        val missing = getAllPackageNames().filter { it !in keepSet }
        missing.chunked(500).forEach { chunk -> deleteByPackages(chunk) }
    }
}
