package lekanich.common.gradle

import java.io.ByteArrayOutputStream
import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "No need to cache")
abstract class GitCheckTag : Exec() {
	@get:Input
	abstract val tagName: Property<String>

	init {
		description = "Check git tag if exists"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
		isIgnoreExitValue = true
	}

	override fun exec() {
		standardOutput = ByteArrayOutputStream()
		commandLine("git", "tag", "-l", tagName.get())

		super.exec()

		val output = standardOutput.toString().trim()
		check(output.isEmpty()) {
			"Git tag ${tagName.get()} already exists\n$output"
		}
	}
}
