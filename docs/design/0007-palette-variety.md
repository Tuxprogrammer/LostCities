# 0007 · Palette variety

- Status: Accepted
- Gate: none (M11's design; the beta review judges the looks)
- Amends: none; applies D25, D28 and D29 and the [block catalog](../new-spec/05-assets/block-catalog/index.md)'s pools
- Date: 2026-11-02

## Context

D25 asks for variety: palettes draw weighted variety from the pack's whole decorative catalog, per role, so that cities stop feeling synthetic. S4 wrote the catalog and its variety pools, each a role's candidates with weights; M02 loaded and resolved them, and nothing used them. The alpha review added three asks for M11 ([reviews/2026-11-02-alpha.md](../reviews/2026-11-02-alpha.md)): interiors are dull (AR-3), the desert's new muted pools lack pack variety (D145, AR-16), and park greenery is vanilla only (D146).

Upstream's format gives one lever: a style draws one palette per group, and a building's own palette and a part's sit on top. Three facts constrain how variety can use it:

- The style's draw consumes the chunk's building random, one float per group, and the same random then rolls loot and ruins. A new group in a style would move every later draw, so the layout of existing seeds would change.
- A wall candidate belongs to its wall family: a stone-brick building may take Railcraft quarried brick, a cyan terracotta one may take cyan hempcrete, never the other way round.
- Upstream's interiors are the wall block. Floors, ceilings and partitions are the `#` of the floor part, so no palette entry can reach a floor without reaching the walls.

## Decision

1. **Option palettes, drawn per building.** Each pool candidate becomes a small option palette that redefines the characters of its role. Per style, groups list options with integer weights and `none`, the baseline (upstream's look), which keeps the largest share. Data: `data/lostcitiesnh/blockmap/variety.json`, written by `tools/asset-convert/variety.py` from the catalog; format in [asset-format.md](../new-spec/05-assets/asset-format.md#palette-variety).
2. **A seed of its own.** The layout draws the style's palettes exactly as upstream, then asks the style for its variety with a seed from the world seed and the chunk (`LayoutPlanner.varietySeed`), never from the chunk's random. Each group draws from that seed mixed with its name. A multibuilding's chunks copy their top-left chunk's palettes, variety included. Chunks without a building (streets, parks, plazas) share one seed per four-by-four block of chunks, so a park spanning chunks keeps one planting scheme and neighboring streets share their compiled palette.
3. **Layering.** The options are appended to the chunk's palette list, so they sit above the style's palettes and below the building's palette and the part's, which keep the last word: `building6` stays deepslate, the cabin keeps its copper, the oil rig and the highway parts keep their local palettes.
4. **Conditions.** A group may apply only when the style drew a given palette (`when`: the stone-brick wall group only for `bricks_standard`, each glass group for its glass palette) and only to building chunks or only to chunks without a building (`for`).
5. **Unavailable means baseline.** An option whose blocks are not all registered is unavailable; its share goes to the baseline, as a pool candidate falls back to the baseline when its mod is absent. No error, so a dev server without the pack's mods loads cleanly; the real-pack report must list no unavailable block.
6. **Interiors (AR-3).** Options may define three characters no upstream part uses: interior floor, ceiling and ceiling light. A building's floor parts (below the roof, not overlay parts) substitute them, column by column: a `#` or `S` with air above and a solid character above that air is a floor; a `#` with air below and a solid character below that is a ceiling, and a light where the column is on a grid of one per four-by-four bay. Roofs and terraces open to the sky, walls and every other character keep their blocks. Interior groups: floors (30 baseline, wood, kitchen tile, lab and marble tiles, polished stone; at most 30 of 100 spawn-proof, D28 rule 1), ceilings (50 baseline, office panels, lab tiles, metal panels), lights (55 unlit, glowstone, sea lanterns, lamp blocks), furniture lists (workshop, kitchen, study, industrial, wood workbenches; upstream's cobweb share kept), bookshelves and fences.
7. **Park flora (D146).** Options may define five flora lists, one per ParkGreenery table; a park chunk's flora list replaces the vanilla table, with the same two draws per grass block. Planters (`p`, `K`) get pack flower options. Only flowers that stay on grass are used (Et Futurum's cornflower and lily of the valley, Biomes O' Plenty's ground flowers but miner's delight, Botania's mystical flowers); `avoidFoliage` removes them like upstream's flowers.
8. **The desert stays muted (AR-16).** Desert walls draw from the desert, bleached and taupe pools only, trims and bands from the desert pools, floors from pale woods, sandstone, white and light gray terracotta and pale tiles; no red, orange or saturated block.
9. **Never a block that regenerates its own chunk.** A fresh chunk's sky light is built before the chunk is registered, and it asks each block for its world-aware light opacity; a block whose override reads the world at its own position generates the chunk again until the stack overflows. Automagy's glowing amber (a Thaumcraft `blockCosmeticOpaque`) did that in the first M11 shot run. Both are on the never-place list, and `assets.catalog` fails on any placed block whose class overrides that method.
10. **Excluded on purpose.** No speed block (D28 rule 3), not even in walls, because a wall is also a roof and a ruin; no tile-entity block that needs data to look right (D28 rule 6); no decorative metal (D29), whose capped accents need a per-building cap the variety does not have; streets, highways, rails, bridges and building-local palettes keep their blocks (M06 owns the highway and rail surfaces).
11. **A switch.** `worldgen.paletteVariety` (on by default), a module flag pinned at world creation, turns the draw off.

Content at this record's date: 359 options in 53 groups across the standard and desert styles (14 to 16 wall, accent and band options per wall family, 10 glass groups, 41 interior floors, 15 ceilings, 12 lights, 9 furniture lists, 10 bookshelves, 7 fences, 4 planters, 2 park flora).

## Consequences

- Every building chunk's palette list grows by up to a dozen option names; a chunk's compiled palette is cached by that list, so caches miss more often. Compiled palettes share their entries' slot arrays, and streets share their variety per block of chunks, to keep the extra compiles cheap; the budget runs (B1 to B5) cover it.
- Block content changes for the same seed, the layout does not: the city goldens and `city.real`'s hashes change, the layout goldens do not. The variety file is part of the asset-set hash a world pins.
- `assets.pack_resolution` reports palette-variety blocks too; `assets.catalog` writes every part under every style's palettes and the variety and fails on an undefined character or an unregistered block, in the real pack.
- The interior characters are a deviation from upstream's interiors, recorded in [05-assets](../new-spec/05-assets/index.md#styles-and-city-styles).
- Color feathering ([0012](0012-facade-color-feathering.md) §5) takes the variety into a facade's tone and, when the default draw clashes with a neighbor, tries further draws of the same groups; the groups and options stay as they are.

## Alternatives considered

- **More palettes in the style's own groups** (as D145 did for the desert). No extra draw, but every candidate would need a full wall palette, the wall family could not be kept, and interiors and flora could not vary independently of the walls. Kept only for D145's three desert palettes.
- **New groups in the styles.** Changes every seed's loot and ruins, because they draw from the chunk's random.
- **Variety per block instead of per building.** Upstream's look is one material per facade (the catalog's rule), and the owner's AR-17 asks for whole facades.
- **Rewriting the parts' floors.** Faithful layouts (0004 §1) and re-pinnable assets rule out editing upstream's parts; the column rule reads them as they are.

## Still open

- Weights are first judgments; the beta review tunes them (the catalog's status).
- D29's metal accents need a per-building cap before they join the variety.
