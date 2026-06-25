# Lottie / dotLottie production checklist

## Input quality

- Prefer SVG, existing Lottie JSON, existing `.lottie`, or layered vector assets.
- Use screenshots only as visual references.
- Do not restyle source assets unless explicitly requested.
- Preserve geometry, proportions, colors, transparency, and style unless asked otherwise.

## Export requirements

- Transparent background unless requested otherwise.
- Android resource filename must be lowercase snake_case.
- Place final `.json` or `.lottie` in `app/src/main/res/raw`.
- Keep a clear source filename if source JSON is also committed.
- Do not commit temporary preview files unless they are explicitly useful.

## Motion requirements

- First frame must be valid and not blank unless the task explicitly asks for reveal/build animation.
- Last frame must connect cleanly to first frame for loops.
- No abrupt scale/position jumps.
- No accidental opacity flicker.
- Keep duration appropriate:
  - micro feedback: short;
  - decorative loop: slow/subtle;
  - loading: loopable and non-annoying;
  - success/error: one-shot.
- Avoid over-animation.
- Prefer clean timing over complex visual detail.

## Android integration requirements

- Use existing dotLottie Android dependency.
- Keep animation playback lifecycle simple.
- Avoid playing many heavy animations in lazy lists.
- Decorative animations should not create noisy accessibility output.
- Test in dark and light themes if visible in both.
- Do not add a second animation runtime without a clear reason.

## Final review questions

- Does the animation explain, confirm, or gently enrich something?
- Can the user ignore it without losing task flow?
- Is the motion precise?
- Is it too busy?
- Is it production-safe?
- Does it preserve app architecture?
- Does it compile and pass the minimum test command?
