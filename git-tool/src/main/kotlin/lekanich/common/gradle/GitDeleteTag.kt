package lekanich.common.gradle

import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream

/**
 * Task to delete a Git tag locally.
 *
 * This task only deletes the tag from the local repository.
 * To delete from remote, use gitDeleteRemoteTag task.
 *
 * @since 1.2.0
 */
@DisableCachingByDefault(because = "Git tag deletion should not be cached")
abstract class GitDeleteTag : Exec() {
    /**
     * The name of the tag to delete.
     */
    @get:Input
    abstract val tagName: Property<String>

    init {
        description = "Delete Git tag locally"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
    }

    override fun exec() {
        val tag = tagName.get()

        logger.info("Deleting local Git tag '$tag'...")

        // Delete local tag using commandLine + super.exec() as per best practices
        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream
        commandLine("git", "tag", "-d", tag)

        super.exec()

        val output = outputStream.toString(Charsets.UTF_8.name()).trim()
        logger.lifecycle("✓ Deleted local tag: $tag")

        if (output.isNotEmpty() && logger.isInfoEnabled) {
            logger.info(output)
        }
    }
}
