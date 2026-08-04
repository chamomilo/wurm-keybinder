<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.1 for Wurm Unlimited

## One key, many actions

Keybinder is a Wurm Unlimited client mod for creating and managing keybinds
through a Wurm-styled in-game interface. Capture an action, choose its tool and
target, combine several actions under one key, and keep different enabled sets
for different characters without editing configuration files or writing console
commands.

Keybinder 0.7.1 is ready for testing.

- [Download releases from GitHub](https://github.com/chamomilo/wurm-keybinder/releases)
- [Read and discuss the SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## What is new in 0.7.1?

### Pick up from bulk

An action chain can take a chosen quantity of an exact item type from bulk
storage and place it into another inventory. Supported sources include:

- bulk storage bins;
- food storage bins;
- small and large crates;
- bulk container units;
- bulk containers opened inside vehicles.

Select the item row in the opened bulk container. Keybinder remembers both the
item and its source storage, so the source window may then be closed.

The destination can be:

- **Player inventory**;
- **Hovered inventory**: an opened inventory window, a container row, or a
  container in the world;
- **Captured inventory**: one exact inventory or container selected while the
  keybind is being edited.

Several bulk transfers can be placed in one keybind and mixed with ordinary
actions. Keybinder sends one transfer, waits locally for the matching server
quantity form, answers it, and only then continues the chain. Server rejection
messages and timeouts release the local continuation instead of leaving later
executions blocked.

### Inventory + filter

**Inventory + filter** stores a portable item type rather than a runtime object
ID. At execution time it searches:

1. the toolbelt;
2. the complete player inventory and backpack tree;
3. containers nested inside other containers.

Materials and state descriptions are ignored where appropriate. For example,
`kindling, oakenwood` matches `kindling`, while `salty water` matches `water`.
This makes target-only actions such as Drink portable between containers and
characters.

Only contents currently delivered to the client can be searched. If a closed
container has not loaded its children, open it before running the keybind.

### Smart Improve source modes

Every Smart Improve step has a **Tool** dropdown with two modes:

- **Take tools only from toolbelt**;
- **First toolbelt, then inventory**.

The second mode is the compatibility default for existing keybinds. In both
modes, a container placed in a toolbelt slot is searched recursively, including
its loaded nested containers. The inventory fallback is used only by the second
mode.

Missing-resource messages name the selected source mode, so it is clear where
Keybinder searched. Tool and material matching remains strict, and ordinary
Improve lookup continues to reject protected premium materials.

### Independent character profiles

Keybind definitions are shared, but every character keeps its own enabled and
disabled selection. On login Keybinder first removes the known managed binds
from that client's live bind table, then installs the saved set for the current
character.

Several Wurm clients may run at the same time:

- each client has its own live bindings;
- the same key may run different Keybinder records on different characters;
- action-queue tracking is local to the client;
- pending and queued **Pick up from bulk** operations are local to the client;
- concurrent profile saves are serialized so one client cannot overwrite
  another character's enabled set.

A conflict is retained only when the live key belongs to a foreign vanilla
command or to an unknown `keybinder_run` command whose record has already been
deleted from the shared definition table.

### Cleaner startup

After the Introduction is completed, Keybinder starts collapsed to its launcher
icon. If the Introduction was disabled earlier, it starts directly in the same
collapsed state. Click the icon to open the full keybind manager.

### Reliability fixes

- HUD Multi preserves the original hovered object while its selector owns and
  moves the mouse pointer.
- Player Inventory, modified inventory windows, ordinary rows, container rows,
  and world containers resolve consistently as bulk destinations.
- Drink is treated as a target-only action and does not display a Tool selector.
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
system. It sends the same ordinary Wurm actions available to the player while
respecting the character's action queue.

## Installation and upgrade

1. Disable the old **Custom Actions**, **Improved Improve**, and **i2improve**
   client mods. Keybinder replaces their supported behavior.
2. Download `keybinder-0.7.1.zip` from the
   [GitHub releases page](https://github.com/chamomilo/wurm-keybinder/releases).
3. Extract the archive into the Wurm Unlimited client directory used by Ago's
   Client Mod Launcher.
4. When upgrading, overwrite the existing Keybinder files. Managed definitions
   are stored separately and are not replaced by the distribution archive.
5. Start the game. If the launcher icon is not visible, enable **Keybinder** in
   **HUD Settings**.

The default shared definition file is:

```text
mods/keybinder/keybinds.properties
```

Per-character enabled selections are stored beside it in:

```text
mods/keybinder/keybinds.properties.accounts
```

Keybinder uses atomic replacement and rolling backups for its structured data.
The console remains optional for normal use.

## Reporting problems

Enable **Debug logging** in Keybinder when reproducing a problem. Include the
relevant `[Keybinder]` Event lines, the selected step type, Tool mode, Target,
and whether the source or destination container was opened in its own window.

Reports and feature ideas are welcome in the
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
