#!/usr/bin/env python3
"""This mod's original buildings for the low-density rings: detached houses and low-rises (city-realism Phase 2d).

Original work by the Lost Cities: New Horizons contributors (MIT), authored from scratch here; no layout is taken from
upstream Lost Cities or any other mod (docs/assets/CREDITS.md). The designs are drawn in code so a change is one edit
and the generated JSON is never edited by hand. convert.py calls original_assets() and writes the files into the
bundled tree and its index next to upstream's.

The parts use the upstream asset format: 16 x 16 footprints, one part per floor, six slices per floor (slice 0 the
floor, 1 to 4 the room, 5 the ceiling), roofs as 'top' parts. Rules every floor part keeps, checked by
OriginalBuildingsTest:
  * slice 3 is a clean repeatable row: walls, glass and air, plus ladders, which a row copy extends (D275), so the
    floor-pitch stretch (Phase 1b) can duplicate it; slice 2 is clean too except door upper halves and shelves;
  * at most two tile-entity blocks per floor (chest, furnace); no BiblioCraft, no Carpenter's Blocks;
  * floors join by ladders, never stairs; the ladder runs through slices 0 to 5 so stacked floors line up.
Block names are 1.20 names the block-name map resolves, or direct 1.7.10 'modid:name:meta' references for plain
blocks the map has no row for (logs, wooden slabs, carpet, beds, crops).

Usage: imported by tools/asset-convert/convert.py; `python3 -I tools/asset-convert/originals.py` prints each part.
"""

import json

SIZE = 16
FLOOR_SLICES = 6

# The shared palette of every original building: one character per block, mnemonic where possible. '#' and '}' are
# left to the city style (its wall block and rubble), so the low-rises take the city's own bricks.
PALETTE = [
    (" ", {"block": "minecraft:air"}),
    ("B", {"block": "minecraft:bricks", "damaged": "minecraft:iron_bars"}),
    ("P", {"block": "minecraft:oak_planks"}),
    ("Q", {"block": "minecraft:spruce_planks"}),
    ("O", {"block": "minecraft:log:0"}),
    ("U", {"block": "minecraft:log:1"}),
    ("V", {"block": "minecraft:white_terracotta", "damaged": "minecraft:iron_bars"}),
    ("S", {"block": "minecraft:stone_bricks", "damaged": "minecraft:iron_bars"}),
    ("m", {"block": "minecraft:smooth_stone"}),
    ("K", {"block": "minecraft:cobblestone"}),
    ("g", {"block": "minecraft:glass_pane"}),
    ("+", {"block": "minecraft:glass"}),
    ("G", {"block": "minecraft:grass_block"}),
    ("r", {"block": "minecraft:smooth_stone_slab[type=double]"}),
    ("f", {"block": "minecraft:oak_fence"}),
    ("v", {"block": "minecraft:oak_leaves[persistent=true]"}),
    ("p", {"blocks": [
        {"random": 40, "block": "minecraft:poppy"},
        {"random": 40, "block": "minecraft:dandelion"},
        {"random": 48, "block": "minecraft:tallgrass:1"},
    ]}),
    ("~", {"block": "minecraft:water"}),
    ("y", {"block": "minecraft:farmland:7"}),
    ("w", {"block": "minecraft:wheat:7"}),
    ("H", {"block": "minecraft:hay_block:0"}),
    # ladders by the way they face: attached to the block behind them
    ("l", {"block": "minecraft:ladder[facing=north]"}),
    ("j", {"block": "minecraft:ladder[facing=south]"}),
    ("k", {"block": "minecraft:ladder[facing=east]"}),
    ("i", {"block": "minecraft:ladder[facing=west]"}),
    # oak doors (plain vanilla doors, no tile entity): lower and upper half per wall
    ("1", {"block": "minecraft:oak_door[facing=north,half=lower]"}),
    ("2", {"block": "minecraft:oak_door[facing=north,half=upper,hinge=left]"}),
    ("3", {"block": "minecraft:oak_door[facing=south,half=lower]"}),
    ("4", {"block": "minecraft:oak_door[facing=south,half=upper,hinge=left]"}),
    # roof stairs: spruce for the garden house, brick tiles for the cottage, stone brick for the workshop
    ("e", {"block": "minecraft:spruce_stairs[facing=east,half=bottom]"}),
    ("x", {"block": "minecraft:spruce_stairs[facing=west,half=bottom]"}),
    ("N", {"block": "minecraft:brick_stairs[facing=north,half=bottom]"}),
    ("Z", {"block": "minecraft:brick_stairs[facing=south,half=bottom]"}),
    ("n", {"block": "minecraft:stone_brick_stairs[facing=north,half=bottom]"}),
    ("q", {"block": "minecraft:wooden_slab:1"}),
    ("z", {"block": "minecraft:brick_slab[type=bottom]"}),
    ("=", {"block": "minecraft:stone_brick_slab[type=bottom]"}),
    ("_", {"block": "minecraft:smooth_stone_slab[type=bottom]"}),
    # furnishings: plain blocks first; chest and furnace are the only tile entities
    ("C", {"block": "minecraft:chest[facing=south]", "loot": "chestloot"}),
    ("F", {"block": "minecraft:furnace[facing=south]"}),
    ("c", {"block": "minecraft:crafting_table"}),
    ("L", {"block": "minecraft:bookshelf"}),
    ("o", {"block": "minecraft:cauldron"}),
    ("a", {"block": "minecraft:anvil[facing=north]"}),
    ("h", {"block": "minecraft:glowstone"}),
    ("R", {"block": "minecraft:red_wool"}),
    ("X", {"block": "minecraft:light_gray_concrete"}),
    ("5", {"block": "minecraft:carpet:14"}),
    ("6", {"block": "minecraft:carpet:12"}),
    ("9", {"block": "minecraft:carpet:15"}),
    # beds: foot and head, 1.7.10 metadata (direction 2 = head to the north, +8 = head)
    ("7", {"block": "minecraft:bed:2"}),
    ("8", {"block": "minecraft:bed:10"}),
]
PALETTE_NAME = "nh_residential"


class Slice:
    """One 16 x 16 layer: rows are z (north first), columns x (west first)."""

    def __init__(self, fill=" "):
        self.cells = [[fill] * SIZE for _ in range(SIZE)]

    def set(self, x, z, c):
        self.cells[z][x] = c
        return self

    def rect(self, x0, z0, x1, z1, c):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                self.cells[z][x] = c
        return self

    def ring(self, x0, z0, x1, z1, c):
        for x in range(x0, x1 + 1):
            self.cells[z0][x] = c
            self.cells[z1][x] = c
        for z in range(z0, z1 + 1):
            self.cells[z][x0] = c
            self.cells[z][x1] = c
        return self

    def corners(self, x0, z0, x1, z1, c):
        for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            self.cells[z][x] = c
        return self

    def rows(self):
        return ["".join(row) for row in self.cells]


def slices(n, fill=" "):
    return [Slice(fill) for _ in range(n)]


def part_json(layers, meta=None):
    data = {"xsize": SIZE, "zsize": SIZE}
    if meta:
        data["meta"] = meta
    data["slices"] = [layer.rows() for layer in layers]
    return json.dumps(data, indent=2) + "\n"


def building_json(parts, filler, minfloors, maxfloors, allow_doors=True):
    data = {
        "filler": filler,
        "rubble": "}",
        "refpalette": PALETTE_NAME,
        "minfloors": minfloors,
        "maxfloors": maxfloors,
        "mincellars": 0,
        "maxcellars": 0,
    }
    if not allow_doors:
        data["allowDoors"] = False
    data["parts"] = parts
    return json.dumps(data, indent=2) + "\n"


def palette_json():
    entries = []
    for char, entry in PALETTE:
        row = {"char": char}
        row.update(entry)
        entries.append(row)
    return json.dumps({"palette": entries}, indent=2) + "\n"


NO_CONNECT = [{"key": "dontconnect", "boolean": True}]


def ladder(layers, x, z, c, lo=0, hi=5):
    for y in range(lo, hi + 1):
        layers[y].set(x, z, c)


# ---------------------------------------------------------------------------------------------------- garden house
# A detached two-floor brick house with spruce trim, set in a fenced garden: front path and flower beds, back yard.
# House: x 3..12, z 3..10. Front door on the south wall, back door on the north wall; gates in the middle of every
# side of the fence, since a building is never rotated toward its street.

GH = (3, 3, 12, 10)


def garden_yard(s0, s1):
    s0.rect(0, 0, 15, 15, "G")
    s1.ring(0, 0, 15, 15, "f")
    for x in (6, 7, 8, 9):
        s1.set(x, 0, " ").set(x, 15, " ")
    for z in (6, 7, 8, 9):
        s1.set(0, z, " ").set(15, z, " ")


def garden_house_walls(layers, ground):
    x0, z0, x1, z1 = GH
    for y in range(0, 6):
        layers[y].ring(x0, z0, x1, z1, "B")
        layers[y].corners(x0, z0, x1, z1, "U")
    layers[0].ring(x0, z0, x1, z1, "K" if ground else "B")
    layers[0].corners(x0, z0, x1, z1, "U")
    layers[0].rect(x0 + 1, z0 + 1, x1 - 1, z1 - 1, "P")
    layers[5].ring(x0, z0, x1, z1, "Q")
    layers[5].rect(x0 + 1, z0 + 1, x1 - 1, z1 - 1, "P")
    layers[5].set(6, 6, "h").set(9, 8, "h")
    # windows: two-pane on the long walls, single on the ends, slices 2 and 3
    for y in (2, 3):
        for x in (4, 5, 10, 11):
            layers[y].set(x, z1, "g")
        for x in (5, 6, 9, 10):
            layers[y].set(x, z0, "g")
        for z in (5, 8):
            layers[y].set(x0, z, "g").set(x1, z, "g")


def garden_house_ground():
    L = slices(FLOOR_SLICES)
    garden_yard(L[0], L[1])
    garden_house_walls(L, True)
    x0, z0, x1, z1 = GH
    # paths to the front and back doors, and on to the gates
    L[0].rect(7, z1 + 1, 8, 15, "r")
    L[0].rect(7, 0, 8, z0 - 1, "r")
    L[0].rect(1, 12, 14, 12, "r")
    L[0].rect(1, 7, 2, 8, "r").rect(13, 7, 14, 8, "r")
    # flower beds along the front, hedges inside the front fence
    for x in (3, 4, 5, 10, 11, 12):
        L[1].set(x, 11, "p")
    for x in list(range(1, 6)) + list(range(10, 15)):
        L[1].set(x, 14, "v")
    L[1].set(1, 1, "v").set(2, 1, "v").set(1, 2, "v")
    L[1].set(14, 1, "H")
    # doors: front (south wall) and back (north wall)
    L[1].set(7, z1, "3")
    L[2].set(7, z1, "4")
    L[1].set(8, z0, "1")
    L[2].set(8, z0, "2")
    # living room and kitchen
    L[1].set(4, 9, "F").set(5, 9, "c").set(6, 9, "o")
    L[1].rect(8, 6, 9, 7, "5")
    L[1].set(11, 5, "L").set(11, 6, "L").set(11, 4, "C")
    L[1].set(10, 8, "q")
    ladder(L, 4, 4, "j", 1, 5)
    return part_json(L, NO_CONNECT)


def garden_house_upper():
    L = slices(FLOOR_SLICES)
    garden_house_walls(L, False)
    x0, z0, x1, z1 = GH
    # bedroom east, study west, a plank partition with an opening
    for y in range(1, 5):
        for z in range(z0 + 1, z1):
            L[y].set(8, z, "P")
    L[1].set(8, 7, " ").set(2 + 6, 6, " ")
    L[2].set(8, 7, " ").set(8, 6, " ")
    L[1].set(10, 5, "7").set(10, 4, "8")
    L[1].rect(10, 6, 11, 7, "6")
    L[1].set(11, 9, "C")
    L[1].set(5, 9, "L").set(6, 9, "L").set(7, 9, "c")
    ladder(L, 4, 4, "j")
    return part_json(L, NO_CONNECT)


def garden_house_roof():
    L = slices(FLOOR_SLICES)
    x0, z0, x1, z1 = GH
    # gable roof, ridge north-south, one block of overhang all round; spruce gables with an attic window
    for k in range(5):
        for z in range(z0 - 1, z1 + 2):
            L[k].set(x0 - 1 + k, z, "e").set(x1 + 1 - k, z, "x")
        for x in range(x0 + k, x1 - k + 1):
            L[k].set(x, z0, "Q").set(x, z1, "Q")
    for z in range(z0 - 1, z1 + 2):
        L[5].set(7, z, "q").set(8, z, "q")
    L[2].set(7, z1, "g").set(8, z1, "g")
    # brick chimney through the east slope
    for y in range(0, 6):
        L[y].set(10, 5, "B")
    return part_json(L, NO_CONNECT)


# ---------------------------------------------------------------------------------------------------------- cottage
# A one-floor whitewashed cottage, spruce-log frame, red brick-tile roof with the ridge east-west, a picket-fenced
# front garden and a small vegetable plot with its water in the back. House: x 4..11, z 5..11.

CT = (4, 5, 11, 11)


def cottage_ground():
    L = slices(FLOOR_SLICES)
    x0, z0, x1, z1 = CT
    garden_yard(L[0], L[1])
    for y in range(0, 6):
        L[y].ring(x0, z0, x1, z1, "V")
        L[y].corners(x0, z0, x1, z1, "U")
        L[y].set(8, z0, "U").set(8, z1, "U")
    L[0].ring(x0, z0, x1, z1, "K")
    L[0].rect(x0 + 1, z0 + 1, x1 - 1, z1 - 1, "Q")
    L[5].ring(x0, z0, x1, z1, "U")
    L[5].rect(x0 + 1, z0 + 1, x1 - 1, z1 - 1, "Q")
    L[5].set(7, 8, "h")
    for y in (2, 3):
        L[y].set(5, z1, "g").set(6, z1, "g").set(10, z1, "g")
        L[y].set(6, z0, "g").set(10, z0, "g")
        L[y].set(x0, 8, "g").set(x1, 8, "g")
    # doors
    L[1].set(9, z1, "3")
    L[2].set(9, z1, "4")
    L[1].set(7, z0, "1")
    L[2].set(7, z0, "2")
    # paths
    L[0].rect(9, z1 + 1, 9, 15, "r")
    L[0].rect(7, 0, 7, z0 - 1, "r")
    # front garden: flower rows either side of the path
    for x in (2, 3, 4, 5, 6, 7):
        L[1].set(x, 13, "p")
    for x in (11, 12, 13):
        L[1].set(x, 13, "p")
    L[1].set(1, 14, "v").set(14, 14, "v")
    # vegetable plot behind, water in the middle
    L[0].rect(9, 1, 13, 3, "y")
    L[0].set(11, 2, "~")
    for x in range(9, 14):
        for z in range(1, 4):
            if (x, z) != (11, 2):
                L[1].set(x, z, "w")
    L[1].set(2, 2, "H").set(2, 3, "H")
    # inside: bed, stove, table, shelf, rug
    L[1].set(10, 7, "7").set(10, 6, "8")
    L[1].set(5, 6, "F").set(6, 6, "c")
    L[1].set(5, 10, "C")
    L[1].set(x1 - 1, 10, "L")
    L[1].rect(6, 8, 7, 9, "5")
    return part_json(L, NO_CONNECT)


def cottage_roof():
    L = slices(FLOOR_SLICES)
    x0, z0, x1, z1 = CT
    for k in range(4):
        for x in range(x0 - 1, x1 + 2):
            L[k].set(x, z0 - 1 + k, "Z").set(x, z1 + 1 - k, "N")
        for z in range(z0 + k, z1 - k + 1):
            L[k].set(x0, z, "V").set(x1, z, "V")
    for x in range(x0 - 1, x1 + 2):
        L[4].set(x, 8, "z")
    L[1].set(x0, 8, "g").set(x1, 8, "g")
    for y in range(0, 6):
        L[y].set(6, 7, "B")
    L[5].set(6, 7, "z")
    return part_json(L, NO_CONNECT)


# ------------------------------------------------------------------------------------------------------ corner shop
# A shop with glass fronts on every side, a red fascia band over the windows and a flat above, in the city's own
# wall block ('#'). The layout puts the street doors in the middle of the street sides (x or z 6..9), which stay
# wall here. Ladder in the north-east corner, up to the roof.

def shop_ground():
    L = slices(FLOOR_SLICES)
    L[0].rect(0, 0, 15, 15, "P").ring(0, 0, 15, 15, "#")
    for y in range(1, 5):
        L[y].ring(0, 0, 15, 15, "#")
    for y in (1, 2, 3):
        for a in list(range(1, 6)) + list(range(10, 15)):
            for x, z in ((a, 0), (a, 15), (0, a), (15, a)):
                L[y].set(x, z, "+")
    L[1].ring(0, 0, 15, 15, "#")
    for a in range(1, 15):
        for x, z in ((a, 0), (a, 15), (0, a), (15, a)):
            L[4].set(x, z, "R")
    L[5].rect(0, 0, 15, 15, "P").ring(0, 0, 15, 15, "#")
    for x, z in ((4, 4), (11, 4), (4, 11), (11, 11)):
        L[5].set(x, z, "h")
    # shelves along the north and west walls, an aisle, a counter in the south-east
    for x in range(2, 6):
        L[1].set(x, 1, "L")
        L[2].set(x, 1, "L")
    for x in range(4, 8):
        L[1].set(x, 7, "L")
    for x in range(10, 14):
        L[1].set(x, 11, "Q")
    L[1].set(13, 12, "Q").set(13, 13, "C").set(11, 13, "c")
    ladder(L, 14, 1, "i", 1, 5)
    return part_json(L)


def window_walls(L, wall, sides_ok=(2, 3, 7, 8, 12, 13)):
    for y in range(0, 6):
        L[y].ring(0, 0, 15, 15, wall)
    for y in (2, 3):
        for a in sides_ok:
            for x, z in ((a, 0), (a, 15), (0, a), (15, a)):
                L[y].set(x, z, "g")


def shop_flat():
    L = slices(FLOOR_SLICES)
    window_walls(L, "#")
    L[0].rect(1, 1, 14, 14, "P")
    L[5].rect(1, 1, 14, 14, "P")
    L[5].set(4, 4, "h").set(11, 11, "h").set(4, 11, "h")
    # two rooms: living (south) and bedroom (north), split at z 7 with a doorway
    for y in range(1, 5):
        for x in range(1, 15):
            L[y].set(x, 7, "P")
    L[1].set(5, 7, " ")
    L[2].set(5, 7, " ")
    L[1].set(3, 3, "7").set(3, 2, "8")
    L[1].rect(5, 3, 6, 4, "6")
    L[1].set(12, 1, "C")
    L[1].set(2, 13, "F").set(3, 13, "c").set(9, 10, "q").set(10, 10, "q")
    L[1].set(13, 9, "L").set(13, 10, "L")
    ladder(L, 14, 1, "i")
    return part_json(L)


def flat_roof(wall, cornice, ladder_at=None, ladder_c=None):
    L = slices(FLOOR_SLICES)
    L[0].ring(0, 0, 15, 15, cornice)
    L[1].ring(0, 0, 15, 15, "=")
    if wall != cornice:
        L[0].corners(0, 0, 15, 15, wall)
    if ladder_at:
        L[0].set(ladder_at[0], ladder_at[1], ladder_c)
    return L


def shop_roof():
    L = flat_roof("#", "#", (14, 1), "i")
    for y in range(0, 3):
        L[y].set(3, 12, "B").set(4, 12, "B")
    L[3].set(3, 12, "z").set(4, 12, "z")
    return part_json(L)


# ---------------------------------------------------------------------------------------------------------- workshop
# A stone-brick workshop: a roller door on the south side (a closed concrete panel), high strip windows, anvil and
# benches; an office above with ribbon windows; a north-light sawtooth roof.

def workshop_ground():
    L = slices(FLOOR_SLICES)
    for y in range(0, 6):
        L[y].ring(0, 0, 15, 15, "S")
    L[0].rect(1, 1, 14, 14, "m")
    L[5].rect(1, 1, 14, 14, "m")
    L[5].set(5, 5, "h").set(10, 10, "h").set(5, 10, "h").set(10, 5, "h")
    # roller door, x 10..14 on the south wall, slices 1..3, under a smooth-stone lintel
    for y in (1, 2, 3):
        for x in range(10, 15):
            L[y].set(x, 15, "X")
    for x in range(9, 15):
        L[4].set(x, 15, "m")
    # high strip windows, slice 3, on the east, west and north walls (the middle stays wall for the street doors)
    for a in list(range(1, 6)) + list(range(10, 15)):
        L[3].set(0, a, "g").set(15, a, "g").set(a, 0, "g")
    # benches and tools
    L[1].set(2, 1, "c").set(3, 1, "c").set(4, 1, "a").set(5, 1, "o")
    L[1].set(11, 1, "F").set(12, 1, "C")
    L[1].rect(10, 9, 13, 9, "q")
    L[1].set(8, 11, "9").set(9, 12, "9").set(12, 12, "9")
    ladder(L, 1, 14, "k", 1, 5)
    return part_json(L)


def workshop_office():
    L = slices(FLOOR_SLICES)
    for y in range(0, 6):
        L[y].ring(0, 0, 15, 15, "S")
    L[0].rect(1, 1, 14, 14, "P")
    L[5].rect(1, 1, 14, 14, "P")
    L[5].set(5, 5, "h").set(10, 10, "h")
    for y in (2, 3):
        for a in range(1, 15):
            if a % 5 != 0:
                for x, z in ((a, 0), (a, 15), (0, a), (15, a)):
                    L[y].set(x, z, "g")
    for y in (1, 4):
        for a in range(1, 15):
            for x, z in ((a, 0), (a, 15), (0, a), (15, a)):
                L[y].set(x, z, "m")
    L[1].corners(0, 0, 15, 15, "S")
    L[4].corners(0, 0, 15, 15, "S")
    # desks and files
    for x in (3, 4, 8, 9):
        L[1].set(x, 4, "q").set(x, 9, "q")
    L[1].set(13, 1, "L").set(13, 2, "L").set(13, 3, "L").set(12, 1, "C")
    L[1].rect(6, 12, 9, 13, "6")
    ladder(L, 1, 14, "k")
    return part_json(L)


def workshop_roof():
    L = slices(FLOOR_SLICES)
    L[0].ring(0, 0, 15, 15, "S")
    for z0 in (1, 6, 11):
        for k in range(4):
            z = z0 + 4 - k
            for x in range(1, 15):
                L[k].set(x, z, "n")
            L[k].set(0, z, "S").set(15, z, "S")
            for y in range(0, k):
                L[y].set(0, z, "S").set(15, z, "S")
        for y in range(0, 4):
            for x in range(1, 15):
                L[y].set(x, z0, "+")
            L[y].set(0, z0, "S").set(15, z0, "S")
        for x in range(0, 16):
            L[4].set(x, z0, "=")
    L[0].set(1, 14, "k")
    return part_json(L)


# ------------------------------------------------------------------------------------------------------------ walk-up
# A brick walk-up: stone-brick quoins, sills and lintels; a central hall (x 6..9) with the ladder at its north end and
# a flat each side; flat roof with a cornice and a spruce water tank.

def walkup_floor(ground):
    L = slices(FLOOR_SLICES)
    windows = (2, 3, 12, 13) if ground else (2, 3, 7, 8, 12, 13)
    window_walls(L, "B", windows)
    for y in range(0, 6):
        L[y].corners(0, 0, 15, 15, "S")
    for a in windows:
        for x, z in ((a, 0), (a, 15), (0, a), (15, a)):
            L[1].set(x, z, "S")
            L[4].set(x, z, "S")
    L[0].rect(1, 1, 14, 14, "P")
    L[0].rect(6, 1, 9, 14, "m" if ground else "Q")
    L[5].rect(1, 1, 14, 14, "P")
    L[5].set(7, 7, "h").set(3, 7, "h").set(12, 7, "h")
    # flat walls at x 5 and 10, a doorway into each flat at z 8..9
    for y in range(1, 5):
        for z in range(1, 15):
            L[y].set(5, z, "P").set(10, z, "P")
    for y in (1, 2):
        L[y].set(5, 9, " ").set(10, 9, " ")
    # west flat: bed and chest; east flat: stove, bench, shelf (two tile entities per floor)
    L[1].set(2, 3, "7").set(2, 2, "8").set(4, 1, "C")
    L[1].rect(2, 10, 3, 11, "5")
    L[1].set(13, 1, "F").set(12, 1, "c").set(13, 13, "L").set(13, 12, "L")
    L[1].set(11, 8, "q").set(12, 8, "q")
    ladder(L, 7, 1, "j", 1 if ground else 0, 5)
    return part_json(L)


def walkup_roof():
    L = flat_roof("S", "S", (7, 1), "j")
    for x, z in ((11, 11), (13, 11), (11, 13), (13, 13)):
        L[0].set(x, z, "U")
    L[1].rect(11, 11, 13, 13, "Q")
    L[2].rect(11, 11, 13, 13, "Q")
    L[3].rect(11, 11, 13, 13, "q")
    return part_json(L)


# ---------------------------------------------------------------------------------------------------------- assembly

def ref(part, top, ground=None):
    r = {"top": top, "part": part}
    if ground is not None:
        r["ground"] = ground
    return r


# name -> (class, footprint, building JSON)
BUILDINGS = {
    "nh_house_garden": ("house", "10x8 house in 16x16 garden", building_json(
        [ref("nh_house_garden_ground", False, True), ref("nh_house_garden_upper", False, False),
         ref("nh_house_garden_roof", True)], "K", 1, 2, allow_doors=False)),
    "nh_cottage": ("house", "8x7 house in 16x16 garden", building_json(
        [ref("nh_cottage_ground", False), ref("nh_cottage_roof", True)], "K", 1, 1, allow_doors=False)),
    "nh_corner_shop": ("low-rise", "16x16", building_json(
        [ref("nh_corner_shop_ground", False, True), ref("nh_corner_shop_flat", False, False),
         ref("nh_corner_shop_roof", True)], "#", 2, 3)),
    "nh_workshop": ("low-rise", "16x16", building_json(
        [ref("nh_workshop_ground", False, True), ref("nh_workshop_office", False, False),
         ref("nh_workshop_roof", True)], "S", 2, 2)),
    "nh_walkup": ("low-rise", "16x16", building_json(
        [ref("nh_walkup_ground", False, True), ref("nh_walkup_upper", False, False),
         ref("nh_walkup_roof", True)], "B", 2, 3)),
}

PARTS = {
    "nh_house_garden_ground": garden_house_ground,
    "nh_house_garden_upper": garden_house_upper,
    "nh_house_garden_roof": garden_house_roof,
    "nh_cottage_ground": cottage_ground,
    "nh_cottage_roof": cottage_roof,
    "nh_corner_shop_ground": shop_ground,
    "nh_corner_shop_flat": shop_flat,
    "nh_corner_shop_roof": shop_roof,
    "nh_workshop_ground": workshop_ground,
    "nh_workshop_office": workshop_office,
    "nh_workshop_roof": workshop_roof,
    "nh_walkup_ground": lambda: walkup_floor(True),
    "nh_walkup_upper": lambda: walkup_floor(False),
    "nh_walkup_roof": walkup_roof,
}


def original_assets():
    """Returns every original file, path relative to the bundled asset root -> text."""
    out = {"palettes/%s.json" % PALETTE_NAME: palette_json()}
    for name, (_, _, text) in BUILDINGS.items():
        out["buildings/%s.json" % name] = text
    for name, make in PARTS.items():
        out["parts/%s.json" % name] = make()
    return out


if __name__ == "__main__":
    for name, make in PARTS.items():
        print("==", name)
        for y, layer in enumerate(json.loads(make())["slices"]):
            print("-- slice", y)
            print("\n".join(layer))
