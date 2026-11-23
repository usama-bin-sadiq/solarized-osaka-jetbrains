package com.craftzdog.solarizedosaka.actions

import com.craftzdog.solarizedosaka.ThemeCatalog
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets
import com.intellij.util.ui.JBUI
import java.awt.event.ActionEvent
import javax.swing.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

private fun resource(path: String) = requireNotNull(CustomizeAction::class.java.getResourceAsStream(path)) { path }
internal fun json(path: String) = resource(path).bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
internal fun hex(value: Color) = "#%06x".format(value.rgb and 0xffffff)
internal class PersonalPalette(val source: JsonObject) {
    val name: String get() = source["name"].asString
    val dark: Boolean get() = source["dark"].asBoolean
    private val schemePath = source["editorScheme"].asString
    private val scheme by lazy {
        resource(schemePath).use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
    }
    private val xpath = XPathFactory.newInstance().newXPath()
    private fun token(key: String, property: String = "FOREGROUND") =
        "/scheme/attributes/option[@name='$key']/value/option[@name='$property']"
    private val syntax = linkedMapOf("Code text" to "TEXT", "JSON key" to "JSON.PROPERTY_KEY", "JSON string" to "JSON.STRING", "Comment" to "DEFAULT_LINE_COMMENT")
    private val selectionRoles = linkedMapOf(
        "Editor selection" to "Editor.selectionBackground",
        "Editor selected text" to "Editor.selectionForeground",
        "Project tree selection" to "Tree.selectionBackground",
        "Project tree selected text" to "Tree.selectionForeground",
        "Menu highlight" to "MenuItem.selectionBackground",
        "Menu highlighted text" to "MenuItem.selectionForeground",
    )

    fun selectionColor(role: String, colors: Map<String, Color>): Color? {
        val color = colors.getValue(role)
        if (color != defaults[role]) return color
        val sharedRole = if (role.endsWith("text")) "Selected text" else "Selection"
        return colors[sharedRole]?.takeIf { it != defaults[sharedRole] }
    }

    val defaults: Map<String, Color> = buildMap {
        val ui = source.getAsJsonObject("ui")
        for ((label,key) in listOf("Editor" to "Editor.background","Directory hover" to "Tree.hoverBackground","Selection" to "Tree.selectionBackground","Selected text" to "Tree.selectionForeground","Menu" to "Menu.background","Menu text" to "Menu.foreground","Focus" to "Focus.borderColor"))
            put(label,Color.decode(ui[key].asString))
        for ((label, key) in selectionRoles) put(label, Color.decode(ui[key].asString))
        val doc = scheme
        for ((label,key) in syntax) put(label,Color.decode("#" + xpath.evaluate(token(key) + "/@value",doc)))
    }

    val accentTokens: List<String> by lazy {
        val nodes = scheme.getElementsByTagName("attributes").item(0).childNodes
        (0 until nodes.length).mapNotNull { (nodes.item(it) as? org.w3c.dom.Element)?.getAttribute("name") }
            .filter { name ->
                listOf("KEYWORD", "STRING", "NUMBER", "FUNCTION", "CLASS_NAME", "PROPERTY_KEY", "TYPE").any(name::contains) &&
                    listOf("ERROR", "WARNING", "INVALID", "DEPRECATED", "COMMENT", "CONSOLE", "DIFF", "ESCAPE").none(name::contains)
            }
    }

    fun uiOverrides(colors: Map<String,Color>, quiet: Boolean = false, accent: Int = 50): Map<String,Color> = buildMap {
        val ui = source.getAsJsonObject("ui")
        if (quiet) {
            val surface = get("Panel.background") ?: Color.decode(ui["Panel.background"].asString)
            val chromeKeys = listOf("MainToolbar.background", "ToolBar.background", "EditorTabs.background",
                "EditorTabs.borderColor", "ToolWindow.Header.inactiveBackground", "ToolWindow.Header.borderColor",
                "StatusBar.borderColor")
            for (key in chromeKeys) {
                val base = get(key) ?: ui[key]?.asString?.let(Color::decode) ?: continue
                put(key, PaletteContrast.mix(base, surface, 55))
            }
        }
        if (accent != 50) {
            val panel = get("Panel.background") ?: Color.decode(ui["Panel.background"].asString)
            for (key in ui.keySet()) {
                if (key !in listOf("Focus.borderColor", "Component.focusedBorderColor", "Button.focusedBorderColor",
                        "TabbedPane.focusColor", "EditorTabs.underlineColor", "Link.activeForeground", "Link.hoverForeground")) continue
                val base = get(key) ?: Color.decode(ui[key].asString)
                val tinted = PaletteContrast.accent(base, accent)
                put(key, if (key.startsWith("Link.")) PaletteContrast.nearbyShade(tinted, listOf(panel)) ?: tinted else tinted)
            }
        }
        for (key in ui.keySet().toList()) {
            val selectionScope = when (key.substringBefore('.')) {
                "Editor" -> "Editor"
                "Tree" -> "Project tree"
                "Menu", "MenuBar", "MenuItem", "PopupMenu", "CheckBoxMenuItem", "RadioButtonMenuItem" -> "Menu"
                else -> null
            }
            val ink = key.endsWith("selectionForeground", true) || key.endsWith("acceleratorSelectionForeground")
            if (selectionScope != null && (ink || key.endsWith("selectionBackground", true))) {
                val role = if (selectionScope == "Menu") {
                    if (ink) "Menu highlighted text" else "Menu highlight"
                } else if (ink) "$selectionScope selected text" else "$selectionScope selection"
                selectionColor(role, colors)?.let { put(key, it) }
                continue
            }
            val role = when {
                key in setOf("Editor.background","Editor.Gutter.background") -> "Editor"
                key == "Editor.foreground" -> "Code text"
                key in setOf("Tree.hoverBackground","Tree.hoverInactiveBackground","List.hoverBackground") -> "Directory hover"
                key.endsWith("selectionBackground",true) -> "Selection"
                key.endsWith("selectionForeground",true) || key.endsWith("acceleratorSelectionForeground") -> "Selected text"
                key.substringBefore('.') in setOf("Menu","MenuBar","MenuItem","PopupMenu","CheckBoxMenuItem","RadioButtonMenuItem") && key.endsWith(".background") -> "Menu"
                key.substringBefore('.') in setOf("Menu","MenuBar","MenuItem","PopupMenu","CheckBoxMenuItem","RadioButtonMenuItem") && key.endsWith(".foreground") -> "Menu text"
                key in setOf("Focus.borderColor","Component.focusedBorderColor","Button.focusedBorderColor","TabbedPane.focusColor") -> "Focus"
                else -> continue
            }
            if (colors.getValue(role) != defaults.getValue(role)) put(key,colors.getValue(role))
        }
    }

    fun syntaxColor(role: String, colors: Map<String, Color>, accent: Int): Color {
        val base = colors.getValue(role)
        if (accent == 50 || syntax[role] !in accentTokens || base != defaults[role]) return base
        val tinted = PaletteContrast.accent(base, accent)
        return PaletteContrast.nearbyShade(tinted, listOf(colors.getValue("Editor"))) ?: tinted
    }

    fun contrastPairs(colors: Map<String, Color>, accent: Int = 50): List<PaletteContrast.Pairing> {
        val ui = source.getAsJsonObject("ui")
        val overrides = uiOverrides(colors, accent = accent)
        fun value(key: String, fallback: Color): Color =
            overrides[key] ?: ui[key]?.asString?.let(Color::decode) ?: fallback
        return buildList {
            val treeText = value("Tree.foreground", colors.getValue("Code text"))
            val menuText = value("Menu.foreground", colors.getValue("Menu text"))
            val menu = value("Menu.background", colors.getValue("Menu"))
            add(PaletteContrast.Pairing("Folder label", treeText, value("Tree.background", colors.getValue("Editor"))))
            add(PaletteContrast.Pairing("Hovered folder", treeText, colors.getValue("Directory hover")))
            add(PaletteContrast.Pairing("Selected folder", value("Tree.selectionForeground", colors.getValue("Selected text")),
                value("Tree.selectionBackground", colors.getValue("Selection")), "Project tree selected text"))
            add(PaletteContrast.Pairing("Inactive folder selection", value("Tree.selectionInactiveForeground", treeText),
                value("Tree.selectionInactiveBackground", colors.getValue("Directory hover"))))
            add(PaletteContrast.Pairing("Menu label", menuText, menu, "Menu text"))
            val menuItem = value("MenuItem.background", menu)
            add(PaletteContrast.Pairing("Menu shortcut", value("MenuItem.acceleratorForeground", menuText), menuItem))
            add(PaletteContrast.Pairing("Highlighted shortcut",
                value("MenuItem.acceleratorSelectionForeground", colors.getValue("Selected text")),
                value("MenuItem.selectionBackground", menuItem), "Menu highlighted text"))
            add(PaletteContrast.Pairing("Selected code", value("Editor.selectionForeground", colors.getValue("Selected text")),
                value("Editor.selectionBackground", colors.getValue("Selection")), "Editor selected text"))
            for (role in syntax.keys) {
                add(PaletteContrast.Pairing(role, syntaxColor(role, colors, accent), colors.getValue("Editor"), role))
            }
        }
    }

    fun adaptInherited(colors: Map<String, Color>): Map<String, Color> {
        val adapted = colors.toMutableMap()
        for (role in defaults.keys) {
            val pairs = contrastPairs(adapted).filter { it.textRole == role }
            if (pairs.none { it.ratio < 4.5 }) continue
            val ink = pairs.first().foreground
            val shade = PaletteContrast.nearbyShade(ink, pairs.map { it.background })
            if (shade != null) adapted[role] = shade
        }
        val hover = contrastPairs(adapted).firstOrNull { it.label == "Hovered folder" }
        if (hover != null && hover.ratio < 4.5 && adapted["Directory hover"] != defaults["Directory hover"]) {
            adapted["Directory hover"] = PaletteContrast.nearbyShade(hover.background, listOf(hover.foreground))
                ?: defaults.getValue("Directory hover")
        }
        return adapted
    }

    fun draftColor(role: String, colors: Map<String, Color>): Pair<Color, String> {
        val current = colors.getValue(role)
        if (current != defaults[role]) return current to "Personal"
        if (role in selectionRoles) selectionColor(role, colors)?.let { return it to "Shared selection" }
        return current to "Preset"
    }

    val schemeName: String get() = scheme.documentElement.getAttribute("name")
}

class CustomizeAction : AnAction(), DumbAware {
    private var openDialog: DialogWrapper? = null


    override fun actionPerformed(event: AnActionEvent) {
        openDialog?.let {
            it.window?.toFront()
            return
        }
        val presets = PersonalThemes.presets().associateBy { it["name"].asString }
        val chooser = JComboBox(presets.keys.sorted().toTypedArray())
        ThemeCatalog.activePresetName()?.let { chooser.selectedItem = it }
        val service = PersonalThemes.instance()
        val saved = JComboBox((listOf("New personal theme") + service.state.profiles.keys.sorted()).toTypedArray())
        val name = JTextField("My palette")
        val fields = JPanel(GridBagLayout())
        fun addRow(label: JLabel, control: JComponent) {
            label.labelFor = control
            val row = fields.componentCount / 2
            fields.add(label, GridBagConstraints().apply {
                gridy = row
                anchor = GridBagConstraints.LINE_START
                insets = Insets(3, 0, 3, 10)
            })
            fields.add(control, GridBagConstraints().apply {
                gridx = 1
                gridy = row
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(3, 0, 3, 8)
            })
        }
        val panel = JPanel(BorderLayout(8,8))
        val colors = linkedMapOf<String,Color>()
        var palette = PersonalPalette(presets.getValue(chooser.selectedItem as String))
        val live = JCheckBox("Preview in IDE")
        val activation = JButton()
        activation.addActionListener { ThemeCatalog.openAppearance(event.project) }
        fun refreshAvailability() {
            val isActive = ThemeCatalog.activePresetName() == palette.name
            if (!isActive && live.isSelected) {
                live.isSelected = false
                service.endPreview()
            }
            live.isEnabled = isActive
            activation.text = "Choose “${palette.name}” in Appearance…"
            activation.isVisible = !isActive
            openDialog?.isOKActionEnabled = isActive
        }
        val quiet = JCheckBox("Quiet chrome")
        val accent = JSlider(0, 100, 50).apply {
            accessibleContext.accessibleName = "Accent strength"
            labelTable = createStandardLabels(50).apply {
                put(0, JLabel("Soft")); put(50, JLabel("Preset")); put(100, JLabel("Vivid"))
            }
            paintLabels = true
        }
        val update = Timer(160) {
            if (live.isSelected) {
                runCatching { service.preview(palette, colors, quiet.isSelected, accent.value) }.onFailure {
                    live.isSelected = false
                    service.endPreview()
                    Messages.showErrorDialog(event.project, it.message ?: "Could not preview the palette.", "Customize Solarized Osaka")
                }
            }
        }.apply { isRepeats = false }
        val contrastSummary = JLabel()
        val contrastButton = JButton("Check contrast")
        val colorButtons = mutableMapOf<String, JButton>()
        fun draftChanged() {
            refreshAvailability()
            val pairs = palette.contrastPairs(colors, accent.value)
            val weak = pairs.count { it.ratio < 4.5 }
            contrastSummary.text = if (weak == 0) "Checked text meets 4.5:1" else "$weak text pairs below 4.5:1"
            for ((role, button) in colorButtons) {
                val (color, source) = palette.draftColor(role, colors)
                val effectiveColor = palette.syntaxColor(role, colors, accent.value)
                val tinted = effectiveColor != colors.getValue(role)
                button.text = hex(if (tinted) effectiveColor else color) + " · " + if (tinted) "Accent strength" else source
                button.toolTipText = pairs.filter { it.textRole == role }.minOfOrNull { it.ratio }
                    ?.let { "Lowest contrast %.2f:1".format(it) }
            }
            panel.repaint()
            if (live.isSelected) update.restart()
        }
        contrastButton.addActionListener {
            val pairs = palette.contrastPairs(colors, accent.value)
            val rows = JPanel(GridLayout(0, 3, 10, 4))
            for (pair in pairs) {
                rows.add(JLabel(pair.label))
                rows.add(JLabel("%.2f:1".format(pair.ratio)))
                val role = pair.textRole
                val surfaces = pairs.filter { it.textRole == role }.map { it.background }
                val hint = if (role != null && pair.ratio < 4.5) PaletteContrast.nearbyShade(pair.foreground, surfaces) else null
                if (hint != null && role != null) rows.add(JButton("Use " + hex(hint)).apply {
                    addActionListener {
                        colors[role] = hint
                        colorButtons[role]?.text = hex(hint)
                        draftChanged()
                        SwingUtilities.getWindowAncestor(rows)?.dispose()
                    }
                }) else rows.add(JLabel(if (pair.ratio < 4.5) "Low contrast" else "Readable"))
            }
            JOptionPane.showMessageDialog(panel, rows, "Text contrast", JOptionPane.PLAIN_MESSAGE)
        }
        accent.addChangeListener { draftChanged() }
        quiet.addActionListener { draftChanged() }
        live.addActionListener {
            if (live.isSelected) draftChanged() else {
                update.stop()
                service.endPreview()
            }
        }
        fun refresh(overrides: Map<String,String> = emptyMap(), quietChrome: Boolean = false, accentStrength: Int = 50) {
            palette = PersonalPalette(presets.getValue(chooser.selectedItem as String))
            colors.clear(); colors.putAll(palette.defaults)
            for ((role,value) in overrides) if (role in colors) colors[role]=Color.decode(value)
            fields.removeAll()
            colorButtons.clear()
            addRow(JLabel("Saved palettes"), saved)
            addRow(JLabel("Base preset"), chooser)
            addRow(JLabel("Personal theme name"), name)
            quiet.isSelected = quietChrome
            accent.value = accentStrength.coerceIn(0, 100)
            addRow(JLabel("Accent strength"), accent)
            for (role in colors.keys) {
                val button = JButton(hex(colors.getValue(role)))
                colorButtons[role] = button
                button.addActionListener {
                    JColorChooser.showDialog(panel,role,palette.draftColor(role, colors).first)?.let {
                        colors[role]=it; button.text=hex(it); draftChanged()
                    }
                }
                val reset = JButton("Reset").apply {
                    accessibleContext.accessibleName = "Reset $role"
                    addActionListener {
                        colors[role] = palette.defaults.getValue(role)
                        draftChanged()
                    }
                }
                val label = JLabel(role)
                addRow(label, JPanel(BorderLayout(4, 0)).apply {
                    add(button, BorderLayout.CENTER)
                    add(reset, BorderLayout.EAST)
                })
                label.labelFor = button
            }
            fields.revalidate(); draftChanged()
        }
        panel.add(JScrollPane(fields).apply {
            border = BorderFactory.createEmptyBorder()
            preferredSize = JBUI.size(500, 440)
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = JBUI.scale(24)
        }, BorderLayout.CENTER)
        fun refreshSavedDraft() {
            val profile = service.state.profiles[saved.selectedItem]
            val selected = PersonalPalette(presets.getValue(chooser.selectedItem as String))
            if (profile == null || (profile.preset in presets && !service.sameFamily(profile.preset, selected.name))) {
                refresh()
                return
            }
            refresh(service.colorsFor(profile, selected).mapValues { (_, color) -> hex(color) },
                profile.quietChrome, profile.accentStrength)
        }
        chooser.addActionListener { refreshSavedDraft() }
        saved.addActionListener {
            val profile = service.state.profiles[saved.selectedItem]
            name.text = if (profile != null) saved.selectedItem.toString() else "My palette"
            profile?.let {
                val active = ThemeCatalog.activePresetName()
                if (active != null && service.sameFamily(it.preset, active)) chooser.selectedItem = active
                else if (it.preset in presets) chooser.selectedItem = it.preset
                else {
                    active?.let { replacement -> chooser.selectedItem = replacement }
                    Messages.showInfoMessage(event.project, "The previous base preset is unavailable. Choose a replacement and apply the saved colors.", "Recover Saved Palette")
                }
            }
            refreshSavedDraft()
        }
        val manage = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 4, 0))
        fun selectedSaved() = (saved.selectedItem as? String)?.takeIf { it in service.state.profiles }
        fun updateSaved(label: String? = null) {
            saved.model = DefaultComboBoxModel((listOf("New personal theme") + service.state.profiles.keys.sorted()).toTypedArray())
            saved.selectedItem = label ?: "New personal theme"
            if (label == null) {
                name.text = "My palette"
                refresh()
            }
        }
        fun manageButton(label: String, operation: () -> Unit) {
            manage.add(JButton(label).apply {
                addActionListener {
                    update.stop()
                    live.isSelected = false
                    service.endPreview()
                    runCatching(operation).onFailure {
                        Messages.showErrorDialog(event.project, it.message ?: "Could not update the saved palette.", "Saved palettes")
                    }
                }
            })
        }
        fun namePrompt(title: String, initial: String): String? =
            JOptionPane.showInputDialog(panel, title, initial)?.let(PaletteFiles::validName)

        manageButton("Rename") {
            val old = selectedSaved() ?: error("Choose a saved palette to rename.")
            val label = namePrompt("Name this palette", old)
            if (label != null && label != old) {
                service.renameProfile(old, label)
                updateSaved(label)
            }
        }
        manageButton("Duplicate") {
            val old = selectedSaved() ?: error("Choose a saved palette to duplicate.")
            namePrompt("Name the copy", "$old copy")?.let { label ->
                service.duplicateProfile(old, label)
                updateSaved(label)
            }
        }
        manageButton("Delete") {
            val label = selectedSaved() ?: error("Choose a saved palette to delete.")
            if (JOptionPane.showConfirmDialog(panel, "Delete the saved palette “$label”?", "Saved palettes",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                service.deleteProfile(label)
                updateSaved()
            }
        }
        manageButton("Import") {
            val chooser = JFileChooser()
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                val file = PaletteFiles.read(chooser.selectedFile.toPath())
                val label = if (file.name in service.state.profiles) namePrompt("Save imported palette as", file.name + " copy") else file.name
                if (label != null) {
                    require(label !in service.state.profiles) { "A saved palette already uses that name." }
                    service.state.profiles[label] = file.palette
                    updateSaved(label)
                }
            }
        }
        manageButton("Export") {
            val label = selectedSaved() ?: error("Choose a saved palette to export.")
            val chooser = JFileChooser().apply { selectedFile = java.io.File("osaka-palette.json") }
            if (chooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                val target = if (file.name.endsWith(".json", true)) file else java.io.File(file.parentFile, file.name + ".json")
                PaletteFiles.write(target.toPath(), label, service.state.profiles.getValue(label))
            }
        }
        panel.add(JPanel(BorderLayout()).apply {
            add(manage, BorderLayout.NORTH)
            add(activation, BorderLayout.SOUTH)
        }, BorderLayout.NORTH)
        refresh()
        if (service.state.active.isNotBlank()) saved.selectedItem = service.state.active
        val preview = object : JPanel() {
            override fun paintComponent(g: java.awt.Graphics) {
                super.paintComponent(g)
                val gfx = g.create() as java.awt.Graphics2D
                try {
                    gfx.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    val unit = gfx.fontMetrics.height + 9
                    fun row(i: Int, text: String, fg: Color, bg: Color) {
                        gfx.color=bg; gfx.fillRect(0,i*unit,width,unit)
                        gfx.color=fg; gfx.drawString(text,10,i*unit+gfx.fontMetrics.ascent+4)
                    }
                    val ui = palette.source.getAsJsonObject("ui")
                    val overrides = palette.uiOverrides(colors,quiet.isSelected,accent.value)
                    fun surface(key: String) = overrides[key] ?: Color.decode(ui[key].asString)
                    if (quiet.isSelected) {
                        gfx.color = surface("Panel.background")
                        gfx.fillRect(0,0,width,height)
                        row(2,"   Toolbar",surface("Panel.foreground"),surface("MainToolbar.background"))
                        gfx.color = surface("Panel.borderColor")
                        gfx.drawLine(0,3*unit-1,width,3*unit-1)
                    }
                    val tree = surface("Tree.foreground")
                    row(0,"▸  src    (hover)",tree,colors.getValue("Directory hover"))
                    row(1,"▾  resources    (selected)",surface("Tree.selectionForeground"),surface("Tree.selectionBackground"))
                    row(3,"Open Recent    ›",surface("Menu.foreground"),surface("Menu.background"))
                    row(4,"Preferences…",surface("MenuItem.selectionForeground"),surface("MenuItem.selectionBackground"))
                    val paper = colors.getValue("Editor")
                    row(6,"{",colors.getValue("Code text"),paper)
                    row(7,"",colors.getValue("Code text"),paper)
                    var offset = 18
                    for ((part,role) in listOf("\"theme\"" to "JSON key", ": " to "Code text", "\"Osaka\"" to "JSON string")) {
                        gfx.color=palette.syntaxColor(role, colors, accent.value)
                        gfx.drawString(part,offset,7*unit+gfx.fontMetrics.ascent+4)
                        offset+=gfx.fontMetrics.stringWidth(part)
                    }
                    row(8,"}",colors.getValue("Code text"),paper)
                    row(9,"// palette preview",colors.getValue("Comment"),paper)
                    gfx.color=colors.getValue("Focus"); gfx.drawRect(0,6*unit,width-1,4*unit-1)
                } finally { gfx.dispose() }
            }
        }
        preview.preferredSize = java.awt.Dimension(235, (preview.getFontMetrics(preview.font).height+9)*10)
        val side = JPanel(BorderLayout(0,8))
        side.add(preview,BorderLayout.CENTER)
        side.add(JButton("Reset colors to preset").apply { addActionListener { refresh() } },BorderLayout.SOUTH)
        panel.add(side,BorderLayout.EAST)
        panel.add(JPanel(BorderLayout(8, 0)).apply {
            add(JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 4, 0)).apply {
                add(live)
                add(quiet)
            }, BorderLayout.WEST)
            add(contrastSummary, BorderLayout.CENTER)
            add(contrastButton, BorderLayout.EAST)
        }, BorderLayout.SOUTH)
        val dialog = object : DialogWrapper(event.project) {
            init {
                title = "Customize Solarized Osaka"
                setModal(false)
                setOKButtonText("Apply personal theme")
                init()
            }

            override fun createCenterPanel(): JComponent = panel

            override fun createActions(): Array<Action> = arrayOf(cancelAction, object : AbstractAction("Use base preset") {
                override fun actionPerformed(action: ActionEvent) {
                    if (ThemeCatalog.activePresetName() != palette.name) {
                        ThemeCatalog.openAppearance(event.project)
                        return
                    }
                    update.stop()
                    service.endPreview()
                    service.useBasePreset()
                    close(OK_EXIT_CODE)
                }
            }, okAction)

            override fun doOKAction() {
                try {
                    val label = PaletteFiles.validName(name.text)
                    if (label in service.state.profiles && saved.selectedItem != label &&
                        JOptionPane.showConfirmDialog(panel, "Replace the saved palette “$label”?", title,
                            JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return
                    update.stop()
                    service.endPreview()
                    service.apply(label, palette, colors, quiet.isSelected, accent.value)
                    super.doOKAction()
                } catch (error: Exception) {
                    Messages.showErrorDialog(event.project, error.message ?: "Could not apply this palette.", title)
                }
            }

            override fun dispose() {
                update.stop()
                service.endPreview()
                openDialog = null
                super.dispose()
            }
        }
        openDialog = dialog
        ApplicationManager.getApplication().messageBus.connect(dialog.disposable)
            .subscribe(LafManagerListener.TOPIC, LafManagerListener { refreshAvailability() })
        refreshAvailability()
        dialog.show()
    }
}
