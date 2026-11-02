#!/usr/bin/env python3
"""Take a shot list's screenshots on the real GTNH 2.9.0 pack, clean, and write a review page.

usage: python3 -I shots-run.py --mod <jar> [--shotlist <file>|-] [--label <name>] [--level-type <type>]
                               [--seed <seed>] [--world <name>] [--fresh|--reuse-world] [--jvm '<server args>']
                               [--client-jvm '<client args>'] [--only <name,...>] [--display <80-89>]
                               [--lock-wait <s>] [--deadline <s>] [--check]

Boots the real-pack server (run.sh) for the shot list's world with the jar and the test hooks, waits for "Done",
joins with the real-pack client (client-run.sh, clean capture on, one retry if the join fails), locks time and
weather, then for each shot moves the player with /lostcities view, waits until the client logs that the viewpoint
has settled (chunks in, terrain built), waits the shot's extra seconds and screenshots the display. Then it stops the
client and the server and writes runs/shots-<id>/: the PNGs, results.json, index.html (the review page) and run.done.
The shot list comes from --shotlist or stdin; its format is in tools/remote/README.md ("Shot lists"). --check only
validates the list and prints the plan, and needs nothing from the build server, so the laptop runs it first.
Every wait is bounded; on any exit the client and server are stopped through their own scripts.
"""
import argparse
import fcntl
import html
import json
import os
import re
import signal
import subprocess
import sys
import time

RP = os.path.dirname(os.path.abspath(__file__))
LC = os.path.dirname(RP)
SERVER_ADDRESS = "127.0.0.1:25800"   # realpack/server's server.properties (our first port)
NAME = re.compile(r"^[A-Za-z0-9_.-]+$")
TOP = re.compile(r"^top([+-]\d+)?$")
PROBE_Y = 220                         # where a top-relative shot first looks down to learn the column's top
DEFAULT_Y = "top+3"                   # a shot without y stands three blocks above its column's top block
SETTLED = re.compile(r"\[LCNH-CAPTURE\] settled x=(?P<x>-?[\d.]+) y=(?P<y>-?[\d.]+) z=(?P<z>-?[\d.]+) "
                     r"yaw=(?P<yaw>-?[\d.]+) pitch=(?P<pitch>-?[\d.]+) top=(?P<top>-?\d+) ticks=(?P<ticks>\d+)")
STATE = re.compile(r"\[LCNH-CAPTURE\] (settled|moving)")
CRASH = re.compile(r"Minecraft Crash Report|A fatal error has been detected")

LIST_DEFAULTS = {"title": "", "user": "LCBot", "time": 6000, "render_distance": 10, "fov": 70, "clouds": False,
                 "mobs": False, "server_jvm": "", "client_jvm": "", "setup": [], "settle_timeout": 300, "stable_timeout": 120,
                 "world": {}}
WORLD_DEFAULTS = {"name": "shots", "level_type": "RWG2_CONTINENTS", "seed": "", "fresh": True}
SHOT_KEYS = {"name", "caption", "x", "y", "z", "yaw", "pitch", "wait", "time", "find", "offset", "settle_timeout",
             "stable_timeout"}
THUMB = "160x90"          # frames are compared as grayscale thumbnails of this size
PIXEL_DELTA = 24          # a thumbnail pixel counts as changed past this many gray levels
STABLE_FRACTION = 0.003   # a frame is calm when under this fraction of its pixels changed
STABLE_CALM = 3           # calm frames in a row before the shot is kept
STABLE_INTERVAL = 3       # seconds between frames


class Abort(Exception):
    """Stops the run; the finally block still cleans up."""


def fail(msg):
    raise ValueError(msg)


def load_shotlist(data, args):
    """Validates a parsed shot list, fills defaults, applies the command-line overrides and returns it."""
    if not isinstance(data, dict):
        fail("the shot list must be a JSON object")
    unknown = set(data) - set(LIST_DEFAULTS) - {"shots", "comment"}
    if unknown:
        fail("unknown shot-list keys: %s" % sorted(unknown))
    out = dict(LIST_DEFAULTS)
    out.update(data)
    world = dict(WORLD_DEFAULTS)
    world.update(out["world"] or {})
    for key, value in (("level_type", args.level_type), ("seed", args.seed), ("name", args.world)):
        if value is not None:
            world[key] = value
    if args.fresh:
        world["fresh"] = True
    if args.reuse_world:
        world["fresh"] = False
    world["seed"] = str(world["seed"])
    if not NAME.match(world["name"]) or "." in world["name"]:
        fail("bad world name: %r" % world["name"])
    if not re.match(r"^[A-Za-z0-9_]+$", world["level_type"]):
        fail("bad level_type: %r" % world["level_type"])
    if world["seed"] and not re.match(r"^-?[A-Za-z0-9_]+$", world["seed"]):
        fail("bad seed: %r" % world["seed"])
    out["world"] = world
    if not re.match(r"^[A-Za-z0-9_]{3,16}$", out["user"]):
        fail("bad user: %r" % out["user"])
    if not 3 <= int(out["render_distance"]) <= 15:
        fail("render_distance must be 3 to 15 (the server's view-distance range)")
    if not 30 <= int(out["fov"]) <= 110:
        fail("fov must be 30 to 110")
    if not isinstance(out["setup"], list) or not all(isinstance(c, str) for c in out["setup"]):
        fail("setup must be a list of console commands")
    shots = data.get("shots")
    if not isinstance(shots, list) or not shots:
        fail("the shot list needs a non-empty \"shots\" array")
    seen, checked = set(), []
    for i, shot in enumerate(shots):
        checked.append(check_shot(i, shot, out, seen))
    if args.only:
        wanted = set(args.only.split(","))
        missing = wanted - seen
        if missing:
            fail("--only names no such shot: %s" % sorted(missing))
        checked = [s for s in checked if s["name"] in wanted]
    out["shots"] = checked
    return out


def check_shot(i, shot, plan, seen):
    """Validates one shot and fills its defaults."""
    where = "shot %d" % (i + 1)
    if not isinstance(shot, dict):
        fail("%s is not an object" % where)
    unknown = set(shot) - SHOT_KEYS - {"comment"}
    if unknown:
        fail("%s: unknown keys %s" % (where, sorted(unknown)))
    name = shot.get("name", "")
    if not isinstance(name, str) or not NAME.match(name) or name in seen:
        fail("%s: bad or repeated name %r" % (where, name))
    seen.add(name)
    s = {"name": name, "caption": shot.get("caption", ""), "yaw": float(shot.get("yaw", 0)),
         "pitch": float(shot.get("pitch", 30)), "wait": int(shot.get("wait", 2)), "time": shot.get("time"),
         "settle_timeout": int(shot.get("settle_timeout", plan["settle_timeout"])), "find": shot.get("find"),
         "stable_timeout": int(shot.get("stable_timeout", plan["stable_timeout"])),
         "offset": shot.get("offset", [0, 0, 0]), "yaw_set": "yaw" in shot, "pitch_set": "pitch" in shot}
    if not -360 <= s["yaw"] <= 360 or not -90 <= s["pitch"] <= 90:
        fail("%s: yaw must be -360..360 and pitch -90..90" % where)
    if not 0 <= s["wait"] <= 600 or not 10 <= s["settle_timeout"] <= 1800:
        fail("%s: wait must be 0..600 s and settle_timeout 10..1800 s" % where)
    if not 0 <= s["stable_timeout"] <= 1800:
        fail("%s: stable_timeout must be 0..1800 s" % where)
    if s["time"] is not None and not isinstance(s["time"], int):
        fail("%s: time must be an integer (ticks)" % where)
    y = shot.get("y")
    if y is None:
        pass   # a find's y if it prints one, otherwise "top+3"
    elif isinstance(y, str):
        if not TOP.match(y):
            fail("%s: y must be a number or \"top\", \"top+N\", \"top-N\"" % where)
    elif not isinstance(y, (int, float)) or not 0 <= y <= 512:
        fail("%s: y must be 0..512" % where)
    s["y"] = y
    if s["find"] is None:
        if not all(isinstance(shot.get(k), (int, float)) for k in ("x", "z")):
            fail("%s: needs numeric x and z, or a find" % where)
        s["x"], s["z"] = float(shot["x"]), float(shot["z"])
    else:
        f = s["find"]
        if not isinstance(f, dict) or not isinstance(f.get("cmd"), str) or not isinstance(f.get("match"), str):
            fail("%s: find needs a console \"cmd\" and a \"match\" regex with named groups x and z" % where)
        if "x" in shot or "z" in shot:
            fail("%s: give either x and z or a find (with an offset), not both" % where)
        try:
            groups = re.compile(f["match"]).groupindex
        except re.error as e:
            fail("%s: bad find match regex: %s" % (where, e))
        if "x" not in groups or "z" not in groups:
            fail("%s: the find match needs named groups (?P<x>...) and (?P<z>...)" % where)
        if not re.match(r"^[ -~]+$", f["cmd"]) or ";" in f["cmd"]:
            fail("%s: find cmd must be one printable console command" % where)
    off = s["offset"]
    if not (isinstance(off, list) and len(off) == 3 and all(isinstance(v, (int, float)) for v in off)):
        fail("%s: offset must be [dx, dy, dz]" % where)
    return s


class Log:
    """A growing log file read from byte offsets."""

    def __init__(self, path):
        self.path = path

    def size(self):
        try:
            return os.path.getsize(self.path)
        except OSError:
            return 0

    def text(self, start=0):
        try:
            with open(self.path, "rb") as f:
                f.seek(start)
                return f.read().decode("utf-8", "replace")
        except OSError:
            return ""


class Run:
    def __init__(self, args, plan):
        self.args, self.plan = args, plan
        stamp = time.strftime("%Y%m%d-%H%M%S")
        self.run_id = "shots-%s%s" % (stamp, "-" + args.label if args.label else "")
        self.dir = os.path.join(RP, "runs", self.run_id)
        os.makedirs(self.dir)
        self.t0 = time.time()
        self.deadline = self.t0 + args.deadline
        self.logf = open(os.path.join(self.dir, "runner.log"), "a")
        self.server = self.client = None
        self.console = self.fifo = self.client_log = None
        self.hold = os.path.join(self.dir, "client.hold")
        self.results, self.status, self.notes = [], "error", []
        self.server_run_dir = self.client_run_dir = ""

    # -- logging and helpers -------------------------------------------------------------------------------------

    def log(self, msg):
        line = "%s %s" % (time.strftime("%H:%M:%S"), msg)
        self.logf.write(line + "\n")
        self.logf.flush()
        try:
            print(line, flush=True)
        except OSError:
            pass  # the ssh session is gone; keep going and clean up

    def left(self):
        return self.deadline - time.time()

    def wait_for(self, what, test, timeout, poll=1.0):
        """Polls test() until it returns a true value, the timeout or the run deadline; returns the value or None."""
        end = min(time.time() + timeout, self.deadline)
        while True:
            value = test()
            if value:
                return value
            if time.time() >= end:
                self.log("timed out after %d s waiting for %s" % (timeout, what))
                return None
            time.sleep(poll)

    def start(self, name, argv):
        out = open(os.path.join(self.dir, name + ".out"), "w")
        self.log("start %s: %s" % (name, " ".join(argv)))
        return subprocess.Popen(argv, stdout=out, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, cwd=RP,
                                start_new_session=True)

    def out_text(self, name):
        return Log(os.path.join(self.dir, name + ".out")).text()

    def stop(self, proc, name, grace):
        """Waits for one of our scripts to exit, then TERMs it (its trap stops its JVM and Xvfb) and waits again."""
        if proc is None or proc.poll() is not None:
            return
        try:
            proc.wait(timeout=grace)
            return
        except subprocess.TimeoutExpired:
            pass
        self.log("%s still running after %d s: TERM pid %d" % (name, grace, proc.pid))
        proc.terminate()
        try:
            proc.wait(timeout=120)
        except subprocess.TimeoutExpired:
            self.log("%s ignored TERM: KILL pid %d (its JVM may need lc-remote.sh status)" % (name, proc.pid))
            proc.kill()
            proc.wait()

    # -- locks -----------------------------------------------------------------------------------------------------

    @staticmethod
    def lock_free(path):
        try:
            with open(path, "a") as f:
                fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
                fcntl.flock(f, fcntl.LOCK_UN)
            return True
        except OSError:
            return False

    def wait_for_locks(self):
        locks = [os.path.join(RP, "run.lock"), os.path.join(RP, "client.lock")]
        end = time.time() + self.args.lock_wait
        while not all(self.lock_free(p) for p in locks):
            if time.time() >= end:
                raise Abort("the real-pack server or client stayed busy for %d s" % self.args.lock_wait)
            self.log("real-pack server or client busy (another run); retrying in 45 s")
            time.sleep(45)

    # -- server ----------------------------------------------------------------------------------------------------

    def start_server(self):
        p, w = self.plan, self.plan["world"]
        jvm = " ".join(x for x in ("-Dlostcitiesnh.testHooks=true", p["server_jvm"], self.args.jvm or "") if x)
        argv = [os.path.join(RP, "run.sh"), "--mod", self.args.mod, "--jvm", jvm, "--world", w["name"],
                "--level-type", w["level_type"], "--view-distance", str(p["render_distance"]),
                "--cmds", "until:LCSHOTS-END-%s" % self.run_id, "--run-timeout", str(int(self.left()) + 60)]
        if w["seed"]:
            argv += ["--seed", w["seed"]]
        if w["fresh"]:
            argv.append("--fresh")
        for attempt in range(2):
            self.server = self.start("server", argv)
            m = self.wait_for("run.sh to start the server",
                              lambda: re.search(r"^run (\S+): started pid \d+, console (\S+)$", self.out_text("server"),
                                                re.M) or self.server.poll() is not None, 120)
            if m is True or m is None:
                rc = self.server.poll()
                if rc == 3 and attempt == 0:   # lost a race for run.lock
                    self.log("run.lock taken meanwhile; waiting for it")
                    self.wait_for_locks()
                    continue
                raise Abort("run.sh did not start the server (exit %s): %s" % (rc, self.out_text("server")[-400:]))
            break
        self.server_run_dir = os.path.dirname(m.group(2))
        self.console = Log(m.group(2))
        self.fifo = os.path.join(self.server_run_dir, "stdin.fifo")
        up = self.wait_for("the server's Done", lambda: "Done (" in self.console.text() or self.server.poll() is not None,
                           self.args.boot_timeout, poll=2)
        if up is None or self.server.poll() is not None:
            raise Abort("the server did not come up; see %s" % self.server_run_dir)
        self.log("server up (%s)" % self.server_run_dir)

    def send(self, cmd):
        """Writes one console command into run.sh's fifo; the server reads it like typed input."""
        try:
            fd = os.open(self.fifo, os.O_WRONLY | os.O_NONBLOCK)
        except OSError as e:
            raise Abort("cannot write to the server console (%s)" % e)
        try:
            os.write(fd, (cmd + "\n").encode())
        finally:
            os.close(fd)
        self.log(">> " + cmd)
        time.sleep(1)

    # -- client ----------------------------------------------------------------------------------------------------

    def client_jvm(self):
        p = self.plan
        # The mod joins from the main menu (captureServer): the launcher's --server races FML's handshake.
        props = ["-Dlostcitiesnh.testHooks=true", "-Dlostcitiesnh.capture=true",
                 "-Dlostcitiesnh.captureServer=%s" % SERVER_ADDRESS,
                 "-Dlostcitiesnh.captureFov=%d" % int(p["fov"]),
                 "-Dlostcitiesnh.captureRenderDistance=%d" % int(p["render_distance"]),
                 "-Dlostcitiesnh.captureClouds=%s" % ("true" if p["clouds"] else "false")]
        return " ".join(props + [x for x in (p["client_jvm"], self.args.client_jvm or "") if x])

    def join(self):
        """Starts the client and waits until it has joined; on a failed join, stops it and tries once more."""
        user = self.plan["user"]
        for attempt in (1, 2):
            if os.path.exists(self.hold):
                os.remove(self.hold)
            mark = self.console.size()
            argv = [os.path.join(RP, "client-run.sh"), "--display", str(self.args.display),
                    "--mod", self.args.mod, "--user", user, "--shot", "menu-%d" % attempt,
                    "--wait", "1", "--timeout", str(int(self.left())), "--hold", self.hold, "--restore-options",
                    "--jvm", self.client_jvm()]
            self.client = self.start("client-%d" % attempt, argv)
            m = self.wait_for("client-run.sh to launch the client",
                              lambda: re.search(r"^(client-\S+): client pid \d+ on :\d+, log (\S+)$",
                                                self.out_text("client-%d" % attempt), re.M)
                              or self.client.poll() is not None, 180)
            if m is True or m is None:
                rc = self.client.poll()
                self.stop(self.client, "client", 5)
                if rc == 3:
                    raise Abort("another client-run.sh holds client.lock")
                self.log("client launch failed (exit %s)" % rc)
                continue
            self.client_run_dir = os.path.dirname(m.group(2))
            self.client_log = Log(m.group(2))

            def outcome():
                new = self.console.text(mark)
                if re.search(r"\b%s joined the game" % user, new):
                    return "joined"
                if len(re.findall(r"\b%s lost connection" % user, new)) >= 2:   # the mod retries once itself
                    return "lost connection twice"
                if self.client.poll() is not None:
                    return "client exited"
                if CRASH.search(self.client_log.text()):
                    return "client crashed"
                return None

            result = self.wait_for("%s to join" % user, outcome, self.args.join_timeout, poll=2) or "join timeout"
            if result == "joined":
                drops = len(re.findall(r"\b%s lost connection" % user, self.console.text(mark)))
                if drops:
                    self.notes.append("client %d: joined after %d dropped join" % (attempt, drops))
                self.log("%s joined (client %d)" % (user, attempt))
                return
            self.log("join attempt %d failed: %s; stopping that client" % (attempt, result))
            self.notes.append("join attempt %d: %s" % (attempt, result))
            self.stop(self.client, "client", 1)   # TERM now: its trap stops the JVM and Xvfb
            self.client = None
        raise Abort("the client could not join twice")

    def last_state(self):
        """Returns the client's latest capture state line as a settled match, "moving", or None."""
        text = self.client_log.text()
        states = list(STATE.finditer(text))
        if not states:
            return None
        if states[-1].group(1) == "moving":
            return "moving"
        return SETTLED.search(text, states[-1].start())

    def wait_settled(self, x, y, z, timeout):
        """Waits until the client reports a settled viewpoint at (x, y, z); returns the match or None."""
        def at_target():
            if self.client.poll() is not None:
                raise Abort("the client died")
            st = self.last_state()
            if st is None or st == "moving":
                return None
            if abs(float(st["x"]) - x) <= 1.5 and abs(float(st["z"]) - z) <= 1.5 and abs(float(st["y"]) - y) <= 1.5:
                return st
            return None
        return self.wait_for("the viewpoint (%.1f %.1f %.1f) to settle" % (x, y, z), at_target, timeout)

    def view(self, x, y, z, yaw, pitch):
        self.send("lostcities view %s %.2f %.2f %.2f %.1f %.1f" % (self.plan["user"], x, y, z, yaw, pitch))

    def screenshot(self, name):
        path = os.path.join(self.dir, name + ".png")
        subprocess.run(["bash", "-c", '. "$1/env.sh" >/dev/null 2>&1; xwd -root -silent -display ":$2" '
                        '| convert xwd:- "$3"', "_", LC, str(self.args.display), path], timeout=120)
        return os.path.exists(path) and os.path.getsize(path) > 0

    def thumbnail(self, name):
        """Returns a screenshot as THUMB gray bytes, for comparing frames."""
        path = os.path.join(self.dir, name + ".png")
        out = subprocess.run(["bash", "-c", '. "$1/env.sh" >/dev/null 2>&1; convert "$2" -resize "$3!" '
                              '-colorspace Gray -depth 8 gray:-', "_", LC, path, THUMB],
                             stdout=subprocess.PIPE, timeout=120).stdout
        return out

    def take_stable(self, name, timeout):
        """Screenshots until the frame stops changing, and keeps the last frame as <name>.png.

        The client's settled line means every chunk has arrived and the renderer's build queue was empty, but
        Celeritas can still be meshing sections (the queue empties between scheduling rounds; on classic RWG whole
        chunks were missing 3 s after it and filled in by 40 s). So the runner also waits until STABLE_CALM frames
        in a row, STABLE_INTERVAL s apart, each differ from the one before in under STABLE_FRACTION of a THUMB
        grayscale thumbnail's pixels (by more than PIXEL_DELTA levels): chunks appearing change far more, water and
        a passing animal far less. Returns (stable, seconds, last changed fraction).
        """
        t0 = time.time()
        if timeout <= 0:   # no stability check: one frame
            return self.screenshot(name), 0, 0.0
        end = min(t0 + timeout, self.deadline)
        tmp = name + ".frame"
        prev, calm, frac = None, 0, 1.0
        while True:
            if not self.screenshot(tmp):
                return False, int(time.time() - t0), frac
            cur = self.thumbnail(tmp)
            if prev is not None and cur and len(cur) == len(prev):
                changed = sum(1 for a, b in zip(prev, cur) if abs(a - b) > PIXEL_DELTA)
                frac = changed / float(len(cur))
                calm = calm + 1 if frac < STABLE_FRACTION else 0
            prev = cur
            if calm >= STABLE_CALM or time.time() >= end:
                os.replace(os.path.join(self.dir, tmp + ".png"), os.path.join(self.dir, name + ".png"))
                return calm >= STABLE_CALM, int(time.time() - t0), frac
            time.sleep(STABLE_INTERVAL)

    # -- shots -----------------------------------------------------------------------------------------------------

    def resolve(self, shot):
        """Returns (x, y, z) for a shot, where y may still be top-relative: ("top", n)."""
        dx, dy, dz = shot["offset"]
        base_y = None
        if shot["find"] is None:
            x, z = shot["x"], shot["z"]
        else:
            f = shot["find"]
            mark = self.console.size()
            self.send(f["cmd"])
            m = self.wait_for("a line matching the find regex",
                              lambda: re.search(f["match"], self.console.text(mark), re.M),
                              int(f.get("timeout", 120)))
            if m is None:
                raise ValueError("find: no console line matched %r" % f["match"])
            x, z = float(m.group("x")), float(m.group("z"))
            base_y = float(m.group("y")) if "y" in m.re.groupindex and m.group("y") else None
            # A find may print the view direction too: named groups yaw and pitch, used when the shot sets none.
            for key in ("yaw", "pitch"):
                if key in m.re.groupindex and m.group(key) and not shot[key + "_set"]:
                    shot[key] = float(m.group(key))
            self.log("find %r -> %s" % (f["cmd"], m.group(0).strip()))
        y = shot["y"]
        if y is None:
            y = base_y if base_y is not None else DEFAULT_Y
        if isinstance(y, str):
            n = TOP.match(y).group(1)
            return x + dx, ("top", (int(n) if n else 0) + dy), z + dz
        return x + dx, float(y) + dy, z + dz

    def take(self, shot):
        r = {"name": shot["name"], "caption": shot["caption"], "status": "error", "yaw": shot["yaw"],
             "pitch": shot["pitch"]}
        self.results.append(r)
        t = time.time()
        try:
            if shot["time"] is not None:
                self.send("time set %d" % shot["time"])
            x, y, z = self.resolve(shot)
            r.update(yaw=shot["yaw"], pitch=shot["pitch"])
            if isinstance(y, tuple):
                self.view(x, PROBE_Y, z, shot["yaw"], shot["pitch"])
                st = self.wait_settled(x, PROBE_Y, z, shot["settle_timeout"])
                if st is None:
                    raise ValueError("the probe above the column never settled")
                r["top"] = int(st["top"])
                y = r["top"] + y[1]
            self.view(x, y, z, shot["yaw"], shot["pitch"])
            st = self.wait_settled(x, y, z, shot["settle_timeout"])
            r.update(x=round(x, 2), y=round(y, 2), z=round(z, 2))
            r["status"] = "ok" if st else "unsettled"
            for _ in range(shot["wait"]):
                time.sleep(1)
            stable, secs, frac = self.take_stable(shot["name"], shot["stable_timeout"])
            r["stable_seconds"], r["last_change"] = secs, round(frac, 4)
            if not os.path.exists(os.path.join(self.dir, shot["name"] + ".png")):
                r["status"] = "no-image"
            else:
                r["image"] = shot["name"] + ".png"
                if r["status"] == "ok" and not stable:
                    r["status"] = "unstable"
        except ValueError as e:
            r["status"] = "failed"
            r["error"] = str(e)
        r["seconds"] = int(time.time() - t)
        self.log("shot %s: %s in %d s" % (shot["name"], r["status"], r["seconds"]))

    def run(self):
        p = self.plan
        self.wait_for_locks()
        self.start_server()
        self.join()
        self.send("gamemode 1 %s" % p["user"])
        self.send("gamerule doDaylightCycle false")
        self.send("time set %d" % p["time"])
        self.send("weather clear 1000000")
        self.send("gamerule doMobSpawning %s" % ("true" if p["mobs"] else "false"))
        for cmd in p["setup"]:
            self.send(cmd.replace("{user}", p["user"]))
        if not self.wait_for("the client's clean capture",
                             lambda: "[LCNH-CAPTURE] active in world" in self.client_log.text(), 120):
            raise Abort("the client never reported clean capture; is the jar's client hook in it?")
        for shot in p["shots"]:
            if self.left() < 60:
                self.notes.append("run deadline reached before shot %s" % shot["name"])
                break
            self.take(shot)
        ok = all(r["status"] == "ok" for r in self.results) and len(self.results) == len(p["shots"])
        self.status = "ok" if ok else "partial"

    def cleanup(self):
        if self.client is not None:
            open(self.hold, "w").close()
            self.stop(self.client, "client", 90)
        if self.server is not None and self.server.poll() is None:
            grace = 1
            if self.fifo is not None:
                try:
                    self.send("say LCSHOTS-END-%s" % self.run_id)   # run.sh's until: sees it and sends stop
                    grace = 300
                except Abort as e:
                    self.log(str(e))
            self.stop(self.server, "server", grace)

    def report(self):
        p, w = self.plan, self.plan["world"]
        summary = {"run_id": self.run_id, "status": self.status, "title": p["title"], "world": w,
                   "mod": os.path.basename(self.args.mod), "server_run_dir": self.server_run_dir,
                   "client_run_dir": self.client_run_dir, "render_distance": p["render_distance"], "fov": p["fov"],
                   "time": p["time"], "notes": self.notes, "wall_seconds": int(time.time() - self.t0),
                   "shots": self.results}
        with open(os.path.join(self.dir, "results.json"), "w") as f:
            json.dump(summary, f, indent=1)
        with open(os.path.join(self.dir, "index.html"), "w") as f:
            f.write(review_page(summary))
        done = ["status=%s" % self.status, "run_id=%s" % self.run_id, "shots_ok=%d/%d" % (
            sum(r["status"] == "ok" for r in self.results), len(p["shots"])),
            "wall_seconds=%d" % summary["wall_seconds"], "server_run_dir=%s" % self.server_run_dir,
            "client_run_dir=%s" % self.client_run_dir, "run_dir=%s" % self.dir]
        with open(os.path.join(self.dir, "run.done"), "w") as f:
            f.write("\n".join(done) + "\n")
        self.logf.write("\n".join(done) + "\n")
        try:
            print("\n".join(done), flush=True)   # bare key=value lines: lc-remote.sh fetches run_dir
        except OSError:
            pass


def review_page(s):
    """The review page: a header with the world and the run, then one card per shot with its caption."""
    e = html.escape
    w = s["world"]
    cards = []
    for r in s["shots"]:
        img = ('<a href="%s"><img src="%s" alt="%s" loading="lazy"></a>' % (e(r["image"]), e(r["image"]), e(r["name"]))
               if r.get("image") else '<div class="missing">no image</div>')
        pos = ""
        if "x" in r:
            pos = "x %s · y %s · z %s · yaw %s · pitch %s" % (r["x"], r["y"], r["z"], r["yaw"], r["pitch"])
        flag = "" if r["status"] == "ok" else ' <span class="bad">%s</span>' % e(r["status"])
        err = '<p class="bad">%s</p>' % e(r["error"]) if r.get("error") else ""
        timing = "%d s (stable after %s s)" % (r.get("seconds", 0), r.get("stable_seconds", "?"))
        cards.append('<figure>%s<figcaption><h2>%s%s</h2><p>%s</p><p class="meta">%s · %s</p>%s</figcaption>'
                     '</figure>' % (img, e(r["name"]), flag, e(r["caption"]), e(pos), e(timing), err))
    notes = "".join("<li>%s</li>" % e(n) for n in s["notes"])
    return """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>%(title)s</title>
<style>
:root { --bg: #f6f6f4; --fg: #1d1d1b; --muted: #6b6b66; --card: #fff; --line: #ddddd8; --bad: #b3261e; }
@media (prefers-color-scheme: dark) { :root { --bg: #161615; --fg: #ecece8; --muted: #a0a09a; --card: #212120;
  --line: #34342f; --bad: #f2b8b5; } }
body { margin: 0; padding: 24px 16px; background: var(--bg); color: var(--fg);
  font: 15px/1.45 system-ui, -apple-system, "Segoe UI", sans-serif; }
header, main { max-width: 1680px; margin: 0 auto; }
h1 { font-size: 22px; margin: 0 0 4px; } h2 { font-size: 16px; margin: 0 0 4px; }
.meta, header p { color: var(--muted); font-size: 13px; margin: 2px 0; }
main { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%%, 520px), 1fr)); gap: 16px;
  margin-top: 16px; }
figure { margin: 0; background: var(--card); border: 1px solid var(--line); border-radius: 8px; overflow: hidden; }
figure img { display: block; width: 100%%; height: auto; aspect-ratio: 16 / 9; object-fit: cover; }
figcaption { padding: 10px 12px 12px; } figcaption p { margin: 2px 0; }
.missing { aspect-ratio: 16 / 9; display: grid; place-items: center; color: var(--muted); }
.bad { color: var(--bad); font-weight: 600; }
</style></head><body>
<header><h1>%(title)s</h1>
<p>%(level_type)s · seed %(seed)s · world %(world)s%(fresh)s · %(mod)s</p>
<p>%(run_id)s · %(status)s · %(n)d shots · render distance %(rd)d · fov %(fov)d · time %(time)d · %(wall)d s</p>
<ul class="meta">%(notes)s</ul></header>
<main>
%(cards)s
</main></body></html>
""" % {"title": e(s["title"] or s["run_id"]), "level_type": e(w["level_type"]), "seed": e(w["seed"] or "random"),
       "world": e(w["name"]), "fresh": " (fresh)" if w["fresh"] else "", "mod": e(s["mod"]), "run_id": e(s["run_id"]),
       "status": e(s["status"]), "n": len(s["shots"]), "rd": s["render_distance"], "fov": s["fov"],
       "time": s["time"], "wall": s["wall_seconds"], "notes": notes, "cards": "\n".join(cards)}


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--shotlist", default="-")
    ap.add_argument("--mod")
    ap.add_argument("--label", default="")
    ap.add_argument("--level-type")
    ap.add_argument("--seed")
    ap.add_argument("--world")
    ap.add_argument("--fresh", action="store_true")
    ap.add_argument("--reuse-world", action="store_true")
    ap.add_argument("--jvm", help="extra server JVM args")
    ap.add_argument("--client-jvm", help="extra client JVM args")
    ap.add_argument("--only", help="comma-separated shot names")
    ap.add_argument("--display", type=int, default=80)
    ap.add_argument("--lock-wait", type=int, default=1800)
    ap.add_argument("--boot-timeout", type=int, default=900)
    ap.add_argument("--join-timeout", type=int, default=420)
    ap.add_argument("--deadline", type=int, default=5400, help="bound on the whole run, in seconds")
    ap.add_argument("--check", action="store_true", help="validate the shot list, print the plan and exit")
    args = ap.parse_args()
    if args.label and not NAME.match(args.label):
        sys.exit("bad --label: %s" % args.label)
    if not 80 <= args.display <= 89:
        sys.exit("--display must be 80-89")
    try:
        raw = sys.stdin.read() if args.shotlist == "-" else open(args.shotlist).read()
        plan = load_shotlist(json.loads(raw), args)
    except (ValueError, OSError) as e:
        sys.exit("shot list: %s" % e)
    if args.check:
        w = plan["world"]
        print("world %s: %s seed %s%s; %d shots: %s" % (w["name"], w["level_type"], w["seed"] or "random",
              " (fresh)" if w["fresh"] else "", len(plan["shots"]), ", ".join(s["name"] for s in plan["shots"])))
        return 0
    if not args.mod or not os.path.isfile(args.mod):
        sys.exit("--mod <jar> is required and must exist")
    run = Run(args, plan)

    def on_signal(signum, _frame):
        raise Abort("signal %d" % signum)
    for sig in (signal.SIGTERM, signal.SIGINT, signal.SIGHUP):
        signal.signal(sig, on_signal)
    try:
        run.log("%s: %s, %d shots" % (run.run_id, plan["title"] or "shots", len(plan["shots"])))
        run.run()
    except Abort as e:
        run.status = "aborted"
        run.notes.append(str(e))
        run.log("abort: %s" % e)
    finally:
        for sig in (signal.SIGTERM, signal.SIGINT, signal.SIGHUP):
            signal.signal(sig, signal.SIG_IGN)   # finish cleaning up, whatever arrives now
        run.cleanup()
        run.report()
    return 0 if run.status == "ok" else 1


if __name__ == "__main__":
    sys.exit(main())
