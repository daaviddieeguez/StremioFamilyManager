# Stremio Family Manager

Android app for connecting multiple Stremio accounts, reading their addon lists, and explicitly synchronizing one account's complete addon list to selected accounts.

## Status and account security

The app signs in directly to `https://api.strem.io` using the API contract implemented by Stremio's [stremio-api-client](https://github.com/Stremio/stremio-api-client). The password is used only for the login request and is not stored. Session tokens are encrypted with Android Keystore and saved on this device. The integration has automated mock-server tests, but it still needs verification with real Stremio accounts and Android devices; the API is not documented as a stable public SDK.

Sync always starts with a read-only preview. Applying the preview replaces the entire addon collection on each selected destination with the source account's collection, so addons that exist only on a destination will be removed. The app re-reads collections before writing and cancels if they changed since preview. Accounts are updated one at a time; if a later write fails, earlier successful updates remain applied and are reported.

## Build an APK

Open **Actions → Build Android APK → Run workflow**. The workflow runs unit and API contract tests before building. When it finishes, download the `StremioFamilyManager-debug-apk` artifact from that run. The APK is a debug build and is not signed for Play Store distribution.

## Build locally

Requirements: JDK 17 and Gradle 8.9. Run `gradle testDebugUnitTest assembleDebug` from the project root.

## Roadmap

- [x] Compose UI for connected profiles
- [x] GitHub Actions APK build and API contract tests
- [x] Stremio API login and encrypted session storage
- [x] Read addon lists per account
- [x] Preview and confirm full-list synchronization
- [ ] Verify login and synchronization with real Stremio accounts and Android devices

Stremio Family Manager is unofficial and not affiliated with Stremio. Never share your password or session token. Because Stremio does not publish this API as a stable public SDK, upstream changes may affect login or addon operations.
