# Project Context for AI Assistants

## Overview

Collection of Gradle plugins for build automation and tooling. Each submodule represents a standalone Gradle plugin with focused functionality.

## Current Plugins

### git-tool

Gradle plugin for Git operations automation:
- Check Git repository state
- Create and manage Git tags
- Validate Git working directory status
- Common Git workflow automation

## Project Structure

- Root project: Multi-module Gradle build
- `git-tool/` - Git automation plugin
- Each plugin is independently publishable

## Key Technologies

- Kotlin 2.*.* (primary language for plugin code)
- Gradle 8.x (build system and plugin API)
- JUnit 5 (testing framework)

## Code Conventions

- Use Kotlin for all new plugin code
- Follow Gradle plugin best practices
- Prefer Gradle's Configuration Cache compatibility
- Use Gradle's lazy configuration APIs (`Provider`, `Property`)
- Implement plugins using `Plugin<Project>` interface
- Use extension objects for plugin configuration
- Prefer strongly-typed task configuration

## Common Gradle Patterns

### Plugin Implementation

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("myPlugin", MyExtension::class.java)
        // Register tasks using lazy APIs
    }
}
```

### Task Definition

```kotlin
abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val myProperty: Property<String>
    
    @TaskAction
    fun execute() {
        // Task implementation
    }
}
```

### Extension Object

```kotlin
abstract class MyExtension {
    abstract val enabled: Property<Boolean>
    abstract val configValue: Property<String>
    
    init {
        enabled.convention(true)
    }
}
```

## Development Setup

### Building Plugins

```bash
./gradlew build
```

### Testing Locally

Publish to local Maven repository for validation:

```bash
./gradlew publishToMavenLocal
./gradlew :git-tool:publishToMavenLocal
```

### Publishing Validation

Validate plugin publication without actually publishing:

```bash
./gradlew :git-tool:publishPlugins --validate-only
```

### Debugging Plugins

To debug a plugin during development:

```bash
./gradlew :git-tool:test --debug-jvm
```

Or add `println()` statements and run with `--info` or `--debug` flags:

```bash
./gradlew build --info
```

## Testing Instructions

- Write tests for all plugin functionality
- Test task execution and configuration
- Test plugin application to projects
- Use Gradle TestKit for integration tests
- Add unit tests for utility classes
- Add or update tests for code changes
- Test data location: `src/test/resources/`

### Running Tests

```bash
./gradlew test
./gradlew :git-tool:test  # Test specific plugin
```

## Using Plugins

Example of how users apply plugins from this collection:

```kotlin
// In settings.gradle.kts or build.gradle.kts
plugins {
    id("io.github.lekanich.git-tool") version "1.0.0"
}

// Configure plugin
gitTool {
    enabled.set(true)
    // Additional configuration
}
```

## Adding New Plugins

1. Create new submodule directory
2. Apply `kotlin-dsl` and `com.gradle.plugin-publish` in build script
3. Implement `Plugin<Project>` interface
4. Define plugin ID in `gradlePlugin` configuration
5. Add tests using Gradle TestKit
6. Document plugin usage in module README

## Plugin Distribution

Plugins can be published to:
- Gradle Plugin Portal (for public plugins)
- Maven repositories (for internal/private plugins)
- Local Maven repository (for development)

## Best Practices

### Configuration Cache Compatibility

- Avoid using `Project` at task execution time
- Use `Provider` and `Property` types for all inputs
- Don't capture `Project` references in task actions
- Use `@Input`, `@InputFile`, `@OutputFile` annotations properly
- Don't use `project.exec` use separate call to commandLine and then `super.exec()` inside the Task that implement Exec
- `git-tool` Tasks should extend Exec.

### Performance

- Use lazy task registration with `tasks.register()` instead of `tasks.create()`
- Avoid eager configuration of tasks
- Make tasks cacheable with `@CacheableTask` when appropriate
- Minimize work done during configuration phase

### Error Handling

```kotlin
// Provide clear error messages
if (!gitDir.exists()) {
    throw GradleException("Git repository not found at ${gitDir.absolutePath}")
}

// Validate configuration early
extension.requiredProperty.orNull 
    ?: throw GradleException("Property 'requiredProperty' must be set")
```

## Troubleshooting

### Common Issues

- **Plugin not found**: Ensure plugin is published to `mavenLocal()` or appropriate repository
- **Configuration cache warnings**: Check for `Project` usage at execution time
- **Task not running**: Verify task inputs have changed or use `--rerun-tasks`
- **Version conflicts**: Check dependency versions in `gradle/libs.versions.toml`
