<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.1 for Wurm Unlimited

## One key, many actions

Now with bulk-storage transfers, portable inventory filters, per-character
profiles, and rebuilt Smart Improve.

[Read and discuss the Keybinder forum post on SKLOTOPOLIS](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder).

Keybinder 0.7.1 is ready for testing.

Keybinder puts all your Wurm keybinds into one in-game window. You can capture
an action, choose its tool and target, combine several actions into one key,
and manage everything without writing console commands or editing configuration
files.

## What is new in 0.7.1?

### Pick up from bulk

Action chains can now take an exact item type from bulk storage and put a
chosen quantity into another inventory. The source picker supports bulk storage
bins, food storage bins, small and large crates, bulk container units, and bulk
containers opened inside vehicles.

Destinations can be:

- player inventory;
- the inventory window, container row, or world container under the pointer;
- an exact captured inventory or container.

Several bulk transfers can be used in one keybind and mixed with ordinary
actions. Keybinder waits locally for each server quantity form, answers it, and
then continues the chain in order.

### Inventory + filter

`Inventory + filter` stores only the portable item type, never a runtime item
ID. At execution time Keybinder searches the toolbelt first, then the complete
player inventory and backpack tree, including containers nested inside other
containers.

Materials and item-state adjectives are ignored where appropriate. For
example, `kindling, oakenwood` matches `kindling`, and `salty water` matches
`water`. This makes target-only actions such as Drink portable between
containers and characters.

### Per-character enabled state

Each character can now enable and disable its own managed keybinds. The
checkbox selection is saved with the account profile and restored when that
character logs in without changing another alt's selection.

### Smart Improve search order

Smart Improve now searches the toolbelt before descending into inventory,
backpack, and nested containers. Its material and tool matching remains strict,
including protection against accidental premium-material use.

### Reliability fixes

- HUD Multi preserves the original hovered object while its selector owns and
  moves the mouse pointer.
- Player Inventory, modified inventory windows, container rows, ordinary
  inventory rows, and world containers resolve consistently as bulk targets.
- Bulk requests time out and release their local continuation instead of
  leaving later keybind executions blocked indefinitely.
- Known server rejection messages release pending bulk transfers immediately.
- Drink is correctly treated as a target-only action with no Tool selector.
- Action names captured from server and mod actions no longer regress to an
  `Unknown action` placeholder.

## What was new in 0.7.0?

### A separate Tool selector for every action

Every action step is now defined by a **Tool | Action | Target** row.

The tool can be:

- your current active item;
- empty hand;
- a hovered inventory item;
- a toolbelt slot;
- an equipment slot;
- an exact object from your inventory, toolbelt, or equipment slot.

For example, a Smart Farmer keybind for `E` can contain:

1. Rake | Farm 7×7 | Hovered
2. Scythe | Harvest & replant 7×7 | Hovered

One press of `E` performs both farming and harvest-and-replant. The tools are
picked directly from inventory.

### Hovered + filter and Nearby + filter

Your keybind can now check whether a hovered or nearby item belongs to a chosen
item type.

Examples:

- **Nearby + filter | Stump** executes only when a stump is nearby.
- **Hovered + filter | Minced meat** executes only when your pointer is over
  minced meat.

Both commands ignore material, so a cedarwood stump and a walnut stump are both
treated as stumps.

With this, you can build one keybind to dice or mince any type of meat, another
to chop any vegetables or herbs, and so on. Experiment!

### Duplicate, merge, and extract

Handling keybinds is now easier:

1. **Duplicate.** If your main Multi-keybind contains 15 commands and you want
   the same set with a different key for an alt, one button does it.
2. **Merge.** Drag a standalone keybind and drop it onto your Multi-keybind to
   add it there.
3. **Extract.** If you merged the wrong command, open the Multi-keybind and
   press Extract.

### New type of Multi-keybind: HUD button

**Ordinary Multi** executes the selected command on a short press and opens the
command list on a long press so you can select another action.

**HUD Multi** opens its menu on a short press and immediately executes the
selected action.

This lets you make one key for many HUD actions: inventory, backpack, cart hold,
sleep bonus, livemap, drink water, pray to Fo, main menu, climb, and more.

The mouse pointer automatically jumps to the selection when you press the HUD
key.

### Portable `.keybinder` files

Keybinder now has **Import file** and **Export all**. Use them to share your
keybind set with another Steam account or a friend.

### Bundled Keybind Essentials (Value Pack)

The update installs examples that answer the question: “What can I do with
Keybinder?” Enable them, see how they work, and start experimenting.

Do not forget to try mouse-wheel-based push, pull, and turn bindings. Now you
are Yoda—or Darth Vader. Choose for yourself.

### Smart Improve rebuilt

Smart Improve was redesigned from scratch again, mainly to remove unclear i2i
defaults and limitations.

1. **Inventory instead of toolbelt.** Smart Improve does not care what you have
   on your toolbelt. It browses your inventory and backpack and uses whatever
   you keep there for improving. Drop your glowing-hot lumps into your pocket
   and start working.
2. **Premium materials are protected.** Smart Improve never uses premium
   materials such as dragon hides as ordinary Improve materials. A dedicated
   check keeps them safe. It can still use a supreme string.
3. **No outside limitation.** It works on improvable outside objects, including
   any altar material. Start with a double-click on the object.
4. **Correct stack sequence.** Stack improving selects the lowest-quality items
   and improves them first.
5. **Server friendly.** Smart Improve calculates exactly how many Repair and
   Improve actions fit into the current action queue. Press the key as often as
   you want; only commands that fit are sent to the server.
6. **Smart material and tool selection.** It distinguishes marble from stone
   shards, carving knives from stone chisels, yarn from string, and handles logs
   correctly.
7. **Roleplay.** Take your future Ring of Power—currently just a simple,
   low-quality gold ring—place it on an anvil, and work on it there. Put the item
   on the table and improve it in the world.

### Safe upgrade and storage

Overwrite the `mods` folder in your WurmLauncher directory. Your existing
keybinds are safely backed up and are not lost.

## A few things Keybinder already does

- Captures an action you perform and turns it into an editable action step. All
  server and client mods are supported.
- Combines several Wurm actions into one keybind while respecting your action
  queue limit.
- Combines several keybinds into one Multi-keybind, giving you a convenient
  command switcher for daily tasks.
- Supports hovered, selected, exact, nearby, filtered, tile, area, inventory,
  equipment, toolbelt, and current-ride targets.
- Enables mouse-wheel bindings; push, pull, and rotate are examples.
- Supports embark and disembark bindings and turns your head toward the horses
  when you embark.
- Shares managed bindings between your alts and remembers who enabled what.
- Checks the available Wurm action queue before sending a complete binding.
- Imports your keybinds and lets you work with them.
- Can be localized for specific languages.
- Does not force you to use the console or chat.

Keybinder is a keybind manager, not a scripting or unattended automation
system. It sends the same ordinary Wurm actions a player can perform while
respecting the character's action queue. It simply makes Wurm much more
convenient.

## Installation and upgrade

1. Disable the old **Custom Actions** and **i2improve** client mods.
2. Download [Keybinder 0.7.1 from GitHub](https://github.com/chamomilo/wurm-keybinder/releases).
3. Extract `keybinder-0.7.1.zip` into your Wurm Unlimited client directory, as
   usual for Ago's Client Mod Launcher.
4. When upgrading, overwrite the previous Keybinder files. Your managed
   bindings are stored separately and are not replaced by the distribution
   archive.
5. Start the game. If the Keybinder introduction does not appear, enable
   **Keybinder** in **HUD Settings**.

The console is optional. Everything needed for normal use is available in the
in-game interface.

## Credits and license

Keybinder began as derivative work based on
[bdew's Custom Actions](https://github.com/bdew-wurm/action).

Smart Improve is a clean-room reimplementation inspired by the work of:

- **Munsta0**, creator of the original Improved Improve;
- **inniria**, creator of i2improve;
- **Snidor**, who continued i2improve.

No source code from those Improved Improve mods is bundled.

Keybinder is licensed under **GNU LGPL 3.0 or later**.

**Thank you FlpSilva and Wolfbane for beta testing!**

If you find a bug, have an idea, or would like to help with another translation,
please reply in the [SKLOTOPOLIS forum thread](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)
or message **Chamomilo** on SKLOTOPOLIS.

Cheers! **Chamomilo**
