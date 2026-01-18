package lekanich.common.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitGetCommitHash task using Gradle TestKit.
 */
class GitGetCommitHashTest : BaseGitTaskTest() {

    @Test
    fun `task gets full commit hash`() {
        writeBuildKtsAndCommit("")

        val result = buildTask("gitGetCommitHash")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetCommitHash")?.outcome)
        assertTrue(result.output.contains("Current commit:"))

        val outputFile = projectDir.resolve("build/git/commit-hash.txt")
        val hash = outputFile.readText().trim()
        assertEquals(40, hash.length, "Full commit hash should be 40 characters")
    }

    @Test
    fun `task gets short commit hash`() {
        writeBuildKtsAndCommit("""
            tasks {
                gitGetCommitHash {
                    shortHash.set(true)
                }
            }
        """.trimIndent())

        val result = buildTask("gitGetCommitHash")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetCommitHash")?.outcome)

        val outputFile = projectDir.resolve("build/git/commit-hash.txt")
        val hash = outputFile.readText().trim()
        assertEquals(7, hash.length, "Short commit hash should be 7 characters")
    }

    @Test
    fun `task exposes commit hash as output property`() {
        writeBuildKtsAndCommit("""
            tasks {
                register("useHash") {
                    dependsOn(gitGetCommitHash)
                    doLast {
                        val hashTask = gitGetCommitHash.get()
                        val hash = hashTask.commitHash.get()
                        println("Hash from property: " + hash)
                    }
                }    
            }
        """.trimIndent())

        val result = buildTask("useHash")

        assertEquals(TaskOutcome.SUCCESS, result.task(":gitGetCommitHash")?.outcome)
        assertTrue(result.output.contains("Hash from property:"))
    }

    @Test
    fun `task respects writeToFile setting`() {
        writeBuildKtsAndCommit("""
            tasks {
                gitGetCommitHash {
                    writeToFile.set(false)
                }
            }
        """.trimIndent())

        buildTask("gitGetCommitHash")

        val outputFile = projectDir.resolve("build/git/commit-hash.txt")
        assertFalse(outputFile.exists(), "Output file should not exist when writeToFile is false")
    }

    @Test
    fun `short and full hashes match`() {
        writeBuildKtsAndCommit("""
            tasks.register("gitGetCommitHashShort", lekanich.common.gradle.GitGetCommitHash::class.java) {
                shortHash.set(true)
                outputFile.set(layout.buildDirectory.file("git/commit-hash-short.txt"))
            }
        """.trimIndent())

        buildTask("gitGetCommitHash", "gitGetCommitHashShort")

        val fullHash = projectDir.resolve("build/git/commit-hash.txt").readText().trim()
        val shortHash = projectDir.resolve("build/git/commit-hash-short.txt").readText().trim()

        assertTrue(fullHash.startsWith(shortHash), "Full hash should start with short hash")
    }
}
