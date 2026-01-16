package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitListTags task using Gradle TestKit.
 */
class GitListTagsTest : BaseGitTaskTest() {

    @Test
    fun `task lists all tags when no tags exist`() {
        makeBuildKtsAndCommit(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
        """.trimIndent())

        val result = buildTask("gitListTags")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitListTags")?.outcome)
        assertTrue(result.output.contains("Found 0 tag(s)"))

        val outputFile = projectDir.resolve("build/git/tags.txt")
        assertTrue(outputFile.exists())
        assertEquals("", outputFile.readText().trim())
    }

    @Test
    fun `task lists all tags`() {
        makeBuildKtsAndCommit(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
        """.trimIndent())

        // Create some tags
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")
        executeGit(projectDir, "tag", "-a", "v1.1.0", "-m", "Release 1.1.0")
        executeGit(projectDir, "tag", "-a", "v2.0.0", "-m", "Release 2.0.0")

        val result = buildTask("gitListTags")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitListTags")?.outcome)
        assertTrue(result.output.contains("Found 3 tag(s)"))

        val outputFile = projectDir.resolve("build/git/tags.txt")
        val tags = outputFile.readText().trim().lines()
        assertEquals(3, tags.size)
        assertTrue(tags.contains("v1.0.0"))
        assertTrue(tags.contains("v1.1.0"))
        assertTrue(tags.contains("v2.0.0"))
    }

    @Test
    fun `task filters tags by pattern`() {
        makeBuildKtsAndCommit(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitListTags>("gitListTags") {
                pattern.set("v1.*")
            }
        """.trimIndent())

        // Create some tags
        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")
        executeGit(projectDir, "tag", "-a", "v1.1.0", "-m", "Release 1.1.0")
        executeGit(projectDir, "tag", "-a", "v2.0.0", "-m", "Release 2.0.0")

        val result = buildTask("gitListTags")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitListTags")?.outcome)
        assertTrue(result.output.contains("Found 2 tag(s)"))

        val outputFile = projectDir.resolve("build/git/tags.txt")
        val tags = outputFile.readText().trim().lines()
        assertEquals(2, tags.size)
        assertTrue(tags.contains("v1.0.0"))
        assertTrue(tags.contains("v1.1.0"))
        assertFalse(tags.contains("v2.0.0"))
    }

    @Test
    fun `task exposes tags as output property`() {
        makeBuildKtsAndCommit(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.register("useTags") {
                dependsOn(tasks.named("gitListTags"))
                doLast {
                    val tagsTask = tasks.named<lekanich.common.gradle.GitListTags>("gitListTags").get()
                    val tagsList = tagsTask.tags.get()
                    println("Tags from property: " + tagsList)
                }
            }
        """.trimIndent())

        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")

        val result = buildTask("useTags")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitListTags")?.outcome)
        assertTrue(result.output.contains("Tags from property:"))
        assertTrue(result.output.contains("v1.0.0"))
    }

    @Test
    fun `task respects writeToFile setting`() {
        makeBuildKtsAndCommit(projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            gitTool {
                writeToFile.set(false)
            }
        """.trimIndent())

        executeGit(projectDir, "tag", "-a", "v1.0.0", "-m", "Release 1.0.0")

        buildTask("gitListTags")

        val outputFile = projectDir.resolve("build/git/tags.txt")
        assertFalse(outputFile.exists(), "Output file should not exist when writeToFile is false")
    }
}
