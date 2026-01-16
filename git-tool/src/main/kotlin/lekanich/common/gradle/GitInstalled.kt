package lekanich.common.gradle

import org.gradle.api.GradleException
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream

/**
 * Task to verify that Git is installed and available on the system.
 *
 * This task checks if Git is properly installed and accessible from the PATH.
 * It executes `git --version` and validates the command succeeds.
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Git availability check should not be cached")
abstract class GitInstalled : Exec() {
    init {
        description = "Verify that Git is installed and available"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
    }

    override fun exec() {
        logger.info("Checking if Git is installed...")

        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream
        isIgnoreExitValue = true
        commandLine("git", "--version")

        try {
            super.exec()

            val exitValue = executionResult.get().exitValue
            if (exitValue != 0) {
                throw GradleException(
                    "Git command failed with exit code $exitValue. " +
                    "Please ensure Git is properly installed and available in PATH."
                )
            }

            val gitVersion = outputStream.toString(Charsets.UTF_8.name()).trim()
            logger.lifecycle("✓ Git is installed: $gitVersion")
        } catch (e: Exception) {
            throw GradleException(
                "Git is not available on this system. " +
                "Please install Git from https://git-scm.com/downloads and ensure it's in your PATH.",
                e
            )
        }
    }
}