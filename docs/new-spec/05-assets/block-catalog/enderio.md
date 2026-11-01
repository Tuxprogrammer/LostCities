# Block catalog: EnderIO

Mod id `EnderIO`, jar `EnderIO-2.10.51.jar`, source read at tag `2.10.51` (`gtnh-src/source/EnderIO`: `ModObject.java`, `material/BlockFusedQuartz.java`, `material/BlockDarkSteelBars.java`, `material/BlockIngotStorage.java`, `material/endergy/*`, `block/*`, `machine/light/*`, `machine/painter/*`, `rail/BlockEnderRail.java`; `BlockEnder` from `gtnh-src/source/EnderCore`). Pack config checked: `config/enderio/EnderIO.cfg` (`reinforcedObsidianEnabled=true`, `enderRailEnabled=true`, `clearGlassSameTexture=false`, `fusedQuartzExplosionResistance=20.0`). Sheet: [`tools/block-contact-sheet/sheets/enderio/decorative.json`](../../../../tools/block-contact-sheet/sheets/enderio/decorative.json). This catalog serves D25 in the [decision log](../../../design/decision-log.md): variety for palettes, with the [D19 mapping](../../00-pack-integration/block-mapping.md) as baseline.

Every block registers under its `ModObject` name, so names are `EnderIO:blockFusedQuartz` and so on.

## Summary

- **47 block+meta entries (counting the 6 Powered Light kinds and 8 painted blocks once each) in 20 registry blocks.**
- Fit upstream's look: 13 `yes`, 20 `partly`, 14 `no`.
- Not GT ore hosts or boulder hosts ([ore-veins.md](../../00-pack-integration/ore-veins.md)).

| Primary role | Entries |
|---|---|
| glass | 4 (fused quartz, quite clear glass, both dark variants) |
| light | 8 (2 enlightened glasses, 6 Powered Light kinds) |
| fence-rail | 3 (bars) |
| industrial-dressing | 16 (alloy blocks) |
| wall-accent | 1 (reinforced obsidian) |
| furniture-deco | 3 (dark steel anvil) |
| special | 12 (ladder, 2 pressure plates, ender rail, 8 painted blocks) |

Flags:

- **Tile entities: every fused quartz and clear glass block has one.** `BlockFusedQuartz` passes `TileEntityPaintedBlock` to EnderCore's `BlockEnder`, whose `hasTileEntity` returns true for any metadata. Placing EnderIO glass in worldgen therefore creates a tile entity per window block: avoid it for large facades. The Powered Light, dark pressure plate and all eight painted blocks are tile entities too. Bars, reinforced obsidian, alloy blocks, ladder, anvil and ender rail are plain blocks.
- **Light:** Enlightened Fused Quartz and Enlightened Clear Glass 15 (no TE data needed: the light comes from the metadata). Powered Light: 15 when on; whether it is on depends on its TE (type) and on power or redstone.
- **Light opacity:** the two dark glasses (metas 4 and 5) have opacity 255: they look like dark tinted glass but block all light.
- **Hardness/resistance:** Reinforced Obsidian hardness 50, resistance 2000 (wither-proof): painful. Bars resistance 2000, hardness 5. Fused quartz (metas 0, 2, 4) explosion resistance is 20 in this pack (config, default 2000); clear glass (1, 3, 5) is ordinary.
- **Gravity:** the Dark Steel Anvil falls like a vanilla anvil.

## Glass

The glass textures are a nearly transparent pane plus a separate frame overlay (`fusedQuartzFrame`, `fusedGlassFrame`); the sheet shows both. The frames connect between all six metas of a kind (quartz with quartz, glass with glass).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `EnderIO:blockFusedQuartz:0` | Fused Quartz | glass | yes: clear with a thin dark-gray frame, a modern window | urban, industrial | TE; explosion resistance 20 |
| `EnderIO:blockFusedQuartz:1` | Quite Clear Glass | glass | yes: clearest glass in the pack, thin light frame | urban | TE |
| `EnderIO:blockFusedQuartz:2` | Enlightened Fused Quartz | light, glass | yes: lit window, light 15 | urban, industrial | TE |
| `EnderIO:blockFusedQuartz:3` | Enlightened Clear Glass | light, glass | yes: lit window, light 15 | urban | TE |
| `EnderIO:blockFusedQuartz:4` | Dark Fused Quartz | glass | yes: tinted office glass | urban, industrial | TE; opacity 255 |
| `EnderIO:blockFusedQuartz:5` | Dark Clear Glass | glass | yes: tinted glass | urban | TE; opacity 255 |

## Bars, obsidian and fittings

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `EnderIO:blockDarkIronBars:0` | Dark Steel Bars | fence-rail | yes: heavy dark grille, an `iron_bars` alternative | urban, industrial, ruined | Pane model; resistance 2000 |
| `EnderIO:blockSoulariumBars:0` | Soularium Bars | fence-rail | partly: rusty brown bars, could read as rusted iron in ruins | ruined | Pane model |
| `EnderIO:blockEndSteelBars:0` | End Steel Bars | fence-rail | partly: cream-colored bars | none | Pane model |
| `EnderIO:blockReinforcedObsidian:0` | Reinforced Obsidian | wall-accent, special | no: purple-black obsidian with a grid | industrial | Hardness 50, resistance 2000, wither-proof |
| `EnderIO:blockDarkSteelLadder:0-5` | Dark Steel Ladder | special | yes: black metal ladder, an alternative for upstream's ladder `l` | urban, industrial | Metadata is facing; faster climbing |
| `EnderIO:blockDarkSteelPressurePlate:0` | Dark Pressure Plate | special | partly: dark plate, only players trigger it | industrial | TE |
| `EnderIO:blockDarkSteelPressurePlate:1` | Dark Pressure Plate (silent) | special | partly | industrial | TE |
| `EnderIO:blockDarkSteelAnvil:0` | Dark Steel Anvil | furniture-deco | yes: upstream's `F` furniture includes anvils | urban, industrial | Falls; metadata also holds facing |
| `EnderIO:blockDarkSteelAnvil:1` | Slightly Damaged Dark Steel Anvil | furniture-deco, rubble-ruin | yes | ruined | Falls |
| `EnderIO:blockDarkSteelAnvil:2` | Very Damaged Dark Steel Anvil | furniture-deco, rubble-ruin | yes | ruined | Falls |
| `EnderIO:blockEnderRail:0` | Ender Rail | special | partly: a rail with an ender-tech look; it extends the vanilla rail and only teleports carts on a Dimensional Transceiver | industrial | Metadata is rail shape |

## Powered Light

One registry block, `EnderIO:blockElectricLight`; metadata is the facing, the kind lives in the tile entity. Thin panel model on one face.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `EnderIO:blockElectricLight` (TE kind ELECTRIC) | Powered Light | light, ceiling | partly: modern ceiling panel; needs RF and redstone to light | urban, industrial | TE; off when unpowered: reads as a dead fixture in ruins |
| `EnderIO:blockElectricLight` (ELECTRIC_INV) | Powered Light (Inverted) | light, ceiling | partly | urban, industrial | TE; needs RF |
| `EnderIO:blockElectricLight` (BASIC) | Light | light, ceiling | yes: panel light driven by redstone only | urban | TE |
| `EnderIO:blockElectricLight` (BASIC_INV) | Light (Inverted) | light, ceiling | yes: lit with no power and no redstone | urban, industrial | TE; the one kind that is lit as placed |
| `EnderIO:blockElectricLight` (WIRELESS) | Wireless Powered Light | light | partly | industrial | TE; places invisible light-node blocks around it |
| `EnderIO:blockElectricLight` (WIRELESS_INV) | Wireless Powered Light (Inverted) | light | partly | industrial | TE; light nodes |

## Alloy blocks

Storage blocks for EnderIO's alloys; hardness per alloy (1 to 10), resistance twice the hardness. These are valuable resource blocks: a building made of them is a loot pile. They fit only as industrial accents or as treasure (`!`).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `EnderIO:blockIngotStorage:0` | Electrical Steel Block | industrial-dressing, wall-accent | partly: light gray steel plate | industrial | Hardness 6 |
| `EnderIO:blockIngotStorage:1` | Energetic Alloy Block | industrial-dressing | no: orange | none | |
| `EnderIO:blockIngotStorage:2` | Vibrant Alloy Block | industrial-dressing | no: lime | none | |
| `EnderIO:blockIngotStorage:3` | Redstone Alloy Block | industrial-dressing | no: red | none | |
| `EnderIO:blockIngotStorage:4` | Conductive Iron Block | industrial-dressing | no: salmon | none | |
| `EnderIO:blockIngotStorage:5` | Pulsating Iron Block | industrial-dressing | no: green | none | |
| `EnderIO:blockIngotStorage:6` | Dark Steel Block | industrial-dressing, wall-accent | partly: dark gray plate | industrial | Hardness 10 |
| `EnderIO:blockIngotStorage:7` | Soularium Block | industrial-dressing | no: brown | none | |
| `EnderIO:blockIngotStorage:8` | End Steel Block | industrial-dressing | no: cream | none | Animated texture |
| `EnderIO:blockIngotStorageEndergy:0` | Crude Steel Block | industrial-dressing, wall-accent | partly: rough gray steel | industrial | |
| `EnderIO:blockIngotStorageEndergy:1` | Crystalline Alloy Block | industrial-dressing | no: pale cyan | none | |
| `EnderIO:blockIngotStorageEndergy:2` | Melodic Alloy Block | industrial-dressing | no: purple | none | |
| `EnderIO:blockIngotStorageEndergy:3` | Stellar Alloy Block | industrial-dressing | no: white with streaks | none | |
| `EnderIO:blockIngotStorageEndergy:4` | Crystalline Pink Slime Block | industrial-dressing | no: pink | none | Animated texture |
| `EnderIO:blockIngotStorageEndergy:5` | Energetic Silver Block | industrial-dressing | no: pale blue | none | |
| `EnderIO:blockIngotStorageEndergy:6` | Vivid Alloy Block | industrial-dressing | no: bright blue | none | |

## Painted blocks

Each takes its look from a "paint source" block stored in its tile entity, so any of them can imitate any full block. Useful only if the writer can set tile-entity NBT; a palette can always use the source block itself instead. Not rendered on a sheet.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `EnderIO:blockPaintedFence:0` | Painted Fence | special, fence-rail | partly | urban | TE; look from NBT |
| `EnderIO:blockPaintedFenceGate:0` | Painted Fence Gate | special, door-trap | partly | urban | TE |
| `EnderIO:blockPaintedWall:0` | Painted Wall | special, fence-rail | partly | urban | TE |
| `EnderIO:blockPaintedStair:0-7` | Painted Stairs | special, stairs-slab | partly | urban | TE |
| `EnderIO:blockPaintedSlab:0` (+8 top) | Painted Slab | special, stairs-slab | partly | urban | TE |
| `EnderIO:blockPaintedDoubleSlab:0` | Painted Double Slab | special | partly | urban | TE |
| `EnderIO:blockPaintedGlowstone:0` | Painted Glowstone | special, light | partly: a hidden light source that looks like any block | urban | TE; light 15 |
| `EnderIO:blockPaintedCarpet:0` | Painted Carpet | special, floor | partly | urban | TE |

Not catalogued: machines, conduits, capacitor banks, travel anchors and telepads (all tile entities with function), the light node (invisible helper block), the enderman skull.

## Best variety pools

- **Glass `glass_*`, `+`:** Fused Quartz (`:0`) and Quite Clear Glass (`:1`) as clear alternatives, Dark Fused Quartz (`:4`) and Dark Clear Glass (`:5`) as tinted office glass for the `glass_full_gray` family. Use sparingly: each block is a tile entity.
- **Light `h`:** Enlightened Fused Quartz and Enlightened Clear Glass (lit windows, light 15, set by metadata alone); Light (Inverted) panels where tile-entity data can be written.
- **Iron bars `:`:** Dark Steel Bars as the main variant; Soularium Bars for rusty ruins.
- **Furniture `F`:** Dark Steel Anvil (and its damaged forms in ruins).
- **Ladder `l`:** Dark Steel Ladder.
- **Industrial set dressing:** Dark Steel and Electrical Steel blocks as rare wall accents in an industrial style; the rest of the alloy blocks only as treasure.
