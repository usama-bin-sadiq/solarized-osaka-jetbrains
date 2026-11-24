"""Text and surface pairs explicitly provided by the bundled theme assets."""
import re

HEX_COLOR = re.compile(r'#?[0-9a-fA-F]{6}$')


def is_color(value):
    return isinstance(value, str) and HEX_COLOR.fullmatch(value) is not None


def luminance(value):
    value = value.lstrip('#')
    rgb = [int(value[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    linear = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in rgb]
    return sum(v * weight for v, weight in zip(linear, (.2126, .7152, .0722)))


def contrast(ink, surface):
    hi, lo = sorted((luminance(ink), luminance(surface)), reverse=True)
    return (hi + .05) / (lo + .05)


def ui_text_surfaces(ui):
    """Map each text key to all of its declared normal and interactive surfaces."""
    pairs = {}
    override = {
        'Button.default.foreground': ('Button.default.startBackground', 'Button.default.endBackground'),
        'Button.foreground': ('Button.background', 'Button.startBackground', 'Button.endBackground'),
        'Plugins.Button.installForeground': ('Plugins.Button.installBackground',),
        'Plugins.Button.installFillForeground': ('Plugins.Button.installFillBackground',),
        'Plugins.Button.updateForeground': ('Plugins.Button.updateBackground',),
        'CompletionPopup.matchForeground': ('CompletionPopup.background',),
        'ParameterInfo.currentParameterForeground': ('ParameterInfo.background',),
        'TitlePane.inactiveInfoForeground': ('TitlePane.inactiveBackground',),
        'Label.selectedForeground': ('List.selectionBackground',),
    }
    for key, ink in ui.items():
        if not key.lower().endswith('foreground') or key.startswith('ProgressBar.') or not is_color(ink):
            continue
        suffix = 'Background' if key.endswith('Foreground') else 'background'
        component = key.rsplit('.', 1)[0]
        state = key[len(component) + 1:-len('foreground')]
        backgrounds = override.get(key, (key[:-len('foreground')] + suffix,))
        backgrounds = tuple(bg for bg in backgrounds if is_color(ui.get(bg)))
        if not backgrounds:
            if state.lower().endswith('selection'):
                backgrounds = (component + '.selectionBackground',)
            elif key.startswith('Link.'):
                backgrounds = ('Panel.background', 'Popup.background')
            elif key.startswith('Debugger.Variables.'):
                backgrounds = ('Tree.background',)
            elif key.startswith(('Vcs.Log.', 'VersionControl.Log.')):
                backgrounds = ('Table.background',)
            elif key.startswith('SearchEverywhere.List.'):
                backgrounds = ('List.background',)
            else:
                backgrounds = (component + '.background',)
            backgrounds = tuple(bg for bg in backgrounds if is_color(ui.get(bg)))
        if not backgrounds:
            backgrounds = ('Panel.background',)
        if not state and component in ('Tree', 'List', 'MenuItem', 'StatusBar', 'TextField', 'RunWidget'):
            backgrounds += tuple(component + '.' + s for s in ('hoverBackground', 'hoverInactiveBackground')
                                 if is_color(ui.get(component + '.' + s)))
        pairs[key] = backgrounds
    return pairs


def editor_text_surfaces(scheme):
    """Yield explicit text attributes and their surfaces, skipping renderer-only colors."""
    colors = {option.get('name'): option.get('value') for option in scheme.findall('./colors/option')}
    attrs = {
        option.get('name'): {entry.get('name'): entry.get('value') for entry in option.findall('./value/option')}
        for option in scheme.findall('./attributes/option')
    }
    paper = attrs['TEXT']['BACKGROUND']
    for name, options in attrs.items():
        ink = options.get('FOREGROUND')
        if not is_color(ink) or name.startswith('DIFF_'):
            continue
        surface = options.get('BACKGROUND')
        if not is_color(surface):
            surface = colors.get('CONSOLE_BACKGROUND_KEY', paper) if name.startswith(('CONSOLE_', 'LOG_')) else paper
        if is_color(surface):
            yield name, ink, surface


def caret_row_inks(scheme):
    """The active editor line uses the caret-row surface behind these syntax inks."""
    for name, ink, _ in editor_text_surfaces(scheme):
        if name.startswith(('CONSOLE_', 'LOG_', 'BREADCRUMBS_')):
            continue
        explicit_background = scheme.find(f"./attributes/option[@name='{name}']/value/option[@name='BACKGROUND']")
        if name == 'TEXT' or explicit_background is None:
            yield name, ink
