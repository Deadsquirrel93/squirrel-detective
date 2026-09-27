# Changelog

All notable changes to Squirrel Detective are documented here.

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
