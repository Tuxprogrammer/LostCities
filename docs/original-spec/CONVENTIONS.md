# Spec conventions

The standard every area of this spec meets, so the city planner, a palette and a command are each documented to the same depth. The underlying rules are in the `behavior-spec` skill. This file adapts them to Lost Cities, which is MIT and may be ported ([0001](../design/0001-port-policy-and-licensing.md), decision log D2).

## What is documented

- **Version:** Lost Cities `1.20` at `f18a094d` (mod version 1.20-7.5.7, Minecraft 1.20.1, Forge 47). Source paths are relative to `src/main/java/mcjty/lostcities/` or `src/main/resources/` on that commit.
- **Older branches:** where `1.12` solves the same problem with APIs closer to 1.7.10, the sheet adds a short **"On 1.12"** note: what exists there, file paths, and whether it behaves differently.

## Ground rules

- **Behavior, numbers, formats and bugs; not a code transcript.** The code is portable, so the spec doesn't restate it line by line. It records what the code must achieve, so tests can check a port:
  - the algorithm as ordered steps;
  - every constant and default;
  - every random choice and what seeds it;
  - every asset field;
  - every edge case.
- **Units always:** blocks, chunks (16 blocks), block-Y levels, ticks (20 per second), percentages and probabilities stated as the code uses them (0–1 or 0–100).
- **Label sides.** Every step says (client), (server) or (both). Most of world generation is (server).
- **Determinism is part of the behavior.** For every random choice, say which `Random` it uses and how that is seeded (world seed, chunk coordinates, a salt), and whether results depend on generation order.
- **1.20 dependencies are called out.** Where behavior rests on a 1.20 mechanism with no direct 1.7.10 counterpart (the world height of -64 to 320, codecs, data-pack reload, biome tags, noise router, block states, structure placement), say so and add the row to `API-MAP.md`. Don't design the 1.7.10 answer here; that belongs in the new spec.
- **Say "none" explicitly** for fields that do not apply.
- **Unreachable content is documented** and marked *unused*, *dead code* or *config-only*.
- **Inferred is not observed.** Where a sheet describes how something looks in the world without having generated it, it says *inferred*.
- **Only add.** Expanding one area never trims another. Correct only what is wrong and note it in the area's coverage section.
- **Cross-references** use area folder names (`04-city-layout`) or relative links. Write class names, asset names, JSON fields, config keys and bug IDs in backticks.

## System sheet

Every documented system gets a sheet with these fields in this order, for example the city rarity map, the street planner, the palette compiler, the highway planner, the profile, or one command. Template: [docs/templates/spec-sheet.md](../templates/spec-sheet.md), with the fields below in place of the entity fields.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | Classes and their roles; the asset types, config keys and profile options involved |
| 2 | **Inputs** | What it reads: seed, chunk coordinates, profile options, assets, biomes, neighbors, saved data |
| 3 | **Numbers** | Every constant, default and range, with units and the option or asset field that changes it |
| 4 | **Behavior** | The algorithm as ordered steps, each labeled with its side, including every edge case and fallback |
| 5 | **Determinism** | Each random source, its seed, and any dependence on order, caches or shared state |
| 6 | **Data formats** | Every JSON field it reads (name, type, default, meaning), and every NBT or saved-data key it writes |
| 7 | **Output** | What ends up in the world, or what it hands to the next stage |
| 8 | **Relationships** | What calls it and what it calls, linked across areas |
| 9 | **1.20 dependencies** | Mechanisms with no direct 1.7.10 counterpart, each also listed in `API-MAP.md` |
| 10 | **Bugs** | Inline bug IDs (`CITY-12`) for everything suspicious |

Small related systems may share a sheet. Asset catalogs (every bundled building, part family or palette, with its numbers) are tables that sit alongside sheets, never in place of them.

## Asset entries

For the bundled assets in `05-assets`:

- **Building or multibuilding:** name, floors above and below ground, parts per floor, chance and min/max settings, the city styles that use it, its file.
- **Part family:** name pattern, footprint (x × z × height), what it depicts in words, the palette characters it uses, its variants.
- **Palette:** each character, what it maps to (block and properties, or a variant), damaged and loot behavior, inheritance.
- **Texture (if any):** path, size, frames, what it depicts, what uses it.

## Lang

Each area lists every lang key in its scope with its exact English text, strings built in code with the condition that produces them, and any missing keys or hard-coded English.

## Area index

Every `index.md` follows [docs/templates/spec-area-index.md](../templates/spec-area-index.md): Overview, Contents, Key facts, Related areas, Coverage (files read out of files in scope; assets and lang keys covered out of those in scope), Known limits, Suspected bugs.

## Bug IDs

| Area | Prefix |
|---|---|
| 01-core | `CORE` |
| 02-profiles-dimensions | `PROF` |
| 03-terrain | `TERR` |
| 04-city-layout | `CITY` |
| 05-assets | `ASSET` |
| 06-highways-railways | `HWY` |
| 07-scattered | `SCAT` |
| 08-loot-mobs-blocks | `LOOT` |
| 09-client-ui | `UI` |

Columns: `ID | Title | Location | What looks wrong | Severity` (crash, worldgen, gameplay, performance, visual, cosmetic). Locations are `path/Class.java:line` on `f18a094d`, or the asset path. Suspected, not confirmed; no fixes designed in the spec. Upstream GitHub issues that confirm a bug are linked in its row. A finding later checked and found correct keeps its ID and row: its severity becomes "not a bug", its row gives the evidence, and BUGS.md moves it to [Not bugs](BUGS.md#not-bugs), out of the counts.
