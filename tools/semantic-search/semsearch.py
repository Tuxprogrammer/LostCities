"""Semantic search over Lost Cities: New Horizons and the upstream Lost Cities branch it backports: corpora, chunking,
embedding and the on-disk indexes."""

import fcntl
import hashlib
import json
import os
import re
import subprocess
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import numpy as np

MODEL = "voyageai/voyage-code-4"
API_URL = "https://openrouter.ai/api/v1/embeddings"
MAX_CHARS = 6000
MIN_CHARS = 400
SNIPPET_CHARS = 1500
BATCH_INPUTS = 64
BATCH_CHARS = 200_000
WORKERS = 4
KINDS = {".md": "docs", ".java": "java", ".py": "python", ".json": "data"}

# The upstream branch this mod backports. The local branch is updated from McJty's repository; see the README.
UPSTREAM_REF = "1.20"
REPO_SUFFIXES = {".md", ".java", ".py", ".gradle", ".properties", ".toml", ".json"}
UPSTREAM_SUFFIXES = {".md", ".java", ".json", ".toml", ".properties", ".gradle", ".txt"}
SKIP_PREFIXES = ("LICENSES/",)


def tool_root():
    return Path(__file__).resolve().parents[2]


def main_root(root):
    """The main working tree, so every worktree session shares one set of indexes."""
    out = subprocess.run(
        ["git", "-C", str(root), "rev-parse", "--path-format=absolute", "--git-common-dir"],
        capture_output=True, text=True, check=True,
    ).stdout.strip()
    return Path(out).parent


def index_path(main, corpus_name):
    return main / "reference" / "semantic-search" / (corpus_name + "-" + MODEL.replace("/", "_") + ".npz")


def rev_parse(root, rev="HEAD"):
    r = subprocess.run(["git", "-C", str(root), "rev-parse", "--verify", "-q", rev + "^{commit}"],
                       capture_output=True, text=True)
    return r.stdout.strip()


def load_key(*roots):
    if os.environ.get("OPENROUTER_API_KEY"):
        return os.environ["OPENROUTER_API_KEY"].strip()
    for root in roots:
        env = Path(root) / ".env"
        if env.is_file():
            for line in env.read_text(encoding="utf-8").splitlines():
                name, _, value = line.partition("=")
                if name.strip() == "OPENROUTER_API_KEY" and value.strip():
                    return value.strip().strip("'\"")
    return None


# Corpora

class RepoCorpus:
    """This repository's working tree. Tracked files only: untracked files may hold notes or secrets that should not
    leave the machine."""

    name = "repo"

    def __init__(self, root):
        self.root = Path(root)

    def commit(self):
        return rev_parse(self.root)

    def files(self):
        out = subprocess.run(["git", "-C", str(self.root), "ls-files", "-z"], capture_output=True, check=True).stdout
        files = []
        for rel in out.decode("utf-8").split("\0"):
            if not rel or rel.startswith(SKIP_PREFIXES) or Path(rel).suffix not in REPO_SUFFIXES:
                continue
            if (self.root / rel).is_file():
                files.append(rel)
        return sorted(files)

    def texts(self):
        for rel in self.files():
            yield rel, (self.root / rel).read_text(encoding="utf-8", errors="replace")


class RefCorpus:
    """Every supported file at a git ref, read from the object store, so no checkout is needed. Chunk paths are
    written `<ref>:<path>`, which `git show` accepts as is."""

    name = "upstream"

    def __init__(self, root, ref=UPSTREAM_REF, suffixes=UPSTREAM_SUFFIXES):
        self.root, self.ref, self.suffixes = Path(root), ref, suffixes

    def commit(self):
        return rev_parse(self.root, self.ref)

    def files(self):
        commit = self.commit()
        if not commit:
            raise RuntimeError("git ref %r not found; fetch it first (see tools/semantic-search/README.md)" % self.ref)
        out = subprocess.run(["git", "-C", str(self.root), "ls-tree", "-r", "-z", "--name-only", commit],
                             capture_output=True, check=True).stdout
        return sorted(rel for rel in out.decode("utf-8").split("\0") if rel and Path(rel).suffix in self.suffixes)

    def texts(self):
        commit, files = self.commit(), self.files()
        if not files:
            return
        request = "".join("%s:%s\n" % (commit, rel) for rel in files).encode("utf-8")
        out = subprocess.run(["git", "-C", str(self.root), "cat-file", "--batch"], input=request,
                             capture_output=True, check=True).stdout
        pos = 0
        for rel in files:
            header_end = out.index(b"\n", pos)
            size = int(out[pos:header_end].split()[2])
            body = out[header_end + 1: header_end + 1 + size]
            pos = header_end + 1 + size + 1
            yield self.ref + ":" + rel, body.decode("utf-8", errors="replace")


def corpora(main):
    return [RepoCorpus(main), RefCorpus(main)]


def kind_of(path):
    return KINDS.get(Path(path).suffix, "build")


def chunk_file(path, text):
    suffix = Path(path).suffix
    if suffix == ".md":
        spans = markdown_spans(text)
    elif suffix == ".java":
        spans = java_spans(text, path)
    elif suffix == ".py":
        spans = python_spans(text)
    else:
        spans = [(0, len(text.split("\n")), "")]
    return finish(path, text.split("\n"), spans)


def finish(path, lines, spans):
    """Merges small spans, splits large ones, and returns chunk dicts with 1-based inclusive line ranges."""
    merged = []
    for start, end, title in spans:
        size = sum(len(l) + 1 for l in lines[start:end])
        if merged:
            ps, pe, pt, psize = merged[-1]
            if pe == start and (psize < MIN_CHARS or size < MIN_CHARS) and psize + size <= MAX_CHARS:
                merged[-1] = (ps, end, pt or title, psize + size)
                continue
        merged.append((start, end, title, size))
    chunks = []
    for start, end, title, size in merged:
        pieces = [(start, end)] if size <= MAX_CHARS else pack_lines(lines, start, end)
        for s, e in pieces:
            text = "\n".join(lines[s:e]).strip("\n")
            if not text.strip():
                continue
            if len(text) > MAX_CHARS:
                text = text[:MAX_CHARS]
            chunks.append({
                "path": path, "start": s + 1, "end": e, "title": title,
                "kind": kind_of(path), "text": text,
            })
    for c in chunks:
        c["id"] = hashlib.sha256((MODEL + "\0" + embed_text(c)).encode("utf-8")).hexdigest()[:32]
    return chunks


def pack_lines(lines, start, end, limit=MAX_CHARS):
    pieces, s, size = [], start, 0
    for i in range(start, end):
        n = len(lines[i]) + 1
        if size and size + n > limit:
            pieces.append((s, i))
            s, size = i, 0
        size += n
    if s < end:
        pieces.append((s, end))
    return pieces


def embed_text(chunk):
    head = chunk["path"] + (" :: " + chunk["title"] if chunk["title"] else "")
    return head + "\n\n" + chunk["text"]


HEADING = re.compile(r"^(#{1,6})\s+(.*?)\s*#*\s*$")


def markdown_spans(text):
    lines = text.split("\n")
    spans, crumbs, start, title, fence = [], [], 0, "", False
    for i, line in enumerate(lines):
        if line.lstrip().startswith(("```", "~~~")):
            fence = not fence
        m = None if fence else HEADING.match(line)
        if not m:
            continue
        if i > start:
            spans.append((start, i, title))
        level = len(m.group(1))
        crumbs = crumbs[: level - 1] + [m.group(2)]
        title, start = " > ".join(crumbs), i
    spans.append((start, len(lines), title))
    return spans


def java_boundaries(text):
    """Line indexes after which a member of a top-level type ends (depth back to 1 at ';' or '}')."""
    ends, depth, paren, i, line, n = [], 0, 0, 0, 0, len(text)
    while i < n:
        c = text[i]
        if c == "\n":
            line += 1
        elif text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
            continue
        elif text.startswith("/*", i):
            j = text.find("*/", i + 2)
            j = n if j < 0 else j + 2
            line += text.count("\n", i, j)
            i = j
            continue
        elif text.startswith('"""', i):
            j = text.find('"""', i + 3)
            j = n if j < 0 else j + 3
            line += text.count("\n", i, j)
            i = j
            continue
        elif c in "\"'":
            j = i + 1
            while j < n and text[j] != c and text[j] != "\n":
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        elif c == "(":
            paren += 1
        elif c == ")":
            paren = max(0, paren - 1)
        elif c == "{":
            depth += 1
            if depth == 1:
                ends.append(line)
        elif c == "}":
            depth = max(0, depth - 1)
            if depth <= 1 and paren == 0:
                ends.append(line)
        elif c == ";" and depth == 1 and paren == 0:
            ends.append(line)
        i += 1
    return sorted(set(ends))


TYPE_DECL = re.compile(r"^[ \t]*(?:(?:public|protected|private|abstract|static|final|strictfp|sealed)\s+)*(class|interface|enum|record|@interface)\s+(\w+)", re.M)


def java_spans(text, path):
    lines = text.split("\n")
    m = TYPE_DECL.search(text)
    type_name = m.group(2) if m else Path(path).stem
    spans, start = [], 0
    for end_line in java_boundaries(text):
        if end_line + 1 > start:
            spans.append((start, end_line + 1))
            start = end_line + 1
    if start < len(lines):
        spans.append((start, len(lines)))
    out = []
    for s, e in spans:
        sig = member_signature(lines[s:e])
        out.append((s, e, type_name + (": " + sig if sig else "")))
    return out


def member_signature(lines):
    for raw in lines:
        l = raw.strip()
        if not l or l.startswith(("//", "/*", "*", "@", "import ", "package ")) or l in "{}":
            continue
        return l.rstrip("{").strip()[:120]
    return ""


PY_TOP = re.compile(r"^(def |class |async def |@)")


def python_spans(text):
    lines = text.split("\n")
    starts = [0]
    for i, l in enumerate(lines):
        if i and PY_TOP.match(l) and not lines[i - 1].startswith("@"):
            starts.append(i)
    starts.append(len(lines))
    spans = []
    for s, e in zip(starts, starts[1:]):
        sig = next((l.strip()[:120] for l in lines[s:e] if PY_TOP.match(l) and not l.startswith("@")), "")
        spans.append((s, e, sig))
    return spans


def chunk_corpus(corpus):
    chunks = []
    for rel, text in corpus.texts():
        for c in chunk_file(rel, text):
            c["source"] = corpus.name
            chunks.append(c)
    return chunks


# Embedding

class OpenRouter:
    def __init__(self, key, model=MODEL):
        self.key, self.model, self.cost, self.tokens = key, model, 0.0, 0

    def _post(self, inputs, input_type):
        body = json.dumps({
            "model": self.model, "input": inputs, "input_type": input_type,
            "provider": {"data_collection": "deny"},
        }).encode("utf-8")
        for attempt in range(7):
            req = urllib.request.Request(API_URL, data=body, headers={
                "Authorization": "Bearer " + self.key, "Content-Type": "application/json",
                "X-Title": "Lost Cities: New Horizons semantic search",
            })
            try:
                with urllib.request.urlopen(req, timeout=180) as f:
                    data = json.load(f)
                if "data" not in data or len(data["data"]) != len(inputs):
                    raise RuntimeError("bad embedding response: " + json.dumps(data)[:300])
                usage = data.get("usage") or {}
                self.cost += usage.get("cost") or 0.0
                self.tokens += usage.get("total_tokens") or 0
                rows = sorted(data["data"], key=lambda d: d["index"])
                return [r["embedding"] for r in rows]
            except urllib.error.HTTPError as e:
                detail = e.read().decode("utf-8", "replace")[:300]
                if e.code not in (408, 429, 500, 502, 503, 504, 524, 529) or attempt == 6:
                    raise RuntimeError("OpenRouter HTTP %d: %s" % (e.code, detail)) from None
            except (urllib.error.URLError, TimeoutError, RuntimeError):
                if attempt == 6:
                    raise
            time.sleep(min(60, 2 ** attempt))

    def documents(self, texts):
        batches, cur, size = [], [], 0
        for t in texts:
            if cur and (len(cur) >= BATCH_INPUTS or size + len(t) > BATCH_CHARS):
                batches.append(cur)
                cur, size = [], 0
            cur.append(t)
            size += len(t)
        if cur:
            batches.append(cur)
        with ThreadPoolExecutor(WORKERS) as pool:
            results = list(pool.map(lambda b: self._post(b, "document"), batches))
        return [v for r in results for v in r]

    def query(self, text):
        return self._post([text], "query")[0]


def normalize(m):
    m = np.asarray(m, dtype=np.float32)
    norms = np.linalg.norm(m, axis=-1, keepdims=True)
    return m / np.where(norms == 0, 1, norms)


# Index

def _json_array(obj):
    return np.frombuffer(json.dumps(obj).encode("utf-8"), dtype=np.uint8)


def save_index(path, chunks, vectors, meta):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(".tmp.npz")
    np.savez(tmp, vectors=vectors.astype(np.float32), chunks=_json_array(chunks), meta=_json_array(meta))
    os.replace(tmp, path)


def load_index(path):
    if not path.is_file():
        return None
    with np.load(path, allow_pickle=False) as z:
        return {
            "vectors": z["vectors"],
            "chunks": json.loads(z["chunks"].tobytes().decode("utf-8")),
            "meta": json.loads(z["meta"].tobytes().decode("utf-8")),
        }


def build(corpus, embedder, path, log=lambda s: None):
    """Re-chunks one corpus and embeds only chunks whose text changed. Returns the new index, or None if another build
    holds the lock."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path.with_suffix(".lock"), "w") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            log("%s: another build is running; skipped" % corpus.name)
            return None
        t0 = time.time()
        old = load_index(path)
        known = {}
        if old:
            for c, v in zip(old["chunks"], old["vectors"]):
                known[c["id"]] = v
        chunks = chunk_corpus(corpus)
        need, seen = [], set()
        for c in chunks:
            if c["id"] not in known and c["id"] not in seen:
                seen.add(c["id"])
                need.append(c)
        log("%s: %d chunks, %d to embed" % (corpus.name, len(chunks), len(need)))
        if need:
            for c, v in zip(need, embedder.documents([embed_text(c) for c in need])):
                known[c["id"]] = np.asarray(v, dtype=np.float32)
        vectors = normalize(np.stack([known[c["id"]] for c in chunks])) if chunks else np.zeros((0, 1), np.float32)
        meta = {
            "model": MODEL, "source": corpus.name, "commit": corpus.commit(), "built_at": int(time.time()),
            "chunks": len(chunks), "files": len({c["path"] for c in chunks}),
            "embedded": len(need), "cost": round(getattr(embedder, "cost", 0.0), 6),
            "seconds": round(time.time() - t0, 1),
        }
        index = {"vectors": vectors, "chunks": chunks, "meta": meta}
        save_index(path, chunks, vectors, meta)
        log("%s: saved %s (%d embedded, $%.4f)" % (corpus.name, path, len(need), meta["cost"]))
        return index


def merge(indexes):
    """One searchable index from several corpus indexes (None entries are skipped)."""
    present = [i for i in indexes if i and i["chunks"]]
    if not present:
        return None
    return {
        "chunks": [c for i in present for c in i["chunks"]],
        "vectors": np.concatenate([i["vectors"] for i in present]),
    }


def search(index, qvec, k=8, path_prefix=None, kind=None, per_file=3, source=None):
    """Top k chunks by cosine, at most per_file from one file. A mixed search keeps about a third of the slots for
    docs, because the code model scores Java above the prose that explains it. `source` is `repo`, `upstream` or
    None/`any` for both."""
    chunks, vectors = index["chunks"], index["vectors"]
    if not chunks:
        return []
    scores = vectors @ normalize(qvec)
    mixed = not kind or kind == "any"
    one_source = source and source != "any"
    docs_min = (k + 2) // 3 if mixed else 0
    picked, per = [], {}

    def take(want_docs, limit):
        for i in np.argsort(-scores):
            if len(picked) >= limit:
                return
            c = chunks[i]
            if path_prefix and not c["path"].startswith(path_prefix):
                continue
            if one_source and c.get("source", "repo") != source:
                continue
            if not mixed and c["kind"] != kind:
                continue
            if want_docs and c["kind"] != "docs":
                continue
            if i in picked or per.get(c["path"], 0) >= per_file:
                continue
            per[c["path"]] = per.get(c["path"], 0) + 1
            picked.append(i)

    take(True, docs_min)
    take(False, k)
    picked.sort(key=lambda i: -scores[i])
    return [dict(chunks[i], score=float(scores[i])) for i in picked]


def format_hits(hits, snippet=SNIPPET_CHARS):
    if not hits:
        return "No matches."
    out = []
    for n, h in enumerate(hits, 1):
        text = h["text"]
        if len(text) > snippet:
            text = text[:snippet].rstrip() + "\n[… truncated; read the file for the rest]"
        title = (" · " + h["title"]) if h["title"] else ""
        out.append("[%d] %s:%d-%d · score %.3f%s\n%s" % (n, h["path"], h["start"], h["end"], h["score"], title, text))
    return "\n\n---\n\n".join(out)
