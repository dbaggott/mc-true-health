# TruHearts

See your true HP instead of the approximation in the heart bar.  Vanilla MC rounds your health to the nearest half-heart — when you see "9½ hearts," that could mean `19.00`, `19.45`, or `18.91`, and you'd never know.

**TruHearts** prints the real number, in red, right above your heart bar.

> ♥ 18.5 / 20

Plus, you get an (optional) recent-damage log of what is actually damaging you and by how much.

## What you actually see

- **Your current HP as a float**, with up to two decimals. Whole values trim cleanly: `20`, not `20.00`. Single-decimal values trim the trailing zero: `18.5`, not `18.50`. Two-decimal precision when the value warrants it: `12.34`.
- **Absorption appears inline in gold** when active — `♥ 18.5 / 20 + 8` after an enchanted golden apple, or whatever you've topped up with. Disappears when your absorption runs out.
- **Recent-damage log.** Stacks the last 5 hits above the HP readout. Each entry shows the exact damage amount (measured as HP + absorption delta, so hits eaten by absorption still count) and a short label for the source — mob name for attacks (`Zombie`, `Skeleton`, `Warden`) or environmental cause (`Fall`, `Fire`, `Lava`, `Drowning`, `Explosion`, `Void`, …). Newest at the bottom; older entries fade out over a 10 s lifetime.
- **Toggle it on or off in-game.** A keybind under **Controls → TruHearts** is a master switch that hides everything TruHearts renders. A separate keybind flips just the recent-damage log if you want the HP readout without the log. Both unbound by default; assign whatever keys you like. Preferences stick between sessions.

## Why you might want this

- **You can see when small damage actually hit you.** Fire ticks, magic damage, environmental wear — the heart pips snap to half-hearts and lose the in-between numbers. TruHearts shows you the exact `0.42` your shield just absorbed.
- **You can see regen working in real time.** Watch HP tick up by the actual `0.5` or `1.0` increments instead of waiting for a pip to flip.
- **PVP / hardcore players know what's about to kill them.** "One more hit" and "one more half-pip" aren't the same thing.
