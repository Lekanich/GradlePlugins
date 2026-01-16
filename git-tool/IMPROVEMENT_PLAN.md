# Git-Tool Plugin Improvement Plan

## 📋 Current State Analysis

### Existing Features
- ✅ `gitCheckStatus` - Verify working directory is clean
- ✅ `gitCheckTag` - Check if tag exists
- ✅ `gitCreateTag` - Create annotated tag
- ✅ `gitPushTag` - Push tag to origin

### Current Issues & Limitations

1. **No Configuration Extension** - Tasks are not configurable via DSL
2. **No Tests** - No test directory or test coverage
3. **Hardcoded Values** - Remote name "origin" is hardcoded
4. **Limited Git Operations** - Only tag-related operations
5. **Poor Error Handling** - No validation of Git availability
6. **No Task Dependencies** - Tasks don't automatically depend on each other
7. **ByteArrayOutputStream Issues** - `standardOutput.toString()` needs casting
8. **No Logging** - Using `println()` instead of Gradle logger
9. **No Configuration Cache Support** - Uses `Project` at execution time potentially
10. **No Documentation** - Missing README and usage examples

---

## 🎯 Improvement Plan

### Phase 1: Foundation & Configuration (High Priority)

#### 1.1 Add Plugin Extension
**Goal**: Make plugin configurable via DSL

```kotlin
abstract class GitToolExtension {
    abstract val enabled: Property<Boolean>
    abstract val remoteName: Property<String>
    abstract val defaultTagMessage: Property<String>
    abstract val validateBeforeTag: Property<Boolean>
    abstract val requireCleanWorkspace: Property<Boolean>
    
    init {
        enabled.convention(true)
        remoteName.convention("origin")
        defaultTagMessage.convention("Release {tag}")
        validateBeforeTag.convention(true)
        requireCleanWorkspace.convention(true)
    }
}
```

**Files to modify**:
- `GitToolPlugin.kt` - Create and register extension
- All task files - Use extension properties

**Benefit**: Users can customize plugin behavior per project

---

#### 1.2 Add Comprehensive Tests
**Goal**: Ensure reliability and prevent regressions

**Test Structure**:
```
src/test/kotlin/lekanich/common/gradle/
├── GitToolPluginTest.kt          # Plugin application tests
├── GitCheckStatusTest.kt          # Task unit tests
├── GitCheckTagTest.kt
├── GitCreateTagTest.kt
├── GitPushTagTest.kt
└── integration/
    └── GitWorkflowIntegrationTest.kt  # End-to-end tests
```

**Test Types**:
- Unit tests for each task
- Integration tests using Gradle TestKit
- Mock Git repository for isolated testing
- Test error scenarios (no Git, dirty workspace, etc.)

**Benefit**: Confidence in releases, easier maintenance

---

#### 1.3 Fix Technical Issues
**Goal**: Improve code quality and Gradle best practices

**Changes**:
1. **Fix ByteArrayOutputStream casting**:
   ```kotlin
   val outputStream = ByteArrayOutputStream()
   standardOutput = outputStream
   super.exec()
   val output = outputStream.toString().trim()
   ```

2. **Replace println with Gradle logger**:
   ```kotlin
   logger.lifecycle("Git workspace is clean")
   logger.info("Checking git status...")
   ```

3. **Add Git availability check**:
   ```kotlin
   private fun checkGitAvailable(project: Project) {
       try {
           project.exec { commandLine("git", "--version") }
       } catch (e: Exception) {
           throw GradleException("Git is not available. Please install Git.", e)
       }
   }
   ```

4. **Configuration Cache compatibility**:
   - Avoid capturing `Project` in task actions
   - Use `Provider` types properly
   - Add `@Input` annotations where needed

**Benefit**: Better Gradle integration, proper logging, configuration cache support

---

### Phase 2: Enhanced Features (Medium Priority)

#### 2.1 Task Dependencies & Workflows
**Goal**: Automate common Git workflows

**New Tasks**:
- `gitReleaseTag` - Composite task that:
  1. Checks status (if enabled)
  2. Checks tag doesn't exist
  3. Creates tag
  4. Pushes tag (if enabled)

**Task Configuration**:
```kotlin
project.tasks.register("gitReleaseTag") {
    description = "Complete Git release workflow: check, create, and push tag"
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    
    dependsOn(gitCheckStatus, gitCheckTag, gitCreateTag, gitPushTag)
    
    // Configure task order
    gitCheckTag.mustRunAfter(gitCheckStatus)
    gitCreateTag.mustRunAfter(gitCheckTag)
    gitPushTag.mustRunAfter(gitCreateTag)
}
```

**Benefit**: One-command release workflow

---

#### 2.2 Additional Git Operations
**Goal**: Expand plugin functionality

**New Tasks**:

1. **`GitGetCurrentBranch`** - Get current branch name
   ```kotlin
   @get:OutputFile
   abstract val outputFile: RegularFileProperty
   ```

2. **`GitGetCommitHash`** - Get current/specific commit hash
   ```kotlin
   @get:Input
   abstract val shortHash: Property<Boolean>  // 7 chars vs full
   
   @get:OutputFile
   abstract val outputFile: RegularFileProperty
   ```

3. **`GitListTags`** - List all tags (useful for version bumping)
   ```kotlin
   @get:Input
   abstract val pattern: Property<String>  // e.g., "v*"
   
   @get:OutputFile
   abstract val outputFile: RegularFileProperty
   ```

4. **`GitDeleteTag`** - Delete local and/or remote tag
   ```kotlin
   @get:Input
   abstract val tagName: Property<String>
   
   @get:Input
   abstract val deleteRemote: Property<Boolean>
   ```

5. **`GitGetLastTag`** - Get most recent tag (for changelog)
   ```kotlin
   @get:OutputFile
   abstract val outputFile: RegularFileProperty
   ```

6. **`GitCommitCount`** - Get number of commits (for build numbers)
   ```kotlin
   @get:Input
   abstract val since: Property<String>  // commit/tag/branch
   
   @get:OutputFile
   abstract val outputFile: RegularFileProperty
   ```

**Benefit**: Cover more use cases for build automation

---

#### 2.3 Better Error Handling & Validation
**Goal**: Provide clear, actionable error messages

**Improvements**:
1. **Validate Git repository exists**:
   ```kotlin
   private fun validateGitRepo(project: Project) {
       val gitDir = project.rootDir.resolve(".git")
       if (!gitDir.exists()) {
           throw GradleException(
               "Not a Git repository. Initialize with 'git init' or check directory."
           )
       }
   }
   ```

2. **Handle common Git errors**:
   - No remote configured
   - Authentication failures
   - Merge conflicts
   - Detached HEAD state

3. **Add dry-run mode**:
   ```kotlin
   @get:Input
   abstract val dryRun: Property<Boolean>
   
   override fun exec() {
       if (dryRun.get()) {
           logger.lifecycle("DRY RUN: Would execute: ${commandLine.joinToString(" ")}")
           return
       }
       super.exec()
   }
   ```

**Benefit**: Better developer experience, easier troubleshooting

---

### Phase 3: Advanced Features (Low Priority)

#### 3.1 Git Hooks Integration
**Goal**: Automate Git hook installation

**New Tasks**:
- `gitInstallHooks` - Copy hooks from `gradle/git-hooks/` to `.git/hooks/`
- `gitValidateHooks` - Check hooks are installed

**Use Case**: Ensure team uses same pre-commit/pre-push hooks

---

#### 3.2 Changelog Generation
**Goal**: Generate changelog from Git history

**New Task**: `gitGenerateChangelog`
```kotlin
@get:Input
abstract val fromTag: Property<String>

@get:Input
abstract val toTag: Property<String>

@get:OutputFile
abstract val outputFile: RegularFileProperty

@get:Input
abstract val format: Property<ChangelogFormat>  // MARKDOWN, JSON, XML
```

**Benefit**: Automate release notes creation

---

#### 3.3 Semantic Versioning Support
**Goal**: Parse and bump semantic versions

**New Tasks**:
- `gitNextVersion` - Calculate next version based on commits
- `gitBumpVersion` - Update version and create tag

**Features**:
- Parse conventional commits (feat:, fix:, breaking:)
- Auto-determine major/minor/patch bump
- Update version files (gradle.properties, package.json, etc.)

---

#### 3.4 Remote Operations
**Goal**: Support multiple remotes and advanced operations

**New Tasks**:
- `gitFetch` - Fetch from remote
- `gitListRemotes` - List configured remotes
- `gitAddRemote` - Add new remote
- `gitPushBranch` - Push current branch

**Configuration**:
```kotlin
abstract class RemoteConfig {
    abstract val name: Property<String>
    abstract val url: Property<String>
    abstract val pushDefault: Property<Boolean>
}

abstract class GitToolExtension {
    abstract val remotes: ListProperty<RemoteConfig>
}
```

---

#### 3.5 Status Reporting
**Goal**: Generate Git status reports

**New Task**: `gitStatusReport`
- Generate HTML/JSON report with:
  - Current branch
  - Last commit info
  - Modified files count
  - Uncommitted changes
  - Remote sync status
  - Tag information

**Use Case**: CI/CD dashboards, build artifacts

---

### Phase 4: Documentation & Polish

#### 4.1 Add Documentation
**Files to create**:
1. **`git-tool/README.md`**:
   - Overview
   - Installation instructions
   - Task descriptions
   - Configuration examples
   - Common use cases
   - Troubleshooting

2. **`git-tool/CHANGELOG.md`**:
   - Version history
   - Breaking changes
   - New features

3. **Inline KDoc comments**:
   - Document all public classes
   - Document all properties
   - Add usage examples

#### 4.2 Add Sample Projects
**Directory**: `git-tool/samples/`
- `basic-usage/` - Simple example
- `custom-workflow/` - Advanced configuration
- `multi-module/` - Multi-project setup

#### 4.3 Performance Optimization
- Cache Git command results when appropriate
- Reduce number of Git invocations
- Parallel execution where possible

---

## 🏗️ Implementation Priority

### Must Have (v1.1.0)
1. ✅ Plugin extension with configuration
2. ✅ Comprehensive tests
3. ✅ Fix technical issues (logging, ByteArrayOutputStream)
4. ✅ Task dependencies
5. ✅ Basic documentation (README)

### Should Have (v1.2.0)
1. ✅ Additional Git operations (get branch, commit hash, list tags)
2. ✅ Better error handling
3. ✅ Git availability validation
4. ✅ Dry-run mode

### Nice to Have (v2.0.0)
1. ⭐ Semantic versioning support
2. ⭐ Changelog generation
3. ⭐ Advanced remote operations
4. ⭐ Git hooks integration

### Future Considerations (v3.0.0+)
1. 🚀 GitFlow workflow support
2. 🚀 GitHub/GitLab API integration
3. 🚀 PR/MR automation
4. 🚀 Code review helpers

---

## 📊 Success Metrics

After improvements, the plugin should:
- ✅ Have >80% test coverage
- ✅ Support configuration cache
- ✅ Have comprehensive documentation
- ✅ Handle errors gracefully
- ✅ Be configurable via DSL
- ✅ Follow Gradle best practices
- ✅ Support common Git workflows

---

## 🎬 Next Steps

**Immediate Actions**:
1. Create test structure and basic tests
2. Add GitToolExtension class
3. Fix ByteArrayOutputStream and logging issues
4. Update GitToolPlugin to use extension
5. Add task dependencies for release workflow
6. Write README.md

**Estimated Effort**:
- Phase 1: ~2-3 days
- Phase 2: ~3-4 days
- Phase 3: ~5-7 days
- Phase 4: ~1-2 days

**Total**: ~11-16 days for complete implementation
