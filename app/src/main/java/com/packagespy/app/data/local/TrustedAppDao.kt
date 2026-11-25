package com.packagespy.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "trusted_apps")
data class TrustedAppEntity(
    @PrimaryKey val packageName: String,
    val addedAtMillis: Long,
)

@Dao
interface TrustedAppDao {

    @Query("SELECT packageName FROM trusted_apps")
    suspend fun getAllPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TrustedAppEntity)

    @Query("DELETE FROM trusted_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
