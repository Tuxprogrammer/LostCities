#!/usr/bin/env python3
"""Turn the block catalog's variety pools into data/lostcitiesnh/blockmap/pools.json.

The pools are written once, as tables in docs/new-spec/05-assets/block-catalog/index.md ("Variety pools"; D25,
D28, D29). This script reads every pool table there and writes the data file the mod loads, so the catalog stays
the source and the data never drifts from it. Rerun it after editing a pool, and commit both.

What it reads, per '#### <pool>' heading under '## Variety pools':
  * candidate tables (Candidate | Weight or Slots | Fallback | Flags | Why): one candidate per row;
  * rubble-set tables (Main wall candidate | Rubble set | Flags): one weightless set per row;
  * set-piece tables (Character | Baseline | Variety): one row per character, its baseline and variety blocks.
Block references are the backticked 'modid:name:meta' tokens of a cell. A bare ':N' after one names another
metadata of the same block, and ':A' to ':B' a range; a 'name:N' after one names a block of the same mod (with the
same dotted prefix), and a bare mod ID token ('TwilightForest') sets the mod for the names after it. A token
without metadata means metadata 0. Tokens with
placeholders ('<color>', '<wood>') name a family, not a block, and are left out; the row keeps its label.

Usage, from the repository root:
    python3 -I tools/asset-convert/pools_from_catalog.py [--catalog ...] [--out ...]
"""

import argparse
import json
import re
import sys

CATALOG = "docs/new-spec/05-assets/block-catalog/index.md"
OUT = "src/main/resources/data/lostcitiesnh/blockmap/pools.json"

TOKEN = re.compile(r"`([^`]+)`")
FULL = re.compile(r"^([A-Za-z0-9_|.\- ]+:[A-Za-z0-9_.\- ]+?)(?::(\d+))?$")
RANGE = re.compile(r"`:(\d+)`\s+to\s+`:(\d+)`")
FLAG = re.compile(r"^(SP|SPD|BH|TE|L\d+|G|H|XM|T|DM)$")


def slug(title):
    return re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")


def blocks_of(cell):
    """Returns the block references of a table cell, in order, each 'modid:name:meta'."""
    out = []
    last = None
    last_mod = None
    ranges = {(m.start(), int(m.group(1)), int(m.group(2))) for m in RANGE.finditer(cell)}
    range_starts = {start: (a, b) for start, a, b in ranges}
    for match in TOKEN.finditer(cell):
        token = match.group(1).strip()
        if "<" in token or ">" in token:
            last = None
            continue
        if re.fullmatch(r":\d+", token):
            if last is None:
                continue
            meta = int(token[1:])
            span = range_starts.get(match.start())
            if span:
                for m in range(span[0], span[1] + 1):
                    add(out, f"{last}:{m}")
                continue
            if any(start < match.start() < start + 40 and b == meta for start, a, b in ranges):
                continue
            add(out, f"{last}:{meta}")
            continue
        if len(token) >= 4 and re.fullmatch(r"[A-Za-z][A-Za-z0-9|]*", token) and token[0].isupper():
            # A bare mod ID ('TwilightForest'), naming the mod of shorthand names that follow.
            last_mod = token
            continue
        short = re.fullmatch(r"([A-Za-z0-9_.]+):(\d+)", token)
        if short:
            # 'name:meta' after a full reference: the same mod, and the same dotted prefix ('stonewalls' after
            # 'projectred.exploration.stone' is 'projectred.exploration.stonewalls').
            if last_mod is None:
                continue
            name = short.group(1)
            if "." not in name and last is not None and "." in last.split(":", 1)[1]:
                name = last.split(":", 1)[1].rsplit(".", 1)[0] + "." + name
            last = last_mod + ":" + name
            add(out, f"{last}:{short.group(2)}")
            continue
        full = FULL.match(token)
        if not full or token.count(":") > 2 or token.startswith("#"):
            continue
        name, meta = full.group(1), full.group(2)
        if ":" not in name or name.split(":")[0] in ("ore", "type", "name", "rwg"):
            continue
        last = name
        last_mod = name.split(":")[0]
        tail = cell[match.end():match.end() + 12]
        rng = re.match(r"\s+to\s+`:(\d+)`", tail)
        if meta is not None and rng:
            for m in range(int(meta), int(rng.group(1)) + 1):
                add(out, f"{name}:{m}")
            continue
        add(out, f"{name}:{meta or 0}")
    return out


def add(out, ref):
    if ref not in out:
        out.append(ref)


def flags_of(cell):
    out = []
    for part in re.split(r"[,;]", re.sub(r"\([^)]*\)", "", cell)):
        part = part.strip()
        if FLAG.match(part) and part not in out:
            out.append(part)
    return out


def cells(line):
    inner = line.strip()[1:-1]
    parts, depth, current = [], 0, ""
    i = 0
    while i < len(inner):
        c = inner[i]
        if c == "\\" and i + 1 < len(inner) and inner[i + 1] == "|":
            current += "|"
            i += 2
            continue
        if c == "`":
            depth ^= 1
        if c == "|" and depth == 0:
            parts.append(current.strip())
            current = ""
        else:
            current += c
        i += 1
    parts.append(current.strip())
    return parts


def plain(text):
    return re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", text).replace("`", "")


def parse(lines):
    pools = []
    in_pools = False
    group = None
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("## "):
            in_pools = line.strip() == "## Variety pools"
        elif in_pools and line.startswith("### "):
            group = line[4:].strip()
        elif in_pools and line.startswith("#### "):
            title = line[5:].strip()
            j = i + 1
            while j < len(lines) and not lines[j].startswith("|") and not lines[j].startswith("#"):
                j += 1
            if j < len(lines) and lines[j].startswith("|"):
                header = [h.lower() for h in cells(lines[j])]
                rows = []
                k = j + 2
                while k < len(lines) and lines[k].startswith("|"):
                    rows.append(cells(lines[k]))
                    k += 1
                pool = build(title, group, header, rows)
                if pool:
                    pools.append(pool)
                i = k
                continue
        i += 1
    return pools


def build(title, group, header, rows):
    pool = {"name": slug(title), "title": title, "group": group}
    if header[0] == "candidate":
        pool["candidates"] = []
        for row in rows:
            label, weight, fallback, flags = row[0], row[1], row[2], row[3]
            baseline = label.startswith("Baseline")
            entry = {"label": plain(label), "weight": int(re.sub(r"\D", "", weight) or 0), "blocks": blocks_of(label)}
            if baseline:
                entry["baseline"] = True
            if fallback.strip().lower() == "baseline":
                entry["fallbackToBaseline"] = True
            elif fallback.strip():
                entry["fallback"] = blocks_of(fallback)
            entry["flags"] = flags_of(flags)
            pool["candidates"].append(entry)
        return pool
    if header[0] == "main wall candidate":
        pool["sets"] = [{"label": plain(r[0]), "blocks": blocks_of(r[1]), "flags": flags_of(r[2])} for r in rows]
        return pool
    if header[0] == "character":
        pool["characters"] = []
        for r in rows:
            pool["characters"].append(
                {"label": plain(r[0]), "blocks": blocks_of(r[1]) + [b for b in blocks_of(r[2]) if b not in blocks_of(r[1])]})
        return pool
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--catalog", default=CATALOG)
    parser.add_argument("--out", default=OUT)
    args = parser.parse_args()
    with open(args.catalog, encoding="utf-8") as f:
        pools = parse(f.read().splitlines())
    if not pools:
        sys.exit("no pool tables found")
    doc = {
        "comment": "Generated by tools/asset-convert/pools_from_catalog.py from "
        + CATALOG
        + " ('Variety pools'). Do not edit by hand: edit the catalog and rerun the script.",
        "pools": pools,
    }
    with open(args.out, "w", encoding="utf-8", newline="") as out:
        json.dump(doc, out, indent=2, ensure_ascii=False)
        out.write("\n")
    blocks = {b for p in pools for key in ("candidates", "sets", "characters") for c in p.get(key, []) for b in c["blocks"]}
    print(f"{len(pools)} pools, {len(blocks)} distinct blocks -> {args.out}")


if __name__ == "__main__":
    main()
