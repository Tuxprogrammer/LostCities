# Block catalog: GT++

GT++ (mod id `miscutils`), merged into the GregTech jar `gregtech-5.09.54.265.jar` as GTNH 2.9.0 ships it. Blocks were read from source at the pack tag (`gtnh-src/source/GT5-Unofficial/src/main/java/gtPlusPlus/`: `core/block/ModBlocks.java`, `xmod/gregtech/loaders/GTPPBlocks.java`, the casing classes in `xmod/gregtech/common/blocks/` and their textures in `TexturesGtBlock.java`), names from the jar's `assets/gregtech/lang/en_US.lang` and `assets/miscutils/lang/en_US.lang`. No GT++ config switches these blocks off. Sheets: `tools/block-contact-sheet/sheets/gtplusplus/casings.json` and `compressed.json`, rendered to `docs/previews/blocks/gtplusplus/`. Part of the D25 block catalog ([decision log](../../../design/decision-log.md)); GregTech itself is in [gregtech.md](gregtech.md).

## Summary

**36 block+meta entries in 5 registry blocks.** Fit with upstream's look: 3 yes, 13 partly, 20 no.

| Role (a block can have several) | Count |
|---|---|
| wall-main | 3 |
| wall-accent | 6 |
| trim-pillar | 0 |
| floor | 0 |
| ceiling | 0 |
| roof | 0 |
| glass | 0 |
| pane | 0 |
| street | 0 |
| sidewalk | 0 |
| stairs-slab | 0 |
| rubble-ruin | 0 |
| light | 0 |
| fence-rail | 1 |
| door-trap | 0 |
| furniture-deco | 0 |
| industrial-dressing | 17 |
| foliage | 0 |
| special | 2 |
| off-theme | 14 |

Not listed:

- **Material blocks and frame boxes.** GT++ registers one block per material and shape (`BlockBaseModular`): 111 "Block of …" and 63 "… Frame Box" blocks for its own alloys and elements (Staballoy, Tumbaga, Inconel, Zirconium Carbide, radioactive isotopes…), each its own registry name with meta 0, colored by a runtime tint. All are exotic; none is construction material.
- **Casings above EV or of unclear tier:** the rest of `miscutils.blockcasings` (4 Metalworking, 8 Matter Generation Coil, 9 Matter Fabricator, 12-14 reactor and blast smelter coil), `gtplusplus.blockcasings.2` 1-3, 9-11, `gtplusplus.blockcasings.3`, `.4`, `.5`, `.6`, `gtplusplus.blockspecialcasings.1` and `.3`, `gtplusplus.blocktieredcasings.1`. Most are IV+ (Hastelloy, Incoloy, Trinium, fusion, Vanadium Redox IV-UHV).
- **Machines and gadgets** (fish trap, circuit programmer, lead-lined box, flask setter, infinite fluid tank, mining explosives, hellfire): tile entities or hazards.

**Flags**

- **Tiers are unverified.** GT++ casings are assembled at low voltage, and their tier comes from the material (Potin and Tumbaga MV, Grisium and Zirconium Carbide around EV, Tantalloy-61 EV-IV). The rows say which material; confirm before use.
- **Tile entities:** none among the listed blocks.
- **Ore and boulder hosts:** none. Compressed netherrack is a different block from `minecraft:netherrack`.
- **Compressed glowstone gives no light.** The block extends obsidian and never sets a light value, so all five compressed glowstone metas are dark glowstone-colored obsidian.
- **Compressed obsidian** is hardness 50, resistance 2000, and worth 9 to 59,049 obsidian.
- **Wither Guard** (`miscutils:blockBlackGate`) is unbreakable (hardness −1) and explosion-proof. Never place it.
- Casings behave like GT casings: wrench mining, no mob spawns, machine-update scan on placement.

## Casings plausibly up to EV (sheet `casings.json`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `miscutils:miscutils.blockcasings:0` | Centrifuge Casing | industrial-dressing | no | industrial | Inconel-era (HV-EV, unverified); vent face |
| `miscutils:miscutils.blockcasings:1` | Structural Coke Oven Casing | wall-accent, industrial-dressing | partly: blue-gray plate | industrial | Tantalloy-61 (EV-IV, unverified) |
| `miscutils:miscutils.blockcasings:2` | Heat Resistant Coke Oven Casing | industrial-dressing | no | industrial | Bronze Firebox texture |
| `miscutils:miscutils.blockcasings:3` | Heat Proof Coke Oven Casing | industrial-dressing | partly | industrial | Steel Firebox texture |
| `miscutils:miscutils.blockcasings:5` | Electrolyzer Casing | wall-accent, industrial-dressing | partly: rose-brown plate | industrial | Potin (MV) |
| `miscutils:miscutils.blockcasings:6` | Wire Factory Casing | wall-accent, industrial-dressing | partly: black plate | industrial | Maraging steel (unverified tier) |
| `miscutils:miscutils.blockcasings:7` | Maceration Stack Casing | industrial-dressing | partly: brass plate | industrial | Tumbaga (MV) |
| `miscutils:miscutils.blockcasings:10` | Iron Plated Bricks | wall-main, industrial-dressing | yes: white riveted panel | urban, industrial | steam age |
| `miscutils:miscutils.blockcasings:11` | Multitank Exterior Casing | industrial-dressing | partly: teal ribbed | industrial | Grisium (EV, unverified) |
| `miscutils:miscutils.blockcasings:15` | Blast Smelter Casing Block | wall-main, industrial-dressing | yes: beige riveted panel | urban, industrial | Zirconium carbide (EV-IV, unverified) |
| `miscutils:gtplusplus.blockcasings.2:0` | Thermal Processing Casing | wall-accent | partly: oxblood plate | industrial | Red steel (HV-EV, unverified) |
| `miscutils:gtplusplus.blockcasings.2:4` | Wash Plant Casing | industrial-dressing | partly | industrial | Grisium (EV, unverified) |
| `miscutils:gtplusplus.blockcasings.2:5` | Industrial Sieve Casing | industrial-dressing | partly | industrial | tier unverified |
| `miscutils:gtplusplus.blockcasings.2:6` | Large Sieve Grate | industrial-dressing, fence-rail | no | industrial | tier unverified |
| `miscutils:gtplusplus.blockcasings.2:7` | Vanadium Redox Power Cell (EV) | industrial-dressing | partly: striped cell | industrial | EV |
| `miscutils:gtplusplus.blockcasings.2:8` | Sub-Station External Casing | industrial-dressing | no: dark maroon | industrial | tier unverified |
| `miscutils:gtplusplus.blockcasings.2:12` | Bulk Production Frame | wall-main | yes: white panel (same texture as Iron Plated Bricks) | urban, industrial | tier unverified |
| `miscutils:gtplusplus.blockcasings.2:13` | Cutting Factory Frame | off-theme | no: purple | none | tier unverified |
| `miscutils:gtplusplus.blockcasings.2:15` | Sterile Farm Casing | industrial-dressing | partly: wooden crate | industrial | tier unverified |
| `miscutils:gtplusplus.blockspecialcasings.2:0` | Strong Bronze Machine Casing | industrial-dressing | no | industrial | steam age; Bronze Plated Bricks texture |
| `miscutils:gtplusplus.blockspecialcasings.2:1` | Sturdy Aluminium Machine Casing | wall-accent | partly: light blue | industrial | steam/LV; Frost Proof texture |

## Compressed blocks and the Wither Guard (sheet `compressed.json`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `miscutils:blockCompressedObsidian:0` | Compressed Obsidian (9) | wall-accent, special | partly: obsidian look | industrial | hardness 50, resistance 2000 (obsidian); worth 9 obsidian: loot |
| `miscutils:blockCompressedObsidian:1` | Double Compressed Obsidian (81) | off-theme | no | none | hardness 50, resistance 2000 (obsidian); huge material value |
| `miscutils:blockCompressedObsidian:2` | Triple Compressed Obsidian (729) | off-theme | no | none | hardness 50, resistance 2000 (obsidian); huge material value |
| `miscutils:blockCompressedObsidian:3` | Quadruple Compressed Obsidian (6561) | off-theme | no | none | hardness 50, resistance 2000 (obsidian); huge material value |
| `miscutils:blockCompressedObsidian:4` | Quintuple Compressed Obsidian (59049) | off-theme | no | none | hardness 50, resistance 2000 (obsidian); huge material value |
| `miscutils:blockCompressedObsidian:5` | Inverted Obsidian | off-theme | no | none | hardness 50, resistance 2000 (obsidian) |
| `miscutils:blockCompressedObsidian:6` | Compressed Glowstone (9) | off-theme | no: looks like glowstone but **gives no light** | none | hardness 50, resistance 2000 (obsidian); the block extends obsidian and sets no light value |
| `miscutils:blockCompressedObsidian:7` | Double Compressed Glowstone (81) | off-theme | no: looks like glowstone but **gives no light** | none | hardness 50, resistance 2000 (obsidian); the block extends obsidian and sets no light value |
| `miscutils:blockCompressedObsidian:8` | Triple Compressed Glowstone (729) | off-theme | no: looks like glowstone but **gives no light** | none | hardness 50, resistance 2000 (obsidian); the block extends obsidian and sets no light value |
| `miscutils:blockCompressedObsidian:9` | Quadruple Compressed Glowstone (6561) | off-theme | no: looks like glowstone but **gives no light** | none | hardness 50, resistance 2000 (obsidian); the block extends obsidian and sets no light value |
| `miscutils:blockCompressedObsidian:10` | Quintuple Compressed Glowstone (59049) | off-theme | no: looks like glowstone but **gives no light** | none | hardness 50, resistance 2000 (obsidian); the block extends obsidian and sets no light value |
| `miscutils:blockCompressedObsidian:11` | Compressed Netherrack | off-theme | no | none | hardness 50, resistance 2000 (obsidian); a different block from netherrack, so not an ore host |
| `miscutils:blockCompressedObsidian:12` | Double Compressed Netherrack | off-theme | no | none | hardness 50, resistance 2000 (obsidian); a different block from netherrack, so not an ore host |
| `miscutils:blockCompressedObsidian:13` | Triple Compressed Netherrack | off-theme | no | none | hardness 50, resistance 2000 (obsidian); a different block from netherrack, so not an ore host |
| `miscutils:blockBlackGate:0` | Wither Guard | special | no | none | **unbreakable** (hardness −1) and explosion-proof: never place |

## Best variety pools

None for the standard pools. Every GT++ block that fits a role is a casing, and casings are reserved for the GTNH-themed buildings after beta (D28 rule 4, [index](index.md#reserved-for-the-gtnh-themed-buildings)). There they would serve as:

- **Main wall, clean panel (quartz family, industrial reskin):** Iron Plated Bricks `miscutils:miscutils.blockcasings:10` and Bulk Production Frame `miscutils:gtplusplus.blockcasings.2:12` (same white riveted panel), Blast Smelter Casing Block `miscutils:miscutils.blockcasings:15` (beige panel).
- **Accent `X`:** Thermal Processing Casing `gtplusplus.blockcasings.2:0` (oxblood), Electrolyzer Casing `miscutils.blockcasings:5` (rose-brown), Wire Factory Casing `:6` (black).
- **Industrial dressing:** Multitank Exterior and Wash Plant Casings (teal tanks), Vanadium Redox Power Cell (EV) for substations, Sterile Farm Casing as crates.

Nothing here for glass, light, streets or desert. Compressed obsidian and compressed glowstone are storage (loot only), and the Wither Guard is unbreakable: never placed (D28 rule 7).
