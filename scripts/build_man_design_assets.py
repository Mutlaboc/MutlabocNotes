#!/usr/bin/env python3
import argparse
import json
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw


NATIVE_SIZE = (36, 68)
SCALE = 5
FRAME_SIZE = (180, 340)
GROUND_Y = 1150


TARGETS = {
    "outline": (0, 0, 0),
    "wood_dark": (38, 24, 14),
    "wood_mid": (74, 45, 25),
    "wood_light": (122, 80, 45),
    "shirt_shadow": (118, 79, 42),
    "shirt": (168, 119, 67),
    "shirt_light": (213, 164, 97),
    "skin_shadow": (133, 82, 48),
    "skin": (196, 132, 78),
    "skin_light": (232, 176, 104),
    "green_dark": (30, 68, 54),
    "green": (53, 101, 77),
    "red": (150, 35, 29),
}


def load_palette(path: Path):
    with path.open("r", encoding="utf-8") as fh:
        data = json.load(fh)
    return [tuple(color[:3]) for color in data["colors"]]


def nearest(color, palette):
    r, g, b = color
    return min(
        palette,
        key=lambda p: (r - p[0]) * (r - p[0]) + (g - p[1]) * (g - p[1]) + (b - p[2]) * (b - p[2]),
    )


def pick_colors(palette):
    return {name: nearest(rgb, palette) for name, rgb in TARGETS.items()}


def rect(draw, box, fill):
    draw.rectangle(box, fill=fill)


def poly(draw, points, fill):
    draw.polygon(points, fill=fill)


def draw_body(draw, c, think=False):
    # Boots and feet.
    poly(draw, [(11, 59), (17, 59), (17, 65), (20, 65), (20, 67), (10, 67), (10, 63)], c["outline"])
    rect(draw, (12, 60, 16, 64), c["wood_mid"])
    rect(draw, (11, 65, 19, 66), c["wood_dark"])
    poly(draw, [(20, 59), (25, 59), (25, 65), (28, 65), (28, 67), (19, 67), (19, 63)], c["outline"])
    rect(draw, (20, 60, 24, 64), c["wood_mid"])
    rect(draw, (20, 65, 27, 66), c["wood_dark"])

    # Porty / trousers.
    poly(draw, [(11, 41), (18, 41), (18, 60), (12, 60), (11, 52)], c["outline"])
    poly(draw, [(12, 42), (17, 42), (17, 59), (13, 59), (12, 52)], c["green_dark"])
    rect(draw, (15, 45, 17, 55), c["green"])
    poly(draw, [(20, 41), (26, 42), (25, 60), (19, 60), (19, 49)], c["outline"])
    poly(draw, [(20, 42), (24, 43), (24, 59), (20, 59), (20, 49)], c["green_dark"])
    rect(draw, (20, 45, 22, 55), c["green"])

    # Kosovorotka silhouette.
    poly(draw, [(9, 22), (22, 21), (26, 28), (26, 39), (23, 44), (10, 44), (7, 38), (8, 28)], c["outline"])
    poly(draw, [(10, 23), (21, 23), (24, 29), (24, 38), (22, 42), (11, 42), (9, 37), (9, 29)], c["shirt"])
    rect(draw, (10, 33, 23, 35), c["shirt_shadow"])
    rect(draw, (12, 24, 19, 27), c["shirt_light"])
    rect(draw, (9, 37, 24, 39), c["wood_dark"])
    rect(draw, (11, 37, 19, 38), c["red"])
    rect(draw, (20, 37, 23, 38), c["green"])

    # Head, profile nose, beard, cap.
    poly(draw, [(13, 10), (23, 10), (25, 13), (25, 18), (22, 22), (14, 22), (12, 19), (12, 13)], c["outline"])
    rect(draw, (14, 11, 22, 17), c["skin"])
    rect(draw, (16, 12, 21, 14), c["skin_light"])
    rect(draw, (22, 14, 26, 16), c["outline"])
    rect(draw, (22, 14, 25, 15), c["skin_light"])
    rect(draw, (17, 14, 18, 15), c["outline"])
    poly(draw, [(13, 17), (23, 17), (23, 24), (15, 25), (13, 21)], c["outline"])
    rect(draw, (15, 18, 22, 23), c["wood_dark"])
    rect(draw, (17, 19, 21, 21), c["wood_mid"])
    rect(draw, (12, 7, 25, 10), c["outline"])
    rect(draw, (14, 4, 22, 8), c["green_dark"])
    rect(draw, (15, 4, 20, 6), c["green"])
    rect(draw, (22, 8, 28, 9), c["outline"])
    rect(draw, (22, 8, 26, 8), c["green_dark"])

    # Embroidery hint on the side collar.
    rect(draw, (21, 24, 22, 30), c["red"])
    rect(draw, (22, 27, 23, 28), c["green"])

    if think:
        # Bent arm up to chin.
        poly(draw, [(7, 28), (12, 28), (12, 38), (10, 41), (7, 39)], c["outline"])
        rect(draw, (8, 29, 10, 37), c["shirt_shadow"])
        poly(draw, [(11, 25), (18, 25), (19, 29), (11, 30)], c["outline"])
        rect(draw, (12, 26, 17, 28), c["shirt"])
        poly(draw, [(18, 21), (22, 22), (22, 27), (18, 27)], c["outline"])
        rect(draw, (19, 22, 21, 26), c["skin_light"])
    else:
        # Relaxed sleeve and visible hand.
        poly(draw, [(6, 27), (11, 27), (12, 40), (10, 44), (7, 43), (6, 36)], c["outline"])
        rect(draw, (8, 28, 10, 39), c["shirt_shadow"])
        rect(draw, (9, 40, 14, 44), c["outline"])
        rect(draw, (10, 40, 13, 43), c["skin"])

    # Pixel texture on cloth and pants.
    for x, y, color_name in [
        (11, 29, "shirt_shadow"),
        (18, 31, "shirt_light"),
        (22, 33, "shirt_shadow"),
        (12, 40, "shirt_light"),
        (14, 49, "green"),
        (21, 50, "green"),
        (15, 55, "green_dark"),
        (23, 56, "green_dark"),
    ]:
        rect(draw, (x, y, x, y), c[color_name])


def make_pose(colors, think=False):
    image = Image.new("RGBA", NATIVE_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw_body(draw, colors, think=think)
    return image.resize(FRAME_SIZE, Image.Resampling.NEAREST)


def extract_base_house(lottie_path: Path, out_path: Path):
    with zipfile.ZipFile(lottie_path) as archive:
        data = archive.read("images/base_house.png")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(data)


def make_overlay(base_house: Path, stand: Path, output: Path):
    house = Image.open(base_house).convert("RGBA")
    man = Image.open(stand).convert("RGBA")
    overlay = house.copy()
    x = 150
    y = GROUND_Y - man.height
    overlay.alpha_composite(man, (x, y))
    draw = ImageDraw.Draw(overlay)
    draw.line((0, GROUND_Y, overlay.width, GROUND_Y), fill=(160, 35, 28, 180), width=2)
    draw.rectangle((x, y, x + man.width - 1, y + man.height - 1), outline=(35, 120, 70, 180), width=2)
    output.parent.mkdir(parents=True, exist_ok=True)
    overlay.convert("RGB").save(output)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--lottie", default="app/src/main/res/raw/home.lottie")
    parser.add_argument("--palette", default="docs/motion/man_design/house_palette.json")
    parser.add_argument("--work-dir", default="docs/motion/man_design")
    parser.add_argument("--res-dir", default="app/src/main/res/drawable-nodpi")
    args = parser.parse_args()

    work_dir = Path(args.work_dir)
    res_dir = Path(args.res_dir)
    work_dir.mkdir(parents=True, exist_ok=True)
    res_dir.mkdir(parents=True, exist_ok=True)

    base_house = work_dir / "base_house.png"
    extract_base_house(Path(args.lottie), base_house)

    palette = load_palette(Path(args.palette))
    colors = pick_colors(palette)
    with (work_dir / "man_design_colors.json").open("w", encoding="utf-8") as fh:
        json.dump({key: list(value) for key, value in colors.items()}, fh, indent=2)
        fh.write("\n")

    stand = make_pose(colors, think=False)
    think = make_pose(colors, think=True)
    stand.save(res_dir / "man_design_stand.png")
    think.save(res_dir / "man_design_think.png")
    stand.save(work_dir / "man_design_stand.png")
    think.save(work_dir / "man_design_think.png")
    make_overlay(base_house, res_dir / "man_design_stand.png", work_dir / "man_scale_overlay.png")


if __name__ == "__main__":
    main()
