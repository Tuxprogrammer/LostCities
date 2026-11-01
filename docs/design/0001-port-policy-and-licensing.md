# 0001 · Port policy and licensing

- Status: Accepted
- Gate: none (foundation)
- Amends: none
- Date: 2026-11-01

## Context

Lost Cities: New Horizons backports McJty's Lost Cities to Minecraft 1.7.10 for GT New Horizons. Unlike the originals behind GregGuns (Techguns, All Rights Reserved) and TrainHorizons (Traincraft 1.7, custom license), Lost Cities is **MIT** on every upstream branch. The baseline is branch `1.20` at commit `f18a094d` (mod version 1.20-7.5.7, 2026-10-04) of [McJtyMods/LostCities](https://github.com/McJtyMods/LostCities). The older branches (`1.12`, `1.16`, `1.18`) are MIT too.

The owner's standing rule for ported code, from GregGuns' ICBM work, is that a port does not come in until it meets the quality bar of native code. Cloning for speed is how bad code ships. The goal is "faithful to the functionality, not to the bugs".

## Decision

1. **The baseline is upstream `1.20` at `f18a094d`.** The behavior spec ([docs/original-spec/](../original-spec/README.md)) documents that commit. Later upstream commits come in only through a deliberate re-pin: update the local `1.20` branch, diff it against the pin, and update the spec and this record's commit.
2. **Upstream code may be ported, under the ported-code gate** ([CONTRIBUTING § Ported code](../../CONTRIBUTING.md#ported-code)). A port is a rewrite that starts from McJty's logic:
   - it follows our package layout, naming and idioms, and the Java 8 API;
   - it carries Javadoc and `// Spec:` comments as native code does;
   - unit tests cover the spec's numbers;
   - every upstream bug in the ported logic is fixed, with a regression test per bug-register ID.

   It is never a copy made for speed.
3. **Provenance on every ported file.** A header comment names the upstream file at the pinned commit (`Ported from Lost Cities (MIT): <permalink>`), then one `Avoids: <ID>` line per bug the port fixes.
4. **The notice.** McJty's copyright line is kept in [LICENSE](../../LICENSE) and in the jar's copy. [THIRD-PARTY-NOTICES.md](../../THIRD-PARTY-NOTICES.md) names the upstream repository, the pinned commit and the license.
5. **Upstream data assets are reused.** The JSON asset system (buildings, parts, palettes, styles, city styles, multibuildings, variants, conditions, scattered structures, world styles, profiles) is MIT. It may be carried over and converted to 1.7.10 block names and metadata. A converted asset keeps a provenance line in its folder's README and a THIRD-PARTY-NOTICES entry. Upstream's textures are not carried over: all 18 are in-game screenshots that show Minecraft's own textures, which are Mojang's to license, not McJty's ([09-client-ui: lang and textures](../original-spec/09-client-ui/lang-and-textures.md)). Any image the port needs is made anew.
6. **Older upstream branches are references, not baselines.** `1.12` is much closer to 1.7.10's APIs (chunk providers, block metadata, world types). Its code may be read, and ported under the same gate, wherever it solves a 1.7.10 problem that `1.20` solves with newer APIs. Cite it with a permalink pinned to its commit.
7. **Our own work is MIT**, copyright Spencer Callicott, as is any LGPL work rebuilt from a description. Other third-party material follows [CONTRIBUTING § Assets](../../CONTRIBUTING.md#assets-and-localization): CC0, CC-BY, MIT and LGPL are acceptable, each with its license and a notices entry. Nothing NC, ND, All Rights Reserved or unlicensed is acceptable.

## Consequences

- Builders may read upstream code while building; clean-room separation is not required. The spec is still the behavior contract: tests check the spec's numbers, and the bug register says what must not be reproduced.
- `PortedCodeGateTest` (roadmap M01) checks the header, the notice, the Javadoc, the test class and the bug IDs on every ported file, so CI fails a port that misses any of them.
- Deviations from upstream behavior are recorded in [docs/new-spec/](../new-spec/README.md) and in design records, as for the other mods.

## Alternatives considered

- **Spec-first clean room**, as in GregGuns and TrainHorizons. The license does not require it, and rebuilding MIT logic blind costs time without a quality gain.
- **Straight port, refactor later.** Rejected under the owner's rule: ported code is not committed until it meets the bar.

## Still open

None.
