Keybinder 0.11.6 for Wurm Unlimited

Keybinder is a Wurm Unlimited client mod for creating, editing and sharing keybinds through Chamomilo UI. Build action chains, choose portable tools and targets, and manage commands from one HUD window.

Download the latest release (https://github.com/chamomilo/wurm-keybinder/releases/latest) | Discuss Keybinder (https://sklotopolis.freeforums.net/thread/8369/new-2026-mod-wurm-keybinder)

What's new

- Updated the shared Chamomilo Updater to 1.2.7 and Chamomilo UI to 0.4.10. The UI SDK is embedded once through the complete updater module.
- English, Brazilian Portuguese, German and Russian interfaces. Select the shared language in the updater; existing Keybinder windows and drafts refresh without restarting the client.
- Quick menus execute a selected variant immediately. Multi bindings remember a variant for later short key presses. Older HUD-mode records and exports remain compatible with Quick.
- The default Value Pack contains eleven example bindings, including a Q Quick menu, E Multi binding, camera controls and an F sprout chain. New definitions arrive disabled for review; existing choices are retained.
- The installation ZIP contains README.txt and license files. This README's banner stays separately in images/keybinder-banner.png in the source repository and is excluded from the ZIP and JAR.

Features

- Create, edit, duplicate, merge, import and export managed keybinds.
- Structured action chains, vanilla commands and raw vanilla commands.
- Quick menus and Multi bindings with separate action sequences per variant.
- Hovered, selected, inventory-filter, nearby, tile, area, toolbelt, equipment and current-ride targets.
- Portable tool selectors for the active item, empty hand, toolbelt, equipment, inventory filters and captured items.
- Smart Improve with automatic tool/resource selection and client-side chance estimates.
- Archaeology Identify with automatic brush or stone-chisel selection.
- Mouse-wheel bindings and command catalogs supplied by compatible client mods.
- Passive shadow recording of actions you perform yourself.
- Action queue monitor, queue-capacity checks, per-character enabled state and Event logging.
- Review-based import of predecessor and vanilla bindings, with backups, ownership checks and restoration.

Installation and upgrade

Requires Wurm Unlimited and Ago's Client Mod Launcher.

1. Download keybinder-0.11.6.zip from the release page.
2. Close the client and extract the ZIP into your Wurm Unlimited client directory. It contains mods/keybinder.properties and mods/keybinder/.
3. When upgrading, overwrite the existing Keybinder distribution files. Managed keybind data is stored separately and is not included in the ZIP.
4. Start the client and enable Keybinder in HUD Settings if its window is hidden.

Keybinder replaces Custom Actions, Improved Improve and i2improve. Import their bindings through Keybinder's review workflow, then disable the predecessor mods for normal use.

Getting started

Open the keybind list and choose Add new keybind. Name the binding, capture its key or mouse-wheel chord, add actions, select tools and targets, then save. Search the current client action catalog or enter a numeric action ID. Use Read instruction for the detailed in-game guide and About information.

For inventory filters and nearby-type targets, choose a stable object type. Toolbelt and equipment capture store portable slot references. Shadow recording observes ordinary actions and asks you to resolve targets it cannot identify reliably. Captured personal items in imported examples need to be replaced with your own selections.

Unavailable steps are explained in Event. Keybinder resolves sources and targets before dispatch and checks queue-consuming actions against available capacity. An over-limit chain does not partially execute; local tool activation costs no queue slots. Nearby and other multi-target actions are resolved at runtime.

Use Print all keybinds to Event or keybinder_list to inspect managed bindings. Import and restore operations check the live binding before changing it and report ownership conflicts.

Quick and Multi

Quick: every key press opens the menu. Clicking a variant immediately executes that variant's sequence. Closing the menu executes nothing; the next press opens it again. For example, Q can offer Inventory, Backpack, Repair, Drink water and Pray.

Multi: with at least two variants and Quick unchecked, hold the key for 0.2 seconds to open the selector. Choosing a variant remembers it without executing it. Later short taps run that variant until you hold the key and choose another. Closing the selector preserves the previous choice. With one variant and Quick unchecked, the binding runs directly.

Each variant keeps its own actions, tools, targets and queue cost. Menu selection adds no queue cost.

Shared Chamomilo updates

Keybinder embeds the complete shared Chamomilo updater and UI SDK. Participating mods use one updater window, available from the native Mod updates menu. Its startup visibility and shared language preference persist between launches.

The registry lists installed, disabled and available mods using chamomilo-mods.properties (chamomilo-mods.properties). Update, Install and Download open the relevant GitHub release page after an explicit click. ZIP installation is manual.

Command catalog integration

A compatible client mod can expose this public static method on its main class:

public static String[][] getKeybinderCommandCatalog()

Each row contains a display label and one complete console command. Keybinder ignores malformed rows, duplicate commands and commands containing placeholder brackets. The category name comes from the provider's updateName property, with its mod-loader name as fallback. No Keybinder compile-time dependency is required.

Building from source

Use JDK 8 and the included Gradle wrapper. Supply the pinned client libraries in the local, ignored libs/ directory: client-patched.jar, common.jar, javassist.jar and modlauncher.jar.

The build also requires the canonical updater project at C:/projects/updater, or the path supplied through CHAMOMILO_UPDATER_REFERENCE or -PchamomiloUpdaterReference. It rebuilds that reference and verifies its complete embedded module, including the pinned Chamomilo UI version and checksum. The SDK source is maintained separately at C:/projects/interface items.

If the native UI probes require an external JavaFX runtime, set WURM_JFX or WURM_JAVAFX_JAR to the existing jfxrt.jar.

./gradlew clean build dist

On Windows, use gradlew.bat. The installable archive is build/distributions/keybinder-0.11.6.zip. Checks cover automated tests, production UI probes, versions, namespaces, exact updater embedding and archive contents. Keybinder previews are written to build/chamomilo-preview.

The ZIP includes the mod descriptor, JAR, runtime HUD artwork, README.txt and license files. It contains no Markdown files or README banner. The build does not install files into a game directory. Live-game checks of pointer movement, hover, resizing, HUD fade, capture and reconnect remain manual.

Reporting issues

Include the Keybinder version, relevant Event messages and a description of the affected binding when reporting a problem on the forum. Enable Debug logging when additional diagnostics are needed.

Credits and license

Keybinder is derivative work based on bdew's Custom Actions (https://github.com/bdew-wurm/action). Thanks to bdew for the original implementation. Original Custom Actions releases (https://github.com/bdew-wurm/action/releases) remain available.

Smart Improve is a clean-room reimplementation inspired by Munsta0 (Improved Improve), inniria (i2improve) and Snidor (continued i2improve). No source code from those Improved Improve mods is bundled.

Keybinder is licensed under GNU LGPL 3.0 or later. See LICENSE (LICENSE) and lgpl-3.0.txt (lgpl-3.0.txt). Bundled Alegreya Sans fonts retain their OFL license in the JAR.

Chamomilo
