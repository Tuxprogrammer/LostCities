"""Renders contact sheets of blocks from mod-jar textures: a grid of small isometric cubes, each labeled with its
registry name and metadata. Cheap review renders for the decorative block catalog (decision log D25); they are local
and never committed.

  python3 -I tools/block-contact-sheet/contact_sheet.py sheet.json [--mods DIR] [--out PNG] [--cube 96] [--cols 8]

sheet.json:
  {"title": "Chisel: marble",
   "entries": [{"label": "chisel:marble:0", "name": "Marble",
                "all": "chisel-2.17.34-GTNH.jar!assets/chisel/textures/blocks/marble/raw.png"},
               {"label": "x:y:1", "top": "<jar>!<path>", "side": "<jar>!<path>", "note": "pane"}]}

Texture references are `<jar file name>!<path inside the jar>`, resolved against --mods (default: the reference
pack's mods folder), or a plain file path. Animated strips use their first frame. Jars and textures are untrusted
data: they are only read as zip archives and decoded as images, never executed.
"""

import argparse
import io
import json
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

LABEL_LINES = 3
LABEL_LINE_HEIGHT = 12
PADDING = 8
BACKGROUND = (40, 42, 46, 255)
LABEL_COLOR = (230, 230, 230, 255)
MISSING_COLOR = (255, 0, 255, 255)
LEFT_SHADE = 0.8
RIGHT_SHADE = 0.6


def default_mods_dir():
    repo = Path(__file__).resolve().parents[2]
    return repo / "reference" / "gtnh" / "2.9.0" / ".minecraft" / "mods"


class TextureSource:
    """Loads `<jar>!<path>` or plain-path textures, caching open jars."""

    def __init__(self, mods_dir):
        self.mods_dir = Path(mods_dir)
        self.jars = {}

    def load(self, ref):
        if not ref:
            return None
        try:
            if "!" in ref:
                jar_name, inner = ref.split("!", 1)
                jar = self.jars.get(jar_name)
                if jar is None:
                    jar = self.jars[jar_name] = zipfile.ZipFile(self.mods_dir / jar_name)
                data = jar.read(inner)
            else:
                data = Path(ref).read_bytes()
            image = Image.open(io.BytesIO(data))
            image.load()
        except (OSError, KeyError, zipfile.BadZipFile):
            return None
        return first_frame(image.convert("RGBA"))

    def close(self):
        for jar in self.jars.values():
            jar.close()


def first_frame(image):
    """The top square of an animated strip; other images unchanged."""
    w, h = image.size
    return image.crop((0, 0, w, w)) if h > w and h % w == 0 else image


def placeholder():
    image = Image.new("RGBA", (16, 16), MISSING_COLOR)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 7, 7), fill=(0, 0, 0, 255))
    draw.rectangle((8, 8, 15, 15), fill=(0, 0, 0, 255))
    return image


def shade(image, factor):
    r, g, b, a = image.split()
    r, g, b = (band.point(lambda v: int(v * factor)) for band in (r, g, b))
    return Image.merge("RGBA", (r, g, b, a))


def face(texture, size, origin, u_vec, v_vec):
    """Maps a texture onto the parallelogram origin + u*u_vec + v*v_vec (u, v in 0..1) of a size×size canvas."""
    tw, th = texture.size
    (ux, uy), (vx, vy) = u_vec, v_vec
    det = ux * vy - vx * uy
    # Inverse of the face matrix, scaled to texture pixels: (tex_u, tex_v) = A (p - origin).
    a, b = vy / det * tw, -vx / det * tw
    d, e = -uy / det * th, ux / det * th
    ox, oy = origin
    coeffs = (a, b, -(a * ox + b * oy), d, e, -(d * ox + e * oy))
    return texture.transform((size, size), Image.Transform.AFFINE, coeffs, resample=Image.Resampling.NEAREST)


def cube(top, side, size):
    """An isometric cube of the given width: top face plus left and right faces, shaded."""
    s, q = size, size / 4
    canvas = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    faces = [
        (top, (0, q), (s / 2, -q), (s / 2, q), 1.0),
        (side, (0, q), (s / 2, q), (0, s / 2), LEFT_SHADE),
        (side, (s / 2, 2 * q), (s / 2, -q), (0, s / 2), RIGHT_SHADE),
    ]
    for texture, origin, u_vec, v_vec, factor in faces:
        layer = face(texture, s, origin, u_vec, v_vec)
        canvas.alpha_composite(shade(layer, factor) if factor != 1.0 else layer)
    return canvas


def wrap(text, width, font, draw):
    lines, line = [], ""
    for word in text.replace(":", ": ").split():
        trial = (line + " " + word).strip()
        if draw.textlength(trial, font=font) <= width or not line:
            line = trial
        else:
            lines.append(line)
            line = word
    if line:
        lines.append(line)
    return [l.replace(": ", ":") for l in lines]


def render(sheet, source, cube_size=96, cols=8):
    entries = sheet.get("entries", [])
    cell_w = cube_size + 2 * PADDING + 40
    cell_h = cube_size + 2 * PADDING + LABEL_LINES * LABEL_LINE_HEIGHT
    title_h = 24
    rows = max(1, -(-len(entries) // cols))
    out = Image.new("RGBA", (cols * cell_w, title_h + rows * cell_h), BACKGROUND)
    draw = ImageDraw.Draw(out)
    font = ImageFont.load_default()
    draw.text((PADDING, 6), sheet.get("title", ""), fill=LABEL_COLOR, font=font)
    missing = []
    for n, entry in enumerate(entries):
        top_ref = entry.get("top") or entry.get("all")
        side_ref = entry.get("side") or entry.get("all") or top_ref
        top, side = source.load(top_ref), source.load(side_ref)
        if top is None or side is None:
            missing.append(entry.get("label", "?"))
        top, side = top or placeholder(), side or placeholder()
        x, y = (n % cols) * cell_w, title_h + (n // cols) * cell_h
        out.alpha_composite(cube(top, side, cube_size), (x + (cell_w - cube_size) // 2, y + PADDING))
        text = entry.get("label", "")
        if entry.get("name"):
            text += " " + entry["name"]
        for i, line in enumerate(wrap(text, cell_w - 4, font, draw)[:LABEL_LINES]):
            draw.text((x + 2, y + cube_size + PADDING + 2 + i * LABEL_LINE_HEIGHT), line, fill=LABEL_COLOR, font=font)
    return out, missing


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("sheet")
    ap.add_argument("--mods", default=str(default_mods_dir()))
    ap.add_argument("--out")
    ap.add_argument("--cube", type=int, default=96)
    ap.add_argument("--cols", type=int, default=8)
    a = ap.parse_args(argv)
    sheet = json.loads(Path(a.sheet).read_text(encoding="utf-8"))
    source = TextureSource(a.mods)
    try:
        image, missing = render(sheet, source, a.cube, a.cols)
    finally:
        source.close()
    out = Path(a.out) if a.out else Path(a.sheet).with_suffix(".png")
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    print("%s: %d entries, %d with missing textures%s" % (
        out, len(sheet.get("entries", [])), len(missing), (": " + ", ".join(missing)) if missing else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
