#!/usr/bin/env python3
import argparse
import json
from pathlib import Path

from PIL import Image


def inspect(path: Path):
    image = Image.open(path).convert("RGBA")
    total = image.width * image.height
    alpha_hist = image.getchannel("A").histogram()
    transparent = alpha_hist[0]
    opaque = alpha_hist[255]
    partial = sum(alpha_hist[1:255])
    color_leak = 0
    opaque_colors = set()
    bbox = image.getbbox()

    for r, g, b, a in image.getdata():
        if a == 0 and (r or g or b):
            color_leak += 1
        if a == 255:
            opaque_colors.add((r, g, b))

    return {
        "path": str(path),
        "width": image.width,
        "height": image.height,
        "mode": image.mode,
        "bbox": bbox,
        "transparent_pixels": transparent,
        "opaque_pixels": opaque,
        "partial_alpha_pixels": partial,
        "partial_alpha_ratio": partial / total if total else 0,
        "color_leak_pixels": color_leak,
        "opaque_color_count": len(opaque_colors),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input")
    parser.add_argument("--pixel-art", action="store_true")
    parser.add_argument("--max-colors", type=int, default=256)
    parser.add_argument("--max-partial-alpha-ratio", type=float, default=0.02)
    args = parser.parse_args()

    report = inspect(Path(args.input))
    failures = []
    if args.pixel_art:
        if report["color_leak_pixels"] != 0:
            failures.append("color_leak_pixels must be 0")
        if report["partial_alpha_ratio"] > args.max_partial_alpha_ratio:
            failures.append("partial_alpha_ratio above limit")
        if report["opaque_color_count"] > args.max_colors:
            failures.append("opaque_color_count above limit")
    report["status"] = "FAIL" if failures else "PASS"
    report["failures"] = failures
    print(json.dumps(report, indent=2))
    raise SystemExit(1 if failures else 0)


if __name__ == "__main__":
    main()
