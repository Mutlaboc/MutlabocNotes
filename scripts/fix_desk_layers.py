"""Rebuild the Home-info desk layers so the whole hand *and* the whole pencil move.

The shipped split cut the figure in half: ``desk_hand`` held the upper hand and the
pencil shaft, while the lower fingers and the pencil tip stayed baked into
``desk_base``. Rocking the hand layer therefore bent the pencil at the cut line and
tore a dark sliver out of the fingers.

This script re-cuts the layers:

* the moving layer becomes the *complete* hand + pencil figure, found by flooding the
  background inwards from the border of a work rectangle -- the figure's black outline
  stops the flood, so whatever the flood cannot reach is the figure;
* the base gets that whole silhouette erased and inpainted from its surroundings
  (wood, paper, sleeve), so a few degrees of rotation never uncover a hole;
* a stray two-pixel fleck that hangs on the wall in the old hand layer is dropped.

Run from the repo root:  python scripts/fix_desk_layers.py [--preview-only]
"""

from __future__ import annotations

import argparse
import os
from collections import deque

from PIL import Image

RES_DIR = os.path.join("app", "src", "main", "res", "drawable-nodpi")
BASE_PATH = os.path.join(RES_DIR, "desk_base.webp")
HAND_PATH = os.path.join(RES_DIR, "desk_hand.webp")

# Work rectangle around the part of the figure that is still stuck in the base
# (lower fingers + pencil below the old cut). Coordinates are asset pixels, 836x736.
WORK_BOX = (274, 350, 360, 414)

# Anything this dark reads as outline: it blocks the background flood, and it joins the
# figure only when it hugs the figure's bright interior (the wall planks and the desk
# shadow are just as dark, but they sit further away).
OUTLINE_MAX = 46
OUTLINE_GROW = 3
ALPHA_MIN = 32


def load_layers():
    base = Image.open(BASE_PATH).convert("RGBA")
    hand = Image.open(HAND_PATH).convert("RGBA")
    if base.size != hand.size:
        raise SystemExit(f"layer sizes differ: {base.size} vs {hand.size}")
    return base, hand


def hand_alpha_mask(hand: Image.Image) -> set:
    alpha = hand.split()[3].load()
    w, h = hand.size
    return {(x, y) for y in range(h) for x in range(w) if alpha[x, y] > ALPHA_MIN}


def lower_figure_mask(comp: Image.Image, seed: set) -> set:
    """The rest of the figure inside WORK_BOX: whatever the background cannot reach.

    Flooding from the border stops at the figure's dark outline, so the bright pixels it
    never reaches are the figure's interior. The outline itself is then added back as a
    thin skin around that interior -- picking it up by darkness alone would also swallow
    the wall planks and the shadow under the desk, which are just as dark.
    """
    px = comp.load()
    x0, y0, x1, y1 = WORK_BOX

    def dark(x, y):
        r, g, b, _ = px[x, y]
        return max(r, g, b) <= OUTLINE_MAX

    def blocked(x, y):
        return (x, y) in seed or dark(x, y)

    reached = set()
    queue = deque()
    for x in range(x0, x1):
        for y in (y0, y1 - 1):
            if not blocked(x, y):
                queue.append((x, y))
    for y in range(y0, y1):
        for x in (x0, x1 - 1):
            if not blocked(x, y):
                queue.append((x, y))
    while queue:
        x, y = queue.popleft()
        if (x, y) in reached:
            continue
        reached.add((x, y))
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if x0 <= nx < x1 and y0 <= ny < y1 and (nx, ny) not in reached and not blocked(nx, ny):
                queue.append((nx, ny))

    interior = {
        (x, y)
        for y in range(y0, y1)
        for x in range(x0, x1)
        if (x, y) not in reached and not dark(x, y)
    }
    figure = set(interior)
    for y in range(y0, y1):
        for x in range(x0, x1):
            if (x, y) in figure or not dark(x, y):
                continue
            near = any(
                (x + dx, y + dy) in interior
                for dy in range(-OUTLINE_GROW, OUTLINE_GROW + 1)
                for dx in range(-OUTLINE_GROW, OUTLINE_GROW + 1)
            )
            if near:
                figure.add((x, y))
    return figure


def largest_component(mask: set) -> set:
    """Drops detached specks -- the old hand layer carries a fleck of wall decor."""
    remaining = set(mask)
    best = set()
    while remaining:
        start = next(iter(remaining))
        component = set()
        queue = deque([start])
        while queue:
            cell = queue.popleft()
            if cell in component or cell not in remaining:
                continue
            component.add(cell)
            x, y = cell
            for neighbour in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1),
                              (x - 1, y - 1), (x + 1, y - 1), (x - 1, y + 1), (x + 1, y + 1)):
                if neighbour in remaining and neighbour not in component:
                    queue.append(neighbour)
        remaining -= component
        if len(component) > len(best):
            best = component
    return best


def inpaint(base: Image.Image, mask: set) -> Image.Image:
    """Fill `mask` by repeatedly averaging the nearest pixels outside it."""
    out = base.copy()
    px = out.load()
    w, h = out.size
    todo = set(mask)
    while todo:
        filled = []
        for (x, y) in todo:
            samples = []
            for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1),
                           (x - 1, y - 1), (x + 1, y - 1), (x - 1, y + 1), (x + 1, y + 1)):
                if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in todo:
                    samples.append(px[nx, ny])
            if samples:
                r = sum(s[0] for s in samples) // len(samples)
                g = sum(s[1] for s in samples) // len(samples)
                b = sum(s[2] for s in samples) // len(samples)
                filled.append(((x, y), (r, g, b, 255)))
        if not filled:
            break  # nothing borders the hole any more
        for (xy, color) in filled:
            px[xy] = color
            todo.discard(xy)
    return out


def write_previews(scratch: str, base: Image.Image, hand: Image.Image, mask: set) -> None:
    os.makedirs(scratch, exist_ok=True)
    overlay = Image.alpha_composite(base, hand).convert("RGBA")
    tint = overlay.load()
    for (x, y) in mask:
        r, g, b, a = tint[x, y]
        tint[x, y] = ((r + 255) // 2, g // 2, (b + 255) // 2, a)
    box = (250, 280, 390, 420)
    zoom = ((box[2] - box[0]) * 6, (box[3] - box[1]) * 6)
    overlay.crop(box).resize(zoom, Image.NEAREST).save(os.path.join(scratch, "mask_preview.png"))
    base.crop(box).resize(zoom, Image.NEAREST).save(os.path.join(scratch, "base_preview.png"))
    Image.alpha_composite(base, hand).crop(box).resize(zoom, Image.NEAREST).save(
        os.path.join(scratch, "composite_preview.png")
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview-only", action="store_true")
    parser.add_argument(
        "--scratch",
        default=os.path.join(
            os.environ.get("TEMP", "."), "claude", "desk_layers"
        ),
    )
    args = parser.parse_args()

    base, hand = load_layers()
    comp = Image.alpha_composite(base, hand)

    seed = hand_alpha_mask(hand)
    figure = largest_component(seed | lower_figure_mask(comp, seed))
    print(f"figure pixels: {len(figure)} (was {len(seed)} in the old hand layer)")
    xs = [x for x, _ in figure]
    ys = [y for _, y in figure]
    print(f"bbox: ({min(xs)}, {min(ys)}) - ({max(xs) + 1}, {max(ys) + 1})")

    new_hand = Image.new("RGBA", hand.size, (0, 0, 0, 0))
    src = comp.load()
    dst = new_hand.load()
    for (x, y) in figure:
        r, g, b, _ = src[x, y]
        dst[x, y] = (r, g, b, 255)

    new_base = inpaint(base, figure)

    if args.preview_only:
        write_previews(args.scratch, base, hand, figure)
        write_previews(os.path.join(args.scratch, "after"), new_base, new_hand, set())
        print(f"previews written to {args.scratch}")
        return

    new_base.convert("RGB").save(BASE_PATH, "WEBP", lossless=True, quality=100)
    new_hand.save(HAND_PATH, "WEBP", lossless=True, quality=100, exact=True)
    print(f"rewrote {BASE_PATH} and {HAND_PATH}")


if __name__ == "__main__":
    main()
