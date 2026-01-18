package lekanich.common.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the GitToolPlugin.
 */
class GitToolPluginTest: BaseGitTest() {

    @Test
    fun `plugin registers extension`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        project.pluginManager.apply("io.github.lekanich.git-tool")

        val extension = project.extensions.findByName("gitTool")
        assertNotNull(extension, "Extension 'gitTool' should be registered")
        assertTrue(extension is GitToolExtension, "Extension should be of type GitToolExtension")
    }

    @Test
    fun `plugin registers all tasks`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        project.pluginManager.apply("io.github.lekanich.git-tool")

        assertNotNull(project.tasks.findByName("gitCheckStatus"))
        assertNotNull(project.tasks.findByName("gitCheckTag"))
        assertNotNull(project.tasks.findByName("gitCreateTag"))
        assertNotNull(project.tasks.findByName("gitPushTag"))
        assertNotNull(project.tasks.findByName("gitReleaseTag"))
    }

    @Test
    fun `extension has default values`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        project.pluginManager.apply("io.github.lekanich.git-tool")

        val extension = project.extensions.getByType(GitToolExtension::class.java)

        assertEquals("origin", extension.remoteName.get(), "remoteName should default to 'origin'")
        assertEquals("Release {tag}", extension.defaultTagMessage.get())
        assertTrue(extension.validateBeforeTag.get())
        assertTrue(extension.requireCleanWorkspace.get())
        assertTrue(extension.writeToFile.get())
    }

    @Test
    fun `extension can be configured`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        project.pluginManager.apply("io.github.lekanich.git-tool")

        val extension = project.extensions.getByType(GitToolExtension::class.java)
        extension.remoteName.set("upstream")
        extension.defaultTagMessage.set("Version {tag}")
        extension.validateBeforeTag.set(false)
        extension.requireCleanWorkspace.set(false)
        extension.writeToFile.set(false)

        assertEquals("upstream", extension.remoteName.get())
        assertEquals("Version {tag}", extension.defaultTagMessage.get())
        assertFalse(extension.validateBeforeTag.get())
        assertFalse(extension.requireCleanWorkspace.get())
        assertFalse(extension.writeToFile.get())
    }

    @Test
    fun `tasks are in correct group`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        project.pluginManager.apply("io.github.lekanich.git-tool")

        val gitCheckStatus = project.tasks.getByName("gitCheckStatus")
        val gitCheckTag = project.tasks.getByName("gitCheckTag")
        val gitCreateTag = project.tasks.getByName("gitCreateTag")
        val gitPushTag = project.tasks.getByName("gitPushTag")
        val gitReleaseTag = project.tasks.getByName("gitReleaseTag")

        assertEquals("publishing", gitCheckStatus.group)
        assertEquals("publishing", gitCheckTag.group)
        assertEquals("publishing", gitCreateTag.group)
        assertEquals("publishing", gitPushTag.group)
        assertEquals("publishing", gitReleaseTag.group)
    }

}
