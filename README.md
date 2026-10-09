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
