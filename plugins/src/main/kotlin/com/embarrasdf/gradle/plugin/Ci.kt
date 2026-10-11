package com.embarrasdf.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

const val TestTaskName = "embarrasdfTest"
const val AppleTestTaskName = "embarrasdfTestApple"
const val ReleaseTaskName = "embarrasdfRelease"

// Matched by name so that each project depends only on the tasks its targets created. Android
// release unit tests and the Android `test` aggregate are left out on purpose.
private val JvmTestTaskNames = setOf("test", "jvmTest", "testDebugUnitTest", "testAndroidHostTest")
private val WebTestTaskNames = setOf("wasmJsBrowserTest")
private val AppleTestTaskNames = setOf("iosSimulatorArm64Test")

/**
 * Registers [TestTaskName], which runs the tests a Linux machine can run, and [AppleTestTaskName],
 * which runs the iOS simulator tests.
 */
internal fun Project.registerTestTasks() {
    if (TestTaskName in tasks.names) return

    tasks.register(TestTaskName) {
        group = "verification"
        description = "Runs the JVM, Android unit and browser tests."
        dependsOn(tasks.withType<Test>().named { name -> name in JvmTestTaskNames })
        dependsOn(tasks.named { name -> name in WebTestTaskNames })
    }
    tasks.register(AppleTestTaskName) {
        group = "verification"
        description = "Runs the iOS simulator tests."
        dependsOn(tasks.named { name -> name in AppleTestTaskNames })
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
