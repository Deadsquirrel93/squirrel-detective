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
- compare scans: when an app gains new sensitive permissions or findings between scans, it is flagged until you open the changes banner
- see whether an accessibility service, device admin, keyboard or notification listener is merely declared or actually turned on right now
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

The five `BIND_*` capabilities above are detected from the components the app actually declares — a `<service>` (or, for device admin, a `<receiver>`) protected by that permission. Merely listing a `BIND_*` permission in `<uses-permission>` grants nothing to a third-party app and is not flagged.

A declared component does nothing until you turn it on in system settings, so Squirrel Detective also checks what is enabled right now:

| Capability | Declared | Turned on right now |
|---|---|---|
| Accessibility service | `MEDIUM` | separate `HIGH` finding |
| Device admin | `MEDIUM` | separate `HIGH` finding |
| Notification listener | `MEDIUM` | separate `HIGH` finding |
| Keyboard (input method) | `MEDIUM` | separate `HIGH` finding |
| VPN service | `MEDIUM` | not detectable |

The enabled state comes from public Android APIs and needs no extra permission. Android does not tell other apps which app owns the active VPN, so VPN apps are always reported as declared. System apps are capped at `MEDIUM`, and whitelisted apps stay `EXPECTED`.

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

Launchers, app stores, file managers and antivirus apps are recognised and downgraded to `EXPECTED` (green) with an explanation, so the report stays focused on apps that genuinely deserve a closer look.

A known package name alone is not enough — any sideloaded APK can reuse it. The downgrade applies only to system apps and to apps installed from a trusted store (Google Play, Galaxy Store, AppGallery, Xiaomi, Amazon, RuStore, F-Droid, Aurora Store). On Android 11+ the store is taken from the package that actually performed the install, which the installed app cannot spoof.

## Product Highlights

- Offline by design: no backend, no telemetry, no network dependency
- Open source and transparent
- No ads, no analytics, no subscriptions, no in-app monetization
- Official distribution only from this repository and its GitHub Releases
- Bilingual interface: Russian and English, follows the system language by default
- Modern Android stack: Kotlin, Jetpack Compose, Material 3
- Diff between scans, persisted locally with Room
- Built-in plain-language Permission Wiki — no need to google what `BIND_ACCESSIBILITY_SERVICE` means

## Privacy Model

Squirrel Detective is built around a strict offline, observe-only model:

- the app has no `INTERNET` permission and makes no network calls of any kind
- nothing about the scanned apps ever leaves the device
- the only persistent storage is a local Room database that holds the baseline scan snapshot, used to compute diffs
- that database is excluded from Android cloud backup and from device-to-device transfer, so the list of your apps never ends up in a backup
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

Every scan is compared with a baseline snapshot stored in a local Room database. When something changed, a banner shows how many apps changed. Expand it to see, for each changed app, its new findings (for example, an accessibility service that was just turned on) and the permissions it added or removed; tap an app to open its detail screen.

The banner stays until you expand it: if you close the app or Android stops it in the background before you look, the next scan shows the same changes again. Expanding the banner marks the changes as seen and moves the baseline forward. If a change is undone before you look (a service is turned back off), it simply disappears from the banner.

Scans run only when you start them — there is no background re-scan.

### Permission Wiki

A built-in reference for what each Android permission actually grants — grouped into Observation, Powerful service binds, Sensors, Communication, Location, Storage, and System. Every entry covers what the permission allows, why apps legitimately request it, and when its presence is suspicious.

### Localization

Russian and English. By default the app follows the system language; you can pick a language in Settings or, on Android 13+, in the system's per-app language settings. The choice is applied immediately and persists across launches.

## Tech Stack

- Kotlin 2.4 (AGP built-in Kotlin), Gradle 9.8, Android Gradle Plugin 9.4
- Jetpack Compose (BOM 2026.09.00, Material 3), edge-to-edge UI
- Hilt 2.60, KSP 2.3
- Room 2.8
- AndroidX Navigation Compose
- minSdk 26, targetSdk 37, compileSdk 37
- R8 code and resource shrinking for release builds
- JUnit unit tests for the risk scorer, scan diff, diff baseline and scanner helpers

## Project Structure

- `app/src/main/java/com/packagespy/app/PackageSpyApp.kt` — application entry, Hilt wiring
- `app/src/main/java/com/packagespy/app/di` — Hilt modules
- `app/src/main/java/com/packagespy/app/domain` — domain models, repository contracts, risk-scoring use case
- `app/src/main/java/com/packagespy/app/data/scanner` — `PackageScanner` (wrapper around `PackageManager`, with progress callback)
- `app/src/main/java/com/packagespy/app/data/local` — Room database, entity, DAO, mapper
- `app/src/main/java/com/packagespy/app/data/repository` — `AppRiskRepository` implementation
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
- `app/build/outputs/apk/release/app-release.apk` (minified and resource-shrunk with R8, ~2.5 MB)

Official release APKs are published as `squirrel-detective-<version>.apk`.

## APK Signing

Official APKs (1.0.0 and later) are signed with a key that was generated on the maintainer's machine and exists only there. It happens to be an Android-SDK-style debug keystore (certificate `CN=Android Debug, O=Android, C=US`), but every machine generates its own random debug key, so nobody else can produce a build that installs as an update over an official one.

Official builds have always used this key, so every new release installs as an update over the previous one and keeps your data. A brand-new certificate would have no Play Protect reputation either, and switching keys would force every user to uninstall first.

Signing certificate SHA-256:

```
DB:AD:E5:21:40:5C:EC:12:11:D3:B1:42:2D:CC:05:80:36:44:6C:1D:08:D2:02:35:A7:26:82:17:4C:1A:94:CF
```

Verify a downloaded APK with `apksigner` from the Android SDK build-tools:

```bash
apksigner verify --print-certs squirrel-detective-2.0.0.apk
```

The `Signer #1 certificate SHA-256 digest` line must match the value above (lowercase, without colons). If it doesn't, the APK is not an official build.

A build you make yourself is signed with your own machine's debug key. It cannot be installed over an official build without uninstalling it first.

## Official Distribution

The only official source for Squirrel Detective code, APK files, and updates is:

- [https://github.com/Deadsquirrel93/squirrel-detective](https://github.com/Deadsquirrel93/squirrel-detective)

If you install a build from another website, Telegram channel, mirror, or repackaged APK source, it is not an official Squirrel Detective release. Squirrel Detective is **not** distributed through Google Play — `QUERY_ALL_PACKAGES` is restricted on Play, and the app cannot do its job without it.

## Installation

1. Download `squirrel-detective-<version>.apk` from the latest [GitHub Release](https://github.com/Deadsquirrel93/squirrel-detective/releases). Optionally compare its SHA-256 with the one in the release notes and check the signing certificate (see [APK Signing](#apk-signing)).
2. On the device, allow installation from the source you used to download it (browser or file manager) when prompted.
3. Open the APK to install.
4. Launch Squirrel Detective and tap **Start scan**.

Updating from an earlier official release: install the new APK over the old one; your data is kept.

Play Protect may warn about an unknown app on sideload. Tap **More details** → **Install anyway**.

Required permissions on the device side:

- `QUERY_ALL_PACKAGES` — declared automatically; no user action required
- nothing else

## Limitations

- Squirrel Detective sees what is in the manifest of each installed app, plus which accessibility services, device admins, keyboards and notification listeners are turned on right now. It does not, and by design cannot, observe network traffic, intercept system calls, or analyse APK bytecode at runtime.
- The owner of an active VPN is not visible to other apps, so VPN services are always reported as declared.
- Switching between scanning with and without system apps drops unacknowledged changes of system apps from the baseline.
- App icons are cached for the lifetime of the process; an app updated while Squirrel Detective is running may keep its old icon until the app is restarted.
- Static manifest analysis cannot tell you what an app *does* with a permission once it is granted, only what it *can* do.
- `INTERNET` is a normal install-time permission and not surfaced as a finding on its own — only in combinations with sensitive read permissions.
- The app cannot be distributed via Google Play, since `QUERY_ALL_PACKAGES` is restricted there.

## License

This project is released under the `MIT` license. See [LICENSE](LICENSE).
