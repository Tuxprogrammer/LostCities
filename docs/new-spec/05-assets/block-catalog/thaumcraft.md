# Block catalog: Thaumcraft

Thaumcraft 4.2.3.5 (`Thaumcraft-1.7.10-4.2.3.5.jar`, closed source) in GTNH 2.9.0. Read from the Vineflower decompile in `gtnh-src/mods/Thaumcraft/` (`thaumcraft/common/config/ConfigBlocks.java`, `thaumcraft/common/blocks/*`), the pack's shipped configs in `gtnh-src/config/`, and the textures in the jar. Part of the decorative block catalog (decision log D25). The Thaumcraft add-ons (Thaumic Bases, Thaumic Tinkerer, Automagy and the rest) have their own catalog, [thaumic-addons.md](thaumic-addons.md). Contact sheets: `tools/block-contact-sheet/sheets/thaumcraft/*.json`, rendered to `docs/previews/blocks/thaumcraft/`.

## Summary

- **Thaumcraft registers 45 blocks, all unconditionally**; nothing in `config/Thaumcraft.cfg` removes a block, and GTNH's TC4Tweaks registers none and changes no registration. The mod ID is `Thaumcraft`; every registry name is `Thaumcraft:<name>` as passed to `GameRegistry.registerBlock` (`blockCosmeticSolid`, `blockWoodenDevice`, …). **58 block+meta rows** are catalogued (a row can cover a meta range, such as the 16 candle colors or the stair facings); fit with upstream's look: 15 yes, 27 partly, 16 no.
- **Inclusion rule.** A block gets a row when a city could hold it as building material, a light, a plant, furniture or loot: every static decorative block, plus lights, candles, tables, banners, urns and crates, plants and the taint blocks (as off-theme, with the reason). **Excluded without a row**: machines and devices (crucible, alembics, infusion pillars, pedestals, tubes, jars, mirrors, bellows, arcane workbench and research table, thaumatorium, essentia reservoir, golem blocks), fluids (`blockFluxGoo`, `blockFluxGas`, `blockFluidPure`, `blockFluidDeath`), portals and technical air blocks (aura nodes `blockAiry:0`/`5`, warding and fields `blockAiry:4`/`10`-`12`, `blockHole`, `blockPortalEldritch`, `blockEldritchNothing`), the node blocks (`blockCosmeticSolid:8` charged totem, `blockMagicalLog:2` silverwood knot), eldritch structure blocks with tile entities (`blockEldritch:0`-`3`, `8`-`10`: altar, obelisk, capstone, lock, crab spawner, trap), Thaumcraft's ores (`blockCustomOre`: cinnabar, infused stones, amber) and Ethereal Bloom (`blockCustomPlant:4`, a taint-cleansing tile entity).
- **GT hosts:** none. No Thaumcraft block is a GT ore or boulder host ([ore-veins.md](../../00-pack-integration/ore-veins.md)).
- **Gameplay flags.**
  - *Light:* Glimmer of Light (15, invisible), Arcane Lamp (15), Nitor (15), candles (14), eldritch 4, 5, 7 (12), travel paving (9), silverwood leaves (7), shimmerleaf, cinderpearl, vishroom and silverwood sapling (8), crusted stone (4). Thaumcraft's own amber emits **no** light (Automagy's glowing amber does, [thaumic-addons.md](thaumic-addons.md#automagy)).
  - *Tile entities:* warding paving, warded glass and door, arcane lamp, nitor, crystal clusters, table, banner. Banners need NBT (color) at placement.
  - *Gravity:* crusted taint (falls and slides).
  - *Unbreakable or nearly so:* warded glass and the arcane door (hardness -1 with the pack's `allow_warded_stone=true`), eldritch blocks (hardness 50, blast resistance 20000), obsidian totem and tile (hardness 30, blast resistance 999). Generation never places unbreakable blocks, so warded blocks are out.
  - *Spreading:* crusted taint, tainted soil and tainted grass spread fibrous taint and the taint biome. Never place them.
  - *Movement:* travel paving gives Speed II and Jump Boost and is never placed; mobs cannot spawn on travel or warding paving.
  - *Transparency:* warded glass (connected texture, 47 faces); leaves; candle, lamp, urn, crate, table and banner are models.
  - *Built-in variety:* Ancient Stone (`blockCosmeticSolid:11`) picks one of four faces by position, Ancient Rock (`:12`) a 2x2 macro texture, and the obsidian totem changes by its neighbors above and below. This is position-based texturing, not CTM.
- **Look.** Thaumcraft's palette is gray arcane stone, dark slate, olive-tan ancient stone, dark and pale magic woods and purple-black obsidian. The gray stone, slate and greatwood fit an older city directly; the rest suits a new **arcane quarter** style (amber, silverwood, candles, banners) and **eldritch ruins** (ancient stone, crusted and glowing eldritch stone, urns).

Catalog rows per role (the column counts rows, and a row can hold several roles and cover a meta range):

| Role | Blocks+meta |
|---|---|
| `wall-main` | 8 |
| `wall-accent` | 7 |
| `trim-pillar` | 4 |
| `floor` | 7 |
| `ceiling` | 1 |
| `glass` | 1 |
| `sidewalk` | 1 |
| `stairs-slab` | 14 |
| `rubble-ruin` | 5 |
| `light` | 8 |
| `door-trap` | 1 |
| `furniture-deco` | 7 |
| `foliage` | 9 |
| `special` | 6 |
| `off-theme` | 10 |

### Gaps this mod fills

- **A second stone-brick family with stairs and slab**: arcane stone bricks (Thaumic Insurgence's pale arcane marble bricks are a third, [thaumic-addons.md](thaumic-addons.md)).
- **Lights.** Et Futurum's sea lantern is off in the pack (`enablePrismarine=false`). Thaumcraft's Glimmer of Light is an invisible light-15 block with no tile entity, a hidden ceiling light; the arcane lamp is a street or hall lamp.
- **Ready-made ruin loot.** Old urns and abandoned crates drop tiered Thaumcraft loot when broken and need no tile entity or loot-table NBT.

## Blocks

### Thaumcraft: cosmetic stone (`blockCosmeticSolid`), slabs and stairs

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Thaumcraft:blockCosmeticSolid:0` | Obsidian Totem | `off-theme` | no | new: eldritch ruin | black-purple pillar with carved faces when stacked (texture chosen by the blocks above and below); hardness 30, blast resistance 999 |
| `Thaumcraft:blockCosmeticSolid:1` | Obsidian Tile | `floor`, `wall-accent` | partly | standard, new: arcane quarter | black-purple obsidian tiles; hardness 30 and blast resistance 999, so players can hardly strip it |
| `Thaumcraft:blockCosmeticSolid:2` | Paving Stone of Travel | `special` | no | new: arcane quarter | gray paving with a green inset; light 9; gives Speed II and Jump Boost to anything walking on it (`BlockCosmeticSolid.onEntityWalking`); mobs cannot spawn on it. A paving block that changes movement is a gameplay hazard in a street: never placed, like Thaumic Tinkerer's travel slab and stairs ([thaumic-addons.md](thaumic-addons.md)) |
| `Thaumcraft:blockCosmeticSolid:3` | Paving Stone of Warding | `sidewalk` | partly | new: arcane quarter | gray paving with a red inset; tile entity (warding: keeps mobs out while unpowered); mobs cannot spawn on it |
| `Thaumcraft:blockCosmeticSolid:4` | Thaumium Block | `wall-accent`, `special` | partly | new: arcane quarter | smooth violet metal; storage block (9 thaumium ingots), in one carving group with Chisel's thaumium variants: an accent behind `decorativeMetals.thaumium` under the per-building cap ([D29](index.md#decorative-metals-d29)); beacon base |
| `Thaumcraft:blockCosmeticSolid:5` | Tallow Block | `wall-accent`, `wall-main` | partly | desert | pale cream, waxy; top differs from the sides; reads as cream render |
| `Thaumcraft:blockCosmeticSolid:6` | Arcane Stone Block | `wall-main`, `floor` | yes | standard, border | gray mottled smooth stone; hardness 4; a plain-stone variant beside smooth stone |
| `Thaumcraft:blockCosmeticSolid:7` | Arcane Stone Bricks | `wall-main`, `trim-pillar`, `stairs-slab` | yes | standard, border | gray stone bricks, four large bricks per face; stairs and slab exist; the closest Thaumcraft block to vanilla stone bricks |
| `Thaumcraft:blockCosmeticSolid:9` | Golem Fetter | `off-theme` | no | - | stone with a fetter top; stops golems (gameplay block) |
| `Thaumcraft:blockCosmeticSolid:10` | Active Golem Fetter | `off-theme` | no | - | powered state of meta 9; not in the creative list; drops meta 9 |
| `Thaumcraft:blockCosmeticSolid:11` | Ancient Stone | `wall-main`, `rubble-ruin`, `stairs-slab` | partly | desert, new: eldritch ruin, new: old town | olive-tan rough stone; each face picks one of four textures from its position, so walls vary on their own; stairs and slab exist |
| `Thaumcraft:blockCosmeticSolid:12` | Ancient Rock | `wall-main`, `wall-accent` | yes | standard, new: old town | dark blue-gray slate brickwork; a 2x2-block macro texture chosen by position (a built-in connected look) |
| `Thaumcraft:blockCosmeticSolid:13` | Ancient Stone | `wall-main`, `rubble-ruin` | partly | desert, new: eldritch ruin | same look as meta 11; not in the creative list (the Eldritch dimension's variant) |
| `Thaumcraft:blockCosmeticSolid:14` | Crusted Stone | `rubble-ruin` | partly | desert, new: eldritch ruin | tan crusty stone; light 4 |
| `Thaumcraft:blockCosmeticSolid:15` | Ancient Stone Pedestal | `trim-pillar`, `furniture-deco` | partly | new: eldritch ruin, new: old town | ancient stone with a banded side; a plinth or column base |
| `Thaumcraft:blockCosmeticSlabStone:0, 8` | Arcane Stone Slab | `stairs-slab`, `floor` | yes | standard, border | bottom 0, top 8; double slab `Thaumcraft:blockCosmeticDoubleSlabStone:0` |
| `Thaumcraft:blockCosmeticSlabStone:1, 9` | Ancient Stone Slab | `stairs-slab` | partly | desert, new: eldritch ruin | bottom 1, top 9; double slab `Thaumcraft:blockCosmeticDoubleSlabStone:1` |
| `Thaumcraft:blockCosmeticDoubleSlabStone:0-1` | Arcane / Ancient Stone double slab | `wall-main`, `stairs-slab` | yes | standard, desert | full-block double slabs of the two above; no item form |
| `Thaumcraft:blockStairsArcaneStone:0-7` | Arcane Stone Stairs | `stairs-slab` | yes | standard, border | stair facing metas as vanilla |
| `Thaumcraft:blockStairsEldritch:0-7` | Ancient Stone Stairs | `stairs-slab` | partly | desert, new: eldritch ruin |  |

### Thaumcraft: amber, warded glass, eldritch and taint blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Thaumcraft:blockCosmeticOpaque:0` | Amber Block | `wall-accent`, `special` | partly | new: arcane quarter | glowing-orange amber; partly transparent to light (opacity 3) but emits none; reads as backlit onyx in a lobby |
| `Thaumcraft:blockCosmeticOpaque:1` | Amber Bricks | `wall-accent` | partly | new: arcane quarter | amber in four bricks |
| `Thaumcraft:blockCosmeticOpaque:2` | Warded Glass | `glass` | no | - | white frosted glass in a brown rune frame; connected texture (47 faces); owner tile entity, blast resistance 999 and, with the pack's `allow_warded_stone=true`, hardness -1 (unbreakable): never place it |
| `Thaumcraft:blockWarded:-` | Warded block | `off-theme` | no | - | not placeable on its own: wraps another block (tile entity) and makes it unbreakable |
| `Thaumcraft:blockEldritch:4` | Glowing Crusted Stone | `light`, `special` | no | new: eldritch ruin | animated crusted stone, light 12; hardness 50, blast resistance 20000, mobs cannot spawn on it; ticks randomly |
| `Thaumcraft:blockEldritch:5` | Glyphed Stone | `light`, `special` | no | new: eldritch ruin | dark glyph stone, light 12; as meta 4 |
| `Thaumcraft:blockEldritch:6` | (unnamed eldritch deco) | `off-theme` | no | new: eldritch ruin | black-purple lattice framed panel, light 5; as meta 4; no lang name, not in the creative list |
| `Thaumcraft:blockEldritch:7` | Ancient Doorway | `off-theme` | no | new: eldritch ruin | black-purple lattice framed panel, light 12; as meta 4; no tile entity |
| `Thaumcraft:blockTaint:0` | Crusted Taint | `off-theme` | no | - | purple crust; falls like sand and spreads fibrous taint and taint biome; spawns taint spores |
| `Thaumcraft:blockTaint:1` | Tainted Soil | `off-theme` | no | - | spreads fibrous taint and taint biome |
| `Thaumcraft:blockTaint:2` | Block of Flesh | `off-theme` | no | - | pink flesh; inert (no spread) |
| `Thaumcraft:blockTaintFibres:1` | Tainted Grass | `off-theme` | no | - | taint plant; spreads |

### Thaumcraft: wood, plants and furniture

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Thaumcraft:blockWoodenDevice:6` | Greatwood Planks | `floor`, `wall-main`, `ceiling`, `stairs-slab` | yes | standard, new: old town | dark brown vertical boards, close to dark oak; stairs and slab exist |
| `Thaumcraft:blockWoodenDevice:7` | Silverwood Planks | `floor`, `wall-accent`, `stairs-slab` | partly | new: arcane quarter | white wood with a carved roundel on every face; a pale parquet |
| `Thaumcraft:blockCosmeticSlabWood:0, 8` | Greatwood Slab | `stairs-slab`, `floor` | yes | standard, new: old town | double slab `Thaumcraft:blockCosmeticDoubleSlabWood:0` |
| `Thaumcraft:blockCosmeticSlabWood:1, 9` | Silverwood Slab | `stairs-slab` | partly | new: arcane quarter | double slab `Thaumcraft:blockCosmeticDoubleSlabWood:1` |
| `Thaumcraft:blockCosmeticDoubleSlabWood:0-1` | Greatwood / Silverwood double slab | `floor`, `stairs-slab` | partly | standard | no item form |
| `Thaumcraft:blockStairsGreatwood:0-7` | Greatwood Stairs | `stairs-slab` | yes | standard, new: old town |  |
| `Thaumcraft:blockStairsSilverwood:0-7` | Silverwood Stairs | `stairs-slab` | partly | new: arcane quarter |  |
| `Thaumcraft:blockMagicalLog:0 (+4, +8 axis)` | Greatwood Log | `trim-pillar`, `foliage` | partly | new: park, new: old town | dark bark; a beam or a park tree trunk |
| `Thaumcraft:blockMagicalLog:1 (+4, +8 axis)` | Silverwood Log | `trim-pillar`, `foliage` | partly | new: park, new: arcane quarter | white bark; no light (only the node-bearing knot, meta 2, emits light 7, and it is excluded) |
| `Thaumcraft:blockMagicalLeaves:0 (+4, +8 decay bits)` | Greatwood Leaves | `foliage` | partly | new: park | biome-tinted like oak (renders gray on the sheet); decays without a log nearby unless the no-decay bit is set |
| `Thaumcraft:blockMagicalLeaves:1 (+4, +8 decay bits)` | Silverwood Leaves | `foliage`, `light` | partly | new: park, new: arcane quarter | fixed blue-gray tint, light 7 |
| `Thaumcraft:blockCustomPlant:0` | Greatwood Sapling | `foliage` | partly | new: park | grows into a large greatwood |
| `Thaumcraft:blockCustomPlant:1` | Silverwood Sapling | `foliage` | partly | new: park | light 8; grows a silverwood with an aura node |
| `Thaumcraft:blockCustomPlant:2` | Shimmerleaf | `foliage` | partly | new: park | silver-leaved flower; light 8 |
| `Thaumcraft:blockCustomPlant:3` | Cinderpearl | `foliage` | partly | new: park, desert | orange desert flower; light 8 |
| `Thaumcraft:blockCustomPlant:5` | Vishroom | `foliage` | no | new: eldritch ruin | purple mushroom, light 8; gives Nausea on contact |
| `Thaumcraft:blockTable:0` | Table | `furniture-deco` | yes | standard, new: old town | plain wooden table model; tile entity (`TileTable`); a wand turns it into a research table, not a hazard |
| `Thaumcraft:blockWoodenDevice:8` | Banner (16 colors, Crimson Cult) | `furniture-deco` | partly | new: old town, new: arcane quarter | cloth banner model; tile entity holding color and facing in NBT, so it needs block-entity data at placement |
| `Thaumcraft:blockArcaneDoor:door halves` | Arcane Door | `door-trap` | no | - | warded iron door with an owner tile entity; unbreakable in the pack's config |

### Thaumcraft: lights and loot

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Thaumcraft:blockCandle:0-15` | Tallow Candle (16 dye colors) | `light`, `furniture-deco` | yes | standard, new: old town, new: arcane quarter | small candle, light 14; dye-tinted white texture; no tile entity; needs a solid block below |
| `Thaumcraft:blockMetalDevice:7` | Arcane Lamp | `light` | yes | standard, new: arcane quarter | black iron cage around a gold glow, light 15; tile entity that also places invisible Glimmer of Light blocks in dark spots nearby |
| `Thaumcraft:blockAiry:1` | Nitor | `light` | partly | new: arcane quarter | floating flame sprite, light 15, no collision; tile entity (`TileNitor`) |
| `Thaumcraft:blockAiry:2-3` | Glimmer of Light | `light`, `special` | yes | standard, border, desert | invisible, no collision, light 15, no tile entity, persists; the arcane lamp's light block. An invisible ceiling light for any building |
| `Thaumcraft:blockCrystal:0-6` | Crystal Cluster (Air ... Mixed) | `light`, `furniture-deco` | partly | new: eldritch ruin, new: arcane quarter | colored crystal model, light 7; tile entity (`TileCrystal`); breaking drops vis shards (loot) |
| `Thaumcraft:blockLootUrn:0-2` | Old Urn (common, uncommon, rare) | `rubble-ruin`, `furniture-deco` | yes | standard, desert, new: eldritch ruin | clay urn model, no tile entity; breaking it drops Thaumcraft loot by tier (meta). Ready-made ruin loot |
| `Thaumcraft:blockLootCrate:0-2` | Abandoned Crate (common, uncommon, rare) | `rubble-ruin`, `furniture-deco` | yes | standard, border | wooden crate model, no tile entity; drops loot by tier when broken. Fits warehouses and ruined shops |

## Best variety pools

Weights are not set here. Each pool is variety in addition to the D19 baseline. The add-ons' candidates (smokey quartz, ancient bricks and cobblestone, glowing amber, frosted crystal glass and the rest) are in [thaumic-addons.md](thaumic-addons.md#best-variety-pools).

| Upstream role (palette char) | Baseline (D19) | Thaumcraft variety |
|---|---|---|
| Stone-brick wall `#` (`bricks_standard`, `bricks_border`) | `minecraft:stonebrick` variants | `Thaumcraft:blockCosmeticSolid:7` (arcane stone bricks), `:6` (arcane stone), `:12` (ancient rock, dark slate) |
| Accent `X` and band `$` | plain stone brick, smooth stone double slab | `Thaumcraft:blockCosmeticDoubleSlabStone:0`; `Thaumcraft:blockCosmeticSolid:12` |
| Pillars and trims | stone brick, quartz pillar | `Thaumcraft:blockCosmeticSolid:15`; greatwood logs as beams |
| Stairs `A t z c d`, `/` | stone-brick and stone stairs | `Thaumcraft:blockStairsArcaneStone`, `Thaumcraft:blockStairsGreatwood` |
| Floors and slabs `_` `=` | smooth stone slab | `Thaumcraft:blockCosmeticSlabStone:0`, `blockCosmeticSlabWood:0`; greatwood planks for interiors |
| Light `h` (glowstone) | `minecraft:glowstone` | `Thaumcraft:blockAiry:2` (invisible); `Thaumcraft:blockMetalDevice:7` (lamp); candles |
| Rubble `}` and ruins | the wall's rubble variant | `Thaumcraft:blockCosmeticSolid:11`, `:14`; urns and crates as loot |
| Desert walls | cut sandstone, terracotta | `Thaumcraft:blockCosmeticSolid:11` and its stairs and slab; `:5` (tallow) |
| Furniture `F` and stuff | furnace, crafting table, brewing stand, anvil, cauldron, enchanting table | `Thaumcraft:blockTable:0`; `Thaumcraft:blockLootCrate`, `blockLootUrn`; candles; banners (with NBT) |
| Parks `p` | flowers, saplings | `Thaumcraft:blockCustomPlant:2`, `:3` |
| New: arcane quarter | (none) | amber blocks, silverwood planks and stairs, obsidian tile, warding paving, banners, candles, arcane lamps; the thaumium block as an accent behind `decorativeMetals.thaumium` (D29) |
| New: eldritch ruins | (none) | ancient stone and rock, crusted stone, eldritch 4-7 (lit, hardness 50: sparing use), urns |

Never placed: travel paving (movement hazard), warded glass and the arcane door (unbreakable), and the taint blocks that spread.
