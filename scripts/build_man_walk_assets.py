#!/usr/bin/env python3
import argparse
import json
import shutil
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw


NATIVE_SIZE = (36, 68)
FRAME_SIZE = (180, 340)
GROUND_Y = 1150
SCENE_SIZE = 1254

TARGETS = {
    "outline": (0, 0, 0),
    "wood_dark": (38, 24, 14),
    "wood_mid": (82, 48, 25),
    "wood_light": (126, 78, 38),
    "cream_shadow": (154, 121, 72),
    "cream": (216, 179, 112),
    "cream_light": (239, 210, 145),
    "skin_shadow": (140, 79, 41),
    "skin": (205, 125, 66),
    "skin_light": (242, 168, 84),
    "pants_dark": (31, 40, 56),
    "pants": (50, 64, 83),
    "pants_light": (66, 82, 100),
    "red": (151, 42, 30),
    "gold": (215, 156, 41),
}


WALK_POSES = [
    {"front": "fwd", "back": "back", "bob": 0, "arm": -2, "bag": 0},
    {"front": "plant", "back": "trail", "bob": 1, "arm": -1, "bag": 1},
    {"front": "under", "back": "lift", "bob": 0, "arm": 0, "bag": 1},
    {"front": "back", "back": "fwd", "bob": -1, "arm": 1, "bag": 0},
    {"front": "back", "back": "fwd", "bob": 0, "arm": 2, "bag": 0},
    {"front": "trail", "back": "plant", "bob": 1, "arm": 1, "bag": -1},
    {"front": "lift", "back": "under", "bob": 0, "arm": 0, "bag": -1},
    {"front": "fwd", "back": "back", "bob": -1, "arm": -1, "bag": 0},
]

THINK_TAP = [0, 1, 2, 1, 0, 0]


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


def extract_base_house(lottie_path: Path, out_path: Path):
    with zipfile.ZipFile(lottie_path) as archive:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_bytes(archive.read("images/base_house.png"))


def draw_boot(draw, c, x, y, forward=0):
    toe = 3 if forward > 0 else -1 if forward < 0 else 1
    poly(
        draw,
        [
            (x, y),
            (x + 5, y),
            (x + 5, y + 5),
            (x + 6 + toe, y + 5),
            (x + 6 + toe, y + 7),
            (x - 1, y + 7),
            (x - 1, y + 3),
        ],
        c["outline"],
    )
    rect(draw, (x + 1, y + 1, x + 4, y + 4), c["wood_mid"])
    rect(draw, (x, y + 5, x + 5 + toe, y + 6), c["wood_dark"])


def leg_points(kind, hip_x, hip_y):
    if kind == "fwd":
        return [(hip_x, hip_y), (hip_x - 1, 52), (hip_x - 4, 60), (hip_x - 5, 60)]
    if kind == "plant":
        return [(hip_x, hip_y), (hip_x, 52), (hip_x - 2, 60), (hip_x - 2, 60)]
    if kind == "under":
        return [(hip_x, hip_y), (hip_x + 1, 52), (hip_x + 1, 60), (hip_x + 1, 60)]
    if kind == "lift":
        return [(hip_x, hip_y), (hip_x + 2, 51), (hip_x + 5, 58), (hip_x + 5, 58)]
    if kind == "trail":
        return [(hip_x, hip_y), (hip_x + 2, 52), (hip_x + 5, 60), (hip_x + 5, 60)]
    return [(hip_x, hip_y), (hip_x + 2, 52), (hip_x + 4, 60), (hip_x + 5, 60)]


def draw_leg(draw, c, kind, hip_x, hip_y, front):
    pts = leg_points(kind, hip_x, hip_y)
    knee_x, knee_y = pts[1]
    foot_x, foot_y = pts[-1]
    color = c["pants"] if front else c["pants_dark"]
    light = c["pants_light"] if front else c["pants"]
    poly(draw, [(hip_x - 2, hip_y), (hip_x + 3, hip_y), (knee_x + 3, knee_y), (knee_x - 2, knee_y)], c["outline"])
    poly(draw, [(knee_x - 2, knee_y), (knee_x + 3, knee_y), (foot_x + 3, foot_y), (foot_x - 2, foot_y)], c["outline"])
    poly(draw, [(hip_x - 1, hip_y + 1), (hip_x + 2, hip_y + 1), (knee_x + 2, knee_y), (knee_x - 1, knee_y)], color)
    poly(draw, [(knee_x - 1, knee_y), (knee_x + 2, knee_y), (foot_x + 2, foot_y), (foot_x - 1, foot_y)], color)
    rect(draw, (min(hip_x, knee_x), min(hip_y + 4, knee_y), min(hip_x, knee_x) + 1, min(hip_y + 8, knee_y + 4)), light)
    forward = 1 if kind in {"fwd", "plant"} else -1 if kind in {"back", "trail"} else 0
    draw_boot(draw, c, foot_x - 2, 60, forward=forward)


def draw_bag(draw, c, y_offset):
    poly(draw, [(3, 33 + y_offset), (11, 30 + y_offset), (15, 36 + y_offset), (14, 48 + y_offset), (5, 49 + y_offset), (2, 42 + y_offset)], c["outline"])
    poly(draw, [(5, 35 + y_offset), (11, 33 + y_offset), (13, 38 + y_offset), (12, 45 + y_offset), (6, 46 + y_offset), (5, 40 + y_offset)], c["cream"])
    rect(draw, (6, 40 + y_offset, 8, 43 + y_offset), c["red"])
    rect(draw, (10, 39 + y_offset, 12, 42 + y_offset), c["red"])
    rect(draw, (8, 35 + y_offset, 11, 36 + y_offset), c["cream_light"])
    rect(draw, (5, 46 + y_offset, 13, 48 + y_offset), c["wood_dark"])


def draw_upper_body(draw, c, bob, arm_swing=0, thinking=False, turn=0):
    y = bob
    if turn == 2:
        poly(draw, [(7, 23 + y), (27, 23 + y), (30, 31 + y), (28, 45 + y), (8, 45 + y), (5, 31 + y)], c["outline"])
        rect(draw, (12, 25 + y, 22, 43 + y), c["cream"])
        rect(draw, (8, 25 + y, 13, 44 + y), c["wood_mid"])
        rect(draw, (22, 25 + y, 27, 44 + y), c["wood_mid"])
        rect(draw, (15, 28 + y, 19, 30 + y), c["red"])
        rect(draw, (15, 34 + y, 18, 37 + y), c["red"])
        rect(draw, (20, 34 + y, 22, 37 + y), c["red"])
    else:
        poly(draw, [(8, 22 + y), (25, 22 + y), (29, 30 + y), (27, 44 + y), (10, 45 + y), (7, 38 + y), (7, 28 + y)], c["outline"])
        poly(draw, [(12, 24 + y), (22, 24 + y), (24, 30 + y), (24, 42 + y), (13, 43 + y), (10, 37 + y), (10, 30 + y)], c["cream"])
        rect(draw, (8, 25 + y, 13, 43 + y), c["wood_mid"])
        rect(draw, (23, 26 + y, 27, 43 + y), c["wood_mid"])
        rect(draw, (10, 30 + y, 12, 42 + y), c["wood_dark"])
        rect(draw, (24, 33 + y, 26, 39 + y), c["wood_dark"])
        rect(draw, (14, 25 + y, 20, 28 + y), c["cream_light"])
        rect(draw, (15, 30 + y, 18, 33 + y), c["red"])
        rect(draw, (20, 30 + y, 22, 33 + y), c["red"])
        rect(draw, (15, 36 + y, 18, 39 + y), c["red"])
        rect(draw, (20, 36 + y, 22, 39 + y), c["red"])
        rect(draw, (11, 39 + y, 26, 41 + y), c["wood_dark"])
        rect(draw, (17, 39 + y, 19, 40 + y), c["gold"])

    # Strap over the shirt and vest, referencing the source image bag.
    rect(draw, (9, 25 + y, 11, 39 + y), c["wood_dark"])
    rect(draw, (11, 29 + y, 12, 34 + y), c["wood_light"])

    if thinking:
        poly(draw, [(8, 28 + y), (13, 28 + y), (13, 39 + y), (10, 42 + y), (7, 39 + y)], c["outline"])
        rect(draw, (9, 29 + y, 11, 37 + y), c["cream_shadow"])
        poly(draw, [(12, 25 + y), (19, 25 + y), (20, 29 + y), (12, 30 + y)], c["outline"])
        rect(draw, (13, 26 + y, 18, 28 + y), c["cream"])
        rect(draw, (19, 22 + y, 22, 27 + y), c["outline"])
        rect(draw, (20, 23 + y, 21, 26 + y), c["skin_light"])
    else:
        sleeve_y = y + max(-1, min(1, arm_swing))
        poly(draw, [(26, 28 + sleeve_y), (30, 29 + sleeve_y), (30, 41 + sleeve_y), (27, 45 + sleeve_y), (24, 41 + sleeve_y)], c["outline"])
        rect(draw, (26, 29 + sleeve_y, 28, 39 + sleeve_y), c["cream_shadow"])
        rect(draw, (26, 40 + sleeve_y, 30, 44 + sleeve_y), c["outline"])
        rect(draw, (27, 40 + sleeve_y, 29, 43 + sleeve_y), c["skin_light"])


def draw_head(draw, c, bob, turn=0):
    y = bob
    if turn == 2:
        poly(draw, [(12, 10 + y), (24, 10 + y), (27, 14 + y), (26, 22 + y), (22, 26 + y), (14, 25 + y), (10, 21 + y), (10, 14 + y)], c["outline"])
        rect(draw, (14, 12 + y, 23, 19 + y), c["skin"])
        rect(draw, (16, 12 + y, 21, 14 + y), c["skin_light"])
        rect(draw, (15, 16 + y, 16, 19 + y), c["outline"])
        rect(draw, (21, 16 + y, 22, 19 + y), c["outline"])
        rect(draw, (14, 20 + y, 23, 25 + y), c["wood_dark"])
        rect(draw, (16, 21 + y, 21, 22 + y), c["wood_mid"])
        rect(draw, (12, 7 + y, 25, 10 + y), c["wood_dark"])
        rect(draw, (14, 5 + y, 23, 8 + y), c["wood_mid"])
        rect(draw, (15, 5 + y, 20, 6 + y), c["wood_light"])
        return

    # Front-leaning 3/4 head from the reference: broad brown hair/cap,
    # two dark eyes, right ear, wide moustache and beard.
    poly(draw, [(12, 9 + y), (24, 9 + y), (27, 13 + y), (27, 19 + y), (24, 24 + y), (15, 25 + y), (11, 21 + y), (10, 14 + y)], c["outline"])
    rect(draw, (13, 11 + y, 24, 19 + y), c["skin"])
    rect(draw, (16, 12 + y, 22, 14 + y), c["skin_light"])
    rect(draw, (24, 15 + y, 26, 20 + y), c["skin_light"])
    rect(draw, (14, 15 + y, 15, 19 + y), c["outline"])
    rect(draw, (21, 15 + y, 22, 19 + y), c["outline"])
    rect(draw, (17, 17 + y, 19, 18 + y), c["skin_shadow"])
    rect(draw, (13, 20 + y, 24, 25 + y), c["wood_dark"])
    rect(draw, (16, 21 + y, 22, 22 + y), c["wood_mid"])
    rect(draw, (14, 19 + y, 22, 20 + y), c["wood_mid"])
    poly(draw, [(10, 8 + y), (14, 5 + y), (24, 5 + y), (28, 9 + y), (27, 13 + y), (23, 11 + y), (18, 11 + y), (13, 12 + y), (10, 14 + y)], c["outline"])
    rect(draw, (14, 6 + y, 23, 9 + y), c["wood_mid"])
    rect(draw, (16, 6 + y, 21, 7 + y), c["wood_light"])
    rect(draw, (11, 10 + y, 16, 13 + y), c["wood_dark"])
    rect(draw, (22, 10 + y, 27, 14 + y), c["wood_dark"])


def make_walk_frame(colors, pose):
    image = Image.new("RGBA", NATIVE_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw_bag(draw, colors, pose["bag"])
    draw_leg(draw, colors, pose["back"], 19, 41, front=False)
    draw_leg(draw, colors, pose["front"], 13, 41, front=True)
    draw_upper_body(draw, colors, pose["bob"], arm_swing=pose["arm"])
    draw_head(draw, colors, pose["bob"])
    return image.resize(FRAME_SIZE, Image.Resampling.NEAREST)


def make_think_frame(colors, tap):
    image = Image.new("RGBA", NATIVE_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw_bag(draw, colors, 0)
    draw_leg(draw, colors, "under", 13, 41, front=True)
    draw_leg(draw, colors, "under", 21, 41, front=False)
    if tap:
        # Toe lift on the front boot while the back boot stays grounded.
        rect(draw, (10, 64 - tap, 18, 67 - tap), (0, 0, 0, 0))
        poly(draw, [(10, 61 - tap), (16, 60 - tap), (20, 63), (20, 66), (10, 66)], colors["outline"])
        rect(draw, (12, 62 - tap, 16, 64 - tap), colors["wood_mid"])
        rect(draw, (11, 65, 19, 66), colors["wood_dark"])
    draw_upper_body(draw, colors, 0, thinking=True)
    draw_head(draw, colors, 0)
    return image.resize(FRAME_SIZE, Image.Resampling.NEAREST)


def make_turn_frame(colors, index):
    image = Image.new("RGBA", NATIVE_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if index == 1:
        draw_bag(draw, colors, 0)
        draw_leg(draw, colors, "under", 13, 41, front=True)
        draw_leg(draw, colors, "under", 21, 41, front=False)
        draw_upper_body(draw, colors, 0, arm_swing=0, turn=1)
        draw_head(draw, colors, 0, turn=1)
    elif index == 2:
        draw_leg(draw, colors, "under", 13, 41, front=True)
        draw_leg(draw, colors, "under", 21, 41, front=False)
        draw_upper_body(draw, colors, 0, arm_swing=0, turn=2)
        draw_head(draw, colors, 0, turn=2)
    else:
        draw_bag(draw, colors, 0)
        draw_leg(draw, colors, "under", 13, 41, front=True)
        draw_leg(draw, colors, "under", 21, 41, front=False)
        draw_upper_body(draw, colors, 0, arm_swing=0, turn=1)
        draw_head(draw, colors, 0, turn=1)
    return image.resize(FRAME_SIZE, Image.Resampling.NEAREST)


def make_overlay(base_house: Path, frame: Path, output: Path):
    house = Image.open(base_house).convert("RGBA")
    man = Image.open(frame).convert("RGBA")
    overlay = house.copy()
    x = 150
    y = GROUND_Y - man.height
    overlay.alpha_composite(man, (x, y))
    draw = ImageDraw.Draw(overlay)
    draw.line((0, GROUND_Y, overlay.width, GROUND_Y), fill=(160, 35, 28, 180), width=2)
    draw.rectangle((x, y, x + man.width - 1, y + man.height - 1), outline=(35, 120, 70, 180), width=2)
    output.parent.mkdir(parents=True, exist_ok=True)
    overlay.convert("RGB").save(output)


def make_preview(work_dir: Path, output: Path):
    bg = Image.open(work_dir / "base_house.png").convert("RGBA").resize((627, 627), Image.Resampling.NEAREST)
    frames = []
    right = [Image.open(work_dir / f"man_walk_{i:02}.png").convert("RGBA") for i in range(1, 9)]
    left = [img.transpose(Image.Transpose.FLIP_LEFT_RIGHT) for img in right]
    think = [Image.open(work_dir / f"man_think_{i:02}.png").convert("RGBA") for i in range(1, 7)]
    turn = [Image.open(work_dir / f"man_turn_{i:02}.png").convert("RGBA") for i in range(1, 4)]
    mini_w, mini_h = 90, 170
    ground = int(GROUND_Y * 0.5)
    y = ground - mini_h

    for step in range(32):
        canvas = bg.copy()
        x = int((150 + (950 * step / 31)) * 0.5) - mini_w // 2
        man = right[step % 8].resize((mini_w, mini_h), Image.Resampling.NEAREST)
        canvas.alpha_composite(man, (x, y))
        frames.append(canvas.convert("P", palette=Image.Palette.ADAPTIVE))
    for step in range(12):
        canvas = bg.copy()
        man = think[step % 6].resize((mini_w, mini_h), Image.Resampling.NEAREST)
        canvas.alpha_composite(man, (int(1100 * 0.5) - mini_w // 2, y))
        frames.append(canvas.convert("P", palette=Image.Palette.ADAPTIVE))
    for idx, man_src in enumerate(turn):
        canvas = bg.copy()
        man = man_src.resize((mini_w, mini_h), Image.Resampling.NEAREST)
        canvas.alpha_composite(man, (int(1100 * 0.5) - mini_w // 2, y))
        frames.extend([canvas.convert("P", palette=Image.Palette.ADAPTIVE)] * 2)
    for step in range(32):
        canvas = bg.copy()
        x = int((1100 - (950 * step / 31)) * 0.5) - mini_w // 2
        man = left[step % 8].resize((mini_w, mini_h), Image.Resampling.NEAREST)
        canvas.alpha_composite(man, (x, y))
        frames.append(canvas.convert("P", palette=Image.Palette.ADAPTIVE))

    output.parent.mkdir(parents=True, exist_ok=True)
    frames[0].save(output, save_all=True, append_images=frames[1:], duration=80, loop=0)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--lottie", default="app/src/main/res/raw/home.lottie")
    parser.add_argument("--reference", default=r"C:\Users\Yuriy\Downloads\ChatGPT Image Jun 24, 2026, 04_16_15 PM.png")
    parser.add_argument("--palette", default="docs/motion/man_walk/house_palette.json")
    parser.add_argument("--work-dir", default="docs/motion/man_walk")
    parser.add_argument("--res-dir", default="app/src/main/res/drawable-nodpi")
    args = parser.parse_args()

    work_dir = Path(args.work_dir)
    res_dir = Path(args.res_dir)
    work_dir.mkdir(parents=True, exist_ok=True)
    res_dir.mkdir(parents=True, exist_ok=True)

    base_house = work_dir / "base_house.png"
    extract_base_house(Path(args.lottie), base_house)
    reference = Path(args.reference)
    if reference.exists():
        shutil.copyfile(reference, work_dir / "source_reference.png")

    palette = load_palette(Path(args.palette))
    colors = pick_colors(palette)
    with (work_dir / "man_walk_colors.json").open("w", encoding="utf-8") as fh:
        json.dump({key: list(value) for key, value in colors.items()}, fh, indent=2)
        fh.write("\n")

    generated = {}
    for index, pose in enumerate(WALK_POSES, start=1):
        generated[f"man_walk_{index:02}.png"] = make_walk_frame(colors, pose)
    for index in range(1, 4):
        generated[f"man_turn_{index:02}.png"] = make_turn_frame(colors, index)
    for index, tap in enumerate(THINK_TAP, start=1):
        generated[f"man_think_{index:02}.png"] = make_think_frame(colors, tap)

    for name, image in generated.items():
        image.save(work_dir / name)
        image.save(res_dir / name)

    make_overlay(base_house, res_dir / "man_walk_01.png", work_dir / "man_scale_overlay.png")
    make_preview(work_dir, work_dir / "man_walk_preview.gif")


if __name__ == "__main__":
    main()
