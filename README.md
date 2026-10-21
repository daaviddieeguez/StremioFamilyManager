# Stremio Family Manager

Android app prototype for managing multiple Stremio profiles and, in a future verified integration, synchronizing addons.

## Status

This first published version is a **UI prototype**, not a working remote account synchronizer. It does not sign in to Stremio or modify addons. Do not enter account passwords.

## Build an APK

Open **Actions → Build Android APK → Run workflow**. When the workflow finishes, download the `StremioFamilyManager-debug-apk` artifact from that run. The APK is a debug build and is not signed for Play Store distribution.

## Build locally

Requirements: JDK 17 and Gradle 8.9. Run `gradle assembleDebug` from the project root.

## Roadmap

- [x] Compose UI for local profile list and selection
- [x] GitHub Actions APK build
- [ ] Verified Stremio login flow
- [ ] Read addon lists per account
- [ ] Preview changes before applying them
- [ ] Safe add/update/remove operations and integration tests

Stremio Family Manager is unofficial and not affiliated with Stremio. Never log passwords or session tokens; session handling must be secured before adding authentication.