# gtnh-reference

A helper for researching the mods in the GTNH pack this project targets (GTNH 2.9.0, [0002](../../docs/design/0002-target-pack.md)). It keeps the extracted pack in the git-ignored `reference/` folder, indexes every mod jar against its GitHub source and the tag the pack ships, clones that source, and decompiles a jar only as a last resort.

The rules that come with it (source first at the pack's tag, decompile only as a fallback, never paste decompiled code, cite what you relied on) are in [CONTRIBUTING § GTNH integration](../../CONTRIBUTING.md#gtnh-integration) and the `gtnh-integration-research` skill.

Requirements: Python 3.9 or later (standard library only), `git`, a JDK on `PATH` for `decompile`, and `gh` for the GitHub commands it suggests.

```sh
T=tools/gtnh-reference/gtnh_ref.py
python3 -I $T fetch [<pack-zip-url>] [--keep-archive]   # download, extract, index; default: the official 2.9.0 MultiMC zip
python3 -I $T index                                     # rebuild MOD-INDEX.md and mod-index.json
python3 -I $T mod <name|modid|jar>                      # jar, mod IDs, license, source link, extras, gh commands
python3 -I $T where <class-or-fragment>                 # which jar ships a class
python3 -I $T clone <name|modid|jar>                    # shallow clone at the pack's tag -> reference/src/
python3 -I $T jars <name|modid|jar>                     # download dev, API-source or sources jars -> reference/maven/
python3 -I $T decompile <name|modid|jar|path.jar> [--class fqcn ...] [--force]   # fallback -> reference/decompiled/
```

`reference/` is the main checkout's, found through `git rev-parse --git-common-dir`, so every worktree shares one copy of the pack and the clones.

GTNH's release metadata knows the source of GTNH-maintained mods only. [`source-overrides.json`](source-overrides.json) adds hand-verified sources, API jars, dev jars and maven coordinates for the others; each entry records the pack mod version it was checked against (`verified_version`), and `index` flags an entry for re-checking when the pack ships a different version. When you find a new source, add it there (the file's `_comment` explains the fields) and run `index`. Unlike `reference/`, it is committed.

The downloaded pack is untrusted data. The script only reads and extracts it (refusing archive entries that escape the target folder) and never runs anything from it. Run the script with `python3 -I`, from outside `reference/`.

What it writes, all under `reference/` (git-ignored):

| Path | Contents |
|---|---|
| `gtnh/CURRENT` | The active pack version |
| `gtnh/<version>/` | The extracted MultiMC instance, plus `manifest.json`, `MOD-INDEX.md` and `mod-index.json` |
| `gtnh/gtnh-assets.json` | GTNH's index of mod repos, licenses and jar names |
| `gtnh/_download/` | The pack zip, if kept |
| `src/<Mod>@<tag>/` | Shallow source clones |
| `maven/<Mod>/` | Downloaded dev, API-source and sources jars |
| `decompiled/<jar>/` | Vineflower output, with SRG names remapped to MCP names |
| `tools/` | The pinned Vineflower jar |

When the pack version changes: update `PACK_VERSION` in the script, run `fetch`, re-check every override whose version changed, and follow the pack-bump steps in [0002](../../docs/design/0002-target-pack.md).
