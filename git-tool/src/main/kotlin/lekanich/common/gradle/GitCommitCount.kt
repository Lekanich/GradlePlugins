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
 * Task to get the number of Git commits.
 *
 * Can count commits since a specific commit, tag, or branch.
 * Useful for generating build numbers.
 *
 * @since 1.1.0
 */
@DisableCachingByDefault(because = "Git commit counts should not be cached")
abstract class GitCommitCount : Exec() {
    /**
     * Starting point for counting commits (commit hash, tag, or branch).
     * If not set, counts all commits in current branch.
     */
    @get:Input
    @get:Optional
    abstract val since: Property<String>

    /**
     * Output file where the commit count will be written.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * The commit count result.
     * This property is populated after the task executes and can be used by other tasks.
     */
    @get:Internal
    abstract val commitCount: Property<String>

    init {
        description = "Get number of Git commits"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        since.convention("")
    }

    override fun exec() {
        logger.info("Counting Git commits...")

        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream

        val args = mutableListOf("git", "rev-list", "--count")
        val sinceValue = since.get()
        if (sinceValue.isNotEmpty()) {
            args.add("$sinceValue..HEAD")
            logger.info("Counting commits since: $sinceValue")
        } else {
            args.add("HEAD")
        }

        commandLine(*args.toTypedArray())

        super.exec()

        val count = outputStream.toString(Charsets.UTF_8.name()).trim()

        if (sinceValue.isNotEmpty()) {
            logger.lifecycle("✓ Commits since $sinceValue: $count")
        } else {
            logger.lifecycle("✓ Total commits: $count")
        }

        // Set the output property for use by other tasks
        commitCount.set(count)

        // Write to output file
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(count)
    }
}
