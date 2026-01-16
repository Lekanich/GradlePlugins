package lekanich.common.gradle

import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream

/**
 * Task to delete a Git tag from a remote repository.
 *
 * This task deletes the tag from the remote repository only.
 * Use gitDeleteTag to delete the local tag first.
 *
 * @since 1.1.0
 */
@DisableCachingByDefault(because = "Git remote tag deletion should not be cached")
abstract class GitDeleteRemoteTag : Exec() {
    /**
     * The name of the tag to delete from remote.
     */
    @get:Input
    abstract val tagName: Property<String>

    /**
     * The name of the remote repository.
     * Default: "origin"
     */
    @get:Input
    @get:Optional
    abstract val remoteName: Property<String>

    init {
        description = "Delete Git tag from remote repository"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        remoteName.convention("origin")
        isIgnoreExitValue = true // Don't fail if tag doesn't exist on remote
    }

    override fun exec() {
        val tag = tagName.get()
        val remote = remoteName.get()

        logger.info("Deleting tag '$tag' from remote '$remote'...")

        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream
        commandLine("git", "push", remote, "--delete", tag)

        super.exec()

        val exitValue = executionResult.get().exitValue
        if (exitValue == 0) {
            logger.lifecycle("✓ Deleted remote tag: $tag from $remote")
        } else {
            val output = outputStream.toString(Charsets.UTF_8.name()).trim()
            logger.warn("Failed to delete remote tag '$tag' from '$remote'. It may not exist remotely.")
            if (output.isNotEmpty() && logger.isInfoEnabled) {
                logger.info(output)
            }
        }
    }
}
