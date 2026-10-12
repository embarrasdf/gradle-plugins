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

Run from the root, a task runs in every project that has it; with a project
path, such as `:apps:foo:androidApp:embarrasdfTest`, in that project only.
