import com.embarrasdf.gradle.plugin.addToWarmCachesTask
import com.embarrasdf.gradle.plugin.registerTestTasks
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class DesktopApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.embarrasdf.gradle.plugin.format")
            apply("com.embarrasdf.gradle.plugin.static.analysis")
        }
        registerTestTasks()
        // The uber jar builds the app and resolves its runtime classpath like createDistributable,
        // but skips jlink's runtime image, which caches nothing and can fail in a container.
        addToWarmCachesTask("packageUberJarForCurrentOS")
    }
}

fun Project.desktopAppTarget(mainClass: String) {
    extensions.configure<KotlinMultiplatformExtension> {
        jvm {
            mainRun {
                this.mainClass.set(mainClass)
            }
        }

        this@configure.sourceSets {
            jvmMain {
                dependencies {
                    implementation(ComposePlugin.DesktopDependencies.currentOs)
                }
            }
        }
    }
}
