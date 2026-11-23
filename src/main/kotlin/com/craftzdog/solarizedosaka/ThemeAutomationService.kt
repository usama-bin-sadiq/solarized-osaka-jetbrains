package com.craftzdog.solarizedosaka

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
@State(name = "SolarizedOsakaAutomation", storages = [Storage("solarized-osaka-automation.xml")])
class ThemeAutomationService : PersistentStateComponent<ThemeAutomationService.State>, Disposable {
    class State {
        var followSystemAppearance: Boolean = false
        var welcomePromptShown: Boolean = false
    }

    private var savedState = State()
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Solarized Osaka appearance sync").apply { isDaemon = true }
    }
    private var scheduled: ScheduledFuture<*>? = null

    override fun getState(): State = savedState
    override fun loadState(state: State) { savedState = state }

    @Synchronized
    fun shouldShowWelcomePrompt(): Boolean {
        if (savedState.welcomePromptShown) return false
        savedState.welcomePromptShown = true
        return true
    }

    @Synchronized
    fun start() {
        if (!savedState.followSystemAppearance || scheduled?.isDone == false) return
        scheduled = executor.scheduleWithFixedDelay({ syncWithSystem() }, 0, 60, TimeUnit.SECONDS)
    }

    @Synchronized
    fun setFollowSystem(enabled: Boolean) {
        savedState.followSystemAppearance = enabled
        if (enabled) start() else {
            scheduled?.cancel(false)
            scheduled = null
        }
    }

    private fun syncWithSystem() {
        val systemIsDark = detectSystemDarkMode() ?: return
        val current = ThemeCatalog.current() ?: return
        if (!current.name.startsWith("Solarized Osaka") || current.isDark == systemIsDark) return
        val target = ThemeCatalog.themeName(
            ThemeCatalog.familyOf(current.name),
            systemIsDark,
            ThemeCatalog.isIslands(current.name)
        )
        ApplicationManager.getApplication().invokeLater {
            if (!com.craftzdog.solarizedosaka.actions.PersonalThemes.instance().isPreviewing &&
                ThemeCatalog.current()?.name == current.name) ThemeCatalog.applyBrightness(target)
        }
    }

    private fun detectSystemDarkMode(): Boolean? {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val command = when {
            os.contains("mac") -> listOf("defaults", "read", "-g", "AppleInterfaceStyle")
            os.contains("win") -> listOf("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme")
            os.contains("linux") -> listOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
            else -> return null
        }
        return try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                null
            } else {
                val output = process.inputStream.bufferedReader().use { it.readText() }.trim().lowercase()
                when {
                    os.contains("mac") -> process.exitValue() == 0 && output.contains("dark")
                    os.contains("win") -> when {
                        output.contains("0x0") -> true
                        output.contains("0x1") -> false
                        else -> null
                    }
                    output.contains("prefer-dark") || output.contains("dark") -> true
                    output.contains("prefer-light") || output.contains("light") -> false
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun dispose() {
        scheduled?.cancel(true)
        executor.shutdownNow()
    }

    companion object {
        fun instance(): ThemeAutomationService =
            ApplicationManager.getApplication().getService(ThemeAutomationService::class.java)
    }
}
