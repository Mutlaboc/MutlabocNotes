#!/usr/bin/env python3
"""Convert AI-generated animation source (GIF / sprite sheet / PNG frames)
into production sprite frames for Mutlaboc Notes.

Output: lossless WebP frames named <name>_NN.webp, uniformly trimmed,
optionally resized, with a loop-seam check and RAM/size report.

Examples:
  # GIF from ChatGPT -> frames
  python prepare_frames.py mascot.gif --name mascot_walk --out app/src/main/res/drawable-nodpi

  # Sprite sheet 4x2 (grid of 8 frames in one image)
  python prepare_frames.py sheet.png --grid 4x2 --name house_anim --out app/src/main/res/drawable-nodpi

  # Loose PNG frames, resize to height 480
  python prepare_frames.py f1.png f2.png f3.png --name man_think --height 480 --out app/src/main/res/drawable-nodpi

Requires: Pillow, numpy.
"""
import argparse
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageSequence

RAM_BUDGET_MB = 15  # decoded budget per animation (w*h*4*frames)


def load_frames(inputs, grid):
    paths = [Path(p) for p in inputs]
    if len(paths) == 1 and paths[0].suffix.lower() == ".gif":
        im = Image.open(paths[0])
        return [f.convert("RGBA") for f in ImageSequence.Iterator(im)]
    if len(paths) == 1 and grid:
        cols, rows = (int(v) for v in grid.lower().split("x"))
        sheet = Image.open(paths[0]).convert("RGBA")
        fw, fh = sheet.width // cols, sheet.height // rows
        return [
            sheet.crop((c * fw, r * fh, (c + 1) * fw, (r + 1) * fh))
            for r in range(rows) for c in range(cols)
        ]
    return [Image.open(p).convert("RGBA") for p in paths]


def uniform_trim(frames):
    """Trim transparent borders with ONE bbox shared by all frames (keeps alignment)."""
    boxes = [f.getbbox() for f in frames]
    if any(b is None for b in boxes):
        sys.exit("error: fully transparent frame in input")
    l = min(b[0] for b in boxes); t = min(b[1] for b in boxes)
    r = max(b[2] for b in boxes); btm = max(b[3] for b in boxes)
    return [f.crop((l, t, r, btm)) for f in frames]


def seam_report(frames):
    a = np.array(frames[-1], dtype=np.int16)
    b = np.array(frames[0], dtype=np.int16)
    diff = np.abs(a - b).mean()
    return f"loop seam (last vs first, mean abs diff): {diff:.2f} " + (
        "(smooth)" if diff < 8 else "(CHECK VISUALLY — may pop)")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("inputs", nargs="+", help="GIF, sprite sheet, or PNG frames in order")
    ap.add_argument("--name", required=True, help="output base name, e.g. mascot_walk")
    ap.add_argument("--out", required=True, help="output dir (app/src/main/res/drawable-nodpi)")
    ap.add_argument("--grid", help="sprite sheet grid COLSxROWS, e.g. 4x2")
    ap.add_argument("--height", type=int, help="resize frames to this height (nearest-neighbor)")
    ap.add_argument("--no-trim", action="store_true", help="skip uniform transparent trim")
    ap.add_argument("--dedupe", action="store_true", help="drop consecutive identical frames")
    args = ap.parse_args()

    frames = load_frames(args.inputs, args.grid)
    if args.dedupe:
        kept = [frames[0]]
        for f in frames[1:]:
            if np.any(np.array(f) != np.array(kept[-1])):
                kept.append(f)
        frames = kept
    if not args.no_trim:
        frames = uniform_trim(frames)
    if args.height:
        w = round(frames[0].width * args.height / frames[0].height)
        frames = [f.resize((w, args.height), Image.NEAREST) for f in frames]

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    total = 0
    for i, f in enumerate(frames, 1):
        p = out / f"{args.name}_{i:02d}.webp"
        f.save(p, "WEBP", lossless=True, quality=100, method=6)
        total += p.stat().st_size
        print(f"  {p.name}  {f.width}x{f.height}  {p.stat().st_size // 1024}K")

    w, h, n = frames[0].width, frames[0].height, len(frames)
    ram = w * h * 4 * n / 1e6
    print(f"\n{n} frames {w}x{h}, files {total / 1e6:.2f}MB, decoded RAM ~{ram:.1f}MB")
    if ram > RAM_BUDGET_MB:
        print(f"WARNING: exceeds {RAM_BUDGET_MB}MB RAM budget — reduce size or frame count")
    print(seam_report(frames))


if __name__ == "__main__":
    main()
