<p align="center">
  <img src="images/keybinder-banner.png" alt="Keybinder for Wurm Unlimited" width="800">
</p>

# Keybinder 0.11.13

Create, edit and share Wurm Unlimited keybinds from a single HUD window. Combine actions and commands, select tools and targets, and give one key several useful variants.

[Download Keybinder](https://github.com/chamomilo/wurm-keybinder/releases/latest) · [Forum and support](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## Install

Requires **Wurm Unlimited** and **Ago's Client Mod Launcher**.

1. Download `keybinder-0.11.13.zip` from the release page.
2. Close the client. Extract the archive into the client directory so its `mods` folder joins the existing one.
3. Start Wurm and enable **Keybinder** in **HUD Settings**.

For an upgrade, replace the supplied Keybinder files. Your saved keybinds are separate from the distribution. Back up `mods/keybinder/keybinds.properties`, its account-state file and your existing `mods/keybinder.properties` before upgrading if you want to preserve a copy of your data and settings.

The archive contains the Keybinder JAR, mod descriptor, required HUD artwork, **README.txt** and license notices. Repository images and source files stay outside the installation archive.

Keybinder replaces **Custom Actions**, **Improved Improve** and **i2improve**. Review and import their bindings in Keybinder, then disable the predecessor mods for regular play.

## Your keybinds in one place

Add, edit, duplicate, reorder and merge bindings. Filter the list by player or server, enable the bindings you need, and import or export them as files. Enabled state is tracked per character.

![Keybinder keybind list](images/keybinds.png)

The bundled **Value Pack** offers eleven starting examples, including a Q Quick menu, an E Multi binding, camera controls and a sprout chain. Examples arrive disabled for review. Set their keys, replace personal item selections and enable the ones you want.

## Build an action sequence

Choose **Add new keybind**, enter a name and capture a key or mouse-wheel chord. Add actions or commands in execution order, choose the source tool and target for each action, then select **Done**.

![Keybind constructor with two variants](images/constructor.png)

Keybinder supports:

- Action chains, vanilla key actions and console commands.
- Hovered and selected objects, inventory filters, nearby objects, current ride, tiles and areas.
- Active tools, empty hand, toolbelt slots, equipment slots and portable item selectors.
- **Smart Improve**, with automatic tool and resource selection and client-side chance estimates.
- **Archaeology Identify**, with automatic brush or stone-chisel selection.
- Command catalogs supplied by compatible client mods.
- **Shadow recording**, which observes actions you perform and asks you to resolve ambiguous tools or targets before saving.

Search the client action catalog by name or numeric ID. Unknown server or mod action IDs require explicit acceptance. Actions that do not use a tool show their target directly.

### Tile targets

Choose one tile relative to the player, or select the whole 3×3 area. **C** is the current tile. Choosing a target in the editor does not send an action.

![Nine-cell tile target selector](images/tile-targets.png)

### Quick menus and Multi bindings

**Quick:** each key press opens a menu. Clicking a variant runs its sequence immediately. Closing the menu runs nothing.

**Multi:** with Quick unchecked and at least two variants, hold the key for **0.2 seconds** to choose the active variant. Choosing it saves the selection; later short taps run it. Closing the selector keeps the previous choice. A binding with one variant runs directly.

![Multi binding variant selector](images/multi-selector.png)

Each variant has its own steps, tools and targets. Menu selection uses no action queue slots.

## Queue and feedback

Before executing a binding, Keybinder checks every step against the current tools, targets and action queue. It skips unavailable steps for that invocation and explains the reason in **Event**. Saved steps remain available for the next press.

Available steps run in order and fill only the free queue capacity. Multi-target actions send the portion that fits. Local tool activation uses no queue slots. The queue monitor can sit at either screen edge.

Use `keybinder_list` to print managed bindings to **Event**. The in-game instruction page explains additional settings, import, export and recording.

## Import and restore

**Import Wurm binds** opens a review of eligible custom bindings. Compatible legacy action commands become structured Keybinder steps; other commands retain their text. Imports keep backups and check that the live binding still matches before changing it.

**Prepare mod removal** restores imported originals where ownership permits. A key occupied by a different binding is reported as a conflict.

## Language and updates

The interface supports **English, Brazilian Portuguese, German and Russian**. Choose the shared language in **Mod updates**; open Keybinder windows refresh without restarting.

This release embeds **Chamomilo Updater 1.4.1** and **Chamomilo UI 0.4.11**. Compatible Chamomilo mods share one update window. The newest compatible embedded updater is selected at startup, including when older mod versions are installed alongside it.

**Update**, **Install** and **Download** open the relevant GitHub release page after a click. Install release ZIPs manually.

## Changes in 0.11.13

- Updated the complete shared updater and UI module.
- Refined list buttons, queue monitor scaling, text and click areas.
- Improved constructor selection and scrolling when adding or reordering variants and steps.
- Corrected tool availability for actions with explicit source requirements.
- Added preflight checks for invalid native actions and nested Keybinder calls.
- Updated the illustrated guide and kept the installation archive limited to mod files and required notices.

## Build from source

Use **JDK 8**, the included Gradle wrapper and the pinned libraries in `libs/`: `client-patched.jar`, `common.jar`, `javassist.jar` and `modlauncher.jar`.

The build uses the shared updater project at `C:/projects/updater`. Set `CHAMOMILO_UPDATER_REFERENCE` or `-PchamomiloUpdaterReference` to use another checkout. The updater is rebuilt and its complete embedded contents and checksum are verified before packaging.

```text
gradlew.bat clean build dist --stacktrace
```

On other platforms, use `./gradlew`. If the native UI checks need an external JavaFX runtime, set `WURM_JFX` or `WURM_JAVAFX_JAR` to `jfxrt.jar`. The archive is written to `build/distributions/keybinder-0.11.13.zip`.

The interface images above are previews rendered from Keybinder's production controls. Rendering and input checks run against the pinned client libraries; pointer movement, HUD fade, reconnect and gameplay require verification in Wurm after manual installation.

### Command catalog for other mods

A compatible mod can expose this method on its main class:

```java
public static String[][] getKeybinderCommandCatalog()
```

Each row contains a display label and one complete console command. Malformed rows, duplicate commands and placeholder commands are ignored. The category uses the provider's `updateName` property, with its loader name as fallback.

## Credits and license

Keybinder is derivative work based on [bdew's Custom Actions](https://github.com/bdew-wurm/action). Thanks to **bdew** for the original implementation. [Original releases](https://github.com/bdew-wurm/action/releases) remain available.

Smart Improve is a clean-room reimplementation inspired by **Munsta0** (Improved Improve), **inniria** (i2improve) and **Snidor** (continued i2improve).

Licensed under **GNU LGPL 3.0 or later**. See [lgpl-3.0.txt](lgpl-3.0.txt) and the accompanying [GPL license](LICENSE). Bundled Alegreya Sans fonts retain their OFL notice in the JAR.

**Chamomilo**
