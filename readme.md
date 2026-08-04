<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.1 for Wurm Unlimited

## One key, many actions

Keybinder is a Wurm Unlimited client mod for creating and managing keybinds
through a Wurm-styled in-game interface. Capture an action, choose its tool and
target, combine several actions under one key, and give each character its own
set of enabled keybinds—without editing configuration files or writing console
commands.

Version 0.7.1 is ready for testing. This is a focused follow-up to 0.7.0, built
around player reports and real in-game use. Its main additions are bulk-storage
transfers, portable inventory filters, per-character enabled keybinds, and a
better Smart Improve search order.

This may be the last major feature update for a while, so now is a good time to
put it through its paces.

- [Download Keybinder from GitHub](https://github.com/chamomilo/wurm-keybinder/releases)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.1?

### Pick up from bulk: Keybinder meets panfilling

Yes, panfilling. I know—it is painful. Wolfbane and I think we found a good
balance between convenience and ordinary Wurm gameplay.

After helping you dice, mince, and chop your ingredients, Keybinder can now
take the prepared components from bulk storage and place them into the pan you
point at. A single keybind can contain several **Pick up from bulk** steps, so
one press can request all six ingredients for a recipe.

There is no unattended automation here: prepare the ingredients, point at the
pan, and press the key. Old-school panfilling, just without the unnecessary
side work. Point and press **F**—or whichever key you prefer.

To create the keybind:

1. Put the prepared ingredients into bulk storage.
2. Add one **Pick up from bulk** step for each ingredient.
3. Select the exact ingredient row and enter the required quantity.
4. Set the target to **Hovered inventory**.
5. Point at the destination pan or container and run the keybind.

A keybind may contain as many bulk-transfer steps as the character's action
queue permits. Bulk transfers may also be mixed with ordinary actions.

Supported bulk sources include:

- bulk storage bins;
- food storage bins;
- small and large crates;
- bulk container units;
- bulk containers opened inside vehicles.

Select the required item row in the opened bulk container. Keybinder remembers
both the item and its source storage, so the source window may then be closed.

Available destinations are:

- **Player inventory**;
- **Hovered inventory**—an opened inventory window, a container row inside an
  inventory, or a container in the world;
- **Captured inventory**—one specific inventory or container selected while
  creating the keybind.

Keybinder sends each transfer, waits locally for the matching server quantity
form, answers it, and then continues the chain. A server rejection or timeout
releases the local continuation instead of leaving the keybind stuck.

### Inventory + filter: drink whatever water you have

**Inventory + filter** was created for a simple HUD command: “drink
something.” I wanted a keybind that could search my inventory for water and
drink whatever it found, because my water regularly moves between a bucket, a
barrel, and other containers.

Instead of remembering one runtime object, this target stores the portable item
type. When the keybind runs, Keybinder searches:

1. the toolbelt;
2. the complete player inventory;
3. the backpack;
4. containers inside the inventory or backpack;
5. containers nested inside other containers.

Material and state descriptions are ignored where appropriate. For example:

- `kindling, oakenwood` is stored as `kindling`;
- `salty water` is stored as `water`.

The action is then applied to the first matching item Keybinder finds. This is
especially useful for target-only actions such as **Drink**.

Only contents currently delivered to the client can be searched. If a closed
container has not loaded its children, open it before running the keybind.

### Smart Improve: toolbelt first

Thanks to Spike for the feedback. Every Smart Improve step now has two
self-explanatory Tool modes:

- **Take tools only from toolbelt**;
- **First toolbelt, then inventory**.

Both modes begin with the toolbelt. The second mode falls back to the inventory
and backpack only when the required tool or material was not found there. It is
also the compatibility default for existing keybinds.

If a container is placed on the toolbelt, Keybinder searches inside it and its
loaded nested containers. Missing-resource messages name the selected search
mode, making it clear where Keybinder looked.

Premium-material protection remains active in either mode. Dragon hide, you
shall not pass.

### Per-character enabled keybinds

Every character can now have a different set of enabled and disabled keybinds.
The checkbox selection is saved separately and restored when that character
logs in.

Several Wurm clients may run at the same time:

- each client has its own live bindings;
- a keybind may be enabled for one character and disabled for another;
- the same key may run different Keybinder records in different clients;
- action-queue tracking is local to each client;
- pending **Pick up from bulk** transfers and their local continuation queues
  are isolated per client;
- concurrent profile saves are serialized so clients do not overwrite one
  another's enabled sets.

The multi-client scenarios have been tested with up to four simultaneously
loaded characters.

### Cleaner startup

Thanks again to Spike for highlighting this issue. After the Introduction,
Keybinder now starts minimised to its **KB** launcher icon instead of opening
the full list of keybinds. If the Introduction was disabled earlier, Keybinder
starts directly in the same minimised state.

Click **KB** whenever you want to open the manager.

### Other reliability fixes

- Drink is treated as a target-only action and no longer displays a Tool
  selector.
- Player Inventory, modified inventory windows, ordinary rows, container rows,
  and world containers resolve consistently as bulk destinations.
- HUD Multi preserves the original hovered object while its selector owns and
  moves the mouse pointer.
- Captured server and mod action names no longer regress to `Unknown action`.
- Bulk-transfer server failures no longer leave the local chain permanently
  waiting for a quantity form.

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
- Smart Improve with stack ordering, queue fitting, strict resource matching,
  premium-material protection, and inventory or examined world-item targets.
- Per-character enabled state across a shared Keybinder installation.
- English and Brazilian Portuguese localization.

Keybinder is a keybind manager, not an unattended automation or scripting
system. It sends ordinary Wurm actions while respecting the character's action
queue.

## Installation and upgrade

1. Disable the old **Custom Actions**, **Improved Improve**, and **i2improve**
   client mods. Keybinder replaces their supported behavior.
2. Download `keybinder-0.7.1.zip` from the
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
