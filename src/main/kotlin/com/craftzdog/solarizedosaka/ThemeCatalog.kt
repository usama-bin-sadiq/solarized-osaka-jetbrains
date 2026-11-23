package com.craftzdog.solarizedosaka

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo

object ThemeCatalog {
    private const val CLASSIC_PREFIX = "Solarized Osaka"
    private const val ISLANDS_PREFIX = "Solarized Osaka Islands"

    val families = listOf(
        "Classic", "OLED", "High Contrast", "Dimmed", "Warm", "Color Vision",
        "Teal Accent", "Purple Accent", "Amber Accent", "Quiet UI", "Vivid", "Muted"
    )

    data class Swatch(val darkBackground: Int, val lightBackground: Int, val accent: Int)

    val swatches = mapOf(
        "Classic" to Swatch(0x001419, 0xFDF5E2, 0x268BD3),
        "OLED" to Swatch(0x000000, 0xFFFFFF, 0x268BD3),
        "High Contrast" to Swatch(0x000D10, 0xFFFFFF, 0x0072B2),
        "Dimmed" to Swatch(0x071A1F, 0xF4EEDC, 0x7EA3B5),
        "Warm" to Swatch(0x1A1512, 0xFBF1DE, 0xC2774A),
        "Color Vision" to Swatch(0x001419, 0xFDF5E2, 0x0072B2),
        "Teal Accent" to Swatch(0x001419, 0xFDF5E2, 0x00A896),
        "Purple Accent" to Swatch(0x001419, 0xFDF5E2, 0x9B6AD6),
        "Amber Accent" to Swatch(0x001419, 0xFDF5E2, 0xD18B00),
        "Quiet UI" to Swatch(0x001419, 0xFDF5E2, 0x268BD3),
        "Vivid" to Swatch(0x001419, 0xFDF5E2, 0x389DE8),
        "Muted" to Swatch(0x071A1F, 0xF4EEDC, 0x718FA0),
    )

    fun current(): UIThemeLookAndFeelInfo? = LafManager.getInstance().currentUIThemeLookAndFeel

    @Suppress("DEPRECATION")
    fun all(): List<UIThemeLookAndFeelInfo> =
        LafManager.getInstance().installedLookAndFeels
            .filterIsInstance<UIThemeLookAndFeelInfo>()
            .filter { it.name.startsWith(CLASSIC_PREFIX) }
            .sortedBy { it.name }

    fun removedPresetReplacement(name: String): String = when (name) {
        "Solarized Osaka Glass" -> "Solarized Osaka Quiet UI"
        "Solarized Osaka Glass Light" -> "Solarized Osaka Quiet UI Light"
        "Solarized Osaka Islands Glass" -> "Solarized Osaka Islands Quiet UI"
        "Solarized Osaka Islands Glass Light" -> "Solarized Osaka Islands Quiet UI Light"
        else -> name
    }

    fun find(name: String?): UIThemeLookAndFeelInfo? =
        all().firstOrNull { it.name == name?.let(::removedPresetReplacement) }

    fun apply(name: String): Boolean {
        val theme = find(name) ?: return false
        val personal = com.craftzdog.solarizedosaka.actions.PersonalThemes.instance()
        personal.rememberApply()
        personal.deactivate()
        LafManager.getInstance().setCurrentUIThemeLookAndFeel(theme)
        com.craftzdog.solarizedosaka.actions.PersonalThemes.instance().rememberChoice("preset:$name")
        return true
    }

    fun applyBrightness(name: String): Boolean {
        val personal = com.craftzdog.solarizedosaka.actions.PersonalThemes.instance()
        if (personal.isPreviewing) return false
        return personal.switchToBrightness(name) || apply(name)
    }

    fun familyOf(name: String?): String {
        if (name.isNullOrBlank()) return "Classic"
        val withoutPrefix = name.removePrefix(ISLANDS_PREFIX).removePrefix(CLASSIC_PREFIX).trim()
        val family = withoutPrefix.removeSuffix(" Light").removeSuffix(" Dark").trim()
        return family.takeIf { it in families } ?: "Classic"
    }

    fun isIslands(name: String?): Boolean = name?.startsWith(ISLANDS_PREFIX) == true

    fun themeName(family: String, isDark: Boolean, islands: Boolean): String {
        val prefix = if (islands) ISLANDS_PREFIX else CLASSIC_PREFIX
        val normalizedFamily = family.takeIf { it in families } ?: "Classic"
        val actualFamily = if (normalizedFamily == "Warm" && isDark) "Warm Dark" else normalizedFamily
        val familyPart = if (actualFamily == "Classic") "" else " $actualFamily"
        val modePart = if (isDark || actualFamily == "Warm Dark") "" else " Light"
        return "$prefix$familyPart$modePart"
    }

    fun toggleBrightness(currentName: String?, isDark: Boolean): String {
        val target = themeName(familyOf(currentName), !isDark, isIslands(currentName))
        return if (find(target) != null) target else themeName("Classic", !isDark, false)
    }
}
