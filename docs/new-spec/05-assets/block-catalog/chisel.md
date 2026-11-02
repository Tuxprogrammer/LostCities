# Block catalog: Chisel

Every decorative block Chisel 2.17.34-GTNH registers in GTNH 2.9.0, with a judgment of how each fits upstream Lost Cities' look and which palette roles it could fill. Decision log [D25](../../../design/decision-log.md): variety is the goal, not one stand-in per missing block. The [block mapping](../../00-pack-integration/block-mapping.md) (D19) stays the baseline and fallback; this catalog is the pool palettes draw extra variety from. Sibling catalogs in this folder cover ChiselTones, Ztones, Et Futurum, vanilla and the other pack mods.

Status: *Draft* for owner review. Roles, fit and styles are judgments made from the contact sheets; they are inputs to palette design, not decisions.

## How this was built

- **Registrations from source.** Every block, metadata and texture is read from [`Features.java`][features] at the pack's tag, including the loops that build the colored and wood families. Registry names are `chisel:<name>` as passed to `GameRegistry.registerBlock` ([CarvableHelper.java#L164-L179][helper]). English names are the variant descriptions from Chisel's `en_US.lang`, which is what the pack shows in tooltips (`tooltipsUseBlockDescriptions=true`).
- **What the pack enables.** The shipped `config/chisel.cfg` turns off only `aeCertusQuartz` and `aeSkyStone`, which add Applied Energistics blocks to carving groups and register no Chisel block. Features missing from the file default to on ([Configurations.java#L305-L321][config]). Every mod a Chisel feature requires is in the pack (Thaumcraft, Blood Magic, Biomes O' Plenty, Railcraft, Twilight Forest), and `oldPillars` is off, so the new pillar textures apply. **Every Chisel block in the source is enabled in the pack.**
- **Textures** come from `chisel-2.17.34-GTNH.jar` in the reference pack, chosen the way Chisel's `TextureType` does ([TextureType.java][texturetype]): `-top`/`-side` pairs, plain faces for connected-texture blocks, and the whole atlas for blocks whose pattern spans several blocks or varies at random (marked `[v9]`, `[v4]`, `[r9]` and so on on the sheets; such a face shows the full 2x2, 3x3 or 4x4 set at once).
- **Contact sheets.** 33 sheets, entry lists in [`tools/block-contact-sheet/sheets/chisel/`](../../../../tools/block-contact-sheet/sheets/chisel/), renders in `docs/previews/blocks/chisel/` (local, not committed). Each group below names its sheet. Rebuild one with:

  ```sh
  python3 -I tools/block-contact-sheet/contact_sheet.py tools/block-contact-sheet/sheets/chisel/marble.json \
      --out docs/previews/blocks/chisel/marble.png --cube 64 --cols 10
  ```

  Sheets show each block alone as a cube. In game, connected textures join with neighbors, panes and carpets are thin, torches are torches, and two-layer blocks (warning signs, lavastone, waterstone, glow overlays) show both layers; the sheet shows one. Stairs are not drawn (they reuse a listed texture); slabs are drawn only where their texture differs from the full block.

**Columns.** *Roles* use the D25 taxonomy (`wall-main`, `wall-accent`, `trim-pillar`, `floor`, `ceiling`, `roof`, `glass`, `pane`, `street`, `sidewalk`, `stairs-slab`, `rubble-ruin`, `light`, `fence-rail`, `door-trap`, `furniture-deco`, `foliage`, `special`, `off-theme`). *Fits upstream?* asks whether the block belongs in upstream's urban 20th-century city (brick, stone brick, concrete, quartz, glass, iron, wood trim, ruined and overgrown variants): *yes* means it could go into today's palettes, *partly* means it suits a themed building or district, *no* means off-theme. *Styles* are upstream's `standard`, `border` and `desert`, plus styles these blocks suggest: `industrial`, `modern`, `ancient`, `ruin`, `overgrown`, `rural`, `asian`, `arctic`, `gothic`, `luxury`, `tropical`, `scifi`. *Notes* include texture behavior: *connected texture* (joins neighbors of the same block and meta), *connects vertically* or *horizontally* (pillars, consoles, bookshelves), *pattern spans 2x2 or 3x3 blocks*, *random texture per block*.

## Summary

| Count | Value |
|---|---|
| Chisel features (config entries) | 141; 139 enabled. The two disabled ones (`aeCertusQuartz`, `aeSkyStone`) register no Chisel block |
| Features that register decorative variants | 119. The other 20 enabled ones register items, the auto chisel machine, empty blocks, or carving groups of other mods' blocks only (vanilla quartz, Railcraft bricks, Twilight Forest stones) |
| Chisel block ids | 290: 242 variant blocks, 41 stair blocks, 4 top-slab blocks, the auto chisel, and two blocks registered with no variants (`chisel:amber`, `chisel:bloodBrick`; their carving groups hold Thaumcraft and Blood Magic blocks) |
| Placeable variants (block + meta), enabled / total | **2,489 / 2,489**: 2,349 full-block variants (including 10 torches and 32 pumpkins and jack o'lanterns, one variant each), 59 slab variants, 81 stair variants |
| Fits upstream: yes / partly / no | 873 / 863 / 753 |
| Texture behavior (non-stair variants) | 367 connected, 37 connect vertically, 13 connect horizontally, 17 other connected (carpets, a slab), 56 span 2x2 or 3x3 blocks, 7 random per block |

Variants per role (a variant with two roles counts in both; slabs also count as `stairs-slab`):

| Role | Variants | | Role | Variants |
|---|---|---|---|---|
| wall-main | 562 | | glass | 97 |
| wall-accent | 542 | | trim-pillar | 87 |
| off-theme | 344 | | ceiling | 69 |
| floor | 306 | | foliage | 67 |
| rubble-ruin | 201 | | street | 45 |
| stairs-slab | 140 | | light | 41 |
| special | 130 | | fence-rail | 27 |
| pane | 115 | | sidewalk | 26 |
| furniture-deco | 101 | | roof | 4 |
| | | | door-trap | 0 |

Chisel has no doors, trapdoors, walls or fences, and almost no roof blocks: its stairs and slabs exist only for marble, marble pillars (slabs), limestone, concrete, aluminum and ice. Roofs and stair runs keep coming from vanilla, Et Futurum and the other catalogs.

## Behavior to know before placing Chisel blocks

1. **No mob spawns on Chisel blocks.** The pack's `chisel.cfg` sets `canMobsSpawnOnTheChiselBlocks=false`, and every `BlockCarvable` returns that from `canCreatureSpawn` ([BlockCarvable.java#L173-L176][carvable]); anti blocks, GloTek and Neonite refuse spawns unconditionally. A building floored with Chisel blocks gets no natural spawns on those floors. Upstream's dark ruins do spawn mobs, so D28 rule 2 uses Chisel freely in walls, trims and roofs and keeps it to a minority share of floors ([index](index.md#d28-as-applied-here)). Glass, panes, ice, packed ice, carpets, torches and pumpkins are not `BlockCarvable` and follow vanilla rules.
2. **Concrete and road blocks speed players up.** `chisel:concrete`, `chisel:road` and `chisel:road2` are `BlockConcrete`, which multiplies a walking player's horizontal motion by `concreteVelocityMult + 0.05` = 1.4 every tick, before ground friction ([BlockConcrete.java][concrete]). By our arithmetic that nearly doubles ground speed (steady state about 1.9 times vanilla); it is client-side and applies only to players. Streets and sidewalks made of these blocks change how a city plays, so D28 rule 3 walks players over them only on highways and primary roads; elsewhere they may stand in walls and trims, never in floors, roofs, sidewalks, stairs or rubble. The block is a full cube in this version: `fullBlockConcrete` is read from the config but used nowhere, so [block-mapping.md](../../00-pack-integration/block-mapping.md)'s note that it is "slightly lower than a full block" is wrong for 2.17.34-GTNH.
3. **Light.** Light 15: glowstone variants, lanterns, froglights, jack o'lanterns, lavastone. Light 14: torches. Light 7: Futura circuits, hex plating. Light 3: holystone. The Chisel beacon sets `setLightLevel(5.0F)`, a light value of 75, outside the 0 to 15 range; avoid it. Anti blocks, GloTek and Neonite render full-bright but emit no light. Laboratory "panels" and glowing vents look lit and are not.
4. **Gravity, tile entities, power.** Hempcrete powder falls like sand. The beacon, the present (a chest with an inventory) and the auto chisel have tile entities. The Chisel redstone block emits redstone power 15.
5. **Ice.** `chisel:ice` and `chisel:ice_pillar` behave like vanilla ice: translucent, slippery, melt near block light above 11 and leave water when broken. Packed ice variants and both ice stair sets are slippery and do not melt.
6. **Shapes and transparency.** Glass is transparent (stained glass translucent); `glass_pane`, stained panes, `iron_bars` and `paperwall` are pane-shaped; `carpet` and `moss_carpet` are 1/16 high; `road_line` is a 1/256-high painted overlay that drops as an item if the block under it is not solid; `technical2` is a see-through glass-type block; leaves are non-opaque but never decay. Scaffold, wireframe and crate looks elsewhere are painted on solid blocks.
7. **GT ore and boulder hosts: none.** Every Chisel block has its own block id, and GT matches hosts by block ([ore-veins.md](../../00-pack-integration/ore-veins.md)). None is an ore host or a boulder host, and Chisel's own stone generation is off in the pack (`worldgen` amounts are all 0). This makes Chisel the host-free source for the looks of cobblestone, netherrack, end stone and patterned colored terracotta. Chisel's carving groups also list vanilla blocks (`minecraft:stone`, `cobblestone`, `netherrack`, `end_stone`, `stained_hardened_clay`); those stay vanilla ids and stay hosts.
8. **Metadata that is not a variant.** Torches, pumpkins and jack o'lanterns are one block per design with the facing in the metadata. Snakestones use metadata for their shape; only metas 1 (head) and 13 (body) come out of the chisel. Stairs use the low three bits for facing and half, bit 8 for the second variant.

## Groups

Each table lists every variant of one Chisel feature. Families with the same patterns across many colors or woods (stained terracotta, stained glass, planks) get a pattern table, a color or wood table, and the full list in a collapsed block.

### Marble

Sheets: `marble.png`.

#### `MARBLE` feature: `chisel:marble`, `chisel:marble_slab`, `chisel:marble_stairs`

Stairs: each `_stairs.N` block holds two variants, item meta 0 for variant 2N and 8 for variant 2N+1; in the world the low three bits are the stair facing and half, as for vanilla stairs. Slabs: the listed block is the bottom slab; the top slab is the separate block `<name>_top` with the same meta.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:marble:0` | Raw Marble | wall-main, floor | yes | standard, border, luxury | plain white stone; reads like smooth quartz |
| `chisel:marble:1` | Marble Brick | wall-main | yes | standard, border, luxury |  |
| `chisel:marble:2` | Classic Marble Panel | wall-accent | yes | standard, border, luxury |  |
| `chisel:marble:3` | Ornate Marble Panel | wall-accent | partly | luxury, ancient | gold inlay |
| `chisel:marble:4` | Marble Panel | wall-accent, ceiling | yes | standard, border, luxury | connected texture |
| `chisel:marble:5` | Marble Block | wall-main | yes | standard, border, luxury | connected texture |
| `chisel:marble:6` | Dark Creeper Marble | off-theme | no | - | creeper face motif |
| `chisel:marble:7` | Light Creeper Marble | off-theme | no | - | creeper face motif |
| `chisel:marble:8` | Carved Marble | wall-accent | partly | ancient, luxury |  |
| `chisel:marble:9` | Radial Carved Marble | wall-accent | partly | ancient, luxury |  |
| `chisel:marble:10` | Marble with Dent | wall-accent | yes | standard, border, luxury | connected texture |
| `chisel:marble:11` | Marble with Large Dent | wall-accent | yes | standard, border, luxury | connected texture |
| `chisel:marble:12` | Marble Tiles | floor, wall-main | yes | standard, border, luxury |  |
| `chisel:marble:13` | Arranged Marble Tiles | floor | yes | standard, border, luxury | connected texture |
| `chisel:marble:14` | Fancy Marble Tiles | floor, wall-accent | yes | standard, border, luxury |  |
| `chisel:marble:15` | Marble Blocks | wall-main, floor | yes | standard, border, luxury |  |
| `chisel:marble_slab:0` | Raw Marble Slab (slab) | wall-main, floor, stairs-slab | yes | standard, border, luxury | plain white stone; reads like smooth quartz |
| `chisel:marble_slab:1` | Marble Brick Slab (slab) | wall-main, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_slab:2` | Classic Marble Panel Slab (slab) | wall-accent, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_slab:3` | Ornate Marble Panel Slab (slab) | wall-accent, stairs-slab | partly | luxury, ancient | gold inlay |
| `chisel:marble_slab:4` | Marble Panel Slab (slab) | wall-accent, ceiling, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_slab:5` | Marble Block Slab (slab) | wall-main, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_slab:6` | Dark Creeper Marble Slab (slab) | off-theme, stairs-slab | no | - | creeper face motif |
| `chisel:marble_slab:7` | Light Creeper Marble Slab (slab) | off-theme, stairs-slab | no | - | creeper face motif |
| `chisel:marble_slab:8` | Carved Marble Slab (slab) | wall-accent, stairs-slab | partly | ancient, luxury |  |
| `chisel:marble_slab:9` | Radial Carved Marble Slab (slab) | wall-accent, stairs-slab | partly | ancient, luxury |  |
| `chisel:marble_slab:10` | Marble Slab with Dent (slab) | wall-accent, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_slab:11` | Marble Slab with Large Dent (slab) | wall-accent, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_slab:12` | Marble Tiled Slab (slab) | floor, wall-main, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_slab:13` | Arranged Marble Tiled Slab (slab) | floor, stairs-slab | yes | standard, border, luxury | connected texture |
| `chisel:marble_slab:14` | Fancy Marble Tiled Slab (slab) | floor, wall-accent, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_slab:15` | Marble Blocks Slab (slab) | wall-main, floor, stairs-slab | yes | standard, border, luxury |  |
| `chisel:marble_stairs.0:0` | Raw Marble Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:0` (Raw Marble) |
| `chisel:marble_stairs.0:8` | Dark Creeper Marble Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:1` (Marble Brick) |
| `chisel:marble_stairs.1:0` | Classic Marble Panel Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:2` (Classic Marble Panel) |
| `chisel:marble_stairs.1:8` | Ornate Marble Panel Stairs | stairs-slab | partly | luxury, ancient | stairs of `chisel:marble:3` (Ornate Marble Panel) |
| `chisel:marble_stairs.2:0` | Marble Panel Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:4` (Marble Panel) |
| `chisel:marble_stairs.2:8` | Marble Block Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:5` (Marble Block) |
| `chisel:marble_stairs.3:0` | Dark Creeper Marble Stairs | stairs-slab | no | - | stairs of `chisel:marble:6` (Dark Creeper Marble) |
| `chisel:marble_stairs.3:8` | Light Creeper Marble Stairs | stairs-slab | no | - | stairs of `chisel:marble:7` (Light Creeper Marble) |
| `chisel:marble_stairs.4:0` | Carved Marble Stairs | stairs-slab | partly | ancient, luxury | stairs of `chisel:marble:8` (Carved Marble) |
| `chisel:marble_stairs.4:8` | Radial Carved Marble Stairs | stairs-slab | partly | ancient, luxury | stairs of `chisel:marble:9` (Radial Carved Marble) |
| `chisel:marble_stairs.5:0` | Marble Stairs with Dent | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:10` (Marble with Dent) |
| `chisel:marble_stairs.5:8` | Marble Stairs with Large Dent | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:11` (Marble with Large Dent) |
| `chisel:marble_stairs.6:0` | Marble Tiled Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:12` (Marble Tiles) |
| `chisel:marble_stairs.6:8` | Arranged Marble Tiled Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:13` (Arranged Marble Tiles); connected texture |
| `chisel:marble_stairs.7:0` | Fancy Marble Tiled Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:14` (Fancy Marble Tiles) |
| `chisel:marble_stairs.7:8` | Marble Block Stairs | stairs-slab | yes | standard, border, luxury | stairs of `chisel:marble:15` (Marble Blocks) |

#### `MARBLE_PILLAR` feature: `chisel:marble_pillar`, `chisel:marble_pillar_slab`

Connected column texture (top, middle, bottom). Slabs: the listed block is the bottom slab; the top slab is the separate block `<name>_top` with the same meta.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:marble_pillar:0` | Marble Pillar | trim-pillar | yes | standard, border, ancient | plain fluted column; connects vertically; connects vertically |
| `chisel:marble_pillar:1` | Original Marble Pillar | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:2` | Simplistic Marble Pillar | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:3` | Convex Marble Pillar | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:4` | Rough Marble Pillar | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:5` | Greek Marble Pillar with Ornate Capstone | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:6` | Greek Marble Pillar | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:7` | Greek Marble Pillar with Plain Capstone | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:8` | Plain Marble Pillar with Ornate Capstone | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:9` | Plain Marble Pillar with Greek Capstone | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:10` | Plain Marble Pillar | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:11` | Wide Marble Pillar with Ornate Capstone | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:12` | Wide Marble Pillar with Greek Capstone | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:13` | Wide Marble Pillar with Plain Capstone | trim-pillar | yes | standard, border, ancient | connects vertically |
| `chisel:marble_pillar:14` | Carved Pillar Capstone | trim-pillar, wall-accent | yes | standard, border, ancient | capstone; no vertical connection |
| `chisel:marble_pillar:15` | Ornamental Pillar Capstone | trim-pillar, wall-accent | yes | standard, border, ancient | capstone; no vertical connection |
| `chisel:marble_pillar_slab:0` | Marble Pillar Slab (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient | plain fluted column; connects vertically |
| `chisel:marble_pillar_slab:1` | Original Marble Pillar Slab (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:2` | Simplistic Marble Pillar Slab (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:3` | Convex Marble Pillar Slab (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:4` | Rough Marble Pillar Slab (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:5` | Greek Marble Pillar Slab with Ornate Capstone (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:6` | Greek Marble Pillar Slab (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:7` | Greek Marble Pillar Slab with Plain Capstone (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:8` | Plain Marble Pillar Slab with Ornate Capstone (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:9` | Plain Marble Pillar Slab with Greek Capstone (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:10` | Plain Marble Pillar Slab (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:11` | Wide Marble Pillar Slab with Ornate Capstone (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:12` | Wide Marble Pillar Slab with Greek Capstone (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:13` | Wide Marble Pillar Slab with Plain Capstone (slab) | trim-pillar, stairs-slab | yes | standard, border, ancient |  |
| `chisel:marble_pillar_slab:14` | Carved Pillar Capstone Slab (slab) | trim-pillar, wall-accent, stairs-slab | yes | standard, border, ancient | capstone; no vertical connection |
| `chisel:marble_pillar_slab:15` | Ornamental Pillar Capstone Slab (slab) | trim-pillar, wall-accent, stairs-slab | yes | standard, border, ancient | capstone; no vertical connection |

### Limestone

Sheets: `limestone.png`.

#### `LIMESTONE` feature: `chisel:limestone`, `chisel:limestone_slab`, `chisel:limestone_stairs`

Pale greenish-beige stone. Stairs: each `_stairs.N` block holds two variants, item meta 0 for variant 2N and 8 for variant 2N+1; in the world the low three bits are the stair facing and half, as for vanilla stairs. Slabs: the listed block is the bottom slab; the top slab is the separate block `<name>_top` with the same meta.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:limestone:0` | Limestone | wall-main | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:1` | Small Limestone Tiles | floor, wall-main | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:2` | French Limestone Tiles | floor | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:3` | French Limestone Tiles | floor | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:4` | Creeper Limestone Tiles | off-theme | no | - | creeper face motif |
| `chisel:limestone:5` | Small Limestone Bricks | wall-main | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:6` | Damaged Limestone Tiles | rubble-ruin | yes | ruin | rust-stained broken tiles |
| `chisel:limestone:7` | Smooth Limestone | wall-main, sidewalk | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:8` | Limestone with Ornate Panel | wall-accent | partly | ancient | pale greenish-beige stone |
| `chisel:limestone:9` | Limestone with Engraved Ornate Panel | wall-accent | partly | ancient | pale greenish-beige stone |
| `chisel:limestone:10` | Limestone with Creeper Panel | off-theme | no | - | creeper face motif |
| `chisel:limestone:11` | Limestone with Dent | wall-accent | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:12` | Limestone with Panel | wall-accent | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:13` | Large Limestone Tiles | floor, sidewalk | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone:14` | Limestone with Light Panel | wall-accent | partly | standard, desert | pale greenish-beige stone |
| `chisel:limestone:15` | Limestone with Dark Panel | wall-accent | partly | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:0` | Limestone Slab (slab) | wall-main, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:1` | Small Limestone Tiled Slab (slab) | floor, wall-main, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:2` | French Limestone Tiled Slab (slab) | floor, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:3` | French Limestone Tiled Slab (slab) | floor, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:4` | Creeper Limestone Tiled Slab (slab) | off-theme, stairs-slab | no | - | creeper face motif |
| `chisel:limestone_slab:5` | Small Limestone Bricks Slab (slab) | wall-main, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:6` | Damaged Limestone Tiled Slab (slab) | rubble-ruin, stairs-slab | yes | ruin | rust-stained broken tiles |
| `chisel:limestone_slab:7` | Smooth Limestone Slab (slab) | wall-main, sidewalk, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:8` | Limestone Slab with Ornate Panel (slab) | wall-accent, stairs-slab | partly | ancient | pale greenish-beige stone |
| `chisel:limestone_slab:9` | Limestone Slab with Ornate Panel (slab) | wall-accent, stairs-slab | partly | ancient | pale greenish-beige stone |
| `chisel:limestone_slab:10` | Limestone Slab with Creeper Panel (slab) | off-theme, stairs-slab | no | - | creeper face motif |
| `chisel:limestone_slab:11` | Limestone Slab with Dent (slab) | wall-accent, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:12` | Limestone Slab with Panel (slab) | wall-accent, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:13` | Large Limestone Tiles Slab (slab) | floor, sidewalk, stairs-slab | yes | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:14` | Limestone Slab with Light Panel (slab) | wall-accent, stairs-slab | partly | standard, desert | pale greenish-beige stone |
| `chisel:limestone_slab:15` | Limestone Slab with Dark Panel (slab) | wall-accent, stairs-slab | partly | standard, desert | pale greenish-beige stone |
| `chisel:limestone_stairs.0:0` | Limestone Stairs | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:0` (Limestone) |
| `chisel:limestone_stairs.0:8` | Small Limestone Tiled Stairs | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:1` (Small Limestone Tiles) |
| `chisel:limestone_stairs.1:0` | French Limestone Tiled Stairs | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:2` (French Limestone Tiles) |
| `chisel:limestone_stairs.1:8` | French Limestone Tiled Stairs | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:3` (French Limestone Tiles) |
| `chisel:limestone_stairs.2:0` | Creeper Limestone Tiled Stairs | stairs-slab | no | - | stairs of `chisel:limestone:4` (Creeper Limestone Tiles) |
| `chisel:limestone_stairs.2:8` | Small Limestone Bricks Stairs | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:5` (Small Limestone Bricks) |
| `chisel:limestone_stairs.3:0` | Damaged Limestone Tiled Stairs | stairs-slab | yes | ruin | stairs of `chisel:limestone:6` (Damaged Limestone Tiles) |
| `chisel:limestone_stairs.3:8` | Smooth Limestone Stairs | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:7` (Smooth Limestone) |
| `chisel:limestone_stairs.4:0` | Limestone Stairs with Ornate Panel | stairs-slab | partly | ancient | stairs of `chisel:limestone:8` (Limestone with Ornate Panel) |
| `chisel:limestone_stairs.4:8` | Limestone Stairs with Ornate Panel | stairs-slab | partly | ancient | stairs of `chisel:limestone:9` (Limestone with Engraved Ornate Panel) |
| `chisel:limestone_stairs.5:0` | Limestone Stairs with Creeper Panel | stairs-slab | no | - | stairs of `chisel:limestone:10` (Limestone with Creeper Panel) |
| `chisel:limestone_stairs.5:8` | Limestone Stairs with Dent | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:11` (Limestone with Dent) |
| `chisel:limestone_stairs.6:0` | Limestone Stairs with Panel | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:12` (Limestone with Panel) |
| `chisel:limestone_stairs.6:8` | Large Limestone Tiled Stairs | stairs-slab | yes | standard, desert | stairs of `chisel:limestone:13` (Large Limestone Tiles) |
| `chisel:limestone_stairs.7:0` | Limestone Stairs with Light Panel | stairs-slab | partly | standard, desert | stairs of `chisel:limestone:14` (Limestone with Light Panel) |
| `chisel:limestone_stairs.7:8` | Limestone Stairs with Dark Panel | stairs-slab | partly | standard, desert | stairs of `chisel:limestone:15` (Limestone with Dark Panel) |

### Andesite, diorite, granite

Sheets: `andesite-diorite-granite.png`.

#### `ANDESITE` feature: `chisel:andesite`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:andesite:0` | Raw Andesite | wall-main | partly | standard, rural | raw natural stone; not a GT ore host (separate block id) |
| `chisel:andesite:1` | Polished Andesite | wall-main, floor | yes | standard |  |
| `chisel:andesite:2` | Andesite Pillar | trim-pillar | yes | standard |  |
| `chisel:andesite:3` | Andesite Bricks | wall-main | yes | standard | connected texture |
| `chisel:andesite:4` | Ornate Andesite | wall-accent | yes | standard |  |
| `chisel:andesite:5` | Prismatic Andesite | wall-accent | partly | standard |  |
| `chisel:andesite:6` | Andesite Small Tiles | floor, sidewalk | yes | standard |  |
| `chisel:andesite:7` | Cobbled Andesite | wall-main, rubble-ruin | partly | ruin, rural |  |
| `chisel:andesite:8` | Mossy Andesite | rubble-ruin, foliage | yes | overgrown, ruin | mossy |
| `chisel:andesite:9` | Modern Andesite | wall-main | yes | standard |  |
| `chisel:andesite:10` | Modern Polished Andesite | wall-main, floor | yes | standard |  |
| `chisel:andesite:11` | Weathered Andesite | wall-main, rubble-ruin | yes | standard, ruin | weathered |

#### `DIORITE` feature: `chisel:diorite`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:diorite:0` | Raw Diorite | wall-main | partly | standard, rural | raw natural stone; not a GT ore host (separate block id) |
| `chisel:diorite:1` | Polished Diorite | wall-main, floor | yes | standard |  |
| `chisel:diorite:2` | Diorite Pillar | trim-pillar | yes | standard |  |
| `chisel:diorite:3` | Diorite Bricks | wall-main | yes | standard | connected texture |
| `chisel:diorite:4` | Ornate Diorite | wall-accent | yes | standard |  |
| `chisel:diorite:5` | Prismatic Diorite | wall-accent | partly | standard |  |
| `chisel:diorite:6` | Diorite Small Tiles | floor, sidewalk | yes | standard |  |
| `chisel:diorite:7` | Cobbled Diorite | wall-main, rubble-ruin | partly | ruin, rural |  |
| `chisel:diorite:8` | Mossy Diorite | rubble-ruin, foliage | yes | overgrown, ruin | mossy |
| `chisel:diorite:9` | Modern Diorite | wall-main | yes | standard |  |
| `chisel:diorite:10` | Modern Polished Diorite | wall-main, floor | yes | standard, border | polished diorite; the D19 fallback for upstream polished_diorite |
| `chisel:diorite:11` | Biotite-Rich Diorite | wall-accent | partly | standard, gothic | near-black speckled |
| `chisel:diorite:12` | Hornblende-Rich Diorite | wall-main | partly | standard, desert | pinkish-white speckled |

#### `GRANITE` feature: `chisel:granite`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:granite:0` | Raw Granite | wall-main | partly | standard, rural | raw natural stone; not a GT ore host (separate block id) |
| `chisel:granite:1` | Polished Granite | wall-main, floor | yes | standard, desert |  |
| `chisel:granite:2` | Granite Pillar | trim-pillar | yes | standard, desert |  |
| `chisel:granite:3` | Granite Bricks | wall-main | yes | standard, desert | connected texture |
| `chisel:granite:4` | Ornate Granite | wall-accent | yes | standard, desert |  |
| `chisel:granite:5` | Prismatic Granite | wall-accent | partly | standard, desert |  |
| `chisel:granite:6` | Granite Small Tiles | floor, sidewalk | yes | standard, desert |  |
| `chisel:granite:7` | Cobbled Granite | wall-main, rubble-ruin | partly | ruin, rural |  |
| `chisel:granite:8` | Mossy Granite | rubble-ruin, foliage | yes | overgrown, ruin | mossy |
| `chisel:granite:9` | Modern Granite | wall-main | yes | standard, desert |  |
| `chisel:granite:10` | Modern Polished Granite | wall-main, floor | yes | standard, desert |  |
| `chisel:granite:11` | Granitic Mineral Sand | wall-main | partly | desert | plain orange; named mineral sand but a normal solid block |

### Stone bricks, cobblestone, snakestone

Sheets: `stonebrick-cobblestone.png`.

#### `STONE_BRICK` feature: `chisel:stonebricksmooth`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:stonebricksmooth:0` | Modern Neutral Masonry | wall-main | partly | standard, industrial | dark masonry with tinted stones; pattern spans 2x2 blocks |
| `chisel:stonebricksmooth:1` | Modern Felsic Masonry | wall-main | partly | standard, industrial | dark masonry, gray-blue stones; pattern spans 2x2 blocks |
| `chisel:stonebricksmooth:2` | Modern Mafic Masonry | wall-main | partly | standard, industrial | dark masonry, red-brown stones; pattern spans 2x2 blocks |
| `chisel:stonebricksmooth:3` | Modern Mixed Masonry | wall-main | partly | standard, industrial | dark masonry, mixed stones; pattern spans 2x2 blocks |
| `chisel:stonebricksmooth:4` | Small Stone Bricks | wall-main | yes | standard, border | close to vanilla stone bricks: strong bricks_standard variety |
| `chisel:stonebricksmooth:5` | Wide Stone Bricks | wall-main | yes | standard, border | close to vanilla stone bricks |
| `chisel:stonebricksmooth:6` | Small Disordered Stone Bricks | wall-main, rubble-ruin | yes | standard, border, ruin |  |
| `chisel:stonebricksmooth:7` | Disordered Stone Bricks | wall-main, rubble-ruin | yes | standard, border, ruin |  |
| `chisel:stonebricksmooth:8` | Disordered Stone Panels | wall-main | yes | standard, border |  |
| `chisel:stonebricksmooth:9` | Stone Bricks in a Fancy Arrangement | wall-accent | yes | standard, border |  |
| `chisel:stonebricksmooth:10` | Ornate Stone Brick tiles | wall-accent, floor | yes | standard, border |  |
| `chisel:stonebricksmooth:11` | Large Ornate Stone Brick Tiles | wall-accent, floor | yes | standard, border |  |
| `chisel:stonebricksmooth:12` | Stone Panel | wall-main, floor, street | yes | standard, border | flat gray panel: closest Chisel match to smooth stone double slab (S, u, $); connected texture |
| `chisel:stonebricksmooth:13` | Sunken Stone Panel | wall-accent, floor | yes | standard, border | connected texture |
| `chisel:stonebricksmooth:14` | Ornate Stone Panel | wall-accent | yes | standard, border |  |
| `chisel:stonebricksmooth:15` | Poison Stone Brick | wall-accent | partly | ancient |  |

#### `COBBLESTONE` feature: `chisel:cobblestone`

Not a GT boulder host, unlike minecraft:cobblestone.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:cobblestone:1` | Aligned Cobblestone Bricks | wall-main | partly | standard, rural, ruin | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:2` | Detailed Cobblestone Bricks | wall-main | partly | standard, rural, ruin | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:3` | Small Cobblestone Bricks | wall-main | partly | standard, rural, ruin | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:4` | Large Cobblestone Tiles | floor, street | yes | standard | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:5` | Small Cobblestone Tiles | floor, street | yes | standard | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:6` | French Cobblestone Tiles | floor, sidewalk | yes | standard | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:7` | French Cobblestone Tiles | floor, sidewalk | yes | standard | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:8` | Creeper Cobblestone Tiles | off-theme | no | - | creeper face motif |
| `chisel:cobblestone:9` | Damaged Cobblestone Tiles | rubble-ruin | yes | ruin | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:10` | Huge Cobblestone Tiles | street, sidewalk | yes | standard | not a GT boulder host, unlike minecraft:cobblestone; connected texture |
| `chisel:cobblestone:11` | Cobblestone with Creeper Panel | off-theme | no | - | creeper face motif |
| `chisel:cobblestone:12` | Cobblestone with Dent | wall-accent | partly | standard, rural, ruin | not a GT boulder host, unlike minecraft:cobblestone; connected texture |
| `chisel:cobblestone:13` | Cobblestone with Panel | wall-accent | partly | standard, rural, ruin | not a GT boulder host, unlike minecraft:cobblestone; connected texture |
| `chisel:cobblestone:14` | Cobblestone with Light Panel | wall-accent | partly | standard, rural, ruin | not a GT boulder host, unlike minecraft:cobblestone |
| `chisel:cobblestone:15` | Cobblestone with Dark Panel | wall-accent | partly | standard, rural, ruin | not a GT boulder host, unlike minecraft:cobblestone; connected texture |

#### `COBBLESTONE_MOSSY` feature: `chisel:mossy_cobblestone`

Moss-covered.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:mossy_cobblestone:1` | Aligned Mossy Cobblestone Bricks | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:2` | Detailed Mossy Cobblestone Bricks | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:3` | Small Mossy Cobblestone Bricks | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:4` | Large Mossy Cobblestone Tiles | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:5` | Small Mossy Cobblestone Tiles | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:6` | French Mossy Cobblestone Tiles | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:7` | French Mossy Cobblestone Tiles | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:8` | Creeper Mossy Cobblestone Tiles | off-theme | no | - | creeper face motif |
| `chisel:mossy_cobblestone:9` | Damaged Mossy Cobblestone Tiles | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:10` | Huge Mossy Cobblestone Tiles | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:11` | Mossy Cobblestone with Creeper Panel | off-theme | no | - | creeper face motif |
| `chisel:mossy_cobblestone:12` | Mossy Cobblestone with Dent | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:13` | Mossy Cobblestone with Pane | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:14` | Mossy Cobblestone with Light Panel | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:mossy_cobblestone:15` | Mossy Cobblestone with Dark Panel | rubble-ruin, foliage | yes | overgrown, ruin |  |

#### `SNAKESTONE` feature: `chisel:stone_snakestone`

Snake-shaped decoration: placing head then body draws a connected snake; metas 1 and 13 are the chisel results, other metas are shape states.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:stone_snakestone:1` | Snakestone head | special | no | ancient |  |
| `chisel:stone_snakestone:13` | Snakestone body | special | no | ancient |  |

### Sandstone

Sheets: `sandstone.png`.

#### `SANDSTONE` feature: `chisel:sandstone`, `chisel:sandstone2`, `chisel:sandstone_scribbles`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:sandstone:0` | Sandstone Glyphs | wall-accent | partly | desert, ancient | hieroglyph glyphs |
| `chisel:sandstone:1` | Sandcobble | rubble-ruin, wall-main | partly | desert, ruin |  |
| `chisel:sandstone:2` | Sandstone Creeper | off-theme | no | - | creeper face motif |
| `chisel:sandstone:3` | Faded Sandstone | wall-main | yes | desert |  |
| `chisel:sandstone:4` | Sandstone Pillar | trim-pillar | yes | desert |  |
| `chisel:sandstone:5` | Sandstone Pillar Capstone | trim-pillar | yes | desert |  |
| `chisel:sandstone:6` | Small Sandstone Pillar | trim-pillar | yes | desert |  |
| `chisel:sandstone:7` | Sandstone Pillar Base | trim-pillar | yes | desert |  |
| `chisel:sandstone:8` | Smooth & Flat Sandstone | wall-main, floor | yes | desert | flat smooth sandstone: cut_sandstone variety (bricks_desert X/#) |
| `chisel:sandstone:9` | Smooth Sandstone Pillar Capstone | trim-pillar | yes | desert |  |
| `chisel:sandstone:10` | Small Smooth Sandstone Pillar | trim-pillar | yes | desert |  |
| `chisel:sandstone:11` | Smooth Sandstone Pillar Base | trim-pillar | yes | desert |  |
| `chisel:sandstone:12` | Sandstone Block | wall-main | yes | desert |  |
| `chisel:sandstone:13` | Small Sandstone Blocks | wall-main, floor | yes | desert |  |
| `chisel:sandstone:14` | Sandstone Mosaic | floor, wall-accent | yes | desert | connected texture |
| `chisel:sandstone:15` | Stacked Sandstone Tiles | wall-main | yes | desert |  |
| `chisel:sandstone2:0` | Sandstone Bricks | wall-main | yes | desert |  |
| `chisel:sandstone2:1` | Sandstone | wall-main | yes | desert |  |
| `chisel:sandstone_scribbles:0` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:1` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:2` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:3` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:4` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:5` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:6` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:7` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:8` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:9` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:10` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:11` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:12` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:13` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:14` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |
| `chisel:sandstone_scribbles:15` | Sandstone Scribbles | wall-accent | partly | desert, ancient |  |

#### `SNAKE_SANDSTONE` feature: `chisel:sand_snakestone`

Snake-shaped decoration: placing head then body draws a connected snake; metas 1 and 13 are the chisel results, other metas are shape states.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:sand_snakestone:1` | Snakestone head | special | no | desert, ancient |  |
| `chisel:sand_snakestone:13` | Snakestone body | special | no | desert, ancient |  |

### Concrete, road, hempcrete

Sheets: `concrete-road.png`.

#### `CONCRETE` feature: `chisel:concrete`, `chisel:concrete_slab`, `chisel:concrete_stairs`

Behavior: Speed: players move nearly twice as fast on `concrete` (and on `road`). Full block in this version (the `fullBlockConcrete` setting is read but unused). Stairs: each `_stairs.N` block holds two variants, item meta 0 for variant 2N and 8 for variant 2N+1; in the world the low three bits are the stair facing and half, as for vanilla stairs. Slabs: the listed block is the bottom slab; the top slab is the separate block `<name>_top` with the same meta.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:concrete:0` | Concrete | wall-main, sidewalk, street | yes | standard, border, industrial |  |
| `chisel:concrete:1` | Concrete Block | wall-main, sidewalk | yes | standard, border, industrial | connected texture |
| `chisel:concrete:2` | Concrete Double Slab | wall-main, sidewalk | yes | standard, border, industrial |  |
| `chisel:concrete:3` | Small Concrete Blocks | wall-main, sidewalk | yes | standard, border, industrial |  |
| `chisel:concrete:4` | Weathered Concrete | wall-main, rubble-ruin | yes | standard, ruin |  |
| `chisel:concrete:5` | Weathered Concrete Block | wall-main, rubble-ruin | yes | standard, ruin |  |
| `chisel:concrete:6` | Weathered Concrete Double Slab | wall-main, rubble-ruin | yes | standard, ruin |  |
| `chisel:concrete:7` | Small Weathered Blocks | wall-main, rubble-ruin | yes | standard, ruin |  |
| `chisel:concrete:8` | Partly Weathered Concrete | wall-main, rubble-ruin | yes | standard, ruin |  |
| `chisel:concrete:9` | Partly Weathered Concrete Block | wall-main, rubble-ruin | yes | standard, ruin |  |
| `chisel:concrete:10` | Asphalt | street | yes | standard, border, industrial | plain dark asphalt |
| `chisel:concrete_slab:0` | Concrete Slab (slab) | wall-main, sidewalk, street, stairs-slab | yes | standard, border, industrial |  |
| `chisel:concrete_slab:1` | Concrete Block Slab (slab) | wall-main, sidewalk, stairs-slab | yes | standard, border, industrial | connected texture |
| `chisel:concrete_slab:2` | Concrete Double Slab (slab) | wall-main, sidewalk, stairs-slab | yes | standard, border, industrial |  |
| `chisel:concrete_slab:3` | Small Concrete Block Slab (slab) | wall-main, sidewalk, stairs-slab | yes | standard, border, industrial |  |
| `chisel:concrete_slab:4` | Weathered Concrete Slab (slab) | wall-main, rubble-ruin, stairs-slab | yes | standard, ruin |  |
| `chisel:concrete_slab:5` | Weathered Concrete Block Slab (slab) | wall-main, rubble-ruin, stairs-slab | yes | standard, ruin |  |
| `chisel:concrete_slab:6` | Weathered Concrete Double Slab (slab) | wall-main, rubble-ruin, stairs-slab | yes | standard, ruin |  |
| `chisel:concrete_slab:7` | Small Weathered Block Slab (slab) | wall-main, rubble-ruin, stairs-slab | yes | standard, ruin |  |
| `chisel:concrete_slab:8` | Partly Weathered Concrete Slab (slab) | wall-main, rubble-ruin, stairs-slab | yes | standard, ruin |  |
| `chisel:concrete_slab:9` | Partly Weathered Concrete Block Slab (slab) | wall-main, rubble-ruin, stairs-slab | yes | standard, ruin |  |
| `chisel:concrete_slab:10` | Asphalt (slab) | street, stairs-slab | yes | standard, border, industrial | plain dark asphalt |
| `chisel:concrete_stairs.0:0` | Concrete Stairs | stairs-slab | yes | standard, border, industrial | stairs of `chisel:concrete:0` (Concrete) |
| `chisel:concrete_stairs.0:8` | Concrete Block Stairs | stairs-slab | yes | standard, border, industrial | stairs of `chisel:concrete:1` (Concrete Block); connected texture |
| `chisel:concrete_stairs.1:0` | Concrete Double Slab Stairs | stairs-slab | yes | standard, border, industrial | stairs of `chisel:concrete:2` (Concrete Double Slab) |
| `chisel:concrete_stairs.1:8` | Small Concrete Block Stairs | stairs-slab | yes | standard, border, industrial | stairs of `chisel:concrete:3` (Small Concrete Blocks) |
| `chisel:concrete_stairs.2:0` | Weathered Concrete Stairs | stairs-slab | yes | standard, ruin | stairs of `chisel:concrete:4` (Weathered Concrete) |
| `chisel:concrete_stairs.2:8` | Weathered Concrete Block Stairs | stairs-slab | yes | standard, ruin | stairs of `chisel:concrete:5` (Weathered Concrete Block) |
| `chisel:concrete_stairs.3:0` | Weathered Concrete Double Slab Stairs | stairs-slab | yes | standard, ruin | stairs of `chisel:concrete:6` (Weathered Concrete Double Slab) |
| `chisel:concrete_stairs.3:8` | Small Weathered Block Stairs | stairs-slab | yes | standard, ruin | stairs of `chisel:concrete:7` (Small Weathered Blocks) |
| `chisel:concrete_stairs.4:0` | Partly Weathered Concrete Stairs | stairs-slab | yes | standard, ruin | stairs of `chisel:concrete:8` (Partly Weathered Concrete) |
| `chisel:concrete_stairs.4:8` | Partly Weathered Concrete Block Stairs | stairs-slab | yes | standard, ruin | stairs of `chisel:concrete:9` (Partly Weathered Concrete Block) |
| `chisel:concrete_stairs.5:0` | Asphalt Stairs | stairs-slab | yes | standard, border, industrial | stairs of `chisel:concrete:10` (Asphalt) |

#### `ROAD` feature: `chisel:road`, `chisel:road2`

Direction-named markings (east, north, ...) are fixed to world axes, so a street running the other way needs the matching variant. Behavior: Speed: players move nearly twice as fast on both road blocks (they are `BlockConcrete`).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:road:0` | Asphalt | street | yes | standard, border | plain asphalt |
| `chisel:road:1` | Crosswalk | street | yes | standard, border | zebra crossing; connected texture |
| `chisel:road:2` | Double White Lines | street | yes | standard, border | connected texture |
| `chisel:road:3` | Double Yellow Lines | street | yes | standard, border | connected texture |
| `chisel:road:4` | Dirt Road | street | partly | rural | direction-named markings (east, north, ...) are fixed to world axes, so a street running the other way needs the matching variant; connected texture |
| `chisel:road:5` | Diverging Lane Marking | street | yes | standard, border | connected texture |
| `chisel:road:6` | Dotted White Line | street | yes | standard, border | connected texture |
| `chisel:road:7` | Dotted Yellow Line | street | yes | standard, border | connected texture |
| `chisel:road:8` | Single Yellow Line - East | street | yes | standard, border |  |
| `chisel:road:9` | Single Yellow Line - North | street | yes | standard, border |  |
| `chisel:road:10` | Single Yellow Line - South | street | yes | standard, border |  |
| `chisel:road:11` | Single Yellow Line - West | street | yes | standard, border |  |
| `chisel:road:12` | Left Lane | street | yes | standard, border | connected texture |
| `chisel:road:13` | Right Lane | street | yes | standard, border | connected texture |
| `chisel:road:14` | Thick White Line | street | yes | standard, border | connected texture |
| `chisel:road:15` | White Outlined Road | street | yes | standard, border | white outline box (parking bay look); connected texture |
| `chisel:road2:0` | Passing Lane - East | street | yes | standard, border |  |
| `chisel:road2:1` | Passing Lane - North | street | yes | standard, border |  |
| `chisel:road2:2` | Passing Lane - South | street | yes | standard, border |  |
| `chisel:road2:3` | Passing Lane - West | street | yes | standard, border |  |
| `chisel:road2:4` | Double Yellow Lines With Reflectors | street | yes | standard, border | double yellow with reflectors; connected texture |
| `chisel:road2:5` | Solid Yellow Line - East/West | street | yes | standard, border |  |
| `chisel:road2:6` | Solid Yellow Line - North/South | street | yes | standard, border |  |
| `chisel:road2:7` | Converging Lane Marking | street | yes | standard, border | connected texture |
| `chisel:road2:8` | White Painted Asphalt | street, sidewalk | partly | standard, border | solid white paint |
| `chisel:road2:9` | Yellow Painted Asphalt | street | partly | standard, border | solid yellow paint |
| `chisel:road2:10` | Gravel Road | street | partly | rural | direction-named markings (east, north, ...) are fixed to world axes, so a street running the other way needs the matching variant; connected texture |
| `chisel:road2:11` | Solid White Line - North/South | street | yes | standard, border |  |
| `chisel:road2:12` | Solid White Line - East/West | street | yes | standard, border |  |
| `chisel:road2:13` | Weathered Asphalt | street, rubble-ruin | yes | standard, ruin | direction-named markings (east, north, ...) are fixed to world axes, so a street running the other way needs the matching variant |
| `chisel:road2:14` | Weathering Asphalt | street, rubble-ruin | yes | standard, ruin | direction-named markings (east, north, ...) are fixed to world axes, so a street running the other way needs the matching variant; connected texture |
| `chisel:road2:15` | Overgrown Dirt Road | street, foliage, rubble-ruin | yes | overgrown, ruin | grass-overgrown dirt road; connected texture |

#### `ROAD_LINE` feature: `chisel:road_line`

Thin painted line laid on top of a street block (1/256 high, no collision); joins neighbors automatically; drops as an item if the block below is not solid.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:road_line:0` | White | street | yes | standard, border, industrial |  |
| `chisel:road_line:1` | Yellow | street | yes | standard, border, industrial |  |
| `chisel:road_line:2` | Double White | street | yes | standard, border, industrial |  |
| `chisel:road_line:3` | Double Yellow | street | yes | standard, border, industrial |  |
| `chisel:road_line:4` | Hazard Tape | street | yes | standard, border, industrial | yellow-black hazard tape; industrial yards |

#### `HEMPCRETE` feature: `chisel:hempcrete`

Plain dyed concrete look; a second source of 1.12-style colored concrete.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:hempcrete:0` | White | wall-main, wall-accent | yes | standard, border, modern | plain dyed concrete look; a second source of 1.12-style colored concrete |
| `chisel:hempcrete:1` | Orange | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:2` | Magenta | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:3` | Light Blue | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:4` | Yellow | wall-main, wall-accent | yes | standard, border, modern | upstream yellow_concrete (oil rig, parking) |
| `chisel:hempcrete:5` | Light Green | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:6` | Pink | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:7` | Gray | wall-main, wall-accent | yes | standard, border, modern | upstream gray_concrete |
| `chisel:hempcrete:8` | Light Gray | wall-main, wall-accent | yes | standard, border, modern | plain dyed concrete look; a second source of 1.12-style colored concrete |
| `chisel:hempcrete:9` | Cyan | wall-main, wall-accent | yes | standard, border, modern | upstream cyan_concrete (city-sphere border 9) |
| `chisel:hempcrete:10` | Purple | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:11` | Blue | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:12` | Brown | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:13` | Green | wall-main, wall-accent | partly | standard, border, modern |  |
| `chisel:hempcrete:14` | Red | wall-main, wall-accent | yes | standard, border, modern | upstream red_concrete (gas station, restaurant) |
| `chisel:hempcrete:15` | Black | wall-main, wall-accent | yes | standard, border, modern | upstream black_concrete |

#### `HEMPCRETE_SAND` feature: `chisel:hempcretesand`

Falls like sand (gravity); concrete-powder look; use only on a solid floor.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:hempcretesand:0` | White | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:1` | Orange | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:2` | Magenta | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:3` | Light Blue | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:4` | Yellow | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:5` | Light Green | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:6` | Pink | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:7` | Gray | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:8` | Light Gray | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:9` | Cyan | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:10` | Purple | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:11` | Blue | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:12` | Brown | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:13` | Green | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:14` | Red | rubble-ruin | partly | ruin |  |
| `chisel:hempcretesand:15` | Black | rubble-ruin | partly | ruin |  |

### Brick, terracotta, woolen clay

Sheets: `brick-clay.png`.

#### `BRICK_CUSTOM` feature: `chisel:brickCustom`, `chisel:brickCustom2`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:brickCustom:1` | Large Bricks | wall-main | yes | standard, border |  |
| `chisel:brickCustom:2` | Mortarless Bricks | wall-main | yes | standard, border |  |
| `chisel:brickCustom:3` | Varied Bricks | wall-main | yes | standard, border |  |
| `chisel:brickCustom:4` | Oops! All Mortar! | wall-main | partly | standard, border | mostly mortar |
| `chisel:brickCustom:5` | Aged Bricks | wall-main, rubble-ruin | yes | standard, ruin | aged and chipped; pattern spans 2x2 blocks |
| `chisel:brickCustom:6` | Yellow Bricks | wall-main | yes | standard, desert |  |
| `chisel:brickCustom:7` | Brick Tile | floor, wall-main | yes | standard, border |  |
| `chisel:brickCustom:8` | Brick Panel | wall-accent | yes | standard, border | connected texture |
| `chisel:brickCustom:9` | Brown Bricks | wall-main | yes | standard, border |  |
| `chisel:brickCustom:10` | Modern Bricks | wall-main | yes | standard, border |  |
| `chisel:brickCustom:11` | White Bricks | wall-main | yes | standard, border, modern |  |
| `chisel:brickCustom:12` | Black Bricks | wall-main, wall-accent | yes | standard, border, industrial |  |
| `chisel:brickCustom:13` | Sooty Bricks | wall-main, rubble-ruin | yes | standard, industrial, ruin | soot-stained |
| `chisel:brickCustom:14` | Mossy Bricks | rubble-ruin, foliage | yes | overgrown, ruin |  |
| `chisel:brickCustom:15` | Neutral Bricks | wall-main | yes | standard, border, industrial | gray bricks |
| `chisel:brickCustom2:0` | Left-Bordered Bricks | wall-accent | partly | standard, border |  |
| `chisel:brickCustom2:1` | Right-Bordered Bricks | wall-accent | partly | standard, border |  |

#### `HARDENED_CLAY` feature: `chisel:hardened_clay`, `chisel:hardened_clay2`

Natural terracotta color.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:hardened_clay:0` | Braid Terracotta Bricks | wall-accent | yes | standard, desert | braid |
| `chisel:hardened_clay:1` | Cracked Terracotta Bricks | wall-main, rubble-ruin | yes | standard, desert | cracked bricks |
| `chisel:hardened_clay:2` | Small Terracotta Brick Tiles | wall-main | yes | standard, desert | natural terracotta color; connected texture |
| `chisel:hardened_clay:3` | Small Terracotta Bricks | wall-main | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay:4` | Aligned Terracotta Bricks | wall-main | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay:5` | Detailed Terracotta Bricks | wall-main | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay:6` | Wide Terracotta Bricks | wall-main | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay:7` | Chaotic Small Terracotta Bricks | wall-main | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay:8` | Small Disordered Terracotta Bricks | wall-main | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay:9` | Ornate Terracotta | wall-accent | yes | standard, desert | natural terracotta color; connected texture |
| `chisel:hardened_clay:10` | Cracked Terracotta | rubble-ruin | yes | standard, desert | cracked |
| `chisel:hardened_clay:11` | Sunken Terracotta Panel | wall-accent | yes | standard, desert | natural terracotta color; connected texture |
| `chisel:hardened_clay:12` | Fancy Terracotta Bricks | wall-accent | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay:13` | Fancy Terracotta Tiles | floor | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay:14` | Terracotta Layers | wall-accent | yes | standard, desert | horizontal bands |
| `chisel:hardened_clay:15` | Terracotta Mosaic | floor | yes | standard, desert | natural terracotta color; connected texture |
| `chisel:hardened_clay2:0` | Small Ornate Terracotta | wall-accent | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay2:1` | Terracotta Large Tile | floor, wall-main | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay2:2` | Terracotta Pillar | trim-pillar | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay2:3` | Prismatic Terracotta | wall-accent | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay2:4` | Terracotta Bricks in a Fancy Arrangement | wall-accent | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay2:5` | Terracotta Panel | wall-main, ceiling | yes | standard, desert | plain panel; connected texture |
| `chisel:hardened_clay2:6` | Small Terracotta Blocks | wall-main | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay2:7` | Small Terracotta Tiles | floor | yes | standard, desert | natural terracotta color |
| `chisel:hardened_clay2:8` | Radial Carved Terracotta | wall-accent | yes | standard, desert | carved motif |
| `chisel:hardened_clay2:9` | Engraved Terracotta Block | wall-accent | yes | standard, desert | carved frame; connected texture |

#### `WOOLEN_CLAY` feature: `chisel:woolen_clay`

Plain matte dyed clay; flat facade color.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:woolen_clay:0` | White Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:1` | Orange Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:2` | Magenta Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:3` | Light Blue Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:4` | Yellow Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:5` | Lime Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:6` | Pink Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:7` | Gray Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:8` | Light Gray Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:9` | Cyan Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:10` | Purple Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:11` | Blue Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:12` | Brown Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:13` | Green Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:14` | Red Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |
| `chisel:woolen_clay:15` | Black Woolen Clay | wall-main, wall-accent | partly | modern, standard |  |

### Stained terracotta

Sheets: `terracotta-stained-1 to -4.png`.

#### `HARDENED_CLAY_STAINED` feature: `chisel:hardened_clay_<color>` and `chisel:hardened_clay_<color>2`

Sixteen colors, each with the same 26 patterns: 16 on `chisel:hardened_clay_<color>` (metas 0 to 15) and 10 on `chisel:hardened_clay_<color>2` (metas 0 to 9), 416 variants in all. Colors as registry suffixes: `white`, `orange`, `magenta`, `lightblue`, `yellow`, `lime`, `pink`, `gray`, `lightgray`, `cyan`, `purple`, `blue`, `brown`, `green`, `red`, `black`. These are separate block ids, so unlike `minecraft:stained_hardened_clay` none of them is a GT boulder host. Patterns only: there is no plain colored block here.

| Block, meta | Pattern (English name for white) | Roles | Notes |
|---|---|---|---|
| `<color>:0` | Braid White Terracotta Bricks | wall-accent | braid |
| `<color>:1` | Cracked White Terracotta Bricks | wall-main, rubble-ruin | cracked bricks |
| `<color>:2` | Small White Terracotta Brick Tiles | wall-main |  |
| `<color>:3` | Small White Terracotta Bricks | wall-main |  |
| `<color>:4` | Aligned White Terracotta Bricks | wall-main |  |
| `<color>:5` | Detailed White Terracotta Bricks | wall-main |  |
| `<color>:6` | Wide White Terracotta Bricks | wall-main |  |
| `<color>:7` | Chaotic Small White Terracotta Bricks | wall-main |  |
| `<color>:8` | Small Disordered White Terracotta Bricks | wall-main |  |
| `<color>:9` | Ornate White Terracotta | wall-accent |  |
| `<color>:10` | Cracked White Terracotta | rubble-ruin | cracked |
| `<color>:11` | Sunken White Terracotta Panel | wall-accent |  |
| `<color>:12` | Fancy White Terracotta Bricks | wall-accent |  |
| `<color>:13` | Fancy White Terracotta Tiles | floor |  |
| `<color>:14` | White Terracotta Layers | wall-accent | horizontal bands |
| `<color>:15` | White Terracotta Mosaic | floor |  |
| `<color>2:0` | Small Ornate White Terracotta | wall-accent |  |
| `<color>2:1` | White Terracotta Large Tile | floor, wall-main |  |
| `<color>2:2` | White Terracotta Pillar | trim-pillar |  |
| `<color>2:3` | Prismatic White Terracotta | wall-accent |  |
| `<color>2:4` | White Terracotta Bricks in a Fancy Arrangement | wall-accent |  |
| `<color>2:5` | White Terracotta Panel | wall-main, ceiling | plain panel |
| `<color>2:6` | Small White Terracotta Blocks | wall-main |  |
| `<color>2:7` | Small White Terracotta Tiles | floor |  |
| `<color>2:8` | Radial Carved White Terracotta | wall-accent | carved motif |
| `<color>2:9` | Engraved White Terracotta Block | wall-accent | carved frame |

| Color | Fits upstream? | Styles | Upstream colored terracotta it adds variety to |
|---|---|---|---|
| `white` | partly | standard, desert | - |
| `orange` | yes | desert | bricks_desert_orange |
| `magenta` | no | - | - |
| `lightblue` | partly | standard | - |
| `yellow` | partly | desert | - |
| `lime` | no | - | - |
| `pink` | no | - | - |
| `gray` | yes | standard, border | bricks_gray, border 9 |
| `lightgray` | yes | standard | bricks_silver |
| `cyan` | yes | standard | bricks_cyan, highway R, border 9 |
| `purple` | no | - | - |
| `blue` | partly | standard | - |
| `brown` | yes | standard, desert | bricks_silver $ |
| `green` | partly | standard, overgrown | - |
| `red` | yes | standard, desert | bricks_desert_red, variant bricks |
| `black` | yes | standard, industrial | bricks_cyan and bricks_gray $ |

<details><summary>Every stained terracotta variant (416 rows)</summary>

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:hardened_clay_white:0` | Braid White Terracotta Bricks | wall-accent | partly | standard, desert | braid |
| `chisel:hardened_clay_white:1` | Cracked White Terracotta Bricks | wall-main, rubble-ruin | partly | standard, desert | cracked bricks |
| `chisel:hardened_clay_white:2` | Small White Terracotta Brick Tiles | wall-main | partly | standard, desert | connected texture |
| `chisel:hardened_clay_white:3` | Small White Terracotta Bricks | wall-main | partly | standard, desert |  |
| `chisel:hardened_clay_white:4` | Aligned White Terracotta Bricks | wall-main | partly | standard, desert |  |
| `chisel:hardened_clay_white:5` | Detailed White Terracotta Bricks | wall-main | partly | standard, desert |  |
| `chisel:hardened_clay_white:6` | Wide White Terracotta Bricks | wall-main | partly | standard, desert |  |
| `chisel:hardened_clay_white:7` | Chaotic Small White Terracotta Bricks | wall-main | partly | standard, desert |  |
| `chisel:hardened_clay_white:8` | Small Disordered White Terracotta Bricks | wall-main | partly | standard, desert |  |
| `chisel:hardened_clay_white:9` | Ornate White Terracotta | wall-accent | partly | standard, desert | connected texture |
| `chisel:hardened_clay_white:10` | Cracked White Terracotta | rubble-ruin | partly | standard, desert | cracked |
| `chisel:hardened_clay_white:11` | Sunken White Terracotta Panel | wall-accent | partly | standard, desert | connected texture |
| `chisel:hardened_clay_white:12` | Fancy White Terracotta Bricks | wall-accent | partly | standard, desert |  |
| `chisel:hardened_clay_white:13` | Fancy White Terracotta Tiles | floor | partly | standard, desert |  |
| `chisel:hardened_clay_white:14` | White Terracotta Layers | wall-accent | partly | standard, desert | horizontal bands |
| `chisel:hardened_clay_white:15` | White Terracotta Mosaic | floor | partly | standard, desert | connected texture |
| `chisel:hardened_clay_white2:0` | Small Ornate White Terracotta | wall-accent | partly | standard, desert |  |
| `chisel:hardened_clay_white2:1` | White Terracotta Large Tile | floor, wall-main | partly | standard, desert |  |
| `chisel:hardened_clay_white2:2` | White Terracotta Pillar | trim-pillar | partly | standard, desert |  |
| `chisel:hardened_clay_white2:3` | Prismatic White Terracotta | wall-accent | partly | standard, desert |  |
| `chisel:hardened_clay_white2:4` | White Terracotta Bricks in a Fancy Arrangement | wall-accent | partly | standard, desert |  |
| `chisel:hardened_clay_white2:5` | White Terracotta Panel | wall-main, ceiling | partly | standard, desert | plain panel; connected texture |
| `chisel:hardened_clay_white2:6` | Small White Terracotta Blocks | wall-main | partly | standard, desert |  |
| `chisel:hardened_clay_white2:7` | Small White Terracotta Tiles | floor | partly | standard, desert |  |
| `chisel:hardened_clay_white2:8` | Radial Carved White Terracotta | wall-accent | partly | standard, desert | carved motif |
| `chisel:hardened_clay_white2:9` | Engraved White Terracotta Block | wall-accent | partly | standard, desert | carved frame; connected texture |
| `chisel:hardened_clay_orange:0` | Braid Orange Terracotta Bricks | wall-accent | yes | desert | braid |
| `chisel:hardened_clay_orange:1` | Cracked Orange Terracotta Bricks | wall-main, rubble-ruin | yes | desert | cracked bricks |
| `chisel:hardened_clay_orange:2` | Small Orange Terracotta Brick Tiles | wall-main | yes | desert | connected texture |
| `chisel:hardened_clay_orange:3` | Small Orange Terracotta Bricks | wall-main | yes | desert |  |
| `chisel:hardened_clay_orange:4` | Aligned Orange Terracotta Bricks | wall-main | yes | desert |  |
| `chisel:hardened_clay_orange:5` | Detailed Orange Terracotta Bricks | wall-main | yes | desert |  |
| `chisel:hardened_clay_orange:6` | Wide Orange Terracotta Bricks | wall-main | yes | desert |  |
| `chisel:hardened_clay_orange:7` | Chaotic Small Orange Terracotta Bricks | wall-main | yes | desert |  |
| `chisel:hardened_clay_orange:8` | Small Disordered Orange Terracotta Bricks | wall-main | yes | desert |  |
| `chisel:hardened_clay_orange:9` | Ornate Orange Terracotta | wall-accent | yes | desert | connected texture |
| `chisel:hardened_clay_orange:10` | Cracked Orange Terracotta | rubble-ruin | yes | desert | cracked |
| `chisel:hardened_clay_orange:11` | Sunken Orange Terracotta Panel | wall-accent | yes | desert | connected texture |
| `chisel:hardened_clay_orange:12` | Fancy Orange Terracotta Bricks | wall-accent | yes | desert |  |
| `chisel:hardened_clay_orange:13` | Fancy Orange Terracotta Tiles | floor | yes | desert |  |
| `chisel:hardened_clay_orange:14` | Orange Terracotta Layers | wall-accent | yes | desert | horizontal bands |
| `chisel:hardened_clay_orange:15` | Orange Terracotta Mosaic | floor | yes | desert | connected texture |
| `chisel:hardened_clay_orange2:0` | Small Ornate Orange Terracotta | wall-accent | yes | desert |  |
| `chisel:hardened_clay_orange2:1` | Orange Terracotta Large Tile | floor, wall-main | yes | desert |  |
| `chisel:hardened_clay_orange2:2` | Orange Terracotta Pillar | trim-pillar | yes | desert |  |
| `chisel:hardened_clay_orange2:3` | Prismatic Orange Terracotta | wall-accent | yes | desert |  |
| `chisel:hardened_clay_orange2:4` | Orange Terracotta Bricks in a Fancy Arrangement | wall-accent | yes | desert |  |
| `chisel:hardened_clay_orange2:5` | Orange Terracotta Panel | wall-main, ceiling | yes | desert | plain panel; connected texture |
| `chisel:hardened_clay_orange2:6` | Small Orange Terracotta Blocks | wall-main | yes | desert |  |
| `chisel:hardened_clay_orange2:7` | Small Orange Terracotta Tiles | floor | yes | desert |  |
| `chisel:hardened_clay_orange2:8` | Radial Carved Orange Terracotta | wall-accent | yes | desert | carved motif |
| `chisel:hardened_clay_orange2:9` | Engraved Orange Terracotta Block | wall-accent | yes | desert | carved frame; connected texture |
| `chisel:hardened_clay_magenta:0` | Braid Magenta Terracotta Bricks | wall-accent | no | - | braid |
| `chisel:hardened_clay_magenta:1` | Cracked Magenta Terracotta Bricks | wall-main, rubble-ruin | no | - | cracked bricks |
| `chisel:hardened_clay_magenta:2` | Small Magenta Terracotta Brick Tiles | wall-main | no | - | connected texture |
| `chisel:hardened_clay_magenta:3` | Small Magenta Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_magenta:4` | Aligned Magenta Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_magenta:5` | Detailed Magenta Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_magenta:6` | Wide Magenta Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_magenta:7` | Chaotic Small Magenta Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_magenta:8` | Small Disordered Magenta Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_magenta:9` | Ornate Magenta Terracotta | wall-accent | no | - | connected texture |
| `chisel:hardened_clay_magenta:10` | Cracked Magenta Terracotta | rubble-ruin | no | - | cracked |
| `chisel:hardened_clay_magenta:11` | Sunken Magenta Terracotta Panel | wall-accent | no | - | connected texture |
| `chisel:hardened_clay_magenta:12` | Fancy Magenta Terracotta Bricks | wall-accent | no | - |  |
| `chisel:hardened_clay_magenta:13` | Fancy Magenta Terracotta Tiles | floor | no | - |  |
| `chisel:hardened_clay_magenta:14` | Magenta Terracotta Layers | wall-accent | no | - | horizontal bands |
| `chisel:hardened_clay_magenta:15` | Magenta Terracotta Mosaic | floor | no | - | connected texture |
| `chisel:hardened_clay_magenta2:0` | Small Ornate Magenta Terracotta | wall-accent | no | - |  |
| `chisel:hardened_clay_magenta2:1` | Magenta Terracotta Large Tile | floor, wall-main | no | - |  |
| `chisel:hardened_clay_magenta2:2` | Magenta Terracotta Pillar | trim-pillar | no | - |  |
| `chisel:hardened_clay_magenta2:3` | Prismatic Magenta Terracotta | wall-accent | no | - |  |
| `chisel:hardened_clay_magenta2:4` | Magenta Terracotta Bricks in a Fancy Arrangement | wall-accent | no | - |  |
| `chisel:hardened_clay_magenta2:5` | Magenta Terracotta Panel | wall-main, ceiling | no | - | plain panel; connected texture |
| `chisel:hardened_clay_magenta2:6` | Small Magenta Terracotta Blocks | wall-main | no | - |  |
| `chisel:hardened_clay_magenta2:7` | Small Magenta Terracotta Tiles | floor | no | - |  |
| `chisel:hardened_clay_magenta2:8` | Radial Carved Magenta Terracotta | wall-accent | no | - | carved motif |
| `chisel:hardened_clay_magenta2:9` | Engraved Magenta Terracotta Block | wall-accent | no | - | carved frame; connected texture |
| `chisel:hardened_clay_lightblue:0` | Braid Light Blue Terracotta Bricks | wall-accent | partly | standard | braid |
| `chisel:hardened_clay_lightblue:1` | Cracked Light Blue Terracotta Bricks | wall-main, rubble-ruin | partly | standard | cracked bricks |
| `chisel:hardened_clay_lightblue:2` | Small Light Blue Terracotta Brick Tiles | wall-main | partly | standard | connected texture |
| `chisel:hardened_clay_lightblue:3` | Small Light Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_lightblue:4` | Aligned Light Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_lightblue:5` | Detailed Light Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_lightblue:6` | Wide Light Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_lightblue:7` | Chaotic Small Light Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_lightblue:8` | Small Disordered Light Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_lightblue:9` | Ornate Light Blue Terracotta | wall-accent | partly | standard | connected texture |
| `chisel:hardened_clay_lightblue:10` | Cracked Light Blue Terracotta | rubble-ruin | partly | standard | cracked |
| `chisel:hardened_clay_lightblue:11` | Sunken Light Blue Terracotta Panel | wall-accent | partly | standard | connected texture |
| `chisel:hardened_clay_lightblue:12` | Fancy Light Blue Terracotta Bricks | wall-accent | partly | standard |  |
| `chisel:hardened_clay_lightblue:13` | Fancy Light Blue Terracotta Tiles | floor | partly | standard |  |
| `chisel:hardened_clay_lightblue:14` | Light Blue Terracotta Layers | wall-accent | partly | standard | horizontal bands |
| `chisel:hardened_clay_lightblue:15` | Light Blue Terracotta Mosaic | floor | partly | standard | connected texture |
| `chisel:hardened_clay_lightblue2:0` | Small Ornate Light Blue Terracotta | wall-accent | partly | standard |  |
| `chisel:hardened_clay_lightblue2:1` | Light Blue Terracotta Large Tile | floor, wall-main | partly | standard |  |
| `chisel:hardened_clay_lightblue2:2` | Light Blue Terracotta Pillar | trim-pillar | partly | standard |  |
| `chisel:hardened_clay_lightblue2:3` | Prismatic Light Blue Terracotta | wall-accent | partly | standard |  |
| `chisel:hardened_clay_lightblue2:4` | Light Blue Terracotta Bricks in a Fancy Arrangement | wall-accent | partly | standard |  |
| `chisel:hardened_clay_lightblue2:5` | Light Blue Terracotta Panel | wall-main, ceiling | partly | standard | plain panel; connected texture |
| `chisel:hardened_clay_lightblue2:6` | Small Light Blue Terracotta Blocks | wall-main | partly | standard |  |
| `chisel:hardened_clay_lightblue2:7` | Small Light Blue Terracotta Tiles | floor | partly | standard |  |
| `chisel:hardened_clay_lightblue2:8` | Radial Carved Light Blue Terracotta | wall-accent | partly | standard | carved motif |
| `chisel:hardened_clay_lightblue2:9` | Engraved Light Blue Terracotta Block | wall-accent | partly | standard | carved frame; connected texture |
| `chisel:hardened_clay_yellow:0` | Braid Yellow Terracotta Bricks | wall-accent | partly | desert | braid |
| `chisel:hardened_clay_yellow:1` | Cracked Yellow Terracotta Bricks | wall-main, rubble-ruin | partly | desert | cracked bricks |
| `chisel:hardened_clay_yellow:2` | Small Yellow Terracotta Brick Tiles | wall-main | partly | desert | connected texture |
| `chisel:hardened_clay_yellow:3` | Small Yellow Terracotta Bricks | wall-main | partly | desert |  |
| `chisel:hardened_clay_yellow:4` | Aligned Yellow Terracotta Bricks | wall-main | partly | desert |  |
| `chisel:hardened_clay_yellow:5` | Detailed Yellow Terracotta Bricks | wall-main | partly | desert |  |
| `chisel:hardened_clay_yellow:6` | Wide Yellow Terracotta Bricks | wall-main | partly | desert |  |
| `chisel:hardened_clay_yellow:7` | Chaotic Small Yellow Terracotta Bricks | wall-main | partly | desert |  |
| `chisel:hardened_clay_yellow:8` | Small Disordered Yellow Terracotta Bricks | wall-main | partly | desert |  |
| `chisel:hardened_clay_yellow:9` | Ornate Yellow Terracotta | wall-accent | partly | desert | connected texture |
| `chisel:hardened_clay_yellow:10` | Cracked Yellow Terracotta | rubble-ruin | partly | desert | cracked |
| `chisel:hardened_clay_yellow:11` | Sunken Yellow Terracotta Panel | wall-accent | partly | desert | connected texture |
| `chisel:hardened_clay_yellow:12` | Fancy Yellow Terracotta Bricks | wall-accent | partly | desert |  |
| `chisel:hardened_clay_yellow:13` | Fancy Yellow Terracotta Tiles | floor | partly | desert |  |
| `chisel:hardened_clay_yellow:14` | Yellow Terracotta Layers | wall-accent | partly | desert | horizontal bands |
| `chisel:hardened_clay_yellow:15` | Yellow Terracotta Mosaic | floor | partly | desert | connected texture |
| `chisel:hardened_clay_yellow2:0` | Small Ornate Yellow Terracotta | wall-accent | partly | desert |  |
| `chisel:hardened_clay_yellow2:1` | Yellow Terracotta Large Tile | floor, wall-main | partly | desert |  |
| `chisel:hardened_clay_yellow2:2` | Yellow Terracotta Pillar | trim-pillar | partly | desert |  |
| `chisel:hardened_clay_yellow2:3` | Prismatic Yellow Terracotta | wall-accent | partly | desert |  |
| `chisel:hardened_clay_yellow2:4` | Yellow Terracotta Bricks in a Fancy Arrangement | wall-accent | partly | desert |  |
| `chisel:hardened_clay_yellow2:5` | Yellow Terracotta Panel | wall-main, ceiling | partly | desert | plain panel; connected texture |
| `chisel:hardened_clay_yellow2:6` | Small Yellow Terracotta Blocks | wall-main | partly | desert |  |
| `chisel:hardened_clay_yellow2:7` | Small Yellow Terracotta Tiles | floor | partly | desert |  |
| `chisel:hardened_clay_yellow2:8` | Radial Carved Yellow Terracotta | wall-accent | partly | desert | carved motif |
| `chisel:hardened_clay_yellow2:9` | Engraved Yellow Terracotta Block | wall-accent | partly | desert | carved frame; connected texture |
| `chisel:hardened_clay_lime:0` | Braid Lime Terracotta Bricks | wall-accent | no | - | braid |
| `chisel:hardened_clay_lime:1` | Cracked Lime Terracotta Bricks | wall-main, rubble-ruin | no | - | cracked bricks |
| `chisel:hardened_clay_lime:2` | Small Lime Terracotta Brick Tiles | wall-main | no | - | connected texture |
| `chisel:hardened_clay_lime:3` | Small Lime Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_lime:4` | Aligned Lime Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_lime:5` | Detailed Lime Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_lime:6` | Wide Lime Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_lime:7` | Chaotic Small Lime Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_lime:8` | Small Disordered Lime Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_lime:9` | Ornate Lime Terracotta | wall-accent | no | - | connected texture |
| `chisel:hardened_clay_lime:10` | Cracked Lime Terracotta | rubble-ruin | no | - | cracked |
| `chisel:hardened_clay_lime:11` | Sunken Lime Terracotta Panel | wall-accent | no | - | connected texture |
| `chisel:hardened_clay_lime:12` | Fancy Lime Terracotta Bricks | wall-accent | no | - |  |
| `chisel:hardened_clay_lime:13` | Fancy Lime Terracotta Tiles | floor | no | - |  |
| `chisel:hardened_clay_lime:14` | Lime Terracotta Layers | wall-accent | no | - | horizontal bands |
| `chisel:hardened_clay_lime:15` | Lime Terracotta Mosaic | floor | no | - | connected texture |
| `chisel:hardened_clay_lime2:0` | Small Ornate Lime Terracotta | wall-accent | no | - |  |
| `chisel:hardened_clay_lime2:1` | Lime Terracotta Large Tile | floor, wall-main | no | - |  |
| `chisel:hardened_clay_lime2:2` | Lime Terracotta Pillar | trim-pillar | no | - |  |
| `chisel:hardened_clay_lime2:3` | Prismatic Lime Terracotta | wall-accent | no | - |  |
| `chisel:hardened_clay_lime2:4` | Lime Terracotta Bricks in a Fancy Arrangement | wall-accent | no | - |  |
| `chisel:hardened_clay_lime2:5` | Lime Terracotta Panel | wall-main, ceiling | no | - | plain panel; connected texture |
| `chisel:hardened_clay_lime2:6` | Small Lime Terracotta Blocks | wall-main | no | - |  |
| `chisel:hardened_clay_lime2:7` | Small Lime Terracotta Tiles | floor | no | - |  |
| `chisel:hardened_clay_lime2:8` | Radial Carved Lime Terracotta | wall-accent | no | - | carved motif |
| `chisel:hardened_clay_lime2:9` | Engraved Lime Terracotta Block | wall-accent | no | - | carved frame; connected texture |
| `chisel:hardened_clay_pink:0` | Braid Pink Terracotta Bricks | wall-accent | no | - | braid |
| `chisel:hardened_clay_pink:1` | Cracked Pink Terracotta Bricks | wall-main, rubble-ruin | no | - | cracked bricks |
| `chisel:hardened_clay_pink:2` | Small Pink Terracotta Brick Tiles | wall-main | no | - | connected texture |
| `chisel:hardened_clay_pink:3` | Small Pink Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_pink:4` | Aligned Pink Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_pink:5` | Detailed Pink Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_pink:6` | Wide Pink Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_pink:7` | Chaotic Small Pink Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_pink:8` | Small Disordered Pink Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_pink:9` | Ornate Pink Terracotta | wall-accent | no | - | connected texture |
| `chisel:hardened_clay_pink:10` | Cracked Pink Terracotta | rubble-ruin | no | - | cracked |
| `chisel:hardened_clay_pink:11` | Sunken Pink Terracotta Panel | wall-accent | no | - | connected texture |
| `chisel:hardened_clay_pink:12` | Fancy Pink Terracotta Bricks | wall-accent | no | - |  |
| `chisel:hardened_clay_pink:13` | Fancy Pink Terracotta Tiles | floor | no | - |  |
| `chisel:hardened_clay_pink:14` | Pink Terracotta Layers | wall-accent | no | - | horizontal bands |
| `chisel:hardened_clay_pink:15` | Pink Terracotta Mosaic | floor | no | - | connected texture |
| `chisel:hardened_clay_pink2:0` | Small Ornate Pink Terracotta | wall-accent | no | - |  |
| `chisel:hardened_clay_pink2:1` | Pink Terracotta Large Tile | floor, wall-main | no | - |  |
| `chisel:hardened_clay_pink2:2` | Pink Terracotta Pillar | trim-pillar | no | - |  |
| `chisel:hardened_clay_pink2:3` | Prismatic Pink Terracotta | wall-accent | no | - |  |
| `chisel:hardened_clay_pink2:4` | Pink Terracotta Bricks in a Fancy Arrangement | wall-accent | no | - |  |
| `chisel:hardened_clay_pink2:5` | Pink Terracotta Panel | wall-main, ceiling | no | - | plain panel; connected texture |
| `chisel:hardened_clay_pink2:6` | Small Pink Terracotta Blocks | wall-main | no | - |  |
| `chisel:hardened_clay_pink2:7` | Small Pink Terracotta Tiles | floor | no | - |  |
| `chisel:hardened_clay_pink2:8` | Radial Carved Pink Terracotta | wall-accent | no | - | carved motif |
| `chisel:hardened_clay_pink2:9` | Engraved Pink Terracotta Block | wall-accent | no | - | carved frame; connected texture |
| `chisel:hardened_clay_gray:0` | Braid Gray Terracotta Bricks | wall-accent | yes | standard, border | braid |
| `chisel:hardened_clay_gray:1` | Cracked Gray Terracotta Bricks | wall-main, rubble-ruin | yes | standard, border | cracked bricks |
| `chisel:hardened_clay_gray:2` | Small Gray Terracotta Brick Tiles | wall-main | yes | standard, border | connected texture |
| `chisel:hardened_clay_gray:3` | Small Gray Terracotta Bricks | wall-main | yes | standard, border |  |
| `chisel:hardened_clay_gray:4` | Aligned Gray Terracotta Bricks | wall-main | yes | standard, border |  |
| `chisel:hardened_clay_gray:5` | Detailed Gray Terracotta Bricks | wall-main | yes | standard, border |  |
| `chisel:hardened_clay_gray:6` | Wide Gray Terracotta Bricks | wall-main | yes | standard, border |  |
| `chisel:hardened_clay_gray:7` | Chaotic Small Gray Terracotta Bricks | wall-main | yes | standard, border |  |
| `chisel:hardened_clay_gray:8` | Small Disordered Gray Terracotta Bricks | wall-main | yes | standard, border |  |
| `chisel:hardened_clay_gray:9` | Ornate Gray Terracotta | wall-accent | yes | standard, border | connected texture |
| `chisel:hardened_clay_gray:10` | Cracked Gray Terracotta | rubble-ruin | yes | standard, border | cracked |
| `chisel:hardened_clay_gray:11` | Sunken Gray Terracotta Panel | wall-accent | yes | standard, border | connected texture |
| `chisel:hardened_clay_gray:12` | Fancy Gray Terracotta Bricks | wall-accent | yes | standard, border |  |
| `chisel:hardened_clay_gray:13` | Fancy Gray Terracotta Tiles | floor | yes | standard, border |  |
| `chisel:hardened_clay_gray:14` | Gray Terracotta Layers | wall-accent | yes | standard, border | horizontal bands |
| `chisel:hardened_clay_gray:15` | Gray Terracotta Mosaic | floor | yes | standard, border | connected texture |
| `chisel:hardened_clay_gray2:0` | Small Ornate Gray Terracotta | wall-accent | yes | standard, border |  |
| `chisel:hardened_clay_gray2:1` | Gray Terracotta Large Tile | floor, wall-main | yes | standard, border |  |
| `chisel:hardened_clay_gray2:2` | Gray Terracotta Pillar | trim-pillar | yes | standard, border |  |
| `chisel:hardened_clay_gray2:3` | Prismatic Gray Terracotta | wall-accent | yes | standard, border |  |
| `chisel:hardened_clay_gray2:4` | Gray Terracotta Bricks in a Fancy Arrangement | wall-accent | yes | standard, border |  |
| `chisel:hardened_clay_gray2:5` | Gray Terracotta Panel | wall-main, ceiling | yes | standard, border | plain panel; connected texture |
| `chisel:hardened_clay_gray2:6` | Small Gray Terracotta Blocks | wall-main | yes | standard, border |  |
| `chisel:hardened_clay_gray2:7` | Small Gray Terracotta Tiles | floor | yes | standard, border |  |
| `chisel:hardened_clay_gray2:8` | Radial Carved Gray Terracotta | wall-accent | yes | standard, border | carved motif |
| `chisel:hardened_clay_gray2:9` | Engraved Gray Terracotta Block | wall-accent | yes | standard, border | carved frame; connected texture |
| `chisel:hardened_clay_lightgray:0` | Braid Light Gray Terracotta Bricks | wall-accent | yes | standard | braid |
| `chisel:hardened_clay_lightgray:1` | Cracked Light Gray Terracotta Bricks | wall-main, rubble-ruin | yes | standard | cracked bricks |
| `chisel:hardened_clay_lightgray:2` | Small Light Gray Terracotta Brick Tiles | wall-main | yes | standard | connected texture |
| `chisel:hardened_clay_lightgray:3` | Small Light Gray Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_lightgray:4` | Aligned Light Gray Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_lightgray:5` | Detailed Light Gray Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_lightgray:6` | Wide Light Gray Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_lightgray:7` | Chaotic Small Light Gray Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_lightgray:8` | Small Disordered Light Gray Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_lightgray:9` | Ornate Light Gray Terracotta | wall-accent | yes | standard | connected texture |
| `chisel:hardened_clay_lightgray:10` | Cracked Light Gray Terracotta | rubble-ruin | yes | standard | cracked |
| `chisel:hardened_clay_lightgray:11` | Sunken Light Gray Terracotta Panel | wall-accent | yes | standard | connected texture |
| `chisel:hardened_clay_lightgray:12` | Fancy Light Gray Terracotta Bricks | wall-accent | yes | standard |  |
| `chisel:hardened_clay_lightgray:13` | Fancy Light Gray Terracotta Tiles | floor | yes | standard |  |
| `chisel:hardened_clay_lightgray:14` | Light Gray Terracotta Layers | wall-accent | yes | standard | horizontal bands |
| `chisel:hardened_clay_lightgray:15` | Light Gray Terracotta Mosaic | floor | yes | standard | connected texture |
| `chisel:hardened_clay_lightgray2:0` | Small Ornate Light Gray Terracotta | wall-accent | yes | standard |  |
| `chisel:hardened_clay_lightgray2:1` | Light Gray Terracotta Large Tile | floor, wall-main | yes | standard |  |
| `chisel:hardened_clay_lightgray2:2` | Light Gray Terracotta Pillar | trim-pillar | yes | standard |  |
| `chisel:hardened_clay_lightgray2:3` | Prismatic Light Gray Terracotta | wall-accent | yes | standard |  |
| `chisel:hardened_clay_lightgray2:4` | Light Gray Terracotta Bricks in a Fancy Arrangement | wall-accent | yes | standard |  |
| `chisel:hardened_clay_lightgray2:5` | Light Gray Terracotta Panel | wall-main, ceiling | yes | standard | plain panel; connected texture |
| `chisel:hardened_clay_lightgray2:6` | Small Light Gray Terracotta Blocks | wall-main | yes | standard |  |
| `chisel:hardened_clay_lightgray2:7` | Small Light Gray Terracotta Tiles | floor | yes | standard |  |
| `chisel:hardened_clay_lightgray2:8` | Radial Carved Light Gray Terracotta | wall-accent | yes | standard | carved motif |
| `chisel:hardened_clay_lightgray2:9` | Engraved Light Gray Terracotta Block | wall-accent | yes | standard | carved frame; connected texture |
| `chisel:hardened_clay_cyan:0` | Braid Cyan Terracotta Bricks | wall-accent | yes | standard | braid |
| `chisel:hardened_clay_cyan:1` | Cracked Cyan Terracotta Bricks | wall-main, rubble-ruin | yes | standard | cracked bricks |
| `chisel:hardened_clay_cyan:2` | Small Cyan Terracotta Brick Tiles | wall-main | yes | standard | connected texture |
| `chisel:hardened_clay_cyan:3` | Small Cyan Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_cyan:4` | Aligned Cyan Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_cyan:5` | Detailed Cyan Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_cyan:6` | Wide Cyan Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_cyan:7` | Chaotic Small Cyan Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_cyan:8` | Small Disordered Cyan Terracotta Bricks | wall-main | yes | standard |  |
| `chisel:hardened_clay_cyan:9` | Ornate Cyan Terracotta | wall-accent | yes | standard | connected texture |
| `chisel:hardened_clay_cyan:10` | Cracked Cyan Terracotta | rubble-ruin | yes | standard | cracked |
| `chisel:hardened_clay_cyan:11` | Sunken Cyan Terracotta Panel | wall-accent | yes | standard | connected texture |
| `chisel:hardened_clay_cyan:12` | Fancy Cyan Terracotta Bricks | wall-accent | yes | standard |  |
| `chisel:hardened_clay_cyan:13` | Fancy Cyan Terracotta Tiles | floor | yes | standard |  |
| `chisel:hardened_clay_cyan:14` | Cyan Terracotta Layers | wall-accent | yes | standard | horizontal bands |
| `chisel:hardened_clay_cyan:15` | Cyan Terracotta Mosaic | floor | yes | standard | connected texture |
| `chisel:hardened_clay_cyan2:0` | Small Ornate Cyan Terracotta | wall-accent | yes | standard |  |
| `chisel:hardened_clay_cyan2:1` | Cyan Terracotta Large Tile | floor, wall-main | yes | standard |  |
| `chisel:hardened_clay_cyan2:2` | Cyan Terracotta Pillar | trim-pillar | yes | standard |  |
| `chisel:hardened_clay_cyan2:3` | Prismatic Cyan Terracotta | wall-accent | yes | standard |  |
| `chisel:hardened_clay_cyan2:4` | Cyan Terracotta Bricks in a Fancy Arrangement | wall-accent | yes | standard |  |
| `chisel:hardened_clay_cyan2:5` | Cyan Terracotta Panel | wall-main, ceiling | yes | standard | plain panel; connected texture |
| `chisel:hardened_clay_cyan2:6` | Small Cyan Terracotta Blocks | wall-main | yes | standard |  |
| `chisel:hardened_clay_cyan2:7` | Small Cyan Terracotta Tiles | floor | yes | standard |  |
| `chisel:hardened_clay_cyan2:8` | Radial Carved Cyan Terracotta | wall-accent | yes | standard | carved motif |
| `chisel:hardened_clay_cyan2:9` | Engraved Cyan Terracotta Block | wall-accent | yes | standard | carved frame; connected texture |
| `chisel:hardened_clay_purple:0` | Braid Purple Terracotta Bricks | wall-accent | no | - | braid |
| `chisel:hardened_clay_purple:1` | Cracked Purple Terracotta Bricks | wall-main, rubble-ruin | no | - | cracked bricks |
| `chisel:hardened_clay_purple:2` | Small Purple Terracotta Brick Tiles | wall-main | no | - | connected texture |
| `chisel:hardened_clay_purple:3` | Small Purple Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_purple:4` | Aligned Purple Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_purple:5` | Detailed Purple Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_purple:6` | Wide Purple Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_purple:7` | Chaotic Small Purple Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_purple:8` | Small Disordered Purple Terracotta Bricks | wall-main | no | - |  |
| `chisel:hardened_clay_purple:9` | Ornate Purple Terracotta | wall-accent | no | - | connected texture |
| `chisel:hardened_clay_purple:10` | Cracked Purple Terracotta | rubble-ruin | no | - | cracked |
| `chisel:hardened_clay_purple:11` | Sunken Purple Terracotta Panel | wall-accent | no | - | connected texture |
| `chisel:hardened_clay_purple:12` | Fancy Purple Terracotta Bricks | wall-accent | no | - |  |
| `chisel:hardened_clay_purple:13` | Fancy Purple Terracotta Tiles | floor | no | - |  |
| `chisel:hardened_clay_purple:14` | Purple Terracotta Layers | wall-accent | no | - | horizontal bands |
| `chisel:hardened_clay_purple:15` | Purple Terracotta Mosaic | floor | no | - | connected texture |
| `chisel:hardened_clay_purple2:0` | Small Ornate Purple Terracotta | wall-accent | no | - |  |
| `chisel:hardened_clay_purple2:1` | Purple Terracotta Large Tile | floor, wall-main | no | - |  |
| `chisel:hardened_clay_purple2:2` | Purple Terracotta Pillar | trim-pillar | no | - |  |
| `chisel:hardened_clay_purple2:3` | Prismatic Purple Terracotta | wall-accent | no | - |  |
| `chisel:hardened_clay_purple2:4` | Purple Terracotta Bricks in a Fancy Arrangement | wall-accent | no | - |  |
| `chisel:hardened_clay_purple2:5` | Purple Terracotta Panel | wall-main, ceiling | no | - | plain panel; connected texture |
| `chisel:hardened_clay_purple2:6` | Small Purple Terracotta Blocks | wall-main | no | - |  |
| `chisel:hardened_clay_purple2:7` | Small Purple Terracotta Tiles | floor | no | - |  |
| `chisel:hardened_clay_purple2:8` | Radial Carved Purple Terracotta | wall-accent | no | - | carved motif |
| `chisel:hardened_clay_purple2:9` | Engraved Purple Terracotta Block | wall-accent | no | - | carved frame; connected texture |
| `chisel:hardened_clay_blue:0` | Braid Blue Terracotta Bricks | wall-accent | partly | standard | braid |
| `chisel:hardened_clay_blue:1` | Cracked Blue Terracotta Bricks | wall-main, rubble-ruin | partly | standard | cracked bricks |
| `chisel:hardened_clay_blue:2` | Small Blue Terracotta Brick Tiles | wall-main | partly | standard | connected texture |
| `chisel:hardened_clay_blue:3` | Small Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_blue:4` | Aligned Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_blue:5` | Detailed Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_blue:6` | Wide Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_blue:7` | Chaotic Small Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_blue:8` | Small Disordered Blue Terracotta Bricks | wall-main | partly | standard |  |
| `chisel:hardened_clay_blue:9` | Ornate Blue Terracotta | wall-accent | partly | standard | connected texture |
| `chisel:hardened_clay_blue:10` | Cracked Blue Terracotta | rubble-ruin | partly | standard | cracked |
| `chisel:hardened_clay_blue:11` | Sunken Blue Terracotta Panel | wall-accent | partly | standard | connected texture |
| `chisel:hardened_clay_blue:12` | Fancy Blue Terracotta Bricks | wall-accent | partly | standard |  |
| `chisel:hardened_clay_blue:13` | Fancy Blue Terracotta Tiles | floor | partly | standard |  |
| `chisel:hardened_clay_blue:14` | Blue Terracotta Layers | wall-accent | partly | standard | horizontal bands |
| `chisel:hardened_clay_blue:15` | Blue Terracotta Mosaic | floor | partly | standard | connected texture |
| `chisel:hardened_clay_blue2:0` | Small Ornate Blue Terracotta | wall-accent | partly | standard |  |
| `chisel:hardened_clay_blue2:1` | Blue Terracotta Large Tile | floor, wall-main | partly | standard |  |
| `chisel:hardened_clay_blue2:2` | Blue Terracotta Pillar | trim-pillar | partly | standard |  |
| `chisel:hardened_clay_blue2:3` | Prismatic Blue Terracotta | wall-accent | partly | standard |  |
| `chisel:hardened_clay_blue2:4` | Blue Terracotta Bricks in a Fancy Arrangement | wall-accent | partly | standard |  |
| `chisel:hardened_clay_blue2:5` | Blue Terracotta Panel | wall-main, ceiling | partly | standard | plain panel; connected texture |
| `chisel:hardened_clay_blue2:6` | Small Blue Terracotta Blocks | wall-main | partly | standard |  |
| `chisel:hardened_clay_blue2:7` | Small Blue Terracotta Tiles | floor | partly | standard |  |
| `chisel:hardened_clay_blue2:8` | Radial Carved Blue Terracotta | wall-accent | partly | standard | carved motif |
| `chisel:hardened_clay_blue2:9` | Engraved Blue Terracotta Block | wall-accent | partly | standard | carved frame; connected texture |
| `chisel:hardened_clay_brown:0` | Braid Brown Terracotta Bricks | wall-accent | yes | standard, desert | braid |
| `chisel:hardened_clay_brown:1` | Cracked Brown Terracotta Bricks | wall-main, rubble-ruin | yes | standard, desert | cracked bricks |
| `chisel:hardened_clay_brown:2` | Small Brown Terracotta Brick Tiles | wall-main | yes | standard, desert | connected texture |
| `chisel:hardened_clay_brown:3` | Small Brown Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_brown:4` | Aligned Brown Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_brown:5` | Detailed Brown Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_brown:6` | Wide Brown Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_brown:7` | Chaotic Small Brown Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_brown:8` | Small Disordered Brown Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_brown:9` | Ornate Brown Terracotta | wall-accent | yes | standard, desert | connected texture |
| `chisel:hardened_clay_brown:10` | Cracked Brown Terracotta | rubble-ruin | yes | standard, desert | cracked |
| `chisel:hardened_clay_brown:11` | Sunken Brown Terracotta Panel | wall-accent | yes | standard, desert | connected texture |
| `chisel:hardened_clay_brown:12` | Fancy Brown Terracotta Bricks | wall-accent | yes | standard, desert |  |
| `chisel:hardened_clay_brown:13` | Fancy Brown Terracotta Tiles | floor | yes | standard, desert |  |
| `chisel:hardened_clay_brown:14` | Brown Terracotta Layers | wall-accent | yes | standard, desert | horizontal bands |
| `chisel:hardened_clay_brown:15` | Brown Terracotta Mosaic | floor | yes | standard, desert | connected texture |
| `chisel:hardened_clay_brown2:0` | Small Ornate Brown Terracotta | wall-accent | yes | standard, desert |  |
| `chisel:hardened_clay_brown2:1` | Brown Terracotta Large Tile | floor, wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_brown2:2` | Brown Terracotta Pillar | trim-pillar | yes | standard, desert |  |
| `chisel:hardened_clay_brown2:3` | Prismatic Brown Terracotta | wall-accent | yes | standard, desert |  |
| `chisel:hardened_clay_brown2:4` | Brown Terracotta Bricks in a Fancy Arrangement | wall-accent | yes | standard, desert |  |
| `chisel:hardened_clay_brown2:5` | Brown Terracotta Panel | wall-main, ceiling | yes | standard, desert | plain panel; connected texture |
| `chisel:hardened_clay_brown2:6` | Small Brown Terracotta Blocks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_brown2:7` | Small Brown Terracotta Tiles | floor | yes | standard, desert |  |
| `chisel:hardened_clay_brown2:8` | Radial Carved Brown Terracotta | wall-accent | yes | standard, desert | carved motif |
| `chisel:hardened_clay_brown2:9` | Engraved Brown Terracotta Block | wall-accent | yes | standard, desert | carved frame; connected texture |
| `chisel:hardened_clay_green:0` | Braid Green Terracotta Bricks | wall-accent | partly | standard, overgrown | braid |
| `chisel:hardened_clay_green:1` | Cracked Green Terracotta Bricks | wall-main, rubble-ruin | partly | standard, overgrown | cracked bricks |
| `chisel:hardened_clay_green:2` | Small Green Terracotta Brick Tiles | wall-main | partly | standard, overgrown | connected texture |
| `chisel:hardened_clay_green:3` | Small Green Terracotta Bricks | wall-main | partly | standard, overgrown |  |
| `chisel:hardened_clay_green:4` | Aligned Green Terracotta Bricks | wall-main | partly | standard, overgrown |  |
| `chisel:hardened_clay_green:5` | Detailed Green Terracotta Bricks | wall-main | partly | standard, overgrown |  |
| `chisel:hardened_clay_green:6` | Wide Green Terracotta Bricks | wall-main | partly | standard, overgrown |  |
| `chisel:hardened_clay_green:7` | Chaotic Small Green Terracotta Bricks | wall-main | partly | standard, overgrown |  |
| `chisel:hardened_clay_green:8` | Small Disordered Green Terracotta Bricks | wall-main | partly | standard, overgrown |  |
| `chisel:hardened_clay_green:9` | Ornate Green Terracotta | wall-accent | partly | standard, overgrown | connected texture |
| `chisel:hardened_clay_green:10` | Cracked Green Terracotta | rubble-ruin | partly | standard, overgrown | cracked |
| `chisel:hardened_clay_green:11` | Sunken Green Terracotta Panel | wall-accent | partly | standard, overgrown | connected texture |
| `chisel:hardened_clay_green:12` | Fancy Green Terracotta Bricks | wall-accent | partly | standard, overgrown |  |
| `chisel:hardened_clay_green:13` | Fancy Green Terracotta Tiles | floor | partly | standard, overgrown |  |
| `chisel:hardened_clay_green:14` | Green Terracotta Layers | wall-accent | partly | standard, overgrown | horizontal bands |
| `chisel:hardened_clay_green:15` | Green Terracotta Mosaic | floor | partly | standard, overgrown | connected texture |
| `chisel:hardened_clay_green2:0` | Small Ornate Green Terracotta | wall-accent | partly | standard, overgrown |  |
| `chisel:hardened_clay_green2:1` | Green Terracotta Large Tile | floor, wall-main | partly | standard, overgrown |  |
| `chisel:hardened_clay_green2:2` | Green Terracotta Pillar | trim-pillar | partly | standard, overgrown |  |
| `chisel:hardened_clay_green2:3` | Prismatic Green Terracotta | wall-accent | partly | standard, overgrown |  |
| `chisel:hardened_clay_green2:4` | Green Terracotta Bricks in a Fancy Arrangement | wall-accent | partly | standard, overgrown |  |
| `chisel:hardened_clay_green2:5` | Green Terracotta Panel | wall-main, ceiling | partly | standard, overgrown | plain panel; connected texture |
| `chisel:hardened_clay_green2:6` | Small Green Terracotta Blocks | wall-main | partly | standard, overgrown |  |
| `chisel:hardened_clay_green2:7` | Small Green Terracotta Tiles | floor | partly | standard, overgrown |  |
| `chisel:hardened_clay_green2:8` | Radial Carved Green Terracotta | wall-accent | partly | standard, overgrown | carved motif |
| `chisel:hardened_clay_green2:9` | Engraved Green Terracotta Block | wall-accent | partly | standard, overgrown | carved frame; connected texture |
| `chisel:hardened_clay_red:0` | Braid Red Terracotta Bricks | wall-accent | yes | standard, desert | braid |
| `chisel:hardened_clay_red:1` | Cracked Red Terracotta Bricks | wall-main, rubble-ruin | yes | standard, desert | cracked bricks |
| `chisel:hardened_clay_red:2` | Small Red Terracotta Brick Tiles | wall-main | yes | standard, desert | connected texture |
| `chisel:hardened_clay_red:3` | Small Red Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_red:4` | Aligned Red Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_red:5` | Detailed Red Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_red:6` | Wide Red Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_red:7` | Chaotic Small Red Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_red:8` | Small Disordered Red Terracotta Bricks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_red:9` | Ornate Red Terracotta | wall-accent | yes | standard, desert | connected texture |
| `chisel:hardened_clay_red:10` | Cracked Red Terracotta | rubble-ruin | yes | standard, desert | cracked |
| `chisel:hardened_clay_red:11` | Sunken Red Terracotta Panel | wall-accent | yes | standard, desert | connected texture |
| `chisel:hardened_clay_red:12` | Fancy Red Terracotta Bricks | wall-accent | yes | standard, desert |  |
| `chisel:hardened_clay_red:13` | Fancy Red Terracotta Tiles | floor | yes | standard, desert |  |
| `chisel:hardened_clay_red:14` | Red Terracotta Layers | wall-accent | yes | standard, desert | horizontal bands |
| `chisel:hardened_clay_red:15` | Red Terracotta Mosaic | floor | yes | standard, desert | connected texture |
| `chisel:hardened_clay_red2:0` | Small Ornate Red Terracotta | wall-accent | yes | standard, desert |  |
| `chisel:hardened_clay_red2:1` | Red Terracotta Large Tile | floor, wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_red2:2` | Red Terracotta Pillar | trim-pillar | yes | standard, desert |  |
| `chisel:hardened_clay_red2:3` | Prismatic Red Terracotta | wall-accent | yes | standard, desert |  |
| `chisel:hardened_clay_red2:4` | Red Terracotta Bricks in a Fancy Arrangement | wall-accent | yes | standard, desert |  |
| `chisel:hardened_clay_red2:5` | Red Terracotta Panel | wall-main, ceiling | yes | standard, desert | plain panel; connected texture |
| `chisel:hardened_clay_red2:6` | Small Red Terracotta Blocks | wall-main | yes | standard, desert |  |
| `chisel:hardened_clay_red2:7` | Small Red Terracotta Tiles | floor | yes | standard, desert |  |
| `chisel:hardened_clay_red2:8` | Radial Carved Red Terracotta | wall-accent | yes | standard, desert | carved motif |
| `chisel:hardened_clay_red2:9` | Engraved Red Terracotta Block | wall-accent | yes | standard, desert | carved frame; connected texture |
| `chisel:hardened_clay_black:0` | Braid Black Terracotta Bricks | wall-accent | yes | standard, industrial | braid |
| `chisel:hardened_clay_black:1` | Cracked Black Terracotta Bricks | wall-main, rubble-ruin | yes | standard, industrial | cracked bricks |
| `chisel:hardened_clay_black:2` | Small Black Terracotta Brick Tiles | wall-main | yes | standard, industrial | connected texture |
| `chisel:hardened_clay_black:3` | Small Black Terracotta Bricks | wall-main | yes | standard, industrial |  |
| `chisel:hardened_clay_black:4` | Aligned Black Terracotta Bricks | wall-main | yes | standard, industrial |  |
| `chisel:hardened_clay_black:5` | Detailed Black Terracotta Bricks | wall-main | yes | standard, industrial |  |
| `chisel:hardened_clay_black:6` | Wide Black Terracotta Bricks | wall-main | yes | standard, industrial |  |
| `chisel:hardened_clay_black:7` | Chaotic Small Black Terracotta Bricks | wall-main | yes | standard, industrial |  |
| `chisel:hardened_clay_black:8` | Small Disordered Black Terracotta Bricks | wall-main | yes | standard, industrial |  |
| `chisel:hardened_clay_black:9` | Ornate Black Terracotta | wall-accent | yes | standard, industrial | connected texture |
| `chisel:hardened_clay_black:10` | Cracked Black Terracotta | rubble-ruin | yes | standard, industrial | cracked |
| `chisel:hardened_clay_black:11` | Sunken Black Terracotta Panel | wall-accent | yes | standard, industrial | connected texture |
| `chisel:hardened_clay_black:12` | Fancy Black Terracotta Bricks | wall-accent | yes | standard, industrial |  |
| `chisel:hardened_clay_black:13` | Fancy Black Terracotta Tiles | floor | yes | standard, industrial |  |
| `chisel:hardened_clay_black:14` | Black Terracotta Layers | wall-accent | yes | standard, industrial | horizontal bands |
| `chisel:hardened_clay_black:15` | Black Terracotta Mosaic | floor | yes | standard, industrial | connected texture |
| `chisel:hardened_clay_black2:0` | Small Ornate Black Terracotta | wall-accent | yes | standard, industrial |  |
| `chisel:hardened_clay_black2:1` | Black Terracotta Large Tile | floor, wall-main | yes | standard, industrial |  |
| `chisel:hardened_clay_black2:2` | Black Terracotta Pillar | trim-pillar | yes | standard, industrial |  |
| `chisel:hardened_clay_black2:3` | Prismatic Black Terracotta | wall-accent | yes | standard, industrial |  |
| `chisel:hardened_clay_black2:4` | Black Terracotta Bricks in a Fancy Arrangement | wall-accent | yes | standard, industrial |  |
| `chisel:hardened_clay_black2:5` | Black Terracotta Panel | wall-main, ceiling | yes | standard, industrial | plain panel; connected texture |
| `chisel:hardened_clay_black2:6` | Small Black Terracotta Blocks | wall-main | yes | standard, industrial |  |
| `chisel:hardened_clay_black2:7` | Small Black Terracotta Tiles | floor | yes | standard, industrial |  |
| `chisel:hardened_clay_black2:8` | Radial Carved Black Terracotta | wall-accent | yes | standard, industrial | carved motif |
| `chisel:hardened_clay_black2:9` | Engraved Black Terracotta Block | wall-accent | yes | standard, industrial | carved frame; connected texture |

</details>

### Modern: albumblock, brutalism, porcelain, alabaster, nucrete

Sheets: `modern.png`.

#### `ALBUMBLOCK` feature: `chisel:albumblock`, `chisel:albumblock2`

Album-cover art; busy or saturated patterns.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:albumblock:0` | That Green Gentleman | wall-accent | partly | luxury | green veined stone |
| `chisel:albumblock:1` | Pretty Odd | off-theme | no | - | connected texture |
| `chisel:albumblock:2` | Laminated Denim | off-theme | no | - |  |
| `chisel:albumblock:3` | Dogmatica | off-theme | no | - |  |
| `chisel:albumblock:4` | And the Sky Opened Up | off-theme | no | - |  |
| `chisel:albumblock:5` | Route Guano | off-theme | no | - |  |
| `chisel:albumblock:6` | A Spell Over the West | off-theme | no | - |  |
| `chisel:albumblock:7` | I Built With Someone In Fall Out Boy And All I Got Was This Stupid Block Made About Me | off-theme | no | - |  |
| `chisel:albumblock:8` | Crumbling Castle | off-theme | no | - |  |
| `chisel:albumblock:9` | Evil Star | off-theme | no | - | connected texture |
| `chisel:albumblock:10` | Gaia | off-theme | no | - |  |
| `chisel:albumblock:11` | Spacesick | off-theme | no | - |  |
| `chisel:albumblock:12` | Reactor | off-theme | no | - |  |
| `chisel:albumblock:13` | Resignation | wall-main | partly | standard, modern | plain gray |
| `chisel:albumblock:14` | Dead! | off-theme | no | - |  |
| `chisel:albumblock:15` | Having Too Much Fun | off-theme | no | - |  |
| `chisel:albumblock2:0` | The Cork Tree | off-theme | no | - |  |
| `chisel:albumblock2:1` | Cooperstown | wall-accent | partly | standard | plain red |
| `chisel:albumblock2:2` | The Most Fun a Girl Can Have | wall-accent | partly | standard | dark red |

#### `BRUTALISM` feature: `chisel:brutalism`

Cast cement.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:brutalism:0` | Plain Cement | wall-main | yes | standard, modern, industrial |  |
| `chisel:brutalism:1` | Cement Bricks | wall-main | yes | standard, modern, industrial | cast cement |
| `chisel:brutalism:2` | Leafy Ecobrutalism | wall-main, foliage | yes | overgrown | cement covered in leaves |
| `chisel:brutalism:3` | Flowering Ecobrutalism | wall-main, foliage | yes | overgrown | leaves with flowers |
| `chisel:brutalism:4` | Faux Window | special | yes | standard, ruin | solid faux window: a boarded-up or dead window that needs no glass; connected texture |
| `chisel:brutalism:5` | Heavily Weathered Cement | rubble-ruin, wall-main | yes | ruin | cast cement |
| `chisel:brutalism:6` | Heavily Weathered Cement Bricks | rubble-ruin, wall-main | yes | ruin | cast cement |
| `chisel:brutalism:7` | Reinforced Cement | rubble-ruin, wall-accent | yes | ruin, industrial | exposed rebar |
| `chisel:brutalism:8` | Rimmed Cement | wall-accent, trim-pillar | yes | standard, modern, industrial | cast cement |
| `chisel:brutalism:9` | Lightly Weathered Cement | wall-main | yes | standard, modern, ruin | cast cement |

#### `PORCELAIN` feature: `chisel:porcelain`

Glazed porcelain.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:porcelain:0` | Plain Porcelain | wall-main, floor | partly | modern | glazed porcelain |
| `chisel:porcelain:1` | Cracked Porcelain | rubble-ruin | partly | ruin | glazed porcelain |
| `chisel:porcelain:2` | Kintsugi | wall-accent | partly | modern, asian |  |
| `chisel:porcelain:3` | Blue Porcelain Tiles | floor, wall-accent | yes | standard, modern | blue-grout tile: bathroom or kitchen floor |
| `chisel:porcelain:4` | Red Porcelain Tiles | floor, wall-accent | yes | standard, modern | red-grout tile |
| `chisel:porcelain:5` | Blue Porcelain Fish | furniture-deco, wall-accent | partly | asian | glazed porcelain |
| `chisel:porcelain:6` | Blue Porcelain Flower | furniture-deco, wall-accent | partly | asian | glazed porcelain |
| `chisel:porcelain:7` | Red Porcelain Dragon | furniture-deco, wall-accent | partly | asian | glazed porcelain |
| `chisel:porcelain:9` | Red Porcelain Pillar | trim-pillar | partly | asian | glazed porcelain |
| `chisel:porcelain:10` | Red And Blue Porcelain Tiles | floor | yes | standard, modern | glazed porcelain |
| `chisel:porcelain:11` | Kintsugi Tiles | floor | partly | modern, luxury | gold-grout tile |
| `chisel:porcelain:12` | Polished Porcelain | floor | yes | standard, modern | glazed porcelain |
| `chisel:porcelain:13` | Faux Ivory | wall-main | partly | modern, luxury | glazed porcelain |

#### `ALABASTER` feature: `chisel:alabaster`

Plain pastel stone.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:alabaster:0` | Calcite-Rich | wall-main | yes | modern, standard | near-white |
| `chisel:alabaster:1` | Chartreuse | wall-main | partly | modern, standard |  |
| `chisel:alabaster:2` | Lavender | wall-main | partly | modern, standard |  |
| `chisel:alabaster:3` | Lilac | wall-main | partly | modern, standard |  |
| `chisel:alabaster:4` | Mauve | wall-main | partly | modern, standard |  |
| `chisel:alabaster:5` | French Grey | wall-main | yes | modern, standard | light gray |
| `chisel:alabaster:6` | Peach | wall-main | partly | modern, standard |  |
| `chisel:alabaster:7` | Periwinkle | wall-main | partly | modern, standard |  |
| `chisel:alabaster:8` | Sage Green | wall-main | partly | modern, standard |  |
| `chisel:alabaster:9` | Sky Blue | wall-main | partly | modern, standard |  |
| `chisel:alabaster:10` | Cream | wall-main | yes | modern, standard | cream |

#### `NUCRETE` feature: `chisel:nucrete`

Structural concrete; metas 0-7 dark, 8-15 light.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:nucrete:0` | Dark Concrete Breezeblocks | wall-main | yes | standard, industrial, modern | breeze blocks |
| `chisel:nucrete:1` | Dark Concrete Panel | wall-accent | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light; connected texture |
| `chisel:nucrete:2` | Dark Concrete Panel 2 | wall-accent | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light; connected texture |
| `chisel:nucrete:3` | Dark Smooth Concrete | wall-main, sidewalk | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light |
| `chisel:nucrete:4` | Dark Concrete Tiles | floor, sidewalk | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light |
| `chisel:nucrete:5` | Dark Concrete Strip | trim-pillar, wall-accent | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light |
| `chisel:nucrete:6` | Dark Concrete Caution Tape | wall-accent | yes | industrial | structural concrete; metas 0-7 dark, 8-15 light; connected texture |
| `chisel:nucrete:7` | Dark Concrete Tiles | floor, wall-main | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light; pattern spans 3x3 blocks |
| `chisel:nucrete:8` | Light Concrete Breezeblocks | wall-main | yes | standard, industrial, modern | breeze blocks |
| `chisel:nucrete:9` | Light Concrete Panel | wall-accent | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light; connected texture |
| `chisel:nucrete:10` | Light Concrete Panel 2 | wall-accent | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light; connected texture |
| `chisel:nucrete:11` | Light Smooth Concrete | wall-main, sidewalk | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light |
| `chisel:nucrete:12` | Light Concrete Tiles | floor, sidewalk | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light |
| `chisel:nucrete:13` | Light Concrete Strip | trim-pillar, wall-accent | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light |
| `chisel:nucrete:14` | Light Concrete Caution Tape | wall-accent | yes | industrial | structural concrete; metas 0-7 dark, 8-15 light; connected texture |
| `chisel:nucrete:15` | Light Concrete Tiles | floor, wall-main | yes | standard, industrial, modern | structural concrete; metas 0-7 dark, 8-15 light; pattern spans 3x3 blocks |

### Glass, panes, iron bars, paper walls

Sheets: `glass.png`.

#### `GLASS` feature: `chisel:glass`, `chisel:glass2`

Behavior: Transparent.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:glass:1` | Bubble Glass | glass | partly | standard, border, modern | connected texture |
| `chisel:glass:2` | Chinese Glass | glass | no | asian | red lattice |
| `chisel:glass:3` | Japanese(?) Glass | glass | partly | asian | wood lattice |
| `chisel:glass:4` | Dungeon Glass | glass | partly | ruin, gothic | dirty, barred |
| `chisel:glass:5` | Light Glass | glass | yes | standard, border, modern | connected texture |
| `chisel:glass:6` | Borderless Glass | glass | yes | standard, border, modern | connected texture |
| `chisel:glass:7` | Ornate Steel Glass | glass | partly | luxury | connected texture |
| `chisel:glass:8` | Screen | glass, fence-rail | partly | industrial | fine mesh screen |
| `chisel:glass:9` | Shale Glass | glass | yes | standard, border, modern | connected texture |
| `chisel:glass:10` | Steel Frame Glass | glass | yes | standard, border, modern | connected texture |
| `chisel:glass:11` | Stone Frame Glass | glass | yes | standard, border, modern | connected texture |
| `chisel:glass:12` | Streak Glass | glass | yes | standard, border, modern | streaked; reads as dirty or old glass |
| `chisel:glass:13` | Thick Grid Glass | glass | yes | standard, industrial |  |
| `chisel:glass:14` | Thin Grid Glass | glass | yes | standard, industrial |  |
| `chisel:glass:15` | Modern Iron Fence | glass, fence-rail | partly | modern |  |
| `chisel:glass2:0` | Asymmetrical Leaded Glass | glass | partly | ancient, luxury | leaded church-style; connected texture |
| `chisel:glass2:1` | Modern Glass | glass | yes | standard, border, modern | modern clear; connected texture |

#### `GLASS_PANE` feature: `chisel:glass_pane`

Behavior: Pane shape; transparent.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:glass_pane:1` | Bubble Glass Pane | pane | yes | standard, border, modern |  |
| `chisel:glass_pane:2` | Borderless Glass Pane | pane | yes | standard, border, modern |  |
| `chisel:glass_pane:3` | Screen Pane | pane, fence-rail | partly | industrial | mesh screen |
| `chisel:glass_pane:4` | Streak Glass Pane | pane | yes | standard, border, modern |  |
| `chisel:glass_pane:12` | Chinese Glass Pane | pane | no | asian |  |
| `chisel:glass_pane:13` | Chinese Glass Pane with Golden Frame | pane | no | asian |  |
| `chisel:glass_pane:14` | Japanese Glass Pane | pane | partly | asian |  |
| `chisel:glass_pane:15` | Ornate Japanese Glass Pane | pane | partly | asian |  |

#### `IRON_BARS` feature: `chisel:iron_bars`

Pane-shaped like vanilla iron bars; varieties for the ":" character and for the "damaged to" block. Behavior: Pane shape.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:iron_bars:1` | Iron Bars without Frame | fence-rail | yes | standard, border, industrial |  |
| `chisel:iron_bars:2` | Menacing Iron Bars | fence-rail | partly | gothic, ruin | pane-shaped like vanilla iron bars; varieties for the ":" character and for the "damaged to" block |
| `chisel:iron_bars:3` | Iron Cage Bars | fence-rail | yes | standard, border, industrial |  |
| `chisel:iron_bars:4` | Menacing Iron Spikes | fence-rail | partly | industrial, gothic | spiked top: security fence |
| `chisel:iron_bars:5` | Thick Iron Grid | fence-rail, pane | yes | standard, border, industrial | grid |
| `chisel:iron_bars:6` | Thin Iron Grid | fence-rail, pane | yes | standard, border, industrial | grid |
| `chisel:iron_bars:7` | Ornate Iron Pane Fence | fence-rail | partly | luxury | pane-shaped like vanilla iron bars; varieties for the ":" character and for the "damaged to" block |
| `chisel:iron_bars:8` | Vertical Iron Bars | fence-rail | yes | standard, border, industrial |  |
| `chisel:iron_bars:9` | Iron Spikes | fence-rail | partly | industrial, gothic | pane-shaped like vanilla iron bars; varieties for the ":" character and for the "damaged to" block |

#### `PAPER_WALL` feature: `chisel:paperwall`, `chisel:paperwall_block`

Shoji screen. Behavior: `paperwall` is pane-shaped.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:paperwall:0` | Boxed Paperwall | pane | partly | asian |  |
| `chisel:paperwall:1` | Middle Striked Paperwall | pane | partly | asian |  |
| `chisel:paperwall:2` | Crossed Paperwall | pane | partly | asian |  |
| `chisel:paperwall:3` | Six Sectioned Paperwall | pane | partly | asian |  |
| `chisel:paperwall:4` | Vertically Striked Paperwall | pane | partly | asian |  |
| `chisel:paperwall:5` | Horizontally Striked Paperwall | pane | partly | asian |  |
| `chisel:paperwall:6` | Floral Adorned Paperwall | pane | partly | asian |  |
| `chisel:paperwall:7` | Plain Paperwall | pane | partly | asian |  |
| `chisel:paperwall:8` | Door Shaped Paperwall | pane | partly | asian |  |
| `chisel:paperwall_block:0` | Boxed Paperwall | wall-accent | partly | asian |  |
| `chisel:paperwall_block:1` | Middle Striked Paperwall | wall-accent | partly | asian |  |
| `chisel:paperwall_block:2` | Crossed Paperwall | wall-accent | partly | asian |  |
| `chisel:paperwall_block:3` | Six Sectioned Paperwall | wall-accent | partly | asian |  |
| `chisel:paperwall_block:4` | Vertically Striked Paperwall | wall-accent | partly | asian |  |
| `chisel:paperwall_block:5` | Horizontally Striked Paperwall | wall-accent | partly | asian |  |
| `chisel:paperwall_block:6` | Floral Adorned Paperwall | wall-accent | partly | asian |  |
| `chisel:paperwall_block:7` | Plain Paperwall | wall-accent | partly | asian |  |
| `chisel:paperwall_block:8` | Door Shaped Paperwall | wall-accent | partly | asian |  |

### Stained glass and panes

Sheets: `glass-stained.png`, `glass-stained-pane.png`.

#### `GLASS_STAINED` and `GLASS_STAINED_PANE` features: `chisel:stained_glass_*`, `chisel:stained_glass_forestry`, `chisel:stained_glass_pane_*`

Chisel packs several colors into each block id, and the block is named after the first color it holds, so `chisel:stained_glass_white` also holds orange, magenta and light blue. Blocks: four colors per `stained_glass_*` (metas 4k to 4k+3 per color: bubble, framed, ornate frame, borderless) and one color per meta in `stained_glass_forestry`; two colors per `stained_glass_pane_*` (metas 0 to 5 and 8 to 13: bubble, framed, ornate frame, borderless, four-pane grid, ornate four-pane grid). Each color is its own carving group with the vanilla stained glass.

| Color | Glass block, metas | Forestry glass meta | Pane block, metas | Fits upstream? | Styles | Upstream use |
|---|---|---|---|---|---|---|
| `white` | `chisel:stained_glass_white` 0-3 | `chisel:stained_glass_forestry:0` | `chisel:stained_glass_pane_white` 0-5 | yes | standard, border | glass_*_white, Y |
| `orange` | `chisel:stained_glass_white` 4-7 | `chisel:stained_glass_forestry:1` | `chisel:stained_glass_pane_white` 8-13 | no | - | - |
| `magenta` | `chisel:stained_glass_white` 8-11 | `chisel:stained_glass_forestry:2` | `chisel:stained_glass_pane_magenta` 0-5 | no | - | - |
| `lightblue` | `chisel:stained_glass_white` 12-15 | `chisel:stained_glass_forestry:3` | `chisel:stained_glass_pane_magenta` 8-13 | yes | standard, border | glass_*_light_blue, Z |
| `yellow` | `chisel:stained_glass_yellow` 0-3 | `chisel:stained_glass_forestry:4` | `chisel:stained_glass_pane_yellow` 0-5 | partly | desert | - |
| `lime` | `chisel:stained_glass_yellow` 4-7 | `chisel:stained_glass_forestry:5` | `chisel:stained_glass_pane_yellow` 8-13 | no | - | - |
| `pink` | `chisel:stained_glass_yellow` 8-11 | `chisel:stained_glass_forestry:6` | `chisel:stained_glass_pane_pink` 0-5 | no | - | - |
| `gray` | `chisel:stained_glass_yellow` 12-15 | `chisel:stained_glass_forestry:7` | `chisel:stained_glass_pane_pink` 8-13 | yes | standard, border | glass_*_gray, Z |
| `lightgray` | `chisel:stained_glass_lightgray` 0-3 | `chisel:stained_glass_forestry:8` | `chisel:stained_glass_pane_lightgray` 0-5 | yes | standard, modern | - |
| `cyan` | `chisel:stained_glass_lightgray` 4-7 | `chisel:stained_glass_forestry:9` | `chisel:stained_glass_pane_lightgray` 8-13 | partly | standard, modern | - |
| `purple` | `chisel:stained_glass_lightgray` 8-11 | `chisel:stained_glass_forestry:10` | `chisel:stained_glass_pane_purple` 0-5 | no | - | - |
| `blue` | `chisel:stained_glass_lightgray` 12-15 | `chisel:stained_glass_forestry:11` | `chisel:stained_glass_pane_purple` 8-13 | yes | standard, border | glass_*_blue, Z |
| `brown` | `chisel:stained_glass_brown` 0-3 | `chisel:stained_glass_forestry:12` | `chisel:stained_glass_pane_brown` 0-5 | partly | ruin | - |
| `green` | `chisel:stained_glass_brown` 4-7 | `chisel:stained_glass_forestry:13` | `chisel:stained_glass_pane_brown` 8-13 | partly | standard | - |
| `red` | `chisel:stained_glass_brown` 8-11 | `chisel:stained_glass_forestry:14` | `chisel:stained_glass_pane_red` 0-5 | no | - | - |
| `black` | `chisel:stained_glass_brown` 12-15 | `chisel:stained_glass_forestry:15` | `chisel:stained_glass_pane_red` 8-13 | partly | modern, industrial | oil rig I |

Behavior: translucent (render pass 1); panes are pane-shaped. No connected textures: every block draws its own frame, so a wall of framed glass reads as a grid of separate panes.

<details><summary>Every stained glass block variant (80 rows)</summary>

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:stained_glass_white:0` | White Bubble Glass | glass | yes | standard, border | bubbled |
| `chisel:stained_glass_white:1` | White Glass Panel | glass | yes | standard, border | framed |
| `chisel:stained_glass_white:2` | White Fancy Glass Panel | glass | yes | standard, border | ornate frame |
| `chisel:stained_glass_white:3` | White Borderless Glass | glass | yes | standard, border | borderless |
| `chisel:stained_glass_white:4` | Orange Bubble Glass | glass | no | - | bubbled |
| `chisel:stained_glass_white:5` | Orange Glass Panel | glass | no | - | framed |
| `chisel:stained_glass_white:6` | Orange Fancy Glass Panel | glass | no | - | ornate frame |
| `chisel:stained_glass_white:7` | Orange Borderless Glass | glass | no | - | borderless |
| `chisel:stained_glass_white:8` | Magenta Bubble Glass | glass | no | - | bubbled |
| `chisel:stained_glass_white:9` | Magenta Glass Panel | glass | no | - | framed |
| `chisel:stained_glass_white:10` | Magenta Fancy Glass Panel | glass | no | - | ornate frame |
| `chisel:stained_glass_white:11` | Magenta Borderless Glass | glass | no | - | borderless |
| `chisel:stained_glass_white:12` | Light Blue Bubble Glass | glass | yes | standard, border | bubbled |
| `chisel:stained_glass_white:13` | Light Blue Glass Panel | glass | yes | standard, border | framed |
| `chisel:stained_glass_white:14` | Light Blue Fancy Glass Panel | glass | yes | standard, border | ornate frame |
| `chisel:stained_glass_white:15` | Light Blue Borderless Glass | glass | yes | standard, border | borderless |
| `chisel:stained_glass_forestry:0` | White Forestry Glass | glass | yes | standard, border | streaked (Forestry style) |
| `chisel:stained_glass_forestry:1` | Orange Forestry Glass | glass | no | - | streaked (Forestry style) |
| `chisel:stained_glass_forestry:2` | Magenta Forestry Glass | glass | no | - | streaked (Forestry style) |
| `chisel:stained_glass_forestry:3` | Light Blue Forestry Glass | glass | yes | standard, border | streaked (Forestry style) |
| `chisel:stained_glass_forestry:4` | Yellow Forestry Glass | glass | partly | desert | streaked (Forestry style) |
| `chisel:stained_glass_forestry:5` | Lime Forestry Glass | glass | no | - | streaked (Forestry style) |
| `chisel:stained_glass_forestry:6` | Pink Forestry Glass | glass | no | - | streaked (Forestry style) |
| `chisel:stained_glass_forestry:7` | Gray Forestry Glass | glass | yes | standard, border | streaked (Forestry style) |
| `chisel:stained_glass_forestry:8` | Light Gray Forestry Glass | glass | yes | standard, modern | streaked (Forestry style) |
| `chisel:stained_glass_forestry:9` | Cyan Forestry Glass | glass | partly | standard, modern | streaked (Forestry style) |
| `chisel:stained_glass_forestry:10` | Purple Forestry Glass | glass | no | - | streaked (Forestry style) |
| `chisel:stained_glass_forestry:11` | Blue Forestry Glass | glass | yes | standard, border | streaked (Forestry style) |
| `chisel:stained_glass_forestry:12` | Brown Forestry Glass | glass | partly | ruin | streaked (Forestry style) |
| `chisel:stained_glass_forestry:13` | Green Forestry Glass | glass | partly | standard | streaked (Forestry style) |
| `chisel:stained_glass_forestry:14` | Red Forestry Glass | glass | no | - | streaked (Forestry style) |
| `chisel:stained_glass_forestry:15` | Black Forestry Glass | glass | partly | modern, industrial | streaked (Forestry style) |
| `chisel:stained_glass_yellow:0` | Yellow Bubble Glass | glass | partly | desert | bubbled |
| `chisel:stained_glass_yellow:1` | Yellow Glass Panel | glass | partly | desert | framed |
| `chisel:stained_glass_yellow:2` | Yellow Fancy Glass Panel | glass | partly | desert | ornate frame |
| `chisel:stained_glass_yellow:3` | Yellow Borderless Glass | glass | partly | desert | borderless |
| `chisel:stained_glass_yellow:4` | Lime Bubble Glass | glass | no | - | bubbled |
| `chisel:stained_glass_yellow:5` | Lime Glass Panel | glass | no | - | framed |
| `chisel:stained_glass_yellow:6` | Lime Fancy Glass Panel | glass | no | - | ornate frame |
| `chisel:stained_glass_yellow:7` | Lime Borderless Glass | glass | no | - | borderless |
| `chisel:stained_glass_yellow:8` | Pink Bubble Glass | glass | no | - | bubbled |
| `chisel:stained_glass_yellow:9` | Pink Glass Panel | glass | no | - | framed |
| `chisel:stained_glass_yellow:10` | Pink Fancy Glass Panel | glass | no | - | ornate frame |
| `chisel:stained_glass_yellow:11` | Pink Borderless Glass | glass | no | - | borderless |
| `chisel:stained_glass_yellow:12` | Gray Bubble Glass | glass | yes | standard, border | bubbled |
| `chisel:stained_glass_yellow:13` | Gray Glass Panel | glass | yes | standard, border | framed |
| `chisel:stained_glass_yellow:14` | Gray Fancy Glass Panel | glass | yes | standard, border | ornate frame |
| `chisel:stained_glass_yellow:15` | Gray Borderless Glass | glass | yes | standard, border | borderless |
| `chisel:stained_glass_lightgray:0` | Light Gray Bubble Glass | glass | yes | standard, modern | bubbled |
| `chisel:stained_glass_lightgray:1` | Light Gray Glass Panel | glass | yes | standard, modern | framed |
| `chisel:stained_glass_lightgray:2` | Light Gray Fancy Glass Panel | glass | yes | standard, modern | ornate frame |
| `chisel:stained_glass_lightgray:3` | Light Gray Borderless Glass | glass | yes | standard, modern | borderless |
| `chisel:stained_glass_lightgray:4` | Cyan Bubble Glass | glass | partly | standard, modern | bubbled |
| `chisel:stained_glass_lightgray:5` | Cyan Glass Panel | glass | partly | standard, modern | framed |
| `chisel:stained_glass_lightgray:6` | Cyan Fancy Glass Panel | glass | partly | standard, modern | ornate frame |
| `chisel:stained_glass_lightgray:7` | Cyan Borderless Glass | glass | partly | standard, modern | borderless |
| `chisel:stained_glass_lightgray:8` | Purple Bubble Glass | glass | no | - | bubbled |
| `chisel:stained_glass_lightgray:9` | Purple Glass Panel | glass | no | - | framed |
| `chisel:stained_glass_lightgray:10` | Purple Fancy Glass Panel | glass | no | - | ornate frame |
| `chisel:stained_glass_lightgray:11` | Purple Borderless Glass | glass | no | - | borderless |
| `chisel:stained_glass_lightgray:12` | Blue Bubble Glass | glass | yes | standard, border | bubbled |
| `chisel:stained_glass_lightgray:13` | Blue Glass Panel | glass | yes | standard, border | framed |
| `chisel:stained_glass_lightgray:14` | Blue Fancy Glass Panel | glass | yes | standard, border | ornate frame |
| `chisel:stained_glass_lightgray:15` | Blue Borderless Glass | glass | yes | standard, border | borderless |
| `chisel:stained_glass_brown:0` | Brown Bubble Glass | glass | partly | ruin | bubbled |
| `chisel:stained_glass_brown:1` | Brown Glass Panel | glass | partly | ruin | framed |
| `chisel:stained_glass_brown:2` | Brown Fancy Glass Panel | glass | partly | ruin | ornate frame |
| `chisel:stained_glass_brown:3` | Brown Borderless Glass | glass | partly | ruin | borderless |
| `chisel:stained_glass_brown:4` | Green Bubble Glass | glass | partly | standard | bubbled |
| `chisel:stained_glass_brown:5` | Green Glass Panel | glass | partly | standard | framed |
| `chisel:stained_glass_brown:6` | Green Fancy Glass Panel | glass | partly | standard | ornate frame |
| `chisel:stained_glass_brown:7` | Green Borderless Glass | glass | partly | standard | borderless |
| `chisel:stained_glass_brown:8` | Red Bubble Glass | glass | no | - | bubbled |
| `chisel:stained_glass_brown:9` | Red Glass Panel | glass | no | - | framed |
| `chisel:stained_glass_brown:10` | Red Fancy Glass Panel | glass | no | - | ornate frame |
| `chisel:stained_glass_brown:11` | Red Borderless Glass | glass | no | - | borderless |
| `chisel:stained_glass_brown:12` | Black Bubble Glass | glass | partly | modern, industrial | bubbled |
| `chisel:stained_glass_brown:13` | Black Glass Panel | glass | partly | modern, industrial | framed |
| `chisel:stained_glass_brown:14` | Black Fancy Glass Panel | glass | partly | modern, industrial | ornate frame |
| `chisel:stained_glass_brown:15` | Black Borderless Glass | glass | partly | modern, industrial | borderless |

</details>

<details><summary>Every stained glass pane variant (96 rows)</summary>

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:stained_glass_pane_white:0` | White Bubble Glass | pane | yes | standard, border | bubbled |
| `chisel:stained_glass_pane_white:1` | White Glass Panel | pane | yes | standard, border | framed |
| `chisel:stained_glass_pane_white:2` | White Fancy Glass Panel | pane | yes | standard, border | ornate frame |
| `chisel:stained_glass_pane_white:3` | White Borderless Glass | pane | yes | standard, border | borderless |
| `chisel:stained_glass_pane_white:4` | White Quadrant Glass | pane | yes | standard, border | four-pane grid |
| `chisel:stained_glass_pane_white:5` | White Fancy Quadrant Glass | pane | yes | standard, border | four-pane ornate grid |
| `chisel:stained_glass_pane_white:8` | Orange Bubble Glass | pane | no | - | bubbled |
| `chisel:stained_glass_pane_white:9` | Orange Glass Panel | pane | no | - | framed |
| `chisel:stained_glass_pane_white:10` | Orange Fancy Glass Panel | pane | no | - | ornate frame |
| `chisel:stained_glass_pane_white:11` | Orange Borderless Glass | pane | no | - | borderless |
| `chisel:stained_glass_pane_white:12` | Orange Quadrant Glass | pane | no | - | four-pane grid |
| `chisel:stained_glass_pane_white:13` | Orange Fancy Quadrant Glass | pane | no | - | four-pane ornate grid |
| `chisel:stained_glass_pane_magenta:0` | Magenta Bubble Glass | pane | no | - | bubbled |
| `chisel:stained_glass_pane_magenta:1` | Magenta Glass Panel | pane | no | - | framed |
| `chisel:stained_glass_pane_magenta:2` | Magenta Fancy Glass Panel | pane | no | - | ornate frame |
| `chisel:stained_glass_pane_magenta:3` | Magenta Borderless Glass | pane | no | - | borderless |
| `chisel:stained_glass_pane_magenta:4` | Magenta Quadrant Glass | pane | no | - | four-pane grid |
| `chisel:stained_glass_pane_magenta:5` | Magenta Fancy Quadrant Glass | pane | no | - | four-pane ornate grid |
| `chisel:stained_glass_pane_magenta:8` | Light Blue Bubble Glass | pane | yes | standard, border | bubbled |
| `chisel:stained_glass_pane_magenta:9` | Light Blue Glass Panel | pane | yes | standard, border | framed |
| `chisel:stained_glass_pane_magenta:10` | Light Blue Fancy Glass Panel | pane | yes | standard, border | ornate frame |
| `chisel:stained_glass_pane_magenta:11` | Light Blue Borderless Glass | pane | yes | standard, border | borderless |
| `chisel:stained_glass_pane_magenta:12` | Light Blue Quadrant Glass | pane | yes | standard, border | four-pane grid |
| `chisel:stained_glass_pane_magenta:13` | Light Blue Fancy Quadrant Glass | pane | yes | standard, border | four-pane ornate grid |
| `chisel:stained_glass_pane_yellow:0` | Yellow Bubble Glass | pane | partly | desert | bubbled |
| `chisel:stained_glass_pane_yellow:1` | Yellow Glass Panel | pane | partly | desert | framed |
| `chisel:stained_glass_pane_yellow:2` | Yellow Fancy Glass Panel | pane | partly | desert | ornate frame |
| `chisel:stained_glass_pane_yellow:3` | Yellow Borderless Glass | pane | partly | desert | borderless |
| `chisel:stained_glass_pane_yellow:4` | Yellow Quadrant Glass | pane | partly | desert | four-pane grid |
| `chisel:stained_glass_pane_yellow:5` | Yellow Fancy Quadrant Glass | pane | partly | desert | four-pane ornate grid |
| `chisel:stained_glass_pane_yellow:8` | Lime Bubble Glass | pane | no | - | bubbled |
| `chisel:stained_glass_pane_yellow:9` | Lime Glass Panel | pane | no | - | framed |
| `chisel:stained_glass_pane_yellow:10` | Lime Fancy Glass Panel | pane | no | - | ornate frame |
| `chisel:stained_glass_pane_yellow:11` | Lime Borderless Glass | pane | no | - | borderless |
| `chisel:stained_glass_pane_yellow:12` | Lime Quadrant Glass | pane | no | - | four-pane grid |
| `chisel:stained_glass_pane_yellow:13` | Lime Fancy Quadrant Glass | pane | no | - | four-pane ornate grid |
| `chisel:stained_glass_pane_pink:0` | Pink Bubble Glass | pane | no | - | bubbled |
| `chisel:stained_glass_pane_pink:1` | Pink Glass Panel | pane | no | - | framed |
| `chisel:stained_glass_pane_pink:2` | Pink Fancy Glass Panel | pane | no | - | ornate frame |
| `chisel:stained_glass_pane_pink:3` | Pink Borderless Glass | pane | no | - | borderless |
| `chisel:stained_glass_pane_pink:4` | Pink Quadrant Glass | pane | no | - | four-pane grid |
| `chisel:stained_glass_pane_pink:5` | Pink Fancy Quadrant Glass | pane | no | - | four-pane ornate grid |
| `chisel:stained_glass_pane_pink:8` | Gray Bubble Glass | pane | yes | standard, border | bubbled |
| `chisel:stained_glass_pane_pink:9` | Gray Glass Panel | pane | yes | standard, border | framed |
| `chisel:stained_glass_pane_pink:10` | Gray Fancy Glass Panel | pane | yes | standard, border | ornate frame |
| `chisel:stained_glass_pane_pink:11` | Gray Borderless Glass | pane | yes | standard, border | borderless |
| `chisel:stained_glass_pane_pink:12` | Gray Quadrant Glass | pane | yes | standard, border | four-pane grid |
| `chisel:stained_glass_pane_pink:13` | Gray Fancy Quadrant Glass | pane | yes | standard, border | four-pane ornate grid |
| `chisel:stained_glass_pane_lightgray:0` | Light Gray Bubble Glass | pane | yes | standard, modern | bubbled |
| `chisel:stained_glass_pane_lightgray:1` | Light Gray Glass Panel | pane | yes | standard, modern | framed |
| `chisel:stained_glass_pane_lightgray:2` | Light Gray Fancy Glass Panel | pane | yes | standard, modern | ornate frame |
| `chisel:stained_glass_pane_lightgray:3` | Light Gray Borderless Glass | pane | yes | standard, modern | borderless |
| `chisel:stained_glass_pane_lightgray:4` | Light Gray Quadrant Glass | pane | yes | standard, modern | four-pane grid |
| `chisel:stained_glass_pane_lightgray:5` | Light Gray Fancy Quadrant Glass | pane | yes | standard, modern | four-pane ornate grid |
| `chisel:stained_glass_pane_lightgray:8` | Cyan Bubble Glass | pane | partly | standard, modern | bubbled |
| `chisel:stained_glass_pane_lightgray:9` | Cyan Glass Panel | pane | partly | standard, modern | framed |
| `chisel:stained_glass_pane_lightgray:10` | Cyan Fancy Glass Panel | pane | partly | standard, modern | ornate frame |
| `chisel:stained_glass_pane_lightgray:11` | Cyan Borderless Glass | pane | partly | standard, modern | borderless |
| `chisel:stained_glass_pane_lightgray:12` | Cyan Quadrant Glass | pane | partly | standard, modern | four-pane grid |
| `chisel:stained_glass_pane_lightgray:13` | Cyan Fancy Quadrant Glass | pane | partly | standard, modern | four-pane ornate grid |
| `chisel:stained_glass_pane_purple:0` | Purple Bubble Glass | pane | no | - | bubbled |
| `chisel:stained_glass_pane_purple:1` | Purple Glass Panel | pane | no | - | framed |
| `chisel:stained_glass_pane_purple:2` | Purple Fancy Glass Panel | pane | no | - | ornate frame |
| `chisel:stained_glass_pane_purple:3` | Purple Borderless Glass | pane | no | - | borderless |
| `chisel:stained_glass_pane_purple:4` | Purple Quadrant Glass | pane | no | - | four-pane grid |
| `chisel:stained_glass_pane_purple:5` | Purple Fancy Quadrant Glass | pane | no | - | four-pane ornate grid |
| `chisel:stained_glass_pane_purple:8` | Blue Bubble Glass | pane | yes | standard, border | bubbled |
| `chisel:stained_glass_pane_purple:9` | Blue Glass Panel | pane | yes | standard, border | framed |
| `chisel:stained_glass_pane_purple:10` | Blue Fancy Glass Panel | pane | yes | standard, border | ornate frame |
| `chisel:stained_glass_pane_purple:11` | Blue Borderless Glass | pane | yes | standard, border | borderless |
| `chisel:stained_glass_pane_purple:12` | Blue Quadrant Glass | pane | yes | standard, border | four-pane grid |
| `chisel:stained_glass_pane_purple:13` | Blue Fancy Quadrant Glass | pane | yes | standard, border | four-pane ornate grid |
| `chisel:stained_glass_pane_brown:0` | Brown Bubble Glass | pane | partly | ruin | bubbled |
| `chisel:stained_glass_pane_brown:1` | Brown Glass Panel | pane | partly | ruin | framed |
| `chisel:stained_glass_pane_brown:2` | Brown Fancy Glass Panel | pane | partly | ruin | ornate frame |
| `chisel:stained_glass_pane_brown:3` | Brown Borderless Glass | pane | partly | ruin | borderless |
| `chisel:stained_glass_pane_brown:4` | Brown Quadrant Glass | pane | partly | ruin | four-pane grid |
| `chisel:stained_glass_pane_brown:5` | Brown Fancy Quadrant Glass | pane | partly | ruin | four-pane ornate grid |
| `chisel:stained_glass_pane_brown:8` | Green Bubble Glass | pane | partly | standard | bubbled |
| `chisel:stained_glass_pane_brown:9` | Green Glass Panel | pane | partly | standard | framed |
| `chisel:stained_glass_pane_brown:10` | Green Fancy Glass Panel | pane | partly | standard | ornate frame |
| `chisel:stained_glass_pane_brown:11` | Green Borderless Glass | pane | partly | standard | borderless |
| `chisel:stained_glass_pane_brown:12` | Green Quadrant Glass | pane | partly | standard | four-pane grid |
| `chisel:stained_glass_pane_brown:13` | Green Fancy Quadrant Glass | pane | partly | standard | four-pane ornate grid |
| `chisel:stained_glass_pane_red:0` | Red Bubble Glass | pane | no | - | bubbled |
| `chisel:stained_glass_pane_red:1` | Red Glass Panel | pane | no | - | framed |
| `chisel:stained_glass_pane_red:2` | Red Fancy Glass Panel | pane | no | - | ornate frame |
| `chisel:stained_glass_pane_red:3` | Red Borderless Glass | pane | no | - | borderless |
| `chisel:stained_glass_pane_red:4` | Red Quadrant Glass | pane | no | - | four-pane grid |
| `chisel:stained_glass_pane_red:5` | Red Fancy Quadrant Glass | pane | no | - | four-pane ornate grid |
| `chisel:stained_glass_pane_red:8` | Black Bubble Glass | pane | partly | modern, industrial | bubbled |
| `chisel:stained_glass_pane_red:9` | Black Glass Panel | pane | partly | modern, industrial | framed |
| `chisel:stained_glass_pane_red:10` | Black Fancy Glass Panel | pane | partly | modern, industrial | ornate frame |
| `chisel:stained_glass_pane_red:11` | Black Borderless Glass | pane | partly | modern, industrial | borderless |
| `chisel:stained_glass_pane_red:12` | Black Quadrant Glass | pane | partly | modern, industrial | four-pane grid |
| `chisel:stained_glass_pane_red:13` | Black Fancy Quadrant Glass | pane | partly | modern, industrial | four-pane ornate grid |

</details>

### Mineral blocks

Sheets: `mineral-blocks.png`.

Every group here chisels back to a vanilla storage block, so each variant is nine ingots or gems. The iron and gold groups are accents behind `decorativeMetals.iron` and `decorativeMetals.gold`, under a per-building cap and never whole walls, floors or roofs ([D29](index.md#decorative-metals-d29)); the diamond, emerald, lapis and redstone groups stay loot.

#### `IRON_BLOCK` feature: `chisel:iron_block`, `chisel:iron2`

Beacon base.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:iron_block:0` | Rusted Block | wall-accent | partly | industrial, ruin | rusted |
| `chisel:iron_block:1` | Large Iron Ingots | special | partly | standard | ingot stack: treasure (!) variety |
| `chisel:iron_block:2` | Small Iron Ingots | special | partly | standard | ingot stack: treasure (!) variety |
| `chisel:iron_block:3` | Iron Gears | wall-accent | partly | industrial | gears |
| `chisel:iron_block:4` | Iron Bricks | wall-accent | yes | industrial, standard | beacon base |
| `chisel:iron_block:5` | Iron Plates | wall-accent | yes | industrial, standard | beacon base |
| `chisel:iron_block:6` | Iron Plates with Rivets | wall-accent | yes | industrial, standard | beacon base |
| `chisel:iron_block:7` | Iron Coin Stack Heads-up | special | partly | standard | coin stack |
| `chisel:iron_block:8` | Iron Coin Stack Heads-down | special | partly | standard | coin stack |
| `chisel:iron_block:9` | Dark Iron Crate | furniture-deco | yes | industrial, standard | metal-banded crate |
| `chisel:iron_block:10` | Light Iron Crate | furniture-deco | yes | industrial, standard | metal-banded crate |
| `chisel:iron_block:11` | Iron Block with Moon Decoration | off-theme | no | - | moon emblem |
| `chisel:iron_block:12` | Iron Moon in Purple Obsidian | off-theme | no | - | moon on obsidian |
| `chisel:iron_block:13` | Iron Moon in Obsidian | off-theme | no | - | moon on obsidian |
| `chisel:iron_block:14` | Iron Vents | wall-accent | yes | industrial, standard | vents |
| `chisel:iron_block:15` | Simple Iron Block | wall-accent | yes | standard, industrial | plain iron |
| `chisel:iron2:0` | Caution Stripes | wall-accent | partly | industrial | caution stripes; connected texture |
| `chisel:iron2:1` | Shipping Crate | furniture-deco | partly | industrial | shipping crate; connected texture |
| `chisel:iron2:2` | Thermal | wall-accent | partly | industrial | smooth thermal finish |
| `chisel:iron2:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:iron2:4` | Egregiously Bordered Block | wall-accent | partly | industrial | bordered plate; connected texture |
| `chisel:iron2:5` | Fancy Bolted Plating | wall-accent | partly | industrial | bolted plating |

#### `GOLD_BLOCK` feature: `chisel:gold_block`, `chisel:gold2`

Treasure (!) variety; beacon base.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:gold_block:1` | Large Golden Ingots | special | partly | luxury |  |
| `chisel:gold_block:2` | Small Golden Ingots | special | partly | luxury |  |
| `chisel:gold_block:3` | Golden Bricks | special | partly | luxury |  |
| `chisel:gold_block:4` | Gold Cart | off-theme | no | - | cart |
| `chisel:gold_block:5` | Golden Coin Stack Heads-up | special | partly | luxury |  |
| `chisel:gold_block:6` | Golden Coin Stack Heads-down | special | partly | luxury |  |
| `chisel:gold_block:7` | Dark Gold Crate | special | partly | luxury |  |
| `chisel:gold_block:8` | Light Gold Crate | special | partly | luxury |  |
| `chisel:gold_block:9` | Golden Plates | special | partly | luxury |  |
| `chisel:gold_block:10` | Gold Plates with Rivets | special | partly | luxury |  |
| `chisel:gold_block:11` | Gold Block with Star Decoration | special | partly | luxury |  |
| `chisel:gold_block:12` | Golden Star in Purple Obsidian | off-theme | no | - | star on obsidian |
| `chisel:gold_block:13` | Golden Star in Obsidian | off-theme | no | - | star on obsidian |
| `chisel:gold_block:14` | Simple Gold Block | special | partly | luxury | plain |
| `chisel:gold_block:15` | Modern Gold Block | special | partly | luxury | plain, bright |
| `chisel:gold2:0` | Caution Stripes | special | partly | luxury | connected texture |
| `chisel:gold2:1` | Shipping Crate | special | partly | luxury | connected texture |
| `chisel:gold2:2` | Thermal | special | partly | luxury |  |
| `chisel:gold2:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:gold2:4` | Egregiously Bordered Block | special | partly | luxury | connected texture |
| `chisel:gold2:5` | Fancy Bolted Plating | special | partly | luxury |  |

#### `DIAMOND_BLOCK` feature: `chisel:diamond_block`

Treasure (!) variety; beacon base.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:diamond_block:1` | Embossed Diamond Block | special | partly | luxury |  |
| `chisel:diamond_block:2` | Diamond Block with Panel | special | partly | luxury |  |
| `chisel:diamond_block:3` | Diamond Cells | special | partly | luxury | pattern spans 2x2 blocks |
| `chisel:diamond_block:4` | Diamonds in Purple Obsidian | off-theme | no | - | on obsidian |
| `chisel:diamond_block:5` | Diamonds in Obsidian | off-theme | no | - | on obsidian |
| `chisel:diamond_block:6` | Simple Diamond Block | special | partly | luxury |  |
| `chisel:diamond_block:7` | Bismuth Diamond Block | off-theme | no | - | bismuth pattern |
| `chisel:diamond_block:8` | Crushed Diamond | special | partly | luxury |  |
| `chisel:diamond_block:9` | Small Diamond Blocks | special | partly | luxury |  |
| `chisel:diamond_block:10` | Small Ornate Diamond Blocks | special | partly | luxury |  |
| `chisel:diamond_block:11` | Zelda Diamond Block | special | partly | luxury |  |
| `chisel:diamond_block:12` | Diamond Block with Ornate Layer | special | partly | luxury |  |
| `chisel:diamond_block:13` | Modern Diamond Block | special | partly | luxury |  |

#### `EMERALD_BLOCK` feature: `chisel:emerald_block`

Treasure (!) variety; beacon base.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:emerald_block:1` | Emerald Panel | special | partly | luxury |  |
| `chisel:emerald_block:2` | Classic emerald Panel | special | partly | luxury |  |
| `chisel:emerald_block:3` | Smooth Emerald | special | partly | luxury |  |
| `chisel:emerald_block:4` | Emerald Chunk | special | partly | luxury |  |
| `chisel:emerald_block:5` | Emerald with Ornate Layer | special | partly | luxury |  |
| `chisel:emerald_block:6` | Zelda Emerald Block | special | partly | luxury |  |
| `chisel:emerald_block:7` | Emerald Cell | special | partly | luxury |  |
| `chisel:emerald_block:8` | Emerald Bismuth | special | partly | luxury |  |
| `chisel:emerald_block:9` | Small Emerald Blocks | special | partly | luxury |  |
| `chisel:emerald_block:10` | Small Ornate Emerald Blocks | special | partly | luxury |  |
| `chisel:emerald_block:11` | Ornate Emerald Block | special | partly | luxury |  |
| `chisel:emerald_block:12` | Modern Emerald Block | special | partly | luxury |  |

#### `LAPIS_BLOCK` feature: `chisel:lapis_block`

Saturated blue; no upstream use.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:lapis_block:1` | Chunky Lapis Block | off-theme | no | - |  |
| `chisel:lapis_block:2` | Dark Lapis Block | off-theme | no | - |  |
| `chisel:lapis_block:3` | Zelda Lapis Block | off-theme | no | - |  |
| `chisel:lapis_block:4` | Ornate Lapis Block | off-theme | no | - |  |
| `chisel:lapis_block:5` | Lapis Tile | wall-accent | partly | luxury | saturated blue; no upstream use |
| `chisel:lapis_block:6` | Lapis Panel | wall-accent | partly | luxury | saturated blue; no upstream use |
| `chisel:lapis_block:7` | Smooth Lapis | off-theme | no | - |  |
| `chisel:lapis_block:8` | Lapis with Ornate Layer | off-theme | no | - |  |
| `chisel:lapis_block:9` | Modern Lapis | off-theme | no | - |  |

#### `REDSTONE_BLOCK` feature: `chisel:redstone_block`

Emits redstone power 15 like the vanilla block: powers adjacent rails, lamps and doors.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:redstone_block:1` | Smooth Redstone | off-theme | no | - |  |
| `chisel:redstone_block:2` | Large Redstone Block | off-theme | no | - | connected texture |
| `chisel:redstone_block:3` | Small Redstone Block | off-theme | no | - |  |
| `chisel:redstone_block:4` | Redstone Bricks | off-theme | no | - |  |
| `chisel:redstone_block:5` | Small Redstone Bricks | off-theme | no | - |  |
| `chisel:redstone_block:6` | Chaotic Redstone Bricks | off-theme | no | - |  |
| `chisel:redstone_block:7` | Chiseled Redstone | off-theme | no | - |  |
| `chisel:redstone_block:8` | Redstone Greek Decoration | off-theme | no | - |  |
| `chisel:redstone_block:9` | Ornate Redstone Tiles | off-theme | no | - |  |
| `chisel:redstone_block:10` | Redstone Pillar | off-theme | no | - |  |
| `chisel:redstone_block:11` | Redstone Tiles | off-theme | no | - |  |
| `chisel:redstone_block:12` | Redstone Circuit | off-theme | no | - |  |
| `chisel:redstone_block:13` | Redstone Supaplex Circuit | off-theme | no | - |  |
| `chisel:redstone_block:14` | Redstone Skulls | off-theme | no | - |  |
| `chisel:redstone_block:15` | Redstone Zelda Block | off-theme | no | - |  |

### Metal blocks

Sheets: `metals-1.png`, `metals-2.png`.

Each group is a decorative metal (D29): accents only, behind one flag per metal and under a per-building cap, never whole walls, floors or roofs ([D29](index.md#decorative-metals-d29)). Tiers are settled there from GT source. Most groups key GT's ore name (`blockCopper`, `blockSteel`, ...) and chisel back to its storage block; the electrum group keys the misspelled `blockElectrun` and the titanium, tungstensteel, rhodium-plated palladium, iridium and osmium groups key `titaniumBlock`-style names that appear nowhere in the pack's sources but Chisel's, so they do not take a storage block. Tungstensteel, rhodium-plated palladium, iridium, osmium, platinum and uranium are above EV and never placed.

#### `ALUMINUM` feature: `chisel:aluminumblock`, `chisel:aluminum_stairs`

Beacon base; named after a GT material: fits a GTNH tier-themed district; also has stairs. Stairs: each `_stairs.N` block holds two variants, item meta 0 for variant 2N and 8 for variant 2N+1; in the world the low three bits are the stair facing and half, as for vanilla stairs.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:aluminumblock:0` | Caution Stripes | wall-accent | partly | industrial | caution stripes; connected texture |
| `chisel:aluminumblock:1` | Shipping Crate | furniture-deco | partly | industrial | shipping crate; connected texture |
| `chisel:aluminumblock:2` | Thermal | wall-accent | partly | industrial | smooth thermal finish |
| `chisel:aluminumblock:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:aluminumblock:4` | Egregiously Bordered Block | wall-accent | partly | industrial | bordered plate; connected texture |
| `chisel:aluminumblock:5` | Fancy Bolted Plating | wall-accent | partly | industrial | bolted plating |
| `chisel:aluminum_stairs.0:0` | Caution Stripes | stairs-slab | partly | industrial | stairs of `chisel:aluminumblock:0` (Caution Stripes); connected texture |
| `chisel:aluminum_stairs.0:8` | Shipping Crate | stairs-slab | partly | industrial | stairs of `chisel:aluminumblock:1` (Shipping Crate); connected texture |
| `chisel:aluminum_stairs.1:0` | Thermal | stairs-slab | partly | industrial | stairs of `chisel:aluminumblock:2` (Thermal) |
| `chisel:aluminum_stairs.1:8` | An Old Relic from the land of Oneteufyv | stairs-slab | no | - | stairs of `chisel:aluminumblock:3` (An Old Relic from the land of Oneteufyv) |
| `chisel:aluminum_stairs.2:0` | Egregiously Bordered Block | stairs-slab | partly | industrial | stairs of `chisel:aluminumblock:4` (Egregiously Bordered Block); connected texture |
| `chisel:aluminum_stairs.2:8` | Fancy Bolted Plating | stairs-slab | partly | industrial | stairs of `chisel:aluminumblock:5` (Fancy Bolted Plating) |

#### `BRONZE` feature: `chisel:bronzeblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:bronzeblock:0` | Caution Stripes | wall-accent | partly | industrial | caution stripes; connected texture |
| `chisel:bronzeblock:1` | Shipping Crate | furniture-deco | partly | industrial | shipping crate; connected texture |
| `chisel:bronzeblock:2` | Thermal | wall-accent | partly | industrial | smooth thermal finish |
| `chisel:bronzeblock:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:bronzeblock:4` | Egregiously Bordered Block | wall-accent | partly | industrial | bordered plate; connected texture |
| `chisel:bronzeblock:5` | Fancy Bolted Plating | wall-accent | partly | industrial | bolted plating |

#### `COPPER` feature: `chisel:copperblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:copperblock:0` | Caution Stripes | wall-accent | partly | industrial | caution stripes; connected texture |
| `chisel:copperblock:1` | Shipping Crate | furniture-deco | partly | industrial | shipping crate; connected texture |
| `chisel:copperblock:2` | Thermal | wall-accent | partly | industrial | smooth thermal finish |
| `chisel:copperblock:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:copperblock:4` | Egregiously Bordered Block | wall-accent | partly | industrial | bordered plate; connected texture |
| `chisel:copperblock:5` | Fancy Bolted Plating | wall-accent | partly | industrial | bolted plating |

#### `LEAD` feature: `chisel:leadblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:leadblock:0` | Caution Stripes | wall-accent | partly | industrial | caution stripes; connected texture |
| `chisel:leadblock:1` | Shipping Crate | furniture-deco | partly | industrial | shipping crate; connected texture |
| `chisel:leadblock:2` | Thermal | wall-accent | partly | industrial | smooth thermal finish |
| `chisel:leadblock:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:leadblock:4` | Egregiously Bordered Block | wall-accent | partly | industrial | bordered plate; connected texture |
| `chisel:leadblock:5` | Fancy Bolted Plating | wall-accent | partly | industrial | bolted plating |

#### `SILVER` feature: `chisel:silverblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:silverblock:0` | Caution Stripes | wall-accent | partly | industrial | caution stripes; connected texture |
| `chisel:silverblock:1` | Shipping Crate | furniture-deco | partly | industrial | shipping crate; connected texture |
| `chisel:silverblock:2` | Thermal | wall-accent | partly | industrial | smooth thermal finish |
| `chisel:silverblock:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:silverblock:4` | Egregiously Bordered Block | wall-accent | partly | industrial | bordered plate; connected texture |
| `chisel:silverblock:5` | Fancy Bolted Plating | wall-accent | partly | industrial | bolted plating |

#### `STEEL` feature: `chisel:steelblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:steelblock:0` | Caution Stripes | wall-accent | partly | industrial | caution stripes; connected texture |
| `chisel:steelblock:1` | Shipping Crate | furniture-deco | partly | industrial | shipping crate; connected texture |
| `chisel:steelblock:2` | Thermal | wall-accent | partly | industrial | smooth thermal finish |
| `chisel:steelblock:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:steelblock:4` | Egregiously Bordered Block | wall-accent | partly | industrial | bordered plate; connected texture |
| `chisel:steelblock:5` | Fancy Bolted Plating | wall-accent | partly | industrial | bolted plating |

#### `TIN` feature: `chisel:tinblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:tinblock:0` | Caution Stripes | wall-accent | partly | industrial | caution stripes; connected texture |
| `chisel:tinblock:1` | Shipping Crate | furniture-deco | partly | industrial | shipping crate; connected texture |
| `chisel:tinblock:2` | Thermal | wall-accent | partly | industrial | smooth thermal finish |
| `chisel:tinblock:3` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:tinblock:4` | Egregiously Bordered Block | wall-accent | partly | industrial | bordered plate; connected texture |
| `chisel:tinblock:5` | Fancy Bolted Plating | wall-accent | partly | industrial | bolted plating |

#### `URANIUM` feature: `chisel:uraniumblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:uraniumblock:0` | Caution Stripes | wall-accent | no | industrial, scifi | caution stripes; connected texture |
| `chisel:uraniumblock:1` | Shipping Crate | furniture-deco | no | industrial, scifi | shipping crate; connected texture |
| `chisel:uraniumblock:2` | Thermal | wall-accent | no | industrial, scifi | smooth thermal finish |
| `chisel:uraniumblock:3` | An Old Relic from the land of Oneteufyv | off-theme | no | industrial, scifi | relic emblem |
| `chisel:uraniumblock:4` | Egregiously Bordered Block | wall-accent | no | industrial, scifi | bordered plate; connected texture |
| `chisel:uraniumblock:5` | Fancy Bolted Plating | wall-accent | no | industrial, scifi | bolted plating |

#### `ELECTRUM` feature: `chisel:electrumblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:electrumblock:0` | Egregious | wall-accent | no | industrial, luxury | connected texture |
| `chisel:electrumblock:1` | Bolted Casing | wall-accent | no | industrial, luxury |  |
| `chisel:electrumblock:2` | Caution Stripes | wall-accent | no | industrial, luxury | connected texture |
| `chisel:electrumblock:3` | Shipping Crate | furniture-deco | no | industrial, luxury | connected texture |
| `chisel:electrumblock:4` | Machinery | special | no | industrial, luxury |  |
| `chisel:electrumblock:5` | Scaffolding | fence-rail, special | no | industrial, luxury | connected texture |
| `chisel:electrumblock:6` | Thermal | wall-accent | no | industrial, luxury |  |

#### `INVAR` feature: `chisel:invarblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:invarblock:0` | Egregious | wall-accent | partly | industrial | connected texture |
| `chisel:invarblock:1` | Bolted Casing | wall-accent | partly | industrial |  |
| `chisel:invarblock:2` | Caution Stripes | wall-accent | partly | industrial | connected texture |
| `chisel:invarblock:3` | Shipping Crate | furniture-deco | partly | industrial | connected texture |
| `chisel:invarblock:4` | Machinery | special | partly | industrial |  |
| `chisel:invarblock:5` | Scaffolding | fence-rail, special | partly | industrial | connected texture |
| `chisel:invarblock:6` | Thermal | wall-accent | partly | industrial |  |

#### `NICKEL` feature: `chisel:nickelblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:nickelblock:0` | Egregious | wall-accent | no | industrial | connected texture |
| `chisel:nickelblock:1` | Bolted Casing | wall-accent | no | industrial |  |
| `chisel:nickelblock:2` | Caution Stripes | wall-accent | no | industrial | connected texture |
| `chisel:nickelblock:3` | Shipping Crate | furniture-deco | no | industrial | connected texture |
| `chisel:nickelblock:4` | Machinery | special | no | industrial |  |
| `chisel:nickelblock:5` | Scaffolding | fence-rail, special | no | industrial | connected texture |
| `chisel:nickelblock:6` | Thermal | wall-accent | no | industrial |  |

#### `PLATINUM` feature: `chisel:platinumblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:platinumblock:0` | Egregious | wall-accent | no | industrial, scifi | connected texture |
| `chisel:platinumblock:1` | Bolted Casing | wall-accent | no | industrial, scifi |  |
| `chisel:platinumblock:2` | Caution Stripes | wall-accent | no | industrial, scifi | connected texture |
| `chisel:platinumblock:3` | Shipping Crate | furniture-deco | no | industrial, scifi | connected texture |
| `chisel:platinumblock:4` | Machinery | special | no | industrial, scifi |  |
| `chisel:platinumblock:5` | Scaffolding | fence-rail, special | no | industrial, scifi | connected texture |
| `chisel:platinumblock:6` | Thermal | wall-accent | no | industrial, scifi |  |

#### `CAST_IRON` feature: `chisel:castironblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:castironblock:0` | Machinery | special | partly | industrial |  |
| `chisel:castironblock:1` | Egregiously Modern | wall-accent | partly | industrial | connected texture |
| `chisel:castironblock:2` | Bolted Casing | wall-accent | partly | industrial |  |
| `chisel:castironblock:3` | Caution Stripes | wall-accent | partly | industrial | connected texture |
| `chisel:castironblock:4` | Shipping Crate | furniture-deco | partly | industrial | connected texture |
| `chisel:castironblock:5` | Egregious | wall-accent | partly | industrial | connected texture |
| `chisel:castironblock:6` | Scaffolding | fence-rail, special | partly | industrial | connected texture |
| `chisel:castironblock:7` | Thermal | wall-accent | partly | industrial |  |

#### `TITANIUM` feature: `chisel:titaniumblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:titaniumblock:0` | Machinery | special | no | industrial, scifi |  |
| `chisel:titaniumblock:1` | Egregiously Modern | wall-accent | no | industrial, scifi | connected texture |
| `chisel:titaniumblock:2` | Bolted Casing | wall-accent | no | industrial, scifi |  |
| `chisel:titaniumblock:3` | Caution Stripes | wall-accent | no | industrial, scifi | connected texture |
| `chisel:titaniumblock:4` | Shipping Crate | furniture-deco | no | industrial, scifi | connected texture |
| `chisel:titaniumblock:5` | Egregious | wall-accent | no | industrial, scifi | connected texture |
| `chisel:titaniumblock:6` | Scaffolding | fence-rail, special | no | industrial, scifi | connected texture |
| `chisel:titaniumblock:7` | Thermal | wall-accent | no | industrial, scifi |  |

#### `STAINLESS_STEEL` feature: `chisel:stainlesssteelblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:stainlesssteelblock:0` | Machinery | special | partly | industrial, modern |  |
| `chisel:stainlesssteelblock:1` | Egregiously Modern | wall-accent | partly | industrial, modern | connected texture |
| `chisel:stainlesssteelblock:2` | Bolted Casing | wall-accent | partly | industrial, modern |  |
| `chisel:stainlesssteelblock:3` | Caution Stripes | wall-accent | partly | industrial, modern | connected texture |
| `chisel:stainlesssteelblock:4` | Shipping Crate | furniture-deco | partly | industrial, modern | connected texture |
| `chisel:stainlesssteelblock:5` | Egregious | wall-accent | partly | industrial, modern | connected texture |
| `chisel:stainlesssteelblock:6` | Scaffolding | fence-rail, special | partly | industrial, modern | connected texture |
| `chisel:stainlesssteelblock:7` | Thermal | wall-accent | partly | industrial, modern |  |

#### `TUNGSTENSTEEL` feature: `chisel:tungstensteelblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:tungstensteelblock:0` | Machinery | special | no | industrial, scifi |  |
| `chisel:tungstensteelblock:1` | Egregiously Modern | wall-accent | no | industrial, scifi | connected texture |
| `chisel:tungstensteelblock:2` | Bolted Casing | wall-accent | no | industrial, scifi |  |
| `chisel:tungstensteelblock:3` | Caution Stripes | wall-accent | no | industrial, scifi | connected texture |
| `chisel:tungstensteelblock:4` | Shipping Crate | furniture-deco | no | industrial, scifi | connected texture |
| `chisel:tungstensteelblock:5` | Egregious | wall-accent | no | industrial, scifi | connected texture |
| `chisel:tungstensteelblock:6` | Scaffolding | fence-rail, special | no | industrial, scifi | connected texture |
| `chisel:tungstensteelblock:7` | Thermal | wall-accent | no | industrial, scifi |  |

#### `RPP` feature: `chisel:rppblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:rppblock:0` | Machinery | special | no | industrial, scifi |  |
| `chisel:rppblock:1` | Egregiously Modern | wall-accent | no | industrial, scifi | connected texture |
| `chisel:rppblock:2` | Bolted Casing | wall-accent | no | industrial, scifi |  |
| `chisel:rppblock:3` | Caution Stripes | wall-accent | no | industrial, scifi | connected texture |
| `chisel:rppblock:4` | Shipping Crate | furniture-deco | no | industrial, scifi | connected texture |
| `chisel:rppblock:5` | Egregious | wall-accent | no | industrial, scifi | connected texture |
| `chisel:rppblock:6` | Scaffolding | fence-rail, special | no | industrial, scifi | connected texture |
| `chisel:rppblock:7` | Thermal | wall-accent | no | industrial, scifi |  |

#### `IRIDIUM` feature: `chisel:iridiumblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:iridiumblock:0` | Machinery | special | partly | industrial, scifi |  |
| `chisel:iridiumblock:1` | Egregiously Modern | wall-accent | partly | industrial, scifi | connected texture |
| `chisel:iridiumblock:2` | Bolted Casing | wall-accent | partly | industrial, scifi |  |
| `chisel:iridiumblock:3` | Caution Stripes | wall-accent | partly | industrial, scifi | connected texture |
| `chisel:iridiumblock:4` | Shipping Crate | furniture-deco | partly | industrial, scifi | connected texture |
| `chisel:iridiumblock:5` | Egregious | wall-accent | partly | industrial, scifi | connected texture |
| `chisel:iridiumblock:6` | Scaffolding | fence-rail, special | partly | industrial, scifi | connected texture |
| `chisel:iridiumblock:7` | Thermal | wall-accent | partly | industrial, scifi |  |

#### `OSMIUM` feature: `chisel:osmiumblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:osmiumblock:0` | Machinery | special | partly | industrial, scifi |  |
| `chisel:osmiumblock:1` | Egregiously Modern | wall-accent | partly | industrial, scifi | connected texture |
| `chisel:osmiumblock:2` | Bolted Casing | wall-accent | partly | industrial, scifi |  |
| `chisel:osmiumblock:3` | Caution Stripes | wall-accent | partly | industrial, scifi | connected texture |
| `chisel:osmiumblock:4` | Shipping Crate | furniture-deco | partly | industrial, scifi | connected texture |
| `chisel:osmiumblock:5` | Egregious | wall-accent | partly | industrial, scifi | connected texture |
| `chisel:osmiumblock:6` | Scaffolding | fence-rail, special | partly | industrial, scifi | connected texture |
| `chisel:osmiumblock:7` | Thermal | wall-accent | partly | industrial, scifi |  |

#### `COBALT` feature: `chisel:cobaltblock`

Beacon base; named after a GT material: fits a GTNH tier-themed district.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:cobaltblock:0` | Egregious | wall-accent | no | industrial, scifi | connected texture |
| `chisel:cobaltblock:1` | Bolted Casing | wall-accent | no | industrial, scifi |  |
| `chisel:cobaltblock:2` | Caution Stripes | wall-accent | no | industrial, scifi | connected texture |
| `chisel:cobaltblock:3` | Shipping Crate | furniture-deco | no | industrial, scifi | connected texture |
| `chisel:cobaltblock:4` | Machinery | special | no | industrial, scifi |  |
| `chisel:cobaltblock:5` | Scaffolding | fence-rail, special | no | industrial, scifi | connected texture |
| `chisel:cobaltblock:6` | Thermal | wall-accent | no | industrial, scifi |  |

#### `THAUMIUM` feature: `chisel:thaumium`

Thaumcraft purple metal.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:thaumium:0` | Ornate Pattern | off-theme | no | ancient |  |
| `chisel:thaumium:1` | Totem Faces | off-theme | no | ancient | random texture per block |
| `chisel:thaumium:2` | Bricks | off-theme | no | ancient | connected texture |
| `chisel:thaumium:3` | Small Tiles | off-theme | no | ancient |  |
| `chisel:thaumium:4` | Lattice | off-theme | no | ancient |  |
| `chisel:thaumium:5` | Bounded Planks | off-theme | no | ancient | connected texture |

### Industrial: factory, technical, laboratory, sheet metal, warning signs

Sheets: `industrial.png`.

#### `FACTORY` feature: `chisel:factoryblock`, `chisel:factoryblock2`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:factoryblock:0` | Rusty Metal Plate with Dotted Pattern | wall-main, rubble-ruin | yes | industrial, ruin | connected texture |
| `chisel:factoryblock:1` | Rusty Metal Plate | wall-main | yes | industrial | connected texture |
| `chisel:factoryblock:2` | Very Rusty Metal Plate | wall-main, rubble-ruin | yes | industrial, ruin | connected texture |
| `chisel:factoryblock:3` | A Metal Plate with Little Rust | wall-main | yes | industrial | connected texture |
| `chisel:factoryblock:4` | Wireframe | wall-accent, fence-rail | partly | industrial | wireframe grid (solid block); connected texture |
| `chisel:factoryblock:5` | Wireframe in a Shade of Purple | wall-accent | partly | industrial, scifi | connected texture |
| `chisel:factoryblock:6` | Yellow-Black Hazard Block | wall-accent | yes | industrial | yellow-black hazard stripes; connected texture |
| `chisel:factoryblock:7` | Orange-White Hazard Block | wall-accent | yes | industrial | orange-white hazard stripes; connected texture |
| `chisel:factoryblock:8` | Fancy Circuit Block | special | partly | scifi | connected texture |
| `chisel:factoryblock:9` | Fancy Metal Box | furniture-deco | partly | industrial |  |
| `chisel:factoryblock:10` | Gold-Plated Fancy Circuit Block | special | no | scifi | connected texture |
| `chisel:factoryblock:11` | Gold-Plated Rusty Purple Block | special | no | scifi | connected texture |
| `chisel:factoryblock:12` | Shiny Metal Construction Block | wall-accent | yes | industrial | connected texture |
| `chisel:factoryblock:13` | Worn Metal Wall with Ventilation Openings | wall-main | yes | industrial | worn plate with vent slots; pattern spans 3x3 blocks |
| `chisel:factoryblock:14` | Rusty Metal Plates | wall-main, rubble-ruin | yes | industrial, ruin | connected texture |
| `chisel:factoryblock:15` | Worn Metal Column | trim-pillar | yes | industrial | connects vertically |
| `chisel:factoryblock2:0` | Neon Blue Plating | wall-accent | no | scifi | connected texture |
| `chisel:factoryblock2:1` | Vertical Venting | wall-accent | yes | industrial | vertical venting; connects vertically |
| `chisel:factoryblock2:2` | Metallic Blue Circuit Plating | wall-accent | no | scifi | connected texture |
| `chisel:factoryblock2:3` | Blue Wireframing | wall-accent | no | scifi | connected texture |

#### `TECHNICAL` feature: `chisel:technical`, `chisel:technical2`, `chisel:technical3`, `chisel:technical4`

Behavior: `technical2` is transparent. `technical` 4 and 14 and `technical2` 1 and 3 are animated fans.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:technical:0` | Scaffold | fence-rail, special | partly | industrial | scaffold look on an opaque block; connected texture |
| `chisel:technical:1` | Panels with Caution Tape | wall-accent | yes | industrial | connected texture |
| `chisel:technical:2` | An Old Relic from the land of Oneteufyv | off-theme | no | - | relic emblem |
| `chisel:technical:3` | Under-Pipe (Large Pipe) | ceiling, special | partly | industrial | pipes; connected texture |
| `chisel:technical:4` | Fan (Fast) | special | partly | industrial | spinning fan (animated) |
| `chisel:technical:5` | Under-Pipe (Small Pipes) | ceiling, special | partly | industrial | pipes; connected texture |
| `chisel:technical:6` | Fan (Off) | special | partly | industrial | fan (still) |
| `chisel:technical:7` | Vents | wall-accent | yes | industrial | connected texture |
| `chisel:technical:8` | Glowing Vents | wall-accent | yes | industrial | looks lit; emits no light; connected texture |
| `chisel:technical:9` | Insulation | wall-accent | yes | industrial | padded insulation; connected texture |
| `chisel:technical:10` | Gears and Flywheels | special | partly | industrial | gears; connected texture |
| `chisel:technical:11` | Cables | special | partly | industrial | cables; connected texture |
| `chisel:technical:12` | Rusty Panel | wall-main, rubble-ruin | yes | industrial, ruin | random texture per block |
| `chisel:technical:13` | Grate | floor, sidewalk | yes | industrial | grate: catwalk floor; connected texture |
| `chisel:technical:14` | Fan (Malfunctioning) | special | partly | industrial | fan, flickering (animated) |
| `chisel:technical:15` | Rusty Grate | floor, rubble-ruin | yes | industrial, ruin | rusty grate; connected texture |
| `chisel:technical2:0` | Scaffold | fence-rail, special | partly | industrial | transparent block (BlockCarvableGlass): see-through between the frame; connected texture |
| `chisel:technical2:1` | Fan (Fast) | special | partly | industrial | fan (animated) |
| `chisel:technical2:2` | Fan (Off) | special | partly | industrial |  |
| `chisel:technical2:3` | Fan (Malfunctioning) | special | partly | industrial | fan (animated) |
| `chisel:technical3:0` | I'm a big fan of this massive fan! | special | partly | industrial | large fan over 3x3 blocks; pattern spans 3x3 blocks |
| `chisel:technical3:1` | Industrial Armor Plating | wall-main | yes | industrial | armor plating over 3x3 blocks; pattern spans 3x3 blocks |
| `chisel:technical4:0` | Corroded Metal Chunks | rubble-ruin | yes | industrial, ruin | random texture per block |
| `chisel:technical4:1` | Rusted Metal Chunks | rubble-ruin | yes | industrial, ruin | random texture per block |
| `chisel:technical4:2` | Sturdy | wall-main | yes | industrial | connected texture |
| `chisel:technical4:3` | MegaCell™ Battery | off-theme | no | scifi | battery |
| `chisel:technical4:4` | Thermal Exhaust Plating | wall-accent | yes | industrial | connected texture |
| `chisel:technical4:5` | Makeshift Plating | wall-main, rubble-ruin | yes | industrial, ruin | patchwork plating; pattern spans 3x3 blocks |
| `chisel:technical4:6` | Pipe Dream | special | partly | industrial | random texture per block |
| `chisel:technical4:7` | Worn Concrete | wall-main, sidewalk | yes | standard, industrial, ruin | worn concrete with random variation; connected texture |
| `chisel:technical4:8` | Cinder Block | wall-main | yes | standard, industrial | cinder block |
| `chisel:technical4:9` | Cinder Block (Polished) | wall-main | yes | standard, industrial | polished cinder block |
| `chisel:technical4:10` | Cinder Brick | wall-main | yes | standard, industrial | cinder brick |
| `chisel:technical4:11` | Cardboard Box | furniture-deco | yes | standard, industrial | cardboard box |
| `chisel:technical4:12` | Folded Cardboard | furniture-deco | yes | standard, industrial |  |
| `chisel:technical4:13` | Cardboard Scrap | furniture-deco, rubble-ruin | yes | standard, ruin |  |

#### `LABORATORY` feature: `chisel:laboratoryblock`

Clean white enamel and steel: offices, labs, hospitals.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:laboratoryblock:0` | Wall Panel | wall-main, ceiling | yes | standard, modern | clean white enamel and steel: offices, labs, hospitals; connects vertically |
| `chisel:laboratoryblock:1` | Dotted Panel | wall-main, ceiling | yes | standard, modern | clean white enamel and steel: offices, labs, hospitals; connects vertically |
| `chisel:laboratoryblock:2` | Enamelled Wall | wall-main | yes | standard, modern | connected texture |
| `chisel:laboratoryblock:3` | Enamelled Roundel Wall | wall-accent | yes | standard, modern | clean white enamel and steel: offices, labs, hospitals; connected texture |
| `chisel:laboratoryblock:4` | Enamelled Wall Vents | wall-accent | yes | standard, modern | vents; connects horizontally |
| `chisel:laboratoryblock:5` | Large Enamelled Tile | wall-main, floor | yes | standard, modern | clean white enamel and steel: offices, labs, hospitals |
| `chisel:laboratoryblock:6` | Small Enamelled Tiles | floor | yes | standard, modern | clean white enamel and steel: offices, labs, hospitals |
| `chisel:laboratoryblock:7` | Laboratory Floor | floor | yes | standard, modern | clean white enamel and steel: offices, labs, hospitals |
| `chisel:laboratoryblock:8` | Checkerboard Floor | floor | yes | standard, modern | checkerboard |
| `chisel:laboratoryblock:9` | Clear Panel | ceiling | partly | standard, modern | looks like a light panel; emits no light; connected texture |
| `chisel:laboratoryblock:10` | Fuzzy Panel | ceiling | partly | standard, modern | looks like a light panel; emits no light; connected texture |
| `chisel:laboratoryblock:11` | Large Steel Tile | floor | yes | industrial, modern | clean white enamel and steel: offices, labs, hospitals |
| `chisel:laboratoryblock:12` | Small Steel Tiles | floor | yes | industrial, modern | clean white enamel and steel: offices, labs, hospitals |
| `chisel:laboratoryblock:13` | Direction Console (Right) | furniture-deco | partly | scifi, modern | console; connects horizontally |
| `chisel:laboratoryblock:14` | Direction Console (Left) | furniture-deco | partly | scifi, modern | console; connects horizontally |
| `chisel:laboratoryblock:15` | Information Console | furniture-deco | partly | scifi, modern | console; connects horizontally |

#### `SHEETMETAL` feature: `chisel:sheetmetal`

Corrugated and plate sheet metal.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:sheetmetal:0` | Walling | wall-main, roof | yes | industrial |  |
| `chisel:sheetmetal:1` | Siding | wall-main, roof | yes | industrial |  |
| `chisel:sheetmetal:2` | Paneling | wall-main, roof | yes | industrial |  |
| `chisel:sheetmetal:3` | Plating | wall-main, roof | yes | industrial |  |
| `chisel:sheetmetal:4` | Treading | floor, sidewalk | yes | industrial | tread plate |
| `chisel:sheetmetal:5` | Walling (Rusty) | wall-main, rubble-ruin | yes | industrial, ruin | corrugated and plate sheet metal |
| `chisel:sheetmetal:6` | Siding (Rusty) | wall-main, rubble-ruin | yes | industrial, ruin | corrugated and plate sheet metal |
| `chisel:sheetmetal:7` | Paneling (Rusty) | wall-main, rubble-ruin | yes | industrial, ruin | corrugated and plate sheet metal |
| `chisel:sheetmetal:8` | Plating (Rusty) | wall-main, rubble-ruin | yes | industrial, ruin | corrugated and plate sheet metal |
| `chisel:sheetmetal:9` | Treading (Rusty) | floor, rubble-ruin | yes | industrial, ruin | rusty tread plate |

#### `WARNING_SIGN` feature: `chisel:warningSign`

Sign symbol drawn over a plate (two-layer render; the sheet shows the symbol only).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:warningSign:0` | Radiation | wall-accent | partly | industrial |  |
| `chisel:warningSign:1` | Biohazard | wall-accent | partly | industrial |  |
| `chisel:warningSign:2` | Fire | wall-accent | partly | industrial |  |
| `chisel:warningSign:3` | Explosion | wall-accent | partly | industrial |  |
| `chisel:warningSign:4` | Death | wall-accent | partly | industrial |  |
| `chisel:warningSign:5` | Falling Objects | wall-accent | yes | industrial | sign symbol drawn over a plate (two-layer render; the sheet shows the symbol only) |
| `chisel:warningSign:6` | Danger of Falling | wall-accent | yes | industrial | sign symbol drawn over a plate (two-layer render; the sheet shows the symbol only) |
| `chisel:warningSign:7` | High Voltage | wall-accent | yes | industrial | sign symbol drawn over a plate (two-layer render; the sheet shows the symbol only) |
| `chisel:warningSign:8` | Generic Warning | wall-accent | partly | industrial |  |
| `chisel:warningSign:9` | Dangerous Chemicals | wall-accent | partly | industrial |  |
| `chisel:warningSign:10` | Under Construction | wall-accent | yes | standard, industrial | under construction |
| `chisel:warningSign:11` | Loud Sounds | wall-accent | partly | industrial |  |
| `chisel:warningSign:12` | No Entry | wall-accent | yes | standard, industrial | no entry |
| `chisel:warningSign:13` | Cryogenic Freezing in progress | wall-accent | partly | industrial |  |
| `chisel:warningSign:14` | Oxygen Required | wall-accent | partly | industrial |  |

### Sci-fi: futura, hex plating, cubits, glotek, neonite, antiblock

Sheets: `scifi-1.png`, `scifi-2.png`.

#### `FUTURA` feature: `chisel:futura`, `chisel:futuraCircuit`

Behavior: `futuraCircuit`: light 7, animated full-bright overlay. `futura` 2 to 5: animated.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:futura:0` | Glowing Screen with Metallic Borders | special | partly | scifi, modern | screen look (no light); connected texture |
| `chisel:futura:1` | Glowing Screen with Cyan Borders | special | partly | scifi, modern | screen look (no light); connected texture |
| `chisel:futura:2` | Applied Labyrinthic Neon Lines | off-theme | no | scifi |  |
| `chisel:futura:3` | Poptart Rainbow | off-theme | no | scifi |  |
| `chisel:futura:4` | Purple Applied Labyrinthic Neon Lines | off-theme | no | scifi |  |
| `chisel:futura:5` | Psychedelic Poptart Rainbow.v02 | off-theme | no | scifi |  |
| `chisel:futuraCircuit:0` | Black | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:1` | Red | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:2` | Green | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:3` | Brown | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:4` | Blue | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:5` | Purple | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:6` | Cyan | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:7` | Light Gray | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:8` | Gray | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:9` | Pink | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:10` | Lime | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:11` | Yellow | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:12` | Light Blue | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:13` | Magenta | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:14` | Orange | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:futuraCircuit:15` | White | off-theme | no | scifi | pattern spans 3x3 blocks |

#### `HEX_PLATING` feature: `chisel:hexPlating`, `chisel:hexLargePlating`

Dark hex plate with a dyed, animated, full-bright overlay (color from chisel.cfg hexcolors); emits light 7.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:hexPlating:0` | Black Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:1` | Red Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:2` | Green Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:3` | Brown Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:4` | Blue Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:5` | Purple Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:6` | Cyan Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:7` | Light Gray Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:8` | Gray Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:9` | Pink Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:10` | Lime Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:11` | Yellow Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:12` | Light Blue Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:13` | Magenta Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:14` | Orange Plating | off-theme | no | scifi |  |
| `chisel:hexPlating:15` | White Plating | off-theme | no | scifi |  |
| `chisel:hexLargePlating:0` | Black Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:1` | Red Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:2` | Green Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:3` | Brown Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:4` | Blue Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:5` | Purple Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:6` | Cyan Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:7` | Light Gray Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:8` | Gray Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:9` | Pink Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:10` | Lime Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:11` | Yellow Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:12` | Light Blue Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:13` | Magenta Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:14` | Orange Plating | off-theme | no | scifi | pattern spans 3x3 blocks |
| `chisel:hexLargePlating:15` | White Plating | off-theme | no | scifi | pattern spans 3x3 blocks |

#### `CUBITS` feature: `chisel:cubit`

Plain gray ramp from black (0) to white (15).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:cubit:0` | Cubit(0) | wall-main | partly | modern |  |
| `chisel:cubit:1` | Cubit(1) | wall-main | partly | modern |  |
| `chisel:cubit:2` | Cubit(2) | wall-main | partly | modern |  |
| `chisel:cubit:3` | Cubit(3) | wall-main | partly | modern |  |
| `chisel:cubit:4` | Cubit(4) | wall-main | partly | modern |  |
| `chisel:cubit:5` | Cubit(5) | wall-main | partly | modern |  |
| `chisel:cubit:6` | Cubit(6) | wall-main | partly | modern |  |
| `chisel:cubit:7` | Cubit(7) | wall-main | partly | modern |  |
| `chisel:cubit:8` | Cubit(8) | wall-main | partly | modern |  |
| `chisel:cubit:9` | Cubit(9) | wall-main | partly | modern |  |
| `chisel:cubit:10` | Cubit(10) | wall-main | partly | modern |  |
| `chisel:cubit:11` | Cubit(11) | wall-main | partly | modern |  |
| `chisel:cubit:12` | Cubit(12) | wall-main | partly | modern |  |
| `chisel:cubit:13` | Cubit(13) | wall-main | partly | modern |  |
| `chisel:cubit:14` | Cubit(14) | wall-main | partly | modern |  |
| `chisel:cubit:15` | Cubit(15) | wall-main | partly | modern |  |

#### `GLOTEK` feature: `chisel:glotek`

Full-bright glyph panel; emits no light; mobs never spawn on it.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:glotek:0` | Ceramic | off-theme | no | scifi | connected texture |
| `chisel:glotek:1` | Sandwisp | off-theme | no | scifi | connected texture |
| `chisel:glotek:2` | Easter Green | off-theme | no | scifi | connected texture |
| `chisel:glotek:3` | Dragon Green | off-theme | no | scifi | connected texture |
| `chisel:glotek:4` | Tealish Green | off-theme | no | scifi | connected texture |
| `chisel:glotek:5` | Green Haze | off-theme | no | scifi | connected texture |
| `chisel:glotek:6` | Petrol | off-theme | no | scifi | connected texture |
| `chisel:glotek:7` | Catalina Blue | off-theme | no | scifi | connected texture |
| `chisel:glotek:8` | Blue Gem | off-theme | no | scifi | connected texture |
| `chisel:glotek:9` | Royal Purple | off-theme | no | scifi | connected texture |
| `chisel:glotek:10` | Dark Magenta | off-theme | no | scifi | connected texture |
| `chisel:glotek:11` | Dogwood Rose | off-theme | no | scifi | connected texture |
| `chisel:glotek:12` | Cranberry | off-theme | no | scifi | connected texture |
| `chisel:glotek:13` | Brink Pink | off-theme | no | scifi | connected texture |
| `chisel:glotek:14` | Dark Salmon | off-theme | no | scifi | connected texture |
| `chisel:glotek:15` | Harvest Gold | off-theme | no | scifi | connected texture |

#### `NEONITE` feature: `chisel:neonite`

Full-bright flat color (neon sign look); emits no light; mobs never spawn on it.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:neonite:0` | Atomic Tangerine | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:1` | Saffron Mango | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:2` | Golden Glow | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:3` | Oasis | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:4` | Pixie Green | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:5` | Fresh Green | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:6` | Jungle Green | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:7` | Mariner | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:8` | Congress Blue | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:9` | Deep Sapphire | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:10` | Deep Cove | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:11` | Meteorite | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:12` | Eminence | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:13` | Warm Purple | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:14` | Raspberry Pink | wall-accent | partly | modern, scifi | connected texture |
| `chisel:neonite:15` | Watermelon Pink | wall-accent | partly | modern, scifi | connected texture |

#### `ANTIBLOCK` feature: `chisel:antiBlock`

Full-bright dye color with a black outline; emits no light; mobs never spawn on it.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:antiBlock:0` | Black Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:1` | Red Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:2` | Green Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:3` | Brown Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:4` | Blue Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:5` | Purple Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:6` | Cyan Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:7` | Light Gray Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:8` | Gray Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:9` | Pink Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:10` | Lime Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:11` | Yellow Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:12` | Light Blue Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:13` | Magenta Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:14` | Orange Anti Block | wall-accent | partly | modern, scifi | connected texture |
| `chisel:antiBlock:15` | White Anti Block | wall-accent | partly | modern, scifi | connected texture |

### Lights

Sheets: `lights.png`.

#### `GLOWSTONE` feature: `chisel:glowstone`

Variety for upstream "h" glowstone. Behavior: Light 15.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:glowstone:1` | Cobble Glowstone Block | light | yes | standard, border, desert |  |
| `chisel:glowstone:2` | Corroded Glowstone Blocks | light | yes | standard, border, desert |  |
| `chisel:glowstone:3` | Glowstone Blocks with Glass | light | yes | standard, border, desert | glowstone blocks behind glass |
| `chisel:glowstone:4` | Neon Glowstone | light | yes | standard, border, desert | smooth white: reads as a modern light panel |
| `chisel:glowstone:5` | Ornate Glowstone Blocks | light | yes | standard, border, desert |  |
| `chisel:glowstone:6` | Rocky Glowstone | light | yes | standard, border, desert |  |
| `chisel:glowstone:7` | Shale-Shaped Glowstone | light | yes | standard, border, desert |  |
| `chisel:glowstone:8` | Glowstone Tiles | light | yes | standard, border, desert |  |
| `chisel:glowstone:9` | Fancy Glowstone Lantern | light | yes | standard, border, desert | lantern-like grid |
| `chisel:glowstone:10` | Crumbling Glowstone Block | light | yes | standard, border, desert |  |
| `chisel:glowstone:11` | Organic Glowstone Growth Block | light | yes | standard, border, desert |  |
| `chisel:glowstone:12` | Glowstone Layers | light | yes | standard, border, desert |  |
| `chisel:glowstone:13` | Corroded Glowstone Tiles | light | yes | standard, border, desert |  |
| `chisel:glowstone:14` | Glowstone Bismuth | light | partly | standard, border, desert | variety for upstream "h" glowstone |
| `chisel:glowstone:15` | Glowstone Bismuth Panel | light | partly | standard, border, desert | variety for upstream "h" glowstone |

#### `LANTERN` feature: `chisel:lantern`

Full-block caged lamp; top, bottom and side faces differ. Behavior: Light 15.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:lantern:0` | Simple | light | yes | standard, border |  |
| `chisel:lantern:1` | Glow | light | yes | standard, border |  |
| `chisel:lantern:2` | Redstone | light | partly | industrial | full-block caged lamp; top, bottom and side faces differ |
| `chisel:lantern:3` | Soul | light | partly | gothic | full-block caged lamp; top, bottom and side faces differ |
| `chisel:lantern:4` | Cursed | light | partly | gothic | full-block caged lamp; top, bottom and side faces differ |
| `chisel:lantern:5` | Ender | light | no | scifi | full-block caged lamp; top, bottom and side faces differ |

#### `FROGLIGHT` feature: `chisel:froglight`

Soft pastel glowing block. Behavior: Light 15.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:froglight:0` | Aluminium | light | yes | standard, modern | white-blue: ceiling light |
| `chisel:froglight:1` | Titanium | light | partly | modern | soft pastel glowing block |
| `chisel:froglight:2` | Tungstensteel | light | partly | modern |  |
| `chisel:froglight:3` | Osmium | light | partly | modern |  |
| `chisel:froglight:4` | Fluxed | light | yes | standard, desert | soft pastel glowing block |
| `chisel:froglight:5` | Uranium | light | partly | modern |  |
| `chisel:froglight:6` | Girisium | light | partly | modern |  |
| `chisel:froglight:7` | Orundum | light | partly | modern |  |
| `chisel:froglight:8` | Blood-Stained | light | no | gothic | soft pastel glowing block |
| `chisel:froglight:9` | Ichor | light | partly | industrial | soft pastel glowing block |

#### `TORCH` feature: `chisel:torch1`, `chisel:torch2`, `chisel:torch3`, `chisel:torch4`, `chisel:torch5`, `chisel:torch6`, `chisel:torch7`, `chisel:torch8`, `chisel:torch9`, `chisel:torch10`

Torch variants (light 14); they need a wall or floor to attach to; metadata is the facing, as for vanilla torches.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:torch1:0` | Torch 1 | light | partly | standard |  |
| `chisel:torch2:0` | Torch 2 | light | partly | standard |  |
| `chisel:torch3:0` | Torch 3 | light | partly | standard |  |
| `chisel:torch4:0` | Torch 4 | light | partly | standard |  |
| `chisel:torch5:0` | Torch 5 | light | partly | standard |  |
| `chisel:torch6:0` | Torch 6 | light | partly | standard |  |
| `chisel:torch7:0` | Torch 7 | light | partly | standard |  |
| `chisel:torch8:0` | Torch 8 | light | partly | standard |  |
| `chisel:torch9:0` | Torch 9 | light | partly | standard |  |
| `chisel:torch10:0` | Torch 10 | light | partly | standard |  |

#### `JACKOLANTERN` feature: `chisel:jackolantern1`, `chisel:jackolantern2`, `chisel:jackolantern3`, `chisel:jackolantern4`, `chisel:jackolantern5`, `chisel:jackolantern6`, `chisel:jackolantern7`, `chisel:jackolantern8`, `chisel:jackolantern9`, `chisel:jackolantern10`, `chisel:jackolantern11`, `chisel:jackolantern12`, `chisel:jackolantern13`, `chisel:jackolantern14`, `chisel:jackolantern15`, `chisel:jackolantern16`

Seasonal carved faces (light 15); metadata is the facing.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:jackolantern1:0` | Jack o'Lantern 1 | off-theme | no | rural |  |
| `chisel:jackolantern2:0` | Jack o'Lantern 2 | off-theme | no | rural |  |
| `chisel:jackolantern3:0` | Jack o'Lantern 3 | off-theme | no | rural |  |
| `chisel:jackolantern4:0` | Jack o'Lantern 4 | off-theme | no | rural |  |
| `chisel:jackolantern5:0` | Jack o'Lantern 5 | off-theme | no | rural |  |
| `chisel:jackolantern6:0` | Jack o'Lantern 6 | off-theme | no | rural |  |
| `chisel:jackolantern7:0` | Jack o'Lantern 7 | off-theme | no | rural |  |
| `chisel:jackolantern8:0` | Jack o'Lantern 8 | off-theme | no | rural |  |
| `chisel:jackolantern9:0` | Jack o'Lantern 9 | off-theme | no | rural |  |
| `chisel:jackolantern10:0` | Jack o'Lantern 10 | off-theme | no | rural |  |
| `chisel:jackolantern11:0` | Jack o'Lantern 11 | off-theme | no | rural |  |
| `chisel:jackolantern12:0` | Jack o'Lantern 12 | off-theme | no | rural |  |
| `chisel:jackolantern13:0` | Jack o'Lantern 13 | off-theme | no | rural |  |
| `chisel:jackolantern14:0` | Jack o'Lantern 14 | off-theme | no | rural |  |
| `chisel:jackolantern15:0` | Jack o'Lantern 15 | off-theme | no | rural |  |
| `chisel:jackolantern16:0` | Jack o'Lantern 16 | off-theme | no | rural |  |

#### `PUMPKIN` feature: `chisel:pumpkin1`, `chisel:pumpkin2`, `chisel:pumpkin3`, `chisel:pumpkin4`, `chisel:pumpkin5`, `chisel:pumpkin6`, `chisel:pumpkin7`, `chisel:pumpkin8`, `chisel:pumpkin9`, `chisel:pumpkin10`, `chisel:pumpkin11`, `chisel:pumpkin12`, `chisel:pumpkin13`, `chisel:pumpkin14`, `chisel:pumpkin15`, `chisel:pumpkin16`

Carved pumpkin, unlit; metadata is the facing; on two snow blocks it spawns a snow golem.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:pumpkin1:0` | Pumpkin 1 | off-theme | no | rural |  |
| `chisel:pumpkin2:0` | Pumpkin 2 | off-theme | no | rural |  |
| `chisel:pumpkin3:0` | Pumpkin 3 | off-theme | no | rural |  |
| `chisel:pumpkin4:0` | Pumpkin 4 | off-theme | no | rural |  |
| `chisel:pumpkin5:0` | Pumpkin 5 | off-theme | no | rural |  |
| `chisel:pumpkin6:0` | Pumpkin 6 | off-theme | no | rural |  |
| `chisel:pumpkin7:0` | Pumpkin 7 | off-theme | no | rural |  |
| `chisel:pumpkin8:0` | Pumpkin 8 | off-theme | no | rural |  |
| `chisel:pumpkin9:0` | Pumpkin 9 | off-theme | no | rural |  |
| `chisel:pumpkin10:0` | Pumpkin 10 | off-theme | no | rural |  |
| `chisel:pumpkin11:0` | Pumpkin 11 | off-theme | no | rural |  |
| `chisel:pumpkin12:0` | Pumpkin 12 | off-theme | no | rural |  |
| `chisel:pumpkin13:0` | Pumpkin 13 | off-theme | no | rural |  |
| `chisel:pumpkin14:0` | Pumpkin 14 | off-theme | no | rural |  |
| `chisel:pumpkin15:0` | Pumpkin 15 | off-theme | no | rural |  |
| `chisel:pumpkin16:0` | Pumpkin 16 | off-theme | no | rural |  |

#### `BEACON` feature: `chisel:beacon`

Working beacon with a dyed beam (tile entity); light level set to 5.0F, i.e. 75, out of range.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:beacon:0` | Black Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:1` | Red Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:2` | Green Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:3` | Brown Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:4` | Blue Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:5` | Purple Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:6` | Cyan Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:7` | Light Gray Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:8` | Gray Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:9` | Pink Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:10` | Lime Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:11` | Yellow Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:12` | Light Blue Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:13` | Magenta Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:14` | Orange Beamed Beacon | off-theme | no | - |  |
| `chisel:beacon:15` | Rainbow Beamed Beacon | off-theme | no | - |  |

### Fantasy, holystone, grimstone, sveltstone

Sheets: `fantasy.png`.

#### `FANTASY` feature: `chisel:fantasyblock`, `chisel:fantasyblock2`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:fantasyblock:0` | Purpled Fantasy Brick | wall-main | no | ancient, luxury |  |
| `chisel:fantasyblock:1` | Purpled Faded Fantasy Brick | wall-main | no | ancient, luxury |  |
| `chisel:fantasyblock:2` | Purpled Worn Fantasy Brick | wall-main | no | ancient, luxury | connects vertically |
| `chisel:fantasyblock:3` | Purpled Damaged Fantasy Bricks | wall-main | no | ancient, luxury |  |
| `chisel:fantasyblock:4` | Purpled Fantasy Decoration | wall-accent | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:5` | Purpled Fantasy Decoration Block | wall-accent | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:6` | Purpled Fantasy Pillar | trim-pillar | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:7` | Purpled Fantasy Pillar Decoration | trim-pillar | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:8` | Purpled Fantasy Gold Snake Decoration | off-theme | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:9` | Purpled Fantasy Gold Noise Decoration | off-theme | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:10` | Purpled Fantasy Gold Engravings Decoration | off-theme | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:11` | Purpled Fantasy Gold Chains Decoration | off-theme | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:12` | Purpled Fantasy Plate Decoration | wall-accent | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:13` | Purpled Fantasy Block | wall-main | no | ancient, luxury | connected texture |
| `chisel:fantasyblock:14` | Purpled Fantasy Bricks in disarray | rubble-ruin | no | ancient, luxury | white stone with purple veins |
| `chisel:fantasyblock:15` | Purpled Worn Fantasy Bricks in disarray | rubble-ruin | no | ancient, luxury | white stone with purple veins; connects vertically |
| `chisel:fantasyblock2:0` | Fantasy Brick | wall-main | partly | ancient, luxury |  |
| `chisel:fantasyblock2:1` | Faded Fantasy Brick | wall-main | partly | ancient, luxury |  |
| `chisel:fantasyblock2:2` | Worn Fantasy Brick | wall-main | partly | ancient, luxury | connects vertically |
| `chisel:fantasyblock2:3` | Damaged Fantasy Bricks | wall-main, rubble-ruin | partly | ancient, ruin | pale cream stone |
| `chisel:fantasyblock2:4` | Fantasy Decoration | wall-accent | partly | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:5` | Fantasy Decoration Block | wall-accent | partly | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:6` | Fantasy Pillar | trim-pillar | partly | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:7` | Fantasy Pillar Decoration | trim-pillar | partly | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:8` | Fantasy Gold Snake Decoration | off-theme | no | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:9` | Fantasy Gold Noise Decoration | off-theme | no | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:10` | Fantasy Gold Engravings Decoration | off-theme | no | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:11` | Fantasy Gold Chains Decoration | off-theme | no | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:12` | Fantasy Plate Decoration | wall-accent | partly | ancient, luxury | pale cream stone |
| `chisel:fantasyblock2:13` | Fantasy Block | wall-main | partly | ancient, luxury | connected texture |
| `chisel:fantasyblock2:14` | Fantasy Bricks in disarray | rubble-ruin | partly | ancient, ruin | pale cream stone |
| `chisel:fantasyblock2:15` | Worn Fantasy Bricks in disarray | rubble-ruin | partly | ancient, ruin | pale cream stone; connects vertically |

#### `HOLYSTONE` feature: `chisel:holystone`

Light gray stone; emits light 3 and sparkle particles.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:holystone:0` | Holystone | wall-main | partly | ancient, standard |  |
| `chisel:holystone:1` | Smooth Holystone | wall-main | partly | ancient, standard |  |
| `chisel:holystone:2` | Mysterious Holystone Symbol | wall-accent | no | ancient | symbol |
| `chisel:holystone:3` | Chiseled Holystone | wall-accent | partly | ancient, standard | light gray stone; emits light 3 and sparkle particles |
| `chisel:holystone:4` | Holystone Blocks | wall-main | partly | ancient, standard |  |
| `chisel:holystone:5` | Rough Holystone Blocks | wall-main | partly | ancient, standard |  |
| `chisel:holystone:6` | Holystone Bricks | wall-main | yes | standard, ancient | bricks |
| `chisel:holystone:7` | Large Holystone Bricks | wall-main | yes | standard, ancient | light gray stone; emits light 3 and sparkle particles |
| `chisel:holystone:8` | Holystone Platform | floor | partly | ancient, standard | light gray stone; emits light 3 and sparkle particles |
| `chisel:holystone:9` | Holystone Platform Tiles | floor | partly | ancient, standard | light gray stone; emits light 3 and sparkle particles |
| `chisel:holystone:10` | Fancy Holystone Construction | wall-accent | partly | ancient, standard | light gray stone; emits light 3 and sparkle particles |
| `chisel:holystone:11` | Fancy Holystone Tiles | floor, wall-accent | partly | ancient, standard | light gray stone; emits light 3 and sparkle particles |
| `chisel:holystone:12` | Smooth Holystone Plate | wall-main | partly | ancient, standard | connected texture |
| `chisel:holystone:13` | Holystone Plate | wall-main | partly | ancient, standard | connected texture |

#### `GRIMSTONE` feature: `chisel:grimstone`

Near-black stone; emits dark particles.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:grimstone:0` | Grimstone | wall-main | partly | gothic, industrial |  |
| `chisel:grimstone:1` | Smooth Grimstone | wall-main | partly | gothic, industrial |  |
| `chisel:grimstone:2` | Mysterious Grimstone Symbol | off-theme | no | gothic | red symbol |
| `chisel:grimstone:3` | Chiseled Grimstone | wall-main | partly | gothic, industrial |  |
| `chisel:grimstone:4` | Grimstone Blocks | wall-main | partly | gothic, industrial |  |
| `chisel:grimstone:5` | Rough Grimstone Blocks | wall-main | partly | gothic, industrial |  |
| `chisel:grimstone:6` | Grimstone Bricks | wall-main | partly | gothic, industrial |  |
| `chisel:grimstone:7` | Large Grimstone Bricks | wall-main | partly | gothic, industrial |  |
| `chisel:grimstone:8` | Grimstone Platform | floor | partly | gothic, industrial | near-black stone; emits dark particles |
| `chisel:grimstone:9` | Grimstone Platform Tiles | floor | partly | gothic, industrial | near-black stone; emits dark particles |
| `chisel:grimstone:10` | Fancy Grimstone Construction | wall-accent | partly | gothic, industrial | near-black stone; emits dark particles |
| `chisel:grimstone:11` | Fancy Grimstone Tiles | wall-accent | partly | gothic, industrial | near-black stone; emits dark particles |
| `chisel:grimstone:12` | Smooth Grimstone Plate | wall-main | partly | gothic, industrial | connected texture |
| `chisel:grimstone:13` | Grimstone Plate | wall-main | partly | gothic, industrial | connected texture |
| `chisel:grimstone:14` | Flaky Grimstone | wall-main | partly | gothic, industrial |  |

#### `SVELSTONE` feature: `chisel:sveltstone`

Plain matte stone in muted colors.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:sveltstone:0` | Silver Sand | wall-main | yes | modern, standard | near-white |
| `chisel:sveltstone:1` | Sand Dune | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:2` | Fuscous Grey | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:3` | Flint | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:4` | Taupe | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:5` | Tobacco Brown | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:6` | Boulder | wall-main | yes | modern, standard | beige-gray |
| `chisel:sveltstone:7` | Olive Haze | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:8` | Pale Oister | wall-main | yes | modern, standard | pale |
| `chisel:sveltstone:9` | Hemlock | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:10` | Rifle Green | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:11` | Everglade | wall-main | partly | modern, standard |  |
| `chisel:sveltstone:12` | Baltic Sea | wall-main | yes | modern, standard | near-black |
| `chisel:sveltstone:13` | Outer Space | wall-main | yes | modern, standard | dark gray |
| `chisel:sveltstone:14` | Gunmetal | wall-main | yes | modern, standard | gunmetal |
| `chisel:sveltstone:15` | Matterhorn | wall-main | partly | modern, standard |  |

### Temple, tyrian, voidstone

Sheets: `temple-tyrian-voidstone.png`.

#### `TEMPLE_BLOCK` feature: `chisel:templeblock`

Blue-gray temple stone.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:templeblock:0` | Temple Cobblestone | wall-main | partly | ancient | pattern spans 2x2 blocks |
| `chisel:templeblock:1` | Ornate Temple Block | wall-accent | partly | ancient | blue-gray temple stone |
| `chisel:templeblock:2` | Temple Plate | wall-main | partly | ancient |  |
| `chisel:templeblock:3` | Cracked Temple Plate | wall-main, rubble-ruin | partly | ancient, ruin | blue-gray temple stone; pattern spans 3x3 blocks |
| `chisel:templeblock:4` | Temple Bricks | wall-main | partly | ancient |  |
| `chisel:templeblock:5` | Large Temple Bricks | wall-main | partly | ancient |  |
| `chisel:templeblock:6` | Worn Temple Bricks | wall-main, rubble-ruin | partly | ancient, ruin | blue-gray temple stone |
| `chisel:templeblock:7` | Temple Bricks in disarray | wall-main, rubble-ruin | partly | ancient, ruin | blue-gray temple stone |
| `chisel:templeblock:8` | Temple Column | trim-pillar | partly | ancient | blue-gray temple stone |
| `chisel:templeblock:9` | Temple Stand | trim-pillar, wall-accent | partly | ancient | blue-gray temple stone |
| `chisel:templeblock:10` | Temple Mosaic Stand | wall-accent | partly | ancient | blue-gray temple stone |
| `chisel:templeblock:11` | Temple Creeper Stand | off-theme | no | - | creeper face motif |
| `chisel:templeblock:12` | Temple Tiles | floor | partly | ancient | blue-gray temple stone |
| `chisel:templeblock:13` | Small Temple Tiles | floor | partly | ancient | blue-gray temple stone |
| `chisel:templeblock:14` | Light Temple Tiles | floor | partly | ancient | blue-gray temple stone |
| `chisel:templeblock:15` | Small Light Temple Tiles | floor | partly | ancient | blue-gray temple stone |

#### `TEMPLE_BLOCK_MOSSY` feature: `chisel:mossy_templeblock`

Temple stone with bright green moss.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:mossy_templeblock:0` | Mossy Temple Cobblestone | rubble-ruin, foliage | partly | ancient, overgrown, ruin | pattern spans 2x2 blocks |
| `chisel:mossy_templeblock:1` | Ornate Mossy Temple Block | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:2` | Mossy Temple Plate | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:3` | Cracked Mossy Temple Plate | rubble-ruin, foliage | partly | ancient, overgrown, ruin | pattern spans 3x3 blocks |
| `chisel:mossy_templeblock:4` | Mossy Temple Bricks | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:5` | Large Mossy Temple Bricks | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:6` | Worn Mossy Temple Bricks | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:7` | Mossy Temple Bricks in disarray | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:8` | Mossy Temple Column | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:9` | Mossy Temple Stand | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:10` | Mossy Temple Mosaic Stand | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:11` | Mossy Temple Creeper Stand | off-theme | no | - | creeper face motif |
| `chisel:mossy_templeblock:12` | Mossy Temple Tiles | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:13` | Small Mossy Temple Tiles | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:14` | Light Mossy Temple Tiles | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |
| `chisel:mossy_templeblock:15` | Small Light Mossy Temple Tiles | rubble-ruin, foliage | partly | ancient, overgrown, ruin |  |

#### `TYRIAN` feature: `chisel:tyrian`

Futuristic armor plating.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:tyrian:0` | Futuristic Armor Plating Block | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:1` | Bleak Futuristic Armor Plating Block | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:2` | Purple Futuristic Armor Plating Block | wall-main | no | scifi, industrial | futuristic armor plating |
| `chisel:tyrian:3` | Faded Purple Futuristic Armor Plating Block | wall-main | no | scifi, industrial | futuristic armor plating |
| `chisel:tyrian:4` | Rusted Futuristic Armor Plating Block | rubble-ruin | partly | industrial, ruin | rusted |
| `chisel:tyrian:5` | Elaborate Futuristic Armor Plating Block | wall-main | yes | modern, industrial | futuristic armor plating |
| `chisel:tyrian:6` | Futuristic Armor Plating Block with many seams | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:7` | Futuristic Platform Block | floor, wall-main | yes | modern, industrial | futuristic armor plating |
| `chisel:tyrian:8` | Futuristic Armor Plating Tiles | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:9` | Diagonal Futuristic Armor Plating Block | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:10` | Futuristic Armor Plating Block with dent | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:11` | Blue Futuristic Armor Plating Block | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:12` | Black Futuristic Armor Plating Block | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:13` | Black Futuristic Armor Plating Tiles | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:14` | Black Futuristic Armor Plating Block with Opening | wall-main | partly | scifi, industrial |  |
| `chisel:tyrian:15` | Shiny Futuristic Armor Plating | wall-accent | yes | modern | shiny steel panel; connected texture |

#### `VOIDSTONE` feature: `chisel:voidstone`, `chisel:voidstone2`, `chisel:voidstoneRunic`

Purple void stone. Behavior: `voidstone2` and `voidstoneRunic` animated.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:voidstone:0` | Raw Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone:1` | Large Voidstone Tiles | off-theme | no | scifi, gothic |  |
| `chisel:voidstone:2` | Smooth Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone:3` | Skull Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone:4` | Runic Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone:5` | Metal-Bordered Voidstone | off-theme | no | scifi, gothic | connected texture |
| `chisel:voidstone:6` | Eye Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone:7` | Beveled Voidstone | off-theme | no | scifi, gothic | connected texture |
| `chisel:voidstone2:0` | Raw Energised Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone2:1` | Large Energised Voidstone Tiles | off-theme | no | scifi, gothic |  |
| `chisel:voidstone2:2` | Smooth Energised Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone2:3` | Skull Energised Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone2:4` | Runic Energised Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone2:5` | Metal-Bordered Energised Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone2:6` | Eye Energised Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstone2:7` | Beveled Voidstone | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:1` | Voidstone - Orange Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:2` | Voidstone - Magenta Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:3` | Voidstone - Light Blue Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:4` | Voidstone - Yellow Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:5` | Voidstone - Lime Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:6` | Voidstone - Pink Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:7` | Voidstone - Gray Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:8` | Voidstone - Light Gray Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:9` | Voidstone - Cyan Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:10` | Voidstone - Purple Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:11` | Voidstone - Blue Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:12` | Voidstone - Brown Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:13` | Voidstone - Green Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:14` | Voidstone - Red Rune | off-theme | no | scifi, gothic |  |
| `chisel:voidstoneRunic:15` | Voidstone - Black Rune | off-theme | no | scifi, gothic |  |

#### `VOIDSTONE_PILLARS` feature: `chisel:voidstonePillar`, `chisel:voidstonePillar2`

Connected column.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:voidstonePillar:0` | Striped Voidstone Pillar | off-theme | no | scifi, gothic | connects vertically |
| `chisel:voidstonePillar2:0` | Striped Energised Voidstone Pillar | off-theme | no | scifi, gothic | connects vertically |

### Nether, end, obsidian, lavastone, waterstone

Sheets: `nether-end.png`.

#### `NETHER_BRICK` feature: `chisel:nether_brick`

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:nether_brick:1` | Blue Nether Brick | off-theme | no | gothic | blue |
| `chisel:nether_brick:2` | Spattered Nether Brick | wall-main | partly | gothic, desert |  |
| `chisel:nether_brick:3` | Nether Brick made of Guts | off-theme | no | gothic | flesh texture |
| `chisel:nether_brick:4` | Dark Nether Brick made of Guts | off-theme | no | gothic | flesh texture |
| `chisel:nether_brick:5` | Small Nether Brick made of Guts | off-theme | no | gothic | flesh texture |
| `chisel:nether_brick:6` | Blue Nether Brick with Lava | off-theme | no | gothic | lava cracks |
| `chisel:nether_brick:7` | Brown Nether Brick | wall-main | partly | standard, desert | brown brick |
| `chisel:nether_brick:8` | Obsidian Nether Brick | off-theme | no | gothic | lava cracks |
| `chisel:nether_brick:9` | Stone Nether Brick | wall-main | partly | gothic | stone and lava |
| `chisel:nether_brick:10` | Nether Brick made of Meat | off-theme | no | gothic | meat texture |
| `chisel:nether_brick:11` | Red Nether Brick made of Meat | off-theme | no | gothic | meat texture |
| `chisel:nether_brick:12` | Small Nether Brick made of Meat | off-theme | no | gothic | meat texture |
| `chisel:nether_brick:13` | Small Red Nether Brick made of Meat | off-theme | no | gothic | meat texture |
| `chisel:nether_brick:14` | Red Nether Brick | wall-main | yes | desert | red nether brick: variety for desert "B" (red_nether_bricks) |
| `chisel:nether_brick:15` | Small Red Nether Brick | wall-main | yes | desert | small red nether brick |

#### `NETHER_RACK` feature: `chisel:netherrack`

Blood, guts and meat textures; not a GT host (separate block id, unlike minecraft:netherrack).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:netherrack:1` | Nethergravel with Blood | off-theme | no | gothic |  |
| `chisel:netherrack:2` | Netherrack with Blood | off-theme | no | gothic |  |
| `chisel:netherrack:3` | Darker Netherrack with Blood | off-theme | no | gothic |  |
| `chisel:netherrack:4` | Blue Netherrack | off-theme | no | gothic |  |
| `chisel:netherrack:5` | Shale Blue Netherrack | off-theme | no | gothic |  |
| `chisel:netherrack:6` | Classic Netherrack | off-theme | no | gothic |  |
| `chisel:netherrack:7` | Spattered Netherrack | off-theme | no | gothic |  |
| `chisel:netherrack:8` | Netherrack made of Guts | off-theme | no | gothic |  |
| `chisel:netherrack:9` | Dark Netherrack made of Guts | off-theme | no | gothic |  |
| `chisel:netherrack:10` | Netherrack made of Meat | off-theme | no | gothic |  |
| `chisel:netherrack:11` | Red Netherrack made of Meat | off-theme | no | gothic |  |
| `chisel:netherrack:12` | Netherrack made of Smaller Meat Chunks | off-theme | no | gothic |  |
| `chisel:netherrack:13` | Dark Red Netherrack | off-theme | no | gothic |  |
| `chisel:netherrack:14` | Netherrack with Flowing Lava | off-theme | no | gothic |  |

#### `END_STONE` feature: `chisel:end_Stone`

Pale yellow end stone; not a GT host (separate block id).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:end_Stone:0` | End Stone Bricks | wall-main | partly | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:1` | End Chaotic Bricks | wall-main, rubble-ruin | partly | desert, ruin | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:2` | Checkered Tiles | floor | partly | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:3` | Ender Circuit | off-theme | no | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:4` | Fancy Bricks | wall-main | partly | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:5` | End Stone Pillar | trim-pillar | partly | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:6` | Etched End Stone | off-theme | no | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:7` | Prismatic End Stone | wall-accent | partly | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:8` | End Stone Chunk | off-theme | no | desert, ancient | pale yellow end stone; not a GT host (separate block id); pattern spans 3x3 blocks |
| `chisel:end_Stone:9` | Framed End Stone Chunk | off-theme | no | desert, ancient | pale yellow end stone; not a GT host (separate block id); connected texture |
| `chisel:end_Stone:10` | Arcane Block of End Stone | off-theme | no | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:11` | Framed End Stone | off-theme | no | desert, ancient | pale yellow end stone; not a GT host (separate block id); connected texture |
| `chisel:end_Stone:12` | Ornate End Stone | wall-accent | partly | desert, ancient | pale yellow end stone; not a GT host (separate block id) |
| `chisel:end_Stone:13` | End Stone Large Tile | floor | partly | desert, ancient | pale yellow end stone; not a GT host (separate block id) |

#### `PURPUR` feature: `chisel:purpur`

Magenta purpur stone.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:purpur:0` | Medium Tiles | off-theme | no | scifi |  |
| `chisel:purpur:1` | Pillar | off-theme | no | scifi |  |
| `chisel:purpur:2` | Not Shulker | off-theme | no | scifi |  |
| `chisel:purpur:3` | Small Tiles | off-theme | no | scifi |  |
| `chisel:purpur:4` | Broken Small Tiles | off-theme | no | scifi |  |
| `chisel:purpur:5` | Prismatic Purpur | off-theme | no | scifi |  |
| `chisel:purpur:6` | Rough Bricks | off-theme | no | scifi |  |
| `chisel:purpur:7` | Cracked Surface | off-theme | no | scifi |  |
| `chisel:purpur:8` | Arcane Block of Purpur | off-theme | no | scifi |  |
| `chisel:purpur:9` | Purpur Large Tile | off-theme | no | scifi |  |
| `chisel:purpur:10` | Ornate Purpur | off-theme | no | scifi |  |

#### `OBSIDIAN` feature: `chisel:obsidian`

Near-black purple obsidian; blast resistant.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:obsidian:1` | Large Obsidian Pillar | trim-pillar | partly | gothic, industrial | near-black purple obsidian; blast resistant |
| `chisel:obsidian:2` | Obsidian Pillar | trim-pillar | partly | gothic, industrial | near-black purple obsidian; blast resistant |
| `chisel:obsidian:3` | Chiseled Obsidian | wall-accent | partly | gothic, industrial |  |
| `chisel:obsidian:4` | Shiny Obsidian Panel | wall-accent | partly | gothic, industrial |  |
| `chisel:obsidian:5` | Obsidian Panel | wall-accent | partly | gothic, industrial |  |
| `chisel:obsidian:6` | Organic-Looking Obsidian Chunks | off-theme | no | gothic, industrial | near-black purple obsidian; blast resistant |
| `chisel:obsidian:7` | Organic-Looking Obsidian Growth | off-theme | no | gothic, industrial | near-black purple obsidian; blast resistant |
| `chisel:obsidian:8` | Obsidian Crystal | off-theme | no | gothic, industrial | near-black purple obsidian; blast resistant |
| `chisel:obsidian:9` | Obsidian Panel with Map | off-theme | no | gothic, industrial | map engraving |
| `chisel:obsidian:10` | Obsidian Panel with Region Map | off-theme | no | gothic, industrial | near-black purple obsidian; blast resistant |
| `chisel:obsidian:11` | Bright Obsidian Panel | wall-accent | partly | gothic, industrial |  |
| `chisel:obsidian:12` | Obsidian Blocks | wall-main, floor | partly | gothic, industrial | near-black purple obsidian; blast resistant |
| `chisel:obsidian:13` | Obsidian Tiles | floor | partly | gothic, industrial | near-black purple obsidian; blast resistant |
| `chisel:obsidian:14` | Light Obsidian Blocks with Greek Decor | wall-accent | partly | gothic, industrial |  |
| `chisel:obsidian:15` | Small Obsidian Blocks inside an Oak Wood Crate | off-theme | no | gothic, industrial | obsidian in a wood crate |

#### `SNAKESTONE_OBSIDIAN` feature: `chisel:obsidian_snakestone`

Snake-shaped decoration: placing head then body draws a connected snake; metas 1 and 13 are the chisel results, other metas are shape states; emits particles.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:obsidian_snakestone:1` | Snakestone head | special | no | gothic |  |
| `chisel:obsidian_snakestone:13` | Snakestone body | special | no | gothic |  |

#### `LAVASTONE` feature: `chisel:lavastone`

Animated lava shows through the stone pattern; light 15 (the sheet shows only the stone layer).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:lavastone:0` | Lavastone | special | no | gothic, industrial |  |
| `chisel:lavastone:1` | Black Lavastone | special | no | gothic, industrial |  |
| `chisel:lavastone:2` | Lavastone Tiles | special | no | gothic, industrial |  |
| `chisel:lavastone:3` | Chaotic Lavastone Bricks | special | no | gothic, industrial |  |
| `chisel:lavastone:4` | Lava Creeper in Tiles | special | no | gothic, industrial |  |
| `chisel:lavastone:5` | Lava Panel | special | no | gothic, industrial |  |
| `chisel:lavastone:6` | Ornate Lava Panel | special | no | gothic, industrial |  |
| `chisel:lavastone:7` | Dark Lavastone | special | no | gothic, industrial |  |

#### `WATERSTONE` feature: `chisel:waterstone`

Animated water shows through the stone pattern (the sheet shows only the stone layer). Behavior: Animated.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:waterstone:0` | Waterstone Bricks | special | no | ancient |  |
| `chisel:waterstone:1` | Black Waterstone | special | no | ancient |  |
| `chisel:waterstone:2` | Waterstone Tiles | special | no | ancient |  |
| `chisel:waterstone:3` | Chaotic Waterstone Bricks | special | no | ancient |  |
| `chisel:waterstone:4` | Water Creeper in Tiles | special | no | ancient |  |
| `chisel:waterstone:5` | Water Panel | special | no | ancient | connected texture |
| `chisel:waterstone:6` | Ornate Water Panel | special | no | ancient |  |

### Ice

Sheets: `ice.png`.

#### `ICE` feature: `chisel:ice`

Vanilla ice behavior: slippery, translucent, melts to water near block light above 11, leaves water when broken.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:ice:1` | Rough Ice Block | wall-main | no | arctic |  |
| `chisel:ice:2` | Cobble-Ice | wall-main | no | arctic |  |
| `chisel:ice:3` | Large Rough Ice Bricks | wall-main | no | arctic |  |
| `chisel:ice:4` | Large Ice Bricks | wall-main | no | arctic |  |
| `chisel:ice:5` | Small Ice Bricks | wall-main | no | arctic |  |
| `chisel:ice:6` | Fancy Glass Wall | wall-main | no | arctic |  |
| `chisel:ice:7` | Large Ice Tiles | wall-main | no | arctic |  |
| `chisel:ice:8` | Fancy Ice Tiles | wall-main | no | arctic |  |
| `chisel:ice:9` | Sunken Ice Tiles | wall-main | no | arctic |  |
| `chisel:ice:10` | Disordered Ice Tiles | wall-main | no | arctic |  |
| `chisel:ice:11` | Ice Panel | wall-main | no | arctic |  |
| `chisel:ice:12` | Ice Double Slab | wall-main | no | arctic |  |
| `chisel:ice:13` | Ice Zelda Block | wall-main | no | arctic |  |
| `chisel:ice:14` | Ice Bismuth Block | wall-main | no | arctic |  |
| `chisel:ice:15` | Ice Poison Block | wall-main | no | arctic |  |

#### `ICE_PILLAR` feature: `chisel:ice_pillar`

Ice behavior (melts near light, slippery).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:ice_pillar:0` | Plain Ice Pillar with Plain Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:ice_pillar:1` | Plain Ice Pillar with Greek Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:ice_pillar:2` | Greek Ice Pillar with Plain Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:ice_pillar:3` | Greek Ice Pillar with Greek Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:ice_pillar:4` | Carved Ice Pillar Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:ice_pillar:5` | Ornamental Ice Pillar Capstone | trim-pillar | no | arctic |  |
| `chisel:ice_pillar:6` | Inscribed Ice Pillar | trim-pillar | no | arctic |  |

#### `PACKEDICE` feature: `chisel:packedice`

Slippery; does not melt.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:packedice:1` | Rough Packed Ice Block | wall-main | no | arctic |  |
| `chisel:packedice:2` | Cobble-Packed Ice | wall-main | no | arctic |  |
| `chisel:packedice:3` | Large Rough Packed Ice Bricks | wall-main | no | arctic |  |
| `chisel:packedice:4` | Large Packed Ice Bricks | wall-main | no | arctic |  |
| `chisel:packedice:5` | Small Packed Ice Bricks | wall-main | no | arctic |  |
| `chisel:packedice:6` | Fancy Glass Wall | wall-main | no | arctic |  |
| `chisel:packedice:7` | Large Packed Ice Tiles | wall-main | no | arctic |  |
| `chisel:packedice:8` | Fancy Packed Ice Tiles | wall-main | no | arctic |  |
| `chisel:packedice:9` | Sunken Packed Ice Tiles | wall-main | no | arctic |  |
| `chisel:packedice:10` | Disordered Packed Ice Tiles | wall-main | no | arctic |  |
| `chisel:packedice:11` | Packed Ice Panel | wall-main | no | arctic |  |
| `chisel:packedice:12` | Packed Ice Double Slab | wall-main | no | arctic |  |
| `chisel:packedice:13` | Packed Ice Zelda Block | wall-main | no | arctic |  |
| `chisel:packedice:14` | Packed Ice Bismuth Block | wall-main | no | arctic |  |
| `chisel:packedice:15` | Packed Ice Poison Block | wall-main | no | arctic |  |

#### `PACKEDICE_PILLAR` feature: `chisel:packedice_pillar`

Slippery; does not melt.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:packedice_pillar:0` | Plain Packed Ice Pillar with Plain Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:packedice_pillar:1` | Plain Packed Ice Pillar with Greek Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:packedice_pillar:2` | Greek Packed Ice Pillar with Plain Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:packedice_pillar:3` | Greek Packed Ice Pillar with Greek Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:packedice_pillar:4` | Carved Packed Ice Pillar Capstone | trim-pillar | no | arctic | connects vertically |
| `chisel:packedice_pillar:5` | Ornamental Packed Ice Pillar Capstone | trim-pillar | no | arctic |  |
| `chisel:packedice_pillar:6` | Inscribed Packed Ice Pillar | trim-pillar | no | arctic |  |

#### `ICE_STAIRS` feature: `chisel:ice_stairs`

Slippery. Stairs: each `_stairs.N` block holds two variants, item meta 0 for variant 2N and 8 for variant 2N+1; in the world the low three bits are the stair facing and half, as for vanilla stairs.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:ice_stairs.0:0` | Ice Stairs | stairs-slab | no | arctic | stairs of vanilla ice |
| `chisel:ice_stairs.0:8` | Rough Ice Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:1` (Rough Ice Block) |
| `chisel:ice_stairs.1:0` | Cobble-Ice Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:2` (Cobble-Ice) |
| `chisel:ice_stairs.1:8` | Large rough Ice Brick Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:3` (Large Rough Ice Bricks) |
| `chisel:ice_stairs.2:0` | Large Ice Brick Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:4` (Large Ice Bricks) |
| `chisel:ice_stairs.2:8` | Small Ice Brick Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:5` (Small Ice Bricks) |
| `chisel:ice_stairs.3:0` | Fancy Ice Wall Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:6` (Fancy Glass Wall) |
| `chisel:ice_stairs.3:8` | Large Ice Tile Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:7` (Large Ice Tiles) |
| `chisel:ice_stairs.4:0` | Fancy Ice Tile Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:8` (Fancy Ice Tiles) |
| `chisel:ice_stairs.4:8` | Sunken Ice Tile Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:9` (Sunken Ice Tiles) |
| `chisel:ice_stairs.5:0` | Disordered Ice Tile Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:10` (Disordered Ice Tiles) |
| `chisel:ice_stairs.5:8` | Ice Panel Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:11` (Ice Panel) |
| `chisel:ice_stairs.6:0` | Ice Double Slab Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:12` (Ice Double Slab) |
| `chisel:ice_stairs.6:8` | Ice Zelda Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:13` (Ice Zelda Block) |
| `chisel:ice_stairs.7:0` | Ice Bismuth Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:14` (Ice Bismuth Block) |
| `chisel:ice_stairs.7:8` | Ice Poison Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:15` (Ice Poison Block) |

#### `PACKEDICE_STAIRS` feature: `chisel:packedice_stairs`

Slippery. Stairs: each `_stairs.N` block holds two variants, item meta 0 for variant 2N and 8 for variant 2N+1; in the world the low three bits are the stair facing and half, as for vanilla stairs.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:packedice_stairs.0:0` | Packed Ice Stairs | stairs-slab | no | arctic | stairs of vanilla packed_ice |
| `chisel:packedice_stairs.0:8` | Rough Packed Ice Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:1` (Rough Ice Block) |
| `chisel:packedice_stairs.1:0` | Cobble-Packed Ice Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:2` (Cobble-Ice) |
| `chisel:packedice_stairs.1:8` | Large rough Packed Ice Brick Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:3` (Large Rough Ice Bricks) |
| `chisel:packedice_stairs.2:0` | Large Packed Ice Brick Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:4` (Large Ice Bricks) |
| `chisel:packedice_stairs.2:8` | Small Packed Ice Brick Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:5` (Small Ice Bricks) |
| `chisel:packedice_stairs.3:0` | Fancy Packed Ice Wall Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:6` (Fancy Glass Wall) |
| `chisel:packedice_stairs.3:8` | Large Packed Ice Tile Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:7` (Large Ice Tiles) |
| `chisel:packedice_stairs.4:0` | Fancy Packed Ice Tile Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:8` (Fancy Ice Tiles) |
| `chisel:packedice_stairs.4:8` | Sunken Packed Ice Tile Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:9` (Sunken Ice Tiles) |
| `chisel:packedice_stairs.5:0` | Disordered Packed Ice Tile Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:10` (Disordered Ice Tiles) |
| `chisel:packedice_stairs.5:8` | Packed Ice Panel Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:11` (Ice Panel) |
| `chisel:packedice_stairs.6:0` | Packed Ice Double Slab Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:12` (Ice Double Slab) |
| `chisel:packedice_stairs.6:8` | Packed Ice Zelda Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:13` (Ice Zelda Block) |
| `chisel:packedice_stairs.7:0` | Packed Ice Bismuth Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:14` (Ice Bismuth Block) |
| `chisel:packedice_stairs.7:8` | Packed Ice Poison Stairs | stairs-slab | no | arctic | stairs of `chisel:ice:15` (Ice Poison Block) |

### Natural: dirt, moss, leaves, mud, coral, cloud

Sheets: `natural.png`.

#### `DIRT` feature: `chisel:dirt`

Dirt shaped into bricks and tiles (shovel block).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:dirt:0` | Dirt Bricks in Disarray | rubble-ruin | partly | rural, ruin |  |
| `chisel:dirt:1` | Dirt Bricks Imitating Nether Brick Design | rubble-ruin | partly | rural, ruin |  |
| `chisel:dirt:2` | Dirt Bricks | rubble-ruin | partly | rural, ruin |  |
| `chisel:dirt:3` | Cobble-Dirt | rubble-ruin | partly | rural, ruin | dirt shaped into bricks and tiles (shovel block) |
| `chisel:dirt:4` | Reinforced Cobble-Dirt | rubble-ruin | partly | rural, ruin | connected texture |
| `chisel:dirt:5` | Reinforced Dirt | rubble-ruin | partly | rural, ruin | connected texture |
| `chisel:dirt:6` | Happy Dirt | off-theme | no | - | smiley face |
| `chisel:dirt:7` | Large Dirt Bricks | rubble-ruin | partly | rural, ruin |  |
| `chisel:dirt:8` | Large Dirt Bricks on top of Dirt | rubble-ruin | partly | rural, ruin | bricks over dirt (connected); connects vertically |
| `chisel:dirt:9` | Horizontal Dirt | rubble-ruin | partly | rural, ruin | connects horizontally |
| `chisel:dirt:10` | Vertical Dirt | rubble-ruin | partly | rural, ruin | connects vertically |
| `chisel:dirt:11` | Dirt Layers | rubble-ruin | partly | rural, ruin |  |
| `chisel:dirt:12` | Crumbling Dirt | rubble-ruin | partly | rural, ruin | connects vertically |
| `chisel:dirt:13` | Jumbled Dirt | rubble-ruin | partly | rural, ruin |  |
| `chisel:dirt:14` | Tilled Dirt | foliage, rubble-ruin | partly | rural, ruin | tilled look (garden bed) |
| `chisel:dirt:15` | Large Dirt Tile | rubble-ruin | partly | rural, ruin |  |

#### `MOSS` feature: `chisel:moss`

Solid moss block (wood material, grass sound).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:moss:0` | Lush | foliage | yes | overgrown | solid moss block (wood material, grass sound) |
| `chisel:moss:1` | Pale | foliage | yes | overgrown | solid moss block (wood material, grass sound) |
| `chisel:moss:2` | Dark | foliage | partly | overgrown |  |
| `chisel:moss:3` | Dry | foliage | yes | overgrown | solid moss block (wood material, grass sound) |
| `chisel:moss:4` | Golden | foliage | no | overgrown | solid moss block (wood material, grass sound) |
| `chisel:moss:5` | Infused | foliage | no | overgrown | solid moss block (wood material, grass sound) |
| `chisel:moss:6` | Mutated | foliage | no | overgrown | solid moss block (wood material, grass sound) |
| `chisel:moss:7` | Scorched | foliage | no | overgrown | solid moss block (wood material, grass sound) |
| `chisel:moss:8` | Vivid | foliage | partly | overgrown |  |
| `chisel:moss:9` | Peaty | foliage | yes | overgrown | solid moss block (wood material, grass sound) |
| `chisel:moss:10` | Pink Sphagnum | foliage | no | overgrown | solid moss block (wood material, grass sound) |

#### `MOSS_CARPET` feature: `chisel:moss_carpet`

Thin carpet layer: moss on roofs, floors and rubble. Behavior: Carpet shape (1/16 high).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:moss_carpet:0` | Lush | foliage | yes | overgrown | thin carpet layer: moss on roofs, floors and rubble |
| `chisel:moss_carpet:1` | Pale | foliage | yes | overgrown | thin carpet layer: moss on roofs, floors and rubble |
| `chisel:moss_carpet:2` | Dark | foliage | partly | overgrown |  |
| `chisel:moss_carpet:3` | Dry | foliage | yes | overgrown | thin carpet layer: moss on roofs, floors and rubble |
| `chisel:moss_carpet:4` | Golden | foliage | no | overgrown | thin carpet layer: moss on roofs, floors and rubble |
| `chisel:moss_carpet:5` | Infused | foliage | no | overgrown | thin carpet layer: moss on roofs, floors and rubble |
| `chisel:moss_carpet:6` | Mutated | foliage | no | overgrown | thin carpet layer: moss on roofs, floors and rubble |
| `chisel:moss_carpet:7` | Scorched | foliage | no | overgrown | thin carpet layer: moss on roofs, floors and rubble |
| `chisel:moss_carpet:8` | Vivid | foliage | partly | overgrown |  |
| `chisel:moss_carpet:9` | Peaty | foliage | yes | overgrown | thin carpet layer: moss on roofs, floors and rubble |
| `chisel:moss_carpet:10` | Pink Sphagnum | foliage | no | overgrown | thin carpet layer: moss on roofs, floors and rubble |

#### `LEAVES` feature: `chisel:leaves`

Leaf block that never decays; transparency follows the graphics setting like vanilla leaves.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:leaves:6` | Dead Leaves | foliage | yes | overgrown, ruin | dead leaves |
| `chisel:leaves:7` | Fancy Leaves | foliage | yes | standard, overgrown | dense hedge |
| `chisel:leaves:8` | Pink Petals | foliage | partly | standard, overgrown | pink blossoms |
| `chisel:leaves:9` | Red Rose | foliage | yes | standard, overgrown | hedge with red roses |
| `chisel:leaves:10` | White Rose | foliage | yes | standard, overgrown | hedge with white roses |
| `chisel:leaves:11` | Christmas Balls | off-theme | no | - | Christmas baubles |
| `chisel:leaves:12` | Christmas Lights | off-theme | no | - | Christmas lights |

#### `MUD` feature: `chisel:mud`

Mud (grass sound).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:mud:0` | Black Mud | rubble-ruin, foliage | partly | rural, overgrown |  |
| `chisel:mud:1` | Loamy Mud | rubble-ruin, foliage | partly | rural, overgrown |  |
| `chisel:mud:2` | Carbonate Mud (Marl) | wall-main | partly | desert | marl: smooth cream |
| `chisel:mud:3` | Brown Mud | rubble-ruin, foliage | partly | rural, overgrown |  |
| `chisel:mud:4` | Packed Mud | wall-main | partly | desert, rural | packed mud |
| `chisel:mud:5` | Rooted Mud | rubble-ruin, foliage | partly | rural, overgrown |  |
| `chisel:mud:6` | Mud Bricks | wall-main | yes | desert, rural | mud bricks (adobe) |

#### `CORAL` feature: `chisel:coral`

Coral (stone, does not die).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:coral:0` | Brain Coral | off-theme | no | tropical |  |
| `chisel:coral:1` | Bubble Coral | off-theme | no | tropical |  |
| `chisel:coral:2` | Fire Coral | off-theme | no | tropical |  |
| `chisel:coral:3` | Horn Coral | off-theme | no | tropical |  |
| `chisel:coral:4` | Tube Coral | off-theme | no | tropical |  |
| `chisel:coral:5` | Dead Brain Coral | off-theme | no | tropical |  |
| `chisel:coral:6` | Dead Bubble Coral | off-theme | no | tropical |  |
| `chisel:coral:7` | Dead Fire Coral | off-theme | no | tropical |  |
| `chisel:coral:8` | Dead Horn Coral | off-theme | no | tropical |  |
| `chisel:coral:9` | Dead Tube Coral | off-theme | no | tropical |  |
| `chisel:coral:10` | Brain Coral Bricks | wall-main | partly | tropical | coral (stone, does not die) |
| `chisel:coral:11` | Bubble Coral Bricks | wall-main | partly | tropical | coral (stone, does not die) |
| `chisel:coral:12` | Fire Coral Bricks | wall-main | partly | tropical | coral (stone, does not die) |
| `chisel:coral:13` | Horn Coral Bricks | wall-main | partly | tropical | coral (stone, does not die) |
| `chisel:coral:14` | Tube Coral Bricks | wall-main | partly | tropical | coral (stone, does not die) |
| `chisel:coral:15` | Dead Coral Bricks | wall-main | partly | tropical | coral (stone, does not die) |

#### `CLOUD` feature: `chisel:cloud`

Non-opaque; entities inside fall slowly and take no fall damage.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:cloud:0` | Cloud Block | off-theme | no | - |  |
| `chisel:cloud:1` | Large Cloud Bricks | off-theme | no | - |  |
| `chisel:cloud:2` | Small Cloud Bricks | off-theme | no | - |  |
| `chisel:cloud:3` | Small Vertical Cloud Bricks | off-theme | no | - |  |
| `chisel:cloud:4` | Gridded Cloud Bricks | off-theme | no | - |  |

### Furnishing and themed blocks

Sheets: `misc-deco.png`.

#### `BOOKSHELF` feature: `chisel:bookshelf`

Variety for upstream "L" bookshelf; flammable; connects horizontally. Behavior: Flammable.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:bookshelf:1` | Bookshelf with Rainbow Books | furniture-deco | yes | standard, border | connects horizontally |
| `chisel:bookshelf:2` | Necromancer’s Apprentice Bookshelf | furniture-deco | partly | gothic | variety for upstream "L" bookshelf; flammable; connects horizontally; connects horizontally |
| `chisel:bookshelf:3` | Necromancer's Bookshelf | furniture-deco | partly | gothic | variety for upstream "L" bookshelf; flammable; connects horizontally; connects horizontally |
| `chisel:bookshelf:4` | Bookshelf with Red Tomes | furniture-deco | yes | standard, border | connects horizontally |
| `chisel:bookshelf:5` | Abandoned Bookshelf | furniture-deco, rubble-ruin | yes | standard, ruin | cobwebbed, abandoned; connects horizontally |
| `chisel:bookshelf:6` | Hoarder's Bookshelf | furniture-deco | yes | standard, border | connects horizontally |
| `chisel:bookshelf:7` | Bookshelf filled to the brim with boring Pastel Books | furniture-deco | partly | standard, border | variety for upstream "L" bookshelf; flammable; connects horizontally; connects horizontally |
| `chisel:bookshelf:8` | Historian's Bookshelf | furniture-deco | yes | standard, border | connects horizontally |

#### `CARPET` feature: `chisel:carpet_block`

Full-block patterned rug.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:carpet_block:0` | White Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:1` | Orange Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:2` | Magenta Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:3` | Light Blue Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:4` | Yellow Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:5` | Light green Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:6` | Pink Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:7` | Gray Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:8` | Light Gray Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:9` | Teal Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:10` | Purple Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:11` | Blue Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:12` | Brown Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:13` | Green Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:14` | Red Carpet Block | floor | partly | standard, luxury | connected texture |
| `chisel:carpet_block:15` | Black Carpet Block | floor | partly | standard, luxury | connected texture |

#### `CARPET_FLOOR` feature: `chisel:carpet`

Thin patterned carpet layer (connected texture across a room). Behavior: Carpet shape (1/16 high).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:carpet:0` | White Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:1` | Orange Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:2` | Magenta Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:3` | Light Blue Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:4` | Yellow Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:5` | Light Green Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:6` | Pink Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:7` | Gray Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:8` | Light Gray Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:9` | Teal Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:10` | Purple Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:11` | Blue Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:12` | Brown Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:13` | Green Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:14` | Red Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |
| `chisel:carpet:15` | Black Carpet | floor, furniture-deco | yes | standard, luxury | connected texture |

#### `VALENTINES` feature: `chisel:valentines`

Pink hearts.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:valentines:0` | Valentines Bricks | off-theme | no | - |  |
| `chisel:valentines:1` | If it speaks, I wouldn't touch it | off-theme | no | - |  |
| `chisel:valentines:2` | Valentines Block | off-theme | no | - |  |
| `chisel:valentines:3` | Valentines Block | off-theme | no | - |  |
| `chisel:valentines:4` | Valentines Block | off-theme | no | - |  |
| `chisel:valentines:5` | Valentines Block | off-theme | no | - |  |
| `chisel:valentines:6` | Valentines Block | off-theme | no | - |  |
| `chisel:valentines:7` | Valentines Block | off-theme | no | - |  |
| `chisel:valentines:8` | Valentines Block | off-theme | no | - |  |
| `chisel:valentines:9` | Valentines Block | off-theme | no | - |  |

#### `NATION` feature: `chisel:nation`, `chisel:rebel`

Military camouflage and caution plates.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:nation:0` | Camouflaged | wall-accent | partly | industrial | pattern spans 2x2 blocks |
| `chisel:nation:1` | Camouflaged (Secluded) | wall-accent | partly | industrial | pattern spans 2x2 blocks |
| `chisel:nation:2` | Bolted Plate | wall-accent | partly | industrial |  |
| `chisel:nation:3` | White Caution Tape | wall-accent | partly | industrial | connected texture |
| `chisel:nation:4` | Teamed Caution Tape | wall-accent | partly | industrial | connected texture |
| `chisel:rebel:0` | Camouflaged | wall-accent | partly | industrial | pattern spans 2x2 blocks |
| `chisel:rebel:1` | Camouflaged (Secluded) | wall-accent | partly | industrial | pattern spans 2x2 blocks |
| `chisel:rebel:2` | Bolted Plate | wall-accent | partly | industrial |  |
| `chisel:rebel:3` | White Caution Tape | wall-accent | partly | industrial | connected texture |
| `chisel:rebel:4` | Teamed Caution Tape | wall-accent | partly | industrial | connected texture |

#### `PRESENT` feature: `chisel:present`

Gift-box chest with an inventory (tile entity); renders as a wrapped box.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:present:0` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:1` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:2` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:3` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:4` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:5` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:6` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:7` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:8` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:9` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:10` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:11` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:12` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:13` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:14` | A pretty present chest | off-theme | no | - |  |
| `chisel:present:15` | A pretty present chest | off-theme | no | - |  |

#### `ARCANE` feature: `chisel:arcane`

Thaumcraft-style arcane stone.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:arcane:0` | Engraved Moon | off-theme | no | ancient, gothic |  |
| `chisel:arcane:1` | Glowing Moon | off-theme | no | ancient, gothic |  |
| `chisel:arcane:2` | Beveled Tile | wall-accent | partly | ancient, gothic | Thaumcraft-style arcane stone; connected texture |
| `chisel:arcane:3` | Engraved Symbols | off-theme | no | ancient, gothic | random texture per block |
| `chisel:arcane:4` | Glowing Symbols | off-theme | no | ancient, gothic | random texture per block |
| `chisel:arcane:5` | Bricks | wall-main | partly | ancient, gothic | dark bricks; connected texture |
| `chisel:arcane:6` | Temporal Conduit | off-theme | no | ancient, gothic | connected texture |
| `chisel:arcane:7` | Braaainz | off-theme | no | ancient, gothic | connected texture |
| `chisel:arcane:8` | Infernally Arcane | off-theme | no | ancient, gothic | connected texture |
| `chisel:arcane:9` | Nasty Nazca Lines | off-theme | no | ancient, gothic | pattern spans 3x3 blocks |
| `chisel:arcane:10` | Fine Thaumaturge's Emblem | off-theme | no | ancient, gothic | pattern spans 3x3 blocks |
| `chisel:arcane:11` | Cracked Rock leaking Eldritch Glow | off-theme | no | ancient, gothic | pattern spans 3x3 blocks |

#### `BLOOD_RUNE` feature: `chisel:bloodRune`

Blood Magic rune stone (decorative only).

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:bloodRune:0` | Arranged Blood Rune | off-theme | no | ancient, gothic | connected texture |
| `chisel:bloodRune:1` | Blood Rune Bricks | wall-main | partly | gothic | Blood Magic rune stone (decorative only) |
| `chisel:bloodRune:2` | Carved Blood Rune | off-theme | no | ancient, gothic |  |
| `chisel:bloodRune:3` | Radial Carved Blood Rune | off-theme | no | ancient, gothic |  |
| `chisel:bloodRune:4` | Classic Panel Blood Rune | off-theme | no | ancient, gothic |  |
| `chisel:bloodRune:5` | Blood Rune Tiles | floor | partly | gothic | Blood Magic rune stone (decorative only) |

#### `TALLOW` feature: `chisel:tallow`

Thaumcraft tallow.

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:tallow:0` | Smooth Tallow Block | off-theme | no | - |  |
| `chisel:tallow:1` | Smooth Tallow Block | off-theme | no | - |  |

### Planks

Sheets: `planks-vanilla.png`, `planks-bop-1 to -3.png`, `planks-thaumcraft.png`.

#### `WOOD`, `WOOD_BOP`, `WOOD_THAUMCRAFT` features: `chisel:<wood>_planks`

Twenty-two woods, each with the same 15 patterns on metas 1 to 15 of `chisel:<wood>_planks` (meta 0 is unused; the carving group adds the source mod's plain planks as the base), 330 variants in all. Flammable. The Biomes O' Plenty and Thaumcraft woods register because both mods are in the pack.

| Meta | Pattern (English name for oak) | Roles | Notes |
|---|---|---|---|
| 1 | Smooth Oak Wood Planks | floor, wall-main | smooth planks |
| 2 | Short Oak Wood Planks | floor |  |
| 3 | Vertical Oak Wood Planks | wall-main | vertical boards |
| 4 | Vertical Uneven Oak Wood Planks | wall-main | vertical boards |
| 5 | Oak Wood Parquet | floor | parquet |
| 6 | Fancy Oak Wood Plank Arrangement | floor |  |
| 7 | Oak Wood Plank Blinds | wall-accent | shutters |
| 8 | Oak Wood Panel | wall-accent, ceiling | panel |
| 9 | Oak Wood Double Slab | floor, ceiling |  |
| 10 | Oak Wood Crate | furniture-deco | crate |
| 11 | Fancy Oak Wood Crate | furniture-deco | crate |
| 12 | Oak Wood Scaffold | special | scaffold frame painted on a solid block |
| 13 | Large Long Oak Wood Planks | floor |  |
| 14 | Oak Wood Planks in Disarray | rubble-ruin | boards in disarray |
| 15 | Vertical Oak Wood Planks in Disarray | rubble-ruin | boards in disarray |

| Wood | Block | Feature | Fits upstream? | Styles | Look |
|---|---|---|---|---|---|
| oak | `chisel:oak_planks` | `WOOD` | yes | standard, rural |  |
| spruce | `chisel:spruce_planks` | `WOOD` | yes | standard, rural |  |
| birch | `chisel:birch_planks` | `WOOD` | yes | standard, rural |  |
| jungle | `chisel:jungle_planks` | `WOOD` | yes | standard, rural |  |
| acacia | `chisel:acacia_planks` | `WOOD` | yes | standard, desert |  |
| dark oak | `chisel:dark_oak_planks` | `WOOD` | yes | standard, rural |  |
| sacredoak | `chisel:sacredoak_planks` | `WOOD_BOP` | no | overgrown | vine-covered |
| cherry | `chisel:cherry_planks` | `WOOD_BOP` | partly | standard, asian | deep red |
| dark | `chisel:dark_planks` | `WOOD_BOP` | partly | gothic | purple-gray |
| fir | `chisel:fir_planks` | `WOOD_BOP` | yes | standard, rural |  |
| ethereal | `chisel:ethereal_planks` | `WOOD_BOP` | no | - | teal |
| magic | `chisel:magic_planks` | `WOOD_BOP` | no | - | saturated blue |
| mangrove | `chisel:mangrove_planks` | `WOOD_BOP` | partly | modern, desert | near-white |
| palm | `chisel:palm_planks` | `WOOD_BOP` | yes | standard, desert, tropical |  |
| redwood | `chisel:redwood_planks` | `WOOD_BOP` | yes | standard, rural |  |
| willow | `chisel:willow_planks` | `WOOD_BOP` | partly | rural | gray-green |
| pine | `chisel:pine_planks` | `WOOD_BOP` | yes | standard, rural |  |
| hellbark | `chisel:hellbark_planks` | `WOOD_BOP` | yes | standard, desert |  |
| jacaranda | `chisel:jacaranda_planks` | `WOOD_BOP` | partly | modern | pale pink-gray |
| mahogany | `chisel:mahogany_planks` | `WOOD_BOP` | partly | standard, luxury | pink-red |
| greatwood | `chisel:greatwood_planks` | `WOOD_THAUMCRAFT` | yes | standard, gothic | very dark brown |
| silverwood | `chisel:silverwood_planks` | `WOOD_THAUMCRAFT` | partly | modern, luxury | near-white |

<details><summary>Every plank variant (330 rows)</summary>

| Block:meta | English name | Roles | Fits upstream? | Styles | Notes |
|---|---|---|---|---|---|
| `chisel:oak_planks:1` | Smooth Oak Wood Planks | floor, wall-main | yes | standard, rural | smooth planks |
| `chisel:oak_planks:2` | Short Oak Wood Planks | floor | yes | standard, rural |  |
| `chisel:oak_planks:3` | Vertical Oak Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:oak_planks:4` | Vertical Uneven Oak Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:oak_planks:5` | Oak Wood Parquet | floor | yes | standard, rural | parquet |
| `chisel:oak_planks:6` | Fancy Oak Wood Plank Arrangement | floor | yes | standard, rural |  |
| `chisel:oak_planks:7` | Oak Wood Plank Blinds | wall-accent | yes | standard, rural | shutters |
| `chisel:oak_planks:8` | Oak Wood Panel | wall-accent, ceiling | yes | standard, rural | panel |
| `chisel:oak_planks:9` | Oak Wood Double Slab | floor, ceiling | yes | standard, rural |  |
| `chisel:oak_planks:10` | Oak Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:oak_planks:11` | Fancy Oak Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:oak_planks:12` | Oak Wood Scaffold | special | yes | standard, rural | scaffold frame painted on a solid block |
| `chisel:oak_planks:13` | Large Long Oak Wood Planks | floor | yes | standard, rural |  |
| `chisel:oak_planks:14` | Oak Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:oak_planks:15` | Vertical Oak Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:spruce_planks:1` | Smooth Spruce Wood Planks | floor, wall-main | yes | standard, rural | smooth planks |
| `chisel:spruce_planks:2` | Short Spruce Wood Planks | floor | yes | standard, rural |  |
| `chisel:spruce_planks:3` | Vertical Spruce Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:spruce_planks:4` | Vertical Uneven Spruce Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:spruce_planks:5` | Spruce Wood Parquet | floor | yes | standard, rural | parquet |
| `chisel:spruce_planks:6` | Fancy Spruce Wood Plank Arrangement | floor | yes | standard, rural |  |
| `chisel:spruce_planks:7` | Spruce Wood Plank Blinds | wall-accent | yes | standard, rural | shutters |
| `chisel:spruce_planks:8` | Spruce Wood Panel | wall-accent, ceiling | yes | standard, rural | panel |
| `chisel:spruce_planks:9` | Spruce Wood Double Slab | floor, ceiling | yes | standard, rural |  |
| `chisel:spruce_planks:10` | Spruce Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:spruce_planks:11` | Fancy Spruce Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:spruce_planks:12` | Spruce Wood Scaffold | special | yes | standard, rural | scaffold frame painted on a solid block |
| `chisel:spruce_planks:13` | Large Long Spruce Wood Planks | floor | yes | standard, rural |  |
| `chisel:spruce_planks:14` | Spruce Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:spruce_planks:15` | Vertical Spruce Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:birch_planks:1` | Smooth Birch Wood Planks | floor, wall-main | yes | standard, rural | smooth planks |
| `chisel:birch_planks:2` | Short Birch Wood Planks | floor | yes | standard, rural |  |
| `chisel:birch_planks:3` | Vertical Birch Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:birch_planks:4` | Vertical Uneven Birch Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:birch_planks:5` | Birch Wood Parquet | floor | yes | standard, rural | parquet |
| `chisel:birch_planks:6` | Fancy Birch Wood Plank Arrangement | floor | yes | standard, rural |  |
| `chisel:birch_planks:7` | Birch Wood Plank Blinds | wall-accent | yes | standard, rural | shutters |
| `chisel:birch_planks:8` | Birch Wood Panel | wall-accent, ceiling | yes | standard, rural | panel |
| `chisel:birch_planks:9` | Birch Wood Double Slab | floor, ceiling | yes | standard, rural |  |
| `chisel:birch_planks:10` | Birch Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:birch_planks:11` | Fancy Birch Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:birch_planks:12` | Birch Wood Scaffold | special | yes | standard, rural | scaffold frame painted on a solid block |
| `chisel:birch_planks:13` | Large Long Birch Wood Planks | floor | yes | standard, rural |  |
| `chisel:birch_planks:14` | Birch Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:birch_planks:15` | Vertical Birch Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:jungle_planks:1` | Smooth Jungle Wood Planks | floor, wall-main | yes | standard, rural | smooth planks |
| `chisel:jungle_planks:2` | Short Jungle Wood Planks | floor | yes | standard, rural |  |
| `chisel:jungle_planks:3` | Vertical Jungle Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:jungle_planks:4` | Vertical Uneven Jungle Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:jungle_planks:5` | Jungle Wood Parquet | floor | yes | standard, rural | parquet |
| `chisel:jungle_planks:6` | Fancy Jungle Wood Plank Arrangement | floor | yes | standard, rural |  |
| `chisel:jungle_planks:7` | Jungle Wood Plank Blinds | wall-accent | yes | standard, rural | shutters |
| `chisel:jungle_planks:8` | Jungle Wood Panel | wall-accent, ceiling | yes | standard, rural | panel |
| `chisel:jungle_planks:9` | Jungle Wood Double Slab | floor, ceiling | yes | standard, rural |  |
| `chisel:jungle_planks:10` | Oak Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:jungle_planks:11` | Fancy jungle Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:jungle_planks:12` | Jungle Wood Scaffold | special | yes | standard, rural | scaffold frame painted on a solid block |
| `chisel:jungle_planks:13` | Large Long Jungle Wood Planks | floor | yes | standard, rural |  |
| `chisel:jungle_planks:14` | Jungle Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:jungle_planks:15` | Vertical Jungle Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:acacia_planks:1` | Smooth Acacia Wood Planks | floor, wall-main | yes | standard, desert | smooth planks |
| `chisel:acacia_planks:2` | Short Acacia Wood Planks | floor | yes | standard, desert |  |
| `chisel:acacia_planks:3` | Vertical Acacia Wood Planks | wall-main | yes | standard, desert | vertical boards |
| `chisel:acacia_planks:4` | Vertical Uneven Acacia Wood Planks | wall-main | yes | standard, desert | vertical boards |
| `chisel:acacia_planks:5` | Acacia Wood Parquet | floor | yes | standard, desert | parquet |
| `chisel:acacia_planks:6` | Fancy Acacia Wood Plank Arrangement | floor | yes | standard, desert |  |
| `chisel:acacia_planks:7` | Acacia Wood Plank Blinds | wall-accent | yes | standard, desert | shutters |
| `chisel:acacia_planks:8` | Acacia Wood Panel | wall-accent, ceiling | yes | standard, desert | panel |
| `chisel:acacia_planks:9` | Acacia Wood Double Slab | floor, ceiling | yes | standard, desert |  |
| `chisel:acacia_planks:10` | Acacia Wood Crate | furniture-deco | yes | standard, desert | crate |
| `chisel:acacia_planks:11` | Fancy Acacia Wood Crate | furniture-deco | yes | standard, desert | crate |
| `chisel:acacia_planks:12` | Acacia Wood Scaffold | special | yes | standard, desert | scaffold frame painted on a solid block |
| `chisel:acacia_planks:13` | Large Long Acacia Wood Planks | floor | yes | standard, desert |  |
| `chisel:acacia_planks:14` | Acacia Wood Planks in Disarray | rubble-ruin | yes | standard, desert, ruin | boards in disarray |
| `chisel:acacia_planks:15` | Vertical Acacia Wood Planks in Disarray | rubble-ruin | yes | standard, desert, ruin | boards in disarray |
| `chisel:dark_oak_planks:1` | Smooth Dark Oak Wood Planks | floor, wall-main | yes | standard, rural | smooth planks |
| `chisel:dark_oak_planks:2` | Short Dark Oak Wood Planks | floor | yes | standard, rural |  |
| `chisel:dark_oak_planks:3` | Vertical Dark Oak Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:dark_oak_planks:4` | Vertical Uneven Dark Oak Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:dark_oak_planks:5` | Dark Oak Wood Parquet | floor | yes | standard, rural | parquet |
| `chisel:dark_oak_planks:6` | Fancy Dark Oak Wood Plank Arrangement | floor | yes | standard, rural |  |
| `chisel:dark_oak_planks:7` | Dark Oak Wood Plank Blinds | wall-accent | yes | standard, rural | shutters |
| `chisel:dark_oak_planks:8` | Dark Oak Wood Panel | wall-accent, ceiling | yes | standard, rural | panel |
| `chisel:dark_oak_planks:9` | Dark Oak Wood Double Slab | floor, ceiling | yes | standard, rural |  |
| `chisel:dark_oak_planks:10` | Dark Oak Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:dark_oak_planks:11` | Fancy Dark Oak Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:dark_oak_planks:12` | Dark Oak Wood Scaffold | special | yes | standard, rural | scaffold frame painted on a solid block |
| `chisel:dark_oak_planks:13` | Large Long Dark Oak Wood Planks | floor | yes | standard, rural |  |
| `chisel:dark_oak_planks:14` | Dark Oak Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:dark_oak_planks:15` | Vertical Dark Oak Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:sacredoak_planks:1` | Smooth Sacred Oak Wood Planks | floor, wall-main | no | overgrown | smooth planks |
| `chisel:sacredoak_planks:2` | Short Sacred Oak Wood Planks | floor | no | overgrown |  |
| `chisel:sacredoak_planks:3` | Vertical Sacred Oak Wood Planks | wall-main | no | overgrown | vertical boards |
| `chisel:sacredoak_planks:4` | Vertical Uneven Sacred Oak Wood Planks | wall-main | no | overgrown | vertical boards |
| `chisel:sacredoak_planks:5` | Sacred Oak Wood Parquet | floor | no | overgrown | parquet |
| `chisel:sacredoak_planks:6` | Fancy Sacred Oak Wood Plank Arrangement | floor | no | overgrown |  |
| `chisel:sacredoak_planks:7` | Sacred Oak Wood Plank Blinds | wall-accent | no | overgrown | shutters |
| `chisel:sacredoak_planks:8` | Sacred Oak Wood Panel | wall-accent, ceiling | no | overgrown | panel |
| `chisel:sacredoak_planks:9` | Sacred Oak Wood Double Slab | floor, ceiling | no | overgrown |  |
| `chisel:sacredoak_planks:10` | Sacred Oak Wood Crate | furniture-deco | no | overgrown | crate |
| `chisel:sacredoak_planks:11` | Fancy Sacred Oak Wood Crate | furniture-deco | no | overgrown | crate |
| `chisel:sacredoak_planks:12` | Sacred Oak Wood Scaffold | special | no | overgrown | scaffold frame painted on a solid block |
| `chisel:sacredoak_planks:13` | Large Long Sacred Oak Wood Planks | floor | no | overgrown |  |
| `chisel:sacredoak_planks:14` | Sacred Oak Wood Planks in Disarray | rubble-ruin | no | overgrown, ruin | boards in disarray |
| `chisel:sacredoak_planks:15` | Vertical Sacred Oak Wood Planks in Disarray | rubble-ruin | no | overgrown, ruin | boards in disarray |
| `chisel:cherry_planks:1` | Smooth Cherry Wood Planks | floor, wall-main | partly | standard, asian | smooth planks |
| `chisel:cherry_planks:2` | Short Cherry Wood Planks | floor | partly | standard, asian |  |
| `chisel:cherry_planks:3` | Vertical Cherry Wood Planks | wall-main | partly | standard, asian | vertical boards |
| `chisel:cherry_planks:4` | Vertical Uneven Cherry Wood Planks | wall-main | partly | standard, asian | vertical boards |
| `chisel:cherry_planks:5` | Cherry Wood Parquet | floor | partly | standard, asian | parquet |
| `chisel:cherry_planks:6` | Fancy Cherry Wood Plank Arrangement | floor | partly | standard, asian |  |
| `chisel:cherry_planks:7` | Cherry Wood Plank Blinds | wall-accent | partly | standard, asian | shutters |
| `chisel:cherry_planks:8` | Cherry Wood Panel | wall-accent, ceiling | partly | standard, asian | panel |
| `chisel:cherry_planks:9` | Cherry Wood Double Slab | floor, ceiling | partly | standard, asian |  |
| `chisel:cherry_planks:10` | Cherry Wood Crate | furniture-deco | partly | standard, asian | crate |
| `chisel:cherry_planks:11` | Fancy Cherry Wood Crate | furniture-deco | partly | standard, asian | crate |
| `chisel:cherry_planks:12` | Cherry Wood Scaffold | special | partly | standard, asian | scaffold frame painted on a solid block |
| `chisel:cherry_planks:13` | Large Long Cherry Wood Planks | floor | partly | standard, asian |  |
| `chisel:cherry_planks:14` | Cherry Wood Planks in Disarray | rubble-ruin | partly | standard, asian, ruin | boards in disarray |
| `chisel:cherry_planks:15` | Vertical Cherry Wood Planks in Disarray | rubble-ruin | partly | standard, asian, ruin | boards in disarray |
| `chisel:dark_planks:1` | Smooth Dark Wood Planks | floor, wall-main | partly | gothic | smooth planks |
| `chisel:dark_planks:2` | Short Dark Wood Planks | floor | partly | gothic |  |
| `chisel:dark_planks:3` | Vertical Dark Wood Planks | wall-main | partly | gothic | vertical boards |
| `chisel:dark_planks:4` | Vertical Uneven Dark Wood Planks | wall-main | partly | gothic | vertical boards |
| `chisel:dark_planks:5` | Dark Wood Parquet | floor | partly | gothic | parquet |
| `chisel:dark_planks:6` | Fancy Dark Wood Plank Arrangement | floor | partly | gothic |  |
| `chisel:dark_planks:7` | Dark Wood Plank Blinds | wall-accent | partly | gothic | shutters |
| `chisel:dark_planks:8` | Dark Wood Panel | wall-accent, ceiling | partly | gothic | panel |
| `chisel:dark_planks:9` | Dark Wood Double Slab | floor, ceiling | partly | gothic |  |
| `chisel:dark_planks:10` | Dark Wood Crate | furniture-deco | partly | gothic | crate |
| `chisel:dark_planks:11` | Fancy Dark Wood Crate | furniture-deco | partly | gothic | crate |
| `chisel:dark_planks:12` | Dark Wood Scaffold | special | partly | gothic | scaffold frame painted on a solid block |
| `chisel:dark_planks:13` | Large Long Dark Wood Planks | floor | partly | gothic |  |
| `chisel:dark_planks:14` | Dark Wood Planks in Disarray | rubble-ruin | partly | gothic, ruin | boards in disarray |
| `chisel:dark_planks:15` | Vertical Dark Wood Planks in Disarray | rubble-ruin | partly | gothic, ruin | boards in disarray |
| `chisel:fir_planks:1` | Smooth Fir Wood Planks | floor, wall-main | yes | standard, rural | smooth planks |
| `chisel:fir_planks:2` | Short Fir Wood Planks | floor | yes | standard, rural |  |
| `chisel:fir_planks:3` | Vertical Fir Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:fir_planks:4` | Vertical Uneven Fir Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:fir_planks:5` | Fir Wood Parquet | floor | yes | standard, rural | parquet |
| `chisel:fir_planks:6` | Fancy Fir Wood Plank Arrangement | floor | yes | standard, rural |  |
| `chisel:fir_planks:7` | Fir Wood Plank Blinds | wall-accent | yes | standard, rural | shutters |
| `chisel:fir_planks:8` | Fir Wood Panel | wall-accent, ceiling | yes | standard, rural | panel |
| `chisel:fir_planks:9` | Fir Wood Double Slab | floor, ceiling | yes | standard, rural |  |
| `chisel:fir_planks:10` | Fir Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:fir_planks:11` | Fancy Fir Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:fir_planks:12` | Fir Wood Scaffold | special | yes | standard, rural | scaffold frame painted on a solid block |
| `chisel:fir_planks:13` | Large Long Fir Wood Planks | floor | yes | standard, rural |  |
| `chisel:fir_planks:14` | Fir Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:fir_planks:15` | Vertical Fir Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:ethereal_planks:1` | Smooth Ethereal Wood Planks | floor, wall-main | no | - | smooth planks |
| `chisel:ethereal_planks:2` | Short Ethereal Wood Planks | floor | no | - |  |
| `chisel:ethereal_planks:3` | Vertical Ethereal Wood Planks | wall-main | no | - | vertical boards |
| `chisel:ethereal_planks:4` | Vertical Uneven Ethereal Wood Planks | wall-main | no | - | vertical boards |
| `chisel:ethereal_planks:5` | Ethereal Wood Parquet | floor | no | - | parquet |
| `chisel:ethereal_planks:6` | Fancy Ethereal Wood Plank Arrangement | floor | no | - |  |
| `chisel:ethereal_planks:7` | Ethereal Wood Plank Blinds | wall-accent | no | - | shutters |
| `chisel:ethereal_planks:8` | Ethereal Wood Panel | wall-accent, ceiling | no | - | panel |
| `chisel:ethereal_planks:9` | Ethereal Wood Double Slab | floor, ceiling | no | - |  |
| `chisel:ethereal_planks:10` | Ethereal Wood Crate | furniture-deco | no | - | crate |
| `chisel:ethereal_planks:11` | Fancy Ethereal Wood Crate | furniture-deco | no | - | crate |
| `chisel:ethereal_planks:12` | Ethereal Wood Scaffold | special | no | - | scaffold frame painted on a solid block |
| `chisel:ethereal_planks:13` | Large Long Ethereal Wood Planks | floor | no | - |  |
| `chisel:ethereal_planks:14` | Ethereal Wood Planks in Disarray | rubble-ruin | no | ruin | boards in disarray |
| `chisel:ethereal_planks:15` | Vertical Ethereal Wood Planks in Disarray | rubble-ruin | no | ruin | boards in disarray |
| `chisel:magic_planks:1` | Smooth Magic Wood Planks | floor, wall-main | no | - | smooth planks |
| `chisel:magic_planks:2` | Short Magic Wood Planks | floor | no | - |  |
| `chisel:magic_planks:3` | Vertical Magic Wood Planks | wall-main | no | - | vertical boards |
| `chisel:magic_planks:4` | Vertical Uneven Magic Wood Planks | wall-main | no | - | vertical boards |
| `chisel:magic_planks:5` | Magic Wood Parquet | floor | no | - | parquet |
| `chisel:magic_planks:6` | Fancy Magic Wood Plank Arrangement | floor | no | - |  |
| `chisel:magic_planks:7` | Magic Wood Plank Blinds | wall-accent | no | - | shutters |
| `chisel:magic_planks:8` | Magic Wood Panel | wall-accent, ceiling | no | - | panel |
| `chisel:magic_planks:9` | Magic Wood Double Slab | floor, ceiling | no | - |  |
| `chisel:magic_planks:10` | Magic Wood Crate | furniture-deco | no | - | crate |
| `chisel:magic_planks:11` | Fancy Magic Wood Crate | furniture-deco | no | - | crate |
| `chisel:magic_planks:12` | Magic Wood Scaffold | special | no | - | scaffold frame painted on a solid block |
| `chisel:magic_planks:13` | Large Long Magic Wood Planks | floor | no | - |  |
| `chisel:magic_planks:14` | Magic Wood Planks in Disarray | rubble-ruin | no | ruin | boards in disarray |
| `chisel:magic_planks:15` | Vertical Magic Wood Planks in Disarray | rubble-ruin | no | ruin | boards in disarray |
| `chisel:mangrove_planks:1` | Smooth Mangrove Wood Planks | floor, wall-main | partly | modern, desert | smooth planks |
| `chisel:mangrove_planks:2` | Short Mangrove Wood Planks | floor | partly | modern, desert |  |
| `chisel:mangrove_planks:3` | Vertical Mangrove Wood Planks | wall-main | partly | modern, desert | vertical boards |
| `chisel:mangrove_planks:4` | Vertical Uneven Mangrove Wood Planks | wall-main | partly | modern, desert | vertical boards |
| `chisel:mangrove_planks:5` | Mangrove Wood Parquet | floor | partly | modern, desert | parquet |
| `chisel:mangrove_planks:6` | Fancy Mangrove Wood Plank Arrangement | floor | partly | modern, desert |  |
| `chisel:mangrove_planks:7` | Mangrove Wood Plank Blinds | wall-accent | partly | modern, desert | shutters |
| `chisel:mangrove_planks:8` | Mangrove Wood Panel | wall-accent, ceiling | partly | modern, desert | panel |
| `chisel:mangrove_planks:9` | Mangrove Wood Double Slab | floor, ceiling | partly | modern, desert |  |
| `chisel:mangrove_planks:10` | Mangrove Wood Crate | furniture-deco | partly | modern, desert | crate |
| `chisel:mangrove_planks:11` | Fancy Mangrove Wood Crate | furniture-deco | partly | modern, desert | crate |
| `chisel:mangrove_planks:12` | Mangrove Wood Scaffold | special | partly | modern, desert | scaffold frame painted on a solid block |
| `chisel:mangrove_planks:13` | Large Long Mangrove Wood Planks | floor | partly | modern, desert |  |
| `chisel:mangrove_planks:14` | Mangrove Wood Planks in Disarray | rubble-ruin | partly | modern, desert, ruin | boards in disarray |
| `chisel:mangrove_planks:15` | Vertical Mangrove Wood Planks in Disarray | rubble-ruin | partly | modern, desert, ruin | boards in disarray |
| `chisel:palm_planks:1` | Smooth Palm Wood Planks | floor, wall-main | yes | standard, desert, tropical | smooth planks |
| `chisel:palm_planks:2` | Short Palm Wood Planks | floor | yes | standard, desert, tropical |  |
| `chisel:palm_planks:3` | Vertical Palm Wood Planks | wall-main | yes | standard, desert, tropical | vertical boards |
| `chisel:palm_planks:4` | Vertical Uneven Palm Wood Planks | wall-main | yes | standard, desert, tropical | vertical boards |
| `chisel:palm_planks:5` | Palm Wood Parquet | floor | yes | standard, desert, tropical | parquet |
| `chisel:palm_planks:6` | Fancy Palm Wood Plank Arrangement | floor | yes | standard, desert, tropical |  |
| `chisel:palm_planks:7` | Palm Wood Plank Blinds | wall-accent | yes | standard, desert, tropical | shutters |
| `chisel:palm_planks:8` | Palm Wood Panel | wall-accent, ceiling | yes | standard, desert, tropical | panel |
| `chisel:palm_planks:9` | Palm Wood Double Slab | floor, ceiling | yes | standard, desert, tropical |  |
| `chisel:palm_planks:10` | Palm Wood Crate | furniture-deco | yes | standard, desert, tropical | crate |
| `chisel:palm_planks:11` | Fancy Palm Wood Crate | furniture-deco | yes | standard, desert, tropical | crate |
| `chisel:palm_planks:12` | Palm Wood Scaffold | special | yes | standard, desert, tropical | scaffold frame painted on a solid block |
| `chisel:palm_planks:13` | Large Long Palm Wood Planks | floor | yes | standard, desert, tropical |  |
| `chisel:palm_planks:14` | Palm Wood Planks in Disarray | rubble-ruin | yes | standard, desert, tropical, ruin | boards in disarray |
| `chisel:palm_planks:15` | Vertical Palm Wood Planks in Disarray | rubble-ruin | yes | standard, desert, tropical, ruin | boards in disarray |
| `chisel:redwood_planks:1` | Smooth Redwood Wood Planks | floor, wall-main | yes | standard, rural | smooth planks |
| `chisel:redwood_planks:2` | Short Redwood Wood Planks | floor | yes | standard, rural |  |
| `chisel:redwood_planks:3` | Vertical Redwood Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:redwood_planks:4` | Vertical Uneven Redwood Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:redwood_planks:5` | Redwood Wood Parquet | floor | yes | standard, rural | parquet |
| `chisel:redwood_planks:6` | Fancy Redwood Wood Plank Arrangement | floor | yes | standard, rural |  |
| `chisel:redwood_planks:7` | Redwood Wood Plank Blinds | wall-accent | yes | standard, rural | shutters |
| `chisel:redwood_planks:8` | Redwood Wood Panel | wall-accent, ceiling | yes | standard, rural | panel |
| `chisel:redwood_planks:9` | Redwood Wood Double Slab | floor, ceiling | yes | standard, rural |  |
| `chisel:redwood_planks:10` | Redwood Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:redwood_planks:11` | Fancy Redwood Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:redwood_planks:12` | Redwood Wood Scaffold | special | yes | standard, rural | scaffold frame painted on a solid block |
| `chisel:redwood_planks:13` | Large Long Redwood Wood Planks | floor | yes | standard, rural |  |
| `chisel:redwood_planks:14` | Redwood Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:redwood_planks:15` | Vertical Redwood Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:willow_planks:1` | Smooth Willow Wood Planks | floor, wall-main | partly | rural | smooth planks |
| `chisel:willow_planks:2` | Short Willow Wood Planks | floor | partly | rural |  |
| `chisel:willow_planks:3` | Vertical Willow Wood Planks | wall-main | partly | rural | vertical boards |
| `chisel:willow_planks:4` | Vertical Uneven Willow Wood Planks | wall-main | partly | rural | vertical boards |
| `chisel:willow_planks:5` | Willow Wood Parquet | floor | partly | rural | parquet |
| `chisel:willow_planks:6` | Fancy Willow Wood Plank Arrangement | floor | partly | rural |  |
| `chisel:willow_planks:7` | Willow Wood Plank Blinds | wall-accent | partly | rural | shutters |
| `chisel:willow_planks:8` | Willow Wood Panel | wall-accent, ceiling | partly | rural | panel |
| `chisel:willow_planks:9` | Willow Wood Double Slab | floor, ceiling | partly | rural |  |
| `chisel:willow_planks:10` | Willow Wood Crate | furniture-deco | partly | rural | crate |
| `chisel:willow_planks:11` | Fancy Willow Wood Crate | furniture-deco | partly | rural | crate |
| `chisel:willow_planks:12` | Willow Wood Scaffold | special | partly | rural | scaffold frame painted on a solid block |
| `chisel:willow_planks:13` | Large Long Willow Wood Planks | floor | partly | rural |  |
| `chisel:willow_planks:14` | Willow Wood Planks in Disarray | rubble-ruin | partly | rural, ruin | boards in disarray |
| `chisel:willow_planks:15` | Vertical Willow Wood Planks in Disarray | rubble-ruin | partly | rural, ruin | boards in disarray |
| `chisel:pine_planks:1` | Smooth Pine Wood Planks | floor, wall-main | yes | standard, rural | smooth planks |
| `chisel:pine_planks:2` | Short Pine Wood Planks | floor | yes | standard, rural |  |
| `chisel:pine_planks:3` | Vertical Pine Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:pine_planks:4` | Vertical Uneven Pine Wood Planks | wall-main | yes | standard, rural | vertical boards |
| `chisel:pine_planks:5` | Pine Wood Parquet | floor | yes | standard, rural | parquet |
| `chisel:pine_planks:6` | Fancy Pine Wood Plank Arrangement | floor | yes | standard, rural |  |
| `chisel:pine_planks:7` | Pine Wood Plank Blinds | wall-accent | yes | standard, rural | shutters |
| `chisel:pine_planks:8` | Pine Wood Panel | wall-accent, ceiling | yes | standard, rural | panel |
| `chisel:pine_planks:9` | Pine Wood Double Slab | floor, ceiling | yes | standard, rural |  |
| `chisel:pine_planks:10` | Pine Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:pine_planks:11` | Fancy Pine Wood Crate | furniture-deco | yes | standard, rural | crate |
| `chisel:pine_planks:12` | Pine Wood Scaffold | special | yes | standard, rural | scaffold frame painted on a solid block |
| `chisel:pine_planks:13` | Large Long Pine Wood Planks | floor | yes | standard, rural |  |
| `chisel:pine_planks:14` | Pine Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:pine_planks:15` | Vertical Pine Wood Planks in Disarray | rubble-ruin | yes | standard, rural, ruin | boards in disarray |
| `chisel:hellbark_planks:1` | Smooth Hellbark Wood Planks | floor, wall-main | yes | standard, desert | smooth planks |
| `chisel:hellbark_planks:2` | Short Hellbark Wood Planks | floor | yes | standard, desert |  |
| `chisel:hellbark_planks:3` | Vertical Hellbark Wood Planks | wall-main | yes | standard, desert | vertical boards |
| `chisel:hellbark_planks:4` | Vertical Uneven Hellbark Wood Planks | wall-main | yes | standard, desert | vertical boards |
| `chisel:hellbark_planks:5` | Hellbark Wood Parquet | floor | yes | standard, desert | parquet |
| `chisel:hellbark_planks:6` | Fancy Hellbark Wood Plank Arrangement | floor | yes | standard, desert |  |
| `chisel:hellbark_planks:7` | Hellbark Wood Plank Blinds | wall-accent | yes | standard, desert | shutters |
| `chisel:hellbark_planks:8` | Hellbark Wood Panel | wall-accent, ceiling | yes | standard, desert | panel |
| `chisel:hellbark_planks:9` | Hellbark Wood Double Slab | floor, ceiling | yes | standard, desert |  |
| `chisel:hellbark_planks:10` | Hellbark Wood Crate | furniture-deco | yes | standard, desert | crate |
| `chisel:hellbark_planks:11` | Fancy Hellbark Wood Crate | furniture-deco | yes | standard, desert | crate |
| `chisel:hellbark_planks:12` | Hellbark Wood Scaffold | special | yes | standard, desert | scaffold frame painted on a solid block |
| `chisel:hellbark_planks:13` | Large Long Hellbark Wood Planks | floor | yes | standard, desert |  |
| `chisel:hellbark_planks:14` | Hellbark Wood Planks in Disarray | rubble-ruin | yes | standard, desert, ruin | boards in disarray |
| `chisel:hellbark_planks:15` | Vertical Hellbark Wood Planks in Disarray | rubble-ruin | yes | standard, desert, ruin | boards in disarray |
| `chisel:jacaranda_planks:1` | Smooth Jacaranda Wood Planks | floor, wall-main | partly | modern | smooth planks |
| `chisel:jacaranda_planks:2` | Short Jacaranda Wood Planks | floor | partly | modern |  |
| `chisel:jacaranda_planks:3` | Vertical Jacaranda Wood Planks | wall-main | partly | modern | vertical boards |
| `chisel:jacaranda_planks:4` | Vertical Uneven Jacaranda Wood Planks | wall-main | partly | modern | vertical boards |
| `chisel:jacaranda_planks:5` | Jacaranda Wood Parquet | floor | partly | modern | parquet |
| `chisel:jacaranda_planks:6` | Fancy Jacaranda Wood Plank Arrangement | floor | partly | modern |  |
| `chisel:jacaranda_planks:7` | Jacaranda Wood Plank Blinds | wall-accent | partly | modern | shutters |
| `chisel:jacaranda_planks:8` | Jacaranda Wood Panel | wall-accent, ceiling | partly | modern | panel |
| `chisel:jacaranda_planks:9` | Jacaranda Wood Double Slab | floor, ceiling | partly | modern |  |
| `chisel:jacaranda_planks:10` | Jacaranda Wood Crate | furniture-deco | partly | modern | crate |
| `chisel:jacaranda_planks:11` | Fancy Jacaranda Wood Crate | furniture-deco | partly | modern | crate |
| `chisel:jacaranda_planks:12` | Jacaranda Wood Scaffold | special | partly | modern | scaffold frame painted on a solid block |
| `chisel:jacaranda_planks:13` | Large Long Jacaranda Wood Planks | floor | partly | modern |  |
| `chisel:jacaranda_planks:14` | Jacaranda Wood Planks in Disarray | rubble-ruin | partly | modern, ruin | boards in disarray |
| `chisel:jacaranda_planks:15` | Vertical Jacaranda Wood Planks in Disarray | rubble-ruin | partly | modern, ruin | boards in disarray |
| `chisel:mahogany_planks:1` | Smooth Mahogany Wood Planks | floor, wall-main | partly | standard, luxury | smooth planks |
| `chisel:mahogany_planks:2` | Short Mahogany Wood Planks | floor | partly | standard, luxury |  |
| `chisel:mahogany_planks:3` | Vertical Mahogany Wood Planks | wall-main | partly | standard, luxury | vertical boards |
| `chisel:mahogany_planks:4` | Vertical Uneven Mahogany Wood Planks | wall-main | partly | standard, luxury | vertical boards |
| `chisel:mahogany_planks:5` | Mahogany Wood Parquet | floor | partly | standard, luxury | parquet |
| `chisel:mahogany_planks:6` | Fancy Mahogany Wood Plank Arrangement | floor | partly | standard, luxury |  |
| `chisel:mahogany_planks:7` | Mahogany Wood Plank Blinds | wall-accent | partly | standard, luxury | shutters |
| `chisel:mahogany_planks:8` | Mahogany Wood Panel | wall-accent, ceiling | partly | standard, luxury | panel |
| `chisel:mahogany_planks:9` | Mahogany Wood Double Slab | floor, ceiling | partly | standard, luxury |  |
| `chisel:mahogany_planks:10` | Mahogany Wood Crate | furniture-deco | partly | standard, luxury | crate |
| `chisel:mahogany_planks:11` | Fancy Mahogany Wood Crate | furniture-deco | partly | standard, luxury | crate |
| `chisel:mahogany_planks:12` | Mahogany Wood Scaffold | special | partly | standard, luxury | scaffold frame painted on a solid block |
| `chisel:mahogany_planks:13` | Large Long Mahogany Wood Planks | floor | partly | standard, luxury |  |
| `chisel:mahogany_planks:14` | Mahogany Wood Planks in Disarray | rubble-ruin | partly | standard, luxury, ruin | boards in disarray |
| `chisel:mahogany_planks:15` | Vertical Mahogany Wood Planks in Disarray | rubble-ruin | partly | standard, luxury, ruin | boards in disarray |
| `chisel:greatwood_planks:1` | Smooth Greatwood Wood Planks | floor, wall-main | yes | standard, gothic | smooth planks |
| `chisel:greatwood_planks:2` | Short Greatwood Wood Planks | floor | yes | standard, gothic |  |
| `chisel:greatwood_planks:3` | Vertical Greatwood Wood Planks | wall-main | yes | standard, gothic | vertical boards |
| `chisel:greatwood_planks:4` | Vertical Uneven Greatwood Wood Planks | wall-main | yes | standard, gothic | vertical boards |
| `chisel:greatwood_planks:5` | Greatwood Wood Parquet | floor | yes | standard, gothic | parquet |
| `chisel:greatwood_planks:6` | Fancy Greatwood Wood Plank Arrangement | floor | yes | standard, gothic |  |
| `chisel:greatwood_planks:7` | Greatwood Wood Plank Blinds | wall-accent | yes | standard, gothic | shutters |
| `chisel:greatwood_planks:8` | Greatwood Wood Panel | wall-accent, ceiling | yes | standard, gothic | panel |
| `chisel:greatwood_planks:9` | Greatwood Wood Double Slab | floor, ceiling | yes | standard, gothic |  |
| `chisel:greatwood_planks:10` | Greatwood Wood Crate | furniture-deco | yes | standard, gothic | crate |
| `chisel:greatwood_planks:11` | Fancy Greatwood Wood Crate | furniture-deco | yes | standard, gothic | crate |
| `chisel:greatwood_planks:12` | Greatwood Wood Scaffold | special | yes | standard, gothic | scaffold frame painted on a solid block |
| `chisel:greatwood_planks:13` | Large Long Greatwood Wood Planks | floor | yes | standard, gothic |  |
| `chisel:greatwood_planks:14` | Greatwood Wood Planks in Disarray | rubble-ruin | yes | standard, gothic, ruin | boards in disarray |
| `chisel:greatwood_planks:15` | Vertical Greatwood Wood Planks in Disarray | rubble-ruin | yes | standard, gothic, ruin | boards in disarray |
| `chisel:silverwood_planks:1` | Smooth Silverwood Wood Planks | floor, wall-main | partly | modern, luxury | smooth planks |
| `chisel:silverwood_planks:2` | Short Silverwood Wood Planks | floor | partly | modern, luxury |  |
| `chisel:silverwood_planks:3` | Vertical Silverwood Wood Planks | wall-main | partly | modern, luxury | vertical boards |
| `chisel:silverwood_planks:4` | Vertical Uneven Silverwood Wood Planks | wall-main | partly | modern, luxury | vertical boards |
| `chisel:silverwood_planks:5` | Silverwood Wood Parquet | floor | partly | modern, luxury | parquet |
| `chisel:silverwood_planks:6` | Fancy Silverwood Wood Plank Arrangement | floor | partly | modern, luxury |  |
| `chisel:silverwood_planks:7` | Silverwood Wood Plank Blinds | wall-accent | partly | modern, luxury | shutters |
| `chisel:silverwood_planks:8` | Silverwood Wood Panel | wall-accent, ceiling | partly | modern, luxury | panel |
| `chisel:silverwood_planks:9` | Silverwood Wood Double Slab | floor, ceiling | partly | modern, luxury |  |
| `chisel:silverwood_planks:10` | Silverwood Wood Crate | furniture-deco | partly | modern, luxury | crate |
| `chisel:silverwood_planks:11` | Fancy Silverwood Wood Crate | furniture-deco | partly | modern, luxury | crate |
| `chisel:silverwood_planks:12` | Silverwood Wood Scaffold | special | partly | modern, luxury | scaffold frame painted on a solid block |
| `chisel:silverwood_planks:13` | Large Long Silverwood Wood Planks | floor | partly | modern, luxury |  |
| `chisel:silverwood_planks:14` | Silverwood Wood Planks in Disarray | rubble-ruin | partly | modern, luxury, ruin | boards in disarray |
| `chisel:silverwood_planks:15` | Vertical Silverwood Wood Planks in Disarray | rubble-ruin | partly | modern, luxury, ruin | boards in disarray |

</details>

## Best variety pools

Proposed Chisel additions per upstream palette family ([catalog-palettes.md](../../../original-spec/05-assets/catalog-palettes.md)), in rough order of value. Each list sits beside the D19 baseline block, not in place of it; weights are for palette design to set. Blocks marked † are speed blocks ([Behavior](#behavior-to-know-before-placing-chisel-blocks) 2): D28 rule 3 allows them in walls and trims and on highways and primary roads, never in floors, roofs, sidewalks, stairs or rubble. The metal groups and the iron and gold groups are decorative metals (D29): capped accents behind one flag per metal, listed in the [index](index.md#decorative-metals-d29) rather than here; the gem groups are loot only.

| Upstream family | Baseline (D19) | Chisel blocks that add the most variety |
|---|---|---|
| `bricks_standard` `#` and variant `stonebrick` (main wall) | stone bricks with cracked and mossy | `stonebricksmooth:4, 5, 8` (small, wide, disordered panels), `stonebricksmooth:6, 7` (disordered, also rubble), `andesite:3`, `diorite:3`, `holystone:6, 7` (light 3), `limestone:5`, `brickCustom:15` (gray bricks), `marble:1` |
| `bricks_standard` `X` (accent; infested stone bricks upstream) | stone bricks | `stonebricksmooth:9, 10, 11, 13, 14`, `marble:2, 4, 10, 11`, `limestone:11, 12`, `andesite:4`, `nucrete:1, 9` |
| `$` band (smooth stone double slab in walls) | smooth stone | `stonebricksmooth:12` (flat stone panel), `concrete:0, 2`†, `nucrete:3, 11`, `technical4:7` (worn concrete, random variation), `limestone:7` |
| `S`, `u`, `_` (smooth stone double slab: side streets, floors; bottom slabs) | smooth stone | `stonebricksmooth:12`, `nucrete:4, 12`, `technical4:7`, `limestone:7`, `marble_slab` for real half slabs; no `concrete` or `concrete_slab` (speed) |
| variant `stonebrick_rubble` and `}` rubble | stone bricks, slabs, stairs | `cobblestone:9`, `limestone:6`, `andesite:7, 11`, `brutalism:5, 6, 7`, `brickCustom:5, 13`, `mossy_cobblestone:1` to `7` (overgrown) |
| `bricks_border` and variant `bricks` (red brick and red terracotta) | bricks, red terracotta | `brickCustom:1, 2, 3, 9, 10`, `hardened_clay:3` to `8` (natural terracotta bricks), `hardened_clay_red:3` to `8`, `brickCustom:11, 12` (white and black bricks for contrast) |
| `bricks_cyan`, `bricks_gray`, `bricks_silver` | stained terracotta (a boulder host) | the matching `hardened_clay_cyan`, `_gray`, `_lightgray` brick and panel patterns (metas 2 to 8, `2:5`, `2:6`): same colors, **not boulder hosts**, patterned instead of flat; `nucrete` dark (0 to 7) and light (8 to 15) |
| `bricks_desert`, `_orange`, `_red`, `X` and `x` (cut sandstone) | sandstone, cut sandstone, orange and red terracotta | `sandstone:8, 12, 13, 15`, `sandstone2:0, 1`, sandstone pillars `sandstone:4` to `7`, `9` to `11` as trims, `hardened_clay_orange` and `_red` and `_yellow` patterns, `mud:6` (mud brick), `granite:1, 10`, `sandstone:0` and `sandstone_scribbles` for an ancient quarter |
| desert `B` (red nether bricks) | red nether bricks | `nether_brick:14, 15`, `brickCustom:1` |
| quartz `Q`, `quartzbricks_border`, `@` quartz columns | quartz, smooth quartz, quartz bricks | `marble:0, 5, 12, 15`, `diorite:10`, `alabaster:0`, `laboratoryblock:2, 5` (white enamel), `marble_pillar:0` to `13` for `@` columns and lobby pillars, `marble_stairs` for the `(` `)` quartz stairs |
| `glass_full*`, `glass_pane*` `+` `a` | glass and white, gray, blue, light blue stained glass | `glass:5, 6, 9, 10, 11, 13, 14`, `glass2:1`; stained glass framed and borderless styles in white, gray, light gray, light blue, blue (`stained_glass_white:0` to `3`, `:12` to `15`; `stained_glass_yellow:12` to `15`; `stained_glass_lightgray:0` to `3`, `:12` to `15`) |
| `` ` `` panes | glass pane, stained panes | `glass_pane:1, 2, 4`; framed, borderless and four-pane styles of `stained_glass_pane_white`, `_pink` (gray at 8 to 13), `_lightgray`, `_magenta` (light blue at 8 to 13), `_purple` (blue at 8 to 13) |
| `Z` random glass (city-sphere) | glass, gray, blue, light blue stained glass | add the Chisel framed and bubble styles of the same four colors |
| `:` iron bars and the "damaged to" block | iron bars | `iron_bars:1, 3, 5, 6, 8`; `iron_bars:4, 9` (spiked) for fenced lots |
| `h` glowstone | glowstone | `glowstone:3, 4, 8, 9`, `lantern:0, 1`, `froglight:0` (white-blue panel), `froglight:4` |
| `T` torch | torch | `torch1` to `torch10` (same light as vanilla torches; metadata is the facing) |
| `L` bookshelf | bookshelf | `bookshelf:1, 4, 6, 8`; `bookshelf:5` (abandoned, cobwebbed) for ruins |
| colored concrete (`9` border, highway gas station and restaurant, oil rig) | Et Futurum concrete | `hempcrete:7, 8, 9, 14, 4, 15, 0` (gray, light gray, cyan, red, yellow, black, white), `woolen_clay` same colors, `nucrete`, `cubit` grays |
| `R` highway surface and primary roads | cyan terracotta, smooth stone | `road:0` and `concrete:10` (asphalt)†, `road:1` crosswalks at junctions, `road:2, 3, 6, 7, 14` and `road2:4, 5, 6, 11, 12` lane markings† (direction-named ones match the street axis), `road2:13, 14` (weathering)† and `road2:15` (overgrown)† for ruined roads; `road_line:0` to `3` as painted lines on any street block (no speed of their own) |
| sidewalks and plazas | smooth stone slab | `nucrete:4, 12`, `limestone:13`, `cobblestone:6, 7, 10`, `stonebricksmooth:12`, `technical:13` (grate) |
| interior floors | wood, stone | `laboratoryblock:6, 7, 8` (enamel tile, lab, checkerboard), `porcelain:3, 4, 10, 12` (tiled bathrooms and kitchens), `marble:12, 13, 14`, plank patterns 1, 5, 6, 13 of any fitting wood, `carpet` floor carpets, `hardened_clay` mosaic and tile patterns |
| ceilings | (same as walls) | `laboratoryblock:0, 1, 9, 10`, `marble:4`, plank patterns 8 and 9 |
| ruin and rubble | upstream damage and rubble chars | `technical4:0, 1, 5`, `factoryblock:0, 2, 14`, `sheetmetal:5` to `9`, plank patterns 14 and 15, `brutalism:7` (rebar), `bookshelf:5`, `technical4:13` (cardboard scraps), `brutalism:4` (dead faux window) |
| overgrown | (none upstream beyond moss and vines) | `brutalism:2, 3`, `leaves:6` to `10` (never decay: hedges and planters), `moss_carpet:0, 3, 9`, `moss:0, 3`, `mossy_cobblestone`, `brickCustom:14` (overgrown road `road2:15`† stays on primary roads) |

New styles these blocks make possible (for the building-variety work after beta):

- **Industrial district** (GTNH flavor, [0004](../../../design/0004-gtnh-layer-and-tier-ceiling.md)): `factoryblock`, `sheetmetal`, `technical` and `technical4` (cinder blocks, pipes, vents, fans, grates, boxes), `nucrete` caution tape, `warningSign`, `road_line:4` hazard tape, and `iron2` and the aluminum, steel and stainless steel groups as capped metal accents (caution stripes, crates, bolted panels) behind their D29 flags.
- **Modern office and lab**: `laboratoryblock`, `glass:10` steel-frame glass, `tyrian:5, 7, 15`, `stainlesssteelblock` as capped accents (D29), `brutalism`, `alabaster`, `cubit` grays.
- **Ancient quarter**: `templeblock`, `fantasyblock2`, `marble_pillar`, `sandstone_scribbles`, `holystone`; `mossy_templeblock` when ruined.
- **Asian quarter**: `paperwall` and `paperwall_block`, `glass:2, 3`, `glass_pane:12` to `15`, `porcelain:5, 6, 7, 9`, `cherry_planks`.
- **Arctic**: `ice`, `packedice` and their pillars and stairs, keeping melting and slipperiness in mind.

## The 15 most valuable additions

1. `chisel:road`, `chisel:road2`, `chisel:road_line`: real asphalt highways and primary roads with crosswalks and lane markings, plus weathered and overgrown road for ruins. †Speed: highways and primary roads only (D28 rule 3).
2. `chisel:hardened_clay_<color>` patterns: terracotta brick facades in exactly upstream's colors that are not GT boulder hosts.
3. `chisel:stonebricksmooth:4` to `12`: stone brick variety for `bricks_standard`, and the flat panel for smooth-stone streets and bands.
4. `chisel:concrete` 0 to 10: concrete walls and trims, and asphalt on highways and primary roads, with built-in weathered variants. †Speed: its slabs and stairs, and any use as a floor, sidewalk or rubble, stay out (D28 rule 3).
5. `chisel:laboratoryblock`: clean white offices, labs and hospitals (walls, tiled floors, ceilings).
6. `chisel:marble`, `marble_pillar`, with slabs and stairs: quartz-like facades with real columns and capstones.
7. `chisel:brickCustom`: fifteen brick styles, including white, black, gray, sooty and aged.
8. `chisel:glass` and `glass2`: steel-frame, stone-frame, grid and streaked windows.
9. Chisel stained glass and panes in white, gray, light blue and blue, framed and borderless: office-tower curtain walls.
10. `chisel:brutalism`: cast cement with weathered, rebar and leaf-covered versions, and a faux window for dead storefronts.
11. `chisel:nucrete`: dark and light structural concrete, breeze blocks, concrete tiles.
12. `chisel:technical4:7` to `13` with `factoryblock` and `sheetmetal`: worn concrete, cinder blocks, cardboard boxes, rusty plate for industrial and ruined blocks.
13. `chisel:glowstone:3, 4, 8, 9`, `lantern:0, 1`, `froglight:0`: light variety beyond plain glowstone.
14. `chisel:iron_bars` variants: fences, cages and grids for `:` and the damage block.
15. `chisel:hempcrete`: a second source of flat colored concrete for gas stations, borders and the oil rig.

[features]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java
[helper]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/carving/CarvableHelper.java#L164-L179
[config]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/config/Configurations.java#L305-L321
[texturetype]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/rendering/TextureType.java
[carvable]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/block/BlockCarvable.java#L173-L176
[concrete]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/block/BlockConcrete.java
