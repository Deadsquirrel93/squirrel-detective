# Squirrel Detective

Squirrel Detective is a fully offline Android auditor for the apps already installed on your phone. It enumerates installed packages locally and highlights apps that can silently observe what else is installed and how the device is used. It is built around a privacy-first model: the app makes no network calls, contains no analytics, and stores nothing outside the device.

Squirrel Detective is published as an open source project. It does not include ads, tracking, analytics, subscriptions, or built-in monetization.

License: `MIT`

Official source: [github.com/Deadsquirrel93/squirrel-detective](https://github.com/Deadsquirrel93/squirrel-detective)

Official Squirrel Detective builds and updates are distributed only through this repository. If an APK or update comes from any other source, it should not be treated as an official release.

Read this in Russian: [README.ru.md](README.ru.md).

## What Squirrel Detective Does

Squirrel Detective lets you:

- scan every installed app on the device, with an option to include system apps
- inspect a single chosen app instead of running a full scan
- see a per-app risk level (`HIGH` / `MEDIUM` / `EXPECTED` / `SAFE`) with a plain-language explanation of every finding
- read every Android permission an app declares
- read every package-related broadcast receiver an app declares (`PACKAGE_ADDED` / `PACKAGE_REMOVED` / `BOOT_COMPLETED`)
- jump from any report straight into Android system settings for that app, to revoke permissions or uninstall it
- compare scans: when an app gains new sensitive permissions between scans, it is flagged automatically
- browse a built-in Permission Wiki that explains, in plain language, what each Android permission actually grants
- switch the entire interface between Russian and English at any time

## What Squirrel Detective Detects

The detector does not rely on a remote signature feed; everything is decided locally based on what is in the app manifest.

### Risky permission combinations

- `QUERY_ALL_PACKAGES + INTERNET` — can enumerate every installed app and send the list to a remote server
- `READ_LOGS` (especially with `INTERNET`) — can observe app launches and intents on non-rooted devices
- `BIND_ACCESSIBILITY_SERVICE` — the most powerful surveillance channel on Android (reads any screen, emulates taps)
- `BIND_DEVICE_ADMIN` — device-administrator privileges (lock, password policy, on older Android — wipe)
- `BIND_NOTIFICATION_LISTENER_SERVICE` — reads every notification, including SMS codes and bank alerts
- `BIND_VPN_SERVICE` — routes the entire device traffic through the app
- `BIND_INPUT_METHOD` — keyboard service that sees every keystroke
- `SYSTEM_ALERT_WINDOW` — overlays drawn on top of other apps
- `PACKAGE_USAGE_STATS` — usage history of every app
- `REQUEST_INSTALL_PACKAGES` — install APKs outside Google Play
- `RECORD_AUDIO + INTERNET`, `CAMERA + INTERNET`
- SMS group (`READ_SMS`, `RECEIVE_SMS`, `SEND_SMS`, `RECEIVE_MMS`, `RECEIVE_WAP_PUSH`)
- Call log group (`READ_CALL_LOG`, `WRITE_CALL_LOG`, `PROCESS_OUTGOING_CALLS`)
- `READ_CONTACTS + INTERNET`
- Location group (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`) with `INTERNET`
- `READ_PHONE_STATE` / `READ_PHONE_NUMBERS`
- `MANAGE_EXTERNAL_STORAGE`
- `WRITE_SETTINGS` / `WRITE_SECURE_SETTINGS`
- `GET_TASKS` / `REAL_GET_TASKS` declared by non-system apps
- `GET_ACCOUNTS + INTERNET`

### Suspicious broadcast receivers

- `ACTION_PACKAGE_ADDED` — the app is notified each time anything is installed
- `ACTION_PACKAGE_REMOVED` — the app is notified each time anything is uninstalled
- `BOOT_COMPLETED` combined with package-list or usage-stats access — typical sign of background scanning

### Other build / install signals

- app marked as `debuggable` in the release manifest
- app targeting an outdated SDK (below API 23) and bypassing the runtime permission model
- hidden app (no launcher activity) combined with sensitive permissions — typical stalkerware pattern
- sideloaded app (unknown installer) with multiple risky permissions

### Whitelist

Launchers, app stores, file managers, antivirus apps and well-known system tools are recognised and downgraded to `EXPECTED` (green) with an explanation, so the report stays focused on apps that genuinely deserve a closer look.

## Product Highlights

- Offline by design: no backend, no telemetry, no network dependency
- Open source and transparent
- No ads, no analytics, no subscriptions, no in-app monetization
- Official distribution only from this repository and its GitHub Releases
- Bilingual interface: Russian and English, switchable in Settings
- Modern Android stack: Kotlin, Jetpack Compose, Material 3
- Diff between scans, persisted locally with Room
- Built-in plain-language Permission Wiki — no need to google what `BIND_ACCESSIBILITY_SERVICE` means

## Privacy Model

Squirrel Detective is built around a strict offline, observe-only model:

- the app has no `INTERNET` permission and makes no network calls of any kind
- nothing about the scanned apps ever leaves the device
- the only persistent storage is a local Room database that holds the previous scan snapshot, used to compute diffs
- the app declares `QUERY_ALL_PACKAGES` only because it is the only way Android 11+ exposes the full installed-package list — that is the entire reason this app exists; without it, an auditor cannot do its job

## Main Features

### Full scan

Walks through every installed package, classifies it, and presents the report grouped by risk level. System apps can be optionally included.

### Single-app scan

Pick one specific installed app and review its permissions, receivers and risk reasons without scanning the whole device.

### Per-app detail screen

For every app the report shows:

- aggregated risk level
- every detected reason, with a short title and a longer plain-language explanation
- the full list of declared permissions (selectable text — copy any permission with one tap)
- broadcast receivers and the actions they listen to
- a one-tap shortcut to the Android system settings for that app, to revoke permissions or uninstall it

### Diff between scans

The previous snapshot is stored in a local Room database. After a fresh scan, apps that have gained new sensitive permissions are surfaced explicitly.

### Background re-scan

A `WorkManager` job and a `PackageChangeReceiver` listen for newly installed or upgraded packages, so the cached report stays in sync without a manual rescan.

### Permission Wiki

A built-in reference for what each Android permission actually grants — grouped into Observation, Powerful service binds, Sensors, Communication, Location, Storage, and System. Every entry covers what the permission allows, why apps legitimately request it, and when its presence is suspicious.

### Localization

Russian and English. The current language is applied immediately and persists across launches.

## Tech Stack

- Kotlin 2.4 (AGP built-in Kotlin), Gradle 9.8, Android Gradle Plugin 9.4
- Jetpack Compose (BOM 2026.09.00, Material 3), edge-to-edge UI
- Hilt 2.60, KSP 2.3
- Room 2.8
- WorkManager + Broadcast receiver for incremental package change tracking
- AndroidX Navigation Compose
- minSdk 26, targetSdk 37, compileSdk 37
- JUnit unit tests for the risk scorer and scan diff

## Project Structure

- `app/src/main/java/com/packagespy/app/PackageSpyApp.kt` — application entry, Hilt + WorkManager wiring
- `app/src/main/java/com/packagespy/app/di` — Hilt modules
- `app/src/main/java/com/packagespy/app/domain` — domain models, repository contracts, risk-scoring use case
- `app/src/main/java/com/packagespy/app/data/scanner` — `PackageScanner` (wrapper around `PackageManager`, with progress callback)
- `app/src/main/java/com/packagespy/app/data/local` — Room database, entity, DAO, mapper
- `app/src/main/java/com/packagespy/app/data/repository` — `AppRiskRepository` implementation
- `app/src/main/java/com/packagespy/app/data/work` — background `RescanWorker` and `PackageChangeReceiver`
- `app/src/main/java/com/packagespy/app/presentation` — Compose screens (welcome, picker, main report, detail, settings, wiki) and theme

## Build Requirements

- A recent Android Studio release with Android Gradle Plugin 9.4 support
- JDK 17 or newer (the JBR shipped with Android Studio works out of the box)
- Android SDK platform 37

## Build and Run

Debug build:

```bash
./gradlew :app:assembleDebug
```

Release build:

```bash
./gradlew :app:assembleRelease
```

Install on a connected device:

```bash
./gradlew :app:installDebug
```

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Output paths:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

## APK Signing

Both the debug and release APKs are signed with the standard Android debug keystore (`~/.android/debug.keystore`). This is intentional for sideloaded distribution: a fresh self-signed release certificate would have no Play Protect reputation and trigger a "harmful app" warning on first install for every user. The debug key is well-known to Android and Play Protect, which keeps the sideload install path frictionless.

The trade-off is that the debug key is not unique to this project — anyone with the same debug key can publish a build that installs as an "update" over yours. For a small, source-available, sideloaded utility this is an acceptable trade.

## Official Distribution

The only official source for Squirrel Detective code, APK files, and updates is:

- [https://github.com/Deadsquirrel93/squirrel-detective](https://github.com/Deadsquirrel93/squirrel-detective)

If you install a build from another website, Telegram channel, mirror, or repackaged APK source, it is not an official Squirrel Detective release. Squirrel Detective is **not** distributed through Google Play — `QUERY_ALL_PACKAGES` is restricted on Play, and the app cannot do its job without it.

## Installation

1. Download the APK from the latest [GitHub Release](https://github.com/Deadsquirrel93/squirrel-detective/releases).
2. On the device, allow installation from the source you used to download it (browser or file manager) when prompted.
3. Open the APK to install.
4. Launch Squirrel Detective and tap **Start scan**.

Required permissions on the device side:

- `QUERY_ALL_PACKAGES` — declared automatically; no user action required
- nothing else

## Limitations

- Squirrel Detective sees only what is in the manifest of each installed app. It does not, and by design cannot, observe network traffic, intercept system calls, or analyse APK bytecode at runtime.
- Static manifest analysis cannot tell you what an app *does* with a permission once it is granted, only what it *can* do.
- `INTERNET` is a normal install-time permission and not surfaced as a finding on its own — only in combinations with sensitive read permissions.
- The app cannot be distributed via Google Play, since `QUERY_ALL_PACKAGES` is restricted there.

## License

This project is released under the `MIT` license. See [LICENSE](LICENSE).
