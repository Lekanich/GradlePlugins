package lekanich.common.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream

/**
 * Task to list Git tags matching a pattern.
 *
 * Outputs the list of tags to a file for use by other tasks or build logic.
 *
 * @since 1.1.0
 */
@DisableCachingByDefault(because = "Git tag lists should not be cached")
abstract class GitListTags : Exec() {
    /**
     * Pattern to filter tags (e.g., "v*" for version tags).
     * If not set, all tags are listed.
     */
    @get:Input
    @get:Optional
    abstract val pattern: Property<String>

    /**
     * Output file where the tag list will be written (one tag per line).
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * The list of tags.
     * This property is populated after the task executes and can be used by other tasks.
     */
    @get:Internal
    abstract val tags: Property<String>

    init {
        description = "List Git tags"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        pattern.convention("")
    }

    override fun exec() {
        logger.info("Listing Git tags...")

        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream

        val args = mutableListOf("git", "tag", "-l")
        val patternValue = pattern.get()
        if (patternValue.isNotEmpty()) {
            args.add(patternValue)
            logger.info("Using pattern: $patternValue")
        }

        commandLine(*args.toTypedArray())

        super.exec()

        val tagsValue = outputStream.toString(Charsets.UTF_8.name()).trim()
        val tagCount = if (tagsValue.isEmpty()) 0 else tagsValue.lines().size

        logger.lifecycle("✓ Found $tagCount tag(s)")
        if (tagCount > 0 && logger.isInfoEnabled) {
            tagsValue.lines().take(10).forEach { logger.info("  - $it") }
            if (tagCount > 10) {
                logger.info("  ... and ${tagCount - 10} more")
            }
        }

        // Set the output property for use by other tasks
        tags.set(tagsValue)

        // Write to output file
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(tagsValue)
    }
}
