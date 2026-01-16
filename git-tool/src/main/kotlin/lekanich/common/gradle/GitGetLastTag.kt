package lekanich.common.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.OutputFile
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream

/**
 * Task to get the most recent Git tag.
 *
 * Outputs the last tag name to a file for use by other tasks or build logic.
 * Useful for changelog generation or version bumping.
 *
 * @since 1.1.0
 */
@DisableCachingByDefault(because = "Git tag checks should not be cached")
abstract class GitGetLastTag : Exec() {
    /**
     * Output file where the last tag name will be written.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        description = "Get most recent Git tag"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        isIgnoreExitValue = true
    }

    override fun exec() {
        logger.info("Getting most recent Git tag...")

        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream
        commandLine("git", "describe", "--tags", "--abbrev=0")

        super.exec()

        val lastTag = outputStream.toString(Charsets.UTF_8.name()).trim()

        if (lastTag.isEmpty()) {
            logger.warn("No tags found in repository")
        } else {
            logger.lifecycle("✓ Most recent tag: $lastTag")
        }

        // Write to output file
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(lastTag)
    }
}
