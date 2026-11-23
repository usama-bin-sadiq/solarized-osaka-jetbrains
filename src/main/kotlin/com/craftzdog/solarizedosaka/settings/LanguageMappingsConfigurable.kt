package com.craftzdog.solarizedosaka.settings

import com.craftzdog.solarizedosaka.CustomTokenMappingsService
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

class LanguageMappingsConfigurable : SearchableConfigurable {
    private var mappings: JTextArea? = null

    override fun getDisplayName(): String = "Solarized Osaka Language Colors"
    override fun getId(): String = "com.craftzdog.solarizedosaka.languageColors"

    override fun createComponent(): JComponent {
        val panel = JPanel(BorderLayout(JBUI.scale(0), JBUI.scale(8)))
        panel.border = JBUI.Borders.empty(12)
        panel.add(JBLabel("Map custom language TextAttribute keys to Osaka syntax colors."), BorderLayout.NORTH)
        val editor = JTextArea(12, 52)
        editor.font = Font("JetBrains Mono", Font.PLAIN, 12)
        editor.text = CustomTokenMappingsService.instance().currentMappings()
        mappings = editor
        panel.add(JBScrollPane(editor), BorderLayout.CENTER)
        panel.add(JBLabel("One mapping per line: PLUGIN_TOKEN_KEY = KEYWORD | STRING | NUMBER | COMMENT | TYPE | FUNCTION | VARIABLE | BUILTIN"), BorderLayout.SOUTH)
        return panel
    }

    override fun isModified(): Boolean = mappings?.text != CustomTokenMappingsService.instance().currentMappings()

    override fun reset() {
        mappings?.text = CustomTokenMappingsService.instance().currentMappings()
    }

    override fun apply() {
        try {
            CustomTokenMappingsService.instance().replaceMappings(mappings?.text.orEmpty())
        } catch (e: IllegalArgumentException) {
            throw ConfigurationException(e.message ?: "Invalid language token mapping")
        }
    }
}
