# 0007 · Facade color feathering

- Status: Accepted (M12, decision log D-M12-1)
- Gate: M12 exit (color feathering)
- Amends: none; refines how [building-info.md](../original-spec/04-city-layout/building-info.md) step 12.13 picks a chunk's palettes
- Date: 2026-11-02

## Context

Alpha review AR-17 ([reviews/2026-11-02-alpha.md](../reviews/2026-11-02-alpha.md)), in the owner's words: "the building's facade should be 1 color but the transitions of side-by-side buildings shouldn't be so abrupt that you put sandstone, a low saturation bright color next to a highly saturated dark red color building. I don't want this to become a rule of 'only light buildings everywhere'".

Upstream draws a chunk's palettes on their own (step 12.13: one `nextFloat()` per palette group of the chunk's style), so nothing relates a building's colors to its neighbors'. Where two city styles meet (a desert city beside a standard one, the alternative style at a city's rim, M11's new styles), any pair can stand side by side. D145 muted the desert palettes, which removed the worst pair in one style; it cannot help across styles, and M11 widens every pool again.

Three things have to be settled: how a block's color is known, what "too abrupt" means, and how a chunk picks its palettes against its neighbors when chunks are planned in any order.

## Decision

### 1. Block colors are measured once and shipped as data

- `tools/block-colors/measure.py` reads every block the contact sheets name (`tools/block-contact-sheet/sheets/`, about 7,800 blocks: vanilla, Chisel, Et Futurum, GT and every other catalogued mod) from the GTNH 2.9.0 jars, the same textures the sheets render: the side face, or the one face, tinted where the sheet says the game tints it.
- A block's color is the mean of its opaque pixels (alpha ≥ 128), averaged in linear light and written back as sRGB; its coverage is the share of opaque pixels.
- The table ships in the jar as `data/lostcitiesnh/blockmap/colors.tsv` (`modid:name:meta`, `#rrggbb`, coverage). It is regenerated when the catalog grows; the commit says so.
- World generation reads only the table: never a texture, so a dedicated server (which has no vanilla textures) and a client with a resource pack plan the same city.
- A block missing from the table falls back to its meta 0 entry, then counts as unknown. An unknown block takes no part in a facade's color.

### 2. A facade's color is its outer walls' blocks

- A building's **facade characters** are the palette characters on the outer ring of its parts (x or z at the part's edge, every y), counted over all its parts. Counts are cached per building.
- A **stack** is the chunk's palette layers, lowest first (the style's picks, then the building's own palette), as `CityPalettes` compiles them. Each facade character resolves to its topmost defining entry, following references, as `CompiledPalette` does; a random list's color is the mean over its 128 slots.
- Blocks with coverage under 60 % (glass, panes, bars, air) and unknown blocks are skipped. The facade color is the count-weighted mean, in CIELAB, of what remains.
- The **tone** of a facade is two numbers: lightness `L*` (0 to 100) and saturation `S = C*ab / max(L*, 25)`, chroma relative to lightness. Saturation, not chroma, is what makes a dark red read as "highly saturated" beside a bleached sandstone of similar chroma.

### 3. The contrast rule

- The contrast of two tones is `J = sqrt(ΔL*² + (60 · ΔS)²)`. Hue is left free, so a city keeps its variety of materials.
- Two neighboring buildings may not exceed `facadeContrast` (profile option, default 45, range 10 to 200; 200 turns feathering off).
- With the default, measured on the pack's blocks: sandstone beside stone brick (`J` ≈ 37), light gray terracotta (36) or brick (35) is allowed; sandstone beside red terracotta (68), orange terracotta (66), cyan terracotta (50) or deepslate (63), and quartz beside cyan terracotta (54), are not. Dark beside mid gray and mid beside pale stay allowed, so this is not a rule of light buildings.
- **Neighbors** are building units (a single building chunk, or a whole multibuilding) whose footprints touch, diagonals included: what stands side by side. A street between two buildings separates them.

### 4. Choosing palettes against neighbors, in any order

Palette choice for buildings becomes its own cached stage of the layout, `facade`, a pure function of the seed, the unit's coordinates, the characteristics of the units around it and the assets:

1. **Units and priority.** Every unit that could hold a building has a priority: units whose facade the palettes cannot change (a building whose own palette fixes its walls) come first, then a hash of the seed and the unit's top-left chunk, then its coordinates. Priorities are a strict total order.
2. **Default picks.** The unit draws one float per palette group from its own facade random (seeded from the world seed and the unit), and each group's default is upstream's weighted pick with that float.
3. **Constraints.** The unit reads the chosen tones of its neighboring units with a higher priority. Each of those is the same stage, asked recursively; every step goes to a strictly higher priority, so the recursion ends, and its depth is the longest increasing-priority path, a handful of units in practice.
4. **Feathered picks.** Groups are taken in order. For each group with more than one palette, a candidate's tone is the facade tone of the stack with that candidate and the picks made so far. The default is kept when its tone is within `facadeContrast` of every constraint. Otherwise the pick is redrawn, with the same float and the original weights, among the candidates that are; when none is, the candidate with the smallest worst contrast is taken.
5. **Use.** The plan stage still draws its palette floats from the building random, so its stream is unchanged, and then takes the facade stage's palettes for every chunk that could hold a building. A multibuilding's chunks share their top-left's palettes, as before. Street chunks keep the plan's own draw.

Every pair of neighbors is then checked once, by the one with the lower priority, against the choice of the one with the higher; a unit's choice never depends on what was generated before it, only on what its higher-priority neighbors would choose, which is itself a pure function. Planning chunks in a different order, or with a different cache size, gives the same palettes.

## Consequences

- **Layout change.** Every building chunk's palettes come from the facade random now, so the layout version rises (D-M12 rows) and a world pinned before keeps generating with the new rule, logging the seam as D153 does.
- **Code.** `layout` gains the facade stage (`FacadePlanner`) and two views: `StyleView.groups()` and `FacadeView`, which `asset.view.LayoutAssets` implements over `asset.block.BlockColors` and `asset.view.FacadeTones`. `layout` stays free of Minecraft types (`LayoutPurityTest`).
- **Cost.** A facade is computed once per unit and cached with the plans; a tone is cached per stack and building. The stage reads characteristics one chunk further than the plan did only along increasing-priority paths, which the plan prefetcher already covers (D125).
- **Data.** `colors.tsv` is about 300 KB. M11's new palettes need no change here as long as their blocks are in the catalog's sheets; a block that is not is unknown and skipped.
- **Spec.** A deviation from upstream's step 12.13, recorded in [new-spec 04-city-layout](../new-spec/04-city-layout/index.md).

## Alternatives considered

- **Reading textures at run time.** A dedicated server has no vanilla textures, and a client's resource pack would change the city. Rejected.
- **Computing colors in the Gradle build.** The build would need the 2.9.0 pack's jars, which CI does not have. Shipping the measured table keeps the build self-contained; the tool is rerun by hand when the catalog grows.
- **A smooth color field** (each chunk picks the palette nearest a noise-driven target tone). It feathers whole districts, but it removes variety where none is needed and needs a projection to keep the target reachable for every style near a boundary. The pairwise rule changes a pick only where a real neighbor would clash.
- **Full CIELAB ΔE.** It counts a hue change between two pale colors as heavily as pale against dark-saturated, and judges sandstone beside gray stone brick worse than sandstone beside light gray terracotta. The owner's rule is about lightness and saturation together.
- **Changing the building, not the palette.** Upstream draws the building in the characteristics stage, which many other decisions read; changing it would move streets and doors. Fixed-facade buildings choose first instead, and their neighbors adapt.

## Still open

- The threshold is set from measured pairs, not yet from the owner's eye; the beta review may move `facadeContrast`.
- Two fixed-facade buildings side by side can still clash; none of the bundled buildings does today. M11's catalog review should check new fixed-facade buildings against the rule.
