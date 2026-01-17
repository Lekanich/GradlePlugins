package lekanich.common.gradle

import org.junit.jupiter.api.BeforeEach
import java.io.File

/**
 * @year 2026
 */
abstract class BaseGitTaskTest : BaseGitTest() {
    protected lateinit var settingsFile: File
    protected lateinit var buildFile: File

    @BeforeEach
    override fun setup() {
        super.setup()

        settingsFile = File(projectDir, "settings.gradle")
        buildFile = File(projectDir, "build.gradle")

        createInitialCommit(projectDir)
    }

    private fun createInitialCommit(dir: File) {
        // Ignore .gradle directory
        File(dir, ".gitignore").writeText(".gradle/\n")

        executeGit(dir, "add", ".gitignore")
        executeGit(dir, "commit", "-m", "Initial commit")
    }

    protected fun makeBuildKtsAndCommit(dir: File, buildKtsContent: String): File {
        val buildFile = makeBuildKts(dir, buildKtsContent)

        executeGit(dir, "add", "build.gradle.kts")
        executeGit(dir, "commit", "-m", "Gradle project")

        return buildFile
    }

    protected fun makeBuildKts(dir: File, buildKtsContent: String): File {
        val buildFile = File(dir, "build.gradle.kts")
        buildFile.writeText(buildKtsContent)
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