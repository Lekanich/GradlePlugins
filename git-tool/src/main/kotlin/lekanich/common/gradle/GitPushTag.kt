package lekanich.common.gradle

import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.work.DisableCachingByDefault

/**
 * Task to push a Git tag to a remote repository.
 *
 * Pushes the specified tag to the configured remote repository.
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Git push operations should not be cached")
abstract class GitPushTag : Exec() {
	/**
	 * The name of the tag to push.
	 */
	@get:Input
	abstract val tagName: Property<String>

	/**
	 * The name of the remote repository to push to.
	 * Default: "origin"
	 */
	@get:Input
	@get:Optional
	abstract val remoteName: Property<String>

	init {
		description = "Push git tag to remote"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
		remoteName.convention("origin")
	}

	override fun exec() {
		val tag = tagName.get()
		val remote = remoteName.get()

		logger.info("Pushing Git tag '$tag' to remote '$remote'...")

		commandLine("git", "push", remote, "refs/tags/$tag")
		super.exec()

		logger.lifecycle("✓ Pushed tag '$tag' to remote '$remote'")
	}
}
