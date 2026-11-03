---
name: remote-build-runner
description: Use when builds, dev servers, real-pack servers or Minecraft clients run on a separate build host instead of the development machine - choosing a slot and instance, running tools/remote/remote.sh, sharing one box between parallel lanes, or fetching logs and screenshots back.
---

# Running builds and Minecraft on a build host

Gradle builds, dev servers, the real-pack server and clients are heavy. When a project has a shared build host, drive it from the development machine with `tools/remote/remote.sh` (setup and commands: `tools/remote/README.md`). Host, user, paths, port ranges and display ranges come from the git-ignored `.env` (`.env.example` lists the names); none of them belongs in git.

## Rules

1. **Slots.** One slot is one checkout on the host (`wt/<slot>/`). Give each parallel lane its own slot, named for the lane. Never share a slot between two runs.
2. **Instances.** The real-pack server has numbered instances (`--instance N`), each with its own port, world and lock. Give each parallel lane its own instance; headless scenario, hash and budget runs then go in parallel. Keep instance 0 for runs that need the pack's world.
3. **Ranges.** Dev-server and real-pack ports and Xvfb displays are drawn from ranges in `.env`. Other projects may share the host: stay inside your ranges and stop only processes you started (the scripts track pids).
4. **Revisions.** `remote.sh` resolves a rev on your machine and either fetches it on the host or sends a git bundle, so unpushed work can be built.
5. **Quiet and cheap.** Wrap calls in `tools/agent/quiet-run.sh`, wait with one Monitor on `run.done` (`waiting-on-remote-jobs`), and read the fetched summary, not the console log (`quiet-tool-output`).
6. **Tiers.** Lanes run T1 and T2 on their own slot; real-pack and client runs belong to the integrator and the gates (`mod-port-budget`, `integration-merge-choreography`).
7. **Screenshots** come back in the fetched run directory; review them as a contact sheet (`cheap-screenshot-review`).
8. **Secrets.** A key or password lives in `.env` only. Never print `.env`, and never commit it (`release-and-public-repo`).
