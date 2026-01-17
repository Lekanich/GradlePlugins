package lekanich.common.gradle

import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.work.DisableCachingByDefault

/**
 * Task to create an annotated Git tag.
 *
 * Creates a new annotated tag with the specified name and message.
 *
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Git tag creation should not be cached")
abstract class GitCreateTag : Exec() {
	/**
	 * The name of the tag to create.
	 */
	@get:Input
	abstract val tagName: Property<String>

	/**
	 * The message to use when creating the tag.
	 * Use {tag} as a placeholder for the tag name.
	 * Default: "Release {tag}"
	 */
	@get:Input
	@get:Optional
	abstract val tagMessage: Property<String>

	init {
		description = "Create git tag"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
		tagMessage.convention("Release {tag}")
		mustRunAfter("gitCheckStatus", "gitCheckTag")
	}

	override fun exec() {
		val tag = tagName.get()
		val message = tagMessage.get().replace("{tag}", tag)

		logger.info("Creating Git tag '$tag' with message: $message")

		commandLine("git", "tag", "-a", tag, "-m", message)
		super.exec()

		logger.lifecycle("✓ Created Git tag: $tag")
	}
}
