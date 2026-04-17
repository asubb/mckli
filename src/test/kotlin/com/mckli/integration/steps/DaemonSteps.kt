package com.mckli.integration.steps

import com.mckli.client.RequestRouter
import com.mckli.config.ConfigManager
import com.mckli.config.ServerConfig
import com.mckli.config.Configuration
import com.mckli.daemon.DaemonProcess
import com.mckli.integration.support.TestConfiguration
import io.cucumber.java8.En
import java.io.File
import java.net.ServerSocket
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DaemonSteps : En {
    private var daemon: DaemonProcess? = null
    private var lastPid: Int? = null
    private var lastOperationResult: Result<Any?> = Result.success(Unit)
    private var statusOutput: String = ""
    private var lastError: String? = null

    init {
        Before { scenario ->
            println("[DEBUG_LOG] Starting scenario: ${scenario.name}")
            TestConfiguration.setup()
            daemon = null
            lastPid = null
        }

        After { scenario ->
            if (scenario.isFailed) {
                println("[DEBUG_LOG] Scenario failed: ${scenario.name}")
                val daemonsDir = File(TestConfiguration.tempDir, "daemons")
                daemonsDir.listFiles()?.forEach { file ->
                    val fileName = file.name
                    if (file.exists()) {
                        println("[DEBUG_LOG] --- $fileName ---")
                        file.readLines().forEach { println("[DEBUG_LOG] $it") }
                        println("[DEBUG_LOG] --- end of $fileName ---")
                    } else {
                        println("[DEBUG_LOG] --- $fileName ---")
                        println("<<EMPTY>>")
                        println("[DEBUG_LOG] --- end of $fileName ---")
                    }
                }
            }

            // Clean up unified daemon
            try {
                val d = getUnifiedDaemon()
                if (d.isRunning()) {
                    d.stop(force = true)
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }

        Given("a clean daemon directory") {
            cleanDaemonDirectory()
        }

        Given("the unified daemon is running") {
            val d = getUnifiedDaemon()
            if (!d.isRunning()) {
                val result = d.start()
                assertTrue(result.isSuccess, "Failed to start unified daemon: ${result.exceptionOrNull()?.message}")
                Thread.sleep(2000) // Give daemon time to initialize
            }
        }

        Given("the daemon for server {string} is running") { serverName: String ->
            val d = getUnifiedDaemon()
            if (!d.isRunning()) {
                d.start()
                Thread.sleep(2000)
            }
        }

        When("I start the unified daemon") {
            val d = getUnifiedDaemon()
            lastOperationResult = d.start()
            Thread.sleep(2000)
        }

        When("I start the daemon for server {string}") { serverName: String ->
            val d = getUnifiedDaemon()
            lastOperationResult = d.start()
            Thread.sleep(2000)
        }

        When("I try to start the unified daemon") {
            try {
                val d = getUnifiedDaemon()
                lastOperationResult = d.start()
            } catch (e: Exception) {
                lastOperationResult = Result.failure(e)
            }
        }

        When("I stop the unified daemon") {
            val d = getUnifiedDaemon()
            lastOperationResult = d.stop(force = false)
            Thread.sleep(500)
        }

        When("I stop the daemon for {string}") { serverName: String ->
            val d = getUnifiedDaemon()
            lastOperationResult = d.stop(force = false)
            Thread.sleep(500)
        }

        When("I restart the unified daemon") {
            val d = getUnifiedDaemon()
            lastPid = d.getPid()

            val stopResult = d.stop(force = false)
            assertTrue(stopResult.isSuccess)
            Thread.sleep(500)

            lastOperationResult = d.start()
            Thread.sleep(2000)
        }

        When("I check daemon status") {
            val d = getUnifiedDaemon()
            val isRunning = d.isRunning()
            val status = if (isRunning) "RUNNING" else "STOPPED"
            statusOutput = "Unified daemon: $status (PID: ${d.getPid()})"
        }

        When("I list tools for server {string}") { serverName: String ->
            // Use a loop to wait for tools to be discovered, as auto-refresh might take a moment
            val router = RequestRouter(serverName)
            var attempts = 0
            var tools: Any? = null
            while (attempts < 10) {
                lastOperationResult = router.listTools(null)
                tools = lastOperationResult.getOrNull()
                if (lastOperationResult.isSuccess && tools is kotlinx.serialization.json.JsonArray && tools.isNotEmpty()) {
                    break
                }
                Thread.sleep(1000)
                attempts++
            }
            if (lastOperationResult.isFailure) {
                lastError = lastOperationResult.exceptionOrNull()?.message
            }
        }

        Then("the unified daemon should be running") {
            val d = getUnifiedDaemon()
            assertTrue(d.isRunning(), "Unified daemon should be running")
        }

        Then("the unified daemon should not be running") {
            val d = getUnifiedDaemon()
            assertFalse(d.isRunning(), "Unified daemon should not be running")
        }

        Then("I should see the unified daemon is RUNNING") {
            assertTrue(statusOutput.contains("Unified daemon: RUNNING"))
        }

        Then("the request should complete successfully") {
            val error = lastOperationResult.exceptionOrNull()?.message ?: lastError
            assertTrue(lastOperationResult.isSuccess, "Request should succeed. Error: $error")
        }

        Then("logs should not have any errors") {
            val daemonsDir = File(TestConfiguration.tempDir, "daemons")
            
            // Check stderr file
            val errFile = File(daemonsDir, "daemon.err")
            if (errFile.exists()) {
                val errors = errFile.readLines().filter { line ->
                    line.isNotBlank() && !line.startsWith("WARNING:")
                }
                assertTrue(errors.isEmpty(), "Daemon stderr should be empty (excluding warnings), but contains:\n${errors.joinToString("\n")}")
            }

            // Check logback file
            val logFile = File(daemonsDir, "mckli-daemon-unified.log")
            if (logFile.exists()) {
                val errorLines = logFile.readLines().filter { line ->
                    line.contains(" ERROR ")
                }
                assertTrue(errorLines.isEmpty(), "Daemon log file should not contain ERROR entries, but found:\n${errorLines.joinToString("\n")}")
            }
        }
    }

    private fun getUnifiedDaemon(): DaemonProcess {
        if (daemon == null) {
            val port = ServerSocket(0).use { it.localPort }
            val dummyConfig = ServerConfig(name = "daemon", endpoint = "http://localhost:$port")
            daemon = DaemonProcess(dummyConfig)
        }
        return daemon!!
    }

    private fun cleanDaemonDirectory() {
        val daemonsDir = File(TestConfiguration.tempDir, "daemons")
        daemonsDir.deleteRecursively()
        daemonsDir.mkdirs()
    }

    private fun assertEquals(expected: Int, actual: Int, message: String) {
        kotlin.test.assertEquals(expected, actual, message)
    }
}
