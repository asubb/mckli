package com.mckli.integration.support

import java.io.File
import java.nio.file.Files

object TestConfiguration {
    val tempDir: File by lazy {
        val dir = Files.createTempDirectory("mckli-integration-tests").toFile()
        println("Using temporary directory for integration tests: ${dir.absolutePath}")
        
        // Ensure the directory exists
        dir.mkdirs()
        
        // Set system properties for ConfigManager
        System.setProperty("mckli.config.dir", dir.absolutePath)
        System.setProperty("mckli.daemons.dir", File(dir, "daemons").absolutePath)
        System.setProperty("MCKLI_LOG_DIR", dir.absolutePath)
        
        dir
    }

    fun setup() {
        // Trigger lazy initialization
        tempDir
    }
}
