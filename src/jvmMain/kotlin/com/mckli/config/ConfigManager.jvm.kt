package com.mckli.config
 
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
 
private val logger = KotlinLogging.logger {}
 
actual class ConfigManager {
    actual fun getConfigPath(): String {
        val home = System.getProperty("user.home")
        val configDir = File(home, ".config/mckli")
        if (!configDir.exists()) {
            logger.debug { "Creating configuration directory: ${configDir.absolutePath}" }
            configDir.mkdirs()
        }
        return File(configDir, "servers.json").absolutePath
    }
 
    actual fun getDaemonsPath(): String {
        val home = System.getProperty("user.home")
        val daemonsDir = File(home, ".config/mckli/daemons")
        if (!daemonsDir.exists()) {
            logger.debug { "Creating daemons directory: ${daemonsDir.absolutePath}" }
            daemonsDir.mkdirs()
        }
        return daemonsDir.absolutePath
    }
 
    actual fun readConfig(): Configuration? {
        val configPath = getConfigPath()
        val configFile = File(configPath)
        if (!configFile.exists()) {
            logger.debug { "Configuration file not found at: $configPath" }
            return null
        }
        return try {
            logger.debug { "Reading configuration from: $configPath" }
            val content = configFile.readText()
            json.decodeFromString<Configuration>(content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to read configuration from $configPath" }
            throw ConfigException("Failed to read configuration: ${e.message}")
        }
    }
 
    actual fun writeConfig(config: Configuration) {
        val configPath = getConfigPath()
        val configFile = File(configPath)
        try {
            logger.debug { "Writing configuration to: $configPath" }
            val content = json.encodeToString(Configuration.serializer(), config)
            configFile.writeText(content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to write configuration to $configPath" }
            throw ConfigException("Failed to write configuration: ${e.message}")
        }
    }
}

class ConfigException(message: String) : Exception(message)
