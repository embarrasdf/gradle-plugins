package com.embarrasdf.gradle.plugin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl

private const val KotlinJsYarnProperty = "kotlin.js.yarn"

/**
 * Makes Kotlin/JS and Kotlin/Wasm install their npm tooling with npm instead of Yarn.
 *
 * Kotlin's bundled test tooling depends on its fork of karma, hosted on GitHub. The
 * Yarn lockfile downloads it from codeload.github.com, which Claude Code cloud
 * sessions can't reach; the npm lockfile fetches it with git, which they can.
 *
 * The Kotlin Gradle plugin reads this from the root project's properties, so it's set
 * there, before the plugin is applied. A repo can still pick Yarn by setting
 * `kotlin.js.yarn=true` in its gradle.properties.
 */
internal fun Project.useNpmForKotlinWeb() {
    if (!rootProject.hasProperty(KotlinJsYarnProperty)) {
        rootProject.extra[KotlinJsYarnProperty] = "false"
    }
}

/**
 * Runs browser tests in headless Chrome without its sandbox, which Chrome requires
 * when running as root, as it does in Claude Code cloud sessions and many containers.
 */
internal fun KotlinJsBrowserDsl.configureBrowserTests() {
    testTask {
        useKarma {
            useChromeHeadlessNoSandbox()
        }
    }
}
