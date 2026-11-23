package com.craftzdog.solarizedosaka.actions

import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal object PaletteFiles {
    private const val MAX_BYTES = 65_536L
    private val gson = GsonBuilder().setPrettyPrinting().create()
    data class Document(val format: Int, val name: String, val palette: PersonalThemes.Profile)

    fun validName(name: String): String {
        val label = name.trim()
        require(label.isNotBlank() && label.length <= 80 && label != "New personal theme" && label.none { it.isISOControl() }) {
            "Choose a personal theme name of up to 80 characters."
        }
        return label
    }

    fun read(path: Path): Document {
        require(Files.isRegularFile(path) && Files.size(path) in 1..MAX_BYTES) { "Choose a palette JSON file under 64 KB." }
        val doc = requireNotNull(gson.fromJson(Files.readString(path), Document::class.java))
        require(doc.format == 1) { "This palette file format is unsupported." }
        val name = validName(doc.name)
        val profile = requireNotNull(doc.palette)
        PersonalThemes.migrateRemovedPreset(profile)
        val base = PersonalThemes.preset(profile.preset)?.let(::PersonalPalette)
            ?: error("This palette needs a preset that is unavailable.")
        require(profile.accentStrength in 0..100) { "Accent strength must be between 0% and 100%." }
        for (map in listOfNotNull(profile.colors, profile.dark?.values, profile.light?.values)) {
            require(map.all { (role, color) -> role in base.defaults && Regex("#[0-9a-fA-F]{6}").matches(color) }) {
                "Palette colors must use known roles and six-digit hex colors."
            }
        }
        requireNotNull(profile.colors) { "The palette is missing its colors map." }
        for (overrides in listOfNotNull(profile.dark, profile.light)) requireNotNull(overrides.values)
        return doc.copy(name = name)
    }

    fun write(path: Path, name: String, profile: PersonalThemes.Profile) {
        Files.writeString(path, gson.toJson(Document(1, name, profile)), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    }
}
