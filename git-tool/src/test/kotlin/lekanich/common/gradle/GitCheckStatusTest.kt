package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for the GitCheckStatus task using Gradle TestKit.
 */
class GitCheckStatusTest : BaseGitTaskTest() {
    private lateinit var buildFile: File

    @BeforeEach
    override fun setup() {
        super.setup()
        buildFile = makeBuildKtsAndCommit(
            projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
        """.trimIndent()
        )
    }

    @Test
    fun `task succeeds with clean workspace`() {
        val result = buildTask("gitCheckStatus")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCheckStatus")?.outcome)
        val output = result.output
        assertTrue(output.contains("Git workspace is clean"))
    }

    @Test
    fun `task fails with dirty workspace`() {
        // Make workspace dirty
        File(projectDir, "dirty.txt").writeText("uncommitted change")

        val result = buildAndFailTask("gitCheckStatus")

        assertEquals(TaskOutcome.FAILED, result.task(":gitCheckStatus")?.outcome)
        assertTrue(result.output.contains("dirty") || result.output.contains("commit"))
    }

    @Test
    fun `task detects staged but uncommitted files`() {
        // Stage a file but don't commit
        File(projectDir, "staged.txt").writeText("staged content")
        executeGit(projectDir, "add", "staged.txt")

        val result = buildAndFailTask("gitCheckStatus")

        assertEquals(TaskOutcome.FAILED, result.task(":gitCheckStatus")?.outcome)
    }
}
