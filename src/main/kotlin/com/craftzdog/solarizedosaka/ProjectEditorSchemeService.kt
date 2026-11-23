package com.craftzdog.solarizedosaka

import com.craftzdog.solarizedosaka.actions.PersonalThemes
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class ProjectEditorSchemeService(private val project: Project) : Disposable {
    init {
        project.messageBus.connect(this).apply {
            subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    applyToFile(source, file)
                }
            })
            subscribe(EditorColorsManager.TOPIC, EditorColorsListener { applySavedScheme() })
        }
    }

    fun currentEditorSchemeName(): String =
        EditorFactory.getInstance().allEditors.firstOrNull { it.project == project }?.let { (it as? EditorEx)?.colorsScheme?.name }
            ?: EditorColorsManager.getInstance().globalScheme.name

    fun saveCurrentScheme() {
        project.getService(ProjectThemePreferences::class.java).state.editorSchemeName = currentEditorSchemeName()
    }

    fun restoreSavedScheme() = applySavedScheme()

    fun applySavedScheme() {
        if (project.isDisposed) return
        val name = project.getService(ProjectThemePreferences::class.java).state.editorSchemeName
        val scheme = PersonalThemes.instance().projectScheme(name) ?: return
        EditorFactory.getInstance().allEditors
            .filter { it.project == project }
            .forEach { (it as? EditorEx)?.setColorsScheme(scheme) }
        val manager = PlatformFileEditors.get(project)
        manager.openFiles.forEach { applyToFile(manager, it, scheme) }
    }

    private fun applyToFile(manager: FileEditorManager, file: VirtualFile, scheme: EditorColorsScheme? = null) {
        val selected = scheme ?: PersonalThemes.instance().projectScheme(
            project.getService(ProjectThemePreferences::class.java).state.editorSchemeName
        ) ?: return
        manager.getEditors(file).filterIsInstance<TextEditor>().forEach { textEditor ->
            (textEditor.editor as? EditorEx)?.setColorsScheme(selected)
        }
    }

    override fun dispose() {}
}
