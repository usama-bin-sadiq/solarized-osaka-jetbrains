package com.craftzdog.solarizedosaka.actions

import com.craftzdog.solarizedosaka.ThemeCatalog
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Window
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

@Service(Service.Level.APP)
@State(name="SolarizedOsakaPersonalThemes",storages=[Storage("solarized-osaka-personal.xml")])
class PersonalThemes : PersistentStateComponent<PersonalThemes.Settings>, Disposable {
    class Profile {
        var quietChrome = false
        var accentStrength = 50
        var preset = ""
        // Legacy profiles stored the complete palette for a single preset.
        var colors: MutableMap<String, String> = linkedMapOf()
        var light: ColorOverrides? = null
        var dark: ColorOverrides? = null
    }
    class ColorOverrides {
        var values: MutableMap<String, String> = linkedMapOf()
    }
    class Settings {
        var profiles: MutableMap<String,Profile> = linkedMapOf()
        var active = ""
        var favorites: MutableList<String> = arrayListOf()
        var recent: MutableList<String> = arrayListOf()
        var schemeAliases: MutableMap<String, String> = linkedMapOf()
    }
    private var settings = Settings()
    private val previous = linkedMapOf<String,Any?>()
    private val applied = linkedMapOf<String, Color>()
    private data class EditorDraft(val preset: String, val colors: Map<String, Color>, val accent: Int)
    private var previewEditor: EditorDraft? = null
    private var defaults: javax.swing.UIDefaults? = null
    private var initialized = false
    private var changing = false
    private var observedPreset: String? = null
    private data class PreviewOrigin(val preset: String?, val scheme: EditorColorsScheme)
    private var previewOrigin: PreviewOrigin? = null
    private data class UndoEntry(val origin: PreviewOrigin, val label: String, val profile: Profile?)
    private var undo: UndoEntry? = null
    val canUndo: Boolean get() = undo != null
    init {
        val application = ApplicationManager.getApplication()
        application.messageBus.connect(this).subscribe(LafManagerListener.TOPIC, LafManagerListener {
            val activePreset = ThemeCatalog.activePresetName()
            if (changing || !initialized || observedPreset == activePreset) return@LafManagerListener
            observedPreset = activePreset
            previewOrigin = null
            previewEditor = null
            undo = null
            clearOverlay()
            if (activePreset != null) rememberChoice("preset:$activePreset")

            val label = settings.active
            val profile = settings.profiles[label] ?: return@LafManagerListener
            if (activePreset == null || !sameFamily(profile.preset, activePreset)) {
                settings.active = ""
                return@LafManagerListener
            }
            application.invokeLater {
                if (settings.active == label && settings.profiles[label] === profile &&
                    ThemeCatalog.activePresetName() == activePreset && previewOrigin == null
                ) restoreProfile(profile, label, activePreset)
            }
        })
    }

    override fun getState() = settings
    override fun loadState(state: Settings) {
        settings = state
        val active = state.profiles[state.active]
        for (profile in state.profiles.values) {
            if (profile !== active) migrateRemovedPreset(profile)
        }
        fun migrated(key: String): String = if (key.startsWith("preset:"))
            "preset:" + ThemeCatalog.removedPresetReplacement(key.removePrefix("preset:")) else key
        state.favorites = state.favorites.map(::migrated).distinct().toMutableList()
        state.recent = state.recent.map(::migrated).distinct().toMutableList()
    }

    fun restore() {
        if (initialized) return
        initialized = true
        val activePreset = ThemeCatalog.activePresetName()
        observedPreset = activePreset
        if (activePreset != null) rememberChoice("preset:$activePreset")
        val label = settings.active
        val profile = settings.profiles[label] ?: return
        migrateRemovedPreset(profile)
        if (activePreset == null || !sameFamily(profile.preset, activePreset)) {
            settings.active = ""
            return
        }
        restoreProfile(profile, label, activePreset)
    }

    private fun restoreProfile(profile: Profile, label: String, activePreset: String) {
        val palette = preset(activePreset)?.let(::PersonalPalette) ?: return
        rememberLegacy(profile)
        render(palette, colorsFor(profile, palette), personalName(label),
            quietChrome = profile.quietChrome, accent = profile.accentStrength)
        profile.preset = activePreset
    }

    internal fun colorsFor(profile: Profile, palette: PersonalPalette): Map<String, Color> {
        val current = if (palette.dark) profile.dark else profile.light
        val opposite = if (palette.dark) profile.light else profile.dark
        val legacy = if (profile.colors.isEmpty()) emptyMap() else {
            val original = preset(profile.preset)?.let(::PersonalPalette)
            profile.colors.filter { (role, value) -> original?.defaults?.get(role)?.let { hex(it) != value } == true }
        }
        val edits = current?.values ?: opposite?.values ?: legacy
        val colors = palette.defaults + edits.mapNotNull { (role, value) ->
            if (role in palette.defaults) runCatching { role to Color.decode(value) }.getOrNull() else null
        }.toMap()
        return if (current == null && opposite != null && edits.isNotEmpty())
            palette.adaptInherited(colors)
        else colors
    }

    internal fun apply(label: String, palette: PersonalPalette, colors: Map<String, Color>, quiet: Boolean = false, accent: Int = 50) {
        val existing = settings.profiles[label]
        val profile = if (existing != null && sameFamily(existing.preset, palette.name)) existing else Profile()
        rememberApply()
        rememberLegacy(profile)
        render(palette, colors, personalName(label), quietChrome = quiet, accent = accent)
        profile.preset = palette.name
        profile.quietChrome = quiet
        profile.accentStrength = accent.coerceIn(0, 100)
        val overrides = ColorOverrides().also {
            it.values = colors.filter { (role, color) -> color != palette.defaults[role] }
                .mapValues { (_, color) -> hex(color) }.toMutableMap()
        }
        if (palette.dark) profile.dark = overrides else profile.light = overrides
        settings.profiles[label] = profile
        settings.active = label
        initialized = true
        rememberChoice("personal:$label")
    }

    fun rememberChoice(key: String) {
        settings.recent.remove(key)
        settings.recent.add(0, key)
        while (settings.recent.size > 12) settings.recent.removeAt(settings.recent.lastIndex)
    }

    internal fun activateProfile(label: String): Boolean {
        val profile = settings.profiles[label] ?: return false
        val activePreset = ThemeCatalog.activePresetName() ?: return false
        if (!sameFamily(profile.preset, activePreset)) return false
        endPreview()
        rememberApply()
        restoreProfile(profile, label, activePreset)
        settings.active = label
        rememberChoice("personal:$label")
        return true
    }

    internal fun renameProfile(old: String, label: String) {
        require(label !in settings.profiles) { "A saved palette already uses that name." }
        val profile = requireNotNull(settings.profiles[old])
        endPreview()
        if (settings.active == old) {
            val palette = preset(profile.preset)?.let(::PersonalPalette)
            if (palette != null) render(palette, colorsFor(profile, palette), personalName(label), quietChrome = profile.quietChrome, accent = profile.accentStrength)
            settings.active = label
        }
        settings.profiles[label] = profile
        settings.profiles.remove(old)
        // A newly created palette with this name must not inherit a retired alias.
        settings.schemeAliases.remove(personalName(label))
        settings.schemeAliases.replaceAll { _, name -> if (name == personalName(old)) personalName(label) else name }
        settings.schemeAliases[personalName(old)] = personalName(label)
        if (undo?.label == old) undo = undo?.copy(label = label)
        com.intellij.openapi.project.ProjectManager.getInstance().openProjects.forEach { project ->
            val prefs = project.getService(com.craftzdog.solarizedosaka.ProjectThemePreferences::class.java).state
            if (prefs.editorSchemeName.removePrefix("_@user_") == personalName(old)) prefs.editorSchemeName = personalName(label)
        }
        settings.favorites.replaceAll { if (it == "personal:$old") "personal:$label" else it }
        settings.recent.replaceAll { if (it == "personal:$old") "personal:$label" else it }
    }

    internal fun deleteProfile(label: String) {
        if (settings.active == label) resetToBasePreset()
        settings.profiles.remove(label)
        settings.favorites.remove("personal:$label")
        settings.recent.remove("personal:$label")
    }

    internal fun duplicateProfile(old: String, label: String) {
        require(label !in settings.profiles) { "A saved palette already uses that name." }
        val source = requireNotNull(settings.profiles[old])
        settings.profiles[label] = copyProfile(source)
    }

    private fun copyProfile(source: Profile) = Profile().also {
            it.preset = source.preset
            it.quietChrome = source.quietChrome
            it.accentStrength = source.accentStrength
            it.colors.putAll(source.colors)
            it.light = source.light?.copy()
            it.dark = source.dark?.copy()
    }

    private fun ColorOverrides.copy() = ColorOverrides().also { it.values.putAll(values) }

    private fun rememberLegacy(profile: Profile) {
        if (profile.colors.isEmpty()) return
        val palette = preset(profile.preset)?.let(::PersonalPalette) ?: return
        val migrated = ColorOverrides().also {
            it.values = profile.colors.filter { (role, value) ->
                palette.defaults[role]?.let { default -> hex(default) != value } == true
            }.toMutableMap()
        }
        if (palette.dark) profile.dark = profile.dark ?: migrated else profile.light = profile.light ?: migrated
        profile.colors.clear()
    }

    fun resolveSchemeName(name: String): String {
        val bare = name.removePrefix("_@user_")
        val replacement = ThemeCatalog.removedPresetReplacement(bare)
        if (replacement != bare) return replacement
        val alias = settings.schemeAliases[bare] ?: return name
        return if (alias.removePrefix("My Osaka — ") in settings.profiles) alias else name
    }

    fun projectScheme(name: String): EditorColorsScheme? {
        val schemeName = resolveSchemeName(name)
        val manager = EditorColorsManager.getInstance()
        if (!schemeName.startsWith("My Osaka — ")) return manager.getScheme(schemeName)
        val profile = settings.profiles[schemeName.removePrefix("My Osaka — ")]
        val palette = profile?.let { preset(it.preset) }?.let(::PersonalPalette)
        return manager.getScheme(schemeName)
            ?: if (profile != null && palette != null) {
                buildScheme(manager, palette, colorsFor(profile, palette), profile.accentStrength, schemeName)
            } else manager.getScheme(name)
    }

    private fun rememberApply() {
        val origin = previewOrigin ?: PreviewOrigin(ThemeCatalog.activePresetName(), EditorColorsManager.getInstance().globalScheme)
        val label = settings.active
        undo = UndoEntry(origin, label, settings.profiles[label]?.let(::copyProfile))
    }

    fun undoLastApply() {
        val entry = undo ?: return
        endPreview()
        undo = null
        if (entry.origin.preset != ThemeCatalog.activePresetName()) return
        if (entry.profile != null && entry.label in settings.profiles) {
            val palette = preset(entry.profile.preset)?.let(::PersonalPalette)
            if (palette != null) {
                render(palette, colorsFor(entry.profile, palette), personalName(entry.label),
                    quietChrome = entry.profile.quietChrome, accent = entry.profile.accentStrength)
                settings.profiles[entry.label] = entry.profile
                settings.active = entry.label
                return
            }
        }
        restoreAppearance(entry.origin)
        settings.active = ""
    }

    internal fun sameFamily(first: String, second: String) =
        preset(first) != null && preset(second) != null &&
            ThemeCatalog.familyOf(first) == ThemeCatalog.familyOf(second) &&
            ThemeCatalog.isIslands(first) == ThemeCatalog.isIslands(second)

    internal fun preview(palette: PersonalPalette, colors: Map<String, Color>, quiet: Boolean, accent: Int) {
        requireActivePreset(palette)
        if (previewOrigin == null) {
            previewOrigin = PreviewOrigin(palette.name, EditorColorsManager.getInstance().globalScheme)
        }
        render(palette, colors, "Solarized Osaka preview", registerScheme = false, quietChrome = quiet, accent = accent)
    }

    fun endPreview() {
        val origin = previewOrigin ?: return
        previewOrigin = null
        previewEditor = null
        if (origin.preset != ThemeCatalog.activePresetName()) return
        val label = settings.active
        val profile = settings.profiles[label]
        val base = profile?.let { preset(it.preset) }?.let(::PersonalPalette)
        if (profile != null && base != null && origin.preset == base.name) {
            render(base, colorsFor(profile, base), personalName(label), quietChrome = profile.quietChrome, accent = profile.accentStrength)
            return
        }
        restoreAppearance(origin)
    }

    private fun restoreAppearance(origin: PreviewOrigin) {
        changing = true
        try {
            clearOverlay()
            EditorColorsManager.getInstance().setGlobalScheme(origin.scheme)
            repaint()
        } finally {
            changing = false
        }
    }

    private fun render(
        palette: PersonalPalette,
        colors: Map<String, Color>,
        schemeName: String,
        registerScheme: Boolean = true,
        quietChrome: Boolean = false,
        accent: Int = 50,
    ) {
        requireActivePreset(palette)
        val preset = palette.name
        val manager=EditorColorsManager.getInstance()
        val editorRoles = setOf("Editor", "Code text", "JSON key", "JSON string", "Comment",
            "Selection", "Selected text", "Editor selection", "Editor selected text")
        val draft = EditorDraft(preset, colors.filterKeys { it in editorRoles }, accent)
        val copy = if (registerScheme || previewEditor != draft)
            buildScheme(manager, palette, colors, accent, schemeName)
        else null
        previewEditor = if (registerScheme) null else draft
        changing=true
        try {
            val changed = updateOverlay(palette.uiOverrides(colors, quietChrome, accent))
            if (copy != null) {
                if (registerScheme) manager.addColorScheme(copy)
                manager.setGlobalScheme(copy)
            }
            if (changed) repaint()
        } finally { changing=false }
    }

    private fun requireActivePreset(palette: PersonalPalette) {
        check(ThemeCatalog.activePresetName() == palette.name) {
            "Select “${palette.name}” in Appearance before applying these colors."
        }
    }

    private fun buildScheme(
        manager: EditorColorsManager,
        palette: PersonalPalette,
        colors: Map<String, Color>,
        accent: Int,
        schemeName: String,
    ): EditorColorsScheme {
        val base=manager.getScheme(palette.schemeName) ?: error("Missing editor scheme "+palette.schemeName)
        val copy=base.clone() as EditorColorsScheme
        copy.name = schemeName
        if (accent != 50) for (token in palette.accentTokens) {
            val key = TextAttributesKey.find(token)
            val original = base.getAttributes(key) ?: continue
            val ink = original.foregroundColor ?: continue
            val background = original.backgroundColor ?: colors.getValue("Editor")
            val tinted = PaletteContrast.accent(ink, accent)
            val attributes = original.clone() as TextAttributes
            attributes.foregroundColor = PaletteContrast.nearbyShade(tinted, listOf(background)) ?: tinted
            copy.setAttributes(key, attributes)
        }
        for ((role,token) in mapOf("Code text" to "TEXT","JSON key" to "JSON.PROPERTY_KEY","JSON string" to "JSON.STRING","Comment" to "DEFAULT_LINE_COMMENT")) {
            val key=TextAttributesKey.find(token)
            val attr=base.getAttributes(key).clone() as TextAttributes
            if (role == "Code text" || colors[role] != palette.defaults[role] || token !in palette.accentTokens) attr.foregroundColor = colors.getValue(role)
            else attr.foregroundColor = copy.getAttributes(key).foregroundColor
            if (role=="Code text") attr.backgroundColor=colors.getValue("Editor")
            copy.setAttributes(key,attr)
        }
        colors["Editor"]?.takeIf { it != palette.defaults["Editor"] }
            ?.let { copy.setColor(ColorKey.find("GUTTER_BACKGROUND"), it) }
        palette.selectionColor("Editor selection", colors)?.let { copy.setColor(EditorColors.SELECTION_BACKGROUND_COLOR, it) }
        palette.selectionColor("Editor selected text", colors)?.let { copy.setColor(EditorColors.SELECTION_FOREGROUND_COLOR, it) }
        return copy
    }

    private fun updateOverlay(next: Map<String, Color>): Boolean {
        val table = UIManager.getLookAndFeelDefaults()
        if (defaults !== table) clearOverlay()
        defaults = table
        var changed = false
        for (key in applied.keys.toList()) {
            if (key in next) continue
            val old = previous.remove(key)
            if (old == null) table.remove(key) else table[key] = old
            applied.remove(key)
            changed = true
        }
        for ((key, color) in next) {
            if (applied[key] == color) continue
            if (key !in previous) previous[key] = table[key]
            table[key] = ColorUIResource(color)
            applied[key] = color
            changed = true
        }
        return changed
    }

    internal fun useBasePreset() {
        endPreview()
        rememberApply()
        resetToBasePreset()
    }

    private fun resetToBasePreset() {
        val activePreset = ThemeCatalog.activePresetName() ?: return
        val base = preset(activePreset)?.let(::PersonalPalette) ?: return
        endPreview()
        settings.active = ""
        clearOverlay()
        val manager = EditorColorsManager.getInstance()
        manager.getScheme(base.schemeName)?.let(manager::setGlobalScheme)
        rememberChoice("preset:$activePreset")
        repaint()
    }

    private fun clearOverlay() {
        defaults?.let { table ->
            previous.forEach { (key,value) -> if (value==null) table.remove(key) else table[key]=value }
        }
        previous.clear()
        applied.clear()
        defaults=null
    }
    private fun repaint() {
        Window.getWindows().filter { it.isShowing }.forEach(SwingUtilities::updateComponentTreeUI)
    }
    override fun dispose() { clearOverlay() }

    companion object {
        fun instance() = ApplicationManager.getApplication().getService(PersonalThemes::class.java)
        internal fun migrateRemovedPreset(profile: Profile) {
            profile.preset = ThemeCatalog.removedPresetReplacement(profile.preset)
        }
        internal fun preset(name: String) = presets().firstOrNull { it["name"].asString == name }
        private fun personalName(label: String) = "My Osaka — " + label
        internal fun presets() = bundledPresets
        private val bundledPresets by lazy { requireNotNull(PersonalThemes::class.java.getResourceAsStream("/themes/osaka-presets.txt")).bufferedReader().use { reader ->
            reader.readLines().map { json("/themes/"+it) }
        } }
    }
}
