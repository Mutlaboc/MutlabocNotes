# AGENTS.md - Mutlaboc Notes

## Project context

Mutlaboc Notes is an Android client written in Kotlin with Jetpack Compose, Navigation Compose, Material Components, Retrofit/OkHttp, DataStore, Room, AndroidX Security Crypto, and dotLottie Android.

The Android app is the mobile UI/client. The backend is a separate Ktor service and must not be modified from this repository unless the task explicitly asks for contract changes.

## Architecture rules

- Keep dependency creation outside UI.
- `MutlabocNotesApplication` owns `AppContainer`.
- `MainActivity` receives `ViewModelProvider.Factory` from `AppContainer`.
- Compose screens must receive state and callbacks; they must not create repositories, API clients, schedulers, session managers, or storage directly.
- Repositories remain thin mapping layers between ViewModel and API/domain models.
- Do not put navigation logic into repositories or low-level UI components.
- Keep root navigation routes stable unless the task explicitly asks to refactor navigation.
- Do not change backend contracts unless the task explicitly asks for it.

## UI entry points

Treat these Compose entry points as stable unless the task explicitly asks to refactor them:

- `HomeScreen`
- `CompletedNotesScreen`
- `EditNoteScreen`
- `HomeInfoScreen`
- `EditHomeInfoCardScreen`
- `NoteItem`
- `BottomBar`
- `HomeHeader`
- `AuthScreen`
- `SettingsScreen`

## Text and resources

- User-facing strings must go through `strings.xml` and `values-en/strings.xml`.
- Inline text is allowed only for preview sample data, test tags, route names, exception messages, and dynamic user content.
- Lottie and dotLottie assets used by Android must live in `app/src/main/res/raw`.
- Drawable assets must live in the appropriate `res/drawable*` folder.
- Do not add large generated assets without explaining why they are necessary.

## Animation rules

Use animation only when it improves clarity, feedback, continuity, or perceived quality.

Prefer this order:

1. Jetpack Compose animation for UI state changes and micro-interactions.
2. dotLottie/Lottie for decorative, illustrative, loading, empty, success, error, and ambient animations.
3. Rive only when an existing `.riv` file with state machines is provided or explicitly requested.
4. Avoid video/GIF unless the task requires raster/photorealistic motion.

For Lottie/dotLottie:

- Keep animations small, precise, and readable.
- Prefer simple transforms, opacity, scale, rotation, path, and trim-path style motion.
- Avoid unsupported or fragile After Effects features.
- Use transparent background unless the task explicitly needs a background.
- Check loop seams.
- Check dark/light theme readability.
- Check accessibility: decorative animations should not add noisy semantics.
- Avoid uncontrolled infinite motion in content-heavy screens.

## Existing HomeHeader animation

`HomeHeader` is the preferred integration point for header animation.

When modifying the header animation:

- Keep the header height stable unless requested.
- Do not break the total coins overlay.
- Keep the animation decorative.
- Do not make the animation compete with note list content.
- Prefer subtle ambient or state-based motion over busy motion.
- If replacing an asset, prefer preserving the existing resource name when safe.
- If changing resource names, update all references carefully.

## Testing and validation

Minimum Android check:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
```

When UI behavior changes, also run relevant Compose UI tests if available.

Before finishing any animation task, report:

- files changed;
- animation purpose;
- whether it is Compose, Lottie/dotLottie, Rive, or another format;
- duration/FPS/loop behavior if applicable;
- validation commands run;
- known limitations.
