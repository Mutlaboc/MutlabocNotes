# Frame-sequence production checklist — Mutlaboc Notes

## Prompting AI art for animation

- Ask for a **sprite sheet**: one image containing all frames in a grid (e.g. "4x2 grid, 8 walk-cycle frames, side view, transparent background, consistent character"). One generation = consistent style across frames.
- Specify: pixel-art style, transparent background, frame count, what moves between frames (and what must stay pixel-identical — background, outline, palette).
- For subtle idle loops (lantern flicker, smoke), ask for the static scene first, then variations that change ONLY the animated region. Fewer moving pixels = smaller WebP + calmer motion.
- GIF output from AI is acceptable as an intermediate; it is never the production format (256-color palette, no partial alpha).

## Producing production assets

- Run `scripts/prepare_frames.py` — it handles extraction, uniform trim, resize, lossless WebP, naming, seam check, and budget report.
- Production format: **lossless WebP** (`name_NN.webp`, lowercase snake_case, 1-based, zero-padded).
- Location: `app/src/main/res/drawable-nodpi/` only. Never `drawable/` (mdpi baseline → density rescaling at decode → memory blowup + blur).
- Trim must be uniform across all frames of one sequence, otherwise frames misalign during playback.
- Note: libwebp zeroes RGB under alpha=0 pixels (invisible; harmless with `FilterQuality.None`).

## Budgets

- Decoded RAM = `width × height × 4 × frames`. One animation ≤ ~15 MB, one scene total ≤ ~40 MB.
- Keep source frames at the smallest size that looks right at max on-screen dp; pixel art scales up cleanly with `FilterQuality.None`.
- 6–12 frames per loop at 100–700 ms/frame covers most needs (mascot walk: 8 × 110 ms; house idle: 12 × 700 ms).

## Motion requirements

- First frame valid and presentable as the static/reduced-motion fallback.
- Loop seam clean: last frame connects to first without a pop.
- No abrupt jumps, no accidental opacity flicker, no layout shifts.
- Micro feedback: short and one-shot. Decorative loop: slow and subtle. Loading: loopable, non-annoying.

## Integration requirements

- Decode once (`rememberPixelBmps`), cycle via frame clock + `derivedStateOf`; never decode per frame.
- `FilterQuality.None` for all pixel art.
- Respect `rememberAnimationsEnabled()` (reduced motion) and any scene start delay.
- No heavy animations inside lazy list items.
- Test dark and light themes if the asset is visible in both.

## Final review questions

- Does the animation explain, confirm, or gently enrich something?
- Can the user ignore it without losing task flow?
- Is it within RAM/size budget?
- Does it compile and pass `.\gradlew.bat :app:testDevDebugUnitTest`?
