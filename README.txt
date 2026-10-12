Keybinder 1.0.0 for Wurm Unlimited
================================

Build and manage keybinds from one HUD window. Combine actions and commands,
choose tools and targets, and give a single key several useful variants.

Download and illustrated guide:
https://github.com/chamomilo/wurm-keybinder

Forum and support:
https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder

INSTALLATION AND UPGRADES

Requires Wurm Unlimited and Ago's Client Mod Launcher.

1. Close the client and extract this ZIP into its directory, merging mods.
2. Start Wurm and enable Keybinder in HUD Settings.

To upgrade, replace the supplied Keybinder files. Saved bindings are separate
from this archive. Back up your existing settings and binding files:
  mods/keybinder.properties
  mods/keybinder/keybinds.properties
  mods/keybinder/keybinds.properties.accounts
  ~/.chamomilo/mod-settings/keybinder.properties
Here ~ means your user home directory. If you configured a different dataFile
path, back up that file and its account-state file instead.

Keybinder replaces Custom Actions, Improved Improve and i2improve. Review
and import their bindings, then disable the predecessor mods for regular play.

GETTING STARTED

Choose Add new keybind. Enter a name, capture a key or mouse-wheel chord,
add steps in order, select tools and targets, then choose Done.

Search the current client action catalog by name or numeric ID. Unknown
server or mod IDs require explicit acceptance. Tool-free actions show only
the target. Read instruction opens the detailed in-game guide.

Supported targets include hovered or selected objects, inventory filters,
nearby objects, current ride, tiles and areas. Tools can use the active item,
empty hand, toolbelt, equipment or portable item selectors. In the tile grid,
C is the current tile. Choosing targets in the editor sends no actions.

The Value Pack contains eleven disabled examples for review. Set their keys,
replace personal item selections and enable the bindings you want.

Smart Improve selects tools and resources automatically. Archaeology Identify
selects a brush or stone chisel. Shadow recording observes ordinary actions;
resolve ambiguous tools or targets before saving.

QUICK AND MULTI

Quick: each key press opens a menu. Click a variant to run its sequence.
Closing the menu runs nothing.

Multi: with Quick unchecked and at least two variants, hold the key for
0.2 seconds to select the active variant. Later short taps run it.
Closing the selector preserves the previous choice. One variant runs directly.

Each variant has its own steps, tools and targets. Menu selection uses no
queue slots.

ACTION QUEUE

Keybinder checks every step before execution. Unavailable steps are skipped
for that invocation and explained in Event. Their saved definitions remain
available for the next press. Available steps run in order and fill only free
queue slots. Multi-target actions send the portion that fits. Local tool
activation uses no queue slots. Use keybinder_list to print managed bindings.

IMPORT, EXPORT AND RESTORE

Import Wurm binds reviews custom bindings before changing them. Compatible
legacy action commands become structured steps; other commands retain their
text. Imports keep backups and verify live binding ownership.

Import file and Export all share managed bindings. Prepare mod removal restores
imported originals where ownership permits. Foreign bindings cause conflicts.

SETTINGS IN MOD UPDATES

Open Mod updates and click Settings on the Keybinder card. Each parameter has
hover help. Valid changes save automatically.

  Center view after embark: align the view with the vehicle heading.
    Applies immediately.
  Action queue position: choose the left or right screen edge.
    Applies immediately.
  Keybind file: choose the file storing keybind definitions.
    Applies on the next client start.

Changing Keybind file shows a restart notice after saving. Continue configuring
keeps the client running. Exit game opens Wurm's normal quit confirmation.
The active binding file stays unchanged until the next start. Restoring the
previous value clears that restart reminder. Changing the path does not move
existing bindings into the newly selected file.

Overrides are stored in ~/.chamomilo/mod-settings/keybinder.properties,
where ~ means your user home directory. They survive replacement of the
installation files. Include this file in your configuration backup.

LANGUAGE AND UPDATES

English, Brazilian Portuguese, German and Russian are supported. Select the
shared language in Mod updates. Existing Keybinder windows refresh without
restarting; unfinished settings input and scroll position are preserved.

This release embeds Chamomilo Updater 1.5.0 and Chamomilo UI 0.4.11.
Participating mods share one update window and select the newest compatible
embedded updater at startup. Failed catalogue logo downloads retry.
Update, Install and Download open GitHub release pages after a click.
Install release ZIPs manually.

ARCHIVE CONTENTS

  mods/keybinder.properties
  mods/keybinder/keybinder.jar
  mods/keybinder/assets/keybinder/kb-tag.png
  mods/keybinder/README.txt
  mods/keybinder/LICENSE
  mods/keybinder/lgpl-3.0.txt

The archive contains no player data, repository images, source files or drafts.

CREDITS AND LICENSE

Keybinder is derivative work based on bdew's Custom Actions.
Thanks to bdew for the original implementation.
https://github.com/bdew-wurm/action
https://github.com/bdew-wurm/action/releases

Smart Improve is a clean-room reimplementation inspired by Munsta0
(Improved Improve), inniria (i2improve) and Snidor (continued i2improve).

GNU LGPL 3.0 or later. See lgpl-3.0.txt and the accompanying GPL license
in LICENSE. Bundled Alegreya Sans fonts retain their OFL notice in the JAR.

Chamomilo
