# Block catalog: IndustrialCraft 2

IndustrialCraft 2 2.2.828-experimental (mod id `IC2`, from the jar's `mcmod.info`; jar `industrialcraft-2-2.2.828-experimental.jar`) as GTNH 2.9.0 ships it. IC2 is closed source: registrations were read from the decompile in `gtnh-src/mods/industrialcraft-2/` (`ic2.core.init.BlocksItems.initBlocks`, `ic2.core.block.*`), cross-checked against the official API at `gtnh-src/source/IC2-API/`. English names are from the jar's `ic2/lang/en_US.properties`. The pack's `config/IC2.ini` disables none of these blocks. Sheets: `tools/block-contact-sheet/sheets/ic2/{construction,metal-blocks,foam-walls}.json`, rendered to `docs/previews/blocks/ic2/` (local only). This catalog feeds palette variety under [decision log](../../../design/decision-log.md) D25; the [D19 block mapping](../../00-pack-integration/block-mapping.md) stays the baseline.

Registry names: IC2 registers each block under its `InternalName` enum name (`GameRegistry.registerBlock(this, itemClass, internalName.name())` in `BlockBase`), so reinforced stone is `IC2:blockAlloy`. Machines, generators, cables, reactor parts, explosives and crops are not catalogued.

## Summary

**43 block variants in 20 registry blocks.** Variants are counted by look: a block whose metadata only encodes strength (scaffolds), facing or resin state (rubber wood) or door halves counts once per distinct texture. **19 are tile entities:** the 16 construction foam walls, the 2 luminators and the barrel.

| Role (a block can have several) | Count |
|---|---|
| wall-main | 7 |
| wall-accent | 9 |
| trim-pillar | 1 |
| floor | 2 |
| ceiling | 2 |
| roof | 1 |
| glass | 1 |
| pane | 0 |
| street, sidewalk | 0 |
| stairs-slab | 0 |
| rubble-ruin | 2 |
| light | 1 |
| fence-rail | 1 |
| door-trap | 1 |
| furniture-deco | 1 |
| industrial-dressing | 11 |
| foliage | 4 |
| special | 2 |
| off-theme | 10 |

**Fit with upstream's look:** 13 yes, 19 partly, 11 no. The useful finds are IC2 basalt (a dark stone that is not a GT host), reinforced glass, the iron fence, scaffolding for construction sites and ruins, the dark luminator as a ceiling panel, and the 16 foam-wall colors, which are flat stucco-like colors with no tint.

**Flags**

- **Tile entities.** `IC2:blockWall` (construction foam wall, `TileEntityWall`), `IC2:blockLuminator` and `IC2:blockLuminatorDark` (`TileEntityLuminator`) and `IC2:blockBarrel` (`TileEntityBarrel`). The foam wall's tile entity only stores optional retexture data, so a placed wall without it renders its color normally (*unverified*: check in M02 that a missing tile entity is recreated, since `BlockMultiID` creates it on demand).
- **Hardness and blast resistance that make moving in painful.** Reinforced stone `IC2:blockAlloy` is **hardness 80, resistance 180** (obsidian is 50/2000; GT's harder machines aside, this is one of the slowest blocks to mine in the pack). The reinforced door is 50/150, reinforced glass 5/180, basalt **20/45**. A building walled in reinforced stone is effectively a bunker, so reinforced stone is never placed (D28 rule 7: hardness above obsidian's 50). The reinforced door, at 50, is at the ceiling and stays a rare vault door.
- **Light.** `IC2:blockLuminator` is light 15 only while it has EU; without power its tile entity turns it into `IC2:blockLuminatorDark` (no light). Unpowered in a city, it is a ceiling panel, not a light source.
- **Unstable blocks.** Construction foam (`blockFoam`, `blockReinforcedFoam`) hardens into a foam wall or reinforced stone on a random tick, faster in light, and has no collision until then. Scaffolds check their support and drop when unsupported. Resin and the rubber sheet need a solid block below.
- **Gravity.** None.
- **No mob spawning** on the metal storage blocks (`canCreatureSpawn` returns false).
- **Ore and boulder hosts.** None. GT's ore hosts and boulder hosts are fixed lists of vanilla, GT and Et Futurum blocks ([ore-veins.md](../../00-pack-integration/ore-veins.md)); IC2 basalt is not GT basalt (`gregtech:gt.blockstones:8`) and is not on either list.
- **Tier.** Only the luminator draws EU (LV). Nothing here passes the EV ceiling.
- **Colors.** The foam walls use one texture per color (`cf/blockWall.<color>.png`), so the sheet shows the real colors. Rubber leaves are biome-tinted at runtime and render gray on the sheet.

## Construction and decorative blocks

Sheet `construction.json`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `IC2:blockBasalt:0` | Basalt | wall-main, wall-accent, floor, rubble-ruin | yes | urban, industrial, ruined | Very dark speckled stone, rough. **Not a GT host**, unlike GT basalt. Hardness 20, resistance 45: slow to mine, so prefer accents and plinths over whole facades |
| `IC2:blockAlloy:0` | Reinforced Stone | wall-main, special | partly: drab olive-gray panel; **hardness 80, resistance 180** | industrial | Would suit a bank's strong room, but painful to dig through: never placed (D28 rule 7) |
| `IC2:blockAlloyGlass:0` | Reinforced Glass | glass | partly: dark frame with blue flecks | industrial | Hardness 5, resistance 180. Industrial windows and bunker slits |
| `IC2:blockDoorAlloy` | Reinforced Door | door-trap | partly: plain gray slab door | industrial | Two-block door, vanilla door metadata. Hardness 50, resistance 150 |
| `IC2:blockFenceIron:0` | Iron Fence | fence-rail, trim-pillar | yes | urban, industrial, ruined | Thin pole that connects like a fence; hardness 1.5. Railing, bollard, fence |
| `IC2:blockScaffold:0-4` | Scaffold | industrial-dressing, rubble-ruin | yes | ruined, urban | Wooden frame with cross braces: unfinished or abandoned construction. Metadata = support strength (wood: 2, reinforced 5). **Drops when unsupported** |
| `IC2:blockScaffold:5` | Reinforced Scaffold | industrial-dressing | yes | ruined, urban | Same frame with extra planks |
| `IC2:blockIronScaffold:0-11` | Iron Scaffold | industrial-dressing | yes | industrial, ruined | Steel tube frame; strength 5, reinforced 12 |
| `IC2:blockIronScaffold:12` | Reinforced Iron Scaffold | industrial-dressing | yes | industrial | |
| `IC2:blockLuminator` | Luminator | light, ceiling | partly: needs EU to stay lit | urban, industrial | **Tile entity**. Thin panel on a face; light 15 while powered, then turns dark |
| `IC2:blockLuminatorDark` | Luminator (unlit) | ceiling, industrial-dressing | yes | urban, industrial, ruined | **Tile entity**. Gray fluorescent-panel look: office and subway ceilings |
| `IC2:blockMiningPipe:0` | Mining Pipe | industrial-dressing | partly: thin gray pipe | industrial, ruined | Vertical 4-px pipe, hardness 6. Drain pipes, conduits |
| `IC2:blockRubber:0` | Rubber Sheet | roof, floor | partly: 2-px black speckled layer, bouncy | industrial, urban | Reads as a tar or rubber roof membrane on flat roofs; entities bounce on it. Needs support |
| `IC2:blockHarz:0` | Resin | special | no | none | Sticky 2-px amber layer, no collision; slows entities |
| `IC2:blockRubWood:0-11` | Rubber Wood | foliage | partly: dark bark log | overgrown | Metadata: axis or resin-hole facing (2-5 wet, 8-11 dry). One look counted, plus the resin spot below |
| `IC2:blockRubWood` (resin spot) | Rubber Wood with resin | foliage | partly | overgrown | Amber resin hole on one face |
| `IC2:blockRubLeaves:0` | Rubber Leaves | foliage | yes | overgrown | Biome-tinted at runtime (gray on the sheet) |
| `IC2:blockRubSapling:0` | Rubber Sapling | foliage | partly | overgrown | Not on the sheet: a cross-model plant like upstream's `p` saplings |
| `IC2:blockBarrel` | Barrel | furniture-deco | partly: dark wood keg | urban, industrial | **Tile entity** (booze barrel). Et Futurum's barrel is the simpler stand-in |
| `IC2:blockFoam:0` | Construction Foam | off-theme | no | none | Hardens into `IC2:blockWall` meta 7 (light gray) on a random tick; no collision before |
| `IC2:blockReinforcedFoam:0` | Reinforced Construction Foam | off-theme | no | none | Hardens into reinforced stone |

## Metal storage blocks

Sheet `metal-blocks.json`. `IC2:blockMetal`, metas from `BlockMetal`. Hardness 4; no mob spawns; bronze and uranium are beacon bases. These are value blocks: upstream's `!` treasure mix uses iron, gold, diamond and emerald blocks, and D29 brings copper, tin, bronze, lead and refined iron back as accents behind their metal flags, under a per-building cap and never as walls ([D29](index.md#decorative-metals-d29)). Uranium is above EV and never placed.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `IC2:blockMetal:0` | Copper Block | industrial-dressing, wall-accent | partly: bright metal | industrial | Accent behind `decorativeMetals.copper` (D29), or treasure |
| `IC2:blockMetal:1` | Tin Block | industrial-dressing | partly | industrial | Accent behind `decorativeMetals.tin` (D29), or treasure |
| `IC2:blockMetal:2` | Bronze Block | industrial-dressing, wall-accent | partly: orange bronze | industrial | Steam-age GTNH; treasure candidate |
| `IC2:blockMetal:3` | Uranium Block | off-theme | no | none | Radiation-hazard texture; also a strong reward. Keep out |
| `IC2:blockMetal:4` | Lead Block | industrial-dressing, wall-accent | partly: pale gray-green plate | industrial | |
| `IC2:blockMetal:5` | Refined Iron Block | industrial-dressing, wall-accent | partly: pale blue-gray plate | industrial | Accent behind `decorativeMetals.iron` (D29), or treasure |

## Construction foam walls

Sheet `foam-walls.json`. `IC2:blockWall`, metas 0-15 in **dye-item order** (0 black, 1 red, 2 green, 3 brown, 4 blue, 5 purple, 6 cyan, 7 light gray, 8 gray, 9 pink, 10 lime, 11 yellow, 12 light blue, 13 magenta, 14 orange, 15 white; `Util.getColorName`). That is the reverse of wool and stained-glass metadata. Hardened foam sets as meta 7. Full cubes; **tile entity** (`TileEntityWall`); hardness 3, resistance 30; drops nothing when mined. Each color has its own texture: a fine stucco grain with a slightly darker outline at the block edge, so a wall shows a faint grid.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `IC2:blockWall:0` | Black Construction Foam Wall | wall-accent | yes | urban, industrial | |
| `IC2:blockWall:1` | Red Construction Foam Wall | wall-accent | partly: bright | urban | |
| `IC2:blockWall:2` | Green Construction Foam Wall | wall-accent | partly: olive | urban | |
| `IC2:blockWall:3` | Brown Construction Foam Wall | wall-main | yes | urban | Stucco brown |
| `IC2:blockWall:4` | Blue Construction Foam Wall | off-theme | no | none | |
| `IC2:blockWall:5` | Purple Construction Foam Wall | off-theme | no | none | |
| `IC2:blockWall:6` | Cyan Construction Foam Wall | wall-accent | partly | urban | Next to upstream's cyan terracotta and concrete |
| `IC2:blockWall:7` | Light Gray Construction Foam Wall | wall-main | yes | urban, industrial | Plain stucco or render; the default foam color |
| `IC2:blockWall:8` | Gray Construction Foam Wall | wall-main | yes | urban, industrial | |
| `IC2:blockWall:9` | Pink Construction Foam Wall | off-theme | no | none | |
| `IC2:blockWall:10` | Lime Construction Foam Wall | off-theme | no | none | |
| `IC2:blockWall:11` | Yellow Construction Foam Wall | off-theme | no | none | |
| `IC2:blockWall:12` | Light Blue Construction Foam Wall | off-theme | no | none | |
| `IC2:blockWall:13` | Magenta Construction Foam Wall | off-theme | no | none | |
| `IC2:blockWall:14` | Orange Construction Foam Wall | wall-main | partly: terracotta-like orange | desert | Southwest stucco |
| `IC2:blockWall:15` | White Construction Foam Wall | wall-main | yes | urban, desert | White render |

## Best variety pools

Weights are suggestions out of the role's total, beside the D19 baseline block.

- **`bricks_standard` `#` / plain facades:** light gray, gray and white foam walls (`IC2:blockWall:7,8,15`) 1 each, as rendered or stucco buildings; tile entities, so low weights (D28 rule 6).
- **Colored-terracotta palettes (`bricks_gray`, `bricks_silver`, `bricks_cyan`):** gray, light gray and cyan foam walls (`:8,7,6`) as non-boulder-host alternatives to terracotta. Unlike `minecraft:stained_hardened_clay`, they are not GT boulder hosts.
- **Desert bricks:** white and orange foam walls (`:15,14`) for stucco; brown (`:3`).
- **Dark accents and plinths (`X`, `$`, `blackstone` variant):** IC2 basalt (`IC2:blockBasalt:0`) 1, sparingly because of hardness 20.
- **`glass_*`:** reinforced glass (`IC2:blockAlloyGlass:0`) in an industrial style only.
- **Ceilings:** unlit luminator (`IC2:blockLuminatorDark`) as office and subway ceiling panels (tile entity).
- **Iron bars `:` and fence `.`:** iron fence (`IC2:blockFenceIron:0`) 2.
- **`}` rubble and ruins:** wooden and iron scaffolds (`IC2:blockScaffold:0`, `IC2:blockIronScaffold:0`) as abandoned construction on damaged floors, placed only where supported.
- **Roof:** rubber sheet (`IC2:blockRubber:0`) as a flat-roof membrane, 1 in an industrial style.
- **Furniture `F` and oil rig:** barrel (tile entity) sparingly.
- **Treasure `!`:** refined iron and bronze blocks (`IC2:blockMetal:5`, `:2`).
- **Metal accents:** copper, tin, bronze, lead and refined iron blocks (`IC2:blockMetal:0`, `:1`, `:2`, `:4`, `:5`) as bands and odd panels in industrial buildings, behind their D29 flags ([D29](index.md#decorative-metals-d29)).
- **Overgrown:** rubber leaves and saplings next to upstream's `p` foliage.

## Decisions (D28)

1. **Foam walls are tile entities.** They give 16 flat colors that are not GT boulder hosts, a way out of the terracotta boulder issue ([ore-veins.md](../../00-pack-integration/ore-veins.md) option B), but each block carries a tile entity. D28 rule 6 allows tile-entity palette entries at low weights, so they take small shares beside Et Futurum concrete, which gives similar flat colors without one.
2. **Reinforced stone, reinforced glass and basalt hardness.** Reinforced stone (hardness 80) is never placed (D28 rule 7). Reinforced glass (5) stays in industrial buildings; basalt (20) stays an accent, since [record 0004](../../../design/0004-gtnh-layer-and-tier-ceiling.md) wants buildings players can move into.
3. **Unpowered luminators.** Use the dark luminator as a ceiling panel only; a lit one placed without EU goes dark on its first tick.
