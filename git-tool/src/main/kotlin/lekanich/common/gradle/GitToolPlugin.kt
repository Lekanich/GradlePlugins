package lekanich.common.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @year 2026
 */
class GitToolPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("gitCheckStatus", GitCheckStatus::class.java)
        project.tasks.register("gitCheckTag", GitCheckTag::class.java)
        project.tasks.register("gitCreateTag", GitCreateTag::class.java)
        project.tasks.register("gitPushTag", GitPushTag::class.java)
    }
}
