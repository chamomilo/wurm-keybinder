Keybinder 0.11.13 for Wurm Unlimited
==================================

Create, edit and share keybinds from one HUD window. Combine actions and
commands, select tools and targets, and give one key several variants.

Download and illustrated guide:
https://github.com/chamomilo/wurm-keybinder

Forum and support:
https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder

INSTALLATION

Requires Wurm Unlimited and Ago's Client Mod Launcher.

1. Close the Wurm Unlimited client.
2. Extract this ZIP into the client directory, merging its mods folder.
3. Start the client and enable Keybinder in HUD Settings.

To upgrade, replace the supplied Keybinder files. Saved keybinds are separate
from this archive. Before upgrading, back up your existing settings and data:
  mods/keybinder.properties
  mods/keybinder/keybinds.properties
  mods/keybinder/keybinds.properties.accounts
If you configured a different dataFile path, back up that file and its
associated account-state file instead.

Keybinder replaces Custom Actions, Improved Improve and i2improve. Import
their bindings through Keybinder's review, then disable the predecessor
mods for regular play.

GETTING STARTED

Choose Add new keybind. Enter a name, capture a key or mouse-wheel chord,
add steps, choose their tools and targets, then select Done. Search actions
by name or numeric ID. Unknown server or mod IDs require explicit acceptance.

Actions can use hovered or selected objects, inventory filters, nearby
objects, current ride, tiles or areas. Tools can use the active item,
empty hand, toolbelt, equipment or portable item selectors. The tile grid
selects one of nine tiles; C is the current tile. Selecting targets in
the editor sends no actions.

The Value Pack contains eleven disabled examples for review. Set their
keys, replace personal item selections and enable the bindings you want.

Smart Improve selects tools and resources automatically. Archaeology
Identify selects a brush or stone chisel. Shadow recording observes
ordinary actions; resolve any ambiguous tools or targets before saving.

Read instruction opens the detailed in-game guide.

QUICK AND MULTI

Quick: each key press opens a menu. Click a variant to run its sequence.
Closing the menu runs nothing.

Multi: with Quick unchecked and at least two variants, hold the key for
0.2 seconds to choose the active variant. Later short taps run it.
Closing the selector keeps the previous choice. One variant runs directly.

Every variant keeps its own steps, tools and targets. Menu selection uses
no action queue slots.

ACTION QUEUE

Keybinder checks every step before execution. Unavailable steps are skipped
for that invocation and explained in Event. Their saved definitions remain
available for the next press. Available steps run in order and fill only
free queue slots. Multi-target actions send the portion that fits.
Local tool activation uses no queue slots.

The queue monitor can sit at either screen edge. Use keybinder_list in
the console to print managed bindings to Event.

IMPORT, EXPORT AND RESTORE

Import Wurm binds reviews eligible custom bindings before changing them.
Compatible legacy action commands become structured steps. Other commands
retain their text. Imports keep backups and verify live binding ownership.
Use Import file and Export all to share managed bindings.

Prepare mod removal restores imported originals where ownership permits.
Foreign bindings are reported as conflicts.

LANGUAGE AND UPDATES

English, Brazilian Portuguese, German and Russian are supported. Select
the shared language in Mod updates; open Keybinder windows refresh.

This release embeds Chamomilo Updater 1.4.1 and Chamomilo UI 0.4.11.
Participating mods share one update window. The newest compatible embedded
updater is selected at startup. Update, Install and Download open GitHub
release pages after a click. Install ZIPs manually.

CHANGES IN THIS RELEASE

Updated the shared updater and UI, refined list controls and queue monitor,
improved constructor scrolling and variant selection, corrected source
requirements for tool-free actions, and added native-command preflight checks.

ARCHIVE CONTENTS

  mods/keybinder.properties
  mods/keybinder/keybinder.jar
  mods/keybinder/assets/keybinder/kb-tag.png
  mods/keybinder/README.txt
  mods/keybinder/LICENSE
  mods/keybinder/lgpl-3.0.txt

The archive contains no saved player data, repository images or source files.

CREDITS AND LICENSE

Keybinder is derivative work based on bdew's Custom Actions. Thanks to bdew
for the original implementation.
https://github.com/bdew-wurm/action
https://github.com/bdew-wurm/action/releases

Smart Improve is a clean-room reimplementation inspired by Munsta0
(Improved Improve), inniria (i2improve) and Snidor (continued i2improve).

GNU LGPL 3.0 or later. See lgpl-3.0.txt and the accompanying GPL license
in LICENSE. Bundled Alegreya Sans fonts retain their OFL notice in the JAR.

Source code:
https://github.com/chamomilo/wurm-keybinder

Chamomilo
