<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.3 for Wurm Unlimited

## One key, many actions — now with portable tool filters and smooth scrolling

Hello, SKLOTOPOLIS!

Keybinder 0.7.3 is ready for testing.

This release adds a portable inventory-filtered Tool source to ordinary custom
actions, removes the first-use work from Smart Improve chance estimates, and
fixes the mouse-wheel scrolling problem in the Keybinder list.

- [Download Keybinder from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.3)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.3?

### Portable Inventory + filter tools

Ordinary custom-action steps now have a new **Tool** option:
**Inventory + filter**.

Choose an example item and Keybinder stores its portable item type instead of a
runtime object ID. When the keybind runs, it finds the first matching item in
this order:

- toolbelt;
- direct player inventory;
- nested containers.

The same keybind can therefore select the correct hammer, sickle, seed, or
other tool after relogging, travelling between servers, or switching to an alt.

### Faster Smart Improve estimates

Improve success chance is now calculated directly instead of running thousands
of sample rolls on the game thread. The live creation-skill catalog is also
reused until the server sends a catalog change.

This keeps a useful client-side estimate while removing the noticeable cold
lookup work when Smart Improve is first used.

### Scrolling and compatibility fixes

- Mouse-wheel scrolling in the main Keybinder list no longer jumps back toward
  the top. Scrollbar dragging remains available and now tracks the same stable
  content layout.
- The small **KB** launcher no longer loads Wurm's `TargetWindow` class as its
  superclass. This avoids freezing that client class before other compatible
  mods can install target-name hooks.
- The launcher still preserves its position, visibility, and lock state.
- Internal action execution, input handling, persistence, and HUD hooks have
  been separated into smaller components without changing saved keybinds.

## Core features

- Wurm-styled keybind manager available through **HUD Settings**.
- One-shot capture of ordinary, server-mod, and client-mod actions.
- Action chains and Multi-keybinds that respect the character's action queue.
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
- English and Brazilian Portuguese localization.

## Installation and upgrade

1. Disable the old **Custom Actions**, **Improved Improve**, and **i2improve**
   client mods.
2. Download
   [Keybinder 0.7.3 from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.3).
3. Extract `keybinder-0.7.3.zip` into your Wurm Unlimited client directory, as
   usual for Ago's Client Mod Launcher.
4. When upgrading, simply overwrite the existing Keybinder files. Your managed
   keybinds are stored separately and are not replaced by the archive.
5. Start the game. If the **KB** icon is not visible, enable **Keybinder** in
   **HUD Settings**.

Keybinder is a keybind manager, not an unattended automation or scripting
system. It sends ordinary Wurm actions while respecting your character's action
queue.

Thank you to everyone who tested the new functions and sent detailed Event
logs. They made these fixes possible. The mouse-wheel fix was also verified
manually in the game before this release was prepared.

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
