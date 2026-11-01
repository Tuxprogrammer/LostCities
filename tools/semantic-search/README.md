# Semantic search

Gives Claude Code a `semantic_search` tool over two sources:

- **`repo`:** this repository: specs, design records, skills, tool READMEs, Java sources and Python tools.
- **`upstream`:** McJty's Lost Cities at the local `1.20` branch, the code and assets this mod backports: Java, the JSON asset system (buildings, parts, palettes, styles, profiles), and its notes and plans.

It finds files by meaning, not by exact name. Use it when you don't know which file or identifier to look for, such as "how does upstream decide whether a chunk is part of a city" or "where are highway routes planned". Use grep for exact names: `git grep <pattern> 1.20` searches upstream without a checkout.

## How it works

- **Corpora.**
  - `repo`: every tracked `.md`, `.java`, `.py`, `.gradle`, `.properties`, `.toml` and `.json` file, except `LICENSES/`. Only tracked files are sent, so untracked notes never leave the machine.
  - `upstream`: every `.java`, `.md`, `.json`, `.toml`, `.properties`, `.gradle` and `.txt` file at `1.20`, read straight from git's object store. No checkout is needed.
  - Upstream paths are written `1.20:<path>`, so `git show 1.20:<path>` opens a hit as is.
- **Chunks.**
  - Markdown is split at headings, and each chunk keeps its heading path.
  - Java is split at the members of each top-level type, ignoring braces inside strings and comments.
  - Python is split at top-level `def` and `class`.
  - Anything else, JSON included, is split by size.
  - Small pieces are merged and long ones split, so a chunk holds at most 6,000 characters. Each chunk is embedded with its path and title.
- **Embeddings.**
  - Model: `voyageai/voyage-code-4` (1,024 dimensions) through OpenRouter, with `input_type` set to `document` or `query` and provider data collection denied.
  - Cost: a full build of upstream is about 1,300 chunks, roughly $0.07. A refresh embeds only chunks whose text changed.
- **Indexes.**
  - One file per source, in `reference/semantic-search/` in the main working tree: `repo-…npz` and `upstream-…npz`.
  - They are git-ignored, written atomically, and shared by every worktree.
  - Vectors are normalized, so a search is one matrix product.
- **Ranking.**
  - Cosine similarity, with at most three hits per file.
  - The code model scores Java above the prose that explains it, so a mixed search keeps about a third of its slots for docs.
  - Use `kind` to search one kind only, and `source` to search one source only.

## Setup

1. Put an OpenRouter key in `.env` at the repository root (git-ignored): `OPENROUTER_API_KEY=...`. The `OPENROUTER_API_KEY` environment variable wins if it is set.
2. Make sure the upstream branch exists locally: `git branch --list 1.20`. To pull McJty's latest, run `git fetch upstream 1.20 && git branch -f 1.20 upstream/1.20` (or check out `1.20` and fast-forward it). The next refresh embeds only what changed.
3. Build the first indexes: `python3 -I tools/semantic-search/index.py`.
4. [`.mcp.json`](../../.mcp.json) registers the server for Claude Code. Approve it once on each machine: either accept the prompt when `claude` starts, or add `"enabledMcpjsonServers": ["semantic-search"]` to the git-ignored `.claude/settings.local.json`.

The server refreshes both indexes in the background when it starts, and again when a search runs more than 10 minutes after the last refresh. A lock file keeps concurrent sessions from building at the same time.

## Tools exposed to Claude Code

| Tool | What it does |
|---|---|
| `semantic_search` | `query` (natural language), optional `k` (1–25, default 8), `kind` (`any`, `docs`, `java`, `python`, `data`, `build`), `source` (`any`, `repo`, `upstream`) and `path` prefix (`docs/design/`, `1.20:src/main/java/`). Returns `[n] path:start-end · score · title` and a snippet of up to 1,500 characters |
| `semantic_index_status` | Per source: chunk and file counts, the commit the index was built at compared with the current one, and whether a refresh is running |
| `semantic_reindex` | Refreshes both indexes now and waits for it to finish |

## Command line

```sh
python3 -I tools/semantic-search/index.py                  # incremental build of both sources
python3 -I tools/semantic-search/index.py --stats          # chunk counts and estimated cost, no API calls
python3 -I tools/semantic-search/index.py --query "how are highway routes planned" -k 5 --source upstream --kind java
python3 -I tools/semantic-search/test_semsearch.py         # offline tests (no API calls)
```

## Limits

- The `repo` index follows the main working tree. A worktree's uncommitted edits are not in it, so read the file a hit points to before acting on it.
- The `upstream` index follows the local `1.20` branch, not the remote. Update the branch to see McJty's newer commits.
- Scores compare well within a single kind, but across kinds they are only a rough guide.
- Changing `MODEL` in `semsearch.py` starts new index files with a full re-embed.
