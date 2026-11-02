#!/usr/bin/env python3
"""Write docs/original-spec/BUGS-COVERAGE.md from BUGS.md, the test sources and the reasons below.

Every BUGS.md ID gets its doesNotReproduce<ID>_... tests from src/test/java, or a reason from the table here (code
not ported, feature dropped or deferred, with the decision). An ID with neither is listed as MISSING.

Usage, from the repository root:
    python3 -I tools/bugs-coverage/generate.py .
"""
import os, re, sys

root = sys.argv[1] if len(sys.argv) > 1 else '.'
bugs = open(root + '/docs/original-spec/BUGS.md').read().splitlines()

tests = {}
for dirpath, _, names in os.walk(root + '/src/test/java'):
    for n in names:
        if not n.endswith('.java'):
            continue
        for m in re.finditer(r'void (doesNotReproduce([A-Z]+)(\d+)_\w+)\(', open(os.path.join(dirpath, n)).read()):
            key = m.group(2) + '-' + m.group(3)
            tests.setdefault(key, []).append(n[:-5] + '#' + m.group(1))

D12 = 'Sphere and cavern landscapes are not ported before beta (D12; new-spec 03-terrain, "Landscapes and spheres")'
UI = ('The upstream world-creation screens (profile screen, preview, config widgets) are deferred or dropped; '
      'profiles are chosen through `dimensionProfiles` and GTNHLib\'s config screen ([0005](../design/0005-rwg-overlay.md) §7, D14, D31; new-spec 09-client-ui)')
M06 = 'Milestone M06 (highways and railways) is still landing; its regression tests land with its code'
reasons = {
    'CORE-07': ('cannot occur', 'No structure-ID avoidance list: landmarks come from the structure oracles, and RWG has no witch huts ([0006](../design/0006-build-around-other-worldgen.md) §3, D26)'),
    'CORE-18': ('cannot occur', 'The bed teleporter is not ported: no Lost City dimension ([0003](../design/0003-world-presence.md), D12)'),
    'CORE-19': ('cannot occur', 'The bed teleporter is not ported ([0003](../design/0003-world-presence.md), D12)'),
    'CORE-39': ('cannot occur', '`specialBedBlock` and the bed teleporter are not ported ([0003](../design/0003-world-presence.md), D12)'),
    'CORE-23': ('cannot occur', '`WorldTools.checkStructures` is dead code upstream and is not ported (new-spec 01-core, utilities row)'),
    'CORE-25': ('kept', 'Kept on purpose: the same-seed octaves shape the city distribution upstream was tuned on (D16); `SameSeedOctavesTest` pins the kept behavior'),
    'PROF-20': ('cannot occur', 'The Lost City dimension is not ported ([0003](../design/0003-world-presence.md), D12)'),
    'UI-07': ('cannot occur', 'No client profile state, so there is no client cleanup to hook; the only logout handler is the server\'s edit-session cleanup ([0005](../design/0005-rwg-overlay.md) §7)'),
    'UI-09': ('cannot occur', 'The Create World button is deferred and upstream\'s textures are not shipped ([0001](../design/0001-port-policy-and-licensing.md) §5, D31)'),
    'TERR-01': ('cannot occur', D12),
    'TERR-02': ('cannot occur', D12 + '; every cache is bounded and owned by its world (D9)'),
    'TERR-12': ('cannot occur', 'No neighbor-shape correction: 1.7.10 connects fences, panes and walls at render time and has no stair shapes (API-MAP; new-spec 03-terrain); cross-chunk writes are rejected (TERR-14\'s test)'),
    'TERR-13': ('cannot occur', 'No raise-only height map update: after the terrain phase the overlay rebuilds the whole height map with vanilla `generateSkylightMap` ([0005](../design/0005-rwg-overlay.md) §2, D122)'),
    'TERR-15': ('cannot occur', 'No staging buffer and no commit: the chunk buffer writes straight into the chunk\'s sections ([0005](../design/0005-rwg-overlay.md) §2, D6)'),
    'TERR-16': ('cannot occur', D12),
    'TERR-17': ('cannot occur', D12),
    'TERR-21': ('cannot occur', '`GlobalTodo` is not ported: no tick-time job queue, and saplings are never grown at generation (P03-10, D31)'),
    'TERR-22': ('cannot occur', D12),
    'TERR-35': ('cannot occur', D12),
    'TERR-36': ('cannot occur', 'No staged-only predicate writes: reads see the chunk\'s real blocks, so a predicate tests every block (D6, [0005](../design/0005-rwg-overlay.md) §2)'),
    'CITY-23': ('cannot occur', 'Legacy bridges and the `LEGACY` street mode are not ported (D11)'),
    'CITY-29': ('cannot occur', '`Transform.randomRotation` is dead code upstream and is not ported (new-spec 04-city-layout)'),
    'CITY-33': ('cannot occur', 'The GUI preview is not ported with the world-preset screen ([0005](../design/0005-rwg-overlay.md) §7)'),
    'HWY-04': ('cannot occur', 'Legacy noise highways are not ported (D11)'),
    'HWY-05': ('cannot occur', 'Legacy noise highways are not ported (D11)'),
    'HWY-11': ('cannot occur', 'No saved hub data: highways are a pure function of seed, coordinates and profile (D11)'),
    'HWY-14': ('cannot occur', 'No planning thread pool: the highway planner runs on the server thread (D11)'),
}
for i in (1, 2, 3, 4, 5, 6, 8, 12, 13, 14, 15):
    reasons['UI-%02d' % i] = ('cannot occur', UI)
for i in ('HWY-01', 'HWY-02', 'HWY-06', 'HWY-08', 'HWY-09', 'HWY-10', 'HWY-15'):
    reasons[i] = ('M06', M06)

rows = []
severity = None
counts = {}
for line in bugs:
    m = re.match(r'^## (\w[\w ]*)$', line)
    if m:
        severity = m.group(1).lower()
        continue
    m = re.match(r'^\| ([A-Z]+-\d+) \| ([^|]*)\|', line)
    if not m:
        continue
    bug, also = m.group(1), m.group(2).strip()
    if severity == 'not bugs':
        status, what = 'not a bug', 'Checked and not a bug ([BUGS.md](BUGS.md#not-bugs)); no test needed'
    elif bug in tests:
        status, what = 'tested', '<br>'.join('`' + t + '`' for t in sorted(tests[bug]))
    elif bug in reasons:
        status, what = reasons[bug]
    else:
        status, what = 'MISSING', ''
    counts[status] = counts.get(status, 0) + 1
    rows.append('| %s | %s | %s | %s | %s |' % (bug, also, severity, status, what))

out = ['# Bug coverage', '',
       '<!-- Generated by tools/bugs-coverage/generate.py from BUGS.md and the test sources; rerun it when a bug gets a test or a reason. -->',
       '',
       'Every ID in [BUGS.md](BUGS.md) and how this mod keeps it out: a regression test named `doesNotReproduce<ID>_…`, '
       'or the reason the bug cannot occur here (the code is not ported, or the feature is dropped or deferred), with the '
       'decision that says so. Aliases in **Also** are the same bug under another area\'s ID and share the row.',
       '',
       '| Status | IDs |', '|---|---|']
for k in ('tested', 'cannot occur', 'kept', 'M06', 'not a bug', 'MISSING'):
    if k in counts:
        out.append('| %s | %d |' % (k, counts[k]))
out += ['',
        '- **tested**: a `doesNotReproduce` test proves the bug does not come back.',
        '- **cannot occur**: the upstream code path is not in this mod; the reason cites the decision.',
        '- **kept**: recorded on purpose and pinned by a test of the kept behavior (D16).',
        '- **M06**: highway and railway bugs whose code is still landing with milestone M06.',
        '',
        '| ID | Also | Severity | Status | Test, or why it cannot occur |',
        '|---|---|---|---|---|'] + rows
open(root + '/docs/original-spec/BUGS-COVERAGE.md', 'w').write('\n'.join(out) + '\n')
print(counts)
