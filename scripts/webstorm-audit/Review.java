package review;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.editor.colors.*;
import com.intellij.openapi.editor.highlighter.*;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.testFramework.LightVirtualFile;
import com.craftzdog.solarizedosaka.ThemeCatalog;
import com.craftzdog.solarizedosaka.actions.*;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import org.jetbrains.plugins.textmate.TextMateService;
import org.jetbrains.plugins.textmate.configuration.TextMateUserBundlesSettings;
import java.awt.Color;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.io.*;
import java.util.regex.*;
import javax.swing.Timer;

public class Review implements StartupActivity.DumbAware {
    record Sample(String filename, String pattern, String source, String fixture, String content) {}
    record Span(int count, int different, int palette, int low, String firstLow, String engine) {}
    interface AuditStep { void run() throws Exception; }
    record PendingPaletteCheck(BooleanSupplier lightReady, AuditStep verifyLight, BooleanSupplier unrelatedReady) {}
    static String q(Object value) {return '"'+String.valueOf(value).replace("\"", "\"\"").replace("\n"," ")+'"';}
    static String csv(Object...values) {return java.util.Arrays.stream(values).map(Review::q).collect(java.util.stream.Collectors.joining(","));}
    static double lin(int x) {double n=x/255.0;return n<=.04045?n/12.92:Math.pow((n+.055)/1.055,2.4);}
    static double lum(Color c) {return .2126*lin(c.getRed())+.7152*lin(c.getGreen())+.0722*lin(c.getBlue());}
    static double ratio(Color a,Color b) {double l=lum(a),m=lum(b);return (Math.max(l,m)+.05)/(Math.min(l,m)+.05);}
    static String hex(Color c) {return String.format("#%06x",c.getRGB()&0xffffff);}
    static Map<String,String> encoded(Path src) throws Exception {
        var map = new HashMap<String,String>();
        for(String line:Files.readAllLines(src)) {var parts=line.split("\t",2);map.put(parts[0],new String(Base64.getDecoder().decode(parts[1]),java.nio.charset.StandardCharsets.UTF_8));}
        return map;
    }
    static String filename(String pattern) {return pattern.replace("*","osaka").replace("?","x");}
    static String fixtureKey(String name) {
        var lower=name.toLowerCase(Locale.ROOT);
        if (lower.equals("yarn.lock"))return "yarn.lock";
        if (lower.startsWith("dockerfile")||lower.startsWith("containerfile")) return "dockerfile";
        if (lower.contains("makefile"))return "makefile";
        if (lower.endsWith("go.mod")) return "go.mod";
        if (lower.startsWith(".env"))return "env";
        if (lower.matches(".*(?:tsconfig|jsconfig|package|settings).*\\.json.*")) return "json";
        if (lower.contains(".github")||lower.contains("compose")&&lower.contains("yaml"))return "yaml";
        return lower.substring(lower.lastIndexOf('.')+1);
    }
    static Span inspect(Project project, Sample sample, EditorColorsScheme scheme, FileType type, LightVirtualFile file) {
        if (type.isBinary()) return new Span(0,0,0,0,"","Binary");
        var highlighter=EditorHighlighterFactory.getInstance().createEditorHighlighter(file,scheme,project);
        highlighter.setText(sample.content);
        var it=highlighter.createIterator(0);
        int spans=0, distinct=0, low=0;
        var colors=new HashSet<String>();
        var warn="";
        while (!it.atEnd()) {
            int end=Math.min(sample.content.length(),it.getEnd());
            int start=Math.max(0,it.getStart());
            if (start<end && !sample.content.substring(start,end).isBlank()) {
                spans++;
                var attributes=it.getTextAttributes();
                Color ink=attributes!=null&&attributes.getForegroundColor()!=null?attributes.getForegroundColor():scheme.getDefaultForeground();
                Color surface=attributes!=null&&attributes.getBackgroundColor()!=null?attributes.getBackgroundColor():scheme.getDefaultBackground();
                colors.add(hex(ink));
                if(!ink.equals(scheme.getDefaultForeground()))distinct++;
                double r=ratio(ink,surface);
                if(r<4.5){
                    low++;
                    if(warn.isBlank()){
                        String key="";
                        try{key=String.join("+",Arrays.stream(it.getTextAttributesKeys()).map(TextAttributesKey::getExternalName).toList());}
                        catch(RuntimeException ignored){}
                        warn=key+" | "+sample.content.substring(start, Math.min(start+25,end))+" | "+hex(ink)+" "+hex(surface)+" "+String.format(Locale.ROOT,"%.2f",r);
                    }
                }
            }
            it.advance();
        }
        return new Span(spans,distinct,colors.size(),low,warn,highlighter.getClass().getSimpleName());
    }
    @Override public void runActivity(Project project) {
        var dir=Path.of(project.getBasePath()).getParent();
        if(Files.isDirectory(dir.resolve("optional-bundles"))) {
            try (var bundles=Files.list(dir.resolve("optional-bundles"))) {
                for(var bundle:bundles.filter(Files::isDirectory).toList())
                    TextMateUserBundlesSettings.getInstance().addBundle(bundle.toString(),bundle.getFileName().toString());
                TextMateService.getInstance().reloadEnabledBundles();
            }catch(Exception e){throw new RuntimeException(e);}
        }
        var timer=new Timer(7500,null);timer.setRepeats(false);
        timer.addActionListener(event->ApplicationManager.getApplication().invokeLater(()->{
            try {
                var paletteCheck = run(project, dir);
                afterThemeChange(dir, paletteCheck.lightReady(), () -> {
                    paletteCheck.verifyLight().run();
                    afterThemeChange(dir, paletteCheck.unrelatedReady(), () -> {
                        if (!paletteCheck.unrelatedReady().getAsBoolean())
                            throw new Error("Personal overlay survived a switch to an unrelated theme");
                        Files.writeString(dir.resolve("personal-palette.txt"), "Preview, cancel, apply, paired preset change, reset, undo, and unrelated preset change passed.\n");
                        menus(project, dir);
                        Files.writeString(dir.resolve("passed"), "Audit completed");
                        finish(dir, null);
                    });
                });
            } catch (Throwable error) { finish(dir, error); }
        }));timer.start();
    }

    private static void afterThemeChange(Path dir, BooleanSupplier ready, AuditStep step) {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        var check = new Timer(100, null);
        check.addActionListener(ignored -> {
            if (!ready.getAsBoolean() && System.nanoTime() < deadline) return;
            check.stop();
            try { step.run(); }
            catch (Throwable error) { finish(dir, error); }
        });
        check.start();
    }

    private static void finish(Path dir, Throwable error) {
        if (error != null) {
            try (var output = new PrintWriter(Files.newBufferedWriter(dir.resolve("failure")))) {
                error.printStackTrace(output);
            } catch (IOException ignored) {}
        }
        ApplicationManager.getApplication().invokeLater(() -> ApplicationManagerEx.getApplicationEx().exit(true, true));
    }

    private PendingPaletteCheck run(Project project, Path dir) throws Exception {
        var fixtures=encoded(dir.resolve("fixtures.tsv"));
        var generic=encoded(dir.resolve("default.tsv")).get("generic");
        var typeRegistry=FileTypeManager.getInstance();
        var patterns=new TreeMap<String,String>();
        for(var type:typeRegistry.getRegisteredFileTypes()){
            for(var association:typeRegistry.getAssociations(type)) patterns.put(association.getPresentableString(),"WebStorm:"+type.getName());
        }
        for(String line:Files.readAllLines(dir.resolve("additional.tsv"))){
            var cell=line.split("\t",2);
            patterns.merge(cell[0],"Candidate:"+cell[1],(a,b)->a+"; "+b);
        }
        patterns.put("osaka-unregistered-plain","Unknown sentinel");
        for(String pattern:List.of("*.ads","*.lhs","*.pp","*.r","*.rhistory","*.rprofile",".Rhistory",".Rprofile")) patterns.putIfAbsent(pattern,"Optional grammar probe");
        var samples=new ArrayList<Sample>();
        for(var e:patterns.entrySet()){
            var name=filename(e.getKey());
            var key=fixtureKey(name);
            String content=fixtures.get(key);
            samples.add(new Sample(name,e.getKey(),e.getValue(),content==null?"Generic probe":"Language sample",content==null?generic:content));
        }
        var manager=EditorColorsManager.getInstance();
        var schemes=new LinkedHashMap<String,EditorColorsScheme>();
        for(String name:Files.readAllLines(dir.resolve("schemes.txt"))) schemes.put(name,Objects.requireNonNull(manager.getScheme(name),name));
        semantic(project,dir,schemes);
        try(var writer=new PrintWriter(Files.newBufferedWriter(dir.resolve("highlighting.csv")))){
            writer.println(csv("Association","Filename","Registry source","Detected type","Fixture","Scheme","Engine","Nonspace spans","Styled spans","Distinct inks","Low contrast spans","First low contrast span","Error"));
            for(var sample:samples){
                var file=new LightVirtualFile(sample.filename,sample.content);
                var type=typeRegistry.getFileTypeByFile(file);
                for(var row:schemes.entrySet()){
                    try{
                        var v=inspect(project,sample,row.getValue(),type,file);
                        writer.println(csv(sample.pattern,sample.filename,sample.source,type.getName(),sample.fixture,row.getKey(),v.engine,v.count,v.different,v.palette,v.low,v.firstLow,""));
                    }catch(RuntimeException e){writer.println(csv(sample.pattern,sample.filename,sample.source,type.getName(),sample.fixture,row.getKey(),"",0,0,0,0,"",e.toString()));}
                }
            }
        }
        var catalog=ThemeCatalog.INSTANCE;
        var presets = new TreeMap<String, UIThemeLookAndFeelInfo>();
        // Inspect the IDE catalog in this audit plugin only. The distributed plugin never enumerates it.
        var installed = LafManager.getInstance().getInstalledThemes().iterator();
        installed.forEachRemaining(theme -> {
            if (theme.getId().startsWith("com.craftzdog.solarizedosaka.")) presets.put(theme.getName(), theme);
        });
        if (presets.size() != 48 || presets.keySet().stream().anyMatch(name -> name.contains("Glass")))
            throw new Error("Unexpected bundled theme catalog: " + presets.keySet());
        try(var writer=new PrintWriter(Files.newBufferedWriter(dir.resolve("themes.csv")))){
            writer.println(csv("Theme","Applied","Editor scheme","Tree contrast","Hover contrast","Menu contrast"));
            for(var preset : presets.values()){
                selectNativePreset(preset);
                if (!preset.getName().equals(catalog.activePresetName()))
                    throw new Error("The plugin missed the active preset " + preset.getName());
                String[] keys={"Tree.selectionForeground","Tree.selectionBackground","Tree.foreground","Tree.hoverBackground","MenuItem.foreground","MenuItem.background"};
                var values=Arrays.stream(keys).map(javax.swing.UIManager::getColor).toList();
                writer.println(csv(preset.getName(),true,manager.getGlobalScheme().getName(),valueRatio(values,0),valueRatio(values,2),valueRatio(values,4)));
            }
        }
        var prefs = project.getService(com.craftzdog.solarizedosaka.ProjectThemePreferences.class).getState();
        prefs.setEditorSchemeName("Solarized Osaka Quiet UI Light");
        project.getService(com.craftzdog.solarizedosaka.ProjectEditorSchemeService.class).restoreSavedScheme();
        var edits = FileEditorManager.getInstance(project);
        var editor = edits.getSelectedTextEditor();
        if (!(editor instanceof com.intellij.openapi.editor.ex.EditorEx ex) || !ex.getColorsScheme().getName().contains("Quiet UI Light"))
            throw new Error("Project editor scheme restoration did not apply the requested scheme");
        paletteButtons(dir, presets);
        return livePalette(dir, presets);
    }

    private static void selectNativePreset(UIThemeLookAndFeelInfo preset) {
        LafManager.getInstance().setCurrentUIThemeLookAndFeel(preset);
    }

    private PendingPaletteCheck livePalette(Path dir, Map<String, UIThemeLookAndFeelInfo> presets) {
        var service = PersonalThemes.Companion.instance();
        var manager = EditorColorsManager.getInstance();
        var dark = "Solarized Osaka";
        var light = "Solarized Osaka Light";
        var label = "Audit paired palette";
        var key = TextAttributesKey.find("JSON.PROPERTY_KEY");
        var darkKey = new Color(0xB6A4E0);
        var lightKey = new Color(0x584383);
        selectNativePreset(presets.get(dark));
        var base = manager.getGlobalScheme();
        var palette = new PersonalPalette(PersonalThemes.Companion.preset$solarized_osaka_intellij(dark));
        var colors = new HashMap<>(palette.getDefaults());
        colors.put("JSON key", darkKey);
        service.preview$solarized_osaka_intellij(palette, colors, false, 50);
        if (!darkKey.equals(manager.getGlobalScheme().getAttributes(key).getForegroundColor()))
            throw new Error("Personal preview did not apply the draft color");
        service.endPreview();
        if (manager.getGlobalScheme() != base) throw new Error("Preview did not restore the base scheme");
        service.apply$solarized_osaka_intellij(label, palette, colors, false, 50);
        var profile = service.getState().getProfiles().get(label);
        var lightOverrides = new PersonalThemes.ColorOverrides();
        lightOverrides.getValues().put("JSON key", hex(lightKey));
        profile.setLight(lightOverrides);
        var inactive = new PersonalPalette(PersonalThemes.Companion.preset$solarized_osaka_intellij(light));
        try {
            service.preview$solarized_osaka_intellij(inactive, inactive.getDefaults(), false, 50);
            throw new Error("Preview accepted a preset that is not active");
        } catch (IllegalStateException expected) {
            if (!expected.getMessage().contains(light)) throw expected;
        }
        selectNativePreset(presets.get(light));
        BooleanSupplier ready = () -> light.equals(profile.getPreset()) &&
            lightKey.equals(manager.getGlobalScheme().getAttributes(key).getForegroundColor());
        return new PendingPaletteCheck(ready, () -> {
            if (!ready.getAsBoolean())
                throw new Error("Personal palette did not follow the matching light preset. Preset=" + profile.getPreset() +
                    ", scheme=" + manager.getGlobalScheme().getName() + ", JSON key=" +
                    manager.getGlobalScheme().getAttributes(key).getForegroundColor());
            service.useBasePreset$solarized_osaka_intellij();
            if (lightKey.equals(manager.getGlobalScheme().getAttributes(key).getForegroundColor()))
                throw new Error("Use base preset did not clear the personal palette");
            service.undoLastApply();
            if (!lightKey.equals(manager.getGlobalScheme().getAttributes(key).getForegroundColor()))
                throw new Error("Undo did not recover the personal palette");
            selectNativePreset(presets.get("Solarized Osaka OLED"));
        }, () -> "Solarized Osaka OLED".equals(ThemeCatalog.INSTANCE.activePresetName()) &&
            service.getState().getActive().isEmpty() && !service.getCanUndo());
    }
    private void semantic(Project project, Path dir, Map<String, EditorColorsScheme> schemes) throws Exception {
        var editors=FileEditorManager.getInstance(project);
        var file=Arrays.stream(editors.getOpenFiles()).filter(it->it.getName().equals("palette.json")).findFirst().orElseThrow();
        var editor=Arrays.stream(editors.getAllEditors(file)).filter(TextEditor.class::isInstance).map(TextEditor.class::cast).findFirst().orElseThrow().getEditor();
        var document=editor.getDocument();
        var highlight=DaemonCodeAnalyzerImpl.getHighlights(document,null,project).stream()
            .filter(h->h.forcedTextAttributesKey != null && h.forcedTextAttributesKey.getExternalName().equals("JSON.PROPERTY_KEY"))
            .findFirst().orElse(null);
        var role=highlight==null ? "JSON.PROPERTY_KEY" : highlight.forcedTextAttributesKey.getExternalName();
        try(var writer=new PrintWriter(Files.newBufferedWriter(dir.resolve("json-semantic.csv")))){
            writer.println(csv("Scheme","Confirmed key annotation","Key ink","String ink","Distinct","Key contrast","String contrast"));
            for(var row:schemes.entrySet()){
                var scheme=row.getValue();
                var key=scheme.getAttributes(TextAttributesKey.find(role)).getForegroundColor();
                var string=scheme.getAttributes(TextAttributesKey.find("JSON.STRING")).getForegroundColor();
                writer.println(csv(row.getKey(),highlight!=null,hex(key),hex(string),!key.equals(string),
                    String.format(Locale.ROOT,"%.2f",ratio(key,scheme.getDefaultBackground())),
                    String.format(Locale.ROOT,"%.2f",ratio(string,scheme.getDefaultBackground()))));
            }
        }
    }

    private void paletteButtons(Path dir, Map<String, UIThemeLookAndFeelInfo> presets) throws Exception {
        try(var writer = new PrintWriter(Files.newBufferedWriter(dir.resolve("palette-buttons.csv")))){
            writer.println(csv("Theme","Palette buttons","Buttons with opaque white text","Filled button contrast"));
            for(String theme : List.of("Solarized Osaka", "Solarized Osaka Light",
                    "Solarized Osaka Islands", "Solarized Osaka Islands Light",
                    "Solarized Osaka Islands Quiet UI", "Solarized Osaka Islands Quiet UI Light")){
                selectNativePreset(presets.get(theme));
                var root = new com.craftzdog.solarizedosaka.settings.ThemeGalleryConfigurable().createComponent();
                var scheme = EditorColorsManager.getInstance().getGlobalScheme();
                var key = scheme.getAttributes(TextAttributesKey.find("JSON.PROPERTY_KEY")).getForegroundColor();
                var value = scheme.getAttributes(TextAttributesKey.find("JSON.STRING")).getForegroundColor();
                if (!key.equals(labelInk(root, "    \"theme\"")) ||
                    !value.equals(labelInk(root, "\"Solarized Osaka\"")))
                    throw new Error("Gallery preview differs from editor syntax colors for " + theme);
                var buttons = new ArrayList<javax.swing.JToggleButton>();
                collectPaletteButtons(root,buttons);
                int readable=0;
                for(var button:buttons){
                    button.setSize(180,38);
                    var image=new java.awt.image.BufferedImage(180,38,java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics();button.paint(graphics);graphics.dispose();
                    int white=0;
                    for(int y=5;y<33;y++)for(int x=32;x<174;x++) if((image.getRGB(x,y)&0xffffff)==0xffffff) white++;
                    if(white>4)readable++;
                }
                var ink=javax.swing.UIManager.getColor("Button.default.foreground");
                var bg=javax.swing.UIManager.getColor("Button.default.startBackground");
                var ratio=String.format(Locale.ROOT,"%.2f",ratio(ink,bg));
                writer.println(csv(theme,buttons.size(),readable,ratio));
                if(buttons.size()!=12||readable!=12||Double.parseDouble(ratio)<4.5)
                    throw new Error("Palette label regression for "+theme);
            }
        }
        var path=dir.resolve("import-old-glass.json");
        Files.writeString(path,"{\"format\":1,\"name\":\"Audit\",\"palette\":{\"preset\":\"Solarized Osaka Islands Glass Light\",\"colors\":{},\"frost\":75,\"light\":{\"values\":{\"JSON key\":\"#255c67\"}}}}");
        var migrated=PaletteFiles.INSTANCE.read(path).getPalette();
        if(!migrated.getPreset().equals("Solarized Osaka Islands Quiet UI Light") ||
            !"#255c67".equals(migrated.getLight().getValues().get("JSON key")) ||
            !PersonalThemes.Companion.instance().resolveSchemeName("Solarized Osaka Glass").equals("Solarized Osaka Quiet UI")){
            throw new Error("Removed palette data did not migrate");
        }
    }

    private static Color labelInk(java.awt.Component component, String text) {
        if (component instanceof javax.swing.JLabel label && text.equals(label.getText()))
            return label.getForeground();
        if (component instanceof java.awt.Container container) {
            for (var child : container.getComponents()) {
                var ink = labelInk(child, text);
                if (ink != null) return ink;
            }
        }
        return null;
    }

    private void collectPaletteButtons(java.awt.Component component, List<javax.swing.JToggleButton> buttons) {
        if(component instanceof javax.swing.JToggleButton button &&
            button.getClass().getSimpleName().equals("PaletteButton")) buttons.add(button);
        if(component instanceof java.awt.Container node)
            for(var child:node.getComponents()) collectPaletteButtons(child,buttons);
    }

    private void menus(Project project, Path dir) throws Exception {
        var actions=ActionManager.getInstance();
        String base="com.craftzdog.solarizedosaka.";
        try(var output=new PrintWriter(Files.newBufferedWriter(dir.resolve("menus.txt")))){
            var tools=(ActionGroup)actions.getAction("ToolsMenu");
            var osaka=(ActionGroup)actions.getAction(base+"Menu");
            var help=(ActionGroup)actions.getAction("HelpMenu");
            if(!Arrays.stream(tools.getChildren(null)).anyMatch(a->a==osaka)) throw new Error("Osaka submenu missing");
            for(String id:List.of("UsageGuide","Changelog")){
                var action=actions.getAction(base+id);
                if(!Arrays.stream(help.getChildren(null)).anyMatch(a->a==action || (base+id).equals(actions.getId(a)))) throw new Error("Missing from Help: "+id);
                if(!Arrays.stream(osaka.getChildren(null)).anyMatch(a->a==action || (base+id).equals(actions.getId(a)))) throw new Error("Missing from Tools: "+id);
            }
            for(String id:List.of("UsageGuide","Changelog","OpenPreview")){
                var action=actions.getAction(base+id);
                var opened=new java.util.concurrent.atomic.AtomicBoolean();
                var click=new Timer(240,null);
                click.addActionListener(event->{
                    for(var w:java.awt.Window.getWindows()){
                        if(w instanceof javax.swing.JDialog d && d.isShowing() &&
                           (d.getTitle().contains("Usage Guide")||d.getTitle().contains("Changelog")||d.getTitle().contains("Syntax Preview"))){
                            var child=locate(d,id.equals("OpenPreview")?"Open sample":"Close");
                            if(child!=null){
                                opened.set(true);
                                if(id.equals("UsageGuide"))capture(d,dir.resolve("usage-guide.png"));
                                if(id.equals("OpenPreview"))capture(d,dir.resolve("syntax-picker.png"));
                                click.stop();child.doClick();return;
                            }
                        }
                    }
                });
                click.start();
                var context=(DataContext)(key->CommonDataKeys.PROJECT.getName().equals(key)?project:null);
                action.actionPerformed(AnActionEvent.createFromAnAction(action,null,"Audit",context));
                click.stop();
                if(!opened.get())throw new Error("Dialog did not open: "+id);
                if(id.equals("OpenPreview")) {
                    var names=Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles())
                        .map(it->it.getName()).filter(name->name.startsWith("OsakaPreview")).toList();
                    if(!names.equals(List.of("OsakaPreview.json"))) throw new Error("Unexpected preview tabs: "+names);
                }
                output.println(id+" opened and confirmed");
            }
            var appearance = actions.getAction(base + "SwitchBrightness");
            var existingWindows = List.of(java.awt.Window.getWindows());
            var themeControl = new java.util.concurrent.atomic.AtomicBoolean();
            var loop = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            var timeout = new Timer(10000, event -> {
                for (var window : java.awt.Window.getWindows())
                    if (window.isShowing() && !existingWindows.contains(window))
                        window.dispatchEvent(new java.awt.event.WindowEvent(window, java.awt.event.WindowEvent.WINDOW_CLOSING));
                loop.exit();
            });
            timeout.setRepeats(false);
            var check = new Timer(100, event -> {
                for (var window : java.awt.Window.getWindows()) {
                    if (!window.isShowing() || !hasThemePicker(window)) continue;
                    themeControl.set(true);
                    capture(window, dir.resolve("appearance-picker.png"));
                    window.dispatchEvent(new java.awt.event.WindowEvent(window, java.awt.event.WindowEvent.WINDOW_CLOSING));
                    loop.exit();
                    return;
                }
            });
            var context = (DataContext)(key -> CommonDataKeys.PROJECT.getName().equals(key) ? project : null);
            timeout.start();
            check.start();
            appearance.actionPerformed(AnActionEvent.createFromAnAction(appearance, null, "Audit", context));
            if (!themeControl.get() && timeout.isRunning()) loop.enter();
            check.stop();
            timeout.stop();
            if (!themeControl.get()) throw new Error("The Choose Preset action did not open the native Appearance theme picker");
            output.println("Choose Preset opened the native Appearance theme picker");
        }
    }

    private static boolean hasThemePicker(java.awt.Component component) {
        if (component.isShowing() && component instanceof javax.swing.JComboBox<?> combo) {
            for (int i = 0; i < combo.getItemCount(); i++)
                if (String.valueOf(combo.getItemAt(i)).contains("Solarized Osaka")) return true;
        }
        if (component instanceof java.awt.Container node)
            for (var child : node.getComponents()) if (hasThemePicker(child)) return true;
        return false;
    }
    static void capture(java.awt.Window window,Path file){
        try{
            var image=new java.awt.image.BufferedImage(window.getWidth(),window.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics=image.createGraphics();window.paintAll(graphics);graphics.dispose();
            javax.imageio.ImageIO.write(image,"png",file.toFile());
        }catch(Exception ignored){}
    }
    static javax.swing.JButton locate(java.awt.Component root,String label){
        if(root instanceof javax.swing.JButton button && label.equals(button.getText()))return button;
        if(root instanceof java.awt.Container group)for(var child:group.getComponents()){var found=locate(child,label);if(found!=null)return found;}
        return null;
    }
    static String valueRatio(java.util.List<Color> values,int i){return values.get(i)==null||values.get(i+1)==null?"Unavailable":String.format(Locale.ROOT,"%.2f",ratio(values.get(i),values.get(i+1)));}
}
