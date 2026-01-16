package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for the GitPushTag task using Gradle TestKit.
 */
class GitPushTagTest : BaseGitTaskTest() {

    @Test
    fun `task is registered correctly`() {
        createBasicGradleProject(
            projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
        """.trimIndent()
        )

        val result = buildTask("tasks", "--group=publishing")

        assertTrue(result.output.contains("gitPushTag"))
    }

    @Test
    fun `task uses remote name from extension`() {
        createBasicGradleProject(
            projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            gitTool {
                remoteName.set("upstream")
            }
            
            tasks {
                gitPushTag {
                    tagName.set("v1.0.0")
                }
            }
        """.trimIndent()
        )

        // Create and configure upstream remote
        val remoteDir = setupRemoteRepository("upstream")

        // Create a tag
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Test tag")

        // Execute the task - it should push to 'upstream', not 'origin'
        val result = buildTask("gitPushTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitPushTag")?.outcome)
        assertTrue(result.output.contains("Pushed tag"))
        assertTrue(result.output.contains("upstream"), "Should push to upstream remote")

        // Verify tag exists in upstream remote
        val remoteTags = executeGitAndGetOutput(remoteDir, "tag", "-l")
        assertTrue(remoteTags.contains("v1.0.0"), "Tag should be pushed to upstream remote")

        // Cleanup
        remoteDir.deleteRecursively()
    }

    @Test
    fun `task pushes tag to configured remote`() {
        createBasicGradleProject(
            projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitPushTag>("gitPushTag") {
                tagName.set(project.findProperty("tagName") as String? ?: "v1.0.0")
            }
        """.trimIndent()
        )

        // Create and configure origin remote
        val remoteDir = setupRemoteRepository("origin")

        // Create a tag
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Test tag")

        val result = buildTask("gitPushTag", "-PtagName=v1.0.0")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitPushTag")?.outcome)
        assertTrue(result.output.contains("Pushed tag"))

        // Verify tag exists in remote
        val remoteTags = executeGitAndGetOutput(remoteDir, "tag", "-l")
        assertTrue(remoteTags.contains("v1.0.0"), "Tag should be pushed to remote")

        // Cleanup
        remoteDir.deleteRecursively()
    }

    @Test
    fun `task fails gracefully when remote is not configured`() {
        createBasicGradleProject(
            projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitPushTag>("gitPushTag") {
                tagName.set("v1.0.0")
            }
        """.trimIndent()
        )

        // Create a tag but don't configure remote
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Test tag")

        val result = buildAndFailTask("gitPushTag")

        assertEquals(TaskOutcome.FAILED, result.task(":gitPushTag")?.outcome)
    }

    @Test
    fun `task fails when tag does not exist locally`() {
        createBasicGradleProject(
            projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitPushTag>("gitPushTag") {
                tagName.set("v1.0.0")
            }
        """.trimIndent()
        )

        // Setup remote
        val remoteDir = File(projectDir.parentFile, "remote-${System.currentTimeMillis()}.git")
        remoteDir.mkdirs()
        executeGit(remoteDir, "init", "--bare")
        executeGit(projectDir, "remote", "add", "origin", remoteDir.absolutePath)

        // Don't create the tag - task should fail
        val result = buildAndFailTask("gitPushTag")

        assertEquals(TaskOutcome.FAILED, result.task(":gitPushTag")?.outcome)

        // Cleanup
        remoteDir.deleteRecursively()
    }

    /**
     * Sets up a bare remote repository and adds it as a remote.
     * Also pushes the initial branch to the remote.
     *
     * @param remoteName The name of the remote to create (e.g., "origin", "upstream")
     * @return The remote repository directory
     */
    private fun setupRemoteRepository(remoteName: String): File {
        val remoteDir = File(projectDir.parentFile, "remote-$remoteName-${System.currentTimeMillis()}.git")
        remoteDir.mkdirs()
        executeGit(remoteDir, "init", "--bare")
        executeGit(projectDir, "remote", "add", remoteName, remoteDir.absolutePath)

        // Push initial branch to remote
        try {
            executeGit(projectDir, "push", "-u", remoteName, "master")
        } catch (_: Exception) {
            executeGit(projectDir, "branch", "-M", "main")
            executeGit(projectDir, "push", "-u", remoteName, "main")
        }

        return remoteDir
    }
}
