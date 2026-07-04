# Changelog

## Android 1.0.1 - Pending RuStore submission

Build: `versionName 1.0.1`, `versionCode 2` (see `app/build.gradle.kts`).
Release tag: `android-v1.0.1` after signed artifact approval.
Tag policy: use `android-v{versionName}` for signed Android releases.

Release policy:

- Increase `versionCode` monotonically for every production APK/AAB.
- Keep `versionName` semantic.
- Record the Git SHA, signed artifact, backend artifact, and go/no-go decision in `RELEASE_CHECKLIST.md`.

Highlights:

- Auth with local login/register and social auth availability.
- Notes CRUD, completion toggle, deadlines, and notification navigation.
- Home card CRUD.
- Settings for dark theme and language.
- Backend compatibility with auth/session, notes, completion, and home-card endpoints.

Operational notes:

- Release signing is external-only through local properties, Gradle properties, environment variables, or GitHub Actions secrets.
- No crash telemetry SDK is included in this release; use the manual crash intake process in `RELEASE_CHECKLIST.md`.
- R8/minify remains disabled for this release and should be revisited in a separate hardening task.

## Android 1.0.0 - First Android release

Build: `versionName 1.0.0`, `versionCode 1`.
Release tag: `android-v{versionName}`.

Release policy:

- Increase `versionCode` monotonically for every production APK/AAB.
- Keep `versionName` semantic.
- Record the Git SHA, signed artifact, backend artifact, and go/no-go decision in `RELEASE_CHECKLIST.md`.

Operational notes:

- No crash telemetry SDK was included in the first Android release.
