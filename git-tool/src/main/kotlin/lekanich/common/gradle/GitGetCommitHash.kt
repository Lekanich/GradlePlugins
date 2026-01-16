package lekanich.common.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream

/**
 * Task to get the current Git commit hash.
 *
 * Outputs the commit hash to a file for use by other tasks or build logic.
 *
 * @since 1.1.0
 */
@DisableCachingByDefault(because = "Git commit hash checks should not be cached")
abstract class GitGetCommitHash : Exec() {
    /**
     * Whether to use short hash (7 characters) instead of full hash.
     * Default: false
     */
    @get:Input
    @get:Optional
    abstract val shortHash: Property<Boolean>

    /**
     * Output file where the commit hash will be written.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        description = "Get current Git commit hash"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        shortHash.convention(false)
    }

    override fun exec() {
        logger.info("Getting current Git commit hash...")

        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream

        val args = mutableListOf("git", "rev-parse")
        if (shortHash.get()) {
            args.add("--short")
        }
        args.add("HEAD")

        commandLine(*args.toTypedArray())

        super.exec()

        val commitHash = outputStream.toString(Charsets.UTF_8.name()).trim()

        logger.lifecycle("✓ Current commit: $commitHash")

        // Write to output file
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(commitHash)
    }
}
