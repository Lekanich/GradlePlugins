package lekanich.common.gradle

import java.io.ByteArrayOutputStream
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "No need to cache")
abstract class GitCheckStatus : Exec() {
	init {
		description = "Check git working directory status"
		group = PublishingPlugin.PUBLISH_TASK_GROUP
	}

	override fun exec() {
		commandLine("git", "status", "--porcelain")
		standardOutput = ByteArrayOutputStream()

		super.exec()

		val output = standardOutput.toString().trim()
		check(output.isEmpty()) {
			"Workspace is dirty. Please commit or stash changes:\n$output"
		}
		println("Git workspace is clean")
	}
}
