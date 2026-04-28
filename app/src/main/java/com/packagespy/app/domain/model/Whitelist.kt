package com.packagespy.app.domain.model

/**
 * Known categories of apps that legitimately need broad package visibility.
 * These cause the risk to be downgraded to GREEN ("explainable").
 */
enum class LegitimateCategory {
    LAUNCHER,
    APP_STORE,
    FILE_MANAGER,
    ANTIVIRUS,
    SYSTEM_TOOL
}

object KnownLegitimateApps {

    /** Common launcher packages — legitimately enumerate apps to render the home screen. */
    val launchers: Set<String> = setOf(
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.android.launcher3",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oneplus.launcher",
        "com.teslacoilsw.launcher",
        "ch.deletescape.lawnchair.plah",
        "com.actionlauncher.playstore",
        "com.microsoft.launcher",
        "org.adw.launcher",
        "com.gau.go.launcherex"
    )

    /** App stores — the whole point of these apps is package management. */
    val stores: Set<String> = setOf(
        "com.android.vending",
        "com.amazon.venezia",
        "com.huawei.appmarket",
        "com.sec.android.app.samsungapps",
        "com.xiaomi.market",
        "ru.vk.store",
        "com.aurora.store",
        "org.fdroid.fdroid",
        "com.fdroid.fdroid",
        "com.aptoide.partners",
        "cm.aptoide.pt"
    )

    /** File managers — list installed apps to show their APKs and clear cache. */
    val fileManagers: Set<String> = setOf(
        "com.google.android.documentsui",
        "com.android.documentsui",
        "com.estrongs.android.pop",
        "com.mi.android.globalFileexplorer",
        "com.mi.android.globalFileexplorer.lite",
        "com.huawei.filemanager",
        "com.sec.android.app.myfiles",
        "com.alphainventor.filemanager",
        "com.simplemobiletools.filemanager",
        "com.simplemobiletools.filemanager.pro",
        "com.marc.files"
    )

    val antivirus: Set<String> = setOf(
        "com.kms.free",                   // Kaspersky
        "com.eset.ems2.gp",               // ESET
        "com.drweb",                      // Dr.Web
        "com.bitdefender.security",
        "com.avast.android.mobilesecurity",
        "com.avg.android.antivirus",
        "com.norton.snap",
        "com.norton.mobilesecurity",
        "com.trendmicro.tmmspersonal",
        "com.malwarebytes.antimalware"
    )

    fun categorize(packageName: String): LegitimateCategory? = when (packageName) {
        in launchers -> LegitimateCategory.LAUNCHER
        in stores -> LegitimateCategory.APP_STORE
        in fileManagers -> LegitimateCategory.FILE_MANAGER
        in antivirus -> LegitimateCategory.ANTIVIRUS
        else -> null
    }
}
