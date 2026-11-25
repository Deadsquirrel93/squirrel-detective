# Changelog

All notable changes to Squirrel Detective are documented here.

## 2.1.0 — 2026-11-25

### Detection & combos

- Runtime permissions (`CAMERA`, `RECORD_AUDIO`, SMS, contacts, call log, phone state, location, accounts) now check whether the permission is actually **granted** (`REQUESTED_PERMISSION_GRANTED`). Revoked runtime permissions no longer trigger false alarms in the risk score and are clearly marked as not granted on the detail screen.
- High-level **threat combination summaries** ("What these allow together") highlight multi-signal attack profiles on the detail screen: 2FA / overlay interception, silent APK dropper, covert surveillance (stalkerware), device uninstall lock, and deep device profiling.
- **Inspect an APK file** before installing it via the Android document picker — no storage permissions required.

### Personal whitelist, search & filtering

- **Personal trusted list**: mark apps you trust on the detail screen to downgrade their current findings to `EXPECTED`. Stored locally in Room (database migration 1 → 2); genuinely new permissions or capabilities on trusted apps still appear in the changes banner.
- **Search and reverse capability filter** on the scan results screen: search by app name, package name, or permission, or filter in one tap by capability (Camera, Microphone, Location, Background location, SMS, Contacts, Accessibility, Notifications, Overlay, Install APK, All apps list).
- **Sort results** by risk level or by most recently updated.

### Permission reference & detail screen

- Every permission on the detail screen is now **tappable**, opening a sheet with its grant status, protection level, and human-friendly Wiki explanation — falling back to Android's system permission description for all other permissions.
- **Permission Wiki** now has search, a "Dangerous permission combinations" cheat sheet, and coverage for modern Android 13–15 permissions (scoped media, Bluetooth/nearby Wi-Fi, body sensors, calendar, exact alarms, battery optimization, full-screen intents).
- **Direct system settings links** on findings cards (Accessibility, Notification access, Device admin, Keyboards, Overlay, Usage access, Install unknown apps, All files access, Modify system settings).
- Detail header now displays `targetSdk`, install/update dates, and cleartext HTTP traffic flag, and auto-refreshes when returning from system settings.
- **Share report** button exports a plain-text audit summary for any app.
- **Updates** card in Settings shows the installed version and links to GitHub Releases and Obtainium without adding any network permission.

### Reliability

- Fixed a potential crash on Android 8–10 when deleting more than 999 missing snapshot rows in SQLite (`deleteByPackages` now runs in chunks of 500).

## 2.0.0 — 2026-09-27

### Detection

- Accessibility services, device admins, notification listeners and keyboards are now checked for whether they are **turned on right now**, not just declared. Each enabled one gets its own `HIGH` finding. A declared accessibility service or device admin alone is now `MEDIUM` (it was `HIGH`), because it can do nothing until you turn it on.
- `BIND_*` capabilities are detected from the components an app actually declares (`<service>` / `<receiver>` protected by the permission). A bare `<uses-permission>` entry is no longer flagged.
- The whitelist downgrade (launchers, stores, file managers, antivirus) now requires a system app or an install from a trusted store. A known package name alone is not enough.
- Receiver actions are resolved with one query per action, which makes full scans faster.

### Diff between scans

- The changes banner can be expanded. For each changed app it shows the new findings and the added or removed permissions, sorted, with readable permission names.
- The banner now stays until you expand it, even if the app is closed or stopped in the background. Changes that are undone before you look disappear on their own.
- The diff compares the findings stored at scan time instead of re-scoring old snapshots.

### Interface and localization

- The app follows the system language by default and supports Android 13+ per-app language settings. Russian and English are available.
- Counters use proper plural forms.
- App icons load asynchronously with an in-memory cache, so scrolling is smoother.
- The detail screen always shows a fresh single-app scan.
- The Permission Wiki has a new entry: "Declared vs. turned on".
- Adaptive launcher icon with a monochrome layer for themed icons.

### Platform and build

- Targets Android 17 (API 37), edge-to-edge UI. Minimum is still Android 8.0 (API 26).
- Gradle 9.8, AGP 9.4 with built-in Kotlin 2.4, updated AndroidX, Compose BOM and Room. The Room schema is exported.
- The release build is minified and resource-shrunk with R8. The APK went from about 14 MB to about 2.5 MB.
- Signed with the same key as 1.0.0, so it installs as an update over 1.0.0 and keeps your data.

### Upgrade notes

- On the first scan after upgrading, every accessibility service, device admin, keyboard and notification listener that is already turned on is listed as a new finding in the changes banner. This happens once.

### Known limitations

- On Android 13+, a language chosen in 1.0.0 is reset to the system language once, on the first launch after upgrading. Choose it again in Settings.
- Android does not tell other apps who owns the active VPN, so VPN services are always reported as declared.
- Switching between scanning with and without system apps drops unacknowledged changes of system apps.
- App icons are cached for the lifetime of the process. An app updated while Squirrel Detective is running may keep its old icon until restart.

## 1.0.0 — 2026-04-29

First public release.
