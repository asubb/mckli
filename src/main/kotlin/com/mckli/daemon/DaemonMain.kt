package com.mckli.daemon

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    // Daemon uses its own logger with specific appender in logback.xml
    logger.debug { "Daemon entry point hit" }

    runBlocking {
        try {
            val daemon = UnifiedDaemon()

            // Start the HTTP server
            val server = UnifiedDaemonServer(daemon.getDaemonManager())
            server.start()

            daemon.start()
        } catch (e: Exception) {
            System.err.println("Daemon error: ${e.message}")
            e.printStackTrace()
            logger.error(e) { "Daemon execution failed" }
            delay(5000) // give some time to flush the logs
            exitProcess(1)
        }
    }
}
