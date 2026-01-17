package lekanich.common.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Task for complete Git release workflow.
 *
 * This task orchestrates the full release process:
 * - Optionally checks Git repository status (if requireCleanWorkspace is enabled)
 * - Optionally validates the tag doesn't already exist (if validateBeforeTag is enabled)
 * - Creates the Git tag
 * - Pushes the tag to remote repository
 *
 * This is a convenience task that depends on other tasks to perform the workflow.
 *
 * @since 1.1.0
 */
@DisableCachingByDefault(because = "Release workflow should not be cached")
abstract class GitReleaseTag : DefaultTask() {
	/**
	 * The name of the tag to create and push.
	 */
	@get:Input
	abstract val tagName: Property<String>

	/**
	 * Whether to require a clean workspace before creating the tag.
	 * Default: true
	 */
	@get:Input
	abstract val requireCleanWorkspace: Property<Boolean>

	/**
	 * Whether to validate that the tag doesn't already exist before creating it.
	 * Default: true
	 */
	@get:Input
	abstract val validateBeforeTag: Property<Boolean>

	init {
		description = "Complete Git release workflow: check, create, and push tag"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
		requireCleanWorkspace.convention(true)
		validateBeforeTag.convention(true)

		// Wire tagName to dependent tasks
		project.tasks.named("gitCheckTag", GitCheckTag::class.java) {
			tagName.set(this@GitReleaseTag.tagName)
		}
		project.tasks.named("gitCreateTag", GitCreateTag::class.java) {
			tagName.set(this@GitReleaseTag.tagName)
		}
		project.tasks.named("gitPushTag", GitPushTag::class.java) {
			tagName.set(this@GitReleaseTag.tagName)
		}

		if (requireCleanWorkspace.get()) {
			dependsOn("gitCheckStatus")
		}
		if (validateBeforeTag.get()) {
			dependsOn("gitCheckTag")
		}
		// Always depend on create and push tasks
		dependsOn("gitCreateTag", "gitPushTag")
	}

	@TaskAction
	fun execute() {
		// This task's actual work is done via task dependencies
		// The task action is just for logging
		val tag = tagName.get()
		logger.lifecycle("✓ Git release workflow completed successfully for tag: $tag")
	}
}
