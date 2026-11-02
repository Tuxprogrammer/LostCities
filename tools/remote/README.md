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

The server-side scripts live in [server/](server/), laid out as they sit under `LC_REMOTE_ROOT`; `lc-remote.sh install-tools` copies them there. Each finds the root from its own location, so nothing in them names the host or the path. The rest of the root is set up by hand once (JDKs and the Xvfb userland in `shared/`, the GTNH 2.9.0 server and client in `realpack/`), and [server/README.txt](server/README.txt) records how:

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
| `lc-remote.sh shots <shotlist.json> [--slot <slot>] [-- <shots-run.py args>]` | Boots the real-pack server and client for a shot list's world, takes every shot clean and writes a review page ([Shot lists](#shot-lists)) | `shots/<run id>/` |
| `lc-remote.sh fetch <path>` | Copies a file or folder under `LC_REMOTE_ROOT` | `fetched/` |
| `lc-remote.sh status` | Lists our running processes and the latest run markers | |
| `lc-remote.sh install-tools` | Copies `tools/remote/server/` to `LC_REMOTE_ROOT`, adding or replacing only those files | |

`<rev>` is resolved to a sha on the laptop. If an `origin/*` branch already contains the sha, the server fetches it over https. Otherwise, or with `--bundle`, the script copies a git bundle across, so unpushed work can be built.

### Real-pack arguments (`run.sh`)

- `--mod <jar>`: installs the jar as `lostcitiesnh-*.jar`. `--slot` passes the slot's jar for you.
- `--fresh`: deletes the world before booting.
- `--world <name>`: uses another world folder.
- `--level-type <type>` and `--seed <seed>`: apply only to a new world.
- `--cmds '<cmd>; wait:<s>; until:<regex>'`: console commands sent once the server is up. `wait:` and `until:` are steps for the script, not commands.
- `--jvm '<args>'`, `--timeout <s>`, `--run-timeout <s>`.
- `--set-config '<file under config/>:<key>=<value>'` (repeatable): changes one config value for this run only and puts the file back when the run ends, however it ends. The overlay scenarios use it for ArchaicFix's `logCascadingWorldgen`, which the pack ships off.
- `--view-distance <3-15>`: the server's `view-distance` for this run (the box's default is 6), put back when the run ends. The shot runner sets it to the capture's render distance.

Each run writes `runs/<id>/` with the console, the FML log, any crash report and a `run.done` summary: status, boot seconds and peak RSS.

World types: a dedicated server reads `level-type` from `server.properties`, and the GTNH 2.9.0 server pack ships `level-type=rwg`. Its default is therefore **classic RWG**, unlike single player, where the client's DefaultWorldGenerator selects RWG2 Continents ([rwg.md](../../docs/new-spec/00-pack-integration/rwg.md)). Pass `--level-type RWG2_CONTINENTS` or `--level-type RWG` with `--fresh` (or a new `--world`) to pick one.

### Client arguments (`client-run.sh`)

- `--display <n>`: picks the Xvfb display, within our range.
- `--server <host:port>`: joins a server. Without it, the client stops at the main menu.
- `--shot <name>`, `--wait <s>`: name the screenshot and set how long to wait before taking it.
- `--timeout <s>`, `--user <name>`, `--heap <size>`.
- `--extra-shot <name>:<seconds>` (repeatable): another screenshot that many seconds after the one before, for viewpoints the server moves the player to.
- `--mod <jar>`: installs a `lostcitiesnh` jar in the client for this run and removes it afterwards. A server running the mod rejects a client without it (FML's mod list check), so joining a `--slot` server needs the same jar here, by its path on the build server (`<LC_REMOTE_ROOT>/wt/<slot>/build/libs/lostcitiesnh-<version>.jar`).

- `--jvm '<args>'`: extra JVM arguments for the client, such as the clean capture's properties.
- `--hold <file>`: after the screenshots, stays up until `<file>` exists (bounded by `--timeout`), so another script can drive the client. The shot runner uses it.
- `--restore-options`: backs up `options.txt` and `optionsnf.txt` before the run and puts them back after it, deleting any the run created.

The client uses an offline account, so it can join only the real-pack server, which runs with `online-mode=false`. Joining with `--server` often fails in FML's handshake (`playClientBlock is null` in the client log, `LCBot lost connection: Disconnected` on the server), even once the server is up: FML's startup join opens the connection before it creates the latch the handshake waits on, and a local server answers first. Run the client again. The shot runner avoids it: the capture mode joins from the main menu after creating that latch (`-Dlostcitiesnh.captureServer`, D-M05S-4).

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

Use creative mode and a safe height. A survival teleport into the air falls. Vanilla `tp` cannot set the view direction; with the test hooks on (`--jvm '-Dlostcitiesnh.testHooks=true'`), `lostcities view <player> <x> <y> <z> <yaw> <pitch>` does, so a client run with `--extra-shot` can take several fixed viewpoints. For review screenshots, use a shot list instead.

## Shot lists

Review screenshots, such as the alpha gate's set ([ROADMAP](../../docs/ROADMAP.md), M05), come from a shot list: a small committed JSON file under [shots/](shots/) naming a world and its viewpoints. One command takes them all, clean:

```sh
tools/remote/lc-remote.sh shots tools/remote/shots/synthetic-city.json --slot m05s
tools/remote/lc-remote.sh shots tools/remote/shots/synthetic-city.json --slot m05s -- --level-type RWG --world shots-rwg
```

`lc-remote.sh` checks the list on the laptop, sends it to [`realpack/shots-run.py`](server/realpack/shots-run.py) on the build server, and fetches `runs/shots-<id>/` to `reference/remote/shots/<id>/`. Open its `index.html`: one card per shot with its caption, position and status. The folder also holds the PNGs, `results.json`, `runner.log` and the server's and client's script output.

The runner:

1. waits (polling every 45 s, at most `--lock-wait`, default 1800 s) until neither the real-pack server nor the client is in use;
2. boots the server with `run.sh` for the list's world, the slot's jar, the test hooks, and `view-distance` set to the list's render distance, and waits for "Done";
3. starts the client with `client-run.sh --hold --restore-options` and the clean capture on. The client joins by itself from the main menu, and retries once after a disconnect. If it still has not joined (dropped twice, exited, crashed, or `--join-timeout`, default 420 s), the runner stops that client and starts another once;
4. sets creative mode, stops the daylight cycle, sets the time, clears the weather for 1,000,000 seconds, turns mob spawning off (unless `"mobs": true`), and runs the list's `setup` commands;
5. for each shot, moves the player with `lostcities view`, waits until the client logs that the viewpoint has settled (at most the shot's `settle_timeout`), waits the shot's `wait` and screenshots the display (`xwd`);
6. releases the client, ends the server through its console, and writes the review page.

Every wait is bounded, and the whole run by `--deadline` (default 5400 s). However the runner ends, it stops the client and server through their own scripts, whose traps stop the JVMs and Xvfb.

### Clean capture

The client side is a dev-only mode in the mod ([`client/CleanCapture`](../../src/main/java/com/tuxprogrammer/lostcities/client/CleanCapture.java)), on with `-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.capture=true` on the client JVM. The runner sets both. The mode:

- hides the GUI as F1 does. That removes the hotbar, the hand and held item, the crosshair, the chat, and every overlay drawn through Forge's `RenderGameOverlayEvent`, which is not fired while the GUI is hidden: the pack's InGame Info XML panel and JourneyMap's minimap among them;
- clears vanilla's achievement window every tick and turns off its hint. The "Press 'E' to open your inventory" toast is that window, and it draws even with the GUI hidden;
- closes any screen that opens in the world;
- with `-Dlostcitiesnh.captureServer=<host>:<port>`, joins that server from the main menu by itself;
- sets the field of view, the render distance and clouds (off by default; they hide a city from above) in memory, before any world loads;
- logs `[LCNH-CAPTURE] settled x=… y=… z=… yaw=… pitch=… top=… ticks=…` once the camera has been still for a second, every chunk within the capture radius has arrived, and the terrain renderer's build queue has stayed empty for a second. With Angelica, as in the pack, that queue is Celeritas's (`isTerrainRenderComplete()`); without it, vanilla's.

Weather and time are the server's, so the runner locks them with console commands.

### Format

```json
{
  "title": "Synthetic test city, seed 4242",
  "world": {"name": "shots-rwg2c", "level_type": "RWG2_CONTINENTS", "seed": "4242", "fresh": true},
  "server_jvm": "-Dlostcitiesnh.planSource=synthetic",
  "time": 6000, "render_distance": 10, "fov": 70,
  "shots": [
    {"name": "above", "x": 32905, "z": 32905, "y": "top+55", "yaw": 135, "pitch": 35, "wait": 3,
     "caption": "City from above, looking north-west."},
    {"name": "village", "find": {"cmd": "lostcities find village", "match": "at (?P<x>-?\\d+) (?P<y>-?\\d+) (?P<z>-?\\d+)"},
     "offset": [-20, 30, 40], "yaw": -153, "pitch": 35, "caption": "A village sewn into a city."}
  ]
}
```

| Key | Default | Meaning |
|---|---|---|
| `title` | run id | Heading of the review page |
| `world` | `shots`, `RWG2_CONTINENTS`, random seed, fresh | World folder, `level_type`, `seed`, and `fresh` (delete it first). `--world`, `--level-type`, `--seed`, `--fresh` and `--reuse-world` override them, so one list serves both world types |
| `server_jvm`, `client_jvm` | none | Extra JVM arguments; the test hooks and the capture properties are always added |
| `user` | `LCBot` | The client's offline account |
| `time` | 6000 | World time, locked |
| `render_distance` | 10 | Client render distance and server `view-distance`, 3 to 15 |
| `fov` | 70 | Field of view, 30 to 110 |
| `clouds`, `mobs` | `false` | Draw clouds; leave mob spawning on |
| `setup` | none | Console commands after the join; `{user}` is replaced by the account |
| `settle_timeout` | 300 | Default bound, in seconds, on each shot's settle wait |

Each shot has a `name` (letters, digits, `.`, `_`, `-`; the PNG's name), an optional `caption`, `yaw` (default 0; 0 faces south, 90 west, 180 north, −90 east), `pitch` (default 30; positive looks down), `wait` (seconds after settling, default 2), optional `time` and `settle_timeout`, and a position:

- `x` and `z`, literal block coordinates; or
- `find`: a console command and a regex with named groups `x`, `z` and optionally `y`. The runner sends the command and takes the first console line after it that matches; `offset` `[dx, dy, dz]` is added. This is the seam for the finder (`/lostcities find <kind>`, M05J): when it lands, a list names a kind instead of coordinates, with `match` fitted to its output.

`y` is the feet height, as `tp` takes it: a number, or `"top"`, `"top+N"`, `"top-N"` for N blocks above or below the topmost block of the shot's column. A relative `y` first looks down from y = 220 until the column has loaded, reads its top from the client, then moves to the shot. It lets one list fit worlds whose terrain differs. Without `y`, a found `y` is used, or else `"top+3"`.

A shot that does not settle in time is still taken and marked `unsettled` on the page; a `find` that matches nothing marks the shot `failed`. `--only <name,...>` takes a subset; `--check` validates the list and prints the plan without the server.

## Shared-box rules

The build server is shared with other projects' agents.

- **One directory.** Everything of ours lives under `LC_REMOTE_ROOT`. Never read from, write to or depend on another project's directories, and never write outside ours.
- **Our ranges.** Xvfb displays `:80`–`:89`. TCP ports `25800`–`25899`: the real-pack server on the first, dev servers from `25810`. The scripts enforce these ranges.
- **Our processes only.** Stop only what a script started, by the PID it recorded. Never `pkill` or `killall` by pattern. Leave nothing running: every script stops its JVM and Xvfb when it ends, fails or is interrupted. `lc-remote.sh status` shows anything left over.
- **Memory.** Stay under 64 GB in total. Typical use: a Gradle build takes about 5 GB, the real-pack server about 8 GB and the real-pack client about 11 GB.
- **One at a time.** Locks allow one real-pack server, one client, and one job per slot at a time. Use separate slots for parallel builds.
