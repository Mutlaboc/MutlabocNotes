---
name: mutlaboc-motion-lottie
description: Use for Android mobile animations in Mutlaboc Notes: Compose micro-interactions, Lottie/dotLottie generation, HomeHeader animation, loading/empty/success/error states, ambient loops, and animation QA. Do not use for unrelated backend or data-layer work.
---

# Mutlaboc Motion + Lottie Skill

You are working in the Mutlaboc Notes Android project. Your goal is to create small, precise, high-quality mobile animations. Accuracy, restraint, and production readiness matter more than scale or visual complexity.

This skill combines:

- motion-design review and direction;
- Lottie/dotLottie production workflow;
- Android/Compose integration rules;
- animation QA.

Use this skill every time the task involves animation, motion, Lottie, dotLottie, micro-interactions, animated headers, loading states, empty states, success/error feedback, animated icons, or visual polish.

## 1. Classify the animation task

First classify the requested animation:

- UI state transition: prefer Jetpack Compose animation.
- Micro-interaction: prefer Jetpack Compose animation.
- Decorative illustration: prefer Lottie/dotLottie.
- Ambient header animation: prefer dotLottie/Lottie, integrated through `HomeHeader`.
- Loading, empty, success, error animation: prefer Lottie/dotLottie unless the motion is very small.
- Interactive character/state machine: use Rive only if a `.riv` file is provided or explicitly requested.
- Photorealistic/raster background: do not force Lottie; recommend video/WebP only if appropriate.

If uncertain, choose the simplest production-safe option.

## 2. Define the motion brief before editing code

Before writing code or generating files, define:

- Purpose: what user understanding or feedback does the animation improve?
- Surface: which screen/component owns it?
- Format: Compose, Lottie JSON, `.lottie`, Rive, or other.
- Duration: one-shot duration or loop duration.
- Loop behavior: no loop, subtle loop, or state-triggered replay.
- Properties: opacity, scale, translation, rotation, path, trim path, color, etc.
- Constraints: screen size, dark/light theme, performance, accessibility.
- Exit criteria: what makes the animation acceptable?

Do not create animation just because it is possible.

## 3. Motion quality rules

Apply these rules:

- Prefer one clear focal point.
- Animate the smallest number of properties needed.
- Avoid moving many UI elements at the same time.
- Use easing intentionally:
  - ease-out for entering/settling;
  - ease-in for leaving;
  - ease-in-out for continuous or reversible motion;
  - spring only for tactile UI feedback.
- Keep micro-interactions short.
- Keep decorative loops slow and subtle.
- Avoid jitter, abrupt stops, accidental jumps, and loop seams.
- Preserve layout stability unless layout motion is the explicit goal.
- Animation must support, not distract from, reading and task completion.
- Prefer visual calm over visual novelty.
- Avoid "demo animation" behavior in production UI.

## 4. Android project rules

Respect the existing project architecture:

- Do not create repositories, API clients, session stores, schedulers, or storage inside Compose UI.
- Keep animation state in Compose state or ViewModel state depending on ownership.
- UI components receive state and callbacks.
- Do not move navigation into child components.
- User-facing strings go through resources.
- Assets for Android playback go into `app/src/main/res/raw`.
- Do not change Gradle dependencies unless necessary.
- The project already has dotLottie Android. Prefer using the existing dependency before adding any new Lottie runtime.
- Do not add Rive unless a `.riv` workflow is explicitly requested.

## 5. Existing HomeHeader integration

When working on the home header:

- Inspect `HomeHeader.kt` first.
- Preserve the existing dotLottie/Lottie integration unless a task explicitly asks for another player.
- Preserve the total coins overlay.
- Preserve the header role as decorative/contextual, not primary content.
- If replacing the asset, keep the resource name stable when possible or update references carefully.
- If changing replay behavior, check where `animationRestartKey` is created and passed.
- Keep the animation visually calm.
- Avoid large camera movement, aggressive parallax, strong flicker, and layout shifts.

## 6. Lottie/dotLottie generation workflow

When generating or modifying a Lottie animation:

1. Ask for or infer the source asset:
   - SVG is preferred.
   - Existing `.lottie` or Lottie JSON is acceptable.
   - Screenshot/reference image can guide motion, but do not redraw the UI unless requested.
2. Preserve original geometry, colors, pixel-art style, transparency, and proportions unless the task explicitly asks to change them.
3. Specify:
   - canvas size;
   - FPS;
   - total duration/frame count;
   - loop or one-shot behavior;
   - transparent background;
   - asset isolation.
4. Prefer simple Lottie-compatible primitives:
   - transforms;
   - opacity;
   - scale;
   - rotation;
   - path motion;
   - trim-path-like reveal;
   - simple masks only when necessary.
5. Avoid:
   - heavy raster sequences;
   - unsupported After Effects effects;
   - blur-heavy effects;
   - excessive particles;
   - text converted unpredictably;
   - noisy infinite loops.
6. Preview/scrub when possible.
7. Check first frame, last frame, and loop seam.
8. Export final Android asset to `app/src/main/res/raw`.

## 7. Compose animation workflow

When implementing Compose motion:

- Prefer `AnimatedVisibility`, `AnimatedContent`, `animate*AsState`, `updateTransition`, `Animatable`, or `animateContentSize`.
- Keep animation declarations close to the UI they animate.
- Do not use arbitrary delays unless choreography requires them.
- Do not block composition or business logic.
- Do not create unbounded infinite animations in list items.
- Add or preserve test tags when tests depend on them.
- Keep previews working.
- Avoid introducing recomposition-heavy animation loops.
- Prefer deterministic state-driven motion.

## 8. Accessibility and semantics

- Decorative animation should not create misleading content descriptions.
- Meaningful animation must have appropriate semantics or equivalent static text.
- Avoid rapid flashing.
- Avoid high-frequency motion.
- Provide a static or reduced-motion-friendly fallback when motion could be distracting.
- Do not make success/error feedback depend only on motion.
- Do not animate text-heavy areas in a way that harms readability.

## 9. Validation checklist

Before finishing:

- Build still compiles.
- Existing tests still pass or relevant failures are explained.
- Animation does not change app architecture.
- Resource names are valid.
- File size is reasonable.
- Dark/light theme readability is checked.
- Loop seam is checked if looping.
- First frame is correct.
- Animation does not hide interactive controls.
- State-triggered replay behaves intentionally.
- No new runtime dependency was added unless explicitly justified.

Minimum command:

```powershell
.\gradlew.bat :app:testDevDebugUnitTest
```

For UI-heavy changes, also run relevant Android/Compose UI tests if available.

## 10. Final response format

When reporting back, include:

- What changed.
- Why this animation approach was chosen.
- Files changed.
- How to preview.
- Validation commands run.
- Any limitations or manual checks still needed.
