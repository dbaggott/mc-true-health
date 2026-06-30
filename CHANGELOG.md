# Changelog

All notable changes to TrueHearts are documented here.

This file follows the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format,
and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> Per-version source-of-truth changelogs live in
> [`.modrinth/changelogs/<version>.md`](.modrinth/changelogs/) — those are what
> Modrinth and GitHub Releases display for each release. This file aggregates them
> in reverse-chronological order for human browsing. When shipping a new release,
> add the version's file in `.modrinth/changelogs/` *and* prepend a new section here.

## [1.0.0] — 2026-06-30

First stable release.

Renames the mod to **TrueHearts** to match the heart icons it overlays. The 1.x line
commits to a stable mod ID (`truehearts`) — future updates change behavior, never
identifier.

- Real (un-rounded) HP shown above the heart bar, with trailing-zero trim: `20`
  instead of `20.00`, `18.5` instead of `18.50`, `12.34` when precision warrants it.
- Absorption shown inline in gold (e.g. `+ 8`) when active, hidden when zero.
- Y-offset automatically clears the armor row and absorption-heart row so the
  readout never overlaps vanilla HUD elements.

Minecraft 26.2 with Fabric Loader 0.19.3+. Client-only — does nothing on a dedicated
server and isn't required there.

[1.0.0]: https://github.com/dbaggott/mc-truehearts/releases/tag/v1.0.0
