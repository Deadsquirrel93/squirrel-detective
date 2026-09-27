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

    @Query("DELETE FROM app_snapshots WHERE packageName NOT IN (:keep)")
    suspend fun deleteMissing(keep: List<String>)

    /**
     * Upserts [rows] (the packages whose baseline moves this scan) and drops
     * every stored row for a package that's no longer installed, keeping
     * [keep] (every currently scanned package, including ones pending
     * acknowledgement whose row isn't in [rows]).
     */
    @Transaction
    suspend fun writeBaseline(rows: List<AppSnapshotEntity>, keep: List<String>) {
        upsertAll(rows)
        deleteMissing(keep)
    }
}
