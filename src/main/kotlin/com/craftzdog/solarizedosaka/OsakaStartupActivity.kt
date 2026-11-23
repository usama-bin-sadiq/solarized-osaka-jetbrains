package com.craftzdog.solarizedosaka

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.options.ShowSettingsUtil

class OsakaStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater { com.craftzdog.solarizedosaka.actions.PersonalThemes.instance().restore() }
        project.getService(ProjectEditorSchemeService::class.java).restoreSavedScheme()
        ThemeAutomationService.instance().start()
        if (!ThemeAutomationService.instance().shouldShowWelcomePrompt()) return
        if (ThemeCatalog.all().isEmpty()) return

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("SolarizedOsakaWelcome")
            .createNotification(
                "Explore Solarized Osaka themes",
                "Browse the palette families, preview them, and choose Classic or Islands UI in the theme gallery.",
                NotificationType.INFORMATION
            )
        notification.addAction(NotificationAction.createSimple("Open theme gallery") {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "com.craftzdog.solarizedosaka.themeGallery")
        })
        Notifications.Bus.notify(notification, project)
    }
}
