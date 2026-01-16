package lekanich.common.gradle

import org.gradle.api.GradleException
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream

/**
 * @year 2026
 */
@DisableCachingByDefault(because = "No need")
abstract class GitInstalled : Exec() {
    init {
        description = "Check that git is installed"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
    }

    override fun exec() {
        logger.info("Checking Git working directory status...")

        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream
        isIgnoreExitValue = true
        commandLine("git", "--version", "--porcelain")

        try {
            super.exec()

            if (executionResult.get().exitValue != 0) {
                throw GradleException("Git command failed. Please ensure Git is properly installed and available in PATH.")
            }
        } catch (e: Exception) {
            throw GradleException(
                "Git is not available on this system. Please install Git from https://git-scm.com/downloads",
                e
            )
        }

        logger.lifecycle("✓ Git is installed")
    }
}