"""run-summary.py - one screen of facts from a finished run directory (real-pack server, shot run or
budget run), so an agent never reads the console log.

Usage: python3 -I run-summary.py <run-dir> [--pattern REGEX ...]

Prints run.done (if any), counts of PASS/FAIL/ERROR/Exception lines, the first 5 FAIL or ERROR
lines, budget lines folded to min/median/max per key (lines like "MYMOD-BUDGET] terrain 12.3ms"),
and any extra pattern's last 10 matches. Standard library only.
"""
import os
import re
import statistics
import sys
from collections import Counter, defaultdict

d = sys.argv[1]
extra = [sys.argv[i + 1] for i, a in enumerate(sys.argv) if a == "--pattern"]
done = os.path.join(d, "run.done")
if os.path.exists(done):
    print("run.done:", open(done, errors="replace").read().strip().replace("\n", " | ")[:400])
logs = [os.path.join(r, f) for r, _, fs in os.walk(d) for f in fs if f.endswith((".log", ".txt")) and "crash" not in f]
crash = [os.path.join(r, f) for r, _, fs in os.walk(d) for f in fs if "crash" in f]
cnt = Counter()
first = []
budget = defaultdict(list)
ex = defaultdict(list)
BUD = re.compile(r"BUDGET\]\s*([\w.-]+)\D*?([\d.]+)\s*(ms|s|us)?")
for p in logs:
    for line in open(p, errors="replace"):
        for k in ("PASS", "FAIL", "ERROR", "Exception"):
            if k in line:
                cnt[k] += 1
                if k in ("FAIL", "ERROR") and len(first) < 5:
                    first.append(line.strip()[:200])
        m = BUD.search(line)
        if m:
            budget[m.group(1)].append(float(m.group(2)))
        for rx in extra:
            if re.search(rx, line):
                ex[rx].append(line.strip()[:200])
print("logs=%d crash-reports=%d counts=%s" % (len(logs), len(crash), dict(cnt)))
for line in first:
    print("  !", line)
for k, v in sorted(budget.items()):
    print("  budget %-24s n=%-4d min=%.1f med=%.1f max=%.1f" % (k, len(v), min(v), statistics.median(v), max(v)))
for rx, lines in ex.items():
    print("  /%s/ %d matches" % (rx, len(lines)))
    for line in lines[-10:]:
        print("    ", line)
