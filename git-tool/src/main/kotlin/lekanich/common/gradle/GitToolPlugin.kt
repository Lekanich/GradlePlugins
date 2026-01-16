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
 * - Complete release workflows
 *
 * @since 1.0.0
 * @year 2026
 */
class GitToolPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create and register extension
        val extension = project.extensions.create("gitTool", GitToolExtension::class.java)

        // Register tasks
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

        // Register composite release workflow task
        val gitReleaseTag = project.tasks.register("gitReleaseTag") {
            description = "Complete Git release workflow: check, create, and push tag"
            group = PublishingPlugin.PUBLISH_TASK_GROUP

            // Always depend on create and push tasks
            if (extension.requireCleanWorkspace.get()) {
                dependsOn(gitCheckStatus)
            }
            if (extension.validateBeforeTag.get()) {
                dependsOn(gitCheckTag)
            }
            dependsOn(gitCreateTag, gitPushTag)
        }

        // Configure task execution order
        gitCheckStatus.configure { mustRunAfter(gitInstalled) }
        gitCheckTag.configure { mustRunAfter(gitCheckStatus) }
        gitCreateTag.configure { mustRunAfter(gitCheckStatus, gitCheckTag) }
        gitPushTag.configure { mustRunAfter(gitCreateTag) }
    }
}
