#!/usr/bin/env python3
"""Reject the bytecode references that broke older IntelliJ Platform builds."""
from io import BytesIO
from pathlib import Path
import sys
from zipfile import ZipFile

if len(sys.argv) < 2:
    raise SystemExit('Usage: verify_plugin_api.py plugin.zip [...]')

for archive in map(Path, sys.argv[1:]):
    class_count = 0
    with ZipFile(archive) as outer:
        for entry in outer.namelist():
            if not entry.endswith('.jar'):
                continue
            with ZipFile(BytesIO(outer.read(entry))) as jar:
                for path in jar.namelist():
                    if not path.startswith('com/craftzdog/solarizedosaka/') or not path.endswith('.class'):
                        continue
                    class_count += 1
                    data = jar.read(path)
                    if b'com/intellij/openapi/fileEditor/FileEditorManager$Companion' in data:
                        raise SystemExit(f'{archive}: {path} references the newer FileEditorManager companion')
                    if b'getInstalledThemes' in data:
                        raise SystemExit(f'{archive}: {path} references experimental theme enumeration')
    if class_count == 0:
        raise SystemExit(f'{archive}: no Solarized Osaka bytecode was found')
    print(f'{archive}: checked {class_count} classes; reported incompatible references absent')
