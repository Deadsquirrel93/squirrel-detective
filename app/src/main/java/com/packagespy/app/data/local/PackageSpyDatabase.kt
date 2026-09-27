package com.packagespy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AppSnapshotEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PackageSpyDatabase : RoomDatabase() {
    abstract fun snapshotDao(): AppSnapshotDao
}
