package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.ui.KeybinderUiController;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.resources.textures.KeybinderTextureFactory;
import com.wurmonline.client.resources.textures.ResourceTexture;

public final class KeybinderIntroWindow extends WWindow implements ButtonListener {
    private static final int CONTENT_WIDTH = 660;
    private static final int BANNER_HEIGHT = CONTENT_WIDTH / 2;
    private final KeybinderUiController controller;
    private final WButton primary;
    private final WButton originalProject;
    private final WButton improveProject;
    private final WButton inniriaImproveProject;
    private final WButton munstaImproveProject;
    private final WCheckBox skip;
    private boolean previousSkip;

    public KeybinderIntroWindow(KeybinderUiController controller) {
        super("keybinder.intro", false);
        this.controller = controller;
        setTitle("Welcome to Keybinder");

        WurmArrayPanel<FlexComponent> content =
                new WurmArrayPanel<>("keybinder.intro.content", WurmArrayPanel.DIR_VERTICAL);
        IntroBanner banner = new IntroBanner();
        banner.setSize(CONTENT_WIDTH, BANNER_HEIGHT);
        content.addComponent(banner);
        content.addComponent(new WurmLabel("Wurm is a wonderful game, but not every convenience is built in."));
        content.addComponent(new WurmLabel("Many powerful tools made by respected modders still assume that players"));
        content.addComponent(new WurmLabel("are programmers who enjoy working in a terminal."));
        content.addComponent(new WurmLabel("Keybinder makes keybind management clear and approachable for everyone."));
        content.addComponent(new WurmLabel("A keybind is now an ordered container of tool, improve, console, and action steps."));
        content.addComponent(new WurmLabel("Legacy binds are converted once, then managed as native Keybinder steps."));
        content.addComponent(new WurmLabel("Smart Improve reimplements the i2improve workflow inside the Keybinder engine."));
        content.addComponent(new WurmLabel("Over-limit keybinds are warned and disabled once; you may explicitly enable them at your risk."));
        content.addComponent(new WurmLabel("It is based on bdew's Custom Actions 0.9, which it fully replaces."));
        content.addComponent(new WurmLabel("Created by Chamomilo. Thanks to bdew, the original Custom Actions developer."));
        content.addComponent(new WurmLabel("License: GNU LGPL 3.0 or later."));
        originalProject = new WButton("Open original Custom Actions project", this);
        originalProject.setHoverString("https://github.com/bdew-wurm/action");
        content.addComponent(originalProject);
        content.addComponent(new WurmLabel("Thanks to every developer in the Improved Improve mod lineage:"));
        content.addComponent(new WurmLabel("Munsta0 created the original Improved Improve client mod."));
        munstaImproveProject = new WButton("Open Munsta0's Improved Improve project", this);
        munstaImproveProject.setHoverString("https://github.com/munsta0/WUClientImprovedImprove");
        content.addComponent(munstaImproveProject);
        content.addComponent(new WurmLabel("inniria rewrote and extended it as i2improve."));
        inniriaImproveProject = new WButton("Open inniria's i2improve project", this);
        inniriaImproveProject.setHoverString("https://github.com/inniria/i2improve");
        content.addComponent(inniriaImproveProject);
        content.addComponent(new WurmLabel("Snidor continued i2improve; release 0.2.1 informed Smart Improve."));
        improveProject = new WButton("Open Snidor's i2improve project", this);
        improveProject.setHoverString("https://github.com/Snidor/i2improve");
        content.addComponent(improveProject);

        boolean legacy = controller.isLegacyActionInstalled();
        content.addComponent(new WurmLabel(legacy
                ? "Custom Actions was found. Keybinder can import its binds, disable it, and open Exit Game."
                : "Custom Actions was not found. Keybinder is ready to use."));
        primary = new WButton(legacy
                ? "Import binds, disable Custom Actions, and exit game"
                : "Start Keybinder", this);
        primary.setSize(CONTENT_WIDTH, primary.height);
        content.addComponent(primary);

        skip = new WCheckBox("Skip intro page on next load");
        skip.checked = controller.isSkipIntro();
        previousSkip = skip.checked;
        content.addComponent(skip);
        WurmArrayPanel<FlexComponent> versionRow =
                new WurmArrayPanel<>("keybinder.intro.version", WurmArrayPanel.DIR_HORIZONTAL);
        FlexComponent versionSpacer = new WurmLabel("");
        versionSpacer.setSize(CONTENT_WIDTH - 120, 18);
        versionRow.addComponent(versionSpacer);
        versionRow.addComponent(new WurmLabel("Keybinder " + KeybinderMod.VERSION));
        content.addComponent(versionRow);

        setComponent(content);
        setInitialSize(CONTENT_WIDTH + 16, Math.max(510, content.calcHeight() + 48), false);
    }

    @Override
    public void gameTick() {
        super.gameTick();
        if (skip.checked != previousSkip) {
            previousSkip = skip.checked;
            controller.setSkipIntro(skip.checked);
        }
    }

    @Override public void buttonPressed(WButton button) {}

    @Override
    public void buttonClicked(WButton button) {
        if (button == originalProject) {
            controller.openOriginalProject();
            return;
        }
        if (button == improveProject) {
            controller.openImproveProject();
            return;
        }
        if (button == inniriaImproveProject) {
            controller.openInniriaImproveProject();
            return;
        }
        if (button == munstaImproveProject) {
            controller.openMunstaImproveProject();
            return;
        }
        if (button != primary) return;
        controller.setSkipIntro(skip.checked);
        if (controller.isLegacyActionInstalled()) controller.importDisableAndRestart();
        else controller.startFromIntro();
    }

    @Override
    protected void closePressed() {
        controller.setSkipIntro(skip.checked);
        controller.startFromIntro();
    }

    private static final class IntroBanner extends FlexComponent {
        private final ResourceTexture texture =
                KeybinderTextureFactory.load("intro-banner.png");

        private IntroBanner() {
            super("keybinder.intro.banner");
        }

        @Override
        protected void renderComponent(Queue queue, float alpha) {
            drawTexture(queue, texture, 1f, 1f, 1f, 1f,
                    x, y, width, height, 0, 0, 256, 256);
        }
    }
}
