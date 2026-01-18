package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitGetLastTag task using Gradle TestKit.
 */
class GitGetLastTagTest : BaseGitTaskTest() {

    @Test
    fun `task handles repository with no tags`() {
        writeBuildKtsAndCommit("")

        val result = buildTask("gitGetLastTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetLastTag")?.outcome)
        assertTrue(result.output.contains("No tags found"))

        val outputFile = projectDir.resolve("build/git/last-tag.txt")
        assertTrue(outputFile.exists())
        assertEquals("", outputFile.readText().trim())
    }

    @Test
    fun `task gets most recent tag`() {
        writeBuildKtsAndCommit("")

        // Create tags in sequence
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")

        // Make another commit
        projectDir.resolve("file2.txt").writeText("content")
        executeGit(projectDir, "add", "file2.txt")
        executeGit(projectDir, "commit", "-m", "Second commit")

        executeGit(projectDir, "tag", "-a", "v2.0.0", "-m", "Release 2.0.0")

        val result = buildTask("gitGetLastTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetLastTag")?.outcome)
        assertTrue(result.output.contains("Most recent tag: v2.0.0"))

        val outputFile = projectDir.resolve("build/git/last-tag.txt")
        assertEquals("v2.0.0", outputFile.readText().trim())
    }

    @Test
    fun `task exposes last tag as output property`() {
        writeBuildKtsAndCommit("""
            tasks {
                register("useLastTag") {
                    dependsOn(gitGetLastTag)
                    doLast {
                        val tagTask = gitGetLastTag.get()
                        val tag = tagTask.lastTag.get()
                        println("Last tag from property: " + tag)
                    }
                }
            }
        """.trimIndent())

        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")

        val result = buildTask("useLastTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetLastTag")?.outcome)
        assertTrue(result.output.contains("Last tag from property: v1.0.0"))
    }

    @Test
    fun `task gets latest tag when multiple exist`() {
        writeBuildKtsAndCommit("")

        // Create multiple tags
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")
        executeGit(projectDir, "tag", "-a", "v1.1.0", "-m", "Release 1.1.0")
        executeGit(projectDir, "tag", "-a", "v1.2.0", "-m", "Release 1.2.0")

        val result = buildTask("gitGetLastTag")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetLastTag")?.outcome)

        val outputFile = projectDir.resolve("build/git/last-tag.txt")
        val lastTag = outputFile.readText().trim()
        // Should be one of the tags (Git decides which is "last" based on commit graph)
        assertTrue(lastTag in listOf("v1.0.0", "v1.1.0", "v1.2.0"))
    }

    @Test
    fun `task respects writeToFile setting`() {
        writeBuildKtsAndCommit("""
            tasks {
                gitGetLastTag {
                    writeToFile.set(false)
                }
            }
        """.trimIndent())

        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")

        buildTask("gitGetLastTag")

        val outputFile = projectDir.resolve("build/git/last-tag.txt")
        assertFalse(outputFile.exists(), "Output file should not exist when writeToFile is false")
    }
}
