package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.chamomilo.wurm.ui.v1.UiColor;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.i18n.Messages;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** On-demand, scrollable instructions. Opening help never changes the editor draft. */
public final class KeybinderInstructionWindow extends KeybinderUiWindow implements ButtonListener {
    private static KeybinderInstructionWindow active;
    private final boolean constructorHelp;
    private final WurmArrayPanel<FlexComponent> content;
    private final KeybinderUiScrollPanel scroll;
    private final WButton instructions, about, close;
    private final List<WButton> links = new ArrayList<>();
    private final List<String> urls = new ArrayList<>();
    private HeadsUpDisplay ownerHud;
    private boolean aboutVisible;

    KeybinderInstructionWindow(boolean constructorHelp) {
        super("keybinder.instructions", true);
        this.constructorHelp = constructorHelp;
        setTitle(Messages.text(constructorHelp ? "help.constructor_title" : "help.title"));
        content = new KeybinderUiArrayPanel<>("keybinder.instructions.body", WurmArrayPanel.DIR_VERTICAL);
        scroll = new KeybinderUiScrollPanel("keybinder.instructions.scroll", content);
        WurmBorderPanel root = new WurmBorderPanel("keybinder.instructions.root");
        WurmArrayPanel<FlexComponent> tabs = new KeybinderUiArrayPanel<>("keybinder.instructions.tabs", WurmArrayPanel.DIR_HORIZONTAL);
        tabs.componentWidthOffset = 8;
        instructions = new KeybinderUiButton(Messages.text("help.instructions"), this);
        about = new KeybinderUiButton(Messages.text("help.about"), this);
        close = new KeybinderUiButton(Messages.text("help.close"), this);
        tabs.addComponents(instructions, about);
        root.setComponent(tabs, WurmBorderPanel.NORTH);
        root.setComponent(scroll, WurmBorderPanel.CENTER);
        root.setComponent(close, WurmBorderPanel.SOUTH);
        setComponent(root);
        showInstructions();
        setInitialSize(760, 620, false);
    }

    public static void open(boolean constructorHelp) {
        KeybinderMod.deferUi(() -> {
            closeActive();
            if (WurmComponent.hud == null) return;
            active = new KeybinderInstructionWindow(constructorHelp);
            active.ownerHud = WurmComponent.hud;
            active.show(active.ownerHud);
        });
    }

    public static void closeActive() {
        if (active != null) {
            active.detach();
            active = null;
        }
    }

    private void detach() {
        if (ownerHud != null) {
            ownerHud.hideComponent(this);
            ownerHud.getComponents().remove(this);
        }
    }

    private void paragraph(String key, boolean heading) {
        content.addComponent(new Paragraph(Messages.text(key), heading));
    }

    private void showInstructions() {
        aboutVisible = false;
        content.removeAllComponents();
        links.clear(); urls.clear();
        if (constructorHelp) {
            paragraph("help.constructor_heading", true);
            for (String key : new String[]{"editor.instructions", "editor.action_selection_help",
                    "editor.long_press", "editor.mouse_help", "editor.extract_help"}) paragraph(key, false);
        } else {
            paragraph("help.list_heading", true);
            paragraph("list.instructions", false);
            paragraph("list.instructions.merge", false);
        }
        for (String section : new String[]{"start", "actions", "sources", "targets", "multi", "hud",
                "capture", "queue", "organize", "profiles", "transfer", "language"}) {
            paragraph("help." + section + ".heading", true);
            paragraph("help." + section + ".body", false);
        }
        finishPage();
    }

    private void showAbout() {
        aboutVisible = true;
        content.removeAllComponents();
        links.clear(); urls.clear();
        content.addComponent(new Paragraph("Keybinder " + KeybinderMod.VERSION, true));
        paragraph("help.about.description", false);
        paragraph("help.about.credits", false);
        link("help.about.original", KeybinderMod.ORIGINAL_PROJECT);
        link("help.about.munsta", KeybinderMod.MUNSTA_IMPROVE_PROJECT);
        link("help.about.inniria", KeybinderMod.INNIRIA_IMPROVE_PROJECT);
        link("help.about.snidor", KeybinderMod.IMPROVE_PROJECT);
        paragraph("help.about.license", false);
        finishPage();
    }

    private void link(String captionKey, String url) {
        WButton button = new KeybinderUiButton(Messages.text(captionKey), this);
        links.add(button); urls.add(url);
        content.addComponent(button);
        KeybinderUiInputField copyable = new KeybinderUiInputField("keybinder.instructions.url", new KeybinderUiInputListener() {
            public void handleInput(String value) {}
            public void handleInputChanged(KeybinderUiInputField field, String value) {}
            public void handleEscape(KeybinderUiInputField field) {}
        });
        copyable.setText(url);
        content.addComponent(copyable);
    }

    private void finishPage() {
        instructions.setEnabled(aboutVisible);
        about.setEnabled(!aboutVisible);
        content.layout(); scroll.layout(); scroll.scrollDownTo(0);
        KeybinderUi.theme(this);
    }

    @Override public void buttonPressed(WButton button) {}
    @Override public void buttonClicked(WButton button) {
        if (button == instructions) showInstructions();
        else if (button == about) showAbout();
        else if (button == close) closePressed();
        else {
            int index = links.indexOf(button);
            if (index >= 0) try {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                    throw new IllegalStateException("Desktop browsing unavailable");
                Desktop.getDesktop().browse(new URI(urls.get(index)));
            } catch (Exception failure) {
                // Every URL is already visible in an editable field for copying.
                button.setHoverString(Messages.text("help.about.copy_url"));
                Logger.getLogger("Chamomilo.Keybinder").log(Level.WARNING, "Unable to open project URL", failure);
            }
        }
    }

    @Override protected void closePressed() {
        detach();
        if (active == this) active = null;
    }

    private static final class Paragraph extends FlexComponent {
        private final String caption;
        private final boolean heading;
        private final List<String> lines = new ArrayList<>();
        private int lastWidth = -1;

        Paragraph(String caption, boolean heading) {
            super("keybinder.instructions.paragraph");
            this.caption = caption; this.heading = heading;
            KeybinderUi.fonts(this);
            setSize(700, 24);
            sizeFlags = FIXED_HEIGHT;
            componentResized();
        }

        @Override void componentResized() {
            super.componentResized();
            if (caption == null || width == lastWidth) return;
            lastWidth = width;
            lines.clear();
            String line = "";
            for (String word : caption.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font().getWidth(candidate) > Math.max(1, width - 16)) {
                    lines.add(line); line = word;
                } else line = candidate;
            }
            if (!line.isEmpty()) lines.add(line);
            int desired = Math.max(1, lines.size()) * 22 + (heading ? 16 : 8);
            if (height != desired) {
                int flags = sizeFlags; sizeFlags = 0;
                setSize(width, desired); sizeFlags = flags;
            }
        }

        private com.wurmonline.client.renderer.gui.text.TextFont font() { return heading ? textBold : text; }

        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            for (int i = 0; i < lines.size(); i++) {
                font().moveTo(x + 8, y + (heading ? 10 : 4) + font().getAscent() + i * 22);
                font().paint(queue, lines.get(i), UiColor.TEXT.red, UiColor.TEXT.green, UiColor.TEXT.blue, 1f);
            }
        }
    }
}
