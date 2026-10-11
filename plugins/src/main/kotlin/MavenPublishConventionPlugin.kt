import com.embarrasdf.gradle.plugin.addToReleaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.vanniktech.maven.publish")

        project.addToReleaseTask(PublishTaskName)
        // Publish before the GitHub release tags the version, so a failed upload can be retried. The
        // task exists only when Maven Central is configured, such as with SONATYPE_HOST.
        project.rootProject.pluginManager.withPlugin("org.shipkit.shipkit-github-release") {
            project.rootProject.tasks.named("githubRelease") {
                mustRunAfter(project.tasks.named { name -> name == PublishTaskName })
            }
        }
    }
}

private const val PublishTaskName = "publishAndReleaseToMavenCentral"
