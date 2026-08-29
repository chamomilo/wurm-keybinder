<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.5 for Wurm Unlimited

## Put the action queue on either side of the screen

Hello, SKLOTOPOLIS!

Keybinder 0.7.5 is ready for testing.

This small interface update lets you place the compact Queue Monitor on either
the right or left edge of the screen. Its complete layout mirrors automatically,
including the drawer direction, lamps, text, border, and header arrow.

- [Download Keybinder from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.5)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.5?

### Left or right screen edge

Open the Keybinder window and use **Queue monitor → Right edge / Left edge**
beside the action queue limit. The choice takes effect immediately and is saved
in `keybinder.properties` for the next launch.

On the left edge the monitor is fully mirrored:

- the panel opens toward the center of the screen;
- the lamp and arrow column moves to the panel's right side;
- the arrow reverses its open and close directions;
- action, Tool, and Target text is right-aligned beside the lamps;
- the inner border moves to the correct side.

### Expanded header

When the panel is open, the space above the action rows now displays the
localized **Queue monitor** title. The collapsed strip remains minimal and shows
only its direction arrow and Build Menu-style queue lamps.

### Compatibility

- The new setting is localized in English, Brazilian Portuguese, and German.
- Existing installations default to the original right-edge layout.
- Saved keybinds remain compatible with 0.7.4.

## Core features

- Wurm-styled keybind manager available through **HUD Settings**.
- One-shot capture of ordinary, server-mod, and client-mod actions.
- Action chains and Multi-keybinds that respect the character's action queue.
- Compact left- or right-edge action queue monitor with deferred cancellation.
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
   [Keybinder 0.7.5 from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.5).
3. Extract `keybinder-0.7.5.zip` into your Wurm Unlimited client directory, as
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
