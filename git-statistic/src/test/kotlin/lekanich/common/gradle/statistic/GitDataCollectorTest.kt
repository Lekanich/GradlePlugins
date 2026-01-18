package lekanich.common.gradle.statistic

import org.gradle.api.logging.Logging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.io.File
import java.time.Instant

class GitDataCollectorTest {

    private lateinit var mockExecutor: MockGitCommandExecutor
    private lateinit var collector: GitDataCollector

    @BeforeEach
    fun setup() {
        mockExecutor = MockGitCommandExecutor()
        collector = GitDataCollector(mockExecutor)
    }

    @Nested
    @DisplayName("collectGitStatus tests")
    inner class CollectGitStatusTests {

        @Test
        fun `should collect complete git status with all sections`() {
            setupDefaultMockResponses()

            val status = collector.collectGitStatus(includeRemoteStatus = true, includeFileChanges = true)

            assertNotNull(status)
            assertNotNull(status.repository)
            assertNotNull(status.commit)
            assertNotNull(status.workingDirectory)
            assertNotNull(status.tags)
            assertNotNull(status.remote)
            assertTrue(status.reportGeneratedAt.isBefore(Instant.now().plusSeconds(1)))
        }

        @Test
        fun `should collect git status without remote info when disabled`() {
            setupDefaultMockResponses()

            val status = collector.collectGitStatus(includeRemoteStatus = false, includeFileChanges = true)

            assertNull(status.remote)
            assertNotNull(status.repository)
            assertNotNull(status.commit)
        }

        @Test
        fun `should collect git status without file changes when disabled`() {
            setupDefaultMockResponses()
            mockExecutor.addResponse("status", "--porcelain", response = " M file1.txt\n?? file2.txt\nA  file3.txt")

            val status = collector.collectGitStatus(includeRemoteStatus = true, includeFileChanges = false)

            assertNotNull(status.workingDirectory)
            assertFalse(status.workingDirectory.isClean)
            assertTrue(status.workingDirectory.modified.isEmpty())
            assertTrue(status.workingDirectory.untracked.isEmpty())
            assertTrue(status.workingDirectory.staged.isEmpty())
        }
    }

    @Nested
    @DisplayName("collectRepositoryInfo tests")
    inner class CollectRepositoryInfoTests {

        @Test
        fun `should collect repository info with upstream branch`() {
            mockExecutor.addResponse("rev-parse", "--abbrev-ref", "HEAD", response = "main")
            mockExecutor.addResponse("rev-parse", "--abbrev-ref", "@{upstream}", response = "origin/main")
            mockExecutor.addResponse("rev-list", "--count", "origin/main..HEAD", response = "3")
            mockExecutor.addResponse("rev-list", "--count", "HEAD..origin/main", response = "1")

            val info = collector.collectRepositoryInfo()

            assertEquals("main", info.currentBranch)
            assertEquals("origin/main", info.upstreamBranch)
            assertNotNull(info.branchStatus)
            assertEquals(3, info.branchStatus?.ahead)
            assertEquals(1, info.branchStatus?.behind)
        }

        @Test
        fun `should collect repository info without upstream branch`() {
            mockExecutor.addResponse("rev-parse", "--abbrev-ref", "HEAD", response = "feature-branch")
            mockExecutor.addResponse("rev-parse", "--abbrev-ref", "@{upstream}", response = null)

            val info = collector.collectRepositoryInfo()

            assertEquals("feature-branch", info.currentBranch)
            assertNull(info.upstreamBranch)
            assertNull(info.branchStatus)
        }

        @Test
        fun `should handle detached HEAD state`() {
            mockExecutor.addResponse("rev-parse", "--abbrev-ref", "HEAD", response = "HEAD")
            mockExecutor.addResponse("rev-parse", "--abbrev-ref", "@{upstream}", response = null)

            val info = collector.collectRepositoryInfo()

            assertEquals("HEAD", info.currentBranch)
            assertNull(info.upstreamBranch)
        }

        @Test
        fun `should handle invalid ahead behind count`() {
            mockExecutor.addResponse("rev-parse", "--abbrev-ref", "HEAD", response = "main")
            mockExecutor.addResponse("rev-parse", "--abbrev-ref", "@{upstream}", response = "origin/main")
            mockExecutor.addResponse("rev-list", "--count", "origin/main..HEAD", response = "invalid")
            mockExecutor.addResponse("rev-list", "--count", "HEAD..origin/main", response = "invalid")

            val info = collector.collectRepositoryInfo()

            assertEquals("main", info.currentBranch)
            assertEquals("origin/main", info.upstreamBranch)
            assertNotNull(info.branchStatus)
            assertEquals(0, info.branchStatus?.ahead)
            assertEquals(0, info.branchStatus?.behind)
        }
    }

    @Nested
    @DisplayName("collectCommitInfo tests")
    inner class CollectCommitInfoTests {

        @Test
        fun `should collect complete commit info`() {
            mockExecutor.addResponse("rev-parse", "HEAD", response = "abc123def456789012345678901234567890abcd")
            mockExecutor.addResponse("rev-parse", "--short", "HEAD", response = "abc123d")
            mockExecutor.addResponse("log", "-1", "--pretty=%s", response = "Initial commit")
            mockExecutor.addResponse("log", "-1", "--pretty=%an", response = "John Doe")
            mockExecutor.addResponse("log", "-1", "--pretty=%cI", response = "2024-01-15T10:30:00Z")
            mockExecutor.addResponse("rev-list", "--count", "HEAD", response = "42")

            val info = collector.collectCommitInfo()

            assertEquals("abc123def456789012345678901234567890abcd", info.hash)
            assertEquals("abc123d", info.shortHash)
            assertEquals("Initial commit", info.message)
            assertEquals("John Doe", info.author)
            assertEquals(Instant.parse("2024-01-15T10:30:00Z"), info.date)
            assertEquals(42, info.totalCount)
        }

        @Test
        fun `should handle commit with multiline message`() {
            mockExecutor.addResponse("rev-parse", "HEAD", response = "abc123def456789012345678901234567890abcd")
            mockExecutor.addResponse("rev-parse", "--short", "HEAD", response = "abc123d")
            mockExecutor.addResponse("log", "-1", "--pretty=%s", response = "Fix bug in collector")
            mockExecutor.addResponse("log", "-1", "--pretty=%an", response = "Jane Smith")
            mockExecutor.addResponse("log", "-1", "--pretty=%cI", response = "2024-01-16T14:20:00Z")
            mockExecutor.addResponse("rev-list", "--count", "HEAD", response = "100")

            val info = collector.collectCommitInfo()

            assertEquals("Fix bug in collector", info.message)
            assertEquals("Jane Smith", info.author)
            assertEquals(100, info.totalCount)
        }

        @Test
        fun `should handle invalid total count`() {
            mockExecutor.addResponse("rev-parse", "HEAD", response = "abc123def456789012345678901234567890abcd")
            mockExecutor.addResponse("rev-parse", "--short", "HEAD", response = "abc123d")
            mockExecutor.addResponse("log", "-1", "--pretty=%s", response = "Test commit")
            mockExecutor.addResponse("log", "-1", "--pretty=%an", response = "Test Author")
            mockExecutor.addResponse("log", "-1", "--pretty=%cI", response = "2024-01-17T12:00:00Z")
            mockExecutor.addResponse("rev-list", "--count", "HEAD", response = "invalid")

            val info = collector.collectCommitInfo()

            assertEquals(0, info.totalCount)
        }
    }

    @Nested
    @DisplayName("collectWorkingDirectoryInfo tests")
    inner class CollectWorkingDirectoryInfoTests {

        @Test
        fun `should detect clean working directory`() {
            mockExecutor.addResponse("status", "--porcelain", response = "")

            val info = collector.collectWorkingDirectoryInfo()

            assertTrue(info.isClean)
            assertTrue(info.modified.isEmpty())
            assertTrue(info.untracked.isEmpty())
            assertTrue(info.staged.isEmpty())
            assertTrue(info.conflicts.isEmpty())
        }

        @Test
        fun `should detect modified files`() {
            mockExecutor.addResponse("status", "--porcelain", response = " M file1.txt\n M file2.kt")

            val info = collector.collectWorkingDirectoryInfo()

            assertFalse(info.isClean)
            assertEquals(listOf("file1.txt", "file2.kt"), info.modified)
            assertTrue(info.untracked.isEmpty())
            assertTrue(info.staged.isEmpty())
            assertTrue(info.conflicts.isEmpty())
        }

        @Test
        fun `should detect untracked files`() {
            mockExecutor.addResponse("status", "--porcelain", response = "?? newfile.txt\n?? another.kt")

            val info = collector.collectWorkingDirectoryInfo()

            assertFalse(info.isClean)
            assertTrue(info.modified.isEmpty())
            assertEquals(listOf("newfile.txt", "another.kt"), info.untracked)
            assertTrue(info.staged.isEmpty())
            assertTrue(info.conflicts.isEmpty())
        }

        @Test
        fun `should detect staged files`() {
            mockExecutor.addResponse("status", "--porcelain", response = "A  newfile.txt\nM  modified.kt")

            val info = collector.collectWorkingDirectoryInfo()

            assertFalse(info.isClean)
            assertTrue(info.modified.isEmpty())
            assertTrue(info.untracked.isEmpty())
            assertEquals(listOf("newfile.txt", "modified.kt"), info.staged)
            assertTrue(info.conflicts.isEmpty())
        }

        @Test
        fun `should detect conflict files`() {
            mockExecutor.addResponse(
                "status",
                "--porcelain",
                response = "UU conflict1.txt\nDD conflict2.kt\nAA conflict3.java"
            )

            val info = collector.collectWorkingDirectoryInfo()

            assertFalse(info.isClean)
            assertTrue(info.modified.isEmpty())
            assertTrue(info.untracked.isEmpty())
            assertTrue(info.staged.isEmpty())
            assertEquals(listOf("conflict1.txt", "conflict2.kt", "conflict3.java"), info.conflicts)
        }

        @Test
        fun `should handle mixed file statuses`() {
            mockExecutor.addResponse(
                "status", "--porcelain",
                response = " M modified.txt\n?? untracked.txt\nA  staged.txt\nUU conflict.txt\n M another.kt"
            )

            val info = collector.collectWorkingDirectoryInfo()

            assertFalse(info.isClean)
            assertEquals(listOf("modified.txt", "another.kt"), info.modified)
            assertEquals(listOf("untracked.txt"), info.untracked)
            assertEquals(listOf("staged.txt"), info.staged)
            assertEquals(listOf("conflict.txt"), info.conflicts)
        }

        @Test
        fun `should return empty lists when includeFileChanges is false`() {
            mockExecutor.addResponse("status", "--porcelain", response = " M file1.txt\n?? file2.txt\nA  file3.txt")

            val info = collector.collectWorkingDirectoryInfo(includeFileChanges = false)

            assertFalse(info.isClean)
            assertTrue(info.modified.isEmpty())
            assertTrue(info.untracked.isEmpty())
            assertTrue(info.staged.isEmpty())
            assertTrue(info.conflicts.isEmpty())
        }

        @Test
        fun `should handle deleted files`() {
            mockExecutor.addResponse("status", "--porcelain", response = " D deleted.txt")

            val info = collector.collectWorkingDirectoryInfo()

            assertFalse(info.isClean)
            assertEquals(listOf("deleted.txt"), info.modified)
        }
    }

    @Nested
    @DisplayName("collectTagInfo tests")
    inner class CollectTagInfoTests {

        @Test
        fun `should collect tag info with tags`() {
            mockExecutor.addResponse("tag", "--sort=-version:refname", response = "v2.0.0\nv1.5.0\nv1.0.0")
            mockExecutor.addResponse("rev-list", "--count", "v2.0.0..HEAD", response = "5")

            val info = collector.collectTagInfo()

            assertEquals("v2.0.0", info.latest)
            assertEquals(5, info.commitsSinceLatest)
            assertEquals(listOf("v2.0.0", "v1.5.0", "v1.0.0"), info.all)
        }

        @Test
        fun `should handle no tags`() {
            mockExecutor.addResponse("tag", "--sort=-version:refname", response = "")

            val info = collector.collectTagInfo()

            assertNull(info.latest)
            assertEquals(0, info.commitsSinceLatest)
            assertTrue(info.all.isEmpty())
        }

        @Test
        fun `should handle single tag`() {
            mockExecutor.addResponse("tag", "--sort=-version:refname", response = "v1.0.0")
            mockExecutor.addResponse("rev-list", "--count", "v1.0.0..HEAD", response = "0")

            val info = collector.collectTagInfo()

            assertEquals("v1.0.0", info.latest)
            assertEquals(0, info.commitsSinceLatest)
            assertEquals(listOf("v1.0.0"), info.all)
        }

        @Test
        fun `should handle invalid commits count`() {
            mockExecutor.addResponse("tag", "--sort=-version:refname", response = "v1.0.0")
            mockExecutor.addResponse("rev-list", "--count", "v1.0.0..HEAD", response = "invalid")

            val info = collector.collectTagInfo()

            assertEquals("v1.0.0", info.latest)
            assertEquals(0, info.commitsSinceLatest)
        }

        @Test
        fun `should filter blank lines in tag list`() {
            mockExecutor.addResponse("tag", "--sort=-version:refname", response = "v2.0.0\n\nv1.5.0\n\n\nv1.0.0\n")
            mockExecutor.addResponse("rev-list", "--count", "v2.0.0..HEAD", response = "3")

            val info = collector.collectTagInfo()

            assertEquals(listOf("v2.0.0", "v1.5.0", "v1.0.0"), info.all)
        }
    }

    @Nested
    @DisplayName("collectRemoteInfo tests")
    inner class CollectRemoteInfoTests {

        @Test
        fun `should collect remote info when remote exists and is reachable`() {
            mockExecutor.addResponse("remote", response = "origin")
            mockExecutor.addResponse("remote", "get-url", "origin", response = "https://github.com/user/repo.git")
            mockExecutor.addResponse("ls-remote", "--exit-code", "origin", response = "some-refs")

            val info = collector.collectRemoteInfo()

            assertNotNull(info)
            assertEquals("origin", info?.name)
            assertEquals("https://github.com/user/repo.git", info?.url)
            assertTrue(info?.reachable ?: false)
        }

        @Test
        fun `should detect unreachable remote`() {
            mockExecutor.addResponse("remote", response = "origin")
            mockExecutor.addResponse("remote", "get-url", "origin", response = "https://github.com/user/repo.git")
            mockExecutor.addResponseWithException("ls-remote", "--exit-code", "origin")

            val info = collector.collectRemoteInfo()

            assertNotNull(info)
            assertEquals("origin", info?.name)
            assertEquals("https://github.com/user/repo.git", info?.url)
            assertFalse(info?.reachable ?: true)
        }

        @Test
        fun `should return null when no remote exists`() {
            mockExecutor.addResponse("remote", response = null)

            val info = collector.collectRemoteInfo()

            assertNull(info)
        }

        @Test
        fun `should return null when remote url is not available`() {
            mockExecutor.addResponse("remote", response = "origin")
            mockExecutor.addResponse("remote", "get-url", "origin", response = null)

            val info = collector.collectRemoteInfo()

            assertNull(info)
        }

        @Test
        fun `should handle SSH remote URL`() {
            mockExecutor.addResponse("remote", response = "origin")
            mockExecutor.addResponse("remote", "get-url", "origin", response = "git@github.com:user/repo.git")
            mockExecutor.addResponse("ls-remote", "--exit-code", "origin", response = "refs")

            val info = collector.collectRemoteInfo()

            assertNotNull(info)
            assertEquals("git@github.com:user/repo.git", info?.url)
        }
    }

    @Nested
    @DisplayName("collectCommitHistory tests")
    inner class CollectCommitHistoryTests {

        @Test
        fun `should collect commit history`() {
            val gitLogOutput = """
                abc123|abc123|John Doe|john@example.com|2024-01-15T10:00:00Z|Initial commit
                def456|def456|Jane Smith|jane@example.com|2024-01-16T11:00:00Z|Add feature
                ghi789|ghi789|Bob Johnson|bob@example.com|2024-01-17T12:00:00Z|Fix bug
            """.trimIndent()

            mockExecutor.addResponse("log", "-10", "--pretty=format:%H|%h|%an|%ae|%cI|%s", response = gitLogOutput)

            val history = collector.collectCommitHistory(10)

            assertEquals(3, history.size)
            assertEquals("abc123", history[0].hash)
            assertEquals("abc123", history[0].shortHash)
            assertEquals("John Doe", history[0].author)
            assertEquals("john@example.com", history[0].authorEmail)
            assertEquals(Instant.parse("2024-01-15T10:00:00Z"), history[0].date)
            assertEquals("Initial commit", history[0].message)

            assertEquals("Jane Smith", history[1].author)
            assertEquals("Add feature", history[1].message)

            assertEquals("Bob Johnson", history[2].author)
            assertEquals("Fix bug", history[2].message)
        }

        @Test
        fun `should handle empty commit history`() {
            mockExecutor.addResponse("log", "-5", "--pretty=format:%H|%h|%an|%ae|%cI|%s", response = "")

            val history = collector.collectCommitHistory(5)

            assertTrue(history.isEmpty())
        }

        @Test
        fun `should filter blank lines in commit history`() {
            val gitLogOutput = """
                abc123|abc123|John Doe|john@example.com|2024-01-15T10:00:00Z|Initial commit
                
                def456|def456|Jane Smith|jane@example.com|2024-01-16T11:00:00Z|Add feature
                
            """.trimIndent()

            mockExecutor.addResponse("log", "-2", "--pretty=format:%H|%h|%an|%ae|%cI|%s", response = gitLogOutput)

            val history = collector.collectCommitHistory(2)

            assertEquals(2, history.size)
        }

        @Test
        fun `should handle commit messages with special characters`() {
            val gitLogOutput =
                "abc123|abc123|John Doe|john@example.com|2024-01-15T10:00:00Z|Fix: issue #123 - Handle | pipe character"

            mockExecutor.addResponse("log", "-1", "--pretty=format:%H|%h|%an|%ae|%cI|%s", response = gitLogOutput)

            val history = collector.collectCommitHistory(1)

            assertEquals(1, history.size)
            assertEquals("Fix: issue #123 - Handle | pipe character", history[0].message)
        }
    }

    @Nested
    @DisplayName("collectContributorStats tests")
    inner class CollectContributorStatsTests {

        @Test
        fun `should collect contributor statistics`() {
            val shortlogOutput = """
                   42	John Doe
                   25	Jane Smith
                   10	Bob Johnson
            """.trimIndent()

            mockExecutor.addResponse("shortlog", "-sn", "--all", "--no-merges", response = shortlogOutput)

            val stats = collector.collectContributorStats()

            assertEquals(3, stats.size)
            assertEquals(42, stats["John Doe"])
            assertEquals(25, stats["Jane Smith"])
            assertEquals(10, stats["Bob Johnson"])
        }

        @Test
        fun `should handle single contributor`() {
            mockExecutor.addResponse("shortlog", "-sn", "--all", "--no-merges", response = "   100\tSolo Developer")

            val stats = collector.collectContributorStats()

            assertEquals(1, stats.size)
            assertEquals(100, stats["Solo Developer"])
        }

        @Test
        fun `should handle no contributors`() {
            mockExecutor.addResponse("shortlog", "-sn", "--all", "--no-merges", response = "")

            val stats = collector.collectContributorStats()

            assertTrue(stats.isEmpty())
        }

        @Test
        fun `should filter blank lines`() {
            val shortlogOutput = """
                   50	John Doe
                   
                   30	Jane Smith
                   
            """.trimIndent()

            mockExecutor.addResponse("shortlog", "-sn", "--all", "--no-merges", response = shortlogOutput)

            val stats = collector.collectContributorStats()

            assertEquals(2, stats.size)
        }
    }

    @Nested
    @DisplayName("collectFileChangeStats tests")
    inner class CollectFileChangeStatsTests {

        @Test
        fun `should collect file change statistics`() {
            val logOutput = """
                src/main/File1.kt
                src/main/File2.kt
                src/main/File1.kt
                src/test/Test1.kt
                src/main/File1.kt
                src/main/File2.kt
                README.md
            """.trimIndent()

            mockExecutor.addResponse("log", "--all", "--pretty=format:", "--name-only", response = logOutput)

            val stats = collector.collectFileChangeStats(limit = 20)

            assertEquals(3, stats["src/main/File1.kt"])
            assertEquals(2, stats["src/main/File2.kt"])
            assertEquals(1, stats["src/test/Test1.kt"])
            assertEquals(1, stats["README.md"])
        }

        @Test
        fun `should limit results to specified count`() {
            val files = (1..30).joinToString("\n") { "file$it.txt" }
            mockExecutor.addResponse("log", "--all", "--pretty=format:", "--name-only", response = files)

            val stats = collector.collectFileChangeStats(limit = 10)

            assertTrue(stats.size <= 10)
        }

        @Test
        fun `should sort by change count descending`() {
            val logOutput = """
                file1.txt
                file2.txt
                file2.txt
                file3.txt
                file3.txt
                file3.txt
            """.trimIndent()

            mockExecutor.addResponse("log", "--all", "--pretty=format:", "--name-only", response = logOutput)

            val stats = collector.collectFileChangeStats(limit = 20)
            val entries = stats.entries.toList()

            assertEquals("file3.txt", entries[0].key)
            assertEquals(3, entries[0].value)
            assertEquals("file2.txt", entries[1].key)
            assertEquals(2, entries[1].value)
            assertEquals("file1.txt", entries[2].key)
            assertEquals(1, entries[2].value)
        }

        @Test
        fun `should filter blank lines`() {
            val logOutput = """
                file1.txt
                
                file2.txt
                
                
                file1.txt
            """.trimIndent()

            mockExecutor.addResponse("log", "--all", "--pretty=format:", "--name-only", response = logOutput)

            val stats = collector.collectFileChangeStats(limit = 20)

            assertEquals(2, stats.size)
            assertEquals(2, stats["file1.txt"])
            assertEquals(1, stats["file2.txt"])
        }

        @Test
        fun `should handle no file changes`() {
            mockExecutor.addResponse("log", "--all", "--pretty=format:", "--name-only", response = "")

            val stats = collector.collectFileChangeStats(limit = 20)

            assertTrue(stats.isEmpty())
        }
    }

    // Helper method to setup common mock responses
    private fun setupDefaultMockResponses() {
        // Repository info
        mockExecutor.addResponse("rev-parse", "--abbrev-ref", "HEAD", response = "main")
        mockExecutor.addResponse("rev-parse", "--abbrev-ref", "@{upstream}", response = "origin/main")
        mockExecutor.addResponse("rev-list", "--count", "origin/main..HEAD", response = "2")
        mockExecutor.addResponse("rev-list", "--count", "HEAD..origin/main", response = "0")

        // Commit info
        mockExecutor.addResponse("rev-parse", "HEAD", response = "abc123def456789012345678901234567890abcd")
        mockExecutor.addResponse("rev-parse", "--short", "HEAD", response = "abc123d")
        mockExecutor.addResponse("log", "-1", "--pretty=%s", response = "Test commit")
        mockExecutor.addResponse("log", "-1", "--pretty=%an", response = "Test Author")
        mockExecutor.addResponse("log", "-1", "--pretty=%cI", response = "2024-01-15T10:30:00Z")
        mockExecutor.addResponse("rev-list", "--count", "HEAD", response = "10")

        // Working directory info
        mockExecutor.addResponse("status", "--porcelain", response = "")

        // Tag info
        mockExecutor.addResponse("tag", "--sort=-version:refname", response = "v1.0.0")
        mockExecutor.addResponse("rev-list", "--count", "v1.0.0..HEAD", response = "2")

        // Remote info
        mockExecutor.addResponse("remote", response = "origin")
        mockExecutor.addResponse("remote", "get-url", "origin", response = "https://github.com/test/repo.git")
        mockExecutor.addResponse("ls-remote", "--exit-code", "origin", response = "refs")
    }

    /**
     * Mock implementation of GitCommandExecutor for testing.
     */
    private class MockGitCommandExecutor :
        GitCommandExecutor(File("."), Logging.getLogger(MockGitCommandExecutor::class.java)) {
        private val responses = mutableMapOf<String, String?>()
        private val exceptions = mutableSetOf<String>()

        fun addResponse(vararg args: String, response: String?) {
            responses[args.joinToString(" ")] = response
        }

        fun addResponseWithException(vararg args: String) {
            exceptions.add(args.joinToString(" "))
        }

        override fun execute(vararg args: String, trimResult: Boolean): String {
            val key = args.joinToString(" ")
            if (exceptions.contains(key)) {
                throw Exception("Mock exception for: $key")
            }
            return responses[key] ?: throw IllegalStateException("No mock response for: $key")
        }

        override fun executeOrNull(vararg args: String): String? {
            return try {
                val result = execute(*args)
                result.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
    }
}
