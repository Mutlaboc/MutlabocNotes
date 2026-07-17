#!/usr/bin/env python3
import argparse
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw


def load_palette(path: Path):
    with path.open("r", encoding="utf-8") as fh:
        data = json.load(fh)
    colors = data["colors"] if isinstance(data, dict) else data
    return [tuple(int(v) for v in color[:3]) for color in colors]


def nearest(color, palette):
    r, g, b = color
    return min(
        palette,
        key=lambda p: (r - p[0]) * (r - p[0]) + (g - p[1]) * (g - p[1]) + (b - p[2]) * (b - p[2]),
    )


def extract_colors(image: Image.Image, max_colors: int):
    rgba = image.convert("RGBA")
    rgb = Image.new("RGB", rgba.size, (0, 0, 0))
    rgb.paste(rgba.convert("RGB"), mask=rgba.getchannel("A"))
    quantized = rgb.quantize(colors=max_colors, method=Image.Quantize.MEDIANCUT)
    palette = quantized.getpalette()[: max_colors * 3]
    counts = quantized.getcolors(max_colors * max_colors) or []
    used = []
    for _count, index in sorted(counts, key=lambda item: item[0], reverse=True):
        start = index * 3
        used.append(tuple(palette[start : start + 3]))
    if (0, 0, 0) not in used:
        used.insert(0, (0, 0, 0))
    return used[:max_colors]


def write_swatch(colors, path: Path):
    cell = 24
    columns = 16
    rows = math.ceil(len(colors) / columns)
    image = Image.new("RGB", (columns * cell, rows * cell), (255, 255, 255))
    draw = ImageDraw.Draw(image)
    for index, color in enumerate(colors):
        x = (index % columns) * cell
        y = (index // columns) * cell
        draw.rectangle((x, y, x + cell - 1, y + cell - 1), fill=color)
    image.save(path)


def quantize_to_palette(input_path: Path, palette_path: Path, output_path: Path):
    palette = load_palette(palette_path)
    image = Image.open(input_path).convert("RGBA")
    output = Image.new("RGBA", image.size, (0, 0, 0, 0))
    pixels_in = image.load()
    pixels_out = output.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels_in[x, y]
            if a == 0:
                pixels_out[x, y] = (0, 0, 0, 0)
            else:
                nr, ng, nb = nearest((r, g, b), palette)
                pixels_out[x, y] = (nr, ng, nb, a)
    output.save(output_path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input")
    parser.add_argument("--out", required=True)
    parser.add_argument("--swatch")
    parser.add_argument("--max-colors", type=int, default=256)
    parser.add_argument("--quantize", action="store_true")
    parser.add_argument("--palette")
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.out)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    if args.quantize:
        if not args.palette:
            raise SystemExit("--palette is required with --quantize")
        quantize_to_palette(input_path, Path(args.palette), output_path)
        return

    colors = extract_colors(Image.open(input_path), args.max_colors)
    with output_path.open("w", encoding="utf-8") as fh:
        json.dump({"source": str(input_path), "colors": colors}, fh, indent=2)
        fh.write("\n")
    if args.swatch:
        write_swatch(colors, Path(args.swatch))


if __name__ == "__main__":
    main()
