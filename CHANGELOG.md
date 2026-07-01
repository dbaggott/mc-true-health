# Changelog

All notable changes to TruHearts are documented here.

This file follows the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format,
and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> Per-version source-of-truth changelogs live in
> [`.modrinth/changelogs/<version>.md`](.modrinth/changelogs/) — those are what
> Modrinth and GitHub Releases display for each release. This file aggregates them
> in reverse-chronological order for human browsing. When shipping a new release,
> add the version's file in `.modrinth/changelogs/` *and* prepend a new section here.

## [1.2.0] — 2026-07-01

Now runs on Minecraft 26.1 as well as 26.2 — one jar covers both.

- Adds support for Minecraft **26.1, 26.1.1, and 26.1.2**. The jar is
  source-compatible with both 26.1 and 26.2 lines.
- The "TruHearts: on/off" toggle message is now rendered by TruHearts
  directly (a brief 0.5 s linear fade), rather than calling vanilla's
  `setOverlayMessage`. That vanilla method's location differs between
  26.1 (`Gui`) and 26.2 (`Gui.hud`), so routing through our own element
  is what lets a single jar cover both.

Requires Fabric Loader 0.19.3+ and Fabric API matching your MC version.

## [1.1.0] — 2026-07-01

Toggle the HP readout on/off in-game.

- New keybind **Toggle HP overlay** under **Controls → TruHearts**. Unbound by
  default — assign it to whatever key you like.
- Preference persists across game sessions in `config/truhearts.json`.
- Action-bar message ("TruHearts: on/off") echoes the state change.

No changes to how the readout looks or where it sits when it's on.

## [1.0.0] — 2026-06-30

First stable release.

- Real (un-rounded) HP shown above the heart bar, with trailing-zero trim: `20`
  instead of `20.00`, `18.5` instead of `18.50`, `12.34` when precision warrants it.
- Absorption shown inline in gold (e.g. `+ 8`) when active, hidden when zero.
- Y-offset automatically clears the armor row and absorption-heart row so the
  readout never overlaps vanilla HUD elements.

Minecraft 26.2 with Fabric Loader 0.19.3+. Client-only — does nothing on a dedicated
server and isn't required there.

[1.2.0]: https://github.com/dbaggott/mc-truhearts/releases/tag/v1.2.0
[1.1.0]: https://github.com/dbaggott/mc-truhearts/releases/tag/v1.1.0
[1.0.0]: https://github.com/dbaggott/mc-truhearts/releases/tag/v1.0.0
