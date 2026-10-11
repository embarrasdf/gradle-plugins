import com.embarrasdf.gradle.plugin.configureBrowserTests
import com.embarrasdf.gradle.plugin.useNpmForKotlinWeb
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class WebApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        useNpmForKotlinWeb()

        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.embarrasdf.gradle.plugin.format")
            apply("com.embarrasdf.gradle.plugin.static.analysis")
        }
    }
}

fun Project.webAppTarget() {
    extensions.configure<KotlinMultiplatformExtension> {
        applyDefaultHierarchyTemplate()
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            browser {
                configureBrowserTests()
            }
            binaries.executable()
        }
    }
}
