package lekanich.common.gradle.statistic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.plugins.PublishingPlugin

/**
 * Git Statistic Plugin - Provides tasks for generating Git statistics, reports, and dashboards.
 *
 * This plugin adds the following tasks:
 * - gitStatusReport: Generate comprehensive Git status report in JSON/HTML/Markdown format
 * - gitDashboard: Generate interactive HTML dashboard with visualizations
 */
class GitStatisticPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create and register extension
        val extension = project.extensions.create("gitStatistic", GitStatisticExtension::class.java)

        // Register tasks
        registerStatusReportTask(project, extension)
        registerDashboardTask(project, extension)

        project.logger.info("Git Statistic Plugin applied to project: ${project.name}")
    }

    private fun registerStatusReportTask(project: Project, extension: GitStatisticExtension) {
        project.tasks.register("gitStatusReport", GitStatusReportTask::class.java) {
            group = PublishingPlugin.PUBLISH_TASK_GROUP
            description = "Generate Git status report in JSON/HTML/Markdown format"

            // Configure from extension
            format.convention(extension.reportFormat)
            outputFile.convention(
                project.layout.buildDirectory.file("reports/git-status/status.${extension.reportFormat.get().extension}")
            )
            includeRemoteStatus.convention(extension.includeRemoteStatus)
            includeFileChanges.convention(extension.includeFileChanges)
        }
    }

    private fun registerDashboardTask(project: Project, extension: GitStatisticExtension) {
        project.tasks.register("gitDashboard", GitDashboardTask::class.java) {
            group = PublishingPlugin.PUBLISH_TASK_GROUP
            description = "Generate interactive Git dashboard with visualizations"

            // Configure from extension
            outputDir.convention(project.layout.buildDirectory.dir("reports/git-dashboard"))
            includeCommitGraph.convention(extension.includeCommitGraph)
            includeContributorStats.convention(extension.includeContributorStats)
            commitHistoryDepth.convention(extension.commitHistoryDepth)
            includeFileHeatmap.convention(extension.includeFileHeatmap)
        }
    }
}

