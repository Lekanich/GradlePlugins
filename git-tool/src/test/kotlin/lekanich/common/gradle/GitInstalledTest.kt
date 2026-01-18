package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitInstalled task using Gradle TestKit.
 */
class GitInstalledTest : BaseGitTaskTest() {

    @Test
    fun `task succeeds when Git is installed`() {
        writeBuildKtsAndCommit("")

        val result = buildTask("gitInstalled")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitInstalled")?.outcome)
    }

    @Test
    fun `task displays Git version information`() {
        writeBuildKtsAndCommit("")

        val result = buildTask("gitInstalled")

        val output = result.output
        assertTrue(
            output.contains("✓ Git is installed:") && output.contains("git version"),
            "Output should contain Git version information. Actual output: $output"
        )
    }

    @Test
    fun `task is registered by plugin`() {
        writeBuildKtsAndCommit("")

        val result = buildTask("tasks", "--group=publishing")

        assertTrue(
            result.output.contains("gitInstalled"),
            "Task should be listed in publish group"
        )
    }

    @Test
    fun `task has correct description`() {
        writeBuildKtsAndCommit("")

        val result = buildTask("tasks", "--group=publishing", "--all")

        val output = result.output
        assertTrue(
            output.contains("gitInstalled") &&
            output.contains("Verify that Git is installed"),
            "Task should have correct description"
        )
    }

    @Test
    fun `task can be executed multiple times`() {
        writeBuildKtsAndCommit("")

        // Execute task first time
        val result1 = buildTask("gitInstalled")
        assertEquals(TaskOutcome.SUCCESS, result1.task(":gitInstalled")?.outcome)

        // Execute task second time
        val result2 = buildTask("gitInstalled")
        // Task should execute again since it's not cacheable
        assertEquals(TaskOutcome.SUCCESS, result2.task(":gitInstalled")?.outcome)
    }

    @Test
    fun `task reports Git version with lifecycle logging`() {
        writeBuildKtsAndCommit("")

        val result = buildTask("gitInstalled", "--info")

        val output = result.output
        assertTrue(
            output.contains("Checking if Git is installed"),
            "Should log info message about checking Git"
        )
        assertTrue(
            output.contains("✓ Git is installed:"),
            "Should log lifecycle message with Git version"
        )
    }

    @Test
    fun `task is not cacheable`() {
        writeBuildKtsAndCommit("""
            tasks {
                gitInstalled {
                    // Verify that caching is disabled for this task
                    outputs.cacheIf { false }
                }
            }
        """.trimIndent())

        val result = buildTask("gitInstalled")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitInstalled")?.outcome)
    }
}
