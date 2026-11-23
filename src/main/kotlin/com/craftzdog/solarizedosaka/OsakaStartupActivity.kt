package com.craftzdog.solarizedosaka

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.options.ShowSettingsUtil
import com.craftzdog.solarizedosaka.actions.PersonalThemes

class OsakaStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        val application = ApplicationManager.getApplication()
        application.invokeLater { PersonalThemes.instance().restore() }
        project.getService(ProjectEditorSchemeService::class.java).restoreSavedScheme()
        if (!application.getService(OsakaWelcomeState::class.java).shouldShowWelcomePrompt()) return

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("SolarizedOsakaWelcome")
            .createNotification(
                "Explore Solarized Osaka themes",
                "Preview the palette families in the gallery, then select a theme in Appearance settings.",
                NotificationType.INFORMATION
            )
        notification.addAction(NotificationAction.createSimple("Open theme gallery") {
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project, com.craftzdog.solarizedosaka.settings.ThemeGalleryConfigurable::class.java
            )
        })
        Notifications.Bus.notify(notification, project)
    }
}

@Service(Service.Level.APP)
@State(name = "SolarizedOsakaAutomation", storages = [Storage("solarized-osaka-automation.xml")])
internal class OsakaWelcomeState : PersistentStateComponent<OsakaWelcomeState.Settings> {
    class Settings {
        var welcomePromptShown = false
    }

    private var settings = Settings()

    override fun getState() = settings

    override fun loadState(state: Settings) {
        settings = state
    }

    @Synchronized
    fun shouldShowWelcomePrompt(): Boolean {
        if (settings.welcomePromptShown) return false
        settings.welcomePromptShown = true
        return true
    }
}
