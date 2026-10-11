import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateLibsTask : DefaultTask() {

    @get:InputFile
    abstract val tomlFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val catalogs = project.extensions.getByType(VersionCatalogsExtension::class.java)
        val libs = catalogs.find("libs").get()

        val libraries = libs.libraryAliases.mapNotNull { alias ->
            libs.findLibrary(alias).orElse(null)?.let { provider ->
                val dep = provider.get()
                Library(
                    alias = alias,
                    group = dep.module.group,
                    artifact = dep.module.name,
                    version = dep.versionConstraint.requiredVersion,
                )
            }
        }

        val kotlinCode = buildString {
            appendLine("package com.embarrasdf.gradle.plugin")
            appendLine()
            appendLine("// This file is auto-generated from gradle/libs.versions.toml")
            appendLine("// Do not edit manually - changes will be overwritten")
            appendLine()

            // Generate Libs object with flat structure
            appendLine("object Libs {")
            appendLine("    private fun dependency(group: String, artifact: String, version: String) =")
            appendLine("        if (version.isEmpty()) \"\$group:\$artifact\" else \"\$group:\$artifact:\$version\"")
            appendLine()

            // Generate flat properties
            libraries.sortedBy { it.alias }.forEach { lib ->
                val propName = toCamelCase(lib.alias)
                val versionPart = if (lib.version.isNotEmpty()) "\"${lib.version}\"" else "\"\""
                appendLine("    val $propName = dependency(\"${lib.group}\", \"${lib.artifact}\", $versionPart)")
            }

            appendLine("}")
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(kotlinCode)
        }
    }

    private fun toCamelCase(alias: String): String {
        val parts = alias.split("-", ".", "_")
        return parts.first() + parts.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    data class Library(
        val alias: String,
        val group: String,
        val artifact: String,
        val version: String,
    )
}
