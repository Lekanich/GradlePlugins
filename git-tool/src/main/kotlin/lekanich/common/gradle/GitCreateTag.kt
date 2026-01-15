package lekanich.common.gradle

import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "No need to cache")
abstract class GitCreateTag : Exec() {
	@get:Input
	abstract val tagName: Property<String>

	init {
		description = "Create git tag"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
	}

	override fun exec() {
		commandLine("git", "tag", "-a", tagName.get(), "-m", "Gradle created tag for ${tagName.get()}")
		super.exec()
		println("Created git tag: ${tagName.get()}")
	}
}
