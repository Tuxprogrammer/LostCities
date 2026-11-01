# Block catalog: Applied Energistics 2

Mod id `appliedenergistics2`, jar `appliedenergistics2-rv3-beta-1095-GTNH.jar`, source read at tag `rv3-beta-1095-GTNH` (`gtnh-src/source/Applied-Energistics-2-Unofficial`: `core/api/definitions/ApiBlocks.java`, `block/solids/*`, `block/stair/*`, `core/features/*FeatureHandler.java`, `services/CompassService.java`). Pack config checked: `config/AppliedEnergistics2/AppliedEnergistics2.cfg`, where `DecorativeQuartzBlocks`, `DecorativeLights`, `MeteoriteWorldGen` and `SkyStoneChests` are all `true`, so every block below is registered. Sheet: [`tools/block-contact-sheet/sheets/ae2/decorative.json`](../../../../tools/block-contact-sheet/sheets/ae2/decorative.json). This catalog serves D25 in the [decision log](../../../design/decision-log.md): variety for palettes, with the [D19 mapping](../../00-pack-integration/block-mapping.md) as baseline.

AE2 registers every block as `tile.<ClassName>` (`AEBlockFeatureHandler`, `StairBlockFeatureHandler`), so registry names look like `appliedenergistics2:tile.BlockSkyStone`. Slabs register a second block `tile.<Name>.double` for the double slab.

## Summary

- **37 block+meta entries in 33 registry blocks**, all enabled in the pack.
- Fit upstream's look: 26 `yes`, 7 `partly`, 4 `no`.
- Not GT ore hosts or boulder hosts (the host lists in [ore-veins.md](../../00-pack-integration/ore-veins.md) name only vanilla stone, netherrack, end stone, GT granites and stones, deepslate and tuff, stained hardened clay and cobblestone).

| Primary role | Entries |
|---|---|
| wall-main | 4 (sky stone block, brick, small brick; certus quartz block) |
| wall-accent | 1 (chiseled certus quartz) |
| trim-pillar | 1 (certus quartz pillar) |
| stairs-slab | 21 (7 stairs, 7 slabs, 7 double slabs; fluix excluded) |
| glass | 1 |
| light | 2 |
| rubble-ruin | 1 (raw sky stone) |
| furniture-deco | 2 (sky stone chests) |
| off-theme | 4 (fluix block, stairs, slab, double slab) |

Each entry counts once, under the first role in its row; the tables list every role.

Flags:

- **Tile entities:** only the sky stone chests. Everything else is a plain block.
- **Light:** Vibrant Quartz Glass 15, Charged Quartz Fixture 14 (a torch-shaped block that needs a supporting face).
- **Hardness:** sky stone is hardness 50, resistance 150 (field value), and harvest level 3. AE2 multiplies break speed by 10 for metas 1 to 3, and for meta 0 when the pick is harvest level 3 or better, so the dressed forms (block, brick, small brick) break like hardness 5. The **stairs and slabs do not get that bonus** (the event handler checks the full block only): sky stone stairs and slabs break at full hardness 50, which makes moving in painful. Certus and fluix blocks use AE2's default hardness 2.2.
- **Meteorite compass:** AE2's meteorite compass searches loaded chunks for `tile.BlockSkyStone` **meta 0 only** (`CompassService.java#L108-L113`), and placing sky stone updates the compass data. Raw sky stone in a city would point every meteorite compass at the city. Metas 1 to 3 are safe.
- **Gravity:** none.

## Sky stone

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `appliedenergistics2:tile.BlockSkyStone:0` | Sky Stone | rubble-ruin, special | partly: rough black rock, reads as basalt rubble | ruined, industrial | Avoid: misleads meteorite compasses; hardness 50 without a level-3 pick; orientable |
| `appliedenergistics2:tile.BlockSkyStone:1` | Sky Stone Block | wall-main, floor, wall-accent | yes: dark polished stone, like polished blackstone | urban, industrial | Breaks like hardness 5; resistance 150 |
| `appliedenergistics2:tile.BlockSkyStone:2` | Sky Stone Brick | wall-main, wall-accent | yes: dark gray large bricks, a blackstone-bricks alternative | urban, industrial, ruined | Breaks like hardness 5 |
| `appliedenergistics2:tile.BlockSkyStone:3` | Sky Stone Small Brick | wall-main, wall-accent, floor | yes: dark small bricks, a deepslate-tiles alternative | urban, industrial | Breaks like hardness 5 |
| `appliedenergistics2:tile.SkyStoneStairBlock:0-7` | Sky Stone Stairs | stairs-slab, rubble-ruin | partly: raw sky stone texture | ruined | Hardness 50, no break bonus |
| `appliedenergistics2:tile.SkyStoneBlockStairBlock:0-7` | Sky Stone Block Stairs | stairs-slab | yes | urban, industrial | Hardness 50, no break bonus |
| `appliedenergistics2:tile.SkyStoneBrickStairBlock:0-7` | Sky Stone Brick Stairs | stairs-slab | yes | urban, industrial | Hardness 50, no break bonus |
| `appliedenergistics2:tile.SkyStoneSmallBrickStairBlock:0-7` | Sky Stone Small Brick Stairs | stairs-slab | yes | urban, industrial | Hardness 50, no break bonus |
| `appliedenergistics2:tile.SkyStoneSlabBlock:0` (+8 top) | Sky Stone Slab | stairs-slab, rubble-ruin | partly: raw texture | ruined | Hardness 50 |
| `appliedenergistics2:tile.SkyStoneBlockSlabBlock:0` (+8 top) | Sky Stone Block Slab | stairs-slab | yes | urban, industrial | Hardness 50 |
| `appliedenergistics2:tile.SkyStoneBrickSlabBlock:0` (+8 top) | Sky Stone Brick Slab | stairs-slab | yes | urban, industrial | Hardness 50 |
| `appliedenergistics2:tile.SkyStoneSmallBrickSlabBlock:0` (+8 top) | Sky Stone Small Brick Slab | stairs-slab | yes | urban, industrial | Hardness 50 |
| `appliedenergistics2:tile.SkyStoneSlabBlock.double:0` | Sky Stone Slabs (double) | stairs-slab | partly | ruined | Same look as meta 0, but a different block: does not trigger the meteorite compass |
| `appliedenergistics2:tile.SkyStoneBlockSlabBlock.double:0` | Sky Stone Block Slabs (double) | stairs-slab, wall-accent | yes | urban, industrial | Hardness 50 |
| `appliedenergistics2:tile.SkyStoneBrickSlabBlock.double:0` | Sky Stone Brick Slabs (double) | stairs-slab | yes | urban, industrial | Hardness 50 |
| `appliedenergistics2:tile.SkyStoneSmallBrickSlabBlock.double:0` | Sky Stone Small Brick Slabs (double) | stairs-slab | yes | urban, industrial | Hardness 50 |
| `appliedenergistics2:tile.BlockSkyChest:0` | Sky Stone Chest | furniture-deco | partly: a dark chest, futuristic | industrial | Tile entity; hardness 50 |
| `appliedenergistics2:tile.BlockSkyChest:1` | Sky Stone Block Chest | furniture-deco | partly | industrial | Tile entity; hardness 50 |

## Certus quartz and fluix

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `appliedenergistics2:tile.BlockQuartz:0` | Certus Quartz Block | wall-main, wall-accent | yes: pale blue-white quartz, a cool-toned partner to `quartz_block` | urban | Hardness 2.2 |
| `appliedenergistics2:tile.BlockQuartzChiseled:0` | Chiseled Certus Quartz Block | wall-accent, trim-pillar | yes: blue-white maze pattern | urban | |
| `appliedenergistics2:tile.BlockQuartzPillar:0` | Certus Quartz Pillar | trim-pillar, wall-main | yes: like the quartz pillar | urban | Orientable |
| `appliedenergistics2:tile.QuartzStairBlock:0-7` | Certus Quartz Stairs | stairs-slab | yes | urban | |
| `appliedenergistics2:tile.ChiseledQuartzStairBlock:0-7` | Chiseled Certus Quartz Stairs | stairs-slab | yes | urban | |
| `appliedenergistics2:tile.QuartzPillarStairBlock:0-7` | Certus Quartz Pillar Stairs | stairs-slab | yes | urban | |
| `appliedenergistics2:tile.QuartzSlabBlock:0` (+8 top) | Certus Quartz Slab | stairs-slab, floor | yes | urban | |
| `appliedenergistics2:tile.ChiseledQuartzSlabBlock:0` (+8 top) | Chiseled Certus Quartz Slab | stairs-slab | yes | urban | |
| `appliedenergistics2:tile.QuartzPillarSlabBlock:0` (+8 top) | Certus Quartz Pillar Slab | stairs-slab | yes | urban | |
| `appliedenergistics2:tile.QuartzSlabBlock.double:0` | Certus Quartz Slabs (double) | stairs-slab, floor | yes | urban | |
| `appliedenergistics2:tile.ChiseledQuartzSlabBlock.double:0` | Chiseled Certus Quartz Slabs (double) | stairs-slab | yes | urban | |
| `appliedenergistics2:tile.QuartzPillarSlabBlock.double:0` | Certus Quartz Pillar Slabs (double) | stairs-slab | yes | urban | |
| `appliedenergistics2:tile.BlockFluix:0` | Fluix Block | off-theme, industrial-dressing | no: glowing purple crystal | industrial | No light emission despite the look |
| `appliedenergistics2:tile.FluixStairBlock:0-7` | Fluix Stairs | off-theme | no | none | |
| `appliedenergistics2:tile.FluixSlabBlock:0` (+8 top) | Fluix Slab | off-theme | no | none | |
| `appliedenergistics2:tile.FluixSlabBlock.double:0` | Fluix Slabs (double) | off-theme | no | none | |

## Glass and lights

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `appliedenergistics2:tile.BlockQuartzGlass:0` | Quartz Glass | glass | yes: nearly clear glass with a thin dark frame and faint streaks | urban, industrial | Random streak variants (B, C, D) chosen per position; frame drawn by a custom renderer |
| `appliedenergistics2:tile.BlockQuartzLamp:0` | Vibrant Quartz Glass | light, glass | yes: looks like quartz glass and emits light 15 | urban, industrial | A glass window that lights the room: ideal lit-window block |
| `appliedenergistics2:tile.BlockQuartzTorch:0` | Charged Quartz Fixture | light | partly: small white crystal fixture, modern sconce look | urban, industrial | Light 14; needs a supporting face (like a torch); spark particles |

Not catalogued: the Matrix Frame (spatial IO, invisible), Tiny TNT, paint splotches, quartz ores, and every machine (all tile entities).

## Best variety pools

- **Main wall `#` (`bricks_standard`, blackstone and deepslate variants):** Sky Stone Brick (`:2`) and Small Brick (`:3`) as dark-brick alternatives to `blackstone` and `deepslate`; Sky Stone Block (`:1`) as the smooth member. A dark tower in sky stone brick with sky stone block bands reads well next to upstream's `building6`/`building7`.
- **Quartz family (`quartzbricks_border`, `Q`, `(` `)`):** Certus Quartz Block, Pillar and Chiseled with their stairs and slabs give a cool blue-white variant of the quartz palette; mix 1 in 4 with vanilla quartz for subtle variation.
- **Accent `X` / band `$`:** Chiseled Certus Quartz; Sky Stone Block slab (double) as a dark band.
- **Glass `glass_*`, `+`:** Quartz Glass as a clear-glass alternative.
- **Light `h`:** Vibrant Quartz Glass for lit windows and ceiling panels; Charged Quartz Fixture for wall sconces.
- **Rubble `}`:** sky stone stairs and slabs only where breaking speed does not matter (hardness 50).
- **Never:** raw Sky Stone (`:0`) in cities (meteorite compass); fluix outside an AE2-themed set piece.
