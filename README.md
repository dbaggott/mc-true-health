# TruHearts

[![Modrinth downloads](https://img.shields.io/modrinth/dt/dnbg-truhearts?label=downloads&logo=modrinth&color=00AF5C)](https://modrinth.com/mod/dnbg-truhearts)
[![Modrinth game versions](https://img.shields.io/modrinth/game-versions/dnbg-truhearts?logo=modrinth&color=00AF5C&label=for%20MC)](https://modrinth.com/mod/dnbg-truhearts)
[![Latest release](https://img.shields.io/github/v/release/dbaggott/mc-truhearts?logo=github&label=release)](https://github.com/dbaggott/mc-truhearts/releases/latest)
[![Build](https://img.shields.io/github/actions/workflow/status/dbaggott/mc-truhearts/build.yml?branch=main&logo=github&label=build)](https://github.com/dbaggott/mc-truhearts/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/dbaggott/mc-truhearts)](LICENSE)

A small client-side Minecraft mod (Fabric) that surfaces the *real* numerical health and damage values the game uses internally — no rounding to half-hearts.

## Shipped

- **Your own current/max HP** as a float above the heart bar (e.g. `♥ 18.5 / 20`), with trailing-zero trim so a whole-number HP reads as `20`, not `20.00`.
- **Absorption** shown inline in gold (`+ 8`) when active.
- **Dynamic Y-offset** clears armor + absorption rows above the heart bar — the readout never overlaps vanilla HUD.
- **In-game toggles** — bind under **Controls → TruHearts**: a *master* switch that hides everything, and per-feature sub-toggles (currently one, for the damage log). Both unbound by default; both preferences persist in `config/truhearts.json`. *(master v1.1.0, sub-toggles v1.3.0)*
- **Recent-damage log** stacked above the HP readout: up to 5 recent hits, each with the amount and its source (mob name or environmental cause like `Fell` / `Fire` / `Lava` / `Explosion`). Amount is measured as HP+absorption delta so hits eaten by absorption still count. Newest at the bottom; each entry fades over a 10 s lifetime. *(v1.3.0)*

## Planned

- HP of whatever entity (player or mob) you're looking at.
- A damage number every time you land a hit, computed from the target's health delta.

## Target toolchain

| Tooling | Version |
|---|---|
| Minecraft (runtime) | 26.1, 26.1.1, 26.1.2, or 26.2 ("Chaos Cubed") — one jar for all |
| Minecraft (build target) | 26.2 by default; the same source also compiles clean against 26.1.2 (verified in CI) |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.153.0+26.2 at build time; players use whichever release matches their MC |
| Fabric Loom | 1.17-SNAPSHOT |
| JDK | Temurin 25 (pinned via `mise.toml`) |
| Gradle | 9.x via wrapper |

The mod is client-only (`"environment": "client"` in `fabric.mod.json`) — no install on dedicated servers.

Cross-version compat is possible because the mod uses only APIs that are shape-stable between 26.1 and 26.2, and renders the toggle toast through its own HUD element (see `ToggleToast`) rather than calling `setOverlayMessage` — that method's location differs between the two MC versions.

## Build

Prerequisites: `mise`, `git`.

```bash
git clone <repo-url>
cd truhearts
mise install        # provisions Temurin 25 per mise.toml
./gradlew build     # produces build/libs/truhearts-<version>.jar
```

## Develop in IntelliJ IDEA

1. Open the project root. IntelliJ + Loom auto-generates run configurations on import.
2. Use the **Minecraft Client** run config to launch a dev MC instance with the mod loaded.
3. Edit, hot-reload via the run config, or restart for resource/mixin changes.

## Install the built jar into a real instance

Drop `build/libs/truhearts-<version>.jar` (the un-suffixed file — not `-sources` or `-dev`) into your Minecraft instance's `mods/` folder, alongside Fabric Loader and Fabric API jars matching the versions above.

## Project layout

```
build.gradle, settings.gradle, gradle.properties   — build config
mise.toml                                            — JDK pin (Temurin 25)
src/main/                                            — shared (client + server) code
src/main/resources/fabric.mod.json                   — mod metadata
src/main/resources/truhearts.mixins.json          — shared mixin config
src/client/                                          — client-only code (HUD lives here)
src/client/resources/truhearts.client.mixins.json — client mixin config
.github/workflows/build.yml                          — CI: gradle build on push/PR
```

Two source sets ("split environment") are used so client-only APIs can't be referenced from shared code — Loom enforces this at compile time.

## Using this project as a template

To start another mod from this scaffold, the pieces that need changing are, top to bottom:

1. `settings.gradle` — `rootProject.name`
2. `gradle.properties` — `mod_version`; bump MC/loader/API versions as needed
3. `fabric.mod.json` — `id`, `name`, `description`, `entrypoints`, `mixins`
4. **Package rename** — `src/{main,client}/java/io/dnbg/minecraft/truhearts/` → `…/<newmod>/`; update imports
5. **Mixin configs** — rename `truhearts.mixins.json` / `truhearts.client.mixins.json` to match the new mod id; update their `package` fields and the `mixins` reference in `fabric.mod.json`
6. `assets/truhearts/` → `assets/<newmod>/`
7. `README.md` content
8. `LICENSE` — keep MIT or replace

A scripted template-init step is a follow-up; for the first few mods the manual rename is fine and forces familiarity with the moving parts.

## License

MIT — see `LICENSE`.
