package lekanich.common.gradle.statistic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Task that generates an interactive HTML dashboard with Git statistics and visualizations.
 */
abstract class GitDashboardTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val includeCommitGraph: Property<Boolean>

    @get:Input
    abstract val includeContributorStats: Property<Boolean>

    @get:Input
    abstract val commitHistoryDepth: Property<Int>

    @get:Input
    abstract val includeFileHeatmap: Property<Boolean>

    init {
        includeCommitGraph.convention(true)
        includeContributorStats.convention(true)
        commitHistoryDepth.convention(50)
        includeFileHeatmap.convention(true)
    }

    @TaskAction
    fun generateDashboard() {
        logger.lifecycle("Generating Git dashboard...")

        val outputDirectory = outputDir.get().asFile
        outputDirectory.mkdirs()

        val executor = GitCommandExecutor(project.projectDir, logger)
        val collector = GitDataCollector(executor)

        val statusData = collector.collectGitStatus()
        val commitHistory = if (includeCommitGraph.get()) collector.collectCommitHistory(commitHistoryDepth.get()) else emptyList()
        val contributorStats = if (includeContributorStats.get()) collector.collectContributorStats() else emptyMap()
        val fileChanges = if (includeFileHeatmap.get()) collector.collectFileChangeStats() else emptyMap()

        val dashboardFile = outputDirectory.resolve("index.html")
        writeDashboard(dashboardFile, statusData, commitHistory, contributorStats, fileChanges)

        logger.lifecycle("Git dashboard generated: ${dashboardFile.absolutePath}")
    }


    private fun writeDashboard(
        file: java.io.File,
        status: GitStatusData,
        commits: List<CommitHistoryEntry>,
        contributors: Map<String, Int>,
        fileChanges: Map<String, Int>
    ) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Git Dashboard - ${status.repository.currentBranch}</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; background: #f5f5f5; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header h1 { font-size: 32px; margin-bottom: 10px; }
                    .header .meta { opacity: 0.9; font-size: 14px; }
                    .container { max-width: 1400px; margin: 0 auto; padding: 30px; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-bottom: 20px; }
                    .card { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .card h2 { font-size: 18px; color: #333; margin-bottom: 15px; border-bottom: 2px solid #667eea; padding-bottom: 10px; }
                    .stat { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #eee; }
                    .stat:last-child { border-bottom: none; }
                    .stat-label { color: #666; font-weight: 500; }
                    .stat-value { color: #333; font-weight: bold; }
                    .status-badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; }
                    .status-clean { background: #10b981; color: white; }
                    .status-dirty { background: #ef4444; color: white; }
                    .commit-list { max-height: 400px; overflow-y: auto; }
                    .commit-item { padding: 12px; border-bottom: 1px solid #eee; }
                    .commit-item:hover { background: #f9f9f9; }
                    .commit-hash { font-family: monospace; color: #667eea; font-size: 12px; }
                    .commit-message { margin: 5px 0; color: #333; }
                    .commit-meta { font-size: 12px; color: #666; }
                    .bar-chart { margin-top: 10px; }
                    .bar-item { margin-bottom: 12px; }
                    .bar-label { font-size: 12px; color: #666; margin-bottom: 4px; display: flex; justify-content: space-between; }
                    .bar-bg { background: #e5e7eb; height: 24px; border-radius: 4px; overflow: hidden; }
                    .bar-fill { background: linear-gradient(90deg, #667eea 0%, #764ba2 100%); height: 100%; transition: width 0.3s ease; display: flex; align-items: center; padding: 0 8px; color: white; font-size: 11px; font-weight: 600; }
                    .file-list { max-height: 300px; overflow-y: auto; }
                    .file-item { padding: 8px; border-bottom: 1px solid #eee; font-size: 13px; font-family: monospace; }
                    .tag-list { display: flex; flex-wrap: wrap; gap: 8px; }
                    .tag-badge { background: #f3f4f6; padding: 6px 12px; border-radius: 6px; font-size: 12px; color: #374151; border: 1px solid #e5e7eb; }
                    .footer { text-align: center; color: #666; padding: 20px; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>📊 Git Dashboard</h1>
                    <div class="meta">
                        <strong>${status.repository.currentBranch}</strong> • 
                        Generated: ${DateTimeUtils.formatDateTime(status.reportGeneratedAt)}
                    </div>
                </div>
                
                <div class="container">
                    <div class="grid">
                        <div class="card">
                            <h2>📍 Repository Status</h2>
                            <div class="stat">
                                <span class="stat-label">Branch</span>
                                <span class="stat-value">${status.repository.currentBranch}</span>
                            </div>
                            ${if (status.repository.branchStatus != null) """
                            <div class="stat">
                                <span class="stat-label">Ahead / Behind</span>
                                <span class="stat-value">↑ ${status.repository.branchStatus.ahead} / ↓ ${status.repository.branchStatus.behind}</span>
                            </div>
                            """ else ""}
                            <div class="stat">
                                <span class="stat-label">Working Directory</span>
                                <span class="stat-value">
                                    <span class="status-badge ${if (status.workingDirectory.isClean) "status-clean" else "status-dirty"}">
                                        ${if (status.workingDirectory.isClean) "✓ Clean" else "⚠ Dirty"}
                                    </span>
                                </span>
                            </div>
                            <div class="stat">
                                <span class="stat-label">Total Commits</span>
                                <span class="stat-value">${status.commit.totalCount}</span>
                            </div>
                        </div>
                        
                        <div class="card">
                            <h2>💾 Latest Commit</h2>
                            <div class="stat">
                                <span class="stat-label">Hash</span>
                                <span class="stat-value commit-hash">${status.commit.shortHash}</span>
                            </div>
                            <div class="stat">
                                <span class="stat-label">Author</span>
                                <span class="stat-value">${status.commit.author}</span>
                            </div>
                            <div class="stat">
                                <span class="stat-label">Date</span>
                                <span class="stat-value">${DateTimeUtils.formatDateTime(status.commit.date)}</span>
                            </div>
                            <div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee;">
                                <div style="color: #333; font-size: 14px;">${status.commit.message}</div>
                            </div>
                        </div>
                        
                        <div class="card">
                            <h2>🏷️ Tags</h2>
                            <div class="stat">
                                <span class="stat-label">Latest Tag</span>
                                <span class="stat-value">${status.tags.latest ?: "None"}</span>
                            </div>
                            <div class="stat">
                                <span class="stat-label">Commits Since</span>
                                <span class="stat-value">${status.tags.commitsSinceLatest}</span>
                            </div>
                            <div class="stat">
                                <span class="stat-label">Total Tags</span>
                                <span class="stat-value">${status.tags.all.size}</span>
                            </div>
                            ${if (status.tags.all.isNotEmpty()) """
                            <div style="margin-top: 10px;">
                                <div class="tag-list">
                                    ${status.tags.all.take(10).joinToString("") { "<span class=\"tag-badge\">$it</span>" }}
                                </div>
                            </div>
                            """ else ""}
                        </div>
                    </div>
                    
                    ${if (!status.workingDirectory.isClean) """
                    <div class="card" style="margin-bottom: 20px;">
                        <h2>⚠️ Working Directory Changes</h2>
                        <div class="grid" style="margin-top: 15px;">
                            ${if (status.workingDirectory.conflicts.isNotEmpty()) """
                            <div>
                                <strong style="color: #ef4444;">Conflicts (${status.workingDirectory.conflicts.size})</strong>
                                <div class="file-list">
                                    ${status.workingDirectory.conflicts.joinToString("") { "<div class=\"file-item\">$it</div>" }}
                                </div>
                            </div>
                            """ else ""}
                            ${if (status.workingDirectory.staged.isNotEmpty()) """
                            <div>
                                <strong style="color: #10b981;">Staged (${status.workingDirectory.staged.size})</strong>
                                <div class="file-list">
                                    ${status.workingDirectory.staged.joinToString("") { "<div class=\"file-item\">$it</div>" }}
                                </div>
                            </div>
                            """ else ""}
                            ${if (status.workingDirectory.modified.isNotEmpty()) """
                            <div>
                                <strong style="color: #f59e0b;">Modified (${status.workingDirectory.modified.size})</strong>
                                <div class="file-list">
                                    ${status.workingDirectory.modified.joinToString("") { "<div class=\"file-item\">$it</div>" }}
                                </div>
                            </div>
                            """ else ""}
                            ${if (status.workingDirectory.untracked.isNotEmpty()) """
                            <div>
                                <strong style="color: #6b7280;">Untracked (${status.workingDirectory.untracked.size})</strong>
                                <div class="file-list">
                                    ${status.workingDirectory.untracked.joinToString("") { "<div class=\"file-item\">$it</div>" }}
                                </div>
                            </div>
                            """ else ""}
                        </div>
                    </div>
                    """ else ""}
                    
                    ${if (commits.isNotEmpty()) """
                    <div class="card" style="margin-bottom: 20px;">
                        <h2>📝 Recent Commits (${commits.size})</h2>
                        <div class="commit-list">
                            ${commits.joinToString("") { commit -> """
                            <div class="commit-item">
                                <div class="commit-hash">${commit.shortHash}</div>
                                <div class="commit-message">${commit.message}</div>
                                <div class="commit-meta">${commit.author} • ${DateTimeUtils.formatDateTime(commit.date)}</div>
                            </div>
                            """ }}
                        </div>
                    </div>
                    """ else ""}
                    
                    <div class="grid">
                        ${if (contributors.isNotEmpty()) """
                        <div class="card">
                            <h2>👥 Contributors</h2>
                            <div class="bar-chart">
                                ${contributors.entries.take(10).map { (name, count) ->
                                    val maxCount = contributors.values.maxOrNull() ?: 1
                                    val percentage = (count.toDouble() / maxCount * 100).toInt()
                                    """
                                    <div class="bar-item">
                                        <div class="bar-label">
                                            <span>$name</span>
                                            <span>$count commits</span>
                                        </div>
                                        <div class="bar-bg">
                                            <div class="bar-fill" style="width: $percentage%">
                                                ${if (percentage > 20) "$count" else ""}
                                            </div>
                                        </div>
                                    </div>
                                    """
                                }.joinToString("")}
                            </div>
                        </div>
                        """ else ""}
                        
                        ${if (fileChanges.isNotEmpty()) """
                        <div class="card">
                            <h2>🔥 Most Changed Files</h2>
                            <div class="bar-chart">
                                ${fileChanges.entries.take(10).map { (file, count) ->
                                    val maxCount = fileChanges.values.maxOrNull() ?: 1
                                    val percentage = (count.toDouble() / maxCount * 100).toInt()
                                    val fileName = file.split("/").lastOrNull() ?: file
                                    """
                                    <div class="bar-item">
                                        <div class="bar-label">
                                            <span title="$file">$fileName</span>
                                            <span>$count changes</span>
                                        </div>
                                        <div class="bar-bg">
                                            <div class="bar-fill" style="width: $percentage%">
                                                ${if (percentage > 20) "$count" else ""}
                                            </div>
                                        </div>
                                    </div>
                                    """
                                }.joinToString("")}
                            </div>
                        </div>
                        """ else ""}
                    </div>
                    
                    ${if (status.remote != null) """
                    <div class="card" style="margin-top: 20px;">
                        <h2>🌐 Remote Repository</h2>
                        <div class="stat">
                            <span class="stat-label">Name</span>
                            <span class="stat-value">${status.remote.name}</span>
                        </div>
                        <div class="stat">
                            <span class="stat-label">URL</span>
                            <span class="stat-value" style="font-size: 12px; word-break: break-all;">${status.remote.url}</span>
                        </div>
                        <div class="stat">
                            <span class="stat-label">Reachable</span>
                            <span class="stat-value">
                                <span class="status-badge ${if (status.remote.reachable) "status-clean" else "status-dirty"}">
                                    ${if (status.remote.reachable) "✓ Yes" else "✗ No"}
                                </span>
                            </span>
                        </div>
                    </div>
                    """ else ""}
                </div>
                
                <div class="footer">
                    Generated by Git Statistic Plugin • ${DateTimeUtils.formatDateTime(status.reportGeneratedAt)}
                </div>
            </body>
            </html>
        """.trimIndent()
        file.writeText(html)
    }
}

