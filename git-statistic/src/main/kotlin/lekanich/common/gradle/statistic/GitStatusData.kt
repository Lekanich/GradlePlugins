package lekanich.common.gradle.statistic

import java.time.Instant

/**
 * Comprehensive Git repository status data.
 */
data class GitStatusData(
    val reportGeneratedAt: Instant,
    val repository: RepositoryInfo,
    val commit: CommitInfo,
    val workingDirectory: WorkingDirectoryInfo,
    val tags: TagInfo,
    val remote: RemoteInfo?
)

/**
 * Repository and branch information.
 */
data class RepositoryInfo(
    val currentBranch: String,
    val upstreamBranch: String?,
    val branchStatus: BranchStatus?
)

/**
 * Branch status relative to upstream.
 */
data class BranchStatus(
    val ahead: Int,
    val behind: Int
)

/**
 * Current commit information.
 */
data class CommitInfo(
    val hash: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val date: Instant,
    val totalCount: Int
)

/**
 * Working directory status.
 */
data class WorkingDirectoryInfo(
    val isClean: Boolean,
    val modified: List<String>,
    val untracked: List<String>,
    val staged: List<String>,
    val conflicts: List<String>
)

/**
 * Tag information.
 */
data class TagInfo(
    val latest: String?,
    val commitsSinceLatest: Int,
    val all: List<String>
)

/**
 * Remote repository information.
 */
data class RemoteInfo(
    val name: String,
    val url: String,
    val reachable: Boolean
)
