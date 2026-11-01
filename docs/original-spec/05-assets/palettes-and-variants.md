# Palettes, variants and block resolution

A palette maps single characters to blocks. A variant is a named weighted block list that palettes can share. This sheet covers both formats, how a block string becomes a block state, how palettes are combined and compiled for a chunk, random lists, damaged blocks, and the "information" characters (loot, spawners, torches, block-entity NBT). Which palettes a chunk uses is decided by its style ([styles-and-city-styles.md](styles-and-city-styles.md)).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `regassets/PaletteRE`, `regassets/data/PaletteEntry`, `regassets/data/BlockEntry`, `regassets/VariantRE` (raw); `cityassets/Palette` (parsed palette with `PE` entries and `Info` records), `cityassets/Variant`, `cityassets/CompiledPalette` (the per-chunk lookup structure); `varia/Tools.stringToState`; `Style.getRandomPalette`; `BuildingInfo.getCompiledPalette`; `LostCityTerrainFeature.computePalette`. |
| 2 | **Inputs** | Palette and variant assets; local palettes inside buildings and parts; the live block registry; for property strings, the server's overworld block lookup; for variants inside palettes, the running server's overworld registry access. |
| 3 | **Numbers** | Random lists always have exactly **128** slots. Weights are integers counted in slots out of 128, filled in list order; the last entry is conventionally given a large weight (1000 or 2000) to fill the rest. Characters are one Java `char`. |
| 4 | **Behavior** | See [Block strings](#block-strings), [Parsing a palette](#parsing-a-palette), [Compiling](#compiling-a-palette) and [Lookups](#lookups). |
| 5 | **Determinism** | Parsing and compiling use no randomness. A random-list lookup uses either the caller's `Random` (`get(c, rand)`) or the per-chunk palette LCG (`get(c)`, see [parts-and-placement.md](parts-and-placement.md)). Which palettes are merged for a chunk is chosen by the style with the building random (`BuildingInfo.getBuildingRandom`, seeded `seed + chunkZ·341873128712 + chunkX·132897987541`), after the draws that precede it in `BuildingInfo` (`04-city-layout`). Compiled maps are `HashMap`s, but nothing iterates them in a way that changes output except `find` (editor only). |
| 6 | **Data formats** | [Palette JSON](#palette-json), [Variant JSON](#variant-json). |
| 7 | **Output** | A `CompiledPalette` per chunk: char → block state or 128-slot array, damaged-block map, char → information. Used by part placement, streets, ruins, rubble, stuff, corridors and spheres. |
| 8 | **Relationships** | Styles pick the palettes; buildings and parts add local palettes; [conditions.md](conditions.md) resolves `loot` and `mob`; `DamageArea` and ruins (`03-terrain`) read damaged blocks; `08-loot-mobs-blocks` owns the tags and loot tables. |
| 9 | **1.20 dependencies** | `BlockStateParser`, block-state properties, `BlockStateData.upgradeBlock` (legacy name upgrade), Forge's defaulted block registry, `CompoundTag.CODEC` for `tag`, `ServerLifecycleHooks`. See the [index table](index.md#120-dependencies). |
| 10 | **Bugs** | [ASSET-02](index.md#suspected-bugs), [ASSET-03](index.md#suspected-bugs), [ASSET-08](index.md#suspected-bugs), [ASSET-11](index.md#suspected-bugs), [ASSET-12](index.md#suspected-bugs), [ASSET-13](index.md#suspected-bugs), [ASSET-14](index.md#suspected-bugs), [ASSET-20](index.md#suspected-bugs), [ASSET-23](index.md#suspected-bugs), [ASSET-30](index.md#suspected-bugs). |

## Palette JSON

A palette file is `{"palette": [ entry, ... ]}`. The same object is used inline as the `palette` field of a building or part.

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `palette` | list of entries | required | The characters | none; an empty list is legal |

Each entry:

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `char` | string | required | The character; only the first char is used | empty string crashes (`charAt(0)`) ([ASSET-23](index.md#suspected-bugs)); duplicates in one palette: last wins |
| `block` | string | none | One block ([Block strings](#block-strings)) | resolved at compile time; failure aborts compilation |
| `variant` | string | none | Name of a variant asset; the character becomes that variant's random list | `getOrThrow`; needs a running server ([ASSET-14](index.md#suspected-bugs)) |
| `frompalette` | string | none | Another character; this character takes whatever that character resolves to in the compiled palette | only the first char of the value is used |
| `blocks` | list of `{random, block}` | none | An inline random list ([Random lists](#random-lists)) | an empty list counts as absent |
| `damaged` | string | none | Block that the entry's block(s) turn into when damaged by explosions or ruins | resolved at compile time |
| `mob` | string | none | Name of a [condition](conditions.md) that picks the spawner's mob | resolved at generation time |
| `loot` | string | none | Name of a condition that picks the chest's loot table | resolved at generation time |
| `torch` | boolean | false | The block is a torch to be re-oriented after generation | none |
| `tag` | NBT compound (JSON object) | none | Block-entity NBT to attach | an empty compound counts as absent |

Exactly one of `block`, `variant`, `frompalette`, `blocks` is used, checked in that order; an entry with none throws `RuntimeException("Illegal palette <id>!")`. `mob`, `loot`, `torch` and `tag` may be combined with any of them.

Example:

```json
{ "palette": [
  { "char": "C", "block": "minecraft:chest[facing=north]", "loot": "chestloot" },
  { "char": "#", "variant": "stonebrick", "damaged": "minecraft:iron_bars" },
  { "char": "@", "frompalette": "a" },
  { "char": "%", "blocks": [ { "random": 40, "block": "minecraft:cobweb" },
                             { "random": 1000, "block": "minecraft:air" } ] } ] }
```

## Variant JSON

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `blocks` | list of `{random, block}` | required | The weighted list, same rules as an inline `blocks` list | blocks resolved when the variant is compiled |

`random` (int, required) is the number of slots; `block` (string, required) is a block string. The comment on `BlockEntry` says "relative to 128".

## Block strings

`Tools.stringToState(s)` (server):

1. If `s` contains `[`, parse it with vanilla `BlockStateParser` against the overworld's block lookup (`WorldTools.getOverworld()`): `namespace:id[prop=value,...]`. Properties not listed keep their default. A parse error is rethrown as `RuntimeException`. NBT (`{...}`) is not allowed (`parseForBlock(..., false)`).
2. Otherwise pass `s` through `BlockStateData.upgradeBlock` (vanilla's table of pre-1.13 block names), then look the result up in `ForgeRegistries.BLOCKS` and return the block's default state. The "Cannot find block" error is meant to fire for unknown names, but Forge's block registry is a defaulted registry that answers unknown names with `minecraft:air`, so a typo silently becomes air (inferred from Forge's registry behavior, [ASSET-13](index.md#suspected-bugs)).
3. The 1.12 `name@meta` syntax is not supported; `@` is not a legal id character and throws ([ASSET-03](index.md#suspected-bugs)).

Default states matter: `minecraft:brick_stairs` without properties means `facing=north, half=bottom, shape=straight` (the shape is then recomputed from neighbors by the chunk driver).

## Random lists

A random list (`blocks` or a variant) becomes a `BlockState[128]` in `CompiledPalette`:

1. Walk the entries in order; write each entry's block into the next `random` slots, stopping at 128.
2. If fewer than 128 slots are filled, compilation throws "Not enough blocks in the random list (factor should go up to 128)".
3. Entries past slot 128 are silently dropped.

So `[32 A, 32 B, 1000 C]` is 32/128 A, 32/128 B, 64/128 C. A lookup draws a slot index in `0..127`.

## Parsing a palette

`Palette(PaletteRE)` (server), for each entry in order:

1. `c` = first char of `char`.
2. If `damaged` is set, resolve it to a state `dmg`.
3. Build `Info(mobId, loot, isTorch, tag)` from `mob`, `loot`, `torch` (null → false), `tag`.
4. Then by kind:
   - `block`: resolve; map `c` → that state; if `dmg`, record damaged[state] = dmg.
   - `variant`: fetch the variant from the running server's overworld registries; if `dmg`, record damaged[s] = dmg for every state `s` of the variant; map `c` → the variant's (weight, state) pairs.
   - `frompalette`: map `c` → the string (resolved later).
   - `blocks`: resolve every block; record damaged for each; map `c` → the pairs.
5. Each mapping is a `PE(blocks, info)` where `blocks` is a state, an array of pairs or a string.

The damaged map is keyed by block state, not by character, and is shared by the whole palette, so any character whose block is (for example) `cyan_terracotta` gets the damaged block recorded by whichever entry with that state came last ([ASSET-30](index.md#suspected-bugs)).

`Palette.merge(other)` copies all of `other`'s characters and damaged entries over this palette's (other wins).

Local palettes of buildings and parts are parsed into palettes named `lostcities:__local__<asset path>`. The palette built by a style is named `lostcities:__random__`.

## Compiling a palette

The chunk's palette is built in up to three layers (server):

1. **Style layer.** `Style.getRandomPalette(provider, random)` starts from an empty palette and, for each group in the style's `randompalettes`, picks one palette by weight and merges it in. Later groups override earlier ones character by character. (Details: [styles-and-city-styles.md](styles-and-city-styles.md#style).)
2. `BuildingInfo.getCompiledPalette()` compiles that palette (`new CompiledPalette(stylePalette)`). If the chunk has a building with a local or referenced palette, it compiles that palette on top (`new CompiledPalette(base, buildingPalette)`). The result is cached on the `BuildingInfo`.
3. **Part layer.** `computePalette` compiles the part's local or referenced palette on top for each part placement (not cached).

`new CompiledPalette(base, palettes...)` copies `base`'s three maps and then runs the add step. `addPalettes(palettes)`:

1. **Direct entries.** For each palette, for each character: a state is stored as is; an array of pairs becomes a 128-slot array ([Random lists](#random-lists)); a string (`frompalette`) is skipped for now; a null mapping throws "Invalid palette entry". For every character seen (including skipped ones), its information is removed.
2. **References.** Repeat until nothing changes: for each `frompalette` character whose target character is already in the compiled map and which is not itself in the map yet, copy the target's value (the same state or the same array) and remove its information. A reference whose target never appears is left undefined. A reference cannot replace a character that already has a value, including one inherited from `base` ([ASSET-11](index.md#suspected-bugs)).
3. **Damage and information.** Copy every palette's damaged map into the compiled one. For every character of every palette whose `Info` is special (any of mob, loot, torch, tag set), store the information. With several palettes in one call, information from an earlier palette survives a later plain redefinition of the same character ([ASSET-12](index.md#suspected-bugs)); every call in the pinned code passes one palette, so this is latent.

## Lookups

| Method | Behavior |
|---|---|
| `get(c)` | The state; for a random character, slot `nextPaletteIndex()`; null if undefined. Other errors are rethrown. |
| `get(c, random)` | The same, with slot `random.nextInt(128)`; any error is logged and null returned. |
| `getAll(c)` | The set of possible states: singleton, empty, or `Set.of(array)`, which throws on the array's duplicate entries, so it crashes for any random character ([ASSET-02](index.md#suspected-bugs)). Used for the city style's `ironbars` and `rubbledirt` characters. |
| `isDefined(c)` | The character has a value. |
| `isSimple(c)` | Tests whether the value is a `Character`, which never happens; always false (dead code). |
| `canBeDamagedToIronBars(state)` | The damaged block for a state, or null. Used by `DamageArea` and ruins. |
| `getInfo(c)` | The information record, or null. |
| `getCharacters()` | All defined characters. |
| `find(state)`, `isMatch(c, state)` | Editor helpers: reverse lookup by identity, and "same block" test; `isMatch` throws on an undefined character. |

## Information characters

| Palette field | Effect at placement ([parts-and-placement.md](parts-and-placement.md#part-placement)) | Profile switch |
|---|---|---|
| `torch: true` | Queued for `fixTorches`, which picks standing or wall torch from the neighbors | `GENERATE_LIGHTING` (off: air) |
| `loot` | Container filled from the loot table chosen by the named condition | `GENERATE_LOOT`, `CHEST_WITHOUT_LOOT_CHANCE`; building `noLoot` |
| `mob` | Spawner with the mob chosen by the named condition | `GENERATE_SPAWNERS` (off: air); building `noLoot` (air) |
| `tag` | NBT attached to the block entity | none |

Only the first applicable one is used, in the order torch, loot, mob, tag.

## Special blocks

| Block | Meaning in a part |
|---|---|
| `minecraft:air` (bundled char ` `) | Leave the existing block unchanged |
| `minecraft:structure_void` (bundled `~` and `b`) | "Hard air": air, water below the water level, or keep, by the part's hard-air setting |
| the profile's liquid (bundled `W` = `minecraft:water`) | Liquid, or air with `AVOID_WATER` |

## On 1.12

`upstream/1.12` palettes have the same entry fields plus a `facing` object for torch orientation, use `"name"` and `"type": "palette"` inside array files, and write blocks as `modid:name` or `modid:name@meta` (`Tools.stringToState` splits on `@` and uses `getStateFromMeta`). That `@meta` form maps directly onto 1.7.10 block + metadata. It also ships `palette_chisel.json` and `palette_chisel_desert.json`, Chisel-based palettes that may help the GTNH block mapping.
