package com.packagespy.app

import android.app.Application
import com.packagespy.app.core.localization.LocaleManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PackageSpyApp : Application() {

    @Inject lateinit var localeManager: LocaleManager

    override fun onCreate() {
        super.onCreate()
        // Force LocaleManager to initialize early so AppCompat applies the
        // saved locale before the first activity is created.
        localeManager.languageCode.value
    }
}
