import com.android.build.api.dsl.TestExtension
import com.embarrasdf.gradle.plugin.AndroidMinSdk
import com.embarrasdf.gradle.plugin.AndroidTargetSdk
import com.embarrasdf.gradle.plugin.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.test")
                apply("com.embarrasdf.gradle.plugin.lint")
            }

            extensions.configure<TestExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = AndroidTargetSdk
                defaultConfig.minSdk = AndroidMinSdk
            }
        }
    }
}
