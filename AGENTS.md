# AGENTS.md - HomeNotes

## Project context

HomeNotes is an Android client written in Kotlin with Jetpack Compose, Navigation Compose, Material Components, Retrofit/OkHttp, DataStore, Room, and AndroidX Security Crypto. The visual style is pixel art; decorative animations are raster sprite frame sequences played by Compose (no Lottie/Rive runtime).

The Android app is the mobile UI/client. The backend is a separate Ktor service and must not be modified from this repository unless the task explicitly asks for contract changes.

## Architecture rules

- Keep dependency creation outside UI.
- `HomeNotesApplication` owns `AppContainer`.
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
- Animation frames and pixel-art assets must live in `app/src/main/res/drawable-nodpi/` as lossless WebP (`name_NN.webp` for frame sequences). Never place them in density-qualified `drawable*` folders: `BitmapFactory.decodeResource` rescales them per density (memory blowup + blurred pixel art).
- Other drawable assets must live in the appropriate `res/drawable*` folder.
- Do not add large generated assets without explaining why they are necessary.

## Animation rules

Use animation only when it improves clarity, feedback, continuity, or perceived quality.

Prefer this order:

1. Jetpack Compose animation for UI state changes and micro-interactions.
2. Sprite frame sequences (lossless WebP in `drawable-nodpi`, played via the frame-clock pattern in `HomeYardScene.kt`) for decorative, illustrative, character, loading, empty, success, error, and ambient animations.
3. Lottie/Rive only when such an asset is explicitly provided and requested; adding a runtime dependency must be justified.
4. GIF is never a production format (acceptable only as an AI-generation intermediate).

For frame sequences:

- Follow the `mutlaboc-motion` skill (`.agents/skills/mutlaboc-motion/`); prepare assets with its `scripts/prepare_frames.py`.
- Decode frames once (`rememberPixelBmps`), cycle via frame clock + `derivedStateOf`; read time-driven values only in `offset {}` / `graphicsLayer {}` lambdas.
- Draw pixel art with `FilterQuality.None`.
- Budget: decoded RAM = width × height × 4 × frames; keep one animation ≤ ~15 MB, one scene ≤ ~40 MB.
- Respect reduced motion (`rememberAnimationsEnabled()`); the first frame must work as a static fallback.
- Check loop seams and dark/light theme readability.
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
- whether it is Compose animation, a sprite frame sequence, or another format;
- duration/FPS/loop behavior if applicable;
- validation commands run;
- known limitations.

## Imported Claude Cowork project instructions
