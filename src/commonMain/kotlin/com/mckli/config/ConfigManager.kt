package com.mckli.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

expect class ConfigManager() {
    fun getConfigPath(): String
    fun getDaemonsPath(): String
    fun readConfig(): Configuration?
    fun writeConfig(config: Configuration)
}

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

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
