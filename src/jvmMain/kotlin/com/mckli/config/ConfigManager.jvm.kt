package com.mckli.config

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

actual class ConfigManager {
    actual fun getConfigPath(): String {
        val home = System.getProperty("user.home")
        val configDir = File(home, ".config/mckli")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        return File(configDir, "servers.json").absolutePath
    }

    actual fun getDaemonsPath(): String {
        val home = System.getProperty("user.home")
        val daemonsDir = File(home, ".config/mckli/daemons")
        if (!daemonsDir.exists()) {
            daemonsDir.mkdirs()
        }
        return daemonsDir.absolutePath
    }

    actual fun readConfig(): Configuration? {
        val configFile = File(getConfigPath())
        if (!configFile.exists()) {
            return null
        }
        return try {
            val content = configFile.readText()
            json.decodeFromString<Configuration>(content)
        } catch (e: Exception) {
            throw ConfigException("Failed to read configuration: ${e.message}")
        }
    }

    actual fun writeConfig(config: Configuration) {
        val configFile = File(getConfigPath())
        try {
            val content = json.encodeToString(Configuration.serializer(), config)
            configFile.writeText(content)
        } catch (e: Exception) {
            throw ConfigException("Failed to write configuration: ${e.message}")
        }
    }
}

class ConfigException(message: String) : Exception(message)
