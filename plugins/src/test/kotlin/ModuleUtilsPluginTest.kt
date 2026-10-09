import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ModuleUtilsPluginTest {

    @TempDir
    lateinit var testProjectDir: File

    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var gradlePropertiesFile: File

    private val testNamespace = "com.example.test"

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts").apply { createNewFile() }
        buildFile = File(testProjectDir, "build.gradle.kts").apply { createNewFile() }
        gradlePropertiesFile = File(testProjectDir, "gradle.properties").apply { createNewFile() }

        val gradleDir = File(testProjectDir, "gradle")
        gradleDir.mkdirs()
        val libsFile = File(gradleDir, "libs.versions.toml")
        libsFile.writeText(
            """
            [versions]
            kotlin = "2.2.20"

            [plugins]
            embarrasdf-kotlin-multiplatform-library = { id = "com.embarrasdf.gradle.plugin.kotlin.multiplatform.library", version = "0.0.1" }
            """.trimIndent(),
        )

        settingsFile.writeText(
            """
            rootProject.name = "test-project"

            enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins {
                id("com.embarrasdf.gradle.plugin.module.utils")
            }
            """.trimIndent(),
        )

        gradlePropertiesFile.writeText(
            """
            namespace=$testNamespace
            """.trimIndent(),
        )
    }

    @Nested
    inner class CreateKmpLibraryModuleTaskTest {
        @Test
        fun `creates module with correct structure and source sets`() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("createKmpLibraryModule", "--name=auth")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":createKmpLibraryModule")?.outcome)

            val moduleDir = File(testProjectDir, "auth")
            assertTrue(moduleDir.exists(), "Module directory should exist")

            val buildFile = File(moduleDir, "build.gradle.kts")
            assertTrue(buildFile.exists(), "build.gradle.kts should exist")

            val sourceSets = listOf(
                "commonMain",
                "commonTest",
                "androidMain",
                "nativeMain",
                "jvmMain",
                "wasmJsMain",
            )
            for (sourceSet in sourceSets) {
                val sourceSetDir = File(moduleDir, "src/$sourceSet/kotlin/com/example/test/auth")
                assertTrue(sourceSetDir.exists(), "Source set $sourceSet should exist")
            }

            val settingsContent = settingsFile.readText()
            assertTrue(settingsContent.contains("include(\":auth\")"), "Settings should include module")

            val buildContent = buildFile.readText()
            assertTrue(
                buildContent.contains("androidNamespace = \"com.example.test.auth\""),
                "Build file should have androidNamespace",
            )
            assertTrue(
                buildContent.contains("iosFrameworkBaseName = \"Auth\""),
                "Build file should have iosFrameworkBaseName",
            )
        }

        @Test
        fun `creates nested module with colon-separated name`() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("createKmpLibraryModule", "--name=:feature:auth")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":createKmpLibraryModule")?.outcome)

            val moduleDir = File(testProjectDir, "feature/auth")
            assertTrue(moduleDir.exists(), "Nested module directory should exist")

            val buildFile = File(moduleDir, "build.gradle.kts")
            assertTrue(buildFile.exists(), "build.gradle.kts should exist in nested location")

            val sourceSets = listOf(
                "commonMain",
                "commonTest",
                "androidMain",
                "nativeMain",
                "jvmMain",
                "wasmJsMain",
            )
            for (sourceSet in sourceSets) {
                val sourceSetDir = File(moduleDir, "src/$sourceSet/kotlin/com/example/test/feature/auth")
                assertTrue(sourceSetDir.exists(), "Source set $sourceSet should exist")
            }

            val settingsContent = settingsFile.readText()
            assertTrue(settingsContent.contains("include(\":feature:auth\")"), "Settings should include nested module")

            val buildContent = buildFile.readText()
            assertTrue(
                buildContent.contains("androidNamespace = \"com.example.test.feature.auth\""),
                "Build file should have androidNamespace",
            )
            assertTrue(
                buildContent.contains("iosFrameworkBaseName = \"FeatureAuth\""),
                "Build file should have iosFrameworkBaseName",
            )
        }

        @Test
        fun `settings file entries are sorted alphabetically`() {
            // Add in unsorted order
            var settingsContent = settingsFile.readText()
            settingsContent += "\ninclude(\":zeta\")\ninclude(\":beta\")"
            settingsFile.writeText(settingsContent)

            File(testProjectDir, "zeta").mkdirs()
            File(testProjectDir, "beta").mkdirs()

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("createKmpLibraryModule", "--name=alpha")
                .withPluginClasspath()
                .build()
            assertEquals(TaskOutcome.SUCCESS, result.task(":createKmpLibraryModule")?.outcome)

            settingsContent = settingsFile.readText()
            val includeStatements = settingsContent.lines()
                .filter { it.contains("include(") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            assertEquals(
                listOf(
                    "include(\":alpha\")",
                    "include(\":beta\")",
                    "include(\":zeta\")",
                ),
                includeStatements,
            )
        }
    }
}
