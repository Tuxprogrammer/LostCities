"""Draw today's rarecities city centers beside jittered-grid centers, with stats.

Usage: python3 -I tools/preview/jitter_preview.py <centers.csv> <out.png> [stats.md]

The CSV (set, chunkX, chunkZ, radiusBlocks) comes from JitteredGridPreviewDump.
"""
import csv
import math
import sys

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Circle

WIN = (-512, 512)
LINK_K = 3
LINK_MAX = 160  # chunks
GAP_STEP = 4  # chunks, sampling grid for the largest empty circle

INK = "#1f2933"
MUTED = "#6b7280"
CITY = "#2f6fa8"
ISOLATED = "#d9822b"
BG = "#ffffff"


def load(path):
    sets = {"today": [], "jitter": []}
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            sets[row["set"]].append((int(row["chunkX"]), int(row["chunkZ"]), int(row["radiusBlocks"])))
    return sets


def links(pts):
    """Symmetric union of each center's LINK_K nearest centers within LINK_MAX chunks."""
    out = set()
    linked = set()
    for i, (x, z, _) in enumerate(pts):
        near = sorted((math.hypot(x - u, z - v), j) for j, (u, v, _) in enumerate(pts) if j != i)
        for d, j in near[:LINK_K]:
            if d <= LINK_MAX:
                out.add((min(i, j), max(i, j)))
                linked.add(i)
                linked.add(j)
    return out, linked


def stats(pts):
    n = len(pts)
    nn = [min(math.hypot(x - u, z - v) for j, (u, v, _) in enumerate(pts) if j != i) for i, (x, z, _) in enumerate(pts)]
    overlap = sum(
        1
        for i in range(n)
        for j in range(i + 1, n)
        if math.hypot(pts[i][0] - pts[j][0], pts[i][1] - pts[j][1]) * 16 < pts[i][2] + pts[j][2]
    )
    edges, linked = links(pts)
    # Largest empty circle: sample the window on a grid, take the point farthest from any center.
    best = (0.0, 0, 0)
    for gx in range(WIN[0], WIN[1] + 1, GAP_STEP):
        for gz in range(WIN[0], WIN[1] + 1, GAP_STEP):
            d = min(math.hypot(gx - x, gz - z) for x, z, _ in pts)
            if d > best[0]:
                best = (d, gx, gz)
    return {
        "count": n,
        "mean_nn": sum(nn) / n,
        "min_nn": min(nn),
        "overlap": overlap,
        "isolated": n - len(linked),
        "gap": best,
        "edges": edges,
        "linked": linked,
    }


def panel(ax, pts, s, title):
    ax.set_facecolor(BG)
    for x, z, r in pts:
        ax.add_patch(Circle((x, z), r / 16.0, facecolor=CITY, alpha=0.18, edgecolor=CITY, linewidth=0.6))
    for i, j in s["edges"]:
        ax.plot([pts[i][0], pts[j][0]], [pts[i][1], pts[j][1]], color=MUTED, linewidth=0.6, alpha=0.8)
    for i, (x, z, _) in enumerate(pts):
        iso = i not in s["linked"]
        ax.plot(x, z, "o", color=ISOLATED if iso else INK, markersize=3.5, markeredgecolor=BG, markeredgewidth=0.5)
    d, gx, gz = s["gap"]
    ax.add_patch(Circle((gx, gz), d, fill=False, edgecolor=ISOLATED, linestyle="--", linewidth=1.0))
    ax.set_xlim(*WIN)
    ax.set_ylim(*WIN)
    ax.set_aspect("equal")
    ax.invert_yaxis()  # +z south, as on a map
    ax.set_title(title, color=INK, fontsize=12, loc="left")
    ax.tick_params(colors=MUTED, labelsize=8)
    for sp in ax.spines.values():
        sp.set_color("#d1d5db")
    ax.set_xlabel("chunk X", color=MUTED, fontsize=9)
    ax.set_ylabel("chunk Z", color=MUTED, fontsize=9)


def lines(s):
    return [
        f"{s['count']} centers",
        f"nearest-neighbor distance: mean {s['mean_nn']:.1f}, min {s['min_nn']:.1f} chunks",
        f"overlapping city pairs: {s['overlap']}",
        f"isolated centers (no link): {s['isolated']}",
        f"largest empty circle: radius {s['gap'][0]:.0f} chunks at ({s['gap'][1]}, {s['gap'][2]})",
    ]


def main(csv_path, out_png, out_md=None):
    sets = load(csv_path)
    res = {k: stats(v) for k, v in sets.items()}
    fig, axes = plt.subplots(1, 2, figsize=(14, 10), facecolor=BG)
    panel(axes[0], sets["today"], res["today"], "Today: random per-chunk centers")
    panel(axes[1], sets["jitter"], res["jitter"], "Jittered grid: one center per 64x64 cell")
    for ax, k in zip(axes, ("today", "jitter")):
        ax.text(0.0, -0.1, "\n".join(lines(res[k])), transform=ax.transAxes, va="top", ha="left", fontsize=9.5, color=INK)
    fig.suptitle(
        "rarecities city centers, seed 5253073417701929942, chunks -512..511",
        color=INK,
        fontsize=13,
        x=0.05,
        ha="left",
    )
    fig.text(
        0.05,
        0.015,
        "Circles at true radius (100-256 blocks); gray links = 3 nearest within 160 chunks; orange dot = no link; "
        "dashed orange = largest empty circle (4-chunk sampling, window edges count).\n"
        "Terrain filters (cityAvoidOcean, height) are NOT applied to either panel. Centers outside the window are not considered.",
        fontsize=8.5,
        color=MUTED,
        ha="left",
        va="bottom",
    )
    fig.subplots_adjust(left=0.05, right=0.98, top=0.92, bottom=0.27, wspace=0.15)
    fig.savefig(out_png, dpi=110, facecolor=BG)
    if out_md:
        with open(out_md, "w") as f:
            f.write("# rarecities center preview stats\n\nSeed 5253073417701929942, window chunks -512..511, no terrain filters.\n")
            for k, name in (("today", "Today"), ("jitter", "Jittered grid")):
                f.write(f"\n## {name}\n\n" + "\n".join(f"- {line}" for line in lines(res[k])) + "\n")
    for k in res:
        print(k, "; ".join(lines(res[k])))


if __name__ == "__main__":
    main(*sys.argv[1:4])
