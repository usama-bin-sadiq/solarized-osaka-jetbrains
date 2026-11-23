# Solarized Osaka Usage Guide

## Install and choose a preset

1. Open Settings → Plugins and choose Install Plugin from Disk from the gear menu.
2. Select the Solarized Osaka ZIP and restart if WebStorm asks.
3. Under Settings → Appearance & Behavior → Appearance, choose a Solarized Osaka theme. Each bundled UI theme selects its matching editor scheme.
4. For a visual tour, open Tools → Solarized Osaka → Solarized Osaka Theme Gallery. Islands presets require an IDE with Islands UI support.

## Customize colors

- Choose Tools → Solarized Osaka → Customize Solarized Osaka and select your base preset.
- Set Accent strength between Soft and Vivid. Edit the editor, directory, and menu selection colors independently. Check contrast suggests readable shades for common text pairs.
- Enable Preview in IDE for live changes. Cancel restores the previous appearance. Apply personal theme saves immediately.
- Quiet chrome reduces the prominence of idle toolbars and borders.

## Switch, undo, and share

- Find Action → Switch Solarized Osaka Palette opens favorites and recent palettes.
- Use Switch Solarized Osaka Dark/Light for the matching brightness. Personal palettes keep separate light and dark edits.
- Undo Last Solarized Osaka Apply restores the previous appearance for this session.
- Rename, duplicate, import, or export palettes in the customization dialog. Exported palettes are JSON.

## Project preferences

- Save Solarized Osaka Theme for Project remembers the current theme and editor scheme.
- Apply Saved Theme and Project Editor Scheme switches UI appearance IDE-wide and restores the project's editor scheme for that project.
- Select font presets such as the Mono Compact or Mono Relaxed scheme in Settings → Editor → Color Scheme.

## Check file highlighting

- Open Solarized Osaka Preview and choose one sample. The picker shows the IDE's detected file type.
- Native language plugins provide parsing and inspections. TextMate bundles provide lexical color for additional languages such as Ruby, Rust, Swift and Go.
- Export Solarized Osaka Syntax Bundles saves a ZIP of optional starter grammars for Ada, Elixir, Haskell, Luau, Nim, OCaml, Pascal, R and Zig. Extract the ZIP, then add only the desired bundle folders in Settings → Editor → TextMate Bundles. Keep the extracted folders in place and disable any overlapping bundle if necessary.
- If a file is plain text, check Settings → Editor → File Types and Settings → Editor → TextMate Bundles before changing the palette.
- For language plugins that expose custom color keys, Settings → Editor → Solarized Osaka Language Colors lets you map keys to Osaka roles. For example, CUSTOM_TOKEN = STRING.
- Binary and image viewers and genuinely unknown extensions do not have a source-code lexer. A theme applies colors to tokens provided by a language plugin, TextMate grammar or custom file type.

## Where to find help

- Tools → Solarized Osaka → Usage Guide or Changelog.
- The same two entries are available in the top Help menu and via Find Action.
