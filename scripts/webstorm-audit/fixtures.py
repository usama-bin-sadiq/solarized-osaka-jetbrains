"""Build small, inert text fixtures for a JetBrains FileType and TextMate coverage sweep."""
from pathlib import Path
import base64
import json
import sys

ROOT = Path(sys.argv[2]) / 'Contents/plugins/textmate-plugin/lib/bundles'
OUT = Path(sys.argv[1])
fixtures = {}
def add(extensions, text):
    for ext in extensions.split():
        fixtures[ext] = text

add('js jsx mjs cjs es es6 ts tsx mts cts', '// Palette sample\nexport function shade(name = "Osaka", level = 42) { return { name, level, active: true }; }\n')
add('json json5 jsonc jsonl ndjson webmanifest har ipynb prettierrc babelrc eslintrc', '{"theme": "Osaka", "enabled": true, "count": 42, "items": [null, "Glass"]}\n')
add('vue svelte astro', '<script>const shade = "Osaka";</script>\n<div class="card">{shade}</div>\n<style>.card { color: #336677; }</style>\n')
add('html htm xhtml jsp php erb ejs njk twig liquid hbs handlebars mustache pug jade', '<!doctype html>\n<!-- Palette -->\n<div class="sample" data-mode="glass">{{ shade }}</div>\n')
add('css scss sass less styl', '/* Surface */\n:root { --tone: #336677; }\n.card:hover { color: var(--tone); margin: 12px; }\n')
add('yaml yml', '# Palette\ntheme: "Osaka"\nenabled: true\ncount: 42\nitems:\n  - glass\n')
add('md markdown mdown mkd mdx rmd qmd', '# Palette\n**Glass** and `Osaka`\n[Preview](https://example.invalid)\n```js\nconst shade = "Osaka";\n```\n')
add('xml svg xsd xsl xslt wsdl plist pom iml csproj fsproj vbproj resx', '<?xml version="1.0"?>\n<!-- Palette -->\n<theme mode="glass"><name>Osaka</name></theme>\n')
add('py pyw pyi pyx pxd', '# Palette\ndef shade(name: str = "Osaka", level: int = 42):\n    return { "name": name, "active": True }\n')
add('sh bash zsh fish ksh command tmux', '#!/usr/bin/env bash\n# Palette\nname="Osaka"\nprintf "%s %d\\n" "$name" 42\n')
add('java kt kts scala groovy gradle dart', '// Palette\nclass Theme { public static final String name = "Osaka"; int level = 42; }\n')
add('c h cc cpp cxx hpp hxx mm m objc cs d swift', '// Palette\nconst char *shade = "Osaka";\nint main() { return 42; }\n')
add('go', 'package demo\n// Palette\nfunc Shade() string { return "Osaka" }\n')
add('rs', '// Palette\npub fn shade() -> &\'static str { "Osaka" }\nconst LEVEL: usize = 42;\n')
add('rb rake gemspec', '# Palette\ndef shade(name = "Osaka")\n  { name: name, level: 42 }\nend\n')
add('php phtml php3 php4 php5', '<?php\n// Palette\nfunction shade(string $name = "Osaka"): string { return $name; }\n')
add('lua luau', '-- Palette\nlocal shade = "Osaka"\nreturn { name = shade, level = 42 }\n')
add('ex exs', '# Palette\ndefmodule Theme do\n  def shade, do: "Osaka"\nend\n')
add('erl hrl', '% Palette\n-module(theme).\nshade() -> "Osaka".\n')
add('hs lhs', '-- Palette\nshade :: String\nshade = "Osaka"\n')
add('lisp lsp el clj cljs cljc edn scm ss racket', ';; Palette\n(define shade "Osaka")\n(list shade 42)\n')
add('sql ddl dml psql', "-- Palette\nSELECT name, 42 AS level FROM themes WHERE name = 'Osaka';\n")
add('graphql gql', '# Palette\nquery Theme { theme(name: "Osaka") { id name } }\n')
add('proto', 'syntax = "proto3";\n// Palette\nmessage Theme { string name = 1; }\n')
add('tf tfvars hcl', '# Palette\nvariable "shade" { default = "Osaka" }\n')
add('toml ini cfg conf properties env editorconfig gitconfig desktop', '# Palette\n[theme]\nname = "Osaka"\nlevel = 42\nactive = true\n')
add('nix', '# Palette\n{ name = "Osaka"; level = 42; }\n')
add('ps1 psm1 psd1', '# Palette\n$name = "Osaka"\nWrite-Output $name\n')
add('bat cmd', '@echo off\nREM Palette\nset "shade=Osaka"\necho %shade%\n')
add('dockerfile containerfile', '# Palette\nFROM scratch\nENV SHADE="Osaka"\n')
add('makefile mk mak', '# Palette\nshade := Osaka\nall:\n\t@echo "$(shade)"\n')
add('vue', '<template><div :title="name">{{ name }}</div></template>\n<script setup lang="ts">const name = "Osaka"</script>\n<style scoped>.card { color: #356; }</style>\n')
add('prisma', '// Palette\nmodel Theme {\n  id Int @id\n  name String @default("Osaka")\n}\n')
add('csv tsv', 'name,level,enabled\nOsaka,42,true\n')
add('diff patch', '--- a/palette\n+++ b/palette\n@@ -1 +1 @@\n-theme=old\n+theme=Osaka\n')
add('tex latex', '% Palette\n\\section{Osaka}\n\\textbf{Glass} $42$\n')
add('cl cr nim zig v vlang ml mli f90 f95 fs fsx fsproj vb asm s asm6502 f as ada adb ads cob pas bas', '// Palette\nlet shade = "Osaka"\nif shade != "" then 42 true end\n')
add('r jl matlab oct', '# Palette\nshade <- "Osaka"\nif (TRUE) print(shade)\n')
add('rst', 'Palette\n=======\n\n**Osaka** and ``Glass``\n')
add('http rest', '### Palette\nGET https://example.invalid/themes\nAccept: application/json\n')
add('feature', 'Feature: Palette\n  Scenario: Glass\n    Given a theme "Osaka"\n')
add('kt kts', '// Palette\nfun shade(name: String = "Osaka"): Int = 42\n')
add('go.mod', 'module example.invalid/theme\n\ngo 1.26\n')
add('yarn.lock', '# yarn lockfile v1\n\nexample@^1.0.0:\n  version "1.2.0"\n  resolved "https://example.invalid/example.tgz"\n')
add('luau', '-- Palette\nlocal function shade(level: number): string\n  return "Osaka"\nend\n')
add('nim', '# Palette\nproc shade(level: int = 42): string = "Osaka"\n')
add('zig', '// Palette\npub const level: u32 = 42;\npub fn shade() []const u8 { return "Osaka"; }\n')
add('ml mli', '(* Palette *)\nlet shade : string = "Osaka"\nlet count = 42\n')
add('adb ads', '-- Palette\nprocedure Theme is\n  Name : constant String := "Osaka";\n  Level : constant Integer := 42;\nbegin\n  null;\nend Theme;\n')
add('pas pp', "program Theme;\n{ Palette }\nconst Name = 'Osaka';\nvar Level: Integer;\nbegin\n  Level := 42;\nend.\n")
add('txt log', 'Palette survey\nOsaka Glass 42 true\n')
generic='// Palette comment\n# Surface comment\nconst shade = "Osaka";\nif (true) { return 42; }\n<theme key="glass" />\n'
def encode(text): return base64.b64encode(text.encode()).decode()
def write(name, data):
    (OUT / name).write_text(''.join(f'{key}\t{encode(value)}\n' for key,value in data.items()))
write('fixtures.tsv', fixtures)
write('default.tsv', {'generic': generic})
candidates = {}
for p in sorted(ROOT.glob('*/package.json')):
    try: langs=json.loads(p.read_text()).get('contributes',{}).get('languages',[])
    except (ValueError, OSError): continue
    for language in langs:
        for ext in language.get('extensions',[]):
            if ext.startswith('.'): candidates['*'+ext] = p.parent.name
        for name in language.get('filenames',[]): candidates[name] = p.parent.name
for ext in 'rs go swift hs ex exs ml mli nim zig v dart pas adb cb ash vhd sol move luau dhall gleam roc cairo wgsl glsl wat wasm osaka-unregistered'.split():
    candidates.setdefault('*.'+ext,'extra-probe')
(OUT/'additional.tsv').write_text(''.join(f'{key}\t{value}\n' for key,value in sorted(candidates.items())))
