#!/usr/bin/env bash
# Runs the audit against an installed WebStorm 2026.3 in a fresh profile.
set -euo pipefail
source_dir="$(cd "$(dirname "$0")/.." && pwd)"
archive="${1:?Usage: bash scripts/audit-webstorm.sh plugin.zip [WebStorm.app]}"
app="${2:-/Applications/WebStorm.app}"
contents="$app/Contents"
textmate="$contents/plugins/textmate-plugin/lib"
profile="$(mktemp -d "${TMPDIR:-/tmp}/osaka-webstorm-audit.XXXXXX")"

mkdir -p "$profile"/{config,system,log,plugins/Review/lib,classes/META-INF,project,optional-bundles}
printf '%s\n' 'Running isolated audit at:' "$profile"
if [ -n "${WEBSTORM_AUDIT_LANGUAGE_PLUGINS:-}" ]; then
    for plugin in prisma vuejs-plugin markdown 'Shell Script' properties; do
        source="$WEBSTORM_AUDIT_LANGUAGE_PLUGINS/$plugin"
        if [ -d "$source" ]; then ln -s "$source" "$profile/plugins/$plugin"; fi
    done
fi
python3 "$source_dir/scripts/webstorm-audit/fixtures.py" "$profile" "$app"
unzip -q "$archive" -d "$profile/plugins"
plugin_jar="$(find "$profile/plugins/solarized-osaka-intellij/lib" -maxdepth 1 -name '*.jar' ! -name '*searchableOptions*' -print)"
python3 - "$plugin_jar" "$profile" <<'PY'
from pathlib import Path
from zipfile import ZipFile
from xml.etree import ElementTree as ET
import io, sys
out = Path(sys.argv[2])
with ZipFile(sys.argv[1]) as jar:
    root = ET.fromstring(jar.read('META-INF/plugin.xml'))
    (out / 'schemes.txt').write_text(''.join(e.get('id')+'\n' for e in root.findall('.//bundledColorScheme')))
    with ZipFile(io.BytesIO(jar.read('osaka-help/solarized-osaka-syntax-bundles.zip'))) as bundles:
        for member in bundles.infolist():
            relative = Path(member.filename)
            if relative.is_absolute() or '..' in relative.parts:
                raise ValueError('Unsafe bundle member')
            bundles.extract(member, out / 'optional-bundles')
PY
cat > "$profile/classes/META-INF/plugin.xml" <<'XML'
<idea-plugin>
    <id>osaka.local.webstorm.audit</id>
    <name>Solarized Osaka Local Audit</name>
    <vendor>Local audit</vendor>
    <version>1</version>
    <depends>com.intellij.modules.platform</depends>
    <depends>com.craftzdog.solarizedosaka</depends>
    <depends>org.jetbrains.plugins.textmate</depends>
    <extensions defaultExtensionNs="com.intellij">
        <postStartupActivity implementation="review.Review"/>
    </extensions>
</idea-plugin>
XML
printf '%s\n' '{"theme":"Osaka","enabled":true,"profile":{"accent":"teal","finish":50}}' > "$profile/project/palette.json"
javac --release 21 -cp "$contents/lib/*:$textmate/*:$textmate/modules/*:$plugin_jar" \
    -d "$profile/classes" "$source_dir/scripts/webstorm-audit/Review.java"
jar cf "$profile/plugins/Review/lib/audit.jar" -C "$profile/classes" .
"$contents/MacOS/webstorm" -Didea.config.path="$profile/config" -Didea.system.path="$profile/system" \
    -Didea.plugins.path="$profile/plugins" -Didea.log.path="$profile/log" -Didea.trust.all.projects=true \
    "$profile/project" "$profile/project/palette.json"
if [ -e "$profile/failure" ]; then
    cat "$profile/failure" >&2
    exit 1
fi
test -e "$profile/passed"
printf 'Audit CSVs and screenshots are in %s\n' "$profile"
