# Solarized Osaka for JetBrains IDEs

A clean, dark (and light) Solarized-inspired theme for **all** IntelliJ-based IDEs.

**Inspired by** the excellent Neovim theme  
**[solarized-osaka.nvim](https://github.com/craftzdog/solarized-osaka.nvim)** by [craftzdog](https://github.com/craftzdog).

This is an unofficial port of the color palette and design spirit of craftzdog’s Solarized Osaka theme to the JetBrains platform.

## Themes included

| Theme | Style | Notes |
|-------|--------|-------|
| **Solarized Osaka** | Classic Dark | Works on all versions |
| **Solarized Osaka Light** | Classic Light | Works on all versions |
| **Solarized Osaka Islands** | Islands Dark | For JetBrains Islands UI (2025.3+) |
| **Solarized Osaka Islands Light** | Islands Light | For JetBrains Islands UI (2025.3+) |

## Color Palette (Dark)

| Role        | Hex       |
|-------------|-----------|
| Background  | `#001419` |
| Highlight   | `#002c38` |
| Border      | `#063540` |
| Foreground  | `#839395` |
| Comment     | `#576d74` |
| Yellow      | `#b28500` |
| Orange      | `#c94c16` |
| Red         | `#db302d` |
| Magenta     | `#d23681` |
| Violet      | `#6d71c4` |
| Blue        | `#268bd3` |
| Cyan        | `#29a298` |
| Green       | `#849900` |

Colors are taken from craftzdog’s [solarized-osaka.nvim](https://github.com/craftzdog/solarized-osaka.nvim) palette.

## Installation

1. Download `SolarizedOsaka-1.1.0.jar`
2. **Settings → Plugins → ⚙️ → Install Plugin from Disk…**
3. Select the JAR and restart the IDE
4. **Settings → Appearance & Behavior → Appearance** → choose one of the four themes

### Manual install (if Install from Disk fails)

Extract the JAR into:
- Windows: `%APPDATA%\JetBrains\<Product><Version>\plugins\solarized-osaka\`
- macOS: `~/Library/Application Support/JetBrains/<Product><Version>/plugins/solarized-osaka/`
- Linux: `~/.local/share/JetBrains/<Product><Version>/plugins/solarized-osaka/`

Then restart the IDE.

## Building

```bash
./gradlew buildPlugin
```

Requires JDK 21.

## Credits & Inspiration

- **Original theme**: [craftzdog/solarized-osaka.nvim](https://github.com/craftzdog/solarized-osaka.nvim)  
  by [Takuya Matsuyama (craftzdog)](https://github.com/craftzdog)
- Original Solarized concept by Ethan Schoonover

This JetBrains theme is **inspired by** and uses the color palette of craftzdog’s Solarized Osaka Neovim theme. All credit for the beautiful color design goes to craftzdog.

## License

Apache-2.0
