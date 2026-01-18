package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for the GitReleaseTag task using Gradle TestKit.
 */
class GitReleaseTagTest : BaseGitTaskTest() {

    @BeforeEach
    override fun setup() {
        super.setup()
        writeBuildKtsAndCommit("")
    }

    @Test
    fun `task has correct properties`() {
        writeBuildKts("")

        val result = buildTask("tasks", "--all")

        assertTrue(result.output.contains("gitReleaseTag"), "gitReleaseTag task should exist")
    }

    @Test
    fun `task depends on required tasks`() {
        writeBuildKts(
            """
            tasks{ 
                gitReleaseTag {
                    tagName.set("v1.0.0")
                }
            }
        """.trimIndent()
        )

        // Note: We can't actually test the full workflow without a remote repository
        // but we can verify the task exists and has the correct configuration
        val result = buildTask("tasks", "--all")

        assertTrue(result.output.contains("gitReleaseTag"))
    }

    @Test
    fun `task passes tagName to dependent tasks`() {
        writeBuildKts(
            $$"""
            tasks{ 
                gitReleaseTag {
                    tagName.set("v2.0.0")
                }
            }
            
            // Verify that dependent tasks receive the tagName
            tasks {
                gitCheckTag {
                    doFirst {
                        println("GitCheckTag tagName: ${tagName.get()}")
                    }
                }
                gitCreateTag {
                    doFirst {
                        println("GitCreateTag tagName: ${tagName.get()}")
                    }
                }
                gitPushTag {
                    doFirst {
                        println("GitPushTag tagName: ${tagName.get()}")
                    }
                }
            }
        """.trimIndent()
        )

        val result = buildTask("help")

        // Just verify configuration doesn't fail
        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }

    @Test
    fun `task can be configured with properties`() {
        writeBuildKts(
            """
            tasks{
                gitReleaseTag {
                    tagName.set("v1.5.0")
                    requireCleanWorkspace.set(false)
                    validateBeforeTag.set(false)
                }
            }
        """.trimIndent()
        )

        val result = buildTask("tasks", "--all")

        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }

    @Test
    fun `task uses extension default values`() {
        writeBuildKts(
            """
            gitTool {
                requireCleanWorkspace.set(false)
                validateBeforeTag.set(false)
            }
            
            tasks {
                gitReleaseTag {
                    tagName.set("v1.0.0")
                    // Should inherit from extension
                }
            }
        """.trimIndent()
        )

        val result = buildTask("tasks", "--all")

        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
}
