#!/usr/bin/env python3
"""Write data/lostcitiesnh/blockmap/variety.json, the palette variety drawn from the block catalog's pools.

The block catalog (docs/new-spec/05-assets/block-catalog/index.md, "Variety pools"; D25, D28, D29) lists, per
upstream palette role, the pack's candidate blocks with weights. This script turns the candidates into option
palettes, one per candidate, and lists per style the groups a building draws one option from
(docs/new-spec/05-assets/asset-format.md, "Palette variety"). Every block is named as the catalog names it,
'modid:name:meta', or as the 1.20 name the block-name map already resolves; the asset tests check that every block
here is catalogued, and the real-pack resolution scenario that every one is registered.

Rules the content follows (D28, D29, AR-16):
  * each group keeps the baseline ('none', upstream's look) as its largest share;
  * no speed block (D28 rule 3) anywhere: walls are also roofs and ruins;
  * no tile-entity block that needs data to look right (D28 rule 6): the options name plain blocks, and the few
    containers (smoker, blast furnace) get their tile entity from the block itself;
  * interior floors keep at least 70 of 100 buildings on spawnable floor (D28 rule 1);
  * no decorative metal (D29): metal accents wait for the capped accent pass;
  * the desert stays muted (AR-16): pale, cream, tan and taupe only, no saturated red or orange.

Usage, from the repository root:
    python3 -I tools/asset-convert/variety.py [--out src/main/resources/data/lostcitiesnh/blockmap/variety.json]
"""

import argparse
import json

OUT = "src/main/resources/data/lostcitiesnh/blockmap/variety.json"

# The interior and park characters (PaletteVariety): no upstream part uses them.
FLOOR = ""
CEILING = ""
LIGHT = ""
FLORA = {
    "temperate": "",
    "arid": "",
    "cold": "",
    "jungle": "",
    "swamp": "",
}

DAMAGED = "minecraft:iron_bars"

OPTIONS = []
OPTION_NAMES = set()


def option(name, pool, *entries):
    """Adds an option palette and returns its name."""
    if name in OPTION_NAMES:
        raise SystemExit(f"option {name} defined twice")
    OPTION_NAMES.add(name)
    OPTIONS.append({"name": name, "pool": pool, "palette": list(entries)})
    return name


def block(char, ref, damaged=None):
    entry = {"char": char, "block": ref}
    if damaged:
        entry["damaged"] = damaged
    return entry


def mix(char, rows, fill, damaged=None):
    """A 128-slot list: each (slots, block) in order, then 'fill' for the rest, as upstream writes lists."""
    blocks = [{"random": n, "block": ref} for n, ref in rows]
    blocks.append({"random": 1000, "block": fill})
    entry = {"char": char, "blocks": blocks}
    if damaged:
        entry["damaged"] = damaged
    return entry


def variant(char, name, damaged=None):
    entry = {"char": char, "variant": name}
    if damaged:
        entry["damaged"] = damaged
    return entry


def group(name, options, when=None, scope="building"):
    """A group: (option or 'none', weight) pairs; 'when' a palette the style must have drawn."""
    out = {"name": name}
    if when:
        out["when"] = when
    out["for"] = scope
    out["options"] = [{"option": o, "weight": w} for o, w in options]
    return out


# --------------------------------------------------------------------------- walls

def rubble(base, cracked=None, slab="minecraft:stone_slab:5", stairs="minecraft:stone_brick_stairs:0"):
    """A '}' rubble set shaped like upstream's stonebrick_rubble: mostly full blocks, some slabs and stairs."""
    rows = []
    if cracked:
        rows.append((17, cracked))
    if slab:
        rows.append((15, slab))
    if stairs:
        rows.append((5, stairs))
    return mix("}", rows, base, DAMAGED)


def wall(name, pool, wall_entry, rubble_entry):
    return option(name, pool, wall_entry, rubble_entry)


STONEBRICK_WALLS = [
    (wall("wall_stonebrick_chisel", "stone-brick-main-wall",
          mix("#", [(9, "chisel:stonebricksmooth:6"), (8, "chisel:stonebricksmooth:7"),
                    (40, "chisel:stonebricksmooth:5")], "chisel:stonebricksmooth:4", DAMAGED),
          mix("}", [(17, "chisel:stonebricksmooth:6"), (10, "chisel:cobblestone:9"),
                    (15, "minecraft:stone_slab:5"), (5, "minecraft:stone_brick_stairs:0")],
              "chisel:stonebricksmooth:7", DAMAGED)), 8),
    (wall("wall_stonebrick_quarried", "stone-brick-main-wall",
          mix("#", [(24, "Railcraft:brick.quarried:1")], "Railcraft:brick.quarried:0", DAMAGED),
          rubble("Railcraft:brick.quarried:0", "Railcraft:brick.quarried:5")), 8),
    (wall("wall_stonebrick_deepslate", "stone-brick-main-wall",
          mix("#", [(9, "etfuturum:deepslate_bricks:1")], "etfuturum:deepslate_bricks:0", DAMAGED),
          rubble("etfuturum:deepslate_bricks:1", "etfuturum:cobbled_deepslate:0",
                 "etfuturum:deepslate_brick_slab:0", "etfuturum:deepslate_brick_stairs:0")), 6),
    (wall("wall_stonebrick_andesite", "stone-brick-main-wall",
          block("#", "Botania:stone:8", DAMAGED),
          rubble("Botania:stone:8", "Botania:stone:0", "Botania:stone8Slab:0", "Botania:stone8Stairs:0")), 3),
    (wall("wall_stonebrick_diorite", "stone-brick-main-wall",
          block("#", "Botania:stone:10", DAMAGED),
          rubble("Botania:stone:10", "Botania:stone:2")), 3),
    (wall("wall_stonebrick_tuff", "stone-brick-main-wall",
          block("#", "etfuturum:tuff:2", DAMAGED),
          rubble("etfuturum:tuff:2")), 4),
    (wall("wall_stonebrick_polished", "stone-brick-main-wall",
          block("#", "TConstruct:decoration.multibrick:3", DAMAGED),
          rubble("TConstruct:decoration.multibrick:3", "minecraft:stonebrick:2")), 4),
    (wall("wall_stonebrick_arcane", "stone-brick-main-wall",
          block("#", "Thaumcraft:blockCosmeticSolid:7", DAMAGED),
          rubble("Thaumcraft:blockCosmeticSolid:7", "minecraft:stonebrick:2")), 4),
    (wall("wall_stonebrick_limestone", "stone-brick-main-wall",
          block("#", "chisel:limestone:5", DAMAGED),
          rubble("chisel:limestone:5", "chisel:limestone:6")), 2),
    (wall("wall_stonebrick_marble", "stone-brick-main-wall",
          block("#", "chisel:marble:1", DAMAGED),
          rubble("chisel:marble:1", "chisel:marble:10")), 2),
    (wall("wall_stonebrick_gt_basalt", "stone-brick-main-wall",
          mix("#", [(9, "gregtech:gt.blockstones:12")], "gregtech:gt.blockstones:11", DAMAGED),
          rubble("gregtech:gt.blockstones:12", "gregtech:gt.blockstones:9")), 2),
    (wall("wall_stonebrick_gt_marble", "stone-brick-main-wall",
          mix("#", [(9, "gregtech:gt.blockstones:4")], "gregtech:gt.blockstones:3", DAMAGED),
          rubble("gregtech:gt.blockstones:4", "gregtech:gt.blockstones:1")), 2),
    (wall("wall_stonebrick_edged", "stone-brick-main-wall",
          block("#", "ExtraUtilities:decorativeBlock1:0", DAMAGED),
          rubble("ExtraUtilities:decorativeBlock1:0", "minecraft:stonebrick:2")), 3),
    (wall("wall_stonebrick_ztyl", "stone-brick-main-wall",
          block("#", "Ztones:tile.ztylBlock:0", DAMAGED),
          rubble("Ztones:tile.ztylBlock:0", "minecraft:stonebrick:2")), 2),
    (wall("wall_stonebrick_basalt", "stone-brick-main-wall",
          block("#", "ProjRed|Exploration:projectred.exploration.stone:4", DAMAGED),
          rubble("ProjRed|Exploration:projectred.exploration.stone:4", "etfuturum:cobbled_deepslate:0")), 2),
]


def single_accent(prefix, pool, char, refs):
    """One option per block for a single-character role ('X' accent, '$' band, 'Q' quartz)."""
    out = []
    for ref, weight in refs:
        slug = ref.replace("|", "_").replace(":", "_").replace(".", "_").lower()
        out.append((option(f"{prefix}_{slug}", pool, block(char, ref, DAMAGED)), weight))
    return out


STONEBRICK_ACCENTS = single_accent("accent", "accent", "X", [
    ("minecraft:stonebrick:3", 10),
    ("chisel:stonebricksmooth:9", 2), ("chisel:stonebricksmooth:10", 2),
    ("chisel:stonebricksmooth:13", 2), ("chisel:stonebricksmooth:14", 2),
    ("Railcraft:brick.quarried:3", 4), ("Railcraft:brick.abyssal:2", 2),
    ("etfuturum:smooth_stone:0", 6),
    ("etfuturum:deepslate_bricks:4", 3),
    ("Botania:stone:12", 3),
    ("chisel:marble:2", 3), ("chisel:limestone:11", 2),
    ("ThaumicTinkerer:darkQuartz:1", 2),
    ("Ztones:tile.zorgBlock:0", 2),
    ("chisel:nucrete:1", 2),
    ("TConstruct:decoration.multibrickfancy:3", 3),
])

STONEBRICK_BANDS = single_accent("band", "band", "$", [
    ("etfuturum:smooth_stone:0", 10),
    ("chisel:stonebricksmooth:12", 8),
    ("chisel:nucrete:3", 3), ("chisel:nucrete:11", 3),
    ("etfuturum:concrete:7", 3), ("etfuturum:concrete:8", 3),
    ("minecraft:double_stone_slab:5", 5),
    ("GalacticraftCore:slabGCDouble:2", 4),
    ("Railcraft:brick.abyssal:2", 4),
    ("chisel:limestone:7", 4),
    ("gregtech:gt.blockstones:7", 4),
    ("ThaumicTinkerer:darkQuartzSlabFull:0", 3),
    ("Ztones:tile.ztylBlock:5", 3),
])


def terracotta_walls(prefix, pool, refs):
    """Terracotta palettes use one block for '#', 'X' and '}' (upstream); each option keeps that."""
    out = []
    for ref, weight in refs:
        slug = ref.replace(":", "_").replace(".", "_").lower()
        out.append((option(f"{prefix}_{slug}", pool, block("#", ref, DAMAGED), block("X", ref, DAMAGED),
                           block("}", ref, DAMAGED)), weight))
    return out


CYAN_WALLS = terracotta_walls("wall_cyan", "cyan-terracotta-walls", [
    ("etfuturum:concrete:9", 12),
    ("chisel:hardened_clay_cyan:2", 3), ("chisel:hardened_clay_cyan:4", 3),
    ("chisel:hardened_clay_cyan:6", 3), ("chisel:hardened_clay_cyan:8", 3),
    ("chisel:hempcrete:9", 8),
    ("Ztones:tile.agonBlock:5", 3), ("Ztones:tile.mystBlock:2", 3), ("Ztones:tile.crayBlock:5", 2),
    ("ExtraUtilities:color_stone:9", 5),
    ("OpenComputers:chameliumBlock:6", 4),
])

GRAY_WALLS = terracotta_walls("wall_gray", "gray-terracotta-walls", [
    ("etfuturum:concrete:7", 12),
    ("chisel:hardened_clay_gray:2", 3), ("chisel:hardened_clay_gray:4", 3),
    ("chisel:hardened_clay_gray:6", 2), ("chisel:hardened_clay_gray:8", 2),
    ("chisel:hempcrete:7", 8),
    ("Ztones:tile.agonBlock:14", 2), ("Ztones:tile.iszmBlock:0", 2),
    ("Ztones:tile.mystBlock:10", 2), ("Ztones:tile.fortBlock:9", 2),
    ("chisel:nucrete:0", 2), ("chisel:nucrete:2", 2), ("chisel:nucrete:5", 1),
    ("ExtraUtilities:color_stone:7", 3), ("ExtraUtilities:colorStoneBrick:7", 2),
    ("GalaxySpace:phobosblocks:0", 4),
])

LIGHT_GRAY_WALLS = terracotta_walls("wall_lightgray", "light-gray-terracotta-walls", [
    ("etfuturum:concrete:8", 12),
    ("chisel:hardened_clay_lightgray:2", 3), ("chisel:hardened_clay_lightgray:4", 3),
    ("chisel:hardened_clay_lightgray:6", 2), ("chisel:hardened_clay_lightgray:8", 2),
    ("chisel:hempcrete:8", 8),
    ("Ztones:tile.agonBlock:15", 2), ("Ztones:tile.iszmBlock:1", 2),
    ("Ztones:tile.mystBlock:15", 2), ("Ztones:tile.crayBlock:0", 2),
    ("chisel:nucrete:8", 2), ("chisel:nucrete:10", 2), ("chisel:nucrete:13", 1),
    ("GalaxySpace:tritonblocks:0", 4),
    ("ExtraUtilities:color_stone:8", 4),
    ("OpenComputers:chameliumBlock:7", 4),
])

BLACK_BANDS = single_accent("band_black", "terracotta-bands", "$", [
    ("etfuturum:concrete:15", 15),
    ("chisel:hempcrete:15", 10),
    ("Ztones:tile.agonBlock:13", 5), ("Ztones:tile.zestBlock:15", 5),
    ("Botania:quartzTypeDark:0", 5), ("ThaumicTinkerer:darkQuartz:0", 5),
])

BROWN_BANDS = single_accent("band_brown", "terracotta-bands", "$", [
    ("etfuturum:concrete:12", 15),
    ("chisel:hempcrete:12", 10),
    ("Ztones:tile.krypBlock:4", 10),
    ("GalaxySpace:oberonblocks:0", 5), ("GalaxySpace:mirandablocks:0", 5),
])

QUARTZ = single_accent("quartz", "quartz-block-and-column", "Q", [
    ("etfuturum:smooth_quartz:0", 10),
    ("Botania:quartzTypeSunny:0", 4), ("Botania:quartzTypeMana:0", 3),
    ("appliedenergistics2:tile.BlockQuartz:0", 5),
    ("etfuturum:calcite:0", 5),
    ("chisel:marble:0", 4),
])

# --------------------------------------------------------------------------- desert (AR-16: muted)

SANDSTONE_RUBBLE = "sandstone_rubble"


def desert_wall(name, pool, wall_entry):
    return option(name, pool, wall_entry, variant("}", SANDSTONE_RUBBLE, DAMAGED))


def desert_walls(prefix, pool, refs):
    out = []
    for ref, weight in refs:
        slug = ref.replace(":", "_").replace(".", "_").lower()
        out.append((desert_wall(f"{prefix}_{slug}", pool, block("#", ref, DAMAGED)), weight))
    return out


DESERT_WALLS = desert_walls("wall_desert", "desert-main-wall", [
    ("chisel:sandstone:8", 2), ("chisel:sandstone:12", 2), ("chisel:sandstone:13", 2),
    ("chisel:sandstone:15", 2), ("chisel:sandstone2:0", 1), ("chisel:sandstone2:1", 1),
    ("Railcraft:brick.sandy:0", 5), ("Railcraft:brick.sandy:1", 5),
    ("TConstruct:decoration.multibrick:1", 4), ("TConstruct:decoration.multibrickfancy:1", 4),
    ("Ztones:tile.roenBlock:0", 2), ("Ztones:tile.roenBlock:7", 2),
    ("Ztones:tile.solsBlock:3", 2), ("Ztones:tile.solsBlock:4", 2),
    ("etfuturum:end_bricks:0", 5),
    ("Thaumcraft:blockCosmeticSolid:11", 5),
    ("TConstruct:decoration.multibrick:9", 2), ("Railcraft:brick.bleachedbone:0", 2),
])

ROUGH_WALLS = desert_walls("wall_desert_rough", "desert-main-wall", [
    ("Railcraft:brick.sandy:1", 8),
    ("TConstruct:decoration.multibrick:1", 8),
    ("chisel:sandstone2:0", 6),
    ("chisel:sandstone:13", 4),
    ("Thaumcraft:blockCosmeticSolid:11", 6),
])

BLEACHED_WALLS = desert_walls("wall_bleached", "bleached-desert-walls", [
    ("chisel:hardened_clay_white:2", 4), ("chisel:hardened_clay_white:4", 4),
    ("chisel:hardened_clay_white:6", 4), ("chisel:hardened_clay_white:8", 3),
    ("Ztones:tile.roenBlock:0", 4), ("Ztones:tile.roenBlock:7", 4), ("Ztones:tile.solsBlock:3", 4),
    ("TConstruct:decoration.multibrick:9", 5), ("Railcraft:brick.bleachedbone:0", 5),
    ("etfuturum:end_bricks:0", 8),
    ("Ztones:tile.krypBlock:5", 5),
    ("Botania:biomeStoneA:5", 5),
])

# No Chisel light gray terracotta bricks here: in 1.7.10 light gray terracotta has vanilla brick's color (CIE76 under
# 10 from minecraft:brick_block), so its brick patterns read as red brick in a desert city (beta audit, D-BA-6).
TAUPE_WALLS = desert_walls("wall_taupe", "taupe-desert-walls", [
    ("Ztones:tile.krypBlock:3", 10),
    ("chisel:hempcrete:8", 8),
    ("Ztones:tile.mystBlock:15", 4), ("Ztones:tile.crayBlock:0", 4),
    ("Thaumcraft:blockCosmeticSolid:11", 6),
])

DESERT_ACCENTS = single_accent("accent_desert", "desert-accent-and-trim", "X", [
    ("minecraft:sandstone:1", 12),
    ("chisel:sandstone:4", 3), ("chisel:sandstone:5", 2), ("chisel:sandstone:6", 2),
    ("chisel:sandstone:7", 2), ("chisel:sandstone:9", 3),
    ("Railcraft:brick.sandy:3", 8),
    ("Ztones:tile.solsBlock:4", 4), ("Ztones:tile.roenBlock:9", 4),
    ("Botania:biomeStoneA:5", 6),
    ("Ztones:tile.krypBlock:5", 4),
])

DESERT_BANDS = single_accent("band_desert", "desert-band", "$", [
    ("minecraft:sandstone:2", 15),
    ("minecraft:sandstone:1", 10),
    ("Railcraft:brick.sandy:2", 10),
    ("Ztones:tile.krypBlock:3", 8),
    ("Ztones:tile.solsBlock:4", 7),
])

# --------------------------------------------------------------------------- glass

# Stained metadata of the four upstream tints, and each candidate's metadata for them.
TINTS = {
    "white": {"dye": 0, "gt": 0, "future": 15},
    "gray": {"dye": 7, "gt": 2, "future": 8},
    "blue": {"dye": 11, "gt": 12, "future": 4},
    "light_blue": {"dye": 3, "gt": 11, "future": 12},
}


def glass_option(name, pool, mode, glass, pane):
    """A glass option: 'full' palettes have glass in '+' and 'a', a pane in '`'; 'pane' palettes panes in 'a'."""
    a = glass if mode == "full" else pane
    return option(name, pool, block("+", glass), block("`", pane), block("a", a))


def clear_glass(mode):
    pane = "minecraft:glass_pane:0"
    glass = "minecraft:glass:0"
    out = []
    for meta, weight in ((10, 3), (11, 3), (13, 3), (14, 3)):
        out.append((glass_option(f"glass_{mode}_chisel_{meta}", "clear-glass-blocks", mode,
                                 f"chisel:glass:{meta}", pane), weight))
    out.append((glass_option(f"glass_{mode}_chisel_borderless", "clear-glass-blocks", mode,
                             "chisel:glass:6", "chisel:glass_pane:2"), 3))
    out.append((glass_option(f"glass_{mode}_strengthened", "clear-glass-blocks", mode,
                             "Railcraft:glass:0", pane), 8))
    out.append((glass_option(f"glass_{mode}_tinkers", "clear-glass-blocks", mode,
                             "TConstruct:GlassBlock:0", "TConstruct:GlassPane:0"), 8))
    out.append((glass_option(f"glass_{mode}_quartz", "clear-glass-blocks", mode,
                             "appliedenergistics2:tile.BlockQuartzGlass:0", pane), 6))
    out.append((glass_option(f"glass_{mode}_thickened", "clear-glass-blocks", mode,
                             "ExtraUtilities:decorativeBlock2:0", pane), 3))
    out.append((glass_option(f"glass_{mode}_edged", "clear-glass-blocks", mode,
                             "ExtraUtilities:decorativeBlock2:1", pane), 3))
    out.append((glass_option(f"glass_{mode}_future", "clear-glass-blocks", mode,
                             "GalaxySpace:futureglass:0", pane), 5))
    out.append((glass_option(f"glass_{mode}_borosilicate", "clear-glass-blocks", mode,
                             "bartworks:BW_TieredGlass:0", pane), 4))
    for meta in (1, 2, 4):
        out.append((glass_option(f"glass_{mode}_pane_chisel_{meta}", "clear-panes", mode, glass,
                                 f"chisel:glass_pane:{meta}"), 4))
    return out


def tinted_glass(mode, tint):
    m = TINTS[tint]
    pane = f"minecraft:stained_glass_pane:{m['dye']}"
    glass = f"minecraft:stained_glass:{m['dye']}"
    out = [
        (glass_option(f"glass_{mode}_{tint}_strengthened", "tinted-glass-blocks", mode,
                      f"Railcraft:glass:{m['dye']}", pane), 8),
        (glass_option(f"glass_{mode}_{tint}_tinkers", "tinted-glass-blocks", mode,
                      f"TConstruct:GlassBlock.StainedClear:{m['dye']}",
                      f"TConstruct:GlassPaneClearStained:{m['dye']}"), 7),
        (glass_option(f"glass_{mode}_{tint}_gregtech", "tinted-glass-blocks", mode,
                      f"gregtech:gt.blocktintedglass:{m['gt']}", pane), 4),
        (glass_option(f"glass_{mode}_{tint}_future", "tinted-glass-blocks", mode,
                      f"GalaxySpace:futureglasses:{m['future']}", pane), 6),
        (glass_option(f"glass_{mode}_{tint}_shaded", "tinted-glass-blocks", mode,
                      f"witchery:shadedglass:{m['dye']}", pane), 5),
        (glass_option(f"glass_{mode}_{tint}_pane_tinkers", "tinted-panes", mode, glass,
                      f"TConstruct:GlassPaneClearStained:{m['dye']}"), 6),
    ]
    if tint == "gray":
        out.append((glass_option(f"glass_{mode}_gray_glaxx", "tinted-glass-blocks", mode,
                                 "Ztones:tile.glaxx:0", pane), 3))
    return out


def glass_groups():
    groups = []
    for mode in ("full", "pane"):
        groups.append(group(f"glass_{mode}", [("none", 40)] + clear_glass(mode), f"glass_{mode}"))
        for tint in TINTS:
            groups.append(group(f"glass_{mode}_{tint}", [("none", 40)] + tinted_glass(mode, tint),
                                f"glass_{mode}_{tint}"))
    return groups


# --------------------------------------------------------------------------- interiors (AR-3)

def singles(prefix, pool, char, refs):
    out = []
    for ref, weight in refs:
        slug = ref.replace("|", "_").replace(":", "_").replace(".", "_").lower()
        out.append((option(f"{prefix}_{slug}", pool, block(char, ref)), weight))
    return out


# D28 rule 1: the spawn-proof floors (chisel, Botania) are 16 of the 100.
FLOORS = singles("floor", "interior-floors", FLOOR, [
    ("minecraft:planks:0", 3), ("minecraft:planks:1", 2), ("minecraft:planks:2", 2),
    ("minecraft:planks:3", 1), ("minecraft:planks:4", 1), ("minecraft:planks:5", 1),
    ("Natura:planks:1", 1), ("Natura:planks:2", 1), ("Natura:planks:3", 1),
    ("Natura:planks:5", 1), ("Natura:planks:6", 1), ("Natura:planks:7", 1),
    ("BiomesOPlenty:planks:2", 1), ("BiomesOPlenty:planks:3", 1), ("BiomesOPlenty:planks:6", 1),
    ("BiomesOPlenty:planks:7", 1), ("BiomesOPlenty:planks:12", 1), ("BiomesOPlenty:planks:14", 1),
    ("cookingforblockheads:white_kitchen_floor:0", 2), ("cookingforblockheads:black_kitchen_floor:0", 1),
    ("cookingforblockheads:light_gray_kitchen_floor:0", 1), ("cookingforblockheads:red_kitchen_floor:0", 1),
    ("Ztones:tile.laveBlock:8", 2), ("Ztones:tile.ztylBlock:8", 2),
    ("etfuturum:polished_deepslate:0", 2), ("etfuturum:smooth_stone:0", 2),
    ("TwilightForest:tile.TFPlank:0", 1), ("TwilightForest:tile.TFPlank:2", 1),
    ("TwilightForest:tile.TFPlank:4", 1), ("witchery:witchwood:0", 1),
    ("chisel:laboratoryblock:6", 2), ("chisel:laboratoryblock:7", 2), ("chisel:laboratoryblock:8", 1),
    ("chisel:porcelain:3", 1), ("chisel:porcelain:4", 1), ("chisel:porcelain:10", 1),
    ("chisel:marble:12", 2), ("chisel:marble:13", 2), ("chisel:marble:14", 1),
    ("Botania:livingwood:1", 2), ("Botania:dreamwood:1", 1),
])

DESERT_FLOORS = singles("floor_desert", "interior-floors", FLOOR, [
    ("minecraft:planks:2", 4), ("minecraft:planks:4", 4), ("minecraft:planks:0", 2),
    ("minecraft:sandstone:2", 5), ("minecraft:double_stone_slab:9", 5),
    ("minecraft:stained_hardened_clay:0", 3), ("minecraft:stained_hardened_clay:8", 3),
    ("Natura:planks:1", 2), ("Natura:planks:2", 2),
    ("cookingforblockheads:white_kitchen_floor:0", 2), ("cookingforblockheads:light_gray_kitchen_floor:0", 2),
    ("Ztones:tile.laveBlock:8", 2),
    ("chisel:laboratoryblock:6", 2), ("chisel:marble:12", 3), ("chisel:sandstone:8", 3),
])

CEILINGS = singles("ceiling", "ceilings", CEILING, [
    ("Ztones:tile.laveBlock:0", 3), ("Ztones:tile.laveBlock:9", 2), ("Ztones:tile.laveBlock:10", 2),
    ("Ztones:tile.laveBlock:11", 1), ("Ztones:tile.laveBlock:12", 1),
    ("Ztones:tile.mintBlock:2", 2), ("Ztones:tile.mintBlock:3", 1),
    ("chisel:laboratoryblock:0", 3), ("chisel:laboratoryblock:1", 3),
    ("chisel:laboratoryblock:9", 2), ("chisel:laboratoryblock:10", 2),
    ("GalacticraftCore:tile.gcBlockCore:4", 8),
    ("chisel:marble:4", 5),
    ("etfuturum:concrete:0", 4),
    ("minecraft:quartz_block:0", 3),
])

# About 45 of 100 buildings are lit (D28 rule 1 lets kept-up or lit buildings be spawn-proof).
LIGHTS = singles("light", "glowstone", LIGHT, [
    ("minecraft:glowstone:0", 10),
    ("Botania:seaLamp:0", 8),
    ("chisel:glowstone:3", 2), ("chisel:glowstone:4", 2), ("chisel:glowstone:8", 2), ("chisel:glowstone:9", 2),
    ("chisel:froglight:0", 2), ("chisel:froglight:4", 2),
    ("appliedenergistics2:tile.BlockQuartzLamp:0", 4),
    ("ExtraUtilities:color_lightgem:0", 3),
    ("IC2NuclearControl:blockNuclearControlLight:1", 3),
    ("GalaxySpace:proteusglowstone:0", 2),
])

COBWEB = "minecraft:cobweb"
FURNACE = "minecraft:furnace[facing=north]"
CRAFTING = "minecraft:crafting_table"
ANVIL = "minecraft:anvil[facing=north]"
CAULDRON = "minecraft:cauldron"
BREWING = "minecraft:brewing_stand"
ENCHANTING = "minecraft:enchanting_table"


def furniture(name, rows):
    """An 'F' list: upstream's share of furniture (65 slots) with the rest cobwebs, as upstream's 'F'."""
    return option(name, "furniture", mix("F", rows, COBWEB))


FURNITURE = [
    (furniture("furniture_workshop", [(15, FURNACE), (10, CRAFTING), (8, "etfuturum:smoker:0"),
                                      (8, "etfuturum:blast_furnace:0"), (8, "etfuturum:smithing_table:0"),
                                      (6, ANVIL), (5, CAULDRON)]), 8),
    (furniture("furniture_kitchen", [(15, "harvestcraft:sink:1"), (10, "harvestcraft:sink:2"),
                                     (8, "harvestcraft:cuttingboard:0"), (6, "harvestcraft:pot:0"),
                                     (15, FURNACE), (6, CRAFTING), (5, CAULDRON)]), 5),
    (furniture("furniture_study", [(15, "etfuturum:cartography_table:0"), (10, "etfuturum:loom:0"),
                                   (8, "etfuturum:fletching_table:0"), (15, CRAFTING), (10, BREWING),
                                   (5, CAULDRON), (2, ENCHANTING)]), 4),
    (furniture("furniture_industrial", [(6, "EnderIO:blockDarkSteelAnvil:0"), (20, FURNACE), (20, CRAFTING),
                                        (6, ANVIL), (8, CAULDRON)]), 2),
] + [
    (furniture(f"furniture_workbench_{wood}", [(25, FURNACE), (20, f"Natura:Natura.workbench:{wood}"),
                                               (7, BREWING), (6, ANVIL), (5, CAULDRON), (2, ENCHANTING)]), 1)
    for wood in (1, 2, 3, 6, 7)
]

BOOKSHELVES = singles("bookshelf", "bookshelves", "L", [
    ("Natura:Natura.bookshelf:1", 3), ("Natura:Natura.bookshelf:3", 3), ("Natura:Natura.bookshelf:5", 3),
    ("Natura:Natura.bookshelf:6", 3), ("Natura:Natura.bookshelf:7", 3),
    ("chisel:bookshelf:1", 4), ("chisel:bookshelf:4", 4), ("chisel:bookshelf:6", 4), ("chisel:bookshelf:8", 3),
    ("Automagy:blockBookshelfEnchanted:0", 8),
])

FENCES = singles("fence", "fences", ".", [
    ("Natura:Natura.fence:0", 4), ("Natura:Natura.fence:1", 4), ("Natura:Natura.fence:3", 4),
    ("Natura:Natura.fence:6", 4),
    ("minecraft:nether_brick_fence:0", 8),
    ("gregtech:gt.blockfencemetal:0", 4), ("IC2:blockFenceIron:0", 4),
])

# --------------------------------------------------------------------------- planters and park flora (D146)

POPPY = "minecraft:red_flower:0"
DANDELION = "minecraft:yellow_flower:0"
OAK_SAPLING = "minecraft:oak_sapling[stage=1]"
SPRUCE_SAPLING = "minecraft:spruce_sapling[stage=1]"
BIRCH_SAPLING = "minecraft:birch_sapling[stage=1]"
ACACIA_SAPLING = "minecraft:acacia_sapling[stage=1]"
CORNFLOWER = "etfuturum:cornflower:0"
LILY = "etfuturum:lily_of_the_valley:0"
SAPLINGS = [(15, OAK_SAPLING), (15, SPRUCE_SAPLING), (15, BIRCH_SAPLING)]


def planter(name, p_rows, k_rows, k_fill):
    """Planters: 'p' keeps upstream's saplings (45 slots) beside the flowers; 'K' is flowers only."""
    return option(name, "flowers-and-planters", mix("p", p_rows + SAPLINGS, ACACIA_SAPLING),
                  mix("K", k_rows, k_fill))


PLANTERS = [
    (planter("planter_modern", [(12, CORNFLOWER), (8, LILY), (8, "BiomesOPlenty:flowers:4"),
                                (8, "BiomesOPlenty:flowers:5"), (8, "BiomesOPlenty:flowers:7"),
                                (8, "BiomesOPlenty:flowers:9")],
             [(32, CORNFLOWER), (16, LILY), (16, "BiomesOPlenty:flowers2:3"), (16, "BiomesOPlenty:flowers2:8"),
              (16, "BiomesOPlenty:flowers2:5")], DANDELION), 20),
    (planter("planter_mystical", [(8, "Botania:flower:0"), (8, "Botania:flower:1"), (8, "Botania:flower:3"),
                                  (8, "Botania:flower:4"), (8, "Botania:flower:6"), (8, "Botania:flower:11")],
             [(20, "Botania:flower:0"), (20, "Botania:flower:3"), (20, "Botania:flower:6"),
              (20, "Botania:flower:11"), (20, "Botania:flower:14")], "Botania:flower:4"), 10),
    (planter("planter_tulips", [(10, "minecraft:red_flower:4"), (10, "minecraft:red_flower:5"),
                                (10, "minecraft:red_flower:6"), (10, "minecraft:red_flower:7"),
                                (8, "minecraft:red_flower:3")],
             [(32, "minecraft:red_flower:4"), (32, "minecraft:red_flower:6"), (32, "minecraft:red_flower:7")],
             "minecraft:red_flower:8"), 15),
    (planter("planter_meadow", [(8, "BiomesOPlenty:flowers2:0"), (8, "BiomesOPlenty:flowers2:4"),
                                (8, "BiomesOPlenty:flowers2:7"), (8, "BiomesOPlenty:flowers:6"),
                                (8, "BiomesOPlenty:flowers:8"), (8, POPPY)],
             [(24, "BiomesOPlenty:flowers:4"), (24, "BiomesOPlenty:flowers:5"), (24, "BiomesOPlenty:flowers:6"),
              (24, "BiomesOPlenty:flowers:7"), (16, "BiomesOPlenty:flowers:8")], "BiomesOPlenty:flowers:9"), 15),
]

GRASS = "minecraft:tallgrass:1"
FERN = "minecraft:tallgrass:2"
DRY = "minecraft:tallgrass:0"
OAK_SHRUB = "minecraft:leaves:4"
SPRUCE_SHRUB = "minecraft:leaves:5"
BIRCH_SHRUB = "minecraft:leaves:6"
JUNGLE_SHRUB = "minecraft:leaves:7"
ACACIA_SHRUB = "minecraft:leaves2:4"


def flora(name, tables):
    """Park flora (ParkGreenery): one 128-slot list per biome table, tall grass filling the rest."""
    return option(name, "flowers-and-planters", *[mix(FLORA[kind], rows, GRASS) for kind, rows in tables.items()])


# Shares follow D146's vanilla tables (out of 100 there, 128 slots here); pack flowers take part of the flower share.
FLORA_OPTIONS = [
    (flora("flora_meadow", {
        "temperate": [(4, FERN), (8, DANDELION), (6, POPPY), (4, "minecraft:red_flower:8"),
                      (4, "minecraft:red_flower:3"), (4, CORNFLOWER), (2, LILY), (2, "BiomesOPlenty:flowers:4"),
                      (2, "BiomesOPlenty:flowers:5"), (3, "BiomesOPlenty:flowers:7"), (2, "BiomesOPlenty:flowers:8"),
                      (2, "BiomesOPlenty:flowers:9"), (6, OAK_SHRUB), (3, BIRCH_SHRUB)],
        "arid": [(10, DRY), (2, DANDELION), (2, POPPY), (3, "BiomesOPlenty:flowers2:4"),
                 (2, "BiomesOPlenty:flowers:5"), (9, ACACIA_SHRUB), (2, OAK_SHRUB)],
        "cold": [(43, FERN), (3, POPPY), (3, DANDELION), (3, "BiomesOPlenty:flowers2:7"),
                 (3, "BiomesOPlenty:flowers2:5"), (2, LILY), (13, SPRUCE_SHRUB)],
        "jungle": [(43, FERN), (3, POPPY), (2, DANDELION), (2, "minecraft:red_flower:1"),
                   (3, "BiomesOPlenty:flowers2:0"), (18, JUNGLE_SHRUB)],
        "swamp": [(20, FERN), (12, "minecraft:red_flower:1"), (4, "BiomesOPlenty:flowers:1"),
                  (2, POPPY), (2, DANDELION), (15, OAK_SHRUB)],
    }), 40),
    (flora("flora_beds", {
        "temperate": [(4, FERN), (6, DANDELION), (6, POPPY), (6, CORNFLOWER), (4, LILY),
                      (4, "BiomesOPlenty:flowers2:3"), (4, "BiomesOPlenty:flowers2:8"), (3, "BiomesOPlenty:flowers2:5"),
                      (3, "Botania:flower:0"), (3, "Botania:flower:3"), (3, "Botania:flower:6"),
                      (3, "minecraft:red_flower:4"), (3, "minecraft:red_flower:6"), (5, OAK_SHRUB), (3, BIRCH_SHRUB)],
        "arid": [(10, DRY), (3, DANDELION), (3, "BiomesOPlenty:flowers2:4"), (2, "BiomesOPlenty:flowers2:3"),
                 (2, "minecraft:red_flower:8"), (9, ACACIA_SHRUB)],
        "cold": [(40, FERN), (3, POPPY), (3, "BiomesOPlenty:flowers2:7"), (4, "BiomesOPlenty:flowers2:5"),
                 (3, CORNFLOWER), (13, SPRUCE_SHRUB)],
        "jungle": [(40, FERN), (3, POPPY), (4, "BiomesOPlenty:flowers2:0"), (3, "Botania:flower:2"),
                   (3, "Botania:flower:5"), (18, JUNGLE_SHRUB)],
        "swamp": [(18, FERN), (10, "minecraft:red_flower:1"), (5, "BiomesOPlenty:flowers:1"),
                  (3, "BiomesOPlenty:flowers2:5"), (2, LILY), (15, OAK_SHRUB)],
    }), 35),
]

# --------------------------------------------------------------------------- styles


GLASS_GROUPS = glass_groups()


def shared_groups(floors):
    """The groups both city styles share: interiors, fittings, planters and park flora."""
    return [
        group("interior_floor", [("none", 30)] + floors),
        group("ceiling", [("none", 50)] + CEILINGS),
        group("ceiling_light", [("none", 55)] + LIGHTS),
        group("furniture", [("none", 40)] + FURNITURE),
        group("bookshelves", [("none", 55)] + BOOKSHELVES),
        group("fences", [("none", 50)] + FENCES, scope="all"),
        group("planters", [("none", 40)] + PLANTERS, scope="all"),
        group("park_flora", [("none", 25)] + FLORA_OPTIONS, scope="street"),
    ]


STYLES = [
    {"style": "standard", "groups": [
        group("wall_stonebrick", [("none", 40)] + STONEBRICK_WALLS, "bricks_standard"),
        group("accent_stonebrick", [("none", 35)] + STONEBRICK_ACCENTS, "bricks_standard"),
        group("band_stonebrick", [("none", 40)] + STONEBRICK_BANDS, "bricks_standard"),
        group("wall_cyan", [("none", 45)] + CYAN_WALLS, "bricks_cyan"),
        group("band_cyan", [("none", 45)] + BLACK_BANDS, "bricks_cyan"),
        group("wall_gray", [("none", 45)] + GRAY_WALLS, "bricks_gray"),
        group("band_gray", [("none", 45)] + BLACK_BANDS, "bricks_gray"),
        group("wall_lightgray", [("none", 45)] + LIGHT_GRAY_WALLS, "bricks_silver"),
        group("band_lightgray", [("none", 45)] + BROWN_BANDS, "bricks_silver"),
        group("quartz", [("none", 50)] + QUARTZ, "default"),
    ] + GLASS_GROUPS + shared_groups(FLOORS)},
    {"style": "desert", "groups": [
        group("wall_desert", [("none", 34)] + DESERT_WALLS, "bricks_desert"),
        group("wall_desert_rough", [("none", 50)] + ROUGH_WALLS, "bricks_desert_rough"),
        group("wall_bleached", [("none", 45)] + BLEACHED_WALLS, "bricks_desert_bleached"),
        # The 15 the brick patterns had go to the baseline, so every other draw lands where it did.
        group("wall_taupe", [("none", 60)] + TAUPE_WALLS, "bricks_desert_taupe"),
        group("accent_desert", [("none", 42)] + DESERT_ACCENTS),
        group("band_desert", [("none", 50)] + DESERT_BANDS),
        group("quartz", [("none", 50)] + QUARTZ, "default_desert"),
    ] + GLASS_GROUPS + shared_groups(DESERT_FLOORS)},
]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", default=OUT)
    args = parser.parse_args()
    document = {
        "comment": "Generated by tools/asset-convert/variety.py from the block catalog's variety pools "
                   "(docs/new-spec/05-assets/block-catalog/index.md). Do not edit by hand: edit the script and rerun "
                   "it.",
        "options": OPTIONS,
        "styles": STYLES,
    }
    with open(args.out, "w", encoding="utf-8", newline="\n") as out:
        json.dump(document, out, indent=2, ensure_ascii=True)
        out.write("\n")
    groups = sum(len(style["groups"]) for style in STYLES)
    print(f"{len(OPTIONS)} options, {groups} groups in {len(STYLES)} styles: {args.out}")


if __name__ == "__main__":
    main()
