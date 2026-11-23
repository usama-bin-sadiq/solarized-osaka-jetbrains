package com.craftzdog.solarizedosaka

import com.craftzdog.solarizedosaka.actions.PersonalThemes
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

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

    fun activePresetName(): String? = current()?.takeIf {
        it.id.startsWith("com.craftzdog.solarizedosaka.") && PersonalThemes.preset(it.name) != null
    }?.name

    fun openAppearance(project: Project?) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project, { it is SearchableConfigurable && it.id == "preferences.lookFeel" }, null
        )
    }

    fun removedPresetReplacement(name: String): String = when (name) {
        "Solarized Osaka Glass" -> "Solarized Osaka Quiet UI"
        "Solarized Osaka Glass Light" -> "Solarized Osaka Quiet UI Light"
        "Solarized Osaka Islands Glass" -> "Solarized Osaka Islands Quiet UI"
        "Solarized Osaka Islands Glass Light" -> "Solarized Osaka Islands Quiet UI Light"
        else -> name
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
}
