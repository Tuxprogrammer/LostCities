# Commands on 1.7.10

The `/lostcities` command set of Lost Cities: New Horizons, subcommand by subcommand, against upstream's [commands.md](../../original-spec/01-core/commands.md) and [part-editor.md](../../original-spec/01-core/part-editor.md). The [01-core index](index.md#commands) summarizes it.

Status: the part-editor and file-writing commands are *Accepted* (decision log D13), as are the profile and timing commands ([0005](../../design/0005-rwg-overlay.md) §7, §8) and the scenario command ([CONTRIBUTING § Testing](../../../CONTRIBUTING.md#testing)). Which of upstream's other commands are ported, and the permission levels, are *Accepted* as recommended (P-CORE-2, decision log D31): every subcommand needs operator level 2; `debug`, `map`, `locate` and `createbuilding` are ported and `testfill` is dropped.

## Shape

- **One command**, `/lostcities` with the aliases `/lost` and `/lostcitiesnh`, a 1.7.10 `CommandBase` that dispatches on its first argument. Upstream's Brigadier argument types become strings parsed by the subcommand; block positions are three integers with vanilla's relative `~` syntax (`CommandBase.clamp_coord`). Tab completion offers subcommand names and, where upstream has suggestion providers, building and part names from the asset registries.
- **Permission per subcommand.** Each subcommand checks the sender's level (`ICommandSender.canCommandSenderUseCommand`) before it runs. Level 2, vanilla's game-master level, everywhere (P-CORE-2, D31); `test` also requires the development environment. Upstream's level 0 for `debug`, `map` and `saveprofile` is not kept (CORE-26, CORE-27).
- **Output** is `ChatComponentTranslation` with keys `lostcitiesnh.command.<subcommand>.<message>`, sent to the sender only. Long diagnostics also go to the mod logger. Nothing prints to standard output ([CONTRIBUTING § Code standards](../../../CONTRIBUTING.md#code-standards)).
- **Player-only subcommands** reject a console sender with a message instead of throwing.
- **Writes** go only under `config/lostcitiesnh/` (D13). A file name argument must match `[a-z0-9_]+`; the extension is added by the command. An existing file is never overwritten without a trailing `confirm` argument.
- **Layout queries** read the immutable layout ([D9](../../design/decision-log.md)); no command writes into a layout cache (CORE-32).

## Subcommands

| Subcommand | Upstream | Here | Permission | Arguments (1.7.10) | Behavior and output | Avoids |
|---|---|---|---|---|---|---|
| `createpart` | level 1, LC dimension | same | 2 | `<part> <x> <y> <z>` | Places the part and starts an edit session ([part editor](index.md#part-editor)). | |
| `editpart` | level 1, edit mode | same | 2 | none | Re-places the recorded part the player stands in and starts a session. | CORE-34, CORE-35 |
| `resumeedit` | level 1, edit mode | same | 2 | none | As `editpart`, without re-placing blocks. | CORE-35 |
| `listparts` | level 1, edit mode | same | 2 | none | One line per recorded part in the player's chunk. | |
| `locatepart` | level 1, edit mode | changed | 2 | `<part>` | Spiral of radius 30 chunks over the edit records, comparing one canonical name form; stops the whole search after 7 hits. | CORE-30, CORE-31 |
| `exportpart` | level 1, any name into the server directory | changed | 2 | `<name> [confirm]` | Writes the edited part as JSON to `config/lostcitiesnh/export/parts/<name>.json`, outside the live assets (P-CORE-4, D31). Sorted `missingpalette`; fails cleanly when the character pool runs out; the writer is closed on every path. | CORE-28, CORE-29 |
| `saveprofile` | level 0, `<profile>.json` into the server directory | changed | 2 | `<profile> <newName> [confirm]` | Writes the named profile in upstream's JSON format to `config/lostcitiesnh/profiles/<newName>.json`, ready to edit and name in `dimensionProfiles` ([0005](../../design/0005-rwg-overlay.md) §7 "Tuning"). The new name may not be a built-in profile's ([02 · profile system](../02-profiles-dimensions/index.md#profile-system), P-PROF-2). Errors are reported as errors, not as success messages. | CORE-27 |
| `profile` | (none) | new | 2 | `[pin <profile> confirm]` | With no argument, shows the dimension's pinned profile: name, layout version, asset-set hash and world type. `pin` re-pins (or, on an existing world, first pins: the per-world opt-in of [0005](../../design/0005-rwg-overlay.md#7-profile-selection-q7) §7) and logs that already-generated chunks will not match. | |
| `timings` | (none; replaces `stats`) | new | 2 | `[reset]` | The overlay's histograms: p50, p95, p99, p99.9 and max for the delegate's `provideChunk` and `populate`, the terrain and population phases and oracle calls, split by city, blend and untouched chunks, plus the count of chunks whose generation failed (P03-12, D31). Also printed to the log at server stop. | CORE-24 |
| `stats` | level 0, average of the last 100 | dropped | | | Replaced by `timings`. | CORE-24 |
| `debug` | level 0, dump to standard output | changed | 2 | none | The chunk's layout state, upstream's fields minus legacy streets, legacy highways and planner parallelism (D11), plus the chunk's landmark zone ([0006](../../design/0006-build-around-other-worldgen.md) §1). To the sender, summarized, and in full to the log. | CORE-26 |
| `map` | level 0, 41 × 41 to standard output | changed | 2 | none | The same 41 × 41 chunk map, to the log; one summary line to the sender. Landmark cores get their own character. | CORE-26 |
| `locate` | level 1 | same | 2 | `<building>` | Layout-only spiral of radius 30 chunks; up to 7 lines `x, groundLevel, z`. | |
| `createbuilding` | level 1, mutates the cached plan | changed | 2 | `<building> <floors> <cellars> <x> <y> <z>` | Builds from a copy of the chunk's plan with the forced building; the cached layout is never touched. Floors 1 to 20 and cellars 0 to 10 as upstream, further capped so nothing passes Y 254 ([0005](../../design/0005-rwg-overlay.md) §6). | CORE-32 |
| `testfill` | level 1, unseeded random 1.20 blocks | dropped | | | A 1.20 block-state test with no 1.7.10 use; the [block catalog](../05-assets/block-catalog/index.md) contact sheets serve authors instead. | |
| `test` (alias `scenario`) | (none) | new | 2, dev only (or `-Dlostcitiesnh.testHooks=true`) | `<scenario\|prefix\|all\|list>` | The scenario harness, built in M01: runs the selected scenarios on the server, one at a time, driven by server ticks, and reports `[LCNH-SCENARIO] PASS\|FAIL <name> <ms> <message>` lines and a `Scenarios: N passed, M failed` summary to the log and as `lostcitiesnh.scenario.*` chat to the sender. `all` skips manual-only scenarios ([docs/testing/](../../testing/README.md)). | |
| `view` | (none) | new | 2, test hooks only | `<player> <x> <y> <z> <yaw> <pitch>` | Moves a player to a viewpoint with a view direction, which vanilla 1.7.10's `tp` cannot set; a creative player hovers there. For scripted screenshots (D82). | |
| `find` | (none) | new | 2, test hooks only | `<center\|street\|interior\|edge\|softedge\|reserved\|village> [radius] [best]` | Prints the nearest city center, primary street, enterable building, city edge, soft edge toward a landmark, reserved lot or sewn village from the sender, from the plan alone, with a suggested viewpoint for `view`; with `best`, the soft edge, reserved lot or village gate within the radius that shows the city and the landmark together most clearly (D130); one machine-readable `[LCNH-FIND]` log line ([docs/testing/](../../testing/README.md#finding-things-to-look-at), D115). | |
| vanilla `resetchunks` | level 2, registered in production | dropped | | | 1.7.10 has no such vanilla command. | CORE-33 |

## Edit-mode gate

Upstream fails edit commands with "This world was not created with edit mode enabled" when the dimension's profile lacks `editMode`, and with "This dimension doesn't support Lost Cities" for a dimension with no profile. Both checks stay, against the world's pinned profile, as lang-keyed messages.
