# Decorative block catalog

The entry point to the catalog of every decorative and construction block GTNH 2.9.0 enables, judged against upstream Lost Cities' look ([decision log](../../../design/decision-log.md) D25). Upstream's cities feel synthetic partly because every building is made of the same handful of blocks; the catalog exists so palettes can draw weighted variety for each role instead of one stand-in per missing block. The [D19 block mapping](../../00-pack-integration/block-mapping.md) stays the baseline and the fallback when a mod is absent. D28 sets the rules the [variety pools](#variety-pools) below follow.

Status: the pools are *live*: M11 turned their candidates into the [palette variety](../asset-format.md#palette-variety) ([0011](../../../design/0011-palette-variety.md)), option palettes a building draws per role, and `tools/asset-convert/variety.py` names the candidates it uses as these tables do. Weights are first judgments, tuned at the beta review. The D28 rules they apply are accepted. City styles that would use distinct pools wait until after beta, with the GTNH-themed buildings ([ROADMAP](../../../ROADMAP.md)).

## How to read the catalog

- **One file per mod or mod family**, listed in [Mods](#mods). [COVERAGE.md](COVERAGE.md) assigns all 243 of the pack's mods to a family (A Chisel, B the big decorative mods and vanilla, C GregTech and the tech mods, D the rest) and lists the 54 mods that were checked and have nothing useful.
- **Each file** has a summary (counts, fit, flags), block tables, and a closing **best variety pools** section with that mod's candidates per upstream role. This index merges those sections across mods into one pool per role.
- **Block tables** name each block by its exact registry name and metadata (`modid:name:meta`), as the block-name map must resolve it ([Prerequisites](#prerequisites-for-the-asset-system)). Columns:
  - *Roles* use the [role taxonomy](#role-taxonomy).
  - *Fits upstream?* asks whether the block belongs in upstream's urban 20th-century city (brick, stone brick, concrete, quartz, glass, iron, wood trim, ruined and overgrown variants): *yes* means today's palettes could use it, *partly* means it suits a themed building or district, *no* means off-theme.
  - *Styles* are upstream's `standard`, `border` and `desert` plus styles the blocks suggest (`industrial`, `modern`, `ancient`, `ruin`, `overgrown` and others).
  - *Notes* carry gameplay flags: tile entity, light, gravity, hardness, spawning, movement, GT ore and boulder hosts.
- **Upstream's palettes, characters and roles** are in [catalog-palettes.md](../../../original-spec/05-assets/catalog-palettes.md), its city styles in [catalog.md](../../../original-spec/05-assets/catalog.md), and the 128 blocks it uses in [block-inventory.md](../../../original-spec/05-assets/block-inventory.md).

### Role taxonomy

| Role | What it covers | Upstream characters |
|---|---|---|
| `wall-main` | The main wall block of a building | `#` |
| `wall-accent` | Accent wall blocks, bands, plinths | `X`, `$`, `9` |
| `trim-pillar` | Columns, pilasters, window-side columns, beams | `@`, `v`, `y`, `x` |
| `floor` | Interior floors (upstream uses its street block) | `S`, `u` in buildings |
| `ceiling` | Ceilings (upstream uses its wall or floor block) | none of its own |
| `roof` | Pitched or flat roofs (upstream's roofs are flat walls and slabs) | none of its own |
| `glass` | Full glass blocks | `+`, `a`, `Y`, `Z` |
| `pane` | Glass panes and thin window fillings | `` ` ``, `a` |
| `street` | Street and highway surfaces, lane markings | `S`, `R`, `B`, `l`, `q` |
| `sidewalk` | Sidewalks and plazas | none of its own |
| `stairs-slab` | Stairs and slabs | `A t z c d`, `/`, `(`, `)`, `_`, `=` |
| `rubble-ruin` | Rubble and ruin variants | `}`, damage blocks |
| `light` | Light sources | `h`, `T`, `g` |
| `fence-rail` | Fences, railings, walls, bars | `.`, `w`, `:`, `{` |
| `door-trap` | Doors, trapdoors, hatches | code-placed doors, part-local doors |
| `furniture-deco` | Furniture and fittings | `F`, `L`, `C`, `*`, `-`, `l` |
| `foliage` | Plants, planters, overgrowth | `p`, `K`, `G`, `D` |
| `industrial-dressing` | Machinery-look blocks (family C files) | oil rig, future industrial buildings |
| `special` | Gameplay blocks that need a decision (spawners, ladders, hazards) | `1`, `4`, `l` |
| `off-theme` | Does not fit a city | none |

### Contact sheets

[`tools/block-contact-sheet/`](../../../../tools/block-contact-sheet/README.md) renders a review sheet: a grid of small isometric cubes made from each block's textures, read from the mod jars in the reference pack, labeled with registry name and metadata. The JSON entry lists are committed in `tools/block-contact-sheet/sheets/<mod>/`; the PNGs render into `docs/previews/blocks/<mod>/`, which is git-ignored. Rebuild one with:

```sh
python3 -I tools/block-contact-sheet/contact_sheet.py tools/block-contact-sheet/sheets/chisel/marble.json \
    --out docs/previews/blocks/chisel/marble.png [--cube 96] [--cols 8]
```

Sheets show no lighting, connected textures, models or in-game tints (the `tint` field approximates runtime colors), so the screenshot reviews in the real pack have the last word on looks.

## Mods

*Entries* counts what each file catalogs as enabled in the pack: block+meta entries, rows or variants as the file counts them, with "useful" where a family D file counts hand-picked entries instead of fit. *Fit* is yes / partly / no where the file counts it, otherwise the file's overall verdict.

| Mod | Catalog | Entries | Fit (yes / partly / no) | Standout blocks |
|---|---|---|---|---|
| Vanilla 1.7.10 | [vanilla.md](vanilla.md) | 274 rows | 159 / 79 / 36 | Unused colors of stained glass and terracotta, chiseled stone brick and quartz, plain `hardened_clay` (not a boulder host) |
| Chisel | [chisel.md](chisel.md) | 2,489 variants, all enabled | 873 / 863 / 753 | Road and road lines, patterned terracotta in upstream's colors (not boulder hosts), hempcrete, stone-brick panels, marble pillars, laboratory blocks, framed glass |
| ChiselTones | [chiseltones.md](chiseltones.md) | 0 (adds Ztones carving groups only) | none | none |
| Ztones | [ztones.md](ztones.md) | 551 of 567 | 118 / 195 / 238 | Matte color sets (Agon, Iszm, Myst, Cray), Lave and Ztyl panels, Ztone Tile paving, Glaxx tinted glass, flush flat lamps |
| Et Futurum Requiem | [etfuturum.md](etfuturum.md) | 478 rows (340 of 532 blocks registered) | 169 / 249 / 60 | Concrete, smooth stone and quartz, deepslate, tuff and blackstone bricks, copper doors and trapdoors, lantern, chain, stripped logs |
| Botania | [botania.md](botania.md) | 501 block+meta | 152 / 212 / 62 | Sea lantern, 1.8 stones and bricks, smokey and sunny quartz, roof tile, Portuguese pavement, managlass |
| Thaumcraft | [thaumcraft.md](thaumcraft.md) | 58 rows | 15 / 27 / 16 | Arcane stone bricks, ancient rock, greatwood, old urns and crates as ruin loot |
| Thaumcraft add-ons | [thaumic-addons.md](thaumic-addons.md) | 108 rows | 22 / 36 / 50 | Thaumic Bases ancient bricks and cobblestone, Thaumic Tinkerer smokey quartz, Automagy glowing amber, Salis Arcana planks |
| GregTech 5 Unofficial | [gregtech.md](gregtech.md) | 247 entries in 35 blocks | 58 / 111 / 78 | Tinted industrial glass, GT stones (boulder hosts), GT concrete (speed), frame boxes and sheetmetal; casings [reserved](#reserved-for-the-gtnh-themed-buildings) |
| bartworks | [bartworks.md](bartworks.md) | 23 | 1 / 3 / 19 | Borosilicate glass |
| GT++ | [gtplusplus.md](gtplusplus.md) | 36 | 3 / 13 / 20 | Casings only ([reserved](#reserved-for-the-gtnh-themed-buildings)) |
| IndustrialCraft 2 | [ic2.md](ic2.md) | 43 variants | 13 / 19 / 11 | IC2 basalt (not a GT host), iron fence, scaffolds for ruins, unlit luminator ceiling panel |
| Railcraft | [railcraft.md](railcraft.md) | 252 variants | 107 / 89 / 56 | Quarried, abyssal, bleached bone and sandy brick families, strengthened glass, 16 lanterns, posts and walls |
| NewHorizonsCoreMod | [dreamcraft.md](dreamcraft.md) | 50 | 3 / 17 / 30 | Steel, aluminum and stainless steel bars |
| ProjectRed | [projectred.md](projectred.md) | 46 | 13 / 24 / 9 | Marble, basalt bricks and their walls; lamps (tile entity) |
| Applied Energistics 2 | [ae2.md](ae2.md) | 37 | 26 / 7 / 4 | Sky stone bricks, certus quartz, quartz glass, vibrant quartz glass |
| EnderIO | [enderio.md](enderio.md) | 47 | 13 / 20 / 14 | Fused quartz and dark clear glass (tile entities), enlightened glass, dark steel bars and ladder |
| Tinkers' Construct | [tinkers.md](tinkers.md) | 212 | 58 / 97 / 57 | Polished stone brick, sandstone brick, clear glass and panes, stone torch and ladder |
| Extra Utilities | [extrautilities.md](extrautilities.md) | 296 | 36 / 140 / 120 | Edged stone bricks, border stone, colored stone and quartz, glass variants, Lapis Caelestis panels |
| Galacticraft | [galacticraft.md](galacticraft.md) | 72 | 16 / 39 / 17 | Tin decoration panel, moon rock slab and wall, moon dungeon brick, asteroid rock as asphalt, arc lamp |
| GalaxySpace | [galaxyspace.md](galaxyspace.md) | 108 | 13 / 41 / 54 | Future glass (reverse color order), dungeon bricks, space glowstones |
| Amun-Ra | [amunra.md](amunra.md) | about 96, 14 useful | partly | Basalt brick, smooth basalt, weathered concrete |
| ArchitectureCraft | [architecturecraft.md](architecturecraft.md) | 106 shapes, about 70 useful | yes, after a new entry kind | Cornices, balustrades, columns, roof slopes (shape and material in NBT) |
| Battlegear 2 | [battlegear2.md](battlegear2.md) | 7 | partly | Iron flag pole |
| BiblioCraft and BiblioWoods | [bibliocraft.md](bibliocraft.md) | about 246, about 25 types useful | yes, tile entities | Desks, bookcases, tables, lamps and lanterns |
| Binnie's Mods | [binnie.md](binnie.md) | about 200 useful | yes, tile entities | Botany ceramic bricks and tiles, ExtraTrees doors and leaded glass |
| Biomes O' Plenty | [biomesoplenty.md](biomesoplenty.md) | about 280, about 140 useful | yes | Polished shale and siltstone, mud bricks, nine plank tones, ivy and moss |
| Blood Magic, Blood Arsenal | [bloodmagic.md](bloodmagic.md) | about 82, 5 partly useful | partly | Bloodstone brick at most |
| BuildCraft | [buildcraft.md](buildcraft.md) | about 40, 2 marginal | no | Tanks as factory clutter |
| Carpenter's Blocks | [carpentersblocks.md](carpentersblocks.md) | 18 names, about 8 useful | partly, tile entities | Covered slopes, glass doors, garage doors |
| Catwalks 2 | [catwalks.md](catwalks.md) | 46 names, about 44 useful | yes | Catwalks, caged ladders (fire escapes), support columns, lit variants |
| CookingForBlockheads | [cookingforblockheads.md](cookingforblockheads.md) | 26 | yes | 16 checkerboard kitchen floors (no tile entity), kitchen furniture |
| Fether | [fether.md](fether.md) | about 30, 3 useful | partly | Crimson planks, glow flower |
| FloodLights | [floodlights.md](floodlights.md) | 8, 3 useful | partly | Unpowered ceiling panels |
| Forestry | [forestry.md](forestry.md) | about 410, about 150 useful | partly, tile entities | 29 woods, fireproof; mailbox |
| Hardcore Ender Expansion | [hardcoreenderexpansion.md](hardcoreenderexpansion.md) | about 75, 16 useful | partly | Ravaged brick with damage states, slab, stairs, fence |
| HarvestCraft | [harvestcraft.md](harvestcraft.md) | 187 names, about 45 useful | partly | 16 candles (light 14, no tile entity), sinks, market stall, wild gardens |
| Iron Chests | [ironchest.md](ironchest.md) | 9, 4 useful | partly | Iron, copper and steel chests |
| JABBA | [jabba.md](jabba.md) | 1 | partly | Barrel |
| LittleTiles | [littletiles.md](littletiles.md) | 11, 10 useful | partly | Plaster, tile and brick finish blocks (hardness 0) |
| MalisisDoors | [malisisdoors.md](malisisdoors.md) | about 56, about 40 useful | yes | Wood-variant doors, glass sliding, jail, lab, factory and garage doors, iron trapdoor, curtains |
| ModernMarkings | [modernmarkings.md](modernmarkings.md) | 170, about 150 useful | yes | Lane lines, crossings, arrows, parking squares, wall signs |
| Natura | [natura.md](natura.md) | about 245, about 185 useful | yes | Seven metadata-only wood doors, 13 bookshelves and workbenches, fences, topiary grass |
| Nuclear Control | [nuclearcontrol.md](nuclearcontrol.md) | 16, about 7 useful | partly | White and orange panel lamps |
| OpenBlocks | [openblocks.md](openblocks.md) | about 51, about 7 useful | partly | Canvas, path, ladder |
| OpenComputers and add-ons | [opencomputers.md](opencomputers.md) | 72 registry names, 17 useful | partly, tile entities | Cases, screens, racks, file cabinet, chamelium (reverse color order) |
| OpenModularTurrets | [openmodularturrets.md](openmodularturrets.md) | 36 names, 6 useful | yes, industrial | Hard walls tiers 1 to 3, chain-link fences tiers 1 and 2 |
| Storage Drawers | [storagedrawers.md](storagedrawers.md) | about 507, about 90 useful | yes | 83 framed-plank trims with no tile entity |
| Twilight Forest | [twilightforest.md](twilightforest.md) | about 264, about 95 useful | partly | Underbrick, eight woods with doors and trapdoors, firefly jar |
| VendingMachine | [vendingmachine.md](vendingmachine.md) | 2 names, 1 useful | partly | A GT multiblock vending machine |
| Village Names | [villagenames.md](villagenames.md) | 4, 1 or 2 useful | partly | Lunarin iron brick |
| Witchery | [witchery.md](witchery.md) | about 200, about 30 useful | partly | Shaded glass, alder, hawthorn and rowan woods |

## Variety pools

One pool per upstream palette family or character role in [catalog-palettes.md](../../../original-spec/05-assets/catalog-palettes.md), drawn across all 52 catalogs. 63 pools, in seven groups:

| Group | Pools |
|---|---|
| [Brick and stone walls](#brick-and-stone-walls) | [stone-brick main wall](#stone-brick-main-wall), [accent](#accent), [band](#band), [stone-brick rubble](#stone-brick-rubble), [red-brick main wall](#red-brick-main-wall), [red-brick rubble](#red-brick-rubble), [cyan terracotta](#cyan-terracotta-walls), [gray terracotta](#gray-terracotta-walls), [light gray terracotta](#light-gray-terracotta-walls), [terracotta bands](#terracotta-bands), [desert main wall](#desert-main-wall), [desert accent and trim](#desert-accent-and-trim), [bleached desert](#bleached-desert-walls), [taupe desert](#taupe-desert-walls), [desert band](#desert-band), [quartz main wall](#quartz-main-wall), [quartz rubble](#quartz-rubble), [deepslate building](#deepslate-building), [blackstone building](#blackstone-building), [stone-and-brick building](#stone-and-brick-building) |
| [Quartz and glass](#quartz-and-glass) | [quartz block and column](#quartz-block-and-column), [quartz stairs](#quartz-stairs), [clear glass blocks](#clear-glass-blocks), [tinted glass blocks](#tinted-glass-blocks), [clear panes](#clear-panes), [tinted panes](#tinted-panes), [white and city-sphere glass](#white-and-city-sphere-glass) |
| [Streets, highways and fittings](#streets-highways-and-fittings) | [street and floor slab](#street-and-floor-slab), [primary roads](#primary-roads), [sidewalks and plazas](#sidewalks-and-plazas), [highway surface](#highway-surface), [street variant brick](#street-variant-brick), [pillars and beds](#pillars-and-beds), [bottom slabs](#bottom-slabs), [walls](#walls), [iron bars](#iron-bars), [stone-brick stairs](#stone-brick-stairs), [stone stairs](#stone-stairs), [street markings](#street-markings) |
| [Interiors and roofs](#interiors-and-roofs) | [interior floors](#interior-floors), [ceilings](#ceilings), [roofs](#roofs), [doors](#doors), [trapdoors and hatches](#trapdoors-and-hatches), [wood trim](#wood-trim) |
| [Lights and fittings](#lights-and-fittings) | [glowstone](#glowstone), [torches](#torches), [hanging lights](#hanging-lights), [furniture](#furniture), [bookshelves](#bookshelves), [chests and barrels](#chests-and-barrels), [ladders](#ladders), [pressure plates](#pressure-plates), [fences](#fences), [chains and cobwebs](#chains-and-cobwebs), [treasure](#treasure), [colored concrete](#colored-concrete) |
| [Plants, ruins and set pieces](#plants-ruins-and-set-pieces) | [flowers and planters](#flowers-and-planters), [overgrowth](#overgrowth), [ruin dressing](#ruin-dressing), [oil rig](#oil-rig), [cabin](#cabin), [gas station and restaurant parking](#gas-station-and-restaurant-parking) |
| [Decorative metals (D29)](#decorative-metals-d29) | metal accents behind one flag per metal |

Characters that keep upstream's block with no pool: `~` and `b` (structure void), `1` and `4` (spawners, [loot-and-mobs.md](../../00-pack-integration/loot-and-mobs.md)), `W` (water), the `rails` palette (vanilla `rail` and `golden_rail`; Railcraft tracks need their own placement call, see [Prerequisites](#prerequisites-for-the-asset-system)), and `@` in `glass_side_variant_bricks` and `_glass`, which copy `#` and `a`.

### How the pools are written

- **The baseline comes first**: upstream's block as [block-mapping.md](../../00-pack-integration/block-mapping.md) maps it to 1.7.10. Every other candidate falls back to it, or to the block named in *Fallback*, when its mod is absent. Pools are proposals; the baseline is the D19 mapping.
- **Weights are per building**, relative within the pool and summing to 100: the share of buildings whose palette maps that character to that candidate. A wall is one material, not a per-block mix of mods; each candidate keeps its own small random mix inside a building (cracked and mossy forms, the way upstream's `stonebrick` variant mixes them) where the row names one. Upstream's own random lists (`!`, `F`, `p`, `Z`, `9`) stay per block. The baseline keeps a large share so cities still read as upstream's.
- **Flags** (blank means none):

| Flag | Meaning |
|---|---|
| SP | Spawn-proof: no natural mob spawns on it (every Chisel `BlockCarvable`, every Botania block, Railcraft decorative blocks, GT casings, the dressed GT stones and concrete, Natura topiary grass, Thaumcraft travel and warding paving) |
| SPD | Speed block: walking on it is faster (Chisel `concrete`, `road`, `road2`; every GT concrete meta; every Tinkers' brownstone block and slab; Thaumcraft travel paving) |
| BH | GT boulder host: GT's boulders can overwrite it (stained hardened clay, every GT stone meta) |
| TE | Tile entity: the palette entry may need tile-entity data |
| L*n* | Emits light level *n* |
| G | Falls like sand |
| H | Slow to mine: hardness 10 to 50, or a diamond-level pick |
| XM | Extended metadata above 15 (EndlessIDs) |
| T | Tinted at render time; the contact sheet only approximates it |
| DM | Decorative metal: placed only while its D29 metal flag is on, as a capped accent ([decorative metals](#decorative-metals-d29)); otherwise the fallback |

#### D28 as applied here

1. **Spawn-proof blocks** go freely in walls, trims and roofs. **Floors** (`S` and `u` in buildings, the [interior floors](#interior-floors) pool) draw from a weighted mix that keeps spawnable floor in at least 70 of every 100 buildings by default; kept-up or lit buildings may be fully spawn-proof, and the share is tunable per city style. Bottom slabs (`_`, `=`) take no spawns in vanilla anyway.
2. **Speed blocks** are walked on only in the [highway surface](#highway-surface) and [primary roads](#primary-roads) pools, as weighted options judged at the M06 screenshots. Elsewhere they may stand in walls and trims (GT concrete bricks and the brownstone blocks in the brick and desert pools, marked SPD), never in floors, roofs, sidewalks, side streets, stairs or rubble. A wall of speed blocks brings a non-speed rubble set, and ruin damage swaps a speed block for a normal block, because roofs and ruins turn walls into walking surfaces.
3. **GT stones** (`gregtech:gt.blockgranites` and `gt.blockstones`, every meta except the raw ore hosts 0 and 8) join on terracotta's terms (D18): they are boulder hosts, kept, and covered by the restore pass if the alpha screenshots show boulder damage ([ore-veins.md](../../00-pack-integration/ore-veins.md)). Black and red granite need a diamond-level pick, so they stay accents.
4. **GT casings** wait for the GTNH-themed buildings ([below](#reserved-for-the-gtnh-themed-buildings)); machine hulls are never placed.
5. **MalisisDoors** supplies every wood door and the iron trapdoor ([doors](#doors)).
6. **Tile entities** are allowed in palette entries. Bulk variety prefers blocks whose look is in metadata, so tile-entity candidates carry low weights and minority roles.
7. **Never placed**: the [never-place list](#never-placed), which exempts vanilla spawners from the hardness rule. D29 relaxes it for metals: each metal at or below EV that a city can use comes back as capped accents behind its own flag ([decorative metals](#decorative-metals-d29)).

### Brick and stone walls

#### Stone-brick main wall

`bricks_standard` `#`, variant `stonebrick`. Also the default for `v`, `y` and `x` ([pillars and beds](#pillars-and-beds)).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: variant `stonebrick` → `minecraft:stonebrick:0` 111, `:2` 9, `:1` 8 of 128 | 40 | | | Upstream |
| `chisel:stonebricksmooth:4`, `:5` (small, wide), with `:6`, `:7` in the cracked slot | 8 | baseline | SP | Same stone, new bond |
| `Railcraft:brick.quarried:0`, `:1` | 8 | baseline | SP, H | Light gray brick family with cobble and ornate forms |
| `etfuturum:deepslate_bricks:0`, with `:1` cracked | 6 | baseline | | Dark gray brick; not the `deepslate` ore host |
| `Botania:stone:8`, `:10` (andesite, diorite bricks) | 6 | baseline | SP | Gray and white brick |
| `etfuturum:tuff:2` (tuff bricks) | 4 | baseline | | Warm gray brick; `tuff:0` is the ore host |
| `TConstruct:decoration.multibrick:3` (polished stone brick) | 4 | baseline | | Light gray brick |
| `Thaumcraft:blockCosmeticSolid:7` (arcane stone bricks) | 4 | baseline | | Old gray brick |
| `chisel:limestone:5`, `chisel:marble:1` | 4 | baseline | SP | Pale civic brick |
| `gregtech:gt.blockstones:11` (basalt bricks), `:3` (marble bricks), cracked `:12`, `:4` in the cracked slot | 4 | baseline | BH | GT's dark and pale bricks |
| `ExtraUtilities:decorativeBlock1:0` (edged stone bricks) | 3 | baseline | | Framed brick |
| `Ztones:tile.ztylBlock:0`, `:12` | 3 | baseline | | Gray framed panel |
| `appliedenergistics2:tile.BlockSkyStone:2` (sky stone brick) | 2 | `etfuturum:deepslate_bricks:0` | H | Near-black brick |
| `ProjRed\|Exploration:projectred.exploration.stone:4` (basalt brick) | 2 | `etfuturum:deepslate_bricks:0` | | Dark brick |
| `GalacticraftAmunRa:tile.baseBlockRock:7` (basalt brick) | 2 | `etfuturum:deepslate_bricks:0` | | Dark brick, crisp mortar |

#### Accent

`X` in `bricks_standard`, `bricks_border` and `quartzbricks_border`; upstream's infested stone bricks, mapped to the plain block (D19).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stonebrick:0` | 35 | | | Upstream (no silverfish) |
| `minecraft:stonebrick:3` (chiseled) | 10 | baseline | | Vanilla variety upstream never used |
| `chisel:stonebricksmooth:9`, `:10`, `:13`, `:14` | 8 | baseline | SP | Ornate and sunken panels |
| `Railcraft:brick.quarried:3` (ornate), `Railcraft:brick.abyssal:2` | 6 | baseline | SP, H | Carved band, dark band |
| `etfuturum:smooth_stone:0` | 6 | `minecraft:double_stone_slab:8` | | Plain band |
| `etfuturum:deepslate_bricks:4`, `etfuturum:tuff:3`, `:4` | 5 | baseline | | Chiseled dark stone |
| `Botania:stone:12`, `:14`, `Botania:quartzTypeDark:1` | 5 | baseline | SP | Chiseled brick, black quartz |
| `chisel:marble:2`, `:4`, `chisel:limestone:11`, `:12` | 5 | baseline | SP | Pale panels |
| `ThaumicTinkerer:darkQuartz:0`, `:1` | 4 | `minecraft:quartz_block:0` | | Black polished trim |
| `Ztones:tile.zorgBlock:0`, `:15`, `Ztones:tile.mintBlock:8` | 4 | baseline | | Framed slabs |
| `chisel:nucrete:1`, `:9` | 3 | baseline | SP | Concrete panels |
| `ExtraUtilities:decorativeBlock1:4`, `:7` (border stone) | 3 | baseline | | Framed band |
| `gregtech:gt.blockgranites:6`, `gregtech:gt.blockstones:14` (chiseled black granite, basalt) | 3 | baseline | BH, H | GT accents |
| `TConstruct:decoration.multibrickfancy:3` | 3 | baseline | | Gray panel |

#### Band

`$` in `bricks_standard`, `bricks_border` and `quartzbricks_border`; smooth stone double slab.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:double_stone_slab:0` | 40 | | | Upstream |
| `etfuturum:smooth_stone:0` | 10 | `minecraft:double_stone_slab:8` | | Seamless band |
| `chisel:stonebricksmooth:12` (stone panel) | 8 | baseline | SP | Flat panel |
| `chisel:nucrete:3`, `:11` (dark, light smooth concrete) | 6 | baseline | SP | Concrete band |
| `etfuturum:concrete:7`, `:8` | 6 | `minecraft:double_stone_slab:0` | | Gray concrete band |
| `minecraft:double_stone_slab:5` (stone brick double slab) | 5 | baseline | | Vanilla |
| `GalacticraftCore:slabGCDouble:2` (moon rock double slab) | 4 | baseline | | Light concrete look; not the moon rock ore host |
| `Railcraft:brick.abyssal:2` | 4 | baseline | SP, H | Dark band |
| `chisel:limestone:7` | 4 | baseline | SP | Smooth pale band |
| `gregtech:gt.blockstones:7`, `gregtech:gt.blockgranites:7` (smooth marble, black granite) | 4 | baseline | BH, H | Polished band |
| `appliedenergistics2:tile.SkyStoneBlockSlabBlock.double:0` | 3 | baseline | H | Dark band |
| `ThaumicTinkerer:darkQuartzSlabFull:0` | 3 | baseline | | Black band |
| `Ztones:tile.ztylBlock:5` | 3 | baseline | | Gray band |

#### Stone-brick rubble

`}` in `bricks_standard`, variant `stonebrick_rubble`. Rubble follows the building's main wall: each wall candidate brings its own rubble set, so a ruin is made of what stood there. Weights inside a set follow upstream's variant (about 70 % full blocks, the rest cracked, slabs and stairs).

| Main wall candidate | Rubble set | Flags |
|---|---|---|
| Baseline | `minecraft:stonebrick:0`, `:1`, `:2`, `minecraft:stone_slab:5`, `minecraft:stone_brick_stairs` (upstream) | |
| Chisel stone bricks | `chisel:stonebricksmooth:6`, `:7`, `chisel:cobblestone:9`, the baseline slabs and stairs | SP |
| Railcraft quarried | `Railcraft:brick.quarried:5` (cobble), the baseline slabs and stairs (Railcraft's own are tile entities) | SP |
| Deepslate or tuff bricks | `etfuturum:deepslate_bricks:1`, `etfuturum:cobbled_deepslate:0`, `etfuturum:deepslate_brick_slab:0`, `etfuturum:deepslate_brick_stairs`; `etfuturum:tuff_slab`, `etfuturum:tuff_brick_stairs` | |
| Botania stones | `Botania:stone:0`, `:2`, `Botania:stone8Slab`, `Botania:stone8Stairs` | SP |
| GT stones | `gregtech:gt.blockstones:12`, `:9`, `:10` (cracked, cobble, mossy cobble) for basalt; `gregtech:gt.blockstones:4`, `:1` (cracked, cobble) for marble | BH |
| Any | `chisel:limestone:6`, `chisel:marble:10` (marble walls), `chisel:brutalism:5`, `:6` (weathered), `thaumicbases:oldCobble:0`, `minecraft:mossy_cobblestone`, `minecraft:gravel` sparingly (G) | |

#### Red-brick main wall

`bricks_border` `#`, variant `bricks`: `minecraft:brick_block` 10, red terracotta 118 of 128.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: variant `bricks` → `minecraft:brick_block`, `minecraft:stained_hardened_clay:14` | 37 | | BH | Upstream |
| `chisel:hardened_clay_red:3` to `:8` (red terracotta bricks) | 12 | baseline | SP | Upstream's color as brick, not a boulder host |
| `chisel:brickCustom:1`, `:2`, `:3`, `:9`, `:10` (large, mortarless, varied, brown, modern) | 10 | `minecraft:brick_block` | SP | Brick bonds |
| `chisel:hardened_clay:3` to `:8` (natural terracotta bricks) | 5 | `minecraft:hardened_clay` | SP | Tan brick |
| `thaumicbases:oldBrick:0` | 5 | `minecraft:brick_block` | | Beta-era brick: a second brick look |
| `Botany:ceramicBrick` brick pattern, brown, sienna or maroon (`meta` NBT) | 5 | `minecraft:brick_block` | TE, T | New brick colors |
| `HardcoreEnderExpansion:ravaged_brick:0` | 4 | `minecraft:brick_block` | | Crimson brick with damage states |
| `TwilightForest:tile.TFUnderBrick:0` | 4 | `minecraft:brick_block` | | Second red-brick shade with damage states |
| `gregtech:gt.blockgranites:11` (red granite bricks) | 4 | `minecraft:brick_block` | BH, H | Reads as red brick |
| `ExtraUtilities:color_brick:12`, `:14` | 3 | `minecraft:brick_block` | | Brown and red brick |
| `Railcraft:brick.bloodstained:0` | 3 | `minecraft:brick_block` | SP, H | Dark red brick |
| `chisel:brickCustom:11`, `:12` (white, black) | 3 | `minecraft:brick_block` | SP | Contrast buildings |
| `GalaxySpace:ioblocks:7` (Io dungeon top) | 2 | `minecraft:brick_block` | | Red-brown quarry tile; not a host |
| `gregtech:gt.blockconcretes:3` (dark concrete bricks) | 3 | `minecraft:brick_block` | SPD, SP | Brown brick; walls only (D28 rule 3) |

GT's Coke Oven Bricks and Firebricks fit this family too, but they are casings ([reserved](#reserved-for-the-gtnh-themed-buildings)).

#### Red-brick rubble

`bricks_border` `}`, variant `bricks_rubble`. Follows the wall, as for [stone-brick rubble](#stone-brick-rubble).

| Main wall candidate | Rubble set | Flags |
|---|---|---|
| Baseline | `minecraft:brick_block`, `minecraft:brick_stairs`, `minecraft:stone_slab:4`, `minecraft:stained_hardened_clay:14` (upstream) | BH |
| Chisel terracotta and brick | `chisel:hardened_clay_red:1` (cracked), `chisel:brickCustom:5`, `:13` (aged, sooty), the baseline stairs and slab | SP |
| Ravaged brick | `HardcoreEnderExpansion:ravaged_brick:1` to `:5`, its slab and stairs | |
| Underbrick | `TwilightForest:tile.TFUnderBrick:1`, `:2` | |
| Old brick | `thaumicbases:oldBrick:0`, `thaumicbases:genericSlab:1` to `:3` | |
| GT dark concrete bricks | the baseline set: concrete speeds walking, so it brings no concrete rubble | |

#### Cyan terracotta walls

`bricks_cyan` `#`, `X`, `}` (all cyan terracotta). Weights are the share of `bricks_cyan` buildings; the band is in [terracotta bands](#terracotta-bands).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_hardened_clay:9` | 45 | | BH | Upstream |
| `etfuturum:concrete:9` | 12 | baseline | | Flat cyan, not a boulder host |
| `chisel:hardened_clay_cyan:2` to `:8` | 12 | baseline | SP | Patterned cyan terracotta |
| `chisel:hempcrete:9` | 8 | baseline | SP | Hard colored concrete (D28) |
| `Ztones:tile.agonBlock:5`, `Ztones:tile.mystBlock:2`, `Ztones:tile.crayBlock:5` | 8 | baseline | | Matte cyan render |
| `ExtraUtilities:color_stone:9` | 5 | baseline | | Painted concrete |
| `OpenComputers:chameliumBlock:6` | 5 | baseline | T | Flat cyan; OC color order |
| `IC2:blockWall:6` (cyan foam wall) | 5 | baseline | TE | Reverse color order |

#### Gray terracotta walls

`bricks_gray` `#`, `X`, `}`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_hardened_clay:7` | 45 | | BH | Upstream |
| `etfuturum:concrete:7` | 12 | baseline | | Flat gray |
| `chisel:hardened_clay_gray:2` to `:8` | 10 | baseline | SP | Patterned |
| `chisel:hempcrete:7` | 8 | baseline | SP | Hard colored concrete |
| `Ztones:tile.agonBlock:14`, `Ztones:tile.iszmBlock:0`, `Ztones:tile.mystBlock:10`, `Ztones:tile.fortBlock:9` | 8 | baseline | | Matte and streaked gray |
| `chisel:nucrete:0` to `:7` (dark concrete) | 5 | baseline | SP | Breeze blocks and panels |
| `ExtraUtilities:color_stone:7`, `ExtraUtilities:colorStoneBrick:7` | 5 | baseline | | Painted concrete and brick |
| `GalaxySpace:phobosblocks:0` | 4 | baseline | | Flat gray-brown stone; not a host |
| `IC2:blockWall:8` | 3 | baseline | TE | Gray foam wall |

#### Light gray terracotta walls

`bricks_silver` `#`, `X`, `}`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_hardened_clay:8` | 45 | | BH | Upstream |
| `etfuturum:concrete:8` | 12 | baseline | | Flat light gray |
| `chisel:hardened_clay_lightgray:2` to `:8` | 10 | baseline | SP | Patterned |
| `chisel:hempcrete:8` | 8 | baseline | SP | Hard colored concrete |
| `Ztones:tile.agonBlock:15`, `Ztones:tile.iszmBlock:1`, `Ztones:tile.mystBlock:15`, `Ztones:tile.crayBlock:0` | 8 | baseline | | Matte light gray |
| `chisel:nucrete:8` to `:15` (light concrete) | 5 | baseline | SP | Breeze blocks and panels |
| `GalaxySpace:tritonblocks:0` | 4 | baseline | | Flat light gray stone; not a host |
| `ExtraUtilities:color_stone:8` | 4 | baseline | | Painted concrete |
| `OpenComputers:chameliumBlock:7` | 4 | baseline | T | OC color order |

#### Terracotta bands

`$` in `bricks_cyan` and `bricks_gray` (black terracotta) and in `bricks_silver` (brown terracotta).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_hardened_clay:15` (black) or `:12` (brown) | 45 | | BH | Upstream |
| `etfuturum:concrete:15` or `:12` | 15 | baseline | | Flat, not a boulder host |
| `chisel:hempcrete:15` or `:12` | 10 | baseline | SP | Hard colored concrete |
| Black: `Ztones:tile.agonBlock:13`, `Ztones:tile.zestBlock:15`; brown: `Ztones:tile.krypBlock:4` | 10 | baseline | | Matte black, polished brown |
| Black: `Botania:quartzTypeDark:0`, `ThaumicTinkerer:darkQuartz:0` | 10 | baseline | SP (Botania) | Polished black band |
| Brown: `GalaxySpace:oberonblocks:0`, `GalaxySpace:mirandablocks:0` | 10 | baseline | | Flat terracotta brown; not hosts |

#### Desert main wall

`bricks_desert` `#`: `minecraft:sandstone:0` 12, `minecraft:sandstone:2` 116 of 128.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:sandstone:2` with `:0` | 34 | | | Upstream |
| `chisel:sandstone:8`, `:12`, `:13`, `:15`, `chisel:sandstone2:0`, `:1` | 10 | baseline | SP | Sandstone bricks and blocks |
| `Railcraft:brick.sandy:0`, `:1`, `:2` | 10 | baseline | SP, H | A full sandy brick family |
| `TConstruct:decoration.multibrick:1`, `TConstruct:decoration.multibrickfancy:1` | 8 | baseline | | Sandstone brick |
| `Ztones:tile.roenBlock:0`, `:7`, `Ztones:tile.solsBlock:3`, `:4` | 8 | baseline | | Chalk and cream masonry |
| `etfuturum:end_bricks:0` | 5 | baseline | | Pale yellow brick |
| `Thaumcraft:blockCosmeticSolid:11` (ancient stone) | 5 | baseline | | Olive-tan old stone |
| `GalaxySpace:ioblocks:0`, `GalaxySpace:mercuryblocks:0` | 4 | baseline | | Flat sandstone stand-ins; not hosts |
| `TConstruct:decoration.multibrick:9` (bone brick), `Railcraft:brick.bleachedbone:0` | 4 | baseline | SP (Railcraft) | Pale cream brick |
| `chisel:sandstone:0`, `chisel:sandstone_scribbles` | 4 | baseline | SP | Ancient quarter only |
| `TConstruct:SpeedBlock:2`, `:3` (brownstone, brownstone brick) | 4 | baseline | SPD | Tan stone and brick; walls only (D28 rule 3) |
| `gregtech:gt.blockconcretes:11` (light concrete bricks) | 4 | baseline | SPD, SP | Reads as cut sandstone; walls only |

#### Desert accent and trim

`X` in `bricks_desert` and `x` in `default_desert` (cut sandstone).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:sandstone:2` | 42 | | | Upstream |
| `minecraft:sandstone:1` (chiseled) | 12 | baseline | | Vanilla variety |
| `chisel:sandstone:4` to `:7`, `:9` to `:11` (pillars) | 12 | baseline | SP | Columns and trims |
| `Railcraft:brick.sandy:3` (ornate) | 8 | baseline | SP, H | Carved band |
| `Ztones:tile.solsBlock:4`, `Ztones:tile.roenBlock:9` | 8 | baseline | | Framed cream panels |
| `Botania:biomeStoneA:5` | 6 | baseline | SP | Botania's desert stone |
| `Ztones:tile.krypBlock:5` | 4 | baseline | | Smooth tan framed |
| `TConstruct:SpeedBlock:5`, `:6` (fancy, chiseled brownstone) | 5 | baseline | SPD | Panel and relief trims |
| `gregtech:gt.blockconcretes:14` (chiseled light concrete) | 3 | baseline | SPD, SP | Carved tan trim |

#### Bleached desert walls

`bricks_desert_bleached` `#`, this mod's palette (AR-16): white terracotta's pale beige, with cut sandstone trim. It and the taupe walls replace upstream's `bricks_desert_orange` and `bricks_desert_red` in the desert style, so a desert city stays muted ([05-assets](../index.md#styles-and-city-styles)); those two palettes are unused and get no pool.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_hardened_clay:0` | 45 | | BH | The palette's block (AR-16) |
| `chisel:hardened_clay_white:2` to `:8` | 15 | baseline | SP | Patterned |
| `Ztones:tile.roenBlock:0`, `:7`, `Ztones:tile.solsBlock:3` | 12 | baseline | | Chalk and cream render |
| `TConstruct:decoration.multibrick:9` (bone brick), `Railcraft:brick.bleachedbone:0` | 10 | baseline | SP (Railcraft) | Pale cream brick |
| `etfuturum:end_bricks:0` | 8 | baseline | | Pale yellow brick |
| `Ztones:tile.krypBlock:5` | 5 | baseline | | Smooth tan framed |
| `Botania:biomeStoneA:5` | 5 | baseline | SP | Botania's desert stone |

#### Taupe desert walls

`bricks_desert_taupe` `#`, this mod's palette (AR-16): light gray terracotta's muted taupe, the earthy wall of a desert city, with cut sandstone trim. Low saturation only: nothing here is a saturated red or orange.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_hardened_clay:8` | 45 | | BH | The palette's block (AR-16) |
| `chisel:hardened_clay_lightgray:2` to `:8` | 15 | baseline | SP | Patterned |
| `Ztones:tile.krypBlock:3` | 10 | baseline | | Taupe tiles |
| `chisel:hempcrete:8` | 8 | baseline | SP | Hard colored concrete |
| `Ztones:tile.mystBlock:15`, `Ztones:tile.crayBlock:0` | 8 | baseline | | Matte warm gray |
| `TConstruct:SpeedBlock:2`, `:3` (brownstone, brownstone brick) | 8 | baseline | SPD | Tan-brown stone and brick; walls only (D28 rule 3) |
| `Thaumcraft:blockCosmeticSolid:11` (ancient stone) | 6 | baseline | | Olive-tan old stone |

#### Desert band

`$` in the desert brick palettes and `S` in `default_desert`: smooth sandstone (`minecraft:double_stone_slab:9`), the pale band and floor of a muted desert city (AR-16; upstream's orange red sandstone double slab is replaced).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:double_stone_slab:9` (smooth sandstone) | 50 | | | The palette's block (AR-16) |
| `minecraft:sandstone:2` (cut) | 15 | | | Pale band |
| `minecraft:sandstone:1` (chiseled) | 10 | | | Carved band |
| `Railcraft:brick.sandy:2` | 10 | baseline | SP, H | Sandy block |
| `Ztones:tile.krypBlock:3` | 8 | baseline | | Taupe tiles |
| `Ztones:tile.solsBlock:4` | 7 | baseline | | Framed cream |

`bricks_desert_redsand` is unused upstream and fails to load there ([ASSET-03](../../../original-spec/05-assets/index.md#suspected-bugs)); it gets no pool.

#### Quartz main wall

`quartzbricks_border` `#`, variant `quartz`: `quartz_bricks` 10, `smooth_quartz` 118 of 128.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `etfuturum:smooth_quartz:0` with `etfuturum:quartz_bricks:0` | 40 | `minecraft:quartz_block:0` | | Upstream (D19) |
| `chisel:marble:0`, `:5`, `:12`, `:15` | 10 | baseline | SP | Marble facades |
| `Botania:quartzTypeSunny`, `Botania:quartzTypeMana` | 8 | baseline | SP | Warm and cool white quartz |
| `Railcraft:brick.bleachedbone:2`, `Railcraft:brick.quarried:2` | 8 | baseline | SP, H | Cream and pale gray block |
| `etfuturum:calcite:0` | 6 | baseline | | Pale polished stone |
| `ProjRed\|Exploration:projectred.exploration.stone:0`, `:1` (marble, marble brick) | 6 | baseline | | Off-white stone |
| `appliedenergistics2:tile.BlockQuartz:0` | 5 | baseline | | Blue-white quartz; one in four with vanilla |
| `chisel:laboratoryblock:2`, `:5` | 5 | baseline | SP | White enamel |
| `chisel:diorite:10`, `chisel:alabaster:0` | 4 | baseline | SP | Polished white stone |
| `gregtech:gt.blockstones:7`, `:3` (smooth marble, marble bricks) | 4 | baseline | BH | GT marble |
| `GalaxySpace:enceladusblocks:2` | 2 | baseline | | Glazed white brick |
| `thaumicinsurgence:tile.arcaneMarbleBrick` | 2 | baseline | H | Accent only, hardness 10 |

#### Quartz rubble

`quartzbricks_border` `}`, variant `quartz_rubble`. Follows the wall: smooth quartz, quartz bricks, `minecraft:stone_slab:7`, `minecraft:quartz_stairs` for the baseline; `chisel:marble:10`, `:11` (dented) with `chisel:marble_slab` and `chisel:marble_stairs` for marble; the stone's own slab and stairs for Botania quartz (`Botania:quartzSlabSunnyHalf`, `Botania:quartzStairsSunny`).

#### Deepslate building

Building `building6` local `#` and `}`, variants `deepslate` and `deepslate_rubble`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `etfuturum:deepslate_bricks:2` 96, `:3` 32 | 50 | `minecraft:stonebrick:0`, `:2` | | Upstream (D19) |
| `Railcraft:brick.abyssal:0`, `:1` | 12 | baseline fallback | SP, H | Near-black brick family |
| `GalacticraftCore:tile.moonBlock:14` (moon dungeon brick) | 8 | baseline fallback | H | Dark gray brick; not the moon rock ore host |
| `GalaxySpace:proteusblocks:3` | 8 | baseline fallback | | Large dark-gray bricks |
| `appliedenergistics2:tile.BlockSkyStone:2`, `:3` | 8 | baseline fallback | H | Sky stone brick |
| `ProjRed\|Exploration:projectred.exploration.stone:4` | 7 | baseline fallback | | Basalt brick |
| `IC2:blockBasalt:0` | 7 | baseline fallback | H | Very dark stone, not GT basalt |

Rubble follows the wall: `etfuturum:cobbled_deepslate:0` with its slab and stairs (upstream), `Railcraft:brick.abyssal:5`, `GalacticraftCore:slabGCHalf:3` and `tile.moonBricksStairs`, `ProjRed|Exploration` basalt cobblestone.

#### Blackstone building

Building `building7` local `#` and `}`, variants `blackstone` and `blackstone_rubble`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `etfuturum:blackstone:1` 64, `:2` 32, `:3` 32 | 50 | `minecraft:stonebrick:0`, `:2` | | Upstream (D19) |
| `ThaumicTinkerer:darkQuartz:0` to `:4` | 12 | baseline fallback | | Black polished quartz set with stairs and slab |
| `Botania:quartzTypeDark:0` to `:4` | 10 | baseline fallback | SP | The same look, Botania's |
| `Railcraft:brick.infernal:0`, `:2` | 10 | baseline fallback | SP, H | Dark gray brick and block |
| `ExtraUtilities:decorativeBlock1:2` (burnt quartz) | 8 | baseline fallback | | Black modern facade |
| `appliedenergistics2:tile.BlockSkyStone:1` | 5 | baseline fallback | H | Polished near-black |
| `Ztones:tile.krypBlock:15` | 5 | baseline fallback | | Dark smooth stone |

Rubble follows the wall: `etfuturum:blackstone_slab:1`, `etfuturum:polished_blackstone_stairs` (upstream), `Railcraft:brick.infernal:5`, `ThaumicTinkerer:darkQuartzSlab:0`.

#### Stone-and-brick building

Building `building8` local `#` and `}`, variants `stoneandbrick` and `stoneandbrick_rubble`. Upstream's raw stone is an ore host and maps to smooth stone (D19).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `etfuturum:smooth_stone:0` 64, `minecraft:stonebrick:0` 32, `:2` 32 | 50 | `minecraft:double_stone_slab:8` for smooth stone | | Upstream (D19) |
| `chisel:stonebricksmooth:0` to `:3` (modern masonry) with `minecraft:stonebrick` | 15 | baseline | SP | Masonry mix |
| `etfuturum:tuff:1` (polished tuff) with `etfuturum:tuff:2` | 10 | baseline | | Gray polished mix; not tuff `0` |
| `Ztones:tile.ztylBlock:8`, `:12` with `minecraft:stonebrick` | 10 | baseline | | Smooth and mottled gray |
| `BiomesOPlenty:rocks:5` with `minecraft:stonebrick` | 8 | baseline | | Charcoal dressed stone |
| `GalacticraftCore:slabGCDouble:2` with `minecraft:stonebrick` | 7 | baseline | | Light concrete look |

Rubble follows the wall: `etfuturum:stone_slab:0`, `etfuturum:stone_stairs` (upstream's slab and stone stairs, D19), `minecraft:stonebrick:2`, `chisel:stonebricksmooth:6`.

### Quartz and glass

#### Quartz block and column

`Q` in `default` and `default_desert`, and `@` in `glass_side_variant_quartz`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:quartz_block:0` | 50 | | | Upstream |
| `minecraft:quartz_block:2` (pillar; for `@` columns) | 10 | baseline | | Vanilla variety |
| `etfuturum:smooth_quartz:0` | 10 | baseline | | Seamless |
| `chisel:marble_pillar:0` to `:13` (for `@`) | 8 | `minecraft:quartz_block:2` | SP | Columns with capitals |
| `Botania:quartzTypeSunny`, `Botania:quartzTypeMana` | 7 | baseline | SP | Tinted quartz |
| `appliedenergistics2:tile.BlockQuartz:0`, `tile.BlockQuartzPillar:0` | 5 | baseline | | Certus quartz |
| `minecraft:quartz_block:1` (chiseled) | 5 | baseline | | Vanilla variety |
| `etfuturum:calcite:0` | 5 | baseline | | Pale stone |

#### Quartz stairs

`(` and `)`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:quartz_stairs` | 60 | | | Upstream |
| `etfuturum:smooth_quartz_stairs` | 15 | baseline | | Pairs with smooth quartz walls |
| `chisel:marble_stairs.<n>` | 10 | baseline | SP | Pairs with marble walls |
| `Botania:quartzStairsSunny`, `quartzStairsMana` | 8 | baseline | SP | Pairs with Botania quartz |
| `appliedenergistics2:tile.QuartzStairBlock` | 7 | baseline | | Pairs with certus quartz |

Stairs follow the wall material where a building uses one of these quartz families.

#### Clear glass blocks

`+` and `a` in `glass_full`, `+` in `glass_pane`, `@` in `glass_side_variant_glass` when `a` is a block.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:glass` | 40 | | | Upstream |
| `chisel:glass:6`, `:10`, `:11`, `:13`, `:14` (borderless, steel frame, stone frame, grid) | 15 | baseline | | Framed windows |
| `Railcraft:glass:0` (white strengthened, stacks into tall windows) | 8 | baseline | | Framed; light gray corner frame |
| `TConstruct:GlassBlock:0` (clear glass) | 8 | baseline | | Frameless |
| `appliedenergistics2:tile.BlockQuartzGlass:0` | 6 | baseline | | Clear |
| `ExtraUtilities:decorativeBlock2:0`, `:1` (thickened, edged) | 6 | baseline | | Framed |
| `GalaxySpace:futureglass` | 5 | baseline | | Hex-leaded curtain wall |
| `bartworks:BW_TieredGlass:0` (borosilicate) | 4 | baseline | | Framed clear glass |
| `Botania:manaGlass:0` | 4 | baseline | SP, L15 | Lights the room |
| `EnderIO:blockFusedQuartz:1` (quite clear glass) | 4 | baseline | TE | Clear |

#### Tinted glass blocks

`+` and `a` in `glass_full_white`, `_gray`, `_blue`, `_light_blue`, and `+` in the matching `glass_pane_*`. Same color as the palette; each candidate names its meta for white, gray, blue and light blue.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_glass:0`, `:7`, `:11`, `:3` | 40 | | | Upstream |
| Chisel framed and borderless stained glass in the same color (`chisel:stained_glass_white:0` to `:3`, `:12` to `:15`, and the other colors as [chisel.md](chisel.md#best-variety-pools) names them) | 15 | baseline | | Office curtain walls |
| `gregtech:gt.blocktintedglass:0`, `:2`, `:12`, `:11` | 10 | baseline | H (wrench) | Frameless, exact colors |
| `Railcraft:glass:0`, `:7`, `:11`, `:3` | 8 | baseline | | Tall framed windows |
| `TConstruct:GlassBlock.StainedClear:0`, `:7`, `:11`, `:3` | 7 | baseline | | Clear stained glass |
| `GalaxySpace:futureglasses:15`, `:8`, `:4`, `:12` (reverse order: 0 black, 15 white) | 6 | baseline | | Hex-leaded |
| `witchery:shadedglass:0`, `:7`, `:11`, `:3` | 5 | baseline | | Smoky tint; place the unpowered block |
| `ThaumicHorizons:crystalTH` (same color; order unverified) | 3 | baseline | | Milky frosted glass |
| Gray only: `EnderIO:blockFusedQuartz:5` (dark clear glass) | 3 | baseline | TE | Office tint |
| Gray only: `Ztones:tile.glaxx:0`, `:14` | 3 | baseline | | Smoke tint, block only |

#### Clear panes

`` ` `` and `a` in `glass_pane`, `` ` `` in `glass_full`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:glass_pane` | 55 | | | Upstream |
| `chisel:glass_pane:1`, `:2`, `:4` (bubble, borderless, streak) | 15 | baseline | | Pane styles |
| `TConstruct:GlassPane:0` | 12 | baseline | | Clear pane |
| `Botania:manaGlassPane:0` | 8 | baseline | L15 | Lit pane |
| `ExtraTrees:stainedglass` (leaded designs, `meta` NBT) | 5 | baseline | TE, T | Leaded windows for older buildings |
| `malisisdoors:curtain_white`, `_silver`, `_gray` hung behind the pane row | 5 | none (skip) | TE | Lived-in windows; a door-like block, not a pane |

#### Tinted panes

`` ` `` and `a` in `glass_pane_white`, `_gray`, `_blue`, `_light_blue`, and `` ` `` in the matching `glass_full_*`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_glass_pane:0`, `:7`, `:11`, `:3` | 55 | | | Upstream |
| Chisel framed, borderless and four-pane stained panes (`chisel:stained_glass_pane_white`, and gray, light blue and blue at metas 8 to 13 of `_pink`, `_magenta`, `_purple`, as [chisel.md](chisel.md#best-variety-pools) records) | 25 | baseline | | Pane styles |
| `TConstruct:GlassPaneClearStained:0`, `:7`, `:11`, `:3` | 20 | baseline | | Clear stained panes |

#### White and city-sphere glass

`Y` (white stained glass) and `Z` (random city-sphere glass: clear, gray, blue, light blue, 32 each) in `common`. `Z` stays a per-block mix; the candidates widen each color's slot.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `Y` `minecraft:stained_glass:0`; `Z` `minecraft:glass`, `minecraft:stained_glass:7`, `:11`, `:3` | 60 | | | Upstream |
| Chisel framed and bubble styles of the same four colors | 15 | baseline | | Variety in the sphere |
| `GalaxySpace:futureglasses:15` (`Y`), `:4`, `:6`, `:8`, `:12` (`Z`) | 10 | baseline | | Hex-leaded dome |
| `gregtech:gt.blocktintedglass:0`, `:2`, `:11`, `:12` | 10 | baseline | H (wrench) | Exact colors |
| `Railcraft:glass:0`, `:7`, `:11`, `:3` | 5 | baseline | | Framed |

### Streets, highways and fittings

#### Street and floor slab

`S` and `u` in `default` (smooth stone double slab), the city style's street block and building floors, plus `@` in the unused `glass_side_variant_street`. **Side streets and floors use this pool; it holds no speed block.** Spawn-proof candidates together stay at or under 30 (D28 rule 1).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:double_stone_slab:0` | 45 | | | Upstream |
| `etfuturum:smooth_stone:0` | 8 | `minecraft:double_stone_slab:8` | | Seamless paving |
| `Ztones:stoneTile:0` | 6 | baseline | | Bordered paving tile |
| `etfuturum:concrete:8`, `:7` | 5 | baseline | | Poured concrete |
| `GalacticraftMars:tile.asteroidsBlock:0`, `:2` | 4 | baseline | H | Asphalt look without speed; not the ore-host meta 1 |
| `BiomesOPlenty:rocks:5`, `:4` (polished shale, shale) | 4 | baseline | | Dark paving |
| `etfuturum:polished_deepslate:0` | 3 | baseline | | Dark slab |
| `GalacticraftCore:slabGCDouble:2` | 3 | baseline | | Light concrete slab |
| `Ztones:tile.ztylBlock:11`, `Ztones:tile.tankBlock:15` | 2 | baseline | | Large gray tiles |
| `chisel:stonebricksmooth:12` | 6 | baseline | SP | Stone panel |
| `Botania:pavement:0`, `:1` | 6 | baseline | SP | White and black setts |
| `Railcraft:cube:1` (concrete) | 5 | baseline | SP | Gray concrete |
| `chisel:nucrete:4`, `:12` | 3 | baseline | SP | Concrete tiles |

Spawnable share: 80 (the SP rows sum to 20). In `default_desert`, `S` is the [desert band](#desert-band) block and keeps that pool.

#### Primary roads

The street surface of primary city roads, as a weighted option beside the [street and floor slab](#street-and-floor-slab) pool, judged at the M06 screenshots (D28 rule 3). Needs the layout to tell a primary road from a side street ([Prerequisites](#prerequisites-for-the-asset-system)).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:double_stone_slab:0` | 50 | | | Upstream |
| `chisel:road:0`, `chisel:concrete:10` (asphalt), with `chisel:road:1` crosswalks at junctions and `chisel:road:2`, `:3`, `:6`, `:7`, `:14` lane markings on marking rows | 25 | baseline | SPD, SP | Real asphalt streets |
| `chisel:road2:13`, `:14` (weathered asphalt) | 8 | baseline | SPD, SP | Ruined cities |
| `gregtech:gt.blockconcretes:15` (smooth light concrete), `:7` (smooth dark concrete) | 7 | baseline | SPD, SP | Concrete roadway |
| `TConstruct:SpeedBlock:1` (rough brownstone road) | 5 | baseline | SPD | Cobbled paving |
| `TConstruct:SpeedBlock:4` (brownstone road; desert cities) | 5 | baseline | SPD | Tan paved road |

Chisel's direction-named markings are fixed to world axes: the writer picks the variant that matches the street's axis.

#### Sidewalks and plazas

A role upstream leaves to its street block; `street_large` `l` (smooth stone slab) and `q` (smooth quartz lane paint) and park paths. No speed blocks.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `l` `minecraft:double_stone_slab:0`; `q` `etfuturum:smooth_quartz:0` | 50 | `q`: `minecraft:quartz_block:0` | | Upstream (D19) |
| `Ztones:stoneTile:0`, `Ztones:tile.ztylBlock:11` | 10 | baseline | | Paving tiles |
| `etfuturum:concrete:8`, `:0` | 8 | baseline | | Light concrete |
| `Ztones:tile.krypBlock:3`, `Ztones:tile.roenBlock:13` (desert), `Ztones:tile.lairBlock:9` (old town) | 6 | baseline | | Small tiles and flagstones |
| `GalaxySpace:tritonblocks:0` | 4 | baseline | | Flat light gray |
| `Botania:pavement:0` to `:5` | 8 | baseline | SP | Portuguese setts |
| `chisel:cobblestone:6`, `:7`, `:10`, `chisel:limestone:13` | 6 | baseline | SP | Tiled plazas |
| `Railcraft:brick.quarried:2` | 4 | baseline | SP, H | Pale block paving |
| `chisel:technical:13` (grate) | 2 | baseline | SP | Drain grates |
| Parks: `etfuturum:grass_path:0`, `Botania:dirtPath:0`, `OpenBlocks:path:0` overlay | 2 | `minecraft:dirt:1` | SP (Botania) | Footpaths |

#### Highway surface

`R` in `default` and `default_desert` (cyan terracotta). Highways are the main home of the speed blocks (D28 rule 3).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stained_hardened_clay:9` | 50 | | BH | Upstream |
| `chisel:road:0`, `chisel:concrete:10` with `chisel:road_line:0` to `:3` on marking rows | 25 | baseline | SPD, SP | Asphalt highway, no boulder host |
| `gregtech:gt.blockconcretes:7`, `:15` | 8 | baseline | SPD, SP | Concrete deck |
| `Railcraft:cube:1` (concrete) | 7 | baseline | SP | Gray highway, no speed |
| `GalacticraftMars:tile.asteroidsBlock:0` | 5 | baseline | H | Asphalt look, no speed |
| `gregtech:gt.blockreinforced:13` (Reinforced Block) | 5 | baseline | H | Bunker-grade concrete deck; hardness 40 |

Highway railings stay `minecraft:iron_bars`: upstream's `clearRailing` finds them by block identity ([highway-parts.md](../../../original-spec/06-highways-railways/highway-parts.md)).

#### Street variant brick

`B` in `default` (bricks, the city style's `streetvariant`) and `default_desert` (sandstone, AR-16: upstream's red nether bricks are replaced).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:brick_block`; desert `minecraft:sandstone:0` | 50 | | | Upstream (D19); the desert's is AR-16's |
| `chisel:brickCustom:1`, `:2`, `:3` | 15 | baseline | SP | Brick paving bonds |
| `thaumicbases:oldBrick:0` | 10 | baseline | | Old brick |
| Desert: `chisel:sandstone:8`, `:12`, `Railcraft:brick.sandy:0` | 10 | baseline | SP | Sandstone paving |
| `Ztones:tile.lairBlock:4`, `:11` | 10 | baseline | | Red brick |
| `HardcoreEnderExpansion:ravaged_brick:0` | 5 | baseline | | Worn brick |

#### Pillars and beds

`v` (support pillars), `y` (rail beds, `railmain`, street border) and `x` (park elevation, corridor roofs) in `default`: variant `stonebrick`. In `default_desert`, `v` and `y` are stone bricks with cracked and mossy, and `x` is cut sandstone ([desert accent and trim](#desert-accent-and-trim)).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: variant `stonebrick` | 50 | | | Upstream |
| `Railcraft:brick.quarried:2`, `Railcraft:brick.abyssal:2`, `Railcraft:brick.infernal:2` | 15 | baseline | SP, H | Solid blocks for piers |
| `Railcraft:cube:1` (concrete) | 10 | baseline | SP | Concrete piers |
| `chisel:stonebricksmooth:4`, `:5` | 10 | baseline | SP | Bond variety |
| `GalacticraftCore:tile.wallGC:2` (moon rock wall, `v` only where a wall shape fits) | 5 | baseline | | Light concrete column |
| `gregtech:gt.blockstones:14` (chiseled basalt) | 5 | baseline | BH | Dark pillar |
| `gregtech:gt.blockframes:305` (steel frame box; exposed steel columns) | 5 | baseline | XM, T | Girder piers |

#### Bottom slabs

`_` and `=` (smooth stone bottom slab).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stone_slab:0` | 50 | | | Upstream |
| `etfuturum:stone_slab:3` and the slab of the building's wall family (`etfuturum:deepslate_brick_slab:0`, `etfuturum:tuff_slab`, `etfuturum:smooth_red_sandstone_slab:0`) | 20 | baseline | | Matching slabs |
| `chisel:marble_slab:0` | 10 | baseline | SP | Marble slab |
| `Botania:pavement0Slab`, `Botania:stone8Slab` | 8 | baseline | SP | Sett and brick slabs |
| `GalacticraftCore:slabGCHalf:0`, `:2` | 7 | baseline | | Tin and moon rock slabs |
| `ThaumicTinkerer:darkQuartzSlab:0`, `Thaumcraft:blockCosmeticSlabStone:0` | 5 | baseline | | Dark and arcane slabs |

Bottom slabs take no mob spawns in vanilla, so this pool is spawn-neutral. `chisel:concrete_slab` and `TConstruct:SpeedSlab` are speed blocks and stay out.

#### Walls

`w` (cobblestone wall) in `default`, the city style's street `wall` block.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:cobblestone_wall:0` | 45 | | | Upstream; not a boulder host |
| `minecraft:cobblestone_wall:1` (mossy) | 10 | baseline | | Vanilla variety |
| `etfuturum:stone_wall`, `etfuturum:deepslate_brick_wall:0`, `etfuturum:blackstone_wall:2` | 12 | baseline | | Brick parapets |
| `Railcraft:wall.alpha:5`, `:10`, `:2` (stone brick, brick, concrete) | 10 | baseline | SP | Matching wall shapes |
| `Botania:stone0Wall:0`, `:2`, `Botania:livingrock0Wall:0` | 8 | baseline | SP | Pale walls |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:0`, `:2`, `:4` | 8 | baseline | | Marble and basalt walls |
| `GalacticraftCore:tile.wallGC:2` | 7 | baseline | | Light concrete wall |

#### Iron bars

`:` in `default` and `default_desert`, and the "damaged to" block of nearly every palette character.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:iron_bars` | 50 | | | Upstream; the only choice for highway railings and the damage block (identity checks) |
| `chisel:iron_bars:1`, `:3`, `:5`, `:6`, `:8` | 15 | baseline | | Grids and frames |
| `dreamcraft:SteelBars`, `dreamcraft:AluminiumBars`, `dreamcraft:StainlessSteelBars` | 12 | baseline | | Pack metal bars |
| `EnderIO:blockDarkIronBars:0` (dark steel bars), `EnderIO:blockSoulariumBars:0` in ruins | 8 | baseline | | Dark and rusty bars |
| `gregtech:gt.blockfencemetal:0`, `IC2:blockFenceIron:0` | 8 | baseline | | Iron railings |
| `etfuturum:copper_grate:4` (waxed) | 4 | baseline | DM | Copper grille |
| `openmodularturrets:fenceTierOne`, `fenceTierTwo` | 3 | baseline | | Chain-link |

Only `:` in building parts takes the pool; the damage block stays `minecraft:iron_bars`.

#### Stone-brick stairs

`A`, `t`, `z`, `c`, `d` (stone brick stairs in four facings and upside down). Stairs follow the building's wall family where it has stairs.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:stone_brick_stairs` | 55 | | | Upstream |
| `etfuturum:mossy_stone_brick_stairs` (ruined) | 8 | baseline | | Upstream's 1.20 block, used here for variety |
| `etfuturum:deepslate_brick_stairs`, `etfuturum:tuff_brick_stairs`, `etfuturum:polished_blackstone_brick_stairs` | 12 | baseline | | Match dark walls |
| `Botania:stone8Stairs`, `Botania:stone10Stairs` | 8 | baseline | SP | Match Botania bricks |
| `Thaumcraft:blockStairsArcaneStone` | 6 | baseline | | Match arcane bricks |
| `appliedenergistics2:tile.SkyStoneBrickStairBlock` | 4 | baseline | H | Match sky stone |
| `GalacticraftCore:tile.moonBricksStairs` | 4 | baseline | H | Match moon dungeon brick |
| `HardcoreEnderExpansion:ravaged_brick_stairs` | 3 | `minecraft:brick_stairs` | | Match ravaged brick |

Railcraft's stairs keep their material in a tile entity and wait for tile-entity palette data.

#### Stone stairs

`/` (1.20 `stone_stairs`, plain stone stairs).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `etfuturum:stone_stairs` | 60 | `minecraft:stone_brick_stairs` | | Upstream (D19); a separate block, not an ore host |
| `etfuturum:polished_deepslate_stairs`, `etfuturum:polished_tuff_stairs` | 15 | baseline | | Polished stone stairs |
| `GalacticraftCore:tile.moonStoneStairs` | 10 | baseline | | Light gray stairs |
| `etfuturum:cobbled_deepslate_stairs`, `etfuturum:mossy_cobblestone_stairs` (ruins) | 10 | baseline | | Rougher stairs |
| `Botania:stone0Stairs` | 5 | baseline | SP | Andesite stairs |

#### Street markings

An overlay layer upstream lacks: paint placed in the air block over a finished surface. No fallback is needed; the layer is skipped when the mod is absent.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: none (upstream's `street_large` parts paint lanes with smooth quartz) | 40 | | | Upstream |
| `modernmarkings:tile.floor_marking_white_lane`, white singleline and sideline, yellow lane and singleline | 30 | skip | | Lane paint |
| ModernMarkings arrows, crossings, lane crosses, yellow stripes; parking squares and letters in lots and garages | 15 | skip | | Junctions and parking |
| `chisel:road_line:0` to `:3` on primary roads and highways (with the asphalt) | 10 | skip | | Painted lines; no speed of their own |
| `chisel:road_line:4` (hazard tape) and ModernMarkings hazard lines in yards and loading bays | 5 | skip | | Industrial floors |

Ruin damage removes most markings with their support.

### Interiors and roofs

#### Interior floors

Floors in buildings where a part or a later overlay gives them their own character; until then, building `S` and `u` take the [street and floor slab](#street-and-floor-slab) pool. D28 rule 1: spawnable candidates keep at least 70 of 100 by default.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:double_stone_slab:0` | 30 | | | Upstream |
| `minecraft:planks:0` to `:5` and `minecraft:wooden_slab` | 10 | baseline | | Wood floors |
| `Natura:planks:1`, `:2`, `:3`, `:5`, `:6`, `:7` | 6 | `minecraft:planks:0` | | Metadata-only wood tones |
| `BiomesOPlenty:planks:2`, `:3`, `:6`, `:7`, `:8`, `:11`, `:12`, `:13`, `:14` | 6 | `minecraft:planks:0` | | Nine more tones |
| `cookingforblockheads:white_kitchen_floor:0`, `black_kitchen_floor:0`, `light_gray_kitchen_floor:0`, `red_kitchen_floor:0`, `light_blue_kitchen_floor:0` (registry names `<dye>_kitchen_floor`) | 5 | `minecraft:stained_hardened_clay:0` | | Kitchens, diners, bathrooms; no tile entity |
| `minecraft:carpet:0`, `:7`, `:8`, `:12`, `:14`, `:15` over a floor block | 5 | skip | | Offices and apartments |
| `Ztones:tile.laveBlock:8`, `Ztones:tile.ztylBlock:8` | 4 | baseline | | Pale tiles |
| `etfuturum:polished_deepslate:0`, `etfuturum:smooth_stone:0` | 4 | baseline | | Lobbies |
| `TwilightForest:tile.TFPlank:0` to `:7`, `witchery:witchwood:0` to `:2` | 3 | `minecraft:planks:0` | | More wood |
| `chisel:laboratoryblock:6`, `:7`, `:8`, `chisel:porcelain:3`, `:4`, `:10`, `:12` | 8 | baseline | SP | Labs, hospitals, bathrooms |
| `chisel:marble:12`, `:13`, `:14` | 5 | baseline | SP | Lobby tiles |
| `Botany:ceramicBrick` tile patterns (white, gray, black, checker) | 4 | `minecraft:quartz_block:0` | TE, T | Bathroom and diner tile |
| `Botania:livingwood:1`, `Botania:dreamwood:1` | 3 | `minecraft:planks:5` | SP | Dark and bleached wood |
| Forestry fireproof planks (`Forestry:planksFireproof`, `WT` NBT: ebony, wenge, ipe, walnut, teak) | 3 | `minecraft:planks:5` | TE | Dark woods; single rooms only |
| `ExtraTrees:planks` (`meta` NBT) | 2 | `minecraft:planks:0` | TE, T | Pale and exotic woods |
| `littletiles:LTColoredBlock:1` | 2 | baseline | | Clinic floor finish (hardness 0) |

Spawnable share: 84 (the SP rows sum to 16).

#### Ceilings

A role upstream leaves to its wall block. Ceiling lights are in [glowstone](#glowstone).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: the building's wall block | 50 | | | Upstream |
| `Ztones:tile.laveBlock:0`, `:8` to `:12`, `Ztones:tile.mintBlock:2`, `:3` | 12 | baseline | | Office ceiling panels |
| `chisel:laboratoryblock:0`, `:1`, `:9`, `:10` | 10 | baseline | SP | Drop-ceiling look |
| `GalacticraftCore:tile.gcBlockCore:4` | 8 | baseline | | Light gray metal panel |
| `chisel:marble:4` | 5 | baseline | SP | Marble panel |
| `IC2:blockLuminatorDark` (unlit luminator panels in a grid) | 5 | baseline | TE | Dead fluorescent panels |
| `FloodLights:smallElectricFloodlightMetaBlock:1` (unpowered square panels) | 5 | baseline | TE | Dead panels; facing in `teDirection` |
| `littletiles:LTColoredBlock:0`, `:6` | 5 | baseline | | Plaster finish |

#### Roofs

A role upstream lacks: its roofs are flat walls and slabs. Stairs, slabs and roof tiles make pitched or framed roofs in parts and overlays that ask for them.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: flat roof in the wall block and `minecraft:stone_slab:0` | 50 | | | Upstream |
| `minecraft:brick_stairs`, `minecraft:spruce_stairs`, `minecraft:dark_oak_stairs`, `minecraft:stone_slab:4` | 16 | baseline | | Vanilla pitched roofs |
| `Botania:customBrick:3` (roof tile) with `Botania:customBrick3Stairs`, `customBrick3Slab` | 10 | `minecraft:brick_stairs` | SP | Terracotta-red roof tile |
| `etfuturum:deepslate_tile_stairs`, `etfuturum:deepslate_brick_slab:1` | 10 | `minecraft:stone_brick_stairs` | | Slate roofs |
| `Ztones:tile.venaBlock:14`, `:15` | 4 | baseline | | Verdigris that never weathers |
| `gregtech:gt.sheetmetal:305`, `:19`, `:36`, `:57` (steel, aluminum, zinc, tin) | 4 | baseline | XM, T | Corrugated warehouse roofs |
| `ThaumicTinkerer:darkQuartzStairs`, `Botania:quartzStairsDark` | 3 | `minecraft:quartz_stairs` | SP (Botania) | Modern parapets |
| `IC2:blockRubber:0` (rubber sheet membrane) | 3 | baseline | | Flat-roof membrane, industrial |

Twilight Forest's castle roof tile is hardness 100 and stays out ([never placed](#never-placed)). Copper and other metal roofs stay out too: D29 keeps metal blocks to capped accents ([decorative metals](#decorative-metals-d29)). True slopes from ArchitectureCraft and Carpenter's Blocks need a "shape of material" palette entry ([Prerequisites](#prerequisites-for-the-asset-system)).

#### Doors

Upstream places random building doors in birch, acacia, dark oak, spruce, oak and jungle, and iron doors in parts. MalisisDoors supplies every wood door (D28 rule 5); with `modifyVanillaDoors=true` the vanilla doors are MalisisDoors blocks with a tile entity too.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: the upstream wood → `malisisdoors:door_<wood>` (oak: `minecraft:wooden_door`); iron → `minecraft:iron_door` | 50 | `minecraft:wooden_door` | TE | Upstream (D19 as amended by D28) |
| `Natura:door.ghostwood`, `door.sakura`, `door.redwood`, `door.hopseed`, `door.redwoodbark` | 10 | `minecraft:wooden_door` | | Metadata-only wood doors |
| `malisisdoors:wood_sliding_door`, `iron_sliding_door` (storefronts, offices) | 10 | `minecraft:wooden_door`, `minecraft:iron_door` | TE | Glass sliding doors |
| `etfuturum:copper_door` to `waxed_oxidized_copper_door` | 8 | `minecraft:iron_door` | DM | Hand-opened metal doors; weathered in ruins |
| `TwilightForest:tile.TFDoor<wood>` (eight woods) | 6 | `minecraft:wooden_door` | | More wood doors |
| `malisisdoors:laboratory_door` (hospitals, labs), `factory_door`, `jail_door` (police) | 6 | `minecraft:iron_door` | TE | Themed doors |
| `malisisdoors:garage_door` (full column) | 4 | air | TE | Garages and loading bays |
| `ExtraTrees:door` (glazed-grid styles, `meta` NBT on both halves) | 4 | `minecraft:wooden_door` | TE, T | Shop fronts |
| `IC2:blockDoorAlloy` (reinforced door) | 2 | `minecraft:iron_door` | H | Vaults; hardness 50 |

Curtains (`malisisdoors:curtain_<color>`) are window dressing ([clear panes](#clear-panes)). `carriage_door`, `medieval_door` and `rustyHatch` are left out ([Prerequisites](#prerequisites-for-the-asset-system)).

#### Trapdoors and hatches

Upstream's oil-rig `V` (iron trapdoor), plus hatches and shutters a part may ask for.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `malisisdoors:iron_trapdoor` | 50 | `etfuturum:waxed_copper_trapdoor` (while the D29 copper flag is on), then `minecraft:trapdoor` | TE | Upstream (D19 as amended) |
| `etfuturum:copper_trapdoor` and its weathered and waxed forms | 15 | `minecraft:trapdoor` | DM | Metal hatches, railings, shutters |
| `malisisdoors:trapdoor_<wood>` (acacia, birch, dark oak, jungle, spruce) | 12 | `minecraft:trapdoor` | TE | Wood shutters |
| `Natura:trapdoor.<wood>` (13 woods) | 10 | `minecraft:trapdoor` | | Metadata-only shutters |
| `TwilightForest:tile.TFTrapDoor<wood>` | 8 | `minecraft:trapdoor` | | More woods |
| `etfuturum:cherry_trapdoor:0` | 5 | `minecraft:trapdoor` | | Pale wood |

#### Wood trim

Upstream's oak planks (`top1x1_5` local `o`), fence posts and the wood in its furniture. Trims and beams in parts that ask for wood.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:planks:0` | 40 | | | Upstream |
| `StorageDrawers:trim:0` to `:5`, `StorageDrawersForestry:trimA`, `trimB` (teak, wenge, ebony, mahogany, walnut, chestnut, ipe, padauk) | 15 | `minecraft:planks:0` | | Framed-plank wainscoting and beams, no tile entity |
| `etfuturum:log_stripped:0` to `:3`, `etfuturum:wood_stripped:0` to `:3` | 12 | `minecraft:log:0` | | Stripped posts and beams |
| `Natura:planks`, `BiomesOPlenty:planks` (as in [interior floors](#interior-floors)) | 12 | `minecraft:planks:0` | | Wood tones |
| `salisarcana:blockCustomPlank:0`, `:1` (greatwood, silverwood) | 6 | `minecraft:planks:5`, `:2` | | Dark and pale planks |
| `etfuturum:wood_planks:3` (cherry) | 5 | `minecraft:planks:2` | | Pale pink wood |
| `chisel:<wood>_planks` patterns 1, 5, 6, 13 | 5 | `minecraft:planks:0` | SP | Parquet and paneling |
| `Forestry:logsFireproof` (`WT` NBT) as timber posts | 5 | `minecraft:log:0` | TE | Old shops and cottages |

### Lights and fittings

#### Glowstone

`h` in `common` (glowstone), and upstream's ceiling and corridor lights.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:glowstone` | 40 | | L15 | Upstream |
| `Botania:seaLamp:0` | 10 | `minecraft:glowstone` | L15, SP | The pack's sea lantern (D19) |
| `chisel:glowstone:3`, `:4`, `:8`, `:9`, `chisel:froglight:0`, `:4` | 10 | baseline | L15, SP | Panel and froglight looks |
| `Ztones:lampf:6`, `Ztones:lampt:6` (flush ceiling panels) | 8 | baseline | L15 | Office lighting; no collision |
| `Railcraft:lantern.metal:0`, `:5`, `Railcraft:lantern.stone:6`, `:9` | 6 | baseline | L15 | Caged lanterns |
| `etfuturum:copper_bulb:12` (waxed, lit) | 5 | baseline | L15, DM | Copper fittings |
| `appliedenergistics2:tile.BlockQuartzLamp:0` | 5 | baseline | L15 | Lit glass panels |
| `ExtraUtilities:greenscreen:0` (white Lapis Caelestis), `ExtraUtilities:color_lightgem:0` | 4 | baseline | L15 | White ceiling panels |
| `IC2NuclearControl:blockNuclearControlLight:1` | 4 | baseline | L15 | White panel lamp; goes dark if tinkered |
| `EnderIO:blockFusedQuartz:3` (enlightened clear glass) | 3 | baseline | L15, TE | Lit windows |
| `GalaxySpace:proteusglowstone`, `GalaxySpace:enceladusglowstone` | 3 | baseline | L15 | Neutral lamp blocks |
| `Automagy:blockTranslucent:0`, `:1` (glowing amber, amber bricks) | 2 | baseline | L15 | Warm lamp block, no tile entity |

`ProjRed|Illumination` lamps (tile entity, lit through `inv`), Computronics' colorful lamp (`clc` NBT) and BiblioCraft lamps are low-weight options once tile-entity palette data exists.

#### Torches

`T` (torch, re-oriented after generation) and `g` (lit redstone torch) in `common`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `T` `minecraft:torch`; `g` `minecraft:redstone_torch` | 60 | | L14, L7 | Upstream |
| `chisel:torch1` to `chisel:torch10` (`T` only; metadata is the facing) | 15 | `minecraft:torch` | L14 | Torch looks |
| `TConstruct:decoration.stonetorch` | 10 | `minecraft:torch` | L14 | Stone torch |
| `GalacticraftCore:tile.glowstoneTorch` | 8 | `minecraft:torch` | L12 | Glowstone torch |
| `harvestcraft:pamcandleDeco1` to `pamcandleDeco16` (white, light gray, red, yellow first) | 7 | `minecraft:torch` | L14 | Interior candles, no tile entity |

#### Hanging lights

`lantern[hanging=true]` (part `highway_restaurant` `n`) and `sea_lantern` (`highway_gas_station` and `highway_restaurant_parking` `e`, oil rig `J`).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: lantern `etfuturum:lantern:1`; sea lantern `Botania:seaLamp:0` | 60 | `minecraft:glowstone` | L15 | Upstream (D19) |
| `Railcraft:lantern.metal:0`, `:2`, `:5` (lanterns) | 15 | baseline | L15 | Street and porch lights |
| `chisel:lantern:0`, `:1` | 10 | baseline | L15, SP | Lantern looks |
| `etfuturum:soul_lantern:0` (old town, ruins) | 5 | baseline | L10 | Dim blue lantern |
| `GalacticraftCore:tile.arclamp` (yards, parking) | 5 | baseline | L15, TE | Floodlight; off under redstone |
| `IC2NuclearControl:blockNuclearControlLight:3` (tunnels, highways) | 5 | baseline | L15 | Orange panel lamp |

#### Furniture

`F` in `common`: furnace 25, crafting table 20, brewing stand 7, anvil 6, cauldron 5, enchanting table 2, cobweb 63 of 128. A per-block mix: candidates take slots from the furniture share, never from the cobwebs.

| Candidate | Slots of the 65 furniture slots | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: upstream's six blocks | 40 | | TE (furnace, brewing stand, enchanting table) | Upstream |
| `etfuturum:smoker:0`, `blast_furnace:0`, `smithing_table:0`, `cartography_table:0`, `loom:0`, `fletching_table:0` | 8 | `minecraft:crafting_table` | TE (smoker, blast furnace) | Usable workstations |
| `Natura:Natura.workbench:0` to `:12` | 5 | `minecraft:crafting_table` | | Wood-variant crafting tables, no tile entity |
| `cookingforblockheads` counters, cabinets, sinks, ovens (kitchens) | 4 | `minecraft:crafting_table` | TE | Kitchens; facing in metadata |
| `harvestcraft:sink:0` to `:3`, `harvestcraft:cuttingboard:0`, `harvestcraft:pot:0` (sinks, cutting board, pot) | 3 | `minecraft:cauldron` | | Kitchen dressing, no tile entity |
| `EnderIO:blockDarkSteelAnvil:0` to `:2`, Railcraft steel anvil | 2 | `minecraft:anvil` | | Industrial anvils |
| `ExtraUtilities:filing:0`, `ExtraUtilities:trashcan:0` | 2 | `minecraft:crafting_table` | TE | Office and street bins |
| `Thaumcraft:blockTable:0` | 1 | `minecraft:crafting_table` | TE | Arcane quarter |

BiblioCraft desks, seats and cases, OpenComputers cases and screens, CookingForBlockheads fridges, Storage Drawers and vending machines need facing in tile-entity data and come with it.

#### Bookshelves

`L` in `common`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:bookshelf` | 55 | | | Upstream |
| `Natura:Natura.bookshelf:0` to `:12` | 15 | baseline | | Wood-variant bookshelves, no tile entity |
| `chisel:bookshelf:1`, `:4`, `:6`, `:8`; `:5` (cobwebbed) in ruins | 15 | baseline | SP | Bookshelf looks |
| `Automagy:blockBookshelfEnchanted:0` | 8 | baseline | | Library interiors |
| `BiblioCraft:BiblioCase:0` to `:6` (bookcase; facing in NBT) | 7 | baseline | TE | Library furniture |

#### Chests and barrels

`C` (loot chest) in `common`, and `barrel` (part `cabin` `B`, oil rig `s`, `N`, `U`). Inventories follow the loot rules ([loot-and-mobs.md](../../00-pack-integration/loot-and-mobs.md)).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:chest`; barrel `etfuturum:barrel` | 75 | barrel: `minecraft:chest` | TE | Upstream (D19) |
| `IronChest:BlockIronChest:0`, `:3`, `:4` (iron, copper, steel; industrial buildings) | 10 | `minecraft:chest` | TE | Metal chests; gold, diamond and obsidian chests are loot themselves and stay out |
| `ProjRed\|Exploration:projectred.exploration.barrel:0` | 5 | `etfuturum:barrel` | TE | Banded barrel |
| `JABBA:barrel` (warehouses, stacks of a few) | 5 | `etfuturum:barrel` | TE | Crates |
| `Thaumcraft:blockLootCrate`, `blockLootUrn` (ruins) | 5 | `minecraft:chest` | | Drop Thaumcraft loot when broken; no tile entity |

#### Ladders

`l` in `common` (ladder facing north) and the oil rig `j`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:ladder` | 60 | | | Upstream |
| `catwalks:cagedLadder_<facing>_unlit` (fire escapes up building sides) | 15 | baseline | | Caged ladders |
| `EnderIO:blockDarkSteelLadder:0` to `:5` | 10 | baseline | | Black metal ladder |
| `TConstruct:decoration.stoneladder` | 8 | baseline | | Stone ladder |
| `malisisdoors:rustyLadder` (manholes, shafts) | 7 | baseline | | Rusty ladder, no tile entity |

#### Pressure plates

`-` in `common` (oak pressure plate) and the oil rig's spruce and stone plates (`P`, `m`).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:wooden_pressure_plate`; `etfuturum:pressure_plate_spruce`; `minecraft:stone_pressure_plate` | 70 | `minecraft:wooden_pressure_plate` | | Upstream (D19) |
| Natura pressure plates (13 woods) | 15 | baseline | | Wood variety |
| `EnderIO:blockDarkSteelPressurePlate:1` (silent) | 15 | baseline | TE | Dark metal plate |

#### Fences

`.` in `common` (oak fence) and the oil rig `E`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:fence` | 50 | | | Upstream |
| `Natura:Natura.fence:0` to `:12` (ghostwood, sakura, redwood, maple first) | 15 | baseline | | Wood tones |
| `minecraft:nether_brick_fence` | 8 | baseline | | Dark railing |
| `gregtech:gt.blockfencemetal:0`, `IC2:blockFenceIron:0` | 8 | baseline | | Iron railings |
| `Railcraft:post:2`, `Railcraft:post.metal:0`, `:7`, `:8`, `Railcraft:post:0` | 8 | baseline | | Posts |
| `openmodularturrets:fenceTierOne`, `fenceTierTwo` | 5 | baseline | | Chain-link |
| `HardcoreEnderExpansion:ravaged_brick_fence` | 3 | `minecraft:nether_brick_fence` | | Brick fence |
| `catwalks:catwalk_unlit_nobottom` on roof edges | 3 | baseline | | Roof railings |

Fence gates: `malisisdoors:<wood>FenceGate` and `Natura:fenceGate.<wood>`, with `minecraft:fence_gate` (a MalisisDoors block in this pack) as the fallback.

#### Chains and cobwebs

`{` (chain), `\` (cobweb) and `%` (cobweb 40, air 88) in `common`.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `etfuturum:chain:0`; `minecraft:web` | 80 | chain: `minecraft:iron_bars` | | Upstream (D19) |
| `chisel:iron_bars:5`, `:6` (for `{` in industrial buildings) | 10 | baseline | | Hanging grilles |
| `etfuturum:glow_lichen:0` with the cobwebs (ruins) | 10 | skip | L7 | Ruin glow |

`GalaxySpace:ceresweb` and `ioweb` hurt on contact and stay out.

#### Treasure

`!` in `common`: diamond 15, gold 20, emerald 15, iron 78 of 128. Treasure is loot, so storage blocks of any kind may go here, whatever the D29 flags say; elsewhere only the flagged metals appear, as capped accents ([decorative metals](#decorative-metals-d29)), and gem blocks never. The mix goes on the [beta balance list](../../beta-balance-list.md).

| Candidate | Slots of 128 | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: upstream's four | 100 | | | Upstream |
| `Botania:storage:0` (manasteel) | 6 | `minecraft:iron_block` | SP | Pack-flavored treasure, below EV |
| `IC2:blockMetal:2`, `:5` (bronze, refined iron) | 8 | `minecraft:iron_block` | | Industrial treasure |
| `gregtech:gt.blockmetal6:13` (steel) | 8 | `minecraft:iron_block` | | Steel at LV |
| `dreamcraft:CompressedCoalCoke` | 6 | `minecraft:coal_block` | | Cheap pack treasure |

#### Colored concrete

`9` in `common` (city-sphere border: cyan and gray terracotta and concrete, 32 each), and the concrete in local palettes: `highway_gas_station` and `highway_restaurant` `p` gray, `r` red, `k` black; `highway_restaurant_parking` `m` yellow; oil rig `&` red, `3` yellow.

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `etfuturum:concrete:<color>`; `9` as upstream (terracotta part BH) | 60 | `minecraft:stained_hardened_clay:<color>` | BH (terracotta) | Upstream (D19) |
| `chisel:hempcrete:<color>` | 20 | baseline | SP | The hard colored block (D28 rule 1) |
| `Ztones:tile.agonBlock` matching color (5 cyan, 14 gray, 13 near-black, 10 red; `Ztones:tile.iszmBlock:9` yellow) | 10 | baseline | | Matte render |
| `ExtraUtilities:color_stone:<color>` | 5 | baseline | | Painted concrete |
| `chisel:woolen_clay` same color | 5 | baseline | SP | Soft-textured color |

### Plants, ruins and set pieces

#### Flowers and planters

`p` (flowers and saplings) and `K` (poppy or dandelion) in `common`, `*` (potted cactus), `G` (grass) and `D` (dirt), and the local `P` sapling list in `park_trees`. Per-block mixes; candidates take slots beside upstream's.

| Candidate | Slots of 128 | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: upstream's flowers and saplings | 70 | | | Upstream |
| `minecraft:red_flower:1` to `:8`, `minecraft:double_plant`, `minecraft:tallgrass:2` (ferns) | 15 | `minecraft:red_flower:0` | | Vanilla flowers upstream never used |
| `etfuturum:cornflower:0`, `lily_of_the_valley:0`, `rose:0`, `pink_petals:0` | 12 | `minecraft:red_flower:0` | | 1.14+ flowers |
| `BiomesOPlenty:flowers:1`, `:4` to `:9`, `BiomesOPlenty:flowers2:0`, `:1`, `:3` to `:5`, `:7`, `:8` (swampflower, hydrangea to anemone, hibiscus, lily of the valley, lavender, goldenrod, bluebells, icy iris, rose; all stay on grass, unlike miner's delight `flowers2:6`, which needs stone) | 12 | `minecraft:red_flower:0` | | Park flowers |
| `Botania:flower:0` to `:15`, `Botania:shinyFlower` for lit beds | 10 | `minecraft:red_flower:0` | SP, L15 (shiny) | Mystical flowers; never spreading |
| `Natura:GrassBlock:0` (topiary hedges and lawns, kept-up parks) | 5 | `minecraft:grass` | SP | Spawn-proof by D28's floor rule |
| `Ztones:cleanDirt:0` (planter soil) | 4 | `minecraft:dirt` | | Planter beds |

Saplings grow into trees on random ticks; planters that must keep their shape use flowers or bushes. Leaves are placed with their no-decay bit.

#### Overgrowth

Upstream's overgrown look comes from vines, leaves and mossy blocks. Upstream's moss block, 68 of the 128 slots of the rubble-dirt default its code uses when a city style sets no `rubbledirt` block, maps to `chisel:moss:0` (Chisel's lush moss, which matches the modern moss block's look), falling back to `minecraft:mossy_cobblestone`, and is one of the blocks the damage pass recognizes as rubble (D31; [block-mapping.md](../../00-pack-integration/block-mapping.md#blocks-upstreams-code-names)). The ruin pass and overgrown variants draw from:

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: `minecraft:vine`, `minecraft:leaves` (no-decay bit), `minecraft:mossy_cobblestone`, `minecraft:stonebrick:1` | 40 | | | Upstream |
| `BiomesOPlenty:ivy:0` to `:15`, `BiomesOPlenty:moss:0` to `:15` | 15 | `minecraft:vine` | | Ivy and moss on facades; spreads like vines |
| `Botania:solidVine` | 10 | `minecraft:vine` | SP | Vines that never spread |
| `chisel:brutalism:2`, `:3`, `chisel:leaves:6` to `:10` | 10 | baseline | SP | Leafy cement, hedges that never decay (Chisel's overgrown road `road2:15` is a speed block and stays on primary roads) |
| `harvestcraft` wild gardens with `minecraft:tallgrass` | 8 | `minecraft:tallgrass:1` | | Overgrown lots |
| `chisel:moss:0`, `:3`, `:9`, `chisel:moss_carpet:0`, `:3`, `:9`, `chisel:mossy_cobblestone:1` to `:7` | 7 | baseline | SP (moss blocks, cobblestone) | Moss blocks and carpets |
| `witchery:spanishmoss:0` | 5 | `minecraft:vine` | | Hanging moss |
| `TwilightForest` roots, moss patch, darkwood hedge (`tile.TFHedge:1`; meta 0 hurts) | 5 | `minecraft:vine` | | Ruin and park follies |

#### Ruin dressing

What explosion and ruin damage leave behind, beside each wall's rubble set ([stone-brick rubble](#stone-brick-rubble)).

| Candidate | Weight | Fallback | Flags | Why |
|---|---|---|---|---|
| Baseline: each character's rubble and upstream's damage | 50 | | | Upstream |
| `chisel:technical4:0`, `:1`, `:5`, `:13` (worn concrete, cardboard scraps), `chisel:factoryblock:0`, `:2`, `:14`, `chisel:sheetmetal:5` to `:9` (rusty plate) | 12 | baseline | SP | Industrial debris |
| `chisel:brutalism:4` (faux window), `:7` (rebar) | 8 | baseline | SP | Dead storefronts |
| `gregtech:gt.blockframes:305`, `:809` (steel and wood frame boxes) | 8 | skip | XM, T | Exposed structure, half-built floors |
| `catwalks:scaffold:0`, `:1`, `IC2:blockScaffold:0`, `IC2:blockIronScaffold:0` (only where supported) | 8 | skip | | Abandoned construction |
| `etfuturum:coarse_dirt:0`, `BiomesOPlenty:hardDirt:0`, `minecraft:gravel` | 6 | `minecraft:dirt:1` | G (gravel) | Rubble ground |
| `thaumicbases:oldCobble:0`, `oldCobbleMossy:0` | 4 | `minecraft:mossy_cobblestone` | | Old cobblestone |
| `TConstruct:trap.barricade.oak`, `.spruce`, `.birch`, `.jungle` over doors and windows | 4 | skip | | Boarded-up openings |

`ExtraUtilities:color_stonebrick:0` (white colored cobblestone) is a cobblestone look that is not a boulder host.

#### Oil rig

The `oilrig` palette (38 characters). Faithful first; its industrial set dressing is where the GTNH-themed buildings start ([reserved](#reserved-for-the-gtnh-themed-buildings)).

| Character | Baseline (D19) | Variety |
|---|---|---|
| `D`, `5` to `8`, `l`, `i`, `o`, `p`, `w` (polished diorite, its stairs and wall) | `Botania:stone:6`, `Botania:stone2Stairs`, `Botania:stone0Wall:2` | `ProjRed\|Exploration:projectred.exploration.stone:0` (marble) with its wall `stonewalls:0`; `chisel:diorite:10` |
| `I` (black stained glass) | `minecraft:stained_glass:15` | `GalaxySpace:futureglasses:0`, `Railcraft:glass:15`, `gregtech:gt.blocktintedglass:3` |
| `J` (sea lantern) | `Botania:seaLamp:0` | [hanging lights](#hanging-lights) |
| `0`, `[` (polished blackstone bricks, cracked) | `etfuturum:blackstone:2`, `:3` | [blackstone building](#blackstone-building) |
| `s`, `N`, `U` (barrels) | `etfuturum:barrel` | `ProjRed\|Exploration:projectred.exploration.barrel:0` |
| `&`, `3` (red, yellow concrete) | `etfuturum:concrete:14`, `:4` | `chisel:hempcrete:14`, `:4` |
| `E` (oak fence), `\|` (redstone torch), `L`, `r` (levers) | vanilla | `dreamcraft:SteelBars`, `StainlessSteelBars` as guard rails |
| `M` (diamond block) | `Ztones:tile.iszmBlock:8` (smooth pale aqua, the closest match to the diamond block's color), then `chisel:hempcrete:3`, then `minecraft:stained_hardened_clay:3` | none. A non-valuable look-alike: no diamond block is placed outside loot (D30) |
| `q` (obsidian) | vanilla | none |
| deck | `minecraft:double_stone_slab:8` (`2`) | `GalacticraftMars:tile.walkway`, `Railcraft:cube:8` (creosote block) |

#### Cabin

Part `cabin` local palette: exposed copper and its cut stairs, quartz, barrel, iron door.

| Character | Baseline (D19) | Variety |
|---|---|---|
| `c`, `1` to `4` (exposed copper, exposed cut copper stairs; DM, see [decorative metals](#decorative-metals-d29)) | `etfuturum:copper_block:1`, `etfuturum:exposed_cut_copper_stairs`; with the copper flag off, `minecraft:planks:1` and `minecraft:spruce_stairs` | the other unwaxed stages (the cabin keeps weathering, as upstream) |
| `D`, `d` (iron door) | `minecraft:iron_door` | `etfuturum:exposed_copper_door` (DM) |

The cabin is one of the two exceptions to D29's accent cap; the other is the [gas station and restaurant parking](#gas-station-and-restaurant-parking).

#### Gas station and restaurant parking

Parts `highway_gas_station` and `highway_restaurant_parking`, the scattered structures beside highways ([07-scattered](../../07-scattered/index.md)). Their `i` is `minecraft:iron_block`: 20 blocks in the gas station's canopy columns and pumps ([highway_gas_station.json](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/parts/highway_gas_station.json)) and 4 in the parking's lamp posts ([highway_restaurant_parking.json](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/parts/highway_restaurant_parking.json)). The iron is the structure, not an accent, so like the [cabin](#cabin)'s copper it keeps upstream's block while its flag is on, the second exception to D29's accent cap (D31).

| Character | Baseline (D19) | Variety |
|---|---|---|
| `i` (iron block; DM, see [decorative metals](#decorative-metals-d29)) | `minecraft:iron_block` while `decorativeMetals.iron` is on; with the flag off, `chisel:factoryblock:12` (Shiny Metal Construction Block, [chisel.md](chisel.md)), then `minecraft:double_stone_slab:8` | none: upstream's block, so the canopy and pumps read as one material |
| `p`, `r`, `k`, `m` (gray, red, black, yellow concrete) | as [colored concrete](#colored-concrete) | as [colored concrete](#colored-concrete) |
| `e` (sea lantern) | `Botania:seaLamp:0` | [hanging lights](#hanging-lights) |
| `a`, `j` (iron door, gas station), `b` (iron bars, parking) | `minecraft:iron_door`, `minecraft:iron_bars` | [iron bars](#iron-bars) for `b` |

### Decorative metals (D29)

Metal storage blocks and their decorative variants come back as accents. Each metal at or below EV that the catalogs place has its own worldgen flag, on by default and pinned into the world profile at creation; one flag covers that metal's blocks in every mod. A flagged metal appears only as accents under a per-building cap: stripe bands, trims, crates and ingot stacks as props, the odd vent or panel, never whole walls, floors or roofs. The roles are `wall-accent`, `trim-pillar`, `furniture-deco` and `special`; the per-mod catalogs drop `wall-main`, `floor` and `roof` from these blocks. With a flag off, each candidate takes its fallback, so a metal accent becomes the building's own accent block. The defaults and the cap are on the [beta balance list](../../beta-balance-list.md). Treasure (`!`) and chest loot are loot, not decoration, and ignore the flags.

**How the tiers are set.** GT's machine casings fix the ladder: the ULV to IV casings are made of cast iron, steel, aluminium, stainless steel, titanium and tungstensteel plates ([AssemblerRecipes.java#L2570-L2618][gt-casings]). A metal that needs no blast furnace (furnace-smelted or alloyed) is steam age. A blast-furnace metal is placed by its blast temperature against that ladder: up to aluminium and stainless steel's 1700K it is at most HV, and up to 2700K, the Kanthal coil titanium already needs (cupronickel gives 1801K, Kanthal 2701K: [HeatingCoilLevel.java#L10-L15][gt-coils]), it is at most EV. Tungstensteel (4000K) and anything above it is IV or later. Platinum and uranium need no blast furnace but stay out by D29's own list.

| Metal | Flag | Blocks it covers | Accent roles | Tier (GT source at 5.09.54.265) |
|---|---|---|---|---|
| Iron | `decorativeMetals.iron` | `minecraft:iron_block` (also the gas station's and restaurant parking's `i`, an exception to the cap: [gas station](#gas-station-and-restaurant-parking)); Chisel `iron_block:0` to `:15`, `iron2:0` to `:5`; Tinkers `decoration.multibrick:4`, `multibrickfancy:4`; IC2 `blockMetal:5` (refined iron); Thaumic Bases `oldIron:0` | `wall-accent` (vents, gears, caution stripes), `furniture-deco` (crates), `special` (ingot and coin stacks as props) | steam: no blast furnace ([MaterialsInit.java#L865-L897][gt-iron]) |
| Gold | `decorativeMetals.gold` | `minecraft:gold_block`; Chisel `gold_block`, `gold2`; Tinkers `decoration.multibrick:5`, `multibrickfancy:5` (gold bricks); Thaumic Bases `oldGold:0` | `special` (ingot and coin stacks), `wall-accent` | steam ([#L725-L759][gt-gold]) |
| Cast iron | `decorativeMetals.castIron` | Chisel `castironblock:0` to `:7` (its group keys `blockCastIron`, [Features.java#L4833][ch-castiron], which GT's block registers); GT `gt.blockmetal8:2` ([LoaderGTBlockFluid.java#L836-L844][gt-metal8]) | `wall-accent`, `furniture-deco`, `special` | ULV: the ULV casing metal; no blast furnace ([MaterialsInit.java#L9648-L9668][gt-castiron]) |
| Copper | `decorativeMetals.copper` | Et Futurum copper blocks, cut and chiseled copper, grates, bulbs, doors, trapdoors, cut copper stairs and slabs; Chisel `copperblock`; GT `gt.blockmetal2:7`; Railcraft `cube:9`; IC2 `blockMetal:0`; Galacticraft `tile.gcBlockCore:9`; Tinkers `MetalBlock:3`; ProjectRed `projectred.exploration.stone:8` | `wall-accent`, `trim-pillar`, `furniture-deco`, `special`; Et Futurum's doors, trapdoors, bulbs and grates as fittings | steam ([#L500-L531][gt-copper]) |
| Tin | `decorativeMetals.tin` | Chisel `tinblock`; GT `gt.blockmetal7:7`; Railcraft `cube:10`; IC2 `blockMetal:1`; Galacticraft `tile.gcBlockCore:10`; Tinkers `MetalBlock:5`; ProjectRed `projectred.exploration.stone:9` | `wall-accent`, `trim-pillar`, `furniture-deco` | steam ([#L1797-L1822][gt-tin]) |
| Bronze | `decorativeMetals.bronze` | Chisel `bronzeblock`; GT `gt.blockmetal2:0`; IC2 `blockMetal:2`; Tinkers `MetalBlock:4` | `wall-accent`, `furniture-deco` | steam: an alloy ([#L5894-L5917][gt-bronze]) |
| Lead | `decorativeMetals.lead` | Chisel `leadblock`; GT `gt.blockmetal4:2`; Railcraft `cube:11`; IC2 `blockMetal:4` | `wall-accent`, `furniture-deco` | steam ([#L919-L945][gt-lead]) |
| Silver | `decorativeMetals.silver` | Chisel `silverblock`; GT `gt.blockmetal6:10`; ProjectRed `projectred.exploration.stone:10` | `wall-accent`, `special` | steam ([#L1610-L1636][gt-silver]) |
| Nickel | `decorativeMetals.nickel` | Chisel `nickelblock`; GT `gt.blockmetal5:4` | `wall-accent`, `furniture-deco` | steam ([#L1212-L1239][gt-nickel]) |
| Invar | `decorativeMetals.invar` | Chisel `invarblock`; GT `gt.blockmetal3:11` | `wall-accent`, `furniture-deco` | steam: an alloy ([#L6604-L6624][gt-invar]) |
| Electrum | `decorativeMetals.electrum` | Chisel `electrumblock`; GT `gt.blockmetal2:15`. Chisel's group keys the misspelled `blockElectrun` ([Features.java#L4754][ch-electrum]), so it does not take GT's block in this pack | `wall-accent`, `furniture-deco` | steam: an alloy ([#L6325-L6346][gt-electrum]) |
| Steel | `decorativeMetals.steel` | Chisel `steelblock`; GT `gt.blockmetal6:13`; Railcraft `cube:2`; Tinkers `MetalBlock:9` | `wall-accent`, `trim-pillar`, `furniture-deco` | LV: the LV casing metal, also made in the steam-age primitive blast furnace ([BlastFurnaceRecipes.java#L740-L760][gt-steel-pbf]) |
| Black steel | `decorativeMetals.blackSteel` | GT `gt.blockmetal1:12` | `wall-accent` | MV at most: blast 1200K ([MaterialsInit.java#L10095-L10117][gt-blacksteel]) |
| Damascus steel | `decorativeMetals.damascusSteel` | GT `gt.blockmetal2:9` | `wall-accent` | MV at most: blast 1500K ([#L10262-L10283][gt-damascus]) |
| Meteoric iron | `decorativeMetals.meteoricIron` | Galacticraft `tile.gcBlockCore:12`; GT `gt.blockmetal4:7` | `wall-accent` | MV at most: blast 1000K ([#L1077-L1105][gt-meteoric]) |
| Electrical steel | `decorativeMetals.electricalSteel` | EnderIO `blockIngotStorage:0` | `wall-accent` | MV at most: blast 1000K ([#L10314-L10336][gt-electricalsteel]) |
| Dark steel | `decorativeMetals.darkSteel` | EnderIO `blockIngotStorage:6` | `wall-accent` | HV at most: blast 1800K ([#L11991-L12012][gt-darksteel]) |
| Aluminum | `decorativeMetals.aluminum` | Chisel `aluminumblock` (GTNH adds GT's block to the group, [ScriptChisel.java#L383][nh-chisel]); GT `gt.blockmetal1:1`; Galacticraft `tile.gcBlockCore:11`; Tinkers `MetalBlock:6` | `wall-accent`, `trim-pillar`, `furniture-deco` | MV: the MV casing metal; blast 1700K ([MaterialsInit.java#L159-L183][gt-aluminium]) |
| Aluminum brass | `decorativeMetals.aluminumBrass` | Tinkers `MetalBlock:7` | `wall-accent` | steam: an alloy, no blast furnace ([#L5341-L5352][gt-albrass]) |
| Cobalt | `decorativeMetals.cobalt` | Chisel `cobaltblock` (keys `blockCobalt`, [Features.java#L4993][ch-cobalt]); GT `gt.blockmetal2:5`; Tinkers `MetalBlock:0` | `wall-accent`, `furniture-deco` | HV at most: blast 1700K, as aluminium ([MaterialsInit.java#L473-L498][gt-cobalt]) |
| Stainless steel | `decorativeMetals.stainlessSteel` | Chisel `stainlesssteelblock`; GT `gt.blockmetal6:12` | `wall-accent`, `trim-pillar`, `furniture-deco` | HV: the HV casing metal; blast 1700K ([#L9337-L9360][gt-stainless]) |
| Titanium | `decorativeMetals.titanium` | Chisel `titaniumblock`; GT `gt.blockmetal7:9`. Chisel's group keys `titaniumBlock` ([Features.java#L4854][ch-titanium]), not GT's `blockTitanium`, so it does not take GT's block | `wall-accent`, `furniture-deco` | EV: the EV casing metal; blast 1940K ([MaterialsInit.java#L1824-L1849][gt-titanium]) |
| Desh | `decorativeMetals.desh` | Galacticraft `tile.mars:8` | `wall-accent` | EV at most: blast 2500K, Kanthal coils ([#L533-L558][gt-desh]) |
| Thaumium | `decorativeMetals.thaumium` | Chisel `thaumium:0` to `:5` and Thaumcraft `blockCosmeticSolid:4`, one carving group ([Features.java#L4055-L4068][ch-thaumium]) | `wall-accent`, `trim-pillar` | steam: GT gives it no blast furnace ([MaterialsInit.java#L11562-L11583][gt-thaumium]); Thaumcraft makes it in the crucible, with no GT voltage |

The other metals at or below EV that the catalogs list (annealed copper, black bronze, blue and red steel, brass, chrome, cupronickel, magnalium, nichrome, pig iron, red alloy, rose gold, soldering alloy, sterling silver, tin alloy, tungsten, zinc and vanadium steel in GT; Tinkers' ardite and alumite; EnderIO's other alloys; magic metals such as Thaumic Bases' void metal and thauminite, Witching Gadgets' void metal and Tainted Magic's shadowmetal) look off-theme in a city and get no flag: they stay loot. Gem blocks (diamond, emerald, lapis, redstone, Chisel's mineral groups, Thaumic Bases' old diamond and lapis, GT's gem blocks), quicksilver blocks, GT++ compressed obsidian, and gold, diamond and obsidian chests and barrels stay loot only.

**Above EV, never placed:** tungstensteel (the IV casing metal, blast 4000K: [#L11678-L11700][gt-tungstensteel]), iridium and osmium (blast 4500K: [#L838-L863][gt-iridium], [#L1316-L1342][gt-osmium]), platinum and uranium (D29), and Chisel's rhodium-plated palladium (`rppblock`; bartworks' material is a 4500K blast-furnace metal, [WerkstoffLoader.java#L1311-L1333][bw-rpp], and the group keys `rppBlock`, which nothing registers). That covers Chisel's `tungstensteelblock`, `iridiumblock`, `osmiumblock`, `platinumblock`, `uraniumblock` and `rppblock`, GT's tungstensteel and uranium blocks, and IC2's uranium block.

**Two exceptions to the cap** keep upstream's look where the metal is the structure, not an accent, both confirmed by the owner. **Upstream's cabin** is the one building upstream makes of copper (exposed copper walls and cut copper stairs, [cabin](#cabin)); it keeps upstream's palette while `decorativeMetals.copper` is on, and with the flag off falls back to spruce planks and stairs (D29). **The highway gas station and restaurant parking** keep their 24 iron blocks (canopy columns, pumps and lamp posts, [gas station and restaurant parking](#gas-station-and-restaurant-parking)) while `decorativeMetals.iron` is on, and with the flag off take `chisel:factoryblock:12`, then `minecraft:double_stone_slab:8` (D31).

[gt-casings]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2570-L2618
[gt-coils]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/HeatingCoilLevel.java#L10-L15
[gt-steel-pbf]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/postload/recipes/BlastFurnaceRecipes.java#L740-L760
[gt-metal8]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/preload/LoaderGTBlockFluid.java#L836-L844
[gt-iron]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L865-L897
[gt-gold]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L725-L759
[gt-castiron]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L9648-L9668
[gt-copper]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L500-L531
[gt-tin]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L1797-L1822
[gt-bronze]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L5894-L5917
[gt-lead]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L919-L945
[gt-silver]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L1610-L1636
[gt-nickel]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L1212-L1239
[gt-invar]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L6604-L6624
[gt-electrum]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L6325-L6346
[gt-blacksteel]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L10095-L10117
[gt-damascus]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L10262-L10283
[gt-meteoric]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L1077-L1105
[gt-electricalsteel]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L10314-L10336
[gt-darksteel]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L11991-L12012
[gt-aluminium]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L159-L183
[gt-albrass]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L5341-L5352
[gt-cobalt]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L473-L498
[gt-stainless]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L9337-L9360
[gt-titanium]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L1824-L1849
[gt-desh]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L533-L558
[gt-thaumium]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L11562-L11583
[gt-tungstensteel]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L11678-L11700
[gt-iridium]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L838-L863
[gt-osmium]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L1316-L1342
[bw-rpp]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/bartworks/system/material/WerkstoffLoader.java#L1311-L1333
[nh-chisel]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.99/src/main/java/com/dreammaster/scripts/ScriptChisel.java#L383
[ch-castiron]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L4833
[ch-electrum]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L4754
[ch-titanium]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L4854
[ch-cobalt]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L4993
[ch-thaumium]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L4055-L4068

## Reserved for the GTNH-themed buildings

GT casings ULV to EV are held back for the GTNH-themed buildings after beta (D28 rule 4), with the tier ceiling of [record 0004](../../../design/0004-gtnh-layer-and-tier-ceiling.md). They are plain blocks with metadata, spawn-proof, mined with a wrench, and registered as GT machine blocks (placing one after `provideChunk` queues a machine-update scan). Every block in a `gt.blockcasings*` registry counts as a casing, including the ones that look like bricks or signs. Machine hulls (`gregtech:gt.blockmachines`, MetaTileEntities) are never placed; the matching casing looks the same.

| Group | Blocks (registry:meta) | Tier |
|---|---|---|
| Machine casings | `gregtech:gt.blockcasings:0` to `:4` | ULV to EV |
| Hermetic casings | `gregtech:gt.blockcasings6:0` to `:4` | ULV to EV |
| Coils | `gregtech:gt.blockcasings5:0` (cupronickel), `:1` (kanthal), `:2` (nichrome) | LV to EV |
| Brick and plate casings | `gregtech:gt.blockcasings:10` (bronze plated bricks), `:11` (heat proof), `gregtech:gt.blockcasings4:15` (firebricks), `gregtech:gt.blockcasings12:0` (coke oven bricks) | steam to LV |
| Structural casings | `gregtech:gt.blockcasings2:0` (solid steel), `:1` (frost proof), `gregtech:gt.blockcasings4:1` (clean stainless steel), `:2` (stable titanium), `gregtech:gt.blockcasings8:0` (chemically inert), `gregtech:gt.blockcasings4:9`, `:10`, `:11` (turbine casings), `:13` (engine intake) | LV to EV |
| Gear box, pipe and firebox casings | `gregtech:gt.blockcasings2:2` to `:4`, `:12` to `:14`, `gregtech:gt.blockcasings8:1`, `gregtech:gt.blockcasings3:13`, `:14`, `gregtech:gt.blockcasings4:3` | steam to EV |
| Vents and grates | `gregtech:gt.blockcasings3:10` (grate), `:11` (filter) | MV |
| Item pipe and reactor casings | `gregtech:gt.blockcasings11:0` to `:3`, `gregtech:gt.blockcasings13:0` to `:4`, `gregtech:gt.blockcasings12:15` | LV to EV |
| Air filter and pyrolyse casings | `gregtech:gt.blockcasingsNH:0` to `:4` | LV to EV |
| Crates | `gregtech:gt.blockcasings9:2` (primitive wooden), `gregtech:gt.blockcasings10:15` (reinforced wooden) | steam |
| Stripes and hazard signs | `gregtech:gt.blockcasings3:0`, `:1` (yellow stripes), `:2` to `:6`, `:8`, `:9` (signs; `:7` magic is off-theme) | LV |
| Cleanroom casing | `gregtech:gt.blockreinforced:2` (Plascrete) | MV |
| GT++ casings | the metas [gtplusplus.md](gtplusplus.md) lists as plausibly up to EV, including `miscutils:miscutils.blockcasings:10` (iron plated bricks), `:15`, `:5`, `:6`, `miscutils:gtplusplus.blockcasings.2:0`, `:12` | up to EV |
| bartworks casings | `bartworks:BW_Machinery_Casings:0`, `:1` | LV to HV |
| Dressing that goes with them | `dreamcraft:SteelBars`, GT frame boxes and sheetmetal (already in the pools), Railcraft tanks and coke ovens, BuildCraft tanks, `VendingMachine` (a GT multiblock) | |

The casings [gregtech.md](gregtech.md) marks with an unverified tier (`gregtech:gt.blockcasings10:0`, `:4`, `:5`, `:13`, `:14`) stay out until their tier is checked.

## Never placed

D28 rule 7, with the pack's other hazards. The never-place list applies to every palette, override and generated part. Vanilla spawners (`minecraft:mob_spawner`) are exempt from the hardness rule: GT makes them hardness 500 (`harderMobSpawner`, [loot-and-mobs.md](../../00-pack-integration/loot-and-mobs.md)), but D20 places them at upstream's density, and "Spawner density and GT's spawner hardness" is on the [beta balance list](../../beta-balance-list.md).

| Reason | Blocks |
|---|---|
| Unbreakable | `minecraft:bedrock`, Et Futurum `barrier` and `light`, Thaumcraft warded glass and arcane door (hardness -1 with `allow_warded_stone=true`), Thaumic Horizons `evanescent`, Galacticraft `tile.spaceStationBase`, Hardcore Ender Expansion `biome_core`, LootGames' shielded dungeon floor |
| Explosive | `gregtech:gt.blockreinforced:5` (Powderbarrel), Blood Magic `blood_tnt`, EnderZoo charges, Hardcore Ender Expansion `energy_cluster` and `enhanced_tnt`, Natura `Dark Tree:1` (fusewood logs explode when broken) |
| Hardness above 50 | Twilight Forest castle brick, castle rune brick, roof tile, mazestone and deadrock (100), giant blocks; GT reinforced blocks `gt.blockreinforced:0` (60), `:1`, `:8`, `:9`, `:11`, `:12`; NewHorizonsCoreMod plated reinforced stones (60 to 750); IC2 reinforced stone `IC2:blockAlloy` (80); OpenModularTurrets `hardWallTierFive` and `fenceTierFive` (80); Extra Utilities bedrockium; Hardcore Ender Expansion `ender_goo`; NewHorizonsCoreMod `BloodyIchorium` (120) |
| Valuable storage outside loot | Gem storage blocks (diamond, emerald, lapis, redstone; GT `gt.blockgem*`; Chisel's mineral groups; Tinkers' gem bricks; Thaumic Bases' old diamond and lapis), quicksilver blocks, GT++ compressed obsidian, and gold, diamond and obsidian chests and barrels: treasure (`!`) and chest loot only. Metal blocks at or below EV are no longer here: each goes behind its D29 flag as a capped accent ([decorative metals](#decorative-metals-d29)), and metals with no flag stay loot |
| Metals above EV | Tungstensteel, iridium, osmium, platinum, uranium and rhodium-plated palladium blocks in every mod (D29) |
| GT ore hosts | Raw `minecraft:stone`, `netherrack`, `end_stone`, `gregtech:gt.blockgranites:0`, `:8`, `gregtech:gt.blockstones:0`, `:8`, `etfuturum:deepslate` (any meta), `etfuturum:tuff:0`, and the planet stones GT treats as hosts in every dimension: Galacticraft `tile.moonBlock:4`, `tile.mars:6`, `tile.mars:9`, `tile.asteroidsBlock:1`, and the 33 GalaxySpace stones in [galaxyspace.md](galaxyspace.md) ([ore-veins.md](../../00-pack-integration/ore-veins.md#which-blocks-gt-replaces)) |
| Infested | Every `minecraft:monster_egg` (D19); Twilight Forest infested towerwood (`tile.TFTowerStone:4`) |
| Casings and machines | GT casings until the GTNH-themed buildings ([above](#reserved-for-the-gtnh-themed-buildings)); machine hulls never; any machine, turret, portal or ticking generator |
| Gameplay hazards | Speed blocks as floors, roofs, sidewalks, side streets, stairs or rubble (D28 rule 3: they are walked on only on highways and primary roads, and may stand in walls and trims); Thaumcraft and Thaumic Tinkerer travel paving, slabs and stairs; OpenBlocks elevators in same-colored columns and scaffolding; OpenModularTurrets fences tier 3 to 5; Extra Utilities ethereal glass, spikes, chandelier and magnum torch (spawn suppression), colored redstone blocks; Natura soul glass, heat glass, heat sand and thornvines; Twilight Forest thorns and hedge meta 0 at reachable height; Witchery brambles; Blood Magic spectral block; Biomes O' Plenty quicksand, poison ivy and thorns; GalaxySpace webs; Galacticraft cavernous vines; crusted taint and tainted soil; OpenSecurity security doors on paths; Ztones `booster`; GT bounce and sticky pads |

## City styles that would gain a distinct pool

Candidates only: styles are an M11 or post-beta decision. Upstream's three city styles take the pools above as written; the candidates group blocks the catalogs suggest.

| Style | Status | Pool emphasis |
|---|---|---|
| `standard` | upstream | All the pools above at their weights |
| `border` (`standard_border`) | upstream | [Red-brick](#red-brick-main-wall) and [quartz](#quartz-main-wall) walls; low single-floor buildings suit red brick bonds and marble |
| `desert` | upstream | The desert pools; Railcraft sandy, Ztones Roen and Sols, Chisel sandstone, red sandstone |
| Modern and office | candidate | Chisel laboratory blocks, marble and steel-frame glass; Ztones Lave and Ztyl panels; smokey quartz; tinted industrial and future glass; flat lamps; glass sliding doors; office furniture from BiblioCraft and OpenComputers |
| Industrial | candidate, after beta with the casings | The [reserved](#reserved-for-the-gtnh-themed-buildings) GT casings; Chisel factory, technical, sheet metal and warning signs; Ztones Tank and Korp; OpenModularTurrets hard walls; GT frame boxes and sheetmetal; catwalks; factory and garage doors; hazard markings |
| Ancient and ruined | candidate | Chisel temple blocks, marble pillars and sandstone scribbles; Thaumcraft ancient stone; ravaged brick, underbrick and old brick; overgrowth weighted high; weathered asphalt |
| Old town | candidate | Ztones Kryp, Lair and Fort; old brick; Botania roof tile and thatch; Railcraft lanterns on posts; Storage Drawers timber trims |
| Mediterranean, waterfront, Asian, arctic, arcane quarter, tech or neon district | candidate | Single catalogs suggest them ([botania.md](botania.md#new-styles-botania-suggests), [chisel.md](chisel.md#best-variety-pools), [ztones.md](ztones.md#new-city-styles-ztones-suggests), [thaumcraft.md](thaumcraft.md#best-variety-pools)) |

## Gaps

Upstream roles where the pack still has no good block:

- **Flat terracotta in upstream's exact colors without a boulder host.** Et Futurum concrete, hempcrete and Ztones are close but read as concrete or render; Chisel's terracotta is patterned. Terracotta stays a boulder host on D18's terms.
- **Polished-diorite stairs and slabs** for the oil rig: Botania has none for its polished stones, and Et Futurum's are off.
- **A gray metal trapdoor without a tile entity.** MalisisDoors' iron trapdoor has one; Et Futurum's copper trapdoors are copper-colored.
- **Asphalt with lane markings that is not a speed block.** Chisel's road blocks and GT concrete all speed players up; for side streets the only asphalt looks without speed are Galacticraft's asteroid rock and GalaxySpace's Callisto surface, and lane paint comes from overlays.
- **Cracked and mossy forms for most pack bricks.** Railcraft, Chisel and Ztones bricks have none, so their rubble borrows cobble, vanilla slabs and stairs.
- **Stairs and slabs for most pack walls without a tile entity.** Railcraft's are tile entities, and Ztones, most Chisel groups and GT stones have none.
- **Panes for tinted and framed glass.** Ztones Glaxx, GT tinted glass, Railcraft strengthened glass, Witchery shaded glass and EnderIO glass are blocks only.
- **Furniture without a tile entity**: no tables or chairs; only Natura's bookshelves and workbenches, HarvestCraft's sinks and candles and CookingForBlockheads' floors are plain blocks.
- **A ceiling light that is lit and looks like a fluorescent panel.** FloodLights and IC2 luminators go dark unpowered; Ztones' flat lamps are the nearest.
- **True roof slopes** need ArchitectureCraft or Carpenter's Blocks, both tile entities with their shape in NBT.

## Prerequisites for the asset system

What M02 must support before these pools can become palettes ([ROADMAP](../../../ROADMAP.md)):

Status after M02: items 2, 3, 7 and 11 are in the asset system (exact registry names with spaces, pipes and dots; metadata up to 32767 with an extended-metadata flag; multi-step fallback chains reported in the asset report; one `decorativeMetals.<metal>` flag per metal, resolved per world). Item 1 is in the format (a `tag` template on any palette entry, and on map rows such as the potted cactus) and waits for the writer (M04, M05). The pools themselves are loaded from `data/lostcitiesnh/blockmap/pools.json`, which `tools/asset-convert/pools_from_catalog.py` builds from the tables below, and resolved against the registry: in GTNH 2.9.0 every candidate block named in the pool tables is registered. Item 5 is in only for dye-order colors (`{color}` rows); per-mod color orders, the per-building accent cap and the spawn-proof floor share (items 9 and 11) are M11's, with the pools becoming palettes.

1. **Tile-entity data in palette entries.** Upstream's palettes already carry block-entity data for chests, spawners and the furnace; generalize it to a per-entry NBT template, with facing derived from the part's rotation and item names resolved to IDs at placement. Needed for Binnie (`meta`), Forestry woods and candles (`WT`), Railcraft slabs and stairs (material), BiblioCraft (facing keys), Carpenter's Blocks (`cbAttrList`, `cbMetadata`), ArchitectureCraft (shape and material), FloodLights (`teDirection`), ProjectRed lamps (`inv`), Computronics lamps (`clc`), OpenBlocks flags, banners, Storage Drawers (`Dir`), JABBA and Iron Chests facing, and IC2 foam walls. Blocks whose tile entity `setBlock` creates correctly by itself (MalisisDoors doors, the canvas, most kitchen furniture) need no data, but the writer must not assume they are tile-entity free.
2. **Exact registry names.** The block-name map keeps every name verbatim: `tile.` prefixes (`appliedenergistics2:tile.BlockSkyStone`, `Ztones:tile.agonBlock`, `modernmarkings:tile.floor_marking_white_lane`, `TwilightForest:tile.TFPlank`, `GalacticraftCore:tile.gcBlockCore`, `GalacticraftAmunRa:tile.baseBlockRock`, `thaumicinsurgence:tile.arcaneMarble`, and *unverified* `vendingmachine:tile.casing`), dotted names (`gregtech:gt.blockcasings`, `Railcraft:brick.abyssal`, `Natura:Natura.fence`), spaces (`Natura:Rare Tree`, `BiblioCraft:Armor Stand`, `BiblioCraft:Printing Press`), pipes in mod IDs (`ProjRed|Exploration:projectred.exploration.stone`), concatenated names (`AWWayofTime:AlchemicalWizardrybloodRune`), and case-sensitive mod IDs (`Botania`, `Ztones`, `TConstruct`, `etfuturum`, `chisel`).
3. **Extended metadata.** GT frame boxes (`gregtech:gt.blockframes:305`), sheetmetal (`gregtech:gt.sheetmetal:<material id>`) and their bartworks counterparts use the GT material ID as metadata, up to about 1000. That works only through EndlessIDs' extended block metadata: the asset format accepts metadata above 15, and the writer uses an API that stores it (`setBlock` does; writing nibble arrays does not).
4. **Tinted blocks.** GT frame boxes and sheetmetal (material color), GT casings (machine-metal color), Binnie ceramics, glass, doors and bark, and OpenComputers chamelium are colored at render time. The contact sheets approximate them; the screenshot reviews judge them in game.
5. **Color numbering.** Asset files name colors, and the block map turns a color into each block's own metadata. Most mods use dye order (0 white to 15 black), but IC2 foam walls (`IC2:blockWall`) and OpenComputers chamelium use dye-item order (0 black, 15 white), and GalaxySpace future glass is reversed too (0 black, 15 white). Railcraft strengthened glass and Witchery shaded glass are in wool order despite reversing internally.
6. **Blocks that need a specific world call or order.**
   - Railcraft tracks keep their kind in a tile entity that `setBlock` does not create; the supported call is `TrackTools.placeTrack`, falling back to vanilla rail ([integration-opportunities.md](../../00-pack-integration/integration-opportunities.md#railways-and-stations)).
   - MalisisDoors' `carriage_door`, `medieval_door` and `rustyHatch` build their multiblock in `onBlockPlacedBy`, which needs a placing entity; they stay out. Garage doors are written as a full column.
   - Binnie and Forestry blocks get their tile data set after placement, or a missing tile shows the default variant and drops it.
   - GT casings, frame boxes, sheetmetal and Plascrete queue a machine-update scan from `onBlockAdded` when placed after `provideChunk`.
   - Overlays (ModernMarkings, Chisel `road_line`, Ztones flat lamps, plants, carpets) are placed after the block that supports them; doors write both halves; leaves carry their no-decay bit.
7. **Fallback chains of more than one step** (mod block, second mod block, vanilla), with the dev self-check reporting each fallback taken.
8. **Primary roads as a layout fact.** Speed blocks are walked on only on highways and primary roads, while upstream uses `S` for every street and for building floors. The city style needs a separate street block or character for primary roads, which M04 and M06 provide.
9. **Per-building palette choice with weights**, per style, with the spawn-proof floor share as a tunable (D28 rule 1).
10. **A "shape of material" palette entry** (ArchitectureCraft, Carpenter's Blocks) for true roof slopes and cornices, after beta.
11. **Decorative metal flags (D29).** One `decorativeMetals.<metal>` worldgen flag per metal in the [decorative metals](#decorative-metals-d29) table, pinned into the world profile at creation, and a per-building cap on metal accents that exempts the cabin and the gas station and restaurant parking. A palette entry marked DM resolves to its fallback when its flag is off.
