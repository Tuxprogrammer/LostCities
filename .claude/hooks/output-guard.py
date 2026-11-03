"""output-guard.py - PreToolUse hook for Bash and Read that blocks the expensive patterns seen in
agent transcripts, with a reason the agent can act on:
  - Read of a spilled tool-results file without offset/limit (paid twice: spill + whole re-read);
  - `cat` of two or more .md files in one command, or of the decision log / roadmap;
  - Read of an image when a contact sheet exists next to it (optional, CONTACT_SHEETS=1).
Exit code 2 with a message on stderr blocks the call and shows the message to the model (the
PreToolUse convention; verify against your Claude Code version before relying on it).

Install: see .claude/README.md and .claude/settings.example.json.
"""
import json
import os
import re
import sys

try:
    p = json.load(sys.stdin)
except ValueError:
    sys.exit(0)
name = p.get("tool_name")
inp = p.get("tool_input") or {}


def block(msg):
    sys.stderr.write(msg + "\n")
    sys.exit(2)


if name == "Read":
    fp = inp.get("file_path", "")
    if "/tool-results/" in fp and not inp.get("limit"):
        block("Spilled output: grep it (Bash: grep -n PATTERN FILE | head -40) or Read with offset/limit; do not read it whole.")
    if os.environ.get("CONTACT_SHEETS") == "1" and re.search(r"\.(png|jpe?g)$", fp, re.I):
        sheets = os.path.join(os.path.dirname(fp), "sheets")
        if os.path.isdir(sheets) and "/sheets/" not in fp:
            block("A contact sheet exists in %s; read that first and open single shots only for suspects." % sheets)
elif name == "Bash":
    cmd = inp.get("command", "")
    for seg in re.split(r"[;&|]+", cmd):
        if re.match(r"\s*cat\b", seg):
            mds = re.findall(r"\S+\.md\b", seg)
            if len(mds) >= 2:
                block("cat of %d docs in one command will spill. Read one section at a time: grep -n '^#' FILE, then sed -n 'A,Bp' FILE." % len(mds))
            if any(re.search(r"decision-log\.md|ROADMAP\.md", m) for m in mds):
                block("Do not cat the decision log or roadmap; grep -n for the rows or anchor you need.")
sys.exit(0)
