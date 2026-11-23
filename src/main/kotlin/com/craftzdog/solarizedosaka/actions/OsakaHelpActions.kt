package com.craftzdog.solarizedosaka.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JEditorPane

class OsakaUsageGuideAction : AnAction(), DumbAware {
    override fun actionPerformed(event: AnActionEvent) = OsakaHelp.show(event.project, false)
}

class OsakaChangelogAction : AnAction(), DumbAware {
    override fun actionPerformed(event: AnActionEvent) = OsakaHelp.show(event.project, true)
}

internal object OsakaHelp {
    fun show(project: Project?, changelog: Boolean) {
        val document = if (changelog) "CHANGELOG.md" else "USAGE.md"
        val content = requireNotNull(javaClass.getResourceAsStream("/osaka-help/$document"))
            .bufferedReader(Charsets.UTF_8).use { it.readLines() }
        val page = JEditorPane("text/html", html(content)).apply {
            isEditable = false
            caretPosition = 0
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            border = JBUI.Borders.empty(8, 12)
        }
        val scroll = JBScrollPane(page).apply { preferredSize = JBUI.size(650, 530) }
        object : DialogWrapper(project) {
            init {
                title = if (changelog) "Solarized Osaka Changelog" else "Solarized Osaka Usage Guide"
                setOKButtonText("Close")
                init()
            }

            override fun createCenterPanel(): JComponent = scroll
            override fun createActions() = arrayOf(okAction)
        }.show()
    }

    private fun html(lines: List<String>) = buildString {
        append("<html><body>")
        for (line in lines) {
            val safe = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            when {
                line.startsWith("### ") -> append("<h3>${safe.drop(4)}</h3>")
                line.startsWith("## ") -> append("<h2>${safe.drop(3)}</h2>")
                line.startsWith("# ") -> append("<h1>${safe.drop(2)}</h1>")
                line.startsWith("- ") -> append("<p>• ${safe.drop(2)}</p>")
                line.isNotBlank() -> append("<p>$safe</p>")
            }
        }
        append("</body></html>")
    }
}

class ExportSyntaxBundlesAction : AnAction(), DumbAware {
    override fun actionPerformed(event: AnActionEvent) {
        val chooser = javax.swing.JFileChooser().apply {
            dialogTitle = "Export Solarized Osaka Syntax Bundles"
            selectedFile = java.io.File("solarized-osaka-syntax-bundles.zip")
        }
        if (chooser.showSaveDialog(null) != javax.swing.JFileChooser.APPROVE_OPTION) return
        val file = chooser.selectedFile
        val target = if (file.name.endsWith(".zip", ignoreCase = true)) file.toPath()
            else file.toPath().resolveSibling(file.name + ".zip")
        try {
            requireNotNull(javaClass.getResourceAsStream("/osaka-help/solarized-osaka-syntax-bundles.zip")).use {
                java.nio.file.Files.copy(it, target)
            }
            com.intellij.openapi.ui.Messages.showInfoMessage(event.project,
                "Extract the ZIP and add the bundle folders you want in Settings → Editor → TextMate Bundles.",
                "Syntax Bundles Exported")
        } catch (error: Exception) {
            com.intellij.openapi.ui.Messages.showErrorDialog(event.project,
                error.message ?: "The syntax bundles could not be exported.", "Export Syntax Bundles")
        }
    }
}
