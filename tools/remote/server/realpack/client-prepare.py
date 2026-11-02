"""Build a launchable GTNH 2.9.0 client from a Prism instance's metadata, without Prism.

usage: python3 -I client-prepare.py <client-dir>

<client-dir>/instance/ holds mmc-pack.json, patches/*.json and libraries/ (the lwjgl3ify forgePatches jar)
copied from the pack's Prism instance; <client-dir>/game/ holds mods/ and config/. This script downloads
every library the patches name for linux x86_64 into <client-dir>/libraries (maven layout), the 1.7.10
client jar, and the asset index and objects into <client-dir>/assets, extracts old-style natives into
<client-dir>/natives, and writes <client-dir>/launch.json (java args, main class, game args).
Sha1s from the metadata are checked. Re-running only fetches what is missing.
"""
import concurrent.futures, hashlib, json, os, sys, urllib.request, zipfile

root = os.path.abspath(sys.argv[1])
inst = os.path.join(root, "instance")
libdir, assets, natives = (os.path.join(root, d) for d in ("libraries", "assets", "natives"))

def fetch(url, dest, sha1=None):
    if os.path.exists(dest) and (not sha1 or hashlib.sha1(open(dest, "rb").read()).hexdigest() == sha1):
        return dest
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    tmp = dest + ".part"
    req = urllib.request.Request(url, headers={"User-Agent": "lostcities-dev-client-prepare/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r, open(tmp, "wb") as f:
            f.write(r.read())
    except Exception as e:
        raise RuntimeError("%s: %s" % (url, e))
    if sha1 and hashlib.sha1(open(tmp, "rb").read()).hexdigest() != sha1:
        os.remove(tmp); raise RuntimeError("sha1 mismatch: " + url)
    os.replace(tmp, dest)
    return dest

def allowed(lib):
    rules = lib.get("rules")
    if not rules:
        return True
    ok = False
    for r in rules:
        osname = (r.get("os") or {}).get("name")
        if osname is None or osname == "linux":
            ok = r["action"] == "allow"
    return ok

def maven_path(name, classifier=None):
    parts = name.split(":")
    g, a, v = parts[0], parts[1], parts[2]
    c = classifier or (parts[3] if len(parts) > 3 else None)
    fn = "%s-%s%s.jar" % (a, v, "-" + c if c else "")
    return os.path.join(g.replace(".", "/"), a, v, fn)

patches = [json.load(open(os.path.join(inst, "patches", f))) for f in os.listdir(os.path.join(inst, "patches"))]
patches.sort(key=lambda p: p.get("order", 0))
libs, keys, jvm_args, tweakers, main_class, game_args, main_jar, asset_index = [], {}, [], [], None, None, None, None
for p in patches:
    for lib in p.get("libraries", []) + p.get("+libraries", []):
        if not allowed(lib):
            continue
        parts = lib["name"].split(":")
        key = (parts[0], parts[1], parts[3] if len(parts) > 3 else "")
        if key in keys:
            libs[keys[key]] = lib   # a later component replaces the same library in place, as Prism does
        else:
            keys[key] = len(libs); libs.append(lib)
    jvm_args += p.get("+jvmArgs", [])
    tweakers += p.get("+tweakers", [])
    main_class = p.get("mainClass", main_class)
    game_args = p.get("minecraftArguments", game_args)
    main_jar = p.get("mainJar", main_jar)
    asset_index = p.get("assetIndex", asset_index)

jobs, classpath, native_jars = [], [], []
for lib in libs:
    dl = lib.get("downloads") or {}
    if lib.get("MMC-hint") == "local":
        parts = lib["name"].split(":")
        classpath.append(os.path.join(inst, "libraries", "%s-%s-%s.jar" % (parts[1], parts[2], parts[3])))
        continue
    if "natives" in lib:
        cls = lib["natives"].get("linux")
        if cls:
            art = dl["classifiers"][cls]
            dest = os.path.join(libdir, maven_path(lib["name"], cls))
            jobs.append((art["url"], dest, art.get("sha1"))); native_jars.append(dest)
        continue
    art = dl.get("artifact")
    if not art:
        url = lib.get("url", "https://libraries.minecraft.net/").rstrip("/") + "/" + maven_path(lib["name"])
        art = {"url": url}
    dest = os.path.join(libdir, maven_path(lib["name"]))
    jobs.append((art["url"], dest, art.get("sha1"))); classpath.append(dest)
mj = main_jar["downloads"]["artifact"]
mj_dest = os.path.join(libdir, maven_path(main_jar["name"]))
jobs.append((mj["url"], mj_dest, mj["sha1"])); classpath.append(mj_dest)

idx_path = fetch(asset_index["url"], os.path.join(assets, "indexes", asset_index["id"] + ".json"), asset_index["sha1"])
for obj in json.load(open(idx_path))["objects"].values():
    h = obj["hash"]
    jobs.append(("https://resources.download.minecraft.net/%s/%s" % (h[:2], h), os.path.join(assets, "objects", h[:2], h), h))

with concurrent.futures.ThreadPoolExecutor(16) as ex:
    list(ex.map(lambda j: fetch(*j), jobs))

os.makedirs(natives, exist_ok=True)
for nj in native_jars:
    with zipfile.ZipFile(nj) as z:
        for n in z.namelist():
            if not n.startswith("META-INF") and not n.endswith("/") and "/" not in n:
                open(os.path.join(natives, n), "wb").write(z.read(n))

missing = [c for c in classpath if not os.path.exists(c)]
if missing:
    sys.exit("missing classpath entries: %s" % missing)
json.dump({"mainClass": main_class, "jvmArgs": jvm_args, "classpath": classpath, "tweakers": tweakers,
           "gameArgs": game_args, "assetIndex": asset_index["id"]}, open(os.path.join(root, "launch.json"), "w"), indent=1)
print("libraries=%d assets=%d mainClass=%s" % (len(classpath), len(jobs) - len(classpath), main_class))
