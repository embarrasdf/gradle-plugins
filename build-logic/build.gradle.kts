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

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("generateLibs") {
            id = "generate-libs"
            implementationClass = "GenerateLibsPlugin"
        }
    }
}
