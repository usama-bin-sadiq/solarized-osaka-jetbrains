# Changelog

## 1.7.2

### Fixed

- Restored compatibility with older IntelliJ Platform file editor APIs for project scheme restore and syntax preview.
- Replaced experimental theme enumeration with the stable look-and-feel API.
- Added IntelliJ IDEA Ultimate 2025.2.6.4 (IU-252.28539.149) to compatibility checks.


## 1.7.1

### Fixed

- Made palette labels white on dark backing, including selected palette cards. Improved dark primary and active button text contrast.

### Removed

- Removed Glass presets, editor schemes and the finish control. Existing personal palettes based on Glass move to the matching Quiet UI preset, preserving saved color overrides.


## 1.7.0

### Added

- In-IDE Usage Guide and Changelog in the Help menu and the Solarized Osaka submenu.
- A language sample picker with detected file type information.
- Optional starter TextMate bundles for select file types that WebStorm opened as plain text.

### Fixed

- Improved readable comment and string colors in Yarn lock files and the Properties separator in Dimmed.
- Added WebStorm runtime verification for registered extension patterns, editor schemes and Glass finishes.

## 1.6.0

- Added accent strength, independent editor/tree/menu selections, and contrast-aware color pairing.
- Added favorite and recent palette switching, undo, and saved-preset recovery.
- Improved console and diff colors, reduced live preview refreshes, and expanded the compatibility matrix.

## 1.5.0

- Added live Glass preview, paired light and dark personal palettes, contrast suggestions, palette management, and Quiet chrome.
- Refined Glass edges, Project tree states, Git labels and popup highlights.

## 1.4.0

- Added dark and light Glass themes for Classic and Islands, with a personal finish slider.

## 1.3.0

- Improved contrast for directory, menu and editor states. Added a personal theme editor with a live preview and immediate apply.

## 1.2.2

- Separated JSON property-key and string-value colors in Color Vision and Teal Accent schemes.

## 1.2.1

- Removed Quiet UI background images to avoid IDE resource-loading errors.
- Improved Quiet UI focus, link, and selection accent visibility.

## 1.2.0

- Expanded the theme set to 48 Classic/Islands dark/light variants across 12 palette families.
- Added OLED, High Contrast, Dimmed, Warm, Color Vision, Teal, Purple, Amber, Quiet UI, Vivid, and Muted families.
- Expanded editor token colors for JetBrains languages and community language plugins; added configurable custom token-key mappings.
- Added a 16-color Run/Debug console palette and more VCS status colors to every editor scheme.
- Raised low-contrast syntax comments and added automated contrast/resource checks.
- Added the theme gallery, first-launch chooser notification, optional operating-system light/dark sync, and broader preview samples.
- Added per-project editor-scheme restore while keeping JetBrains UI themes IDE-wide.
- Added JetBrains Mono Compact and Relaxed editor schemes in dark and light variants.
- Added a Gradle wrapper and CI builds/plugin verification for IntelliJ Platform 2024.3 and 2025.3.

## 1.1.1

- Improved context-menu and popup contrast.

## 1.1.0

- Added Islands UI dark and light themes.

## 1.0.0

- Initial release with dark and light UI themes and editor schemes.
