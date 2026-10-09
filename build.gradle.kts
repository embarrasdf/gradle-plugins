import org.shipkit.changelog.GenerateChangelogTask
import org.shipkit.github.release.GithubReleaseTask

plugins {
    alias(embarrasdfPluginLibs.plugins.kotlin.multiplatform) apply false
    alias(embarrasdfPluginLibs.plugins.android.kotlin.multiplatform.library) apply false
    alias(embarrasdfPluginLibs.plugins.maven.publish) apply false

    alias(embarrasdfPluginLibs.plugins.shipkit.autoversion) apply true
    alias(embarrasdfPluginLibs.plugins.shipkit.changelog) apply true
    alias(embarrasdfPluginLibs.plugins.shipkit.githubrelease) apply true
    id("embarrasdf-format")
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
