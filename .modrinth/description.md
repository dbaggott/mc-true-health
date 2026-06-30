# True Health

The vanilla heart bar lies a little. It rounds your health to the nearest half-heart pip, so the actual float Minecraft uses internally is hidden — that pip showing "9½ hearts" might really mean `19.00`, `19.45`, or `18.91`, and you'd never know.

**True Health** prints the real number, in red, right above your heart bar.

> ♥ 18.5 / 20

That's it. One small overlay, one line of text. Half-second to learn, instant to read at a glance.

## What you actually see

- **Your current HP as a float**, with up to two decimals. Whole values trim cleanly: `20`, not `20.00`. Single-decimal values trim the trailing zero: `18.5`, not `18.50`. Two-decimal precision when the value warrants it: `12.34`.
- **Absorption appears inline in gold** when active — `♥ 18.5 / 20 + 8` after an enchanted golden apple, or whatever you've topped up with. Disappears when your absorption runs out.
- **The overlay sits in the right place at all times.** Wearing armor? It shifts up a row to clear the armor icons. Got absorption hearts? Another row up. Both? Both rows. No collision with the vanilla HUD, ever.

## Why you might want this

- **You can see when small damage actually hit you.** Fire ticks, magic damage, environmental wear — the heart pips snap to half-hearts and lose the in-between numbers. True Health shows you the exact `0.42` your shield just absorbed.
- **You can see regen working in real time.** Watch HP tick up by the actual `0.5` or `1.0` increments instead of waiting for a pip to flip.
- **PVP / hardcore players know what's about to kill them.** "One more hit" and "one more half-pip" aren't the same thing.

## What this mod does *not* do

- Doesn't change gameplay. No new mechanics, no balance changes.
- Doesn't show other entities' HP yet (planned for a future version).
- Doesn't show a damage-dealt number on attack yet (planned for a future version).
- Doesn't show food saturation or exhaustion (intentionally cut — those values aren't reliably available client-side on multiplayer servers, and we'd rather ship nothing than something wrong).
