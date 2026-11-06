# Agent guide

Lost Cities: New Horizons backports McJty's Lost Cities (MIT, upstream branch `1.20`) to **GT New Horizons 2.9.0 (Minecraft 1.7.10, Forge)**. It is faithful to the original's functionality, not its bugs. Cities generate in the GTNH overworld over RWG terrain, with a GTNH layer on top. This file is a map: load the skill or document you need, not everything up front.

Skills live in [.claude/skills/](.claude/skills/); agent definitions in [.claude/agents/](.claude/agents/), guard hooks in [.claude/hooks/](.claude/hooks/) (see [.claude/README.md](.claude/README.md)), brief templates in [docs/agent-setup/](docs/agent-setup/README.md), and quiet-run and wait helpers in [tools/agent/](tools/agent/README.md). Claude Code loads one automatically when its description matches the task; you can also ask for one by name.

## First step in every session and every subagent: load the search tool

[.mcp.json](.mcp.json) sets `alwaysLoad: true` on the `semantic-search` server, so its tools should already be callable. If they instead appear only as deferred names (older Claude Code, or a setting that overrides it), calling one before its schema is loaded fails with `InputValidationError`. Then your **first tool call**, before any grep, glob or file read, is:

```
ToolSearch  query: "select:mcp__semantic-search__semantic_search,mcp__semantic-search__semantic_index_status,mcp__semantic-search__semantic_reindex"  max_results: 3
```

- **Subagents too.** Whether a subagent gets the schemas up front is not documented, so check your own tool list and load them yourself if they are deferred. Do not rely on the brief to remind you.
- **Not listed as deferred?** Then the tool is already callable; skip the load. **Not listed at all?** The MCP server is not approved or not running: use the CLI fallback below and say so in your report.
- **Use it first** for any "where is / how does / which doc covers" question when you do not know the exact identifier or file. Use grep or `git grep 1.20 <pattern>` once you do. Don't open several candidate files to find something; one search replaces that.
- **Index looks stale?** Call `semantic_index_status`; if it is behind the current commit, call `semantic_reindex`.
- **CLI fallback** (same index, no MCP needed): `python3 -I tools/semantic-search/index.py --query "..." -k 5 [--source upstream|repo] [--kind java|docs|data]`.

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
12. **Spend tokens like they're scarce.** Subagents default to Sonnet at medium effort (`.claude/settings.json`); use Opus only for hard design or code, and say why. Every agent brief has a hard stop (report by ~120 calls or ~200k context) and at most two verification rounds; lanes never merge `gtnh`, the orchestrator integrates once. Wait on long jobs with one capped done-marker loop. → `orchestrating-parallel-agents`, `agent-context-budget`, `mod-port-budget`

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
| [mod-port-budget](.claude/skills/mod-port-budget/SKILL.md) | Budgets, verification tiers T1–T4 and doc depth for a port run by agents |
| [orchestrating-parallel-agents](.claude/skills/orchestrating-parallel-agents/SKILL.md) | Spawning, briefing or supervising agents: scope, model, stop rules |
| [agent-context-budget](.claude/skills/agent-context-budget/SKILL.md) | Any session past ~60 calls or ~150k context; handing off |
| [integration-merge-choreography](.claude/skills/integration-merge-choreography/SKILL.md) | Landing lanes on `gtnh`: one integrator, when to re-verify |
| [remote-build-runner](.claude/skills/remote-build-runner/SKILL.md) | Slots, instances and runs on the build server |
| [waiting-on-remote-jobs](.claude/skills/waiting-on-remote-jobs/SKILL.md) | Waiting on builds, servers, clients or shot runs |
| [quiet-tool-output](.claude/skills/quiet-tool-output/SKILL.md) | Commands or files with large output: builds, logs, diffs, docs |
| [cheap-screenshot-review](.claude/skills/cheap-screenshot-review/SKILL.md) | Looking at screenshots or shot-list output |

## Where to look

| You need… | Read |
|---|---|
| Where something is specified, decided or implemented, here or upstream, when you don't know its name | The `semantic_search` MCP tool, loaded via ToolSearch as in "First step" above ([tools/semantic-search/](tools/semantic-search/README.md)); grep or `git grep <pattern> 1.20` for exact names |
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

Builds and Minecraft runs belong on the build server, not a development laptop (decision log D4). Drive it with [tools/remote/lc-remote.sh](tools/remote/README.md) (`build <slot> <rev>`, `devserver`, `realpack`, `client`); host and paths come from the git-ignored `.env`.

## Housekeeping

- Changes that meet the definition of done may land straight on `gtnh`; parallel work uses worktrees and merges with `--no-ff` (decision log D1).
- When behavior changes, update the spec deviation notes or design records in the same change. Docs state the current truth; git keeps the history.
- Keep reference packs, other mods' clones and decompiled output in the git-ignored `reference/` folder.
- American English in code, docs and lang (`color`, `armor`, `gray`); keep external API identifiers as they are.
