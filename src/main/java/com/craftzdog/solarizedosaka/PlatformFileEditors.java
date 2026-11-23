package com.craftzdog.solarizedosaka;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class PlatformFileEditors {
    private PlatformFileEditors() {}

    public static @NotNull FileEditorManager get(@NotNull Project project) {
        // The Java static method exists on both older Java and newer Kotlin implementations.
        return FileEditorManager.getInstance(project);
    }
}
