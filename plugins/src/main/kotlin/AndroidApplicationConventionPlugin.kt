import com.android.build.api.dsl.ApplicationExtension
import com.embarrasdf.gradle.plugin.AndroidMinSdk
import com.embarrasdf.gradle.plugin.AndroidTargetSdk
import com.embarrasdf.gradle.plugin.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("com.embarrasdf.gradle.plugin.format")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = AndroidTargetSdk
                defaultConfig.minSdk = AndroidMinSdk
            }
        }
    }
}
