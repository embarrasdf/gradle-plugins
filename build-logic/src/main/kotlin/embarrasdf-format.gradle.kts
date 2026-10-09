import java.util.Properties

// gradle-plugins can't apply its own format plugin, so this configures Spotless the same way,
// from the same ktlint settings file the plugin ships.

plugins {
    id("com.diffplug.spotless")
}

val ktlintVersion = extensions.getByType<VersionCatalogsExtension>()
    .named("embarrasdfPluginLibs")
    .findVersion("ktlint")
    .get()
    .requiredVersion
val ktlintEditorConfig = Properties().apply {
    file("plugins/src/main/resources/com/embarrasdf/gradle/plugin/ktlint.editorconfig.properties")
        .inputStream()
        .use { load(it) }
}.let { properties -> properties.stringPropertyNames().associateWith { properties.getProperty(it) } }

spotless {
    isEnforceCheck = false
    kotlin {
        target("plugins/src/**/*.kt", "build-logic/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintEditorConfig)
    }
    kotlinGradle {
        target("*.gradle.kts", "plugins/*.gradle.kts", "build-logic/*.gradle.kts", "build-logic/src/**/*.gradle.kts")
        ktlint(ktlintVersion).editorConfigOverride(ktlintEditorConfig)
    }
}

tasks.register("embarrasdfFormat") {
    group = "formatting"
    description = "Fixes formatting issues that can be fixed automatically."
    dependsOn("spotlessApply")
}

tasks.register("embarrasdfFormatCheck") {
    group = "verification"
    description = "Fails if any file has a formatting issue."
    dependsOn("spotlessCheck")
}
