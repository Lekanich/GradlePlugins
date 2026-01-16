package lekanich.common.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.plugins.PublishingPlugin

/**
 * Gradle plugin for Git operations automation.
 *
 * Provides tasks for:
 * - Checking Git repository status
 * - Creating and managing Git tags
 * - Pushing tags to remote repositories
 * - Getting Git information (branch, commit hash, tags)
 * - Complete release workflows
 *
 * @since 1.0.0
 * @year 2026
 */
class GitToolPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create and register extension
        val extension = project.extensions.create("gitTool", GitToolExtension::class.java)

        // Register core tasks
        val gitInstalled = project.tasks.register("gitInstalled", GitInstalled::class.java)
        val gitCheckStatus = project.tasks.register("gitCheckStatus", GitCheckStatus::class.java)
        val gitCheckTag = project.tasks.register("gitCheckTag", GitCheckTag::class.java) {
            remoteName.set(extension.remoteName)
        }
        val gitCreateTag = project.tasks.register("gitCreateTag", GitCreateTag::class.java) {
            tagMessage.set(extension.defaultTagMessage)
        }
        val gitPushTag = project.tasks.register("gitPushTag", GitPushTag::class.java) {
            remoteName.set(extension.remoteName)
        }

        // Register Phase 2 tasks - Git information retrieval
        project.tasks.register("gitGetCurrentBranch", GitGetCurrentBranch::class.java) {
            outputFile.set(project.layout.buildDirectory.file("git/branch.txt"))
            writeToFile.set(extension.writeToFile)
        }
        project.tasks.register("gitGetCommitHash", GitGetCommitHash::class.java) {
            outputFile.set(project.layout.buildDirectory.file("git/commit-hash.txt"))
            writeToFile.set(extension.writeToFile)
        }
        project.tasks.register("gitListTags", GitListTags::class.java) {
            outputFile.set(project.layout.buildDirectory.file("git/tags.txt"))
            writeToFile.set(extension.writeToFile)
        }
        project.tasks.register("gitGetLastTag", GitGetLastTag::class.java) {
            outputFile.set(project.layout.buildDirectory.file("git/last-tag.txt"))
            writeToFile.set(extension.writeToFile)
        }
        project.tasks.register("gitCommitCount", GitCommitCount::class.java) {
            outputFile.set(project.layout.buildDirectory.file("git/commit-count.txt"))
            writeToFile.set(extension.writeToFile)
        }

        // Register  Delete Tag management
        project.tasks.register("gitDeleteTag", GitDeleteTag::class.java)
        project.tasks.register("gitDeleteRemoteTag", GitDeleteRemoteTag::class.java) {
            remoteName.set(extension.remoteName)
        }

        // Register composite release workflow task
        project.tasks.register("gitReleaseTag") {
            description = "Complete Git release workflow: check, create, and push tag"
            group = PublishingPlugin.PUBLISH_TASK_GROUP

            if (extension.requireCleanWorkspace.get()) {
                dependsOn(gitCheckStatus)
            }
            if (extension.validateBeforeTag.get()) {
                dependsOn(gitCheckTag)
            }

            // Always depend on create and push tasks
            dependsOn(gitCreateTag, gitPushTag)
        }

        // Configure task execution order
        gitCheckStatus.configure { mustRunAfter(gitInstalled) }
        gitCheckTag.configure { mustRunAfter(gitCheckStatus) }
        gitCreateTag.configure { mustRunAfter(gitCheckStatus, gitCheckTag) }
        gitPushTag.configure { mustRunAfter(gitCreateTag) }
    }
}
