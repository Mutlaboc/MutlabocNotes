---
name: mutlaboc-motion
description: "Use for Android mobile animations in Mutlaboc Notes: Compose micro-interactions, pixel-art sprite frame sequences (AI-generated art), HomeHeader/HomeYardScene animation, loading/empty/success/error states, ambient loops, asset preparation (GIF/frames -> WebP), and animation QA. Do not use for unrelated backend or data-layer work."
---

# Mutlaboc Motion Skill

You are working in the Mutlaboc Notes Android project (Kotlin + Jetpack Compose, pixel-art style). Your goal is small, precise, production-ready mobile animations. Accuracy, restraint, and production readiness matter more than scale or visual complexity.

## 1. How animation works in this project (facts)

- Decorative/character animation = **raster frame sequences** (sprites), NOT Lottie. There is no Lottie/dotLottie dependency in the project and none should be added: the art is AI-generated raster pixel-art, which does not translate to vector formats.
- Frames live in `app/src/main/res/drawable-nodpi/` as **lossless WebP**, named `name_NN.webp` (e.g. `mascot_walk_01.webp`). `drawable-nodpi` is mandatory: any density-qualified folder makes `BitmapFactory.decodeResource` rescale bitmaps (memory blowup + blurred pixel art).
- Playback: frames are decoded **once** via `rememberPixelBmps(...)`, cycled by a frame clock (`rememberElapsedMillis`), frame index derived with `derivedStateOf`. See `HomeYardScene.kt` — it is the reference implementation.
- Pixel art is always drawn with `FilterQuality.None`.
- Time-driven values are read only inside deferred lambdas (`offset {}` / `graphicsLayer {}`) so the 60fps clock never recomposes the scene.
- Reduced motion: `rememberAnimationsEnabled()` checks `ANIMATOR_DURATION_SCALE`; every scene must render a valid static first frame when animations are off.
- UI-state transitions and micro-interactions = **Compose animation APIs**, not sprites.

## 2. Classify the task

- UI state transition / micro-interaction → Compose animation.
- Decorative illustration, ambient loop, character, loading/empty/success/error art → sprite frame sequence.
- Photorealistic/video-like motion → question the requirement first; animated WebP/video only if truly needed.
- Rive/Lottie → only if the user explicitly provides such an asset and asks for it (requires adding a runtime — flag the cost).

If uncertain, choose the simplest production-safe option.

## 3. Motion brief before code

Define: purpose, owning screen/component, format, duration, loop behavior (no loop / subtle loop / state-triggered replay), animated properties, constraints (theme, performance, accessibility), exit criteria. Do not create animation just because it is possible.

## 4. Asset pipeline (AI art → app)

Source art comes from AI generation (ChatGPT etc.) as a GIF, frame PNGs, or a sprite sheet. See `references/frame-production-checklist.md` for the full checklist.

1. Prefer prompting for a **sprite sheet** (one image, grid of frames) — a single generation keeps the character consistent across frames. Per-frame generations drift.
2. Run `scripts/prepare_frames.py` to convert input into production assets. It extracts frames (GIF / sprite sheet / PNG list), applies a uniform trim, optional resize, checks the loop seam, converts to lossless WebP, names files `name_NN.webp`, and reports file-size and decoded-RAM cost.
3. Budgets (enforce, don't guess):
   - RAM after decode: `width × height × 4 × frames` bytes. One animation ≤ ~15 MB decoded; one scene total ≤ ~40 MB.
   - Frame dimensions: no larger than the biggest on-screen size needs; pixel art upscales cleanly with `FilterQuality.None`, so err small.
   - Frame count: 6–12 per loop is usually enough at 100–700 ms/frame.
4. Verify loop seam (last→first), valid first frame, transparent background.
5. Never commit intermediate GIFs/renders; only final WebP frames.

## 5. Compose integration rules

- Reuse `rememberPixelBmps` / `pixelBmp` / `rememberElapsedMillis` / `rememberAnimationsEnabled` from `HomeYardScene.kt`; extract them to a shared file if a second scene needs them, don't duplicate.
- One scene coordinate space scaled to host size (see the `SCENE_W`/`SCENE_H` pattern).
- Per-frame values read in `offset {}` / `graphicsLayer {}` lambdas only.
- Frame cadence via `derivedStateOf { (elapsed / FRAME_MS) % frames.size }` — recomposition only on integer frame change.
- For UI-state motion prefer `AnimatedVisibility`, `AnimatedContent`, `animate*AsState`, `updateTransition`, `Animatable`, `animateContentSize`.
- No unbounded infinite animations inside lazy list items.
- Keep animation state in Compose state or ViewModel depending on ownership; UI components receive state and callbacks.
- Do not create repositories, API clients, session stores, or storage inside Compose UI. Do not move navigation into child components. User-facing strings go through resources.
- Do not add runtime dependencies for animation without explicit justification.

## 6. Motion quality rules

- One clear focal point; animate the fewest properties needed.
- Easing: ease-out entering, ease-in leaving, ease-in-out continuous, spring only for tactile feedback.
- Micro-interactions short; decorative loops slow and subtle.
- No jitter, abrupt stops, loop seams, or layout shifts.
- Calm over novelty; animation supports reading and task completion, never competes with it.

## 7. HomeHeader / HomeYardScene

- Inspect `HomeHeader.kt` and `HomeYardScene.kt` first.
- Preserve the total-coins overlay and the header's decorative role.
- Respect the static-scene start delay (`ANIMATION_START_DELAY_MS`) and `animationRestartKey` behavior.
- Keep resource names stable when replacing assets, or update references carefully.
- Avoid camera movement, parallax, particles, flicker, layout height changes.

## 8. Accessibility

- Decorative animation: no noisy content descriptions.
- Meaningful animation: appropriate semantics or equivalent static text.
- No rapid flashing or high-frequency motion.
- Static fallback when `rememberAnimationsEnabled()` is false.
- Success/error feedback must not depend on motion alone.

## 9. Validation checklist

- Build compiles; existing tests pass or failures explained.
- Assets are lossless WebP in `drawable-nodpi`, lowercase snake_case names, within RAM/size budget.
- First frame correct; loop seam checked; dark/light theme checked.
- No interactive controls hidden; no layout shifts; replay behavior intentional.
- No new runtime dependency unless explicitly justified.

Minimum command:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
```

## 10. Final response format

Report: what changed, why this approach, files changed, how to preview, validation commands run, remaining manual checks.
