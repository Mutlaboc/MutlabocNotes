#!/usr/bin/env python3
import argparse
from pathlib import Path

from PIL import Image


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input")
    parser.add_argument("--out", required=True)
    parser.add_argument("--threshold", type=int, default=128)
    args = parser.parse_args()

    source = Image.open(args.input).convert("RGBA")
    output = Image.new("RGBA", source.size, (0, 0, 0, 0))
    src = source.load()
    dst = output.load()
    for y in range(source.height):
        for x in range(source.width):
            r, g, b, a = src[x, y]
            if a >= args.threshold:
                dst[x, y] = (r, g, b, 255)
            else:
                dst[x, y] = (0, 0, 0, 0)

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    output.save(out)


if __name__ == "__main__":
    main()
