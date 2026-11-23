package com.craftzdog.solarizedosaka.actions

import com.craftzdog.solarizedosaka.ThemeCatalog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*

class QuickPaletteAction : AnAction(), DumbAware {
    private data class Choice(val key: String, val title: String, val group: String) {
        override fun toString() = "$title  ·  $group"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val service = PersonalThemes.instance()
        val choices = ThemeCatalog.all().associate { "preset:${it.name}" to it.name } +
            service.state.profiles.keys.associate { "personal:$it" to "$it  (Personal)" }
        val model = DefaultListModel<Choice>()
        val list = JBList(model).apply { selectionMode = ListSelectionModel.SINGLE_SELECTION }
        val favorite = JButton("Add to favorites")
        fun populate(selected: String? = null) {
            val favorites = service.state.favorites
            val recent = service.state.recent
            val keys = (favorites + recent + choices.keys.sorted()).distinct().filter { it in choices }
            model.clear()
            for (key in keys) {
                val group = when (key) {
                    in favorites -> "Favorite"
                    in recent -> "Recent"
                    else -> if (key.startsWith("personal:")) "Saved palette" else "Preset"
                }
                model.addElement(Choice(key, choices.getValue(key), group))
            }
            list.selectedIndex = keys.indexOf(selected).takeIf { it >= 0 } ?: 0
        }
        list.addListSelectionListener {
            favorite.isEnabled = list.selectedValue != null
            favorite.text = if (list.selectedValue?.key in service.state.favorites) "Remove favorite" else "Add to favorites"
        }
        favorite.addActionListener {
            val key = list.selectedValue?.key ?: return@addActionListener
            if (!service.state.favorites.remove(key)) service.state.favorites.add(key)
            populate(key)
        }
        populate(service.state.active.takeIf { it.isNotBlank() }?.let { "personal:$it" }
            ?: ThemeCatalog.current()?.name?.let { "preset:$it" })
        val panel = JPanel(BorderLayout(JBUI.scale(8), JBUI.scale(8))).apply {
            add(JBScrollPane(list).apply { preferredSize = JBUI.size(450, 330) }, BorderLayout.CENTER)
            add(favorite, BorderLayout.SOUTH)
        }
        object : DialogWrapper(event.project) {
            init {
                title = "Switch Solarized Osaka Palette"
                setOKButtonText("Apply palette")
                init()
            }
            override fun createCenterPanel(): JComponent = panel
            override fun getPreferredFocusedComponent(): JComponent = list
            override fun doOKAction() {
                val key = list.selectedValue?.key ?: return
                val name = key.substringAfter(':')
                val applied = if (key.startsWith("personal:")) service.activateProfile(name) else ThemeCatalog.apply(name)
                if (applied) super.doOKAction()
                else Messages.showErrorDialog(event.project, "This palette’s preset is unavailable. Open Customize Solarized Osaka to select a replacement.", title)
            }
        }.show()
    }
}

class UndoPaletteApplyAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = com.intellij.openapi.actionSystem.ActionUpdateThread.EDT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = PersonalThemes.instance().canUndo
    }

    override fun actionPerformed(event: AnActionEvent) {
        PersonalThemes.instance().undoLastApply()
    }
}
