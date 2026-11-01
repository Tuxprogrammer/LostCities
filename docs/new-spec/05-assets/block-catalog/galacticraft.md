# Block catalog: Galacticraft

Galacticraft 3.4.37-GTNH registers its blocks under two mod IDs: `GalacticraftCore` (core and Moon) and `GalacticraftMars` (the planets module: Mars and asteroids). Jar `Galacticraft-3.4.37-GTNH.jar`; source read at tag `3.4.37-GTNH` in `gtnh-src/source/Galacticraft/` (`core/blocks/GCBlocks.java`, `BlockBasic`, `BlockBasicMoon`, `BlockSlabGC`, `BlockWallGC`, `BlockStairsGC`, `planets/mars/blocks/MarsBlocks.java`, `BlockBasicMars`, `planets/asteroids/blocks/AsteroidBlocks.java`) with English names from the four `en_US.lang` files. Pack config checked: `config/Galacticraft/core.conf` and `planets.conf` (no block is switched off; overworld GC ores and Moon/Mars GC ore generation are off). Most blocks register under their unlocalized name, so the registry name keeps the `tile.` prefix (`GalacticraftCore:tile.gcBlockCore`); the slabs drop it (`GalacticraftCore:slabGCHalf`). GT's own `StoneType` names the same blocks this way (`"tile.moonBlock"`, `"tile.mars"`, `"tile.asteroidsBlock"`). Contact sheets: `tools/block-contact-sheet/sheets/galacticraft/decoration.json`, `planet-stone.json`, `fixtures.json`. This catalog serves D25 in the [decision log](../../../design/decision-log.md): variety per palette role, with the [D19 mapping](../../00-pack-integration/block-mapping.md) as baseline.

## Summary

**72 block+meta entries from 26 registry blocks** (ores, treasure chests, machines, the hidden unlit-torch replacements, fluids and air blocks are left out; see [Not catalogued](#not-catalogued)). Fit with upstream's look: 16 yes, 39 partly (mostly industrial or space-flavored metal), 17 no.

| Role | Count |
|---|---|
| wall-main | 7 |
| wall-accent | 7 |
| trim-pillar | 1 |
| floor | 6 |
| ceiling | 1 |
| roof | 2 |
| street | 3 |
| sidewalk | 3 |
| stairs-slab | 17 |
| rubble-ruin | 8 |
| light | 2 |
| fence-rail | 5 |
| industrial-dressing | 28 |
| foliage | 1 |
| special | 3 |
| off-theme | 14 |

Entries carry several roles, so the column sums to more than 72.

Flags:

- **Tile entities:** the arc lamp, the wire and oxygen-pipe walkways, both landing pads, the air lock frame and controller, all 15 sealable (`tile.enclosed`) variants, the space station arrival block and the fallen meteor. The plain walkway has none. The sealable blocks create other mods' cable and pipe tile entities (IC2, BuildCraft, AE2) and only make sense as inert dressing; placing them in worldgen is a cross-mod risk.
- **Light:** arc lamp 15 (goes dark on a redstone signal), glowstone torch 12.
- **Gravity:** the fallen meteor falls (its own `tryToFall`, not `BlockFalling`). Nothing else.
- **Hard to move:** Moon and Mars dungeon bricks (block, slab, stairs, wall) need a diamond-level pick (harvest level 3) and the full blocks resist explosions at 40; the fallen meteor has hardness 40 and harvest level 3. Decoration blocks are soft (hardness 1, blast 2 for the tin decoration blocks).
- **GT ore hosts, in every dimension including the overworld:** `tile.moonBlock:4` (Moon Rock), `tile.mars:9` (Mars Stone), `tile.mars:6` (Mars Sub-Surface Rock) and `tile.asteroidsBlock:1`. GT's `StoneType` lists them as the Moon, Mars and Asteroid stone types without a dimension restriction, and `OreManager` places ore into any enabled stone type valid in the dimension ([StoneType.java#L84-L97](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L84-L97), [#L385-L409](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L385-L409)). **Never use them as building blocks.** This extends the overworld host table in [ore-veins.md](../../00-pack-integration/ore-veins.md), which lists only overworld-native stones. Mars Cobblestone (`tile.mars:4`) is the Mars type's cobble, which `contains` does not test, so it is not a host. The slabs, stairs and walls are separate blocks and never hosts. None of these blocks are GT boulder hosts (the boulder list is fixed in ore-veins.md).
- **Tier:** nothing here is a GT machine. The walkways, pads and air lock are Galacticraft space-program gear (in GTNH, roughly the HV to EV rocket stage); as inert dressing they are within the EV ceiling of record 0004, but they read as a spaceport, not a city.

## Decoration blocks, slabs, stairs and walls

Sheet `decoration.json`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalacticraftCore:tile.gcBlockCore:3` | Tin Decoration Block | wall-accent, floor, industrial-dressing | partly: sci-fi chevron panel, top and sides differ | industrial, space | Hardness 1, blast 2 |
| `GalacticraftCore:tile.gcBlockCore:4` | Tin Decoration Block | wall-main, floor, ceiling, sidewalk | yes: plain light-gray metal panel with a border, reads as modern cladding or floor tile | urban, industrial, space | Hardness 1, blast 2. One of the most usable blocks in the mod |
| `GalacticraftCore:tile.gcBlockCore:9` | Copper Block | wall-accent, roof | partly: orange striated metal, looks like copper sheet | industrial | `blockCopper`. Cheap material |
| `GalacticraftCore:tile.gcBlockCore:10` | Tin Block | wall-accent, trim-pillar | partly: pale striated metal | industrial | `blockTin` |
| `GalacticraftCore:tile.gcBlockCore:11` | Aluminum Block | wall-main, roof, industrial-dressing | partly: ribbed blue-white siding, reads as corrugated cladding | industrial | `blockAluminium` |
| `GalacticraftCore:tile.gcBlockCore:12` | Solid Meteoric Iron | wall-accent | partly: dark brown metal | industrial, space | Blast 8 |
| `GalacticraftCore:tile.moonBlock:14` | Moon Dungeon Brick | wall-main, wall-accent, rubble-ruin | yes: dark gray brick, close to upstream's blackstone-brick building | urban, ruined, space | Hardness 4, blast 40, harvest level 3 |
| `GalacticraftMars:tile.mars:7` | Mars Dungeon Brick | off-theme | no: bright green brick | space | Hardness 4, blast 40, harvest level 3 |
| `GalacticraftMars:tile.mars:8` | Block of Desh | industrial-dressing, wall-accent | partly: dark machined panel | industrial, space | |
| `GalacticraftCore:slabGCHalf:0` | Tin Decoration Slab | stairs-slab, floor, sidewalk | yes | urban, industrial | Meta +8 = top half |
| `GalacticraftCore:slabGCHalf:1` | Tin Decoration Slab | stairs-slab, industrial-dressing | partly | industrial, space | Chevron sides |
| `GalacticraftCore:slabGCHalf:2` | Moon Rock Slab | stairs-slab, street, sidewalk | yes: smooth light-gray slab, like a concrete slab | urban, ruined | Not an ore host (separate block) |
| `GalacticraftCore:slabGCHalf:3` | Moon Dungeon Brick Slab | stairs-slab, rubble-ruin | yes | urban, ruined | Harvest level 3 |
| `GalacticraftCore:slabGCHalf:4` | Mars Cobblestone Slab | stairs-slab, rubble-ruin | partly: red cobble | desert, ruined | |
| `GalacticraftCore:slabGCHalf:5` | Mars Dungeon Brick Slab | off-theme, stairs-slab | no: green | space | Harvest level 3 |
| `GalacticraftCore:slabGCDouble:0`, `:2`, `:3` (3 entries) | Tin Decoration, Moon Rock and Moon Dungeon Brick double slabs | stairs-slab, wall-main | yes | urban, ruined | Full blocks; hidden from NEI. Upstream uses double slabs for `$` bands |
| `GalacticraftCore:slabGCDouble:1`, `:4`, `:5` (3 entries) | Tin Decoration (chevron), Mars Cobblestone and Mars Dungeon Brick double slabs | stairs-slab | partly: 1 and 4 as their slabs; 5 is green | industrial, desert | Full blocks; hidden from NEI |
| `GalacticraftCore:tile.wallGC:0` | Tin Decoration Wall | fence-rail | yes | urban, industrial | |
| `GalacticraftCore:tile.wallGC:1` | Tin Decoration Wall | fence-rail, industrial-dressing | partly | industrial, space | |
| `GalacticraftCore:tile.wallGC:2` | Moon Rock Wall | fence-rail | yes: light concrete wall | urban | |
| `GalacticraftCore:tile.wallGC:3` | Moon Dungeon Brick Wall | fence-rail, rubble-ruin | yes | urban, ruined | Harvest level 3 |
| `GalacticraftCore:tile.wallGC:4` | Mars Cobblestone Wall | fence-rail | partly: red | desert | |
| `GalacticraftCore:tile.wallGC:5` | Mars Dungeon Brick Wall | off-theme | no: green | space | |
| `GalacticraftCore:tile.tinStairs1` | Tin Decoration Stairs | stairs-slab | yes | urban, industrial | Metadata = facing |
| `GalacticraftCore:tile.tinStairs2` | Tin Decoration Stairs | stairs-slab | partly | industrial, space | |
| `GalacticraftCore:tile.moonStoneStairs` | Moon Rock Stairs | stairs-slab | yes: light-gray stairs | urban | |
| `GalacticraftCore:tile.moonBricksStairs` | Moon Dungeon Brick Stairs | stairs-slab, rubble-ruin | yes | urban, ruined | Harvest level 3 |
| `GalacticraftMars:tile.marsCobblestoneStairs` | Mars Cobblestone Stairs | stairs-slab | partly: red cobble | desert, ruined | |
| `GalacticraftMars:tile.marsDungeonBricksStairs` | Mars Dungeon Brick Stairs | off-theme | no: green | space | Harvest level 3 |

## Planet terrain

Sheet `planet-stone.json`. Terrain is listed because a few of these stones make plausible city materials, but the GT ore hosts among them must never be placed.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalacticraftCore:tile.moonBlock:3` | Moon Dirt | off-theme | no: looks like pale concrete but is dirt (shovel, hardness 0.5) | space | Not a host |
| `GalacticraftCore:tile.moonBlock:4` | Moon Rock | off-theme | no | space | **GT ore host anywhere.** Use the Moon Rock slab, stairs and wall instead |
| `GalacticraftCore:tile.moonBlock:5` | Moon Turf | off-theme | no | space | Metas 5 to 13 are turf growth stages of the same look |
| `GalacticraftMars:tile.mars:4` | Mars Cobblestone | rubble-ruin, wall-accent | partly: rough red cobble | desert, ruined | Not a host (GT's Mars cobble is not tested) |
| `GalacticraftMars:tile.mars:5` | Mars Surface Rock | wall-main | partly: flat red-brown, a terracotta stand-in | desert | Shovel tool. Not a host |
| `GalacticraftMars:tile.mars:6` | Mars Sub-Surface Rock | off-theme | no | space | **GT ore host anywhere** |
| `GalacticraftMars:tile.mars:9` | Mars Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalacticraftMars:tile.asteroidsBlock:0` | Asteroid Rock | street, floor | partly: dark speckled gray, reads as asphalt | urban, industrial | Hardness 3. Not a host |
| `GalacticraftMars:tile.asteroidsBlock:1` | Asteroid Rock | off-theme | no | space | **GT ore host anywhere** |
| `GalacticraftMars:tile.asteroidsBlock:2` | Asteroid Rock | street, floor, rubble-ruin | partly: dark gray, asphalt-like | urban, industrial | Hardness 3. Not a host |
| `GalacticraftMars:tile.denseIce` | Dense Ice | off-theme | no | space | |
| `GalacticraftCore:tile.cheeseBlock` | Block of Cheese | off-theme | no | none | Cake-like model, edible |
| `GalacticraftCore:tile.fallenMeteor` | Fallen Meteor | special, rubble-ruin | partly: a crater centerpiece for a ruined lot | ruined | Tile entity, falls, hardness 40, harvest level 3, drops meteoric iron |

## Lights, walkways and space-base fixtures

Sheet `fixtures.json`. The sheet renders models as cubes; read the shapes from the notes.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalacticraftCore:tile.glowstoneTorch` | Glowstone Torch | light | yes: a torch that never needs oxygen | urban, ruined, industrial | Light 12; metadata = facing like a torch |
| `GalacticraftCore:tile.arclamp` | Arc Lamp | light, industrial-dressing | yes: a mounted floodlight, reads as a street or yard lamp | urban, industrial | Tile entity, light 15, off under redstone. Metadata = mounting side |
| `GalacticraftMars:tile.walkway` | Walkway | floor, industrial-dressing | partly: a gray grated catwalk surface | industrial | Not opaque, hardness 1, no tile entity |
| `GalacticraftMars:tile.walkwayWire` | Walkway (aluminum wire) | industrial-dressing | partly | industrial | Tile entity (aluminum wire) |
| `GalacticraftMars:tile.walkwayOxygenPipe` | Walkway (oxygen pipe) | industrial-dressing | partly | industrial | Tile entity (oxygen pipe) |
| `GalacticraftCore:tile.landingPad:0` | Rocket Launch Pad | special, industrial-dressing | no: spaceport furniture | space | Tile entity, 0.2 blocks high |
| `GalacticraftCore:tile.landingPad:1` | Buggy Fueling Pad | special, industrial-dressing | partly: could pass as a vehicle bay plate | industrial, space | Tile entity |
| `GalacticraftCore:tile.airLockFrame:0` | Air Lock Frame | industrial-dressing | no | space | Tile entity |
| `GalacticraftCore:tile.airLockFrame:1` | Air Lock Controller | industrial-dressing | no | space | Tile entity |
| `GalacticraftCore:tile.enclosed:0`–`:15` (15 entries; meta 4 is unused) | Sealable cables and pipes (HV, oxygen, copper, gold, glass fiber, tin, BuildCraft item, fluid and kinesis pipes, ME cable, aluminum wires) | industrial-dressing | partly: gray panels with a port, read as service ducts | industrial | Tile entity per variant, created from IC2, BuildCraft or AE2 classes. Only the oxygen pipe and aluminum wires are Galacticraft's own. Sheet shows metas 1, 2, 7, 13, 14 |
| `GalacticraftCore:tile.spaceStationBase` | Space Station arrival block | off-theme | no | none | Tile entity, unbreakable |
| `GalacticraftMars:tile.cavernVines` | Cavernous Vines | foliage, off-theme | no: glowing green vines that hurt | space | Damages entities that touch it |

## Not catalogued

- Ores: `tile.gcBlockCore:5`–`:8`, `tile.moonBlock:0`–`:2`, `tile.mars:0`–`:3`, `tile.asteroidsBlock:3`–`:5`. GC's own ore generation is switched off in the pack; GT generates space ores.
- Treasure chests (`tile.treasureChest`, `tile.treasureT2`, `tile.treasureT3`), the Mars dungeon spawner (`tile.mars:10`), machines, solar panels, the NASA workbench, oxygen gear, cargo loaders, telemetry, screens and the beam and telepad blocks: all tile entities with function, not decoration.
- `tile.unlitTorch`, `tile.unlitTorchLit` and the `_Stone` pair: hidden replacements GC swaps in for torches where there is no oxygen.
- Breathable air, bright air, fluids and slime eggs.
- The `tintedGlassPane_*` textures in the Mars assets have no registered block.

## Best variety pools

| Upstream role or palette family | Galacticraft candidates |
|---|---|
| `bricks_standard` `#` main wall | `tile.gcBlockCore:4` (modern panel facade), `tile.moonBlock:14` (dark brick) |
| `X` accent | `tile.gcBlockCore:3`, `tile.gcBlockCore:12`, `tile.mars:8` |
| `$` band / double slab | `slabGCDouble:0`, `slabGCDouble:2`, `slabGCDouble:3` |
| `}` rubble | `tile.moonBlock:14` with `slabGCHalf:3` and `tile.moonBricksStairs` (a dark-brick rubble set like upstream's `blackstone_rubble`) |
| Blackstone building (`building7`) | `tile.moonBlock:14` family as a darker alternative |
| Street and sidewalk `S` / `u` | `tile.asteroidsBlock:0` and `:2` (asphalt look), `slabGCHalf:2` (light slab), `slabGCHalf:0` |
| Pillars `v` | `tile.gcBlockCore:10` (industrial), `tile.wallGC:2` |
| Highway `R` | `tile.asteroidsBlock:0` |
| Light `h` | `tile.arclamp` (industrial streets and yards), `tile.glowstoneTorch` |
| Iron bars `:` / fence `.` | `tile.wallGC:0`, `tile.wallGC:2` |
| Oil rig | `tile.walkway` (deck), `tile.gcBlockCore:11` (cladding), `tile.arclamp` |
| Desert bricks | `tile.mars:5` (flat red-brown), `tile.mars:4` with `slabGCHalf:4` and `tile.marsCobblestoneStairs` (ruined desert) |
| Roofs | `tile.gcBlockCore:9` (copper), `tile.gcBlockCore:11` (corrugated) |
