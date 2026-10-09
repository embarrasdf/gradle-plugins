package com.embarrasdf.gradle.plugin

import java.util.Properties

const val FormatTaskName = "embarrasdfFormat"
const val FormatCheckTaskName = "embarrasdfFormatCheck"

internal val KtlintVersion = Libs.ktlintCli.substringAfterLast(':')

internal val KtlintEditorConfig: Map<String, String> by lazy {
    val properties = Properties()
    val resource = checkNotNull(Libs::class.java.getResourceAsStream("ktlint.editorconfig.properties")) {
        "ktlint.editorconfig.properties is missing from the plugin jar"
    }
    resource.use { properties.load(it) }
    properties.stringPropertyNames().associateWith { properties.getProperty(it) }
}
