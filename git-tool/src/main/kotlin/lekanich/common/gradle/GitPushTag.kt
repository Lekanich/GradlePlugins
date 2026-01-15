package lekanich.common.gradle

import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "No need to cache")
abstract class GitPushTag : Exec() {
	@get:Input
	abstract val tagName: Property<String>

	init {
		description = "Push git tag to origin"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
	}

	override fun exec() {
		commandLine("git", "push", "origin", "refs/tags/${tagName.get()}")
		super.exec()
		println("Pushed tag ${tagName.get()} to origin")
	}
}
