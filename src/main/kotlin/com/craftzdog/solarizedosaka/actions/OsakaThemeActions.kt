package com.craftzdog.solarizedosaka.actions

import com.craftzdog.solarizedosaka.ProjectEditorSchemeService
import com.craftzdog.solarizedosaka.ProjectThemePreferences
import com.craftzdog.solarizedosaka.ThemeCatalog
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import com.intellij.testFramework.LightVirtualFile

class SwitchOsakaBrightnessAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val current = ThemeCatalog.current()
        event.presentation.isEnabledAndVisible = current?.name?.startsWith("Solarized Osaka") == true
    }

    override fun actionPerformed(event: AnActionEvent) {
        val current = ThemeCatalog.current() ?: return
        ThemeCatalog.applyBrightness(ThemeCatalog.toggleBrightness(current.name, current.isDark))
    }
}

class OpenThemeGalleryAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(event.project, "com.craftzdog.solarizedosaka.themeGallery")
    }
}

class SaveProjectThemeAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val isOsaka = LafManager.getInstance().currentUIThemeLookAndFeel?.name?.startsWith("Solarized Osaka") == true
        event.presentation.isEnabledAndVisible = event.project != null && isOsaka
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val preferences = project.getService(ProjectThemePreferences::class.java).state
        preferences.themeName = ThemeCatalog.current()?.name.orEmpty()
        project.getService(ProjectEditorSchemeService::class.java).saveCurrentScheme()
    }
}

class ApplyProjectThemeAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val project = event.project
        event.presentation.isEnabledAndVisible = project != null &&
            project.getService(ProjectThemePreferences::class.java).state.themeName.isNotBlank()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val preferences = project.getService(ProjectThemePreferences::class.java).state
        ThemeCatalog.apply(preferences.themeName) // UI themes are IDE-wide; the editor scheme is scoped to this project.
        project.getService(ProjectEditorSchemeService::class.java).restoreSavedScheme()
    }
}

class OpenOsakaPreviewAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val manager = FileEditorManager.getInstance(project)
        val typeManager = FileTypeManager.getInstance()
        val sorted = samples.sortedBy { it.name.substringAfterLast('.').lowercase() }
        val chooser = JComboBox(sorted.toTypedArray())
        val detail = JLabel()
        fun selected(): Sample = chooser.selectedItem as Sample
        fun previewFile(sample: Sample) = LightVirtualFile(sample.name, sample.content)
        fun refresh() {
            val type = typeManager.getFileTypeByFile(previewFile(selected()))
            detail.text = when (type.name) {
                "textmate" -> "Detected: TextMate grammar"
                "AUTO_DETECTED", "UNKNOWN", "PLAIN_TEXT" -> "Detected: plain text or content detection"
                else -> "Detected: ${type.displayName}"
            }
        }
        chooser.selectedItem = sorted.first { it.name.endsWith(".json") }
        chooser.addActionListener { refresh() }
        refresh()
        val panel = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))).apply {
            add(JLabel("Sample file").also { it.labelFor = chooser }, BorderLayout.NORTH)
            add(chooser, BorderLayout.CENTER)
            add(detail, BorderLayout.SOUTH)
        }
        object : DialogWrapper(project) {
            init {
                title = "Solarized Osaka Syntax Preview"
                setOKButtonText("Open sample")
                init()
            }

            override fun createCenterPanel(): JComponent = panel
            override fun getPreferredFocusedComponent(): JComponent = chooser

            override fun doOKAction() {
                val sample = selected()
                val existing = manager.openFiles.filterIsInstance<LightVirtualFile>().firstOrNull {
                    it.name == sample.name && it.content.toString() == sample.content
                }
                manager.openFile(existing ?: previewFile(sample), true)
                super.doOKAction()
            }
        }.show()
    }

    private data class Sample(val name: String, val content: String) {
        override fun toString() = name
    }

    private val samples = listOf(
        Sample("yarn.lock", "# yarn lockfile v1\n\nexample@^1.0.0:\n  version \"1.2.0\"\n"),
        Sample("OsakaPreview.properties", "# Palette\ntheme = Osaka\nenabled = true\n"),
        Sample("OsakaPreview.vue", "<template><div class=\"glass\">{{ name }}</div></template>\n<script setup lang=\"ts\">const name = \"Osaka\"</script>\n"),
        Sample("OsakaPreview.nim", "# Palette\nproc shade(): string = \"Osaka\"\n"),
        Sample("OsakaPreview.zig", "// Palette\npub const level: u32 = 42;\n"),
        Sample("OsakaPreview.osaka-unregistered", "This file has no registered syntax.\nname = \"Osaka\"\n"),
        Sample("OsakaPreview.java", """// TODO: Java, annotations, and comments
import java.util.List;

@Deprecated
class OsakaTheme {
    private static final int COUNT = 42;
    String greet(String name) { return "Hello, " + name + COUNT; }
}
"""),
        Sample("OsakaPreview.kt", """// TODO: Kotlin, strings, and numbers
data class OsakaTheme(val name: String, val enabled: Boolean = true)

fun greet(theme: OsakaTheme): String {
    val count = 42
    return "Hello, ${'$'}{theme.name}! (${ '$' }count)"
}
"""),
        Sample("OsakaPreview.py", """@dataclass
class OsakaTheme:
    name: str
    enabled: bool = True

def greet(theme: OsakaTheme) -> str:
    count = 42
    return f"Hello, {theme.name}! ({count})"  # TODO: inspect strings
"""),
        Sample("OsakaPreview.ts", """type OsakaTheme = { name: string; enabled: boolean };

const greet = (theme: OsakaTheme): string => {
  const count = 42;
  return `Hello, ${'$'}{theme.name}! (${ '$' }count)`;
};
"""),
        Sample("OsakaPreview.go", """package main

import "fmt"

type OsakaTheme struct { Name string; Enabled bool }

func greet(theme OsakaTheme) string {
    count := 42
    return fmt.Sprintf("Hello, %s! (%d)", theme.Name, count)
}
"""),
        Sample("OsakaPreview.rs", """struct OsakaTheme { name: String, enabled: bool }

fn greet(theme: &OsakaTheme) -> String {
    let count = 42;
    format!("Hello, {}! ({})", theme.name, count)
}
"""),
        Sample("OsakaPreview.php", """<?php
final class OsakaTheme {
    public function greet(string ${'$'}name): string {
        ${'$'}count = 42;
        return "Hello, ${'$'}name (${ '$' }count)"; // TODO: preview PHP
    }
}
"""),
        Sample("OsakaPreview.rb", """class OsakaTheme
  def greet(name)
    count = 42
    "Hello, #{name} (#{count})" # TODO: preview Ruby
  end
end
"""),
        Sample("OsakaPreview.sh", """#!/usr/bin/env bash
# TODO: shell highlighting
THEME="Solarized Osaka"
COUNT=42
printf '%s (%s)\\n' "${'$'}THEME" "${'$'}COUNT"
"""),
        Sample("OsakaPreview.yaml", """theme:
  name: Solarized Osaka
  enabled: true
  count: 42
"""),
        Sample("OsakaPreview.html", """<!doctype html>
<main class="osaka-preview">
  <h1>Solarized Osaka</h1>
  <p data-count="42">Preview <strong>markup</strong> and attributes.</p>
</main>
"""),
        Sample("OsakaPreview.json", """{
  "theme": "Solarized Osaka",
  "enabled": true,
  "count": 42
}
"""),
        Sample("OsakaPreview.lua", """-- TODO: Lua through a community language plugin
local theme = { name = "Solarized Osaka", enabled = true }
local count = 42
print(theme.name, count)
"""),
        Sample("OsakaPreview.tf", """# HCL/Terraform example
variable "theme_name" { default = "Solarized Osaka" }
locals { count = 42, enabled = true }
"""),
        Sample("OsakaPreview.hs", """-- Haskell example
data Theme = Theme { name :: String, enabled :: Bool }
greet theme = "Hello, " ++ name theme
"""),
        Sample("OsakaPreview.ex", """# Elixir example
defmodule OsakaTheme do
  @count 42
  def greet(name), do: "Hello, #{name} (#{@count})"
end
"""),
        Sample("OsakaPreview.sql", """-- SQL syntax colors
SELECT theme_name, COUNT(*) AS installs
FROM plugin_events
WHERE enabled = TRUE
GROUP BY theme_name
ORDER BY installs DESC;
"""),
        Sample("OsakaPreview.md", """# Solarized Osaka preview

**Bold**, *italic*, `inline code`, and [links](https://plugins.jetbrains.com/).

- TODO comments
- Search and selection colors
- Diff and warning accents
""")
    )
}
