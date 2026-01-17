package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitDeleteRemoteTag task using Gradle TestKit.
 */
class GitDeleteRemoteTagTest : BaseGitTaskTest() {

    @Test
    fun `task deletes tag from remote`() {
        writeBuildKtsAndCommit("""
            tasks.named("gitDeleteRemoteTag") {
                tagName.set("v1.0.0")
            }
        """.trimIndent())

        // Setup remote
        val remoteDir = setupRemoteRepository("origin")

        // Create and push tag
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")
        executeGit(projectDir, "push", "origin", "v1.0.0")

        // Verify tag exists in remote
        val tagsBeforeRemote = executeGitAndGetOutput(remoteDir, "tag", "-l")
        assertTrue(tagsBeforeRemote.contains("v1.0.0"))

        val result = buildTask("gitDeleteRemoteTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitDeleteRemoteTag")?.outcome)
        assertTrue(result.output.contains("Deleted remote tag: v1.0.0"))

        // Verify tag is deleted from remote
        val tagsAfterRemote = executeGitAndGetOutput(remoteDir, "tag", "-l")
        assertFalse(tagsAfterRemote.contains("v1.0.0"))

        // Cleanup
        remoteDir.deleteRecursively()
    }

    @Test
    fun `task warns when remote tag does not exist`() {
        writeBuildKtsAndCommit("""
            tasks.named("gitDeleteRemoteTag") {
                tagName.set("non-existent-tag")
            }
        """.trimIndent())

        // Setup remote
        val remoteDir = setupRemoteRepository("origin")

        val result = buildTask("gitDeleteRemoteTag")

        // Task should succeed but warn
        assertEquals(TaskOutcome.SUCCESS, result.task(":gitDeleteRemoteTag")?.outcome)
        assertTrue(result.output.contains("Failed to delete") || result.output.contains("may not exist"))

        // Cleanup
        remoteDir.deleteRecursively()
    }

    @Test
    fun `task uses custom remote name`() {
        writeBuildKtsAndCommit("""
            gitTool {
                remoteName.set("upstream")
            }
            
            tasks.named("gitDeleteRemoteTag") {
                tagName.set("v1.0.0")
            }
        """.trimIndent())

        // Setup upstream remote
        val remoteDir = setupRemoteRepository("upstream")

        // Create and push tag to upstream
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")
        executeGit(projectDir, "push", "upstream", "v1.0.0")

        val result = buildTask("gitDeleteRemoteTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitDeleteRemoteTag")?.outcome)
        assertTrue(result.output.contains("Deleted remote tag: v1.0.0 from upstream"))

        // Cleanup
        remoteDir.deleteRecursively()
    }

    @Test
    fun `task fails when no remote is configured`() {
        writeBuildKtsAndCommit("""
            tasks.named("gitDeleteRemoteTag") {
                tagName.set("v1.0.0")
            }
        """.trimIndent())

        // Create tag locally but don't setup remote
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")

        val result = buildTask("gitDeleteRemoteTag")

        // Should succeed but warn about missing remote
        assertEquals(TaskOutcome.SUCCESS, result.task(":gitDeleteRemoteTag")?.outcome)
    }

    @Test
    fun `task can delete from multiple remotes`() {
        writeBuildKtsAndCommit("""
            tasks.register("deleteFromOrigin", lekanich.common.gradle.GitDeleteRemoteTag::class.java) {
                tagName.set("v1.0.0")
                remoteName.set("origin")
            }
            
            tasks.register("deleteFromUpstream", lekanich.common.gradle.GitDeleteRemoteTag::class.java) {
                tagName.set("v1.0.0")
                remoteName.set("upstream")
                mustRunAfter(tasks.named("deleteFromOrigin"))
            }
        """.trimIndent())

        // Setup two remotes
        val originDir = setupRemoteRepository("origin")
        val upstreamDir = setupRemoteRepository("upstream")

        // Create and push tag to both remotes
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")
        executeGit(projectDir, "push", "origin", "v1.0.0")
        executeGit(projectDir, "push", "upstream", "v1.0.0")

        val result = buildTask("deleteFromOrigin", "deleteFromUpstream")

        assertEquals(TaskOutcome.SUCCESS, result.task(":deleteFromOrigin")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":deleteFromUpstream")?.outcome)

        // Verify tag deleted from both remotes
        val tagsOrigin = executeGitAndGetOutput(originDir, "tag", "-l")
        val tagsUpstream = executeGitAndGetOutput(upstreamDir, "tag", "-l")
        assertFalse(tagsOrigin.contains("v1.0.0"))
        assertFalse(tagsUpstream.contains("v1.0.0"))

        // Cleanup
        originDir.deleteRecursively()
        upstreamDir.deleteRecursively()
    }
}
