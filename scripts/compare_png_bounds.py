#!/usr/bin/env python3
import argparse
import json
from pathlib import Path

from PIL import Image


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("inputs", nargs="+")
    args = parser.parse_args()

    entries = []
    failures = []
    first_size = None
    first_bottom = None

    for raw in args.inputs:
        path = Path(raw)
        image = Image.open(path).convert("RGBA")
        bbox = image.getbbox()
        bottom = bbox[3] if bbox else None
        entry = {
            "path": str(path),
            "size": [image.width, image.height],
            "bbox": list(bbox) if bbox else None,
            "content_bottom": bottom,
        }
        entries.append(entry)
        if first_size is None:
            first_size = image.size
            first_bottom = bottom
        elif image.size != first_size:
            failures.append(f"{path} size {image.size} != {first_size}")
        elif bottom != first_bottom:
            failures.append(f"{path} content_bottom {bottom} != {first_bottom}")

    report = {"status": "FAIL" if failures else "PASS", "frames": entries, "failures": failures}
    print(json.dumps(report, indent=2))
    raise SystemExit(1 if failures else 0)


if __name__ == "__main__":
    main()
