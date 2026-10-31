import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DependencyLockingTest {

    @TempDir
    lateinit var workspaceDir: File

    private lateinit var repoDir: File
    private lateinit var mavenDir: File

    @BeforeEach
    fun setup() {
        // A local Maven repo: lib depends on core, and lib 1.1 moves core from 1.0 to 2.0.
        mavenDir = File(workspaceDir, "maven")
        publish("core", "1.0")
        publish("core", "2.0")
        publish("lib", "1.0", dependency = "core" to "1.0")
        publish("lib", "1.1", dependency = "core" to "2.0")

        repoDir = File(workspaceDir, "repo").apply { mkdirs() }
        File(repoDir, "settings.gradle.kts").writeText("""
            plugins {
                id("com.embarrasdf.gradle.plugin.settings")
            }

            rootProject.name = "repo"
        """.trimIndent())
        writeBuildFile(libVersion = "1.0")
    }

    private fun publish(name: String, version: String, dependency: Pair<String, String>? = null) {
        val dependencies = dependency?.let { (depName, depVersion) ->
            """
            <dependencies>
              <dependency>
                <groupId>com.example</groupId>
                <artifactId>$depName</artifactId>
                <version>$depVersion</version>
              </dependency>
            </dependencies>
            """
        }.orEmpty()
        File(mavenDir, "com/example/$name/$version").apply {
            mkdirs()
            File(this, "$name-$version.pom").writeText("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>$name</artifactId>
                  <version>$version</version>
                  <packaging>pom</packaging>
                  $dependencies
                </project>
            """.trimIndent())
        }
    }

    private fun writeBuildFile(libVersion: String) {
        File(repoDir, "build.gradle.kts").writeText("""
            plugins {
                `java-library`
            }

            repositories {
                maven(url = "${mavenDir.toURI()}")
            }

            dependencies {
                implementation("com.example:lib:$libVersion")
            }

            // Resolves the classpath's files, as a compile task does, which is when locks are checked.
            tasks.register("resolveRuntimeClasspath") {
                inputs.files(configurations.runtimeClasspath)
                val versions = configurations.runtimeClasspath.flatMap { configuration ->
                    configuration.incoming.resolutionResult.rootComponent.map { root ->
                        root.dependencies.joinToString { it.toString() }
                    }
                }
                doLast { println("resolved: " + versions.get()) }
            }
        """.trimIndent())
    }

    private fun enableLocking() {
        File(repoDir, "gradle.properties").writeText("embarrasdf.dependencyLocking=true")
    }

    private val lockfile get() = File(repoDir, "gradle.lockfile")

    private fun runner(vararg arguments: String) = GradleRunner.create()
        .withProjectDir(repoDir)
        .withArguments(*arguments)
        .withPluginClasspath()

    @Test
    fun `does nothing without the property`() {
        runner("resolveRuntimeClasspath").build()
        val result = runner("tasks", "--all").build()

        assertFalse(lockfile.exists())
        assertFalse(result.output.contains("embarrasdfWriteLocks"), result.output)
    }

    @Test
    fun `writes direct and transitive versions for every configuration`() {
        enableLocking()

        runner("embarrasdfWriteLocks").build()

        val lines = lockfile.readLines()
        assertTrue(lines.any { it.startsWith("com.example:lib:1.0=") && "runtimeClasspath" in it }, lockfile.readText())
        assertTrue(lines.any { it.startsWith("com.example:core:1.0=") && "compileClasspath" in it }, lockfile.readText())
        assertTrue(File(repoDir, "buildscript-gradle.lockfile").exists())
    }

    @Test
    fun `fails when a bump moves a transitive version until the locks are rewritten`() {
        enableLocking()
        runner("embarrasdfWriteLocks").build()

        writeBuildFile(libVersion = "1.1")
        val stale = runner("resolveRuntimeClasspath").buildAndFail()

        assertTrue(stale.output.contains("com.example:lib"), stale.output)

        runner("embarrasdfWriteLocks").build()
        runner("resolveRuntimeClasspath").build()

        val lines = lockfile.readLines()
        assertTrue(lines.any { it.startsWith("com.example:core:2.0=") }, lockfile.readText())
        assertFalse(lines.any { it.startsWith("com.example:core:1.0=") }, lockfile.readText())
    }

    @Test
    fun `resolves with locks that match`() {
        enableLocking()
        runner("embarrasdfWriteLocks").build()

        val result = runner("resolveRuntimeClasspath").build()

        assertTrue(result.output.contains("resolved: com.example:lib:1.0"), result.output)
    }
}
