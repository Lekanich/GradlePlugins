package lekanich.common.gradle.statistic

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GitStatisticPluginTest {

    @Test
    fun `plugin registers extension`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.lekanich.git-statistic")

        assertNotNull(project.extensions.findByName("gitStatistic"))
        assertTrue(project.extensions.getByName("gitStatistic") is GitStatisticExtension)
    }

    @Test
    fun `plugin registers gitStatusReport task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.lekanich.git-statistic")

        val task = project.tasks.findByName("gitStatusReport")
        assertNotNull(task)
        assertTrue(task is GitStatusReportTask)
    }

    @Test
    fun `plugin registers gitDashboard task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.lekanich.git-statistic")

        val task = project.tasks.findByName("gitDashboard")
        assertNotNull(task)
        assertTrue(task is GitDashboardTask)
    }

    @Test
    fun `extension has correct default values`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.lekanich.git-statistic")

        val extension = project.extensions.getByType(GitStatisticExtension::class.java)

        assertEquals(ReportFormat.JSON, extension.reportFormat.get())
        assertTrue(extension.includeRemoteStatus.get())
        assertTrue(extension.includeFileChanges.get())
        assertTrue(extension.includeCommitGraph.get())
        assertTrue(extension.includeContributorStats.get())
        assertEquals(50, extension.commitHistoryDepth.get())
        assertTrue(extension.includeFileHeatmap.get())
    }

    @Test
    fun `extension can be configured`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.lekanich.git-statistic")

        val extension = project.extensions.getByType(GitStatisticExtension::class.java)
        extension.reportFormat.set(ReportFormat.HTML)
        extension.includeRemoteStatus.set(false)
        extension.commitHistoryDepth.set(100)

        assertEquals(ReportFormat.HTML, extension.reportFormat.get())
        assertFalse(extension.includeRemoteStatus.get())
        assertEquals(100, extension.commitHistoryDepth.get())
    }
}
