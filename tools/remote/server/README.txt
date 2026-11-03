Lost Cities: New Horizons (modid lostcitiesnh) build and test box
==================================================================

Owned by LostCities sessions only. Everything lives under this directory (LC_REMOTE_ROOT on the laptop) and nowhere
else. Nothing here depends on another project's directories at runtime: the JDKs, the Xvfb userland
and the user.home shim were copied (once, read-only) from another project's shared/ folder.

Shared-box rules
  - Stay inside this directory. Never touch other projects' dirs.
  - Our ranges: Xvfb displays :80-:89; TCP ports 25800-25899 (25800 real-pack server,
    25810+ dev servers). Other projects use :90+ and the 25600s/25700s.
  - Kill only PIDs we started (pid files); never pkill/killall by pattern. Leave nothing running.
  - Stay under 64 GB RAM in total. Typical: Gradle build ~4-5 GB, real-pack server 8 GB heap
    (~8 GiB RSS), real-pack client 8 GB heap (~10.5 GiB RSS).

Layout
  env.sh       source me: JAVA_HOME=shared/jdk21 (runs Gradle 9; RFG toolchains shared/jdk8 to
               compile Java 8 bytecode), GRADLE_USER_HOME=gradle/, PATH with the Xvfb userland,
               JAVA_TOOL_OPTIONS=-Duser.home=shared/home (shim; shared/home/.gradle -> gradle/),
               lc_xvfb N / lc_xvfb_stop N (N in 80-89; pid in logs/xvfb-N.pid).
  shared/      jdk8 jdk17 jdk21 jdk25 jbr17 xvfb home
  gradle/      GRADLE_USER_HOME. gradle.properties lists our JDKs as the only toolchains
               (auto-detect and auto-download off), Gradle heap 4g, daemon off.
  bin/         lc-sync.sh, lc-build.sh, lc-devserver.sh, lc-leveldat.py (below)
  wt/<slot>/   one checkout per verification slot (detached HEAD). wt/.bundles/ holds git bundles
               pushed from a laptop. wt/.<slot>.lock serializes work on a slot.
  logs/        build-<slot>-<stamp>.log (+ build-<slot>-latest.log link), devserver-<slot>-*.log,
               devserver-<slot>.done, xvfb-N.log/.pid
  realpack/    the real GTNH 2.9.0 server and client (below)

bin/lc-sync.sh <slot> <git-rev> [--bundle <file>]
  Create or update wt/<slot> at <git-rev> (branch, tag or sha) from
  https://github.com/Tuxprogrammer/LostCities.git, or from a git bundle (--bundle or LC_BUNDLE) when
  the rev is not pushed. Detached checkout; untracked files cleaned except build/, run/, .gradle/;
  build/libs/*.jar deleted, so a slot's jar is always from its current checkout.
  Laptop side: the repo's tools/remote/lc-remote.sh wraps all of this (its README.md).

bin/lc-build.sh <slot> [gradle args...]          (default: spotlessCheck build)
  Sources env.sh and runs ./gradlew --no-daemon --console=plain in wt/<slot>. No daemon, so
  nothing outlives the build. Log: logs/build-<slot>-<stamp>.log. Prints BUILD result, test totals
  and "exit=<rc> log=<path>". LC_BUILD_TIMEOUT (default 3600 s) kills the build's process group
  (exit 124). Before every run it writes the dev server's eula.txt and server.properties into
  wt/<slot>/run/ (RFG's run dir) and run/server/: server-port=LC_DEV_PORT (default 25810, must be
  25810-25899), server-ip=127.0.0.1, online-mode=false, view-distance=6.
  LC_STDIN / LC_LOG override Gradle's stdin (default /dev/null) and the log path.

bin/lc-devserver.sh <slot> [--timeout <s>] [--cmds '<cmd>; wait:<s>; until:<regex>']
  Headless dev-server check: gradlew runServer through lc-build.sh with a fifo on stdin, waits for
  "Done (", sends --cmds, sends stop, waits for Gradle to exit. Writes logs/devserver-<slot>.done
  (status=ok|died-during-boot|boot-timeout|stop-timeout+killed|gradle-failed, boot_seconds, log).

bin/lc-leveldat.py <level.dat>   prints generatorName, RandomSeed, LevelName (python3 -I).

Proof (2026-11-01, gtnh 5450e6cc, the renamed ExampleMod skeleton)
  lc-build.sh main spotlessCheck build: BUILD SUCCESSFUL in 1m29s (cold caches); classes are
  major 52 (Java 8). lc-devserver.sh main: "Starting Minecraft server on 127.0.0.1:25810",
  Done after 16 s, stop, Gradle exit 0.

Real-pack server: realpack/
  dl/          GT_New_Horizons_2.9.0_Server_Java_17-26.zip (568,348,402 bytes)
               from https://downloads.gtnewhorizons.com/ServerPacks/GT_New_Horizons_2.9.0_Server_Java_17-26.zip
               sha256 0ab3de26e4d0de80a2df286916570d9af31c3394cb1f44aee8b9aa1f2ead9831 (also in dl/*.sha256)
  server/      the unpacked zip. Edits: eula.txt eula=true; server.properties server-port=25800,
               server-ip=127.0.0.1, online-mode=false, view-distance=6, white-list=false,
               snooper-enabled=false. Everything else is the pack's (level-name=World,
               level-type=rwg, difficulty=3, ...). server.properties.pack-original is the zip's file.
               The server rewrites server.properties on boot (adds keys, reorders); that is harmless.
  run.sh       the only supported way to start it (below).
  runs/<id>/   one folder per run: console.log, fml-server-latest.log, latest.log, new crash
               reports, server.pid, run.done. Client runs are runs/client-<id>/.
  run.done     marker of the latest server run (copy of runs/<id>/run.done); deleted when a run
               starts, written when it ends however it ends. run.lock is run.sh's flock.

World types (verified)
  The dedicated server takes the world type from server.properties level-type, matched
  case-insensitively against registered WorldType names (vanilla WorldType.parseWorldType).
    RWG2 Continents   level-type=RWG2_CONTINENTS   (rwg2-2.0.6, rwg2/RWG.java)
    RWG2 (no continents) level-type=RWG2           (registered, hidden on the client's screen)
    classic RWG       level-type=RWG  (or rwg)     (RWG-alpha-1.5.3, rwg/RWG.java)
  The server zip ships level-type=rwg, so the server pack's default is CLASSIC RWG: a --fresh boot
  writes generatorName=RWG to level.dat. RWG2_CONTINENTS is the default only on the client's
  Create World screen, set by config/defaultworldgenerator.cfg ("World Generator"=RWG2_CONTINENTS,
  whitelist flat/RWG/RWG2_CONTINENTS); DefaultWorldGenerator is a client-only GUI mod.
  The world type is fixed when a world is created; it only matters with --fresh or a new --world.

run.sh [--mod <jar>] [--jvm '<args>'] [--cmds '<cmd>; ...'] [--fresh] [--world <name>]
       [--level-type <type>] [--seed <seed>] [--timeout <s>] [--cmd-wait <s>] [--run-timeout <s>]
  --mod <jar>       install this jar as server/mods/lostcitiesnh-<version>.jar. Old lostcitiesnh-*.jar
                    are always removed first, so a run without --mod is the bare pack.
  --jvm '<args>'    extra JVM args (word-split).
  --cmds '...'      console commands, ';'-separated, sent after "Done (", each followed by
                    --cmd-wait s (default 2). 'wait:<s>' sleeps; 'until:<regex>' polls the console
                    every 5 s until an extended-regex match, JVM death or --run-timeout (default
                    21600 s). Neither is sent to the server. Then "stop" is always sent.
  --fresh           delete the world folder (server/World, or the --world one) first.
  --world <name>    use server/<name> (passed as --world <name>); default level-name=World.
  --level-type <t>  level-type for this run (default rwg, the pack's). Only affects a new world.
  --seed <seed>     level-seed for this run (default empty = random). Only affects a new world.
                    level-type and level-seed are put back to the pack's values when the run ends.
  --timeout <s>     boot timeout (default 900); the server is killed after it.
  --set-config '<file under config/>:<key>=<value>'  (repeatable) change one config value for this run; the file is
                    backed up to runs/<id>/cfg-backup/ and restored when the run ends (added for M04's cascade log).
  --view-distance <3-15>  server.properties view-distance for this run; the old value is put back when it
                    ends (added for M05S's shot runner, which matches it to the client's render distance).
  Java REALPACK_JAVA (default shared/jdk21), heap REALPACK_HEAP (default 8G, -Xms = -Xmx).
  Always -Dfml.queryResult=confirm. Kills only the PID it started (TERM, KILL after 30 s) on boot
  timeout, stop timeout (180 s) or interrupt. Exit 0 only when it booted and stopped cleanly.

  run.done keys: status (ok | died-during-boot | boot-timeout | stop-timeout+killed |
  interrupted+killed | bad-mod | error), exit_code, run_id, run_dir, log, fml_log, boot_seconds,
  peak_rss_kb (VmHWM), wall_seconds, mod, jvm_extra, until (matched|timeout|died|none), world,
  fresh, level_type, seed, view_distance (the value set, or unchanged).

  Examples
    ./run.sh --fresh                                                  # bare pack, classic RWG
    ./run.sh --fresh --world rwg2c --level-type RWG2_CONTINENTS --seed 12345
    ./run.sh --mod ../wt/main/build/libs/lostcitiesnh-<ver>.jar --cmds 'forge tps'

  Proof runs (2026-11-01, JDK 21, 8G heap, bare pack)
    --fresh, level-type=rwg: status=ok, boot 57 s launch-to-"Done (", peak RSS 8,195,880 kB
      (7.8 GiB), 73 s wall with 'list; forge tps' and stop; level.dat generatorName=RWG.
    --fresh --world rwg2c --level-type RWG2_CONTINENTS --seed 12345: status=ok, boot 43 s,
      peak RSS 6,098,088 kB; level.dat generatorName=RWG2_CONTINENTS RandomSeed=12345.
  Disk: dl 543M, server ~650M before worlds.

Real-pack client: realpack/client/, client-prepare.py, client-run.sh
  client/game/      mods/ and config/ of the GTNH 2.9.0 client pack (copied from a laptop's
                    Prism instance), and everything the client writes (logs/, options.txt, ...).
  client/instance/  mmc-pack.json, patches/*.json and libraries/ (lwjgl3ify forgePatches jar).
  client/libraries, assets, natives   fetched by client-prepare.py (Mojang, Forge maven).
  client/launch.json                  the resolved launch (java args, classpath, main class).
  python3 -I client-prepare.py client   resolve the Prism patches for linux x86_64 and fetch what is
                    missing (sha1-checked). Re-run after replacing client/instance.
  client-run.sh [--display 80-89] [--shot <name>] [--wait <s>] [--timeout <s>]
                [--server <host:port>] [--user <name>] [--heap <size>] [--extra-shot <name>:<seconds>]...
    --extra-shot takes another screenshot <seconds> after the previous one (added for M04's viewpoints).
    --mod <jar> installs a lostcitiesnh jar in client/game/mods for the run and removes it after; a server with the
    mod rejects clients without it.
    --jvm '<args>' extra client JVM args (word-split), e.g. the clean capture's -D properties.
    --hold <file> (under this root) after the shots, stays up until <file> exists, the client dies or --timeout;
    status hold-timeout / died-while-holding otherwise. shots-run.py drives the client this way.
    --restore-options backs up client/game/options.txt and optionsnf.txt into the run dir first and puts them back
    (or deletes them, if they did not exist) when the run ends. It also turns JourneyMap's waypoint beacons and
    deathpoints off for the run (game/journeymap/config/5.2/journeymap.waypoint.config, put back at the end): a
    deathpoint's beacon is a translucent red column in the world that hiding the GUI does not hide.
    Starts Xvfb on our display (default :80), runs the client with Mesa llvmpipe
    (LIBGL_ALWAYS_SOFTWARE=1, __GLX_VENDOR_LIBRARY_NAME=mesa) and OpenAL's null backend, offline
    account (default LCBot), waits for the main menu (the atlases rebuilt after "Forge Mod Loader has
    successfully loaded") or, with --server, for the first [CHAT] line after joining; waits --wait s;
    screenshots the display (xwd | convert) into runs/client-<id>/<shot>.png; stops only its JVM and
    Xvfb. run.done there: status, ready_seconds, peak_rss_kb, wall_seconds, run_dir, shot.
    client.lock allows one client at a time.
  Proof (2026-11-01): main menu in 67 s, 323/323 mods, Angelica on llvmpipe (GL 4.5), peak RSS
    10.1 GiB. Joined the real-pack server (--server 127.0.0.1:25800) in 57 s; the server's 'op' and
    'tp' applied and the world rendered. For screenshots use 'gamemode 1' and a safe y (a survival
    tp to y=120 fell). Camera pitch/yaw: /lostcities view (test hooks). Clean shots: shots-run.py below.

Shot runner: realpack/shots-run.py   (python3 -I; laptop side: lc-remote.sh shots, format in its README.md)
  python3 -I shots-run.py --mod <jar> [--shotlist <file>|-] [--label <name>] [--level-type <t>] [--seed <s>]
      [--world <name>] [--fresh|--reuse-world] [--jvm '<server args>'] [--client-jvm '<client args>']
      [--only <a,b>] [--display 80-89] [--lock-wait <s>] [--boot-timeout <s>] [--join-timeout <s>]
      [--deadline <s>] [--finds-only] [--check]
  Reads a JSON shot list (stdin by default). Waits for run.lock and client.lock to be free (45 s polls,
  --lock-wait 1800), runs run.sh (test hooks, --view-distance = render distance, --cmds
  'until:LCSHOTS-END-<id>') and waits for "Done (". Then it sends every shot's find ("lostcities find ...")
  with no client on the server: a find over a large radius holds the server thread for 20 to 30 s on
  classic RWG, and vanilla's client drops after 20 s without a packet (M06's run lost its client twice
  that way). Console finds all start at block (0, 0), so the order does not matter. A find that logs
  "[LCNH-FIND] kind=... none within" fails its shot at once. --finds-only stops here (no client).
  Then client-run.sh --hold --restore-options with -Dlostcitiesnh.capture=true (the mod's clean capture:
  GUI, hand, toast and overlays hidden, logs "[LCNH-CAPTURE] settled ..." when a viewpoint's chunks are
  in and built) and -Dlostcitiesnh.captureReadTimeout=180 (the client's read timeout, raised from 20 s).
  No --server: the client joins from the main menu itself (-Dlostcitiesnh.captureServer=127.0.0.1:25800),
  because FML's startup join races its own handshake latch ("playClientBlock is null"); the runner
  restarts the client once if it has not joined (dropped twice, exited, crashed, --join-timeout 420).
  Locks time and weather (gamerule doDaylightCycle false, time set, weather clear 1000000), creative mode,
  no mob spawning. Before each shot it checks the console for the account's last join or drop: after a
  drop it waits 60 s for the mod's own rejoin, else starts a new client (at most 2 per run), sends the
  game mode and setup again, and takes again (once) a shot the client dropped out of.
  Per shot: console "lostcities view" through run.sh's stdin fifo, wait for the settled line at the
  target, the shot's wait, then xwd | convert every 3 s until 3 frames in a row each change under 0.3%
  of a 160x90 grayscale thumbnail (stable_timeout 120 s; the last frame is the shot). Then touches the hold file, says LCSHOTS-END-<id> so
  run.sh stops the server, and TERMs either script that outlives its grace (90 s client, 300 s server).
  Writes runs/shots-<id>/: <shot>.png, results.json, index.html (review page), runner.log, server.out,
  client-<n>.out and run.done (status ok|partial|aborted, shots_ok, run_dir). --deadline (5400 s) bounds it all.
  Proof (2026-11-02, tools/remote/shots/synthetic-city.json, seed 4242, render distance 12, fresh worlds):
    RWG2_CONTINENTS 7/7 ok in 325 s wall (first aerial shot 86 s, the rest about 20 s each);
    RWG 7/7 ok in 1024 s wall including a 7 min lock wait (first aerial 226 s: Celeritas meshed
    for about 3 min on llvmpipe). Join first try from Custom Main Menu's GuiCustom. No HUD, hand,
    toast, chat, InGame Info panel or JourneyMap minimap in any frame. An interrupted run (TERM) and
    a failed join both stopped the client and server; view-distance and the option files came back.

Rebuild from scratch
  Server: delete server/, unzip dl/*.zip into server/ (python3 zipfile; no unzip on this box), redo
  the eula.txt and server.properties edits above. Client: rsync mods/ config/ and the instance
  metadata again, then python3 -I client-prepare.py client.
