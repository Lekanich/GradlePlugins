package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitCommitCount task using Gradle TestKit.
 */
class GitCommitCountTest : BaseGitTaskTest() {

    @Test
    fun `task counts all commits`() {
        makeBuildKts(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
        """.trimIndent())

        val result = buildTask("gitCommitCount")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCommitCount")?.outcome)
        assertTrue(result.output.contains("Total commits: 1"))

        val outputFile = projectDir.resolve("build/git/commit-count.txt")
        assertEquals("1", outputFile.readText().trim())
    }

    @Test
    fun `task counts commits since a tag`() {
        makeBuildKts(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitCommitCount>("gitCommitCount") {
                since.set("v1.0.0")
            }
        """.trimIndent())

        // Create a tag at current commit
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")

        // Make more commits
        projectDir.resolve("file2.txt").writeText("content 2")
        executeGit(projectDir, "add", "file2.txt")
        executeGit(projectDir, "commit", "-m", "Commit after tag")

        projectDir.resolve("file3.txt").writeText("content 3")
        executeGit(projectDir, "add", "file3.txt")
        executeGit(projectDir, "commit", "-m", "Another commit")

        val result = buildTask("gitCommitCount")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCommitCount")?.outcome)
        assertTrue(result.output.contains("Commits since v1.0.0: 2"))

        val outputFile = projectDir.resolve("build/git/commit-count.txt")
        assertEquals("2", outputFile.readText().trim())
    }

    @Test
    fun `task counts commits since a commit hash`() {
        writeBuildKtsAndCommit("")

        // Get the hash of the current (second) commit
        val firstCommit = executeGitAndGetOutput(projectDir, "rev-parse", "HEAD~1")

        // Make another commit
        projectDir.resolve("file2.txt").writeText("content 2")
        executeGit(projectDir, "add", "file2.txt")
        executeGit(projectDir, "commit", "-m", "Third commit")

        // Configure task to count since first commit
        writeBuildKts("""
            tasks.named<lekanich.common.gradle.GitCommitCount>("gitCommitCount") {
                since.set("${firstCommit.trim()}")
            }
        """.trimIndent())

        val result = buildTask("gitCommitCount")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCommitCount")?.outcome)

        val outputFile = projectDir.resolve("build/git/commit-count.txt")
        assertEquals("2", outputFile.readText().trim())
    }

    @Test
    fun `task exposes commit count as output property`() {
        writeBuildKts("""
            tasks.register("useCount") {
                dependsOn(tasks.named("gitCommitCount"))
                doLast {
                    val countTask = tasks.named<lekanich.common.gradle.GitCommitCount>("gitCommitCount").get()
                    val count = countTask.commitCount.get()
                    println("Count from property: " + count)
                }
            }
        """.trimIndent())

        val result = buildTask("useCount")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCommitCount")?.outcome)
        assertTrue(result.output.contains("Count from property: 1"))
    }

    @Test
    fun `task respects writeToFile setting`() {
        writeBuildKts("""
            gitTool {
                writeToFile.set(false)
            }
        """.trimIndent())

        buildTask("gitCommitCount")

        val outputFile = projectDir.resolve("build/git/commit-count.txt")
        assertFalse(outputFile.exists(), "Output file should not exist when writeToFile is false")
    }

    @Test
    fun `task returns zero commits when since is HEAD`() {
        writeBuildKts("""
            tasks.named<lekanich.common.gradle.GitCommitCount>("gitCommitCount") {
                since.set("HEAD")
            }
        """.trimIndent())

        val result = buildTask("gitCommitCount")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCommitCount")?.outcome)

        val outputFile = projectDir.resolve("build/git/commit-count.txt")
        assertEquals("0", outputFile.readText().trim())
    }
}
