# Changelog

All notable changes to the Git Statistic Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-18

### Added

#### Core Features
- **Git Status Report Task** (`gitStatusReport`)
  - Generate comprehensive Git repository status reports
  - Support for multiple output formats: JSON, HTML, Markdown
  - Collects repository, commit, working directory, tag, and remote information
  - Configurable inclusion of remote status and file changes
  - Machine-readable JSON for CI/CD integration
  - Human-readable HTML with styling
  - Documentation-friendly Markdown format

- **Git Dashboard Task** (`gitDashboard`)
  - Generate interactive HTML dashboard with visualizations
  - Commit history timeline
  - Contributor statistics with bar charts
  - File change heatmap showing most frequently changed files
  - Working directory changes visualization
  - Tag information display
  - Remote repository status
  - Responsive design with modern styling

#### Configuration Extension
- **GitStatisticExtension** with 7 configurable properties:
  - `reportFormat`: JSON, HTML, or MARKDOWN (default: JSON)
  - `includeRemoteStatus`: Include remote repository status (default: true)
  - `includeFileChanges`: Include file changes in reports (default: true)
  - `includeCommitGraph`: Show commit history in dashboard (default: true)
  - `includeContributorStats`: Show contributor statistics (default: true)
  - `commitHistoryDepth`: Number of commits to analyze (default: 50)
  - `includeFileHeatmap`: Show file change heatmap (default: true)

#### Data Models
- **GitStatusData**: Comprehensive repository status container
- **RepositoryInfo**: Branch and upstream information
- **CommitInfo**: Current commit details
- **WorkingDirectoryInfo**: File change tracking
- **TagInfo**: Tag information
- **RemoteInfo**: Remote repository details
- **CommitHistoryEntry**: Historical commit data

#### Use Cases
- CI/CD pipeline integration with JSON reports
- Build artifact documentation
- Development team dashboards
- Release notes generation
- Compliance and auditing
- Build provenance tracking

#### Documentation
- Comprehensive README with examples
- Usage documentation for all tasks
- Configuration examples
- Multiple use case scenarios
- API documentation

### Technical Details
- Configuration cache compatible
- Proper Gradle task outputs
- Efficient Git command execution
- Error handling with clear messages
- Follows Gradle best practices

## [Unreleased]

### Planned Features
- Chart.js integration for advanced visualizations
- Commit graph timeline with zoom
- Real-time auto-refresh capability
- Export to additional formats (PDF, CSV)
- Git blame integration
- Branch comparison views
- Custom report templates
- GitHub/GitLab API integration
