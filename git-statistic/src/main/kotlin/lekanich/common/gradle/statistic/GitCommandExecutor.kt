package lekanich.common.gradle.statistic

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Utility class for executing Git commands.
 * Provides consistent error handling and logging for Git operations.
 */
open class GitCommandExecutor(
    private val workingDirectory: File,
    private val logger: Logger
) {
    /**
     * Execute a Git command and return its output.
     * Throws GradleException if the command fails.
     */
    open fun execute(vararg args: String, trimResult: Boolean = true): String {
        val processBuilder = ProcessBuilder("git", *args)
            .directory(workingDirectory)
            .redirectErrorStream(false)

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            logger.error("Git command failed: git ${args.joinToString(" ")}")
            if (errorOutput.isNotEmpty()) {
                logger.error("Error output: $errorOutput")
            }
            throw GradleException("Git command failed: git ${args.joinToString(" ")}")
        }

        return if (trimResult) {
            output.trim()
        } else {
            output
        }
    }

    /**
     * Execute a Git command and return its output, or null if the command fails.
     * Does not throw exceptions on failure.
     */
    open fun executeOrNull(vararg args: String): String? {
        return try {
            execute(*args).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
