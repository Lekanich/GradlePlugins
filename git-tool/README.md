# Git Tool Plugin

A Gradle plugin for automating Git operations in your build process.

## Features

- ✅ Check Git working directory status
- ✅ Create and validate Git tags
- ✅ Push tags to remote repositories
- ✅ Delete tags locally and remotely
- ✅ Get Git information (branch, commit hash, tags, commit count)
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

### `gitInstalled`

Verifies that Git is installed and available on the system.

```bash
./gradlew gitInstalled
```

This task checks if Git is properly installed and accessible from PATH by executing `git --version`.

**Fails if:**
- Git is not installed
- Git is not available in PATH
- Git command fails to execute

**Use case:** Validate Git availability before running other Git tasks in CI/CD pipelines.

---

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

### `gitDeleteTag`

Deletes a Git tag locally.

```bash
./gradlew gitDeleteTag -PtagName=v1.0.0
```

**Options:**
- `tagName` (required) - The name of the tag to delete

**Use case:** Remove obsolete or incorrect tags from your local repository.

---

### `gitDeleteRemoteTag`

Deletes a Git tag from a remote repository.

```bash
./gradlew gitDeleteRemoteTag -PtagName=v1.0.0
```

**Options:**
- `tagName` (required) - The name of the tag to delete
- `remoteName` (optional) - Remote repository name (default: "origin")

**Use case:** Remove tags from remote repository after deleting locally.

**Example - Delete both local and remote:**
```bash
./gradlew gitDeleteTag gitDeleteRemoteTag -PtagName=v1.0.0
```

---

### `gitGetCurrentBranch`

Gets the current Git branch name and writes it to a file.

```bash
./gradlew gitGetCurrentBranch
```

**Output:** `build/git/branch.txt`

**Use case:** Include branch name in build artifacts or version strings.

---

### `gitGetCommitHash`

Gets the current Git commit hash and writes it to a file.

```bash
# Get full commit hash
./gradlew gitGetCommitHash

# Get short commit hash (7 characters)
./gradlew gitGetCommitHash -PshortHash=true
```

**Options:**
- `shortHash` (optional) - Use short hash format (default: false)

**Output:** `build/git/commit-hash.txt`

**Use case:** Include commit hash in build metadata or version strings.

---

### `gitListTags`

Lists all Git tags matching a pattern and writes them to a file.

```bash
# List all tags
./gradlew gitListTags

# List only version tags
./gradlew gitListTags -Ppattern="v*"
```

**Options:**
- `pattern` (optional) - Pattern to filter tags (e.g., "v*")

**Output:** `build/git/tags.txt` (one tag per line)

**Use case:** Version bumping, changelog generation.

---

### `gitGetLastTag`

Gets the most recent Git tag and writes it to a file.

```bash
./gradlew gitGetLastTag
```

**Output:** `build/git/last-tag.txt`

**Use case:** Determine the previous version for changelog generation.

---

### `gitCommitCount`

Counts Git commits and writes the count to a file.

```bash
# Count all commits
./gradlew gitCommitCount

# Count commits since a specific tag
./gradlew gitCommitCount -Psince=v1.0.0
```

**Options:**
- `since` (optional) - Starting point (tag, commit, or branch)

**Output:** `build/git/commit-count.txt`

**Use case:** Generate build numbers based on commit count.

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

### Include Git Info in Build

```kotlin
// Get current branch and commit hash
val getBranch = tasks.register("getBranch", GitGetCurrentBranch::class.java) {
    outputFile.set(layout.buildDirectory.file("git/branch.txt"))
}

val getCommit = tasks.register("getCommit", GitGetCommitHash::class.java) {
    shortHash.set(true)
    outputFile.set(layout.buildDirectory.file("git/commit.txt"))
}

// Use output properties directly (no file reading needed!)
tasks.register("printVersion") {
    dependsOn(getBranch, getCommit)
    doLast {
        val branch = getBranch.get().branchName.get()
        val commit = getCommit.get().commitHash.get()
        println("Version: ${project.version}-$branch-$commit")
    }
}

// Or read from file if preferred
tasks.register("printVersionFromFile") {
    dependsOn(getBranch, getCommit)
    doLast {
        val branch = getBranch.get().outputFile.get().asFile.readText()
        val commit = getCommit.get().outputFile.get().asFile.readText()
        println("Version: ${project.version}-$branch-$commit")
    }
}
```

### Generate Build Number from Commits

```kotlin
val commitCount = tasks.register("commitCount", GitCommitCount::class.java) {
    since.set("v1.0.0")  // Count since last release
    outputFile.set(layout.buildDirectory.file("git/build-number.txt"))
}

// Access the count directly from the task property
tasks.register("printBuildNumber") {
    dependsOn(commitCount)
    doLast {
        val buildNumber = commitCount.get().commitCount.get()
        println("Build #$buildNumber")
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