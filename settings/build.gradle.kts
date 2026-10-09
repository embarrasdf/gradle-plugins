import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kept apart from :plugins and free of dependencies. A settings plugin's jar
// goes on the settings classpath, which every project's classpath inherits; if
// it carried the convention plugins, the published ones would shadow those
// built from ../gradle-plugins.
plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(embarrasdfPluginLibs.plugins.maven.publish)
}

group = "com.embarrasdf.gradle.plugin"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(embarrasdfPluginLibs.junit.jupiter)
    testImplementation(embarrasdfPluginLibs.junit.platform.launcher)
    testImplementation(gradleTestKit())
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }

    test {
        useJUnitPlatform()
    }
}

gradlePlugin {
    plugins {
        register("settings") {
            id = "com.embarrasdf.gradle.plugin.settings"
            implementationClass = "SettingsConventionPlugin"
        }
    }
}
