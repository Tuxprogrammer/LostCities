# Remote builds and runs

Builds, tests and Minecraft runs happen on the shared build server, never on a development laptop (decision log D4). [`lc-remote.sh`](lc-remote.sh) drives that server from the laptop over ssh. It checks out a commit there, builds it, boots the dev server, runs the real GTNH 2.9.0 server and client, and copies logs and screenshots back into the git-ignored `reference/remote/`.

Nothing about the server is committed: its address and paths live in the git-ignored `.env`.

## Setup

Add two lines to `.env` at the root of the main checkout. The file is git-ignored; keep it mode 600.

```sh
LC_BUILD_HOST=<user>@<build host>      # ssh target; passwordless (key) login
LC_REMOTE_ROOT=<project dir on the host>
```

Environment variables of the same names override `.env`, and `LC_ENV_FILE` points the script at another file. Worktrees read the main checkout's `.env` and write to its `reference/remote/`.

The server side must already exist under `LC_REMOTE_ROOT`. A `README.txt` there documents it:

- `env.sh` sets up the toolchain: JDK 21 runs Gradle, the buildscript compiles with the JDK 8 toolchain, and `GRADLE_USER_HOME` is the project's own;
- `bin/lc-sync.sh`, `bin/lc-build.sh` and `bin/lc-devserver.sh` handle the per-slot checkouts in `wt/<slot>/`;
- `realpack/run.sh` runs the GTNH 2.9.0 dedicated server;
- `realpack/client-run.sh` runs the GTNH 2.9.0 client under Xvfb.

## Commands

| Command | What it does | Fetched to `reference/remote/` |
|---|---|---|
| `lc-remote.sh sync <slot> [<rev>] [--bundle]` | Checks out `<rev>` (default `HEAD`) in slot `<slot>` | |
| `lc-remote.sh build <slot> [<rev>] [-- <gradle args>]` | Syncs, then runs `./gradlew --no-daemon` (default `spotlessCheck build`) | `builds/<log>` |
| `lc-remote.sh devserver <slot> [<rev>] [-- --timeout <s> --cmds '<cmds>']` | Syncs, runs `runServer` until "Done", sends the commands, stops it | `devserver/<log>` |
| `lc-remote.sh realpack [--slot <slot>] [-- <run.sh args>]` | Boots the real-pack server, with the slot's built jar if `--slot` is given | `realpack/<run id>/` |
| `lc-remote.sh client [-- <client-run.sh args>]` | Runs the real-pack client headless and takes a screenshot | `client/<run id>/` |
| `lc-remote.sh fetch <path>` | Copies a file or folder under `LC_REMOTE_ROOT` | `fetched/` |
| `lc-remote.sh status` | Lists our running processes and the latest run markers | |

`<rev>` is resolved to a sha on the laptop. If an `origin/*` branch already contains the sha, the server fetches it over https. Otherwise, or with `--bundle`, the script copies a git bundle across, so unpushed work can be built.

### Real-pack arguments (`run.sh`)

- `--mod <jar>`: installs the jar as `lostcitiesnh-*.jar`. `--slot` passes the slot's jar for you.
- `--fresh`: deletes the world before booting.
- `--world <name>`: uses another world folder.
- `--level-type <type>` and `--seed <seed>`: apply only to a new world.
- `--cmds '<cmd>; wait:<s>; until:<regex>'`: console commands sent once the server is up. `wait:` and `until:` are steps for the script, not commands.
- `--jvm '<args>'`, `--timeout <s>`, `--run-timeout <s>`.
- `--set-config '<file under config/>:<key>=<value>'` (repeatable): changes one config value for this run only and puts the file back when the run ends, however it ends. The overlay scenarios use it for ArchaicFix's `logCascadingWorldgen`, which the pack ships off.

Each run writes `runs/<id>/` with the console, the FML log, any crash report and a `run.done` summary: status, boot seconds and peak RSS.

World types: a dedicated server reads `level-type` from `server.properties`, and the GTNH 2.9.0 server pack ships `level-type=rwg`. Its default is therefore **classic RWG**, unlike single player, where the client's DefaultWorldGenerator selects RWG2 Continents ([rwg.md](../../docs/new-spec/00-pack-integration/rwg.md)). Pass `--level-type RWG2_CONTINENTS` or `--level-type RWG` with `--fresh` (or a new `--world`) to pick one.

### Client arguments (`client-run.sh`)

- `--display <n>`: picks the Xvfb display, within our range.
- `--server <host:port>`: joins a server. Without it, the client stops at the main menu.
- `--shot <name>`, `--wait <s>`: name the screenshot and set how long to wait before taking it.
- `--timeout <s>`, `--user <name>`, `--heap <size>`.
- `--extra-shot <name>:<seconds>` (repeatable): another screenshot that many seconds after the one before, for viewpoints the server moves the player to.
- `--mod <jar>`: installs a `lostcitiesnh` jar in the client for this run and removes it afterwards. A server running the mod rejects a client without it (FML's mod list check), so joining a `--slot` server needs the same jar here, by its path on the build server (`<LC_REMOTE_ROOT>/wt/<slot>/build/libs/lostcitiesnh-<version>.jar`).

The client uses an offline account, so it can join only the real-pack server, which runs with `online-mode=false`. Joining with `--server` while the client is still starting up sometimes fails in FML's handshake (`playClientBlock is null` in the client log, `LCBot lost connection: Disconnected` on the server); run the client again.

## Examples

```sh
tools/remote/lc-remote.sh build main gtnh                       # format check, compile, tests
tools/remote/lc-remote.sh build feat HEAD -- test               # unpushed work, tests only
tools/remote/lc-remote.sh devserver main gtnh -- --cmds 'forge tps'
tools/remote/lc-remote.sh realpack -- --fresh                   # bare pack, classic RWG
tools/remote/lc-remote.sh realpack --slot main -- --fresh --world rwg2c \
    --level-type RWG2_CONTINENTS --seed 12345 --cmds 'forge tps'
tools/remote/lc-remote.sh client -- --shot menu                 # main-menu screenshot
```

Screenshot in a world (two terminals; the server waits for the client):

```sh
tools/remote/lc-remote.sh realpack --slot main -- --cmds \
  'until:LCBot joined the game; op LCBot; gamemode 1 LCBot; time set 6000; tp LCBot 0 200 0; wait:90'
tools/remote/lc-remote.sh client -- --server 127.0.0.1:25800 --shot spawn --wait 45
```

Use creative mode and a safe height. A survival teleport into the air falls. Vanilla `tp` cannot set the view direction; with the test hooks on (`--jvm '-Dlostcitiesnh.testHooks=true'`), `lostcities view <player> <x> <y> <z> <yaw> <pitch>` does, so a client run with `--extra-shot` can take several fixed viewpoints.

## Shared-box rules

The build server is shared with other projects' agents.

- **One directory.** Everything of ours lives under `LC_REMOTE_ROOT`. Never read from, write to or depend on another project's directories, and never write outside ours.
- **Our ranges.** Xvfb displays `:80`–`:89`. TCP ports `25800`–`25899`: the real-pack server on the first, dev servers from `25810`. The scripts enforce these ranges.
- **Our processes only.** Stop only what a script started, by the PID it recorded. Never `pkill` or `killall` by pattern. Leave nothing running: every script stops its JVM and Xvfb when it ends, fails or is interrupted. `lc-remote.sh status` shows anything left over.
- **Memory.** Stay under 64 GB in total. Typical use: a Gradle build takes about 5 GB, the real-pack server about 8 GB and the real-pack client about 11 GB.
- **One at a time.** Locks allow one real-pack server, one client, and one job per slot at a time. Use separate slots for parallel builds.

## Not yet built

- The scripted alpha screenshot set: a hidden HUD (F1 needs key input on the X display) and locked weather. Fixed coordinates, view angles (`lostcities view`) and time (`time set`, `gamerule doDaylightCycle false`) work.
