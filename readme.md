<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.2 for Wurm Unlimited

## One key, many actions — now with Archaeology Identify and Smart Smart Improve

Hello, SKLOTOPOLIS!

Keybinder 0.7.2 is ready for testing.

This release adds a dedicated Archaeology Identify step, makes Smart Improve
even smarter, and fixes several small issues found during play.

- [Download Keybinder from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.2)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.2?

### Archaeology Identify

There is a new dedicated step type: **Archaeology Identify**.

Point at one unidentified fragment or a group of fragments and press **F**.
Keybinder selects the correct tool from your inventory for every fragment. As
with Smart Improve, you can use either **Toolbelt only** or **First toolbelt,
then inventory** as the search area.

With one key, identify your fragments up to the free action-queue limit.

```text
[12:34:18] [Keybinder] Archeology Identify: using metal brush, steel (w84c90) from "backpack" in inventory to identify "unidentified wooden fragment"
```

### Smart Improve now became smarter

Smart Improve now adds the QL of the improved item, the estimated Improve
action success chance, and the rarity-upgrade chance after a rarity drumroll —
for those who want to know.

```text
[04:31:42] [Keybinder] Smart Improve: using log, cherrywood from toolbelt to improve "rare fruit press, applewood" QL 90.45 (improve chance ~57%, improve to supreme chance after drumroll 0.80%)
```

The Improve estimate uses the live server crafting catalog and the character
data available to the client: relevant skill and parent skill, target QL, tool
QL and damage, Epic curve, priest penalty, and the Vynora bonus.

The rarity estimate accounts for the target's current rarity,
rarity-improvement runes seen in Examine, and a rarer consumable source.

These are client-side estimates. The rarity percentage is conditional on an
active rarity window and a successful Improve; it is not the chance of the
rarity window opening.

### Some bug fixes

- Hitched wagons can be used as proper Smart Improve targets.
- Smart Improve reads the data it needs from external objects after Examine.
- Rift stone shards are covered by premium-material protection and are not
  selected as ordinary rock shards.
- Stone chisel and carving knife identification has been corrected again; it
  now uses the actual item type and is covered by tests.
- Cold metal target improvements are rejected locally and are not sent to the
  server.
- Smart Improve source and failure messages are clearer.
- Debug logging now provides extended details about what happened.
- A HUD Multi button can work with even one action inside.
- HUD and Multi selectors close automatically when they are no longer needed.
- Between-server travel and server-specific keybind records have been fixed.
- Hovered, nearby, and inventory filters now state and match their item type
  more consistently.

## Core features

- Wurm-styled keybind manager available through **HUD Settings**.
- One-shot capture of ordinary, server-mod, and client-mod actions.
- Action chains and Multi-keybinds that respect the character's action queue.
- Hovered, selected, filtered, nearby, tile, area, inventory, equipment,
  toolbelt, and current-ride targets.
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
   [Keybinder 0.7.2 from GitHub](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.7.2).
3. Extract `keybinder-0.7.2.zip` into your Wurm Unlimited client directory, as
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
