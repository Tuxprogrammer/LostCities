# Thaumcraft add-ons

Family D. Thaumcraft itself is family B; this sheet covers the add-ons in the pack: Thaumic Bases, Thaumic Tinkerer, Salis Arcana, Automagy, Thaumic Insurgence, Thaumic Horizons, Thaumic Exploration, Witching Gadgets, Forbidden Magic, Tainted Magic, Thaumic Machina, Gadomancy, Thaumic Energistics, Thaumic Boots. Sheets: `tools/block-contact-sheet/sheets/thaumic-addons/` (`thaumicbases.json`, `others.json`; previews in `docs/previews/blocks/thaumic-addons/`, local only).

## Summary

| Mod (mod ID, jar) | Registry names | Block+meta entries | Useful | Verdict |
|---|---|---|---|---|
| Thaumic Bases (`thaumicbases`, `Thaumic-Based-1.9.22.jar`) | 53 | about 110 | 12 | **The "old" (beta-texture) blocks: classic brick, cobblestone, mossy cobblestone, gravel, and their slabs.** Best ruin material in this slice |
| Thaumic Tinkerer (`ThaumicTinkerer`, `ThaumicTinkerer-2.12.34.jar`) | about 40 | about 60 | 4 | **Smokey (dark) quartz** block, chiseled, pillar, slab, stairs |
| Salis Arcana (`salisarcana`, `salisarcana-v2.7.0.jar`) | 1 | 2 | 2 | Greatwood and silverwood planks as a plain block |
| Automagy (`Automagy`, `Automagy-0.29.8-GTNH.jar`) | 53 | about 75 | 2 | Two bookshelf variants |
| Thaumic Insurgence (`thaumicinsurgence`, `thaumicinsurgence-0.4.1.jar`) | 6 | 6 | 2 | Arcane marble and marble brick |
| Thaumic Horizons (`ThaumicHorizons`, `ThaumicHorizons-1.8.28.jar`) | 40 | about 70 | 0 (16 partly) | Pastel translucent crystal in 16 colors at most |
| Thaumic Exploration (`ThaumicExploration`, `Thaumic-Exploration-1.5.29-GTNH.jar`) | 11 | about 30 | 0 (partly: floating candles) | Tile-entity devices |
| Witching Gadgets (`WitchingGadgets`, `WitchingGadgets-1.8.53-GTNH.jar`) | 7 | about 20 | 0 | Machines, a mirror, a void walkway |
| Forbidden Magic (`ForbiddenMagic`, `Forbidden.Magic-0.9.18-GTNH.jar`) | 11 | about 15 | 0 | Tainted (purple) woods and stone, cake, flowers |
| Tainted Magic (`TaintedMagic`, `Tainted-Magic-7.7.8-GTNH.jar`) | 6 | about 8 | 0 | Warpwood (deep purple) planks and logs, shadowmetal, ore |
| Thaumic Machina (`ThaumicMachina`, `ThaumicMachina-0.2.6-GTNH.jar`) | 3 | 3 | 0 | Metaphysical brick, rose, node transmodifier |
| Gadomancy (`gadomancy`, `gadomancy-1.5.17.jar`) | 11 | about 20 | 0 | Jars, node machines, pylons |
| Thaumic Energistics (`thaumicenergistics`, `thaumicenergistics-1.7.68-GTNH.jar`) | about 10 | about 10 | 0 | AE2 essentia machines |
| Thaumic Boots (`thaumicboots`, `thaumicboots-1.5.13.jar`) | 0 | 0 | 0 | No blocks: the jar carries 64 block textures (a copy of Thaumic Insurgence's set) but registers nothing |

**Ore and host rule:** none of the useful blocks is a GT ore or boulder host. Thaumic Bases' old cobblestone is ore-dictionaried as `cobblestone` but is a separate block, so GT's boulder replacement (which keys on `minecraft:cobblestone`) does not touch it (unverified: check GT's boulder host list in M02).

**Taint and flux:** none of the useful blocks spreads taint or emits flux. Forbidden Magic's and Tainted Magic's tainted woods and stone are only colored purple; they do not spread (no `updateTick` in `BlockStoneTainted`, `BlockPlanksTainted`), but they are off-theme anyway.

**Without the mods:** each pick needs a vanilla fallback (below). Salis Arcana's planks use Thaumcraft's textures, so they need Thaumcraft too (always present with it in the pack).

## Thaumic Bases

Source read: `mods/Thaumic-Based/tb/init/TBBlocks.java` (definitions lines 63-200, registration 216-336; registration goes through DummyCore's `BlocksRegistry.registerBlock` with the literal name), `tb/common/block/BlockHalfSlab.java` (slab metas, lines 22-23), `tb/common/block/BlockTBLog.java`. Pack config `config/Thaumic Bases.cfg`: `allowTobacco`, `enableBloodyMess` (spikes), `enableFlora`, `enableNodeTools` all true.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `thaumicbases:oldBrick:0` | Old Bricks | wall-main | yes | urban-brick, ruin | The beta-era brick texture: brighter orange-red with heavy white mortar. A second brick look beside vanilla's. Hardness 1. Fallback `minecraft:brick_block` |
| `thaumicbases:oldCobble:0` | Old Cobblestone | rubble-ruin, wall-main | yes | ruin | Beta cobblestone: lighter, rounder stones. Fallback `minecraft:cobblestone` (a boulder host; acceptable as a fallback only) |
| `thaumicbases:oldCobbleMossy:0` | Old Mossy Cobblestone | rubble-ruin | yes | ruin, overgrown | Fallback `minecraft:mossy_cobblestone` |
| `thaumicbases:oldGravel:0` | Old Gravel | rubble-ruin | yes | ruin | **Falls** (`BlockFalling`): place on solid ground only. Fallback `minecraft:gravel` |
| `thaumicbases:genericSlab:0-7` (+8 top), `thaumicbases:genericSlab_full:0-7` | Slabs of eldritch ark, old brick, old cobble, old mossy cobble, old diamond, old gold, old iron, old lapis | stairs-slab, rubble-ruin | yes (1-3) | ruin | Metas follow `BlockHalfSlab.parents`: 0 eldritch ark, 1 old brick, 2 old cobble, 3 old mossy cobble, 4 diamond, 5 gold, 6 iron, 7 lapis. 1-3 are the useful ones |
| `thaumicbases:oldIron:0`, `oldGold:0`, `oldDiamond:0`, `oldLapis:0` | Old storage blocks | special | partly | | Beta-texture metal and gem blocks, ore-dictionaried as `blockIron` and so on, hardness 1. A loot-value block: same treasure caveat as upstream's `!` palette ([beta balance list](../../beta-balance-list.md)) |
| `thaumicbases:enderPlanks:0` | Ender Planks | floor | partly | rustic | Light orange planks, ore-dictionaried `plankWood`. Fallback `minecraft:planks:4` |
| `thaumicbases:genLogs:0-2` | Peaceful, Nether, Ender Tree Log | trim-pillar | partly | rustic | |
| `thaumicbases:ironGreatwood:0` | Iron-Bound Greatwood | wall-accent | partly | rustic, industrial | Banded crate look |
| `thaumicbases:eldritchArk:0` | Eldritch Ark | off-theme | no | magic | Black and gold eldritch pattern |
| `thaumicbases:quicksilverBlock`, `quicksilverBrick`, `thauminiteBlock`, `voidBlock`, `blockSalisMundus`, `crystalBlock:0-7`, `crystalSlab*`, `cryingObsidian`, `pyrofluid` | Storage, crystals, fluid | off-theme | no | magic | Salis mundus block falls like sand |
| `thaumicbases:campfire:0`, `thaumicbases:brazier:0` | Campfire, Brazier | light | partly | ruin | Light 15; a campfire suits a squatter camp in a ruin. Both are functional (cook, burn) |
| `thaumicbases:spike:*` | Spikes | off-theme | no | | Damage entities |
| `thaumicbases:thaumicAnvil`, `voidAnvil`, `overchanter`, `advAlchFurnace`, `relocator`, `entityDeconstructor`, `nodeManipulator`, `nodeLinker` | Machines | off-theme | no | | Tile entities |
| `thaumicbases:goldenOakSapling`, `genLeaves`, plants (`plax`, `aurelia`, `aureliaPetal`, `metalleat`, `lucrite`, `knose`, `sweed`, `lazullia`, `rainbowCactus`, `redlonStem`, `ashroom`, `flaxium`, `glieonia`, `briar`, `tobacco`, `voidPlant`) | Trees and crops | off-theme | no | magic | Briar damages; rainbow cactus damages like cactus |

## Thaumic Tinkerer

Source read: `mods/ThaumicTinkerer/thaumic/tinkerer/common/registry/TTRegistry.java` (registration by `getBlockName()`, lines 159-276), `common/lib/LibBlockNames.java`, `common/block/quartz/BlockDarkQuartz.java`, `common/block/BlockTravelSlab.java`, `assets/ttinkerer/lang/en_US.lang`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicTinkerer:darkQuartz:0` | Block of Smokey Quartz | wall-main, wall-accent | yes | modern | Near-black polished quartz: a black-granite facade or lobby floor. Hardness 0.8. Fallback `minecraft:quartz_block:0` (color lost) or `etfuturum:blackstone:1` |
| `ThaumicTinkerer:darkQuartz:1` | Chiseled Smokey Quartz | wall-accent | yes | modern | |
| `ThaumicTinkerer:darkQuartz:2-4` | Pillar Smokey Quartz (Y, X, Z) | trim-pillar | yes | modern | Vanilla quartz-pillar meta scheme (`BlockDarkQuartz.onBlockPlaced`) |
| `ThaumicTinkerer:darkQuartzSlab:0` (+8 top), `ThaumicTinkerer:darkQuartzSlabFull:0` | Smokey Quartz Slab | stairs-slab, roof | yes | modern | |
| `ThaumicTinkerer:darkQuartzStairs` | Smokey Quartz Stairs | stairs-slab | yes | modern | |
| `ThaumicTinkerer:travelSlab`, `travelSlabFull`, `travelStairs` | Paving Slab and Stair of Travel | off-theme | no | | Grants speed and jump boost when walked on (`BlockTravelSlab.onEntityWalking`); a gameplay effect in a street would be odd |
| `ThaumicTinkerer:wardSlab`, `wardSlabFull` | Paving Slab of Warding | off-theme | no | | Thaumcraft warding paving; tile entity |
| `ThaumicTinkerer:gaseousLight`, `gaseousShadow`, `nitorGas` | Gases | off-theme | no | | Invisible light/dark sources that tick away |
| `ThaumicTinkerer:forcefield`, `platform`, fires (`fireAir`, `fireWater`, `fireOrder`, `fireChaos`, `fireFire`, `fireEarth`), `infusedFarmland`, `infusedGrainBlock`, `bedrockPortal`, `warpGate`, and the machines | Mechanisms | off-theme | no | | Fires spread and transmute; bedrock portal is a dimension gate |

## Salis Arcana

Source read: `mods/salisarcana/dev/rndmorris/salisarcana/common/blocks/CustomBlocks.java`, `BlockPlank.java`. Pack config `config/salisarcana/enhancements.cfg` line 87 `enableLookalikePlanks=true`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `salisarcana:blockCustomPlank:0` | Greatwood Planks | floor, wall-main | yes | rustic, interior | Dark brown striped planks, a plain block (Thaumcraft's own greatwood planks live in its `blockWoodenDevice` at meta 6, a device block). Uses Thaumcraft's texture. Fallback `minecraft:planks:5` |
| `salisarcana:blockCustomPlank:1` | Silverwood Planks | floor, wall-accent | partly | interior | Cream with an ornate carved pattern: parquet or paneling. Fallback `minecraft:planks:2` |

## Automagy

Source read: `mods/Automagy/tuhljin/automagy/blocks/ModBlocks.java` (lines 81-120), `BlockBookshelfEnchanted.java`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Automagy:blockBookshelfEnchanted:0` | Enchanted Greatwood Bookshelf | furniture-deco | yes | interior | A bookshelf with greatwood ends and colorful books, plain block, no tile entity. Library and office variety beside `minecraft:bookshelf`. Boosts enchanting tables like a bookshelf |
| `Automagy:blockBookshelfEnchanted:1` | Enchanted Silverwood Bookshelf | furniture-deco | yes | interior | Pale silverwood ends |
| everything else (`blockRedcrystal*`, `blockTorchInversion*`, `blockRemoteComparator`, `blockVisReader`, `blockTally`, `blockHourglass*`, chests, maws, golem blocks, `blockEssentiaLocus*`, `blockInventarium`, `blockUnseenScribe`, `blockReqTome`, `blockTranslucent`, `blockMetalGlow`, `blockNethermind`, `blockRunedObsidian`, `blockNetherRune*`, lures, `blockThirstyTank`, `blockBoiler`, `blockCreativeJar`, mirrors, fluids) | Redstone, golem and essentia devices | off-theme | no | magic | Mostly tile entities; lures attract mobs |

## Thaumic Insurgence

Source read: `mods/thaumicinsurgence/thaumicinsurgence/main/Config.java` (lines 87-146), `block/BlockArcaneMarble.java`, `block/BlockArcaneMarbleBrick.java`. Registered under `getUnlocalizedName()`, so the names carry `tile.`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `thaumicinsurgence:tile.arcaneMarble:0` | Arcane Marble | wall-main, floor | yes | urban-stone | Smooth white marble. **Hardness 10.** Fallback `minecraft:quartz_block:0` |
| `thaumicinsurgence:tile.arcaneMarbleBrick:0` | Arcane Marble Brick | wall-main | yes | urban-stone | White marble bricks, hardness 10. Civic buildings. Fallback `minecraft:stonebrick:0` |
| `thaumicinsurgence:infusionIntercepter`, `matrixAlpha`, `pillarAlpha`, `marblePedestal` | Infusion parts | off-theme | no | magic | Tile entities |

## Thaumic Horizons

Source read: `mods/ThaumicHorizons/com/kentington/thaumichorizons/common/ThaumicHorizons.java` (lines 598-637), `common/blocks/BlockCrystal.java`, `BlockEvanescent.java`, `BlockLight.java`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicHorizons:crystalTH:0-15` | Crystal (16 colors) | glass | partly | modern | Translucent pastel glass (`BlockBreakable`, render pass 1), hardness 1.75. Lighter and more milky than stained glass; partly for glass-block facades. Fallback `minecraft:stained_glass` same meta (color order unverified) |
| `ThaumicHorizons:evanescent` | Evanescent | off-theme | no | | **Unbreakable** (hardness `Float.MAX_VALUE`) |
| `ThaumicHorizons:light`, `lightSolar` | Light | off-theme | no | | Invisible tile-entity lights |
| `ThaumicHorizons:boneTH`, `fleshTH`, `brainTH`, `eyeTH`, `dustTH`, `chocolate`, `cloudTH`, `cloudGlowingTH`, `spikeTH`, `spikeWoodTH`, `spikeToothTH`, `crystalDeep`, `leviathanTH`, `slotTH`, `portalTH`, `gatewayTH`, `voidTH`, `vortexTH` and the machines (`synthNode`, `alchemite`, dynamos, `soulSieve`, `inspiratron`, `soulforge`, `soulJar`, `bloodInfuser`, `vat*`, `soulBeacon`, `modMatrix`, `transductionAmplifier`, `recombinator`, `vortexStabilizer`, `nodeMonitor`) | Pocket-dimension and machine blocks | off-theme | no | alien | Spikes damage; portals and vortices are hazards |

## Thaumic Exploration, Witching Gadgets and the rest

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ThaumicExploration:floatCandle:0-15` | Floating Candle | light | partly | interior | Thaumcraft candle that hovers; tile entity, colored by meta. Fallback `minecraft:torch` |
| `ThaumicExploration:everfullUrn`, `everburnUrn`, `trashJar`, `boundChest`, `boundJar`, `thinkTankJar`, `meltyIce`, `soulBrazier`, `crucibleSouls`, `replicator` | Devices | off-theme | no | | Urn fills with water; melty ice melts |
| `WitchingGadgets:WG_VoidWalkway`, `WG_WallMirror`, `WG_StoneDevice:0-2` (ethereal wall, time stone, blast furnace), `WG_WoodenDevice:*`, `WG_MetalDevice:*`, `WG_RoseVine`, `WG_CustomAir` | Devices and decorations | off-theme | no | magic | Wall mirror is a two-block tile-entity portal mirror; rose vine damages on contact (`BlockRoseVines` line 66) |
| `ForbiddenMagic:TaintPlank`, `TaintLog`, `TaintLeaves`, `TaintSapling`, `TaintBlock:0-1`, `StarBlock`, `ArcaneCake`, `WrathCage`, `InkFlower`, `UmbralBush` | Tainted wood and stone, misc | off-theme | no | magic | Saturated purple. Wrath cage is a spawner |
| `TaintedMagic:BlockWarpwoodPlanks`, `BlockWarpwoodLog`, `BlockWarpwoodLeaves`, `BlockWarpwoodSapling`, `BlockShadowmetal`, `BlockShadowOre` | Warpwood, shadowmetal | off-theme | no | magic | Deep purple-black planks |
| `ThaumicMachina:metaphysicalBrick`, `metaphysicalRose`, `nodeTransmodifier` | Metaphysical blocks | off-theme | no | magic | Near-black brick |
| `gadomancy:*` (`BlockStickyJar`, `BlockArcaneDropper`, `BlockInfusionClaw`, `BlockRemoteJar`, `BlockExtendedNodeJar`, `BlockNodeManipulator`, `BlockStoneMachine`, `BlockAdditionalEldritchPortal`, `BlockAuraPylon`, `BlockKnowledgeBook`, `BlockEssentiaCompressor`) | Devices | off-theme | no | | Registered under the class simple name |
| `thaumicenergistics:*` | Essentia ME machines | off-theme | no | | |

## Best variety pools

- **Ruined brick and rubble (`wall-main`, `rubble-ruin`):** `thaumicbases:oldBrick`, `oldCobble`, `oldCobbleMossy`, `oldGravel` (ground only), and the slabs `genericSlab:1-3`. They give a "different era" brick and cobble next to the vanilla ones without leaving the upstream look.
- **Dark modern facade (`wall-main`, `trim-pillar`, `stairs-slab`):** smokey quartz block, chiseled, pillar, slab, stairs. The only complete black-stone building set in this slice.
- **White civic stone (`wall-main`):** arcane marble and marble brick, as accents because of hardness 10.
- **Wood floors and paneling (`floor`):** Salis Arcana greatwood and silverwood planks.
- **Library interiors (`furniture-deco`):** Automagy bookshelves, mixed with vanilla bookshelves.

## Worldgen notes

- All useful blocks are plain blocks without tile entities, except the floating candle.
- Old gravel falls; support it.
- Do not place `travelSlab`, `wardSlab`, spikes, gases or any portal.
