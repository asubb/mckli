package com.mckli.integration.steps

import com.mckli.config.ConfigManager
import com.mckli.config.ServerConfig
import com.mckli.daemon.DaemonProcess
import io.cucumber.java8.En
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DaemonSteps : En {
    private val daemons = mutableMapOf<String, DaemonProcess>()
    private val pidsBefore = mutableMapOf<String, Int?>()
    private var lastOperationResult: Result<Unit> = Result.success(Unit)
    private var statusOutput: String = ""

    init {
        Before { ->
            daemons.clear()
            pidsBefore.clear()
        }

        After { ->
            // Clean up any running daemons
            daemons.values.forEach { daemon ->
                try {
                    if (daemon.isRunning()) {
                        daemon.stop(force = true)
                    }
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }

        Given("a clean daemon directory") {
            cleanDaemonDirectory()
        }

        Given("the daemon for server {string} is running") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            if (!daemon.isRunning()) {
                val result = daemon.start()
                assertTrue(result.isSuccess, "Failed to start daemon: ${result.exceptionOrNull()?.message}")
                Thread.sleep(1000) // Give daemon time to initialize
            }
        }

        When("I start the daemon for server {string}") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            lastOperationResult = daemon.start()
            Thread.sleep(1000) // Give daemon time to initialize
        }

        When("I try to start the daemon for server {string}") { serverName: String ->
            try {
                val daemon = getDaemonForServer(serverName)
                lastOperationResult = daemon.start()
            } catch (e: Exception) {
                lastOperationResult = Result.failure(e)
            }
        }

        When("I stop the daemon for {string}") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            lastOperationResult = daemon.stop(force = false)
            Thread.sleep(500) // Give time for cleanup
        }

        When("I restart the daemon for {string}") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            pidsBefore[serverName] = daemon.getPid()

            val stopResult = daemon.stop(force = false)
            assertTrue(stopResult.isSuccess)
            Thread.sleep(500)

            lastOperationResult = daemon.start()
            Thread.sleep(1000)
        }

        When("I check daemon status") {
            statusOutput = buildString {
                daemons.forEach { (name, daemon) ->
                    val status = if (daemon.isRunning()) "RUNNING" else "STOPPED"
                    appendLine("$name: $status (PID: ${daemon.getPid()})")
                }
            }
        }

        When("I send a tools list request to {string}") { serverName: String ->
            // This would trigger auto-start
            val daemon = getDaemonForServer(serverName)
            if (!daemon.isRunning()) {
                lastOperationResult = daemon.start()
                Thread.sleep(1000)
            }
        }

        Then("the daemon for {string} should be running") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            assertTrue(daemon.isRunning(), "Daemon for $serverName should be running")
        }

        Then("the daemon for {string} should not be running") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            assertFalse(daemon.isRunning(), "Daemon for $serverName should not be running")
        }

        Then("a PID file should exist for {string}") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            val pid = daemon.getPid()
            assertNotNull(pid, "PID file should exist for $serverName")
        }

        Then("the PID file for {string} should not exist") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            val pid = daemon.getPid()
            assertTrue(pid == null, "PID file should not exist for $serverName")
        }

        Then("a socket file should exist for {string}") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            val socketPath = daemon.getSocketPath()
            assertTrue(File(socketPath).exists(), "Socket file should exist at $socketPath")
        }

        Then("the socket file for {string} should not exist") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            val socketPath = daemon.getSocketPath()
            assertFalse(File(socketPath).exists(), "Socket file should not exist at $socketPath")
        }

        Then("I should see {string} is RUNNING") { serverName: String ->
            assertTrue(statusOutput.contains("$serverName: RUNNING"))
        }

        Then("I should see the PID for {string}") { serverName: String ->
            val daemon = getDaemonForServer(serverName)
            val pid = daemon.getPid()
            assertNotNull(pid)
            assertTrue(statusOutput.contains("PID: $pid"))
        }

        Then("the PID should be different from before") {
            daemons.forEach { (name, daemon) ->
                val oldPid = pidsBefore[name]
                val newPid = daemon.getPid()
                if (oldPid != null && newPid != null) {
                    assertNotEquals(oldPid, newPid, "PID should have changed for $name")
                }
            }
        }

        Then("the daemon start should fail") {
            assertTrue(lastOperationResult.isFailure, "Operation should have failed")
        }

        Then("I should see an error about server not found") {
            val exception = lastOperationResult.exceptionOrNull()
            assertNotNull(exception)
            assertTrue(
                exception.message?.contains("not found", ignoreCase = true) == true,
                "Error should mention server not found"
            )
        }

        Then("the request should complete successfully") {
            assertTrue(lastOperationResult.isSuccess, "Request should succeed")
        }

        Then("each daemon should have its own PID file") {
            daemons.forEach { (name, daemon) ->
                assertNotNull(daemon.getPid(), "Daemon $name should have a PID")
            }
        }

        Then("each daemon should have its own socket file") {
            val socketPaths = daemons.values.map { it.getSocketPath() }
            assertEquals(socketPaths.size, socketPaths.toSet().size, "Each daemon should have unique socket")
        }
    }

    private fun getDaemonForServer(serverName: String): DaemonProcess {
        return daemons.getOrPut(serverName) {
            val configManager = ConfigManager()
            val config = configManager.readConfig()
            val serverConfig = config?.servers?.find { it.name == serverName }
                ?: ServerConfig(name = serverName, endpoint = "http://localhost:8080/api")

            DaemonProcess(serverConfig)
        }
    }

    private fun cleanDaemonDirectory() {
        val daemonPath = File(System.getProperty("java.io.tmpdir"), "mckli-test-daemons")
        daemonPath.deleteRecursively()
        daemonPath.mkdirs()

        System.setProperty("mckli.daemon.dir", daemonPath.absolutePath)
    }

    private fun assertEquals(expected: Int, actual: Int, message: String) {
        kotlin.test.assertEquals(expected, actual, message)
    }
}
