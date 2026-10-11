package com.embarrasdf.gradle.plugin

import dev.detekt.gradle.Detekt
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File

const val StaticAnalysisTaskName = "embarrasdfStaticAnalysis"
const val StaticAnalysisReportTaskName = "embarrasdfStaticAnalysisReport"

/** Set to false (in gradle.properties or with -P) to skip type resolution and the compilation it needs. */
const val StaticAnalysisTypeResolutionProperty = "embarrasdf.staticAnalysis.typeResolution"

internal val DetektConfig: String by lazy {
    val resource = checkNotNull(Libs::class.java.getResourceAsStream("detekt.yml")) {
        "detekt.yml is missing from the plugin jar"
    }
    resource.bufferedReader().use { it.readText() }
}

/** Writes the detekt config shipped in the plugin jar to a file, which detekt tasks read. */
@CacheableTask
abstract class WriteDetektConfigTask : DefaultTask() {
    @get:Input
    abstract val content: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        outputFile.get().asFile.writeText(content.get())
    }
}

/** Whether a task dependency refers to a detekt task, as a task or a task provider. */
internal fun Any.isDetektTask(): Boolean = this is Detekt || (this is TaskProvider<*> && name.startsWith("detekt"))

/**
 * Whether a detekt task runs with type resolution: detekt's own `detekt` task and the per-source-set
 * tasks don't, and release variants are skipped since debug covers the same sources.
 */
internal fun isTypeResolvedDetektTask(name: String): Boolean = name != "detekt" &&
    !name.endsWith("SourceSet") &&
    !name.contains("Release")

/**
 * Kotlin source directories that no JVM or Android compilation includes, such as iosMain or
 * wasmJsMain. detekt only creates type-resolved tasks for JVM and Android targets, so these get
 * the light analysis instead.
 */
internal fun Project.sourceDirsWithoutTypeResolution(): List<File> {
    val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
    val typeResolvedSourceSets = kotlin.targets
        .filter { it.platformType == KotlinPlatformType.jvm || it.platformType == KotlinPlatformType.androidJvm }
        .flatMap { target -> target.compilations.flatMap { it.allKotlinSourceSets } }
        .toSet()
    return kotlin.sourceSets
        .filter { it !in typeResolvedSourceSets }
        .flatMap { it.kotlin.srcDirs }
        .filter { it.exists() }
}
