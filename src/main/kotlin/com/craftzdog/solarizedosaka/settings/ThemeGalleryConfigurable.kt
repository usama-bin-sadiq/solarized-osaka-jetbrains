package com.craftzdog.solarizedosaka.settings

import com.craftzdog.solarizedosaka.ThemeCatalog
import com.craftzdog.solarizedosaka.actions.PersonalPalette
import com.craftzdog.solarizedosaka.actions.PersonalThemes
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.ButtonGroup
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToggleButton

class ThemeGalleryConfigurable : SearchableConfigurable {
    private val familyButtons = linkedMapOf<String, PaletteButton>()
    private var mode: JComboBox<String>? = null
    private var uiStyle: JComboBox<String>? = null
    private var preview: JPanel? = null
    private var instructions: JLabel? = null
    private var previewLabels: List<Pair<JLabel, String>> = emptyList()
    private val previewColors = mutableMapOf<String, Map<String, Color>>()

    override fun getDisplayName(): String = "Solarized Osaka Theme Gallery"
    override fun getId(): String = "com.craftzdog.solarizedosaka.themeGallery"

    override fun createComponent(): JComponent {
        val root = JBPanel<JBPanel<*>>(BorderLayout(JBUI.scale(0), JBUI.scale(12)))
        root.border = JBUI.Borders.empty(12)

        val gallery = JPanel(GridLayout(0, 3, JBUI.scale(8), JBUI.scale(8)))
        val group = ButtonGroup()
        for (family in ThemeCatalog.families) {
            val button = PaletteButton(family)
            button.addActionListener { updatePreview() }
            familyButtons[family] = button
            group.add(button)
            gallery.add(button)
        }

        val chooser = JPanel(BorderLayout())
        chooser.add(JBLabel("Choose a palette family"), BorderLayout.NORTH)
        chooser.add(JBScrollPane(gallery), BorderLayout.CENTER)

        val controls = JPanel(GridLayout(0, 2, JBUI.scale(12), JBUI.scale(8)))
        controls.add(JBLabel("Brightness"))
        mode = JComboBox(arrayOf("Dark", "Light"))
        controls.add(mode)
        controls.add(JBLabel("UI style"))
        uiStyle = JComboBox(arrayOf("Classic", "Islands"))
        controls.add(uiStyle)

        val sample = JPanel(GridLayout(0, 1, 0, JBUI.scale(4)))
        sample.border = JBUI.Borders.empty(12)
        val code = listOf(
            listOf("{" to "Code text"),
            listOf("    \"theme\"" to "JSON key", ": " to "Code text", "\"Solarized Osaka\"" to "JSON string"),
            listOf("}" to "Code text", "  // palette preview" to "Comment")
        )
        val font = Font(Font.MONOSPACED, Font.PLAIN, 13)
        previewLabels = code.flatMap { line ->
            val row = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0)).apply { isOpaque = false }
            sample.add(row)
            line.map { (text, role) ->
                val label = JLabel(text).apply { this.font = font }
                row.add(label)
                label to role
            }
        }
        preview = sample
        val previewContainer = JPanel(BorderLayout())
        previewContainer.add(JBLabel("Palette preview"), BorderLayout.NORTH)
        previewContainer.add(sample, BorderLayout.CENTER)

        val bottom = JPanel(BorderLayout(JBUI.scale(0), JBUI.scale(8)))
        val bottomTop = JPanel(BorderLayout())
        bottomTop.add(controls, BorderLayout.CENTER)
        bottomTop.add(previewContainer, BorderLayout.SOUTH)
        bottom.add(bottomTop, BorderLayout.CENTER)
        instructions = JBLabel()
        bottom.add(instructions, BorderLayout.SOUTH)

        root.add(chooser, BorderLayout.CENTER)
        root.add(bottom, BorderLayout.SOUTH)
        mode?.addActionListener { updatePreview() }
        uiStyle?.addActionListener { updatePreview() }
        reset()
        return root
    }

    override fun isModified(): Boolean = false

    override fun reset() {
        val current = ThemeCatalog.current()
        val family = ThemeCatalog.familyOf(current?.name)
        familyButtons[family]?.isSelected = true
        mode?.selectedItem = if (current?.isDark == false) "Light" else "Dark"
        uiStyle?.selectedItem = if (ThemeCatalog.isIslands(current?.name)) "Islands" else "Classic"
        updatePreview()
    }

    override fun apply() = Unit

    private fun selectedThemeName(): String {
        val family = familyButtons.entries.firstOrNull { it.value.isSelected }?.key ?: "Classic"
        val isDark = mode?.selectedItem != "Light"
        val islands = uiStyle?.selectedItem == "Islands"
        return ThemeCatalog.themeName(family, isDark, islands)
    }

    private fun updatePreview() {
        val family = familyButtons.entries.firstOrNull { it.value.isSelected }?.key ?: "Classic"
        val isDark = mode?.selectedItem != "Light"
        familyButtons.forEach { (name, button) ->
            val colors = ThemeCatalog.swatches.getValue(name)
            button.setSwatch(Color(if (isDark) colors.darkBackground else colors.lightBackground), Color(colors.accent))
            button.isSelected = name == family
        }
        val presetName = selectedThemeName()
        val preset = PersonalThemes.preset(presetName) ?: return
        val colors = previewColors.getOrPut(presetName) { PersonalPalette(preset).defaults }
        preview?.background = colors.getValue("Editor")
        for ((label, role) in previewLabels) {
            label.foreground = colors.getValue(role)
        }
        instructions?.text = "To use this preset, select “$presetName” in Appearance → Theme."
        preview?.repaint()
    }
}


// JetBrains look-and-feel delegates can set different colors on a selected toggle.
// Keep the text and its backing together, while the square shows the actual palette.
private class PaletteButton(family: String) : JToggleButton(family) {
    private var paper = Color(0x001419)
    private var accent = Color(0x268BD3)

    init {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        isFocusPainted = false
        foreground = Color.WHITE
        border = JBUI.Borders.empty(10, 12)
    }

    fun setSwatch(paperColor: Color, accentColor: Color) {
        paper = paperColor
        accent = accentColor
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics.create() as Graphics2D
        try {
            val corner = JBUI.scale(8)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.color = when {
                isSelected -> Color(0x2B4D59)
                model.isRollover -> Color(0x24414A)
                else -> Color(0x19343C)
            }
            g.fillRoundRect(0, 0, width, height, corner, corner)
            if (isSelected || hasFocus()) {
                g.color = if (hasFocus()) Color(0x9BC9CF) else Color(0x779FA8)
                g.drawRoundRect(1, 1, width - 3, height - 3, corner, corner)
            }

            val chip = JBUI.scale(12)
            val chipX = JBUI.scale(9)
            val chipY = (height - chip) / 2
            g.color = accent
            g.fillRoundRect(chipX, chipY, chip, chip, JBUI.scale(4), JBUI.scale(4))
            val inset = JBUI.scale(2)
            g.color = paper
            g.fillRect(chipX + inset, chipY + inset, chip - 2 * inset, chip - 2 * inset)

            val x = chipX + chip + JBUI.scale(7)
            val metrics = g.getFontMetrics(font)
            val available = (width - x - JBUI.scale(8)).coerceAtLeast(0)
            val shown = if (metrics.stringWidth(text) <= available) text else {
                val ellipsis = "…"
                text.indices.reversed().firstOrNull { metrics.stringWidth(text.take(it) + ellipsis) <= available }
                    ?.let { text.take(it) + ellipsis }.orEmpty()
            }
            g.font = font
            g.color = Color.WHITE
            g.drawString(shown, x, (height - metrics.height) / 2 + metrics.ascent)
        } finally {
            g.dispose()
        }
    }
}
