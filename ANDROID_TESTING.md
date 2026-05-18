# Android release testing

Use this checklist before promoting a release candidate. It is intentionally manual: no
runtime code, Gradle wiring, CI jobs, or backend URL switching are part of this release
hardening step.

## Pre-RC local checks

Run from the Android repository root:

```powershell
.\gradlew.bat assembleDevDebug testDevDebugUnitTest lintDevDebug
```

Expected result:

- Debug APK assembles successfully.
- Unit tests pass.
- Dev debug lint completes without errors.
- No new warnings or failures are accepted without a linked blocker.

## Connected smoke suite

Run the critical Compose UI smoke suite before release builds:

```powershell
.\gradlew.bat :app:connectedDevDebugAndroidTest
```

This suite covers the auth, home, settings, and logout path on a connected emulator or
device. Run it at least once on each release target API level:

| Android version | API level | Device or emulator | Build | Tester | Result | Blockers |
| --- | --- | --- | --- | --- | --- | --- |
| Android 12 | 31/32 | TBD | TBD | TBD | TBD | TBD |
| Android 13 | 33 | TBD | TBD | TBD | TBD | TBD |
| Android 14 | 34 | TBD | TBD | TBD | TBD | TBD |

## Manual RC regression matrix

Complete these checks for Android 12, 13, and 14. Record any failure in
`RELEASE_CHECKLIST.md` with the build, device, and backend environment.

| Area | Required checks | Android 12 | Android 13 | Android 14 |
| --- | --- | --- | --- | --- |
| Auth | Register or sign in, invalid credentials, social auth availability, `/auth/me` session restore | TBD | TBD | TBD |
| Notes | Create, edit, delete, list refresh, empty state, validation errors | TBD | TBD | TBD |
| Completion | Toggle completion, verify persistence after refresh and app restart | TBD | TBD | TBD |
| Home cards | Create, edit, delete, list refresh, invalid or missing data handling | TBD | TBD | TBD |
| Settings | Theme toggle, language switch, persisted preferences after restart | TBD | TBD | TBD |
| Logout | Logout returns to auth, protected screens are inaccessible, refresh token is revoked | TBD | TBD | TBD |
| Backend errors | Airplane/offline mode, backend unavailable, 401 after expired session, retry after recovery | TBD | TBD | TBD |

## Staging backend smoke

Run the Android app against the backend environment selected for the RC and verify:

- Public health endpoint succeeds outside the app.
- Login/register succeeds for a staging test account.
- Session restore works after force-closing and reopening the app.
- Notes CRUD and completion updates succeed.
- Home card CRUD succeeds.
- Logout succeeds and the same refresh token cannot restore the session.

`ApiConfig.BASE_URL` comes from the selected Gradle flavor through `BuildConfig`.
For RC testing, prefer `stageDebug` against staging and `prodRelease` for the signed
production artifact. Record the exact backend URL, flavor, version code, version name,
and artifact in `RELEASE_CHECKLIST.md`.
