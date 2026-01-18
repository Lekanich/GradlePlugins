package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitGetCurrentBranch task using Gradle TestKit.
 */
class GitGetCurrentBranchTest : BaseGitTaskTest() {

    @Test
    fun `task gets current branch name`() {
        writeBuildKtsAndCommit("")

        val result = buildTask("gitGetCurrentBranch")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetCurrentBranch")?.outcome)
        assertTrue(result.output.contains("Current branch:") || result.output.contains("master") || result.output.contains("main"))
    }

    @Test
    fun `task writes branch name to file`() {
        writeBuildKtsAndCommit("")

        buildTask("gitGetCurrentBranch")

        val outputFile = projectDir.resolve("build/git/branch.txt")
        assertTrue(outputFile.exists(), "Output file should exist")

        val branchName = outputFile.readText().trim()
        assertTrue(branchName.isNotEmpty(), "Branch name should not be empty")
        assertTrue(branchName == "master" || branchName == "main", "Should be master or main branch")
    }

    @Test
    fun `task exposes branch name as output property`() {
        writeBuildKtsAndCommit("""
            tasks {
                register("useBranch") {
                    dependsOn(gitGetCurrentBranch)
                    doLast {
                        val branchTask = gitGetCurrentBranch.get()
                        val branch = branchTask.branchName.get()
                        println("Branch from property: " + branch)
                    }
                }
            }
        """.trimIndent())

        val result = buildTask("useBranch")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetCurrentBranch")?.outcome)
        assertTrue(result.output.contains("Branch from property:"))
    }

    @Test
    fun `task respects writeToFile setting`() {
        writeBuildKtsAndCommit("""
            gitTool {
                writeToFile.set(false)
            }
        """.trimIndent())

        buildTask("gitGetCurrentBranch")

        val outputFile = projectDir.resolve("build/git/branch.txt")
        assertFalse(outputFile.exists(), "Output file should not exist when writeToFile is false")
    }

    @Test
    fun `task handles detached HEAD gracefully`() {
        writeBuildKtsAndCommit("")

        // Create a detached HEAD state
        val firstCommit = executeGitAndGetOutput(projectDir, "rev-parse", "HEAD")
        executeGit(projectDir, "checkout", firstCommit.trim())

        val result = buildTask("gitGetCurrentBranch")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetCurrentBranch")?.outcome)
        // Should warn about detached HEAD
        assertTrue(result.output.contains("detached HEAD") || result.output.contains("No branch name"))
    }
}
