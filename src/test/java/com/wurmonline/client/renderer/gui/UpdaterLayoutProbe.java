package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.update.ModUpdate;
import org.chamomilo.wurm.update.UpdatePreferences;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Offscreen preview uses the production component tree and paint coordinates. */
public final class UpdaterLayoutProbe {
    private static BufferedImage canvas, artwork, buttonArtwork;
    private static com.wurmonline.client.resources.textures.ResourceTexture buttonTexture;
    private static final Map<Integer,BufferedImage> tintedButtons = new HashMap<Integer,BufferedImage>();
    private static final Map<String,Point> paintedLabels = new HashMap<String,Point>();
    private static Graphics2D graphics;
    private static final Deque<Shape> clips = new LinkedList<Shape>();
    private static int fontSize = 12;
    private static UpdatePreferences preferences;
    private static Path preferenceFile;

    public static Object allocate(Class<?> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return ((sun.misc.Unsafe) field.get(null)).allocateInstance(type);
    }
    public static void main(String[] args) throws Exception {
        artwork = ImageIO.read(new File(args[0]));
        buttonArtwork = ImageIO.read(new File(args[2]));
        canvas = new BufferedImage(1000, 900, BufferedImage.TYPE_INT_ARGB);
        graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        preferenceFile = Paths.get(args[1]).getParent().resolve("updater-preference-fixture/settings.properties");
        Files.deleteIfExists(preferenceFile);
        Path user = preferenceFile.getParent().resolve("user");
        Files.deleteIfExists(user.resolve(".chamomilo/updater.properties"));
        System.setProperty("user.home", user.toAbsolutePath().toString());
        preferences = new UpdatePreferences(preferenceFile);
        java.util.List<ModUpdate> rows = rows();
        final int[] downloads = {0};
        ChamomiloUpdateWindow window = window(rows, 1280, 1080, downloads);
        WurmScrollPanel scroll = scroll(window);
        window.gameTick();
        check(window.getTitle().equals("Mods Registry by Chamomilo"), "Registry heading");
        check(!children(scroll).contains(scroll.verticalScrollBar), "Scrollbar has no visible or clickable child");
        check(scroll.content.height <= ((WurmComponent) scroll.offs).height, "Full list must fit without scrolling on a tall screen: "
                + scroll.content.height + "/" + ((WurmComponent) scroll.offs).height);
        check(window.height < 850, "Compact eight-card window");
        WurmArrayPanel<?> content = (WurmArrayPanel<?>) scroll.content;
        int previousBottom = Integer.MIN_VALUE;
        for (FlexComponent card : content.components) {
            if (card.height == 6) continue;
            check(card.y > previousBottom, "Card spacing"); previousBottom = card.y + card.height;
            check(card.height >= 64 && card.height <= 76, "Compact card height: " + card.height);
            WButton button = action(card);
            check(button.width == 108, "Fixed action width: " + button.width);
            check(button.height >= 28 && button.height >= button.text.getHeight() + 12,
                    "Button has room for original rails and a centered label");
            check(button.height < card.height / 2, "Action button must not stretch to card height");
            if (button.getLabel().equals("LATEST")) {
                check(!button.isEnabled(), "LATEST is disabled"); window.buttonClicked(button);
            }
        }
        check(downloads[0] == 0, "Disabled actions do not download");
        WButton firstAction = action(content.components.get(0));
        window.buttonClicked(firstAction);
        check(downloads[0] == 1, "DOWNLOAD routes to the host");

        window.setPosition(30, 30);
        rect(.025f,.023f,.018f,1f,0,0,canvas.getWidth(),canvas.getHeight());
        window.render(null, .2f);
        verifyHeading(window);
        for(FlexComponent card:content.components)if(card.height!=6)verifyButtonArtwork(action(card));
        verifyButtonArtwork((WButton)field(ChamomiloUpdateWindow.class,"laterButton").get(window));
        window.render(null,.2f);
        ImageIO.write(canvas.getSubimage(window.x, window.y, window.width, window.height), "png", new File(args[1]));
        verifyHorizontalScaling();
        renderButtonExamples(Paths.get(args[1]).getParent().resolve("updater-buttons-preview.png").toFile());

        WButton skip = checkbox(window.getComponent());
        check(!checked(skip), "First launch offers an unchecked opt-out");
        verifyFooter(window);
        click(skip,skip.x+30,skip.y+skip.height/2);
        window.gameTick();
        check(new UpdatePreferences(preferenceFile).isSkipNextStart(), "Native checkbox saves on tick");
        preferences = new UpdatePreferences(preferenceFile);
        ChamomiloUpdateWindow nextStart = window(rows.subList(0, 3), 1280, 1080, downloads);
        WButton nextSkip = checkbox(nextStart.getComponent());
        check(checked(nextSkip), "Saved opt-out survives restart and a changed mod list");
        click(nextSkip,nextSkip.x+5,nextSkip.y+nextSkip.height/2);
        nextStart.closePressed();
        check(!new UpdatePreferences(preferenceFile).isSkipNextStart(), "Close saves an unticked box before hiding");
        preferences = new UpdatePreferences(preferenceFile);

        ChamomiloUpdateWindow small = window(rows, 1024, 480, downloads);
        WurmScrollPanel smallScroll = scroll(small); small.gameTick();
        check(small.height <= 460, "Small-screen window stays within HUD");
        check(!children(smallScroll).contains(smallScroll.verticalScrollBar), "Overflow has no visible scrollbar");
        check(smallScroll.content.height > ((WurmComponent) smallScroll.offs).height, "Overflow scrolls");
        int oldOffset = smallScroll.yo;
        smallScroll.mouseWheeled(100,100,4);
        check(smallScroll.yo != oldOffset, "Mouse wheel scrolls");
        smallScroll.scrollDownToBottom();
        check(smallScroll.yo > 0, "Last card reachable");
        small.setSize(700, 260); small.gameTick();
        check(small.height == 260, "Native resize remains available");
        verifyFooter(small);

        fontSize = 10;
        ChamomiloUpdateWindow smallFont = window(rows, 1280, 1080, downloads);
        smallFont.gameTick();
        verifyFooter(smallFont);
        smallFont.setPosition(30,30); smallFont.render(null,1f); verifyHeading(smallFont);
        verifyButtonArtwork(action(((WurmArrayPanel<?>)scroll(smallFont).content).components.get(0)));

        fontSize = 18;
        ChamomiloUpdateWindow largeFont = window(rows, 1280, 1080, downloads);
        largeFont.gameTick();
        verifyFooter(largeFont);
        largeFont.setPosition(30,30); largeFont.render(null,1f); verifyHeading(largeFont);
        verifyButtonArtwork(action(((WurmArrayPanel<?>)scroll(largeFont).content).components.get(0)));
        check(scroll(largeFont).content.height <= ((WurmComponent) scroll(largeFont).offs).height, "Large font layout fits");
        check(ChamomiloUpdateWindow.FRAME_PIXELS == 5, "Frame thickness");
        verifyMenuLifecycle(rows);
        System.out.println("UPDATER_UI_OK: native layout, full list, overflow, wheel, resize, actions and large fonts; preview=" + args[1]);
        graphics.dispose();
    }

    private static ChamomiloUpdateWindow window(java.util.List<ModUpdate> rows, int w, int h, final int[] count) throws Exception {
        HeadsUpDisplay hud = fixtureHud(w,h);
        return new ChamomiloUpdateWindow(hud, rows,"","","","","Close","Close", row -> count[0]++, () -> {}, preferences);
    }
    private static HeadsUpDisplay fixtureHud(int w, int h) throws Exception {
        HeadsUpDisplay hud = (HeadsUpDisplay) allocate(HeadsUpDisplay.class);
        field(HeadsUpDisplay.class,"width").setInt(hud,w); field(HeadsUpDisplay.class,"height").setInt(hud,h);
        field(HeadsUpDisplay.class,"components").set(hud,new ArrayList<WurmComponent>());
        WurmComponent.SCREEN_WIDTH=w; WurmComponent.SCREEN_HEIGHT=h; WurmComponent.hud=hud;
        field(HeadsUpDisplay.class,"mainMenu").set(hud,new MainMenu());
        return hud;
    }
    public static com.wurmonline.client.options.MultiOption guiSkin() throws Exception {
        com.wurmonline.client.options.MultiOption skin=(com.wurmonline.client.options.MultiOption)allocate(com.wurmonline.client.options.MultiOption.class);
        field(com.wurmonline.client.options.MultiOption.class,"options").set(skin,new String[]{"Default"});
        return skin;
    }
    private static void verifyMenuLifecycle(java.util.List<ModUpdate> rows) throws Exception {
        resetSharedHost();
        deliver(rows); HeadsUpDisplay hud=fixtureHud(1280,1080);
        ChamomiloUpdateBridge.hudReady(hud);
        ChamomiloUpdateWindow window=sharedWindow();
        check(window!=null && hud.isComponentEnabled(window),"First launch automatically shows Mod updates");
        check(hud.mainMenu.getMenuSet().size()==1,"Exactly one menu entry");
        WButton menuButton=hud.mainMenu.getMenuSet().iterator().next().getKey();
        check(menuButton.getLabel().equals("Mod updates"),"Exact main menu label");
        window.closePressed();
        check(!hud.isComponentEnabled(window) && menuButton.isEnabled(),"Closed window stays available in Main menu");
        hud.mainMenu.buttonClicked(menuButton);
        check(hud.isComponentEnabled(window),"Main menu reopens a closed window");
        WButton skip=checkbox(window.getComponent());click(skip,skip.x+5,skip.y+skip.height/2);window.gameTick();
        check(UpdatePreferences.shared().isSkipNextStart(),"Host window saves startup opt-out");
        window.closePressed();

        resetSharedHost(); HeadsUpDisplay next=fixtureHud(1280,1080);
        ChamomiloUpdateBridge.hudReady(next);
        check(sharedWindow()==null,"HUD can precede the background catalogue result");
        deliver(rows.subList(0,3)); ChamomiloUpdateBridge.tick(next);
        ChamomiloUpdateWindow hidden=sharedWindow();
        check(hidden!=null && !next.isComponentEnabled(hidden),"Opted-out startup registers a hidden window with a changed catalogue");
        ChamomiloUpdateBridge.hudReady(next); ChamomiloUpdateBridge.tick(next);
        check(next.mainMenu.getMenuSet().size()==1,"Repeated HUD callbacks do not duplicate the entry");
        WButton nextButton=next.mainMenu.getMenuSet().iterator().next().getKey();next.mainMenu.buttonClicked(nextButton);
        check(next.isComponentEnabled(hidden),"Main menu works with startup display disabled");
        WButton remembered=checkbox(hidden.getComponent());check(checked(remembered),"Reopened box retains checked state");
        click(remembered,remembered.x+30,remembered.y+remembered.height/2);hidden.closePressed();
        check(!UpdatePreferences.shared().isSkipNextStart(),"Unticking and closing re-enables startup");
        resetSharedHost(); HeadsUpDisplay last=fixtureHud(1280,1080);
        deliver(rows); ChamomiloUpdateBridge.hudReady(last);
        check(last.isComponentEnabled(sharedWindow()),"Following startup shows the window again");
        HeadsUpDisplay replacement=fixtureHud(1280,1080);
        ChamomiloUpdateBridge.hudReady(replacement);
        check(replacement.mainMenu.getMenuSet().size()==1 && !replacement.isComponentEnabled(sharedWindow()),
                "A replacement HUD registers one hidden menu entry without repeating the startup popup");
        System.out.println("UPDATER_MENU_OK: first startup, persistent checkbox, changed catalogue, hidden registration, close/reopen and duplicate prevention");
    }
    private static void resetSharedHost() throws Exception {
        field(ChamomiloUpdateBridge.class,"readyHud").set(null,null);
        field(ChamomiloUpdateBridge.class,"window").set(null,null);
        field(ChamomiloUpdateBridge.class,"startupHandled").setBoolean(null,false);
        deliver(null);
    }
    private static void deliver(java.util.List<ModUpdate> rows) throws Exception {
        field(org.chamomilo.wurm.update.SharedUpdateHooks.class,"pending").set(null,rows);
    }
    private static ChamomiloUpdateWindow sharedWindow() throws Exception {
        return (ChamomiloUpdateWindow)field(ChamomiloUpdateBridge.class,"window").get(null);
    }
    private static java.util.List<ModUpdate> rows() throws Exception {
        // Same catalogue/version examples as the user's reference screenshot.
        String[] ids={"wurm-highres","highres-hud","highres-startup","idleanimations","keybinder","armor-material-colors","WU-third_person_view","wurm-waypointer"};
        String[] names={"High Res Icons","HighRes HUD","HighRes Startup","Idle Animations","Keybinder","Material Colors","Third Person View","Waypointer"};
        String[] repos={"wurm-high-res-icons","Wurm-HighRes-HUD","wurm-highres-startup","wurm-idle-animations","wurm-keybinder","wurm-material-colors","WU-3rd-person-view","wurm-waypointer"};
        String[] installed={"0.1.30","0.1.0","0.3.5","0.1.5","0.9.1","0.3.17","0.3.7","1.4.3"};
        String[] latest={"0.1.31","0.1.2","0.3.6","0.1.6","0.10.0","0.3.18","0.3.9","1.4.0"};
        Constructor<ModUpdate> constructor=ModUpdate.class.getDeclaredConstructor(String.class,String.class,String.class,String.class,String.class,boolean.class,String.class);
        constructor.setAccessible(true);
        java.util.List<ModUpdate> rows=new ArrayList<ModUpdate>();
        for(int i=0;i<ids.length;i++) rows.add(constructor.newInstance(ids[i],names[i],installed[i],latest[i],
                "https://github.com/chamomilo/"+repos[i]+"/releases/latest",i<7,""));
        return rows;
    }
    private static Field field(Class<?> type,String name) throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static java.util.List<FlexComponent> children(WurmComponent parent) throws Exception {
        if(parent instanceof WurmArrayPanel) return new ArrayList<FlexComponent>(((WurmArrayPanel<?>)parent).components);
        if(parent instanceof WurmBorderPanel) return Arrays.asList((FlexComponent[])field(WurmBorderPanel.class,"components").get(parent));
        return Collections.emptyList();
    }
    private static WurmScrollPanel scroll(WurmComponent parent) throws Exception {
        if(parent instanceof WWindow) return scroll(((WWindow)parent).getComponent());
        if(parent instanceof WurmScrollPanel) return (WurmScrollPanel)parent;
        for(FlexComponent child:children(parent))if(child!=null){WurmScrollPanel result=scroll(child);if(result!=null)return result;}
        return null;
    }
    private static WButton action(WurmComponent parent) throws Exception {
        if(parent instanceof WButton) return (WButton)parent;
        for(FlexComponent child:children(parent))if(child!=null){WButton result=action(child);if(result!=null)return result;}
        return null;
    }
    private static WButton checkbox(WurmComponent parent) throws Exception {
        if(parent instanceof WButton && ((WButton)parent).getLabel().equals("Don't show on next start")) return (WButton)parent;
        for(FlexComponent child:children(parent))if(child!=null){WButton result=checkbox(child);if(result!=null)return result;}
        return null;
    }
    private static boolean checked(WButton checkbox) throws Exception {return field(checkbox.getClass(),"checked").getBoolean(checkbox);}
    private static void click(WButton button,int x,int y){button.leftPressed(x,y,0);button.leftReleased(x,y);}
    private static void verifyFooter(ChamomiloUpdateWindow window) throws Exception {
        WButton skip=checkbox(window.getComponent());
        WButton close=(WButton)field(ChamomiloUpdateWindow.class,"laterButton").get(window);
        check(skip.height==close.height && skip.y==close.y,"Footer controls share one compact row");
        check(skip.height>=skip.text.getHeight()+1,"Footer label fits with extra bottom space");
        check(window.y+window.height-(skip.y+skip.height)<=24,"Footer has no excess empty area");
    }
    private static void verifyHeading(ChamomiloUpdateWindow window) throws Exception {
        Point label = paintedLabels.get(window.getTitle());
        check(label != null && label.y - window.text.getAscent() >= window.y + ChamomiloUpdateWindow.FRAME_PIXELS + 5,
                "Heading is inside the black header below the frame");
        check(label.y + window.text.getDescent() + 5 < scroll(window).y,
                "Heading clears the first card at every font size");
    }
    private static void verifyButtonArtwork(WButton button) {
        for(int state=0;state<3;state++) {
            button.hovered=state==1;
            button.isDown=state==2;
            button.render(null,1f);
            float tint=!button.isEnabled()?.55f:state==2?.72f:state==1?1.12f:1f;
            BufferedImage reference=new BufferedImage(button.width,button.height,BufferedImage.TYPE_INT_ARGB);
            Graphics2D referenceGraphics=reference.createGraphics();
            referenceGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            referenceGraphics.setColor(new Color(.105f,.097f,.079f,1f));
            referenceGraphics.fillRect(0,0,button.width,button.height);
            BufferedImage tinted=new java.awt.image.RescaleOp(new float[]{tint,tint,tint,1f},new float[4],null).filter(buttonArtwork,null);
            // Independently shrink the entire original face. Each real rail must
            // retain its own average colour, including the darker lower iron.
            referenceGraphics.drawImage(tinted,0,0,button.width,button.height,14,96,2159,624,null);
            referenceGraphics.dispose();
            for(int row:new int[]{1,button.height-2}) {
                int[] actual=averageRail(canvas,button.x,button.y+row,button.width);
                int[] expected=averageRail(reference,0,row,button.width);
                for(int channel=0;channel<3;channel++) check(Math.abs(actual[channel]-expected[channel])<=15,
                        "Original rail retained: " + button.getLabel() + ", state=" + state + ", row=" + row);
            }
            Point label=paintedLabels.get(button.getLabel());
            int pressed=button.isEnabled()&&state==2?1:0;
            check(label.x==button.x+(button.width-button.text.getWidth(button.getLabel()))/2+pressed,
                    "Centered button label");
            check(label.y-button.text.getAscent()>=button.y+5 && label.y+button.text.getDescent()<=button.y+button.height-4,
                    "Label clears both original rails");
        }
        button.hovered=false;button.isDown=false;
    }
    private static int[] averageRail(BufferedImage source,int x,int y,int width) {
        int[] sum=new int[3];
        for(int px=x+10;px<x+width-10;px++) {
            Color colour=new Color(source.getRGB(px,y),true);
            sum[0]+=colour.getRed();sum[1]+=colour.getGreen();sum[2]+=colour.getBlue();
        }
        for(int i=0;i<3;i++)sum[i]/=width-20;
        return sum;
    }
    private static BufferedImage buttonSnapshot(WButton button) {
        BufferedImage result=new BufferedImage(button.width,button.height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D copy=result.createGraphics();
        copy.drawImage(canvas,0,0,button.width,button.height,button.x,button.y,button.x+button.width,button.y+button.height,null);
        copy.dispose();return result;
    }
    private static void verifyHorizontalScaling() {
        ChamomiloSkinnedButton button=new ChamomiloSkinnedButton("DOWNLOAD",null,108);
        button.setPosition(30,720);
        for(int state=0;state<4;state++) {
            button.setEnabled(state!=3);button.hovered=state==1;button.isDown=state==2;
            button.setSize(108,28);
            rect(.105f,.097f,.079f,1f,30,720,400,28);button.render(null,1f);
            BufferedImage narrow=buttonSnapshot(button);
            button.setSize(320,28);
            check(button.width==320 && button.height==28,"Reusable button accepts a wider layout");
            rect(.105f,.097f,.079f,1f,30,720,400,28);button.render(null,.2f);
            BufferedImage wide=buttonSnapshot(button);
            for(int y=0;y<28;y++)for(int x=0;x<7;x++) {
                check(narrow.getRGB(x,y)==wide.getRGB(x,y),"Left corners are unchanged when stretched, including HUD opacity");
                check(narrow.getRGB(107-x,y)==wide.getRGB(319-x,y),"Right corners are unchanged when stretched");
            }
        }
        button.setEnabled(true);button.isDown=false;button.hovered=false;
        button.setLabel("A longer reusable button label");
        check(button.width>=button.text.getWidth(button.getLabel())+22,"Changed labels retain face padding");
        verifyButtonArtwork(button);
    }
    private static void renderButtonExamples(File output) throws Exception {
        rect(.070f,.062f,.049f,1f,0,0,800,270);
        int y=16;
        for(int width:new int[]{88,108,180,300}) {
            ChamomiloSkinnedButton button=new ChamomiloSkinnedButton(width==88?"Close":"DOWNLOAD",null,width);
            button.setPosition(16,y);button.render(null,1f);
            graphics.setColor(new Color(.87f,.85f,.75f));graphics.drawString(width+" x "+button.height,340,y+19);
            y+=40;
        }
        String[] labels={"Normal","Hover","Pressed","Disabled"};
        for(int state=0;state<4;state++) {
            ChamomiloSkinnedButton button=new ChamomiloSkinnedButton("DOWNLOAD",null,180);
            button.setPosition(16+state*195,198);
            button.setEnabled(state!=3);button.hovered=state==1;button.isDown=state==2;
            button.render(null,1f);
            graphics.setColor(new Color(.87f,.85f,.75f));graphics.drawString(labels[state],button.x,249);
        }
        ImageIO.write(canvas.getSubimage(0,0,800,270),"png",output);
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static boolean clip(int x,int y,int w,int h){clips.push(graphics.getClip()==null?new Rectangle(0,0,1000,900):graphics.getClip());graphics.clipRect(x,y,w,h);return w>0&&h>0;}
    public static void unclip(){graphics.setClip(clips.pop());}
    public static void rect(float r,float g,float b,float a,int x,int y,int w,int h){graphics.setColor(new Color(r,g,b,a));graphics.fillRect(x,y,w,h);}
    public static com.wurmonline.client.resources.textures.ResourceTexture buttonTexture() throws Exception {
        if(buttonTexture==null)buttonTexture=(com.wurmonline.client.resources.textures.ResourceTexture)allocate(com.wurmonline.client.resources.textures.ResourceTexture.class);
        return buttonTexture;
    }
    public static void texture(com.wurmonline.client.resources.textures.Texture texture,float tint,float x,float y,float w,float h,float u0,float v0,float u1,float v1){
        BufferedImage source=artwork;
        if(texture==buttonTexture){
            check(v1>v0,"Original button artwork is never flipped");
            check(Math.round(v0*buttonArtwork.getHeight())==96 && Math.round(v1*buttonArtwork.getHeight())==624,
                    "Every strip contains the complete original upper and lower rails");
            int key=Math.round(tint*100);
            source=tintedButtons.get(key);
            if(source==null){source=new java.awt.image.RescaleOp(new float[]{tint,tint,tint,1f},new float[4],null).filter(buttonArtwork,null);tintedButtons.put(key,source);}
        }
        graphics.drawImage(source,(int)x,(int)y,(int)(x+w),(int)(y+h),Math.round(u0*source.getWidth()),Math.round(v0*source.getHeight()),Math.round(u1*source.getWidth()),Math.round(v1*source.getHeight()),null);
    }
    public static void button(WButton button){
        rect(.27f,.24f,.18f,1f,button.x,button.y,button.width,button.height);
        rect(.15f,.14f,.11f,1f,button.x+1,button.y+1,button.width-2,button.height-2);
        button.text.moveTo(button.x+(button.width-button.text.getWidth(button.label))/2,button.y+(button.height-button.text.getHeight())/2+button.text.getAscent());
        button.text.paint(null,button.label,button.isEnabled()?.94f:.46f,button.isEnabled()?.88f:.46f,button.isEnabled()?.70f:.42f,1f);
    }
    public static final class ProbeFont extends TextFont {
        private final Font font;
        private int x,y;
        public ProbeFont(String name){font=new Font("Verdana",name.equals("bold")?Font.BOLD:Font.PLAIN,fontSize);}
        public void moveTo(int x,int y){this.x=x;this.y=y;}
        public int paint(Queue queue,String value,float r,float g,float b,float a){paintedLabels.put(value,new Point(x,y));graphics.setFont(font);graphics.setColor(new Color(r,g,b,a));graphics.drawString(value,x,y);return getWidth(value);}
        private FontMetrics metrics(){return graphics.getFontMetrics(font);}
        public int getWidth(String value){return metrics().stringWidth(value);}
        public int getWidth(char[] value,int start,int length){return metrics().charsWidth(value,start,length);}
        public int getHeight(){return metrics().getHeight();}
        public int getAscent(){return metrics().getAscent();}
        public int getDescent(){return metrics().getDescent();}
        public int getLeading(){return metrics().getLeading();}
    }
}
