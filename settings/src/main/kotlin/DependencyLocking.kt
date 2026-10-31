import org.gradle.api.initialization.Settings

internal const val DependencyLockingProperty = "embarrasdf.dependencyLocking"
internal const val WriteLocksTaskName = "embarrasdfWriteLocks"

/**
 * With `embarrasdf.dependencyLocking=true` in gradle.properties, locks every configuration of every
 * project, and each project's build script classpath, to the versions in its gradle.lockfile and
 * buildscript-gradle.lockfile. A dependency change that moves any locked version, direct or
 * transitive, then fails the build until the lockfiles are rewritten, so the change shows in the
 * diff.
 *
 * [WriteLocksTaskName] rewrites them: it resolves every configuration with `--write-locks`, which
 * it turns on itself.
 */
internal fun Settings.configureDependencyLocking() {
    if (!providers.gradleProperty(DependencyLockingProperty).orNull.toBoolean()) return

    val writeLocks = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == WriteLocksTaskName }
    if (writeLocks) {
        gradle.startParameter.isWriteDependencyLocks = true
    }

    gradle.lifecycle.beforeProject {
        // Not strict: AGP and KGP create some configurations only when a task needs them, after
        // the locks were written, and strict mode fails on a configuration without lock state.
        // CI writes the locks and fails on any difference instead.
        dependencyLocking {
            lockAllConfigurations()
        }
        buildscript.configurations.named("classpath") {
            resolutionStrategy.activateDependencyLocking()
        }

        tasks.register(WriteLocksTaskName) {
            group = "help"
            description = "Writes this project's gradle.lockfile from the versions every configuration resolves to."
            notCompatibleWithConfigurationCache("Resolves the project's configurations when it runs")
            doLast {
                // The dependency graph is enough to record the versions; no artifacts are downloaded.
                project.configurations
                    .filter { it.isCanBeResolved }
                    .forEach { it.incoming.resolutionResult.root }
            }
        }
    }
}
