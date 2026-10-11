package com.embarrasdf.gradle.plugin

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.apply {
        buildFeatures.compose = true

        dependencies {
            "implementation"(platform(Libs.composeBom))
            "implementation"(Libs.composeFoundation)
            "implementation"(Libs.composeRuntime)
            "implementation"(Libs.composeUiToolingPreview)
            "implementation"(Libs.lifecycleRuntimeCompose)

            "androidTestImplementation"(platform(Libs.composeBom))
            "androidTestImplementation"(Libs.composeUiTestJunit4)
            "androidTestImplementation"(Libs.androidxTestEspressoCore)

            "debugImplementation"(Libs.composeUiTooling)
        }
    }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        }
    }
}
