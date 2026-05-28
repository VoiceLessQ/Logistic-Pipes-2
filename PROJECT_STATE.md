LP2 / Morph — Project State
===========================

Branch you should be on for active work: **mc-1.21.1**
Remote: origin → https://github.com/VoiceLessQ/Logistic-Pipes-2.git (this branch pushes to GitHub branch **1.21.1**)
Mod ID: morph        Group: com.Morph        Version: 1.0.0

--- Quick sanity check ---
  git rev-parse --abbrev-ref HEAD     → should print: mc-1.21.1
  git status                          → should be clean or only show in-progress work

--- Target platforms ---
Minecraft 1.21.1
Architectury 19.0.1 (common split between Fabric + NeoForge)
  Fabric 0.18.4 loader + Fabric API 0.139.5+1.21.11
  NeoForge 21.11.38-beta
Mojang mappings (NOT Yarn)
Java 21
Gradle 8.14 + architectury-loom 1.13-SNAPSHOT

(gradle.properties says minecraft_version=1.21.1 — branch name and dep versions are aligned. CLAUDE.md previously read "1.21.11" — that was a typo, ignore it.)

--- Where things live ---

common/                         All shared LP2 logic. Compiled into a single artifact that both platforms re-package.
  src/main/java/com/Morph/      Java source. No net.fabricmc or net.neoforged imports allowed here.
    logisticspipes/             All ported LP1 logic — pipes, modules, routing, request, transport, network, platform.
    logisticspipes/client/      Client-only code (BESR renderer). Loaded conditionally per platform.
  src/main/resources/           Assets — blockstates, models, lang, textures, OBJ pipe geometry.

fabric/                         Fabric entry points + FabricPlatformHelper (Transfer API).
neoforge/                       NeoForge entry points + NeoForgePlatformHelper (Capabilities API).

reference/                      Non-runtime reference material kept in git for porting work.
  power-1.20.1/                 Power system as it existed in commit bd1f0d6 (the NeoForge 1.20.1 migration checkpoint).
                                151 files. Original layout (common/logisticspipes/*, resources/assets/logisticspipes/*).
                                These compile only against the 1.20.1 codebase — they were stripped from the active
                                tree at commit 422db66 (Phase 1 1.21.1 strip). Lift from here when porting power.

REMAINING_WORK.md               Local working checklist. Gitignored (lives only in your tree).
LP1_ANALYSIS.md                 LP1 1.12.2 code breakdown. Tracked.
CLAUDE.md                       Codebase + instructions for AI assistants. Tracked.

--- What's been done ---
Phase 1 → 6 + rendering + checkpoint commit c31f263 (Phase 7+: rendering, asset overhaul, gitignore).
See git log for the commit-by-commit story; the Phase labels mark major milestones.

--- What's been stripped (and where to find it) ---
Power system               → reference/power-1.20.1/      (also commit bd1f0d627 on this branch)
Old 1.21.11 attempt        → commits before 422db66       (look for "1.21.11" in git log)
Old 1.20.1 NeoForge port   → commit bd1f0d627 + earlier   (see git log --all)
LP1 1.12.2 upstream        → F:/Minecraft modding/Mod Github/LogisticsPipes (separate repo, branch 1.20.1)
                             — that repo is the PUBLIC GitHub repo's 1.20.1 branch (broken 0.0.1 jar release).
                             We branched 1.20.1-lp2-port off it locally but are not porting INTO it.

--- Related repos / paths on this machine ---
This project:               f:/Minecraft modding/Projects/Fabric-Neoforge/Logistic Pipes 2   (active 1.21.1 dev)
LP1 reference + 1.20.1:     F:/Minecraft modding/Mod Github/LogisticsPipes                  (separate repo,
                                                                                             public GitHub source)
Sibling early port:         F:/Minecraft modding/Projects/Fabric-Neoforge/Logistic Pipes    (older sibling attempt,
                                                                                             not the live one)

--- Active GitHub issue ---
#1 (mod refuses to load — NoSuchFieldError BLOCK_ENTITY_TYPE)
   Caused by: 0.0.1 jar built against NeoForge 20.1.88 (line dropped by NeoForge maven; loader mappings mismatch
   on current Forge 47.x / NeoForge 47.x). Resolution: deprecate 0.0.1 jar, point users at the 1.21.1 build once
   it's release-ready. The 1.21.1 branch on GitHub is the new home (was just pushed).
