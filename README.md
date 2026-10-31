# gradle-plugins

Convention plugins for the embarrasdf Kotlin Multiplatform repos, published to
Maven Central as `com.embarrasdf.gradle.plugin:plugins`.

## Working on these plugins from another repo

Repos apply the settings plugin in their `settings.gradle.kts`:

```kotlin
plugins {
    id("com.embarrasdf.gradle.plugin.settings") version "<version>"
}
```

With a gradle-plugins checkout beside the repo, add `includeGradlePlugins=true`
to the repo's `local.properties` and it builds against these plugins from source.
Without that line it uses the published plugins, the same ones its CI uses.

## CI tasks

The convention plugins register the same tasks in every repo, so the shared
workflows in [embarrasdf/devops](https://github.com/embarrasdf/devops) can run
one command everywhere:

| Task | Runs |
| --- | --- |
| `embarrasdfTest` | JVM, Android unit (debug) and browser tests |
| `embarrasdfTestApple` | iOS simulator tests |
| `embarrasdfWarmCaches` | Builds what `embarrasdfTest` needs without running the tests, plus the Android (debug), desktop and web apps, to fill the Gradle caches for workspace images |
| `embarrasdfRelease` | Maven Central publishing, the templated README and the GitHub release |
| `embarrasdfFormat`, `embarrasdfFormatCheck` | ktlint fixes and checks |
| `embarrasdfWriteLocks` | Rewrites the lockfiles, in a repo that locks dependencies |

Run from the root, a task runs in every project that has it; with a project
path, such as `:apps:foo:androidApp:embarrasdfTest`, in that project only.

## Dependency locking

With this line in a repo's `gradle.properties`, the settings plugin locks every
configuration of every project, and each project's build script classpath, with
Gradle's [dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html):

```properties
embarrasdf.dependencyLocking=true
```

Each project then has a `gradle.lockfile`, and a `buildscript-gradle.lockfile`
for its Gradle plugins, listing every resolved version, transitive ones
included. Commit them. When a dependency change moves a locked version, the
build fails until the lockfiles are rewritten, so the change shows in the pull
request. To rewrite them after changing a dependency:

```sh
./gradlew embarrasdfWriteLocks
```

It resolves every configuration with `--write-locks`, which it turns on itself.
On pull requests, CI's Dependency Locks job from
[embarrasdf/devops](https://github.com/embarrasdf/devops#dependency-changes)
rewrites them too, commits them to Renovate's branches, and reports what moved.

Locking isn't strict. AGP and the Kotlin plugin create a few configurations
only when a task needs them, such as AGP's `androidApis`, so
`embarrasdfWriteLocks` never sees them. In strict mode those would fail the
build; here they resolve unlocked. Every configuration that exists when the
project is configured is locked, and that includes all the compile, runtime,
test and tooling classpaths.
