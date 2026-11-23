import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

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
    publishing {
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }

    pluginVerification {
        ides {
            val target = providers.gradleProperty("verifyIde").orNull
            if (target != null) {
                val type = IntelliJPlatformType.valueOf(target)
                val release = providers.gradleProperty("verifyIdeVersion").get()
                ide(type, release)
            } else {
                recommended()
            }
        }
    }

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
            <h2>Solarized Osaka for JetBrains IDEs</h2>
            <p>48 Classic and Islands UI themes across 12 palette families.</p>
            <p>Includes a theme gallery, language-aware editor colors, custom token mappings, VCS and console palettes,
            font presets, live personal palettes, and preview samples.</p>
        """.trimIndent()
        changeNotes = providers.fileContents(layout.projectDirectory.file("CHANGELOG.md")).asText.map { text ->
            buildString {
                var list = false
                for (line in text.lineSequence()) {
                    val escaped = line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    if (line.startsWith("## ")) {
                        if (list) append("</ul>")
                        append("<h3>${escaped.removePrefix("## ")}</h3>")
                        list = false
                    } else if (line.startsWith("- ")) {
                        if (!list) append("<ul>")
                        append("<li>${escaped.removePrefix("- ")}</li>")
                        list = true
                    }
                }
                if (list) append("</ul>")
            }
        }.get()
    }
}

val starterSyntaxBundles by tasks.registering(Zip::class) {
    from("src/main/syntax-bundles")
    archiveFileName.set("solarized-osaka-syntax-bundles.zip")
    destinationDirectory.set(layout.buildDirectory.dir("syntax-bundles"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks {
    processResources {
        from(listOf("USAGE.md", "CHANGELOG.md")) { into("osaka-help") }
        from(starterSyntaxBundles) { into("osaka-help") }
    }


    wrapper {
        gradleVersion = "8.13"
    }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
