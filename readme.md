# Keybinder 0.5.4 for Wurm Unlimited

Keybinder is a standalone Wurm Unlimited client mod for constructing and managing
ordered keybind workflows from the HUD.

## What's new in 0.5.4

- Complete English and Brazilian Portuguese localization with a persistent
  Wurm-styled language selector and safe English fallback.
- Vanilla gameplay actions selected from the catalog now use ordinary
  target-aware action steps when the current client provides an unambiguous
  `PlayerAction`; HUD, movement, and unresolved commands retain native
  compatibility behavior.
- Runtime queue preflight accounts for occupied slots and rejects an entire
  keybind before its first action when the remaining capacity is insufficient.
- Repeated `Push` and `Push gently` actions retain the selected object across
  every server-side recreation.
- Fixed the `KB` tag right-click crash, stale English list content after a
  language change, and incorrect red styling for user-disabled keybinds.

Each managed keybind is a stable container of steps:

- Activate tool
- Smart improve
- Console command
- Custom action

Managed keys execute through `keybinder_run <id>` and run their stored,
type-safe steps directly. Commands from Custom Actions and the Improved Improve
lineage are supported only by the one-time import workflow and are replaced
with native Keybinder records during import.

An unavailable runtime target skips only its own step and writes the reason to
the system Event tab. Remaining steps continue in their saved order. Queue
limits are checked at runtime against the steps that can actually execute.
Missing `nearby` targets are skipped silently; a target found outside the
action range still produces a useful “come closer” Event message.

`nearby` and `nearby by type` use the selected Wurm action's standard
`ActionEntry` range. Keybinder scans at least twice that range; when a matching
object is visible but outside the action range, Event identifies the action and
nearest object and asks the player to move closer.

The `current ride` target resolves at execution time to the creature or vehicle
currently carrying the player. For example, `Open` on `current ride` opens the
inventory of the ridden horse, cart, wagon, or boat when the server permits it.

Enabled managed keybinds are remembered per Wurm account and restored after the
player profile is loaded. Keybinder never overwrites a foreign binding while
performing this restore.

## Installation

1. Disable the old `action` and `i2improve` client mods.
2. Extract `keybinder-0.5.4.zip` into the Wurm Unlimited client directory.
3. Verify `mods/keybinder.properties` and `mods/keybinder/keybinder.jar`.
4. Enable **Keybinder** in HUD Settings.

Keybinder reports legacy Custom Actions and Improved Improve installations,
imports their known bindings into native steps, and can then disable the
superseded Custom Actions mod for the next launch.

## Runtime commands

```text
keybinder_run <managed-id>
keybinder_list [commands]
```

## Attribution

Keybinder is derivative work based on bdew's
[Custom Actions](https://github.com/bdew-wurm/action), licensed under
LGPL-3.0-or-later.

Smart Improve is a clean-room reimplementation inspired by the complete
Improved Improve mod lineage. Special thanks to:

- [Munsta0](https://github.com/munsta0/WUClientImprovedImprove), who created
  the original Improved Improve client mod;
- [inniria](https://github.com/inniria/i2improve), who rewrote and extended it
  as i2improve;
- [Snidor](https://github.com/Snidor/i2improve), who continued i2improve and
  whose 0.2.1 release was used as the final research baseline.

No source code from these Improved Improve mods is bundled.

See `lgpl-3.0.txt` for the inherited Custom Actions license.
