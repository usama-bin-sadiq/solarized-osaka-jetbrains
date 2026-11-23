# Solarized Osaka for JetBrains IDEs

An unofficial JetBrains IDE theme inspired by [solarized-osaka.nvim](https://github.com/craftzdog/solarized-osaka.nvim) by craftzdog.

Compatible IntelliJ Platform IDEs: **2024.3+** (build 243+). Islands UI variants require an IDE version with Islands UI support (**2025.3+**).

## Install

Install from the [JetBrains Marketplace](https://plugins.jetbrains.com/) when available, or download a plugin archive from the [project Releases page](https://github.com/usama-bin-sadiq/solarized-osaka-jetbrains/releases):

1. Open **Settings → Plugins**.
2. Select the gear menu and choose **Install Plugin from Disk…**.
3. Select the plugin archive and restart if the IDE requests it.
4. Choose a Solarized Osaka variant in **Settings → Appearance & Behavior → Appearance → Theme**.

## Quick start

1. Open **Tools → Solarized Osaka → Solarized Osaka Theme Gallery** to choose a family.
2. Open **Customize Solarized Osaka** in the same submenu to change colors or accent strength.
3. Open **Solarized Osaka Usage Guide** or **Solarized Osaka Changelog** in the **Help** menu or in **Tools → Solarized Osaka**.

[Full usage guide](USAGE.md) · [Changelog](CHANGELOG.md)

## Personal themes

Open **Tools → Solarized Osaka → Customize Solarized Osaka**, choose a base and a name, and edit the colors in the live preview. **Apply personal theme** saves and applies the palette immediately. Reopen it under **Saved palettes**, use **Reset colors to preset** to reset a draft, or **Use base preset** to return to stock colors.


### Live preview and palette controls

- Enable **Preview in IDE** to see a draft in the actual IDE. Cancel or turn off the preview to restore your previous appearance.
- **Quiet chrome** softens inactive toolbars and tabs.
- Use **Check contrast** to review readability. Reset individual colors to their preset, and save the palette with **Apply personal theme**.
- Rename, duplicate or delete saved palettes, or share them as JSON with **Import** and **Export**. Edits to light and dark colors are saved separately when you switch brightness and apply edits to each mode.

### Palette shortcuts and comfort

Use **Switch Solarized Osaka Palette** from Tools or Find Action to open favorites and recent palettes. **Undo Last Solarized Osaka Apply** restores the previous appearance for the current session. In the customizer, set **Accent strength** and edit the editor, tree and menu selection colors independently. Legacy shared selection colors still apply wherever a surface has no personal override.

## Theme gallery

The plugin includes 12 palette families, each available in Classic Dark, Classic Light, Islands Dark, and Islands Light:

| Family | Character |
|---|---|
| Classic | Original Solarized Osaka palette |
| OLED | Near-black dark surfaces |
| High Contrast | Stronger text and UI separation |
| Dimmed | Lower-brightness accents |
| Warm | Warmer dark and light palettes |
| Color Vision | Adjusted accent/status distinctions |
| Teal Accent | Teal selection and focus accents |
| Purple Accent | Purple selection and focus accents |
| Amber Accent | Amber selection and focus accents |
| Quiet UI | Soft surfaces with clearer focus, link, and selection accents |
| Vivid | Brighter accents |
| Muted | Softer accents |

Open **Settings → Appearance → Solarized Osaka Theme Gallery** to browse color swatches, choose a palette and UI style, and see a code preview. The plugin offers the gallery once on first launch.

## Editor features

- Syntax colors for Java, Kotlin, JavaScript/TypeScript, Python, Go, Rust, PHP, Ruby, Scala, Groovy, Dart, C/C++, C#, SQL, YAML, XML, HTML, CSS, JSON, Bash, and community language plugins.
- Shared IntelliJ syntax fallback colors for language plugins using `DefaultLanguageHighlighterColors`.
- **Custom language token mappings** in **Settings → Editor → Solarized Osaka Language Colors**. Map a plugin's token key to `KEYWORD`, `STRING`, `NUMBER`, `COMMENT`, `TYPE`, `FUNCTION`, `VARIABLE`, or `BUILTIN`. The language plugin must still provide its parser and syntax highlighter.
- VCS file-status colors and a 16-color Run/Debug console palette across the editor schemes.
- Mono Compact and Mono Relaxed JetBrains Mono schemes in dark and light variants.
- Temporary preview samples for core languages and optional community language plugins.

## Switching and project behavior

- Choose a UI theme in **Settings → Appearance & Behavior → Appearance → Theme**.
- Choose font presets in **Settings → Editor → Color Scheme**. Each UI theme selects its matching Osaka editor scheme automatically.
- Use **Tools → Solarized Osaka → Solarized Osaka Theme Gallery** or **Tools → Solarized Osaka → Switch Solarized Osaka Dark/Light**.
- Optional **Follow the operating system's light/dark setting** checks macOS, Windows, or GNOME Linux every minute and preserves the current Osaka palette family.
- **Tools → Solarized Osaka → Save Solarized Osaka Theme for Project** stores the current UI theme and editor scheme. **Tools → Solarized Osaka → Apply Saved Theme and Project Editor Scheme** restores the editor scheme only in that project's editors. JetBrains UI themes are IDE-wide, so applying the saved UI theme changes the whole IDE.
- **Tools → Solarized Osaka → Open Solarized Osaka Preview** opens a picker for disposable sample files. Close them without saving.

## Build

Requires JDK 21. The repository includes a Gradle 8.13 wrapper:

```bash
./gradlew buildPlugin
```

The plugin archive is written under `build/distributions/`. CI checks theme assets, contrast and plugin compatibility for the six IDE targets in the verification workflow.

On macOS with WebStorm 2026.3 and a JDK 21 or newer, run `bash scripts/audit-webstorm.sh build/distributions/solarized-osaka-intellij-1.7.1.zip` to repeat the isolated runtime sweep. Its output includes the filename × scheme matrix, semantic JSON key checks, palette button contrast and the guide/preview menu check.

## Run and publish

To launch a sandbox IDE with the plugin loaded, run:

```bash
./gradlew runIde
```

To publish a built plugin, create a release in the repository for direct downloads or configure a JetBrains Marketplace publishing token and use the IntelliJ Platform Gradle Plugin's `publishPlugin` task. See the [JetBrains publishing guide](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html) for token setup and release channels.

## Credits and license

The palette and design inspiration come from [craftzdog/solarized-osaka.nvim](https://github.com/craftzdog/solarized-osaka.nvim), with the Solarized color concept by Ethan Schoonover. Licensed under Apache-2.0.
