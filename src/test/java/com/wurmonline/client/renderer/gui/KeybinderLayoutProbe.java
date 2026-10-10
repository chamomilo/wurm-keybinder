package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.*;
import org.keybinder.wurm.i18n.Messages;
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
    public static int nativeFontPixels = 12;

    public static void main(String[] args) throws Exception {
        output = Paths.get(args[0]); Files.createDirectories(output);
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
        preview("cyrillic-font", new KeybinderUiConfirmWindow("Подтверждение", "Проверка шрифта Chamomilo — русский текст", () -> {}));
        Messages.select("en");
        for (int nativeSize : new int[]{10,18,32}) {
            nativeFontPixels = nativeSize;
            KeybinderWindow independent = new KeybinderWindow(controller);
            independent.showKeybinds(); independent.setSize(1200,540);
            preview("native-font-" + nativeSize, independent);
        }
        System.out.println("KEYBINDER_UI_OK: " + screenshots + " production previews; fonts, alpha, grouped buttons, native input, wheel/thumb/drag sync, shrinking lists, long constructor, centered glyphs, row spacing and help");
        graphics.dispose();
    }
    private static void preview(String id, WurmComponent component) throws Exception {
        component.setPosition(16, 16);
        if (component instanceof KeybinderWindow) component.gameTick();
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
    private static void click(WButton button) { button.leftPressed(button.x+5,button.y+5,1); button.leftReleased(button.x+5,button.y+5); }
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

        KeybinderUiDropDown language = (KeybinderUiDropDown)field(KeybinderWindow.class,"languageSelector").get(window);
        snapshot(window, 1f);
        WButton maximize = KeybinderUi.maximizeControl(window);
        check(language.x + language.width + 8 == maximize.x - 28, "Language must precede top-right controls");
        check(window.getComponentAt(language.x + 5, language.y + 5) == language, "Header language hit testing");
        language.leftPressed(language.x + 5, language.y + 5, 1);
        check(!popups().isEmpty(), "Header language dropdown opens");
        popups().clear();

        List<KeybindVariant> variants = Arrays.asList(new KeybindVariant("default", "Default", Collections.nCopies(14, steps.get(0))),
                new KeybindVariant("alternative", "Alternative", Collections.nCopies(14, steps.get(0))));
        KeybindRecord longRecord = new KeybindRecord("long", "Long draft", "SPACE", variants, "default");
        KeybinderEditorWindow editor = new KeybinderEditorWindow(controller(KeybindEditorController.class, Collections.singletonList(longRecord)), longRecord.getId());
        window.showEditor(editor); window.setSize(1240, 720);
        preview("long-constructor", window);
        check(language.x > window.x && language.x + language.width < window.x + window.width,
                "Language selector must remain in the host header while the constructor is embedded");
        check(window.getComponentAt(language.x + 5, language.y + 5) == language,
                "Embedded constructor header language hit testing");
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
                case "getLanguage": return Messages.languageCode();
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
