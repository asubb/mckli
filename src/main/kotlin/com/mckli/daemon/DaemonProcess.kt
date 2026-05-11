package com.mckli.daemon

import com.mckli.config.ConfigManager
import com.mckli.config.ServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import java.io.File

private val log = KotlinLogging.logger {  }

class DaemonProcess(private val config: ServerConfig) {
    private val configManager = ConfigManager()
    private val daemonsDir = File(configManager.getDaemonsPath())
    private val pidFile = File(daemonsDir, "daemon.pid")
    private val logFile = File(daemonsDir, "daemon.log")
    private val errFile = File(daemonsDir, "daemon.err")

    init {
        if (!daemonsDir.exists()) {
            daemonsDir.mkdirs()
        }
        log.info { "Initialized unified daemon process, daemon directory: ${daemonsDir.absolutePath}" }
    }

    fun start(): Result<Unit> {
        return try {
            // Check if already running
            if (isRunning()) {
                return Result.success(Unit)
            }

            // Clean up old PID file
            pidFile.delete()

            // Get the JAR path (simplified - in production would need proper packaging)
            val javaHome = System.getProperty("java.home")
            val javaBin = File(javaHome, "bin/java").absolutePath
            val classpath = System.getProperty("java.class.path")

            // Spawn daemon process
            val args = mutableListOf(javaBin, "-cp", classpath)
            
            // Pass configuration and daemon directory properties if they are set
            System.getProperty("mckli.config.dir")?.let { args.add("-Dmckli.config.dir=$it") }
            System.getProperty("mckli.daemons.dir")?.let { args.add("-Dmckli.daemons.dir=$it") }
            System.getProperty("java.class.path")?.let { args.add("-Djava.class.path=$it") }
            
            args.add("-DMCKLI_LOG_DIR=${daemonsDir.absolutePath}")
            args.add("com.mckli.daemon.DaemonMainKt")

            log.debug { "Spawning unified daemon: ${args.joinToString(" ")}" }
            val processBuilder = ProcessBuilder(args)

            processBuilder.redirectOutput(logFile)
            processBuilder.redirectError(errFile)

            val process = processBuilder.start()

            // Wait briefly to ensure process started
            var waited = 0L
            val waitStep = 100L
            val maxWait = 5000L
            
            while (waited < maxWait) {
                Thread.sleep(waitStep)
                waited += waitStep
                if (!process.isAlive) break
            }

            if (!process.isAlive) {
                val exitCode = process.exitValue()
                return Result.failure(DaemonException("Daemon failed to start (exit code: $exitCode)"))
            }

            // Write PID file
            // Let the daemon write its own PID file or do it here
            val pid = process.pid()
            pidFile.writeText(pid.toString())

            // Wait for HTTP server to be ready
            var ready = false
            val httpClient = java.net.http.HttpClient.newHttpClient()
            val request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://127.0.0.1:5030/health"))
                .GET()
                .build()

            for (i in 1..50) {
                try {
                    val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    if (response.statusCode() == 200) {
                        ready = true
                        break
                    }
                } catch (e: Exception) {
                    // Ignore and retry
                }
                Thread.sleep(200)
            }

            if (!ready) {
                log.warn { "Daemon HTTP server not ready after timeout" }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DaemonException("Failed to start daemon: ${e.message}", e))
        }
    }

    fun stop(force: Boolean = false): Result<Unit> {
        return try {
            val pid = getPid() ?: return Result.failure(DaemonException("Daemon not running"))

            if (!isRunning()) {
                cleanup()
                return Result.success(Unit)
            }

            // Send SIGTERM
            val killProcess = ProcessBuilder("kill", "-TERM", pid.toString())
                .start()

            killProcess.waitFor()

            // Wait for graceful shutdown
            if (!force) {
                val maxWait = 10_000L // 10 seconds
                val start = System.currentTimeMillis()

                while (isRunning() && (System.currentTimeMillis() - start) < maxWait) {
                    Thread.sleep(100)
                }
            }

            // Force kill if still running
            if (isRunning()) {
                val killForceProcess = ProcessBuilder("kill", "-KILL", pid.toString())
                    .start()
                killForceProcess.waitFor()

                Thread.sleep(500)
            }

            cleanup()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DaemonException("Failed to stop daemon: ${e.message}", e))
        }
    }

    fun isRunning(): Boolean {
        val pid = getPid() ?: return false

        return try {
            // Check if process exists
            val process = ProcessBuilder("kill", "-0", pid.toString())
                .start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun getPid(): Int? {
        if (!pidFile.exists()) return null

        return try {
            pidFile.readText().trim().toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getSocketPath(): String {
        return pidFile.parentFile.absolutePath + "/daemon.sock" // Not used for connection anymore, but kept for tests/compatibility
    }

    private fun cleanup() {
        pidFile.delete()
    }
}

@Serializable
data class DaemonStatus(
    val isRunning: Boolean,
    val pid: Int?,
    val managedServers: List<String> = emptyList(),
    val connectionStates: Map<String, ConnectionState> = emptyMap(),
    val lastErrors: Map<String, String?> = emptyMap()
)

enum class ConnectionState {
    Unknown,
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
    Failed
}

class DaemonException(message: String, cause: Throwable? = null) : Exception(message, cause)
