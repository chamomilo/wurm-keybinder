# Shared Chamomilo updater protocol 1

From Keybinder 0.10.0 / Waypointer 1.4.0, the window appears at every launch and
contains current, absent and failed-check rows too. Its catalogue is refreshed
from the public `chamomilo-mods.properties` file in `chamomilo/wurm-keybinder`.
Maintain this file when adding a public mod; clients need no new binary release.
An unauthenticated public-repository listing filters out private or foreign
repositories. A validated catalogue is cached in
`~/.chamomilo/mod-catalog.properties`; the last verified copy, then the initial
initial published mods, provides offline fallback.

Mod discovery reads properties and checks implementation JAR presence regardless
of enabled/load state. Runtime versions win over disk metadata; disk versions
come from the properties, versioned JAR filename or manifest. Unknown versions
are labelled explicitly. UPDATE is offered only for a newer installed version;
INSTALL is offered for an absent mod with a stable release. Buttons open a ZIP
or release page for manual installation. No files are deployed by this updater.

The public Host interface, window constructor and original ModUpdate getters are
retained. Older copies remain capable of coordinating their old notifications;
the first loaded copy determines features. Upgrade Keybinder as well when it is
installed, because its classes normally load first.

Keybinder contains the canonical embedded updater used by Chamomilo Wurm client
mods. This is deliberately not a separate installed mod: every participating JAR
contains the same compatible classes, and Ago's shared mod classloader resolves
them to one process-wide static coordinator.

## Required metadata

Each mod properties file declares:

```properties
sharedClassLoader=true
updateProvider=chamomilo
updateCoordinatorProtocol=1
updateId=unique-lowercase-id
updateName=Display Name
updateRepo=chamomilo/github-repository
updateAsset=archive-{version}.zip
```

The main class implements `ModListener`. During `init`, it calls
`SharedUpdateCoordinator.registerHost(...)`; its `modInitialized` method forwards
the received `ModEntry`; and its first completed HUD initialization calls
`SharedUpdateCoordinator.startOnce()`.

The first host registration wins. Repeated listener callbacks are deduplicated by
`updateId`. The HUD call occurs after mod loading, so the coordinator snapshots a
complete installed-mod catalog, groups it by GitHub repository, checks at most four
repositories concurrently on daemon threads, and reports one immutable result
list to the winning host.

## Release invariants

- Embed `org.chamomilo.wurm.update`, `ChamomiloUpdateWindow`,
  `com.wurmonline.client.resources.ChamomiloResourceUrl`, and the canonical
  `org/chamomilo/wurm/update/update-frame.png` JAR resource in every release.
- Mods without their own HUD bridge call `SharedUpdateHooks.install()` in
  preInit, `SharedUpdateHooks.registerHost(id)` in init, and delegate their
  ModListener callbacks. The shared hooks resolve `ChamomiloUpdateBridge` only
  after HUD init and deliver its window on the HUD thread.
- Keep protocol-1 public interfaces binary compatible across all participating
  JARs. Whichever JAR is first on the shared classpath supplies the runtime copy.
- Use numeric `vX.Y.Z` GitHub release tags and attach the ZIP named by
  `updateAsset`.
- Verify the classes and all metadata during the release build; fail the build if
  either is absent.
- Open a URL only after the player clicks UPDATE or INSTALL.
- Preserve one aggregate window and one check per unique repository per game
  process.
