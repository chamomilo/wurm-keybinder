package org.keybinder.wurm;

import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.KeybinderActionQueueMonitor;
import com.wurmonline.client.renderer.gui.KeybinderCaptureWindow;
import com.wurmonline.client.renderer.gui.KeybinderConflictWindow;
import com.wurmonline.client.renderer.gui.KeybinderEditorWindow;
import com.wurmonline.client.renderer.gui.KeybinderLegacyWindow;
import com.wurmonline.client.renderer.gui.KeybinderImportWindow;
import com.wurmonline.client.renderer.gui.KeybinderMergeWindow;
import com.wurmonline.client.renderer.gui.KeybinderMultiSelectorWindow;
import com.wurmonline.client.renderer.gui.KeybinderSelectionWindow;
import com.wurmonline.client.renderer.gui.KeybinderSelectionBridge;
import com.wurmonline.client.renderer.gui.KeybinderInventorySelectionBridge;
import com.wurmonline.client.renderer.gui.KeybinderTileWindow;
import com.wurmonline.client.renderer.gui.KeybinderWindow;
import com.wurmonline.client.renderer.gui.KeybinderTagWindow;
import com.wurmonline.client.renderer.gui.SelectBar;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.shared.constants.PlayerAction;
import com.wurmonline.mesh.Tiles;
import org.keybinder.wurm.bind.VanillaBindService;
import org.keybinder.wurm.bind.BindSnapshot;
import org.keybinder.wurm.bind.AccountActivationGate;
import org.keybinder.wurm.bind.VanillaImportCandidate;
import org.keybinder.wurm.bind.VanillaImportReviewService;
import org.keybinder.wurm.command.ActionExecutor;
import org.keybinder.wurm.command.KeybindExecutionService;
import org.keybinder.wurm.command.KeybinderCommandRouter;
import org.keybinder.wurm.command.PushSelectionRetention;
import org.keybinder.wurm.command.WorldImproveTracker;
import org.keybinder.wurm.command.BulkTransferCoordinator;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.integration.CreationSkillRegistry;
import org.keybinder.wurm.integration.DeferredUiQueue;
import org.keybinder.wurm.integration.EmbarkHeadingController;
import org.keybinder.wurm.integration.ExecutionHoverOverride;
import org.keybinder.wurm.integration.FailOpenHookInstaller;
import org.keybinder.wurm.integration.KeybinderClientHooks;
import org.keybinder.wurm.integration.ManagedInputCoordinator;
import org.keybinder.wurm.integration.HudIntegration;
import org.keybinder.wurm.integration.CurrentServerTracker;
import org.keybinder.wurm.integration.HudSessionController;
import org.keybinder.wurm.integration.HudSessionDisposer;
import org.keybinder.wurm.integration.ServerNameResolver;
import org.keybinder.wurm.integration.SmartImproveOriginGuard;
import org.keybinder.wurm.integration.WorldImproveEventScope;
import org.keybinder.wurm.integration.TransferFileChooser;
import org.keybinder.wurm.integration.BulkStorageSourceResolver;
import org.keybinder.wurm.integration.BulkInventoryDestinationPolicy;
import org.keybinder.wurm.i18n.Language;
import org.keybinder.wurm.i18n.LanguageChangePolicy;
import org.keybinder.wurm.i18n.LocalizationSettings;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.i18n.ExistingWindowRelocalizer;
import org.keybinder.wurm.migration.CustomActionsMigrationService;
import org.keybinder.wurm.migration.ImprovedImproveMigrationService;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindConflict;
import org.keybinder.wurm.model.ConflictResolution;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.InventoryReference;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.queue.ActionQueueEntry;
import org.keybinder.wurm.queue.ActionQueueOccupancyTracker;
import org.keybinder.wurm.queue.QueueCost;
import org.keybinder.wurm.queue.QueueLimitService;
import org.keybinder.wurm.recording.ActionCapture;
import org.keybinder.wurm.recording.SelectionController;
import org.keybinder.wurm.recording.TargetClickGesture;
import org.keybinder.wurm.storage.KeybindStore;
import org.keybinder.wurm.storage.AccountKeybindStateStore;
import org.keybinder.wurm.storage.ModPropertiesStore;
import org.keybinder.wurm.resource.PackagedResourceLoader;
import org.keybinder.wurm.transfer.KeybindTransferStore;
import org.keybinder.wurm.transfer.TransferImportResult;
import org.keybinder.wurm.transfer.ValuePackProvider;
import org.keybinder.wurm.transfer.KeybindTransferWorkflow;
import org.keybinder.wurm.ui.KeybinderUiController;
import org.keybinder.wurm.ui.KeybindEditorController;
import org.keybinder.wurm.ui.EditorWorkflow;
import org.keybinder.wurm.ui.QueueMonitorSide;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import java.awt.Desktop;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KeybinderMod implements WurmClientMod, Initable, PreInitable, Configurable,
        KeybinderUiController, KeybindEditorController {
    public static final String VERSION = "0.7.6";
    public static final String IMPROVE_PROJECT = "https://github.com/Snidor/i2improve";
    public static final String INNIRIA_IMPROVE_PROJECT = "https://github.com/inniria/i2improve";
    public static final String MUNSTA_IMPROVE_PROJECT =
            "https://github.com/munsta0/WUClientImprovedImprove";
    public static final String ORIGINAL_PROJECT = "https://github.com/bdew-wurm/action";
    private static final Logger LOGGER = Logger.getLogger("Chamomilo.Keybinder");
    /*
     * Do not instantiate integrations that reference renderer cell classes while
     * the mod entry class is being loaded. Other client mods still need to patch
     * those classes during preInit; resolving them here freezes Javassist CtClass.
     */
    private static ClientAccess ACCESS;
    private static final ActionCapture ACTION_CAPTURE = new ActionCapture();
    private static final EventLogger EVENTS = new EventLogger(LOGGER);
    private static final SelectionController SELECTION = new SelectionController(EVENTS);
    private static final PushSelectionRetention PUSH_SELECTION =
            new PushSelectionRetention();
    private static final WorldImproveTracker WORLD_IMPROVE =
            new WorldImproveTracker();
    private static final BulkTransferCoordinator BULK_TRANSFERS =
            new BulkTransferCoordinator(EVENTS);
    private static final CustomActionsImporter CUSTOM_ACTIONS_IMPORTER = new CustomActionsImporter();
    private static final QueueLimitService LIMITS = new QueueLimitService();
    private static final ActionQueueCostCalculator COSTS = new ActionQueueCostCalculator();
    private static final ActionQueueOccupancyTracker ACTION_QUEUE =
            new ActionQueueOccupancyTracker();
    private static final VanillaBindService BINDS = new VanillaBindService();
    private static final CustomActionsMigrationService LEGACY_ACTION =
            new CustomActionsMigrationService();
    private static final ImprovedImproveMigrationService LEGACY_IMPROVE =
            new ImprovedImproveMigrationService();
    private static final ServerNameResolver SERVER_NAMES = new ServerNameResolver();
    private static final CurrentServerTracker CURRENT_SERVER = new CurrentServerTracker();
    private static EmbarkHeadingController embarkHeading;
    private static final ModPropertiesStore MOD_PROPERTIES = new ModPropertiesStore();
    private static ActionExecutor EXECUTOR;
    private static KeybindExecutionService KEYBIND_EXECUTOR;
    private static final DeferredUiQueue UI_AFTER_TICK = new DeferredUiQueue(
            64, failure -> EVENTS.error(Messages.text("error.deferred_hud"), failure));
    private static final FailOpenHookInstaller HOOKS = new FailOpenHookInstaller(LOGGER);
    private static final Map<Short, String> ACTION_NAMES = new ConcurrentHashMap<>();
    private static final Map<Short, String> ACTION_PATHS = new ConcurrentHashMap<>();
    private static final Map<Object, String> POPUP_PATHS =
            Collections.synchronizedMap(new java.util.WeakHashMap<Object, String>());

    private static volatile HeadsUpDisplay hud;
    private static final HudSessionController<HeadsUpDisplay> HUD_SESSIONS =
            new HudSessionController<HeadsUpDisplay>();
    private static final AccountActivationGate ACCOUNT_ACTIVATION =
            new AccountActivationGate();
    private static volatile KeybinderWindow window;
    private static volatile KeybinderTagWindow tagWindow;
    private static volatile KeybinderActionQueueMonitor queueMonitor;
    private static volatile KeybinderEditorWindow editorWindow;
    private static volatile KeybinderCaptureWindow captureWindow;
    private static volatile KeybinderConflictWindow conflictWindow;
    private static volatile KeybinderTileWindow tileWindow;
    private static volatile KeybinderSelectionWindow selectionWindow;
    private static volatile KeybinderLegacyWindow legacyWindow;
    private static volatile KeybinderImportWindow importWindow;
    private static volatile KeybinderMultiSelectorWindow multiSelectorWindow;
    private static volatile ExecutionHoverOverride.Snapshot multiSelectorHoverSnapshot;
    private static volatile KeybinderMergeWindow mergeWindow;
    private static final KeybindTransferStore TRANSFER = new KeybindTransferStore();
    private static final ValuePackProvider VALUE_PACK = new ValuePackProvider();
    private static final VanillaImportReviewService IMPORT_REVIEW =
            new VanillaImportReviewService();
    private static final TransferFileChooser TRANSFER_CHOOSER = new TransferFileChooser(
            Paths.get("mods", "keybinder", "transfer"));
    private static volatile boolean toolbeltOpenedForSelection;
    private static volatile boolean equipmentOpenedForSelection;
    private static volatile boolean inventoryOpenedForSelection;
    private static final TargetClickGesture TARGET_CLICK = new TargetClickGesture();
    private static final BulkStorageSourceResolver BULK_SOURCES =
            new BulkStorageSourceResolver();
    private static volatile long lastSharedSyncPoll;
    private static volatile long lastCreationCatalogRequest;
    private static volatile int creationCatalogRequestAttempts;
    private static volatile String selectedFullServerName = "";
    private static KeybindRegistry registry;
    private static EditorWorkflow editorWorkflow;
    private static KeybindTransferWorkflow transferWorkflow;
    private static final KeybinderCommandRouter COMMANDS =
            new KeybinderCommandRouter(EVENTS);
    private static final ManagedInputCoordinator INPUT =
            new ManagedInputCoordinator(new ManagedInputCoordinator.Environment() {
                @Override public HeadsUpDisplay hud() { return hud; }
                @Override public KeybindRecord findEnabledByChord(String chord) {
                    return registry == null ? null : registry.findEnabledByChord(chord);
                }
                @Override public KeybindRecord findById(String id) {
                    return registry == null ? null : registry.find(id);
                }
                @Override public void execute(KeybindRecord record, HeadsUpDisplay currentHud)
                        throws Exception {
                    executeManaged(record, currentHud);
                }
                @Override public void openSelector(KeybindRecord record,
                                                   boolean hudSelection, int triggerKey) {
                    openMultiSelector(record, hudSelection, triggerKey);
                }
                @Override public void closeSelector() { closeMultiSelector(); }
                @Override public void warning(String message, Throwable failure) {
                    LOGGER.log(Level.WARNING, message, failure);
                }
                @Override public void fine(String message, Throwable failure) {
                    LOGGER.log(Level.FINE, message, failure);
                }
                @Override public void reportWheelFailure(String failure, Throwable cause) {
                    EVENTS.error(Messages.text("error.mouse_wheel")
                            + " (" + failure + ")", cause);
                }
                @Override public long nanoTime() { return System.nanoTime(); }
                @Override public long currentTimeMillis() {
                    return System.currentTimeMillis();
                }
            });
    private static final KeybinderCommandRouter.Context COMMAND_CONTEXT =
            new KeybinderCommandRouter.Context() {
                @Override public void ensureReady() { ensureRegistry(); }
                @Override public KeybindRecord find(String id) { return registry.find(id); }
                @Override public void execute(KeybindRecord record) throws Exception {
                    executeManaged(record);
                }
                @Override public String selectedTarget() {
                    return SELECTION.getSelectedTarget();
                }
                @Override public void add(KeybindRecord record) throws Exception {
                    registry.add(record, ACCESS.console(hud), LIMITS.readLimit(hud));
                }
                @Override public int importAllReviewed() throws Exception {
                    return registry.importAllReviewed(
                            ACCESS.console(hud), LIMITS.readLimit(hud));
                }
                @Override public boolean delete(String id) throws Exception {
                    return registry.delete(id, ACCESS.console(hud));
                }
                @Override public void restoreOriginalBindings() {
                    if (INSTANCE == null)
                        throw new IllegalStateException(Messages.text("error.registry_not_ready"));
                    INSTANCE.restoreOriginalBindings();
                }
                @Override public void refreshWindow() {
                    if (window != null) window.refresh();
                }
            };
    private Properties properties = new Properties();
    private volatile boolean skipIntro;
    private volatile boolean centerViewAfterEmbark = true;
    private volatile String language = Language.ENGLISH.getCode();
    private volatile QueueMonitorSide queueMonitorSide = QueueMonitorSide.RIGHT;
    private volatile String pendingLanguage;
    private volatile boolean vanillaImportPromptDismissed;
    private volatile boolean importPromptOfferedThisSession;

    @Override
    public String getVersion() { return VERSION; }

    @Override
    public void configure(Properties properties) {
        this.properties = properties == null ? new Properties() : properties;
        skipIntro = Boolean.parseBoolean(this.properties.getProperty("skipIntroPage", "false"));
        centerViewAfterEmbark = Boolean.parseBoolean(
                this.properties.getProperty("centerViewAfterEmbark", "true"));
        language = LocalizationSettings.load(this.properties);
        queueMonitorSide = QueueMonitorSide.fromSetting(
                this.properties.getProperty("queueMonitorSide"));
        vanillaImportPromptDismissed = Boolean.parseBoolean(
                this.properties.getProperty("vanillaImportPromptDismissed", "false"));
        Messages.select(language);
    }

    @Override
    public void preInit() {
        new KeybinderClientHooks(HOOKS, LOGGER).install();
    }

    @Override
    public void init() {
        try {
            String path = properties.getProperty("dataFile", "mods/keybinder/keybinds.properties");
            java.nio.file.Path dataPath = Paths.get(path);
            java.nio.file.Path accountStatePath = dataPath.resolveSibling(
                    dataPath.getFileName().toString() + ".accounts");
            registry = new KeybindRegistry(new KeybindStore(dataPath),
                    new AccountKeybindStateStore(accountStatePath), BINDS,
                    CUSTOM_ACTIONS_IMPORTER, COSTS, EVENTS);
            registry.load();
            editorWorkflow = new EditorWorkflow(registry, EVENTS,
                    new EditorWorkflow.Environment() {
                        @Override public WurmConsole console()
                                throws ReflectiveOperationException {
                            return ACCESS.console(hud);
                        }
                        @Override public int queueLimit() { return LIMITS.readLimit(hud); }
                        @Override public void showConflict(final KeybindConflict conflict) {
                            deferUi(new Runnable() {
                                @Override public void run() {
                                    hideSafely(conflictWindow);
                                    conflictWindow = new KeybinderConflictWindow(INSTANCE, conflict);
                                    try { new HudIntegration(ACCESS).add(hud, conflictWindow); }
                                    catch (ReflectiveOperationException failure) {
                                        conflictWindow = null;
                                        if (editorWorkflow != null) editorWorkflow.clear();
                                        EVENTS.error(Messages.text("error.conflict_window"), failure);
                                    }
                                }
                            });
                        }
                        @Override public void closeConflict() {
                            final KeybinderConflictWindow dialog = conflictWindow;
                            conflictWindow = null;
                            deferUi(new Runnable() {
                                @Override public void run() { hideSafely(dialog); }
                            });
                        }
                        @Override public void closeEditor() { INSTANCE.closeEditor(); }
                        @Override public void refreshList() {
                            if (window != null) window.refresh();
                        }
                        @Override public void enabledStateChanged(String id, boolean enabled) {
                            if (!enabled && ((multiSelectorWindow != null
                                    && multiSelectorWindow.selectsRecord(id))
                                    || INPUT.isHoldingRecord(id))) closeMultiSelector();
                        }
                        @Override public void extractedFrom(KeybindRecord parent) {
                            if (parent != null && !parent.isMultiPurpose()
                                    && ((multiSelectorWindow != null
                                    && multiSelectorWindow.selectsRecord(parent.getId()))
                                    || INPUT.isHoldingRecord(parent.getId())))
                                closeMultiSelector();
                        }
                    });
            transferWorkflow = new KeybindTransferWorkflow(registry, TRANSFER,
                    TRANSFER_CHOOSER, EVENTS, new KeybindTransferWorkflow.Environment() {
                @Override public void defer(Runnable task) { deferUi(task); }
                @Override public void refreshCreationContext() {
                    KeybinderMod.refreshCreationContext();
                }
                @Override public String currentUser() { return registry.getCurrentUser(); }
                @Override public String currentServer() { return registry.getCurrentServer(); }
                @Override public void refreshWindow() {
                    if (window != null) window.refresh();
                }
            }, VERSION);
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize Keybinder", e);
        }
    }

    public static void handleMouseWheel(final int x, final int y, final int delta) {
        INPUT.handleMouseWheel(x, y, delta);
    }

    public static void alignViewAfterEmbark(PlayerObj player, float vehicleRotation) {
        EmbarkHeadingController controller = embarkHeading;
        if (controller != null) controller.align(player, vehicleRotation);
    }

    public static void onHudTick(HeadsUpDisplay currentHud) {
        applyAccountBindingsIfReady(currentHud);
        ensureCreationSkillCatalog(currentHud);
        if (KEYBIND_EXECUTOR != null) KEYBIND_EXECUTOR.tick();
        drainUiQueue();
    }

    public static void deferUi(Runnable operation) {
        UI_AFTER_TICK.defer(operation);
    }

    private static void drainUiQueue() {
        UI_AFTER_TICK.drain();
        BULK_TRANSFERS.tick();
        pollSharedDefinitions();
        pollLongPress();
    }

    public static boolean handleKeyToggle(WurmConsole console, int key, boolean pressed) {
        return INPUT.handleKeyToggle(console, key, pressed);
    }

    private static void pollLongPress() {
        INPUT.pollLongPress();
    }

    private static void openMultiSelector(KeybindRecord record, boolean hudSelection,
                                          int triggerKey) {
        final long sessionToken = INPUT.openSelectorSession(triggerKey);
        final int originalMouseX = hud == null ? 0
                : hud.getWorld().getClient().getXMouse();
        final int originalMouseY = hud == null ? 0
                : hud.getWorld().getClient().getYMouse();
        final PickableUnit originalHovered = hud == null ? null
                : hud.getWorld().getCurrentHoveredObject();
        boolean worldPoint = hud != null
                && hud.getComponentAt(originalMouseX, originalMouseY) == null;
        multiSelectorHoverSnapshot = hudSelection && worldPoint && originalHovered != null
                ? new ExecutionHoverOverride.Snapshot(originalHovered)
                : null;
        EVENTS.diagnostic("multi-selector opening: recordId=" + record.getId()
                + ", hudSelection=" + hudSelection + ", originalMouseX="
                + originalMouseX + ", originalMouseY=" + originalMouseY
                + ", originalWorldHoveredId="
                + (originalHovered == null ? 0L : originalHovered.getId())
                + ", worldPoint=" + worldPoint
                + ", overrideCaptured=" + (multiSelectorHoverSnapshot != null));
        deferUi(() -> {
            if (!INPUT.isCurrentSelectorSession(sessionToken)) return;
            try {
                hideSafely(multiSelectorWindow);
                multiSelectorWindow = new KeybinderMultiSelectorWindow(
                        record, hudSelection, originalMouseX, originalMouseY);
                new HudIntegration(ACCESS).add(hud, multiSelectorWindow);
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.multi_selector"), e);
                closeMultiSelector();
            }
        });
    }

    public static void observeKeyPressed(int key) {
        INPUT.observeKeyPressed(key);
    }

    public static void observeKeyReleased(int key) {
        INPUT.observeKeyReleased(key);
    }

    private static void executeManaged(KeybindRecord record) throws ReflectiveOperationException {
        executeManaged(record, hud);
    }

    private static void executeManaged(KeybindRecord record, HeadsUpDisplay currentHud)
            throws ReflectiveOperationException {
        executeManaged(record, currentHud, null);
    }

    private static void executeManaged(KeybindRecord record, HeadsUpDisplay currentHud,
                                       ExecutionHoverOverride.Snapshot hoverSnapshot)
            throws ReflectiveOperationException {
        String account = currentPlayerName(currentHud);
        if (!ACCOUNT_ACTIVATION.isApplied(account)) {
            LOGGER.fine("Ignored managed keybind " + record.getId()
                    + " while the player activation profile is not ready");
            return;
        }
        final int queueLimit = LIMITS.readLimit(currentHud);
        KEYBIND_EXECUTOR.execute(record, currentHud, queueLimit,
                () -> ACTION_QUEUE.occupied(hudShowsAction(currentHud)),
                hoverSnapshot);
        EVENTS.execution(Messages.text("event.executed", record.getDisplayName(),
                org.keybinder.wurm.catalog.InputKeyCatalog.displayChord(record.getKey())));
    }

    private static boolean hudShowsAction(HeadsUpDisplay currentHud) {
        if (currentHud == null) return false;
        String action = currentHud.getActionString();
        return action != null && !action.trim().isEmpty();
    }

    public static void chooseMultiVariant(String recordId, String variantId,
                                          boolean hudSelection) {
        KeybindRecord selected;
        try {
            if (hudSelection) {
                KeybindRecord stored = registry.find(recordId);
                if (stored == null || !stored.isEnabled())
                    throw new IllegalStateException(Messages.text("error.selected_unavailable"));
                selected = stored.executionViewForVariant(variantId);
            } else {
                KeybindRecord before = registry.find(recordId);
                String oldName = before == null ? "" : before.getDisplayName();
                if (registry.selectVariant(recordId, variantId)) {
                    selected = registry.find(recordId);
                    if (selected == null || !selected.isEnabled())
                        throw new IllegalStateException(Messages.text("error.selected_unavailable"));
                    EVENTS.info(Messages.text("event.active_action_changed",
                            selected.getName(), oldName, selected.getDisplayName()));
                    if (window != null) window.refresh();
                } else selected = registry.find(recordId);
                if (selected == null || !selected.isEnabled())
                    throw new IllegalStateException(Messages.text("error.selected_unavailable"));
            }
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.change_active_action"), e);
            closeMultiSelector();
            return;
        }
        if (!hudSelection) {
            closeMultiSelector();
            return;
        }
        final KeybindRecord executionRecord = selected;
        final ExecutionHoverOverride.Snapshot hoverSnapshot = multiSelectorHoverSnapshot;
        multiSelectorHoverSnapshot = null;
        final KeybinderMultiSelectorWindow selector = multiSelectorWindow;
        multiSelectorWindow = null;
        INPUT.clear();
        hideSafely(selector);
        try {
            if (selector != null) selector.restoreOriginalPointer();
            EVENTS.diagnostic("multi-selector execution hover restored: recordId="
                    + recordId + ", mouseX="
                    + (selector == null ? 0 : selector.getOriginalMouseX())
                    + ", mouseY="
                    + (selector == null ? 0 : selector.getOriginalMouseY()));
        } catch (RuntimeException failure) {
            debugMultiPointerWarp(failure);
        }
        // Execute after the next native HUD tick so Wurm has refreshed its
        // GUI/world hover state at the restored pointer coordinates.
        deferUi(() -> {
            try {
                executeManaged(executionRecord, hud, hoverSnapshot);
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.execute_selected_action"), e);
            }
            if (INSTANCE.pendingLanguage != null)
                INSTANCE.applyLanguage(INSTANCE.pendingLanguage);
        });
    }

    public static void closeMultiSelector() {
        INPUT.closeSelectorSession();
        INPUT.clear();
        multiSelectorHoverSnapshot = null;
        KeybinderMultiSelectorWindow selector = multiSelectorWindow;
        multiSelectorWindow = null;
        deferUi(() -> {
            hideSafely(selector);
            if (INSTANCE.pendingLanguage != null)
                INSTANCE.applyLanguage(INSTANCE.pendingLanguage);
        });
    }

    public static void debugMultiPointerWarp(Throwable failure) {
        LOGGER.log(Level.FINE, "Multi selector pointer warp failed open", failure);
    }

    private static void pollSharedDefinitions() {
        long now = System.currentTimeMillis();
        if (now - lastSharedSyncPoll < 1000L || registry == null || hud == null) return;
        lastSharedSyncPoll = now;
        try {
            if (registry.syncExternal(ACCESS.console(hud))) {
                if (window != null) window.refresh();
                if (multiSelectorWindow != null) {
                    boolean valid = false;
                    for (KeybindRecord record : registry.snapshot())
                        if (record.isEnabled() && multiSelectorWindow.selectsRecord(record.getId())) {
                            valid = true;
                            break;
                        }
                    if (!valid) closeMultiSelector();
                }
                if (editorWindow != null) {
                    String edited = editorWindow.getEditedRecordId();
                    if (registry.find(edited) == null) INSTANCE.closeEditor();
                    else INSTANCE.editKeybind(edited);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to poll shared keybind definitions", e);
        }
    }

    public static void rememberSelectedServer(Object tableView) {
        try {
            Object selection = tableView.getClass().getMethod("getSelectionModel").invoke(tableView);
            Object selected = selection.getClass().getMethod("getSelectedItem").invoke(selection);
            if (selected == null) return;
            Object name = selected.getClass().getMethod("getServerName").invoke(selected);
            if (name != null && !name.toString().trim().isEmpty()) {
                String selectedName = name.toString().trim();
                selectedFullServerName = selectedName;
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.FINE, "Unable to remember selected Steam server", e);
        }
    }

    public static void observeWorldImproveAction(long[] targets, PlayerAction action) {
        try {
            if (action == null || targets == null || targets.length != 1) return;
            short actionId = action.getId();
            if (actionId == PlayerAction.REPAIR.getId()
                    || actionId == PlayerAction.IMPROVE.getId()) {
                WORLD_IMPROVE.invalidate(targets[0]);
                return;
            }
            // Wurm's context-menu Examine sends EXAMINE, while an ordinary
            // double click goes through sendDefaultAction and sends
            // DEFAULT_ACTION. The tracker confirms either candidate only when
            // the server returns an item description containing Ql and Dam.
            if (actionId != PlayerAction.EXAMINE.getId()
                    && actionId != PlayerAction.DEFAULT_ACTION.getId()) return;
            WORLD_IMPROVE.examineSent(targets[0],
                    SmartImproveOriginGuard.isActive());
        } catch (Throwable failure) {
            WORLD_IMPROVE.clear();
            LOGGER.log(Level.FINE, "Unable to capture world Examine target", failure);
        }
    }

    public static void observeWorldImproveSelection(PickableUnit selected) {
        try {
            WORLD_IMPROVE.selectionChanged(
                    selected == null ? Long.MIN_VALUE : selected.getId());
        } catch (Throwable failure) {
            WORLD_IMPROVE.clear();
            LOGGER.log(Level.FINE, "Unable to update world Improve selection", failure);
        }
    }

    public static boolean observeWorldImproveEvent(String context, String message) {
        try {
            BULK_TRANSFERS.observeEvent(context, message);
        } catch (Throwable failure) {
            LOGGER.log(Level.FINE, "Unable to observe bulk-transfer rejection", failure);
        }
        try {
            return WORLD_IMPROVE.event(context, message);
        } catch (Throwable failure) {
            WORLD_IMPROVE.clear();
            LOGGER.log(Level.FINE, "Unable to capture world Improve Event metadata", failure);
            return false;
        }
    }

    public static void observeActionSent(long sourceId, long[] targets, PlayerAction action) {
        if (targets == null || targets.length == 0) return;
        if (isStopAction(action)) return;
        String actionName = queueActionName(action);
        String sourceName = queueObjectName(sourceId, true);
        boolean smartImprove = SmartImproveOriginGuard.isActive()
                && isSmartImproveMutation(action);
        if (action != null && action.isAtomic()) {
            String targetName = targets.length == 1
                    ? queueObjectName(targets[0], false)
                    : Messages.text("queue.monitor.targets", targets.length);
            ACTION_QUEUE.actionSent(actionName, sourceName, targetName, targets[0],
                    smartImprove);
            return;
        }
        for (int index = 0; index < Math.min(10, targets.length); index++)
            ACTION_QUEUE.actionSent(actionName, sourceName,
                    queueObjectName(targets[index], false), targets[index], smartImprove);
    }

    /**
     * Observe inbound Event text before HUD replacements can consume it, then
     * keep the quiet-Examine decision alive through the native render path.
     */
    public static void beginWorldImproveEvent(String context, String message) {
        WorldImproveEventScope.enter(observeWorldImproveEvent(context, message));
    }

    public static void endWorldImproveEvent() {
        WorldImproveEventScope.exit();
    }

    public static boolean suppressWorldImproveEvent() {
        return WorldImproveEventScope.shouldSuppress();
    }

    public static void observeSingleActionSent(long sourceId, long targetId,
                                               PlayerAction action) {
        if (isStopAction(action)) return;
        if (action != null && (action.getId() == PlayerAction.REPAIR.getId()
                || action.getId() == PlayerAction.IMPROVE.getId()))
            WORLD_IMPROVE.invalidate(targetId);
        ACTION_QUEUE.actionSent(queueActionName(action),
                queueObjectName(sourceId, true), queueObjectName(targetId, false), targetId,
                SmartImproveOriginGuard.isActive() && isSmartImproveMutation(action));
    }

    public static void observeActionState(String actionText, float durationSeconds) {
        try {
            ACTION_QUEUE.actionState(actionText, durationSeconds);
            ActionQueueEntry remembered = ACTION_QUEUE.claimReadyCancellation();
            if (remembered != null && sendStopForMonitoredAction(remembered)) {
                EVENTS.info(Messages.text("event.queue_monitor_auto_stop",
                        remembered.getAction()));
            }
        } catch (Throwable failure) {
            LOGGER.log(Level.WARNING, "Unable to process a remembered queue cancellation",
                    failure);
        }
    }

    private static boolean isStopAction(PlayerAction action) {
        return action != null && action.getId() == PlayerAction.STOP.getId();
    }

    private static boolean isSmartImproveMutation(PlayerAction action) {
        return action != null && (action.getId() == PlayerAction.REPAIR.getId()
                || action.getId() == PlayerAction.IMPROVE.getId());
    }

    private static String queueActionName(PlayerAction action) {
        if (action == null) return Messages.text("editor.action");
        KeybinderMod instance = INSTANCE;
        if (instance != null) {
            String displayed = instance.getActionName(action.getId());
            if (displayed != null && !displayed.trim().isEmpty())
                return stripActionNumber(displayed.trim());
        }
        String name = action.getName();
        if (name != null && !name.trim().isEmpty()) return stripActionNumber(name.trim());
        return Messages.text("event.action_number", action.getId());
    }

    private static String queueObjectName(long objectId, boolean source) {
        if (objectId < 0L)
            return Messages.text(source ? "source.empty_hand" : "queue.monitor.no_target");
        if (!source && isTileId(objectId))
            return Messages.text("queue.monitor.tile",
                    Tiles.decodeTileX(objectId), Tiles.decodeTileY(objectId));
        HeadsUpDisplay currentHud = hud;
        ClientAccess access = ACCESS;
        if (currentHud != null && access != null) {
            try {
                InventoryMetaItem item = access.inventoryItem(currentHud, objectId);
                String name = preferredName(item);
                if (!name.isEmpty()) return name;
            } catch (ReflectiveOperationException | RuntimeException failure) {
                LOGGER.log(Level.FINEST, "Unable to describe queued inventory object "
                        + objectId, failure);
            }
            try {
                String name = access.objectType(currentHud, objectId);
                if (name != null && !name.trim().isEmpty()) return name.trim();
            } catch (ReflectiveOperationException | RuntimeException failure) {
                LOGGER.log(Level.FINEST, "Unable to describe queued world object "
                        + objectId, failure);
            }
        }
        return Messages.text("queue.monitor.object", objectId);
    }

    private static boolean isTileId(long id) {
        int type = (int) (id & 0xffL);
        return type == 3 || type == 17;
    }

    public static Object interceptToolbeltSelection(Object proxy,
                                                    java.lang.reflect.Method method,
                                                    Object[] args) throws Throwable {
        if (SELECTION.getMode() == SelectionController.Mode.TOOLBELT) {
            java.lang.reflect.Method slotMethod = proxy.getClass().getDeclaredMethod(
                    "getSlotNumberAt", int.class, int.class, boolean.class);
            slotMethod.setAccessible(true);
            int slot = (Integer) slotMethod.invoke(proxy, args[0], args[1], true);
            if (SELECTION.acceptToolbelt(slot)) {
                deferUi(() -> finishSlotSelection(true));
                if (window != null) window.refresh();
                return null;
            }
        } else if (SELECTION.getMode() == SelectionController.Mode.EXACT_OBJECT) {
            java.lang.reflect.Method slotMethod = proxy.getClass().getDeclaredMethod(
                    "getSlotNumberAt", int.class, int.class, boolean.class);
            slotMethod.setAccessible(true);
            int slot = (Integer) slotMethod.invoke(proxy, args[0], args[1], true);
            if (captureExactToolbeltSlot(slot)) return null;
        }
        return method.invoke(proxy, args);
    }

    public static Object interceptEquipmentSelection(Object proxy,
                                                     java.lang.reflect.Method method,
                                                     Object[] args) throws Throwable {
        if (SELECTION.getMode() == SelectionController.Mode.EQUIPMENT) {
            java.lang.reflect.Method slotMethod = proxy.getClass().getDeclaredMethod(
                    "getSlotNumberAt", int.class, int.class);
            slotMethod.setAccessible(true);
            byte slot = (Byte) slotMethod.invoke(proxy, args[0], args[1]);
            if (SELECTION.acceptEquipment(slot)) {
                deferUi(() -> finishSlotSelection(false));
                if (window != null) window.refresh();
                return null;
            }
        } else if (SELECTION.getMode() == SelectionController.Mode.EXACT_OBJECT) {
            java.lang.reflect.Method slotMethod = proxy.getClass().getDeclaredMethod(
                    "getSlotNumberAt", int.class, int.class);
            slotMethod.setAccessible(true);
            byte slot = (Byte) slotMethod.invoke(proxy, args[0], args[1]);
            if (captureExactEquipmentSlot(slot)) return null;
        }
        return method.invoke(proxy, args);
    }

    public static void observeCapturedAction(PlayerAction action) {
        try {
            if (action != null) rememberActionName(action.getId(), action.getName());
            ACTION_CAPTURE.observe(action);
        } catch (Throwable failure) {
            // Observation is passive. It must never block the action selected
            // by the player if a client build exposes unexpected metadata.
            LOGGER.log(Level.FINE, "Unable to observe action for capture", failure);
        }
    }

    public static boolean interceptBulkTransferBml(final HeadsUpDisplay currentHud,
                                                   String bml,
                                                   String title) {
        try {
            return BULK_TRANSFERS.intercept(bml, title,
                    (fields, buttonId) -> currentHud.getWorld()
                            .getServerConnection()
                            .sendBmlResponse(fields, buttonId));
        } catch (Throwable failure) {
            LOGGER.log(Level.WARNING, "Bulk-transfer BML hook failed open", failure);
            return false;
        }
    }

    public static void observeMousePressed(int mouseX, int mouseY, int button) {
        try {
            KeybinderMultiSelectorWindow selector = multiSelectorWindow;
            String variantId = selector == null ? null : selector.variantAt(mouseX, mouseY);
            if (INPUT.pointerPressed(button, variantId)) closeMultiSelector();
            if ((SELECTION.getMode() == SelectionController.Mode.EXACT_OBJECT
                    || SELECTION.getMode() == SelectionController.Mode.NEARBY_TYPE
                    || SELECTION.getMode() == SelectionController.Mode.HOVER_TYPE)
                    && button == 0) {
                TARGET_CLICK.press(mouseX, mouseY, System.currentTimeMillis());
            }
        } catch (Throwable e) {
            resetExactPress();
            LOGGER.log(Level.WARNING, "Unable to observe mouse press for target selection", e);
        }
    }

    public static void observeMouseDragged() {
        try {
            TARGET_CLICK.dragged();
        } catch (Throwable e) {
            resetExactPress();
            LOGGER.log(Level.WARNING, "Unable to observe mouse drag for target selection", e);
        }
    }

    public static void observeMouseReleased(Object eventHandler, int mouseX, int mouseY, int button) {
        try {
            KeybinderMultiSelectorWindow selector = multiSelectorWindow;
            String variantId = selector == null ? null : selector.variantAt(mouseX, mouseY);
            if (INPUT.pointerReleased(button, variantId)) closeMultiSelector();
            if ((SELECTION.getMode() == SelectionController.Mode.EXACT_OBJECT
                    || SELECTION.getMode() == SelectionController.Mode.NEARBY_TYPE
                    || SELECTION.getMode() == SelectionController.Mode.HOVER_TYPE)
                    && button == 0
                    && isExactObjectClick(eventHandler, mouseX, mouseY))
                captureWorldTarget(eventHandler);
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Unable to observe mouse release for target selection", e);
        } finally {
            if (button == 0) resetExactPress();
        }
    }

    public static void afterPushTargetRecreated(SelectBar selectBar, PickableUnit unit) {
        if (selectBar == null || unit == null) return;
        try {
            long unitId = unit.getId();
            if (PUSH_SELECTION.afterRecreated(
                    unitId, KeybinderSelectionBridge.isSelected(selectBar, unitId)))
                selectBar.keepSelectedItem(unitId);
        } catch (Throwable e) {
            PUSH_SELECTION.clear();
            LOGGER.log(Level.FINE,
                    "Unable to retain selection across a queued Push action", e);
        }
    }

    private static boolean isExactObjectClick(Object eventHandler, int releaseX, int releaseY) {
        long now = System.currentTimeMillis();
        if (!TARGET_CLICK.isCandidate(releaseX, releaseY, now)) return false;
        try {
            Field mouseLooking = eventHandler.getClass().getDeclaredField("isMouseLooking");
            Field draggedLooking = eventHandler.getClass().getDeclaredField("hasBeenDraggedInMouseLooking");
            mouseLooking.setAccessible(true);
            draggedLooking.setAccessible(true);
            return TARGET_CLICK.matches(releaseX, releaseY, now,
                    (Boolean) mouseLooking.get(eventHandler),
                    (Boolean) draggedLooking.get(eventHandler));
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Unable to verify exact-object click gesture", e);
            return false;
        }
    }

    private static void resetExactPress() {
        TARGET_CLICK.reset();
    }

    private static void captureWorldTarget(Object eventHandler) {
        try {
            Field validPickField = eventHandler.getClass().getDeclaredField("wasValidPick");
            Field pickedField = eventHandler.getClass().getDeclaredField("oldCurrentpickable");
            validPickField.setAccessible(true);
            pickedField.setAccessible(true);
            if (!(Boolean) validPickField.get(eventHandler)) return;
            Object value = pickedField.get(eventHandler);
            if (!(value instanceof PickableUnit)) return;
            PickableUnit picked = (PickableUnit) value;
            boolean accepted = SELECTION.getMode() == SelectionController.Mode.NEARBY_TYPE
                    ? SELECTION.acceptNearbyType(ACCESS.objectType(picked))
                    : SELECTION.getMode() == SelectionController.Mode.HOVER_TYPE
                    ? SELECTION.acceptHoverType(ACCESS.objectType(picked))
                    : SELECTION.acceptExactObject(picked.getId(), picked.getHoverName());
            if (accepted)
                deferUi(KeybinderMod::finishExactObjectSelection);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Unable to capture world target", e);
        }
    }

    private static boolean captureExactToolbeltSlot(int zeroBasedSlot) {
        try {
            if (zeroBasedSlot < 0 || hud == null || hud.getToolBelt() == null) return false;
            InventoryMetaItem item = hud.getToolBelt().getItemInSlot(zeroBasedSlot);
            if (item == null) {
                EVENTS.warning(Messages.text("event.toolbelt_empty"));
                return false;
            }
            if (!SELECTION.acceptExactObject(item.getId(), item.getDisplayName())) return false;
            deferUi(KeybinderMod::finishExactObjectSelection);
            return true;
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to capture exact object from toolbelt", e);
            return false;
        }
    }

    private static boolean captureExactEquipmentSlot(byte slot) {
        try {
            if (slot < 0 || hud == null) return false;
            com.wurmonline.client.renderer.gui.PaperDollSlot frame =
                    ACCESS.equipmentSlot(hud.getPaperDollInventory(), slot);
            if (frame == null || frame.getEquippedItem() == null) {
                EVENTS.warning(Messages.text("event.equipment_empty"));
                return false;
            }
            InventoryMetaItem item = frame.getEquippedItem().getItem();
            if (item == null) {
                EVENTS.warning(Messages.text("event.equipment_empty"));
                return false;
            }
            if (!SELECTION.acceptExactObject(item.getId(), item.getDisplayName())) return false;
            deferUi(KeybinderMod::finishExactObjectSelection);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to capture exact object from equipment", e);
            return false;
        }
    }

    public static Object withCapturedTarget(String target, Object proxy,
                                            java.lang.reflect.Method method, Object[] args)
            throws Throwable {
        ACTION_CAPTURE.setTargetContext(target);
        try { return method.invoke(proxy, args); }
        finally { ACTION_CAPTURE.clearTargetContext(); }
    }

    private static void hideSafely(com.wurmonline.client.renderer.gui.WurmComponent component) {
        try {
            if (hud != null && component != null) ACCESS.hideComponent(hud, component);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.close_window"), e);
        }
    }

    private static void finishSlotSelection(boolean toolbelt) {
        hideSafely(selectionWindow);
        selectionWindow = null;
        try {
            if (toolbelt && toolbeltOpenedForSelection)
                hideSafely(ACCESS.toolbeltComponent(hud));
            if (!toolbelt && equipmentOpenedForSelection)
                hideSafely(ACCESS.paperDollComponent(hud));
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.restore_selection_windows"), e);
        } finally {
            if (toolbelt) toolbeltOpenedForSelection = false;
            else equipmentOpenedForSelection = false;
        }
    }

    private static void finishExactObjectSelection() {
        hideSafely(selectionWindow);
        selectionWindow = null;
        if (inventoryOpenedForSelection && hud != null)
            hideSafely(hud.getInventoryWindow());
        inventoryOpenedForSelection = false;
    }

    private static void cancelSlotSelection() {
        hideSafely(selectionWindow);
        selectionWindow = null;
        try {
            if (toolbeltOpenedForSelection) hideSafely(ACCESS.toolbeltComponent(hud));
            if (equipmentOpenedForSelection) hideSafely(ACCESS.paperDollComponent(hud));
            if (inventoryOpenedForSelection && hud != null)
                hideSafely(hud.getInventoryWindow());
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.restore_selection_windows"), e);
        } finally {
            toolbeltOpenedForSelection = false;
            equipmentOpenedForSelection = false;
            inventoryOpenedForSelection = false;
        }
    }

    public static void rememberActionName(short actionId, String name) {
        if (name == null) return;
        String clean = name.trim();
        if (!clean.isEmpty()) {
            String previous = ACTION_NAMES.put(actionId, clean);
            if (!clean.equals(previous) && INSTANCE != null && INSTANCE.registry != null)
                INSTANCE.registry.rememberActionName(actionId, clean);
        }
    }

    public static void rememberPopupSubmenu(Object parentPopup, Object submenu, String label) {
        if (submenu == null || label == null) return;
        String cleanLabel = stripActionNumber(label.trim());
        if (cleanLabel.isEmpty()) return;
        String parentPath = POPUP_PATHS.get(parentPopup);
        POPUP_PATHS.put(submenu, parentPath == null || parentPath.isEmpty()
                ? cleanLabel : parentPath + " -> " + cleanLabel);
    }

    public static void rememberActionMenuPath(Object popup, PlayerAction action) {
        if (popup == null || action == null) return;
        String parent = POPUP_PATHS.get(popup);
        if (parent == null) return;
        String cleanParent = stripActionNumber(parent.trim());
        String leaf = stripActionNumber(action.getName() == null ? "" : action.getName().trim());
        if (cleanParent.isEmpty() || leaf.isEmpty() || cleanParent.equalsIgnoreCase(leaf)) return;
        String path = cleanParent + " -> " + leaf;
        ACTION_PATHS.put(action.getId(), path);
        if (INSTANCE != null && INSTANCE.registry != null)
            INSTANCE.registry.rememberActionName(action.getId(), path);
    }

    private static String stripActionNumber(String value) {
        return value.replaceFirst("\\s+\\(-?\\d+\\)$", "");
    }

    public static void onHudReady(HeadsUpDisplay newHud) {
        try {
            ensureRuntimeServices();
            HeadsUpDisplay previousHud = HUD_SESSIONS.replace(newHud);
            if (previousHud != null) disposeHudSession(previousHud);
            if (INSTANCE.pendingLanguage != null)
                INSTANCE.activatePendingLanguageForHudReplacement();
            ACCESS.setup();
            hud = newHud;
            creationCatalogRequestAttempts = 0;
            lastCreationCatalogRequest = 0L;
            requestCreationSkillCatalog(newHud);
            ACCOUNT_ACTIVATION.clear();
            embarkHeading.initializeClientAccess(INSTANCE.centerViewAfterEmbark);
            refreshCreationContext();
            EVENTS.attach(newHud);
            provideValuePackIfNeeded();
            ACTION_CAPTURE.cancel();
            ACTION_QUEUE.clear();
            PUSH_SELECTION.clear();
            WORLD_IMPROVE.clear();
            BULK_TRANSFERS.clear("HUD initialized");
            resetExactPress();
            INPUT.clear();
            closeMultiSelector();
            window = new KeybinderWindow(INSTANCE);
            tagWindow = new KeybinderTagWindow(INSTANCE);
            queueMonitor = new KeybinderActionQueueMonitor(INSTANCE);
            HudIntegration hudIntegration = new HudIntegration(ACCESS);
            hudIntegration.register(newHud, window);
            hudIntegration.registerTag(newHud, tagWindow);
            hudIntegration.registerQueueMonitor(newHud, queueMonitor);
            if (LEGACY_ACTION.isInstalled())
                EVENTS.warning(Messages.text("event.legacy_installed"));
            if (LEGACY_IMPROVE.isInstalled())
                EVENTS.warning(Messages.text("event.improve_installed"));
            if (INSTANCE.skipIntro) {
                window.showKeybinds();
                showTagInsteadOfWindow();
            }
            else {
                window.showIntro();
                ACCESS.ensureComponentVisible(newHud, window);
                ACCESS.setComponentVisible(newHud, tagWindow, false);
            }
            if (registry != null) {
                WurmConsole console = ACCESS.console(newHud);
                applyAccountBindingsIfReady(newHud);
                List<org.keybinder.wurm.bind.BindSnapshot> candidates = registry.importCandidates(console);
                if (!candidates.isEmpty()) {
                    EVENTS.info(Messages.text("event.import_candidates_found", candidates.size()));
                    if (!INSTANCE.vanillaImportPromptDismissed
                            && !INSTANCE.importPromptOfferedThisSession) {
                        INSTANCE.importPromptOfferedThisSession = true;
                        INSTANCE.showImportReview(candidates);
                    }
                }
            }
            EVENTS.info(Messages.text("event.ready", VERSION, LIMITS.readLimit(newHud)));
        } catch (Throwable e) {
            EVENTS.error(Messages.text("error.attach_hud",
                    e.getClass().getSimpleName(), safeMessage(e)), e);
        }
    }

    private static void provideValuePackIfNeeded() {
        try (InputStream input = PackagedResourceLoader.open(
                KeybinderMod.class, ValuePackProvider.RESOURCE,
                Paths.get("mods", "keybinder", "keybinder.jar"))) {
            ValuePackProvider.ProvisionResult provision = VALUE_PACK.provideIfNeeded(
                    INSTANCE.properties, input, registry, MOD_PROPERTIES::save);
            if (provision.wasProvidedNow()) {
                TransferImportResult result = provision.getImportResult();
                EVENTS.info(Messages.text("event.value_pack_provided",
                        result.getImported(), result.getSkippedDuplicates()));
            }
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.value_pack_provision"), e);
        }
    }

    private static void disposeHudSession(HeadsUpDisplay oldHud) {
        if (KEYBIND_EXECUTOR != null) KEYBIND_EXECUTOR.cancelPending();
        ACTION_CAPTURE.cancel();
        SELECTION.cancel();
        ACTION_QUEUE.clear();
        PUSH_SELECTION.clear();
        WORLD_IMPROVE.clear();
        BULK_TRANSFERS.clear("HUD replaced");
        resetExactPress();
        INPUT.clear();
        UI_AFTER_TICK.clear();
        if (ACCESS != null)
            new HudSessionDisposer<HeadsUpDisplay, WurmComponent>(
                    LOGGER, ACCESS::hideComponent).hideAll(oldHud,
                    queueMonitor, captureWindow, conflictWindow, tileWindow, selectionWindow,
                    multiSelectorWindow, mergeWindow, editorWindow, importWindow,
                    legacyWindow);
        captureWindow = null;
        conflictWindow = null;
        tileWindow = null;
        selectionWindow = null;
        multiSelectorWindow = null;
        mergeWindow = null;
        editorWindow = null;
        importWindow = null;
        legacyWindow = null;
        window = null;
        tagWindow = null;
        queueMonitor = null;
        if (editorWorkflow != null) editorWorkflow.clear();
        toolbeltOpenedForSelection = false;
        equipmentOpenedForSelection = false;
        inventoryOpenedForSelection = false;
    }

    private static synchronized void ensureRuntimeServices() {
        if (ACCESS != null) return;
        ACCESS = new ClientAccess();
        embarkHeading = new EmbarkHeadingController(false,
                failure -> LOGGER.log(Level.WARNING,
                        "Center-view-after-embark integration failed open and is disabled "
                                + "until the next HUD initialization", failure));
        EXECUTOR = new ActionExecutor(ACCESS, INSTANCE::getActionName, PUSH_SELECTION);
        KEYBIND_EXECUTOR = new KeybindExecutionService(
                EXECUTOR, ACCESS, EVENTS, WORLD_IMPROVE, BULK_TRANSFERS,
                targetId -> ACTION_QUEUE.hasSmartImproveActions(targetId));
    }

    public static void onConnectionEnded() {
        CURRENT_SERVER.connectionChanging();
        clearConnectionState();
    }

    private static void requestCreationSkillCatalog(HeadsUpDisplay currentHud) {
        if (currentHud == null || creationCatalogRequestAttempts >= 3) return;
        try {
            creationCatalogRequestAttempts++;
            lastCreationCatalogRequest = System.currentTimeMillis();
            currentHud.getWorld().getServerConnection()
                    .sendRequestFullCreateItemList();
            LOGGER.info("[Keybinder] [Smart Improve diagnostic] requested the "
                    + "server creation-skill catalog; attempt="
                    + creationCatalogRequestAttempts);
        } catch (Throwable failure) {
            LOGGER.log(Level.WARNING, "[Keybinder] [Smart Improve diagnostic] "
                    + "unable to request the server creation-skill catalog", failure);
        }
    }

    private static void ensureCreationSkillCatalog(HeadsUpDisplay currentHud) {
        if (CreationSkillRegistry.size() > 0 || creationCatalogRequestAttempts >= 3)
            return;
        long now = System.currentTimeMillis();
        if (now - lastCreationCatalogRequest >= 3000L)
            requestCreationSkillCatalog(currentHud);
    }

    public static void onServerTransfer(String host, int port) {
        CURRENT_SERVER.connectionChanging();
        clearConnectionState();
        LOGGER.fine("Server transfer started: " + host + ":" + port);
    }

    public static void onServerInformation(String serverName) {
        CURRENT_SERVER.serverInformation(serverName);
        refreshCreationContext();
        LOGGER.info("Current server updated to "
                + (registry == null ? cleanServerName(serverName) : registry.getCurrentServer()));
    }

    private static void clearConnectionState() {
        try {
            if (registry != null) registry.persistAccountBindings();
            if (KEYBIND_EXECUTOR != null) KEYBIND_EXECUTOR.cancelPending();
            ACTION_QUEUE.clear();
            WORLD_IMPROVE.clear();
            CreationSkillRegistry.clear();
            creationCatalogRequestAttempts = 0;
            lastCreationCatalogRequest = 0L;
            BULK_TRANSFERS.clear("connection ended");
            ACCOUNT_ACTIVATION.clear();
        } catch (Throwable failure) {
            LOGGER.log(Level.FINE, "Unable to clear action queue on disconnect", failure);
        }
    }

    private static void refreshCreationContext() {
        if (registry == null) return;
        String user = "";
        String server = "";
        String shortServer = "";
        try {
            if (hud != null && hud.getWorld() != null) {
                try {
                    user = hud.getWorld().getUsername();
                } catch (RuntimeException ignored) {
                    // ServerConnection is not available during early HUD init.
                }
                shortServer = CURRENT_SERVER.currentOrWorld(hud.getWorld().getServerName());
                server = resolveFullServerName(shortServer);
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "Creation context is not ready yet", e);
        }
        if (user == null || user.trim().isEmpty()) {
            try {
                user = WurmClientBase.getUsername();
            } catch (RuntimeException ignored) {
                user = "";
            }
        }
        registry.setCreationContext(user, server);
        registry.enrichCurrentServerName(shortServer, server);
    }

    /**
     * HUD construction can finish before World exposes the logged-in player
     * name. Retry cheaply from the HUD tick and apply exactly once per alt.
     */
    private static boolean applyAccountBindingsIfReady(HeadsUpDisplay currentHud) {
        if (registry == null || ACCESS == null || currentHud == null) return false;
        String account = currentPlayerName(currentHud);
        if (ACCOUNT_ACTIVATION.isApplied(account)) return true;
        long now = System.currentTimeMillis();
        if (!ACCOUNT_ACTIVATION.shouldApply(account, now)) return false;
        try {
            refreshCreationContext();
            WurmConsole console = ACCESS.console(currentHud);
            if (!registry.restoreAccountBindings(account, console)) {
                ACCOUNT_ACTIVATION.failed(account, now, 5000L);
                return false;
            }
            registry.reconcileManagedBindings(console);
            ACCOUNT_ACTIVATION.applied(account);
            if (window != null) window.refresh();
            LOGGER.info("Applied Keybinder activation profile for " + account);
            EVENTS.info(Messages.text("registry.account_applied", account));
            return true;
        } catch (Throwable failure) {
            ACCOUNT_ACTIVATION.failed(account, now, 5000L);
            LOGGER.log(Level.WARNING,
                    "Unable to apply Keybinder activation profile for " + account, failure);
            return false;
        }
    }

    private static String currentPlayerName(HeadsUpDisplay currentHud) {
        try {
            if (currentHud.getWorld() == null) return "";
            String value = currentHud.getWorld().getUsername();
            return value == null ? "" : value.trim();
        } catch (RuntimeException notReady) {
            return "";
        }
    }

    private static String resolveFullServerName(String worldServerName) {
        String shortName = worldServerName == null ? "" : worldServerName.trim();
        if (shortName.isEmpty()) return "";
        if (matchesShard(selectedFullServerName, shortName)) return selectedFullServerName;
        try {
            if (WurmClientBase.steamHandler == null
                    || WurmClientBase.steamHandler.getServerListFX() == null) return shortName;
            List<String> names = new java.util.ArrayList<>();
            for (com.wurmonline.client.steam.SteamServerFX entry
                    : WurmClientBase.steamHandler.getServerListFX()) {
                if (entry != null) names.add(entry.getServerName());
            }
            return SERVER_NAMES.resolve(shortName, names);
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "Unable to resolve full Steam server name", e);
            return shortName;
        }
    }

    private static boolean matchesShard(String fullName, String shortName) {
        if (fullName == null || shortName == null) return false;
        String full = fullName.trim().toLowerCase(Locale.ENGLISH);
        String shard = shortName.trim().toLowerCase(Locale.ENGLISH);
        return full.equals(shard) || full.endsWith(" - " + shard)
                || full.endsWith("-" + shard);
    }

    private static String cleanServerName(String value) {
        return value == null ? "" : value.trim();
    }

    private static KeybinderMod INSTANCE;
    public KeybinderMod() { INSTANCE = this; }

    public static boolean handleCommand(String command, String[] data) {
        return COMMANDS.route(command, data, COMMAND_CONTEXT);
    }

    private static void ensureHud() {
        if (hud == null) throw new IllegalStateException(Messages.text("error.hud_not_ready"));
    }

    private static void ensureRegistry() {
        ensureHud();
        if (registry == null)
            throw new IllegalStateException(Messages.text("error.registry_not_ready"));
    }

    @Override public int getQueueLimit() { return LIMITS.readLimit(hud); }
    @Override public int getQueueMonitorSlots() {
        int limit = LIMITS.readLimit(hud);
        return limit > 0 ? Math.min(10, limit) : 10;
    }
    @Override public List<ActionQueueEntry> getMonitoredActions() {
        ACTION_QUEUE.occupied(hudShowsAction(hud));
        return ACTION_QUEUE.snapshot(10);
    }
    @Override public void cancelMonitoredAction(long sequence) {
        HeadsUpDisplay currentHud = hud;
        if (currentHud == null) return;
        ACTION_QUEUE.occupied(hudShowsAction(currentHud));
        ActionQueueEntry selected = null;
        for (ActionQueueEntry entry : ACTION_QUEUE.snapshot(10))
            if (entry.getSequence() == sequence) {
                selected = entry;
                break;
            }
        ActionQueueOccupancyTracker.CancellationRequest request =
                ACTION_QUEUE.requestCancellation(sequence);
        switch (request) {
            case CURRENT:
                sendStopForMonitoredAction(selected);
                return;
            case QUEUED_SCHEDULED:
                EVENTS.info(Messages.text("event.queue_monitor_queued_cancel_scheduled",
                        selected == null ? Messages.text("editor.action")
                                : selected.getAction()));
                return;
            case ALREADY_REQUESTED:
                EVENTS.warning(Messages.text("event.queue_monitor_stop_pending"));
                return;
            case NOT_FOUND:
            default:
                return;
        }
    }

    private static boolean sendStopForMonitoredAction(ActionQueueEntry selected) {
        if (selected == null) return false;
        HeadsUpDisplay currentHud = hud;
        if (currentHud == null) {
            ACTION_QUEUE.cancellationFailed(selected.getSequence());
            return false;
        }
        try {
            currentHud.sendAction(PlayerAction.STOP, selected.getTargetId());
            return true;
        } catch (Throwable failure) {
            ACTION_QUEUE.cancellationFailed(selected.getSequence());
            EVENTS.error(Messages.text("error.queue_monitor_stop"), failure);
            return false;
        }
    }
    @Override public QueueCost getKeybindCost(KeybindRecord record) { return COSTS.keybindCost(record); }
    @Override public List<KeybindRecord> getRecords() { return registry == null ? Collections.<KeybindRecord>emptyList() : registry.snapshot(); }
    @Override public void addNewKeybind() {
        try {
            refreshCreationContext();
            registry.createDraft();
            if (window != null) window.refresh();
        } catch (Exception e) { EVENTS.error(Messages.text("error.create_keybind"), e); }
    }
    @Override public void addNewKeybindAfter(String id) {
        try {
            refreshCreationContext();
            registry.createDraftAfter(id);
            if (window != null) window.refresh();
        } catch (Exception e) { EVENTS.error(Messages.text("error.create_keybind"), e); }
    }
    @Override public void moveKeybind(String id, List<String> visibleIds, int insertionIndex) {
        try {
            registry.moveVisible(id, visibleIds, insertionIndex);
            if (window != null) window.refresh();
        } catch (Exception e) { EVENTS.error(Messages.text("error.reorder_keybind"), e); }
    }
    @Override public void duplicateKeybind(String id) {
        try {
            refreshCreationContext();
            KeybindRecord copy = registry.duplicate(id);
            EVENTS.info(Messages.text("event.duplicate_complete", copy.getName()));
            if (window != null) window.refresh();
            editKeybind(copy.getId());
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.duplicate_keybind"), e);
            if (window != null) window.refresh();
        }
    }
    @Override public void requestMerge(String sourceId, String destinationId) {
        try {
            if (sourceId == null || sourceId.equals(destinationId)) return;
            KeybindRecord source = record(sourceId);
            KeybindRecord destination = record(destinationId);
            if (source == null || destination == null)
                throw new IllegalArgumentException(Messages.text("merge.record_missing"));
            int result = source.getVariants().size() + destination.getVariants().size();
            if (result > org.keybinder.wurm.model.KeybindLimits.MAX_VARIANTS) {
                EVENTS.warning(Messages.text("merge.too_many",
                        destination.getVariants().size(), source.getVariants().size(), result,
                        org.keybinder.wurm.model.KeybindLimits.MAX_VARIANTS));
                return;
            }
            final KeybindRecord sourceRecord = source;
            final KeybindRecord destinationRecord = destination;
            deferUi(new Runnable() {
                @Override public void run() {
                    hideSafely(mergeWindow);
                    mergeWindow = new KeybinderMergeWindow(INSTANCE,
                            sourceRecord, destinationRecord);
                    try { new HudIntegration(ACCESS).add(hud, mergeWindow); }
                    catch (ReflectiveOperationException e) {
                        mergeWindow = null;
                        EVENTS.error(Messages.text("error.merge_window"), e);
                    }
                }
            });
        } catch (Exception e) { EVENTS.error(Messages.text("error.merge_keybind"), e); }
    }
    @Override public void confirmMerge(String sourceId, String destinationId) {
        hideSafely(mergeWindow);
        mergeWindow = null;
        try {
            boolean editingSource = editorWindow != null && editorWindow.editsRecord(sourceId);
            boolean editingDestination = editorWindow != null
                    && editorWindow.editsRecord(destinationId);
            KeybindRecord merged = registry.merge(sourceId, destinationId, ACCESS.console(hud));
            if (editingSource) {
                editorWindow = null;
                if (window != null) window.showKeybinds();
            } else if (editingDestination) {
                editKeybind(destinationId);
            }
            closeMultiSelector();
            EVENTS.info(Messages.text("event.merge_complete",
                    merged.getName(), merged.getVariants().size()));
            if (window != null) window.refresh();
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.merge_keybind"), e);
            if (window != null) window.refresh();
        }
    }
    @Override public void cancelMerge() {
        hideSafely(mergeWindow);
        mergeWindow = null;
    }

    private KeybindRecord record(String id) {
        for (KeybindRecord candidate : getRecords())
            if (candidate.getId().equals(id)) return candidate;
        return null;
    }
    @Override public void editKeybind(String id) {
        deferUi(() -> {
            try {
                editorWindow = new KeybinderEditorWindow(INSTANCE, id);
                if (window != null) window.showEditor(editorWindow);
            } catch (Exception e) { EVENTS.error(Messages.text("error.open_editor"), e); }
        });
    }
    @Override public void deleteKeybind(String id) {
        try {
            registry.delete(id, ACCESS.console(hud));
            if ((multiSelectorWindow != null && multiSelectorWindow.selectsRecord(id))
                    || INPUT.isHoldingRecord(id)) closeMultiSelector();
            if (window != null) window.refresh();
        } catch (Exception e) { EVENTS.error(Messages.text("error.delete_keybind"), e); }
    }
    @Override public void setKeybindEnabled(String id, boolean enabled) {
        if (!applyAccountBindingsIfReady(hud)) {
            EVENTS.warning(Messages.text("registry.account_not_ready"));
            return;
        }
        if (editorWorkflow != null) editorWorkflow.setEnabled(id, enabled);
    }

    @Override public void setKeybindsEnabled(List<String> ids, boolean enabled) {
        if (!applyAccountBindingsIfReady(hud)) {
            EVENTS.warning(Messages.text("registry.account_not_ready"));
            return;
        }
        int changed = 0;
        for (String id : new java.util.ArrayList<>(ids)) {
            try {
                registry.setEnabled(id, enabled, ACCESS.console(hud), LIMITS.readLimit(hud));
                changed++;
            } catch (Exception e) {
                EVENTS.error(Messages.text(enabled
                        ? "error.enable_filtered" : "error.disable_filtered", id), e);
            }
        }
        EVENTS.info(Messages.text(enabled
                ? "event.filtered_enabled" : "event.filtered_disabled", changed));
        if (window != null) window.refresh();
    }
    @Override public void requestImport() {
        try {
            refreshCreationContext();
            List<BindSnapshot> candidates = registry.importCandidates(ACCESS.console(hud));
            EVENTS.info(Messages.text("event.import_review", candidates.size()));
            showImportReview(candidates);
        } catch (Exception e) { EVENTS.error(Messages.text("error.import_inspect"), e); }
    }

    private void showImportReview(List<BindSnapshot> bindings) {
        final List<VanillaImportCandidate> review = IMPORT_REVIEW.review(
                bindings, registry == null
                        ? Collections.<KeybindRecord>emptyList() : registry.snapshot());
        if (review.isEmpty()) {
            EVENTS.info(Messages.text("event.import_none"));
            return;
        }
        deferUi(new Runnable() {
            @Override public void run() {
                hideSafely(importWindow);
                importWindow = new KeybinderImportWindow(INSTANCE, review);
                try {
                    new HudIntegration(ACCESS).add(hud, importWindow);
                } catch (ReflectiveOperationException e) {
                    importWindow = null;
                    EVENTS.error(Messages.text("error.import_window"), e);
                }
            }
        });
    }
    @Override public void requestImportFile() {
        if (transferWorkflow != null) transferWorkflow.requestImport();
    }
    @Override public void requestExportAll() {
        if (transferWorkflow != null) transferWorkflow.requestExport();
    }
    @Override public void confirmImport(List<BindSnapshot> selected) {
        try {
            refreshCreationContext();
            int imported = registry.importReviewed(
                    ACCESS.console(hud), selected, LIMITS.readLimit(hud));
            EVENTS.info(Messages.text("event.import_complete", imported));
            closeImportReview(false);
            if (window != null) window.refresh();
            if (LEGACY_ACTION.isInstalled()) showLegacyMigrationControls();
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.import_reviewed"), e);
        }
    }

    @Override public void closeImportReview(boolean doNotAskAgain) {
        hideSafely(importWindow);
        importWindow = null;
        if (!doNotAskAgain) return;
        vanillaImportPromptDismissed = true;
        properties.setProperty("vanillaImportPromptDismissed", "true");
        try {
            MOD_PROPERTIES.save(properties);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.save_import_prompt"), e);
        }
    }

    private void showLegacyMigrationControls() {
        deferUi(new Runnable() {
            @Override public void run() {
                hideSafely(legacyWindow);
                legacyWindow = new KeybinderLegacyWindow(INSTANCE);
                try { new HudIntegration(ACCESS).add(hud, legacyWindow); }
                catch (ReflectiveOperationException e) {
                    legacyWindow = null;
                    EVENTS.error(Messages.text("error.migration_window"), e);
                }
            }
        });
    }

    public static void captureInventoryTarget(Object panel, int mouseX, int mouseY) {
        try {
            SelectionController.Mode mode = SELECTION.getMode();
            if (mode != SelectionController.Mode.EXACT_OBJECT
                    && mode != SelectionController.Mode.NEARBY_TYPE
                    && mode != SelectionController.Mode.HOVER_TYPE
                    && mode != SelectionController.Mode.INVENTORY_FILTER
                    && mode != SelectionController.Mode.BULK_SOURCE
                    && mode != SelectionController.Mode.BULK_DESTINATION) return;
            if (mode == SelectionController.Mode.BULK_SOURCE
                    || mode == SelectionController.Mode.BULK_DESTINATION) {
                KeybinderInventorySelectionBridge.Row row =
                        KeybinderInventorySelectionBridge.rowAt(panel, mouseX, mouseY);
                if (row == null || row.getItem() == null) return;
                InventoryMetaItem clicked = row.getItem();
                String clickedName = preferredName(clicked);
                if (mode == SelectionController.Mode.BULK_DESTINATION) {
                    InventoryMetaItem destination =
                            KeybinderInventorySelectionBridge.itemUnderMouse(
                                    hud, mouseX, mouseY);
                    if (destination == null || !BulkInventoryDestinationPolicy.isValid(
                            destination.getId())) {
                        EVENTS.warning(Messages.text("event.bulk_destination_rejected"));
                        return;
                    }
                    String destinationName = preferredName(destination);
                    EVENTS.diagnostic("bulk destination click: clickedRowId="
                            + clicked.getId() + ", clickedRowName='" + clickedName
                            + "', clickedRowContainer="
                            + com.wurmonline.shared.util.ItemTypeUtilites.isContainer(
                            clicked.getTypeBits()) + ", resolvedDestinationId="
                            + destination.getId() + ", resolvedDestinationName='"
                            + destinationName + "'");
                    if (SELECTION.acceptBulkDestination(
                            destination.getId(), destinationName))
                        deferUi(KeybinderMod::finishExactObjectSelection);
                    return;
                }
                InventoryReference storage = ACCESS.openInventoryContaining(hud, clicked.getId());
                InventoryMetaItem storageRoot = storage == null
                        ? null : ACCESS.inventoryItem(hud, storage.getId());
                InventoryReference sourceStorage = BULK_SOURCES.resolve(
                        clicked, row.getAncestors(), storage, storageRoot,
                        id -> ACCESS.inventoryItem(hud, id));
                EVENTS.diagnostic("bulk source click: rowId=" + clicked.getId()
                        + ", rowName='" + clickedName + "', inventoryGroup="
                        + row.isInventoryGroup() + ", containingWindowId="
                        + (storage == null ? 0L : storage.getId()) + ", containingWindowName='"
                        + (storage == null ? "" : storage.getName())
                        + "', clickedParentId=" + clicked.getParentId()
                        + ", visibleAncestorCount=" + row.getAncestors().size()
                        + ", resolvedBulkStorageId="
                        + (sourceStorage == null ? 0L : sourceStorage.getId())
                        + ", resolvedBulkStorageName='"
                        + (sourceStorage == null ? "" : sourceStorage.getName()) + "'");
                if (row.isInventoryGroup() || sourceStorage == null) {
                    EVENTS.warning(Messages.text("event.bulk_source_rejected",
                            clickedName, clicked.getId(),
                            storage == null ? "?" : storage.getName()));
                    return;
                }
                if (SELECTION.acceptBulkSource(sourceStorage.getId(),
                        sourceStorage.getName(),
                        clicked.getId(), clickedName))
                    deferUi(KeybinderMod::finishExactObjectSelection);
                return;
            }
            InventoryMetaItem item =
                    KeybinderInventorySelectionBridge.itemAt(panel, mouseX, mouseY);
            if (item == null) return;
            if (mode == SelectionController.Mode.INVENTORY_FILTER) {
                if (SELECTION.acceptInventoryFilter(ACCESS.objectType(item)))
                    deferUi(KeybinderMod::finishExactObjectSelection);
                return;
            }
            boolean accepted = SELECTION.getMode() == SelectionController.Mode.NEARBY_TYPE
                    ? SELECTION.acceptNearbyType(item.getBaseName())
                    : SELECTION.getMode() == SelectionController.Mode.HOVER_TYPE
                    ? SELECTION.acceptHoverType(item.getBaseName())
                    : SELECTION.acceptExactObject(item.getId(), item.getDisplayName());
            if (accepted) deferUi(KeybinderMod::finishExactObjectSelection);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to resolve clicked inventory row", e);
        }
    }

    private static String preferredName(InventoryMetaItem item) {
        if (item == null) return "";
        String display = item.getDisplayName();
        if (display != null && !display.trim().isEmpty()) return display.trim();
        String base = item.getBaseName();
        return base == null ? "" : base.trim();
    }
    @Override public void restoreOriginalBindings() {
        try {
            KeybindRegistry.RestoreResult result =
                    registry.prepareForUninstall(ACCESS.console(hud));
            EVENTS.info(Messages.text("event.restore_originals_complete",
                    result.getRestored(), result.getRemoved(), result.getConflicts()));
            if (window != null) window.refresh();
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.restore_originals"), e);
        }
    }
    @Override public boolean isLegacyActionInstalled() { return LEGACY_ACTION.isInstalled(); }
    @Override public void startFromIntro() {
        deferUi(() -> {
            try {
                if (hud != null && window != null) {
                    window.showKeybinds();
                    if (pendingLanguage != null) applyLanguage(pendingLanguage);
                    showTagInsteadOfWindow();
                }
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.open_keybinder"), e);
            }
        });
    }
    @Override public void importDisableAndRestart() {
        deferUi(() -> {
            try {
                if (hud != null && window != null) {
                    window.showKeybinds();
                    showTagInsteadOfWindow();
                }
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.open_keybinder"), e);
            }
        });
        requestImport();
    }
    @Override public boolean isSkipIntro() { return skipIntro; }
    @Override public void setSkipIntro(boolean skip) {
        if (skipIntro == skip && String.valueOf(skip).equals(properties.getProperty("skipIntroPage"))) return;
        skipIntro = skip;
        properties.setProperty("skipIntroPage", String.valueOf(skip));
        try {
            MOD_PROPERTIES.save(properties);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.save_intro"), e);
        }
    }
    @Override public String getLanguage() { return language; }
    @Override public QueueMonitorSide getQueueMonitorSide() {
        return queueMonitorSide;
    }
    @Override public void setQueueMonitorSide(QueueMonitorSide requested) {
        QueueMonitorSide selected = requested == null ? QueueMonitorSide.RIGHT : requested;
        if (queueMonitorSide == selected && selected.getSetting().equals(
                properties.getProperty("queueMonitorSide"))) return;
        queueMonitorSide = selected;
        properties.setProperty("queueMonitorSide", selected.getSetting());
        try {
            MOD_PROPERTIES.save(properties);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.save_queue_monitor_side"), e);
        }
    }
    @Override public void setLanguage(String requested) {
        String selected = Language.fromCode(requested).getCode();
        LanguageChangePolicy.Decision decision = LanguageChangePolicy.decide(
                language, selected,
                (window != null && window.isEditorOpen()) || multiSelectorWindow != null,
                pendingLanguage);
        if (decision == LanguageChangePolicy.Decision.UNCHANGED) return;
        LocalizationSettings.save(properties, selected);
        try {
            MOD_PROPERTIES.save(properties);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.save_language"), e);
        }
        if (decision == LanguageChangePolicy.Decision.CANCEL_PENDING) {
            pendingLanguage = null;
            return;
        }
        if (decision == LanguageChangePolicy.Decision.DEFER) {
            pendingLanguage = selected;
            EVENTS.warning(Messages.text("event.language_deferred"));
            return;
        }
        applyLanguage(selected);
    }

    private void applyLanguage(String selected) {
        final String normalized = Language.fromCode(selected).getCode();
        String previous = language;
        final KeybinderWindow currentWindow = window;
        pendingLanguage = null;
        ExistingWindowRelocalizer.apply(previous, normalized, () -> {
            language = normalized;
            Messages.select(normalized);
        }, currentWindow == null ? null : () -> deferUi(currentWindow::relocalize));
    }

    private void activatePendingLanguageForHudReplacement() {
        language = Language.fromCode(pendingLanguage).getCode();
        pendingLanguage = null;
        Messages.select(language);
    }
    @Override public void disableLegacyAction() {
        try {
            java.nio.file.Path disabled = LEGACY_ACTION.disableForNextLaunch();
            EVENTS.warning(Messages.text("event.legacy_disabled_next_restart", disabled));
            if (window != null) window.refresh();
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.disable_legacy"), e);
            EVENTS.warning(Messages.text("error.disable_legacy_manual"));
        }
    }
    @Override public KeybindRecord getRecord(String id) { return registry == null ? null : registry.find(id); }
    @Override public String getActionName(short actionId) {
        String path = ACTION_PATHS.get(actionId);
        if (path != null) return path;
        String remembered = ACTION_NAMES.get(actionId);
        if (remembered != null) return remembered;
        if (registry != null) {
            for (KeybindRecord record : registry.snapshot()) {
                for (org.keybinder.wurm.model.KeybindVariant variant : record.getVariants()) {
                    for (org.keybinder.wurm.model.KeybindStep step : variant.getSteps()) {
                        if (!(step instanceof ActionStep)) continue;
                        ActionStep action = (ActionStep) step;
                        if (action.getActionId() == actionId
                                && !action.getLastKnownName().isEmpty()) {
                            ACTION_NAMES.put(actionId, action.getLastKnownName());
                            return action.getLastKnownName();
                        }
                    }
                }
            }
        }
        PlayerAction direct = PlayerAction.getByActionId(actionId);
        if (direct != null && direct.getName() != null && !direct.getName().trim().isEmpty())
            return direct.getName();
        try {
            for (PlayerAction action : new org.keybinder.wurm.catalog.PlayerActionCatalog().snapshot())
                if (action.getId() == actionId && action.getName() != null
                        && !action.getName().trim().isEmpty()) return action.getName();
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Unable to resolve action name for " + actionId, e);
        }
        String constantName = actionConstantName(actionId);
        if (constantName != null) {
            ACTION_NAMES.put(actionId, constantName);
            return constantName;
        }
        // Callers own their context-specific fallback. Returning an empty value
        // also prevents a localized "Unknown action" label from being persisted
        // as if it were authoritative action metadata.
        return "";
    }

    private static String actionConstantName(short actionId) {
        for (Field field : PlayerAction.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != PlayerAction.class) continue;
            try {
                PlayerAction action = (PlayerAction) field.get(null);
                if (action != null && action.getId() == actionId) return humanize(field.getName());
            } catch (IllegalAccessException ignored) {
                // Continue defensively if a client build exposes an inaccessible field.
            }
        }
        return null;
    }

    private static String humanize(String constant) {
        String lower = constant.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return lower.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
    @Override public boolean saveVariants(String id, String name, String key,
                                          List<KeybindVariant> variants, String activeVariantId,
                                          boolean hudMulti,
                                          String createdByUser, String createdOnServer) {
        return editorWorkflow != null && editorWorkflow.saveVariants(id, name, key,
                variants, activeVariantId, hudMulti, createdByUser, createdOnServer);
    }
    @Override public boolean extractVariant(String id, String name, String key,
                                            List<KeybindVariant> variants,
                                            String activeVariantId, boolean hudMulti,
                                            String extractedVariantId,
                                            String createdByUser, String createdOnServer) {
        return editorWorkflow != null && editorWorkflow.extractVariant(id, name, key,
                variants, activeVariantId, hudMulti, extractedVariantId,
                createdByUser, createdOnServer);
    }
    @Override public String currentUser() {
        refreshCreationContext();
        return registry == null ? "" : registry.getCurrentUser();
    }
    @Override public String currentServer() {
        refreshCreationContext();
        return registry == null ? "" : registry.getCurrentServer();
    }

    @Override public void resolveKeybindConflict(ConflictResolution resolution) {
        if (editorWorkflow != null) editorWorkflow.resolve(resolution);
    }
    @Override public void showEditorError(String message) { EVENTS.warning(message); }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
    @Override public void beginCapture() {
        deferUi(() -> {
            try {
                ACTION_CAPTURE.cancel();
                SELECTION.cancel();
                resetExactPress();
                cancelSlotSelection();
                hideSafely(captureWindow);
                captureWindow = null;
                ACTION_CAPTURE.arm();
                captureWindow = new KeybinderCaptureWindow(INSTANCE, editorWindow);
                new HudIntegration(ACCESS).add(hud, captureWindow);
            } catch (Exception e) {
                ACTION_CAPTURE.cancel();
                hideSafely(captureWindow);
                captureWindow = null;
                EVENTS.error(Messages.text("error.capture_start"), e);
            }
        });
    }
    @Override public void cancelCapture() {
        ACTION_CAPTURE.cancel();
        final KeybinderCaptureWindow captureToClose = captureWindow;
        captureWindow = null;
        deferUi(() -> hideSafely(captureToClose));
    }
    @Override public ActionStep pollCapturedAction() {
        return ACTION_CAPTURE.poll();
    }
    @Override public void requestTargetSelection(String kind) {
        ACTION_CAPTURE.cancel();
        final KeybinderCaptureWindow captureToClose = captureWindow;
        captureWindow = null;
        deferUi(() -> hideSafely(captureToClose));
        if ("toolbelt".equals(kind)) {
            SELECTION.requestToolbelt();
            deferUi(() -> {
                try {
                    cancelSlotSelection();
                    toolbeltOpenedForSelection = !ACCESS.isToolbeltVisible(hud);
                    ACCESS.ensureToolbeltVisible(hud);
                    selectionWindow = new KeybinderSelectionWindow(
                            INSTANCE, Messages.text("selection.toolbelt"));
                    new HudIntegration(ACCESS).add(hud, selectionWindow);
                }
                catch (Exception e) {
                    SELECTION.cancel();
                    EVENTS.error(Messages.text("error.show_toolbelt"), e);
                }
            });
        } else if ("equipment".equals(kind)) {
            SELECTION.requestEquipment();
            deferUi(() -> {
                try {
                    cancelSlotSelection();
                    equipmentOpenedForSelection = !ACCESS.isPaperDollVisible(hud);
                    ACCESS.ensurePaperDollVisible(hud);
                    selectionWindow = new KeybinderSelectionWindow(
                            INSTANCE, Messages.text("selection.equipment"));
                    new HudIntegration(ACCESS).add(hud, selectionWindow);
                }
                catch (Exception e) {
                    SELECTION.cancel();
                    EVENTS.error(Messages.text("error.show_paperdoll"), e);
                }
            });
        } else if ("tiles".equals(kind)) {
            try {
                deferUi(() -> {
                    try {
                        tileWindow = new KeybinderTileWindow(INSTANCE);
                        new HudIntegration(ACCESS).add(hud, tileWindow);
                    } catch (Exception e) {
                        SELECTION.cancel();
                        EVENTS.error(Messages.text("error.tile_open"), e);
                    }
                });
            } catch (RuntimeException e) {
                SELECTION.cancel();
                EVENTS.error(Messages.text("error.tile_select"), e);
            }
        } else if ("exact object".equals(kind)) {
            ACTION_CAPTURE.cancel();
            resetExactPress();
            SELECTION.requestExactObject();
            deferUi(() -> {
                try {
                    cancelSlotSelection();
                    selectionWindow = new KeybinderSelectionWindow(
                            INSTANCE, Messages.text("selection.object"));
                    new HudIntegration(ACCESS).add(hud, selectionWindow);
                } catch (Exception e) {
                    SELECTION.cancel();
                    EVENTS.error(Messages.text("error.exact_start"), e);
                }
            });
        } else if ("nearby by type".equals(kind)) {
            ACTION_CAPTURE.cancel();
            resetExactPress();
            SELECTION.requestNearbyType();
            deferUi(() -> {
                try {
                    cancelSlotSelection();
                    selectionWindow = new KeybinderSelectionWindow(
                            INSTANCE, Messages.text("selection.nearby_type"));
                    new HudIntegration(ACCESS).add(hud, selectionWindow);
                } catch (Exception e) {
                    SELECTION.cancel();
                    EVENTS.error(Messages.text("error.nearby_start"), e);
                }
            });
        } else if ("hover by type".equals(kind)) {
            ACTION_CAPTURE.cancel();
            resetExactPress();
            SELECTION.requestHoverType();
            deferUi(() -> {
                try {
                    cancelSlotSelection();
                    selectionWindow = new KeybinderSelectionWindow(
                            INSTANCE, Messages.text("selection.hover_type"));
                    new HudIntegration(ACCESS).add(hud, selectionWindow);
                } catch (Exception e) {
                    SELECTION.cancel();
                    EVENTS.error(Messages.text("error.hover_type_start"), e);
                }
            });
        } else if ("inventory+filter".equals(kind)) {
            ACTION_CAPTURE.cancel();
            resetExactPress();
            SELECTION.requestInventoryFilter();
            deferUi(() -> {
                try {
                    cancelSlotSelection();
                    inventoryOpenedForSelection = !ACCESS.isInventoryVisible(hud);
                    ACCESS.ensureInventoryVisible(hud);
                    selectionWindow = new KeybinderSelectionWindow(
                            INSTANCE, Messages.text("selection.inventory_filter"));
                    new HudIntegration(ACCESS).add(hud, selectionWindow);
                } catch (Exception e) {
                    inventoryOpenedForSelection = false;
                    SELECTION.cancel();
                    EVENTS.error(Messages.text("error.inventory_filter_start"), e);
                }
            });
        } else if ("bulk source".equals(kind)) {
            ACTION_CAPTURE.cancel();
            resetExactPress();
            SELECTION.requestBulkSource();
            deferUi(() -> {
                try {
                    cancelSlotSelection();
                    selectionWindow = new KeybinderSelectionWindow(
                            INSTANCE, Messages.text("selection.bulk_source"));
                    new HudIntegration(ACCESS).add(hud, selectionWindow);
                } catch (Exception e) {
                    SELECTION.cancel();
                    EVENTS.error(Messages.text("error.bulk_source_start"), e);
                }
            });
        } else if ("bulk destination".equals(kind)) {
            ACTION_CAPTURE.cancel();
            resetExactPress();
            SELECTION.requestBulkDestination();
            deferUi(() -> {
                try {
                    cancelSlotSelection();
                    selectionWindow = new KeybinderSelectionWindow(
                            INSTANCE, Messages.text("selection.bulk_destination"));
                    new HudIntegration(ACCESS).add(hud, selectionWindow);
                } catch (Exception e) {
                    SELECTION.cancel();
                    EVENTS.error(Messages.text("error.bulk_destination_start"), e);
                }
            });
        } else {
            SELECTION.selectTile(kind);
        }
    }
    @Override public void cancelTargetSelection() {
        SELECTION.cancel();
        resetExactPress();
        deferUi(KeybinderMod::cancelSlotSelection);
    }
    @Override public String consumeSelectedTarget() { return SELECTION.consumeSelectedTarget(); }
    @Override public BulkStorageItem consumeSelectedBulkSource() {
        return SELECTION.consumeSelectedBulkSource();
    }
    @Override public InventoryReference consumeSelectedBulkDestination() {
        return SELECTION.consumeSelectedBulkDestination();
    }
    @Override public void closeEditor() {
        ACTION_CAPTURE.cancel();
        SELECTION.cancel();
        resetExactPress();
        final KeybinderCaptureWindow captureToClose = captureWindow;
        final KeybinderConflictWindow conflictToClose = conflictWindow;
        final KeybinderTileWindow tileToClose = tileWindow;
        deferUi(() -> {
            cancelSlotSelection();
            hideSafely(captureToClose);
            hideSafely(conflictToClose);
            hideSafely(tileToClose);
            try {
                if (hud != null && window != null) {
                    window.showKeybinds();
                    ACCESS.ensureComponentVisible(hud, window);
                    if (pendingLanguage != null) applyLanguage(pendingLanguage);
                }
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.window_restore"), e);
            }
        });
        editorWindow = null;
        captureWindow = null;
        conflictWindow = null;
        if (editorWorkflow != null) editorWorkflow.clear();
        tileWindow = null;
    }

    @Override public void openOriginalProject() {
        try {
            if (!Desktop.isDesktopSupported())
                throw new IllegalStateException(Messages.text("error.desktop_unavailable"));
            Desktop.getDesktop().browse(new URI(ORIGINAL_PROJECT));
        } catch (Throwable e) {
            EVENTS.warning(Messages.text("error.open_url"));
            EVENTS.warning(Messages.text("event.url_manual", ORIGINAL_PROJECT));
            LOGGER.log(Level.WARNING, "Unable to open project URL", e);
        }
    }
    @Override public void openImproveProject() {
        openProject(IMPROVE_PROJECT, "i2improve");
    }
    @Override public void openInniriaImproveProject() {
        openProject(INNIRIA_IMPROVE_PROJECT, "inniria i2improve");
    }
    @Override public void openMunstaImproveProject() {
        openProject(MUNSTA_IMPROVE_PROJECT, "Munsta0 Improved Improve");
    }
    private static void openProject(String url, String name) {
        try {
            if (!Desktop.isDesktopSupported())
                throw new IllegalStateException(Messages.text("error.desktop_unavailable"));
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable e) {
            EVENTS.warning(Messages.text("error.open_url"));
            EVENTS.warning(Messages.text("event.url_manual", url));
            LOGGER.log(Level.WARNING, "Unable to open " + name + " project URL", e);
        }
    }
    @Override public void windowClosed() {
        ACTION_CAPTURE.cancel();
        SELECTION.cancel();
        resetExactPress();
        final KeybinderCaptureWindow captureToClose = captureWindow;
        final KeybinderConflictWindow conflictToClose = conflictWindow;
        final KeybinderTileWindow tileToClose = tileWindow;
        final KeybinderSelectionWindow selectionToClose = selectionWindow;
        editorWindow = null;
        captureWindow = null;
        conflictWindow = null;
        tileWindow = null;
        selectionWindow = null;
        if (editorWorkflow != null) editorWorkflow.clear();
        deferUi(() -> {
            try {
                if (hud != null) {
                    hideSafely(captureToClose);
                    hideSafely(conflictToClose);
                    hideSafely(tileToClose);
                    hideSafely(selectionToClose);
                    showTagInsteadOfWindow();
                }
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.replace_with_tag"), e);
            }
        });
    }

    @Override public void openFromTag() {
        deferUi(() -> {
            try {
                if (hud != null && window != null && tagWindow != null) {
                    ACCESS.setComponentVisible(hud, tagWindow, false);
                    window.showKeybinds();
                    ACCESS.setComponentVisible(hud, window, true);
                }
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.open_from_tag"), e);
            }
        });
    }

    private static void showTagInsteadOfWindow() throws ReflectiveOperationException {
        if (hud == null) return;
        if (window != null) ACCESS.setComponentVisible(hud, window, false);
        if (tagWindow != null) ACCESS.setComponentVisible(hud, tagWindow, true);
    }
}
