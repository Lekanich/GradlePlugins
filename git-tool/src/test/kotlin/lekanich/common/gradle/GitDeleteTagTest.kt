package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitDeleteTag task using Gradle TestKit.
 */
class GitDeleteTagTest : BaseGitTaskTest() {

    @Test
    fun `task deletes local tag`() {
        makeBuildKtsAndCommit(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitDeleteTag>("gitDeleteTag") {
                tagName.set("v1.0.0")
            }
        """.trimIndent())

        // Create a tag
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")

        // Verify tag exists
        val tagsBefore = executeGitAndGetOutput(projectDir, "tag", "-l")
        assertTrue(tagsBefore.contains("v1.0.0"))

        val result = buildTask("gitDeleteTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitDeleteTag")?.outcome)
        assertTrue(result.output.contains("Deleted local tag: v1.0.0"))

        // Verify tag is deleted
        val tagsAfter = executeGitAndGetOutput(projectDir, "tag", "-l")
        assertFalse(tagsAfter.contains("v1.0.0"))
    }

    @Test
    fun `task fails when tag does not exist`() {
        makeBuildKtsAndCommit(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitDeleteTag>("gitDeleteTag") {
                tagName.set("non-existent-tag")
            }
        """.trimIndent())

        val result = buildAndFailTask("gitDeleteTag")

        assertEquals(TaskOutcome.FAILED, result.task(":gitDeleteTag")?.outcome)
        val output = result.output
        assertTrue(output.contains("tag 'non-existent-tag' not found"))
    }

    @Test
    fun `task can delete multiple tags in sequence`() {
        makeBuildKtsAndCommit(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.register("deleteTag1", lekanich.common.gradle.GitDeleteTag::class.java) {
                tagName.set("v1.0.0")
            }
            
            tasks.register("deleteTag2", lekanich.common.gradle.GitDeleteTag::class.java) {
                tagName.set("v2.0.0")
                mustRunAfter(tasks.named("deleteTag1"))
            }
        """.trimIndent())

        // Create tags
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")
        executeGit(projectDir, "tag", "-a", "v2.0.0", "-m", "Release 2.0.0")

        val result = buildTask("deleteTag1", "deleteTag2")

        assertEquals(TaskOutcome.SUCCESS, result.task(":deleteTag1")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":deleteTag2")?.outcome)

        // Verify both tags are deleted
        val tagsAfter = executeGitAndGetOutput(projectDir, "tag", "-l")
        assertEquals("", tagsAfter.trim())
    }
}
