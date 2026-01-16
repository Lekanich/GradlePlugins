package lekanich.common.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Tests for the GitCreateTag task using Gradle TestKit.
 */
class GitCreateTagTest : BaseGitTaskTest() {
    private lateinit var buildFile: File

    @BeforeEach
    override fun setup() {
        super.setup()
        buildFile = createBasicGradleProject(
            projectDir, """
            plugins {
                id("io.github.lekanich.git-tool")
            }
        """.trimIndent()
        )
    }

    @Test
    fun `task creates tag with property defined tag name`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("gitCreateTag", "-PtagName=v1.0.0")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCreateTag")?.outcome)
        assertTrue(result.output.contains("Created Git tag: v1.0.0"))

        // Verify tag was created
        val tags = getGitTags(projectDir)
        assertTrue(tags.contains("v1.0.0"), "Tag v1.0.0 should be created")
    }

    @Test
    fun `task creates tag with default message`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitCreateTag>("gitCreateTag") {
                tagName.set("v1.0.0")
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("gitCreateTag")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCreateTag")?.outcome)
        assertTrue(result.output.contains("Created Git tag: v1.0.0"))

        // Verify tag was created
        val tags = getGitTags(projectDir)
        assertTrue(tags.contains("v1.0.0"), "Tag v1.0.0 should be created")
    }

    @Test
    fun `task creates tag with custom message`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            tasks.named<lekanich.common.gradle.GitCreateTag>("gitCreateTag") {
                tagName.set("v2.0.0")
                tagMessage.set("Custom release message for {tag}")
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("gitCreateTag")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCreateTag")?.outcome)

        // Verify tag was created
        val tags = getGitTags(projectDir)
        assertTrue(tags.contains("v2.0.0"), "Tag v2.0.0 should be created")

        // Verify tag message contains the custom message
        val tagMessage = executeGitAndGetOutput(projectDir, "tag", "-l", "-n1", "v2.0.0")
        assertTrue(tagMessage.contains("Custom release message for v2.0.0"))
    }

    @Test
    fun `task uses extension default message template`() {
        buildFile.writeText(
            """
            plugins {
                id("io.github.lekanich.git-tool")
            }
            
            gitTool {
                defaultTagMessage.set("Version {tag} released")
            }
            
            tasks.named<lekanich.common.gradle.GitCreateTag>("gitCreateTag") {
                tagName.set("v3.0.0")
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("gitCreateTag")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitCreateTag")?.outcome)

        // Verify tag message
        val tagMessage = executeGitAndGetOutput(projectDir, "tag", "-l", "-n1", "v3.0.0")
        assertTrue(tagMessage.contains("Version v3.0.0 released"))
    }

    private fun getGitTags(dir: File): List<String> {
        val output = executeGitAndGetOutput(dir, "tag", "-l")
        return output.split("\n").filter { it.isNotBlank() }
    }
}
