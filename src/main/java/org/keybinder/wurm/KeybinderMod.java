package org.keybinder.wurm;

import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.KeybinderCaptureWindow;
import com.wurmonline.client.renderer.gui.KeybinderConflictWindow;
import com.wurmonline.client.renderer.gui.KeybinderEditorWindow;
import com.wurmonline.client.renderer.gui.KeybinderLegacyWindow;
import com.wurmonline.client.renderer.gui.KeybinderMultiSelectorWindow;
import com.wurmonline.client.renderer.gui.KeybinderSelectionWindow;
import com.wurmonline.client.renderer.gui.KeybinderSelectionBridge;
import com.wurmonline.client.renderer.gui.KeybinderTileWindow;
import com.wurmonline.client.renderer.gui.KeybinderWindow;
import com.wurmonline.client.renderer.gui.KeybinderTagWindow;
import com.wurmonline.client.renderer.gui.SelectBar;
import com.wurmonline.shared.constants.PlayerAction;
import javassist.ClassPool;
import javassist.CtClass;
import org.keybinder.wurm.bind.VanillaBindService;
import org.keybinder.wurm.bind.LongPressController;
import org.keybinder.wurm.bind.WheelInputHandler;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.command.ActionExecutor;
import org.keybinder.wurm.command.KeybindExecutionService;
import org.keybinder.wurm.command.ImproveRequirementTracker;
import org.keybinder.wurm.command.PushSelectionRetention;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.integration.HudIntegration;
import org.keybinder.wurm.i18n.Language;
import org.keybinder.wurm.i18n.LanguageChangePolicy;
import org.keybinder.wurm.i18n.LocalizationSettings;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.ExistingWindowRelocalizer;
import org.keybinder.wurm.migration.CustomActionsMigrationService;
import org.keybinder.wurm.migration.ImprovedImproveMigrationService;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindConflict;
import org.keybinder.wurm.model.ConflictResolution;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.queue.ActionQueueOccupancyTracker;
import org.keybinder.wurm.queue.QueueCapacityException;
import org.keybinder.wurm.queue.QueueCost;
import org.keybinder.wurm.queue.QueueLimitService;
import org.keybinder.wurm.recording.ShadowRecorder;
import org.keybinder.wurm.recording.SelectionController;
import org.keybinder.wurm.storage.KeybindStore;
import org.keybinder.wurm.storage.AccountKeybindStateStore;
import org.keybinder.wurm.storage.ModPropertiesStore;
import org.keybinder.wurm.ui.KeybinderUiController;
import org.keybinder.wurm.ui.KeybindEditorController;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import java.awt.Desktop;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KeybinderMod implements WurmClientMod, Initable, PreInitable, Configurable,
        KeybinderUiController, KeybindEditorController {
    public static final String VERSION = "0.5.4";
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
    private static final ShadowRecorder RECORDER = new ShadowRecorder();
    private static final EventLogger EVENTS = new EventLogger(LOGGER);
    private static final SelectionController SELECTION = new SelectionController(EVENTS);
    private static final PushSelectionRetention PUSH_SELECTION =
            new PushSelectionRetention();
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
    private static final org.keybinder.wurm.integration.ServerNameResolver SERVER_NAMES =
            new org.keybinder.wurm.integration.ServerNameResolver();
    private static final ModPropertiesStore MOD_PROPERTIES = new ModPropertiesStore();
    private static ActionExecutor EXECUTOR;
    private static final ImproveRequirementTracker IMPROVE_REQUIREMENTS = new ImproveRequirementTracker();
    private static KeybindExecutionService KEYBIND_EXECUTOR;
    private static final Queue<Runnable> UI_AFTER_TICK = new ConcurrentLinkedQueue<>();
    private static final int MAX_UI_OPERATIONS_PER_TICK = 64;
    private static final Map<Short, String> ACTION_NAMES = new ConcurrentHashMap<>();
    private static final Map<Short, String> ACTION_PATHS = new ConcurrentHashMap<>();
    private static final Map<Object, String> POPUP_PATHS =
            Collections.synchronizedMap(new java.util.WeakHashMap<Object, String>());

    private static volatile HeadsUpDisplay hud;
    private static volatile KeybinderWindow window;
    private static volatile KeybinderTagWindow tagWindow;
    private static volatile KeybinderEditorWindow editorWindow;
    private static volatile KeybinderCaptureWindow captureWindow;
    private static volatile KeybinderConflictWindow conflictWindow;
    private static volatile KeybinderTileWindow tileWindow;
    private static volatile KeybinderSelectionWindow selectionWindow;
    private static volatile KeybinderLegacyWindow legacyWindow;
    private static volatile KeybinderMultiSelectorWindow multiSelectorWindow;
    private static final LongPressController LONG_PRESS = new LongPressController();
    private static final long LONG_PRESS_NANOS = 1_000_000_000L;
    private static volatile boolean showActionIds;
    private static volatile boolean toolbeltOpenedForSelection;
    private static volatile boolean equipmentOpenedForSelection;
    private static volatile boolean exactPressArmed;
    private static volatile boolean exactPressDragged;
    private static volatile int exactPressX;
    private static volatile int exactPressY;
    private static volatile long exactPressTime;
    private static volatile long lastSharedSyncPoll;
    private static volatile String selectedFullServerName = "";
    private static volatile String observedServerCluster = "";
    private static KeybindRegistry registry;
    private static PendingSave pendingSave;
    private static String pendingEnableId;
    private static KeybindConflict pendingConflict;
    private Properties properties = new Properties();
    private volatile boolean skipIntro;
    private volatile String language = Language.ENGLISH.getCode();
    private volatile String pendingLanguage;

    @Override
    public String getVersion() { return VERSION; }

    @Override
    public void configure(Properties properties) {
        this.properties = properties == null ? new Properties() : properties;
        skipIntro = Boolean.parseBoolean(this.properties.getProperty("skipIntroPage", "false"));
        language = LocalizationSettings.load(this.properties);
        Messages.select(language);
    }

    @Override
    public void preInit() {
        ClassPool pool = HookManager.getInstance().getClassPool();
        installCapability("action names", () -> hookActionNames(pool));
        installCapability("action menu paths", () -> hookActionMenuPaths(pool));
        installCapability("console commands", () -> hookConsole(pool));
        installCapability("multi-purpose long press", () -> hookLongPress(pool));
        installCapability("mouse wheel keybinds", () -> hookMouseWheel(pool));
        installCapability("Smart Improve messages", () -> hookImproveMessages(pool));
        installCapability("HUD lifecycle", () -> hookHud(pool));
        installCapability("action queue occupancy", () -> hookActionQueue(pool));
        installCapability("shadow recording", () -> hookRecording(pool));
        installCapability("target selection", () -> hookSelections(pool));
        installCapability("push selection retention", () -> hookPushSelection(pool));
        installCapability("server identity", () -> hookSelectedServer(pool));
    }

    private void installCapability(String name, HookInstallation installation) {
        try {
            installation.install();
            LOGGER.fine("Installed Keybinder " + name + " integration");
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Keybinder " + name
                    + " integration is unavailable; other features will continue", e);
        }
    }

    private interface HookInstallation {
        void install() throws Exception;
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
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize Keybinder", e);
        }
    }

    private void hookActionNames(ClassPool pool) throws Exception {
        CtClass type = pool.getCtClass("com.wurmonline.shared.constants.PlayerAction");
        type.getMethod("getName", "()Ljava/lang/String;").insertAfter(
                "{ org.keybinder.wurm.KeybinderMod.rememberActionName(this.id, $_);"
                        + " if ($_ != null && org.keybinder.wurm.KeybinderMod.isShowingIds())"
                        + " $_ = $_ + \" (\" + this.id + \")\"; }");
    }

    private void hookActionMenuPaths(ClassPool pool) throws Exception {
        CtClass abstractButton = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.WurmPopup$WPopupAbstractButton");
        abstractButton.getDeclaredMethod("mouseMoved").insertBefore(
                "org.keybinder.wurm.KeybinderMod.rememberPopupSubmenu("
                        + "this.this$0, this.getSubmenu(), this.getLabel());");
        CtClass button = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.WurmPopup$WPopupActionButton");
        button.getDeclaredMethod("handleLeftClick").insertBefore(
                "org.keybinder.wurm.KeybinderMod.rememberActionMenuPath("
                        + "this.this$0, this.action);");
    }

    private void hookImproveMessages(ClassPool pool) throws Exception {
        CtClass chat = pool.getCtClass("com.wurmonline.client.renderer.gui.ChatPanelComponent");
        chat.getMethod("addText", "(Ljava/lang/String;Ljava/lang/String;FFFZ)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeImproveMessage($1,$2);");
    }

    private void hookConsole(ClassPool pool) throws Exception {
        CtClass console = pool.getCtClass("com.wurmonline.client.console.WurmConsole");
        console.getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z").insertBefore(
                "if (org.keybinder.wurm.KeybinderMod.handleCommand($1,$2)) return true;");
    }

    private void hookLongPress(ClassPool pool) throws Exception {
        CtClass console = pool.getCtClass("com.wurmonline.client.console.WurmConsole");
        console.getMethod("toggleKey", "(IZ)V").insertBefore(
                "if (org.keybinder.wurm.KeybinderMod.handleKeyToggle(this,$1,$2)) return;");
    }

    private void hookMouseWheel(ClassPool pool) throws Exception {
        CtClass handler = pool.getCtClass("com.wurmonline.client.WurmEventHandler");
        handler.getMethod("mouseWheeled", "(III)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.handleMouseWheel($1,$2,$3);");
    }

    public static void handleMouseWheel(final int x, final int y, final int delta) {
        try {
            final HeadsUpDisplay currentHud = hud;
            WheelInputHandler.handle(new WheelInputHandler.Environment() {
                @Override public boolean isOverHudComponent(int px, int py) {
                    return currentHud == null || currentHud.getComponentAt(px, py) != null;
                }
                @Override public boolean isControlDown() { return currentHud.isControlDown(); }
                @Override public boolean isShiftDown() { return currentHud.isShiftDown(); }
                @Override public boolean isAltDown() { return currentHud.isAltDown(); }
            }, new WheelInputHandler.Dispatcher() {
                @Override public boolean executeExact(String chord) throws Exception {
                    KeybindRecord record = registry == null
                            ? null : registry.findEnabledByChord(chord);
                    if (record == null) return false;
                    executeManaged(record, currentHud);
                    return true;
                }
            }, x, y, delta);
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Mouse wheel keybind hook failed open", e);
            EVENTS.error(Messages.text("error.mouse_wheel"), e);
        }
    }

    private void hookHud(ClassPool pool) {
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V",
                () -> (proxy, method, args) -> {
                    Object result = method.invoke(proxy, args);
                    onHudReady((HeadsUpDisplay) proxy);
                    return result;
                });
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "gameTick", "()V",
                () -> (proxy, method, args) -> {
                    Object result = method.invoke(proxy, args);
                    drainUiQueue();
                    return result;
                });
    }

    public static void deferUi(Runnable operation) {
        if (operation != null) UI_AFTER_TICK.offer(operation);
    }

    private static void drainUiQueue() {
        Runnable operation;
        int processed = 0;
        while (processed++ < MAX_UI_OPERATIONS_PER_TICK
                && (operation = UI_AFTER_TICK.poll()) != null) {
            try {
                operation.run();
            } catch (Throwable e) {
                EVENTS.error(Messages.text("error.deferred_hud"), e);
            }
        }
        pollSharedDefinitions();
        pollLongPress();
    }

    public static boolean handleKeyToggle(WurmConsole console, int key, boolean pressed) {
        try {
            if (!pressed) {
                LongPressController.Release release = LONG_PRESS.release(key);
                if (release != null) {
                    KeybindRecord held = registry == null
                            ? null : registry.find(release.getRecordId());
                    if (release.isTap() && held != null && held.isEnabled()) executeManaged(held);
                    return true;
                }
            }
            com.wurmonline.client.console.KeyBinding binding = console.getCurrentBinding(key);
            if (binding == null || binding.getAction() != null) return false;
            String command = binding.getStrCommand();
            if (command == null || !command.toLowerCase(Locale.ENGLISH).startsWith("keybinder_run "))
                return false;
            String id = command.substring("keybinder_run ".length()).trim();
            KeybindRecord record = registry == null ? null : registry.find(id);
            if (record == null || !record.isEnabled() || !record.isMultiPurpose()) return false;
            if (pressed) {
                LONG_PRESS.press(id, key, System.nanoTime());
                return true;
            }
            return true;
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Long-press key hook failed open", e);
            clearLongPress();
            return false;
        }
    }

    private static void pollLongPress() {
        String id = LONG_PRESS.triggerIfElapsed(System.nanoTime(), LONG_PRESS_NANOS);
        if (id == null) return;
        KeybindRecord record = registry == null ? null : registry.find(id);
        if (record == null || !record.isMultiPurpose()) {
            clearLongPress();
            return;
        }
        deferUi(() -> {
            try {
                hideSafely(multiSelectorWindow);
                multiSelectorWindow = new KeybinderMultiSelectorWindow(record);
                new HudIntegration(ACCESS).add(hud, multiSelectorWindow);
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.multi_selector"), e);
                clearLongPress();
            }
        });
    }

    private static void executeManaged(KeybindRecord record) throws ReflectiveOperationException {
        executeManaged(record, hud);
    }

    private static void executeManaged(KeybindRecord record, HeadsUpDisplay currentHud)
            throws ReflectiveOperationException {
        final int queueLimit = LIMITS.readLimit(currentHud);
        try {
            KEYBIND_EXECUTOR.execute(record, currentHud, queueLimit,
                    () -> ACTION_QUEUE.occupied(hudShowsAction(currentHud)));
        } catch (QueueCapacityException capacity) {
            EVENTS.warning(capacity.getMessage());
            return;
        }
        EVENTS.execution(Messages.text("event.executed", record.getDisplayName(),
                org.keybinder.wurm.catalog.InputKeyCatalog.displayChord(record.getKey())));
    }

    private static boolean hudShowsAction(HeadsUpDisplay currentHud) {
        if (currentHud == null) return false;
        String action = currentHud.getActionString();
        return action != null && !action.trim().isEmpty();
    }

    private static void clearLongPress() {
        LONG_PRESS.clear();
    }

    public static void chooseMultiVariant(String recordId, String variantId) {
        KeybindRecord selected;
        try {
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
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.change_active_action"), e);
            closeMultiSelector();
            return;
        }
        try {
            executeManaged(selected);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.execute_selected_action"), e);
        } finally {
            closeMultiSelector();
        }
    }

    public static void closeMultiSelector() {
        KeybinderMultiSelectorWindow selector = multiSelectorWindow;
        multiSelectorWindow = null;
        deferUi(() -> {
            hideSafely(selector);
            if (INSTANCE.pendingLanguage != null)
                INSTANCE.applyLanguage(INSTANCE.pendingLanguage);
        });
    }

    private static void pollSharedDefinitions() {
        long now = System.currentTimeMillis();
        if (now - lastSharedSyncPoll < 1000L || registry == null || hud == null) return;
        lastSharedSyncPoll = now;
        try {
            if (registry.syncExternal(ACCESS.console(hud)) && window != null) window.refresh();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to poll shared keybind definitions", e);
        }
    }

    private void hookSelectedServer(ClassPool pool) {
        try {
            CtClass browser = pool.getCtClass("com.wurmonline.client.startup.ServerBrowserFX");
            String signature = "(Ljavafx/scene/control/TableView;)V";
            browser.getMethod("ConnectTo", signature).insertBefore(
                    "org.keybinder.wurm.KeybinderMod.rememberSelectedServer($1);");
            browser.getMethod("ConnectWithPassword",
                    "(Ljavafx/scene/control/TableView;Ljava/lang/String;Ljava/lang/String;)V")
                    .insertBefore("org.keybinder.wurm.KeybinderMod.rememberSelectedServer($1);");
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Full server-name capture is unavailable", e);
        }
    }

    public static void rememberSelectedServer(Object tableView) {
        try {
            Object selection = tableView.getClass().getMethod("getSelectionModel").invoke(tableView);
            Object selected = selection.getClass().getMethod("getSelectedItem").invoke(selection);
            if (selected == null) return;
            Object name = selected.getClass().getMethod("getServerName").invoke(selected);
            if (name != null && !name.toString().trim().isEmpty())
                selectedFullServerName = name.toString().trim();
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.FINE, "Unable to remember selected Steam server", e);
        }
    }

    private void hookRecording(ClassPool pool) {
        HookManager.getInstance().registerHook("com.wurmonline.client.comm.SimpleServerConnectionClass",
                "sendAction", "(J[JLcom/wurmonline/shared/constants/PlayerAction;)V",
                () -> (proxy, method, args) -> {
                    RECORDER.observe((PlayerAction) args[2]);
                    return method.invoke(proxy, args);
                });
        HookManager.getInstance().registerHook("com.wurmonline.client.game.World",
                "sendHoveredAction", "(Lcom/wurmonline/shared/constants/PlayerAction;)V",
                () -> (proxy, method, args) -> withTarget("hover", proxy, method, args));
        HookManager.getInstance().registerHook("com.wurmonline.client.game.World",
                "sendLocalAction", "(Lcom/wurmonline/shared/constants/PlayerAction;)V",
                () -> (proxy, method, args) -> withTarget("tile", proxy, method, args));
    }

    private void hookActionQueue(ClassPool pool) throws Exception {
        CtClass connection =
                pool.getCtClass("com.wurmonline.client.comm.SimpleServerConnectionClass");
        connection.getMethod("sendAction",
                "(J[JLcom/wurmonline/shared/constants/PlayerAction;)V").insertAfter(
                "org.keybinder.wurm.KeybinderMod.observeActionSent($2, $3);");
        connection.getMethod("sendSingleAction",
                "(JJLcom/wurmonline/shared/constants/PlayerAction;)V").insertAfter(
                "org.keybinder.wurm.KeybinderMod.observeSingleActionSent();");
        CtClass hudClass =
                pool.getCtClass("com.wurmonline.client.renderer.gui.HeadsUpDisplay");
        hudClass.getMethod("setAction", "(Ljava/lang/String;F)V").insertAfter(
                "org.keybinder.wurm.KeybinderMod.observeActionState($1, $2);");
    }

    public static void observeActionSent(long[] targets, PlayerAction action) {
        if (targets == null || targets.length == 0) return;
        int count = action != null && action.isAtomic()
                ? 1 : Math.min(10, targets.length);
        ACTION_QUEUE.actionsSent(count);
    }

    public static void observeSingleActionSent() {
        ACTION_QUEUE.actionsSent(1);
    }

    public static void observeActionState(String actionText, float durationSeconds) {
        ACTION_QUEUE.actionState(actionText, durationSeconds);
    }

    private void hookSelections(ClassPool pool) throws Exception {
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.ToolBeltComponent",
                "leftPressed", "(III)V", () -> (proxy, method, args) -> {
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
                    } else if (RECORDER.isRecording()) {
                        java.lang.reflect.Method slotMethod = proxy.getClass().getDeclaredMethod(
                                "getSlotNumberAt", int.class, int.class, boolean.class);
                        slotMethod.setAccessible(true);
                        int slot = (Integer) slotMethod.invoke(proxy, args[0], args[1], true);
                        RECORDER.observeToolbeltSlot(slot);
                    }
                    return method.invoke(proxy, args);
                });
        HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.PaperDollInventory",
                "leftPressed", "(III)V", () -> (proxy, method, args) -> {
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
                });
        /*
         * These global mouse methods must not use HookManager reflection wrappers.
         * If the original client method throws, Method.invoke wraps the real cause
         * in InvocationTargetException and HookManager reports it as a Keybinder
         * HookException. Direct bytecode callbacks preserve the original call and
         * keep Keybinder's observation fail-open.
         */
        CtClass eventHandler = pool.getCtClass("com.wurmonline.client.WurmEventHandler");
        eventHandler.getMethod("mousePressed", "(IIII)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeMousePressed($1, $2, $3);");
        eventHandler.getMethod("mouseDragged", "(II)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeMouseDragged();");
        eventHandler.getMethod("mouseReleased", "(III)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeMouseReleased(this, $1, $2, $3);");

        /*
         * Wurm can retain a TargetWindow briefly across reconnect while its
         * renderer is null. A right click in that interval otherwise crashes in
         * TargetWindow.rightPressed. Ignore only that unusable stale window.
         */
        CtClass targetWindow =
                pool.getCtClass("com.wurmonline.client.renderer.gui.TargetWindow");
        targetWindow.getDeclaredMethod("rightPressed").insertBefore(
                "{ if (this.renderer == null) {"
                        + " org.keybinder.wurm.KeybinderMod.targetWindowNotReady();"
                        + " return; } }");
        HookManager.getInstance().registerHook(
                "com.wurmonline.client.renderer.gui.WurmTreeList$TreeListPanel",
                "leftPressed", "(III)V", () -> (proxy, method, args) -> {
                    if (SELECTION.getMode() == SelectionController.Mode.EXACT_OBJECT
                            || SELECTION.getMode() == SelectionController.Mode.NEARBY_TYPE)
                        captureInventoryTarget(proxy, (Integer) args[0], (Integer) args[1]);
                    return method.invoke(proxy, args);
                });
    }

    private void hookPushSelection(ClassPool pool) throws Exception {
        CtClass selectBar =
                pool.getCtClass("com.wurmonline.client.renderer.gui.SelectBar");
        selectBar.getMethod("setNewSelectedIfKeepId",
                "(Lcom/wurmonline/client/renderer/PickableUnit;)V").insertAfter(
                "org.keybinder.wurm.KeybinderMod.afterPushTargetRecreated(this, $1);");
    }

    public static void observeMousePressed(int mouseX, int mouseY, int button) {
        try {
            if ((SELECTION.getMode() == SelectionController.Mode.EXACT_OBJECT
                    || SELECTION.getMode() == SelectionController.Mode.NEARBY_TYPE)
                    && button == 0) {
                exactPressArmed = true;
                exactPressDragged = false;
                exactPressX = mouseX;
                exactPressY = mouseY;
                exactPressTime = System.currentTimeMillis();
            }
        } catch (Throwable e) {
            resetExactPress();
            LOGGER.log(Level.WARNING, "Unable to observe mouse press for target selection", e);
        }
    }

    public static void observeMouseDragged() {
        try {
            if (exactPressArmed) exactPressDragged = true;
        } catch (Throwable e) {
            resetExactPress();
            LOGGER.log(Level.WARNING, "Unable to observe mouse drag for target selection", e);
        }
    }

    public static void observeMouseReleased(Object eventHandler, int mouseX, int mouseY, int button) {
        try {
            if ((SELECTION.getMode() == SelectionController.Mode.EXACT_OBJECT
                    || SELECTION.getMode() == SelectionController.Mode.NEARBY_TYPE)
                    && button == 0
                    && isExactObjectClick(eventHandler, mouseX, mouseY))
                captureWorldTarget(eventHandler);
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Unable to observe mouse release for target selection", e);
        } finally {
            if (button == 0) resetExactPress();
        }
    }

    public static void targetWindowNotReady() {
        LOGGER.fine("Ignored input for a TargetWindow without a renderer during reconnect");
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
        if (!exactPressArmed || exactPressDragged) return false;
        if (Math.abs(releaseX - exactPressX) > 3 || Math.abs(releaseY - exactPressY) > 3) return false;
        if (System.currentTimeMillis() - exactPressTime > 1000L) return false;
        try {
            Field mouseLooking = eventHandler.getClass().getDeclaredField("isMouseLooking");
            Field draggedLooking = eventHandler.getClass().getDeclaredField("hasBeenDraggedInMouseLooking");
            mouseLooking.setAccessible(true);
            draggedLooking.setAccessible(true);
            return !(Boolean) mouseLooking.get(eventHandler) && !(Boolean) draggedLooking.get(eventHandler);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Unable to verify exact-object click gesture", e);
            return false;
        }
    }

    private static void resetExactPress() {
        exactPressArmed = false;
        exactPressDragged = false;
        exactPressTime = 0L;
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
                    : SELECTION.acceptExactObject(picked.getId(), picked.getHoverName());
            if (accepted)
                deferUi(KeybinderMod::finishExactObjectSelection);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Unable to capture world target", e);
        }
    }

    private static void captureInventoryTarget(Object panel, int mouseX, int mouseY) {
        try {
            java.lang.reflect.Method getNodeAt = panel.getClass().getDeclaredMethod(
                    "getNodeAt", int.class, int.class);
            getNodeAt.setAccessible(true);
            Object node = getNodeAt.invoke(panel, mouseX, mouseY);
            if (node == null) return;
            Field nodeItem = node.getClass().getDeclaredField("item");
            nodeItem.setAccessible(true);
            Object treeItem = nodeItem.get(node);
            if (treeItem == null) return;
            Field inventoryItem = findField(treeItem.getClass(), "item");
            if (inventoryItem == null) return;
            inventoryItem.setAccessible(true);
            Object value = inventoryItem.get(treeItem);
            if (!(value instanceof InventoryMetaItem)) return;
            InventoryMetaItem item = (InventoryMetaItem) value;
            boolean accepted = SELECTION.getMode() == SelectionController.Mode.NEARBY_TYPE
                    ? SELECTION.acceptNearbyType(item.getBaseName())
                    : SELECTION.acceptExactObject(item.getId(), item.getDisplayName());
            if (accepted)
                deferUi(KeybinderMod::finishExactObjectSelection);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Unable to capture inventory target", e);
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

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Continue with the parent class.
            }
        }
        return null;
    }

    private static Object withTarget(String target, Object proxy, java.lang.reflect.Method method, Object[] args)
            throws Throwable {
        RECORDER.setTargetContext(target);
        try { return method.invoke(proxy, args); }
        finally { RECORDER.clearTargetContext(); }
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
    }

    private static void cancelSlotSelection() {
        hideSafely(selectionWindow);
        selectionWindow = null;
        try {
            if (toolbeltOpenedForSelection) hideSafely(ACCESS.toolbeltComponent(hud));
            if (equipmentOpenedForSelection) hideSafely(ACCESS.paperDollComponent(hud));
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.restore_selection_windows"), e);
        } finally {
            toolbeltOpenedForSelection = false;
            equipmentOpenedForSelection = false;
        }
    }

    public static boolean isShowingIds() { return showActionIds; }

    public static void rememberActionName(short actionId, String name) {
        if (name == null) return;
        String clean = name.trim();
        if (!clean.isEmpty()) ACTION_NAMES.put(actionId, clean);
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
        ACTION_PATHS.put(action.getId(), cleanParent + " -> " + leaf);
    }

    private static String stripActionNumber(String value) {
        return value.replaceFirst("\\s+\\(-?\\d+\\)$", "");
    }

    public static void observeImproveMessage(String context, String message) {
        try {
            IMPROVE_REQUIREMENTS.observe(hud, context, message);
            rememberServerCluster(context);
            rememberServerCluster(message);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Unable to observe improve requirement", e);
        }
    }

    private static void rememberServerCluster(String text) {
        if (text == null) return;
        String marker = "Welcome back to ";
        int start = text.indexOf(marker);
        if (start < 0) return;
        start += marker.length();
        int end = text.indexOf(" - ", start);
        if (end <= start) return;
        String cluster = text.substring(start, end).trim();
        if (cluster.isEmpty() || cluster.equalsIgnoreCase(observedServerCluster)) return;
        observedServerCluster = cluster;
        refreshCreationContext();
        if (window != null) window.refresh();
    }

    public static void onHudReady(HeadsUpDisplay newHud) {
        try {
            ensureRuntimeServices();
            if (hud != null && hud != newHud) disposeHudSession(hud);
            if (INSTANCE.pendingLanguage != null)
                INSTANCE.activatePendingLanguageForHudReplacement();
            ACCESS.setup();
            hud = newHud;
            refreshCreationContext();
            EVENTS.attach(newHud);
            RECORDER.cancel();
            IMPROVE_REQUIREMENTS.clear();
            ACTION_QUEUE.clear();
            PUSH_SELECTION.clear();
            resetExactPress();
            clearLongPress();
            closeMultiSelector();
            window = new KeybinderWindow(INSTANCE);
            tagWindow = new KeybinderTagWindow(INSTANCE);
            HudIntegration hudIntegration = new HudIntegration(ACCESS);
            hudIntegration.register(newHud, window);
            hudIntegration.registerTag(newHud, tagWindow);
            if (LEGACY_ACTION.isInstalled())
                EVENTS.warning(Messages.text("event.legacy_installed"));
            if (LEGACY_IMPROVE.isInstalled())
                EVENTS.warning(Messages.text("event.improve_installed"));
            if (INSTANCE.skipIntro) {
                window.showKeybinds();
                ACCESS.ensureComponentVisible(newHud, window);
            }
            else {
                window.showIntro();
                ACCESS.ensureComponentVisible(newHud, window);
            }
            ACCESS.setComponentVisible(newHud, tagWindow, false);
            if (registry != null) {
                WurmConsole console = ACCESS.console(newHud);
                registry.enforceLimit(LIMITS.readLimit(newHud), console);
                registry.restoreAccountBindings(
                        registry.getCurrentUser(), console, LIMITS.readLimit(newHud));
                List<org.keybinder.wurm.bind.BindSnapshot> candidates = registry.importCandidates(console);
                if (!candidates.isEmpty())
                    EVENTS.info(Messages.text("event.import_candidates_found", candidates.size()));
            }
            EVENTS.info(Messages.text("event.ready", VERSION, LIMITS.readLimit(newHud)));
        } catch (Throwable e) {
            EVENTS.error(Messages.text("error.attach_hud",
                    e.getClass().getSimpleName(), safeMessage(e)), e);
        }
    }

    private static void disposeHudSession(HeadsUpDisplay oldHud) {
        RECORDER.cancel();
        SELECTION.cancel();
        IMPROVE_REQUIREMENTS.clear();
        ACTION_QUEUE.clear();
        PUSH_SELECTION.clear();
        resetExactPress();
        clearLongPress();
        UI_AFTER_TICK.clear();
        hideOnHud(oldHud, captureWindow);
        hideOnHud(oldHud, conflictWindow);
        hideOnHud(oldHud, tileWindow);
        hideOnHud(oldHud, selectionWindow);
        hideOnHud(oldHud, multiSelectorWindow);
        hideOnHud(oldHud, editorWindow);
        captureWindow = null;
        conflictWindow = null;
        tileWindow = null;
        selectionWindow = null;
        multiSelectorWindow = null;
        editorWindow = null;
        window = null;
        tagWindow = null;
        pendingSave = null;
        pendingEnableId = null;
        pendingConflict = null;
        toolbeltOpenedForSelection = false;
        equipmentOpenedForSelection = false;
    }

    private static void hideOnHud(HeadsUpDisplay targetHud,
                                  com.wurmonline.client.renderer.gui.WurmComponent component) {
        if (targetHud == null || component == null || ACCESS == null) return;
        try {
            ACCESS.hideComponent(targetHud, component);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Unable to dispose old HUD component", e);
        }
    }

    private static synchronized void ensureRuntimeServices() {
        if (ACCESS != null) return;
        ACCESS = new ClientAccess();
        EXECUTOR = new ActionExecutor(ACCESS, INSTANCE::getActionName, PUSH_SELECTION);
        KEYBIND_EXECUTOR = new KeybindExecutionService(
                EXECUTOR, ACCESS, EVENTS, IMPROVE_REQUIREMENTS, KeybinderMod::deferUi);
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
                shortServer = hud.getWorld().getServerName();
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

    private static String resolveFullServerName(String worldServerName) {
        String shortName = worldServerName == null ? "" : worldServerName.trim();
        if (shortName.isEmpty()) return "";
        if (matchesShard(selectedFullServerName, shortName)) return selectedFullServerName;
        if (!observedServerCluster.isEmpty())
            return observedServerCluster + " - " + shortName;
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

    private static KeybinderMod INSTANCE;
    public KeybinderMod() { INSTANCE = this; }

    public static boolean handleCommand(String command, String[] data) {
        try {
            if ("keybinder_run".equalsIgnoreCase(command)) {
                ensureHud();
                ensureRegistry();
                if (data.length != 2)
                    throw new IllegalArgumentException(Messages.text("command.usage.run"));
                KeybindRecord record = registry.find(data[1]);
                if (record == null)
                    throw new IllegalArgumentException(
                            Messages.text("event.record_missing", data[1]));
                if (!record.isEnabled()) {
                    EVENTS.warning(Messages.text("event.record_disabled", record.getName(),
                            DisableReason.display(record.getDisabledReason())));
                    return true;
                }
                executeManaged(record);
                return true;
            }
            if ("keybinder_list".equalsIgnoreCase(command)) {
                ensureRegistry();
                registry.printAll(EVENTS, LIMITS.readLimit(hud), data.length > 1 && "commands".equalsIgnoreCase(data[1]));
                return true;
            }
            if ("keybinder_actions".equalsIgnoreCase(command)) {
                String filter = data.length > 1 ? data[1].toLowerCase() : "";
                int shown = 0;
                for (PlayerAction action : new org.keybinder.wurm.catalog.PlayerActionCatalog().snapshot()) {
                    String line = action.getName() + " (" + action.getId() + ")";
                    if (filter.isEmpty() || line.toLowerCase().contains(filter)) {
                        EVENTS.info(line);
                        if (++shown >= 100) {
                            EVENTS.warning(Messages.text("event.action_list_truncated"));
                            break;
                        }
                    }
                }
                return true;
            }
            if ("keybinder_add_selected".equalsIgnoreCase(command)) {
                ensureRegistry();
                if (data.length != 4) throw new IllegalArgumentException(
                        Messages.text("command.usage.add_selected"));
                int parsed = Integer.parseInt(data[3]);
                if (parsed < Short.MIN_VALUE || parsed > Short.MAX_VALUE)
                    throw new IllegalArgumentException(
                            Messages.text("validation.action_id_range") + ": " + parsed);
                KeybindRecord record = KeybindRecord.actionChain(
                        data[2].replace('_', ' '), data[1],
                        Collections.singletonList(new ActionStep((short) parsed,
                                TargetCodec.decode(SELECTION.getSelectedTarget()))));
                registry.add(record, ACCESS.console(hud), LIMITS.readLimit(hud));
                if (window != null) window.refresh();
                return true;
            }
            if ("keybinder_commit".equalsIgnoreCase(command)) {
                ensureRegistry();
                if (data.length != 3) throw new IllegalArgumentException(
                        Messages.text("command.usage.commit"));
                List<KeybindStep> captured = RECORDER.snapshot();
                if (captured.isEmpty())
                    throw new IllegalStateException(Messages.text("validation.recording_empty"));
                for (KeybindStep step : captured)
                    if (step instanceof ActionStep
                            && ((ActionStep) step).getTarget().getKind() == TargetKind.UNRESOLVED)
                        throw new IllegalStateException(Messages.text("validation.recorded_target",
                                ((ActionStep) step).getActionId()));
                registry.add(new KeybindRecord(null, data[2].replace('_', ' '), data[1], captured),
                        ACCESS.console(hud), LIMITS.readLimit(hud));
                if (window != null) window.refresh();
                return true;
            }
            if ("keybinder_import_confirm".equalsIgnoreCase(command)) {
                ensureRegistry();
                if (data.length != 2 || !"CONFIRM".equals(data[1]))
                    throw new IllegalArgumentException(
                            Messages.text("command.usage.import_confirm"));
                int imported = registry.importAllReviewed(ACCESS.console(hud), LIMITS.readLimit(hud));
                EVENTS.info(Messages.text("event.import_complete", imported));
                if (window != null) window.refresh();
                return true;
            }
            if ("keybinder_delete".equalsIgnoreCase(command)) {
                ensureRegistry();
                if (data.length != 2)
                    throw new IllegalArgumentException(Messages.text("command.usage.delete"));
                if (!registry.delete(data[1], ACCESS.console(hud)))
                    EVENTS.warning(Messages.text("event.record_missing", data[1]));
                if (window != null) window.refresh();
                return true;
            }
        } catch (Throwable e) {
            EVENTS.error(e.getMessage() == null
                    ? Messages.text("error.command_failed") : e.getMessage(), e);
            return true;
        }
        return false;
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
    @Override public QueueCost getQueueCost(List<ActionStep> steps) { return COSTS.chainCost(steps); }
    @Override public QueueCost getKeybindCost(KeybindRecord record) { return COSTS.keybindCost(record); }
    @Override public boolean isShowingActionIds() { return showActionIds; }
    @Override public void toggleActionIds() {
        showActionIds = !showActionIds;
        EVENTS.info(Messages.text(showActionIds
                ? "event.action_ids_enabled" : "event.action_ids_disabled"));
    }
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
            if (window != null) window.refresh();
        } catch (Exception e) { EVENTS.error(Messages.text("error.delete_keybind"), e); }
    }
    @Override public void setKeybindEnabled(String id, boolean enabled) {
        try {
            if (enabled) {
                KeybindConflict conflict = registry.findEnableConflict(id, ACCESS.console(hud));
                if (conflict != null) {
                    pendingEnableId = id;
                    pendingConflict = conflict;
                    deferUi(() -> {
                        hideSafely(conflictWindow);
                        conflictWindow = new KeybinderConflictWindow(INSTANCE, conflict);
                        try { new HudIntegration(ACCESS).add(hud, conflictWindow); }
                        catch (ReflectiveOperationException e) {
                            pendingEnableId = null;
                            pendingConflict = null;
                            EVENTS.error(Messages.text("error.conflict_window"), e);
                        }
                        if (window != null) window.refresh();
                    });
                    return;
                }
            }
            registry.setEnabled(id, enabled, ACCESS.console(hud), LIMITS.readLimit(hud));
            if (window != null) window.refresh();
        } catch (Exception e) {
            EVENTS.error(Messages.text(enabled
                    ? "error.enable_keybind" : "error.disable_keybind"), e);
            if (window != null) window.refresh();
        }
    }
    @Override public void setKeybindsEnabled(List<String> ids, boolean enabled) {
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
    @Override public void printAll() { if (registry != null) registry.printAll(EVENTS, getQueueLimit(), false); }
    @Override public void toggleShadowRecording() {
        if (RECORDER.isRecording()) {
            List<KeybindStep> result = RECORDER.stop();
            EVENTS.info(Messages.text("event.recording_stopped", result.size()));
            for (KeybindStep step : result)
                EVENTS.info(Messages.text("event.recorded", describeRecorded(step)));
            EVENTS.info(Messages.text("event.recording_save"));
        } else {
            RECORDER.start();
            EVENTS.info(Messages.text("event.recording_started"));
        }
    }
    @Override public boolean isShadowRecording() { return RECORDER.isRecording(); }
    @Override public void requestImport() {
        try {
            refreshCreationContext();
            List<org.keybinder.wurm.bind.BindSnapshot> candidates =
                    registry.importCandidates(ACCESS.console(hud));
            EVENTS.info(Messages.text("event.import_review", candidates.size()));
            for (org.keybinder.wurm.bind.BindSnapshot bind : candidates)
                EVENTS.info(Messages.text("event.import_candidate",
                        bind.getKey(), bind.getCommand()));
            EVENTS.warning(Messages.text("event.import_no_changes"));
        } catch (Exception e) { EVENTS.error(Messages.text("error.import_inspect"), e); }
    }
    @Override public void confirmImport() {
        try {
            refreshCreationContext();
            int imported = registry.importAllReviewed(ACCESS.console(hud), LIMITS.readLimit(hud));
            EVENTS.info(Messages.text("event.import_complete", imported));
            if (window != null) window.refresh();
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.import_reviewed"), e);
        }
    }
    @Override public boolean isLegacyActionInstalled() { return LEGACY_ACTION.isInstalled(); }
    @Override public void startFromIntro() {
        deferUi(() -> {
            try {
                if (hud != null && window != null) {
                    window.showKeybinds();
                    ACCESS.ensureComponentVisible(hud, window);
                    if (pendingLanguage != null) applyLanguage(pendingLanguage);
                }
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.open_keybinder"), e);
            }
        });
    }
    @Override public void importDisableAndRestart() {
        try {
            ensureRegistry();
            int imported = registry.importAllReviewed(ACCESS.console(hud), LIMITS.readLimit(hud));
            java.nio.file.Path disabled = LEGACY_ACTION.disableForNextLaunch();
            EVENTS.info(Messages.text("event.imported", imported));
            EVENTS.warning(Messages.text("event.legacy_disabled", disabled));
            MOD_PROPERTIES.save(properties);
            deferUi(() -> {
                if (hud != null) hud.showQuitConfirmationWindow();
            });
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.migration_stopped"), e);
        }
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
    @Override public void requestToolbeltSelection() {
        RECORDER.cancel();
        requestTargetSelection("toolbelt");
    }
    @Override public void requestEquipmentSelection() {
        RECORDER.cancel();
        requestTargetSelection("equipment");
    }
    @Override public void selectTileTarget(String target) { SELECTION.selectTile(target); }
    @Override public String getSelectedTarget() { return SELECTION.getSelectedTarget(); }
    @Override public KeybindRecord getRecord(String id) { return registry == null ? null : registry.find(id); }
    @Override public String getActionName(short actionId) {
        String path = ACTION_PATHS.get(actionId);
        if (path != null) return path;
        String remembered = ACTION_NAMES.get(actionId);
        if (remembered != null) return remembered;
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
        return Messages.text("editor.unknown_action", actionId);
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
    @Override public boolean saveRecord(String id, String name, String key, List<ActionStep> steps) {
        try {
            if (name == null || name.trim().isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.name_missing"));
            if (key == null || key.trim().isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.key_missing"));
            if (steps == null || steps.isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.action_missing"));
            PendingSave requested = new PendingSave(id, name, key, steps);
            KeybindConflict conflict = registry.findSaveConflict(
                    id, key, requested.steps, ACCESS.console(hud));
            if (conflict != null) {
                pendingSave = requested;
                pendingConflict = conflict;
                deferUi(() -> {
                    hideSafely(conflictWindow);
                    conflictWindow = new KeybinderConflictWindow(INSTANCE, conflict);
                    try {
                        new HudIntegration(ACCESS).add(hud, conflictWindow);
                    } catch (ReflectiveOperationException e) {
                        pendingSave = null;
                        EVENTS.error(Messages.text("error.conflict_window"), e);
                    }
                });
                return false;
            }
            return commitSave(requested);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.save_keybind", safeMessage(e)), e);
            return false;
        }
    }

    @Override public boolean saveKeybind(String id, String name, String key, List<KeybindStep> steps,
                                         String createdByUser, String createdOnServer) {
        try {
            if (name == null || name.trim().isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.name_missing"));
            if (key == null || key.trim().isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.key_missing"));
            if (steps == null || steps.isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.step_missing"));
            PendingSave requested = new PendingSave(id, name, key, steps,
                    createdByUser, createdOnServer);
            KeybindConflict conflict = registry.findKeybindSaveConflict(id, key, ACCESS.console(hud));
            if (conflict != null) {
                pendingSave = requested;
                pendingConflict = conflict;
                deferUi(() -> {
                    hideSafely(conflictWindow);
                    conflictWindow = new KeybinderConflictWindow(INSTANCE, conflict);
                    try { new HudIntegration(ACCESS).add(hud, conflictWindow); }
                    catch (ReflectiveOperationException e) {
                        pendingSave = null;
                        EVENTS.error(Messages.text("error.conflict_window"), e);
                    }
                });
                return false;
            }
            return commitSave(requested);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.save_keybind", safeMessage(e)), e);
            return false;
        }
    }
    @Override public boolean saveVariants(String id, String name, String key,
                                          List<KeybindVariant> variants, String activeVariantId,
                                          String createdByUser, String createdOnServer) {
        try {
            if (name == null || name.trim().isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.name_missing"));
            if (key == null || key.trim().isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.key_missing"));
            if (variants == null || variants.isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.variant_missing"));
            for (KeybindVariant variant : variants)
                if (variant.getSteps().isEmpty())
                    throw new IllegalArgumentException(
                            Messages.text("validation.variant_step_missing"));
            PendingSave requested = new PendingSave(id, name, key, variants, activeVariantId,
                    createdByUser, createdOnServer);
            KeybindConflict conflict = registry.findKeybindSaveConflict(id, key, ACCESS.console(hud));
            if (conflict != null) {
                pendingSave = requested;
                pendingConflict = conflict;
                deferUi(() -> {
                    hideSafely(conflictWindow);
                    conflictWindow = new KeybinderConflictWindow(INSTANCE, conflict);
                    try { new HudIntegration(ACCESS).add(hud, conflictWindow); }
                    catch (ReflectiveOperationException e) {
                        pendingSave = null;
                        pendingConflict = null;
                        EVENTS.error(Messages.text("error.conflict_window"), e);
                    }
                });
                return false;
            }
            return commitSave(requested);
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.save_keybind", safeMessage(e)), e);
            return false;
        }
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
        PendingSave requested = pendingSave;
        String enableId = pendingEnableId;
        KeybindConflict conflict = pendingConflict;
        pendingSave = null;
        pendingEnableId = null;
        pendingConflict = null;
        KeybinderConflictWindow dialog = conflictWindow;
        conflictWindow = null;
        deferUi(() -> hideSafely(dialog));
        if (enableId != null) {
            if (resolution == ConflictResolution.KEEP_NEW) {
                try {
                    registry.setEnabled(enableId, true, ACCESS.console(hud), LIMITS.readLimit(hud));
                    if (window != null) window.refresh();
                } catch (Exception e) {
                    EVENTS.error(Messages.text("error.enable_keybind"), e);
                }
            } else {
                EVENTS.info(Messages.text(resolution == ConflictResolution.CANCEL
                        ? "event.enable_cancelled" : "event.existing_kept"));
                if (window != null) window.refresh();
            }
            return;
        }
        if (resolution == ConflictResolution.CANCEL || requested == null) {
            EVENTS.info(Messages.text("event.save_cancelled"));
            return;
        }
        if (resolution == ConflictResolution.KEEP_OLD) {
            try {
                if (requested.kind == SaveKind.ACTIONS)
                    throw new IllegalStateException(
                            Messages.text("error.disabled_save_container"));
                String reason = conflict == null
                        ? DisableReason.value("key_used_unknown", requested.key)
                        : conflict.isVanillaOwner()
                        ? DisableReason.value("key_used_vanilla", requested.key)
                        : DisableReason.value("key_used", requested.key, conflict.getOwner());
                if (requested.kind == SaveKind.VARIANTS)
                    registry.updateVariantsDisabled(requested.id, requested.name, requested.key,
                            requested.variants, requested.activeVariantId, reason,
                            requested.createdByUser, requested.createdOnServer,
                            ACCESS.console(hud), LIMITS.readLimit(hud));
                else
                    registry.updateKeybindDisabled(requested.id, requested.name, requested.key,
                            requested.keybindSteps, reason, requested.createdByUser,
                            requested.createdOnServer, ACCESS.console(hud), LIMITS.readLimit(hud));
                INSTANCE.closeEditor();
                if (window != null) window.refresh();
            } catch (Exception e) {
                EVENTS.error(Messages.text("error.save_disabled", safeMessage(e)), e);
            }
            return;
        }
        commitSave(requested);
    }

    private static boolean commitSave(PendingSave requested) {
        try {
            if (requested.kind == SaveKind.VARIANTS)
                registry.updateVariants(requested.id, requested.name, requested.key,
                        requested.variants, requested.activeVariantId,
                        requested.createdByUser, requested.createdOnServer,
                        ACCESS.console(hud), LIMITS.readLimit(hud));
            else if (requested.kind == SaveKind.STEPS)
                registry.updateKeybind(requested.id, requested.name, requested.key,
                        requested.keybindSteps, requested.createdByUser, requested.createdOnServer,
                        ACCESS.console(hud), LIMITS.readLimit(hud));
            else
                registry.update(requested.id, requested.name, requested.key, requested.steps,
                        ACCESS.console(hud), LIMITS.readLimit(hud));
            INSTANCE.closeEditor();
            if (window != null) window.refresh();
            return true;
        } catch (Exception e) {
            EVENTS.error(Messages.text("error.save_keybind", safeMessage(e)), e);
            return false;
        }
    }
    @Override public void showEditorError(String message) { EVENTS.warning(message); }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
    @Override public void beginCapture() {
        deferUi(() -> {
            try {
                RECORDER.start();
                captureWindow = new KeybinderCaptureWindow(INSTANCE, editorWindow);
                new HudIntegration(ACCESS).add(hud, captureWindow);
            } catch (Exception e) {
                RECORDER.cancel();
                EVENTS.error(Messages.text("error.capture_start"), e);
            }
        });
    }
    @Override public void cancelCapture() { RECORDER.cancel(); }
    @Override public ActionStep pollCapturedAction() {
        List<KeybindStep> captured = RECORDER.snapshot();
        if (captured.isEmpty()) return null;
        RECORDER.stop();
        for (KeybindStep step : captured)
            if (step instanceof ActionStep) return (ActionStep) step;
        return null;
    }

    private static String describeRecorded(KeybindStep step) {
        if (step instanceof ActionStep) {
            ActionStep action = (ActionStep) step;
            return action.getActionId() + " " + TargetCodec.encode(action.getTarget());
        }
        if (step instanceof org.keybinder.wurm.model.ActivateToolStep)
            return Messages.text("event.describe_activate",
                    TargetCodec.display(
                            ((org.keybinder.wurm.model.ActivateToolStep) step).getTarget()));
        return Messages.text("event.describe_unknown");
    }
    @Override public void requestTargetSelection(String kind) {
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
            RECORDER.cancel();
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
            RECORDER.cancel();
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
    @Override public void closeEditor() {
        RECORDER.cancel();
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
        pendingSave = null;
        pendingEnableId = null;
        pendingConflict = null;
        tileWindow = null;
    }

    private static final class PendingSave {
        private final String id;
        private final String name;
        private final String key;
        private final List<ActionStep> steps;
        private final List<KeybindStep> keybindSteps;
        private final List<KeybindVariant> variants;
        private final String activeVariantId;
        private final SaveKind kind;
        private final String createdByUser;
        private final String createdOnServer;

        private PendingSave(String id, String name, String key, List<ActionStep> steps) {
            this.id = id;
            this.name = name;
            this.key = key;
            this.steps = Collections.unmodifiableList(new java.util.ArrayList<>(steps));
            this.keybindSteps = Collections.<KeybindStep>unmodifiableList(
                    new java.util.ArrayList<KeybindStep>(steps));
            this.variants = Collections.emptyList();
            this.activeVariantId = "";
            this.kind = SaveKind.ACTIONS;
            this.createdByUser = "";
            this.createdOnServer = "";
        }

        private PendingSave(String id, String name, String key, List<KeybindStep> steps,
                            String createdByUser, String createdOnServer) {
            this.id = id;
            this.name = name;
            this.key = key;
            this.steps = Collections.emptyList();
            this.keybindSteps = Collections.unmodifiableList(new java.util.ArrayList<KeybindStep>(steps));
            this.variants = Collections.emptyList();
            this.activeVariantId = "";
            this.kind = SaveKind.STEPS;
            this.createdByUser = createdByUser == null ? "" : createdByUser;
            this.createdOnServer = createdOnServer == null ? "" : createdOnServer;
        }

        private PendingSave(String id, String name, String key, List<KeybindVariant> variants,
                            String activeVariantId, String createdByUser, String createdOnServer) {
            this.id = id;
            this.name = name;
            this.key = key;
            this.steps = Collections.emptyList();
            this.keybindSteps = Collections.emptyList();
            this.variants = Collections.unmodifiableList(
                    new java.util.ArrayList<KeybindVariant>(variants));
            this.activeVariantId = activeVariantId;
            this.kind = SaveKind.VARIANTS;
            this.createdByUser = createdByUser == null ? "" : createdByUser;
            this.createdOnServer = createdOnServer == null ? "" : createdOnServer;
        }
    }

    private enum SaveKind {
        ACTIONS,
        STEPS,
        VARIANTS
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
        RECORDER.cancel();
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
        pendingSave = null;
        pendingEnableId = null;
        pendingConflict = null;
        deferUi(() -> {
            try {
                if (hud != null) {
                    hideSafely(captureToClose);
                    hideSafely(conflictToClose);
                    hideSafely(tileToClose);
                    hideSafely(selectionToClose);
                    ACCESS.setComponentVisible(hud, window, false);
                    ACCESS.setComponentVisible(hud, tagWindow, true);
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
}
