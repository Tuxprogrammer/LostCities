# Catwalks 2

Family D. Mod ID `catwalks`, jar `catwalks-2.4.2-GTNH.jar`. Source read: `mods/catwalks/com/thecodewarrior/catwalks/CatwalkMod.java` (L135, L176-L242), `block/BlockCatwalk.java`, `block/BlockCagedLadder.java`, `block/BlockScaffold.java`, `block/BlockSupportColumn.java`, `block/BlockSturdyRail*.java`; pack config `config/catwalks.cfg`. Sheets: `tools/block-contact-sheet/sheets/catwalks/catwalks.json` (previews in `docs/previews/blocks/catwalks/`, local only).

## Summary

- **46 registry names**: 8 catwalks (`catwalk_{lit,unlit}[_nobottom][_tape]`), 32 caged ladders (`cagedLadder_{north,south,west,east}_{lit,unlit}[_nobottom][_tape]`), `scaffold` (metas 0-1), `support_column`, and 4 sturdy rails. Each variant is its own block; catwalk meta 0-15 holds the open-side bits.
- **No tile entities.** Lit variants emit light 12 (`CatwalkMod.lightLevel`, `BlockCatwalk.java` L528-L530) with no power: built-in strip lights. No gravity.
- **Look**: dark steel grating with railings, optional yellow-and-black caution tape on the edges, optional strip lights; caged ladders with steel hoops. Exactly the industrial and fire-escape detail upstream lacks: rooftop walkways, fire escapes on brick facades, factory mezzanines, bridges between buildings, ruined scaffolding.
- **Catwalk meta**: bits 0-3 mark the east, west, south and north sides open (`getBit(meta, 0..3)` in `BlockCatwalk.java` L287-L320); meta 0 has railings on all four sides. `_nobottom` variants have no floor grate (a railing frame only).
- **Caged ladder** facing is the registry name (north, south, west, east), not meta. `_nobottom` means the bottom of the cage is open (a landing).
- **Interaction**: catwalks and ladders are climbable and walkable at configured speed (`catwalkSpeed=1.0`); the blowtorch and caution tape items change variants in game. Scaffolds are climbable.
- **Fallback when the mod is absent**: catwalk to `minecraft:iron_bars` railing over a `minecraft:stone_slab` (or a `minecraft:trapdoor` laid flat), caged ladder to `minecraft:ladder`, scaffold to `minecraft:fence`, support column to `minecraft:cobblestone_wall` or `minecraft:iron_bars`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `catwalks:catwalk_unlit:0-15` | Catwalk | floor, fence-rail | yes | industrial, ruin | the default; meta = open sides |
| `catwalks:catwalk_unlit_tape:0-15` | Catwalk with caution tape | floor, fence-rail | yes | industrial | |
| `catwalks:catwalk_lit:0-15`, `catwalk_lit_tape:0-15` | Lit Catwalk | floor, fence-rail, light | yes | industrial | light 12, no power |
| `catwalks:catwalk_unlit_nobottom:0-15`, `…_tape`, `catwalk_lit_nobottom…` | Catwalk railing (no floor) | fence-rail | yes | industrial | rooftop guard rails, balcony rails over a solid floor |
| `catwalks:cagedLadder_{north,south,west,east}_unlit` | Caged Ladder | special | yes | industrial, urban-brick | fire escapes, rooftop access, water towers |
| `catwalks:cagedLadder_*_unlit_tape`, `…_lit`, `…_lit_tape` | Caged Ladder variants | special, light | yes | industrial | lit = light 12 |
| `catwalks:cagedLadder_*_nobottom…` | Caged Ladder, open bottom | special | yes | industrial | the bottom block of a run |
| `catwalks:scaffold:0` | Scaffolding | rubble-ruin, special | yes | ruin, industrial | steel frame cube; construction sites, abandoned facades |
| `catwalks:scaffold:1` | Builder's Scaffolding | rubble-ruin, special | yes | ruin | wood-and-steel variant |
| `catwalks:support_column:0-3` | Support Column | trim-pillar | yes | industrial | thin steel post; meta toggles orientation (`BlockSupportColumn.java` L87-L101) |
| `catwalks:sturdy_rail`, `sturdy_rail_powered`, `sturdy_rail_detector`, `sturdy_rail_activator` | Sturdy Rails | special | partly | industrial | rails that sit on catwalks; for elevated tracks |

## Best variety pools

- **floor + fence-rail (industrial)**: unlit catwalk mostly, taped and lit catwalks at low weight; meta chosen from neighbors so railings face the drop.
- **fence-rail (rooftops)**: `_nobottom` catwalks along roof edges.
- **special (fire escapes)**: caged ladders up the side of brick buildings with short catwalk landings: a strong "20th-century city" detail.
- **rubble-ruin**: scaffolding on half-finished or collapsed upper floors.
- **trim-pillar**: support columns under catwalks and elevated walkways.

## Worldgen notes

- Cheap: plain blocks, no TE, no ticking. Rendering is a custom ISBRH (thin geometry), modest cost.
- Choose catwalk meta from the layout (open where the walkway continues, closed where it ends) rather than relying on in-game neighbor logic.
- Lit variants are a free light source for industrial interiors and alleys.
