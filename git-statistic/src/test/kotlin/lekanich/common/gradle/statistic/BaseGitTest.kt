package lekanich.common.gradle.statistic

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * @year 2026
 */
abstract class BaseGitTest {
    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    open fun setup() {
        initGitRepo(projectDir)
    }

    @AfterEach
    fun tearDown() {
        // Clean up the project directory
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
    }

    private fun initGitRepo(dir: File) {
        executeGit(dir, "init")
        executeGit(dir, "config", "user.email", "test@example.com")
        executeGit(dir, "config", "user.name", "Test User")
    }

    protected fun executeGit(dir: File, vararg args: String) {
        val process = ProcessBuilder("git", *args)
            .directory(dir)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText()
            throw RuntimeException("Git command failed: ${args.joinToString(" ")}\n$output")
        }
    }

    protected fun executeGitAndGetOutput(dir: File, vararg args: String): String {
        val process = ProcessBuilder("git", *args)
            .directory(dir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("Git command failed: ${args.joinToString(" ")}\n$output")
        }

        return output.trim()
    }

    protected fun buildTask(vararg arguments: String, dir: File = projectDir): BuildResult =
        createRunner(*arguments, dir = dir)
            .build()

    protected fun buildAndFailTask(vararg arguments: String, dir: File = projectDir): BuildResult =
        createRunner(*arguments, dir = dir)
            .buildAndFail()

    private fun createRunner(vararg arguments: String, dir: File): GradleRunner =
        GradleRunner.create()
            .withProjectDir(dir)
            .withArguments(*arguments)
            .withPluginClasspath()
            .withDebug(true)
}
