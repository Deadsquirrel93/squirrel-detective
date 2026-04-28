package com.packagespy.app.di

import android.content.Context
import androidx.room.Room
import com.packagespy.app.data.local.AppSnapshotDao
import com.packagespy.app.data.local.PackageSpyDatabase
import com.packagespy.app.data.repository.AppRiskRepositoryImpl
import com.packagespy.app.domain.repository.AppRiskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PackageSpyDatabase {
        return Room.databaseBuilder(
            context,
            PackageSpyDatabase::class.java,
            "package_spy.db"
        ).build()
    }

    @Provides
    fun provideSnapshotDao(db: PackageSpyDatabase): AppSnapshotDao = db.snapshotDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppRiskRepository(impl: AppRiskRepositoryImpl): AppRiskRepository
}
