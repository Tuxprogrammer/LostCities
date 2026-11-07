#!/usr/bin/env python3
"""Build the bundled asset tree from upstream's JSON at the pinned commit.

Upstream Lost Cities (MIT) ships its assets as data-pack JSON under
src/main/resources/data/lostcities/lostcities/<type>/<name>.json. This mod bundles the same files, file for file,
so a re-pin diffs against them (docs/new-spec/05-assets/asset-format.md, "Loading and override"; D14). The files
are copied byte for byte except for the documented conversions below, applied as exact text patches that must
each match, so a re-pin that moves a patched line fails loudly instead of shipping a half-converted file:

  * biome selectors take the 1.7.10 vocabulary (P05-6, P-PROF-4, D31, D32): #minecraft:is_ocean, is_river,
    is_beach and is_deep_ocean become type:OCEAN, type:RIVER, type:BEACH and type:OCEAN; standard's ocean
    multiplier also lists name:Kelp Forest and name:Coral Reef; the cabin's and radio tower's land filters exclude
    those two as well; the desert city style takes type:SANDY excluding type:OCEAN, plus rwg:canyon,
    rwg:canyon_forest, rwg:oasis and rwg:red_oasis;
  * highway tunnels are always lit (D-tunnel-light-1): the four upstream tunnel parts are copied unchanged, and this
    mod adds nh_highway_tunnel, nh_highway_tunnel_bi, nh_highway_tunnel_bend and nh_highway_tunnel_t, the same
    parts with the ceiling blocks at x and z in {2, 6, 9, 13} (slice 5) turned into 'h', common's glowstone, which
    places whatever generateLighting says; both bundled world styles name them in a 'parts' block;
  * #forge:stone in block matchers becomes ore:stone (D19);
  * bricks_desert_redsand's 1.12 'minecraft:red_sandstone@2' becomes its 1.20 name, minecraft:cut_red_sandstone
    (ASSET-03);
  * the desert palettes get a '}' rubble character (P05-5, D31; fixes ASSET-20) from two new variants this mod adds,
    sandstone_rubble and red_sandstone_rubble;
  * the desert city style is muted (AR-16): its building palettes are bricks_desert and three palettes this mod adds,
    bricks_desert_rough, bricks_desert_bleached and bricks_desert_taupe, instead of the saturated bricks_desert_red
    and bricks_desert_orange (those two files stay bundled, unused); bricks_desert's '$' band and default_desert's
    'S' (floors, park borders) become smooth sandstone instead of orange red sandstone, and default_desert's 'B'
    street variant becomes sandstone instead of red nether bricks.

It also writes this mod's original buildings for the low-density rings (city-realism Phase 2d), drawn in
originals.py: the nh_* buildings and parts and the nh_residential palette.

Block names stay 1.20 names: the block-name map (data/lostcitiesnh/blockmap/mapping.json) turns them into 1.7.10
blocks at load. The script also writes data/lostcitiesnh/assets.index, the list of bundled files the mod reads
from the jar.

Usage, from the repository root:
    python3 -I tools/asset-convert/convert.py [--ref 1.20] [--out src/main/resources]
"""

import argparse
import importlib.util
import json
import os
import re
import subprocess
import sys

UPSTREAM_ROOT = "src/main/resources/data/lostcities/lostcities"
BUNDLED = "data/lostcities/lostcities"
INDEX = "data/lostcitiesnh/assets.index"

DESERT_BIOMES_OLD = """        "if_any": [
          "minecraft:desert",
          "minecraft:badlands"
        ]"""
DESERT_BIOMES_NEW = """        "if_any": [
          "type:SANDY",
          "rwg:canyon",
          "rwg:canyon_forest",
          "rwg:oasis",
          "rwg:red_oasis"
        ],
        "excluding": [
          "type:OCEAN"
        ]"""

OCEAN_MULTIPLIER_OLD = """      "multiplier": 0.1,
      "biomes": {
        "if_any": [
          "#minecraft:is_ocean"
        ]"""
OCEAN_MULTIPLIER_NEW = """      "multiplier": 0.1,
      "biomes": {
        "if_any": [
          "#minecraft:is_ocean",
          "name:Kelp Forest",
          "name:Coral Reef"
        ]"""

TAGS = [
    ('"#minecraft:is_deep_ocean"', '"type:OCEAN"'),
    ('"#minecraft:is_ocean"', '"type:OCEAN"'),
    ('"#minecraft:is_river"', '"type:RIVER"'),
    ('"#minecraft:is_beach"', '"type:BEACH"'),
]

DESERT_PALETTES_OLD = """      {
        "factor": 1.0,
        "palette": "bricks_desert_red"
      },
      {
        "factor": 1.0,
        "palette": "bricks_desert_orange"
      }"""
# AR-16: sandstone keeps the largest share; the bleached and taupe walls are the pale and earthy variety.
DESERT_PALETTES_NEW = """      {
        "factor": 0.6,
        "palette": "bricks_desert_rough"
      },
      {
        "factor": 0.8,
        "palette": "bricks_desert_bleached"
      },
      {
        "factor": 0.5,
        "palette": "bricks_desert_taupe"
      }"""

RED_SANDSTONE_DOUBLE = '"block": "minecraft:red_sandstone_slab[type=double]"'
SMOOTH_SANDSTONE = '"block": "minecraft:smooth_sandstone"'

RUBBLE_ENTRY = """,
    {{
      "char": "}}",
      "variant": "{variant}",
      "damaged": "minecraft:iron_bars"
    }}
  ]
}}"""



def desert_palette(wall, trim, band):
    """A muted desert building palette (AR-16): '#' the wall, 'X' the trim, '$' the band, '}' sandstone rubble."""
    return f"""{{
  "palette": [
    {{
      "char": "X",
      "block": "{trim}",
      "damaged": "minecraft:iron_bars"
    }},
    {{
      "char": "$",
      "block": "{band}",
      "damaged": "minecraft:iron_bars"
    }},
    {{
      "char": "#",
      "blocks": [
{wall}
      ],
      "damaged": "minecraft:iron_bars"
    }},
    {{
      "char": "}}",
      "variant": "sandstone_rubble",
      "damaged": "minecraft:iron_bars"
    }}
  ]
}}
"""


def weighted(*pairs):
    """The rows of a palette entry's 'blocks' list, each (random, block), as upstream writes them."""
    rows = []
    for weight, block in pairs:
        rows.append(f"""        {{
          "random": {weight},
          "block": "{block}"
        }}""")
    return ",\n".join(rows)


ADDED = {
    # Shaped like upstream's stonebrick_rubble: about 70 % full blocks, the rest slabs and stairs (P05-5, D31).
    "variants/sandstone_rubble.json": """{
  "blocks": [
    {
      "random": 17,
      "block": "minecraft:sandstone"
    },
    {
      "random": 5,
      "block": "minecraft:sandstone_stairs"
    },
    {
      "random": 15,
      "block": "minecraft:sandstone_slab"
    },
    {
      "random": 1000,
      "block": "minecraft:cut_sandstone"
    }
  ]
}
""",
    # The red desert palette's set: Et Futurum's red sandstone and its slabs and stairs, vanilla sandstone without it.
    "variants/red_sandstone_rubble.json": """{
  "blocks": [
    {
      "random": 17,
      "block": "minecraft:red_sandstone"
    },
    {
      "random": 5,
      "block": "minecraft:red_sandstone_stairs"
    },
    {
      "random": 15,
      "block": "minecraft:red_sandstone_slab"
    },
    {
      "random": 1000,
      "block": "minecraft:cut_red_sandstone"
    }
  ]
}
""",
    # AR-16: layered sandstone with chiseled trim, the rough face of the sandstone family.
    "palettes/bricks_desert_rough.json": desert_palette(
        weighted((40, "minecraft:cut_sandstone"), (2000, "minecraft:sandstone")),
        "minecraft:chiseled_sandstone",
        "minecraft:cut_sandstone"),
    # AR-16: bleached walls, white terracotta's pale beige, with sandstone trim.
    "palettes/bricks_desert_bleached.json": desert_palette(
        weighted((2000, "minecraft:white_terracotta")),
        "minecraft:cut_sandstone",
        "minecraft:smooth_sandstone"),
    # AR-16: the earthy one, light gray terracotta's muted taupe, with sandstone trim; never a saturated color.
    "palettes/bricks_desert_taupe.json": desert_palette(
        weighted((2000, "minecraft:light_gray_terracotta")),
        "minecraft:cut_sandstone",
        "minecraft:smooth_sandstone"),
}


TUNNEL_PARTS = ("highway_tunnel", "highway_tunnel_bi", "highway_tunnel_bend", "highway_tunnel_t")
TUNNEL_LAMP_CHAR = "h"
TUNNEL_LAMP_AXIS = (2, 6, 9, 13)
TUNNEL_CEILING_SLICE = 5
TUNNEL_SLOTS = ("tunnel", "tunnel_bi", "tunnel_bend", "tunnel_t")
WORLDSTYLE_PARTS_ANCHOR = '  "highwaysupport": "v",\n'
WORLDSTYLE_PARTS = WORLDSTYLE_PARTS_ANCHOR + '  "parts": {\n    "highways": {\n' + ",\n".join(
    f'      "{slot}": "nh_highway_{slot}"' for slot in TUNNEL_SLOTS) + "\n    }\n  },\n"


def lit_tunnel(text, path):
    """Returns an upstream tunnel part with a ceiling lamp at every (x, z) of the 4 x 4 lamp lattice."""
    data = json.loads(text)
    for z in TUNNEL_LAMP_AXIS:
        for x in TUNNEL_LAMP_AXIS:
            row = data["slices"][TUNNEL_CEILING_SLICE][z]
            if row[x] != "v":
                continue  # not ceiling here
            data["slices"][TUNNEL_CEILING_SLICE][z] = row[:x] + TUNNEL_LAMP_CHAR + row[x + 1:]
    return json.dumps(data, indent=2) + "\n"


def git(repo, *args):
    return subprocess.run(["git", "-C", repo] + list(args), check=True, capture_output=True).stdout


def replace(text, old, new, path, count=None):
    found = text.count(old)
    if found == 0 or (count is not None and found != count):
        sys.exit(f"{path}: expected {count or 'some'} of {old!r}, found {found}; re-check the patch for this pin")
    return text.replace(old, new)


def add_rubble(text, variant, path):
    patched, n = re.subn(r"\n    }\n  ]\n}\s*$", "\n    }" + RUBBLE_ENTRY.format(variant=variant) + "\n", text)
    if n != 1:
        sys.exit(f"{path}: cannot find the end of the palette list")
    return patched


def land_only(text, name, path):
    pattern = re.compile(r'("name": "' + name + r'",(?:(?!\n      \}).)*?"#minecraft:is_beach")', re.S)
    patched, n = pattern.subn(r'\1,\n            "name:Kelp Forest",\n            "name:Coral Reef"', text)
    if n != 1:
        sys.exit(f"{path}: cannot find the {name} entry's land filter")
    return patched


def convert(path, text):
    if path == "palettes/bricks_desert_redsand.json":
        text = replace(text, "minecraft:red_sandstone@2", "minecraft:cut_red_sandstone", path, 2)
    if path in ("palettes/bricks_desert.json", "palettes/bricks_desert_orange.json", "palettes/default_desert.json"):
        text = add_rubble(text, "sandstone_rubble", path)
    if path == "palettes/bricks_desert_red.json":
        text = add_rubble(text, "red_sandstone_rubble", path)
    if path in ("palettes/bricks_desert.json", "palettes/default_desert.json"):
        text = replace(text, RED_SANDSTONE_DOUBLE, SMOOTH_SANDSTONE, path, 1)
    if path == "palettes/default_desert.json":
        text = replace(text, '"block": "minecraft:red_nether_bricks"', '"block": "minecraft:sandstone"', path, 1)
    if path == "styles/desert.json":
        text = replace(text, DESERT_PALETTES_OLD, DESERT_PALETTES_NEW, path, 1)
    if path.startswith("worldstyles/"):
        text = replace(text, DESERT_BIOMES_OLD, DESERT_BIOMES_NEW, path, 1)
        if path == "worldstyles/standard.json":
            text = replace(text, OCEAN_MULTIPLIER_OLD, OCEAN_MULTIPLIER_NEW, path, 1)
        text = replace(text, WORLDSTYLE_PARTS_ANCHOR, WORLDSTYLE_PARTS, path, 1)
        for name in ("radiotower", "cabin"):
            text = land_only(text, name, path)
    if path.startswith("worldstyles/") or path.startswith("stuff/"):
        for old, new in TAGS:
            if old in text:
                text = text.replace(old, new)
    if path.startswith("stuff/"):
        if '"#forge:stone"' in text:
            text = text.replace('"#forge:stone"', '"ore:stone"')
    return text


def load_originals():
    """Loads originals.py beside this script (run with -I, so the script's folder is not on the path)."""
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "originals.py")
    spec = importlib.util.spec_from_file_location("lcnh_originals", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--ref", default="1.20")
    parser.add_argument("--out", default="src/main/resources")
    parser.add_argument("--repo", default=".")
    args = parser.parse_args()
    names = git(args.repo, "ls-tree", "-r", "--name-only", args.ref, UPSTREAM_ROOT).decode().split()
    written = []
    for name in sorted(names):
        rel = name[len(UPSTREAM_ROOT) + 1:]
        if not rel.endswith(".json"):
            continue
        text = git(args.repo, "show", f"{args.ref}:{name}").decode("utf-8")
        target = os.path.join(args.out, BUNDLED, rel)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8", newline="") as out:
            out.write(convert(rel, text))
        written.append(rel)
        if rel.startswith("parts/") and rel[len("parts/"):-len(".json")] in TUNNEL_PARTS:
            lit = os.path.join(args.out, BUNDLED, "parts", "nh_" + rel[len("parts/"):])
            with open(lit, "w", encoding="utf-8", newline="") as out:
                out.write(lit_tunnel(text, rel))
            written.append("parts/nh_" + rel[len("parts/"):])
    added = dict(ADDED)
    added.update(load_originals().original_assets())
    for rel, text in added.items():
        target = os.path.join(args.out, BUNDLED, rel)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8", newline="") as out:
            out.write(text)
        written.append(rel)
    index = os.path.join(args.out, INDEX)
    os.makedirs(os.path.dirname(index), exist_ok=True)
    with open(index, "w", encoding="utf-8", newline="") as out:
        out.write("# Every bundled asset file, relative to data/. Written by tools/asset-convert/convert.py.\n")
        for rel in sorted(written):
            out.write(f"lostcities/lostcities/{rel}\n")
    print(f"{len(written)} files ({len(ADDED)} added) under {os.path.join(args.out, BUNDLED)}; index {index}")


if __name__ == "__main__":
    main()
