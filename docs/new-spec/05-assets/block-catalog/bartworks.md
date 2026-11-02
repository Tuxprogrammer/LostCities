# Block catalog: bartworks

bartworks (mod id `bartworks`), merged into the GregTech jar `gregtech-5.09.54.265.jar` as GTNH 2.9.0 ships it. Blocks were read from source at the pack tag (`gtnh-src/source/GT5-Unofficial/src/main/java/bartworks/`: `common/loaders/ItemRegistry.java`, `MainMod.java`, `system/material/WerkstoffLoader.java`), names from the jar's `assets/bartworks/lang/en_US.lang`. `config/bartworks.cfg` has no block toggles. Sheet: `tools/block-contact-sheet/sheets/bartworks/glass-casings.json`, rendered to `docs/previews/blocks/bartworks/`. Part of the D25 block catalog ([decision log](../../../design/decision-log.md)); GregTech itself is in [gregtech.md](gregtech.md).

## Summary

**23 block+meta entries in 6 registry blocks.** Fit with upstream's look: 1 yes, 3 partly, 19 no.

| Role (a block can have several) | Count |
|---|---|
| wall-main | 0 |
| wall-accent | 1 |
| trim-pillar | 0 |
| floor | 0 |
| ceiling | 0 |
| roof | 0 |
| glass | 2 |
| pane | 0 |
| street | 0 |
| sidewalk | 0 |
| stairs-slab | 0 |
| rubble-ruin | 0 |
| light | 0 |
| fence-rail | 0 |
| door-trap | 0 |
| furniture-deco | 0 |
| industrial-dressing | 5 |
| foliage | 0 |
| special | 0 |
| off-theme | 16 |

Not listed: bartworks' Werkstoff blocks (`bw.blocks`-style `BWMetaGeneratedWerkstoffBlocks`, `BWMetaGeneratedBlocksCasing` small and advanced casings, ores) store the material in a **tile entity** because their material ids exceed 16 metas; all are exotic materials. The rotor block and heated water pump are tile-entity machines. `bw.sheetmetal` and `bw.frames` are registered by GT (see [gregtech.md](gregtech.md)).

**Flags**

- **Tile entities:** none among the listed blocks.
- **Ore and boulder hosts:** none.
- **Tier:** the borosilicate glass is tiered for multiblocks: meta 0 is HV, meta 1 EV, metas 2-10 IV to UXV (out by record 0004).
- **Deprecated blocks:** `BW_GlasBlocks` and `BW_GlasBlocks2` are still registered for old worlds but hidden in NEI; the colored borosilicate glass lives only there. Never place them.
- No light, no gravity, ordinary hardness.

## Glass and casings (sheet `glass-casings.json`)

Borosilicate glass is a clear pane with a thin colored frame per tier; the HV one is a clean white-framed window.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `bartworks:BW_TieredGlass:0` | Borosilicate Glass Block | glass | yes: clear, thin white frame | urban, industrial | HV tiered glass; plain block |
| `bartworks:BW_TieredGlass:1` | Titanium Reinforced Borosilicate Glass Block | glass | partly: lilac frame | industrial | EV tiered glass |
| `bartworks:BW_TieredGlass:2` | Tungstensteel Reinforced Borosilicate Glass Block | off-theme | no | none | IV: above the EV ceiling |
| `bartworks:BW_TieredGlass:3` | Rhodium-Plated Palladium Reinforced Borosilicate Glass Block | off-theme | no | none | LuV: above the EV ceiling |
| `bartworks:BW_TieredGlass:4` | Iridium Reinforced Borosilicate Glass Block | off-theme | no | none | ZPM: above the EV ceiling |
| `bartworks:BW_TieredGlass:5` | Osmium Reinforced Borosilicate Glass Block | off-theme | no | none | UV: above the EV ceiling |
| `bartworks:BW_TieredGlass:6` | Neutronium Reinforced Borosilicate Glass Block | off-theme | no | none | UHV: above the EV ceiling |
| `bartworks:BW_TieredGlass:7` | Infinity Reinforced Borosilicate Glass Block | off-theme | no | none | UEV: above the EV ceiling |
| `bartworks:BW_TieredGlass:8` | Transcendentally Reinforced Borosilicate Glass Block | off-theme | no | none | UIV: above the EV ceiling |
| `bartworks:BW_TieredGlass:9` | Hexanite Reinforced Borosilicate Glass Block | off-theme | no | none | UMV: above the EV ceiling |
| `bartworks:BW_TieredGlass:10` | Universium Reinforced Borosilicate Glass Block | off-theme | no | none | UXV: above the EV ceiling |
| `bartworks:BW_ExtraGlass:0` | Thorium Yttrium Glass Block | off-theme | no | none | endgame glass |
| `bartworks:BW_GlasBlocks:6` | Colored Borosilicate Glass 1 | off-theme | no | none | deprecated block, hidden in NEI; never place |
| `bartworks:BW_GlasBlocks:7` | Colored Borosilicate Glass 2 | off-theme | no | none | deprecated block, hidden in NEI; never place |
| `bartworks:BW_GlasBlocks:8` | Colored Borosilicate Glass 3 | off-theme | no | none | deprecated block, hidden in NEI; never place |
| `bartworks:BW_GlasBlocks:9` | Colored Borosilicate Glass 4 | off-theme | no | none | deprecated block, hidden in NEI; never place |
| `bartworks:BW_GlasBlocks:10` | Colored Borosilicate Glass 5 | off-theme | no | none | deprecated block, hidden in NEI; never place |
| `bartworks:BW_GlasBlocks:11` | Colored Borosilicate Glass 6 | off-theme | no | none | deprecated block, hidden in NEI; never place |
| `bartworks:BW_Machinery_Casings:0` | Nickel-Zinc Ferrite Block | wall-accent, industrial-dressing | partly: dark slate | industrial | machine casing (HV-EV era); plain block |
| `bartworks:BW_Machinery_Casings:1` | Transformer-Winding Block | industrial-dressing | partly: copper winding | industrial | coil look |
| `bartworks:GT_LESU_CASING:0` | LESU Casing | industrial-dressing | no: lapis panel | industrial | GT2-style energy storage casing |
| `bartworks:BW_ItemBlocks:0` | Etched Lapis Cell | industrial-dressing | no | industrial | lapis cell |
| `bartworks:BW_ItemBlocks:1` | Plated Lapis Cell | industrial-dressing | no | industrial | lapis cell |

## Best variety pools

- **Glass `a`, `+`:** Borosilicate Glass Block `bartworks:BW_TieredGlass:0` as plain framed clear glass, Titanium Reinforced `:1` for an industrial building's lilac-framed windows.
- **Reserved for GTNH-themed buildings (D28):** Nickel-Zinc Ferrite Block `bartworks:BW_Machinery_Casings:0` as a dark slate panel and Transformer-Winding Block `:1` for substations are casings, so they wait with GT's casings ([index](index.md#reserved-for-the-gtnh-themed-buildings)).
