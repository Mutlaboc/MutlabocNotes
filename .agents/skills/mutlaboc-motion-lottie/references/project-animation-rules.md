# Project animation rules - Mutlaboc Notes

## Current project facts

- Android client: Kotlin + Jetpack Compose.
- Navigation: Navigation Compose.
- UI layer: Compose screens receive state and callbacks.
- Architecture: dependencies are created in `AppContainer`, not in UI.
- dotLottie Android is already available through the version catalog and app dependencies.
- `HomeHeader` is the preferred place for header animation.
- Minimum Android check: `.\gradlew.bat :app:testDevDebugUnitTest`.

## Preferred animation targets

Good targets:

- Home header ambient animation.
- Loading state.
- Empty state.
- Success/error confirmation.
- Completing a note.
- Adding a note.
- Opening/closing small UI blocks.
- Bottom bar tap feedback.
- Auth loading feedback.

Bad targets:

- Constantly moving note list items.
- Distracting background loops behind text.
- Navigation motion that makes the app feel slower.
- Large animation frameworks added for one small effect.
- Motion that hides or shifts touch targets.
- Infinite decorative animations inside scrolling lists.

## HomeHeader quality bar

The header animation must feel calm, readable, and decorative.

Allowed:

- subtle idle loop;
- small light/glow movement;
- slight environmental motion;
- one-shot replay when content state changes;
- transparent or controlled background;
- stable overlay for total coins.

Avoid:

- strong camera shake;
- large parallax;
- busy particles;
- fast flicker;
- layout height changes;
- animation that competes with the note list.

## Recommended technology choice

Use Compose animation when the animation is tied to UI state.

Use Lottie/dotLottie when the animation is decorative, illustrative, ambient, loading, empty, success, or error.

Use Rive only when the task explicitly needs interactive state machines or a `.riv` asset is already provided.

Use video/WebP only when the animation is raster, photorealistic, ASMR-like, or impossible to express cleanly as vector motion.
