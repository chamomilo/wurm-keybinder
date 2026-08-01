<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.7.0 for Wurm Unlimited

Tired of managing keybinds through console commands, juggling toolbelt setup
files, and remembering dozens of keys? Keybinder replaces all of that with one
clear in-game window. It is a convenient keybind manager with many QOL improvements.
All your keybinds are visible in one place, where you can create, edit, disable, or delete them
without using the console, digging through configuration files, or memorizing commands.

Start with a single key binding. Let Keybinder capture your action and convert it into a keybind.
Add target selection like an exact object, nearby item, or toolbelt slot. Each custom action has an independent
**Tool | Action | Target** row, so it can resolve a tool without changing the active item.
So, with your keypress you do it all - select the tool, target, and do the action.
Then, add more actions to the same keybind - "select rake"+"farm hovered tile 7x7"+"harvest and replant 7x7 over hovered tile".
Enjoy a complex action by pressing one key. Nice?

Then, add an alternative keybind to the same key. Repeat 13 times - now you have 15 keybinds on 1 key. Switch by long press.
Dice meat - mince meat - chop veggie - chop herb - all on one key, easily switched in 1 second. Nice? It's called Multi-keybind.

Then, bind Mouse wheel up and down with push and pull commands. Feel like a telekinetic mage :)
Then bind embark to Tab, disembark to Shift-Tab. Super convenient. When embarking, you will face your horses automatically. Nice?

Get rid of a 10-year-old mod with complex programming and console usage.
The future has come, and it's convenient.

## What's new in 0.7.0

- Every custom action now has its own portable **Tool** selector: current active item,
  empty hand, hovered inventory item, toolbelt slot, equipment slot, or an exact
  inventory object. The advanced **Switch active item** step remains available when
  later actions really should inherit a new active item.
- Added **Hovered + filter** and renamed the visible nearby option to
  **Nearby + filter**. Both compare normalized object types without
  material, articles, or creature condition prefixes.
- Added **Duplicate**, row double-click editing, and drag-to-merge. A center drop
  appends all alternatives after confirmation; the destination keeps its key,
  HUD mode, active alternative, and restore history. The source record and its
  independent restore history are removed.
- Alternative actions can now be extracted with the curved-arrow icon beside
  Remove. Extraction saves current editor changes, removes that alternative from
  its parent, and inserts a disabled standalone keybind directly below it. Names
  receive derived `Multi` and `HUD Multi` prefixes; the prefixes and multi/HUD
  tracking are removed automatically when only one alternative remains.
- Added portable `.keybinder` **Import file** and **Export all**. Imported records
  receive new identities and stay disabled until reviewed. Exact object IDs are
  deliberately marked non-portable and require extra review.
- Multi-keybinds now offer separate **Ordinary Multi** and **HUD Multi** behavior.
  Ordinary Multi runs the active choice on a short press and opens a select-only
  menu on a long press; the pointer moves to the active choice once. HUD Multi
  opens immediately and executes the item chosen from the menu.
- Smart Improve now fills only the currently free queue slots. A damaged inventory
  item normally costs exactly two actions (Repair + Improve), while an undamaged
  item costs one. A metal target that is not glowing is repaired when damaged but
  is not improved, so it costs one action when damaged and zero otherwise. A cold
  metal lump is not used as an improve material; Keybinder prefers another matching
  glowing lump and otherwise applies only the optional Repair. Large selections are
  trimmed to a deterministic fitting prefix instead of being rejected in full.
  Damage is checked again immediately before sending, and the real sequential
  `Repair, Improve` / `Improve` command list is never expanded by a hidden queue.
- The persisted definition format is now schema 8. Before the first successful
  schema-8 save, Keybinder creates `mods/keybinder/keybinds.pre-v8.properties`
  once. To downgrade, close the client and manually restore that file as
  `keybinds.properties`; the normal rolling `.bak` remains a separate recovery file.

## Previously in 0.6.2

- Fixed **Nearby by type** targets reverting to **Hovered** when a keybind was
  saved immediately after selecting an object.
- Smart Improve can now find the required tool or material inside a backpack or another
  container placed in a toolbelt slot. Yes, now you can have a sack, backpack, or huge tub called "imping wood item" in
  toolbelt slot 1, and it's enough. Keep related mats and tools inside. C for convenience.
- Smart Improve processes inventory targets in a predictable order - if you selected several items,
  one by one or in a stack, it will start with the item of the lowest ql.

## Previously in 0.6.1

- Target selection is now more reliable across inventories, containers, the
  toolbelt, equipment, and the game world. Keybinder can select a tool from
  your toolbelt, backpack, inventory or outside easily.
- Added **Import Wurm keybinds** and **Prepare mod removal** controls to the
  main window. Keybinder can restore imported commands without overwriting
  keys that now belong to something else.
- Vanilla keybinds like movement, camera, and essential HUD controls are excluded
  from import, so no mess inside Keybinder list of keybinds.
- Complete English and Brazilian Portuguese localization with a persistent
  Wurm-styled language selector and a safe English fallback. More languages
  are welcome! Ask me for help with this.
- There are now 3 ways to add an action: perform it and let Keybinder record
  it, choose it from the complete list of vanilla actions, or enter your console command directly.
- Keybinder is server-friendly. It checks the remaining slots in action queue on key press. If the entire
  keybind cannot fit into the remaining queue, it will tell you "sorry, you are still too busy, push it a bit later".
  And nothing will go to server this time.
- Sometimes, with complex keybinds, some of the commands cannot find the proper target on keypress.
  Again, server-friendly. Keybinder will just skip those exact steps and send nothing to server. It's safe.
  "chop tree"+"bash nearby stump"+"chop up felled tree"+"pick up any log"+"pick up scraps" = E key for tree chopper.
  Like it? Give it a try!
- Even more server-friendly. If, for instance, you have Push on a keybind and try to push your horse :)
  What will happen? Answer - nothing will happen. And the command will not go to the server. Keybinder will even
  notify you about your tragic fault in Event tab.
- There was a bug in Wurm with `Push` and `Push gently` commands - after them, the server deselects your target.
  Inconvenient, I know. Fixed. Now you are a true Anakin, a telekinetic guy. Try it yourself.
- All "nearby" targets are now smart. They check the surroundings, keeping in mind the commands which you are using.
  Some commands have very short range, some very long. If a target is close, but not close enough,
  Keybinder will confirm you are doing well but need to come closer.
- Your current mount or vehicle can now be used as a target. Want to disembark
  or open a cart hold with one key? Easy.
- All your alts use the same keybinds list. What one created, all can use. It's so convenient!

## Installation

1. Disable the old `action` and `i2improve` client mods.
2. Extract `keybinder-0.7.0.zip` into the Wurm Unlimited client directory using
   Ago's mod loader as usual.
3. If the Keybinder introduction does not appear after startup, enable
   **Keybinder** in HUD Settings.

## Runtime commands

You do not need these, but if you can't live without the console, they are available:
```text
keybinder_run <managed-id>
keybinder_list [commands]
```

## Attribution

Keybinder began as derivative work based on bdew's
[Custom Actions](https://github.com/bdew-wurm/action), licensed under
LGPL-3.0-or-later.

The Smart Improve feature is a clean-room reimplementation inspired by the
complete Improved Improve mod lineage. Special thanks to:

- [Munsta0](https://github.com/munsta0/WUClientImprovedImprove), who created
  the original Improved Improve client mod;
- [inniria](https://github.com/inniria/i2improve), who rewrote and extended it
  as i2improve;
- [Snidor](https://github.com/Snidor/i2improve), who continued i2improve and
  whose 0.2.1 release was used as the final research baseline.

No source code from these Improved Improve mods is bundled.

See `lgpl-3.0.txt` for the inherited Custom Actions license.
