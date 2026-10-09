import com.diffplug.gradle.spotless.SpotlessExtension
import com.embarrasdf.gradle.plugin.FormatCheckTaskName
import com.embarrasdf.gradle.plugin.FormatTaskName
import com.embarrasdf.gradle.plugin.KtlintEditorConfig
import com.embarrasdf.gradle.plugin.KtlintVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Formats and checks Kotlin sources and Gradle Kotlin scripts with ktlint.
 *
 * Applied automatically by the application and library convention plugins. Apply it to the root
 * project too, so the root build and settings scripts are covered:
 * ```kotlin
 * plugins {
 *     alias(libs.plugins.embarrasdf.lint)
 * }
 * ```
 *
 * Tasks:
 * - `embarrasdfFormat` rewrites files to fix every issue that can be fixed automatically.
 * - `embarrasdfFormatCheck` fails if any file has an issue, and lists what needs fixing.
 *
 * Neither task is attached to `check`, so formatting never fails a test run.
 */
class LintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")

            extensions.configure<SpotlessExtension> {
                isEnforceCheck = false
                kotlin {
                    target("src/**/*.kt")
                    targetExclude("**/build/**")
                    ktlint(KtlintVersion).editorConfigOverride(KtlintEditorConfig)
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktlint(KtlintVersion).editorConfigOverride(KtlintEditorConfig)
                }
            }

            tasks.register(FormatTaskName) {
                group = "formatting"
                description = "Fixes formatting issues that can be fixed automatically."
                dependsOn("spotlessApply")
            }
            tasks.register(FormatCheckTaskName) {
                group = "verification"
                description = "Fails if any file has a formatting issue."
                dependsOn("spotlessCheck")
            }
        }
    }
}
