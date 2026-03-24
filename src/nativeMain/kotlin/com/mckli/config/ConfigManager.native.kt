package com.mckli.config

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual class ConfigManager {
    actual fun getConfigPath(): String {
        val home = getenv("HOME")?.toKString() ?: throw ConfigException("HOME environment variable not set")
        val configDir = "$home/.config/mckli"
        mkdir(configDir, 0x1FFu) // 0777
        return "$configDir/servers.json"
    }

    actual fun getDaemonsPath(): String {
        val home = getenv("HOME")?.toKString() ?: throw ConfigException("HOME environment variable not set")
        val daemonsDir = "$home/.config/mckli/daemons"
        mkdir(daemonsDir, 0x1FFu) // 0777
        return daemonsDir
    }

    actual fun readConfig(): Configuration? {
        val configPath = getConfigPath()
        val file = fopen(configPath, "r") ?: return null

        return try {
            val buffer = StringBuilder()
            memScoped {
                val readBuffer = allocArray<ByteVar>(1024)
                while (true) {
                    val read = fgets(readBuffer, 1024, file)?.toKString()
                    if (read == null) break
                    buffer.append(read)
                }
            }
            json.decodeFromString<Configuration>(buffer.toString())
        } catch (e: Exception) {
            throw ConfigException("Failed to read configuration: ${e.message}")
        } finally {
            fclose(file)
        }
    }

    actual fun writeConfig(config: Configuration) {
        val configPath = getConfigPath()
        val file = fopen(configPath, "w") ?: throw ConfigException("Failed to open config file for writing")

        try {
            val content = json.encodeToString(config)
            fputs(content, file)
        } catch (e: Exception) {
            throw ConfigException("Failed to write configuration: ${e.message}")
        } finally {
            fclose(file)
        }
    }
}

class ConfigException(message: String) : Exception(message)
