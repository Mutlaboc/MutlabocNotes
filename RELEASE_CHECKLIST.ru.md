# Чеклист release candidate

Используйте этот документ для Sprint 8 release-candidate pass. У каждого незакрытого
пункта должен быть owner или связанный blocker до go/no-go.

## RC entry criteria

| Gate | Owner | Evidence | Status |
| --- | --- | --- | --- |
| Android branch is rebased or merged onto the intended release branch | TBD | Commit SHA / PR | TBD |
| `.\gradlew.bat assembleDevDebug testDevDebugUnitTest lintDevDebug` passes | TBD | Local or CI run link | TBD |
| `.\gradlew.bat :app:connectedDevDebugAndroidTest` passes on at least one target device | TBD | Run notes | TBD |
| Backend staging environment is identified | TBD | URL / deployment reference | TBD |
| Backend migration and rollback runbook is reviewed | TBD | `notes-backend/RELEASE_RUNBOOK.md` revision | TBD |
| Test accounts and social auth configuration are ready | TBD | Account references, no secrets | TBD |

## Android QA matrix

| Android version | API level | Device or emulator | Build | Tester | Result | Blocker links |
| --- | --- | --- | --- | --- | --- | --- |
| Android 12 | 31/32 | TBD | TBD | TBD | TBD | TBD |
| Android 13 | 33 | TBD | TBD | TBD | TBD | TBD |
| Android 14 | 34 | TBD | TBD | TBD | TBD | TBD |

Обязательное покрытие сценариев для каждой строки:

- Auth: register/login, invalid credentials, social auth availability, `/auth/me` restore.
- Notes: create, edit, delete, list refresh, completion toggle.
- Home cards: create, edit, delete, list refresh.
- Settings: theme, language, persistence after restart.
- Logout: пользователь возвращается на auth, и refresh token не может восстановить session.
- Failure paths: offline mode, backend unavailable, expired or unauthorized session.

## Backend compatibility matrix

Запустите против staging перед production rollout. Для каждой строки запишите Android
build, backend artifact, database backup reference и result.

| Compatibility check | Android build | Backend artifact | Required smoke | Result | Blockers |
| --- | --- | --- | --- | --- | --- |
| Previous Android build + new backend | TBD | TBD | Auth, `/auth/me`, notes, home-cards, logout | TBD | TBD |
| New Android build + current backend | TBD | TBD | Auth, `/auth/me`, notes, home-cards, logout | TBD | TBD |
| New Android build + new backend | TBD | TBD | Full Android RC matrix | TBD | TBD |

Минимальное покрытие backend endpoints:

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET/POST/PUT/DELETE /notes`
- `PATCH /notes/{id}/completion`
- `GET/POST/PUT/DELETE /home-cards`

## Go/no-go gates

Release может идти дальше только когда:

- Строки Android 12, 13 и 14 заполнены или явно waived release owner.
- Нет открытых P0/P1 blockers.
- Backend migration status проверен на staging.
- Rollback owner, artifact reference и database backup reference записаны.
- Staging smoke проходит для auth, notes, home-cards и logout.

## Rollout record

| Field | Value |
| --- | --- |
| Release owner | TBD |
| Android commit SHA | TBD |
| Android build artifact | TBD |
| Backend artifact/container | TBD |
| Backend environment URL | TBD |
| Database backup reference | TBD |
| Rollback owner | TBD |
| Go/no-go decision | TBD |
| Production rollout time | TBD |
| Post-rollout validation result | TBD |
