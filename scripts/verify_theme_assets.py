#!/usr/bin/env python3
"""Static integrity and contrast checks for bundled Solarized Osaka assets."""
from collections import Counter
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
plugin = ET.parse(resources / 'META-INF/plugin.xml').getroot()
extensions = plugin.find('extensions')
providers = [e for e in extensions if e.tag == 'themeProvider']
schemes = [e for e in extensions if e.tag == 'bundledColorScheme']
provider_paths = {e.get('path') for e in providers}
scheme_paths = {e.get('path') for e in schemes}
provider_ids = [e.get('id') for e in providers]
scheme_ids = [e.get('id') for e in schemes]
action_ids = [e.get('id') for e in plugin.findall('./actions/action')]
theme_files = sorted((resources / 'themes').glob('*.theme.json'))
scheme_files = sorted((resources / 'themes').glob('SolarizedOsaka*.xml'))
issues = []
matrix = {}

def color(root_el, key, prop, section='attributes'):
    option = root_el.find(f"./{section}/option[@name='{key}']")
    if option is None:
        return None
    if section == 'colors':
        return option.get('value')
    value = option.find(f"./value/option[@name='{prop}']")
    return value.get('value') if value is not None else None

def luminance(value):
    rgb = [int(value[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    rgb = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in rgb]
    return .2126 * rgb[0] + .7152 * rgb[1] + .0722 * rgb[2]

def contrast(a, b):
    hi, lo = sorted((luminance(a), luminance(b)), reverse=True)
    return (hi + .05) / (lo + .05)

for path in theme_files:
    data = json.loads(path.read_text())
    ui = data['ui']
    for widget in ('Tree', 'List', 'Menu', 'MenuItem', 'PopupMenu', 'CompletionPopup'):
        for surface, ink in [('selectionBackground', 'selectionForeground'), ('hoverBackground', 'foreground'),
                             ('selectionInactiveBackground', 'selectionInactiveForeground')]:
            background = ui.get(f'{widget}.{surface}')
            if background is None:
                continue
            foreground = ui.get(f'{widget}.{ink}', ui.get(f'{widget}.foreground', ui['*']['foreground']))
            if contrast(foreground.lstrip('#'), background.lstrip('#')) < 4.5:
                issues.append(f'{path.name}: {widget}.{ink} on {surface} is below 4.5:1')
    if data['dark']:
        for foreground, background in (
            ('Button.default.foreground', 'Button.default.startBackground'),
            ('Button.default.foreground', 'Button.default.endBackground'),
            ('ToggleButton.onForeground', 'ToggleButton.onBackground'),
        ):
            if foreground in ui and background in ui and contrast(ui[foreground][1:], ui[background][1:]) < 4.5:
                issues.append(f'{path.name}: {foreground} on {background} is below 4.5:1')
    theme_path = '/themes/' + path.name
    if theme_path not in provider_paths:
        issues.append(f'unregistered theme: {path.name}')
    name = data.get('name', '')
    if not name:
        issues.append(f'missing theme name: {path.name}')
    if not data.get('editorScheme'):
        issues.append(f'missing editor scheme mapping: {path.name}')
    islands = name.startswith('Solarized Osaka Islands')
    provider = next((entry for entry in providers if entry.get('path') == theme_path), None)
    if provider is not None and islands != (provider.get('targetUi') == 'islands'):
        issues.append(f'UI target does not match theme variant: {path.name}')
    family = name.removeprefix('Solarized Osaka Islands').removeprefix('Solarized Osaka').strip()
    for suffix in (' Light', ' Dark'):
        if family.endswith(suffix):
            family = family[:-len(suffix)].strip()
    family = 'Classic' if family in ('', 'Light') else family
    mode = 'Light' if data.get('dark') is False else 'Dark'
    matrix.setdefault(family, set()).add(('Islands' if islands else 'Classic', mode))
    scheme = data.get('editorScheme')
    if scheme and not (resources / scheme.lstrip('/')).exists():
        issues.append(f'missing editor scheme for {path.name}: {scheme}')
    background = data.get('background', {})
    image = background.get('image') if isinstance(background, dict) else None
    if image and not (resources / image.lstrip('/')).is_file():
        issues.append(f'missing background image for {path.name}: {image}')

for path in scheme_files:
    try:
        scheme = ET.parse(path).getroot()
    except ET.ParseError as error:
        issues.append(f'invalid scheme XML {path.name}: {error}')
        continue
    attr_names = [e.get('name') for e in scheme.findall('./attributes/option')]
    if len(attr_names) != len(set(attr_names)):
        issues.append(f'duplicate editor attribute in {path.name}')
    if '/themes/' + path.name not in scheme_paths:
        issues.append(f'unregistered editor scheme: {path.name}')
    bg = color(scheme, 'TEXT', 'BACKGROUND')
    comment = color(scheme, 'DEFAULT_LINE_COMMENT', 'FOREGROUND')
    if 'ColorVision' in path.name or 'TealAccent' in path.name:
        json_key = color(scheme, 'JSON.PROPERTY_KEY', 'FOREGROUND')
        json_string = color(scheme, 'JSON.STRING', 'FOREGROUND') or color(scheme, 'DEFAULT_STRING', 'FOREGROUND')
        if json_key and json_string and json_key.lower() == json_string.lower():
            issues.append(f'JSON keys and string values share a color in {path.name}')
        if bg and json_string and contrast(bg, json_string) < 4.5:
            issues.append(f'JSON string contrast below 4.5:1 in {path.name}')
    if bg and comment and contrast(bg, comment) < 4.5:
        issues.append(f'comment contrast below 4.5:1 in {path.name}')
    selection_bg = color(scheme, 'SELECTION_BACKGROUND', 'value', section='colors')
    selection_fg = color(scheme, 'SELECTION_FOREGROUND', 'value', section='colors')
    if selection_bg and selection_fg and contrast(selection_bg, selection_fg) < 4.5:
        issues.append(f'selection contrast below 4.5:1 in {path.name}')
    for attr in ('CUSTOM_LINE_COMMENT_ATTRIBUTES', 'CUSTOM_STRING_ATTRIBUTES', 'PROPERTIES.KEY_VALUE_SEPARATOR'):
        ink = color(scheme, attr, 'FOREGROUND')
        if not ink or not bg or contrast(ink, bg) < 4.5:
            issues.append(f'{path.name}: {attr} is below 4.5:1 against editor background')
    console_surface = color(scheme, 'CONSOLE_BACKGROUND_KEY', 'value', section='colors')
    for attr in attr_names:
        if attr.startswith(('CONSOLE_', 'LOG_')):
            foreground = color(scheme, attr, 'FOREGROUND')
            if foreground and console_surface and contrast(foreground, console_surface) < 4.5:
                issues.append(f'{path.name}: {attr} below 4.5:1 on console background')
    diff_markers = {'DIFF_INSERTED': '1', 'DIFF_DELETED': '3', 'DIFF_MODIFIED': '1', 'DIFF_CONFLICT': '2'}
    for attr, marker in diff_markers.items():
        # The diff renderer uses FOREGROUND as the ignored-fragment *background*.
        ink = color(scheme, 'TEXT', 'FOREGROUND')
        surfaces = [color(scheme, attr, prop) for prop in ('BACKGROUND', 'FOREGROUND')]
        if not ink or any(not surface or contrast(ink, surface) < 4.5 for surface in surfaces):
            issues.append(f'{path.name}: {attr} does not have readable line and fragment colors')
        if color(scheme, attr, 'EFFECT_TYPE') != marker or not color(scheme, attr, 'EFFECT_COLOR'):
            issues.append(f'{path.name}: {attr} has no distinct diff marker')
    expected_console = {
        'CONSOLE_BLACK_OUTPUT', 'CONSOLE_DARKGRAY_OUTPUT', 'CONSOLE_RED_OUTPUT',
        'CONSOLE_RED_BRIGHT_OUTPUT', 'CONSOLE_GREEN_OUTPUT', 'CONSOLE_GREEN_BRIGHT_OUTPUT',
        'CONSOLE_YELLOW_OUTPUT', 'CONSOLE_YELLOW_BRIGHT_OUTPUT', 'CONSOLE_BLUE_OUTPUT',
        'CONSOLE_BLUE_BRIGHT_OUTPUT', 'CONSOLE_MAGENTA_OUTPUT', 'CONSOLE_MAGENTA_BRIGHT_OUTPUT',
        'CONSOLE_CYAN_OUTPUT', 'CONSOLE_CYAN_BRIGHT_OUTPUT', 'CONSOLE_GRAY_OUTPUT', 'CONSOLE_WHITE_OUTPUT'
    }
    actual = set(attr_names)
    if not expected_console.issubset(actual):
        issues.append(f'incomplete console palette in {path.name}: {sorted(expected_console - actual)}')

if len(theme_files) != 48 or len(providers) != 48:
    issues.append(f'expected 48 registered themes, found {len(theme_files)} files and {len(providers)} registrations')
if len(provider_ids) != len(set(provider_ids)):
    issues.append('duplicate theme provider IDs')
if len(scheme_ids) != len(set(scheme_ids)):
    issues.append('duplicate bundled color scheme IDs')
if len(action_ids) != len(set(action_ids)):
    issues.append('duplicate action IDs')
theme_names = [json.loads(path.read_text()).get('name') for path in theme_files]
if len(theme_names) != len(set(theme_names)):
    issues.append('duplicate theme names')
if len(scheme_files) != len(schemes):
    issues.append(f'scheme file/registration mismatch: {len(scheme_files)} files, {len(schemes)} registrations')
if {f'/themes/{path.name}' for path in theme_files} != provider_paths:
    issues.append('theme files and provider registration paths do not match exactly')
if {f'/themes/{path.name}' for path in scheme_files} != scheme_paths:
    issues.append('editor scheme files and registration paths do not match exactly')
if len(matrix) != 12 or any(len(combinations) != 4 for combinations in matrix.values()):
    issues.append('theme family matrix is incomplete')

version = plugin.findtext('version')
properties = (root / 'gradle.properties').read_text()
match = re.search(r'^pluginVersion\s*=\s*(\S+)', properties, re.M)
if not match or match.group(1) != version:
    issues.append(f'plugin version mismatch: descriptor={version}, properties={match.group(1) if match else "missing"}')

preset_index = (resources / 'themes/osaka-presets.txt').read_text().splitlines()
if sorted(preset_index) != sorted(p.name for p in theme_files):
    issues.append('personal theme chooser does not list every bundled preset exactly once')

if issues:
    print('\n'.join(f'ERROR: {issue}' for issue in issues))
    sys.exit(1)
print(f'OK: {len(theme_files)} themes, {len(scheme_files)} schemes, 12 complete family matrices, contrast and ANSI checks passed.')
