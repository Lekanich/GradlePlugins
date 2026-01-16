package lekanich.common.gradle

import org.junit.jupiter.api.BeforeEach
import java.io.File

/**
 * @year 2026
 */
abstract class BaseGitTaskTest : BaseGitTest() {

    @BeforeEach
    override fun setup() {
        super.setup()

        createInitialCommit(projectDir)
    }

    private fun createInitialCommit(dir: File) {
        // Ignore .gradle directory
        File(dir, ".gitignore").writeText(".gradle/\n")

        executeGit(dir, "add", ".gitignore")
        executeGit(dir, "commit", "-m", "Initial commit")
    }

    protected fun createBasicGradleProject(dir: File, buildKtsContent: String): File {
        val buildFile = File(dir, "build.gradle.kts")
        buildFile.writeText(buildKtsContent)

        executeGit(dir, "add", "build.gradle.kts")
        executeGit(dir, "commit", "-m", "Gradle project")

        return buildFile
    }
}