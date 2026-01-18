package lekanich.common.gradle.statistic

import java.time.Instant

/**
 * Utility class for collecting Git repository data.
 * Centralizes the logic for querying Git repository information.
 */
class GitDataCollector(private val executor: GitCommandExecutor) {

    /**
     * Collect complete Git status data.
     */
    fun collectGitStatus(includeRemoteStatus: Boolean = true, includeFileChanges: Boolean = true): GitStatusData {
        return GitStatusData(
            reportGeneratedAt = Instant.now(),
            repository = collectRepositoryInfo(),
            commit = collectCommitInfo(),
            workingDirectory = collectWorkingDirectoryInfo(includeFileChanges),
            tags = collectTagInfo(),
            remote = if (includeRemoteStatus) collectRemoteInfo() else null
        )
    }

    /**
     * Collect repository and branch information.
     */
    fun collectRepositoryInfo(): RepositoryInfo {
        val currentBranch = executor.execute("rev-parse", "--abbrev-ref", "HEAD")
        val upstreamBranch = executor.executeOrNull("rev-parse", "--abbrev-ref", "@{upstream}")

        val branchStatus = if (upstreamBranch != null) {
            val ahead = executor.execute("rev-list", "--count", "$upstreamBranch..HEAD").toIntOrNull() ?: 0
            val behind = executor.execute("rev-list", "--count", "HEAD..$upstreamBranch").toIntOrNull() ?: 0
            BranchStatus(ahead, behind)
        } else null

        return RepositoryInfo(currentBranch, upstreamBranch, branchStatus)
    }

    /**
     * Collect current commit information.
     */
    fun collectCommitInfo(): CommitInfo {
        val hash = executor.execute("rev-parse", "HEAD")
        val shortHash = executor.execute("rev-parse", "--short", "HEAD")
        val message = executor.execute("log", "-1", "--pretty=%s")
        val author = executor.execute("log", "-1", "--pretty=%an")
        val dateStr = executor.execute("log", "-1", "--pretty=%cI")
        val totalCount = executor.execute("rev-list", "--count", "HEAD").toIntOrNull() ?: 0

        return CommitInfo(
            hash = hash,
            shortHash = shortHash,
            message = message,
            author = author,
            date = Instant.parse(dateStr),
            totalCount = totalCount
        )
    }

    /**
     * Collect working directory status information.
     */
    fun collectWorkingDirectoryInfo(includeFileChanges: Boolean = true): WorkingDirectoryInfo {
        val statusOutput = executor.execute("status", "--porcelain")

        if (!includeFileChanges) {
            return WorkingDirectoryInfo(
                isClean = statusOutput.isEmpty(),
                modified = emptyList(),
                untracked = emptyList(),
                staged = emptyList(),
                conflicts = emptyList()
            )
        }

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

    /**
     * Collect tag information.
     */
    fun collectTagInfo(): TagInfo {
        val allTags = executor.execute("tag", "--sort=-version:refname").lines().filter { it.isNotBlank() }
        val latestTag = allTags.firstOrNull()
        val commitsSinceLatest = if (latestTag != null) {
            executor.execute("rev-list", "--count", "$latestTag..HEAD").toIntOrNull() ?: 0
        } else 0

        return TagInfo(latestTag, commitsSinceLatest, allTags)
    }

    /**
     * Collect remote repository information.
     */
    fun collectRemoteInfo(): RemoteInfo? {
        val remoteName = executor.executeOrNull("remote") ?: return null
        val remoteUrl = executor.executeOrNull("remote", "get-url", remoteName) ?: return null
        val reachable = try {
            executor.execute("ls-remote", "--exit-code", remoteName)
            true
        } catch (_: Exception) {
            false
        }

        return RemoteInfo(remoteName, remoteUrl, reachable)
    }

    /**
     * Collect commit history.
     */
    fun collectCommitHistory(depth: Int): List<CommitHistoryEntry> {
        val output = executor.execute("log", "-$depth", "--pretty=format:%H|%h|%an|%ae|%cI|%s")
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

    /**
     * Collect contributor statistics.
     */
    fun collectContributorStats(): Map<String, Int> {
        val output = executor.execute("shortlog", "-sn", "--all", "--no-merges")
        return output.lines()
            .filter { it.isNotBlank() }
            .associate { line ->
                val parts = line.trim().split("\t", limit = 2)
                parts[1] to parts[0].toInt()
            }
    }

    /**
     * Collect file change statistics.
     */
    fun collectFileChangeStats(limit: Int = 20): Map<String, Int> {
        val output = executor.execute("log", "--all", "--pretty=format:", "--name-only")
        return output.lines()
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
            .toMap()
    }
}

/**
 * Commit history entry for dashboard visualizations.
 */
data class CommitHistoryEntry(
    val hash: String,
    val shortHash: String,
    val author: String,
    val authorEmail: String,
    val date: Instant,
    val message: String
)
