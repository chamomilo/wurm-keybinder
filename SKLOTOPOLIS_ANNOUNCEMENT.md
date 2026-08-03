# [Client Mod] Keybinder 0.5.4 — One key, many actions

Hello, SKLOTOPOLIS!

I would like to introduce **Keybinder**, a new client mod for **Wurm Unlimited** that makes keyboard controls easier to create, understand, and manage — all through a Wurm-styled in-game interface.

No more digging through configuration files, building complicated console commands, or trying to remember dozens of keys. Keybinder brings your custom keybinds together in one window, where you can create, edit, disable, and delete them.

## What can Keybinder do?

### Record what you do

Start capturing, perform an action normally through the Wurm menu, and Keybinder will remember it. You can also choose actions directly from the complete vanilla action list.

### Put a whole routine on one key

A single keybind can contain several actions, tools, and targets, up to your character's action queue limit.

For example, you can create:

- a key that bashes stumps;
- a key that smelts selected items;
- a key that chops nearby items of a chosen type;
- a sequence that activates the correct tool and performs several actions;
- convenient mouse-wheel binds for moving or rotating objects.

This is **not automation or scripting**. Keybinder simply gives you a much more convenient way to perform ordinary Wurm actions through custom keybinds.

### Use intelligent targets

Each action can use the target you need: the hovered or selected object, an exact object, a nearby object or object type, your current mount or vehicle, a tile, a 3×3 area, an inventory item, equipment, or a toolbelt slot.

Keybinder can even activate a tool directly from your backpack before performing an action.

### Keep several actions on the same key

With multi-keybinds, several bindings can share one key. Tap it to use the active action, or hold it for more than one second to choose another. Your favourite actions stay close without turning you into a keyboard pianist.

### Improve items with one smart key

Keybinder includes **Smart Improve**, an updated reimplementation inspired by the Improved Improve family of mods. Choose an improve key and keep the required tools and materials in your inventory or backpacks; Keybinder will select the appropriate resource for the item.

The old Improved Improve and i2improve mods are not required.

### Import your existing keybinds

Keybinder can review and import custom Wurm keybinds and compatible Custom Actions binds, allowing you to manage everything from one window. Imported commands can later be restored safely, without overwriting keys that have since been assigned to something else.

Movement, camera controls, and essential HUD keys are excluded from import.

## New in version 0.5.4

- More reliable target selection across inventories, containers, equipment, the toolbelt, and the game world.
- Automatic tool selection from your backpack.
- **Import Wurm keybinds** and **Prepare mod removal** controls in the main window.
- Action capture and selection from the complete vanilla action list.
- Safe queue preflight: if the entire sequence cannot fit into your action queue, no actions are sent.
- Reliable repeated `Push` and `Push gently` actions, even when the server recreates the moved object.
- Better handling of unavailable and out-of-range targets, with useful messages in the Event tab.
- Support for your current mount or vehicle as a target.
- One shared keybind list for all accounts.
- Complete English and Brazilian Portuguese localization, with a Wurm-styled language selector. More translations are welcome!

## Installation

1. Disable the old `action` and `i2improve` client mods.
2. Download **Keybinder 0.5.4**: **[ADD DOWNLOAD LINK HERE]**
3. Extract `keybinder-0.5.4.zip` into your Wurm Unlimited client directory, as usual for Ago's Client Mod Launcher.
4. Start the game. If the introduction does not appear, enable **Keybinder** in **HUD Settings**.

You do not need to use the console. Everything can be configured through the in-game interface.

## Credits and license

Keybinder began as derivative work based on **bdew's Custom Actions**:
https://github.com/bdew-wurm/action

Smart Improve is a clean-room reimplementation inspired by the work of:

- **Munsta0**, creator of the original Improved Improve;
- **inniria**, creator of i2improve;
- **Snidor**, who continued i2improve.

Keybinder is licensed under **GNU LGPL 3.0 or later**. No source code from the Improved Improve mods is bundled.

I hope you enjoy Keybinder as much as I do. If you find a bug, have an idea, or would like to help with a translation, please reply here or message **Chamomilo** on SKLOTOPOLIS.

Cheers!  
**Chamomilo**
