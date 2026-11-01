"""Builds or queries the semantic search indexes from the command line.

  python3 -I tools/semantic-search/index.py            incremental build of both corpora (embeds only changed chunks)
  python3 -I tools/semantic-search/index.py --stats    chunk counts and an estimated full-build cost, no API calls
  python3 -I tools/semantic-search/index.py --query "how are highways placed" [-k 8] [--kind java] [--source upstream] [--path 1.20:src/]
"""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import semsearch as s  # noqa: E402

PRICE_PER_MTOK = 0.12


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--stats", action="store_true")
    ap.add_argument("--query")
    ap.add_argument("-k", type=int, default=8)
    ap.add_argument("--kind", choices=["any", "docs", "java", "python", "data", "build"], default="any")
    ap.add_argument("--source", choices=["any", "repo", "upstream"], default="any")
    ap.add_argument("--path")
    a = ap.parse_args()

    root = s.tool_root()
    main_root = s.main_root(root)
    corpora = s.corpora(main_root)

    if a.stats:
        for corpus in corpora:
            chunks = s.chunk_corpus(corpus)
            chars = sum(len(s.embed_text(c)) for c in chunks)
            print("%s: %d chunks from %d files, %.1f M chars, about $%.2f for a full build"
                  % (corpus.name, len(chunks), len({c["path"] for c in chunks}), chars / 1e6,
                     chars / 3.5 / 1e6 * PRICE_PER_MTOK))
            index = s.load_index(s.index_path(main_root, corpus.name))
            print("  index:", index["meta"] if index else "none yet")
        return 0

    key = s.load_key(root, main_root)
    if not key:
        print("OPENROUTER_API_KEY is not set (environment or .env at the repository root)", file=sys.stderr)
        return 2
    api = s.OpenRouter(key)

    if a.query:
        index = s.merge([s.load_index(s.index_path(main_root, c.name)) for c in corpora])
        if not index:
            print("no index yet; run without --query first", file=sys.stderr)
            return 1
        print(s.format_hits(s.search(index, api.query(a.query), a.k, a.path, a.kind, source=a.source)))
        return 0

    ok = True
    for corpus in corpora:
        ok = s.build(corpus, api, s.index_path(main_root, corpus.name), log=lambda m: print(m, file=sys.stderr)) and ok
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
