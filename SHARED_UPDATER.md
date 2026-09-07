# Shared Chamomilo updater protocol 1

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

- Embed `org.chamomilo.wurm.update` and `ChamomiloUpdateWindow` in every release.
- Keep protocol-1 public interfaces binary compatible across all participating
  JARs. Whichever JAR is first on the shared classpath supplies the runtime copy.
- Use numeric `vX.Y.Z` GitHub release tags and attach the ZIP named by
  `updateAsset`.
- Verify the classes and all metadata during the release build; fail the build if
  either is absent.
- Open a URL only after the player clicks its Download button.
- Preserve one aggregate window and one check per unique repository per game
  process.
