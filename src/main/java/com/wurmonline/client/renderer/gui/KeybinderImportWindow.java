package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.bind.BindSnapshot;
import org.keybinder.wurm.bind.VanillaImportCandidate;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.ui.ImportReviewController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Non-mutating review window for the one-time vanilla binding import. */
public final class KeybinderImportWindow extends KeybinderUiWindow implements ButtonListener {
    private static final int WIDTH = 860;
    private static final int HEIGHT = 430;
    private final ImportReviewController controller;
    private final Map<KeybinderUiCheckBox, VanillaImportCandidate> selections =
            new LinkedHashMap<KeybinderUiCheckBox, VanillaImportCandidate>();
    private final WButton importSelected;
    private final WButton later;
    private final WButton neverAsk;

    public KeybinderImportWindow(ImportReviewController controller,
                                 List<VanillaImportCandidate> candidates) {
        super("keybinder.import.review", false);
        this.controller = controller;
        setTitle(Messages.text("import.title"));

        WurmArrayPanel<FlexComponent> content = new KeybinderUiArrayPanel<FlexComponent>(
                "keybinder.import.content", WurmArrayPanel.DIR_VERTICAL);
        content.addComponent(new KeybinderUiLabel(Messages.text("import.instructions")));
        content.addComponent(header());
        for (VanillaImportCandidate candidate : candidates)
            content.addComponent(row(candidate));

        WurmBorderPanel root = new WurmBorderPanel("keybinder.import.root");
        root.setComponent(new KeybinderUiScrollPanel(
                "keybinder.import.scroll", content), WurmBorderPanel.CENTER);

        WurmArrayPanel<FlexComponent> buttons = new KeybinderUiArrayPanel<FlexComponent>(
                "keybinder.import.buttons", WurmArrayPanel.DIR_HORIZONTAL);
        buttons.componentWidthOffset = 8;
        importSelected = new KeybinderUiButton(Messages.text("import.selected"), this);
        later = new KeybinderUiButton(Messages.text("import.not_now"), this);
        neverAsk = new KeybinderUiButton(Messages.text("import.never_ask"), this);
        buttons.addComponent(importSelected);
        buttons.addComponent(later);
        buttons.addComponent(neverAsk);
        root.setComponent(buttons, WurmBorderPanel.SOUTH);
        setComponent(root);
        setInitialSize(WIDTH, HEIGHT, false);
    }

    private FlexComponent header() {
        return columns("keybinder.import.header", KeybinderUiLabel.header(""),
                KeybinderUiLabel.header(Messages.text("import.key")),
                KeybinderUiLabel.header(Messages.text("import.command")),
                KeybinderUiLabel.header(Messages.text("import.type")),
                KeybinderUiLabel.header(Messages.text("import.conversion")),
                KeybinderUiLabel.header(Messages.text("import.status")));
    }

    private FlexComponent row(VanillaImportCandidate candidate) {
        KeybinderUiCheckBox selected = new KeybinderUiCheckBox("");
        selected.checked = candidate.isSelectedByDefault();
        selections.put(selected, candidate);
        WurmLabel command = new KeybinderUiLabel(candidate.getBinding().getCommand());
        WurmLabel status = new KeybinderUiLabel(status(candidate));
        return columns("keybinder.import.row", selected,
                new KeybinderUiLabel(candidate.getBinding().getKey()), command,
                new KeybinderUiLabel(type(candidate.getType())),
                new KeybinderUiLabel(conversion(candidate.getType())), status);
    }

    private static FlexComponent columns(String id, FlexComponent selected,
                                         FlexComponent key, FlexComponent command,
                                         FlexComponent type, FlexComponent conversion,
                                         FlexComponent status) {
        WurmArrayPanel<FlexComponent> row = id.endsWith(".header") ? new KeybinderUiHeader(id)
                : new KeybinderUiArrayPanel<FlexComponent>(id, WurmArrayPanel.DIR_HORIZONTAL);
        row.componentWidthOffset = 8;
        selected.setSize(28, selected.height);
        key.setSize(85, key.height);
        command.setSize(250, command.height);
        type.setSize(125, type.height);
        conversion.setSize(150, conversion.height);
        status.setSize(150, status.height);
        row.addComponent(selected);
        row.addComponent(key);
        row.addComponent(command);
        row.addComponent(type);
        row.addComponent(conversion);
        row.addComponent(status);
        return row;
    }

    private static String type(VanillaImportCandidate.Type type) {
        switch (type) {
            case ACTION_CHAIN: return Messages.text("import.type.action");
            case SMART_IMPROVE: return Messages.text("import.type.improve");
            case VANILLA_COMMAND: return Messages.text("import.type.vanilla");
            default: return Messages.text("import.type.raw");
        }
    }

    private static String conversion(VanillaImportCandidate.Type type) {
        switch (type) {
            case ACTION_CHAIN: return Messages.text("import.convert.action");
            case SMART_IMPROVE: return Messages.text("import.convert.improve");
            case VANILLA_COMMAND: return Messages.text("import.convert.vanilla");
            default: return Messages.text("import.convert.raw");
        }
    }

    private static String status(VanillaImportCandidate candidate) {
        switch (candidate.getStatus()) {
            case READY: return Messages.text("import.status.ready");
            case NEEDS_REVIEW: return Messages.text("import.status.review");
            case CONFLICT: return Messages.text("import.status.conflict", candidate.getDetail());
            default: return Messages.text("import.status.invalid", candidate.getDetail());
        }
    }

    @Override public void buttonPressed(WButton button) { }

    @Override public void buttonClicked(WButton button) {
        if (button == importSelected) {
            List<BindSnapshot> selected = new ArrayList<BindSnapshot>();
            for (Map.Entry<KeybinderUiCheckBox, VanillaImportCandidate> entry : selections.entrySet()) {
                if (entry.getKey().checked && entry.getValue().isImportable())
                    selected.add(entry.getValue().getBinding());
            }
            controller.confirmImport(selected);
        } else if (button == neverAsk) {
            controller.closeImportReview(true);
        } else if (button == later) {
            controller.closeImportReview(false);
        }
    }

    @Override protected void closePressed() {
        KeybinderMod.deferUi(new Runnable() {
            @Override public void run() { controller.closeImportReview(false); }
        });
    }
}
