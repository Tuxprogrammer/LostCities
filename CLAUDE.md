# Agent guide

Lost Cities: New Horizons backports McJty's Lost Cities (MIT, upstream branch `1.20`) to **GT New Horizons 2.9.0 (Minecraft 1.7.10, Forge)**. It is faithful to the original's functionality, not its bugs. Cities generate in the GTNH overworld over RWG terrain, with a GTNH layer on top. This file is a map: load the skill or document you need, not everything up front.

Skills live in [.claude/skills/](.claude/skills/). Claude Code loads one automatically when its description matches the task; you can also ask for one by name.

## Non-negotiables

1. **Ported code meets the bar before it lands.** Upstream is MIT, so its code may be ported, but a port is a rewrite in our style, with Javadoc, tests, a provenance header and every known upstream bug fixed. See [CONTRIBUTING § Ported code](CONTRIBUTING.md#ported-code) and [0001](docs/design/0001-port-policy-and-licensing.md).
2. **Faithful to functionality, not bugs.** Behavior follows [docs/original-spec/](docs/original-spec/README.md); nothing in [BUGS.md](docs/original-spec/BUGS.md) comes back.
3. **World generation is deterministic, never cascades, and stays in budget.** See [CONTRIBUTING § World generation rules](CONTRIBUTING.md#world-generation-rules).
4. **GT and the pack first.** Pack blocks, pack loot at EV or below, GT ores untouched, RWG as the overworld. → `gt-first-design`, [0003](docs/design/0003-world-presence.md), [0004](docs/design/0004-gtnh-layer-and-tier-ceiling.md)
5. **Source before guesses.** Read another mod's source at the tag the pack ships before integrating with it; cite it. → `gtnh-integration-research`
6. **Config never changes what is registered.** → `config-safe-registration`
7. **Servers never load client classes, and never trust clients.** → `networking-side-safety`
8. **Java 8 API.** `enableModernJavaSyntax = jabel` allows modern syntax, but the runtime is Java 8: no records, no `List.of`, no `String.isBlank`.
9. **Tests and comments ship with the code.** → `testing-gtnh-mods`, [CONTRIBUTING § Testing](CONTRIBUTING.md#testing)
10. **Never edit `build.gradle.kts` or `settings.gradle.kts`.** They come from the GTNH buildscript. Use `dependencies.gradle`, `repositories.gradle`, `gradle.properties`.
11. **Nothing machine-specific in git:** no hostnames, usernames, local paths, IPs or secrets. → `release-and-public-repo`

## Skills

| Skill | Use it when |
|---|---|
| [behavior-spec](.claude/skills/behavior-spec/SKILL.md) | Documenting upstream; bug register; deviations |
| [clean-room-reimplementation](.claude/skills/clean-room-reimplementation/SKILL.md) | License triage for anything not from upstream Lost Cities; the port rules for MIT code |
| [gtnh-integration-research](.claude/skills/gtnh-integration-research/SKILL.md) | Any code touching RWG, GT, Chisel or another pack mod; dependency bumps |
| [gt-first-design](.claude/skills/gt-first-design/SKILL.md) | Blocks, loot, tiers, anything the pack may already provide |
| [config-safe-registration](.claude/skills/config-safe-registration/SKILL.md) | Module toggles, registration, retiring content, NBT and saved-data formats |
| [testing-gtnh-mods](.claude/skills/testing-gtnh-mods/SKILL.md) | Unit, static, headless, worldgen, real-pack and client tests |
| [client-rendering-shaders](.claude/skills/client-rendering-shaders/SKILL.md) | Anything rendered; anything that breaks under shaders |
| [networking-side-safety](.claude/skills/networking-side-safety/SKILL.md) | Packets, sync, GUIs, server crashes on client classes |
| [in-pack-content-delivery](.claude/skills/in-pack-content-delivery/SKILL.md) | Quests, loot, pack configs; shipping from the jar |
| [release-and-public-repo](.claude/skills/release-and-public-repo/SKILL.md) | Tags, changelogs, upgrade notes, going public |
| [port-workflow](.claude/skills/port-workflow/SKILL.md) | Roadmap and gates, decision records, landing, long jobs, agents |
| [pack-contribution-policy](.claude/skills/pack-contribution-policy/SKILL.md) | Before planning any submission to a pack or another mod |

## Where to look

| You need… | Read |
|---|---|
| Where something is specified, decided or implemented, here or upstream, when you don't know its name | The `semantic_search` MCP tool ([tools/semantic-search/](tools/semantic-search/README.md)); grep or `git grep <pattern> 1.20` for exact names |
| Upstream code and assets | The local `1.20` branch, pinned at `f18a094d`: `git show 1.20:<path>`. Never commit to it except to fast-forward from upstream |
| The binding rules: workflow, porting, architecture, worldgen, GTNH integration, code, tests, assets, landing | [CONTRIBUTING.md](CONTRIBUTING.md) |
| What to build next, and its gates | [docs/ROADMAP.md](docs/ROADMAP.md) |
| Accepted design decisions | [docs/design/](docs/design/README.md) and the [decision log](docs/design/decision-log.md) |
| How upstream behaves | [docs/original-spec/](docs/original-spec/README.md) (layout: [CONVENTIONS.md](docs/original-spec/CONVENTIONS.md)) |
| Upstream bugs not to reproduce | [docs/original-spec/BUGS.md](docs/original-spec/BUGS.md) |
| How this mod differs and fits the pack | [docs/new-spec/](docs/new-spec/README.md), and what is undecided in [OPEN-QUESTIONS.md](docs/new-spec/OPEN-QUESTIONS.md) |
| Changes the pack would need | [docs/PACK-CHANGES.md](docs/PACK-CHANGES.md) |
| Templates: decision record, spec sheet, area index, bug register, change checklist | [docs/templates/](docs/templates/) |
| Build settings, mod ID, package | [gradle.properties](gradle.properties) |
| Dependencies and repositories | [dependencies.gradle](dependencies.gradle), [repositories.gradle](repositories.gradle) |
| Buildscript FAQ, migrating or porting a build | The GTNH buildscript's own docs: [FAQ](https://github.com/GTNewHorizons/ExampleMod1.7.10/blob/master/docs/FAQ.md), [migration](https://github.com/GTNewHorizons/ExampleMod1.7.10/blob/master/docs/migration.md), [porting](https://github.com/GTNewHorizons/ExampleMod1.7.10/blob/master/docs/porting.md) |

## Commands

```sh
./gradlew spotlessApply build   # format, compile, test: run before every commit
./gradlew test                  # unit and static-check tests
./gradlew runServer             # headless boot and worldgen scenarios
./gradlew runClient             # visual checks
python3 -I tools/semantic-search/index.py --query "..."   # semantic search from the shell
```

Builds and Minecraft runs belong on the build server, not a development laptop (decision log D4).

## Housekeeping

- Changes that meet the definition of done may land straight on `gtnh`; parallel work uses worktrees and merges with `--no-ff` (decision log D1).
- When behavior changes, update the spec deviation notes or design records in the same change. Docs state the current truth; git keeps the history.
- Keep reference packs, other mods' clones and decompiled output in the git-ignored `reference/` folder.
- American English in code, docs and lang (`color`, `armor`, `gray`); keep external API identifiers as they are.
