# Block catalog: Thaumcraft and its add-ons

Thaumcraft 4.2.3.5 (`Thaumcraft-1.7.10-4.2.3.5.jar`, closed source) and the decorative blocks of the Thaumcraft add-ons in GTNH 2.9.0. Read from the Vineflower decompile in `gtnh-src/mods/Thaumcraft/` (`thaumcraft/common/config/ConfigBlocks.java`, `thaumcraft/common/blocks/*`), the add-ons' sources or decompiles in `gtnh-src/mods/<mod>/`, the pack's shipped configs in `gtnh-src/config/`, and the textures in the jars. Part of the decorative block catalog (decision log D25). Contact sheets: `tools/block-contact-sheet/sheets/thaumcraft/*.json` and one folder per add-on (`thaumicbases`, `thaumictinkerer`, `forbiddenmagic`, `taintedmagic`, `thaumichorizons`, and `thaumcraft-addons` for Automagy, Witching Gadgets, Thaumic Insurgence and Thaumic Exploration), rendered to `docs/previews/blocks/<same folder>/`.

## Summary

- **Thaumcraft registers 45 blocks, all unconditionally**; nothing in `config/Thaumcraft.cfg` removes a block, and GTNH's TC4Tweaks registers none and changes no registration. The mod ID is `Thaumcraft`; every registry name is `Thaumcraft:<name>` as passed to `GameRegistry.registerBlock` (`blockCosmeticSolid`, `blockWoodenDevice`, …). **58 block+meta rows** are catalogued (a row can cover a meta range, such as the 16 candle colors or the stair facings); fit with upstream's look: 15 yes, 27 partly, 16 no.
- **Add-ons:** 105 rows across nine mods. No add-on block is switched off by the pack's configs (Thaumic Bases, Thaumic Tinkerer and Forbidden Magic register their decorative blocks unconditionally).
- **Inclusion rule.** A block gets a row when a city could hold it as building material, a light, a plant, furniture or loot: every static decorative block, plus lights, candles, tables, banners, urns and crates, plants and the taint and pocket-plane blocks (as off-theme, with the reason). **Excluded without a row**: machines and devices (crucible, alembics, infusion pillars, pedestals, tubes, jars, mirrors, bellows, arcane workbench and research table, thaumatorium, essentia reservoir, golem blocks, and every Gadomancy, Thaumic Energistics, Thaumic Machina and Automagy machine), fluids (`blockFluxGoo`, `blockFluxGas`, `blockFluidPure`, `blockFluidDeath`, Thaumic Bases' pyrofluid), portals and technical air blocks (aura nodes `blockAiry:0`/`5`, warding and fields `blockAiry:4`/`10`-`12`, `blockHole`, `blockPortalEldritch`, `blockEldritchNothing`, TT's gaseous light and shadow, imbued fires, Thaumic Horizons' vortex and void), the node blocks (`blockCosmeticSolid:8` charged totem, `blockMagicalLog:2` silverwood knot), eldritch structure blocks with tile entities (`blockEldritch:0`-`3`, `8`-`10`: altar, obelisk, capstone, lock, crab spawner, trap), Thaumcraft's ores (`blockCustomOre`: cinnabar, infused stones, amber; Tainted Magic's shadow ore), crops (Thaumic Bases' plants, TT's infused grain, Thaumic Exploration's taint berries) and Ethereal Bloom (`blockCustomPlant:4`, a taint-cleansing tile entity).
- **GT hosts:** none. No Thaumcraft or add-on block is a GT ore or boulder host ([ore-veins.md](../../00-pack-integration/ore-veins.md)); Thaumic Bases' ancient cobblestone is ore-dictionary `cobblestone` but a different block from `minecraft:cobblestone`, which is what the boulder test compares.
- **Gameplay flags.**
  - *Light:* Glimmer of Light (15, invisible), Arcane Lamp (15), Nitor (15), Automagy's glowing amber (15), candles (14), eldritch 4, 5, 7 (12), travel paving (9), Thaumic Bases' crystal blocks and slabs (7), silverwood leaves (7), shimmerleaf, cinderpearl, vishroom and silverwood sapling (8), crusted stone (4). Thaumcraft's own amber emits **no** light.
  - *Tile entities:* warding paving, warded glass and door, arcane lamp, nitor, crystal clusters, table, banner, TT's ward slab and platform, Thaumic Bases' campfire and spikes, Thaumic Horizons' clouds, Thaumic Exploration's floating candle. Banners need NBT (color) at placement.
  - *Gravity:* crusted taint (falls and slides), Thaumic Bases' ancient gravel.
  - *Unbreakable or nearly so:* warded glass and the arcane door (hardness -1 with the pack's `allow_warded_stone=true`), eldritch blocks (hardness 50, blast resistance 20000), obsidian totem and tile (hardness 30, blast resistance 999). Generation never places unbreakable blocks, so warded blocks are out.
  - *Spreading:* crusted taint, tainted soil and tainted grass spread fibrous taint and the taint biome. Never place them.
  - *Movement:* travel paving (block, TT slab and stairs) gives Speed II and Jump Boost; mobs cannot spawn on travel or warding paving.
  - *Transparency:* warded glass (connected texture, 47 faces), Thaumic Horizons' crystal (render pass 1, alpha 225), Witching Gadgets' void walkway; leaves; candle, lamp, urn, crate, table and banner are models.
  - *Built-in variety:* Ancient Stone (`blockCosmeticSolid:11`) picks one of four faces by position, Ancient Rock (`:12`) a 2x2 macro texture, and the obsidian totem changes by its neighbors above and below. This is position-based texturing, not CTM.
- **Look.** Thaumcraft's palette is gray arcane stone, dark slate, olive-tan ancient stone, dark and pale magic woods and purple-black obsidian. The gray stone, slate and greatwood fit an older city directly; the rest suits a new **arcane quarter** style (amber, silverwood, thaumium, travel paving, candles, banners) and **eldritch ruins** (ancient stone, crusted and glowing eldritch stone, urns).

| Mod | Blocks registered (all enabled) | Catalog rows | yes | partly |
|---|---|---|---|---|
| Thaumcraft | 45 | 58 | 15 | 27 |
| Thaumic Bases | 53 | 36 | 5 | 18 |
| Thaumic Tinkerer | about 36 | 9 | 5 | 3 |
| Forbidden Magic | 10 | 8 | 0 | 2 |
| Tainted Magic | 6 | 4 | 0 | 0 |
| Thaumic Horizons | 40 | 34 | 7 | 11 |
| Automagy | about 50 | 6 | 3 | 1 |
| Witching Gadgets | 7 | 5 | 0 | 3 |
| Thaumic Insurgence | 6 | 2 | 2 | 0 |
| Thaumic Exploration | 14 | 1 | 0 | 1 |
| Gadomancy, Thaumic Energistics, Thaumic Machina, Thaumic Boots | all functional | 0 | | |

Catalog rows per role, all mods in this file (the column counts rows, and a row can hold several roles and cover a meta range):

| Role | Blocks+meta |
|---|---|
| `wall-main` | 14 |
| `wall-accent` | 46 |
| `trim-pillar` | 10 |
| `floor` | 11 |
| `ceiling` | 1 |
| `glass` | 19 |
| `sidewalk` | 4 |
| `stairs-slab` | 26 |
| `rubble-ruin` | 8 |
| `light` | 21 |
| `door-trap` | 2 |
| `furniture-deco` | 11 |
| `foliage` | 17 |
| `special` | 4 |
| `off-theme` | 47 |

### Gaps this family fills

- **A black polished stone with stairs, slab and pillar.** Upstream has no dark cladding; vanilla 1.7.10 has none. Thaumic Tinkerer's smokey quartz is a full quartz family (block, chiseled, pillar, slab, stairs) in near-black: modern office cladding and trims.
- **A second stone-brick family with stairs and slab**: arcane stone bricks, plus Thaumic Insurgence's pale arcane marble bricks.
- **A second red brick**: Thaumic Bases' ancient bricks (classic texture) as variety beside `minecraft:brick_block`.
- **Lights.** Et Futurum's sea lantern is off in the pack (`enablePrismarine=false`). Automagy's glowing amber (light 15, no tile entity) is a warm solid lamp block; Thaumcraft's Glimmer of Light is an invisible light-15 block with no tile entity, a hidden ceiling light; the arcane lamp is a street or hall lamp.
- **Ready-made ruin loot.** Old urns and abandoned crates drop tiered Thaumcraft loot when broken and need no tile entity or loot-table NBT.
- **Colored frosted glass** in the 16 dye colors (Thaumic Horizons' crystal), denser than vanilla stained glass.

## Blocks

### Thaumcraft: cosmetic stone (`blockCosmeticSolid`), slabs and stairs

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Thaumcraft:blockCosmeticSolid:0` | Obsidian Totem | `off-theme` | no | new: eldritch ruin | black-purple pillar with carved faces when stacked (texture chosen by the blocks above and below); hardness 30, blast resistance 999 |
| `Thaumcraft:blockCosmeticSolid:1` | Obsidian Tile | `floor`, `wall-accent` | partly | standard, new: arcane quarter | black-purple obsidian tiles; hardness 30 and blast resistance 999, so players can hardly strip it |
| `Thaumcraft:blockCosmeticSolid:2` | Paving Stone of Travel | `sidewalk`, `light` | partly | standard, new: arcane quarter | gray paving with a green inset; light 9; gives Speed II and Jump Boost to anything walking on it; mobs cannot spawn on it |
| `Thaumcraft:blockCosmeticSolid:3` | Paving Stone of Warding | `sidewalk` | partly | new: arcane quarter | gray paving with a red inset; tile entity (warding: keeps mobs out while unpowered); mobs cannot spawn on it |
| `Thaumcraft:blockCosmeticSolid:4` | Thaumium Block | `wall-accent` | no | new: arcane quarter | smooth violet metal; storage block (9 thaumium ingots, an EV-era loot magnet); beacon base |
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

### Thaumic Bases 1.9.22 (`thaumicbases`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `thaumicbases:oldBrick:0` | Ancient Bricks | `wall-main`, `wall-accent`, `stairs-slab` | yes | standard, new: old town | the classic pre-1.0 red brick with pale mortar; slab `genericSlab:1`. A second red brick beside vanilla's |
| `thaumicbases:oldCobble:0` | Ancient Cobblestone | `rubble-ruin`, `wall-main`, `stairs-slab` | yes | standard, new: old town | old cobblestone texture; slab `genericSlab:2`. Ore-dictionary `cobblestone`, but not `minecraft:cobblestone`, so not a GT boulder host |
| `thaumicbases:oldCobbleMossy:0` | Ancient Mossy Cobblestone | `rubble-ruin`, `stairs-slab` | yes | standard, new: old town | slab `genericSlab:3`; overgrown ruins |
| `thaumicbases:oldGravel:0` | Ancient Gravel | `rubble-ruin` | partly | standard | falls (gravity) |
| `thaumicbases:oldIron:0` | Ancient Iron Block | `wall-accent`, `stairs-slab` | partly | standard | old iron-block texture: light gray metal with a shaded side; ore-dictionary `blockIron`; slab `genericSlab:6` |
| `thaumicbases:oldGold:0` | Ancient Gold Block | `off-theme` | no | - | ore-dictionary `blockGold`: valuable loot; slab `genericSlab:5` |
| `thaumicbases:oldDiamond:0` | Ancient Diamond Block | `off-theme` | no | - | ore-dictionary `blockDiamond`: valuable loot; slab `genericSlab:4` |
| `thaumicbases:oldLapis:0` | Ancient Lapis Block | `wall-accent` | partly | standard | deep blue; ore-dictionary `blockLapis`; slab `genericSlab:7` |
| `thaumicbases:genericSlab:0-7 (+8 top)` | Slabs: etched obsidian, ancient brick, cobble, mossy cobble, diamond, gold, iron, lapis | `stairs-slab` | partly | standard, new: old town | double slabs `thaumicbases:genericSlab_full:0-7`; metas 1-3 and 6 are the useful ones |
| `thaumicbases:quicksilverBlock:0` | Quicksilver Block | `wall-accent` | partly | standard | pale silver-gray with blue streaks; reads as polished metal or marble |
| `thaumicbases:quicksilverBrick:0` | Quicksilver Brick | `wall-main`, `wall-accent` | yes | standard, border | pale blue-gray large bricks; a cool, clean facade brick |
| `thaumicbases:thauminiteBlock:0` | Thauminite Block | `off-theme` | no | new: arcane quarter | bright blue metal storage block |
| `thaumicbases:voidBlock:0` | Void Metal Block | `wall-accent` | no | new: arcane quarter | near-black violet metal storage block |
| `thaumicbases:eldritchArk:0` | Gold Etched Obsidian | `off-theme` | no | new: eldritch ruin | black with gold glyph corners; slab `genericSlab:0` |
| `thaumicbases:ironGreatwood:0` | Iron Framed Greatwood | `wall-accent`, `door-trap` | partly | new: old town | dark planks in an iron frame; reads as a crate or barn-door panel |
| `thaumicbases:enderPlanks:0` | Ender Wood Planks | `floor`, `wall-accent` | partly | standard | bright orange planks; a warm floor |
| `thaumicbases:blockSalisMundus:0` | Salis Mundus Block | `off-theme` | no | - | lilac crystalline dust block |
| `thaumicbases:cryingObsidian:0` | Crying Obsidian | `off-theme` | no | new: eldritch ruin | blue-veined obsidian; sets the player's spawn point when used; hardness 10 |
| `thaumicbases:genLogs:0 (+4, +8 axis)` | Peaceful Wood | `trim-pillar`, `foliage` | partly | new: park | pale birch-like bark |
| `thaumicbases:genLogs:1 (+4, +8 axis)` | Nether Wood | `trim-pillar` | partly | new: old town | very dark bark |
| `thaumicbases:genLogs:2 (+4, +8 axis)` | Ender Wood | `trim-pillar` | no | - | yellow bark |
| `thaumicbases:genLeaves:0` | Golden Oak Leaves | `foliage` | partly | new: park | yellow-gold leaves, colored in the texture |
| `thaumicbases:genLeaves:1` | Peaceful Leaves | `foliage` | yes | new: park, standard | plain green leaves, colored in the texture |
| `thaumicbases:genLeaves:2` | Nether Leaves | `foliage` | no | - | red leaves |
| `thaumicbases:genLeaves:3` | Ender Leaves | `foliage` | no | - | magenta leaves |
| `thaumicbases:crystalBlock:0` | Air Crystal Block | `wall-accent`, `light` | partly | new: arcane quarter | solid flat-colored crystal (yellow, red, blue, green, lilac, slate, violet, dark purple for metas 0-7); light 7; slabs `thaumicbases:crystalSlab:0-7`, doubles `crystalSlab_full`. Glowing colored panels |
| `thaumicbases:crystalBlock:1` | Fire Crystal Block | `wall-accent`, `light` | partly | new: arcane quarter |  |
| `thaumicbases:crystalBlock:2` | Water Crystal Block | `wall-accent`, `light` | partly | new: arcane quarter |  |
| `thaumicbases:crystalBlock:3` | Earth Crystal Block | `wall-accent`, `light` | partly | new: arcane quarter |  |
| `thaumicbases:crystalBlock:4` | Order Crystal Block | `wall-accent`, `light` | partly | new: arcane quarter |  |
| `thaumicbases:crystalBlock:5` | Entropy Crystal Block | `wall-accent`, `light` | partly | new: arcane quarter |  |
| `thaumicbases:crystalBlock:6` | Mixed Crystal Block | `off-theme` | no | new: arcane quarter |  |
| `thaumicbases:crystalBlock:7` | Tainted Crystal Block | `off-theme` | no | new: arcane quarter |  |
| `thaumicbases:crystalSlab:0-7 (+8 top)` | Crystal Slabs | `stairs-slab`, `light` | partly | new: arcane quarter | light 7 |
| `thaumicbases:campfire:0` | Mage's Campfire | `light`, `furniture-deco` | partly | new: park, new: eldritch ruin | light 15; tile entity; burns |
| `thaumicbases:spike:0-2` | Spikes (iron, thaumic, void) | `off-theme` | no | - | damages entities; tile entity variants |

### Thaumic Tinkerer 2.12.34 (`ThaumicTinkerer`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicTinkerer:darkQuartz:0` | Block of Smokey Quartz | `wall-main`, `wall-accent`, `floor`, `stairs-slab` | yes | standard, border | near-black polished quartz with a faint sheen; reads as black granite or black glass cladding; stairs and slab exist |
| `ThaumicTinkerer:darkQuartz:1` | Chiseled Smokey Quartz | `wall-accent`, `trim-pillar` | yes | standard, border | black with an etched maze pattern |
| `ThaumicTinkerer:darkQuartz:2, 3, 4` | Pillar Smokey Quartz (Y, X, Z) | `trim-pillar` | yes | standard, border | black fluted column; meta is the axis |
| `ThaumicTinkerer:darkQuartzSlab:0, 8` | Smokey Quartz Slab | `stairs-slab`, `floor` | yes | standard, border | double slab `ThaumicTinkerer:darkQuartzSlabFull:0` |
| `ThaumicTinkerer:darkQuartzStairs:0-7` | Smokey Quartz Stairs | `stairs-slab` | yes | standard, border |  |
| `ThaumicTinkerer:travelSlab:0, 8` | Paving Slab of Travel | `sidewalk`, `stairs-slab` | partly | new: arcane quarter | Thaumcraft's travel paving as a slab; speed and jump boost when walked on; double `travelSlabFull` |
| `ThaumicTinkerer:travelStairs:0-7` | Paving Stair of Travel | `stairs-slab` | partly | new: arcane quarter | speed and jump boost |
| `ThaumicTinkerer:wardSlab:0, 8` | Paving Slab of Warding | `sidewalk`, `stairs-slab` | partly | new: arcane quarter | tile entity; double `wardSlabFull` |
| `ThaumicTinkerer:platform:-` | Ethereal Platform | `off-theme` | no | - | camouflage tile entity; players pass through from below |

### Forbidden Magic 0.9.18-GTNH (`ForbiddenMagic`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ForbiddenMagic:TaintBlock:0` | Tainted Stone | `off-theme` | no | new: eldritch ruin | purple-veined stone; taint material but does not spread |
| `ForbiddenMagic:TaintBlock:1` | Tainted Stone Bricks | `wall-accent` | no | new: eldritch ruin | purple-veined stone bricks; a tainted-ruin wall |
| `ForbiddenMagic:TaintPlank:0` | Tainted Planks | `off-theme` | no | new: eldritch ruin | dark violet planks |
| `ForbiddenMagic:TaintLog:0 (+4, +8 axis)` | Tainted Log | `off-theme` | no | new: eldritch ruin | violet-ringed log |
| `ForbiddenMagic:TaintLeaves:0` | Tainted Leaves | `off-theme` | no | new: eldritch ruin | purple leaves |
| `ForbiddenMagic:StarBlock:0` | Nether Star Block | `off-theme` | no | - | iridescent white; nine nether stars: never place |
| `ForbiddenMagic:InkFlower:0` | Umbral Rose | `foliage` | partly | new: park, new: eldritch ruin | black rose |
| `ForbiddenMagic:UmbralBush:0` | Umbral Rose Bush | `foliage` | partly | new: park, new: eldritch ruin | two-high black rose bush |

### Tainted Magic 7.7.8-GTNH (`TaintedMagic`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TaintedMagic:BlockWarpwoodPlanks:0` | Warpwood Planks | `off-theme` | no | new: eldritch ruin | near-black indigo planks |
| `TaintedMagic:BlockWarpwoodLog:0 (+4, +8 axis)` | Warpwood Log | `off-theme` | no | new: eldritch ruin | indigo log with a glowing ring |
| `TaintedMagic:BlockWarpwoodLeaves:0` | Warpwood Leaves | `off-theme` | no | new: eldritch ruin | dark leaves with purple specks |
| `TaintedMagic:BlockShadowmetal:0` | Shadowmetal Block | `off-theme` | no | - | flat near-black indigo; storage block |

### Thaumic Horizons 1.8.28 (`ThaumicHorizons`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicHorizons:crystalTH:0` | White Crystal | `glass`, `wall-accent` | yes | standard, border | dye-colored crystal: render pass 1, alpha 225 (barely see-through), light opacity 1, no light; a frosted colored glass block in the 16 dye colors |
| `ThaumicHorizons:crystalTH:1` | Orange Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:2` | Magenta Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:3` | Light blue Crystal | `glass`, `wall-accent` | yes | standard, border |  |
| `ThaumicHorizons:crystalTH:4` | Yellow Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:5` | Lime Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:6` | Pink Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:7` | Gray Crystal | `glass`, `wall-accent` | yes | standard, border |  |
| `ThaumicHorizons:crystalTH:8` | Light gray Crystal | `glass`, `wall-accent` | yes | standard, border |  |
| `ThaumicHorizons:crystalTH:9` | Cyan Crystal | `glass`, `wall-accent` | yes | standard, border |  |
| `ThaumicHorizons:crystalTH:10` | Purple Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:11` | Blue Crystal | `glass`, `wall-accent` | yes | standard, border |  |
| `ThaumicHorizons:crystalTH:12` | Brown Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:13` | Green Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:14` | Red Crystal | `glass`, `wall-accent` | partly | standard, border |  |
| `ThaumicHorizons:crystalTH:15` | Black Crystal | `glass`, `wall-accent` | yes | standard, border |  |
| `ThaumicHorizons:crystalDeep:0` | Deep crystal | `light`, `glass` | no | new: eldritch ruin | animated red crystal glass, light 15 |
| `ThaumicHorizons:boneTH:0` | Bone | `wall-accent` | partly | desert, new: eldritch ruin | tan bone texture, close to sandstone from afar |
| `ThaumicHorizons:fleshTH:0` | Lump of Flesh | `off-theme` | no | - | pink flesh |
| `ThaumicHorizons:chocolate:0` | Block of Chocolate | `wall-accent` | partly | standard | brown with a darker frame; edible block (cake material) |
| `ThaumicHorizons:dustTH:0` | Dust | `off-theme` | no | - | slows entities that move through it |
| `ThaumicHorizons:evanescent:0` | Evanescent | `off-theme` | no | - | dark green translucent; cannot be placed (a client-side pocket-plane block) |
| `ThaumicHorizons:alchemite:0` | Alchemite | `off-theme` | no | - | a TNT variant: explodes |
| `ThaumicHorizons:eyeTH:0` | Eye Cluster | `off-theme` | no | - | eyes |
| `ThaumicHorizons:cloudTH:0` | Cloud | `off-theme` | no | - | no collision, tile entity, effects on contact (acid, lightning and so on); glowing twin `cloudGlowingTH` has light 15 |
| `ThaumicHorizons:cloudTH:1` | Plasma Cloud | `off-theme` | no | - |  |
| `ThaumicHorizons:cloudTH:2` | Thundercloud | `off-theme` | no | - |  |
| `ThaumicHorizons:cloudTH:3` | Acid Cloud | `off-theme` | no | - |  |
| `ThaumicHorizons:cloudTH:4` | Alloy Cloud | `off-theme` | no | - |  |
| `ThaumicHorizons:cloudTH:5` | Tumorous Cloud | `off-theme` | no | - |  |
| `ThaumicHorizons:cloudTH:6` | Vis Cloud | `off-theme` | no | - |  |
| `ThaumicHorizons:cloudTH:7` | Glyph Cloud | `off-theme` | no | - |  |
| `ThaumicHorizons:cloudTH:8` | Spore Cloud | `off-theme` | no | - |  |
| `ThaumicHorizons:cloudTH:9` | Animus Cloud | `off-theme` | no | - |  |

### Automagy 0.29.8-GTNH (`Automagy`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Automagy:blockTranslucent:0` | Glowing Amber | `light`, `wall-accent` | yes | standard, new: arcane quarter | Thaumcraft's amber texture but light 15 and no tile entity: a warm, solid light block (a glowstone alternative) |
| `Automagy:blockTranslucent:1` | Glowing Amber Bricks | `light`, `wall-accent` | yes | standard, new: arcane quarter | amber bricks, light 15; the best light block in this family |
| `Automagy:blockBookshelfEnchanted:0` | Enchanting Bookshelf | `furniture-deco` | yes | standard, new: old town | greatwood bookshelf; counts as enchanting power |
| `Automagy:blockBookshelfEnchanted:1` | Ensorcelled Bookshelf | `furniture-deco` | partly | new: arcane quarter | silverwood bookshelf |
| `Automagy:blockRunedObsidian:0` | Runed Obsidian | `off-theme` | no | new: eldritch ruin | obsidian with violet runes (Nethermind part) |
| `Automagy:blockMetalGlow:0-1` | Memory Block | `off-theme` | no | - | gold-cross dark panel; functional (memory) |

### Witching Gadgets 1.8.53-GTNH (`WitchingGadgets`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `WitchingGadgets:WG_MetalDevice:1` | Void Metal Block | `wall-accent` | partly | standard | uses Thaumcraft's dark metal-base texture: plain dark gray steel; ore-dictionary void metal |
| `WitchingGadgets:WG_StoneDevice:0` | Ethereal Wall | `off-theme` | no | - | gray framed stone with a violet gem; camouflage tile entity that turns passable |
| `WitchingGadgets:WG_StoneDevice:2` | Infernal Blast Furnace | `wall-accent` | partly | new: old town | dark gray bricks, but a multiblock part with a tile entity |
| `WitchingGadgets:WG_VoidWalkway:0` | Void Walkway | `glass` | no | - | translucent gray; tile entity |
| `WitchingGadgets:WG_RoseVine:0` | Rose Vines | `foliage` | partly | new: park, new: eldritch ruin | thorned vines; 1 damage on contact |

### Thaumic Insurgence 0.4.1 (`thaumicinsurgence`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `thaumicinsurgence:tile.arcaneMarble:0` | Arcane Marble Block | `wall-main`, `floor` | yes | standard, border | pale gray marble; registry name keeps the `tile.` prefix |
| `thaumicinsurgence:tile.arcaneMarbleBrick:0` | Arcane Marble Brick | `wall-main`, `trim-pillar` | yes | standard, border | pale gray marble in four bricks; a light stone-brick variant |

### Thaumic Exploration 1.5.29-GTNH (`ThaumicExploration`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicExploration:floatCandle:0-15` | Floating Candle | `light`, `furniture-deco` | partly | new: arcane quarter, new: old town | Thaumcraft candle that hovers; tile entity; light as the tallow candle |

## Best variety pools

Weights are not set here. Each pool is variety in addition to the D19 baseline.

| Upstream role (palette char) | Baseline (D19) | Thaumcraft-family variety |
|---|---|---|
| Stone-brick wall `#` (`bricks_standard`, `bricks_border`) | `minecraft:stonebrick` variants | `Thaumcraft:blockCosmeticSolid:7` (arcane stone bricks), `:6` (arcane stone), `:12` (ancient rock, dark slate); `thaumicinsurgence:tile.arcaneMarbleBrick`, `tile.arcaneMarble`; `thaumicbases:quicksilverBrick` |
| Accent `X` and band `$` | plain stone brick, smooth stone double slab | `ThaumicTinkerer:darkQuartz:0`, `:1`, `ThaumicTinkerer:darkQuartzSlabFull:0`; `Thaumcraft:blockCosmeticDoubleSlabStone:0`; `thaumicbases:oldIron` |
| Red brick `B` | `minecraft:brick_block` | `thaumicbases:oldBrick` |
| Quartz `Q`, quartz stairs `(` `)` | `minecraft:quartz_block`, `quartz_stairs` | dark counterpart: `ThaumicTinkerer:darkQuartz:0`-`4`, `darkQuartzStairs` (a black-and-white mix, not a replacement) |
| Pillars and trims | stone brick, quartz pillar | `ThaumicTinkerer:darkQuartz:2`-`4`; `Thaumcraft:blockCosmeticSolid:15`; greatwood logs as beams |
| Stairs `A t z c d`, `/` | stone-brick and stone stairs | `Thaumcraft:blockStairsArcaneStone`, `ThaumicTinkerer:darkQuartzStairs`, `Thaumcraft:blockStairsGreatwood` |
| Floors and slabs `_` `=` | smooth stone slab | `Thaumcraft:blockCosmeticSlabStone:0`, `blockCosmeticSlabWood:0`, `ThaumicTinkerer:darkQuartzSlab`; greatwood planks for interiors |
| Glass `+` | glass, stained glass | `ThaumicHorizons:crystalTH` white, light gray, gray, black, light blue, cyan, blue (frosted) |
| Light `h` (glowstone) | `minecraft:glowstone` | `Automagy:blockTranslucent:0`, `:1` (glowing amber); `Thaumcraft:blockAiry:2` (invisible); `Thaumcraft:blockMetalDevice:7` (lamp); candles |
| Rubble `}` and ruins | the wall's rubble variant | `Thaumcraft:blockCosmeticSolid:11`, `:14`; `thaumicbases:oldCobble`, `oldCobbleMossy`; urns and crates as loot |
| Desert walls | cut sandstone, terracotta | `Thaumcraft:blockCosmeticSolid:11` and its stairs and slab; `:5` (tallow); `ThaumicHorizons:boneTH` |
| Furniture `F` and stuff | furnace, crafting table, brewing stand, anvil, cauldron, enchanting table | `Thaumcraft:blockTable:0`; `Automagy:blockBookshelfEnchanted:0`; `Thaumcraft:blockLootCrate`, `blockLootUrn`; candles; banners (with NBT) |
| Parks `p` | flowers, saplings | `Thaumcraft:blockCustomPlant:2`, `:3`; `thaumicbases:genLeaves:1` with `genLogs:0`; `ForbiddenMagic:InkFlower` |
| New: arcane quarter | (none) | amber blocks, silverwood planks and stairs, obsidian tile, travel paving, thaumium and crystal blocks as accents, banners, candles, arcane lamps |
| New: eldritch ruins | (none) | ancient stone and rock, crusted stone, eldritch 4-7 (lit, near-unbreakable: sparing use), tainted stone bricks, urns |
