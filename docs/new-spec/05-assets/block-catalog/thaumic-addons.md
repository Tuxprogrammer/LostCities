# Block catalog: Thaumcraft add-ons

Family D. This catalog owns every Thaumcraft add-on in GTNH 2.9.0: Thaumic Bases, Thaumic Tinkerer, Salis Arcana, Automagy, Thaumic Insurgence, Thaumic Horizons, Thaumic Exploration, Witching Gadgets, Forbidden Magic and Tainted Magic, with one-line verdicts for Thaumic Machina, Gadomancy, Thaumic Energistics and Thaumic Boots. Thaumcraft itself is family B, in [thaumcraft.md](thaumcraft.md). Read from the add-ons' sources or decompiles in `gtnh-src/mods/<mod>/`, the pack's configs in `gtnh-src/config/` and the textures in the jars. Part of the decorative block catalog (decision log D25). Contact sheets: `tools/block-contact-sheet/sheets/thaumic-addons/` (`thaumicbases.json`, `others.json`) and the per-mod folders `thaumicbases`, `thaumictinkerer`, `forbiddenmagic`, `taintedmagic`, `thaumichorizons` and `thaumcraft-addons` (Automagy, Witching Gadgets, Thaumic Insurgence and Thaumic Exploration), rendered to `docs/previews/blocks/<same folder>/` (local only).

## Summary

**108 block+meta rows** across ten mods (a row can cover a meta range, such as the 16 crystal colors or the stair facings); fit with upstream's look: 22 yes, 36 partly, 50 no. No add-on block is switched off by the pack's configs.

| Mod (mod ID, jar) | Registry names | Catalog rows | yes | partly | Verdict |
|---|---|---|---|---|---|
| Thaumic Bases (`thaumicbases`, `Thaumic-Based-1.9.22.jar`) | 53 | 37 | 4 | 17 | **The ancient (beta-texture) blocks: brick, cobblestone, mossy cobblestone, gravel and their slabs.** The best ruin material among the add-ons; glowing crystal panels for an arcane quarter |
| Thaumic Tinkerer (`ThaumicTinkerer`, `ThaumicTinkerer-2.12.34.jar`) | about 40 | 9 | 5 | 1 | **Smokey quartz**: block, chiseled, pillar, slab, stairs |
| Salis Arcana (`salisarcana`, `salisarcana-v2.7.0.jar`) | 1 | 2 | 1 | 1 | Greatwood and silverwood planks as plain blocks |
| Automagy (`Automagy`, `Automagy-0.29.8-GTNH.jar`) | 53 | 6 | 3 | 1 | **Glowing amber** (light 15, no tile entity) and two bookshelves |
| Thaumic Insurgence (`thaumicinsurgence`, `thaumicinsurgence-0.4.1.jar`) | 6 | 2 | 2 | 0 | Arcane marble and marble brick (hardness 10) |
| Thaumic Horizons (`ThaumicHorizons`, `ThaumicHorizons-1.8.28.jar`) | 40 | 34 | 7 | 11 | Frosted crystal glass in the 16 dye colors |
| Thaumic Exploration (`ThaumicExploration`, `Thaumic-Exploration-1.5.29-GTNH.jar`) | 11 | 1 | 0 | 1 | Floating candles (tile entities) at most |
| Witching Gadgets (`WitchingGadgets`, `WitchingGadgets-1.8.53-GTNH.jar`) | 7 | 5 | 0 | 2 | Rose vines; the rest are devices |
| Forbidden Magic (`ForbiddenMagic`, `Forbidden.Magic-0.9.18-GTNH.jar`) | 10 | 8 | 0 | 2 | Black roses; tainted woods and stone are off-theme |
| Tainted Magic (`TaintedMagic`, `Tainted-Magic-7.7.8-GTNH.jar`) | 6 | 4 | 0 | 0 | Warpwood and shadowmetal: off-theme |
| Thaumic Machina (`ThaumicMachina`, `ThaumicMachina-0.2.6-GTNH.jar`) | 3 | 0 | | | Metaphysical brick (near black), rose, node transmodifier: nothing fits |
| Gadomancy (`gadomancy`, `gadomancy-1.5.17.jar`) | 11 | 0 | | | Jars, node machines, pylons; registered under the class simple name |
| Thaumic Energistics (`thaumicenergistics`, `thaumicenergistics-1.7.68-GTNH.jar`) | about 10 | 0 | | | AE2 essentia machines |
| Thaumic Boots (`thaumicboots`, `thaumicboots-1.5.13.jar`) | 0 | 0 | | | No blocks: the jar carries 64 block textures (a copy of Thaumic Insurgence's set) but registers nothing |

Catalog rows per role (a row can hold several roles):

| Role | Rows |
|---|---|
| `wall-main` | 6 |
| `wall-accent` | 35 |
| `trim-pillar` | 6 |
| `floor` | 6 |
| `glass` | 18 |
| `sidewalk` | 1 |
| `stairs-slab` | 9 |
| `rubble-ruin` | 4 |
| `light` | 13 |
| `door-trap` | 1 |
| `furniture-deco` | 5 |
| `foliage` | 8 |
| `special` | 11 |
| `off-theme` | 34 |

- **GT hosts: none.** No add-on block is a GT ore or boulder host ([ore-veins.md](../../00-pack-integration/ore-veins.md)). Thaumic Bases' ancient cobblestone is ore-dictionary `cobblestone` (`TBBlocks.java` line 273), but GT's boulders test `isReplaceableOreGen` against `minecraft:cobblestone` (`WorldgenStone.java` lines 223-229), and the vanilla default of that method is block identity (`Block.java` line 1867). `TBBlockDeco` does not override it, so the old cobblestone is not a boulder host.
- **Gameplay flags.**
  - *Light:* Automagy's glowing amber (15), Thaumic Bases' campfire and brazier (15), Thaumic Horizons' deep crystal (15), Thaumic Bases' crystal blocks and slabs (7), the floating candle (as the tallow candle).
  - *Tile entities:* Thaumic Tinkerer's ward slab and platform, Thaumic Bases' campfire, brazier and spikes, Thaumic Horizons' clouds, the floating candle, Witching Gadgets' ethereal wall, blast furnace and void walkway.
  - *Gravity:* Thaumic Bases' ancient gravel.
  - *Movement:* Thaumic Tinkerer's travel slab and stairs give Speed and Jump Boost: never placed.
  - *Unbreakable:* Thaumic Horizons' evanescent. *Explosive:* Thaumic Horizons' alchemite.
  - *Contact damage:* Thaumic Bases' spikes, Witching Gadgets' rose vines.
  - *Storage blocks:* Thaumic Bases' ancient iron and gold blocks are accents behind the D29 iron and gold flags; the ancient diamond and lapis blocks, quicksilver block and brick, thauminite and void metal, Witching Gadgets' void metal and Tainted Magic's shadowmetal stay loot ([decorative metals](index.md#decorative-metals-d29)).
  - *Taint and flux:* none of the useful blocks spreads taint or emits flux.
- **Without the mods:** each pick names a vanilla fallback. Salis Arcana's planks use Thaumcraft's textures, so they need Thaumcraft too (always present with it in the pack).

### Gaps the add-ons fill

- **A second black polished stone set.** Botania's `quartzTypeDark` (block, chiseled, pillar, slab, stairs) is the pack's dark quartz family ([botania.md](botania.md)); Thaumic Tinkerer's smokey quartz is a second one with the same pieces, so a black modern facade can come from either mod.
- **A third pale stone-brick family**: Thaumic Insurgence's arcane marble bricks, after vanilla stone bricks and Thaumcraft's arcane stone bricks.
- **A second red brick**: Thaumic Bases' ancient bricks (the classic texture) beside `minecraft:brick_block`.
- **A warm lamp block with no tile entity**: Automagy's glowing amber.
- **Colored frosted glass** in the 16 dye colors (Thaumic Horizons' crystal), denser than vanilla stained glass.

## Thaumic Bases

Source read: `mods/Thaumic-Based/tb/init/TBBlocks.java` (definitions lines 63-212, registration 216-336, through DummyCore's `BlocksRegistry.registerBlock` with the literal name), `tb/common/block/BlockHalfSlab.java` (slab metas, lines 22-23), `TBBlockDeco.java`, `BlockBrazier.java`, `tb/init/TBRecipes.java`, NHCore's `ScriptThaumicBases.java`, names from `assets/thaumicbases/lang/en_US.lang`. Pack config `config/Thaumic Bases.cfg`: `allowTobacco`, `enableBloodyMess` (spikes), `enableFlora`, `enableNodeTools` all true. Excluded without a row: the machines (`thaumicAnvil`, `voidAnvil`, `overchanter`, `advAlchFurnace`, `relocator`, `entityDeconstructor`, `nodeManipulator`, `nodeLinker`), `pyrofluid`, and the saplings and crops (`goldenOakSapling`, `plax`, `aurelia`, `metalleat`, `lucrite`, `knose`, `sweed`, `lazullia`, `rainbowCactus`, `redlonStem`, `ashroom`, `flaxium`, `glieonia`, `briar`, `tobacco`, `voidPlant`; the briar and rainbow cactus damage on contact).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `thaumicbases:oldBrick:0` | Ancient Bricks | `wall-main`, `wall-accent`, `stairs-slab` | yes | standard, ruin, new: old town | the classic pre-1.0 red brick: brighter orange-red with heavy pale mortar, a second red brick beside vanilla's; hardness 1; slab `genericSlab:1`. Fallback `minecraft:brick_block` |
| `thaumicbases:oldCobble:0` | Ancient Cobblestone | `rubble-ruin`, `wall-main`, `stairs-slab` | yes | standard, ruin, new: old town | beta cobblestone: lighter, rounder stones; hardness 1; slab `genericSlab:2`. Ore-dictionary `cobblestone`, but its own block, so not a GT boulder host ([Summary](#summary)). Fallback `minecraft:mossy_cobblestone` (plain `minecraft:cobblestone` is a boulder host) |
| `thaumicbases:oldCobbleMossy:0` | Ancient Mossy Cobblestone | `rubble-ruin`, `stairs-slab` | yes | standard, ruin, overgrown, new: old town | slab `genericSlab:3`; overgrown ruins. Fallback `minecraft:mossy_cobblestone` |
| `thaumicbases:oldGravel:0` | Ancient Gravel | `rubble-ruin` | partly | standard, ruin | **falls** (`BlockFalling`): place on solid ground only. Fallback `minecraft:gravel` |
| `thaumicbases:oldIron:0` | Ancient Iron Block | `wall-accent`, `special` | partly | standard | old iron-block texture: light gray metal with a shaded side; ore-dictionary `blockIron`, so a storage block: an accent behind `decorativeMetals.iron` under the per-building cap ([D29](index.md#decorative-metals-d29)); slab `genericSlab:6` (a slab of a storage block, loot only) |
| `thaumicbases:oldGold:0` | Ancient Gold Block | `special` | no | - | ore-dictionary `blockGold`: a storage block, at most an accent behind `decorativeMetals.gold` ([D29](index.md#decorative-metals-d29)); slab `genericSlab:5` |
| `thaumicbases:oldDiamond:0` | Ancient Diamond Block | `special` | no | - | ore-dictionary `blockDiamond`: loot only; slab `genericSlab:4` |
| `thaumicbases:oldLapis:0` | Ancient Lapis Lazuli Block | `special` | no | - | deep blue; ore-dictionary `blockLapis`: loot only; slab `genericSlab:7` |
| `thaumicbases:genericSlab:0-7 (+8 top)` | Slabs: gold-etched obsidian, ancient brick, cobble, mossy cobble, diamond, gold, iron, lapis | `stairs-slab`, `rubble-ruin` | partly | standard, ruin, new: old town | metas follow `BlockHalfSlab.parents`; double slabs `thaumicbases:genericSlab_full:0-7`. Metas 1-3 are the useful ones; 4-7 are slabs of the storage blocks (loot only) |
| `thaumicbases:quicksilverBlock:0` | Quicksilver Block | `special` | no | - | pale silver-gray with blue streaks; nine quicksilver, ore-dictionary `blockQuicksilver`: a storage block, loot only |
| `thaumicbases:quicksilverBrick:0` | Quicksilver Brick | `special` | no | - | pale blue-gray large bricks, a clean facade look, but also ore-dictionary `blockQuicksilver`: four come from four quicksilver blocks and each smelts back to nine quicksilver (`TBRecipes`, NHCore's `ScriptThaumicBases`). A storage block: loot only |
| `thaumicbases:thauminiteBlock:0` | Thauminite Block | `special` | no | - | bright blue metal storage block: loot only |
| `thaumicbases:voidBlock:0` | Void Metal Block | `special` | no | - | near-black violet metal storage block: loot only |
| `thaumicbases:eldritchArk:0` | Gold Etched Obsidian | `off-theme` | no | new: eldritch ruin | black with gold glyph corners; slab `genericSlab:0` |
| `thaumicbases:ironGreatwood:0` | Iron Framed Greatwood | `wall-accent`, `door-trap` | partly | new: old town | dark planks in an iron frame; reads as a crate or barn-door panel |
| `thaumicbases:enderPlanks:0` | Ender Wood Planks | `floor`, `wall-accent` | partly | standard | bright orange planks, ore-dictionary `plankWood`; a warm floor. Fallback `minecraft:planks:4` |
| `thaumicbases:blockSalisMundus:0` | Salis Mundus Block | `off-theme` | no | - | lilac crystalline dust block; sand material but a plain block, so it does not fall |
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
| `thaumicbases:campfire:0` | Mage's Campfire | `light`, `furniture-deco` | partly | new: park, new: eldritch ruin | light 15; tile entity; burns. A squatter camp in a ruin |
| `thaumicbases:brazier:0` | Brazier | `light`, `furniture-deco` | partly | ruin, new: old town | light 15 (`setLightLevel(1)`); tile entity (`TileBrazier`); burns |
| `thaumicbases:spike:0-2` | Spikes (iron, thaumic, void) | `off-theme` | no | - | damages entities; tile entity variants |

## Thaumic Tinkerer

Source read: `mods/ThaumicTinkerer/thaumic/tinkerer/common/registry/TTRegistry.java` (registration by `getBlockName()`, lines 159-276), `common/lib/LibBlockNames.java`, `common/block/quartz/BlockDarkQuartz.java`, `common/block/BlockTravelSlab.java`, `assets/ttinkerer/lang/en_US.lang`. Excluded without a row: the gases (`gaseousLight`, `gaseousShadow`, `nitorGas`: invisible light and dark sources that tick away), `forcefield`, the imbued fires (`fireAir`, `fireWater`, `fireOrder`, `fireChaos`, `fireFire`, `fireEarth`: they spread and transmute), `infusedFarmland`, `infusedGrainBlock`, `bedrockPortal` (a dimension gate), `warpGate` and the machines.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicTinkerer:darkQuartz:0` | Block of Smokey Quartz | `wall-main`, `wall-accent`, `floor`, `stairs-slab` | yes | standard, border, modern | near-black polished quartz with a faint sheen; reads as black granite or black glass cladding; hardness 0.8; stairs and slab exist. Fallback `Botania:quartzTypeDark:0`, then `etfuturum:blackstone:1` |
| `ThaumicTinkerer:darkQuartz:1` | Chiseled Smokey Quartz | `wall-accent`, `trim-pillar` | yes | standard, border | black with an etched maze pattern |
| `ThaumicTinkerer:darkQuartz:2, 3, 4` | Pillar Smokey Quartz (Y, X, Z) | `trim-pillar` | yes | standard, border | black fluted column; meta is the axis, as for vanilla quartz pillars (`BlockDarkQuartz.onBlockPlaced`) |
| `ThaumicTinkerer:darkQuartzSlab:0, 8` | Smokey Quartz Slab | `stairs-slab`, `floor` | yes | standard, border | double slab `ThaumicTinkerer:darkQuartzSlabFull:0` |
| `ThaumicTinkerer:darkQuartzStairs:0-7` | Smokey Quartz Stairs | `stairs-slab` | yes | standard, border |  |
| `ThaumicTinkerer:travelSlab:0, 8` | Paving Slab of Travel | `special` | no | new: arcane quarter | Thaumcraft's travel paving as a slab; Speed and Jump Boost when walked on (`BlockTravelSlab.onEntityWalking`), a gameplay hazard in a street: never placed, like the full block ([thaumcraft.md](thaumcraft.md)); double `travelSlabFull` |
| `ThaumicTinkerer:travelStairs:0-7` | Paving Stair of Travel | `special` | no | new: arcane quarter | Speed and Jump Boost when walked on: never placed |
| `ThaumicTinkerer:wardSlab:0, 8` | Paving Slab of Warding | `sidewalk`, `stairs-slab` | partly | new: arcane quarter | Thaumcraft's warding paving as a slab; tile entity; double `wardSlabFull` |
| `ThaumicTinkerer:platform:-` | Ethereal Platform | `off-theme` | no | - | camouflage tile entity; players pass through from below |

## Salis Arcana

Source read: `mods/salisarcana/dev/rndmorris/salisarcana/common/blocks/CustomBlocks.java`, `BlockPlank.java`. Pack config `config/salisarcana/enhancements.cfg` line 87 `enableLookalikePlanks=true`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `salisarcana:blockCustomPlank:0` | Greatwood Planks | `floor`, `wall-main` | yes | standard, new: old town | dark brown striped planks as a plain block (Thaumcraft's own greatwood planks live in its `blockWoodenDevice` at meta 6, a device block); uses Thaumcraft's texture. Fallback `minecraft:planks:5` |
| `salisarcana:blockCustomPlank:1` | Silverwood Planks | `floor`, `wall-accent` | partly | new: arcane quarter | cream with an ornate carved pattern: parquet or paneling. Fallback `minecraft:planks:2` |

## Automagy

Source read: `mods/Automagy/tuhljin/automagy/blocks/ModBlocks.java` (lines 81-120), `BlockTranslucent.java`, `BlockBookshelfEnchanted.java`, names from `assets/automagy/lang/en_US.lang`. Excluded without a row: the redstone, golem and essentia devices (`blockRedcrystal*`, `blockTorchInversion*`, `blockRemoteComparator`, `blockVisReader`, `blockTally`, `blockHourglass*`, chests, maws, golem blocks, `blockEssentiaLocus*`, `blockInventarium`, `blockUnseenScribe`, `blockReqTome`, `blockNethermind`, `blockNetherRune*`, lures, `blockThirstyTank`, `blockBoiler`, `blockCreativeJar`, mirrors, fluids): mostly tile entities, and lures attract mobs.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Automagy:blockTranslucent:0` | Glowing Amber | `light`, `wall-accent` | yes | standard, new: arcane quarter | Thaumcraft's amber texture, but light 15 (`BlockTranslucent.getLightValue` returns 15 for metas 0 and 1), light opacity 3, hardness 1.5 and no tile entity: a warm, solid light block, a glowstone alternative. Fallback `minecraft:glowstone` |
| `Automagy:blockTranslucent:1` | Glowing Amber Bricks | `light`, `wall-accent` | yes | standard, new: arcane quarter | amber bricks, light 15, as meta 0; the best light block among the add-ons. Fallback `minecraft:glowstone` |
| `Automagy:blockBookshelfEnchanted:0` | Enchanting Bookshelf | `furniture-deco` | yes | standard, new: old town | greatwood ends and colorful books, plain block, no tile entity; library and office variety beside `minecraft:bookshelf`; boosts enchanting tables (four times a bookshelf). Fallback `minecraft:bookshelf` |
| `Automagy:blockBookshelfEnchanted:1` | Ensorcelled Bookshelf | `furniture-deco` | partly | new: arcane quarter | pale silverwood ends; boosts enchanting tables (eight times a bookshelf) |
| `Automagy:blockRunedObsidian:0` | Runed Obsidian | `off-theme` | no | new: eldritch ruin | obsidian with violet runes (Nethermind part) |
| `Automagy:blockMetalGlow:0-1` | Memory Block | `off-theme` | no | - | gold-cross dark panel; functional (memory) |

## Thaumic Insurgence

Source read: `mods/thaumicinsurgence/thaumicinsurgence/main/Config.java` (lines 87-146), `block/BlockArcaneMarble.java`, `block/BlockArcaneMarbleBrick.java`. Registered under `getUnlocalizedName()`, so the names carry `tile.`. Excluded without a row: the infusion parts (`infusionIntercepter`, `matrixAlpha`, `pillarAlpha`, `marblePedestal`), all tile entities.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `thaumicinsurgence:tile.arcaneMarble:0` | Arcane Marble Block | `wall-main`, `floor` | yes | standard, border | smooth pale gray-white marble; **hardness 10**; registry name keeps the `tile.` prefix. Fallback `minecraft:quartz_block:0` |
| `thaumicinsurgence:tile.arcaneMarbleBrick:0` | Arcane Marble Brick | `wall-main`, `trim-pillar` | yes | standard, border | pale marble in four bricks, hardness 10; a light stone-brick variant for civic buildings. Fallback `minecraft:stonebrick:0` |

## Thaumic Horizons

Source read: `mods/ThaumicHorizons/com/kentington/thaumichorizons/common/ThaumicHorizons.java` (lines 598-637), `common/blocks/BlockCrystal.java`, `BlockEvanescent.java`, `BlockLight.java`. Excluded without a row: the invisible tile-entity lights (`light`, `lightSolar`), `brainTH`, `spikeTH`, `spikeWoodTH`, `spikeToothTH` (they damage), `leviathanTH`, `slotTH`, `portalTH`, `gatewayTH`, `voidTH`, `vortexTH` (hazards) and the machines (`synthNode`, dynamos, `soulSieve`, `inspiratron`, `soulforge`, `soulJar`, `bloodInfuser`, `vat*`, `soulBeacon`, `modMatrix`, `transductionAmplifier`, `recombinator`, `vortexStabilizer`, `nodeMonitor`).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicHorizons:crystalTH:0` | White Crystal | `glass`, `wall-accent` | yes | standard, border | dye-colored crystal (`BlockBreakable`): render pass 1, alpha 225 (barely see-through), light opacity 1, no light, hardness 1.75; a frosted colored glass block, milkier than stained glass, in the 16 dye colors (the lang names follow dye order). Fallback `minecraft:stained_glass` of the same color |
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
| `ThaumicHorizons:evanescent:0` | Evanescent | `off-theme` | no | - | dark green translucent pocket-plane block; **unbreakable** (hardness `Float.MAX_VALUE`): never placed |
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

## Thaumic Exploration

Source read: `mods/Thaumic-Exploration/` (block registration). Excluded without a row: the devices (`everfullUrn`, `everburnUrn`, `trashJar`, `boundChest`, `boundJar`, `thinkTankJar`, `meltyIce`, `soulBrazier`, `crucibleSouls`, `replicator`); the urn fills with water and the melty ice melts.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicExploration:floatCandle:0-15` | Floating Candle | `light`, `furniture-deco` | partly | new: arcane quarter, new: old town | Thaumcraft candle that hovers; tile entity, colored by meta; light as the tallow candle. Fallback `minecraft:torch` |

## Witching Gadgets

Excluded without a row: the wall mirror (a two-block tile-entity portal mirror), `WG_StoneDevice:1` (time stone), the wooden and other metal devices and `WG_CustomAir`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `WitchingGadgets:WG_MetalDevice:1` | Void Metal Block | `special` | no | - | uses Thaumcraft's dark metal-base texture: plain dark gray steel; ore-dictionary void metal, so a storage block: loot only (D28 rule 7) |
| `WitchingGadgets:WG_StoneDevice:0` | Ethereal Wall | `off-theme` | no | - | gray framed stone with a violet gem; camouflage tile entity that turns passable |
| `WitchingGadgets:WG_StoneDevice:2` | Infernal Blast Furnace | `wall-accent` | partly | new: old town | dark gray bricks, but a multiblock part with a tile entity |
| `WitchingGadgets:WG_VoidWalkway:0` | Void Walkway | `glass` | no | - | translucent gray; tile entity |
| `WitchingGadgets:WG_RoseVine:0` | Rose Vines | `foliage` | partly | new: park, new: eldritch ruin | thorned vines; 1 damage on contact (`BlockRoseVines` line 66), so out of reach only |

## Forbidden Magic

None of these spreads taint: `BlockStoneTainted` and `BlockPlanksTainted` have no `updateTick`; the purple wood and stone are off-theme anyway. Excluded without a row: `TaintSapling`, `ArcaneCake` and `WrathCage` (a spawner).

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

## Tainted Magic

Excluded without a row: `BlockWarpwoodSapling` and `BlockShadowOre` (an ore).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TaintedMagic:BlockWarpwoodPlanks:0` | Warpwood Planks | `off-theme` | no | new: eldritch ruin | near-black indigo planks |
| `TaintedMagic:BlockWarpwoodLog:0 (+4, +8 axis)` | Warpwood Log | `off-theme` | no | new: eldritch ruin | indigo log with a glowing ring |
| `TaintedMagic:BlockWarpwoodLeaves:0` | Warpwood Leaves | `off-theme` | no | new: eldritch ruin | dark leaves with purple specks |
| `TaintedMagic:BlockShadowmetal:0` | Shadowmetal Block | `off-theme` | no | - | flat near-black indigo; storage block |

## Best variety pools

Weights are not set here. Each pool is variety in addition to the D19 baseline; Thaumcraft's own candidates are in [thaumcraft.md](thaumcraft.md#best-variety-pools).

| Upstream role (palette char) | Baseline (D19) | Add-on variety |
|---|---|---|
| Stone-brick wall `#` | `minecraft:stonebrick` variants | `thaumicinsurgence:tile.arcaneMarbleBrick`, `tile.arcaneMarble` (hardness 10: accents and small civic buildings) |
| Red brick `B`, red-brick walls | `minecraft:brick_block` | `thaumicbases:oldBrick:0` with its slab `genericSlab:1` |
| Accent `X` and band `$` | plain stone brick, smooth stone double slab | `ThaumicTinkerer:darkQuartz:0`, `:1`, `ThaumicTinkerer:darkQuartzSlabFull:0` |
| Quartz `Q`, quartz stairs `(` `)` | `minecraft:quartz_block`, `quartz_stairs` | dark counterpart: `ThaumicTinkerer:darkQuartz:0`-`4`, `darkQuartzStairs` (a black-and-white mix, not a replacement) |
| Pillars and trims | stone brick, quartz pillar | `ThaumicTinkerer:darkQuartz:2`-`4`; Thaumic Bases logs as beams |
| Stairs and slabs | stone-brick stairs, smooth stone slab | `ThaumicTinkerer:darkQuartzStairs`, `darkQuartzSlab` |
| Floors | smooth stone double slab, planks | `salisarcana:blockCustomPlank:0`, `:1`; `thaumicbases:enderPlanks:0` for a warm room |
| Glass `+` | glass, stained glass | `ThaumicHorizons:crystalTH` white, light gray, gray, black, light blue, cyan, blue (frosted) |
| Light `h` (glowstone) | `minecraft:glowstone` | `Automagy:blockTranslucent:0`, `:1` (glowing amber) |
| Rubble `}` and ruins | the wall's rubble variant | `thaumicbases:oldCobble:0`, `oldCobbleMossy:0`, `oldGravel:0` (on solid ground), slabs `genericSlab:1`-`3` |
| Desert walls | cut sandstone, terracotta | `ThaumicHorizons:boneTH` as a sandstone-like accent |
| Bookshelves `L` | `minecraft:bookshelf` | `Automagy:blockBookshelfEnchanted:0` |
| Parks `p` | flowers, saplings | `thaumicbases:genLeaves:1` with `genLogs:0`; `ForbiddenMagic:InkFlower`, `UmbralBush` |
| New: arcane quarter | (none) | Thaumic Bases crystal blocks as glowing panels (light 7), smokey quartz, glowing amber, silverwood planks, the ward slab, floating candles |
| New: eldritch ruins | (none) | ancient cobblestone and gravel, Thaumic Horizons' bone |

Never placed: the travel slab and stairs, spikes, the evanescent block, alchemite, the clouds, portals and gases, and the storage blocks the D29 flags do not cover.

## Worldgen notes

- The useful blocks are plain blocks without tile entities, except the floating candle, the campfire, the brazier and the ward slab.
- Ancient gravel falls; support it.
- Leaves need their no-decay bit.
