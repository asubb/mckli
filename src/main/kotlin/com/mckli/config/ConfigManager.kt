package com.mckli.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

class ConfigManager() {
    fun getConfigPath(): String {
        val configDirStr = System.getProperty("mckli.config.dir")
        val configDir = if (configDirStr != null) {
            File(configDirStr)
        } else {
            val home = System.getProperty("user.home")
            File(home, ".mckli")
        }

        if (!configDir.exists()) {
            logger.debug { "Creating configuration directory: ${configDir.absolutePath}" }
            configDir.mkdirs()
        }
        return File(configDir, "servers.json").absolutePath
    }

    fun getDaemonsPath(): String {
        val daemonsDirStr = System.getProperty("mckli.daemons.dir")
        val daemonsDir = if (daemonsDirStr != null) {
            File(daemonsDirStr)
        } else {
            val home = System.getProperty("user.home")
            File(home, ".mckli/daemons")
        }

        if (!daemonsDir.exists()) {
            logger.debug { "Creating daemons directory: ${daemonsDir.absolutePath}" }
            daemonsDir.mkdirs()
        }
        return daemonsDir.absolutePath
    }

    fun readConfig(): Configuration? {
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

    fun writeConfig(config: Configuration) {
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

class ConfigValidator {
    fun validate(config: Configuration): List<String> {
        val errors = mutableListOf<String>()

        config.servers.forEach { server ->
            // Validate URL format
            if (!server.endpoint.startsWith("http://") && !server.endpoint.startsWith("https://")) {
                errors.add("Server '${server.name}': Endpoint must start with http:// or https://")
            }

            // Validate timeout
            if (server.timeout <= 0) {
                errors.add("Server '${server.name}': timeout must be positive")
            }

            // Validate pool size
            if (server.poolSize <= 0) {
                errors.add("Server '${server.name}': pool size must be positive")
            }
        }

        // Check for duplicate server names
        val nameGroups = config.servers.groupBy { it.name }
        nameGroups.forEach { (name, servers) ->
            if (servers.size > 1) {
                errors.add("Duplicate server name: $name")
            }
        }

        // Check default server exists
        config.defaultServer?.let { defaultName ->
            if (!config.servers.any { it.name == defaultName }) {
                errors.add("Default server '$defaultName' not found in configuration")
            }
        }

        return errors
    }
}
