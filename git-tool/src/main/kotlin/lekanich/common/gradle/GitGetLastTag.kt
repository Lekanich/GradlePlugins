package lekanich.common.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Internal
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

    /**
     * The last tag name.
     * This property is populated after the task executes and can be used by other tasks.
     */
    @get:Internal
    abstract val lastTag: Property<String>

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

        val lastTagValue = outputStream.toString(Charsets.UTF_8.name()).trim()

        if (lastTagValue.isEmpty()) {
            logger.warn("No tags found in repository")
        } else {
            logger.lifecycle("✓ Most recent tag: $lastTagValue")
        }

        // Set the output property for use by other tasks
        lastTag.set(lastTagValue)

        // Write to output file
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(lastTagValue)
    }
}
