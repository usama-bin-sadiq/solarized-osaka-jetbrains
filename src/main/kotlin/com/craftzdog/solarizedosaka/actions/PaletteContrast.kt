package com.craftzdog.solarizedosaka.actions

import java.awt.Color
import kotlin.math.pow
import kotlin.math.roundToInt

internal object PaletteContrast {
    data class Pairing(val label: String, val foreground: Color, val background: Color, val textRole: String? = null) {
        val ratio: Double get() = contrast(foreground, background)
    }

    fun contrast(first: Color, second: Color): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    fun nearbyShade(color: Color, surfaces: List<Color>): Color? {
        if (surfaces.isEmpty() || surfaces.all { contrast(color, it) >= 4.5 }) return null
        for (step in 1..100) {
            for (tone in listOf(Color.BLACK, Color.WHITE)) {
                val candidate = mix(color, tone, step)
                if (surfaces.all { contrast(candidate, it) >= 4.5 }) return candidate
            }
        }
        return null
    }

    fun mix(a: Color, b: Color, percentage: Int): Color {
        val t = percentage.coerceIn(0, 100) / 100.0
        fun channel(x: Int, y: Int) = (x + (y - x) * t).roundToInt()
        return Color(channel(a.red, b.red), channel(a.green, b.green), channel(a.blue, b.blue))
    }

    fun accent(color: Color, strength: Int): Color {
        if (strength == 50) return color
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        val multiplier = 0.45f + 1.1f * strength.coerceIn(0, 100) / 100f
        return Color(Color.HSBtoRGB(hsb[0], (hsb[1] * multiplier).coerceAtMost(1f), hsb[2]))
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Int): Double {
            val v = value / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}
