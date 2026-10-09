# gradle-plugins

## Formatting

`com.embarrasdf.gradle.plugin.lint` formats Kotlin sources and Gradle Kotlin scripts with
[ktlint](https://pinterest.github.io/ktlint/), run through [Spotless](https://github.com/diffplug/spotless).
The application and library convention plugins apply it to every module. Apply it to the root project
as well, so the root `build.gradle.kts` and `settings.gradle.kts` are covered:

```kotlin
plugins {
    alias(libs.plugins.embarrasdf.lint)
}
```

| Task | What it does |
| --- | --- |
| `./gradlew embarrasdfFormat` | Fixes everything that can be fixed automatically. |
| `./gradlew embarrasdfFormatCheck` | Fails and lists every remaining issue. |

Neither task runs as part of `check`. Some issues, like a constant that isn't PascalCase, can't be
fixed automatically: `embarrasdfFormat` fixes the rest and then fails, listing them. Run it with
`--continue` to have it format every module before failing.

The ktlint settings live in
[`ktlint.editorconfig.properties`](plugins/src/main/resources/com/embarrasdf/gradle/plugin/ktlint.editorconfig.properties).
In short: ktlint's `intellij_idea` style, 120-character lines, trailing commas on declarations and
call sites, PascalCase constants and `@Composable` functions, and author's choice for how function
signatures and supertype lists wrap.
