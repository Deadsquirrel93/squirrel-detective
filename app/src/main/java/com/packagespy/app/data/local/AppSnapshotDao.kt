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

    @Query("SELECT * FROM app_snapshots WHERE packageName = :pkg LIMIT 1")
    suspend fun get(pkg: String): AppSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AppSnapshotEntity>)

    @Query("DELETE FROM app_snapshots WHERE packageName NOT IN (:keep)")
    suspend fun deleteMissing(keep: List<String>)

    @Transaction
    suspend fun replaceAll(items: List<AppSnapshotEntity>) {
        upsertAll(items)
        deleteMissing(items.map { it.packageName })
    }
}
