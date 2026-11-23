package com.craftzdog.solarizedosaka

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import java.util.regex.Pattern

@Service(Service.Level.APP)
@State(name = "SolarizedOsakaCustomLanguageColors", storages = [Storage("solarized-osaka-language-colors.xml")])
class CustomTokenMappingsService : PersistentStateComponent<CustomTokenMappingsService.State>, Disposable {
    class State {
        var mappings: String = ""
    }

    private var savedState = State()
    @Volatile private var applying = false

    init {
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(EditorColorsManager.TOPIC, EditorColorsListener { scheme -> scheme?.let(::onSchemeChanged) })
    }

    override fun getState(): State = savedState

    override fun loadState(state: State) {
        savedState = state
    }

    fun currentMappings(): String = savedState.mappings

    fun replaceMappings(text: String) {
        parse(text) // Validate before persisting.
        val manager = EditorColorsManager.getInstance()
        val current = manager.globalScheme
        val baseName = current.name.removeSuffix(CUSTOM_SUFFIX)
        val base = manager.getScheme(baseName) ?: current
        savedState.mappings = text
        if (text.isBlank()) {
            if (current.name.endsWith(CUSTOM_SUFFIX)) manager.setGlobalScheme(base)
        } else {
            applyTo(base)
        }
    }

    private fun onSchemeChanged(scheme: EditorColorsScheme) {
        if (applying || !scheme.name.startsWith("Solarized Osaka") || scheme.name.endsWith(CUSTOM_SUFFIX)) return
        applyTo(scheme)
    }

    private fun applyTo(sourceScheme: EditorColorsScheme) {
        if (savedState.mappings.isBlank() || !sourceScheme.name.startsWith("Solarized Osaka") || sourceScheme.name.endsWith(CUSTOM_SUFFIX)) return
        val manager = EditorColorsManager.getInstance()
        val custom = sourceScheme.clone() as? EditorColorsScheme ?: return
        custom.name = sourceScheme.name + CUSTOM_SUFFIX
        for ((externalName, role) in parse(savedState.mappings)) {
            val roleKey = TextAttributesKey.find(roleKeyName(role))
            val inherited = sourceScheme.getAttributes(roleKey)
            custom.setAttributes(TextAttributesKey.find(externalName), inherited.clone() as TextAttributes)
        }
        applying = true
        try {
            manager.addColorScheme(custom)
            manager.setGlobalScheme(custom)
        } finally {
            applying = false
        }
    }

    private fun parse(text: String): List<Pair<String, String>> {
        val allowedRoles = setOf("KEYWORD", "STRING", "NUMBER", "COMMENT", "TYPE", "FUNCTION", "VARIABLE", "BUILTIN")
        val namePattern = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,95}")
        val mappings = text.lineSequence().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }.map { line ->
            val pair = line.split('=', limit = 2)
            require(pair.size == 2) { "Use KEY_NAME = ROLE on each mapping line." }
            val key = pair[0].trim()
            val role = pair[1].trim().uppercase()
            require(namePattern.matcher(key).matches()) { "Invalid text attribute key: $key" }
            require(role in allowedRoles) { "Unknown role '$role'. Choose one of ${allowedRoles.joinToString()}" }
            key to role
        }.toList()
        require(mappings.map { it.first }.distinct().size == mappings.size) {
            "Each text attribute key can only be mapped once."
        }
        return mappings
    }

    private fun roleKeyName(role: String): String = when (role) {
        "KEYWORD" -> "DEFAULT_KEYWORD"
        "STRING" -> "DEFAULT_STRING"
        "NUMBER" -> "DEFAULT_NUMBER"
        "COMMENT" -> "DEFAULT_LINE_COMMENT"
        "TYPE" -> "DEFAULT_CLASS_NAME"
        "FUNCTION" -> "DEFAULT_FUNCTION_CALL"
        "VARIABLE" -> "DEFAULT_LOCAL_VARIABLE"
        else -> "DEFAULT_PREDEFINED_SYMBOL"
    }

    override fun dispose() {}

    companion object {
        private const val CUSTOM_SUFFIX = " (Osaka community mappings)"

        fun instance(): CustomTokenMappingsService =
            ApplicationManager.getApplication().getService(CustomTokenMappingsService::class.java)
    }
}
