import com.embarrasdf.gradle.plugin.DetektConfig
import com.embarrasdf.gradle.plugin.Libs
import com.embarrasdf.gradle.plugin.StaticAnalysisReportTaskName
import com.embarrasdf.gradle.plugin.StaticAnalysisTaskName
import com.embarrasdf.gradle.plugin.StaticAnalysisTypeResolutionProperty
import com.embarrasdf.gradle.plugin.WriteDetektConfigTask
import com.embarrasdf.gradle.plugin.isDetektTask
import com.embarrasdf.gradle.plugin.isTypeResolvedDetektTask
import com.embarrasdf.gradle.plugin.sourceDirsWithoutTypeResolution
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.report.ReportMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Runs static analysis on Kotlin sources with detekt and the compose-rules rule set.
 *
 * Applied automatically by the application and library convention plugins. Apply it to the root
 * project too, which merges every module's findings into one SARIF report:
 * ```kotlin
 * plugins {
 *     alias(libs.plugins.embarrasdf.static.analysis)
 * }
 * ```
 *
 * Tasks:
 * - `embarrasdfStaticAnalysis` fails if any module has a finding, and lists them. Run it from the
 *   root project to analyze every module.
 * - `embarrasdfStaticAnalysisReport` (root project) writes the merged SARIF report to
 *   `build/reports/detekt/static-analysis.sarif`. It runs after the analysis.
 *
 * Analysis uses type resolution, which compiles the JVM and Android targets. Kotlin that only
 * builds for other targets (iOS, wasm) is analyzed without it. Set
 * `embarrasdf.staticAnalysis.typeResolution=false` to analyze all sources without type
 * resolution; detekt then skips the rules that need it.
 *
 * The analysis isn't attached to `check` (detekt's own hook is removed), so findings never fail
 * a test run.
 */
class StaticAnalysisConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.detekt")
            configureDetekt()
            registerStaticAnalysisTask()
            registerReportMerge()
        }
    }

    private fun Project.configureDetekt() {
        val detektConfig = tasks.register<WriteDetektConfigTask>("embarrasdfDetektConfig") {
            content.set(DetektConfig)
            outputFile.set(layout.buildDirectory.file("embarrasdf/detekt.yml"))
        }

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
            config.setFrom(detektConfig.flatMap { it.outputFile })
            source.setFrom("src")
            parallel.set(true)
        }
        dependencies.add("detektPlugins", Libs.composeRulesDetekt)

        val buildDir = layout.buildDirectory.get().asFile
        tasks.withType<Detekt>().configureEach {
            // Type-resolved tasks also see generated sources (KSP, Compose resources), which sit
            // under the build directory as separate source roots.
            exclude { it.file.startsWith(buildDir) }
            reports {
                sarif.required.set(true)
                html.required.set(true)
                checkstyle.required.set(false)
                markdown.required.set(false)
            }
        }

        // detekt attaches its `detekt` task to `check`; keep static analysis out of test runs.
        tasks.matching { it.name == "check" }.configureEach {
            setDependsOn(dependsOn.filterNot { it.isDetektTask() })
        }
    }

    private fun Project.registerStaticAnalysisTask() {
        val typeResolution = providers.gradleProperty(StaticAnalysisTypeResolutionProperty)
            .getOrElse("true")
            .toBoolean()

        val staticAnalysis = tasks.register(StaticAnalysisTaskName) {
            group = "verification"
            description = "Runs static analysis and fails if there are any findings."
        }
        if (typeResolution) {
            staticAnalysis.configure {
                dependsOn(tasks.withType<Detekt>().matching { task -> isTypeResolvedDetektTask(task.name) })
            }
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                tasks.named<Detekt>("detekt") {
                    setSource(provider { sourceDirsWithoutTypeResolution() })
                }
                staticAnalysis.configure { dependsOn("detekt") }
            }
        } else {
            staticAnalysis.configure { dependsOn("detekt") }
        }
    }

    /** The root project merges every module's SARIF report into one, which CI uploads. */
    private fun Project.registerReportMerge() {
        if (this == rootProject) {
            tasks.register<ReportMergeTask>(StaticAnalysisReportTaskName) {
                group = "verification"
                description = "Merges every module's static analysis findings into one SARIF report."
                output.set(layout.buildDirectory.file("reports/detekt/static-analysis.sarif"))
            }
        }
        rootProject.pluginManager.withPlugin("com.embarrasdf.gradle.plugin.static.analysis") {
            val report = rootProject.tasks.named<ReportMergeTask>(StaticAnalysisReportTaskName)
            report.configure {
                // Read lazily: Android variant tasks are registered after this plugin applies.
                input.from(provider { tasks.withType<Detekt>().map { it.reports.sarif.outputLocation.get() } })
            }
            tasks.withType<Detekt>().configureEach { finalizedBy(report) }
        }
    }
}
