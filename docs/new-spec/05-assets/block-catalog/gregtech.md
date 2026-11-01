# Block catalog: GregTech 5 Unofficial

GregTech 5 Unofficial 5.09.54.265 (mod id `gregtech`; jar `gregtech-5.09.54.265.jar`) as GTNH 2.9.0 ships it. Blocks were read from source at the pack tag (`gtnh-src/source/GT5-Unofficial/`: `gregtech/loaders/preload/LoaderGTBlockFluid.java` and the classes in `gregtech/common/blocks/`), names from the jar's `assets/gregtech/lang/en_US.lang`, and tiers from the recipes in `gregtech/loaders/postload/recipes/` and the multiblock each casing belongs to. GT has no per-block toggles for these blocks: everything below is registered in the pack (`config/GregTech/` only switches world generation). The same jar also carries bartworks and GT++, catalogued in [bartworks.md](bartworks.md) and [gtplusplus.md](gtplusplus.md). Sheets: `tools/block-contact-sheet/sheets/gregtech/*.json` (`stones`, `concrete`, `reinforced`, `glass`, `casings-tiered`, `casings-structural`, `hazard`, `metal-blocks`, `frames-sheetmetal`, `hulls-misc`), rendered to `docs/previews/blocks/gregtech/`. This is part of the D25 block catalog ([decision log](../../../design/decision-log.md)): every decorative and construction block judged against upstream's look so palettes can draw variety per role; [block-mapping.md](../../00-pack-integration/block-mapping.md) (D19) stays the baseline.

Registry names: GT registers each block under its unlocalized name, so they read `gregtech:gt.blockcasings` and so on. GT textures are composed in code from icon sets: each entry below names the PNG in `assets/gregtech/textures/blocks/iconsets/` that the block actually shows (`Textures.BlockIcons`), and the sheets use those files.

## Summary

**247 block+meta entries in 35 registry blocks**, plus three groups summarized rather than listed row by row (below). Fit with upstream's look: **58 yes, 111 partly, 78 no**.

| Role (a block can have several) | Count |
|---|---|
| wall-main | 49 |
| wall-accent | 55 |
| trim-pillar | 27 |
| floor | 9 |
| ceiling | 6 |
| roof | 18 |
| glass | 18 |
| pane | 0 |
| street | 6 |
| sidewalk | 4 |
| stairs-slab | 0 |
| rubble-ruin | 35 |
| light | 0 |
| fence-rail | 19 |
| door-trap | 0 |
| furniture-deco | 2 |
| industrial-dressing | 114 |
| foliage | 12 |
| special | 35 |
| off-theme | 26 |

Summarized, not listed per meta:

- **Other storage blocks**: `gt.blockmetal1` to `gt.blockmetal10` and `gt.blockgem1` to `gt.blockgem3` hold 183 material blocks; the 43 that could pass for construction metal or treasure are listed in [Storage blocks](#storage-blocks). The other 140 are exotic or radioactive materials (Americium, Naquadria, Neutronium, Infinity and so on): `special` at most, mostly `off-theme`.
- **Frame boxes and sheetmetal of other materials**: `gt.blockframes` and `gt.sheetmetal` have one meta per GT material (material id as metadata) that generates the prefix, several hundred each, decided at runtime by material flags. The 33 that fit a city are listed. The GT jar also registers `bw.sheetmetal` and `bw.frames` for bartworks materials (meta = Werkstoff id), all exotic.
- **Casings above EV**: 143 metas are excluded by the EV ceiling (record 0004): `gt.blockcasings` 5-9 and 12-15, `gt.blockcasings2` 5-11 and 15, `gt.blockcasings3` 12 and 15, `gt.blockcasings4` 0, 6-8, 12, 14, `gt.blockcasings5` 3-13, `gt.blockcasings6` 5-14, `gt.blockcasings8` 2-8 and 10-14, `gt.blockcasings9` 0, 1, 3-15, `gt.blockcasings10` 1-3 and 6-12, `gt.blockcasings11` 4-7, `gt.blockcasings12` 1-14 except 8, `gt.blockcasings13` 5-15, `gt.blockcasings14` 3-7, `gt.blockcasingsNH` 5, 6, 10-14, all of `gt.foundrycasings` (13) and `gt.blockcasings.bec` (7). The merged addons' casings (TecTech, GoodGenerator, KekzTech, KubaTech, gtnhlanth, GGFab) are not enumerated: their multiblocks start at IV almost without exception. Check one by one if an industrial style wants more.

**Rock wool does not exist in GTNH 2.9.0.** No block, item, texture or lang key named rock wool, rockwool or mineral wool is in any of the pack's 216 jars or in any mod's source. The nearest things are NewHorizonsCoreMod's *Alumino Silicate Wool* (an item, not a block) and GT's `INSULATION_*` icon sets (cable insulation overlays). If the owner saw rock wool elsewhere, it is from a later GT or another pack.

**Flags**

- **Ore and boulder hosts.** All 32 metas of `gt.blockgranites` and `gt.blockstones` are **boulder hosts** (GT's `WorldgenStone` matches the block, ignoring metadata), and metas 0 and 8 (raw black granite, red granite, marble, basalt) are also **ore hosts** ([ore-veins.md](../../00-pack-integration/ore-veins.md)). Raw stones are never building blocks (D19 rule 5). The cobble, brick, chiseled and smooth forms are usable only under the same terms as terracotta (D18: accepted for alpha, restore pass if screenshots show damage). Nothing else here is a host: GT concrete, reinforced blocks, casings and glass are all safe.
- **Tile entities.** Only the machine hulls (`gt.blockmachines`, MetaTileEntities) need one. **Every GT casing, glass, concrete, stone, storage block, frame box and sheetmetal is a plain block with metadata.** A frame box gains a tile entity only when a cover is put on it.
- **Machine-update scans.** Casings, `gt.blockreinforced` metas 2, 3 and 10, frame boxes and sheetmetal are registered as GT machine blocks. Their `onBlockAdded` queues a flood-fill scan for nearby multiblock controllers (`RunnableMachineUpdate`, batched per tick per world, skipped when the world is null). A block array written in `provideChunk` never calls `onBlockAdded`; blocks set during population or later do. A large casing facade set block by block in population costs one scan over all connected casings; cheap per block, but measure it.
- **Extended metadata.** `gt.blockframes`, `gt.sheetmetal`, `bw.frames` and `bw.sheetmetal` use the GT material id as metadata (19 to about 1000). That only works through EndlessIDs' extended block metadata, so the chunk writer must use an API that stores more than 4 bits (`setBlock` on the world or chunk does; writing nibble arrays directly does not).
- **Mining.** GT stones are hardness 4.5 (three times stone) and resistance 60; granites need a diamond-level pick. Casings and GT glass take a **wrench** (harvest level 2); a pickaxe is slow. `gt.blockreinforced` is the painful one: Reinforced Block 40/120, Bronzeplate 60/150, Steel 150/200, Titanium 200/300. GT concrete is as soft as stone.
- **Mob spawning.** No mob spawns naturally on casings (`canCreatureSpawn` false) or on the brick, chiseled and smooth forms of GT stones and GT concrete (only metas 0-2 and 8-10 allow spawns). Floors of these change spawning in dark interiors.
- **Movement.** GT concrete speeds walking by 1.4× horizontally, like Chisel concrete: good for streets and sidewalks, odd for floors.
- **Light and gravity.** None of these blocks emits light or falls. GT has no lamp block.
- **Hazards.** The Powderbarrel (`gt.blockreinforced:5`) explodes. The Bounce and Sticky Pads move players.
- **Tints.** Machine casings are tinted by the client's "machine metal" color, white by default (no change). Frame boxes and sheetmetal are grayscale textures colored by the material's RGB at runtime: the sheet approximates them with the renderer's `tint` field.

## Stones (sheet `stones.json`)

All four stones share one pattern of eight forms: raw, cobblestone, mossy cobblestone, bricks, cracked bricks, mossy bricks, chiseled, smooth. Black granite is near black, red granite crimson-pink, marble white with soft gray veining, basalt dark charcoal. They are good-looking and on-theme (marble and granite civic buildings, basalt plinths), but every meta is a GT boulder host. They need the owner's decision before any palette uses them.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockgranites:0` | Black Granite | off-theme | no | none | **Ore host and boulder host**: never a building block (D19 rule 5). Near-black speckled stone. |
| `gregtech:gt.blockgranites:1` | Black Granite Cobblestone | rubble-ruin | partly: boulder host | ruined | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60, needs a diamond-level pick |
| `gregtech:gt.blockgranites:2` | Mossy Black Granite Cobblestone | rubble-ruin, foliage | partly: boulder host | overgrown, ruined | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockgranites:3` | Black Granite Bricks | wall-main, wall-accent | partly: boulder host | urban, industrial | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60, needs a diamond-level pick |
| `gregtech:gt.blockgranites:4` | Cracked Black Granite Bricks | rubble-ruin, wall-main | partly: boulder host | ruined | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockgranites:5` | Mossy Black Granite Bricks | wall-main, foliage | partly: boulder host | overgrown | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockgranites:6` | Chiseled Black Granite | wall-accent, trim-pillar | partly: boulder host | urban, industrial | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockgranites:7` | Smooth Black Granite | wall-main, floor, trim-pillar | partly: boulder host | urban, industrial | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60, needs a diamond-level pick |
| `gregtech:gt.blockgranites:8` | Red Granite | off-theme | no | none | **Ore host and boulder host**: never a building block (D19 rule 5). Crimson-pink stone. |
| `gregtech:gt.blockgranites:9` | Red Granite Cobblestone | rubble-ruin | partly: boulder host | ruined | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60, needs a diamond-level pick |
| `gregtech:gt.blockgranites:10` | Mossy Red Granite Cobblestone | rubble-ruin, foliage | partly: boulder host | overgrown, ruined | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockgranites:11` | Red Granite Bricks | wall-main | partly: boulder host | urban, desert | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60, needs a diamond-level pick |
| `gregtech:gt.blockgranites:12` | Cracked Red Granite Bricks | rubble-ruin, wall-main | partly: boulder host | ruined | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockgranites:13` | Mossy Red Granite Bricks | wall-main, foliage | partly: boulder host | overgrown | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockgranites:14` | Chiseled Red Granite | wall-accent, trim-pillar | partly: boulder host | urban, desert | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockgranites:15` | Smooth Red Granite | wall-main, floor, trim-pillar | partly: boulder host | urban, desert | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60, needs a diamond-level pick |
| `gregtech:gt.blockstones:0` | Marble | off-theme | no | none | **Ore host and boulder host**: never a building block (D19 rule 5). White stone. |
| `gregtech:gt.blockstones:1` | Marble Cobblestone | rubble-ruin | partly: boulder host | ruined | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60 |
| `gregtech:gt.blockstones:2` | Mossy Marble Cobblestone | rubble-ruin, foliage | partly: boulder host | overgrown, ruined | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockstones:3` | Marble Bricks | wall-main | partly: boulder host | urban | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60 |
| `gregtech:gt.blockstones:4` | Cracked Marble Bricks | rubble-ruin, wall-main | partly: boulder host | ruined | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockstones:5` | Mossy Marble Bricks | wall-main, foliage | partly: boulder host | overgrown | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockstones:6` | Chiseled Marble | wall-accent, trim-pillar | partly: boulder host | urban | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockstones:7` | Smooth Marble | wall-main, floor, trim-pillar | partly: boulder host | urban | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60 |
| `gregtech:gt.blockstones:8` | Basalt | off-theme | no | none | **Ore host and boulder host**: never a building block (D19 rule 5). Dark charcoal stone. |
| `gregtech:gt.blockstones:9` | Basalt Cobblestone | rubble-ruin | partly: boulder host | ruined | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60 |
| `gregtech:gt.blockstones:10` | Mossy Basalt Cobblestone | rubble-ruin, foliage | partly: boulder host | overgrown, ruined | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockstones:11` | Basalt Bricks | wall-main, wall-accent | partly: boulder host | urban, industrial | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60 |
| `gregtech:gt.blockstones:12` | Cracked Basalt Bricks | rubble-ruin, wall-main | partly: boulder host | ruined | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockstones:13` | Mossy Basalt Bricks | wall-main, foliage | partly: boulder host | overgrown | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockstones:14` | Chiseled Basalt | wall-accent, trim-pillar | partly: boulder host | urban, industrial | boulder host (all 16 metas): GT boulders overwrite it |
| `gregtech:gt.blockstones:15` | Smooth Basalt | wall-main, floor, trim-pillar | partly: boulder host | urban, industrial | boulder host (all 16 metas): GT boulders overwrite it; hardness 4.5, resistance 60 |

## Concrete (sheet `concrete.json`)

Not gray: dark concrete renders mid-brown and light concrete beige. The brick, cracked and chiseled forms read as brown and sand-colored masonry rather than poured concrete, so they widen the brick and desert families more than the concrete one. Light Concrete Bricks are a close match for upstream's cut sandstone. Not ore or boulder hosts.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockconcretes:0` | Dark Concrete | wall-main, street, sidewalk | partly: brown, not gray | urban, industrial | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:1` | Dark Concrete Cobblestone | rubble-ruin | partly | ruined | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:2` | Mossy Dark Concrete Cobblestone | rubble-ruin, foliage | partly | overgrown, ruined | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:3` | Dark Concrete Bricks | wall-main | yes: brown brick | urban | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:4` | Cracked Dark Concrete Bricks | rubble-ruin, wall-main | yes | ruined | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:5` | Mossy Dark Concrete Bricks | wall-main, foliage | yes | overgrown | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:6` | Chiseled Dark Concrete | wall-accent, trim-pillar | yes | urban | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:7` | Smooth Dark Concrete | floor, street, sidewalk | partly: brown | urban, industrial | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:8` | Light Concrete | wall-main, sidewalk | yes | urban, desert | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:9` | Light Concrete Cobblestone | rubble-ruin | yes | ruined, desert | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:10` | Mossy Light Concrete Cobblestone | rubble-ruin, foliage | yes | overgrown, ruined | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:11` | Light Concrete Bricks | wall-main | yes: beige brick, reads as cut sandstone | urban, desert | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:12` | Cracked Light Concrete Bricks | rubble-ruin, wall-main | yes | ruined, desert | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:13` | Mossy Light Concrete Bricks | wall-main, foliage | yes | overgrown | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:14` | Chiseled Light Concrete | wall-accent, trim-pillar | yes | urban, desert | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |
| `gregtech:gt.blockconcretes:15` | Smooth Light Concrete | floor, sidewalk, street, ceiling | yes | urban, desert | not an ore or boulder host; hardness 1.5 (stone), resistance 20; walking on it is 1.4× faster (like Chisel concrete) |

## Reinforced blocks (sheet `reinforced.json`)

`gt.blockreinforced` mixes building materials, fuels and endgame plates. Two are worth having: Plascrete (`:2`, clean white panel, soft) and the Reinforced Block (`:13`, gray poured concrete, but hardness 40).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockreinforced:0` | Bronzeplate Reinforced Block | industrial-dressing, wall-accent | no: bright orange plate | industrial | hardness 60, resistance 150 (painful); steam age |
| `gregtech:gt.blockreinforced:1` | Iridium Reinforced Block | off-theme | no | none | hardness 400, resistance 600; iridium is past EV |
| `gregtech:gt.blockreinforced:2` | Plascrete Block | wall-main, floor, ceiling | yes: clean white panel | urban, industrial | hardness 5 (iron), resistance 6, wrench; cleanroom casing (MV), registered as a GT machine block: placing it schedules a machine-structure update scan |
| `gregtech:gt.blockreinforced:3` | Tungstensteel Reinforced Block | off-theme | no | none | hardness 5 (iron) but resistance 400; tungstensteel is IV, above the EV ceiling; also a machine block |
| `gregtech:gt.blockreinforced:4` | Brittle Charcoal | off-theme | no | none | looks like a coal block; hardness 0.5; drops charcoal |
| `gregtech:gt.blockreinforced:5` | Powderbarrel | special | no | none | **explosive**: ignites like TNT; never place |
| `gregtech:gt.blockreinforced:6` | Solid Super Fuel | off-theme | no | none | coal-block look, fuel item |
| `gregtech:gt.blockreinforced:7` | Magic Solid Super Fuel | off-theme | no | none | coal-block look, fuel item |
| `gregtech:gt.blockreinforced:8` | Steel Reinforced Block | industrial-dressing, wall-accent | partly: riveted steel | industrial | hardness 150, resistance 200: very painful to dig |
| `gregtech:gt.blockreinforced:9` | Titanium Reinforced Block | off-theme | no: purple | none | hardness 200, resistance 300 |
| `gregtech:gt.blockreinforced:10` | Naquadah Reinforced Block | off-theme | no | none | naquadah is past EV; also a machine block |
| `gregtech:gt.blockreinforced:11` | Neutronium Reinforced Block | off-theme | no | none | hardness 750, resistance 2500 |
| `gregtech:gt.blockreinforced:12` | Raw Deep Dark Portal Block | off-theme | no | none | hardness 1500, resistance 5000; endgame |
| `gregtech:gt.blockreinforced:13` | Reinforced Block (reinforced concrete) | wall-main, floor, street | yes: gray poured concrete | urban, industrial | hardness 40, resistance 120: close to obsidian to dig. Best GT gray concrete, but bunker-grade |

## Glass (sheet `glass.json`)

Tinted Industrial Glass is the find: 16 dye colors of frameless EV glass, including exact matches for upstream's white, gray, light blue, blue and black glass palettes and the city-sphere `Z` mix. As casings they take a wrench to break. `gt.blockglass1` metas 1-9 are endgame multiblock glass.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockglass1:0` | Chemical Grade Glass | glass | partly: frosted gray, riveted frame | industrial | EV recipe; casing block: wrench is the harvest tool (a pickaxe is slow), hardness 5, no mob spawns on it |
| `gregtech:gt.blockglass1:1` | Electron-Permeable Neutronium Coated Glass | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:2` | Omni-Purpose Infinity Fused Glass | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:3` | Non-Photonic Matter Exclusion Glass | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:4` | Hawking Radiation Realignment Focus | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:5` | Nanite Shielding Glass | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:6` | Chamber Grate | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:7` | Exo-Foundry Containment Glass | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:8` | Nanochip Complex Glass | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:9` | Electromagnetic Waveguide | off-theme | no | none | endgame |
| `gregtech:gt.blockglass1:10` | Reinforced Glass | glass | partly: dark, glittering | industrial | LV (alloy smelter); resistance 108; casing block: wrench is the harvest tool (a pickaxe is slow), hardness 5, no mob spawns on it |
| `gregtech:gt.blocktintedglass:0` | Tinted Industrial Glass (White) | glass | yes: matches upstream's white stained glass | urban, industrial | EV glass (`blockGlassEV`); clean, frameless; casing block: wrench is the harvest tool (a pickaxe is slow), hardness 5, no mob spawns on it |
| `gregtech:gt.blocktintedglass:1` | Tinted Industrial Glass (Light Gray) | glass | yes | urban, industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:2` | Tinted Industrial Glass (Gray) | glass | yes: matches upstream's gray stained glass | urban, industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:3` | Tinted Industrial Glass (Black) | glass | yes: matches upstream's black stained glass | urban, industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:4` | Tinted Industrial Glass (Brown) | glass | yes | urban, industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:5` | Tinted Industrial Glass (Red) | glass | partly: loud color | industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:6` | Tinted Industrial Glass (Orange) | glass | partly: loud color | industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:7` | Tinted Industrial Glass (Yellow) | glass | partly: loud color | industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:8` | Tinted Industrial Glass (Lime) | glass | partly: loud color | industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:9` | Tinted Industrial Glass (Green) | glass | partly: loud color | industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:10` | Tinted Industrial Glass (Cyan) | glass | yes | urban, industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:11` | Tinted Industrial Glass (Light Blue) | glass | yes: matches upstream's light blue stained glass | urban, industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:12` | Tinted Industrial Glass (Blue) | glass | yes: matches upstream's blue stained glass | urban, industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:13` | Tinted Industrial Glass (Purple) | glass | partly: loud color | industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:14` | Tinted Industrial Glass (Magenta) | glass | partly: loud color | industrial | as meta 0 |
| `gregtech:gt.blocktintedglass:15` | Tinted Industrial Glass (Pink) | glass | partly: loud color | industrial | as meta 0 |

## Tiered casings, hermetic casings and coils (sheet `casings-tiered.json`)

The ULV-EV machine casings are what a machine hull looks like without its tile entity: the same textures, minus the output-face overlay. The LV casing is a nearly flat light-gray panel that passes for cladding.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockcasings:0` | ULV Machine Casing | industrial-dressing, wall-accent | partly | industrial | ULV; dark plate with corner bolts; plain block, not a tile entity; registered as a machine block (placement schedules a machine-update scan); casing block: wrench is the harvest tool (a pickaxe is slow), hardness 5, no mob spawns on it |
| `gregtech:gt.blockcasings:1` | LV Machine Casing | industrial-dressing, wall-main, ceiling | yes: plain light-gray panel | urban, industrial | LV; reads as a flat steel panel |
| `gregtech:gt.blockcasings:2` | MV Machine Casing | industrial-dressing | partly | industrial | MV |
| `gregtech:gt.blockcasings:3` | HV Machine Casing | industrial-dressing | partly | industrial | HV; vent slots |
| `gregtech:gt.blockcasings:4` | EV Machine Casing | industrial-dressing | partly | industrial | EV; the ceiling |
| `gregtech:gt.blockcasings6:0` | Hermetic Casing | industrial-dressing | partly | industrial | ULV; blue gauge panel |
| `gregtech:gt.blockcasings6:1` | Hermetic Casing I | industrial-dressing | partly | industrial | LV |
| `gregtech:gt.blockcasings6:2` | Hermetic Casing II | industrial-dressing | partly | industrial | MV |
| `gregtech:gt.blockcasings6:3` | Hermetic Casing III | industrial-dressing | partly | industrial | HV |
| `gregtech:gt.blockcasings6:4` | Hermetic Casing IV | industrial-dressing | partly | industrial | EV |
| `gregtech:gt.blockcasings5:0` | Cupronickel Coil Block | industrial-dressing | partly | industrial | LV/MV EBF coil; copper winding |
| `gregtech:gt.blockcasings5:1` | Kanthal Coil Block | industrial-dressing | partly | industrial | MV/HV coil |
| `gregtech:gt.blockcasings5:2` | Nichrome Coil Block | industrial-dressing | partly | industrial | HV/EV coil |

## Structural casings up to EV (sheet `casings-structural.json`)

Casings of steam-age to EV multiblocks. All are plain blocks. Tiers are the multiblock's tier or, where the recipe tier says little (most casings are assembled at LV), the tier of the material. The last five rows have unverified tiers and stay out until checked.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockcasings:10` | Bronze Plated Bricks | industrial-dressing, wall-accent | partly: orange | industrial | steam age |
| `gregtech:gt.blockcasings:11` | Heat Proof Machine Casing | industrial-dressing, wall-main | partly: green-gray riveted plate | industrial | LV (invar) |
| `gregtech:gt.blockcasings2:0` | Solid Steel Machine Casing | wall-main, industrial-dressing, ceiling | yes: gray riveted steel panel | urban, industrial | LV; best neutral GT wall plate |
| `gregtech:gt.blockcasings2:1` | Frost Proof Machine Casing | wall-accent, industrial-dressing | partly: light blue | industrial | MV (aluminium) |
| `gregtech:gt.blockcasings4:1` | Clean Stainless Steel Machine Casing | wall-main, ceiling, industrial-dressing | yes: pale steel panel | urban, industrial | HV |
| `gregtech:gt.blockcasings4:2` | Stable Titanium Machine Casing | wall-accent, industrial-dressing | partly: pink-violet | industrial | EV |
| `gregtech:gt.blockcasings8:0` | Chemically Inert Machine Casing | wall-accent, industrial-dressing | partly: black panel | industrial | HV (PTFE) |
| `gregtech:gt.blockcasings4:15` | Firebricks | wall-main | yes: tan brick | urban, desert, industrial | steam age; a plain brick look |
| `gregtech:gt.blockcasings12:0` | Coke Oven Bricks | wall-main, wall-accent | yes: dark brown brick | urban, industrial | primitive; brick that is not terracotta and not a boulder host |
| `gregtech:gt.blockcasings9:2` | Primitive Wooden Casing | industrial-dressing, furniture-deco | partly: log crate on stone | industrial, ruined | steam age; top differs from sides |
| `gregtech:gt.blockcasings10:15` | Reinforced Wooden Casing | industrial-dressing, furniture-deco | partly: dark planked crate | industrial | steam age |
| `gregtech:gt.blockcasings2:2` | Bronze Gear Box Casing | industrial-dressing | no | industrial | steam age; gear face |
| `gregtech:gt.blockcasings2:3` | Steel Gear Box Casing | industrial-dressing | partly | industrial | LV |
| `gregtech:gt.blockcasings2:4` | Titanium Gear Box Casing | industrial-dressing | no | industrial | EV |
| `gregtech:gt.blockcasings2:12` | Bronze Pipe Casing | industrial-dressing | no | industrial | steam age; pipe ribs |
| `gregtech:gt.blockcasings2:13` | Steel Pipe Casing | industrial-dressing, wall-accent | partly | industrial | LV |
| `gregtech:gt.blockcasings2:14` | Titanium Pipe Casing | industrial-dressing | no | industrial | EV |
| `gregtech:gt.blockcasings8:1` | PTFE Pipe Casing | industrial-dressing | partly | industrial | HV |
| `gregtech:gt.blockcasings3:13` | Bronze Firebox Casing | industrial-dressing | no | industrial | steam age; grille sides |
| `gregtech:gt.blockcasings3:14` | Steel Firebox Casing | industrial-dressing | partly | industrial | LV |
| `gregtech:gt.blockcasings4:3` | Titanium Firebox Casing | industrial-dressing | no | industrial | EV |
| `gregtech:gt.blockcasings3:10` | Grate Machine Casing | industrial-dressing, fence-rail | partly: barred vent | industrial, urban | MV; reads as a ventilation grille |
| `gregtech:gt.blockcasings3:11` | Filter Machine Casing | industrial-dressing, ceiling | partly: white vent | industrial, urban | MV (cleanroom); a ceiling vent |
| `gregtech:gt.blockcasings4:9` | Turbine Casing | wall-main, industrial-dressing | yes: gray panel | urban, industrial | MV/HV (magnalium) |
| `gregtech:gt.blockcasings4:10` | Stainless Steel Turbine Casing | wall-accent, industrial-dressing | partly | industrial | HV |
| `gregtech:gt.blockcasings4:11` | Titanium Turbine Casing | industrial-dressing | no | industrial | EV |
| `gregtech:gt.blockcasings4:13` | Engine Intake Casing | industrial-dressing | no | industrial | EV (titanium) |
| `gregtech:gt.blockcasings11:0` | Tin Item Pipe Casing | industrial-dressing | partly | industrial | LV |
| `gregtech:gt.blockcasings11:1` | Brass Item Pipe Casing | industrial-dressing | no | industrial | MV |
| `gregtech:gt.blockcasings11:2` | Electrum Item Pipe Casing | industrial-dressing | no | industrial | HV |
| `gregtech:gt.blockcasings11:3` | Platinum Item Pipe Casing | industrial-dressing | no | industrial | EV |
| `gregtech:gt.blockcasings13:0` | Cable Casing | industrial-dressing | no: rainbow wires | industrial | LV |
| `gregtech:gt.blockcasings13:1` | Graphite Moderator Casing | industrial-dressing, wall-accent | partly: black | industrial | EV (HTGR) |
| `gregtech:gt.blockcasings13:2` | Insulated Fluid Pipe Casing | industrial-dressing | partly | industrial | EV (HTGR) |
| `gregtech:gt.blockcasings13:3` | Beryllium Integrated Reactor Casing | industrial-dressing | no | industrial | EV (HTGR) |
| `gregtech:gt.blockcasings13:4` | Refined Graphite Block | wall-accent, floor | partly: matte black | industrial | MV; reads as black tile |
| `gregtech:gt.blockcasings12:15` | Algae Casing | industrial-dressing | no: pink | industrial | HV |
| `gregtech:gt.blockcasingsNH:0` | Air Filter Turbine Casing | industrial-dressing | partly | industrial | MV; same texture as the Turbine Casing |
| `gregtech:gt.blockcasingsNH:1` | Air Filter Vent Casing | industrial-dressing | partly | industrial | MV; same texture as the Steel Pipe Casing |
| `gregtech:gt.blockcasingsNH:2` | Pyrolyse Oven Casing | industrial-dressing | partly | industrial | LV; ULV-casing texture |
| `gregtech:gt.blockcasingsNH:3` | Advanced Air Filter Turbine Casing | industrial-dressing | no | industrial | EV; Stable Titanium texture |
| `gregtech:gt.blockcasingsNH:4` | Advanced Air Filter Vent Casing | industrial-dressing | no | industrial | EV; Titanium Pipe texture |
| `gregtech:gt.blockcasings10:0` | MagTech Casing | industrial-dressing | no: neon magenta | none | tier unverified (Industrial Electromagnetic Separator); exclude until checked |
| `gregtech:gt.blockcasings10:4` | Electric Compressor Casing | industrial-dressing | partly | industrial | tier unverified; exclude until checked |
| `gregtech:gt.blockcasings10:5` | Compression Pipe Casing | industrial-dressing | partly | industrial | tier unverified; exclude until checked |
| `gregtech:gt.blockcasings10:13` | Solidifier Casing | industrial-dressing | no | industrial | tier unverified; exclude until checked |
| `gregtech:gt.blockcasings10:14` | Solidifier Radiator | industrial-dressing | no | industrial | tier unverified; exclude until checked |

## Stripes and hazard signs (sheet `hazard.json`)

`gt.blockcasings3` metas 0-9. The two stripe blocks are the most useful GT dressing for a city: yellow and black caution stripes for loading docks, curbs, barriers and subway edges. The signs show the symbol on every face; one block at a door or fence reads as a sign, a wall of them as wallpaper.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockcasings3:0` | Yellow Stripes Block (A) | wall-accent, street, industrial-dressing | yes: caution stripes | urban, industrial, ruined | LV (from a Solid Steel Machine Casing); stripes for loading docks, curbs, barriers; casing block: wrench is the harvest tool (a pickaxe is slow), hardness 5, no mob spawns on it |
| `gregtech:gt.blockcasings3:1` | Yellow Stripes Block (B) | wall-accent, street, industrial-dressing | yes: caution stripes | urban, industrial, ruined | LV (from a Solid Steel Machine Casing); stripes for loading docks, curbs, barriers |
| `gregtech:gt.blockcasings3:2` | Radioactive Hazard Sign Block | industrial-dressing, wall-accent | yes: warning sign | industrial, ruined | a yellow sign on a steel plate on every face; use one block, not a wall |
| `gregtech:gt.blockcasings3:3` | Bio Hazard Sign Block | industrial-dressing, wall-accent | yes: warning sign | industrial, ruined | a yellow sign on a steel plate on every face; use one block, not a wall |
| `gregtech:gt.blockcasings3:4` | Explosion Hazard Sign Block | industrial-dressing, wall-accent | yes: warning sign | industrial, ruined | a yellow sign on a steel plate on every face; use one block, not a wall |
| `gregtech:gt.blockcasings3:5` | Fire Hazard Sign Block | industrial-dressing, wall-accent | yes: warning sign | industrial, ruined | a yellow sign on a steel plate on every face; use one block, not a wall |
| `gregtech:gt.blockcasings3:6` | Acid Hazard Sign Block | industrial-dressing, wall-accent | yes: warning sign | industrial, ruined | a yellow sign on a steel plate on every face; use one block, not a wall |
| `gregtech:gt.blockcasings3:7` | Magic Hazard Sign Block | industrial-dressing, wall-accent | no: fantasy sign | industrial, ruined | a yellow sign on a steel plate on every face; use one block, not a wall |
| `gregtech:gt.blockcasings3:8` | Frost Hazard Sign Block | industrial-dressing, wall-accent | yes: warning sign | industrial, ruined | a yellow sign on a steel plate on every face; use one block, not a wall |
| `gregtech:gt.blockcasings3:9` | Noise Hazard Sign Block | industrial-dressing, wall-accent | yes: warning sign | industrial, ruined | a yellow sign on a steel plate on every face; use one block, not a wall |

## Storage blocks (sheet `metal-blocks.json`)

GT's storage blocks have real per-material textures (`BLOCK_<MATERIAL>.png`), not tints: a brushed-metal plate in the material's color. Upstream uses vanilla metal blocks only as treasure (`!`). A few could serve as industrial cladding (steel, stainless steel, black steel, aluminium), but every one is nine ingots of loot, which the owner may not want in walls.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockmetal1:1` | Block of Aluminium | wall-accent, industrial-dressing | partly: light blue | urban, industrial | storage block; no tile entity |
| `gregtech:gt.blockmetal1:3` | Block of Annealed Copper | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal1:11` | Block of Black Bronze | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal1:12` | Block of Black Steel | wall-accent, industrial-dressing | yes: dark steel | urban, industrial | storage block; no tile entity |
| `gregtech:gt.blockmetal1:14` | Block of Blue Steel | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal1:15` | Block of Brass | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal2:0` | Block of Bronze | special, wall-accent | partly: copper-orange | industrial | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal2:3` | Block of Chrome | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal2:5` | Block of Cobalt | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal2:7` | Block of Copper | special, roof | partly: copper roofing | urban | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal2:8` | Block of Cupronickel | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal2:9` | Block of Damascus Steel | wall-accent | partly | industrial | storage block; no tile entity |
| `gregtech:gt.blockmetal2:10` | Block of Dark Iron | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal2:15` | Block of Electrum | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal3:11` | Block of Invar | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal4:2` | Block of Lead | wall-accent, roof | partly: dull purple-gray | industrial | storage block; no tile entity |
| `gregtech:gt.blockmetal4:4` | Block of Magnalium | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal4:7` | Block of Meteoric Iron | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal5:3` | Block of Nichrome | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal5:4` | Block of Nickel | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal5:11` | Block of Pig Iron | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal6:1` | Block of Red Alloy | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal6:2` | Block of Red Steel | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal6:3` | Block of Rose Gold | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal6:10` | Block of Silver | special | partly: treasure | urban | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal6:11` | Block of Soldering Alloy | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal6:12` | Block of Stainless Steel | wall-accent, industrial-dressing | yes: pale steel | urban, industrial | storage block; no tile entity |
| `gregtech:gt.blockmetal6:13` | Block of Steel | wall-accent, industrial-dressing, special | yes: gray steel | urban, industrial | storage block; no tile entity |
| `gregtech:gt.blockmetal6:15` | Block of Sterling Silver | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal7:7` | Block of Tin | special | partly: light gray | industrial | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal7:8` | Block of Tin Alloy | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal7:9` | Block of Titanium | special | no: lavender | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal7:11` | Block of Tungsten | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal7:12` | Block of Tungstensteel | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal8:2` | Block of Cast Iron | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal8:6` | Block of Zinc | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockmetal8:8` | Block of Vanadium Steel | special | no | none | a storage block of 9 ingots: loot, not a wall. Plain block, pickaxe, no tile entity |
| `gregtech:gt.blockgem2:1` | Block of Lignite | off-theme | no | none | storage block; no tile entity |
| `gregtech:gt.blockgem3:4` | Block of Charcoal | off-theme | no | none | storage block; no tile entity |
| `gregtech:gt.blockgem2:9` | Block of Quartzite | wall-accent | partly: pale green tile | urban | storage block; no tile entity |
| `gregtech:gt.blockgem1:5` | Block of Certus Quartz | wall-accent | partly | urban | storage block; no tile entity |
| `gregtech:gt.blockgem2:0` | Block of Lazurite | off-theme | no | none | storage block; no tile entity |
| `gregtech:gt.blockgem2:13` | Block of Sodalite | off-theme | no | none | storage block; no tile entity |

## Frame boxes and sheetmetal (sheet `frames-sheetmetal.json`)

Frame boxes are see-through girder lattices: steel and aluminium frames make scaffolding, gantries, fire escapes, half-built floors and ruined skeletons, which upstream does not have at all. Sheetmetal is corrugated siding: warehouses, sheds, roofs. Both are tinted at runtime; the render is an approximation. Both need extended metadata (see Flags).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockframes:305` | Steel Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | yes: girder lattice | urban, industrial, ruined | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:19` | Aluminium Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | yes: girder lattice | urban, industrial, ruined | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:306` | Stainless Steel Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | yes: girder lattice | urban, industrial, ruined | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:28` | Titanium Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | partly: colored girder | industrial | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:32` | Iron Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | yes: girder lattice | urban, industrial, ruined | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:304` | Cast Iron (Wrought Iron) Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | yes: girder lattice | urban, industrial, ruined | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:300` | Bronze Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | partly: colored girder | industrial | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:301` | Brass Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | partly: colored girder | industrial | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:35` | Copper Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | partly: colored girder | industrial | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:57` | Tin Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | yes: girder lattice | urban, industrial, ruined | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:89` | Lead Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | partly: colored girder | industrial | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:334` | Black Steel Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | yes: girder lattice | urban, industrial, ruined | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:302` | Invar Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | partly: colored girder | industrial | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:36` | Zinc Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | yes: girder lattice | urban, industrial, ruined | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:348` | Red Steel Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | partly: colored girder | industrial | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.blockframes:349` | Blue Steel Frame Box | fence-rail, trim-pillar, industrial-dressing, rubble-ruin | partly: colored girder | industrial | see-through lattice, climbable like scaffolding; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; plain block until a cover is attached (then a tile entity) |
| `gregtech:gt.sheetmetal:305` | Steel Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | yes: corrugated siding | urban, industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:19` | Aluminium Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | yes: corrugated siding | urban, industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:306` | Stainless Steel Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | yes: corrugated siding | urban, industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:28` | Titanium Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | partly: colored siding | industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:32` | Iron Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | yes: corrugated siding | urban, industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:304` | Cast Iron (Wrought Iron) Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | yes: corrugated siding | urban, industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:300` | Bronze Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | partly: colored siding | industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:301` | Brass Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | partly: colored siding | industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:35` | Copper Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | partly: colored siding | industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:57` | Tin Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | yes: corrugated siding | urban, industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:89` | Lead Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | partly: colored siding | industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:334` | Black Steel Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | yes: corrugated siding | urban, industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:302` | Invar Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | partly: colored siding | industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:36` | Zinc Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | yes: corrugated siding | urban, industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:348` | Red Steel Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | partly: colored siding | industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.sheetmetal:349` | Blue Steel Sheetmetal | wall-main, wall-accent, roof, industrial-dressing | partly: colored siding | industrial | corrugated panel; meta is the GT material id (> 15): needs EndlessIDs' extended metadata; registered as a machine block; color is a runtime tint |
| `gregtech:gt.blockframes:809` | Wood Frame Box | fence-rail, trim-pillar, rubble-ruin | yes: timber scaffold | urban, ruined, overgrown | meta is the GT material id (> 15): needs EndlessIDs' extended metadata |

## Machine hulls, fence and pads (sheet `hulls-misc.json`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockmachines (MTE 10)` | ULV Machine Hull | industrial-dressing | partly | industrial | ULV; **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockmachines (MTE 11)` | LV Machine Hull | industrial-dressing | partly | industrial | LV; **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockmachines (MTE 12)` | MV Machine Hull | industrial-dressing | partly | industrial | MV; **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockmachines (MTE 13)` | HV Machine Hull | industrial-dressing | partly | industrial | HV; **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockmachines (MTE 14)` | EV Machine Hull | industrial-dressing | partly | industrial | EV; **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockmachines (MTE 1)` | Bronze Hull | industrial-dressing | no | industrial | steam; **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockmachines (MTE 2)` | Bricked Bronze Hull | industrial-dressing | partly | industrial | steam; **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockmachines (MTE 3)` | Steel Hull | industrial-dressing | partly | industrial | steam (high pressure); **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockmachines (MTE 4)` | Bricked Wrought Iron Hull | industrial-dressing | partly | industrial | steam; **MetaTileEntity** (`gt.blockmachines` with tile-entity NBT `mID`): costly and risky in worldgen; the matching casing looks the same without the tile entity |
| `gregtech:gt.blockfencemetal:0` | Iron Fence | fence-rail | yes: iron railing | urban, industrial, ruined | a fence block; alternative to iron bars and oak fence |
| `gregtech:gt.blockpad:0` | Bounce Pad | special | no | none | launches players; thin pad |
| `gregtech:gt.blockpad:1` | Sticky Pad | special | no | none | slows players; thin pad |

## Best variety pools

Suggested weights are relative within a pool. Boulder hosts are marked †; they wait for the owner's decision.

- **Main wall `#`, stone-brick family (`bricks_standard`, `stonebrick` variant):** Basalt Bricks† `gt.blockstones:11` (dark), Light Concrete Bricks `gt.blockconcretes:11` (warm), Solid Steel Machine Casing `gt.blockcasings2:0` for an industrial reskin. Marble Bricks† `gt.blockstones:3` as a pale civic brick.
- **Main wall, brick and terracotta family (`bricks`, `bricks_gray`, `bricks_silver`, `bricks_cyan`):** Red Granite Bricks† `gt.blockgranites:11` (reads as red brick), Coke Oven Bricks `gt.blockcasings12:0` (dark brown brick, not a host), Firebricks `gt.blockcasings4:15` (tan brick), Dark Concrete Bricks `gt.blockconcretes:3`. Coke oven bricks and firebricks are the host-free way to get brick variety.
- **Quartz family (`quartzbricks_border`, `quartz` variant, `Q`):** Smooth Marble† `gt.blockstones:7`, Marble Bricks† `:3`, Chiseled Marble† `:6` as the accent; host-free: Plascrete `gt.blockreinforced:2` (white panel), Iron Plated Bricks from GT++.
- **Accent `X` and band `$`:** Chiseled Black Granite† `gt.blockgranites:6`, Smooth Black Granite† `:7`, Chiseled Light Concrete `gt.blockconcretes:14`, Refined Graphite Block `gt.blockcasings13:4` (black band), Yellow Stripes `gt.blockcasings3:0/1` (industrial band).
- **Rubble `}`:** Cracked Light and Dark Concrete Bricks `gt.blockconcretes:4/12`, Light Concrete Cobblestone `:9`; mossy forms `:2/5/10/13` for the overgrown variant. Steel and wood frame boxes as exposed structure in ruins.
- **Glass `a`, `+`, `` ` ``, `Z`, `Y`:** Tinted Industrial Glass white `gt.blocktintedglass:0`, light gray `:1`, gray `:2`, black `:3`, light blue `:11`, blue `:12` (the full upstream glass set), cyan `:10` as an extra. Borosilicate Glass `bartworks:BW_TieredGlass:0` as clear glass. Reinforced Glass `gt.blockglass1:10` for a dark industrial window. Panes: none in GT.
- **Street and sidewalk `S`, `u`, `_`:** Smooth Light Concrete `gt.blockconcretes:15` (sidewalk), Light Concrete `:8`, Reinforced Block `gt.blockreinforced:13` (gray, very hard: highway decks only), Yellow Stripes for curbs, crossings and loading bays.
- **Highway `R`:** Reinforced Block `gt.blockreinforced:13`; Smooth Dark Concrete `gt.blockconcretes:7` for a brown-gray deck.
- **Pillars `v`:** Chiseled Basalt† `gt.blockstones:14`, Chiseled Light Concrete `gt.blockconcretes:14`, steel frame boxes `gt.blockframes:305` for exposed steel columns.
- **Iron bars `:` and fences `.`:** Iron Fence `gt.blockfencemetal:0`, steel and aluminium frame boxes, Grate Machine Casing `gt.blockcasings3:10` as a vent grille.
- **Roof:** Steel, Aluminium, Zinc and Tin Sheetmetal (`gt.sheetmetal:305/19/36/57`), Lead and Copper storage blocks as metal roofing accents.
- **Light `h`:** none in GT.
- **Oil rig and industrial set dressing:** Solid Steel, Clean Stainless Steel and Turbine Casings, Steel Pipe and Gear Box Casings, hazard signs, Steel Firebox, Hermetic Casings, Cupronickel Coil, Primitive and Reinforced Wooden Casings (crates), Wood Frame Box; the ULV-EV machine casings in place of hulls.
- **Desert bricks:** Light Concrete Bricks and Smooth Light Concrete `gt.blockconcretes:11/15`, Firebricks `gt.blockcasings4:15`, Red Granite Bricks† for `bricks_desert_red`.

## Owner decisions

1. **GT stones as building blocks.** Every meta of `gt.blockgranites` and `gt.blockstones` is a boulder host. They add the best marble, granite and basalt in the pack, and the D18 stance on terracotta (accept, then restore pass if screenshots show damage) could extend to them, or they stay out entirely. Raw metas 0 and 8 stay out either way (ore hosts).
2. **Casings as set dressing and as cladding.** Casings are plain blocks (no tile entity), so they are cheap to place. Costs: a machine-update scan per block placed after `provideChunk`, wrench mining, no mob spawns on them, and a "GT factory" look that suits only an industrial style. Tier rule: only the ULV-EV metas listed above.
3. **Machine hulls.** Real hulls are MetaTileEntities: each needs a `BaseMetaTileEntity` with `mID` NBT, ticks, and can be broken for a hull item worth resources. The matching casing looks the same. Recommendation: never place hulls; use casings.
4. **Storage blocks in walls.** Steel and stainless steel blocks are good cladding but are nine ingots each.
5. **Frames and sheetmetal** need extended-metadata support in the chunk writer.
6. **Hard blocks.** The Reinforced Block (hardness 40) is the best gray concrete in GT but slow to break; steel and bronze reinforced blocks are worse. Vaults and bunkers only?
