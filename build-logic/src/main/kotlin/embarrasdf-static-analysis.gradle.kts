import dev.detekt.gradle.Detekt

// gradle-plugins can't apply its own static analysis plugin, so this configures detekt the same
// way for its modules, from the same config file the plugin ships.

plugins {
    id("dev.detekt")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("embarrasdfPluginLibs")

detekt {
    buildUponDefaultConfig.set(true)
    config.setFrom(rootProject.file("plugins/src/main/resources/com/embarrasdf/gradle/plugin/detekt.yml"))
    source.setFrom("src")
    parallel.set(true)
}

dependencies {
    // The shared config turns on compose-rules, so they have to be loaded.
    detektPlugins(catalog.findLibrary("compose-rules-detekt").get())
}

val buildDir = layout.buildDirectory.get().asFile
tasks.withType<Detekt>().configureEach {
    // Type-resolved tasks also see generated sources, like the generated Libs object, which sit
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
    setDependsOn(dependsOn.filterNot { it is Detekt || (it is TaskProvider<*> && it.name.startsWith("detekt")) })
}

tasks.register("embarrasdfStaticAnalysis") {
    group = "verification"
    description = "Runs static analysis and fails if there are any findings."
    dependsOn(tasks.withType<Detekt>().matching { it.name != "detekt" && !it.name.endsWith("SourceSet") })
}
