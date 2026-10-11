import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingsConventionPluginTest {

    @TempDir
    lateinit var workspaceDir: File

    private lateinit var repoDir: File

    @BeforeEach
    fun setup() {
        // A stand-in gradle-plugins checkout beside the repo.
        File(workspaceDir, "gradle-plugins").apply {
            mkdirs()
            File(this, "settings.gradle.kts").writeText("""rootProject.name = "gradle-plugins"""")
        }

        repoDir = File(workspaceDir, "repo").apply { mkdirs() }
        File(repoDir, "settings.gradle.kts").writeText("""
            plugins {
                id("com.embarrasdf.gradle.plugin.settings")
            }

            rootProject.name = "repo"
        """.trimIndent())
        File(repoDir, "build.gradle.kts").writeText("""
            tasks.register("printIncludedBuilds") {
                val names = gradle.includedBuilds.map { it.name }
                doLast { println("included builds: " + names) }
            }
        """.trimIndent())
    }

    private fun runner(vararg arguments: String) = GradleRunner.create()
        .withProjectDir(repoDir)
        .withArguments(*arguments)
        .withPluginClasspath()

    @Test
    fun `uses the published plugins without local properties`() {
        val result = runner("printIncludedBuilds").build()

        assertTrue(result.output.contains("included builds: []"), result.output)
    }

    @Test
    fun `uses the published plugins when includeGradlePlugins is false`() {
        File(repoDir, "local.properties").writeText("includeGradlePlugins=false")

        val result = runner("printIncludedBuilds").build()

        assertTrue(result.output.contains("included builds: []"), result.output)
    }

    @Test
    fun `includes gradle-plugins when includeGradlePlugins is true`() {
        File(repoDir, "local.properties").writeText("includeGradlePlugins=true")

        val result = runner("printIncludedBuilds").build()

        assertTrue(result.output.contains("included builds: [gradle-plugins]"), result.output)
    }

    @Test
    fun `fails when includeGradlePlugins is true without a checkout`() {
        File(workspaceDir, "gradle-plugins").deleteRecursively()
        File(repoDir, "local.properties").writeText("includeGradlePlugins=true")

        val result = runner("printIncludedBuilds").buildAndFail()

        assertTrue(result.output.contains("there's no"), result.output)
        assertFalse(result.output.contains("included builds:"), result.output)
    }
}
