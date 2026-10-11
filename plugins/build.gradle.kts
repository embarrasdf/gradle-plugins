import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(embarrasdfPluginLibs.plugins.maven.publish)
    alias(embarrasdfPluginLibs.plugins.embarrasdf.static.analysis)
    id("generate-libs")
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
    implementation(embarrasdfPluginLibs.android.gradle.plugin)
    implementation(embarrasdfPluginLibs.android.kotlin.multiplatform.library.plugin)
    implementation(embarrasdfPluginLibs.android.tools.common)
    implementation(embarrasdfPluginLibs.androidx.baselineprofile.plugin)
    implementation(embarrasdfPluginLibs.jetbrains.compose)
    implementation(embarrasdfPluginLibs.compose.compiler.plugin)
    implementation(embarrasdfPluginLibs.kotlin.gradle.plugin)
    implementation(embarrasdfPluginLibs.kotlin.multiplatform.plugin)
    implementation(embarrasdfPluginLibs.kotlin.serialization.plugin)
    implementation(embarrasdfPluginLibs.ksp.gradle.plugin)
    implementation(embarrasdfPluginLibs.room.gradle.plugin)
    implementation(embarrasdfPluginLibs.shipkit.autoversion.plugin)
    implementation(embarrasdfPluginLibs.shipkit.changelog.plugin)
    implementation(embarrasdfPluginLibs.maven.publish.plugin)
    implementation(embarrasdfPluginLibs.firebase.testlab.plugin)
    implementation(embarrasdfPluginLibs.spotless.plugin)
    implementation(embarrasdfPluginLibs.detekt.gradle.plugin)

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
        register("androidApplicationCompose") {
            id = "com.embarrasdf.gradle.plugin.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "com.embarrasdf.gradle.plugin.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidBaselineProfileConsumerApp") {
            id = "com.embarrasdf.gradle.plugin.android.baselineprofile.consumer.app"
            implementationClass = "AndroidBaselineProfileConsumerAppConventionPlugin"
        }
        register("androidBaselineProfileConsumerLibrary") {
            id = "com.embarrasdf.gradle.plugin.android.baselineprofile.consumer.library"
            implementationClass = "AndroidBaselineProfileConsumerLibraryConventionPlugin"
        }
        register("androidBaselineProfileGenerator") {
            id = "com.embarrasdf.gradle.plugin.android.baselineprofile.generator"
            implementationClass = "AndroidBaselineProfileGeneratorConventionPlugin"
        }
        register("androidBenchmark") {
            id = "com.embarrasdf.gradle.plugin.android.benchmark"
            implementationClass = "AndroidBenchmarkConventionPlugin"
        }
        register("androidInstrumentedTest") {
            id = "com.embarrasdf.gradle.plugin.android.instrumented.test"
            implementationClass = "AndroidInstrumentedTestConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "com.embarrasdf.gradle.plugin.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.embarrasdf.gradle.plugin.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidComposeTest") {
            id = "com.embarrasdf.gradle.plugin.android.compose.test"
            implementationClass = "AndroidComposeTestConventionPlugin"
        }
        register("androidLibraryTestFixtures") {
            id = "com.embarrasdf.gradle.plugin.android.library.test.fixtures"
            implementationClass = "AndroidLibraryTestFixturesConventionPlugin"
        }
        register("androidTest") {
            id = "com.embarrasdf.gradle.plugin.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidRoom") {
            id = "com.embarrasdf.gradle.plugin.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "com.embarrasdf.gradle.plugin.compose.multiplatform"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("desktopApplication") {
            id = "com.embarrasdf.gradle.plugin.desktop.application"
            implementationClass = "DesktopApplicationConventionPlugin"
        }
        register("format") {
            id = "com.embarrasdf.gradle.plugin.format"
            implementationClass = "FormatConventionPlugin"
        }
        register("githubRelease") {
            id = "com.embarrasdf.gradle.plugin.github.release"
            implementationClass = "GithubReleaseConventionPlugin"
        }
        register("jvmLibrary") {
            id = "com.embarrasdf.gradle.plugin.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("kotlinMultiplatformLibrary") {
            id = "com.embarrasdf.gradle.plugin.kotlin.multiplatform.library"
            implementationClass = "KotlinMultiplatformLibraryConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "com.embarrasdf.gradle.plugin.kotlin.serialization"
            implementationClass = "KotlinSerializationConventionPlugin"
        }
        register("mavenPublish") {
            id = "com.embarrasdf.gradle.plugin.maven.publish"
            implementationClass = "MavenPublishConventionPlugin"
        }
        register("moduleUtils") {
            id = "com.embarrasdf.gradle.plugin.module.utils"
            implementationClass = "ModuleUtilsPlugin"
        }
        register("staticAnalysis") {
            id = "com.embarrasdf.gradle.plugin.static.analysis"
            implementationClass = "StaticAnalysisConventionPlugin"
        }
        register("webApplication") {
            id = "com.embarrasdf.gradle.plugin.web.application"
            implementationClass = "WebApplicationConventionPlugin"
        }
    }
}
