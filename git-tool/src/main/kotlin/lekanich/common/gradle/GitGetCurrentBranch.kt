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
 * Task to get the current Git branch name.
 *
 * Outputs the current branch name to a file for use by other tasks or build logic.
 *
 * @since 1.1.0
 */
@DisableCachingByDefault(because = "Git branch checks should not be cached")
abstract class GitGetCurrentBranch : Exec() {
    /**
     * Output file where the branch name will be written.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * The current branch name.
     * This property is populated after the task executes and can be used by other tasks.
     */
    @get:Internal
    abstract val branchName: Property<String>

    /**
     * Whether to write the result to a file.
     * When false, only the branchName property is populated (no file I/O).
     * Default: true
     */
    @get:Input
    @get:Optional
    abstract val writeToFile: Property<Boolean>

    init {
        description = "Get current Git branch name"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        writeToFile.convention(true)
    }

    override fun exec() {
        logger.info("Getting current Git branch name...")

        val outputStream = ByteArrayOutputStream()
        standardOutput = outputStream
        commandLine("git", "branch", "--show-current")

        super.exec()

        val branchNameValue = outputStream.toString(Charsets.UTF_8.name()).trim()

        if (branchNameValue.isEmpty()) {
            logger.warn("No branch name found (possibly detached HEAD state)")
        } else {
            logger.lifecycle("✓ Current branch: $branchNameValue")
        }

        // Set the output property for use by other tasks
        branchName.set(branchNameValue)

        // Write to output file only if enabled
        if (writeToFile.get()) {
            val output = outputFile.get().asFile
            output.parentFile.mkdirs()
            output.writeText(branchNameValue)
        }
    }
}
