package lekanich.common.gradle

import java.io.ByteArrayOutputStream
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.work.DisableCachingByDefault

/**
 * Task to check if a Git tag already exists locally or remotely.
 *
 * This task fails if the specified tag already exists, preventing duplicate tags.
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Git tag checks should not be cached")
abstract class GitCheckTag : Exec() {
	/**
	 * The name of the tag to check.
	 */
	@get:Input
	abstract val tagName: Property<String>

	/**
	 * The name of the remote repository to check for tags.
	 * Default: "origin"
	 */
	@get:Input
	@get:Optional
	abstract val remoteName: Property<String>

	init {
		description = "Check if git tag exists"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
		isIgnoreExitValue = true
		remoteName.convention("origin")
	}

	override fun exec() {
		val tag = tagName.get()
		logger.info("Checking if Git tag '$tag' exists...")

		// Check local tags
		val outputStream = ByteArrayOutputStream()
		standardOutput = outputStream
		commandLine("git", "tag", "-l", tag)

		super.exec()

		val output = outputStream.toString(Charsets.UTF_8.name()).trim()
		if (output.isNotEmpty()) {
			logger.error("Git tag '$tag' already exists locally")
			throw GradleException("Git tag '$tag' already exists. Please use a different tag name or delete the existing tag.")
		}

		logger.lifecycle("✓ Git tag '$tag' does not exist")
	}
}
