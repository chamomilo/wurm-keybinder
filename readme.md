<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.2 for Wurm Unlimited

## One key, many actions

Keybinder is a Wurm Unlimited client mod for creating and managing keybinds
through a Wurm-styled in-game interface. Capture an action, choose its tool and
target, combine several actions under one key, and give each character its own
set of enabled keybinds without editing configuration files or writing console
commands.

Version 0.7.2 adds a dedicated Archaeology Identify step, makes Smart Improve
substantially more transparent, fixes examined world targets such as renamed
hitched wagons, and tightens HUD selector and server-profile behavior.

- [Download Keybinder from GitHub](https://github.com/chamomilo/wurm-keybinder/releases)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.2?

### Archaeology Identify

There is a new dedicated step type: **Archaeology Identify**.

Point at one unidentified fragment or a group of fragments and press the
keybind. Keybinder reads the tool icon supplied by the server, chooses the
required metal brush or stone chisel, and fills only the free part of the
character's action queue. Fragments are processed in stable lowest-QL order.

The tool search can be limited to the toolbelt or use **First toolbelt, then
inventory**. Containers already loaded into the client are searched as well.
Unsupported fragments and missing tools are skipped safely when processing a
hovered group; if nothing can be sent, Event explains the first failure.

### Smart Improve now explains its decision

Smart Improve logs the selected resource location, target QL, estimated Improve
success chance, and the conditional chance of advancing to the next rarity
level after a rarity drumroll. For example:

```text
[Keybinder] Smart Improve: using rock shards from inventory to improve "forge" QL 90.05 (improve chance ~63%, improve to rare chance after drumroll 10.40%)
```

The Improve estimate uses the live server crafting catalog together with the
character's relevant skill, parent skill, target QL, tool QL and damage, Epic
curve, priest penalty, and Vynora bonus available to the client. The rarity
estimate accounts for target rarity, observed rarity-improvement runes, and a
rarer consumable source. It deliberately ignores possible runes on an
unexamined target.

These percentages are client-side estimates. The rarity value is conditional
on an active rarity window and a successful Improve; it is not the chance of a
rarity window opening.

### Better Smart Improve world targets and resources

- A renamed wagon hitched to animals can be correlated with its Examine target
  without requiring two incompatible client object identities.
- Examined external targets retain QL, damage, rarity, requirement, and observed
  rarity-rune information needed by the next Smart Improve command.
- Resource matching is stricter: a rift stone shard is not accepted as ordinary
  rock shards, and image collisions such as carving knife versus stone chisel
  are resolved by the actual item type.
- Cold metal targets are rejected before sending Improve with an explicit
  Event message explaining that the target must be glowing hot.
- Source locations are shorter and clearer: `from inventory`, `from backpack`,
  or the actual containing item.
- Debug logging records world-target correlation, examined metadata, resource
  candidates, rejections, and chance inputs for reproducible bug reports.

### HUD Multi and profile reliability

- HUD Multi now supports a single action as well as a list of alternatives.
- Its selector closes after the next unrelated key or pointer action and does
  not immediately reopen from the same trigger key.
- Managed keybinds cannot fire before the current character's enabled profile
  has been applied.
- Server changes and shard transfers clear stale connection state and use the
  newly reported server name before restoring server-specific bindings.
- Hovered, nearby, and inventory filters share one broader normalization path
  for rarity, material, wood species, and temporary item-state decorations.

## Core features

- Wurm-styled keybind manager available through **HUD Settings**.
- One-shot capture of ordinary, server-mod, and client-mod actions.
- A separate **Tool | Action | Target** definition for each structured step.
- Action chains that respect the character's current action-queue limit.
- Ordinary Multi-keybinds with short-press execution and long-press selection.
- HUD Multi-keybinds that open a command selector immediately.
- Hovered, selected, exact, filtered, nearby, tile, area, inventory, equipment,
  toolbelt, and current-ride targets.
- Mouse-wheel bindings for actions such as push, pull, and turn.
- Duplicate, merge, and extract operations for managed keybinds.
- Portable `.keybinder` import and export files.
- Bundled Keybind Essentials examples.
- **Pick up from bulk** steps for BSBs, FSBs, crates, bulk container units, and
  opened bulk containers inside vehicles.
- **Inventory + filter** targets that find a portable item type in loaded
  toolbelt, inventory, backpack, and nested-container contents.
- Smart Improve with stack ordering, queue fitting, strict resource matching,
  premium-material protection, chance estimates, and inventory or examined
  world-item targets.
- Archaeology Identify with per-fragment brush or chisel selection and adaptive
  queue fitting.
- Per-character enabled state across a shared Keybinder installation.
- English and Brazilian Portuguese localization.

Keybinder is a keybind manager, not an unattended automation or scripting
system. It sends ordinary Wurm actions while respecting the character's action
queue.

## Installation and upgrade

1. Disable the old **Custom Actions**, **Improved Improve**, and **i2improve**
   client mods. Keybinder replaces their supported behavior.
2. Download `keybinder-0.7.2.zip` from the
   [GitHub releases page](https://github.com/chamomilo/wurm-keybinder/releases).
3. Extract the archive into the Wurm Unlimited client directory used by Ago's
   Client Mod Launcher.
4. When upgrading, overwrite the existing Keybinder files. Managed keybinds are
   stored separately and are not replaced by the distribution archive.
5. Start the game. If the **KB** icon is not visible, enable **Keybinder** in
   **HUD Settings**.

The shared keybind definitions are stored in:

```text
mods/keybinder/keybinds.properties
```

Per-character enabled selections are stored beside them in:

```text
mods/keybinder/keybinds.properties.accounts
```

Keybinder uses atomic replacement and rolling backups for its structured data.
The console remains optional for normal use.

## Reporting problems

Thank you to everyone who tested the new functions and sent detailed Event
logs. They were extremely useful.

If you find a bug, enable **Debug logging**, reproduce the problem, and include
the relevant `[Keybinder]` Event lines in your report. Please also mention the
step type, Tool mode, Target, and whether the source or destination container
was opened in its own window.

Bug reports and ideas are welcome in the
[SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)
or by message to **Chamomilo** on SKLOTOPOLIS.

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

Thank you **FlpSilva**, **Wolfbane**, **Spike**, and everyone who supplied test
results and Event logs.

Cheers! **Chamomilo**
