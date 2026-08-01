<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.6.2 for Wurm Unlimited

Tired of managing keybinds through console commands, juggling toolbelt setup
files, and remembering dozens of keys? Keybinder replaces all of that with one
clear in-game window. All your keybinds are visible in one place, where you can
create, edit, disable, or delete them without digging through configuration
files or memorizing commands.

A single key can perform an entire routine. Start recording, carry out the
actions normally, and Keybinder will remember them for you. Want a key for
bashing stumps or smelting items? Easy. Want to chop anything in your kitchen
with just one key? Easy. You can even assign several keybinds to the same key
and switch between them in-game with a one-second long press. Keep all your
favorite actions on one key and select the one you need without becoming a
keyboard pianist.

Keybinder even makes the mouse wheel useful: bind it to actions such as moving
and rotating objects. Less console work, less clicking, fewer keys to remember:
just press a key or turn the wheel and get back to playing.

## What's new in 0.6.2

- Fixed **Nearby by type** targets reverting to **Hovered** when a keybind was
  saved immediately after selecting an object.
- Smart Improve can now find the required tool inside a backpack or another
  container placed in a toolbelt slot.
- Smart Improve processes inventory targets in a predictable order without
  changing their visible order in Wurm and ignores duplicate target entries.

## Previously in 0.6.1

- Target selection is now more reliable across inventories, containers, the
  toolbelt, equipment, and the game world. Keybinder can select a tool from
  your backpack automatically.
- Added **Import Wurm keybinds** and **Prepare mod removal** controls to the
  main window. Keybinder can restore imported commands without overwriting
  keys that now belong to something else.
- Movement, camera, and essential HUD controls are excluded from import, so
  Keybinder does not take ownership of Wurm's fundamental controls.
- Complete English and Brazilian Portuguese localization with a persistent
  Wurm-styled language selector and a safe English fallback. More languages
  are welcome!
- There are now two ways to add an action: perform it and let Keybinder record
  it, or choose it from the complete list of vanilla actions.
- Keybinder checks the available action queue before it starts. If the entire
  keybind cannot fit, nothing is sent. Empty your queue and press the key again.
- Repeated `Push` and `Push gently` actions retain the selected object across
  every server-side recreation. Finally, you can push, push, push, and keep
  going.
- An unavailable target skips only its own step and writes the reason to the
  system Event tab. The remaining steps continue in their saved order.
- Missing `nearby` targets are skipped silently. If a target is visible but
  outside the action range, Keybinder writes a useful "come closer" message to
  the Event tab.
- Your current mount or vehicle can now be used as a target. Want to disembark
  or open a cart hold with one key? Easy.
- All your accounts can use the same keybind list.
- Embarking on a vehicle turns your view to face the same direction as the vehicle.

## Installation

1. Disable the old `action` and `i2improve` client mods.
2. Extract `keybinder-0.6.2.zip` into the Wurm Unlimited client directory using
   Ago's mod loader as usual.
3. If the Keybinder introduction does not appear after startup, enable
   **Keybinder** in HUD Settings.

## Runtime commands

You do not need these, but if you still prefer the console, they are available:

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
