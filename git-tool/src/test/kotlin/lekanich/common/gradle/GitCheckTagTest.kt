package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for the GitCheckTag task using Gradle TestKit.
 */
class GitCheckTagTest : BaseGitTaskTest() {

    @BeforeEach
    override fun setup() {
        super.setup()
        buildFile = makeBuildKtsAndCommit(
            projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitCheckTag>("gitCheckTag") {
                tagName.set(project.findProperty("tagName") as String? ?: "v1.0.0")
            }
        """.trimIndent()
        )
    }

    @Test
    fun `task succeeds when tag does not exist`() {
        val result = buildTask("gitCheckTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCheckTag")?.outcome)
        assertTrue(result.output.contains("does not exist"))
    }

    @Test
    fun `task fails when tag already exists`() {
        // Create a tag
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Test tag")

        val result = buildAndFailTask("gitCheckTag")

        assertEquals(TaskOutcome.FAILED, result.task(":gitCheckTag")?.outcome)
        assertTrue(result.output.contains("already exists"))
    }

    @Test
    fun `task uses remote name from extension`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            gitTool {
                remoteName.set("upstream")
            }
            
            tasks.named<lekanich.common.gradle.GitCheckTag>("gitCheckTag") {
                tagName.set("v1.0.0")
            }
        """.trimIndent()
        )

        val result = buildTask("gitCheckTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCheckTag")?.outcome)
    }

    @Test
    fun `task checks multiple tags correctly`() {
        // Create some tags
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Version 1.0.0")
        executeGit(projectDir, "tag", "-a", "v1.1.0", "-m", "Version 1.1.0")

        // Check existing tag fails
        val failResult = buildAndFailTask("gitCheckTag")

        assertEquals(TaskOutcome.FAILED, failResult.task(":gitCheckTag")?.outcome)

        // Check non-existing tag succeeds
        val successResult = buildTask("gitCheckTag", "-PtagName=v2.0.0")

        assertEquals(TaskOutcome.SUCCESS, successResult.task(":gitCheckTag")?.outcome)
    }
}
