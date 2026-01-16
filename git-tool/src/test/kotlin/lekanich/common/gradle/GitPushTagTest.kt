package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
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

        val result = buildTask("gitPushTag", "--dry-run")
        assertNotNull(result.task(":gitPushTag"))
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

        // Create a bare remote repository
        val remoteDir = File(projectDir.parentFile, "remote-${System.currentTimeMillis()}.git")
        remoteDir.mkdirs()
        executeGit(remoteDir, "init", "--bare")

        // Add remote
        executeGit(projectDir, "remote", "add", "origin", remoteDir.absolutePath)

        // Push initial branch
        try {
            executeGit(projectDir, "push", "-u", "origin", "master")
        } catch (e1: Exception) {
            println(e1)
            // Try main if master doesn't work
            try {
                executeGit(projectDir, "branch", "-M", "main")
                executeGit(projectDir, "push", "-u", "origin", "main")
            } catch (e2: Exception) {
                println(e2)
            }
        }

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
}
