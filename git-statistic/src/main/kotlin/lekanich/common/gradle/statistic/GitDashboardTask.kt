package lekanich.common.gradle.statistic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

        val statusData = collectGitStatus()
        val commitHistory = if (includeCommitGraph.get()) collectCommitHistory() else emptyList()
        val contributorStats = if (includeContributorStats.get()) collectContributorStats() else emptyMap()
        val fileChanges = if (includeFileHeatmap.get()) collectFileChangeStats() else emptyMap()

        val dashboardFile = outputDirectory.resolve("index.html")
        writeDashboard(dashboardFile, statusData, commitHistory, contributorStats, fileChanges)

        logger.lifecycle("Git dashboard generated: ${dashboardFile.absolutePath}")
    }

    private fun collectGitStatus(): GitStatusData {
        return GitStatusData(
            reportGeneratedAt = Instant.now(),
            repository = collectRepositoryInfo(),
            commit = collectCommitInfo(),
            workingDirectory = collectWorkingDirectoryInfo(),
            tags = collectTagInfo(),
            remote = collectRemoteInfo()
        )
    }

    private fun collectRepositoryInfo(): RepositoryInfo {
        val currentBranch = executeGit("rev-parse", "--abbrev-ref", "HEAD")
        val upstreamBranch = executeGitOrNull("rev-parse", "--abbrev-ref", "@{upstream}")

        val branchStatus = if (upstreamBranch != null) {
            val ahead = executeGit("rev-list", "--count", "$upstreamBranch..HEAD").toIntOrNull() ?: 0
            val behind = executeGit("rev-list", "--count", "HEAD..$upstreamBranch").toIntOrNull() ?: 0
            BranchStatus(ahead, behind)
        } else null

        return RepositoryInfo(currentBranch, upstreamBranch, branchStatus)
    }

    private fun collectCommitInfo(): CommitInfo {
        val hash = executeGit("rev-parse", "HEAD")
        val shortHash = executeGit("rev-parse", "--short", "HEAD")
        val message = executeGit("log", "-1", "--pretty=%s")
        val author = executeGit("log", "-1", "--pretty=%an")
        val dateStr = executeGit("log", "-1", "--pretty=%cI")
        val totalCount = executeGit("rev-list", "--count", "HEAD").toIntOrNull() ?: 0

        return CommitInfo(
            hash = hash,
            shortHash = shortHash,
            message = message,
            author = author,
            date = Instant.parse(dateStr),
            totalCount = totalCount
        )
    }

    private fun collectWorkingDirectoryInfo(): WorkingDirectoryInfo {
        val statusOutput = executeGit("status", "--porcelain")
        val modified = mutableListOf<String>()
        val untracked = mutableListOf<String>()
        val staged = mutableListOf<String>()
        val conflicts = mutableListOf<String>()

        statusOutput.lines().forEach { line ->
            if (line.isBlank()) return@forEach
            val status = line.substring(0, 2)
            val file = line.substring(3)

            when {
                status.contains("U") || status == "DD" || status == "AA" -> conflicts.add(file)
                status[0] != ' ' && status[0] != '?' -> staged.add(file)
                status == "??" -> untracked.add(file)
                status[1] == 'M' || status[1] == 'D' -> modified.add(file)
            }
        }

        return WorkingDirectoryInfo(
            isClean = statusOutput.isEmpty(),
            modified = modified,
            untracked = untracked,
            staged = staged,
            conflicts = conflicts
        )
    }

    private fun collectTagInfo(): TagInfo {
        val allTags = executeGit("tag", "--sort=-version:refname").lines().filter { it.isNotBlank() }
        val latestTag = allTags.firstOrNull()
        val commitsSinceLatest = if (latestTag != null) {
            executeGit("rev-list", "--count", "$latestTag..HEAD").toIntOrNull() ?: 0
        } else 0

        return TagInfo(latestTag, commitsSinceLatest, allTags)
    }

    private fun collectRemoteInfo(): RemoteInfo? {
        val remoteName = executeGitOrNull("remote") ?: return null
        val remoteUrl = executeGitOrNull("remote", "get-url", remoteName) ?: return null
        val reachable = try {
            executeGit("ls-remote", "--exit-code", remoteName)
            true
        } catch (e: Exception) {
            false
        }

        return RemoteInfo(remoteName, remoteUrl, reachable)
    }

    private fun collectCommitHistory(): List<CommitHistoryEntry> {
        val depth = commitHistoryDepth.get()
        val output = executeGit("log", "-$depth", "--pretty=format:%H|%h|%an|%ae|%cI|%s")
        return output.lines().filter { it.isNotBlank() }.map { line ->
            val parts = line.split("|")
            CommitHistoryEntry(
                hash = parts[0],
                shortHash = parts[1],
                author = parts[2],
                authorEmail = parts[3],
                date = Instant.parse(parts[4]),
                message = parts[5]
            )
        }
    }

    private fun collectContributorStats(): Map<String, Int> {
        val output = executeGit("shortlog", "-sn", "--all", "--no-merges")
        return output.lines()
            .filter { it.isNotBlank() }
            .associate { line ->
                val parts = line.trim().split("\t", limit = 2)
                parts[1] to parts[0].toInt()
            }
    }

    private fun collectFileChangeStats(): Map<String, Int> {
        val output = executeGit("log", "--all", "--pretty=format:", "--name-only")
        return output.lines()
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(20)
            .toMap()
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
                        Generated: ${formatDateTime(status.reportGeneratedAt)}
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
                                <span class="stat-value">${formatDateTime(status.commit.date)}</span>
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
                                <div class="commit-meta">${commit.author} • ${formatDateTime(commit.date)}</div>
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
                    Generated by Git Statistic Plugin • ${formatDateTime(status.reportGeneratedAt)}
                </div>
            </body>
            </html>
        """.trimIndent()
        file.writeText(html)
    }

    private fun executeGit(vararg args: String): String {
        val output = ByteArrayOutputStream()
        val result = project.exec { spec ->
            spec.commandLine("git", *args)
            spec.standardOutput = output
            spec.errorOutput = ByteArrayOutputStream()
            spec.isIgnoreExitValue = true
        }

        if (result.exitValue != 0) {
            throw GradleException("Git command failed: git ${args.joinToString(" ")}")
        }

        return output.toString(Charsets.UTF_8).trim()
    }

    private fun executeGitOrNull(vararg args: String): String? {
        return try {
            executeGit(*args).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDateTime(instant: Instant): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}

data class CommitHistoryEntry(
    val hash: String,
    val shortHash: String,
    val author: String,
    val authorEmail: String,
    val date: Instant,
    val message: String
)
