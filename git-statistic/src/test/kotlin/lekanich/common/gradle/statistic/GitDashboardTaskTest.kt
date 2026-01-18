package lekanich.common.gradle.statistic

import org.junit.jupiter.api.Test
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled


class GitDashboardTaskTest : BaseGitTaskTest() {

    @Disabled
    @Test
    fun testGeneratesDashboardForRealGitRepo() {
        // Create a tag
        executeGit(projectDir, "git tag v1.0.0")
        // Add another commit
        File(projectDir, "file2.txt").writeText("Another file")
        executeGit(projectDir, "git add file2.txt")
        executeGit(projectDir, "git commit -m 'Second commit'")

        // Write build.gradle.kts to apply plugin and configure task
        writeBuildKts(
            """
            gitStatistic {
                dashboard {
                    projectDirectory.set(file("."))
                    outputDir.set(layout.buildDirectory.dir("dashboard"))
                }
            }
            """.trimIndent()
        )

        // Run the dashboard task
        buildTask("gitDashboard")

        // Check that dashboard HTML was generated
        val dashboardFile = File(projectDir, "build/dashboard/index.html")
        assertTrue(dashboardFile.exists(), "Dashboard HTML should be generated")
        val html = dashboardFile.readText()
        assertTrue(html.contains("Git Dashboard"), "Dashboard HTML should contain title")
        assertTrue(html.contains("v1.0.0"), "Dashboard HTML should contain tag")
        assertTrue(html.contains("Initial commit"), "Dashboard HTML should contain commit message")
    }
}
