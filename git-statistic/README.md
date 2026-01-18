# Git Statistic Plugin

A Gradle plugin for generating comprehensive Git statistics, status reports, and interactive dashboards.

## 🎯 Features

- **Status Reports**: Generate Git repository status in JSON, HTML, or Markdown format
- **Interactive Dashboards**: Visual HTML dashboards with commit history, contributor stats, and file change heatmaps
- **CI/CD Integration**: Machine-readable JSON output for pipeline decisions
- **Configurable**: Flexible DSL for customizing report content and format
- **Build Provenance**: Embed Git context in build artifacts

## 📦 Installation

### Using Gradle Plugin Portal

```kotlin
plugins {
    id("io.github.lekanich.git-statistic") version "1.0.0"
}
```

### Configuration

```kotlin
gitStatistic {
    // Report format: JSON, HTML, or MARKDOWN (default: JSON)
    reportFormat.set(ReportFormat.JSON)
    
    // Include remote repository status (default: true)
    includeRemoteStatus.set(true)
    
    // Include file changes in reports (default: true)
    includeFileChanges.set(true)
    
    // Dashboard configuration
    includeCommitGraph.set(true)       // Show commit history (default: true)
    includeContributorStats.set(true)  // Show contributor statistics (default: true)
    commitHistoryDepth.set(50)         // Number of commits to analyze (default: 50)
    includeFileHeatmap.set(true)       // Show most changed files (default: true)
}
```

## 📋 Tasks

### `gitStatusReport`

Generate a comprehensive Git status report.

**Usage:**
```bash
./gradlew gitStatusReport
```

**Output Location:** `build/reports/git-status/status.[json|html|md]`

**Example JSON Output:**
```json
{
  "reportGeneratedAt": "2026-01-18T10:30:00Z",
  "repository": {
    "currentBranch": "main",
    "upstreamBranch": "origin/main",
    "branchStatus": {
      "ahead": 2,
      "behind": 0
    }
  },
  "commit": {
    "hash": "a1b2c3d4e5f6...",
    "shortHash": "a1b2c3d",
    "message": "Add new feature",
    "author": "John Doe",
    "date": "2026-01-18T09:15:00Z",
    "totalCount": 147
  },
  "workingDirectory": {
    "isClean": false,
    "modified": ["src/Main.kt"],
    "untracked": ["docs/NEW.md"],
    "staged": [],
    "conflicts": []
  },
  "tags": {
    "latest": "v1.0.0",
    "commitsSinceLatest": 5,
    "all": ["v1.0.0", "v0.9.0"]
  },
  "remote": {
    "name": "origin",
    "url": "git@github.com:user/repo.git",
    "reachable": true
  }
}
```

### `gitDashboard`

Generate an interactive HTML dashboard with visualizations.

**Usage:**
```bash
./gradlew gitDashboard
```

**Output Location:** `build/reports/git-dashboard/index.html`

**Features:**
- Repository status overview
- Latest commit information
- Working directory changes
- Recent commit history
- Contributor statistics with bar charts
- Most changed files heatmap
- Tag information
- Remote repository status

## 🎨 Use Cases

### 1. CI/CD Integration

Use JSON reports for automated build decisions:

```yaml
# .github/workflows/build.yml
- name: Generate Git Report
  run: ./gradlew gitStatusReport

- name: Check if clean
  id: git
  run: |
    IS_CLEAN=$(jq -r '.workingDirectory.isClean' build/reports/git-status/status.json)
    echo "clean=$IS_CLEAN" >> $GITHUB_OUTPUT

- name: Deploy
  if: steps.git.outputs.clean == 'true'
  run: ./gradlew publish
```

### 2. Build Artifacts Documentation

Embed Git status in your JAR files:

```kotlin
tasks.register<Jar>("fatJar") {
    dependsOn("gitStatusReport")
    from(layout.buildDirectory.file("reports/git-status/status.json")) {
        into("META-INF/git")
    }
}
```

Now every JAR contains `META-INF/git/status.json` with build-time context.

### 3. Development Dashboards

Generate dashboards for team visibility:

```kotlin
tasks.register("refreshDashboard") {
    dependsOn("gitDashboard")
    doLast {
        println("Dashboard: ${layout.buildDirectory.file("reports/git-dashboard/index.html").get().asFile.absolutePath}")
    }
}
```

### 4. Release Documentation

Generate Markdown reports for release notes:

```kotlin
gitStatistic {
    reportFormat.set(ReportFormat.MARKDOWN)
}

tasks.register<Copy>("generateReleaseNotes") {
    dependsOn("gitStatusReport")
    from(layout.buildDirectory.file("reports/git-status/status.md"))
    into(projectDir)
    rename { "RELEASE_NOTES.md" }
}
```

### 5. Compliance & Auditing

Prove what code was built and when:

```kotlin
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named("gitStatusReport").map {
                layout.buildDirectory.file("reports/git-status/status.json").get()
            }) {
                classifier = "git-status"
                extension = "json"
            }
        }
    }
}
```

## 🔧 Advanced Configuration

### Custom Report Location

```kotlin
tasks.named<GitStatusReportTask>("gitStatusReport") {
    outputFile.set(layout.projectDirectory.file("git-status.json"))
}
```

### Custom Dashboard Location

```kotlin
tasks.named<GitDashboardTask>("gitDashboard") {
    outputDir.set(layout.projectDirectory.dir("dashboard"))
}
```

### Multiple Report Formats

```kotlin
tasks.register<GitStatusReportTask>("gitStatusReportJson") {
    format.set(ReportFormat.JSON)
    outputFile.set(layout.buildDirectory.file("reports/status.json"))
}

tasks.register<GitStatusReportTask>("gitStatusReportHtml") {
    format.set(ReportFormat.HTML)
    outputFile.set(layout.buildDirectory.file("reports/status.html"))
}

tasks.register<GitStatusReportTask>("gitStatusReportMarkdown") {
    format.set(ReportFormat.MARKDOWN)
    outputFile.set(layout.buildDirectory.file("reports/status.md"))
}
```

## 📊 Report Data Structure

The plugin collects the following Git information:

- **Repository**: Current branch, upstream branch, ahead/behind status
- **Commit**: Hash, message, author, date, total commit count
- **Working Directory**: Clean status, modified/untracked/staged/conflicted files
- **Tags**: Latest tag, commits since latest, all tags
- **Remote**: Remote name, URL, reachability
- **History**: Commit timeline (dashboard only)
- **Contributors**: Commit counts by author (dashboard only)
- **File Changes**: Most frequently changed files (dashboard only)

## 🚀 Performance

- **Fast**: Uses efficient Git commands
- **Lightweight**: No external dependencies
- **Configurable**: Disable expensive operations when not needed
- **Cacheable**: Task outputs are properly cached

## 🛠️ Requirements

- Git installed and available in PATH
- Gradle 8.0 or higher
- JDK 17 or higher