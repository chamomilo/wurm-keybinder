<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.3 for Wurm Unlimited

## One key, many actions — now with portable tool filters and smooth scrolling

Hello, SKLOTOPOLIS!

Keybinder 0.7.3 is ready for testing.

This is a small update with several fixes and a useful new option for actions
that need materials from your inventory — for example, finding a sprout and
using it to plant.

- [Download Keybinder from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.3)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.3?

### Inventory + filter tools

This is helpful when an action must use a material stored in your inventory as
its Tool. For example, you can plant whatever sprout you have or create kindling
from whatever suitable wood scrap is available.

It is simple to use:

1. Select **Tool → Inventory + filter**. Keybinder asks you to choose a filter
   item; click a sprout.
2. When you press the keybind, Keybinder searches your inventory for any
   matching sprout and selects it.
3. The selected sprout is used for the action — in this example, **Plant**.

The command stores the item's normalized short type as a portable filter. At
execution time it searches in this order:

- toolbelt;
- direct player inventory;
- nested containers.

### Smart Improve became faster

Smart Improve was optimized to calculate its success-rate predictions faster.

### Smooth mouse-wheel scrolling

Thanks to **Zeex**, the bug affecting mouse-wheel scrolling in large keybind
lists was found and fixed. The list no longer jumps back toward the top.
Scroll and enjoy!

### Compatibility and internal fixes

- Resolved an incompatibility with the **Archery** mod. Creature names are now
  displayed correctly instead of labels such as `20m null`.
- The **KB** launcher continues to preserve its position, visibility, and lock
  state.
- Added small fixes and internal refactoring without changing saved keybinds.

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
