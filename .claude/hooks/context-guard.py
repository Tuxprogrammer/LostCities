"""context-guard.py - PostToolUse hook: tell the agent its context size and when to hand off.

Reads the hook payload on stdin (uses `transcript_path`), takes the usage of the last assistant
message from the end of the transcript, and, above a threshold, injects a short note through
`additionalContext`. Agents cannot otherwise see their own context size, which is why the
"hand off at 200k" rule in the agent-context-budget skill is otherwise easy to miss.

Install: see .claude/README.md and .claude/settings.example.json. Test on one session first; the
output JSON shape follows the documented hookSpecificOutput form - verify against your Claude Code version.
Env: CTX_WARN (default 150000), CTX_STOP (default 200000).
"""
import json
import os
import sys


def last_usage(path, tail_bytes=400000):
    try:
        with open(path, "rb") as f:
            f.seek(0, 2)
            size = f.tell()
            f.seek(max(0, size - tail_bytes))
            lines = f.read().decode("utf-8", "replace").splitlines()
    except OSError:
        return None
    for line in reversed(lines):
        if '"usage"' not in line:
            continue
        try:
            d = json.loads(line)
        except ValueError:
            continue
        u = (d.get("message") or {}).get("usage")
        if d.get("type") == "assistant" and u:
            return u
    return None


def main():
    try:
        payload = json.load(sys.stdin)
    except ValueError:
        return
    u = last_usage(payload.get("transcript_path") or "")
    if not u:
        return
    ctx = u.get("input_tokens", 0) + u.get("cache_read_input_tokens", 0) + u.get("cache_creation_input_tokens", 0)
    warn = int(os.environ.get("CTX_WARN", "150000"))
    stop = int(os.environ.get("CTX_STOP", "200000"))
    if ctx < warn:
        return
    if ctx >= stop:
        msg = ("Context is %dk tokens (limit %dk). Every call now re-reads all of it. Finish the current "
               "step, write your handoff file (skill agent-context-budget) and stop." % (ctx // 1000, stop // 1000))
    else:
        msg = "Context is %dk tokens; hand off at %dk. Avoid large reads and long waits." % (ctx // 1000, stop // 1000)
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PostToolUse", "additionalContext": msg}}))


if __name__ == "__main__":
    main()
