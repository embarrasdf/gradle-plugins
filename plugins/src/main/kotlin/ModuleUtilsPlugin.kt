import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault

class ModuleUtilsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("createKmpLibraryModule", CreateKmpLibraryModuleTask::class.java) {
            group = "module-automation"
            description = "Create a KMP library module with standard configuration."
            projectRootDir.set(target.layout.projectDirectory)
        }
    }
}

@DisableCachingByDefault(because = "Creates new files and modifies settings.gradle.kts based on user input")
abstract class CreateKmpLibraryModuleTask : DefaultTask() {
    @get:Optional
    @get:Input
    @get:Option(option = "name", description = "The name of the library.")
    abstract val moduleName: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectRootDir: DirectoryProperty

    @TaskAction
    fun run() {
        val name = moduleName.orNull
        if (name.isNullOrBlank()) {
            println("Library name must be provided using --name.")
            return
        }

        val projectDir = projectRootDir.get().asFile
        val gradlePropertiesFile = projectDir.resolve("gradle.properties")
        val namespace = if (gradlePropertiesFile.exists()) {
            val properties = java.util.Properties()
            gradlePropertiesFile.inputStream().use { properties.load(it) }
            properties.getProperty("namespace") ?: run {
                println("Error: 'namespace' property not found in gradle.properties")
                return
            }
        } else {
            println("Error: gradle.properties file not found in project root")
            return
        }

        // Support both ":feature:auth" and "feature:auth" formats
        val normalizedName = if (name.startsWith(":")) name.substring(1) else name
        val modulePath = normalizedName.replace(":", "/")
        val moduleIncludeName = if (name.startsWith(":")) name else ":$name"

        val moduleDir = projectDir.resolve(modulePath)
        if (moduleDir.exists()) {
            println("Module directory '$modulePath' already exists. Skipping creation.")
            return
        }
        moduleDir.mkdirs()

        val packageSuffix = normalizedName.replace(":", ".").replace("-", "")
        val packageName = "$namespace.$packageSuffix"

        val sourceSets = listOf(
            "commonMain",
            "commonTest",
            "androidMain",
            "nativeMain",
            "jvmMain",
            "wasmJsMain",
        )
        for (dir in sourceSets) {
            val sourceSetDir = "src/$dir/kotlin/"
            val dirPath = moduleDir.resolve(sourceSetDir).resolve(packageName.replace(".", "/"))
            dirPath.mkdirs()
        }

        val androidNamespace = packageName
        val iosFrameworkName = normalizedName.split(":")
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        val buildFileContent = """
            plugins {
                id(libs.plugins.embarrasdf.kotlin.multiplatform.library.get().pluginId)
            }

            kotlin {
                libraryTargets(
                    androidNamespace = "$androidNamespace",
                    iosFrameworkBaseName = "$iosFrameworkName",
                )

                sourceSets {}
            }

        """.trimIndent()
        moduleDir.resolve("build.gradle.kts").writeText(buildFileContent)

        val settingsFile = projectDir.resolve("settings.gradle.kts")
        val newIncludeLine = """include("$moduleIncludeName")"""
        val settingsLines = settingsFile.readLines().toMutableList()

        val includes = settingsLines.filter { it.trim().startsWith("include(") }.toMutableList()
        val otherLines = settingsLines.filterNot { it.trim().startsWith("include(") }

        includes.add(newIncludeLine)
        includes.sort()

        val newSettingsContent = (otherLines + includes).joinToString("\n").plus("\n")
        settingsFile.writeText(newSettingsContent)

        println("Successfully created module '$moduleIncludeName' at '$modulePath' and updated settings.gradle.kts.")
    }
}
