package com.craftzdog.solarizedosaka

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(
    name = "SolarizedOsakaProjectTheme",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class ProjectThemePreferences : PersistentStateComponent<ProjectThemePreferences.State> {
    class State {
        var themeName: String = ""
        var editorSchemeName: String = ""
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }
}
