# Release candidate checklist

Use this document for the Sprint 8 release-candidate pass. Every unchecked item needs an
owner or a linked blocker before go/no-go.

## RC entry criteria

| Gate | Owner | Evidence | Status |
| --- | --- | --- | --- |
| Android branch is rebased or merged onto the intended release branch | TBD | Commit SHA / PR | TBD |
| `.\gradlew.bat assembleDebug testDebugUnitTest` passes | TBD | Local or CI run link | TBD |
| `.\gradlew.bat :app:connectedDebugAndroidTest` passes on at least one target device | TBD | Run notes | TBD |
| Backend staging environment is identified | TBD | URL / deployment reference | TBD |
| Backend migration and rollback runbook is reviewed | TBD | `notes-backend/RELEASE_RUNBOOK.md` revision | TBD |
| Test accounts and social auth configuration are ready | TBD | Account references, no secrets | TBD |

## Android QA matrix

| Android version | API level | Device or emulator | Build | Tester | Result | Blocker links |
| --- | --- | --- | --- | --- | --- | --- |
| Android 12 | 31/32 | TBD | TBD | TBD | TBD | TBD |
| Android 13 | 33 | TBD | TBD | TBD | TBD | TBD |
| Android 14 | 34 | TBD | TBD | TBD | TBD | TBD |

Required scenario coverage for each row:

- Auth: register/login, invalid credentials, social auth availability, `/auth/me` restore.
- Notes: create, edit, delete, list refresh, completion toggle.
- Home cards: create, edit, delete, list refresh.
- Settings: theme, language, persistence after restart.
- Logout: user returns to auth and refresh token cannot restore the session.
- Failure paths: offline mode, backend unavailable, expired or unauthorized session.

## Backend compatibility matrix

Run against staging before production rollout. Record the Android build, backend artifact,
database backup reference, and result for each row.

| Compatibility check | Android build | Backend artifact | Required smoke | Result | Blockers |
| --- | --- | --- | --- | --- | --- |
| Previous Android build + new backend | TBD | TBD | Auth, `/auth/me`, notes, home-cards, logout | TBD | TBD |
| New Android build + current backend | TBD | TBD | Auth, `/auth/me`, notes, home-cards, logout | TBD | TBD |
| New Android build + new backend | TBD | TBD | Full Android RC matrix | TBD | TBD |

Minimum backend endpoint coverage:

- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/me`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET/POST/PUT/DELETE /notes`
- `PATCH /notes/{id}/completion`
- `GET/POST/PUT/DELETE /home-cards`

## Go/no-go gates

Release may proceed only when:

- Android 12, 13, and 14 rows are complete or explicitly waived by the release owner.
- No P0/P1 blockers remain open.
- Backend migration status is verified on staging.
- Rollback owner, artifact reference, and database backup reference are recorded.
- Staging smoke passes for auth, notes, home-cards, and logout.

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
