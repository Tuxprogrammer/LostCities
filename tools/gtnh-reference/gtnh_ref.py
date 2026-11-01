"""Local GTNH reference pack: fetch, index, locate source, and (as a fallback) decompile.

Agents and contributors use this to research the APIs and configuration of GTNH and the other
mods in the pack. The rules that go with it (source first at the pack's tag, decompile only as a
fallback, never paste decompiled code, cite what you relied on) are in CONTRIBUTING.md
(GTNH integration) and the `gtnh-integration-research` skill; the target pack is set by
docs/design/0002-target-pack.md.

Everything this script writes goes under the git-ignored `reference/` folder of the *main*
checkout, so every git worktree of this repository shares one reference pack. Nothing it produces
may be committed. The downloaded pack is untrusted data: this script only reads and extracts it,
it never runs anything from it.

Standard library only. Run it with `python3 -I`, from anywhere outside `reference/`:

    python3 -I tools/gtnh-reference/gtnh_ref.py fetch [<pack-zip-url>] [--keep-archive]
    python3 -I tools/gtnh-reference/gtnh_ref.py index
    python3 -I tools/gtnh-reference/gtnh_ref.py mod <name|modid|jar>
    python3 -I tools/gtnh-reference/gtnh_ref.py where <class-or-fragment>
    python3 -I tools/gtnh-reference/gtnh_ref.py clone <name|modid|jar>
    python3 -I tools/gtnh-reference/gtnh_ref.py jars <name|modid|jar>
    python3 -I tools/gtnh-reference/gtnh_ref.py decompile <name|modid|jar|path.jar> [--class fqcn ...]
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath

# The pack this project targets (docs/design/0002-target-pack.md). `fetch` without a URL takes
# the official MultiMC client zip for this version from GTNH's download server.
PACK_VERSION = "2.9.0"
DEFAULT_PACK_URL = (
    "https://downloads.gtnewhorizons.com/Multi_mc_downloads/GT_New_Horizons_{v}_Java_17-26.zip".format(v=PACK_VERSION)
)


def _repo_root() -> Path:
    """The main checkout's root, so all worktrees share one reference/ folder.

    `git rev-parse --git-common-dir` names the main repository's .git directory even inside a
    worktree; its parent is the main checkout. Falls back to this file's own repository.
    """
    here = Path(__file__).resolve().parent
    try:
        out = subprocess.run(
            ["git", "-C", str(here), "rev-parse", "--path-format=absolute", "--git-common-dir"],
            check=True,
            capture_output=True,
            text=True,
        )
        return Path(out.stdout.strip()).parent
    except (OSError, subprocess.CalledProcessError):
        return here.parents[1]


REPO_ROOT = _repo_root()
REFERENCE = REPO_ROOT / "reference"
GTNH_DIR = REFERENCE / "gtnh"
CURRENT_FILE = GTNH_DIR / "CURRENT"
SRC_DIR = REFERENCE / "src"
MAVEN_DIR = REFERENCE / "maven"
DECOMPILED_DIR = REFERENCE / "decompiled"
TOOLS_DIR = REFERENCE / "tools"

# Hand-found source locations that GTNH's index lacks (checked in, unlike everything under reference/).
OVERRIDES_FILE = Path(__file__).resolve().parent / "source-overrides.json"

# Source categories, best first. Decompiling is refused without --force for the first two.
FULL_SOURCE = ("gtnh-github", "upstream-tag")
CLONABLE = ("gtnh-github", "upstream-tag", "upstream-branch", "api-source", "decompiled-source")

# GTNH's release tooling publishes, per pack version, the exact mod versions (manifest) and,
# across all versions, each mod's repo, license and jar filename (assets).
DAXXL_RAW = "https://raw.githubusercontent.com/GTNewHorizons/DreamAssemblerXXL/master"
MANIFEST_URL = DAXXL_RAW + "/releases/manifests/{version}.json"
ASSETS_URL = DAXXL_RAW + "/gtnh-assets.json"

# Decompiler for the fallback path. Pinned so output is reproducible; verified by SHA-1.
VINEFLOWER_VERSION = "1.12.0"
VINEFLOWER_URL = (
    "https://repo1.maven.org/maven2/org/vineflower/vineflower/{v}/vineflower-{v}.jar".format(v=VINEFLOWER_VERSION)
)

# MCP mappings for 1.7.10, as unpacked by the GTNH buildscript (RetroFuturaGradle) after a build.
MCP_CSV_DIR = Path.home() / ".gradle/caches/minecraft/de/oceanlabs/mcp/mcp_stable/12"
SRG_NAME = re.compile(r"\b(?:func|field)_\d+_[A-Za-z]+_?\b")

# The FML annotation that marks a mod class; read when a jar's mcmod.info is missing or incomplete.
FML_MOD_DESCRIPTOR = b"Lcpw/mods/fml/common/Mod;"

INDEX_JSON = "mod-index.json"
INDEX_MD = "MOD-INDEX.md"
RULES_DOC = "CONTRIBUTING.md (GTNH integration) and .claude/skills/gtnh-integration-research"


# --------------------------------------------------------------------------- helpers


def die(message: str) -> None:
    print("error: " + message, file=sys.stderr)
    sys.exit(1)


def download(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    print("downloading " + url)
    tmp = dest.with_suffix(dest.suffix + ".part")
    with urllib.request.urlopen(url) as response, open(tmp, "wb") as out:
        shutil.copyfileobj(response, out, length=1 << 20)
    tmp.replace(dest)


def current_version() -> str:
    if not CURRENT_FILE.exists():
        die("no reference pack yet; run `fetch` first (see tools/gtnh-reference/README.md)")
    return CURRENT_FILE.read_text(encoding="utf-8").strip()


def pack_dir(version: str) -> Path:
    return GTNH_DIR / version


def load_index(version: str) -> list[dict]:
    path = pack_dir(version) / INDEX_JSON
    if not path.exists():
        die("no index for " + version + "; run `index`")
    return json.loads(path.read_text(encoding="utf-8"))["mods"]


def safe_extract(zf: zipfile.ZipFile, dest: Path) -> None:
    """Extract an untrusted archive, refusing absolute paths and `..` components (zip slip)."""
    for info in zf.infolist():
        parts = PurePosixPath(info.filename.replace("\\", "/")).parts
        if info.filename.startswith(("/", "\\")) or ".." in parts or (parts and ":" in parts[0]):
            die("refusing unsafe archive entry: " + info.filename)
    zf.extractall(dest)


# --------------------------------------------------------------------------- mod IDs


def read_modids(jar: Path) -> list[str]:
    """Mod IDs from a jar: mcmod.info first, then the @Mod annotations of its classes.

    Many mcmod.info files are hand-written and not strict JSON, and some jars have none or list
    only one of several mods, so both sources are merged.
    """
    ids: set[str] = set()
    try:
        with zipfile.ZipFile(jar) as zf:
            try:
                raw = zf.read("mcmod.info").decode("utf-8", errors="replace")
                ids.update(re.findall(r'"modid"\s*:\s*"([^"]+)"', raw))
            except KeyError:
                pass
            for name in zf.namelist():
                if not name.endswith(".class"):
                    continue
                data = zf.read(name)
                if FML_MOD_DESCRIPTOR in data:
                    modid = annotation_modid(data)
                    if modid:
                        ids.add(modid)
    except zipfile.BadZipFile:
        return []
    return sorted(ids)


def annotation_modid(data: bytes) -> str | None:
    """The `modid` of a class's @cpw.mods.fml.common.Mod annotation, by parsing the class file.

    Only reads the constant pool and the class-level RuntimeVisibleAnnotations attribute; returns
    None for anything it does not understand.
    """
    try:
        return _ClassReader(data).mod_annotation_modid()
    except (struct.error, IndexError, KeyError, ValueError):
        return None


class _ClassReader:
    """Just enough of the JVM class-file format to read one annotation value."""

    def __init__(self, data: bytes) -> None:
        self.data = data
        self.pos = 8  # magic, minor, major
        self.pool: dict[int, object] = {}
        count = self.u2()
        index = 1
        while index < count:
            tag = self.u1()
            if tag == 1:
                length = self.u2()
                self.pool[index] = self.data[self.pos : self.pos + length].decode("utf-8", errors="replace")
                self.pos += length
            elif tag in (3, 4):
                self.pos += 4
            elif tag in (5, 6):
                self.pos += 8
                index += 1
            elif tag in (7, 8, 16, 19, 20):
                self.pos += 2
            elif tag in (9, 10, 11, 12, 17, 18):
                self.pos += 4
            elif tag == 15:
                self.pos += 3
            else:
                raise ValueError("unknown constant tag " + str(tag))
            index += 1

    def u1(self) -> int:
        value = self.data[self.pos]
        self.pos += 1
        return value

    def u2(self) -> int:
        value = struct.unpack_from(">H", self.data, self.pos)[0]
        self.pos += 2
        return value

    def u4(self) -> int:
        value = struct.unpack_from(">I", self.data, self.pos)[0]
        self.pos += 4
        return value

    def skip_members(self) -> None:
        for _ in range(self.u2()):
            self.pos += 6
            for _ in range(self.u2()):
                self.pos += 2
                self.pos += self.u4()

    def mod_annotation_modid(self) -> str | None:
        self.pos += 6  # access, this, super
        self.pos += 2 * self.u2()  # interfaces
        self.skip_members()  # fields
        self.skip_members()  # methods
        for _ in range(self.u2()):
            name = self.pool.get(self.u2())
            length = self.u4()
            end = self.pos + length
            if name == "RuntimeVisibleAnnotations":
                for _ in range(self.u2()):
                    found = self.annotation()
                    if found:
                        return found
            self.pos = end
        return None

    def annotation(self) -> str | None:
        type_name = self.pool.get(self.u2())
        found = None
        for _ in range(self.u2()):
            key = self.pool.get(self.u2())
            value = self.element_value()
            if type_name == FML_MOD_DESCRIPTOR.decode() and key == "modid" and isinstance(value, str):
                found = value
        return found

    def element_value(self) -> object:
        tag = chr(self.u1())
        if tag in "BCDFIJSZs":
            return self.pool.get(self.u2())
        if tag == "e":
            self.pos += 4
        elif tag == "c":
            self.pos += 2
        elif tag == "@":
            self.annotation()
        elif tag == "[":
            for _ in range(self.u2()):
                self.element_value()
        else:
            raise ValueError("unknown element tag " + tag)
        return None


def find_mod(version: str, query: str) -> dict:
    """Resolve a mod by pack name, mod ID or jar filename (case-insensitive, exact before partial)."""
    mods = load_index(version)
    q = query.lower()

    def keys(m: dict) -> list[str]:
        return [m["name"].lower(), (m["jar"] or "").lower()] + [i.lower() for i in m["modids"]]

    exact = [m for m in mods if q in keys(m)]
    if len(exact) == 1:
        return exact[0]
    partial = exact or [m for m in mods if any(q in k for k in keys(m))]
    if len(partial) == 1:
        return partial[0]
    if not partial:
        die("no mod matches '" + query + "'")
    die("'" + query + "' is ambiguous: " + ", ".join(m["name"] for m in partial[:15]))
    return {}


# --------------------------------------------------------------------------- fetch / index


def cmd_fetch(args: argparse.Namespace) -> None:
    url = args.url or DEFAULT_PACK_URL
    download_dir = GTNH_DIR / "_download"
    archive = download_dir / url.rsplit("/", 1)[-1]
    if not archive.exists():
        download(url, archive)

    with zipfile.ZipFile(archive) as zf:
        top = {n.split("/", 1)[0] for n in zf.namelist()}
        if len(top) != 1:
            die("expected one top-level folder in the archive, found " + str(sorted(top)))
        top_name = top.pop()
        match = re.match(r"GT New Horizons (.+)$", top_name)
        if not match:
            die("unexpected top-level folder '" + top_name + "'")
        version = match.group(1)
        target = pack_dir(version)
        if target.exists():
            print(str(target) + " already exists; skipping extraction")
        else:
            staging = Path(tempfile.mkdtemp(dir=GTNH_DIR, prefix="_extract-"))
            print("extracting to " + str(target))
            safe_extract(zf, staging)
            (staging / top_name).rename(target)
            staging.rmdir()

    download(MANIFEST_URL.format(version=version), target / "manifest.json")
    download(ASSETS_URL, GTNH_DIR / "gtnh-assets.json")
    CURRENT_FILE.write_text(version + "\n", encoding="utf-8")
    build_index(version)
    if not args.keep_archive:
        archive.unlink()


def cmd_index(args: argparse.Namespace) -> None:
    build_index(args.version or current_version())


def build_index(version: str) -> None:
    root = pack_dir(version)
    mods_dir = root / ".minecraft" / "mods"
    manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
    assets = json.loads((GTNH_DIR / "gtnh-assets.json").read_text(encoding="utf-8"))
    assets_by_name = {m["name"]: m for m in assets["mods"]}
    jars = {p.name.lower(): p for p in mods_dir.rglob("*.jar")}
    overrides = json.loads(OVERRIDES_FILE.read_text(encoding="utf-8"))
    claimed: set[str] = set()

    rows = []
    for group in ("github_mods", "external_mods"):
        for name, info in sorted(manifest.get(group, {}).items(), key=lambda kv: kv[0].lower()):
            mod_version = info["version"]
            asset = assets_by_name.get(name, {})
            filename = next(
                (v.get("filename") for v in asset.get("versions", []) if v.get("version_tag") == mod_version),
                None,
            )
            jar = jars.get((filename or "").lower())
            if jar is not None:
                claimed.add(jar.name.lower())
            repo = asset.get("repo_url")
            external = asset.get("external_url")
            ref = mod_version if repo else None
            if repo:
                source = "gtnh-github"
            elif external and "github.com" in external:
                # Unverified: GTNH's index names an upstream repo, but not which tag matches the jar.
                source, repo = "upstream-github", external
            else:
                source = "none-known"
            row = {
                "name": name,
                "version": mod_version,
                "jar": jar.relative_to(mods_dir).as_posix() if jar else None,
                "modids": read_modids(jar) if jar else [],
                "side": info.get("side"),
                "license": asset.get("license"),
                "source": source,
                "repo": repo,
                "tag": ref,
                "source_url": (repo + "/tree/" + ref) if repo and ref else (repo or external),
                "note": None,
                "extras": [],
            }
            apply_override(row, overrides.get(name), mod_version)
            rows.append(row)

    for key, jar in sorted(jars.items()):
        if key not in claimed:
            rows.append(
                {
                    "name": jar.stem,
                    "version": None,
                    "jar": jar.relative_to(mods_dir).as_posix(),
                    "modids": read_modids(jar),
                    "side": None,
                    "license": None,
                    "source": "unlisted",
                    "repo": None,
                    "tag": None,
                    "source_url": None,
                    "note": None,
                    "extras": [],
                }
            )

    (root / INDEX_JSON).write_text(json.dumps({"pack": version, "mods": rows}, indent=2), encoding="utf-8")
    write_index_md(root / INDEX_MD, version, rows)
    counts: dict[str, int] = {}
    for r in rows:
        counts[r["source"]] = counts.get(r["source"], 0) + 1
    print("indexed " + str(len(rows)) + " mods for " + version + ": " + json.dumps(counts))


def apply_override(row: dict, override: dict | None, mod_version: str) -> None:
    """Merge a hand-verified entry from source-overrides.json into an index row.

    An override records the mod version it was verified against; when the pack ships a different
    version, the row keeps the override but says it needs re-checking.
    """
    if not override:
        return
    if override.get("repo"):
        row["repo"] = override["repo"]
        row["tag"] = override["ref"]
        row["source_url"] = override["repo"] + "/tree/" + override["ref"]
        scope = override.get("scope", "full")
        if scope == "api":
            row["source"] = "api-source"
        elif scope == "decompiled":
            row["source"] = "decompiled-source"
        else:
            row["source"] = "upstream-tag" if override.get("exact") else "upstream-branch"
    elif override.get("scope") == "api":
        row["source"] = "api-source"
    row["note"] = override.get("note")
    verified = override.get("verified_version")
    if verified and verified != mod_version:
        stale = "override verified against " + verified + ", pack ships " + mod_version + "; re-check"
        row["note"] = (row["note"] + " " if row["note"] else "") + "(" + stale + ")"
        if row["source"] == "upstream-tag":
            row["source"] = "upstream-branch"
    row["extras"] = override.get("extras", [])


def write_index_md(path: Path, version: str, rows: list[dict]) -> None:
    lines = [
        "# GTNH " + version + " mod index",
        "",
        "Generated by `tools/gtnh-reference/gtnh_ref.py index`. Do not edit; regenerate. Rules for using it: " + RULES_DOC + ".",
        "",
        "Source values: `gtnh-github` (GTNH repo, tag = pack version), `upstream-tag` (hand-verified exact tag),",
        "`upstream-branch` (a branch or unverified ref: pin the commit you read), `api-source` (only the API is",
        "published), `decompiled-source` (third-party decompiled source: hedge comments apply), `upstream-github`",
        "(GTNH names a repo but no tag), `none-known`, `unlisted` (a jar the manifest does not list).",
        "Run `gtnh_ref.py mod <name>` for a mod's notes and extras (dev jars, API source, maven coordinates).",
        "",
        "| Mod | Version | Jar | Mod IDs | Side | License | Source | Repo | Tag |",
        "|---|---|---|---|---|---|---|---|---|",
    ]
    for r in rows:
        lines.append(
            "| {name} | {version} | {jar} | {modids} | {side} | {license} | {source} | {repo} | {tag} |".format(
                name=r["name"],
                version=r["version"] or "",
                jar=r["jar"] or "*(not in mods/)*",
                modids=", ".join(r["modids"]),
                side=r["side"] or "",
                license=(r["license"] or "").replace("|", "/"),
                source=r["source"] + (" (" + ", ".join(sorted({e["kind"] for e in r["extras"]})) + ")" if r["extras"] else ""),
                repo=r["repo"] or (r["source_url"] or ""),
                tag=r["tag"] or "",
            )
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


# --------------------------------------------------------------------------- lookups


def describe(mod: dict) -> str:
    out = [
        "mod:      " + mod["name"] + " " + (mod["version"] or ""),
        "jar:      " + str(mod["jar"]),
        "modids:   " + ", ".join(mod["modids"]),
        "license:  " + str(mod["license"]),
        "source:   " + mod["source"] + ("  " + mod["source_url"] if mod["source_url"] else ""),
    ]
    if mod.get("note"):
        out.append("note:     " + mod["note"])
    for extra in mod.get("extras", []):
        out.append("extra:    [" + extra["kind"] + "] " + extra["url"] + ("  (" + extra["note"] + ")" if extra.get("note") else ""))
    if mod["source"] in CLONABLE and mod["repo"] and mod["tag"]:
        repo = mod["repo"].replace("https://github.com/", "")
        out.append("search:   gh search code --repo " + repo + " '<symbol>'   (default branch, not the ref)")
        out.append("read:     gh api 'repos/" + repo + "/contents/<path>?ref=" + mod["tag"] + "' -H 'Accept: application/vnd.github.raw'")
        if mod["source"] in FULL_SOURCE:
            out.append("permalink " + mod["repo"] + "/blob/" + mod["tag"] + "/<path>#L<n>")
        else:
            # A branch moves; permalinks must pin the commit actually read.
            out.append("permalink " + mod["repo"] + "/blob/<commit-sha>/<path>#L<n>   (pin the commit, not the branch)")
    if mod["source"] == "decompiled-source":
        out.append("hedge:    third-party decompiled source; code relying on it needs a `// Decompiled:` comment")
    elif mod["source"] in ("upstream-github", "none-known", "unlisted"):
        out.append("fallback: no verified source; search GitHub, then `decompile` (hedge comment required)")
    return "\n".join(out)


def cmd_mod(args: argparse.Namespace) -> None:
    print(describe(find_mod(current_version(), args.query)))


def cmd_where(args: argparse.Namespace) -> None:
    """Find which jar(s) contain a class, given an FQCN, a path fragment or a simple name."""
    version = current_version()
    by_jar = {m["jar"].lower(): m for m in load_index(version) if m["jar"]}
    mods_dir = pack_dir(version) / ".minecraft" / "mods"
    needle = args.query.replace(".", "/").lower()
    hits = 0
    for jar in sorted(mods_dir.rglob("*.jar")):
        try:
            with zipfile.ZipFile(jar) as zf:
                names = [n for n in zf.namelist() if n.endswith(".class") and "$" not in n and needle in n.lower()]
        except zipfile.BadZipFile:
            continue
        if not names:
            continue
        rel = jar.relative_to(mods_dir).as_posix()
        mod = by_jar.get(rel.lower(), {})
        print(rel + "  [" + mod.get("source", "?") + "]  " + (mod.get("source_url") or ""))
        for n in names[: args.limit]:
            print("    " + n[: -len(".class")].replace("/", "."))
        if len(names) > args.limit:
            print("    ... " + str(len(names) - args.limit) + " more")
        hits += 1
    if not hits:
        print("no class matching '" + args.query + "' in the pack")


def cmd_clone(args: argparse.Namespace) -> None:
    mod = find_mod(current_version(), args.query)
    if mod["source"] not in CLONABLE or not mod["repo"]:
        die(mod["name"] + " has no verified source repo (source: " + mod["source"] + "). " + (mod["source_url"] or ""))
    dest = SRC_DIR / (mod["name"] + "@" + mod["tag"].replace("/", "_"))
    if dest.exists():
        print("already cloned: " + str(dest))
        return
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["git", "-c", "advice.detachedHead=false", "clone", "--quiet", "--depth", "1", "--branch", mod["tag"],
         mod["repo"] + ".git", str(dest)],
        check=True,
    )
    sha = subprocess.run(["git", "-C", str(dest), "rev-parse", "HEAD"], check=True, capture_output=True, text=True)
    print("cloned " + mod["repo"] + " @ " + mod["tag"] + " (" + sha.stdout.strip() + ") -> " + str(dest))


def cmd_jars(args: argparse.Namespace) -> None:
    """Download a mod's extra jars (dev jars, API source, decompiled source) into reference/maven/."""
    mod = find_mod(current_version(), args.query)
    urls = [e for e in mod.get("extras", []) if e["url"].endswith(".jar")]
    if not urls:
        die(mod["name"] + " lists no downloadable jars; see `mod " + args.query + "`")
    for extra in urls:
        dest = MAVEN_DIR / mod["name"] / extra["url"].rsplit("/", 1)[-1]
        if not dest.exists():
            download(extra["url"], dest)
        print("[" + extra["kind"] + "] " + str(dest))


# --------------------------------------------------------------------------- decompile (fallback)


def vineflower() -> Path:
    jar = TOOLS_DIR / ("vineflower-" + VINEFLOWER_VERSION + ".jar")
    if jar.exists():
        return jar
    download(VINEFLOWER_URL, jar)
    with urllib.request.urlopen(VINEFLOWER_URL + ".sha1") as response:
        expected = response.read().decode().split()[0].strip()
    actual = hashlib.sha1(jar.read_bytes()).hexdigest()
    if actual != expected:
        jar.unlink()
        die("vineflower checksum mismatch: expected " + expected + ", got " + actual)
    return jar


def load_mcp_names() -> dict[str, str]:
    names: dict[str, str] = {}
    for csv_name in ("fields.csv", "methods.csv"):
        path = MCP_CSV_DIR / csv_name
        if not path.exists():
            return {}
        with open(path, newline="", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                names[row["searge"]] = row["name"]
    return names


def cmd_decompile(args: argparse.Namespace) -> None:
    version = current_version()
    given = Path(args.query)
    if given.suffix == ".jar" and given.is_file():
        # A specific jar, typically a dev jar from `jars`: already MCP-named, so remapping is a no-op.
        jar = given.resolve()
    else:
        mod = find_mod(version, args.query)
        if not mod["jar"]:
            die(mod["name"] + " has no jar in this pack")
        if mod["source"] in FULL_SOURCE and not args.force:
            die(
                mod["name"] + " has source at " + mod["source_url"] + ". Read that instead; "
                "pass --force only if you have a concrete reason the source does not answer the question."
            )
        if any(e["kind"] == "dev-jar" for e in mod.get("extras", [])):
            print("note: " + mod["name"] + " has a dev jar (`jars " + args.query + "`); decompiling that gives cleaner output.")
        jar = pack_dir(version) / ".minecraft" / "mods" / mod["jar"]
    out = DECOMPILED_DIR / jar.stem
    out.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory(prefix="gtnh-decompile-") as tmp:
        target = jar
        if args.classes:
            # Decompile just the requested classes (and their inner classes); the full jar is a library for context.
            staging = Path(tmp) / "classes"
            with zipfile.ZipFile(jar) as zf:
                wanted = [c.replace(".", "/") for c in args.classes]
                picked = [
                    n for n in zf.namelist()
                    if n.endswith(".class") and any(n == w + ".class" or n.startswith(w + "$") for w in wanted)
                ]
                if not picked:
                    die("none of " + ", ".join(args.classes) + " found in " + jar.name)
                for n in picked:
                    zf.extract(n, staging)
            target = staging
        # Vineflower only reads the jar's bytecode; nothing from the jar is loaded or run.
        cmd = ["java", "-jar", str(vineflower()), "--silent", "-e=" + str(jar), str(target), str(out)]
        subprocess.run(cmd, check=True)

    banner = (
        "// DECOMPILED from {jar} (GTNH {pack}) with Vineflower {vf}; SRG names remapped with MCP stable_12.\n"
        "// Reference only: decompiler output can be wrong. Never copy this into the project.\n"
        "// Code that relies on it needs a `// Decompiled:` hedge comment (CONTRIBUTING.md, GTNH integration).\n"
    ).format(jar=jar.name, pack=version, vf=VINEFLOWER_VERSION)
    mcp = load_mcp_names()
    if not mcp:
        print("note: MCP CSVs not found at " + str(MCP_CSV_DIR) + "; run ./gradlew build once to unpack them."
              " Leaving func_/field_ names as they are.")
    for java in out.rglob("*.java"):
        text = java.read_text(encoding="utf-8", errors="replace")
        if text.startswith("// DECOMPILED"):
            continue
        if mcp:
            text = SRG_NAME.sub(lambda m: mcp.get(m.group(0), m.group(0)), text)
        java.write_text(banner + text, encoding="utf-8")
    print("decompiled " + jar.name + " -> " + str(out))


# --------------------------------------------------------------------------- CLI


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("fetch", help="download and extract a GTNH MultiMC pack zip, then index it")
    p.add_argument("url", nargs="?", help="pack zip URL (default: the official " + PACK_VERSION + " MultiMC zip)")
    p.add_argument("--keep-archive", action="store_true", help="keep the downloaded zip")
    p.set_defaults(func=cmd_fetch)

    p = sub.add_parser("index", help="rebuild MOD-INDEX.md and mod-index.json")
    p.add_argument("--version", help="pack version folder (default: CURRENT)")
    p.set_defaults(func=cmd_index)

    p = sub.add_parser("mod", help="show a mod's jar, mod IDs, license and where its source is")
    p.add_argument("query")
    p.set_defaults(func=cmd_mod)

    p = sub.add_parser("where", help="find which jar provides a class")
    p.add_argument("query", help="FQCN, package/path fragment or simple class name")
    p.add_argument("--limit", type=int, default=10)
    p.set_defaults(func=cmd_where)

    p = sub.add_parser("clone", help="shallow-clone a mod's source at the pack's pinned tag into reference/src/")
    p.add_argument("query")
    p.set_defaults(func=cmd_clone)

    p = sub.add_parser("jars", help="download a mod's dev / API-source / decompiled-source jars into reference/maven/")
    p.add_argument("query")
    p.set_defaults(func=cmd_jars)

    p = sub.add_parser("decompile", help="FALLBACK: decompile a mod (or a .jar path) that has no available source")
    p.add_argument("query", help="mod name, mod ID, pack jar name, or a path to a .jar")
    p.add_argument("--class", dest="classes", action="append", help="FQCN to decompile (repeatable); default: whole jar")
    p.add_argument("--force", action="store_true", help="decompile even though GTNH source exists")
    p.set_defaults(func=cmd_decompile)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
