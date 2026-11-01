# Block catalog: Extra Utilities

Mod id `ExtraUtilities`, jar `extrautilities-1.2.12.jar`. Extra Utilities has no public source; this catalog reads the decompile in `gtnh-src/mods/extrautilities/` (`com/rwtema/extrautils/ExtraUtils.java` registration code, `block/*`, `tileentity/chests/*`) and the jar's `en_US.lang`. Pack config checked: `config/ExtraUtilities.cfg`: every block below is enabled; the pack disables the Ender Lily, the Peaceful Table and the Golden Bag. Sheets: [`decorative`](../../../../tools/block-contact-sheet/sheets/extrautilities/decorative.json), [`colored`](../../../../tools/block-contact-sheet/sheets/extrautilities/colored.json), [`misc`](../../../../tools/block-contact-sheet/sheets/extrautilities/misc.json) under `tools/block-contact-sheet/sheets/extrautilities/`. This catalog serves D25 in the [decision log](../../../design/decision-log.md): variety for palettes, with the [D19 mapping](../../00-pack-integration/block-mapping.md) as baseline.

`ExtraUtils.registerBlock` strips `tile.` and `extrautils:` from the unlocalized name, so names are `ExtraUtilities:decorativeBlock1`, `ExtraUtilities:etherealglass` and so on. The colored blocks take their name from the vanilla block's unlocalized name: `color_lightgem` (glowstone), `color_hellsand` (soul sand), `color_redstoneLight` (lit redstone lamp), and `color_stonebrick`, which is **colored cobblestone** (vanilla cobblestone's unlocalized name is `stonebrick`); colored stone bricks are `colorStoneBrick`.

## Summary

- **296 block+meta entries in 36 registry blocks**, 208 of them the 13 colored blocks × 16 colors.
- Fit upstream's look: 36 `yes`, 140 `partly`, 120 `no`.
- Not GT ore hosts or boulder hosts ([ore-veins.md](../../00-pack-integration/ore-veins.md)). `ExtraUtilities:color_stone` and `color_stonebrick` look like stone and cobblestone but are different blocks, so neither GT ores nor boulders replace them: they are host-safe stand-ins.

| Primary role | Entries |
|---|---|
| wall-main | 68 (4 decorative stones; colored stone bricks, stone, quartz and bricks, 16 each) |
| wall-accent | 2 (border stones) |
| street | 1 (gravel road) |
| glass | 11 |
| light | 50 (glowstone glass; colored glowstone, colored lamps and Lapis Caelestis, 16 each; chandelier) |
| floor | 16 (colored planks) |
| rubble-ruin | 16 (colored cobblestone) |
| furniture-deco | 9 |
| industrial-dressing | 3 (conveyor, drums) |
| special | 11 (ethereal glass, magnum torch, spikes) |
| off-theme | 109 |

Flags:

- **Colors are metadata and render-time tints.** `BlockColor` multiplies the vanilla texture by a per-meta color: the sheep fleece colors desaturated by 15 % (`BlockColor.initColor`). The `colored` sheet shows the untinted faces; a local-only tinted approximation (`docs/previews/blocks/extrautilities/colored-tinted.png`, made by the same multiply) was used for judging. The color palette can be changed per chunk by a "datablock" (`ExtraUtilities:datablock`, tile entity) that the paintbrush places; without one, the default palette applies.
- **Colored blocks copy their base block**: hardness, resistance, light and opacity, and they delegate `canProvidePower`, so **colored redstone blocks emit redstone power** and colored glowstone and colored lamps give light 15. The colored lamp is a plain block, always lit, with no redstone needed.
- **Decorative blocks are soft:** `BlockDecoration` sets hardness 0.45 for every meta except Ender-infused Obsidian (obsidian hardness 50, resistance five times obsidian's) and the two obsidian glasses (hardness 4, resistance 2000).
- **Tile entities:** chandelier, magnum torch, spikes, trading post, filing cabinets, trash cans, drums, sound mufflers, both chests. The colored and decorative blocks, ethereal glass, Lapis Caelestis, curtains and compressed blocks are plain blocks.
- **Mob spawning:** the chandelier stops mob spawns within 16 blocks and the magnum torch in a much larger radius (lang documentation). Either one placed in a city would suppress natural mob spawning around it, which changes how dangerous the city is.
- **Hazards:** spikes damage entities; ethereal glass lets players (metas 0 to 2) or mobs (3 to 5) pass through, so walls of it are holes; conveyors move entities.
- **Hardness:** Bedrockium Block 1000 (resistance 6,000,000); compressed cobblestone and its dirt, gravel and sand forms scale with compression (8× cobblestone is hardness about 3000).
- **Light:** Glowstone Glass 15, Diamond-Etched Matrix 14, Ender Core 5, chandelier 15, magnum torch 15; Lapis Caelestis emits light by color brightness (white 15, black about 1).

## Decorative blocks

`ExtraUtilities:decorativeBlock1` (15 metas) and `decorativeBlock2` (12 metas). Most use Extra Utilities' own connected textures, in which the plain file is the fully connected interior and `<name>_corners.png` is an isolated block; the sheet shows the `_corners` face.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ExtraUtilities:decorativeBlock1:0` | Edged Stone Bricks | wall-main, wall-accent | yes: stone bricks whose wall edges get a carved border | urban | CTM; hardness 0.45 |
| `ExtraUtilities:decorativeBlock1:1` | Ender-infused Obsidian | off-theme | no: green-speckled obsidian | none | Hardness 50 |
| `ExtraUtilities:decorativeBlock1:2` | Burnt Quartz | wall-main, wall-accent | yes: glossy black, like black polished stone or black glass cladding | urban, industrial | CTM; hardness 0.45 |
| `ExtraUtilities:decorativeBlock1:3` | Frosted Stone | off-theme | no: icy blue | none | |
| `ExtraUtilities:decorativeBlock1:4` | Border Stone | wall-accent, trim-pillar | yes: smooth stone with a carved frame, a smooth-stone band (`$`) alternative | urban | CTM |
| `ExtraUtilities:decorativeBlock1:5` | Unstable Ingot Block | off-theme | no | none | |
| `ExtraUtilities:decorativeBlock1:6` | Gravel Bricks | wall-main, rubble-ruin | yes: rough gray bricks | urban, ruined | |
| `ExtraUtilities:decorativeBlock1:7` | Border Stone (Alternate) | wall-accent, trim-pillar | yes: smooth stone with an inset square | urban | CTM |
| `ExtraUtilities:decorativeBlock1:8` | Magical Wood | furniture-deco | partly: gilded bookcase look, for libraries | urban | Boosts enchanting |
| `ExtraUtilities:decorativeBlock1:9` | Sandy Glass | glass | partly: translucent sand-flecked glass | desert | |
| `ExtraUtilities:decorativeBlock1:10` | Gravel Road | street, sidewalk | yes: gray paving with a border at the edges | urban | CTM; top and bottom flipped |
| `ExtraUtilities:decorativeBlock1:11` | Ender Core | off-theme | no | none | Light 5 |
| `ExtraUtilities:decorativeBlock1:12` | Diamond-Etched Computational Matrix | off-theme | no: glowing teal circuitry | industrial | Light 14 |
| `ExtraUtilities:decorativeBlock1:13` | Ender-Sand Alloy | wall-main | partly: pale yellow smooth block | desert | CTM |
| `ExtraUtilities:decorativeBlock1:14` | Carved 'Eminence' Stone | off-theme | no: purple | none | CTM |
| `ExtraUtilities:decorativeBlock2:0` | Thickened Glass | glass | yes: clear glass with a white frame | urban | CTM |
| `ExtraUtilities:decorativeBlock2:1` | Edged Glass | glass | yes: double-framed window | urban | CTM |
| `ExtraUtilities:decorativeBlock2:2` | Glass Bricks | glass, wall-accent | yes: glass-block wall, a 20th-century staple | urban | CTM |
| `ExtraUtilities:decorativeBlock2:3` | Carved Glass | glass | partly: patterned | urban | CTM |
| `ExtraUtilities:decorativeBlock2:4` | Golden Edged Glass | glass | partly: gold frame, luxury lobbies | urban | CTM |
| `ExtraUtilities:decorativeBlock2:5` | Obsidian Glass | glass | yes: clear glass with a dark frame | urban, industrial | Hardness 4, resistance 2000 |
| `ExtraUtilities:decorativeBlock2:6` | Swirling Glass | glass | partly: decorative swirls | urban | CTM |
| `ExtraUtilities:decorativeBlock2:7` | Glowstone Glass | light, glass | yes: lit window, light 15 | urban | CTM |
| `ExtraUtilities:decorativeBlock2:8` | Heart Glass | off-theme | no | none | |
| `ExtraUtilities:decorativeBlock2:9` | Square Glass | glass | yes: four-pane window with mullions | urban | Not CTM |
| `ExtraUtilities:decorativeBlock2:10` | Dark Glass | glass | partly: near-opaque dark glass | urban, industrial | Opacity 255 |
| `ExtraUtilities:decorativeBlock2:11` | Reinforced Dark Glass | glass | partly | industrial | Opacity 255; hardness 4, resistance 2000 |

## Colored blocks

Thirteen registry blocks, metas 0 to 15 in dye order (white 0 … black 15). Each row covers 16 entries; "fits" names the colors judged `yes`, the rest of a `partly` row are `partly`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ExtraUtilities:colorStoneBrick:0-15` | Colored Stone Bricks | wall-main, wall-accent | partly: `yes` for white 0, gray 7, light gray 8, black 15; the rest read as painted brick | urban | Stone brick hardness |
| `ExtraUtilities:colorWoodPlanks:0-15` | Colored Wood Planks | floor, furniture-deco | partly: painted floors and paneling | urban | Flammable |
| `ExtraUtilities:color_lightgem:0-15` | Colored Glowstone | light | partly: `yes` for white 0; the colors suit neon signs and bar interiors | urban | Light 15 |
| `ExtraUtilities:color_stone:0-15` | Colored Stone | wall-main | partly: tinted smooth stone reads as painted concrete or plaster; `yes` for white, gray, light gray, cyan, black | urban | Not an ore host |
| `ExtraUtilities:color_quartzBlock:0-15` | Colored Quartz Block | wall-main, wall-accent | partly: flat color like concrete; `yes` for white, gray, light gray, cyan, black | urban | |
| `ExtraUtilities:color_hellsand:0-15` | Colored Soul Sand | off-theme | no | none | Slows walking |
| `ExtraUtilities:color_redstoneLight:0-15` | Colored Redstone Lamp | light | partly: `yes` for white 0 (a lit lamp face); colored lamps for signs | urban, industrial | Light 15, always on |
| `ExtraUtilities:color_brick:0-15` | Colored Bricks | wall-main | partly: `yes` for white 0 (vanilla look), brown 12, red 14 | urban | |
| `ExtraUtilities:color_stonebrick:0-15` | Colored Cobblestone | rubble-ruin | partly: `yes` for white 0, a cobblestone that is not a boulder host | ruined | Name is `color_stonebrick` |
| `ExtraUtilities:color_blockLapis:0-15` | Colored Lapis Block | off-theme | no | none | |
| `ExtraUtilities:color_obsidian:0-15` | Colored Obsidian | off-theme | no | none | Hardness 50 |
| `ExtraUtilities:color_blockRedstone:0-15` | Colored Redstone Block | off-theme | no | none | Emits redstone power |
| `ExtraUtilities:color_blockCoal:0-15` | Colored Coal Block | off-theme | no | none | Flammable |

## Special glass, lamps, furniture and hazards

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ExtraUtilities:etherealglass:0` | Ethereal Glass | special | no: players walk through | none | Hazard in walls |
| `ExtraUtilities:etherealglass:1` | Ineffable Glass | special | no | none | Players walk through |
| `ExtraUtilities:etherealglass:2` | Dark Ethereal Glass | special | no | none | Players walk through; opacity 255 |
| `ExtraUtilities:etherealglass:3` | Ethereal Glass (Inverted) | special | no | none | Mobs walk through |
| `ExtraUtilities:etherealglass:4` | Ineffable Glass (Inverted) | special | no | none | Mobs walk through |
| `ExtraUtilities:etherealglass:5` | Dark Ethereal Glass (Inverted) | special | no | none | Mobs walk through; opacity 255 |
| `ExtraUtilities:greenscreen:0-15` | Lapis Caelestis (16 colors) | light, wall-accent | partly: flat full-bright color; `yes` for white 0 as a ceiling light panel | urban, industrial | 16 entries; untinted on the sheet; light by color brightness |
| `ExtraUtilities:curtains:0` | Blackout Curtains | furniture-deco | partly: dark hanging curtains for windows | urban, ruined | Opacity 8 |
| `ExtraUtilities:chandelier:0` | Chandelier | light, furniture-deco | partly: diamond and gold chandelier for lobbies | urban | TE; light 15; stops spawns within 16 blocks |
| `ExtraUtilities:magnumTorch:0` | Magnum Torch | special | no | none | TE; large no-spawn radius |
| `ExtraUtilities:cobblestone_compressed:0-15` | Compressed Cobblestone (single to octuple), Compressed Dirt (single to quadruple), Compressed Gravel and Sand (single, double) | off-theme | no | none | 16 entries; texture generated at runtime; hardness grows 2.5× per level |
| `ExtraUtilities:angelBlock:0` | Angel Block | off-theme | no | none | |
| `ExtraUtilities:block_bedrockium:0` | Block of Bedrockium | off-theme | no | none | Hardness 1000 |
| `ExtraUtilities:pureLove:0` | Block of Pure Love | off-theme | no | none | Breeds nearby animals |
| `ExtraUtilities:spike_base:0` | Iron Spike | special | no | none | TE; damages entities; spike model |
| `ExtraUtilities:spike_base_wood:0` | Wooden Spike | special | no | none | TE |
| `ExtraUtilities:spike_base_gold:0` | Golden Spike | special | no | none | TE |
| `ExtraUtilities:spike_base_diamond:0` | Diamond Spike | special | no | none | TE |
| `ExtraUtilities:conveyor:0` | Conveyor Belt | industrial-dressing | partly: factory floor belts | industrial | Moves entities |
| `ExtraUtilities:trading_post:0` | Trading Post | off-theme | no | none | TE |
| `ExtraUtilities:filing:0` | Filing Cabinet | furniture-deco | yes: office furniture | urban | TE |
| `ExtraUtilities:filing:1` | Filing Cabinet (Advanced) | furniture-deco | yes | urban | TE |
| `ExtraUtilities:trashcan:0` | Trash Can | furniture-deco | yes: street and office bins | urban, ruined | TE |
| `ExtraUtilities:trashcan:1` | Trash Can (Fluids) | furniture-deco | partly | industrial | TE |
| `ExtraUtilities:trashcan:2` | Trash Can (Energy) | furniture-deco | partly | industrial | TE |
| `ExtraUtilities:drum:0` | Drum | industrial-dressing | partly: steel drum, oil-rig and factory dressing | industrial | TE |
| `ExtraUtilities:drum:1` | Bedrockium Drum | industrial-dressing | partly | industrial | TE |
| `ExtraUtilities:sound_muffler:0-1` | Sound Muffler, Rain Muffler | off-theme | no | none | 2 entries; TE |
| `ExtraUtilities:chestMini:0` | Mini-Chest | furniture-deco | partly | urban | TE |
| `ExtraUtilities:chestFull:0` | Slightly Larger Chest | furniture-deco | partly: chest alternative | urban | TE |

## Best variety pools

- **Main wall `#`:** Edged Stone Bricks and Gravel Bricks next to stone bricks; Burnt Quartz for black modern facades; colored stone and colored quartz in white, gray, light gray, cyan and black as concrete-like walls (upstream's `9` city-sphere border mixes cyan and gray concrete with terracotta).
- **Colored brick buildings (`bricks_cyan`, `bricks_gray`, `bricks_silver`, `bricks`):** colored stone bricks and colored bricks give tinted brick in the same hues as upstream's terracotta palettes, without terracotta's boulder-host problem.
- **Band `$` and trim:** Border Stone and Border Stone (Alternate).
- **Street and sidewalk `S`/`u`:** Gravel Road.
- **Glass `glass_*`:** Thickened, Edged, Obsidian and Square Glass; Glass Bricks for stairwells and lobbies.
- **Light `h`:** Glowstone Glass (lit windows), white colored lamp and white Lapis Caelestis (ceiling panels); colored glowstone and lamps for neon signs.
- **Rubble `}`:** white colored cobblestone (host-safe cobblestone) and Gravel Bricks.
- **Furniture `F`:** filing cabinets and trash cans (office and street dressing).
- **Avoid:** ethereal glass, spikes, chandelier and magnum torch (spawn suppression), colored redstone blocks (redstone power), compressed blocks and bedrockium (hardness).
