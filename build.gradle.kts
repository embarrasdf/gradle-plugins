import org.shipkit.changelog.GenerateChangelogTask
import org.shipkit.github.release.GithubReleaseTask
import java.util.Properties

plugins {
    alias(embarrasdfPluginLibs.plugins.kotlin.multiplatform) apply false
    alias(embarrasdfPluginLibs.plugins.android.kotlin.multiplatform.library) apply false
    alias(embarrasdfPluginLibs.plugins.maven.publish) apply false

    alias(embarrasdfPluginLibs.plugins.shipkit.autoversion) apply true
    alias(embarrasdfPluginLibs.plugins.shipkit.changelog) apply true
    alias(embarrasdfPluginLibs.plugins.shipkit.githubrelease) apply true
    alias(embarrasdfPluginLibs.plugins.spotless)
}

tasks.named<GenerateChangelogTask>("generateChangelog") {
    previousRevision = project.extra["shipkit-auto-version.previous-tag"] as String
    githubToken = System.getenv("GITHUB_TOKEN")
    repository = "embarrasdf/gradle-plugins"
}

tasks.named<GithubReleaseTask>("githubRelease") {
    dependsOn(tasks.named("generateChangelog"))
    val isSnapshot = version.toString().endsWith("SNAPSHOT")
    enabled = !isSnapshot
    repository = "embarrasdf/gradle-plugins"
    changelog = tasks.named("generateChangelog").get().outputs.files.singleFile
    githubToken = System.getenv("GITHUB_TOKEN")
    newTagRevision = System.getenv("GITHUB_SHA")
}

// gradle-plugins can't apply its own lint plugin, so configure Spotless the same way here,
// from the same ktlint settings file the plugin ships.
val ktlintVersion = embarrasdfPluginLibs.versions.ktlint.get()
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
        target("*.gradle.kts", "plugins/*.gradle.kts", "build-logic/*.gradle.kts")
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
