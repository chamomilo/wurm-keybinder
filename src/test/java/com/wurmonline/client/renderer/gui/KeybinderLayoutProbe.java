package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.i18n.Language;
import org.keybinder.wurm.KeybinderMod;
import org.chamomilo.wurm.update.SharedLanguageCoordinator;
import org.chamomilo.wurm.update.SharedUpdateCoordinator;
import org.chamomilo.wurm.update.SharedUpdateHooks;
import org.chamomilo.wurm.update.ModUpdate;
import org.chamomilo.wurm.update.UpdatePreferences;
import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.keybinder.wurm.model.*;
import org.keybinder.wurm.queue.QueueCost;
import org.keybinder.wurm.ui.*;
import org.keybinder.wurm.integration.TransferFileBrowser;
import java.awt.*;
import java.awt.image.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Headless raster preview of actual production painters; no game installation is touched. */
public final class KeybinderLayoutProbe {
    private static final BufferedImage surface = new BufferedImage(2600, 1800, BufferedImage.TYPE_INT_ARGB);
    private static final Graphics2D graphics = surface.createGraphics();
    private static final Deque<Shape> clips = new ArrayDeque<>();
    private static final Map<UiAsset, BufferedImage> assets = new EnumMap<>(UiAsset.class);
    private static final Map<String, BufferedImage> tints = new HashMap<>();
    private static Path output;
    private static int screenshots;
    private static final List<String> localeAudit = new ArrayList<>();
    public static UpdatePreferences sharedPreferences;
    public static Path settingsPath() { return output.resolve("language-state/keybinder.properties"); }
    public static int nativeFontPixels = 12;

    public static void main(String[] args) throws Exception {
        output = Paths.get(args[0]); Files.createDirectories(output);
        sharedPreferences = new UpdatePreferences(output.resolve("language-state/updater.properties"));
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        fixtureHud(); Messages.select("en");
        List<KeybindStep> steps = Arrays.asList(new ConsoleCommandStep("say hello"), new ConsoleCommandStep("toggleconsole"));
        List<KeybindVariant> variants = Arrays.asList(new KeybindVariant("default", "Default", steps), new KeybindVariant("other", "Alternative", steps));
        KeybindRecord record = new KeybindRecord("fixture", "Woodcutting", "SHIFT+SPACE", variants, "default");
        record.setCreatedByUser("Player"); record.setCreatedOnServer("Freedom");
        KeybindRecord second = new KeybindRecord("second", "Inventory", "SPACE", steps);
        KeybinderUiController controller = controller(KeybinderUiController.class, Arrays.asList(record, second));
        KeybindEditorController editorController = controller(KeybindEditorController.class, Arrays.asList(record, second));
        KeybinderWindow window = new KeybinderWindow(controller);
        window.showKeybinds(); window.setSize(Math.max(1100, window.width), 540);
        preview("keybinds", window);
        preview("instructions", new KeybinderInstructionWindow(false));
        preview("constructor-instructions", new KeybinderInstructionWindow(true));
        KeybinderInstructionWindow about = new KeybinderInstructionWindow(false);
        about.buttonClicked((WButton)field(KeybinderInstructionWindow.class, "about").get(about));
        preview("about", about);
        KeybinderEditorWindow editor = new KeybinderEditorWindow(editorController, record.getId());
        window.showEditor(editor); window.setSize(1240, 720); preview("editor", window);
        preview("capture", new KeybinderCaptureWindow(editorController, editor));
        preview("conflict", new KeybinderConflictWindow(editorController, new KeybindConflict("SPACE", "Inventory", "toggleconsole", "Player", "Freedom")));
        preview("import", new KeybinderImportWindow(controller, Collections.emptyList()));
        preview("migration", new KeybinderLegacyWindow(controller));
        preview("merge", new KeybinderMergeWindow(controller, record, second));
        preview("multi", new KeybinderMultiSelectorWindow(record, false, 0, 0));
        preview("selection", new KeybinderSelectionWindow(editorController, "Click the equipment or toolbelt slot to capture its portable selector."));
        preview("tiles", new KeybinderTileWindow(editorController));
        preview("confirmation", new KeybinderUiConfirmWindow("Restore original bindings?", "Restore the selected original bindings after checking ownership.", () -> {}));
        Path files = output.resolve("files"); Files.createDirectories(files.resolve("Archive"));
        Files.write(files.resolve("example.keybinder"), new byte[0]);
        preview("file-import", new KeybinderFileWindow(new TransferFileBrowser(files), false, value -> {}));
        preview("file-export", new KeybinderFileWindow(new TransferFileBrowser(files), true, value -> {}));
        preview("launcher", new KeybinderTagWindow(controller));
        KeybinderDragIndicator.InsertionGap gap = new KeybinderDragIndicator.InsertionGap("keybinder.probe.insertion", 0);
        gap.setSize(450, KeybinderDragIndicator.INSERT_GAP_HEIGHT);
        preview("drag-insertion", gap);
        KeybinderActionQueueMonitor monitor = new KeybinderActionQueueMonitor(controller);
        monitor.gameTick(); preview("queue-collapsed", monitor);
        field(KeybinderActionQueueMonitor.class,"expanded").setBoolean(monitor,true);
        monitor.gameTick();
        field(KeybinderActionQueueMonitor.class,"animationStarted").setLong(monitor,System.currentTimeMillis()-1000);
        monitor.gameTick(); preview("queue-expanded", monitor);
        verifyControls();
        verifyScrollAndRowRegressions();
        for (String language : new String[]{"de", "pt-BR"}) {
            Messages.select(language);
            KeybinderWindow translated = new KeybinderWindow(controller);
            translated.showKeybinds(); translated.setSize(Math.max(1200, translated.width), 540);
            preview("keybinds-" + language, translated);
        }
        verifyRussianWindows(record, second, controller, editorController, files);
        verifySharedLanguage(record, controller, editorController);
        verifyLiveDialogs(record, second, controller, editorController, files);
        Files.write(output.resolve("russian-layout-audit.txt"), localeAudit, java.nio.charset.StandardCharsets.UTF_8);
        preview("cyrillic-font", new KeybinderUiConfirmWindow("Подтверждение", "Проверка шрифта Chamomilo — русский текст", () -> {}));
        Messages.select("en");
        for (int nativeSize : new int[]{10,18,32}) {
            nativeFontPixels = nativeSize;
            KeybinderWindow independent = new KeybinderWindow(controller);
            independent.showKeybinds(); independent.setSize(1200,540);
            preview("native-font-" + nativeSize, independent);
        }
        System.out.println("KEYBINDER_UI_OK: " + screenshots + " production previews; Russian windows/buttons/dropdowns, shared language round-trip, fonts, alpha, grouped buttons, native input, wheel/thumb/drag sync, shrinking lists, long constructor, centered glyphs, row spacing and help");
        graphics.dispose();
    }
    private static void preview(String id, WurmComponent component) throws Exception {
        drainUiOperations();
        component.setPosition(16, 16);
        if (component instanceof KeybinderWindow) component.gameTick();
        drainUiOperations();
        for (WurmComponent node : KeybinderUi.tree(component)) {
            if (node instanceof KeybinderUiButton) ((KeybinderUiButton) node).motion.setAnimationsEnabled(false);
            if (node instanceof ContainerComponent) ((ContainerComponent) node).layout();
        }
        KeybinderUi.theme(component);
        BufferedImage expected = snapshot(component, 1f);
        for (float alpha : new float[]{0f, .2f, .7f})
            check(Arrays.equals(pixels(expected), pixels(snapshot(component, alpha))), id + " fades with HUD alpha " + alpha);
        ImageIO.write(expected, "png", output.resolve(id + ".png").toFile()); screenshots++;
        for (WurmComponent node : KeybinderUi.tree(component)) if (node instanceof KeybinderUiButton)
            check(node.width > 0 && node.height > 0, id + " has invalid button geometry");
        verifyGlyphCenters(component, id);
        if (Messages.language() == Language.RUSSIAN) {
            for (WurmComponent node : KeybinderUi.tree(component)) {
                if (node instanceof KeybinderUiButton && !(node instanceof KeybinderGlyphButton)) {
                    KeybinderUiButton button = (KeybinderUiButton) node;
                    String display = (String)field(KeybinderUiButton.class, "display").get(button);
                    int pixels = field(KeybinderUiButton.class, "pixels").getInt(button);
                    localeAudit.add(id + " | " + button.getLabel() + " | " + button.width + "x" + button.height
                            + " | font=" + pixels + " | displayed=" + display);
                    check(button.getLabel().equals(display), id + " Russian caption is shortened: " + button.getLabel());
                    check(pixels >= 12, id + " Russian caption below readable size");
                }
                if (node instanceof KeybinderUiLabel) {
                    String caption = (String)field(KeybinderUiLabel.class,"caption").get(node);
                    Object source = field(KeybinderUiLabel.class,"captionSource").get(node);
                    if (source != null && node.parent != null) {
                        localeAudit.add(id + " | label=" + caption + " | width=" + node.width);
                        check(node.text.getWidth(caption) <= node.width-8,
                                id + " Russian label is shortened: " + caption);
                    }
                }
            }
        }
    }

    private static void verifyRussianWindows(KeybindRecord record, KeybindRecord second,
            KeybinderUiController controller, KeybindEditorController editorController, Path files) throws Exception {
        Messages.select("ru");
        for (UiDensity density : UiDensity.values()) for (boolean bold : new boolean[]{false,true}) {
            Font font = UiTypography.font(16, bold, density);
            check(font.canDisplayUpTo("АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя") == -1,
                    "Bundled font must contain every Russian glyph: " + density + " bold=" + bold);
        }
        KeybinderWindow window = new KeybinderWindow(controller);
        window.showKeybinds(); window.setSize(Math.max(1100, window.width), 540);
        preview("keybinds-ru", window);
        List<KeybindRecord> defaults = new ArrayList<>();
        try (java.io.InputStream pack = KeybinderLayoutProbe.class.getResourceAsStream(
                org.keybinder.wurm.transfer.ValuePackProvider.RESOURCE)) {
            for (org.keybinder.wurm.transfer.PortableKeybindDefinition definition :
                    new org.keybinder.wurm.transfer.KeybindTransferStore().read(pack)) {
                KeybindRecord sample = definition.toRecord("", "");
                sample.setValuePack(true); sample.setEnabled(false);
                sample.setDisabledReason(org.keybinder.wurm.i18n.DisableReason.value(
                        definition.hasExactObject() ? "nonportable_object_review" : "import_review"));
                check(!KeybindListViewModel.status(sample, defaults, 10).isError(), "Default pack review retains green provenance");
                defaults.add(sample);
            }
        }
        check(defaults.size() == 11, "Eleven current default keybinds");
        KeybinderWindow defaultWindow = new KeybinderWindow(controller(KeybinderUiController.class, defaults));
        defaultWindow.showKeybinds(); defaultWindow.setSize(Math.max(1400, defaultWindow.width), 680);
        preview("default-pack-disabled-ru", defaultWindow);
        for (Object checkbox : ((Map<?,?>)field(KeybinderWindow.class,"checkboxStates").get(defaultWindow)).values())
            check(Boolean.FALSE.equals(checkbox), "Every default keybind starts unchecked");
        KeybindRecord quick = defaults.get(8);
        preview("quick-ru", new KeybinderMultiSelectorWindow(quick, true, 0, 0));
        KeybinderEditorWindow quickEditor = new KeybinderEditorWindow(
                controller(KeybindEditorController.class, Collections.singletonList(quick)), quick.getId());
        check(((KeybinderUiCheckBox)field(KeybinderEditorWindow.class,"hudMulti").get(quickEditor)).checked,
                "Quick editor preserves the saved selector mode");
        defaultWindow.showEditor(quickEditor); defaultWindow.setSize(Math.max(1240, defaultWindow.width), 720);
        preview("quick-editor-ru", defaultWindow);
        preview("instructions-ru", new KeybinderInstructionWindow(false));
        KeybinderInstructionWindow quickInstructions = new KeybinderInstructionWindow(false);
        preview("instructions-quick-ru", quickInstructions);
        WurmComponent instructionContent = (WurmComponent)field(KeybinderInstructionWindow.class, "content").get(quickInstructions);
        for (WurmComponent paragraph : KeybinderUi.tree(instructionContent))
            if (paragraph.getClass().getSimpleName().equals("Paragraph")
                    && Messages.text("help.hud.heading").equals(field(paragraph.getClass(), "caption").get(paragraph))) {
                ((KeybinderUiScrollPanel)field(KeybinderInstructionWindow.class,"scroll").get(quickInstructions))
                        .scrollDownTo(paragraph.y - instructionContent.y);
                break;
            }
        preview("instructions-quick-ru", quickInstructions);
        preview("constructor-instructions-ru", new KeybinderInstructionWindow(true));
        KeybinderInstructionWindow about = new KeybinderInstructionWindow(false);
        click((WButton)field(KeybinderInstructionWindow.class, "about").get(about)); preview("about-ru", about);
        KeybindRecord allTypes = new KeybindRecord("types", "Проверка всех типов шагов", "SPACE", Arrays.asList(
                new ActivateToolStep(TargetSpec.simple(TargetKind.EMPTY_HAND)),
                new SmartImproveStep(TargetSpec.simple(TargetKind.HOVER)),
                new ArcheologyIdentifyStep(TargetSpec.simple(TargetKind.HOVER)),
                new ConsoleCommandStep("toggleconsole"),
                new ActionStep((short)163, TargetSpec.simple(TargetKind.HOVER)),
                new BulkTransferStep(null, 1, BulkDestinationKind.PLAYER_INVENTORY, null)));
        KeybinderEditorWindow editor = new KeybinderEditorWindow(
                controller(KeybindEditorController.class, Collections.singletonList(allTypes)), allTypes.getId());
        window.showEditor(editor); window.setSize(Math.max(1240, window.width), 720);
        preview("editor-ru", window);
        int popupIndex = 0;
        for (WurmComponent node : KeybinderUi.tree(editor)) if (node instanceof KeybinderUiDropDown) {
            KeybinderUiDropDown drop = (KeybinderUiDropDown)node;
            drop.leftPressed(drop.x+5,drop.y+5,1);
            if (!popups().isEmpty()) { preview("editor-options-ru-" + (++popupIndex), popups().get(0)); popups().clear(); }
        }
        preview("capture-ru", new KeybinderCaptureWindow(editorController, editor));
        preview("conflict-ru", new KeybinderConflictWindow(editorController, new KeybindConflict("SPACE", "Инвентарь", "toggleconsole", "Игрок", "Freedom")));
        List<org.keybinder.wurm.bind.VanillaImportCandidate> candidates = new ArrayList<>();
        for (org.keybinder.wurm.bind.VanillaImportCandidate.Type type : org.keybinder.wurm.bind.VanillaImportCandidate.Type.values())
            candidates.add(new org.keybinder.wurm.bind.VanillaImportCandidate(
                    new org.keybinder.wurm.bind.BindSnapshot(1, "F1", "act 163 tool"), type,
                    org.keybinder.wurm.bind.VanillaImportCandidate.Status.READY, "", true));
        preview("import-ru", new KeybinderImportWindow(controller, candidates));
        preview("migration-ru", new KeybinderLegacyWindow(controller));
        preview("merge-ru", new KeybinderMergeWindow(controller, record, second));
        preview("multi-ru", new KeybinderMultiSelectorWindow(record, false, 0, 0));
        for (String kind : new String[]{"toolbelt","equipment","object","nearby_type","hover_type","bulk_source","bulk_destination","inventory_filter"})
            preview("selection-ru-" + kind, new KeybinderSelectionWindow(editorController, Messages.text("selection." + kind)));
        preview("tiles-ru", new KeybinderTileWindow(editorController));
        preview("confirmation-ru", new KeybinderUiConfirmWindow(Messages.text("list.restore_originals.question"), Messages.text("list.restore_originals.confirm"), () -> {}));
        preview("file-import-ru", new KeybinderFileWindow(new TransferFileBrowser(files), false, value -> {}));
        preview("file-export-ru", new KeybinderFileWindow(new TransferFileBrowser(files), true, value -> {}));
        preview("launcher-ru", new KeybinderTagWindow(controller));
        KeybinderActionQueueMonitor monitor = new KeybinderActionQueueMonitor(controller);
        monitor.gameTick(); preview("queue-ru", monitor);
        verifyControls(); verifyScrollAndRowRegressions();
    }

    private static ModEntry<Object> entry(String name, Object mod) {
        return new ModEntry<Object>() {
            public String getName() { return name; }
            public Object getWurmMod() { return mod; }
            public Properties getProperties() { return new Properties(); }
        };
    }
    public static final class OtherMod implements SharedLanguageCoordinator.Participant {
        String language = "en";
        public String[] supportedLanguageCodes() { return new String[]{"en", "pt-BR", "de", "ru"}; }
        public void setUserLanguage(String code) { language = code; }
    }
    private static void verifySharedLanguage(KeybindRecord record, KeybinderUiController controller,
            KeybindEditorController editorController) throws Exception {
        SharedLanguageCoordinator.selectLanguage("en");
        KeybinderMod mod = new KeybinderMod(); mod.configure(new Properties());
        OtherMod other = new OtherMod();
        // Registration arrives through another mod's updater, then through Keybinder's.
        SharedUpdateHooks.registerHost("other-mod");
        SharedUpdateHooks.registerHost("keybinder");
        SharedUpdateCoordinator.modInitialized(entry("other-mod", mod));
        SharedUpdateCoordinator.modInitialized(entry("keybinder", mod));
        SharedUpdateCoordinator.modInitialized(entry("other-participant", other));
        check(Arrays.equals(mod.getSupportedLanguages(), new String[]{"en","pt-BR","de","ru"}), "Four shared languages");
        KeybinderWindow window = new KeybinderWindow(controller);
        field(KeybinderMod.class, "window").set(null, window);
        window.showKeybinds();
        KeybinderUiConfirmWindow hidden = new KeybinderUiConfirmWindow(
                Messages.text("list.restore_originals.question"),
                Messages.text("list.restore_originals.confirm"), () -> {});
        // No component ticks, window reopen or preview/theme pass may be needed.
        checkListCaptions(window);
        Constructor<ModUpdate> constructor = ModUpdate.class.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class, boolean.class, String.class);
        constructor.setAccessible(true);
        ModUpdate row = constructor.newInstance("keybinder", "Keybinder", KeybinderMod.VERSION,
                KeybinderMod.VERSION, "https://github.com/chamomilo/wurm-keybinder/releases/latest", false, "");
        ChamomiloUpdateWindow updater = new ChamomiloUpdateWindow(WurmComponent.hud, Collections.singletonList(row),
                "", "", "", "", "", "", value -> {}, () -> {});
        ChamomiloUiV1DropDown updaterChoice = (ChamomiloUiV1DropDown)field(ChamomiloUpdateWindow.class,"languageChoice").get(updater);
        updaterChoice.selectIndex(Arrays.asList(SharedLanguageCoordinator.languageCodes()).indexOf("ru"));
        drainUiOperations();
        check(mod.getLanguage().equals("ru") && other.language.equals("ru"), "Any updater applies Russian to Keybinder and peers");
        checkListCaptions(window);
        check(((WButton)field(KeybinderUiConfirmWindow.class, "cancel").get(hidden)).getLabel()
                .equals(Messages.text("common.cancel")), "Detached dialog content switches before its first tick");
        check(new UpdatePreferences(output.resolve("language-state/updater.properties")).getForcedLanguage().equals("ru"), "Shared Russian preference persists");
        Properties saved = new Properties();
        try (java.io.InputStream stream = Files.newInputStream(settingsPath())) { saved.load(stream); }
        check(saved.getProperty("language").equals("ru"), "Keybinder Russian preference persists");
        updaterChoice.selectIndex(Arrays.asList(SharedLanguageCoordinator.languageCodes()).indexOf("de"));
        drainUiOperations();
        checkListCaptions(window);
        check(mod.getLanguage().equals("de") && other.language.equals("de") && sharedPreferences.getForcedLanguage().equals("de"), "Updater alone updates shared choice");
        preview("live-language-switch-de", window);
        updater.gameTick();
        check(updater.getLanguageCode().equals("de") && updaterChoice.selectedText().equals("Deutsch"), "Updater language remains synchronized");
        KeybinderEditorWindow editor = new KeybinderEditorWindow(editorController, record.getId());
        window.showEditor(editor);
        check(window.height >= 430, "Opening constructor grows a short list window");
        window.setSize(Math.max(window.width,1240),720);
        List<?> draftRows = new ArrayList<>((List<?>)field(KeybinderEditorWindow.class,"rows").get(editor));
        List<String> commands = new ArrayList<>();
        for (Object rowDraft : draftRows)
            commands.add(((KeybinderUiInputField)field(rowDraft.getClass(),"command").get(rowDraft)).getText());
        KeybinderUiInputField name = (KeybinderUiInputField)field(KeybinderEditorWindow.class,"nameField").get(editor);
        name.setText("Несохранённый черновик");
        updaterChoice.selectIndex(Arrays.asList(SharedLanguageCoordinator.languageCodes()).indexOf("ru"));
        drainUiOperations();
        checkListCaptions(window);
        updater.gameTick();
        preview("updater-shared-ru", updater);
        window.gameTick();
        check(mod.getLanguage().equals("ru") && name.getText().equals("Несохранённый черновик"), "Shared change applies immediately and preserves open draft");
        check(((WButton)field(KeybinderEditorWindow.class,"done").get(editor)).getLabel().equals(Messages.text("common.done")), "Open editor button relocalizes immediately");
        check(draftRows.equals(field(KeybinderEditorWindow.class,"rows").get(editor)), "Locale changes preserve every draft row object");
        for (int i=0;i<draftRows.size();i++)
            check(commands.get(i).equals(((KeybinderUiInputField)field(draftRows.get(i).getClass(),"command").get(draftRows.get(i))).getText()), "Draft command remains exact");
        preview("live-editor-switch-ru", window);
        updaterChoice.selectIndex(Arrays.asList(SharedLanguageCoordinator.languageCodes()).indexOf("ru"));
        SharedLanguageCoordinator.applyPending(); drainUiOperations();
        check(name.getText().equals("Несохранённый черновик"), "Repeated shared choice preserves draft");
        window.showKeybinds();
        check(mod.getLanguage().equals("ru"), "Russian stays active after editor closes");
        preview("live-language-switch-ru", window);
        for (Language language : Language.values()) {
            updaterChoice.selectIndex(Arrays.asList(SharedLanguageCoordinator.languageCodes()).indexOf(language.getCode()));
            drainUiOperations();
            check(mod.getLanguage().equals(language.getCode()) && other.language.equals(language.getCode()), "Shared round-trip " + language);
            checkListCaptions(window);
        }
        field(KeybinderMod.class,"window").set(null,null);
        SharedLanguageCoordinator.selectLanguage("en"); Messages.select("en");
    }

    private static void checkListCaptions(KeybinderWindow window) throws Exception {
        String[] fields = {"instructionButton", "addButton", "importButton", "importFileButton", "exportAllButton", "restoreButton"};
        String[] keys = {"help.read", "list.add", "list.import", "list.import_file", "list.export_all", "list.restore_originals"};
        for (int i = 0; i < fields.length; i++) {
            KeybinderUiButton button = (KeybinderUiButton)field(KeybinderWindow.class, fields[i]).get(window);
            String expected = Messages.text(keys[i]);
            check(button.getLabel().equals(expected), "Existing " + fields[i] + " caption switches to " + Messages.languageCode());
            check(field(KeybinderUiButton.class, "display").get(button).equals(expected),
                    "Painted " + fields[i] + " caption switches without preview refitting");
        }
        for (WurmComponent node : KeybinderUi.tree(window))
            check(!KeybinderUi.id(node).equals("keybinder.header.language"), "Language selector belongs only to Updater");
    }

    private static void drainUiOperations() throws Exception {
        ((org.keybinder.wurm.integration.DeferredUiQueue)field(KeybinderMod.class, "UI_AFTER_TICK").get(null)).drain();
    }

    private static void verifyLiveDialogs(KeybindRecord record, KeybindRecord second,
            KeybinderUiController controller, KeybindEditorController editorController, Path files) throws Exception {
        Messages.select("en");
        KeybinderEditorWindow editor = new KeybinderEditorWindow(editorController,record.getId());
        KeybinderUiInputField name = (KeybinderUiInputField)field(KeybinderEditorWindow.class,"nameField").get(editor);
        name.setText("Cancel — пользовательский черновик");
        KeybinderFileWindow file = new KeybinderFileWindow(new TransferFileBrowser(files),true,value -> {});
        KeybinderUiInputField path = (KeybinderUiInputField)field(KeybinderFileWindow.class,"path").get(file);
        path.setText("мой-черновик.keybinder");
        KeybinderMultiSelectorWindow multi = new KeybinderMultiSelectorWindow(record,false,0,0);
        field(KeybinderMultiSelectorWindow.class,"centered").setBoolean(multi,true);
        field(KeybinderMultiSelectorWindow.class,"warpAttempted").setBoolean(multi,true);
        KeybinderImportWindow imports = new KeybinderImportWindow(controller,Collections.singletonList(
                new org.keybinder.wurm.bind.VanillaImportCandidate(new org.keybinder.wurm.bind.BindSnapshot(1,"F2","act 163 tool"),
                        org.keybinder.wurm.bind.VanillaImportCandidate.Type.ACTION_CHAIN,
                        org.keybinder.wurm.bind.VanillaImportCandidate.Status.READY,"",true)));
        Map<?,?> selected = (Map<?,?>)field(KeybinderImportWindow.class,"selections").get(imports);
        KeybinderUiCheckBox checkBox = (KeybinderUiCheckBox)selected.keySet().iterator().next(); checkBox.checked=false;
        List<KeybinderUiWindow> dialogs = Arrays.asList(editor,file,multi,imports,
                new KeybinderInstructionWindow(false),new KeybinderInstructionWindow(true),
                new KeybinderConflictWindow(editorController,new KeybindConflict("SPACE","Inventory","toggleconsole","Player","Freedom")),
                new KeybinderMergeWindow(controller,record,second),new KeybinderLegacyWindow(controller),
                new KeybinderCaptureWindow(editorController,editor),
                new KeybinderSelectionWindow(editorController,Messages.text("selection.toolbelt")),
                new KeybinderTileWindow(editorController),
                new KeybinderUiConfirmWindow(Messages.text("list.restore_originals.question"),Messages.text("list.restore_originals.confirm"),() -> {}));
        KeybinderUiDropDown openDropDown = (KeybinderUiDropDown)field(KeybinderEditorWindow.class,"keyDropDown").get(editor);
        openDropDown.leftPressed(openDropDown.x+5,openDropDown.y+5,1);
        check(!popups().isEmpty(), "Open menu fixture");
        for (String code : new String[]{"ru","de","pt-BR","en","ru"}) {
            Messages.select(code);
            for (KeybinderUiWindow dialog : dialogs) {
                dialog.gameTick();
                if (dialog==editor) check(popups().isEmpty(), "Language change dismisses only stale own menus");
                check(((WButton)field(KeybinderEditorWindow.class,"done").get(editor)).getLabel().equals(Messages.text("common.done")), "Live editor language " + code);
                check(name.getText().equals("Cancel — пользовательский черновик") && path.getText().equals("мой-черновик.keybinder"), "Inputs survive live dialog language " + code);
                check(!checkBox.checked && selected.keySet().iterator().next()==checkBox, "Import selection survives live language " + code);
                if (code.equals("ru")) preview("live-" + KeybinderUi.id(dialog).replace('.','-') + "-ru",dialog);
            }
        }
        Messages.select("en");
    }
    private static BufferedImage snapshot(WurmComponent component, float alpha) {
        graphics.setClip(null); graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(new Color(22, 21, 19)); graphics.fillRect(0, 0, surface.getWidth(), surface.getHeight());
        graphics.setComposite(AlphaComposite.SrcOver); component.render(null, alpha);
        int w = Math.min(surface.getWidth(), component.x + component.width + 16);
        int h = Math.min(surface.getHeight(), component.y + component.height + 16);
        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(surface.getSubimage(0, 0, w, h), 0, 0, null); return copy;
    }
    private static int[] pixels(BufferedImage image) { return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()); }
    private static void verifyControls() throws Exception {
        int[] calls = {0};
        ButtonListener listener = new ButtonListener() {
            public void buttonPressed(WButton button) {}
            public void buttonClicked(WButton button) { calls[0]++; }
        };
        KeybinderUiButton button = new KeybinderUiButton("Apply", listener); button.setPosition(20,20);
        check(button.height == 32, "Default captioned button is 32 pixels");
        KeybinderGlyphButton glyph = new KeybinderGlyphButton(KeybinderGlyphButton.Kind.PLUS_BOX, listener, "Add");
        check(glyph.width == 20 && glyph.height == 20, "Native padding must not change icon geometry");
        click(button); check(calls[0] == 1, "Completed native click");
        button.leftPressed(25,25,1); button.mouseDragged(0,0); button.leftReleased(0,0);
        check(calls[0] == 1, "Drag must cancel action");
        button.leftPressed(25,25,1); button.setEnabled(false); button.leftReleased(25,25);
        check(calls[0] == 1, "Disabled armed action");
        KeybinderUiCheckBox checkbox = new KeybinderUiCheckBox("Enabled"); checkbox.setPosition(20,20);
        checkbox.leftPressed(25,25,1); checkbox.leftReleased(25,25); check(checkbox.checked, "Checkbox activation");
        checkbox.leftPressed(25,25,1); checkbox.mouseDragged(0,0); checkbox.leftReleased(25,25);
        check(checkbox.checked, "Checkbox drag cancellation");
        KeybinderUiDropDown drop = new KeybinderUiDropDown("keybinder.probe.dropdown", 0, new String[]{"hover", "selected", "toolbelt"});
        drop.setPosition(20,20); drop.leftPressed(25,25,1);
        WurmDropdownPopup popup = popups().get(0);
        preview("dropdown", popup);
        for (WurmComponent child : KeybinderUi.tree(popup)) if (!(child instanceof WButton))
            check(child.text instanceof PaintFont && ((PaintFont)child.text).branded, "Detached popup uses native font");
        int[] changed = {0}, escaped = {0};
        KeybinderUiInputField input = new KeybinderUiInputField("probe.input", new KeybinderUiInputListener() {
            public void handleInput(String value) {}
            public void handleInputChanged(KeybinderUiInputField field, String value) { changed[0]++; }
            public void handleEscape(KeybinderUiInputField field) { escaped[0]++; }
        });
        input.setText("hello");
        for (WurmComponent child : KeybinderUi.tree(input)) if (child instanceof WurmInputField)
            ((WurmInputField) child).keyTyped('!');
        check(input.getText().equals("hello!") && changed[0] > 0, "Native editing child retains input and callback");
        for (WurmComponent child : KeybinderUi.tree(input)) if (child instanceof WurmInputField)
            ((InputFieldListener)field(WurmInputField.class,"inputFieldListener").get(child)).handleEscape((WurmInputField)child);
        check(escaped[0] == 1, "Escape reaches the containing branded dialog");
        int[] confirmed = {0};
        KeybinderUiConfirmWindow confirm = new KeybinderUiConfirmWindow("Confirm", "Review the change.", () -> confirmed[0]++);
        confirm.show(WurmComponent.hud);
        WButton accept = (WButton)field(KeybinderUiConfirmWindow.class,"accept").get(confirm);
        click(accept);click(accept);
        check(confirmed[0] == 1 && !WurmComponent.hud.getComponents().contains(confirm), "Confirmation runs once and removes its window");
        KeybinderUiConfirmWindow cancel = new KeybinderUiConfirmWindow("Confirm", "Review the change.", () -> confirmed[0]++);
        cancel.show(WurmComponent.hud);cancel.closePressed();
        check(confirmed[0] == 1 && !WurmComponent.hud.getComponents().contains(cancel), "Closing confirmation cancels and detaches it");
        KeybinderUiButton shortButton = new KeybinderUiButton("Save", null), longButton = new KeybinderUiButton("Restore bindings", null);
        shortButton.setSize(120,32); longButton.setSize(150,32);
        KeybinderUiButton.fitGroup("probe.footer", Arrays.asList(shortButton,longButton));
        check(field(KeybinderUiButton.class,"pixels").getInt(shortButton) == field(KeybinderUiButton.class,"pixels").getInt(longButton)
                && field(KeybinderUiButton.class,"baseline").getInt(shortButton) == field(KeybinderUiButton.class,"baseline").getInt(longButton), "Peer group common size/baseline");
        longButton.setSize(130,32); longButton.setLabel("Restore original bindings");
        check(field(KeybinderUiButton.class,"pixels").getInt(shortButton) == field(KeybinderUiButton.class,"pixels").getInt(longButton)
                && field(KeybinderUiButton.class,"baseline").getInt(shortButton) == field(KeybinderUiButton.class,"baseline").getInt(longButton), "Caption/geometry changes refit all peers");
    }
    private static void click(WButton button) throws Exception {
        button.leftPressed(button.x+5,button.y+5,1); button.leftReleased(button.x+5,button.y+5);
        drainUiOperations();
    }
    private static void verifyGlyphCenters(WurmComponent component, String id) {
        for (WurmComponent child : KeybinderUi.tree(component))
            if (child instanceof KeybinderGlyphButton && child.parent instanceof KeybinderUiArrayPanel)
                check(Math.abs((child.y * 2 + child.height) - (child.parent.y * 2 + child.parent.height)) <= 1,
                        id + " glyph is not vertically centered in " + KeybinderUi.id(child.parent));
    }

    private static void wheelAndDrag(KeybinderUiScrollPanel scroll, String id) throws Exception {
        scroll.layout(); scroll.gameTick();
        check(scroll.isBarVisible(), id + " scrollbar missing");
        int originalHeight = scroll.content.height;
        scroll.scrollDownTo(0);
        FlexComponent hit = scroll.getComponentAt(scroll.x + 8, scroll.y + 8);
        check(hit != null, id + " content hit test");
        hit.mouseWheeled(scroll.x + 8, scroll.y + 8, 3);
        scroll.gameTick();
        check(scroll.yo > 0 && scroll.yo == scroll.verticalBar().value(), id + " wheel offset/thumb mismatch");
        check(scroll.content.height == originalHeight, id + " wheel caused transient height collapse");
        ChamomiloUiV1ScrollBar bar = scroll.verticalBar();
        UiScrollModel model = (UiScrollModel)field(ChamomiloUiV1ScrollBar.class, "model").get(bar);
        int thumb = model.thumbStart();
        bar.leftPressed(bar.x + 8, bar.y + thumb + 2, 1);
        bar.mouseDragged(bar.x + 8, bar.y + bar.height - model.arrowSize());
        bar.leftReleased(bar.x + 8, bar.y + bar.height - model.arrowSize());
        check(scroll.yo == bar.value() && scroll.yo == bar.maximum(), id + " drag must reach bottom and keep thumb synchronized");
        scroll.content.mouseWheeled(scroll.x + 8, scroll.y + 8, -2);
        check(scroll.yo < bar.maximum() && scroll.yo == bar.value(), id + " wheel after drag");
    }

    private static void verifyScrollAndRowRegressions() throws Exception {
        List<KeybindRecord> records = new ArrayList<>();
        List<KeybindStep> steps = Collections.singletonList(new ConsoleCommandStep("toggleconsole"));
        for (int i = 0; i < 30; i++) records.add(new KeybindRecord("scroll-" + i, "Test " + i, "F" + i, steps));
        KeybinderWindow window = new KeybinderWindow(controller(KeybinderUiController.class, records));
        window.showKeybinds(); window.setSize(1200, 540);
        preview("long-list", window);
        KeybinderUiScrollPanel scroll = (KeybinderUiScrollPanel)field(KeybinderWindow.class, "listScroll").get(window);
        wheelAndDrag(scroll, "registered list");
        verifyGlyphCenters(window, "scrolled list");
        WurmArrayPanel<?> table = (WurmArrayPanel<?>)field(KeybinderWindow.class, "table").get(window);
        WurmComponent previousEdit = null;
        for (Object entry : table.components) {
            if (!(entry instanceof KeybinderUiArrayPanel) || !KeybinderUi.id((WurmComponent)entry).startsWith("keybinder.row.")) continue;
            WurmArrayPanel<?> row = (WurmArrayPanel<?>)entry;
            WurmComponent edit = row.components.get(row.components.size() - 2);
            if (previousEdit != null) check(edit.y - previousEdit.y - previousEdit.height == 2, "Edit/Duplicate vertical gap must be 2px");
            previousEdit = edit;
        }
        records.subList(2, records.size()).clear(); window.refresh(); window.gameTick();
        check(!scroll.isBarVisible() && scroll.yo == 0 && scroll.verticalBar().value() == 0,
                "Shrinking a previously scrolled list must hide the bar and reset the offset");
        check(scroll.content.y == scroll.y, "Shrinking list leaves content above viewport");
        preview("shortened-list", window);
        window.setSize(1200, 800); window.gameTick();
        check(!scroll.isBarVisible(), "Resizing a short list must not restore an unnecessary bar");

        snapshot(window, 1f);
        WButton maximize = KeybinderUi.maximizeControl(window);
        check(window.getComponentAt(maximize.x + 5, maximize.y + 5) == maximize, "Native header controls retain hit testing");

        List<KeybindVariant> variants = Arrays.asList(new KeybindVariant("default", "Default", Collections.nCopies(14, steps.get(0))),
                new KeybindVariant("alternative", "Alternative", Collections.nCopies(14, steps.get(0))));
        KeybindRecord longRecord = new KeybindRecord("long", "Long draft", "SPACE", variants, "default");
        KeybinderEditorWindow editor = new KeybinderEditorWindow(controller(KeybindEditorController.class, Collections.singletonList(longRecord)), longRecord.getId());
        window.showEditor(editor); window.setSize(1240, 720);
        preview("long-constructor", window);
        check(KeybinderUi.maximizeControl(window) == maximize, "Embedded constructor retains the host header controls");
        KeybinderUiScrollPanel editorScroll = null;
        for (WurmComponent child : KeybinderUi.tree(editor)) if (child instanceof KeybinderUiScrollPanel) editorScroll = (KeybinderUiScrollPanel)child;
        check(editorScroll != null, "Constructor scroll panel");
        wheelAndDrag(editorScroll, "long constructor");
        verifyGlyphCenters(editor, "scrolled constructor");
        String draft = ((KeybinderUiInputField)field(KeybinderEditorWindow.class,"nameField").get(editor)).getText();
        click((WButton)field(KeybinderEditorWindow.class,"instructions").get(editor));
        check(((KeybinderUiInputField)field(KeybinderEditorWindow.class,"nameField").get(editor)).getText().equals(draft), "Opening help must preserve the draft");
        KeybinderInstructionWindow.closeActive();
    }
    @SuppressWarnings("unchecked") private static List<WurmDropdownPopup> popups() throws Exception { return (List<WurmDropdownPopup>)field(HeadsUpDisplay.class,"dropdownPopups").get(WurmComponent.hud); }
    @SuppressWarnings("unchecked") private static <T> T controller(Class<T> type, List<KeybindRecord> records) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy,method,args) -> {
            switch (method.getName()) {
                case "getRecords": return records;
                case "getRecord": return records.get(0);
                case "getQueueLimit": return 10;
                case "getKeybindCost": return QueueCost.fixed(2);
                case "getQueueMonitorSide": return QueueMonitorSide.RIGHT;
                case "getQueueMonitorSlots": return 10;
                case "getMonitoredActions":
                    Constructor<org.keybinder.wurm.queue.ActionQueueEntry> entry = org.keybinder.wurm.queue.ActionQueueEntry.class.getDeclaredConstructor(long.class,String.class,String.class,String.class,long.class,boolean.class,boolean.class,boolean.class);
                    entry.setAccessible(true);
                    return Arrays.asList(entry.newInstance(1L,"Chop up","Hatchet","Birch tree",1L,false,true,false),
                            entry.newInstance(2L,"Improve","Hammer","Large anvil",2L,false,false,false));
                case "getActionName": return "Chop up";
                case "currentUser": return "Player";
                case "currentServer": return "Freedom";
            }
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            return null;
        });
    }
    private static void fixtureHud() throws Exception {
        Field unsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
        HeadsUpDisplay hud = (HeadsUpDisplay)((sun.misc.Unsafe)unsafe.get(null)).allocateInstance(HeadsUpDisplay.class);
        field(HeadsUpDisplay.class,"width").setInt(hud,1920); field(HeadsUpDisplay.class,"height").setInt(hud,1080);
        field(HeadsUpDisplay.class,"components").set(hud,new ArrayList<WurmComponent>());
        field(HeadsUpDisplay.class,"dropdownPopups").set(hud,new ArrayList<WurmDropdownPopup>());
        WurmComponent.hud = hud; WurmComponent.SCREEN_WIDTH = 1920; WurmComponent.SCREEN_HEIGHT = 1080;
    }
    private static Field field(Class<?> type,String name) throws Exception { Field field=type.getDeclaredField(name);field.setAccessible(true);return field; }
    private static void check(boolean condition,String message) { if (!condition) throw new AssertionError(message); }
    public static boolean clip(int x,int y,int w,int h) { clips.push(graphics.getClip()==null?new Rectangle(0,0,surface.getWidth(),surface.getHeight()):graphics.getClip()); graphics.clipRect(x,y,w,h);return w>0&&h>0; }
    public static void unclip() { graphics.setClip(clips.pop()); }
    public static void rect(float r,float g,float b,float a,int x,int y,int w,int h) { graphics.setColor(new Color(r,g,b,a));graphics.fillRect(x,y,w,h); }
    public static void illustration(WurmComponent owner,int x,int y,int w,int h) throws Exception {
        String type = owner.getClass().getSimpleName();
        String name = type.equals("TileSelector")
                ? "tile-selector.png" : type.equals("KeybinderTagWindow") ? "kb-tag.png" : null;
        if (name == null) return;
        try(java.io.InputStream input = KeybinderLayoutProbe.class.getResourceAsStream("/keybinder/" + name)) {
            graphics.drawImage(ImageIO.read(input),x,y,w,h,null);
        }
    }
    public static boolean texture(UiAsset asset,float tint,float alpha,int x,int y,int w,int h,float u0,float v0,float u1,float v1) throws Exception {
        check(alpha == 1f,"UI texture opacity");
        BufferedImage image=assets.get(asset);
        if(image==null) { try(java.io.InputStream in=UiResources.open(asset)) { image=ImageIO.read(in); } assets.put(asset,image); }
        String key=asset.name()+tint;BufferedImage tinted=tints.get(key);
        if(tinted==null) { tinted=new RescaleOp(new float[]{tint,tint,tint,1f},new float[4],null).filter(image,null);tints.put(key,tinted); }
        graphics.drawImage(tinted,x,y,x+w,y+h,Math.round(u0*image.getWidth()),Math.round(v0*image.getHeight()),Math.round(u1*image.getWidth()),Math.round(v1*image.getHeight()),null);return true;
    }
    public static final class PaintFont extends TextFont {
        private final Font font;
        final boolean branded;
        private int x,y;
        public PaintFont(Font font,boolean branded) { this.font=font;this.branded=branded; }
        public void moveTo(int x,int y) { this.x=x;this.y=y; }
        public int paint(Queue queue,String value,float r,float g,float b,float a) {
            check(branded,"Visible Keybinder text uses a native font: "+value);check(a==1f,"Text opacity");
            graphics.setFont(font);graphics.setColor(new Color(r,g,b,a));
            int position=x;for(char c:value.toCharArray()) { graphics.drawString(String.valueOf(c),position,y);position+=metrics().charWidth(c); }
            return getWidth(value);
        }
        private FontMetrics metrics() { return graphics.getFontMetrics(font); }
        public int getWidth(String value) { int result=0;for(char c:value.toCharArray())result+=metrics().charWidth(c);return result; }
        public int getWidth(char[] value,int start,int length) { return getWidth(new String(value,start,length)); }
        public int getHeight() { return metrics().getHeight(); }
        public int getAscent() { return metrics().getAscent(); }
        public int getDescent() { return metrics().getDescent(); }
        public int getLeading() { return metrics().getLeading(); }
    }
}
