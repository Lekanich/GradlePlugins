package lekanich.common.gradle

import org.junit.jupiter.api.BeforeEach
import java.io.File

/**
 * @year 2026
 */
abstract class BaseGitTaskTest : BaseGitTest() {
    protected lateinit var settingsFile: File
    private lateinit var buildFile: File

    @BeforeEach
    override fun setup() {
        super.setup()

        settingsFile = File(projectDir, "settings.gradle.kts")
        buildFile = File(projectDir, "build.gradle.kts")

        createInitialCommit()
    }

    protected fun writeBuildKts(content: String): File {
        buildFile.writeText(
            """
            plugins {
                id("io.github.lekanich.git-tool")
                id("jacoco-testkit-coverage") // this will dump coverage data
            }
            
            $content
        """.trimIndent()
        )
        return buildFile
    }

    private fun createInitialCommit() {
        // Ignore .gradle directory
        File(projectDir, ".gitignore").writeText(".gradle/\n")

        executeGit(projectDir, "add", ".gitignore")
        executeGit(projectDir, "commit", "-m", "Initial commit")
    }

    protected fun writeBuildKtsAndCommit(buildKtsContent: String): File {
        val buildFile = writeBuildKts(buildKtsContent)

        executeGit(projectDir, "add", "build.gradle.kts")
        executeGit(projectDir, "commit", "-m", "Gradle project")

        return buildFile
    }

    /**
     * Sets up a bare remote repository and adds it as a remote.
     * Also pushes the initial branch to the remote.
     *
     * @param remoteName The name of the remote to create (e.g., "origin", "upstream")
     * @return The remote repository directory
     */
    protected fun setupRemoteRepository(remoteName: String): File {
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