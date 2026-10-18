plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.craftzdog.solarizedosaka"
        name = "Solarized Osaka"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "243"
        }
        vendor {
            name = "craftzdog inspired"
            url = "https://github.com/craftzdog/solarized-osaka.nvim"
        }
        description = """
            <h2>Solarized Osaka</h2>
            <p>A clean, dark (and light) Solarized-inspired theme for all JetBrains IDEs,
            ported from the popular Neovim theme by craftzdog.</p>
            <p>Features slightly brightened base colors for modern displays and rich accent palette.</p>
            <ul>
              <li>Solarized Osaka (Dark)</li>
              <li>Solarized Osaka Light</li>
            </ul>
            <p>Based on <a href="https://github.com/craftzdog/solarized-osaka.nvim">solarized-osaka.nvim</a>.</p>
        """.trimIndent()
        changeNotes = """
            <h3>1.0.0</h3>
            <ul>
              <li>Initial release – full UI + editor scheme for Dark and Light</li>
              <li>Accurate palette from solarized-osaka.nvim</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    wrapper {
        gradleVersion = "8.11.1"
    }
}
