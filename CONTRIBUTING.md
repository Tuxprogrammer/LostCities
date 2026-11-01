# Contributing to Lost Cities: New Horizons

Lost Cities: New Horizons backports McJty's Lost Cities to **GT New Horizons (Minecraft 1.7.10)**. It is faithful to the original's functionality, not to its bugs. These rules apply to every change, whether a person or an AI agent makes it. They exist so that the mod feels like part of GTNH rather than an add-on bolted to it, and so that the code stays readable, tested and maintainable.

> [!IMPORTANT]
> These rules use **must** for hard rules and **should** for strong defaults. A change that breaks a *must* does not land. To depart from a *should*, say why in the commit message or pull request.

## Contents

1. [Workflow](#workflow)
2. [Ported code](#ported-code)
3. [Toolchain and build](#toolchain-and-build)
4. [Architecture](#architecture)
5. [World generation rules](#world-generation-rules)
6. [GTNH integration](#gtnh-integration)
7. [Code standards](#code-standards)
8. [Comments and documentation](#comments-and-documentation)
9. [Testing](#testing)
10. [Assets and localization](#assets-and-localization)
11. [Commits and landing](#commits-and-landing)
12. [Definition of done](#definition-of-done)

---

## Workflow

1. **Start from the roadmap.** Every change belongs to a milestone, spike or decision gate in [docs/ROADMAP.md](docs/ROADMAP.md).
   - Don't start a milestone whose entry gate isn't met.
   - Don't build anything a decision gate blocks until its record in [docs/design/](docs/design/README.md) is `Accepted`.
2. **Read the spec.** Before building something, read its sheets in two places:
   - [docs/original-spec/](docs/original-spec/README.md) for how upstream behaves. Start from the area's `index.md`.
   - [docs/new-spec/](docs/new-spec/README.md) for what changes and how it plugs into the pack.
3. **Check the bug register.** Read the feature's rows in [BUGS.md](docs/original-spec/BUGS.md). Upstream bugs **must not** be reproduced, and where it's practical, a test proves that.
4. **Record deviations.** When this mod deliberately behaves differently from upstream (a 1.7.10 limitation, a pack block in place of a 1.20 block, a bug fix, a GTNH integration), record it in the new-spec area and, if it is a design choice, in a design record. For a small deviation, a `// Deviation:` comment that cites the spec section is enough (see [Comments](#comments-and-documentation)).
5. **Keep changes focused.** One logical change per commit. Parallel work happens on branches in worktrees ([Commits and landing](#commits-and-landing)).

## Ported code

Upstream Lost Cities is MIT ([0001](docs/design/0001-port-policy-and-licensing.md)), so its code may be ported. A port is a rewrite that starts from McJty's logic, never a copy made for speed. Ported code goes through this gate before it lands (**must**):

- **Pinned source.** Port from the baseline commit (`1.20` at `f18a094d`), or from an older MIT branch (`1.12`) where its era fits 1.7.10 better. Cite the exact commit.
- **A header on every ported file**, directly under the package declaration:

  ```java
  // Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/City.java
  // Avoids: CITY-03, CITY-07
  ```

  Omit the `Avoids:` line only when the spec lists no bug in the ported logic.
- **The notice.** [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) keeps the upstream MIT notice. The `LICENSE` file keeps McJty's copyright line.
- **Our style, not theirs.**
  - Our package layout, naming, idioms and the Java 8 API.
  - Javadoc on every class and every non-trivial public method, and spec-traceability comments, as for native code ([Comments](#comments-and-documentation)).
  - 1.20 abstractions with no 1.7.10 counterpart (codecs, holders, registries, data-pack reload) become plain Java. They are not emulated.
- **Tests.** Unit tests cover the spec's numbers, and each bug on the `Avoids:` line has a regression test.
- **Every known bug is fixed.** No bug-register entry in the ported logic survives.

Other third-party code: MIT is preferred, and CC0, CC-BY and LGPL are acceptable. LGPL logic is preferably described in the spec in our own words and rebuilt; the result is ours and MIT. LGPL code adapted from its source, rather than rebuilt from a description, stays LGPL. It lives in `com.tuxprogrammer.lostcities.thirdparty.<project>` with its license text and a notices entry, and passes the same gate. Never unlicensed, NC or All Rights Reserved code.

`PortedCodeGateTest` checks the header, the notice, the Javadoc, the test class and the bug IDs automatically, so CI fails a port that misses any of them. Review checks the rest.

## Toolchain and build

| Item | Rule |
|---|---|
| JDK | 25, as in [`.java-version`](.java-version). Gradle provisions it. |
| Language level | Modern **syntax** only. `enableModernJavaSyntax = jabel` compiles to **Java 8 bytecode, running on the Java 8 API**. Allowed: `var`, switch expressions, text blocks, `instanceof` patterns. **Not allowed:** records, sealed types, and any API newer than Java 8 (`List.of`, `Map.of`, `Optional.isEmpty`, `String.isBlank`, `Stream.toList`…). If unsure, check the Java 8 Javadoc. |
| Formatting | Spotless, using the GTNH shared Eclipse config. Run `./gradlew spotlessApply` before every commit; CI rejects unformatted code. |
| Static checks | Checkstyle: no wildcard imports. The static-check tests ([Testing](#testing)) run with the unit tests. |
| Build files | **Never edit** `build.gradle.kts` or `settings.gradle.kts`, which come from the GTNH buildscript. Put dependencies in [`dependencies.gradle`](dependencies.gradle), repositories in [`repositories.gradle`](repositories.gradle), and mod settings in [`gradle.properties`](gradle.properties). |
| Dependencies | GTNH Maven coordinates with the `:dev` classifier, pinned to the versions GTNH 2.9.0 ships ([0002](docs/design/0002-target-pack.md)). Use `compileOnly` for optional integrations, and `devOnlyNonPublishable` for things only needed at runtime in development. Never shade a mod. |

Commands:

```sh
./gradlew spotlessApply build   # format, compile, test, package
./gradlew test                  # unit and static-check tests only
./gradlew runServer             # dev dedicated server (boot smoke test, worldgen scenarios)
./gradlew runClient             # dev client
```

## Architecture

Root package: `com.tuxprogrammer.lostcities`. Target layout:

| Package | Contents |
|---|---|
| `lostcities` | `@Mod` entry class, proxies, `Tags` (generated) |
| `lostcities.api` | Stable interfaces other mods may use, kept minimal and documented |
| `lostcities.core` | Module system, registration helpers, logging, the dev self-check |
| `lostcities.config` | Mod config (GTNHLib) and the profile system |
| `lostcities.asset` | The JSON asset system: loading, validation and the in-memory model of buildings, parts, palettes and styles. Pure Java |
| `lostcities.layout` | City, street, building, highway and railway layout: which chunk holds what. Pure Java, no Minecraft types |
| `lostcities.worldgen` | Minecraft glue: world type or chunk provider hooks, terrain, writing parts into chunks, population |
| `lostcities.block`, `lostcities.item` | Blocks and items this mod owns, if any |
| `lostcities.command` | Operator and debug commands |
| `lostcities.network` | Channel, packet base classes, packet types |
| `lostcities.compat` | Optional integrations, one package per mod, each guarded |
| `lostcities.scenario` | The dev-only in-world test harness |
| `lostcities.client` | **All** client-only code: GUIs, the world-creation screen, renderers |

The layout is a starting point. Milestones refine it in design records, and they never move code across the client, pure-Java and glue boundaries.

Rules:

- **Side safety (must).** Client-only classes live under `client`, and common code never references them, even through imports in code paths that load on the server. Reach the client through the proxy or `@SideOnly` methods. `ClientIsolationTest` enforces this.
- **Logic and Minecraft glue are separate (must).** City detection, street and building selection, highway and railway routing, palette resolution, part transforms and the asset model live in `asset` and `layout`. Those classes take seeds, coordinates, profiles and assets, and return plain data; no `World`, `Block` or `Chunk` appears in their signatures. `worldgen` stays thin: it asks the layout what goes in a chunk, and writes it through a chunk-buffer interface that tests can back with an array. `LayoutPurityTest` enforces the package rule.
- **Modules.** Each feature area (cities, highways, railways, scattered structures, and so on) has a config flag. A flag never changes what is registered: content always exists, so client/server flag differences and flipped flags cannot break joining or delete blocks. A flag switches generation and behavior off.
- **No mutable statics** except registries, config and bounded caches that the world lifecycle owns. Per-world state lives in world saved data, never in static maps keyed by nothing.
- **Registration order** follows the FML lifecycle:
  - `preInit`: config, blocks and items, network;
  - `init`: world type and generator hooks, event handlers, client-side setup;
  - `postInit`: asset loading against the full block registry, cross-mod hooks, the dev self-check.

## World generation rules

This mod is world generation, so these are the rules that matter most (**must** unless marked).

- **Deterministic.** What a chunk contains is a pure function of the world seed, the chunk coordinates, the active profile and the loaded assets.
  - No wall-clock time and no unseeded `Random`.
  - Never iterate a `HashMap` or `HashSet` where the order affects output.
  - No dependence on the order chunks are generated in.
  - Every random choice uses a `Random` seeded from the world seed and the coordinates it decides for.
  - `DeterminismTest` and the golden-chunk tests ([Testing](#testing)) prove it.
- **No cascading generation.** Generating or populating a chunk never loads, generates or writes into a chunk that isn't ready. Population follows 1.7.10's +8 offset convention. A structure that spans chunks is written piece by piece, each chunk writing only its own share from the shared layout. The headless worldgen scenario fails on any cascading-worldgen warning from Hodgepodge.
- **Existing worlds stay valid.** Layout decisions never change for a given seed, profile and asset set without a recorded deviation, so already-generated chunks keep matching new ones at the border. A layout change is a deliberate, documented break.
- **Performance budgets.** Generation has a per-chunk time budget, set by a design record and checked by the worldgen scenario. Caches are bounded and owned by the world. There is no per-block allocation in the inner write loop.
- **Blocks by name.** Assets name blocks as `modid:name` plus metadata, resolved once at load against the live registry. There are no numeric block IDs anywhere. A palette entry whose mod is absent falls back to a documented substitute, and the dev self-check reports it.
- **Dimensions** are config-gated, with the overworld the only default. Nothing generates where the config says it doesn't.

## GTNH integration

The mod must feel native to GTNH. These are the coupling rules; [docs/new-spec/](docs/new-spec/README.md) decides what each one means for each feature.

> [!NOTE]
> GT5-Unofficial renamed its API classes in 2024 (for example `GT_Values` became `GTValues`). **Always check names against the GT5-Unofficial version pinned in `dependencies.gradle`.**

**Researching external APIs (must)**
- These rules cover code that touches GT, RWG, Chisel, NEI or any other pack mod. The extracted pack and its mod index live in the git-ignored `reference/gtnh/2.9.0/` ([0002](docs/design/0002-target-pack.md)).
- **Source first.** Read the mod's source at the tag the pack ships.
- **Decompile only as a fallback**, for mods or internals with no available source. Never paste decompiled code.
- **Cite it.**
  - Put a `// Source:` permalink, pinned to the tag (or the commit), where code relies on another mod's behavior.
  - Put a `// Decompiled:` hedge comment wherever decompiled bytecode was the evidence.

**Blocks and materials**
- Prefer the pack's own blocks in palettes: vanilla, then pack decorative mods (Chisel, ChiselTones, Ztones, Et Futurum Requiem, Carpenter's Blocks, BiblioCraft), as the new spec decides. Register a block of our own only when the pack has nothing that fits, and say why in a design record.
- Never register an ore, ingot, plate or fluid that GT provides.

**The overworld, RWG and ores**
- Cities generate in the GTNH overworld, as an overlay on RWG2 Continents and classic RWG terrain ([0003](docs/design/0003-world-presence.md)). They never require a different world type.
- No ore generation; the GTNH ore system owns ores.
- How cities coexist with GT ore veins, RWG terrain and other pack worldgen is settled in the new spec, never assumed.

**Loot, spawners and items**
- Chest loot and spawners follow the pack's conventions, for example vanilla spawners with NBT rather than invented spawner blocks.
- Nothing generated in the overworld is above **EV**, and the distribution is weighted toward the lower tiers ([0004](docs/design/0004-gtnh-layer-and-tier-ceiling.md)). Balance within that ceiling waits for the beta pass.
- Buildings stay ordinary, breakable blocks that players can move into and build on. Generation never places unbreakable or protected blocks.

**Recipes** (if the mod adds any)
- Machine processing uses GT recipe maps through the recipe builder, at the EU/t of the item's tier. Crafting-table recipes follow GTNH conventions.

**Config**
- Mod-level options use GTNHLib's annotation config. Every option has a comment, a default and a range where relevant, and is documented.
- World options a server owner tunes are server-side and synced. They can't be changed in a way that breaks a running world (no footguns).

**Mixins**
- Only when no event or API exists. Use UniMixins in the GTNH style, through `usesMixins` in `gradle.properties`.
- No ASM coremods.
- Each mixin has a comment saying why no event works.

**Optional mods**
- Guard with `Loader.isModLoaded` or `@Optional`, and keep the code under `compat`.
- Hard dependencies are declared in `@Mod(dependencies=…)` and `mcmod.info`.

**Pack fit**
- Names, tooltips and textures follow GTNH style.
- Quest-book content ships in the jar behind a config key, never as a pack pull request (`in-pack-content-delivery`).
- Before adding a mechanic, check that the pack doesn't already provide it.

## Code standards

- **Readable first.** Use clear names: no abbreviations beyond common Minecraft ones (`te`, `nbt`, `pos`). Keep methods small, prefer early returns, and use one class per concept.
- **No magic numbers.** Use named constants, or config for anything a server owner might tune. Spec numbers become constants named after what they mean, with the spec section in their Javadoc.
- **Validate everything the client sends (must).** Packet handlers check that the player may act (operator status for world tools, same dimension, within reach), and check every index and enum against its bounds.
- **Server authority.** The server decides world content. The client never mutates world state.
- **No swallowed exceptions.** Never `catch (Exception e) {}`. Catch the narrowest type, then handle it or log it with the mod logger. Never use `System.out`.
- **Fail loudly on bad assets.** A malformed or unresolvable asset is reported with its file and field at load, and the dev self-check fails. At runtime in a real pack, the asset is skipped with one clear log line, never a crash mid-generation.
- **Null safety.** Check stacks, NBT and registry lookups. Use one helper rather than repeated checks.
- **NBT and saved data.** Keys are constants. Readers tolerate missing keys with sensible defaults.
- **Thread discipline.** Don't touch world state from network threads.

## Comments and documentation

- **Javadoc (must)** on every public class and every public method that isn't trivial. Say what it does, what each parameter means, its units (blocks, chunks, ticks, block-Y levels) and its side (client, server or both).
- **Comments explain why**, not what. Explain constraints, 1.7.10 limitations, pack conventions and non-obvious math.
- **Spec traceability.** A class that implements a spec sheet cites it at the top: `// Spec: docs/original-spec/04-city-layout/city-spheres.md`.
- **Deviations.** When behavior intentionally differs from upstream, say so where it happens: `// Deviation: palettes resolve to Chisel blocks (new-spec 05-buildings).` or `// Avoids CITY-04: street edges no longer double-place.`
- **External behavior.** Where code depends on how another mod behaves, cite it with `// Source: <pinned permalink>`, or `// Decompiled: <jar, pack version, member>; inferred from decompiled bytecode and may be wrong`.
- **No commented-out code** in commits. Delete it; git keeps history.
- **Docs state the current truth.** Design records, specs and the roadmap say what is true now. When a decision changes, rewrite the text in place; git keeps the history. Player-facing text (README, guides, lang, tooltips, config comments) never tells players what changed between versions; that belongs only in the changelog.
- **Fix stale docs on sight.** A broken link, stale statement or wrong fact found in any doc is fixed in the change that found it, not left for later.

## Testing

Each layer catches different bugs, and features need every layer that applies to them. Testing guidance lives in the `testing-gtnh-mods` skill; these are the rules.

### 1. Unit tests: must

- **Framework:** JUnit 5 in `src/test/java`, mirroring the main package structure; run with `./gradlew test`.
- **What they cover:**
  - every formula and decision in `asset` and `layout`: city spheres and density, building and part selection, street and highway routing, palette resolution, part rotation and mirroring;
  - asset parsing and validation, including rejection of malformed files;
  - config and profile defaults, and NBT round-trips.
- **Determinism:** the same seed, coordinates, profile and assets give the same layout across runs and across generation orders.
- **Golden chunks:** for a fixed set of seeds, profiles and chunk coordinates, the chunk buffer's content hash matches a checked-in golden value. A golden value changes only in a change that records why.
- **Bug regressions:** each upstream bug a feature could reproduce gets a regression test named for it, for example `doesNotReproduceCITY04_streetEdgeDoublePlaced`.
- **Speed:** tests are deterministic (seeded randomness, no wall-clock time) and fast.

### 2. Static checks: must

Tests that read the compiled classes and resources and fail the build:
- `ClientIsolationTest`: no common class references a client class;
- `LayoutPurityTest`: no Minecraft type in `asset` or `layout`;
- `Java8ApiTest`: no API newer than Java 8;
- `PortedCodeGateTest`: the [ported-code gate](#ported-code);
- `LangKeysTest`: every player-visible string is a lang key;
- `AssetValidationTest`: every bundled asset parses, and every block it names is in the block-name map.

### 3. In-world tests: must, for anything that touches the world

- **Server boot smoke test:** `./gradlew runServer` starts with the pack dependencies loaded and no errors or warnings from this mod.
- **Dev self-check:** in the dev environment, a `postInit` check that every asset resolves, every palette block exists, and every lang key is present. A failure stops the dev server.
- **Worldgen scenarios:** `/lostcities test <scenario>` (op only, dev only) generates a fixed region on a fresh world with a given seed and profile, then checks:
  - no cascading worldgen;
  - the per-chunk time budget;
  - expected structures where the layout says they are;
  - an identical result on a second fresh world.

  Each feature adds its scenario, listed in `docs/testing/`. Run new worlds, not only ones that already hold the terrain.

### 4. Real-pack and visual checks: must, at the gates

- **Real pack:** the release jar on a GTNH 2.9.0 dedicated server and client, alongside RWG and GT ore generation, at the alpha and beta gates.
- **Visual:** `runClient` and real-pack screenshots of each feature's structures, including with shaders where rendering is involved. Visual changes keep their screenshots with the change's evidence.

Builds and Minecraft runs happen on the build server, not on a development laptop (decision log D4).

## Assets and localization

- **Provenance (must).** Every asset is upstream MIT material carried over ([0001](docs/design/0001-port-policy-and-licensing.md)), our own work, or third-party material under CC0, CC-BY, MIT or LGPL. Its source, author and license are recorded in `docs/assets/CREDITS.md`. Never anything NC, ND, share-alike-only or All Rights Reserved.
- **MIT for our work (must).** By contributing, you release your contribution under the MIT License.
- **Converted upstream assets** keep their upstream file name and structure where the format allows, so a later upstream re-pin can be diffed against them.
- **Minecraft content.** The rule is to never break [Mojang's usage guidelines](https://www.minecraft.net/en-us/usage-guidelines), not to avoid Mojang content. Mojang assets never ship in the jar, and nothing may look official.
- **Lang keys (must) for every player-visible string:** names, tooltips, chat, GUI text and command output.
  - `en_US.lang` is the source of truth.
  - No hard-coded English.
  - Use the key namespace `tile.lostcitiesnh.<name>`, `item.lostcitiesnh.<name>`, `lostcitiesnh.<area>.<key>`.
- **American English** in code, docs and lang (`color`, `armor`, `gray`). External identifiers keep their own spelling.

## Commits and landing

- **Commits.** Use an imperative subject (`Port city sphere selection`), a body explaining why, and one logical change per commit. Agents add their `Co-Authored-By` line.
- **Landing.** A change that meets the [definition of done](#definition-of-done) may be committed straight to `gtnh`. Parallel or long work happens on its own branch in a git worktree. It lands only after the default branch is merged into it and the result is verified, then it merges with `--no-ff` and the branch and worktree are removed (`port-workflow`).
- **Pushing.** `gtnh` is pushed at roadmap gates and when the owner asks. Release tags need the owner's go-ahead.
- **Nothing machine-specific** in tracked files or commit metadata: no hostnames, usernames, local paths, IPs or secrets (`release-and-public-repo`).
- **Record of a change:** the commit message, or the pull request when one is used, carries the [checklist](docs/templates/pull_request_template.md) items that apply: roadmap item, spec sheets, deviations, inherited bugs avoided, test evidence.

## Definition of done

A change is done when all of these hold:

- [ ] It implements its spec sheets, with any deviations recorded.
- [ ] `./gradlew spotlessApply build` passes, and unit tests cover the new logic and the relevant BUGS.md regressions.
- [ ] Static checks pass, and ported code passes the [ported-code gate](#ported-code).
- [ ] `runServer` boots cleanly, and the dev self-check passes.
- [ ] A worldgen scenario exists for world-touching features, and it passes on a fresh world, deterministically, within budget, with no cascading generation.
- [ ] Every string is a lang key, and every asset's provenance is recorded.
- [ ] Javadoc and spec-traceability comments are in place. There is no commented-out code.
- [ ] The roadmap box is ticked, and docs and the changelog are updated in the same change.
