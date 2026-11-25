package com.packagespy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AppSnapshotEntity::class, TrustedAppEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class PackageSpyDatabase : RoomDatabase() {
    abstract fun snapshotDao(): AppSnapshotDao
    abstract fun trustedAppDao(): TrustedAppDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trusted_apps` (
                        `packageName` TEXT NOT NULL,
                        `addedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
