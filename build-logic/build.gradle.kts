import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(embarrasdfPluginLibs.kotlin.gradle.plugin)
    implementation(embarrasdfPluginLibs.spotless.plugin)
    implementation(embarrasdfPluginLibs.detekt.gradle.plugin)
}

// Compiled by the JDK that runs Gradle, for Java 17, as the plugins module is.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

gradlePlugin {
    plugins {
        create("generateLibs") {
            id = "generate-libs"
            implementationClass = "GenerateLibsPlugin"
        }
    }
}
