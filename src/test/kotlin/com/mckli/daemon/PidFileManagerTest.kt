package com.mckli.daemon

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

class PidFileManagerTest {
    private lateinit var tempDir: Path
    private lateinit var pidFileManager: PidFileManager

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("mckli-test-pids")
        pidFileManager = PidFileManager(tempDir)
    }

    @AfterTest
    fun teardown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `should create PID file with process ID`() = runTest {
        // Given
        val serverName = "testserver"
        val pid = 12345

        // When
        pidFileManager.writePidFile(serverName, pid)

        // Then
        val pidFile = tempDir.resolve("$serverName.pid")
        assertTrue(pidFile.exists(), "PID file should exist")
        assertEquals(pid.toString(), pidFile.readText().trim())
    }

    @Test
    fun `should read existing PID file`() = runTest {
        // Given
        val serverName = "testserver"
        val pid = 67890
        val pidFile = tempDir.resolve("$serverName.pid")
        pidFile.writeText(pid.toString())

        // When
        val readPid = pidFileManager.readPidFile(serverName)

        // Then
        assertEquals(pid, readPid)
    }

    @Test
    fun `should return null for non-existent PID file`() = runTest {
        // Given
        val serverName = "nonexistent"

        // When
        val readPid = pidFileManager.readPidFile(serverName)

        // Then
        assertNull(readPid)
    }

    @Test
    fun `should delete PID file`() = runTest {
        // Given
        val serverName = "testserver"
        val pidFile = tempDir.resolve("$serverName.pid")
        pidFile.writeText("12345")

        // When
        pidFileManager.deletePidFile(serverName)

        // Then
        assertFalse(pidFile.exists(), "PID file should be deleted")
    }

    @Test
    fun `should not fail when deleting non-existent PID file`() = runTest {
        // Given
        val serverName = "nonexistent"

        // When/Then - should not throw
        pidFileManager.deletePidFile(serverName)
    }

    @Test
    fun `should validate process is running`() = runTest {
        // Given - current process
        val currentPid = ProcessHandle.current().pid().toInt()

        // When
        val isRunning = pidFileManager.isProcessRunning(currentPid)

        // Then
        assertTrue(isRunning, "Current process should be running")
    }

    @Test
    fun `should detect non-running process`() = runTest {
        // Given - impossible PID
        val impossiblePid = 999999

        // When
        val isRunning = pidFileManager.isProcessRunning(impossiblePid)

        // Then
        assertFalse(isRunning, "Impossible PID should not be running")
    }

    @Test
    fun `should clean up stale PID files`() = runTest {
        // Given
        val stalePid = 999999
        val validPid = ProcessHandle.current().pid().toInt()
        
        pidFileManager.writePidFile("stale", stalePid)
        pidFileManager.writePidFile("valid", validPid)

        // When
        val cleaned = pidFileManager.cleanupStalePidFiles()

        // Then
        assertEquals(1, cleaned, "Should clean up 1 stale PID file")
        assertFalse(tempDir.resolve("stale.pid").exists())
        assertTrue(tempDir.resolve("valid.pid").exists())
    }

    @Test
    fun `should list all daemon PIDs`() = runTest {
        // Given
        pidFileManager.writePidFile("daemon1", 1111)
        pidFileManager.writePidFile("daemon2", 2222)
        pidFileManager.writePidFile("daemon3", 3333)

        // When
        val daemons = pidFileManager.listAllDaemons()

        // Then
        assertEquals(3, daemons.size)
        assertTrue(daemons.containsKey("daemon1"))
        assertTrue(daemons.containsKey("daemon2"))
        assertTrue(daemons.containsKey("daemon3"))
        assertEquals(1111, daemons["daemon1"])
    }

    @Test
    fun `should handle corrupted PID file`() = runTest {
        // Given
        val serverName = "corrupted"
        val pidFile = tempDir.resolve("$serverName.pid")
        pidFile.writeText("not-a-number")

        // When
        val readPid = pidFileManager.readPidFile(serverName)

        // Then
        assertNull(readPid, "Should return null for corrupted PID file")
    }

    @Test
    fun `should overwrite existing PID file`() = runTest {
        // Given
        val serverName = "testserver"
        pidFileManager.writePidFile(serverName, 1111)

        // When
        pidFileManager.writePidFile(serverName, 2222)

        // Then
        val readPid = pidFileManager.readPidFile(serverName)
        assertEquals(2222, readPid)
    }
}

/**
 * Helper class for managing PID files in tests.
 * This would be part of the actual implementation in src/jvmMain/.
 */
class PidFileManager(private val pidDir: Path) {
    
    fun writePidFile(serverName: String, pid: Int) {
        pidDir.toFile().mkdirs()
        val pidFile = pidDir.resolve("$serverName.pid")
        pidFile.writeText(pid.toString())
    }

    fun readPidFile(serverName: String): Int? {
        val pidFile = pidDir.resolve("$serverName.pid")
        if (!pidFile.exists()) return null
        
        return try {
            pidFile.readText().trim().toInt()
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun deletePidFile(serverName: String) {
        val pidFile = pidDir.resolve("$serverName.pid")
        pidFile.deleteIfExists()
    }

    fun isProcessRunning(pid: Int): Boolean {
        return ProcessHandle.of(pid.toLong()).map { it.isAlive }.orElse(false)
    }

    fun cleanupStalePidFiles(): Int {
        var cleaned = 0
        pidDir.toFile().listFiles()?.forEach { file ->
            if (file.extension == "pid") {
                val pid = readPidFile(file.nameWithoutExtension)
                if (pid != null && !isProcessRunning(pid)) {
                    deletePidFile(file.nameWithoutExtension)
                    cleaned++
                }
            }
        }
        return cleaned
    }

    fun listAllDaemons(): Map<String, Int> {
        val daemons = mutableMapOf<String, Int>()
        pidDir.toFile().listFiles()?.forEach { file ->
            if (file.extension == "pid") {
                val serverName = file.nameWithoutExtension
                val pid = readPidFile(serverName)
                if (pid != null) {
                    daemons[serverName] = pid
                }
            }
        }
        return daemons
    }
}
