# Чеклист Android-релиза Sprint 10

Используйте этот документ для первого production release candidate. У каждого
незакрытого пункта должен быть owner, evidence и результат pass или связанный blocker
до go/no-go.

## Критерии входа в RC

| Gate | Owner | Evidence | Status |
| --- | --- | --- | --- |
| Android branch is rebased or merged onto the intended release branch | TBD | Commit SHA / PR | TBD |
| `.\gradlew.bat :app:testDevDebugUnitTest` проходит | TBD | Local log или CI run link | TBD |
| `.\gradlew.bat :app:lintDevDebug` проходит | TBD | Local log или CI run link | TBD |
| `.\gradlew.bat :app:assembleProdRelease` проходит без local signing values | TBD | Local log | TBD |
| Signed `prodRelease` APK/AAB собран с release signing inputs | TBD | GitHub Actions run или local artifact path | TBD |
| `.\gradlew.bat :app:connectedDevDebugAndroidTest` проходит хотя бы на одном target device | TBD | Run notes | TBD |
| Backend tests проходят в `notes-backend` | TBD | `.\gradlew.bat test` log или CI run link | TBD |
| Backend staging deploy runbook проверен | TBD | `notes-backend/RELEASE_RUNBOOK.md` revision | TBD |
| Test accounts и Google/Yandex auth configuration готовы | TBD | Account references, no secrets | TBD |

## Android QA matrix

| Android version | API level | Device or emulator | Build | Tester | Result | Blocker links |
| --- | --- | --- | --- | --- | --- | --- |
| Android 12 | 31/32 | TBD | TBD | TBD | TBD | TBD |
| Android 13 | 33 | TBD | TBD | TBD | TBD | TBD |
| Android 14 | 34 | TBD | TBD | TBD | TBD | TBD |

Обязательное покрытие сценариев для каждой строки:

- Auth: register/login, invalid credentials, Google/Yandex auth availability, `/auth/me` restore.
- Notes: create, edit, delete, list refresh, empty state, validation errors, completion toggle.
- Home cards: create, edit, delete, list refresh, invalid or missing data handling.
- Notifications: one-time deadline notification, repeating deadline notification, tap opens the note, exact-alarm permission fallback.
- Settings: dark theme, light theme, language switch, persistence after app restart.
- Logout: пользователь возвращается на auth, protected screens недоступны, refresh token не восстанавливает session.
- Failure paths: offline mode, backend unavailable, expired or unauthorized session, retry after recovery.

## Backend и release compatibility

Запустите против staging перед production rollout. Для каждой строки запишите Android
build, backend artifact, database backup reference, rollback owner и result.

| Compatibility check | Android build | Backend artifact | Required smoke | Result | Blockers |
| --- | --- | --- | --- | --- | --- |
| Previous Android build + new backend | TBD | TBD | Auth, `/auth/me`, notes, completion, home-cards, logout | TBD | TBD |
| New Android build + current backend | TBD | TBD | Auth, `/auth/me`, notes, completion, home-cards, logout | TBD | TBD |
| New Android build + new backend | TBD | TBD | Full Android RC matrix | TBD | TBD |

Минимальное покрытие backend endpoints:

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

- `versionCode` монотонно увеличивается для каждого production artifact.
- `versionName` использует semantic versioning и начинается с `1.0.0`.
- Перед сборкой signed artifact обновите `CHANGELOG.md`.
- Принятые production artifacts помечаются tag `android-v{versionName}` после go/no-go approval.
- R8/minify остаётся выключенным для этого релиза; включение R8 считается отдельной release-hardening задачей.
- Release signing использует только внешние inputs: `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. GitHub Actions декодирует `ANDROID_KEYSTORE_BASE64` в keystore file.

## Crash и logging strategy

Crash telemetry SDK в этот релиз не добавляется. Для production crash intake фиксируйте:

- App version name, version code, Git SHA, build artifact, device model, Android API, language и theme.
- Reproduction steps, screenshots или screen recording, expected result и actual result.
- `adb logcat` excerpt вокруг сбоя с удалёнными tokens, passwords, emails и другой PII.
- Backend timestamp, environment и request context, если проблема связана с network behavior.

## Go/no-go gates

Release может идти дальше только когда:

- Строки Android 12, 13 и 14 заполнены или явно waived release owner.
- Clean build, unit tests, lint, connected smoke, backend tests и manual regression evidence записаны.
- Нет открытых P0/P1 blockers.
- Backend migration status проверен на staging.
- Rollback owner, artifact reference, database backup reference и previous known-good backend artifact записаны.
- Signed prod APK/AAB сохранён и связан с commit SHA, version code и version name.

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
