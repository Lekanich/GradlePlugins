package lekanich.common.gradle

import java.io.ByteArrayOutputStream
import org.gradle.api.GradleException
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.work.DisableCachingByDefault

/**
 * Task to check if the Git working directory is clean (no uncommitted changes).
 *
 * This task fails if there are any uncommitted changes in the working directory.
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Git status checks should not be cached")
abstract class GitCheckStatus : Exec() {
	init {
		description = "Check git working directory status"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
		mustRunAfter("gitInstalled")
	}

	override fun exec() {
		logger.info("Checking Git working directory status...")

		val outputStream = ByteArrayOutputStream()
		standardOutput = outputStream
		commandLine("git", "status", "--porcelain")

		super.exec()

		val output = outputStream.toString(Charsets.UTF_8.name()).trim()
		if (output.isNotEmpty()) {
			logger.error("Git workspace has uncommitted changes:")
			logger.error(output)
			throw GradleException("Workspace is dirty. Please commit or stash changes before proceeding.")
		}

		logger.lifecycle("✓ Git workspace is clean")
	}
}
