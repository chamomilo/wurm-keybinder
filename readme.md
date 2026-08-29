<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.4 for Wurm Unlimited

## See your action queue and cancel queued actions before they start

Hello, SKLOTOPOLIS!

Keybinder 0.7.4 is ready for testing.

This update adds a compact Wurm-styled action queue monitor, safer target
validation, and German localization. It also lets you mark a later queued
action for cancellation: Keybinder remembers the request and stops that action
as soon as Wurm starts it.

- [Download Keybinder from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.4)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.4?

### Compact action queue monitor

The new monitor sits against the right edge of the screen. In its compact mode
it shows up to ten native Build Menu queue lamps and nothing else. Click its
left arrow above the first lamp to slide it open and see each action together
with its Tool and Target. The arrow then points right to close the panel. The
expanded width follows the displayed text instead of wasting screen space.

Click a lit lamp to cancel that action. Wurm can cancel only the action currently
in progress, so when you click a later queue entry Keybinder remembers it and
automatically sends Stop when that action becomes current. Reconnects and HUD
replacement clear stale queue state and pending cancellation requests.

### Safer action execution

Keybinder now validates the source and target against Wurm's action rules before
sending each action. Impossible combinations are skipped with a clear Event
message instead of being sent silently. Hovered, selected, inventory, creature,
ride, and tile targets use their actual target capabilities; unknown server-mod
actions remain permissive.

The queue budget is checked step by step against the slots currently available.
Long saved chains are no longer disabled merely because their total theoretical
cost exceeds the character's queue limit.

### Localization and compatibility

- Added complete German localization alongside English and Brazilian
  Portuguese.
- Corrected Embark and Disembark source handling.
- Improved hover matching, nearby resolution, batch queue costs, and managed
  bind restoration.
- Saved keybinds remain compatible with 0.7.3.

## Core features

- Wurm-styled keybind manager available through **HUD Settings**.
- One-shot capture of ordinary, server-mod, and client-mod actions.
- Action chains and Multi-keybinds that respect the character's action queue.
- Compact right-edge action queue monitor with deferred cancellation.
- Hovered, selected, filtered, nearby, tile, area, inventory, equipment,
  toolbelt, and current-ride targets.
- Custom actions can resolve their Tool through a portable inventory filter,
  using toolbelt, direct inventory, and nested containers in that order.
- Mouse-wheel bindings for actions such as push, pull, and turn.
- Duplicate, merge, extract, import, and export tools for managed keybinds.
- Smart Improve with automatic resource selection and client-side chance
  estimates.
- Archaeology Identify with automatic brush or chisel selection.
- Per-character enabled state across a shared Keybinder installation.
- English, Brazilian Portuguese, and German localization.

## Installation and upgrade

1. Disable the old **Custom Actions**, **Improved Improve**, and **i2improve**
   client mods.
2. Download
   [Keybinder 0.7.4 from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.4).
3. Extract `keybinder-0.7.4.zip` into your Wurm Unlimited client directory, as
   usual for Ago's Client Mod Launcher.
4. When upgrading, simply overwrite the existing Keybinder files. Your managed
   keybinds are stored separately and are not replaced by the archive.
5. Start the game. If the **KB** icon is not visible, enable **Keybinder** in
   **HUD Settings**.

Keybinder is a keybind manager, not an unattended automation or scripting
system. It sends ordinary Wurm actions while respecting your character's action
queue.

Thank you to everyone who tested the new functions and sent detailed Event
logs. They made these fixes possible.

If you find a bug or have another idea, please reply in the forum thread and
include the relevant Event log. Enable **Debug logging** when possible.

## Credits and license

Keybinder began as derivative work based on
[bdew's Custom Actions](https://github.com/bdew-wurm/action).

Smart Improve is a clean-room reimplementation inspired by the work of:

- **Munsta0**, creator of the original Improved Improve;
- **inniria**, creator of i2improve;
- **Snidor**, who continued i2improve.

No source code from those Improved Improve mods is bundled.

Keybinder is licensed under **GNU LGPL 3.0 or later**. See
[`lgpl-3.0.txt`](lgpl-3.0.txt).

Cheers!

**Chamomilo**
