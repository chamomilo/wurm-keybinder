<p align="center">
  <img src="src/main/resources/keybinder/intro-banner.png" alt="Wurm Keybinder" width="800">
</p>

# Keybinder 0.10.2 for Wurm Unlimited

Keybinder is a Wurm Unlimited client mod for creating, editing and sharing keybinds through a Wurm-styled window. It supports action chains, context-specific targets, Smart Improve and Archaeology Identify.

- [Download Keybinder 0.10.2](https://github.com/chamomilo/wurm-keybinder/releases/tag/v0.10.2)
- [Discuss Keybinder on the SKLOTOPOLIS forum](https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

## New in 0.10.2

Action chains now resolve their available tools and targets before the first action is sent. A step with a missing item, missing target or incompatible target type is reported in Event and skipped without consuming queue capacity. The remaining valid steps retain their original order.

Resolved sources and targets are captured during preflight. Earlier actions cannot redirect later steps by changing the active item or selection. Tool activation is simulated during validation and applied only when execution reaches that step.

Smart Improve and Archaeology Identify keep their prepared batches when fitting actions into the remaining queue space. Bulk transfers check that the captured source item is still visible before sending a request.

For world objects without improvement metadata, Smart Improve still selects the object and sends a quiet Examine. Improve is sent only after the response supplies enough information to validate the required resource. Validation uses data visible to the client; permissions, hidden server conditions and changes after preflight remain under server control.

## Features

- Native HUD keybind manager with create, edit, duplicate, merge, import and export controls.
- Action chains, vanilla commands and Multi-keybinds with selectable variants.
- Hovered, selected, inventory-filter, nearby, tile, area, toolbelt, equipment and current-ride targets.
- Portable tool selectors for the active item, empty hand, toolbelt, equipment, inventory filters and captured items.
- Action queue monitor on either screen edge, with deferred cancellation of queued actions.
- Smart Improve with automatic tool/resource selection and client-side chance estimates.
- Archaeology Identify with automatic brush or stone-chisel selection.
- Mouse-wheel bindings and optional integration with WU-third_person_view.
- One-shot shadow recording of actions you perform yourself.
- Import of predecessor bindings into native Keybinder records.
- Per-character enabled state, Event logging and English, Brazilian Portuguese and German localization.

Keybinder sends ordinary Wurm actions. It is a keybind manager, not an unattended automation system.

## Installation and upgrade

Keybinder requires Wurm Unlimited and Ago's Client Mod Launcher.

1. Download `keybinder-0.10.2.zip` from the release page.
2. Extract the ZIP into your Wurm Unlimited client directory. It contains `mods/keybinder.properties` and `mods/keybinder/`.
3. When upgrading, overwrite the existing Keybinder distribution files. Managed keybinds are stored separately and are not included in the ZIP.
4. Start the client and enable **Keybinder** in **HUD Settings** if its window is hidden.

Keybinder replaces Custom Actions, Improved Improve and i2improve. Import predecessor bindings through Keybinder's review workflow, then disable the predecessor mods for normal use.

## Getting started

Open the **Keybinds** tab, create a record and choose its key or mouse-wheel chord. Add actions, select their tools and targets, then save. You can enter numeric action IDs or search the current client catalog.

For inventory filters and nearby-type targets, choose a stable object type rather than a temporary runtime ID. Use the toolbelt and equipment selectors to capture portable slot references. Shadow recording observes your ordinary actions and asks you to resolve targets it cannot identify reliably.

At execution, unavailable steps are explained in Event. Queue-consuming actions are checked against the remaining capacity; local tool activation costs no queue slots. Nearby and multi-target actions are resolved at runtime.

Use **Print all keybinds to Event** or `keybinder_list` to inspect managed bindings. Import and restore operations respect bind ownership and report conflicts instead of silently replacing another binding.

## Shared Chamomilo updates

Keybinder includes the shared Chamomilo Mods Registry. Participating mods use one update window and one coordinator. The window opens after HUD startup and is available from the native **Mod updates** menu. Its startup preference is saved between launches.

The registry shows installed, disabled and available mods from [chamomilo-mods.properties](chamomilo-mods.properties). **UPDATE**, **INSTALL** and **DOWNLOAD** open the relevant GitHub release page after an explicit click. Installation of ZIP updates is manual.

## Reporting issues

Please include the relevant Event messages, Keybinder version and a description of the affected chain when reporting a problem in the forum. Enable **Debug logging** when additional diagnostics are needed.

## Building from source

Use JDK 8 and the included Gradle wrapper. Supply the pinned client libraries in the local, ignored `libs/` directory:

- `client-patched.jar`
- `common.jar`
- `javassist.jar`
- `modlauncher.jar`

The updater UI check also needs the client's JavaFX runtime. Set `WURM_JAVAFX_JAR` to the existing `jfxrt.jar`, or pass `-PwurmJavaFxJar=/path/to/jfxrt.jar`.

```text
./gradlew clean build dist
```

On Windows, use `gradlew.bat`. The installable archive is written to `build/distributions/keybinder-0.10.2.zip`. The build checks tests, version consistency, namespaces and shared updater packaging. It does not install the mod into a game directory.

Version 0.10.2 passed 541 automated tests and the updater layout/input checks. Manual in-game verification of this release is still required.

## Credits and license

Keybinder is derivative work based on [bdew's Custom Actions](https://github.com/bdew-wurm/action). Thanks to bdew for the original implementation. [Original Custom Actions releases](https://github.com/bdew-wurm/action/releases) remain available.

Smart Improve is a clean-room reimplementation inspired by **Munsta0** (Improved Improve), **inniria** (i2improve) and **Snidor** (continued i2improve). No source code from those Improved Improve mods is bundled.

Keybinder is licensed under **GNU LGPL 3.0 or later**. See [LICENSE](LICENSE) and [lgpl-3.0.txt](lgpl-3.0.txt).

**Chamomilo**
