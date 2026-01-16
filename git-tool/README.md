# Git Tool Plugin

A Gradle plugin for automating Git operations in your build process.

## Features

- ✅ Check Git working directory status
- ✅ Create and validate Git tags
- ✅ Push tags to remote repositories
- ✅ Complete release workflows
- ✅ Configurable via DSL
- ✅ Proper Gradle integration with logging

## Installation

### Using Plugin DSL (Recommended)

```kotlin
plugins {
    id("io.github.lekanich.git-tool") version "1.0.0"
}
```

### Using Legacy Plugin Application

```kotlin
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("io.github.lekanich.tool.gradle:git-tool:1.0.0")
    }
}

apply(plugin = "io.github.lekanich.git-tool")
```

## Configuration

Configure the plugin using the `gitTool` extension:

```kotlin
gitTool {
    enabled.set(true)                              // Enable/disable plugin (default: true)
    remoteName.set("origin")                       // Remote name for push operations (default: "origin")
    defaultTagMessage.set("Release {tag}")         // Default tag message template (default: "Release {tag}")
    validateBeforeTag.set(true)                    // Check tag doesn't exist before creating (default: true)
    requireCleanWorkspace.set(true)                // Require clean workspace for tag operations (default: true)
}
```

## Available Tasks

### `gitCheckStatus`

Checks if the Git working directory is clean (no uncommitted changes).

```bash
./gradlew gitCheckStatus
```

**Fails if:**
- Working directory has uncommitted changes
- Files are staged but not committed

---

### `gitCheckTag`

Checks if a Git tag already exists locally.

```bash
./gradlew gitCheckTag -PtagName=v1.0.0
```

**Options:**
- `tagName` (required) - The name of the tag to check

**Fails if:**
- The specified tag already exists

---

### `gitCreateTag`

Creates an annotated Git tag with a message.

```bash
./gradlew gitCreateTag -PtagName=v1.0.0
```

**Options:**
- `tagName` (required) - The name of the tag to create
- `tagMessage` (optional) - Custom message for the tag (uses `defaultTagMessage` from extension if not specified)

**Example with custom message:**
```bash
./gradlew gitCreateTag -PtagName=v1.0.0 -PtagMessage="Release version 1.0.0"
```

---

### `gitPushTag`

Pushes a Git tag to the remote repository.

```bash
./gradlew gitPushTag -PtagName=v1.0.0
```

**Options:**
- `tagName` (required) - The name of the tag to push
- `remoteName` (optional) - Remote repository name (uses `remoteName` from extension if not specified)

---

### `gitReleaseTag` (Workflow Task)

Complete Git release workflow that orchestrates multiple tasks:

1. Checks working directory status (if `requireCleanWorkspace` is true)
2. Validates tag doesn't exist (if `validateBeforeTag` is true)
3. Creates the tag
4. Pushes the tag to remote

```bash
./gradlew gitReleaseTag -PtagName=v1.0.0
```

This is the recommended way to create and push tags as it ensures all validations are performed.

---

## Usage Examples

### Basic Release Workflow

```bash
# Create and push a release tag
./gradlew gitReleaseTag -PtagName=v1.0.0
```

### Custom Configuration

```kotlin
gitTool {
    remoteName.set("upstream")
    defaultTagMessage.set("🚀 Version {tag}")
    requireCleanWorkspace.set(false)  // Allow dirty workspace
}
```

### Manual Step-by-Step

```bash
# 1. Check workspace is clean
./gradlew gitCheckStatus

# 2. Verify tag doesn't exist
./gradlew gitCheckTag -PtagName=v1.0.0

# 3. Create the tag
./gradlew gitCreateTag -PtagName=v1.0.0

# 4. Push to remote
./gradlew gitPushTag -PtagName=v1.0.0
```

### Integration with Publishing

```kotlin
tasks.named("publish") {
    dependsOn("gitReleaseTag")
}
```

### Automated Versioning

```kotlin
val projectVersion = providers.gradleProperty("version").get()

tasks.register("release") {
    group = "release"
    description = "Create release tag and publish artifacts"
    
    dependsOn("gitReleaseTag", "publish")
    
    doFirst {
        tasks.getByName<GitCreateTag>("gitCreateTag").apply {
            tagName.set("v$projectVersion")
        }
        tasks.getByName<GitPushTag>("gitPushTag").apply {
            tagName.set("v$projectVersion")
        }
    }
}
```

## Requirements

- Git must be installed and available in PATH
- Git repository must be initialized (`.git` directory exists)
- Git user name and email must be configured for tag creation

## Troubleshooting

### Git not found

**Error:** `Git is not available on this system`

**Solution:** Install Git from https://git-scm.com/downloads and ensure it's in your PATH.

### Tag already exists

**Error:** `Git tag 'v1.0.0' already exists`

**Solution:** 
- Use a different tag name, or
- Delete the existing tag: `git tag -d v1.0.0`
- For remote: `git push origin --delete v1.0.0`

### Dirty workspace

**Error:** `Workspace is dirty. Please commit or stash changes`

**Solution:**
- Commit your changes: `git add . && git commit -m "message"`
- Or stash them: `git stash`
- Or disable the check: `gitTool { requireCleanWorkspace.set(false) }`

### Push authentication failed

**Error:** Git push fails with authentication error

**Solution:**
- Ensure you have push access to the remote repository
- Configure Git credentials (SSH keys or credential helper)
- Use HTTPS with credential manager or SSH with SSH keys

## Contributing

Issues and pull requests are welcome at https://github.com/Lekanich/GradlePlugins

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.