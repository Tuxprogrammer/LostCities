"""MCP server (stdio, JSON-RPC 2.0) giving Claude Code semantic search over this repository and the upstream branch. Standard library plus numpy."""

import json
import sys
import threading
import time
import traceback
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import semsearch as s  # noqa: E402

REFRESH_AFTER = 600

INSTRUCTIONS = (
    "Semantic search over Lost Cities: New Horizons (source 'repo': docs, design records, skills, tool READMEs, Java "
    "and Python, split by heading or class member) and over the upstream Lost Cities branch it backports (source "
    "'upstream': McJty's 1.20 Java, JSON assets such as buildings, parts and palettes, and its notes). Upstream hits "
    "are written '1.20:<path>'; read them with `git show 1.20:<path>`. Use semantic_search for conceptual questions "
    "when you do not know the identifier or file: where upstream implements a behavior, which spec sheet covers it, "
    "why a design is the way it is. Use grep (or `git grep 1.20`) for exact names. Each hit is a path:line range; "
    "read the file before acting on it. The repo index follows the main working tree, so a worktree's uncommitted "
    "edits are not in it."
)

TOOLS = [
    {
        "name": "semantic_search",
        "description": (
            "Search this repository and the upstream 1.20 branch by meaning. Returns the best-matching chunks as "
            "path:start-end with a score and a snippet. Phrase the query as a question or description, e.g. 'how "
            "does upstream decide where a city sphere is' or 'decision about worldgen dimension support'."
        ),
        "inputSchema": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "What you are looking for, in natural language."},
                "k": {"type": "integer", "minimum": 1, "maximum": 25, "default": 8, "description": "Number of hits."},
                "kind": {"type": "string", "enum": ["any", "docs", "java", "python", "data", "build"], "default": "any",
                         "description": "Restrict to Markdown docs, Java, Python tools, JSON data or build files."},
                "source": {"type": "string", "enum": ["any", "repo", "upstream"], "default": "any",
                           "description": "This repository, the upstream 1.20 branch, or both."},
                "path": {"type": "string", "description": "Only paths starting with this prefix, e.g. 'docs/design/' or '1.20:src/main/java/'."},
            },
            "required": ["query"],
        },
    },
    {
        "name": "semantic_index_status",
        "description": "Shows each index's size, the commit it was built at and whether a refresh is running.",
        "inputSchema": {"type": "object", "properties": {}},
    },
    {
        "name": "semantic_reindex",
        "description": "Refreshes both indexes now, embedding only chunks that changed. Usually unnecessary: the server refreshes itself in the background.",
        "inputSchema": {"type": "object", "properties": {}},
    },
]


def log(msg):
    print("[semantic-search] " + msg, file=sys.stderr, flush=True)


class Service:
    def __init__(self, corpora, embedder, paths):
        """`corpora` and `paths` are parallel lists: one index file per corpus."""
        self.corpora, self.embedder, self.paths = corpora, embedder, paths
        self.indexes = [s.load_index(p) for p in paths]
        self.index = s.merge(self.indexes)
        self.lock = threading.Lock()
        self.refreshing = False
        self.last_refresh = 0.0
        self.last_error = None

    def refresh(self):
        with self.lock:
            if self.refreshing or self.embedder is None:
                return False
            self.refreshing, self.last_refresh = True, time.time()
        errors = []
        try:
            for n, (corpus, path) in enumerate(zip(self.corpora, self.paths)):
                try:
                    built = s.build(corpus, self.embedder, path, log)
                    self.indexes[n] = built or s.load_index(path) or self.indexes[n]
                except Exception as e:  # a failed refresh must not take the server down
                    errors.append("%s: %s" % (corpus.name, e))
                    log("refresh of %s failed: %s" % (corpus.name, e))
            self.index = s.merge(self.indexes)
            self.last_error = "; ".join(errors) or None
        finally:
            self.refreshing = False
        return True

    def refresh_in_background(self):
        if not self.refreshing and time.time() - self.last_refresh > REFRESH_AFTER:
            threading.Thread(target=self.refresh, daemon=True).start()

    def status(self):
        lines = []
        for corpus, index, path in zip(self.corpora, self.indexes, self.paths):
            if index:
                m = index["meta"]
                built = time.strftime("%Y-%m-%d %H:%M", time.localtime(m["built_at"]))
                current = corpus.commit()
                lines.append("%s: %d chunks from %d files, model %s; built at %s, %s; now %s (%s)" % (
                    corpus.name, m["chunks"], m["files"], m["model"], m["commit"][:10], built, current[:10],
                    "same" if current == m["commit"] else "differs; refresh pending or running"))
            else:
                lines.append("%s: no index yet at %s." % (corpus.name, path))
        lines.append("Refresh running: %s" % ("yes" if self.refreshing else "no"))
        if self.embedder is None:
            lines.append("OPENROUTER_API_KEY is not set (environment or .env at the repository root); search is unavailable.")
        if self.last_error:
            lines.append("Last refresh error: " + self.last_error)
        return "\n".join(lines)

    def call(self, name, args):
        if name == "semantic_index_status":
            return self.status(), False
        if name == "semantic_reindex":
            if not self.refresh():
                return "A refresh is already running or no API key is set.\n" + self.status(), True
            return self.status(), bool(self.last_error)
        if name == "semantic_search":
            query = (args.get("query") or "").strip()
            if not query:
                return "query is required", True
            if self.embedder is None:
                return self.status(), True
            if self.index is None:
                if not self.refreshing:
                    threading.Thread(target=self.refresh, daemon=True).start()
                return "The index is being built for the first time (a few minutes). Try again shortly, or use grep.", True
            self.refresh_in_background()
            k = max(1, min(25, int(args.get("k") or 8)))
            hits = s.search(self.index, self.embedder.query(query), k, args.get("path"), args.get("kind"),
                            source=args.get("source"))
            return s.format_hits(hits), False
        return "unknown tool: %s" % name, True


def handle(service, msg):
    """Returns the JSON-RPC response for one message, or None for notifications."""
    method, mid, params = msg.get("method"), msg.get("id"), msg.get("params") or {}
    if mid is None:
        return None
    if method == "initialize":
        result = {
            "protocolVersion": params.get("protocolVersion", "2025-06-18"),
            "capabilities": {"tools": {}},
            "serverInfo": {"name": "lostcitiesnh-semantic-search", "version": "1.0.0"},
            "instructions": INSTRUCTIONS,
        }
    elif method == "ping":
        result = {}
    elif method == "tools/list":
        result = {"tools": TOOLS}
    elif method == "tools/call":
        try:
            text, is_error = service.call(params.get("name"), params.get("arguments") or {})
        except Exception as e:
            log(traceback.format_exc())
            text, is_error = "error: %s" % e, True
        result = {"content": [{"type": "text", "text": text}], "isError": is_error}
    else:
        return {"jsonrpc": "2.0", "id": mid, "error": {"code": -32601, "message": "method not found: %s" % method}}
    return {"jsonrpc": "2.0", "id": mid, "result": result}


def main():
    root = s.tool_root()
    main_root = s.main_root(root)
    key = s.load_key(root, main_root)
    corpora = s.corpora(main_root)
    service = Service(corpora, s.OpenRouter(key) if key else None, [s.index_path(main_root, c.name) for c in corpora])
    if key:
        threading.Thread(target=service.refresh, daemon=True).start()
    out_lock = threading.Lock()
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            msg = json.loads(line)
        except ValueError:
            continue
        reply = handle(service, msg)
        if reply is not None:
            with out_lock:
                sys.stdout.write(json.dumps(reply) + "\n")
                sys.stdout.flush()


if __name__ == "__main__":
    main()
