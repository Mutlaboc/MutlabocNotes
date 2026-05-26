# Changelog

## Android 1.0.0 - Unreleased

Release tag: `android-v{versionName}` after signed artifact approval.

Release policy:

- Increase `versionCode` monotonically for every production APK/AAB.
- Keep `versionName` semantic; the first production release starts at `1.0.0`.
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
