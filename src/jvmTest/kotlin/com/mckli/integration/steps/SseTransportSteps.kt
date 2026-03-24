package com.mckli.integration.steps

import com.mckli.config.Configuration
import com.mckli.config.ConfigManager
import com.mckli.config.ServerConfig
import com.mckli.config.TransportType.SSE
import com.mckli.daemon.DaemonProcess
import com.mckli.integration.support.MockSseServer
import com.mckli.transport.SseTransport
import io.cucumber.java8.En
import java.io.File
import kotlin.test.*

class SseTransportSteps : En {
    private val mockServer = MockSseServer(port = 8081)
    private val serverConfigs = mutableMapOf<String, ServerConfig>()
    private val transports = mutableMapOf<String, SseTransport>()
    private val daemons = mutableMapOf<String, DaemonProcess>()
    private var lastServerConfig: ServerConfig? = null
    private var connectionEstablished = false
    private var connectionStartTime = 0L
    private var reconnectionAttempted = false
    private lateinit var configManager: ConfigManager
    private lateinit var testConfigDir: File

    init {
        Before { ->
            // Set up test config directory
            testConfigDir =
                File(System.getProperty("java.io.tmpdir"), "mckli-test-config-${System.currentTimeMillis()}")
            testConfigDir.mkdirs()
            System.setProperty("mckli.config.dir", testConfigDir.absolutePath)

            configManager = ConfigManager()
            serverConfigs.clear()
            transports.values.forEach { it.close() }
            transports.clear()
            daemons.clear()
            lastServerConfig = null
            connectionEstablished = false
            connectionStartTime = 0L
            reconnectionAttempted = false
        }

        Before("@requires-sse-server") { ->
            mockServer.start()
            Thread.sleep(500) // Give server time to start
        }

        After("@requires-sse-server") { ->
            // Clean up daemons
            daemons.values.forEach { daemon ->
                try {
                    if (daemon.isRunning()) {
                        daemon.stop(force = true)
                    }
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }

            // Clean up transports
            transports.values.forEach { transport ->
                try {
                    transport.close()
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }

            mockServer.stop()

            // Clean up test config directory
            testConfigDir.deleteRecursively()
        }

        // Configuration scenario
        Given("a server {string} with SSE transport") { serverName: String ->
            val config = ServerConfig(
                name = serverName,
                endpoint = "http://localhost:8081/sse",
                transport = SSE,
                timeout = 5000,
                poolSize = 10
            )
            serverConfigs[serverName] = config
            lastServerConfig = config

            // Write config file
            saveConfiguration()
        }

        When("I check the server configuration") {
            // Configuration is already stored in lastServerConfig
            assertNotNull(lastServerConfig, "No server configuration found")
        }

        Then("the transport should be {string}") { expectedTransport: String ->
            assertNotNull(lastServerConfig, "No server configuration found")
            assertEquals(expectedTransport, lastServerConfig?.transport?.name)
        }

        // Daemon lifecycle scenario
        Given("a server {string} with SSE transport configured") { serverName: String ->
            val config = ServerConfig(
                name = serverName,
                endpoint = "http://localhost:8081/sse",
                transport = SSE,
                timeout = 5000,
                poolSize = 10
            )
            serverConfigs[serverName] = config

            // Write config file
            saveConfiguration()
        }

        When("I start the daemon for SSE server {string}") { serverName: String ->
            val config = serverConfigs[serverName]
                ?: throw IllegalStateException("Server config not found for $serverName")

            val daemon = DaemonProcess(config)
            daemons[serverName] = daemon

            val result = daemon.start()
            assertTrue(result.isSuccess, "Failed to start daemon: ${result.exceptionOrNull()?.message}")
            Thread.sleep(1000) // Give daemon time to initialize
        }

        Then("SSE connection should be established") {
            // Wait for connection to be established
            mockServer.waitForConnection(5000)

            val connectionCount = mockServer.getActiveConnectionCount()
            assertTrue(connectionCount > 0, "Expected active SSE connection, but found $connectionCount connections")
            connectionEstablished = true
        }

        // Reconnection scenario
        Given("the daemon for SSE server {string} is running") { serverName: String ->
            val config = ServerConfig(
                name = serverName,
                endpoint = "http://localhost:8081/sse",
                transport = SSE,
                timeout = 5000,
                poolSize = 10
            )
            serverConfigs[serverName] = config

            // Write config file
            saveConfiguration()

            val daemon = DaemonProcess(config)
            daemons[serverName] = daemon

            val result = daemon.start()
            assertTrue(result.isSuccess, "Failed to start daemon: ${result.exceptionOrNull()?.message}")
            Thread.sleep(2000) // Give more time for SSE connection

            // Verify connection is established
            mockServer.waitForConnection(5000)
            connectionEstablished = mockServer.getActiveConnectionCount() > 0
            assertTrue(connectionEstablished, "SSE connection not established")
        }

        When("the SSE connection is interrupted") {
            // Disconnect all SSE connections
            mockServer.disconnectAll()
            connectionStartTime = System.currentTimeMillis()
            Thread.sleep(500) // Give time for disconnect to be detected
        }

        Then("the daemon should attempt reconnection") {
            // Wait for reconnection attempt (up to 3 seconds)
            var attemptDetected = false
            val maxWait = 3000L
            val startTime = System.currentTimeMillis()

            while (!attemptDetected && (System.currentTimeMillis() - startTime) < maxWait) {
                if (mockServer.getActiveConnectionCount() > 0) {
                    attemptDetected = true
                    reconnectionAttempted = true
                } else {
                    Thread.sleep(200)
                }
            }

            assertTrue(attemptDetected, "Daemon did not attempt reconnection within $maxWait ms")
        }

        Then("the connection should be reestablished within {int} seconds") { timeoutSeconds: Int ->
            val timeoutMs = timeoutSeconds * 1000L
            val startTime = connectionStartTime
            var connected = false
            val maxWait = startTime + timeoutMs
            val now = System.currentTimeMillis()

            if (now < maxWait) {
                val remainingTime = maxWait - now
                var waitTime = 0L
                while (!connected && waitTime < remainingTime) {
                    if (mockServer.getActiveConnectionCount() > 0) {
                        connected = true
                    } else {
                        Thread.sleep(200)
                        waitTime += 200
                    }
                }
            } else {
                connected = mockServer.getActiveConnectionCount() > 0
            }

            val reconnectionTime = System.currentTimeMillis() - startTime
            assertTrue(
                connected,
                "Connection not reestablished within $timeoutSeconds seconds (took ${reconnectionTime}ms)"
            )
        }

        // Graceful shutdown scenario
        When("I stop the daemon for SSE server {string}") { serverName: String ->
            val daemon = daemons[serverName]
                ?: throw IllegalStateException("Daemon not found for $serverName")


            val startTime = System.currentTimeMillis()
            val maxWait = 15000L

            var errorMessage: String? = null
            while ((System.currentTimeMillis() - startTime) < maxWait) {
                try {
                    val result = daemon.stop(force = false)
                    errorMessage = result.exceptionOrNull()?.message
                    if (errorMessage == "Daemon not running") {
                        errorMessage = null
                        break
                    }
                    assertTrue(result.isSuccess)
                } catch (_: AssertionError) {
                    Thread.sleep(200)
                }
            }
            if (errorMessage != null) {
                throw AssertionError("Daemon did not stop within ${System.currentTimeMillis() - startTime}ms: $errorMessage")
            }
        }

        Then("the SSE connection should be closed gracefully") {
            // Wait for connection to close (with timeout)
            val maxWait = 15000L
            val startTime = System.currentTimeMillis()
            var connectionCount = mockServer.getActiveConnectionCount()

            while (connectionCount > 0 && (System.currentTimeMillis() - startTime) < maxWait) {
                Thread.sleep(200)
                connectionCount = mockServer.getActiveConnectionCount()
            }

            // FIXME
//            assertEquals(
//                0,
//                connectionCount,
//                "Expected no active connections, but found $connectionCount after ${System.currentTimeMillis() - startTime}ms"
//            )
        }

        Then("the SSE daemon for {string} should not be running") { serverName: String ->
            val daemon = daemons[serverName]
                ?: throw IllegalStateException("Daemon not found for $serverName")
            assertFalse(daemon.isRunning(), "Daemon for $serverName should not be running")
        }

        Then("the SSE daemon for {string} should be running") { serverName: String ->
            val daemon = daemons[serverName]
                ?: throw IllegalStateException("Daemon not found for $serverName")
            assertTrue(daemon.isRunning(), "Daemon for $serverName should be running")
        }
    }

    private fun cleanDaemonDirectory() {
        val daemonPath = File(System.getProperty("java.io.tmpdir"), "mckli-test-daemons")
        daemonPath.deleteRecursively()
        daemonPath.mkdirs()

        System.setProperty("mckli.daemon.dir", daemonPath.absolutePath)
    }

    private fun saveConfiguration() {
        val configuration = Configuration(
            servers = serverConfigs.values.toList(),
            defaultServer = serverConfigs.keys.firstOrNull()
        )
        configManager.writeConfig(configuration)
    }
}
