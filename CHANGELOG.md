# Changelog

All notable changes to TruHearts are documented here.

This file follows the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format,
and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> Per-version source-of-truth changelogs live in
> [`.modrinth/changelogs/<version>.md`](.modrinth/changelogs/) — those are what
> Modrinth and GitHub Releases display for each release. This file aggregates them
> in reverse-chronological order for human browsing. When shipping a new release,
> add the version's file in `.modrinth/changelogs/` *and* prepend a new section here.

## [1.3.0] — 2026-07-01

A new overlay: the last few hits you took.

- **Recent-damage log** stacks above the HP readout. Each entry shows the
  amount (as an un-rounded value like the HP readout) and a short label
  for the source — mob or player name for attacks, or the environmental
  cause (`Fell`, `Fire`, `Lava`, `Drowned`, `Explosion`, `Void`, …).
  Newest at the bottom; older entries stack upward and fade over the
  trailing ~2 s of a 10 s lifetime. Up to 5 entries on screen at once.
- Amount is measured as **health + absorption delta**, so hits eaten by
  absorption count at what the player actually felt.
- **Two-level toggle model.** The existing keybind is now the *master*
  switch (**Controls → TruHearts → Toggle everything (master)**) that
  hides everything TruHearts renders in one shot. A new keybind,
  **Toggle recent-damage log**, flips just the damage log while leaving
  the HP readout alone. Both are unbound by default; both preferences
  persist in `config/truhearts.json`.
- Coupled to vanilla-hearts visibility the same way the HP readout is:
  no vanilla hearts (creative, spectator) → no damage log.

Still one jar for MC 26.1 through 26.2.

## [1.2.1] — 2026-07-01

Two small fixes based on player feedback.

- The "TruHearts: on/off" toast now shows in **every gamemode**, not just
  survival. Previously the toast was silent in creative and spectator —
  the keybind fired and the state flipped, but you had no on-screen
  confirmation until you switched back to survival and noticed the HP
  overlay was gone. The toast now attaches to the same HUD slot as
  vanilla's held-item-name message so it renders wherever that does.
- Longer, more legible toast: **1.5 s total** (1 s hold + 0.5 s fade),
  up from 0.75 s.

The HP overlay itself is unchanged — still shown only when vanilla
hearts are visible (i.e., not in creative or spectator).

## [1.2.0] — 2026-07-01

Now runs on Minecraft 26.1 as well as 26.2 — one jar covers both.

- Adds support for Minecraft **26.1, 26.1.1, and 26.1.2**. The jar is
  source-compatible with both 26.1 and 26.2 lines.
- The "TruHearts: on/off" toggle message is now rendered by TruHearts
  directly (a brief 0.75 s linear fade), rather than calling vanilla's
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

[1.3.0]: https://github.com/dbaggott/mc-truhearts/releases/tag/v1.3.0
[1.2.1]: https://github.com/dbaggott/mc-truhearts/releases/tag/v1.2.1
[1.2.0]: https://github.com/dbaggott/mc-truhearts/releases/tag/v1.2.0
[1.1.0]: https://github.com/dbaggott/mc-truhearts/releases/tag/v1.1.0
[1.0.0]: https://github.com/dbaggott/mc-truhearts/releases/tag/v1.0.0
