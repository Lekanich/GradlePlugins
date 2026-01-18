package lekanich.common.gradle.statistic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Task that generates a comprehensive Git status report.
 *
 * Supports multiple output formats:
 * - JSON: Machine-readable format for CI/CD integration
 * - HTML: Human-readable format with styling
 * - Markdown: Documentation-friendly format
 */
abstract class GitStatusReportTask : DefaultTask() {

    @get:Input
    abstract val format: Property<ReportFormat>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val includeRemoteStatus: Property<Boolean>

    @get:Input
    abstract val includeFileChanges: Property<Boolean>

    init {
        format.convention(ReportFormat.JSON)
        includeRemoteStatus.convention(true)
        includeFileChanges.convention(true)
    }

    @TaskAction
    fun generateReport() {
        logger.lifecycle("Generating Git status report in ${format.get()} format...")

        val statusData = collectGitStatus()
        val output = outputFile.get().asFile

        output.parentFile.mkdirs()

        when (format.get()) {
            ReportFormat.JSON -> writeJsonReport(statusData, output)
            ReportFormat.HTML -> writeHtmlReport(statusData, output)
            ReportFormat.MARKDOWN -> writeMarkdownReport(statusData, output)
        }

        logger.lifecycle("Git status report generated: ${output.absolutePath}")
    }

    private fun collectGitStatus(): GitStatusData {
        return GitStatusData(
            reportGeneratedAt = Instant.now(),
            repository = collectRepositoryInfo(),
            commit = collectCommitInfo(),
            workingDirectory = collectWorkingDirectoryInfo(),
            tags = collectTagInfo(),
            remote = if (includeRemoteStatus.get()) collectRemoteInfo() else null
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
        if (!includeFileChanges.get()) {
            val isClean = executeGit("status", "--porcelain").isEmpty()
            return WorkingDirectoryInfo(isClean, emptyList(), emptyList(), emptyList(), emptyList())
        }

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
        } catch (_: Exception) {
            false
        }

        return RemoteInfo(remoteName, remoteUrl, reachable)
    }

    private fun writeJsonReport(data: GitStatusData, output: java.io.File) {
        val json = buildString {
            appendLine("{")
            appendLine("  \"reportGeneratedAt\": \"${data.reportGeneratedAt}\",")
            appendLine("  \"repository\": {")
            appendLine("    \"currentBranch\": \"${data.repository.currentBranch}\",")
            appendLine("    \"upstreamBranch\": ${data.repository.upstreamBranch?.let { "\"$it\"" } ?: "null"},")
            if (data.repository.branchStatus != null) {
                appendLine("    \"branchStatus\": {")
                appendLine("      \"ahead\": ${data.repository.branchStatus.ahead},")
                appendLine("      \"behind\": ${data.repository.branchStatus.behind}")
                appendLine("    }")
            } else {
                appendLine("    \"branchStatus\": null")
            }
            appendLine("  },")
            appendLine("  \"commit\": {")
            appendLine("    \"hash\": \"${data.commit.hash}\",")
            appendLine("    \"shortHash\": \"${data.commit.shortHash}\",")
            appendLine("    \"message\": \"${data.commit.message.replace("\"", "\\\"")}\",")
            appendLine("    \"author\": \"${data.commit.author}\",")
            appendLine("    \"date\": \"${data.commit.date}\",")
            appendLine("    \"totalCount\": ${data.commit.totalCount}")
            appendLine("  },")
            appendLine("  \"workingDirectory\": {")
            appendLine("    \"isClean\": ${data.workingDirectory.isClean},")
            appendLine("    \"modified\": ${data.workingDirectory.modified.toJsonArray()},")
            appendLine("    \"untracked\": ${data.workingDirectory.untracked.toJsonArray()},")
            appendLine("    \"staged\": ${data.workingDirectory.staged.toJsonArray()},")
            appendLine("    \"conflicts\": ${data.workingDirectory.conflicts.toJsonArray()}")
            appendLine("  },")
            appendLine("  \"tags\": {")
            appendLine("    \"latest\": ${data.tags.latest?.let { "\"$it\"" } ?: "null"},")
            appendLine("    \"commitsSinceLatest\": ${data.tags.commitsSinceLatest},")
            appendLine("    \"all\": ${data.tags.all.toJsonArray()}")
            appendLine("  }${if (data.remote != null) "," else ""}")
            if (data.remote != null) {
                appendLine("  \"remote\": {")
                appendLine("    \"name\": \"${data.remote.name}\",")
                appendLine("    \"url\": \"${data.remote.url}\",")
                appendLine("    \"reachable\": ${data.remote.reachable}")
                appendLine("  }")
            }
            appendLine("}")
        }
        output.writeText(json)
    }

    private fun writeHtmlReport(data: GitStatusData, output: java.io.File) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Git Status Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
                    .container { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }
                    h2 { color: #555; margin-top: 30px; }
                    table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                    td { padding: 10px; border-bottom: 1px solid #ddd; }
                    td:first-child { font-weight: bold; width: 200px; color: #666; }
                    .clean { color: #4CAF50; }
                    .dirty { color: #f44336; }
                    .badge { display: inline-block; padding: 2px 8px; border-radius: 3px; font-size: 12px; }
                    .badge.success { background: #4CAF50; color: white; }
                    .badge.warning { background: #ff9800; color: white; }
                    .badge.error { background: #f44336; color: white; }
                    .file-list { background: #f9f9f9; padding: 10px; border-radius: 4px; margin: 10px 0; }
                    .file-list ul { margin: 5px 0; padding-left: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Git Status Report</h1>
                    <p><strong>Generated:</strong> ${formatDateTime(data.reportGeneratedAt)}</p>
                    
                    <h2>Repository</h2>
                    <table>
                        <tr>
                            <td>Current Branch</td>
                            <td>${data.repository.currentBranch}</td>
                        </tr>
                        <tr>
                            <td>Upstream Branch</td>
                            <td>${data.repository.upstreamBranch ?: "N/A"}</td>
                        </tr>
                        ${if (data.repository.branchStatus != null) """
                        <tr>
                            <td>Branch Status</td>
                            <td>↑ ${data.repository.branchStatus.ahead} ahead, ↓ ${data.repository.branchStatus.behind} behind</td>
                        </tr>
                        """ else ""}
                    </table>
                    
                    <h2>Latest Commit</h2>
                    <table>
                        <tr>
                            <td>Hash</td>
                            <td>${data.commit.shortHash}</td>
                        </tr>
                        <tr>
                            <td>Message</td>
                            <td>${data.commit.message}</td>
                        </tr>
                        <tr>
                            <td>Author</td>
                            <td>${data.commit.author}</td>
                        </tr>
                        <tr>
                            <td>Date</td>
                            <td>${formatDateTime(data.commit.date)}</td>
                        </tr>
                        <tr>
                            <td>Total Commits</td>
                            <td>${data.commit.totalCount}</td>
                        </tr>
                    </table>
                    
                    <h2>Working Directory</h2>
                    <table>
                        <tr>
                            <td>Status</td>
                            <td class="${if (data.workingDirectory.isClean) "clean" else "dirty"}">
                                ${if (data.workingDirectory.isClean) "✓ Clean" else "⚠ Dirty"}
                            </td>
                        </tr>
                        ${if (!data.workingDirectory.isClean) """
                        <tr>
                            <td colspan="2">
                                <div class="file-list">
                                    ${if (data.workingDirectory.conflicts.isNotEmpty()) """
                                    <strong>Conflicts (${data.workingDirectory.conflicts.size}):</strong>
                                    <ul>${data.workingDirectory.conflicts.joinToString("") { "<li>$it</li>" }}</ul>
                                    """ else ""}
                                    ${if (data.workingDirectory.staged.isNotEmpty()) """
                                    <strong>Staged (${data.workingDirectory.staged.size}):</strong>
                                    <ul>${data.workingDirectory.staged.joinToString("") { "<li>$it</li>" }}</ul>
                                    """ else ""}
                                    ${if (data.workingDirectory.modified.isNotEmpty()) """
                                    <strong>Modified (${data.workingDirectory.modified.size}):</strong>
                                    <ul>${data.workingDirectory.modified.joinToString("") { "<li>$it</li>" }}</ul>
                                    """ else ""}
                                    ${if (data.workingDirectory.untracked.isNotEmpty()) """
                                    <strong>Untracked (${data.workingDirectory.untracked.size}):</strong>
                                    <ul>${data.workingDirectory.untracked.joinToString("") { "<li>$it</li>" }}</ul>
                                    """ else ""}
                                </div>
                            </td>
                        </tr>
                        """ else ""}
                    </table>
                    
                    <h2>Tags</h2>
                    <table>
                        <tr>
                            <td>Latest Tag</td>
                            <td>${data.tags.latest ?: "None"}</td>
                        </tr>
                        <tr>
                            <td>Commits Since Latest</td>
                            <td>${data.tags.commitsSinceLatest}</td>
                        </tr>
                        <tr>
                            <td>Total Tags</td>
                            <td>${data.tags.all.size}</td>
                        </tr>
                    </table>
                    
                    ${if (data.remote != null) """
                    <h2>Remote</h2>
                    <table>
                        <tr>
                            <td>Name</td>
                            <td>${data.remote.name}</td>
                        </tr>
                        <tr>
                            <td>URL</td>
                            <td>${data.remote.url}</td>
                        </tr>
                        <tr>
                            <td>Reachable</td>
                            <td class="${if (data.remote.reachable) "clean" else "dirty"}">
                                ${if (data.remote.reachable) "✓ Yes" else "✗ No"}
                            </td>
                        </tr>
                    </table>
                    """ else ""}
                </div>
            </body>
            </html>
        """.trimIndent()
        output.writeText(html)
    }

    private fun writeMarkdownReport(data: GitStatusData, output: java.io.File) {
        val markdown = buildString {
            appendLine("# Git Status Report")
            appendLine()
            appendLine("**Generated:** ${formatDateTime(data.reportGeneratedAt)}")
            appendLine()
            appendLine("## Repository")
            appendLine()
            appendLine("- **Current Branch:** ${data.repository.currentBranch}")
            appendLine("- **Upstream Branch:** ${data.repository.upstreamBranch ?: "N/A"}")
            if (data.repository.branchStatus != null) {
                appendLine("- **Branch Status:** ↑ ${data.repository.branchStatus.ahead} ahead, ↓ ${data.repository.branchStatus.behind} behind")
            }
            appendLine()
            appendLine("## Latest Commit")
            appendLine()
            appendLine("- **Hash:** `${data.commit.shortHash}`")
            appendLine("- **Message:** ${data.commit.message}")
            appendLine("- **Author:** ${data.commit.author}")
            appendLine("- **Date:** ${formatDateTime(data.commit.date)}")
            appendLine("- **Total Commits:** ${data.commit.totalCount}")
            appendLine()
            appendLine("## Working Directory")
            appendLine()
            appendLine("- **Status:** ${if (data.workingDirectory.isClean) "✓ Clean" else "⚠ Dirty"}")
            if (!data.workingDirectory.isClean) {
                if (data.workingDirectory.conflicts.isNotEmpty()) {
                    appendLine("- **Conflicts:** ${data.workingDirectory.conflicts.size}")
                    data.workingDirectory.conflicts.forEach { appendLine("  - $it") }
                }
                if (data.workingDirectory.staged.isNotEmpty()) {
                    appendLine("- **Staged:** ${data.workingDirectory.staged.size}")
                    data.workingDirectory.staged.forEach { appendLine("  - $it") }
                }
                if (data.workingDirectory.modified.isNotEmpty()) {
                    appendLine("- **Modified:** ${data.workingDirectory.modified.size}")
                    data.workingDirectory.modified.forEach { appendLine("  - $it") }
                }
                if (data.workingDirectory.untracked.isNotEmpty()) {
                    appendLine("- **Untracked:** ${data.workingDirectory.untracked.size}")
                    data.workingDirectory.untracked.forEach { appendLine("  - $it") }
                }
            }
            appendLine()
            appendLine("## Tags")
            appendLine()
            appendLine("- **Latest Tag:** ${data.tags.latest ?: "None"}")
            appendLine("- **Commits Since Latest:** ${data.tags.commitsSinceLatest}")
            appendLine("- **Total Tags:** ${data.tags.all.size}")
            if (data.remote != null) {
                appendLine()
                appendLine("## Remote")
                appendLine()
                appendLine("- **Name:** ${data.remote.name}")
                appendLine("- **URL:** ${data.remote.url}")
                appendLine("- **Reachable:** ${if (data.remote.reachable) "✓ Yes" else "✗ No"}")
            }
        }
        output.writeText(markdown)
    }

    private fun executeGit(vararg args: String): String {
        val processBuilder = ProcessBuilder("git", *args)
            .directory(project.projectDir)
            .redirectErrorStream(false)

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            logger.error("Git command failed: git ${args.joinToString(" ")}")
            if (errorOutput.isNotEmpty()) {
                logger.error("Error output: $errorOutput")
            }
            throw GradleException("Git command failed: git ${args.joinToString(" ")}")
        }

        return output.trim()
    }

    private fun executeGitOrNull(vararg args: String): String? {
        return try {
            executeGit(*args).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun List<String>.toJsonArray(): String {
        return "[" + joinToString(", ") { "\"$it\"" } + "]"
    }

    private fun formatDateTime(instant: Instant): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }
}
