package lekanich.common.gradle.statistic

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * Extension for configuring Git Statistic plugin.
 *
 * Example usage:
 * ```kotlin
 * gitStatistic {
 *     reportFormat.set(ReportFormat.JSON)
 *     includeRemoteStatus.set(true)
 *     includeCommitGraph.set(true)
 *     commitHistoryDepth.set(100)
 * }
 * ```
 */
abstract class GitStatisticExtension {
    /**
     * Format for the status report (JSON, HTML, or MARKDOWN).
     * Default: JSON
     */
    @get:Input
    abstract val reportFormat: Property<ReportFormat>

    /**
     * Whether to include remote repository status in reports.
     * Default: true
     */
    @get:Input
    abstract val includeRemoteStatus: Property<Boolean>

    /**
     * Whether to include file changes in reports.
     * Default: true
     */
    @get:Input
    abstract val includeFileChanges: Property<Boolean>

    /**
     * Whether to include commit graph visualization in dashboard.
     * Default: true
     */
    @get:Input
    abstract val includeCommitGraph: Property<Boolean>

    /**
     * Whether to include contributor statistics in dashboard.
     * Default: true
     */
    @get:Input
    abstract val includeContributorStats: Property<Boolean>

    /**
     * Number of commits to include in history analysis.
     * Default: 50
     */
    @get:Input
    abstract val commitHistoryDepth: Property<Int>

    /**
     * Whether to include file change heatmap in dashboard.
     * Default: true
     */
    @get:Input
    abstract val includeFileHeatmap: Property<Boolean>

    init {
        reportFormat.convention(ReportFormat.JSON)
        includeRemoteStatus.convention(true)
        includeFileChanges.convention(true)
        includeCommitGraph.convention(true)
        includeContributorStats.convention(true)
        commitHistoryDepth.convention(50)
        includeFileHeatmap.convention(true)
    }
}

/**
 * Report output format options.
 */
enum class ReportFormat(val extension: String) {
    JSON("json"),
    HTML("html"),
    MARKDOWN("md")
}
