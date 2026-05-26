# Sprint 10 Android release checklist

Use this document for the first production release candidate. Every open item needs an
owner, evidence, and either a pass result or a linked blocker before go/no-go.

## RC entry criteria

| Gate | Owner | Evidence | Status |
| --- | --- | --- | --- |
| Android branch is rebased or merged onto the intended release branch | TBD | Commit SHA / PR | TBD |
| `.\gradlew.bat :app:assembleDevDebug` passes without OAuth secrets | TBD | Local log or CI run link | TBD |
| `.\gradlew.bat :app:testDevDebugUnitTest` passes without OAuth secrets | TBD | Local log or CI run link | TBD |
| `.\gradlew.bat :app:lintDevDebug` passes without OAuth secrets | TBD | Local log or CI run link | TBD |
| `.\gradlew.bat :app:assembleProdRelease` passes without local signing values | TBD | Local log | TBD |
| Signed `prodRelease` APK/AAB is built with release signing inputs | TBD | GitHub Actions run or local artifact path | TBD |
| `.\gradlew.bat :app:connectedDevDebugAndroidTest` passes on at least one target device | TBD | Run notes | TBD |
| Backend tests pass in `notes-backend` | TBD | `.\gradlew.bat test` log or CI run link | TBD |
| Backend staging deploy runbook is reviewed | TBD | `notes-backend/RELEASE_RUNBOOK.md` revision | TBD |
| Test accounts and Google/Yandex auth configuration are ready for manual social-auth QA | TBD | Account references, no secrets | TBD |

## Android QA matrix

| Android version | API level | Device or emulator | Build | Tester | Result | Blocker links |
| --- | --- | --- | --- | --- | --- | --- |
| Android 12 | 31/32 | TBD | TBD | TBD | TBD | TBD |
| Android 13 | 33 | TBD | TBD | TBD | TBD | TBD |
| Android 14 | 34 | TBD | TBD | TBD | TBD | TBD |

Required scenario coverage for each row:

- Auth: register/login, invalid credentials, Google/Yandex auth with real client IDs, `/auth/me` restore.
- Notes: create, edit, delete, list refresh, empty state, validation errors, completion toggle.
- Home cards: create, edit, delete, list refresh, invalid or missing data handling.
- Notifications: one-time deadline notification, repeating deadline notification, tap opens the note, exact-alarm granted/denied fallback, re-entry from Settings.
- Settings: dark theme, light theme, language switch, persistence after app restart.
- Logout: user returns to auth, protected screens are inaccessible, and refresh token cannot restore the session.
- Failure paths: offline mode, backend unavailable, expired or unauthorized session, retry after recovery.

## Backend and release compatibility

Run against staging before production rollout. Record Android build, backend artifact,
database backup reference, rollback owner, and result for each row.

| Compatibility check | Android build | Backend artifact | Required smoke | Result | Blockers |
| --- | --- | --- | --- | --- | --- |
| Previous Android build + new backend | TBD | TBD | Auth, `/auth/me`, notes, completion, home-cards, logout | TBD | TBD |
| New Android build + current backend | TBD | TBD | Auth, `/auth/me`, notes, completion, home-cards, logout | TBD | TBD |
| New Android build + new backend | TBD | TBD | Full Android RC matrix | TBD | TBD |

Minimum backend endpoint coverage:

- `GET /health`
- `GET /health/db`
- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET/POST/PUT/DELETE /notes`
- `PATCH /notes/{id}/completion`
- `GET/POST/PUT/DELETE /home-cards`

## Release policy

- `versionCode` increases monotonically for every production artifact.
- `versionName` follows semantic versioning and starts at `1.0.0`.
- Update `CHANGELOG.md` before creating a signed artifact.
- Tag accepted production artifacts as `android-v{versionName}` after go/no-go approval.
- R8/minify remains disabled for this release; treat enabling it as a separate release-hardening task.
- Release signing uses external inputs only: `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. GitHub Actions decodes `ANDROID_KEYSTORE_BASE64` into the keystore file.
- The `release` build type must not fall back to debug signing. If release signing inputs are missing, local release artifacts remain unsigned.
- `dev` flavor compile smoke may use placeholder OAuth client IDs; real Google/Yandex sign-in must be verified manually with environment-specific values.

## Crash and logging strategy

No crash telemetry SDK is included in this release. For production crash intake, record:

- App version name, version code, Git SHA, build artifact, device model, Android API, language, and theme.
- Reproduction steps, screenshots or screen recording, expected result, and actual result.
- `adb logcat` excerpt around the failure with tokens, passwords, emails, and other PII removed.
- Backend timestamp, environment, and request context when the issue involves network behavior.

## Go/no-go gates

Release may proceed only when:

- Android 12, 13, and 14 rows are complete or explicitly waived by the release owner.
- Clean build, unit tests, lint, connected smoke, backend tests, and manual regression evidence are recorded.
- No P0/P1 blockers remain open.
- Backend migration status is verified on staging.
- Rollback owner, artifact reference, database backup reference, and previous known-good backend artifact are recorded.
- Signed prod APK/AAB is archived and traceable to commit SHA, version code, and version name.

## Rollout record

| Field | Value |
| --- | --- |
| Release owner | TBD |
| Android commit SHA | TBD |
| Android versionCode / versionName | TBD |
| Android signed APK/AAB artifact | TBD |
| Backend artifact/container | TBD |
| Backend environment URL | TBD |
| Database backup reference | TBD |
| Previous known-good backend artifact | TBD |
| Rollback owner | TBD |
| Go/no-go decision | TBD |
| Git tag | `android-vTBD` |
| Production rollout time | TBD |
| Post-rollout validation result | TBD |
