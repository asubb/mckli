package com.mckli.daemon

import com.mckli.config.ServerConfig
import com.mckli.config.ConfigManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

actual class DaemonProcess actual constructor(private val config: ServerConfig) {
    private val configManager = ConfigManager()
    private val daemonsDir = File(configManager.getDaemonsPath())
    private val pidFile = File(daemonsDir, "${config.name}.pid")
    private val socketPath = File(daemonsDir, "${config.name}.sock").absolutePath

    init {
        if (!daemonsDir.exists()) {
            daemonsDir.mkdirs()
        }
        cleanupStaleSocket()
    }

    actual fun start(): Result<Unit> {
        return try {
            // Check if already running
            if (isRunning()) {
                return Result.failure(DaemonException("Daemon for '${config.name}' is already running"))
            }

            // Clean up old PID file
            pidFile.delete()

            // Get the JAR path (simplified - in production would need proper packaging)
            val javaHome = System.getProperty("java.home")
            val javaBin = File(javaHome, "bin/java").absolutePath
            val classpath = System.getProperty("java.class.path")

            // Spawn daemon process
            val processBuilder = ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "com.mckli.daemon.DaemonMainKt",
                config.name
            )

            processBuilder.redirectOutput(File(daemonsDir, "${config.name}.log"))
            processBuilder.redirectError(File(daemonsDir, "${config.name}.err"))

            val process = processBuilder.start()

            // Wait briefly to ensure process started
            Thread.sleep(500)

            if (!process.isAlive) {
                return Result.failure(DaemonException("Daemon failed to start"))
            }

            // Write PID file
            val pid = process.pid()
            pidFile.writeText(pid.toString())

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DaemonException("Failed to start daemon: ${e.message}", e))
        }
    }

    actual fun stop(force: Boolean): Result<Unit> {
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

    actual fun isRunning(): Boolean {
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

    actual fun getPid(): Int? {
        if (!pidFile.exists()) return null

        return try {
            pidFile.readText().trim().toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    actual fun getSocketPath(): String {
        return socketPath
    }

    private fun cleanup() {
        pidFile.delete()
        File(socketPath).delete()
    }

    private fun cleanupStaleSocket() {
        val socketFile = File(socketPath)
        if (socketFile.exists() && !isRunning()) {
            socketFile.delete()
        }
    }
}
