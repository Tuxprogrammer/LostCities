"""context-guard.py - PostToolUse hook: tell the agent its context size, and when to hand off.

Reads the hook payload on stdin (uses `transcript_path`, `cwd`, and `agent_id` / `agent_type` when a
subagent fires it, reading the subagent's own transcript), takes the usage of the last assistant message from the end of the transcript, and,
above a threshold, injects a short note through `additionalContext`. Agents cannot otherwise see
their own context size.

Autocompaction changes the advice. When it is on and fires at or before CTX_STOP, Claude Code
summarizes the conversation before the limit is reached, so a handoff is unnecessary. The note is
then informational only and never says "hand off" or "stop". The hook keeps the assertive handoff
note only when autocompaction is off or fires after CTX_STOP, since that is the case the
agent-context-budget skill's "hand off at 200k" rule exists for.

Autocompaction is detected the way Claude Code resolves it, with the most specific source winning:
  enabled: env CTX_AUTOCOMPACT (1/0, override) > env DISABLE_AUTO_COMPACT / DISABLE_COMPACT
           (process env or a settings "env" block) > "autoCompactEnabled" in settings or
           ~/.claude.json > on by default.
  window:  the subagent's .claude/agents/<agent_type>.md frontmatter "autoCompactWindow" >
           .claude/settings.local.json > .claude/settings.json > ~/.claude/settings.json >
           CTX_MODEL_WINDOW (default 200000); scaled by CLAUDE_AUTOCOMPACT_PCT_OVERRIDE if set.

Install: see .claude/README.md and .claude/settings.example.json. The output follows the documented
hookSpecificOutput form. Env: CTX_WARN (default 150000), CTX_STOP (default 200000),
CTX_AUTOCOMPACT, CTX_MODEL_WINDOW. Stdlib only; run with `python3 -I`.
"""
import json
import os
import re
import sys

FALSY = ("0", "false", "no", "off", "")


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


def read_json(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            d = json.load(f)
        return d if isinstance(d, dict) else {}
    except (OSError, ValueError):
        return {}


def truthy(v):
    return str(v).strip().lower() not in FALSY


def agent_window(project, agent_type):
    """autoCompactWindow from a subagent definition's frontmatter, or None."""
    if not agent_type or not re.match(r"^[A-Za-z0-9_.-]+$", agent_type):
        return None
    try:
        with open(os.path.join(project, ".claude", "agents", agent_type + ".md"), "r", encoding="utf-8") as f:
            text = f.read(4000)
    except OSError:
        return None
    m = re.match(r"^---\s*\n(.*?)\n---", text, re.S)
    if not m:
        return None
    w = re.search(r"^autoCompactWindow:\s*(\d+)\s*$", m.group(1), re.M)
    return int(w.group(1)) if w else None


def autocompact(payload):
    """Return (enabled, trigger_tokens) as best as can be told from env and settings."""
    home = os.path.expanduser("~")
    project = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd") or os.getcwd()
    # Lowest precedence first; later sources override earlier ones.
    sources = [
        read_json(os.path.join(home, ".claude.json")),
        read_json(os.path.join(home, ".claude", "settings.json")),
        read_json(os.path.join(project, ".claude", "settings.json")),
        read_json(os.path.join(project, ".claude", "settings.local.json")),
    ]
    enabled, window, env = True, None, {}
    for s in sources:
        if "autoCompactEnabled" in s:
            enabled = bool(s["autoCompactEnabled"])
        if isinstance(s.get("autoCompactWindow"), int):
            window = s["autoCompactWindow"]
        if isinstance(s.get("env"), dict):
            env.update(s["env"])
    env.update(os.environ)
    if truthy(env.get("DISABLE_AUTO_COMPACT", "")) or truthy(env.get("DISABLE_COMPACT", "")):
        enabled = False
    if "CTX_AUTOCOMPACT" in os.environ:
        enabled = truthy(os.environ["CTX_AUTOCOMPACT"])
    window = agent_window(project, payload.get("agent_type")) or window
    if window is None:
        window = int(os.environ.get("CTX_MODEL_WINDOW", "200000"))
    try:
        pct = float(env.get("CLAUDE_AUTOCOMPACT_PCT_OVERRIDE", ""))
        if 0 < pct < 100:
            window = int(window * pct / 100)
    except ValueError:
        pass
    return enabled, window


def transcript(payload):
    """The firing agent's own transcript.

    For a subagent's tool call the payload's ``transcript_path`` is the parent session's, so reading
    it would report the parent's context as the subagent's (three agents handed off at once on that
    misreading). A subagent's own transcript is ``<session dir>/subagents/agent-<agent_id>.jsonl``.
    """
    path = payload.get("transcript_path") or ""
    agent_id = payload.get("agent_id")
    if path and agent_id and re.match(r"^[A-Za-z0-9_-]+$", str(agent_id)):
        own = os.path.join(path[:-len(".jsonl")] if path.endswith(".jsonl") else path,
                           "subagents", "agent-%s.jsonl" % agent_id)
        # Never fall back to the parent's transcript for a subagent: say nothing rather than mislead.
        return own if os.path.exists(own) else ""
    return path


def main():
    try:
        payload = json.load(sys.stdin)
    except ValueError:
        return
    u = last_usage(transcript(payload))
    if not u:
        return
    ctx = u.get("input_tokens", 0) + u.get("cache_read_input_tokens", 0) + u.get("cache_creation_input_tokens", 0)
    warn = int(os.environ.get("CTX_WARN", "150000"))
    stop = int(os.environ.get("CTX_STOP", "200000"))
    if ctx < warn:
        return
    enabled, trigger = autocompact(payload)
    k = lambda n: n // 1000
    if enabled and trigger <= stop:
        # Compaction arrives first: inform, never push a handoff.
        if ctx < trigger:
            msg = ("Context is %dk tokens. Autocompaction is on and will summarize at about %dk, before the "
                   "%dk handoff limit, so no handoff is needed: keep working normally." % (k(ctx), k(trigger), k(stop)))
        else:
            msg = ("Context is %dk tokens, at the autocompaction point (about %dk). It will summarize shortly; "
                   "keep working, no handoff needed." % (k(ctx), k(trigger)))
    elif ctx >= stop:
        why = ("autocompaction is off" if not enabled
               else "autocompaction only fires at about %dk" % k(trigger))
        msg = ("Context is %dk tokens (limit %dk; %s). Every call now re-reads all of it. Finish the current "
               "step, write your handoff file (skill agent-context-budget) and stop." % (k(ctx), k(stop), why))
    else:
        msg = ("Context is %dk tokens; hand off at %dk (%s). Avoid large reads and long waits."
               % (k(ctx), k(stop), "autocompaction is off" if not enabled
                  else "autocompaction only fires at about %dk" % k(trigger)))
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PostToolUse", "additionalContext": msg}}))


if __name__ == "__main__":
    main()
