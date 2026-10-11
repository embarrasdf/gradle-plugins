import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import java.util.Properties

/**
 * Applied in a repo's settings.gradle.kts:
 *
 * ```
 * plugins {
 *     id("com.embarrasdf.gradle.plugin.settings") version "<version>"
 * }
 * ```
 *
 * When local.properties sets `includeGradlePlugins=true`, the build uses the
 * convention plugins from a gradle-plugins checkout beside the repo instead of
 * the published ones. Without it, the repo builds against the published plugins,
 * as its CI does, so every build uses the same plugins and shares cached outputs.
 */
class SettingsConventionPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val localProperties = Properties()
        settings.rootDir.resolve("local.properties").takeIf { it.isFile }
            ?.reader()?.use { localProperties.load(it) }

        if (localProperties.getProperty("includeGradlePlugins").toBoolean()) {
            val gradlePlugins = settings.rootDir.resolve("../gradle-plugins")
            check(gradlePlugins.isDirectory) {
                "local.properties sets includeGradlePlugins=true, but there's no ${gradlePlugins.normalize()}"
            }
            settings.includeBuild(gradlePlugins)
        }
    }
}
