package lekanich.common.gradle

import org.gradle.api.provider.Property

/**
 * Extension for configuring the Git Tool plugin.
 *
 * Example usage:
 * ```kotlin
 * gitTool {
 *     remoteName.set("origin")
 *     defaultTagMessage.set("Release {tag}")
 *     validateBeforeTag.set(true)
 *     requireCleanWorkspace.set(true)
 *     writeToFile.set(true)
 * }
 * ```
 *
 * @since 1.1.0
 */
abstract class GitToolExtension {

    /**
     * The name of the remote repository to use for push operations.
     * Default: "origin"
     */
    abstract val remoteName: Property<String>

    /**
     * Default message template for creating tags.
     * Use {tag} as a placeholder for the tag name.
     * Default: "Release {tag}"
     */
    abstract val defaultTagMessage: Property<String>

    /**
     * Whether to validate Git status before creating tags.
     * Default: true
     */
    abstract val validateBeforeTag: Property<Boolean>

    /**
     * Whether to require a clean workspace for tag operations.
     * Default: true
     */
    abstract val requireCleanWorkspace: Property<Boolean>

    /**
     * Whether Git info tasks should write their results to files.
     * When false, only output properties are populated (no file I/O).
     * Default: true
     */
    abstract val writeToFile: Property<Boolean>

    init {
        remoteName.convention("origin")
        defaultTagMessage.convention("Release {tag}")
        validateBeforeTag.convention(true)
        requireCleanWorkspace.convention(true)
        writeToFile.convention(true)
    }
}
