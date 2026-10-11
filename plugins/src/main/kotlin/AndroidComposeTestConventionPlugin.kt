import com.embarrasdf.gradle.plugin.Libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                "debugImplementation"(Libs.composeUiTestManifest)
                "androidTestImplementation"(Libs.composeUiTestJunit4)
            }
        }
    }
}
