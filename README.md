# TruHeart

A small client-side Minecraft mod (Fabric) that surfaces the *real* numerical health and damage values the game uses internally — no rounding to half-hearts.

## Shipped (v0.2.0)

- **Your own current/max HP** as a float above the heart bar (e.g. `♥ 18.5 / 20`), with trailing-zero trim so a whole-number HP reads as `20`, not `20.00`.
- **Absorption** shown inline in gold (`+ 8`) when active.
- **Dynamic Y-offset** clears armor + absorption rows above the heart bar — the readout never overlaps vanilla HUD.

## Planned

- HP of whatever entity (player or mob) you're looking at.
- A damage number every time you land a hit, computed from the target's health delta.

## Target toolchain

| Tooling | Version |
|---|---|
| Minecraft | 26.2 ("Chaos Cubed") |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.153.0+26.2 |
| Fabric Loom | 1.17-SNAPSHOT |
| JDK | Temurin 25 (pinned via `mise.toml`) |
| Gradle | 9.x via wrapper |

The mod is client-only (`"environment": "client"` in `fabric.mod.json`) — no install on dedicated servers.

## Build

Prerequisites: `mise`, `git`.

```bash
git clone <repo-url>
cd truheart
mise install        # provisions Temurin 25 per mise.toml
./gradlew build     # produces build/libs/truheart-<version>.jar
```

## Develop in IntelliJ IDEA

1. Open the project root. IntelliJ + Loom auto-generates run configurations on import.
2. Use the **Minecraft Client** run config to launch a dev MC instance with the mod loaded.
3. Edit, hot-reload via the run config, or restart for resource/mixin changes.

## Install the built jar into a real instance

Drop `build/libs/truheart-<version>.jar` (the un-suffixed file — not `-sources` or `-dev`) into your Minecraft instance's `mods/` folder, alongside Fabric Loader and Fabric API jars matching the versions above.

## Project layout

```
build.gradle, settings.gradle, gradle.properties   — build config
mise.toml                                            — JDK pin (Temurin 25)
src/main/                                            — shared (client + server) code
src/main/resources/fabric.mod.json                   — mod metadata
src/main/resources/truheart.mixins.json          — shared mixin config
src/client/                                          — client-only code (HUD lives here)
src/client/resources/truheart.client.mixins.json — client mixin config
.github/workflows/build.yml                          — CI: gradle build on push/PR
```

Two source sets ("split environment") are used so client-only APIs can't be referenced from shared code — Loom enforces this at compile time.

## Using this project as a template

To start another mod from this scaffold, the pieces that need changing are, top to bottom:

1. `settings.gradle` — `rootProject.name`
2. `gradle.properties` — `mod_version`; bump MC/loader/API versions as needed
3. `fabric.mod.json` — `id`, `name`, `description`, `entrypoints`, `mixins`
4. **Package rename** — `src/{main,client}/java/io/dnbg/minecraft/truheart/` → `…/<newmod>/`; update imports
5. **Mixin configs** — rename `truheart.mixins.json` / `truheart.client.mixins.json` to match the new mod id; update their `package` fields and the `mixins` reference in `fabric.mod.json`
6. `assets/truheart/` → `assets/<newmod>/`
7. `README.md` content
8. `LICENSE` — keep MIT or replace

A scripted template-init step is a follow-up; for the first few mods the manual rename is fine and forces familiarity with the moving parts.

## License

MIT — see `LICENSE`.
