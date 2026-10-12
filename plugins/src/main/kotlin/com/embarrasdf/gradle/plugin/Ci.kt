package com.embarrasdf.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import java.util.concurrent.Callable

const val TestTaskName = "embarrasdfTest"
const val AppleTestTaskName = "embarrasdfTestApple"
const val ReleaseTaskName = "embarrasdfRelease"
const val WarmCachesTaskName = "embarrasdfWarmCaches"

// Matched by name so that each project depends only on the tasks its targets created. Android
// release unit tests and the Android `test` aggregate are left out on purpose.
private val JvmTestTaskNames = setOf("test", "jvmTest", "testDebugUnitTest", "testAndroidHostTest")
private val WebTestTaskNames = setOf("wasmJsBrowserTest")
private val AppleTestTaskNames = setOf("iosSimulatorArm64Test")

/**
 * Registers [TestTaskName], which runs the tests a Linux machine can run, [AppleTestTaskName],
 * which runs the iOS simulator tests, and [WarmCachesTaskName], which builds everything those
 * Linux tests need without running them.
 */
internal fun Project.registerTestTasks() {
    if (TestTaskName in tasks.names) return

    val jvmTests = tasks.withType<Test>().named { name -> name in JvmTestTaskNames }
    val webTests = tasks.named { name -> name in WebTestTaskNames }
    tasks.register(TestTaskName) {
        group = "verification"
        description = "Runs the JVM, Android unit and browser tests."
        dependsOn(jvmTests)
        dependsOn(webTests)
    }
    tasks.register(AppleTestTaskName) {
        group = "verification"
        description = "Runs the iOS simulator tests."
        dependsOn(tasks.named { name -> name in AppleTestTaskNames })
    }
    tasks.register(WarmCachesTaskName) {
        group = "build"
        description = "Fills the Gradle caches a session or CI run needs: builds the apps and " +
            "compiles the main and test code, without running tests."
        // What the tests depend on, but not the tests: running the browser tests needs Chrome.
        dependsOn(Callable { (jvmTests + webTests).map { test -> test.taskDependencies } })
    }
}

/**
 * Adds [taskName], such as an app's build, to [WarmCachesTaskName]. A task that doesn't exist is
 * skipped.
 */
internal fun Project.addToWarmCachesTask(taskName: String) {
    registerTestTasks()
    tasks.named(WarmCachesTaskName) {
        dependsOn(tasks.named { name -> name == taskName })
    }
}

/**
 * Registers [ReleaseTaskName], or adds [taskName] to it if it's already registered. A task that
 * doesn't exist is skipped.
 */
internal fun Project.addToReleaseTask(taskName: String) {
    if (ReleaseTaskName !in tasks.names) {
        tasks.register(ReleaseTaskName) {
            group = "publishing"
            description = "Publishes this project's release."
        }
    }
    tasks.named(ReleaseTaskName) {
        dependsOn(tasks.named { name -> name == taskName })
    }
}
