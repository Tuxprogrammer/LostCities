# Amun-Ra

Family D (a Galacticraft add-on; family C covers only Galacticraft and GalaxySpace). Mod ID `GalacticraftAmunRa`, jar `AmunRa-GC-0.8.14.jar`. Source read: `mods/AmunRa-GC/de/katzenpapst/amunra/block/ARBlocks.java` (lines 330-725), `.../block/BlockBasicMeta.java` (lines 53-135), `.../block/BlockStairsAR.java` (lines 38-47), `.../block/BlockSlabMeta.java` line 169, `assets/amunra/lang/en_US.lang`, `config/GalacticraftAmunRa.cfg` (no block toggles). Sheets: `tools/block-contact-sheet/sheets/amunra/` (previews in `docs/previews/blocks/amunra/`, local only).

## Summary

- **Registry names.** Amun-Ra's meta blocks register under their unlocalized name, which includes the `tile.` prefix: `GalacticraftAmunRa:tile.baseBlockRock`, not `...:baseBlockRock` (`BlockBasicMeta.register()` passes `getUnlocalizedName()`, line 125). Stairs are `GalacticraftAmunRa:tile.<subblock>.stairs` (`BlockStairsAR.java` lines 38-47); slabs `tile.rockSlab`, `tile.woodSlab` with their `...Doubleslab` partners.
- **Counts.** About 43 registry names (34 meta blocks, 7 stairs, 2 chests) and about 96 block+meta entries; most are alien-planet terrain, ores in exotic hosts, mothership machines and engines. **Useful: 14** (the rock and plank sub-blocks below and their stairs and slabs).
- **Look.** A small set of very good urban stones: **basalt brick** (dark gray bricks), **smooth basalt** (a dark framed panel), **Urbolite** (weathered gray concrete with stains, Amun-Ra's "old concrete"), the **aluminum box** (riveted light metal panel), and **olivine basalt** (pale sandy stone). Lumipod planks are a dark brown wood. Everything else is alien (green Virilig wood, purple obsidian sand, glowing coral).
- **Caveats.** No TEs in the useful set. Obsidian brick has hardness 50 and resistance 6000. The falling sub-blocks (`tile.baseFalling`) have gravity. Basalt (`tile.baseBlockRock:1`), red rock (`:3`) and olivine basalt (`:5`) drop their cobblestone. Glowing coral emits light 15 and Lumipod pulp light 12. `tile.baseBlockRock:12` (mothership base block) is unbreakable (hardness -1): never place it.
- **Ore rule.** These are Amun-Ra's own blocks, not GT ore or boulder hosts in the overworld. Amun-Ra's ore blocks (`tile.basaltMultiOre` and similar) are ores and never building blocks.
- **Optional dependency.** A server without Amun-Ra (which needs Galacticraft) loses these. Fallbacks: basalt brick to `minecraft:stonebrick:0` or `etfuturum:deepslate_bricks:0`, smooth basalt to `etfuturum:smooth_stone:0`, Urbolite to `etfuturum:concrete:7`, aluminum box to `minecraft:iron_block`, olivine basalt to `minecraft:sandstone:2`.

## Blocks

### Rock (`GalacticraftAmunRa:tile.baseBlockRock`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalacticraftAmunRa:tile.baseBlockRock:7` | Basalt Brick | `wall-main`, `wall-accent`, `trim-pillar` | yes | `urban-brick`, `urban-stone`, `industrial` | Dark gray brick, crisp mortar lines. A darker stone-brick alternative |
| `GalacticraftAmunRa:tile.baseBlockRock:8` | Smooth Basalt | `wall-accent`, `trim-pillar`, `floor` | yes | `modern`, `urban-stone` | Dark slab with a beveled frame, like polished granite cladding |
| `GalacticraftAmunRa:tile.baseBlockRock:10` | Urbolite | `wall-main`, `street`, `sidewalk`, `rubble-ruin` | yes | `modern`, `ruin`, `industrial` | Weathered stained concrete. Hardness 3, harvest level 3 (needs a diamond-tier pickaxe in vanilla terms) |
| `GalacticraftAmunRa:tile.baseBlockRock:6` | Aluminum Box | `wall-accent`, `trim-pillar` | yes | `industrial`, `modern` | Riveted pale metal panel; very soft (hardness 1) |
| `GalacticraftAmunRa:tile.baseBlockRock:5` | Olivine Basalt | `wall-main` | partly | `desert`, `urban-stone` | Pale sandy stone; drops its cobblestone (`:4`) |
| `GalacticraftAmunRa:tile.baseBlockRock:4` | Olivine Basalt Cobblestone | `rubble-ruin` | partly | `desert`, `ruin` | |
| `GalacticraftAmunRa:tile.baseBlockRock:1` | Basalt | `wall-main` | partly | `urban-stone`, `ruin` | Very dark, nearly flat stone; drops basalt cobblestone (`:0`) |
| `GalacticraftAmunRa:tile.baseBlockRock:0` | Basalt Cobblestone | `rubble-ruin` | partly | `ruin` | Black cobblestone |
| `GalacticraftAmunRa:tile.baseBlockRock:3` | Red Sandstone (rock) | `wall-accent` | partly | `desert` | Rough orange-red stone; drops red cobblestone (`:2`) |
| `GalacticraftAmunRa:tile.baseBlockRock:2` | Red Cobblestone | `rubble-ruin` | partly | `desert`, `ruin` | |
| `GalacticraftAmunRa:tile.baseBlockRock:9` | Obsidian Brick | `special` | partly | `fantasy` | Purple-black brick, hardness 50, resistance 6000 |
| `GalacticraftAmunRa:tile.baseBlockRock:11` | Block of Uranium (deco) | `off-theme` (glowing green) | no | `alien` | |
| `GalacticraftAmunRa:tile.baseBlockRock:13` | Workbench | `off-theme` | no | | Crafting block |
| `GalacticraftAmunRa:tile.baseBlockRock:14` | Dark Matter | `off-theme` | no | `alien` | Pure black, hardness 50 |
| `GalacticraftAmunRa:tile.baseBlockRock:12` | Mothership Base Block | never | no | | Unbreakable |

### Wood, stairs, slabs

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalacticraftAmunRa:tile.wood1:3` | Lumipod Planks | `floor`, `wall-accent` | yes | `rustic`, `interior` | Dark brown planks, like dark oak |
| `GalacticraftAmunRa:tile.wood1:2` | Virilig Planks | `off-theme` (bright green) | no | `alien` | |
| `GalacticraftAmunRa:tile.wood1:0` | Lumipod Bark | `wall-accent` | partly | `rustic` | Dark bark texture all round |
| `GalacticraftAmunRa:tile.wood1:1` | Lumipod Pulp | `off-theme` | no | `alien` | Light 12 |
| `GalacticraftAmunRa:tile.basaltbrick.stairs` (meta: stair facing) | Basalt Brick Stairs | `stairs-slab` | yes | `urban-brick` | |
| `GalacticraftAmunRa:tile.smoothbasalt.stairs` | Smooth Basalt Stairs | `stairs-slab` | yes | `modern` | |
| `GalacticraftAmunRa:tile.alucrate.stairs` | Aluminum Stairs | `stairs-slab` | yes | `industrial` | |
| `GalacticraftAmunRa:tile.basalt.stairs` | Basalt Stairs | `stairs-slab` | partly | `urban-stone` | |
| `GalacticraftAmunRa:tile.podPlanks.stairs` | Lumipod Stairs | `stairs-slab` | yes | `rustic` | |
| `GalacticraftAmunRa:tile.obsidianbrick.stairs`, `tile.methanePlanks.stairs` | Obsidian Brick, Virilig Stairs | `off-theme` | no | | |
| `GalacticraftAmunRa:tile.rockSlab:0-4` (+8 top) | Basalt, Basalt Brick, Smooth Basalt, Obsidian Brick, Aluminum slabs | `stairs-slab` | yes (0-2, 4) | `urban-stone`, `industrial` | Double slabs: `tile.rockDoubleslab` |
| `GalacticraftAmunRa:tile.woodSlab:0-1` (+8 top) | Lumipod, Virilig slabs | `stairs-slab` | partly (0) | `rustic` | Double: `tile.woodDoubleslab` |

### Terrain, plants, machines (off-theme)

Methane dirt and grasses (`tile.baseBlockGround`, `tile.baseGrass`), falling obsidian sand, obsidian gravel and basalt regolith (`tile.baseFalling`, gravity), glowing coral (`tile.baseBlockCrystal:0`, light 15), Virilig logs, leaves, saplings and tall grass, ores in basalt, obsidian, hardened clay, Urbolite and asteroid hosts, the Osiris boss spawner (unbreakable), mothership controllers, engines and boosters, the gravity and scale machines, the shuttle dock, hydroponics and the two chests: all `off-theme` (alien terrain, machines, ores).

## Best variety pools

- **Dark brick walls:** `tile.baseBlockRock:7` (with `minecraft:stonebrick`, `etfuturum:deepslate_bricks`).
- **Modern cladding and lobby floors:** `tile.baseBlockRock:8`, `tile.rockSlab:2`, `tile.smoothbasalt.stairs`.
- **Concrete, weathered:** `tile.baseBlockRock:10` (Urbolite), mixed with `etfuturum:concrete:7` and `:8`.
- **Industrial metal panels:** `tile.baseBlockRock:6`, `tile.rockSlab:4`, `tile.alucrate.stairs`.
- **Rubble:** basalt cobblestone `:0` among other rubble.
- **Dark wood floors:** `tile.wood1:3`, `tile.woodSlab:0`, `tile.podPlanks.stairs`.

## Worldgen notes

- All useful blocks are plain metadata blocks: no TE, no ticking, no light, no gravity.
- Urbolite needs harvest level 3: players without a diamond-tier pickaxe cannot mine it, which matters early in GTNH. Keep it to accents or ruins, or accept it as a feature.
