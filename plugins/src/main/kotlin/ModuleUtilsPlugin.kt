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
import java.io.File
import java.util.Properties

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
        val namespace = readNamespace(projectDir) ?: return

        // Support both ":feature:auth" and "feature:auth" formats
        val normalizedName = name.removePrefix(":")
        val modulePath = normalizedName.replace(oldChar = ':', newChar = '/')
        val moduleDir = projectDir.resolve(modulePath)
        if (moduleDir.exists()) {
            println("Module directory '$modulePath' already exists. Skipping creation.")
        } else {
            createModule(
                projectDir = projectDir,
                moduleDir = moduleDir,
                normalizedName = normalizedName,
                namespace = namespace,
            )
            println("Successfully created module ':$normalizedName' at '$modulePath' and updated settings.gradle.kts.")
        }
    }

    /** Reads `namespace` from the project's gradle.properties, or prints why it can't. */
    private fun readNamespace(projectDir: File): String? {
        val gradlePropertiesFile = projectDir.resolve("gradle.properties")
        if (!gradlePropertiesFile.exists()) {
            println("Error: gradle.properties file not found in project root")
            return null
        }
        val properties = Properties()
        gradlePropertiesFile.inputStream().use { properties.load(it) }
        return properties.getProperty("namespace")
            ?: null.also { println("Error: 'namespace' property not found in gradle.properties") }
    }

    private fun createModule(
        projectDir: File,
        moduleDir: File,
        normalizedName: String,
        namespace: String,
    ) {
        val packageSuffix = normalizedName.replace(oldChar = ':', newChar = '.').replace(oldValue = "-", newValue = "")
        val packageName = "$namespace.$packageSuffix"
        val packagePath = packageName.replace(oldChar = '.', newChar = '/')
        for (sourceSet in ModuleSourceSets) {
            moduleDir.resolve("src/$sourceSet/kotlin/").resolve(packagePath).mkdirs()
        }

        val iosFrameworkName = normalizedName.split(":")
            .joinToString("") { part -> part.replaceFirstChar { c -> c.uppercase() } }
        moduleDir.resolve("build.gradle.kts").writeText(
            buildFileContent(androidNamespace = packageName, iosFrameworkName = iosFrameworkName),
        )

        addInclude(settingsFile = projectDir.resolve("settings.gradle.kts"), moduleIncludeName = ":$normalizedName")
    }

    private fun buildFileContent(
        androidNamespace: String,
        iosFrameworkName: String,
    ): String = """
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

    /** Adds the module's include() to settings.gradle.kts, keeping the includes sorted at the end. */
    private fun addInclude(
        settingsFile: File,
        moduleIncludeName: String,
    ) {
        val settingsLines = settingsFile.readLines()
        val isInclude = { line: String -> line.trim().startsWith("include(") }
        val includes = (settingsLines.filter(isInclude) + """include("$moduleIncludeName")""").sorted()
        val otherLines = settingsLines.filterNot(isInclude)
        settingsFile.writeText((otherLines + includes).joinToString("\n").plus("\n"))
    }

    private companion object {
        val ModuleSourceSets = listOf(
            "commonMain",
            "commonTest",
            "androidMain",
            "nativeMain",
            "jvmMain",
            "wasmJsMain",
        )
    }
}
