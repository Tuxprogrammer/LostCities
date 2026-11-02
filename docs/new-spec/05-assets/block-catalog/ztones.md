# Block catalog: Ztones

Ztones 2.2.2 (`Ztones-1.7.10-2.2.2.jar`), the pack's CurseForge build by riciJak. No public source exists; everything here is read from the Vineflower decompile in `gtnh-src/mods/Ztones/` (`init/ModBlocks.java`, `block/MBlocks/*`, `block/VBlocks/*`), the shipped `config/Ztones.cfg` and the textures in the jar. Part of the decorative block catalog (decision log D25). Contact sheets: `tools/block-contact-sheet/sheets/ztones/*.json`, rendered to `docs/previews/blocks/ztones/`.

## Summary

- **Enabled: 551 block+meta** (34 sixteen-meta sets and 7 single blocks) of **567 the code can register**. The only block the pack's config turns off is the `test` set (`enableTest=false`, 16 metas). The three flat lamps take meta as orientation (1 to 6), so they are counted once each.
- **Registry names.** The 34 sets register under their unlocalized name, which keeps vanilla's `tile.` prefix: `Ztones:tile.agonBlock`, …, `Ztones:tile.glaxx`. The singles use plain names: `Ztones:stoneTile`, `Ztones:cleanDirt`, `Ztones:booster`, `Ztones:lampf`, `Ztones:lampt`, `Ztones:lampb`, `Ztones:auroraBlock`. The mod ID is `Ztones` (capital Z). `ztylBlock`, the singles and the lamps have no config flag.
- **Every set is a plain full cube** (`Material.iron`, hardness 1.2 or 1.3, one texture on all six faces, meta = variant). No tile entities, no gravity, no light (Zeta, Korp 12-13 and others only *look* lit), no connected textures, no stairs, slabs, walls or panes for any Ztones block. None is a GT ore or boulder host.
- **Glaxx is the exception**: translucent tinted glass, render pass 1, full block only.
- **The lamps** are thin wall/ceiling panels with light 15 and no collision; their meta is a facing and they drop when their support goes.
- **Look:** about a third of Ztones is plain color (Agon, Iszm, Myst, Cray, Fort, Zest, Zane), a third stone and metal panels that fit a 20th-century city (Lave, Mint, Kryp, Roen, Sols, Lair, Ztyl, Tank, Korp, Vena), and a third sci-fi or neon patterns (Azur, Bitt, Zech, Zone, Zeta, Zion, Zome, Zyth, Zkul, Zoea, Reds, Sync, Jelt, Vect) that are off-theme for upstream but would carry a new tech or neon district.
- **Fit with upstream's look: 118 yes, 195 partly, 238 no.**

Blocks per role (a block can hold several roles):

| Role | Blocks+meta |
|---|---|
| `wall-main` | 128 |
| `wall-accent` | 262 |
| `trim-pillar` | 41 |
| `floor` | 50 |
| `ceiling` | 14 |
| `roof` | 8 |
| `glass` | 16 |
| `street` | 5 |
| `sidewalk` | 6 |
| `rubble-ruin` | 13 |
| `light` | 3 |
| `fence-rail` | 1 |
| `door-trap` | 1 |
| `furniture-deco` | 1 |
| `foliage` | 2 |
| `special` | 5 |
| `off-theme` | 232 |

### Gaps Ztones fills

- **Colored concrete and painted render without a boulder host.** Upstream's terracotta palettes (`bricks_cyan`, `bricks_gray`, `bricks_silver`, desert orange and red) map to `minecraft:stained_hardened_clay`, a GT boulder host ([ore-veins.md](../../00-pack-integration/ore-veins.md)). Agon, Iszm, Cray and Myst give matte colors in the same range with no host risk, as variety beside terracotta and Et Futurum's concrete.
- **Dark tinted glass.** Vanilla's stained glass has no smoke, bronze or dark-teal office tints; Glaxx does. It has no pane, so it suits glass-block (`+`) windows only.
- **Flush ceiling lights.** Et Futurum's sea lantern is off in the pack (`enablePrismarine=false`). Ztones' flat lamps are light-15 panels that sit flush on a ceiling or wall.
- **A copper-roof look that does not weather**: Vena 14-15.
- **Paving**: Ztone Tile, Ztyl 11, Kryp 3, Roen 13, Tank 10 and 15.

### New city styles Ztones suggests

- **Tech district** (Vect, Korp, Azur, Bitt, Zech, Zone, Ztyl 1-5): black panels with gray or colored frames; the look of a GTNH-era research campus.
- **Neon/arcade strip** (Reds, Sync, Jelt, Zeta, Zion, Zyth): saturated light-colored patterns on black. The blocks emit no light, so a lamp block would have to carry the glow.
- **Industrial** (Tank, Korp 3-9): steel plating, vents and tread plate for factories and the oil rig.
- **Old town** (Kryp, Lair, Fort): carved stone, dark red brick and streaked render.

## Blocks

### Agon: matte solid colors

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.agonBlock:0` | Agon 0 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | matte white-gray; flat paint-like color with a faint edge shade; reads as painted concrete |
| `Ztones:tile.agonBlock:1` | Agon 1 | `wall-accent`, `wall-main` | partly | standard | matte olive yellow |
| `Ztones:tile.agonBlock:2` | Agon 2 | `wall-accent`, `wall-main` | partly | standard | matte yellow-green |
| `Ztones:tile.agonBlock:3` | Agon 3 | `wall-accent`, `wall-main` | partly | standard | matte green |
| `Ztones:tile.agonBlock:4` | Agon 4 | `wall-accent`, `wall-main` | partly | standard | matte teal |
| `Ztones:tile.agonBlock:5` | Agon 5 | `wall-accent`, `wall-main` | partly | standard | matte cyan |
| `Ztones:tile.agonBlock:6` | Agon 6 | `wall-accent`, `wall-main` | partly | standard | matte blue |
| `Ztones:tile.agonBlock:7` | Agon 7 | `wall-accent`, `wall-main` | partly | standard | matte violet |
| `Ztones:tile.agonBlock:8` | Agon 8 | `wall-accent`, `wall-main` | partly | standard | matte magenta |
| `Ztones:tile.agonBlock:9` | Agon 9 | `wall-accent`, `wall-main` | partly | standard | matte rose |
| `Ztones:tile.agonBlock:10` | Agon 10 | `wall-accent`, `wall-main` | partly | standard | matte red |
| `Ztones:tile.agonBlock:11` | Agon 11 | `wall-accent`, `wall-main` | partly | standard | matte orange |
| `Ztones:tile.agonBlock:12` | Agon 12 | `wall-main`, `wall-accent`, `floor` | yes | standard, desert | matte tan |
| `Ztones:tile.agonBlock:13` | Agon 13 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | matte near-black |
| `Ztones:tile.agonBlock:14` | Agon 14 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | matte gray |
| `Ztones:tile.agonBlock:15` | Agon 15 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | matte light gray |

### Iszm: smooth saturated colors

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.iszmBlock:0` | Iszm 0 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | smooth gray; slightly mottled plastic-like finish |
| `Ztones:tile.iszmBlock:1` | Iszm 1 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | smooth light gray |
| `Ztones:tile.iszmBlock:2` | Iszm 2 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | smooth dark gray |
| `Ztones:tile.iszmBlock:3` | Iszm 3 | `wall-main`, `wall-accent` | yes | standard | smooth slate blue |
| `Ztones:tile.iszmBlock:4` | Iszm 4 | `wall-accent`, `wall-main` | partly | standard | smooth lime |
| `Ztones:tile.iszmBlock:5` | Iszm 5 | `wall-accent`, `wall-main` | partly | standard | smooth blue |
| `Ztones:tile.iszmBlock:6` | Iszm 6 | `wall-accent`, `wall-main` | partly | standard | smooth red |
| `Ztones:tile.iszmBlock:7` | Iszm 7 | `wall-accent`, `wall-main` | partly | standard | smooth orange |
| `Ztones:tile.iszmBlock:8` | Iszm 8 | `wall-accent`, `wall-main`, `special` | partly | standard | smooth aqua; the oil rig's diamond-block look-alike (D30, [oil rig](index.md#oil-rig)) |
| `Ztones:tile.iszmBlock:9` | Iszm 9 | `wall-accent`, `wall-main` | partly | standard | smooth yellow |
| `Ztones:tile.iszmBlock:10` | Iszm 10 | `wall-main`, `wall-accent` | partly | standard, border | smooth maroon |
| `Ztones:tile.iszmBlock:11` | Iszm 11 | `wall-accent`, `wall-main` | partly | standard | smooth dark teal |
| `Ztones:tile.iszmBlock:12` | Iszm 12 | `wall-accent`, `wall-main` | partly | standard | smooth green |
| `Ztones:tile.iszmBlock:13` | Iszm 13 | `wall-main`, `wall-accent` | partly | standard, desert | smooth ochre |
| `Ztones:tile.iszmBlock:14` | Iszm 14 | `wall-accent`, `wall-main` | partly | standard | smooth sky blue |
| `Ztones:tile.iszmBlock:15` | Iszm 15 | `wall-accent`, `wall-main` | partly | standard | smooth purple |

### Myst: soft saturated colors

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.mystBlock:0` | Myst 0 | `wall-accent`, `wall-main` | partly | standard | soft yellow; cloudy matte color |
| `Ztones:tile.mystBlock:1` | Myst 1 | `wall-accent`, `wall-main` | partly | standard | soft pink-magenta |
| `Ztones:tile.mystBlock:2` | Myst 2 | `wall-accent`, `wall-main` | partly | standard | soft cyan |
| `Ztones:tile.mystBlock:3` | Myst 3 | `wall-main`, `wall-accent` | yes | standard, desert | soft taupe |
| `Ztones:tile.mystBlock:4` | Myst 4 | `wall-accent`, `wall-main` | partly | standard | soft lime |
| `Ztones:tile.mystBlock:5` | Myst 5 | `wall-main`, `wall-accent` | partly | standard, desert | soft rust |
| `Ztones:tile.mystBlock:6` | Myst 6 | `wall-accent`, `wall-main` | partly | standard | soft violet |
| `Ztones:tile.mystBlock:7` | Myst 7 | `wall-accent`, `wall-main` | partly | standard | soft orange |
| `Ztones:tile.mystBlock:8` | Myst 8 | `wall-accent`, `wall-main` | partly | standard | soft teal |
| `Ztones:tile.mystBlock:9` | Myst 9 | `wall-accent`, `wall-main` | partly | standard | soft blue |
| `Ztones:tile.mystBlock:10` | Myst 10 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | soft gray |
| `Ztones:tile.mystBlock:11` | Myst 11 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | soft dark gray |
| `Ztones:tile.mystBlock:12` | Myst 12 | `wall-accent`, `wall-main` | partly | standard | soft green |
| `Ztones:tile.mystBlock:13` | Myst 13 | `wall-accent`, `wall-main` | partly | standard | soft magenta |
| `Ztones:tile.mystBlock:14` | Myst 14 | `wall-accent`, `wall-main` | partly | standard | soft crimson |
| `Ztones:tile.mystBlock:15` | Myst 15 | `wall-main`, `wall-accent`, `floor` | yes | standard, border | soft light gray |

### Cray: pastel stucco with a thin border

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.crayBlock:0` | Cray 0 | `wall-main`, `wall-accent`, `ceiling` | yes | standard, border | pastel pale gray; muted painted-render look; the thin darker border repeats every block, so large walls read as panels |
| `Ztones:tile.crayBlock:1` | Cray 1 | `wall-main`, `wall-accent` | yes | standard | pastel pale olive |
| `Ztones:tile.crayBlock:2` | Cray 2 | `wall-main`, `wall-accent` | yes | standard | pastel pale olive-green |
| `Ztones:tile.crayBlock:3` | Cray 3 | `wall-main`, `wall-accent` | yes | standard | pastel sage |
| `Ztones:tile.crayBlock:4` | Cray 4 | `wall-main`, `wall-accent` | yes | standard | pastel sea green |
| `Ztones:tile.crayBlock:5` | Cray 5 | `wall-main`, `wall-accent` | yes | standard | pastel pale teal |
| `Ztones:tile.crayBlock:6` | Cray 6 | `wall-main`, `wall-accent` | yes | standard | pastel powder blue |
| `Ztones:tile.crayBlock:7` | Cray 7 | `wall-main`, `wall-accent` | yes | standard | pastel slate blue |
| `Ztones:tile.crayBlock:8` | Cray 8 | `wall-main`, `wall-accent` | yes | standard | pastel periwinkle |
| `Ztones:tile.crayBlock:9` | Cray 9 | `wall-main`, `wall-accent` | yes | standard | pastel lavender |
| `Ztones:tile.crayBlock:10` | Cray 10 | `wall-main`, `wall-accent` | yes | standard | pastel mauve |
| `Ztones:tile.crayBlock:11` | Cray 11 | `wall-main`, `wall-accent` | yes | standard | pastel dusty pink |
| `Ztones:tile.crayBlock:12` | Cray 12 | `wall-main`, `wall-accent` | yes | standard | pastel dusty red |
| `Ztones:tile.crayBlock:13` | Cray 13 | `wall-main`, `wall-accent` | yes | standard, desert | pastel salmon terracotta |
| `Ztones:tile.crayBlock:14` | Cray 14 | `wall-main`, `wall-accent` | yes | standard, desert | pastel sand |
| `Ztones:tile.crayBlock:15` | Cray 15 | `wall-main`, `wall-accent` | yes | standard, border | pastel gray |

### Zane: blotchy colored stone

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zaneBlock:0` | Zane 0 | `wall-main`, `wall-accent` | partly | standard, border | blotchy white; dark blotches over a saturated base, like dyed rough plaster; busy at scale |
| `Ztones:tile.zaneBlock:1` | Zane 1 | `wall-accent` | partly | standard | blotchy olive |
| `Ztones:tile.zaneBlock:2` | Zane 2 | `wall-accent` | partly | standard | blotchy yellow-green |
| `Ztones:tile.zaneBlock:3` | Zane 3 | `wall-accent` | partly | standard | blotchy green |
| `Ztones:tile.zaneBlock:4` | Zane 4 | `wall-accent` | partly | standard | blotchy emerald |
| `Ztones:tile.zaneBlock:5` | Zane 5 | `wall-accent` | partly | standard | blotchy teal |
| `Ztones:tile.zaneBlock:6` | Zane 6 | `wall-accent` | partly | standard | blotchy sky blue |
| `Ztones:tile.zaneBlock:7` | Zane 7 | `wall-accent` | partly | standard | blotchy blue |
| `Ztones:tile.zaneBlock:8` | Zane 8 | `wall-accent` | partly | standard | blotchy indigo |
| `Ztones:tile.zaneBlock:9` | Zane 9 | `wall-accent` | partly | standard | blotchy purple |
| `Ztones:tile.zaneBlock:10` | Zane 10 | `wall-accent` | partly | standard | blotchy magenta |
| `Ztones:tile.zaneBlock:11` | Zane 11 | `wall-accent` | partly | standard | blotchy crimson |
| `Ztones:tile.zaneBlock:12` | Zane 12 | `wall-accent` | partly | standard | blotchy red |
| `Ztones:tile.zaneBlock:13` | Zane 13 | `wall-accent` | partly | standard, desert | blotchy orange |
| `Ztones:tile.zaneBlock:14` | Zane 14 | `wall-accent` | partly | standard, desert | blotchy ochre |
| `Ztones:tile.zaneBlock:15` | Zane 15 | `wall-accent`, `rubble-ruin` | partly | standard | blotchy black |

### Fort: streaked painted render

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.fortBlock:0` | Fort 0 | `wall-main`, `wall-accent`, `rubble-ruin` | partly | standard, new: old town | streaked umber; faint vertical streaks, like weathered painted render or stained wood; good for grimy older blocks |
| `Ztones:tile.fortBlock:1` | Fort 1 | `wall-main`, `wall-accent` | partly | standard | streaked dark olive |
| `Ztones:tile.fortBlock:2` | Fort 2 | `wall-main`, `wall-accent` | partly | standard | streaked olive |
| `Ztones:tile.fortBlock:3` | Fort 3 | `wall-main`, `wall-accent` | partly | standard | streaked moss green |
| `Ztones:tile.fortBlock:4` | Fort 4 | `wall-main`, `wall-accent` | partly | standard | streaked khaki yellow |
| `Ztones:tile.fortBlock:5` | Fort 5 | `wall-main`, `wall-accent` | partly | standard | streaked dark khaki |
| `Ztones:tile.fortBlock:6` | Fort 6 | `wall-accent`, `trim-pillar` | partly | standard | streaked charcoal |
| `Ztones:tile.fortBlock:7` | Fort 7 | `wall-main`, `wall-accent` | partly | standard | streaked slate violet |
| `Ztones:tile.fortBlock:8` | Fort 8 | `wall-main`, `wall-accent` | partly | standard | streaked slate |
| `Ztones:tile.fortBlock:9` | Fort 9 | `wall-main`, `wall-accent` | yes | standard, border | streaked gray |
| `Ztones:tile.fortBlock:10` | Fort 10 | `wall-main`, `wall-accent` | yes | standard, border | streaked off-white |
| `Ztones:tile.fortBlock:11` | Fort 11 | `wall-main`, `wall-accent` | yes | standard, desert | streaked sand |
| `Ztones:tile.fortBlock:12` | Fort 12 | `wall-main`, `wall-accent` | partly | standard, desert | streaked salmon |
| `Ztones:tile.fortBlock:13` | Fort 13 | `wall-main`, `wall-accent` | yes | standard, desert | streaked brick red |
| `Ztones:tile.fortBlock:14` | Fort 14 | `wall-main`, `wall-accent` | partly | standard | streaked crimson |
| `Ztones:tile.fortBlock:15` | Fort 15 | `wall-main`, `wall-accent` | partly | standard | streaked oxblood |

### Zest: large colored masonry blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zestBlock:0` | Zest 0 | `wall-main`, `wall-accent`, `trim-pillar` | yes | standard, border | white large blocks; four offset glazed blocks per face (half-size ashlar); a painted-brick look |
| `Ztones:tile.zestBlock:1` | Zest 1 | `wall-accent` | partly | standard | yellow large blocks |
| `Ztones:tile.zestBlock:2` | Zest 2 | `wall-accent` | partly | standard | yellow-green large blocks |
| `Ztones:tile.zestBlock:3` | Zest 3 | `wall-accent` | partly | standard | lime large blocks |
| `Ztones:tile.zestBlock:4` | Zest 4 | `wall-accent` | partly | standard | green large blocks |
| `Ztones:tile.zestBlock:5` | Zest 5 | `wall-accent` | partly | standard | teal large blocks |
| `Ztones:tile.zestBlock:6` | Zest 6 | `wall-accent` | partly | standard | sky blue large blocks |
| `Ztones:tile.zestBlock:7` | Zest 7 | `wall-accent` | partly | standard | blue large blocks |
| `Ztones:tile.zestBlock:8` | Zest 8 | `wall-accent` | partly | standard | purple large blocks |
| `Ztones:tile.zestBlock:9` | Zest 9 | `wall-accent` | partly | standard | magenta large blocks |
| `Ztones:tile.zestBlock:10` | Zest 10 | `wall-accent` | partly | standard | crimson large blocks |
| `Ztones:tile.zestBlock:11` | Zest 11 | `wall-main`, `wall-accent` | yes | standard | red large blocks |
| `Ztones:tile.zestBlock:12` | Zest 12 | `wall-main`, `wall-accent` | yes | standard, desert | vermilion large blocks |
| `Ztones:tile.zestBlock:13` | Zest 13 | `wall-main`, `wall-accent` | yes | standard, desert | tan large blocks |
| `Ztones:tile.zestBlock:14` | Zest 14 | `wall-accent` | partly | standard, desert | gold large blocks |
| `Ztones:tile.zestBlock:15` | Zest 15 | `wall-main`, `wall-accent`, `trim-pillar` | yes | standard, border | black large blocks |

### Lave: pale office panels

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.laveBlock:0` | Lave 0 | `wall-main`, `ceiling`, `floor` | yes | standard, border | smooth pale gray; the closest Ztones has to modern office cladding |
| `Ztones:tile.laveBlock:1` | Lave 1 | `wall-main`, `wall-accent` | yes | standard, border | pale panel, beveled frame |
| `Ztones:tile.laveBlock:2` | Lave 2 | `wall-main`, `wall-accent` | yes | standard, border | pale panel, inset frame |
| `Ztones:tile.laveBlock:3` | Lave 3 | `wall-accent`, `trim-pillar` | yes | standard, border | pale panel, side bars |
| `Ztones:tile.laveBlock:4` | Lave 4 | `wall-main`, `floor` | yes | standard, border | pale fine-textured |
| `Ztones:tile.laveBlock:5` | Lave 5 | `wall-accent`, `trim-pillar` | yes | standard, border | pale vertical ribs |
| `Ztones:tile.laveBlock:6` | Lave 6 | `wall-accent` | yes | standard, border | pale horizontal ribs |
| `Ztones:tile.laveBlock:7` | Lave 7 | `wall-accent`, `trim-pillar` | yes | standard | pale framed, corner bolts |
| `Ztones:tile.laveBlock:8` | Lave 8 | `floor`, `ceiling`, `wall-main` | yes | standard, border | pale four tiles |
| `Ztones:tile.laveBlock:9` | Lave 9 | `wall-main`, `ceiling` | yes | standard, border | pale bevel panel |
| `Ztones:tile.laveBlock:10` | Lave 10 | `wall-main`, `ceiling` | yes | standard, border | pale bevel panel, deeper |
| `Ztones:tile.laveBlock:11` | Lave 11 | `wall-accent`, `ceiling` | yes | standard | white framed panel, bright |
| `Ztones:tile.laveBlock:12` | Lave 12 | `wall-main`, `ceiling`, `floor` | yes | standard, border | smooth pale gray, flat |
| `Ztones:tile.laveBlock:13` | Lave 13 | `wall-accent`, `roof` | yes | standard | steel blue smooth |
| `Ztones:tile.laveBlock:14` | Lave 14 | `wall-accent`, `roof` | partly | standard | blue mottled |
| `Ztones:tile.laveBlock:15` | Lave 15 | `wall-accent`, `roof` | partly | standard | dark blue mottled |

### Mint: pale stone and framed slabs

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.mintBlock:0` | Mint 0 | `wall-accent` | partly | standard | pale green smooth |
| `Ztones:tile.mintBlock:1` | Mint 1 | `wall-accent`, `floor` | partly | standard | pale green tile pattern |
| `Ztones:tile.mintBlock:2` | Mint 2 | `wall-main`, `floor`, `ceiling` | yes | standard, border | white smooth stone |
| `Ztones:tile.mintBlock:3` | Mint 3 | `floor`, `ceiling`, `wall-accent` | yes | standard, border | white small tiles |
| `Ztones:tile.mintBlock:4` | Mint 4 | `wall-main`, `wall-accent` | yes | standard, desert | khaki smooth |
| `Ztones:tile.mintBlock:5` | Mint 5 | `floor`, `wall-accent` | yes | standard, desert | khaki checkered tiles |
| `Ztones:tile.mintBlock:6` | Mint 6 | `wall-accent`, `trim-pillar` | partly | standard | gray-green framed slab |
| `Ztones:tile.mintBlock:7` | Mint 7 | `wall-accent`, `trim-pillar`, `floor` | yes | standard, border | gray framed slab, pale trim |
| `Ztones:tile.mintBlock:8` | Mint 8 | `wall-accent`, `trim-pillar` | yes | standard, border | gray framed slab, ornate trim |
| `Ztones:tile.mintBlock:9` | Mint 9 | `wall-accent` | partly | standard | gray ornate medallion |
| `Ztones:tile.mintBlock:10` | Mint 10 | `wall-accent`, `floor` | partly | standard | gray concentric squares |
| `Ztones:tile.mintBlock:11` | Mint 11 | `wall-accent`, `trim-pillar` | partly | standard | gray framed slab, green trim |
| `Ztones:tile.mintBlock:12` | Mint 12 | `wall-accent`, `trim-pillar` | partly | standard | gray framed slab, green trim (variant) |
| `Ztones:tile.mintBlock:13` | Mint 13 | `wall-accent`, `trim-pillar` | partly | standard | gray framed slab, green trim (variant) |
| `Ztones:tile.mintBlock:14` | Mint 14 | `wall-main`, `floor`, `street` | yes | standard, border | gray smooth stone |
| `Ztones:tile.mintBlock:15` | Mint 15 | `wall-main`, `floor`, `street` | yes | standard, border | gray smooth stone, darker |

### Kryp: taupe and carved stone

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.krypBlock:0` | Kryp 0 | `wall-main`, `rubble-ruin` | partly | standard, new: old town | rough taupe stone; an old-masonry family; carved metas suit temples and civic ruins more than offices |
| `Ztones:tile.krypBlock:1` | Kryp 1 | `rubble-ruin`, `wall-accent` | partly | standard, new: old town | taupe cobble |
| `Ztones:tile.krypBlock:2` | Kryp 2 | `floor`, `wall-accent` | partly | standard, new: old town | taupe stone planks |
| `Ztones:tile.krypBlock:3` | Kryp 3 | `floor`, `sidewalk` | yes | standard, desert | taupe small tiles |
| `Ztones:tile.krypBlock:4` | Kryp 4 | `wall-accent`, `floor` | partly | standard | polished brown marble, sheen |
| `Ztones:tile.krypBlock:5` | Kryp 5 | `wall-main`, `wall-accent` | yes | standard, desert | smooth tan framed |
| `Ztones:tile.krypBlock:6` | Kryp 6 | `wall-accent`, `trim-pillar` | partly | new: old town, desert | taupe ornate carving |
| `Ztones:tile.krypBlock:7` | Kryp 7 | `wall-accent`, `trim-pillar` | partly | new: old town | dark striped wood-stone |
| `Ztones:tile.krypBlock:8` | Kryp 8 | `trim-pillar` | partly | new: old town | dark striped wood-stone, vertical |
| `Ztones:tile.krypBlock:9` | Kryp 9 | `wall-accent` | partly | new: old town, desert | taupe carved panel |
| `Ztones:tile.krypBlock:10` | Kryp 10 | `wall-accent` | partly | new: old town, desert | taupe carved panel (variant) |
| `Ztones:tile.krypBlock:11` | Kryp 11 | `wall-accent` | partly | new: old town, desert | taupe carved panel (variant) |
| `Ztones:tile.krypBlock:12` | Kryp 12 | `wall-accent` | partly | new: old town, desert | taupe carved panel (variant) |
| `Ztones:tile.krypBlock:13` | Kryp 13 | `wall-accent` | partly | new: old town, desert | taupe carved glyph panel |
| `Ztones:tile.krypBlock:14` | Kryp 14 | `wall-accent` | no | new: old town | dark framed ornament |
| `Ztones:tile.krypBlock:15` | Kryp 15 | `wall-accent`, `floor` | partly | standard | dark smooth stone |

### Roen: pale yellow-green limestone

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.roenBlock:0` | Roen 0 | `wall-main` | yes | desert, standard | smooth pale chalk; reads as limestone or end-stone brick; a strong desert wall family |
| `Ztones:tile.roenBlock:1` | Roen 1 | `wall-main`, `wall-accent` | yes | desert | chalk bevel panel |
| `Ztones:tile.roenBlock:2` | Roen 2 | `wall-main` | yes | desert | chalk smooth, faint |
| `Ztones:tile.roenBlock:3` | Roen 3 | `rubble-ruin`, `wall-main` | yes | desert | chalk cracked |
| `Ztones:tile.roenBlock:4` | Roen 4 | `wall-accent` | no | desert | chalk ornate framed, gold studs |
| `Ztones:tile.roenBlock:5` | Roen 5 | `wall-accent`, `trim-pillar` | partly | desert | chalk horizontal bands |
| `Ztones:tile.roenBlock:6` | Roen 6 | `trim-pillar` | partly | desert | chalk vertical bands |
| `Ztones:tile.roenBlock:7` | Roen 7 | `wall-main`, `wall-accent` | yes | desert | chalk bricks, dark mortar |
| `Ztones:tile.roenBlock:8` | Roen 8 | `wall-accent` | partly | desert | chalk ornate |
| `Ztones:tile.roenBlock:9` | Roen 9 | `wall-accent` | partly | desert | chalk framed, dark border |
| `Ztones:tile.roenBlock:10` | Roen 10 | `wall-accent` | partly | desert | chalk woven pattern |
| `Ztones:tile.roenBlock:11` | Roen 11 | `wall-accent` | partly | desert | chalk woven pattern (variant) |
| `Ztones:tile.roenBlock:12` | Roen 12 | `wall-main`, `rubble-ruin` | partly | desert | chalk speckled |
| `Ztones:tile.roenBlock:13` | Roen 13 | `floor`, `sidewalk` | yes | desert | chalk small tiles |
| `Ztones:tile.roenBlock:14` | Roen 14 | `wall-main`, `rubble-ruin` | yes | desert | chalk mottled |
| `Ztones:tile.roenBlock:15` | Roen 15 | `wall-main` | yes | desert, standard | smooth pale chalk (variant) |

### Sols: sun yellow, cream and rust

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.solsBlock:0` | Sols 0 | `wall-accent` | partly | desert | gold-yellow smooth |
| `Ztones:tile.solsBlock:1` | Sols 1 | `wall-accent` | partly | desert | orange smooth |
| `Ztones:tile.solsBlock:2` | Sols 2 | `wall-accent` | partly | desert | orange marbled |
| `Ztones:tile.solsBlock:3` | Sols 3 | `wall-main`, `ceiling` | yes | desert, standard | pale cream smooth; cream and rust metas pair with the terracotta of the desert brick palettes |
| `Ztones:tile.solsBlock:4` | Sols 4 | `wall-main`, `wall-accent` | yes | desert, standard | cream framed |
| `Ztones:tile.solsBlock:5` | Sols 5 | `wall-accent`, `trim-pillar` | partly | desert | cream carved |
| `Ztones:tile.solsBlock:6` | Sols 6 | `off-theme` | no | new: neon/arcade | black-gold maze |
| `Ztones:tile.solsBlock:7` | Sols 7 | `off-theme` | no | new: neon/arcade | black-gold pattern |
| `Ztones:tile.solsBlock:8` | Sols 8 | `off-theme` | no | new: neon/arcade | black-gold pattern (variant) |
| `Ztones:tile.solsBlock:9` | Sols 9 | `wall-accent` | no | desert | gold dimpled |
| `Ztones:tile.solsBlock:10` | Sols 10 | `wall-accent` | no | desert | gold carved |
| `Ztones:tile.solsBlock:11` | Sols 11 | `floor`, `wall-accent` | partly | desert | yellow tiles |
| `Ztones:tile.solsBlock:12` | Sols 12 | `wall-main`, `wall-accent` | yes | desert | rust red-orange smooth |
| `Ztones:tile.solsBlock:13` | Sols 13 | `wall-main`, `wall-accent` | yes | desert | rust red-orange smooth, deeper |
| `Ztones:tile.solsBlock:14` | Sols 14 | `wall-main`, `rubble-ruin` | yes | desert | rust textured |
| `Ztones:tile.solsBlock:15` | Sols 15 | `wall-accent`, `trim-pillar` | yes | desert | rust framed |

### Lair: dark reds and brick

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.lairBlock:0` | Lair 0 | `wall-accent`, `rubble-ruin` | partly | standard, new: old town | dark red rough |
| `Ztones:tile.lairBlock:1` | Lair 1 | `wall-accent` | partly | new: old town | dark red woven |
| `Ztones:tile.lairBlock:2` | Lair 2 | `wall-accent` | partly | standard | red smooth |
| `Ztones:tile.lairBlock:3` | Lair 3 | `wall-accent` | partly | standard | red diagonal brick |
| `Ztones:tile.lairBlock:4` | Lair 4 | `wall-main`, `wall-accent` | yes | standard, new: old town | red bricks |
| `Ztones:tile.lairBlock:5` | Lair 5 | `wall-main`, `wall-accent` | yes | standard, desert | dusty rose smooth |
| `Ztones:tile.lairBlock:6` | Lair 6 | `floor` | partly | standard | red and dark checker |
| `Ztones:tile.lairBlock:7` | Lair 7 | `wall-accent` | partly | standard | dark framed, red border |
| `Ztones:tile.lairBlock:8` | Lair 8 | `floor`, `rubble-ruin` | partly | standard | dark speckled |
| `Ztones:tile.lairBlock:9` | Lair 9 | `floor`, `sidewalk` | partly | new: old town | mixed-color flagstones |
| `Ztones:tile.lairBlock:10` | Lair 10 | `wall-main`, `wall-accent` | yes | standard | maroon smooth |
| `Ztones:tile.lairBlock:11` | Lair 11 | `wall-main`, `wall-accent` | yes | standard, new: old town | dark red large bricks |
| `Ztones:tile.lairBlock:12` | Lair 12 | `wall-accent`, `furniture-deco` | partly | new: old town | dark wood-like framed panel |
| `Ztones:tile.lairBlock:13` | Lair 13 | `wall-accent` | partly | new: old town | dark wood-like framed panel, gold edge |
| `Ztones:tile.lairBlock:14` | Lair 14 | `wall-accent`, `door-trap` | partly | new: old town | dark wood-like panel, studded; door-trap here means a solid stand-in for a heavy door panel, not a working door |
| `Ztones:tile.lairBlock:15` | Lair 15 | `wall-main` | yes | standard, desert | khaki smooth |

### Ztyl: grays and blacks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.ztylBlock:0` | Ztyl 0 | `wall-main`, `wall-accent` | yes | standard, border | gray framed smooth; registered unconditionally (no config flag) |
| `Ztones:tile.ztylBlock:1` | Ztyl 1 | `wall-accent`, `special` | partly | standard, new: tech district | black grid of gray squares |
| `Ztones:tile.ztylBlock:2` | Ztyl 2 | `wall-accent`, `floor` | yes | standard | black smooth, soft sheen |
| `Ztones:tile.ztylBlock:3` | Ztyl 3 | `wall-accent` | partly | new: tech district | black framed hollow |
| `Ztones:tile.ztylBlock:4` | Ztyl 4 | `wall-accent` | partly | new: tech district | black framed, corner studs |
| `Ztones:tile.ztylBlock:5` | Ztyl 5 | `wall-accent`, `trim-pillar` | yes | standard, new: industrial | gray framed, studs |
| `Ztones:tile.ztylBlock:6` | Ztyl 6 | `off-theme` | no | new: tech district | black ornate lattice |
| `Ztones:tile.ztylBlock:7` | Ztyl 7 | `wall-accent` | partly | standard | black textured |
| `Ztones:tile.ztylBlock:8` | Ztyl 8 | `wall-main`, `floor`, `ceiling` | yes | standard, border | light gray smooth |
| `Ztones:tile.ztylBlock:9` | Ztyl 9 | `wall-main`, `floor`, `street` | yes | standard, border | mid gray smooth |
| `Ztones:tile.ztylBlock:10` | Ztyl 10 | `off-theme` | no | - | gray skull-like ornament |
| `Ztones:tile.ztylBlock:11` | Ztyl 11 | `floor`, `sidewalk`, `wall-main` | yes | standard, border | gray four large tiles |
| `Ztones:tile.ztylBlock:12` | Ztyl 12 | `wall-main`, `floor` | yes | standard, border | gray mottled |
| `Ztones:tile.ztylBlock:13` | Ztyl 13 | `off-theme` | no | new: tech district | black with white dots |
| `Ztones:tile.ztylBlock:14` | Ztyl 14 | `off-theme` | no | new: tech district | white X brace, glossy |
| `Ztones:tile.ztylBlock:15` | Ztyl 15 | `off-theme` | no | new: tech district | dark ornate |

### Tank: steel plating

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.tankBlock:0` | Tank 0 | `wall-main`, `wall-accent` | yes | standard, new: industrial | gray steel panel |
| `Ztones:tile.tankBlock:1` | Tank 1 | `wall-accent` | partly | new: industrial | gray panel with emblem |
| `Ztones:tile.tankBlock:2` | Tank 2 | `wall-accent` | partly | new: industrial | gray panel with holes |
| `Ztones:tile.tankBlock:3` | Tank 3 | `wall-main`, `wall-accent` | yes | standard, new: industrial | gray framed plate |
| `Ztones:tile.tankBlock:4` | Tank 4 | `wall-accent`, `trim-pillar` | yes | new: industrial | gray framed plate, bolts |
| `Ztones:tile.tankBlock:5` | Tank 5 | `wall-accent` | yes | new: industrial | gray plating, horizontal seams |
| `Ztones:tile.tankBlock:6` | Tank 6 | `wall-accent`, `trim-pillar` | yes | new: industrial | gray plating, vertical seams |
| `Ztones:tile.tankBlock:7` | Tank 7 | `floor`, `fence-rail` | partly | new: industrial | dark grate; solid opaque block despite the grate texture |
| `Ztones:tile.tankBlock:8` | Tank 8 | `wall-accent` | partly | new: industrial | gray riveted |
| `Ztones:tile.tankBlock:9` | Tank 9 | `wall-accent` | partly | new: industrial | gray riveted (variant) |
| `Ztones:tile.tankBlock:10` | Tank 10 | `floor`, `street` | yes | new: industrial | dark diamond tread |
| `Ztones:tile.tankBlock:11` | Tank 11 | `floor`, `wall-accent` | partly | new: industrial | dark woven mesh |
| `Ztones:tile.tankBlock:12` | Tank 12 | `floor`, `rubble-ruin` | partly | new: industrial | dark mottled mesh |
| `Ztones:tile.tankBlock:13` | Tank 13 | `wall-accent`, `roof` | yes | standard, new: industrial | dark smooth plate |
| `Ztones:tile.tankBlock:14` | Tank 14 | `wall-main`, `wall-accent` | yes | standard, new: industrial | dark small bricks |
| `Ztones:tile.tankBlock:15` | Tank 15 | `floor`, `sidewalk` | yes | standard, new: industrial | gray large tiles |

### Korp: dark industrial

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.korpBlock:0` | Korp 0 | `wall-main`, `floor` | partly | standard | light speckled granite-like |
| `Ztones:tile.korpBlock:1` | Korp 1 | `wall-accent`, `floor` | partly | standard | dark speckled |
| `Ztones:tile.korpBlock:2` | Korp 2 | `wall-accent`, `floor` | yes | standard | dark smooth |
| `Ztones:tile.korpBlock:3` | Korp 3 | `wall-accent` | yes | standard, new: industrial | dark panel |
| `Ztones:tile.korpBlock:4` | Korp 4 | `wall-accent`, `special` | yes | standard, new: industrial | dark vertical vent grille |
| `Ztones:tile.korpBlock:5` | Korp 5 | `wall-accent`, `special` | yes | standard, new: industrial | dark horizontal vent grille |
| `Ztones:tile.korpBlock:6` | Korp 6 | `wall-accent` | partly | new: industrial | dark perforated |
| `Ztones:tile.korpBlock:7` | Korp 7 | `wall-accent` | partly | standard | gray with faint X |
| `Ztones:tile.korpBlock:8` | Korp 8 | `floor`, `wall-accent` | yes | standard | dark tile grid |
| `Ztones:tile.korpBlock:9` | Korp 9 | `wall-accent` | partly | new: industrial | dark bolted panel |
| `Ztones:tile.korpBlock:10` | Korp 10 | `wall-accent` | partly | new: tech district | dark, four studs |
| `Ztones:tile.korpBlock:11` | Korp 11 | `off-theme` | no | new: tech district | black and white maze |
| `Ztones:tile.korpBlock:12` | Korp 12 | `off-theme` | no | new: tech district, new: neon/arcade | cyan light dashes on black; texture looks lit but the block emits no light |
| `Ztones:tile.korpBlock:13` | Korp 13 | `off-theme` | no | new: tech district, new: neon/arcade | cyan light dashes on black (variant) |
| `Ztones:tile.korpBlock:14` | Korp 14 | `wall-accent` | partly | standard | dark horizontal bands |
| `Ztones:tile.korpBlock:15` | Korp 15 | `trim-pillar` | partly | standard | dark vertical bands |

### Vena: dark and teal

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.venaBlock:0` | Vena 0 | `wall-accent`, `rubble-ruin` | partly | standard | dark gray rough |
| `Ztones:tile.venaBlock:1` | Vena 1 | `wall-accent`, `rubble-ruin` | partly | standard | dark gray rough (variant) |
| `Ztones:tile.venaBlock:2` | Vena 2 | `wall-accent` | partly | standard | dark framed |
| `Ztones:tile.venaBlock:3` | Vena 3 | `off-theme` | no | new: tech district | dark with teal outline |
| `Ztones:tile.venaBlock:4` | Vena 4 | `off-theme` | no | new: tech district | teal studded pattern |
| `Ztones:tile.venaBlock:5` | Vena 5 | `off-theme` | no | new: tech district | teal horizontal bands |
| `Ztones:tile.venaBlock:6` | Vena 6 | `off-theme` | no | new: tech district | teal vertical bands |
| `Ztones:tile.venaBlock:7` | Vena 7 | `off-theme` | no | new: tech district | teal maze |
| `Ztones:tile.venaBlock:8` | Vena 8 | `off-theme` | no | new: tech district | teal ornate |
| `Ztones:tile.venaBlock:9` | Vena 9 | `off-theme` | no | new: tech district | teal ornate (variant) |
| `Ztones:tile.venaBlock:10` | Vena 10 | `off-theme` | no | new: tech district | teal ornate (variant) |
| `Ztones:tile.venaBlock:11` | Vena 11 | `off-theme` | no | new: tech district | teal ornate (variant) |
| `Ztones:tile.venaBlock:12` | Vena 12 | `roof`, `wall-accent` | partly | standard | teal rough |
| `Ztones:tile.venaBlock:13` | Vena 13 | `roof`, `wall-accent` | partly | standard | pale teal framed |
| `Ztones:tile.venaBlock:14` | Vena 14 | `roof`, `wall-accent` | yes | standard, new: old town | verdigris teal smooth; metas 14-15 read as oxidized copper; a non-weathering copper-roof stand-in |
| `Ztones:tile.venaBlock:15` | Vena 15 | `roof`, `wall-accent` | yes | standard, new: old town | verdigris teal smooth, darker |

### Vect: black tech panels

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.vectBlock:0` | Vect 0 | `wall-accent` | partly | standard | black smooth |
| `Ztones:tile.vectBlock:1` | Vect 1 | `wall-accent`, `trim-pillar` | partly | standard, new: tech district | black framed |
| `Ztones:tile.vectBlock:2` | Vect 2 | `off-theme` | no | new: tech district | black, corner studs |
| `Ztones:tile.vectBlock:3` | Vect 3 | `off-theme` | no | new: tech district | black studded cross |
| `Ztones:tile.vectBlock:4` | Vect 4 | `wall-accent` | partly | new: tech district | black vertical stripes |
| `Ztones:tile.vectBlock:5` | Vect 5 | `wall-accent` | partly | new: tech district | black horizontal stripes |
| `Ztones:tile.vectBlock:6` | Vect 6 | `wall-accent` | partly | standard | black textured |
| `Ztones:tile.vectBlock:7` | Vect 7 | `off-theme` | no | new: tech district | black concentric squares |
| `Ztones:tile.vectBlock:8` | Vect 8 | `off-theme` | no | new: tech district | black dotted |
| `Ztones:tile.vectBlock:9` | Vect 9 | `off-theme` | no | new: tech district | black faint glyphs |
| `Ztones:tile.vectBlock:10` | Vect 10 | `wall-accent`, `special` | partly | standard, new: tech district | black with gray grid, 3x3; grid metas 10-13 could imitate dark curtain-wall mullions on a solid block |
| `Ztones:tile.vectBlock:11` | Vect 11 | `wall-accent`, `special` | partly | standard, new: tech district | black with gray grid, 2x2 |
| `Ztones:tile.vectBlock:12` | Vect 12 | `wall-accent` | partly | new: tech district | black with gray grid, split |
| `Ztones:tile.vectBlock:13` | Vect 13 | `wall-accent` | partly | new: tech district | black with gray grid, split (variant) |
| `Ztones:tile.vectBlock:14` | Vect 14 | `off-theme` | no | new: tech district | black circuit maze |
| `Ztones:tile.vectBlock:15` | Vect 15 | `off-theme` | no | new: tech district | black, three dots |

### Jelt: black and gold

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.jeltBlock:0` | Jelt 0 | `wall-accent` | partly | new: neon/arcade | gold smooth; art-deco lobby trim at most; off the urban palette otherwise |
| `Ztones:tile.jeltBlock:1` | Jelt 1 | `wall-accent` | no | new: neon/arcade | gold wavy |
| `Ztones:tile.jeltBlock:2` | Jelt 2 | `wall-accent` | partly | new: neon/arcade | gold fine-textured |
| `Ztones:tile.jeltBlock:3` | Jelt 3 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:4` | Jelt 4 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:5` | Jelt 5 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:6` | Jelt 6 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:7` | Jelt 7 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:8` | Jelt 8 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:9` | Jelt 9 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:10` | Jelt 10 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:11` | Jelt 11 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:12` | Jelt 12 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:13` | Jelt 13 | `off-theme` | no | new: neon/arcade | black and gold ornament |
| `Ztones:tile.jeltBlock:14` | Jelt 14 | `wall-accent`, `trim-pillar` | partly | new: neon/arcade | black panel, gold frame |
| `Ztones:tile.jeltBlock:15` | Jelt 15 | `wall-accent` | partly | new: tech district | black fine grid |

### Zorg: gray stone frames with tinted edges

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zorgBlock:0` | Zorg 0 | `wall-accent`, `trim-pillar`, `wall-main` | yes | standard, border | gray stone, gray-tinted frame; heavy frame on every face, so it tiles as panels, not as a wall surface |
| `Ztones:tile.zorgBlock:1` | Zorg 1 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, olive-tinted frame |
| `Ztones:tile.zorgBlock:2` | Zorg 2 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, olive-tinted frame |
| `Ztones:tile.zorgBlock:3` | Zorg 3 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, green-tinted frame |
| `Ztones:tile.zorgBlock:4` | Zorg 4 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, sea green-tinted frame |
| `Ztones:tile.zorgBlock:5` | Zorg 5 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, teal-tinted frame |
| `Ztones:tile.zorgBlock:6` | Zorg 6 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, slate blue-tinted frame |
| `Ztones:tile.zorgBlock:7` | Zorg 7 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, blue-tinted frame |
| `Ztones:tile.zorgBlock:8` | Zorg 8 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, indigo-tinted frame |
| `Ztones:tile.zorgBlock:9` | Zorg 9 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, violet-tinted frame |
| `Ztones:tile.zorgBlock:10` | Zorg 10 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, mauve-tinted frame |
| `Ztones:tile.zorgBlock:11` | Zorg 11 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, rose-tinted frame |
| `Ztones:tile.zorgBlock:12` | Zorg 12 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, dusty red-tinted frame |
| `Ztones:tile.zorgBlock:13` | Zorg 13 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, brown-tinted frame |
| `Ztones:tile.zorgBlock:14` | Zorg 14 | `wall-accent`, `trim-pillar` | partly | standard | gray stone, tan-tinted frame |
| `Ztones:tile.zorgBlock:15` | Zorg 15 | `wall-accent`, `trim-pillar` | yes | standard, border | gray stone, gray-tinted frame |

### Azur: blue tech

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.azurBlock:0` | Azur 0 | `wall-accent` | partly | new: neon/arcade | blue bricks; off-theme apart from the plain black metas 14-15 |
| `Ztones:tile.azurBlock:1` | Azur 1 | `off-theme` | no | new: tech district | blue panel, black frame |
| `Ztones:tile.azurBlock:2` | Azur 2 | `wall-accent` | no | new: neon/arcade | blue tile pattern |
| `Ztones:tile.azurBlock:3` | Azur 3 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:4` | Azur 4 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:5` | Azur 5 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:6` | Azur 6 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:7` | Azur 7 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:8` | Azur 8 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:9` | Azur 9 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:10` | Azur 10 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:11` | Azur 11 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:12` | Azur 12 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:13` | Azur 13 | `off-theme` | no | new: tech district | blue-on-black circuitry |
| `Ztones:tile.azurBlock:14` | Azur 14 | `wall-accent`, `floor` | partly | standard | black, four quarters |
| `Ztones:tile.azurBlock:15` | Azur 15 | `wall-accent`, `floor` | partly | standard | black fine texture |

### Bitt: dark boxes, colored outline

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.bittBlock:0` | Bitt 0 | `wall-accent` | partly | new: tech district | dark box, black outline; off-theme: sci-fi display-case look; usable only in a new tech or neon district |
| `Ztones:tile.bittBlock:1` | Bitt 1 | `wall-accent` | partly | new: tech district | dark box, dark gray outline |
| `Ztones:tile.bittBlock:2` | Bitt 2 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, yellow outline |
| `Ztones:tile.bittBlock:3` | Bitt 3 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, lime outline |
| `Ztones:tile.bittBlock:4` | Bitt 4 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, green outline |
| `Ztones:tile.bittBlock:5` | Bitt 5 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, emerald outline |
| `Ztones:tile.bittBlock:6` | Bitt 6 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, cyan outline |
| `Ztones:tile.bittBlock:7` | Bitt 7 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, blue outline |
| `Ztones:tile.bittBlock:8` | Bitt 8 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, navy outline |
| `Ztones:tile.bittBlock:9` | Bitt 9 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, indigo outline |
| `Ztones:tile.bittBlock:10` | Bitt 10 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, purple outline |
| `Ztones:tile.bittBlock:11` | Bitt 11 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, magenta outline |
| `Ztones:tile.bittBlock:12` | Bitt 12 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, crimson outline |
| `Ztones:tile.bittBlock:13` | Bitt 13 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, red outline |
| `Ztones:tile.bittBlock:14` | Bitt 14 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, orange outline |
| `Ztones:tile.bittBlock:15` | Bitt 15 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, gold outline |

### Zech: dark boxes, colored corner frames

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zechBlock:0` | Zech 0 | `wall-accent` | partly | new: tech district | dark box, gray corner frame; off-theme: glowing-frame machine look |
| `Ztones:tile.zechBlock:1` | Zech 1 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, yellow corner frame |
| `Ztones:tile.zechBlock:2` | Zech 2 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, olive corner frame |
| `Ztones:tile.zechBlock:3` | Zech 3 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, green corner frame |
| `Ztones:tile.zechBlock:4` | Zech 4 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, emerald corner frame |
| `Ztones:tile.zechBlock:5` | Zech 5 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, teal corner frame |
| `Ztones:tile.zechBlock:6` | Zech 6 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, cyan corner frame |
| `Ztones:tile.zechBlock:7` | Zech 7 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, blue corner frame |
| `Ztones:tile.zechBlock:8` | Zech 8 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, indigo corner frame |
| `Ztones:tile.zechBlock:9` | Zech 9 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, violet corner frame |
| `Ztones:tile.zechBlock:10` | Zech 10 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, purple corner frame |
| `Ztones:tile.zechBlock:11` | Zech 11 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, magenta corner frame |
| `Ztones:tile.zechBlock:12` | Zech 12 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, red corner frame |
| `Ztones:tile.zechBlock:13` | Zech 13 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, orange corner frame |
| `Ztones:tile.zechBlock:14` | Zech 14 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, amber corner frame |
| `Ztones:tile.zechBlock:15` | Zech 15 | `wall-accent` | partly | new: tech district | dark box, black corner frame |

### Zone: dark boxes, colored frames

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zoneBlock:0` | Zone 0 | `wall-accent` | partly | new: tech district | dark box, gray frame; off-theme: as Bitt and Zech |
| `Ztones:tile.zoneBlock:1` | Zone 1 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, yellow frame |
| `Ztones:tile.zoneBlock:2` | Zone 2 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, olive frame |
| `Ztones:tile.zoneBlock:3` | Zone 3 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, green frame |
| `Ztones:tile.zoneBlock:4` | Zone 4 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, emerald frame |
| `Ztones:tile.zoneBlock:5` | Zone 5 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, teal frame |
| `Ztones:tile.zoneBlock:6` | Zone 6 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, cyan frame |
| `Ztones:tile.zoneBlock:7` | Zone 7 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, blue frame |
| `Ztones:tile.zoneBlock:8` | Zone 8 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, navy frame |
| `Ztones:tile.zoneBlock:9` | Zone 9 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, indigo frame |
| `Ztones:tile.zoneBlock:10` | Zone 10 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, purple frame |
| `Ztones:tile.zoneBlock:11` | Zone 11 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, magenta frame |
| `Ztones:tile.zoneBlock:12` | Zone 12 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, red frame |
| `Ztones:tile.zoneBlock:13` | Zone 13 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, orange frame |
| `Ztones:tile.zoneBlock:14` | Zone 14 | `off-theme` | no | new: tech district, new: neon/arcade | dark box, amber frame |
| `Ztones:tile.zoneBlock:15` | Zone 15 | `wall-accent` | partly | new: tech district | dark box, black frame |

### Zeta: black with colored corner lights

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zetaBlock:0` | Zeta 0 | `off-theme` | no | new: tech district, new: neon/arcade | black, white corner lights; off-theme: texture suggests light but the block emits none |
| `Ztones:tile.zetaBlock:1` | Zeta 1 | `off-theme` | no | new: tech district, new: neon/arcade | black, yellow corner lights |
| `Ztones:tile.zetaBlock:2` | Zeta 2 | `off-theme` | no | new: tech district, new: neon/arcade | black, yellow-green corner lights |
| `Ztones:tile.zetaBlock:3` | Zeta 3 | `off-theme` | no | new: tech district, new: neon/arcade | black, lime corner lights |
| `Ztones:tile.zetaBlock:4` | Zeta 4 | `off-theme` | no | new: tech district, new: neon/arcade | black, green corner lights |
| `Ztones:tile.zetaBlock:5` | Zeta 5 | `off-theme` | no | new: tech district, new: neon/arcade | black, teal corner lights |
| `Ztones:tile.zetaBlock:6` | Zeta 6 | `off-theme` | no | new: tech district, new: neon/arcade | black, sky corner lights |
| `Ztones:tile.zetaBlock:7` | Zeta 7 | `off-theme` | no | new: tech district, new: neon/arcade | black, blue corner lights |
| `Ztones:tile.zetaBlock:8` | Zeta 8 | `off-theme` | no | new: tech district, new: neon/arcade | black, indigo corner lights |
| `Ztones:tile.zetaBlock:9` | Zeta 9 | `off-theme` | no | new: tech district, new: neon/arcade | black, violet corner lights |
| `Ztones:tile.zetaBlock:10` | Zeta 10 | `off-theme` | no | new: tech district, new: neon/arcade | black, purple corner lights |
| `Ztones:tile.zetaBlock:11` | Zeta 11 | `off-theme` | no | new: tech district, new: neon/arcade | black, magenta corner lights |
| `Ztones:tile.zetaBlock:12` | Zeta 12 | `off-theme` | no | new: tech district, new: neon/arcade | black, red corner lights |
| `Ztones:tile.zetaBlock:13` | Zeta 13 | `off-theme` | no | new: tech district, new: neon/arcade | black, orange corner lights |
| `Ztones:tile.zetaBlock:14` | Zeta 14 | `off-theme` | no | new: tech district, new: neon/arcade | black, amber corner lights |
| `Ztones:tile.zetaBlock:15` | Zeta 15 | `off-theme` | no | new: tech district, new: neon/arcade | black, gray corner lights |

### Zion: dark with colored studs

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zionBlock:0` | Zion 0 | `off-theme` | no | new: tech district, new: neon/arcade | dark, gray studs; off-theme: sci-fi |
| `Ztones:tile.zionBlock:1` | Zion 1 | `off-theme` | no | new: tech district, new: neon/arcade | dark, olive studs |
| `Ztones:tile.zionBlock:2` | Zion 2 | `off-theme` | no | new: tech district, new: neon/arcade | dark, yellow studs |
| `Ztones:tile.zionBlock:3` | Zion 3 | `off-theme` | no | new: tech district, new: neon/arcade | dark, green studs |
| `Ztones:tile.zionBlock:4` | Zion 4 | `off-theme` | no | new: tech district, new: neon/arcade | dark, emerald studs |
| `Ztones:tile.zionBlock:5` | Zion 5 | `off-theme` | no | new: tech district, new: neon/arcade | dark, teal studs |
| `Ztones:tile.zionBlock:6` | Zion 6 | `off-theme` | no | new: tech district, new: neon/arcade | dark, cyan studs |
| `Ztones:tile.zionBlock:7` | Zion 7 | `off-theme` | no | new: tech district, new: neon/arcade | dark, blue studs |
| `Ztones:tile.zionBlock:8` | Zion 8 | `off-theme` | no | new: tech district, new: neon/arcade | dark, navy studs |
| `Ztones:tile.zionBlock:9` | Zion 9 | `off-theme` | no | new: tech district, new: neon/arcade | dark, indigo studs |
| `Ztones:tile.zionBlock:10` | Zion 10 | `off-theme` | no | new: tech district, new: neon/arcade | dark, purple studs |
| `Ztones:tile.zionBlock:11` | Zion 11 | `off-theme` | no | new: tech district, new: neon/arcade | dark, magenta studs |
| `Ztones:tile.zionBlock:12` | Zion 12 | `off-theme` | no | new: tech district, new: neon/arcade | dark, red studs |
| `Ztones:tile.zionBlock:13` | Zion 13 | `off-theme` | no | new: tech district, new: neon/arcade | dark, orange studs |
| `Ztones:tile.zionBlock:14` | Zion 14 | `off-theme` | no | new: tech district, new: neon/arcade | dark, amber studs |
| `Ztones:tile.zionBlock:15` | Zion 15 | `off-theme` | no | new: tech district, new: neon/arcade | dark, black studs |

### Zome: dark with colored ovals

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zomeBlock:0` | Zome 0 | `off-theme` | no | new: tech district, new: neon/arcade | dark, gray ovals; off-theme: sci-fi |
| `Ztones:tile.zomeBlock:1` | Zome 1 | `off-theme` | no | new: tech district, new: neon/arcade | dark, yellow ovals |
| `Ztones:tile.zomeBlock:2` | Zome 2 | `off-theme` | no | new: tech district, new: neon/arcade | dark, olive ovals |
| `Ztones:tile.zomeBlock:3` | Zome 3 | `off-theme` | no | new: tech district, new: neon/arcade | dark, lime ovals |
| `Ztones:tile.zomeBlock:4` | Zome 4 | `off-theme` | no | new: tech district, new: neon/arcade | dark, green ovals |
| `Ztones:tile.zomeBlock:5` | Zome 5 | `off-theme` | no | new: tech district, new: neon/arcade | dark, teal ovals |
| `Ztones:tile.zomeBlock:6` | Zome 6 | `off-theme` | no | new: tech district, new: neon/arcade | dark, cyan ovals |
| `Ztones:tile.zomeBlock:7` | Zome 7 | `off-theme` | no | new: tech district, new: neon/arcade | dark, blue ovals |
| `Ztones:tile.zomeBlock:8` | Zome 8 | `off-theme` | no | new: tech district, new: neon/arcade | dark, navy ovals |
| `Ztones:tile.zomeBlock:9` | Zome 9 | `off-theme` | no | new: tech district, new: neon/arcade | dark, indigo ovals |
| `Ztones:tile.zomeBlock:10` | Zome 10 | `off-theme` | no | new: tech district, new: neon/arcade | dark, purple ovals |
| `Ztones:tile.zomeBlock:11` | Zome 11 | `off-theme` | no | new: tech district, new: neon/arcade | dark, magenta ovals |
| `Ztones:tile.zomeBlock:12` | Zome 12 | `off-theme` | no | new: tech district, new: neon/arcade | dark, red ovals |
| `Ztones:tile.zomeBlock:13` | Zome 13 | `off-theme` | no | new: tech district, new: neon/arcade | dark, brown ovals |
| `Ztones:tile.zomeBlock:14` | Zome 14 | `off-theme` | no | new: tech district, new: neon/arcade | dark, orange ovals |
| `Ztones:tile.zomeBlock:15` | Zome 15 | `off-theme` | no | new: tech district, new: neon/arcade | dark, black ovals |

### Zyth: dark with colored rings

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zythBlock:0` | Zyth 0 | `off-theme` | no | new: tech district, new: neon/arcade | dark, gray ring; off-theme: sci-fi |
| `Ztones:tile.zythBlock:1` | Zyth 1 | `off-theme` | no | new: tech district, new: neon/arcade | dark, yellow ring |
| `Ztones:tile.zythBlock:2` | Zyth 2 | `off-theme` | no | new: tech district, new: neon/arcade | dark, olive ring |
| `Ztones:tile.zythBlock:3` | Zyth 3 | `off-theme` | no | new: tech district, new: neon/arcade | dark, green ring |
| `Ztones:tile.zythBlock:4` | Zyth 4 | `off-theme` | no | new: tech district, new: neon/arcade | dark, emerald ring |
| `Ztones:tile.zythBlock:5` | Zyth 5 | `off-theme` | no | new: tech district, new: neon/arcade | dark, teal ring |
| `Ztones:tile.zythBlock:6` | Zyth 6 | `off-theme` | no | new: tech district, new: neon/arcade | dark, cyan ring |
| `Ztones:tile.zythBlock:7` | Zyth 7 | `off-theme` | no | new: tech district, new: neon/arcade | dark, blue ring |
| `Ztones:tile.zythBlock:8` | Zyth 8 | `off-theme` | no | new: tech district, new: neon/arcade | dark, navy ring |
| `Ztones:tile.zythBlock:9` | Zyth 9 | `off-theme` | no | new: tech district, new: neon/arcade | dark, indigo ring |
| `Ztones:tile.zythBlock:10` | Zyth 10 | `off-theme` | no | new: tech district, new: neon/arcade | dark, purple ring |
| `Ztones:tile.zythBlock:11` | Zyth 11 | `off-theme` | no | new: tech district, new: neon/arcade | dark, magenta ring |
| `Ztones:tile.zythBlock:12` | Zyth 12 | `off-theme` | no | new: tech district, new: neon/arcade | dark, crimson ring |
| `Ztones:tile.zythBlock:13` | Zyth 13 | `off-theme` | no | new: tech district, new: neon/arcade | dark, red ring |
| `Ztones:tile.zythBlock:14` | Zyth 14 | `off-theme` | no | new: tech district, new: neon/arcade | dark, orange ring |
| `Ztones:tile.zythBlock:15` | Zyth 15 | `off-theme` | no | new: tech district, new: neon/arcade | dark, gold ring |

### Zkul: colored skulls

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zkulBlock:0` | Zkul 0 | `off-theme` | no | new: tech district, new: neon/arcade | white skull pattern; off-theme: skull motif; at most a haunted-ruin easter egg |
| `Ztones:tile.zkulBlock:1` | Zkul 1 | `off-theme` | no | new: tech district, new: neon/arcade | yellow skull pattern |
| `Ztones:tile.zkulBlock:2` | Zkul 2 | `off-theme` | no | new: tech district, new: neon/arcade | lime skull pattern |
| `Ztones:tile.zkulBlock:3` | Zkul 3 | `off-theme` | no | new: tech district, new: neon/arcade | green skull pattern |
| `Ztones:tile.zkulBlock:4` | Zkul 4 | `off-theme` | no | new: tech district, new: neon/arcade | emerald skull pattern |
| `Ztones:tile.zkulBlock:5` | Zkul 5 | `off-theme` | no | new: tech district, new: neon/arcade | cyan skull pattern |
| `Ztones:tile.zkulBlock:6` | Zkul 6 | `off-theme` | no | new: tech district, new: neon/arcade | sky skull pattern |
| `Ztones:tile.zkulBlock:7` | Zkul 7 | `off-theme` | no | new: tech district, new: neon/arcade | blue skull pattern |
| `Ztones:tile.zkulBlock:8` | Zkul 8 | `off-theme` | no | new: tech district, new: neon/arcade | indigo skull pattern |
| `Ztones:tile.zkulBlock:9` | Zkul 9 | `off-theme` | no | new: tech district, new: neon/arcade | violet skull pattern |
| `Ztones:tile.zkulBlock:10` | Zkul 10 | `off-theme` | no | new: tech district, new: neon/arcade | pink skull pattern |
| `Ztones:tile.zkulBlock:11` | Zkul 11 | `off-theme` | no | new: tech district, new: neon/arcade | red skull pattern |
| `Ztones:tile.zkulBlock:12` | Zkul 12 | `off-theme` | no | new: tech district, new: neon/arcade | crimson skull pattern |
| `Ztones:tile.zkulBlock:13` | Zkul 13 | `off-theme` | no | new: tech district, new: neon/arcade | orange skull pattern |
| `Ztones:tile.zkulBlock:14` | Zkul 14 | `off-theme` | no | new: tech district, new: neon/arcade | amber skull pattern |
| `Ztones:tile.zkulBlock:15` | Zkul 15 | `off-theme` | no | new: tech district, new: neon/arcade | gray skull pattern |

### Zoea: glossy pastel crystal

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.zoeaBlock:0` | Zoea 0 | `off-theme` | no | new: tech district, new: neon/arcade | glossy white crystal; off-theme: candy-crystal look |
| `Ztones:tile.zoeaBlock:1` | Zoea 1 | `off-theme` | no | new: tech district, new: neon/arcade | glossy yellow crystal |
| `Ztones:tile.zoeaBlock:2` | Zoea 2 | `off-theme` | no | new: tech district, new: neon/arcade | glossy lime crystal |
| `Ztones:tile.zoeaBlock:3` | Zoea 3 | `off-theme` | no | new: tech district, new: neon/arcade | glossy lime crystal |
| `Ztones:tile.zoeaBlock:4` | Zoea 4 | `off-theme` | no | new: tech district, new: neon/arcade | glossy mint crystal |
| `Ztones:tile.zoeaBlock:5` | Zoea 5 | `off-theme` | no | new: tech district, new: neon/arcade | glossy aqua crystal |
| `Ztones:tile.zoeaBlock:6` | Zoea 6 | `off-theme` | no | new: tech district, new: neon/arcade | glossy sky crystal |
| `Ztones:tile.zoeaBlock:7` | Zoea 7 | `off-theme` | no | new: tech district, new: neon/arcade | glossy blue crystal |
| `Ztones:tile.zoeaBlock:8` | Zoea 8 | `off-theme` | no | new: tech district, new: neon/arcade | glossy lavender crystal |
| `Ztones:tile.zoeaBlock:9` | Zoea 9 | `off-theme` | no | new: tech district, new: neon/arcade | glossy violet crystal |
| `Ztones:tile.zoeaBlock:10` | Zoea 10 | `off-theme` | no | new: tech district, new: neon/arcade | glossy orchid crystal |
| `Ztones:tile.zoeaBlock:11` | Zoea 11 | `off-theme` | no | new: tech district, new: neon/arcade | glossy pink crystal |
| `Ztones:tile.zoeaBlock:12` | Zoea 12 | `off-theme` | no | new: tech district, new: neon/arcade | glossy salmon crystal |
| `Ztones:tile.zoeaBlock:13` | Zoea 13 | `off-theme` | no | new: tech district, new: neon/arcade | glossy peach crystal |
| `Ztones:tile.zoeaBlock:14` | Zoea 14 | `off-theme` | no | new: tech district, new: neon/arcade | glossy beige crystal |
| `Ztones:tile.zoeaBlock:15` | Zoea 15 | `off-theme` | no | new: tech district, new: neon/arcade | glossy black crystal |

### Reds: red on black

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.redsBlock:0` | Reds 0 | `wall-accent` | partly | new: neon/arcade | solid red |
| `Ztones:tile.redsBlock:1` | Reds 1 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern; off-theme apart from the plain metas |
| `Ztones:tile.redsBlock:2` | Reds 2 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:3` | Reds 3 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:4` | Reds 4 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:5` | Reds 5 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:6` | Reds 6 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:7` | Reds 7 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:8` | Reds 8 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:9` | Reds 9 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:10` | Reds 10 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:11` | Reds 11 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:12` | Reds 12 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:13` | Reds 13 | `off-theme` | no | new: neon/arcade, new: tech district | red-on-black pattern |
| `Ztones:tile.redsBlock:14` | Reds 14 | `wall-accent` | partly | standard | dark with red haze |
| `Ztones:tile.redsBlock:15` | Reds 15 | `wall-accent`, `floor` | partly | standard | near-black |

### Sync: lime green tech

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.syncBlock:0` | Sync 0 | `off-theme` | no | new: neon/arcade | bright lime smooth; off-theme: slime-green circuitry |
| `Ztones:tile.syncBlock:1` | Sync 1 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:2` | Sync 2 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:3` | Sync 3 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:4` | Sync 4 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:5` | Sync 5 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:6` | Sync 6 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:7` | Sync 7 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:8` | Sync 8 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:9` | Sync 9 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:10` | Sync 10 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:11` | Sync 11 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:12` | Sync 12 | `off-theme` | no | new: neon/arcade, new: tech district | lime-on-black pattern |
| `Ztones:tile.syncBlock:13` | Sync 13 | `off-theme` | no | new: tech district | black, lime dots |
| `Ztones:tile.syncBlock:14` | Sync 14 | `wall-accent` | partly | standard | black textured |
| `Ztones:tile.syncBlock:15` | Sync 15 | `wall-accent` | partly | standard | black, soft frame |

### Reed: green woven

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.reedBlock:0` | Reed 0 | `foliage`, `wall-accent` | partly | standard | pale green smooth; off-theme: bright green woven reed/bamboo patterns; a park hedge stand-in at best (meta 0) |
| `Ztones:tile.reedBlock:1` | Reed 1 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:2` | Reed 2 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:3` | Reed 3 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:4` | Reed 4 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:5` | Reed 5 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:6` | Reed 6 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:7` | Reed 7 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:8` | Reed 8 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:9` | Reed 9 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:10` | Reed 10 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:11` | Reed 11 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:12` | Reed 12 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:13` | Reed 13 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:14` | Reed 14 | `off-theme` | no | - | green framed or woven reed |
| `Ztones:tile.reedBlock:15` | Reed 15 | `off-theme` | no | - | green framed or woven reed |

### Tinted Glass (Glaxx)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:tile.glaxx:0` | Tinted Glass 0 | `glass` | yes | standard, border | neutral gray tinted glass; translucent (render pass 1), full block, no pane form; dark office-window tints that vanilla stained glass lacks |
| `Ztones:tile.glaxx:1` | Tinted Glass 1 | `glass` | partly | standard, border | olive tinted glass |
| `Ztones:tile.glaxx:2` | Tinted Glass 2 | `glass` | partly | standard, border | olive tinted glass |
| `Ztones:tile.glaxx:3` | Tinted Glass 3 | `glass` | partly | standard, border | green tinted glass |
| `Ztones:tile.glaxx:4` | Tinted Glass 4 | `glass` | partly | standard, border | dark green tinted glass |
| `Ztones:tile.glaxx:5` | Tinted Glass 5 | `glass` | yes | standard, border | teal tinted glass |
| `Ztones:tile.glaxx:6` | Tinted Glass 6 | `glass` | yes | standard, border | blue-teal tinted glass |
| `Ztones:tile.glaxx:7` | Tinted Glass 7 | `glass` | yes | standard, border | navy tinted glass |
| `Ztones:tile.glaxx:8` | Tinted Glass 8 | `glass` | partly | standard, border | indigo tinted glass |
| `Ztones:tile.glaxx:9` | Tinted Glass 9 | `glass` | partly | standard, border | purple tinted glass |
| `Ztones:tile.glaxx:10` | Tinted Glass 10 | `glass` | partly | standard, border | plum tinted glass |
| `Ztones:tile.glaxx:11` | Tinted Glass 11 | `glass` | partly | standard, border | brown-red tinted glass |
| `Ztones:tile.glaxx:12` | Tinted Glass 12 | `glass` | yes | standard, border | brown tinted glass |
| `Ztones:tile.glaxx:13` | Tinted Glass 13 | `glass` | yes | standard, border | olive-brown tinted glass |
| `Ztones:tile.glaxx:14` | Tinted Glass 14 | `glass` | yes | standard, border | smoke gray tinted glass |
| `Ztones:tile.glaxx:15` | Tinted Glass 15 | `glass` | yes | standard, border | near-black tinted glass |

### Single blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Ztones:stoneTile:0` | Ztone Tile 0 | `floor`, `sidewalk`, `street`, `wall-main` | yes | standard, border | gray stone tile with a raised border; registered unconditionally; one of the best paving tiles in the pack |
| `Ztones:cleanDirt:0` | Garden Soil 0 | `foliage` | partly | standard | tilled soil top, green sides; Material.grass; planter beds in parks and courtyards |
| `Ztones:booster:0` | Booster 0 | `off-theme` | no | - | red speed pad, 1/16 high; gameplay: gives Speed IV and pushes players; no collision box; needs a block below |
| `Ztones:lampf:1-6` | Flat Lamp | `light`, `ceiling` | yes | standard, border | white flat light panel; light 15; a 0.1-thick panel with no collision; meta is the face it hangs on (1-4 walls, 5 floor, 6 ceiling) and it drops if that block goes; the best flush ceiling light in this family |
| `Ztones:lampt:1-6` | Flat Lamp | `light`, `ceiling` | yes | standard, border | gray translucent flat light panel; light 15; a 0.1-thick panel with no collision; meta is the face it hangs on (1-4 walls, 5 floor, 6 ceiling) and it drops if that block goes; the best flush ceiling light in this family |
| `Ztones:lampb:1-6` | Flat Lamp | `light`, `ceiling` | yes | standard, border | black flat light panel; light 15; a 0.1-thick panel with no collision; meta is the face it hangs on (1-4 walls, 5 floor, 6 ceiling) and it drops if that block goes; the best flush ceiling light in this family |
| `Ztones:auroraBlock:0` | Aurora 0 | `off-theme` | no | - | rainbow oil-slick; off-theme: iridescent |

## Best variety pools

Weights are not set here; the pools list what fits each upstream role. Each pool is in addition to the D19 baseline block.

| Upstream role (palette char) | Baseline (D19) | Ztones variety |
|---|---|---|
| Brick wall `#` in `bricks_standard` / border | `minecraft:stonebrick` variants | Ztyl 0, 8, 9, 12; Mint 2, 14, 15; Lave 0, 4, 12; Zest 0 and 15 (painted brick); Lair 4, 11 (red brick); Cray 0, 15 |
| Accent `X` / band `$` | infested (= plain) stone brick, smooth stone double slab | Zorg 0, 15; Mint 7, 8; Lave 1-3, 9, 10; Tank 3, 4; Ztyl 5 |
| Terracotta walls (`bricks_cyan`, `_gray`, `_silver`) | `stained_hardened_clay` 9, 7, 8 | Cray 5, 15, 0; Agon 5, 14, 15; Iszm 0-2; Myst 10, 11, 15; Fort 9; Zest 0 |
| Desert walls (`bricks_desert*`) | cut sandstone, orange and red terracotta | Roen 0-3, 7, 14, 15; Sols 3, 4, 12-15; Kryp 5; Fort 11, 13; Lair 5, 15; Cray 13, 14; Agon 12; Zest 12, 13 |
| Glass `+` | `minecraft:glass`, stained glass | Glaxx 0, 5, 6, 7, 12-15 (dark tints) |
| Floors and street `S`, `u` | smooth stone double slab | Ztone Tile; Ztyl 9, 11; Mint 14, 15; Tank 10, 15; Korp 2, 8 |
| Sidewalk and plaza | (none in upstream) | Ztone Tile; Ztyl 11; Kryp 3; Roen 13; Lair 9 (old town) |
| Ceilings | (walls in upstream) | Lave 0, 8-12; Mint 2, 3; Cray 0; flat lamps |
| Light `h` (glowstone) | `minecraft:glowstone` | Flat lamps `lampf`, `lampt`, `lampb` (flush, light 15) |
| Roof | (walls or slabs in upstream) | Vena 14, 15 (verdigris); Lave 13; Tank 13 |
| Rubble `}` | wall block's rubble variant | Roen 3, 12, 14 (desert); Kryp 0, 1; Fort 0; Lair 0, 8 |
| Park soil | `minecraft:dirt`, grass | Garden Soil `cleanDirt` |
