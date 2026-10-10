package org.keybinder.wurm.ui;

import javassist.*;

/** Runs production Keybinder/SDK layout and input; substitutes only GPU and HUD startup. */
public final class ChamomiloUiProbe {
    public static void main(String[] args) throws Throwable {
        ClassPool pool = new ClassPool(true);
        String gui = "com.wurmonline.client.renderer.gui.";
        String probe = gui + "KeybinderLayoutProbe";
        CtClass fonts = pool.get(gui + "text.TextFont");
        fonts.getClassInitializer().setBody("{}");
        fonts.getDeclaredMethod("getText", new CtClass[]{pool.get("java.lang.String")})
                .setBody("{ return new " + probe + ".PaintFont(new java.awt.Font(\"Dialog\",0," + probe + ".nativeFontPixels),false); }");
        pool.get("com.wurmonline.client.options.Options").getClassInitializer().setBody("{}");
        CtClass buffer = pool.get("com.wurmonline.client.renderer.backend.VertexBuffer");
        if (buffer.getClassInitializer() != null) buffer.getClassInitializer().setBody("{}");
        for (CtConstructor constructor : buffer.getDeclaredConstructors()) constructor.setBody("{}");
        buffer.getDeclaredMethod("lock").setBody("{ return java.nio.FloatBuffer.allocate(4096); }");
        buffer.getDeclaredMethod("unlock").setBody("{}");
        CtClass component = pool.get(gui + "WurmComponent");
        component.getClassInitializer().setBody("{}");
        component.getDeclaredMethod("fillRect").setBody("{ " + probe + ".rect($2,$3,$4,$5,$6,$7,$8,$9); }");
        component.getDeclaredMethod("drawTexture").setBody("{ " + probe + ".illustration(this,$7,$8,$9,$10); }");
        CtClass hud = pool.get(gui + "HeadsUpDisplay");
        hud.getClassInitializer().setBody("{ scissor = new com.wurmonline.client.renderer.backend.ScissorControl(); }");
        hud.getDeclaredMethod("showDropdownPopupComponent").setBody("{ dropdownPopups.add($1); }");
        hud.getDeclaredMethod("clearAllPopups").setBody("{ dropdownPopups.clear(); }");
        hud.getDeclaredMethod("showComponent").setBody("{}");
        hud.getDeclaredMethod("hideComponent").setBody("{}");
        hud.getDeclaredMethod("setActiveWindow").setBody("{}");
        hud.getDeclaredMethod("addComponent").setBody("{ components.add($1); return true; }");
        hud.getDeclaredMethod("removeComponent").setBody("{ return components.remove($1); }");
        CtClass scissor = pool.get("com.wurmonline.client.renderer.backend.ScissorControl");
        scissor.getDeclaredMethod("pushClip").setBody("{ return " + probe + ".clip($1,$2,$3,$4); }");
        scissor.getDeclaredMethod("popClip").setBody("{ " + probe + ".unclip(); }");
        CtClass canvas = pool.get(gui + "ChamomiloUiV1Canvas");
        canvas.getDeclaredMethod("fill").setBody("{ " + probe + ".rect($1.red,$1.green,$1.blue,$2,$3,$4,$5,$6); }");
        canvas.getDeclaredMethod("texture").setBody("{ return " + probe + ".texture($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11); }");
        pool.get(gui + "text.ChamomiloUiV1Fonts").getDeclaredMethod("caption", new CtClass[]{
                CtClass.intType,CtClass.booleanType,pool.get("org.chamomilo.wurm.ui.v1.UiDensity")})
                .setBody("{ return new " + probe + ".PaintFont(org.chamomilo.wurm.ui.v1.UiTypography.font($1,$2,$3),true); }");
        pool.get(gui + "WurmInputField").getDeclaredMethod("renderComponent")
                .setBody("{ text.moveTo(x,y+text.getAscent()); text.paint($1,getText(),1f,1f,1f,1f); }");
        pool.get("com.wurmonline.client.resources.textures.KeybinderTextureFactory")
                .getDeclaredMethod("load").setBody("{ return null; }");
        CtClass renderer = pool.get(gui + "Renderer");
        if (renderer.getClassInitializer() != null) renderer.getClassInitializer().setBody("{}");
        for (CtMethod method : renderer.getDeclaredMethods("texturedQuadAlphaBlend"))
            if (method.getParameterTypes().length == 14) method.setBody("{}");
        pool.get("org.keybinder.wurm.KeybinderMod").getDeclaredMethod("deferUi")
                .setBody("{ $1.run(); }");
        Loader loader = new Loader(pool);
        loader.run(probe, args);
    }
}
