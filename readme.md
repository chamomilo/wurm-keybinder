<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.6 for Wurm Unlimited

## Smarter preparation for Improve

Hello, SKLOTOPOLIS!

Keybinder 0.7.6 is ready for testing.

Smart Improve can now prepare an external world object automatically: it selects
the exact target, sends a quiet Examine, waits for the server response without
freezing the client, and then continues the keybind. This release also fixes
partially used marble, slate, and sandstone shards not being found.

- [Download Keybinder from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.6)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.6?

### Automatic external-object preparation

Smart Improve no longer requires the player to double-click Examine before
improving a forge, altar, fence, or another external object. For Hover, Selected,
and exact-object targets, Keybinder now:

- pins the exact target ID and selects it in Wurm's native Select Bar;
- sends a quiet Examine and waits asynchronously for its improvement data;
- resumes the remaining keybind steps once the matching response arrives;
- times out safely instead of acting on a different or stale target.

Queued Smart Improve actions for the same target can reuse their known
requirement while still respecting the live action queue.

### Partially used stone shards

Marble, slate, and sandstone shards remain valid improvement resources after
their first use, including when the Wurm client reports the shortened generic
name `shards`. Material and icon checks remain exact, so unrelated shards are
not selected.

### 3rd Person View commands

When WU-third_person_view is installed and exposes its Keybinder command
catalog, the editor adds a **3rd Person View** category. This is an optional
integration and does not add a hard dependency on that mod.

### Compatibility

- The new messages are localized in English, Brazilian Portuguese, and German.
- Saved keybinds remain compatible with 0.7.5.
- Existing queue-monitor settings and layouts are unchanged.

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
- Conditional `3rd Person View` command catalog when WU-third_person_view is installed.
- Duplicate, merge, extract, import, and export tools for managed keybinds.
- Smart Improve with automatic resource selection, client-side chance
  estimates, and automatic Select/quiet Examine preparation for external
  objects.
- Archaeology Identify with automatic brush or chisel selection.
- Per-character enabled state across a shared Keybinder installation.
- English, Brazilian Portuguese, and German localization.

## Installation and upgrade

1. Disable the old **Custom Actions**, **Improved Improve**, and **i2improve**
   client mods.
2. Download
   [Keybinder 0.7.6 from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.6).
3. Extract `keybinder-0.7.6.zip` into your Wurm Unlimited client directory, as
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
