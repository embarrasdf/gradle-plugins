import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CiTasksTest {

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":lib")
            """.trimIndent(),
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.embarrasdf.gradle.plugin.github.release")
            }
            """.trimIndent(),
        )
        File(projectDir, "version.properties").writeText("version=0.0.*\n")
        File(projectDir, "gradle.properties").writeText("SONATYPE_HOST=CENTRAL_PORTAL\n")
        File(projectDir, "lib").mkdirs()
        File(projectDir, "lib/build.gradle.kts").writeText(
            """
            plugins {
                id("com.embarrasdf.gradle.plugin.jvm.library")
                id("com.embarrasdf.gradle.plugin.maven.publish")
            }
            """.trimIndent(),
        )
        git("init", "-q")
        git("commit", "-q", "--allow-empty", "-m", "Initial")
        // The release plugin needs a previous release.
        git("tag", "v0.0.1")
    }

    @Test
    fun `embarrasdfTest runs the JVM tests`() {
        val tasks = dryRun("embarrasdfTest")

        assertTrue(":lib:test" in tasks, "Expected :lib:test in $tasks")
    }

    @Test
    fun `embarrasdfTestApple skips the JVM tests`() {
        val tasks = dryRun("embarrasdfTestApple")

        assertTrue(":lib:test" !in tasks, "Expected no :lib:test in $tasks")
    }

    @Test
    fun `embarrasdfWarmCaches compiles the JVM tests without running them`() {
        val tasks = dryRun("embarrasdfWarmCaches")

        assertTrue(":lib:compileTestKotlin" in tasks, "Expected :lib:compileTestKotlin in $tasks")
        assertTrue(":lib:test" !in tasks, "Expected no :lib:test in $tasks")
    }

    @Test
    fun `embarrasdfRelease publishes before creating the GitHub release`() {
        val tasks = dryRun("embarrasdfRelease")

        val publish = tasks.indexOf(":lib:publishAndReleaseToMavenCentral")
        val release = tasks.indexOf(":githubRelease")
        assertTrue(publish >= 0 && release >= 0, "Expected both tasks in $tasks")
        assertTrue(publish < release, "Expected publishing before the release in $tasks")
        assertTrue(":updateReadmeVersion" !in tasks, "Expected no README update without a template in $tasks")
    }

    @Test
    fun `embarrasdfRelease updates the README when it has a template`() {
        File(projectDir, "README.template.md").writeText("{{VERSION}}\n")

        val tasks = dryRun("embarrasdfRelease")

        assertTrue(":updateReadmeVersion" in tasks, "Expected :updateReadmeVersion in $tasks")
    }

    private fun dryRun(task: String): List<String> {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(task, "--dry-run")
            .withPluginClasspath()
            .build()
        return result.output.lines()
            .filter { it.endsWith(" SKIPPED") }
            .map { it.removeSuffix(" SKIPPED") }
    }

    private fun git(vararg args: String) {
        val identity = arrayOf("-c", "user.name=test", "-c", "user.email=test@example.com")
        val process = ProcessBuilder("git", *identity, *args).directory(projectDir).inheritIO().start()
        assertEquals(0, process.waitFor(), "git ${args.joinToString(" ")} failed")
    }
}
