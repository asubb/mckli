package com.mckli.config
 
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import io.github.oshai.kotlinlogging.KotlinLogging
 
private val logger = KotlinLogging.logger {}
 
class ConfigCommand : CliktCommand(name = "config") {
    override fun help(context: Context) = "Manage MCP server configuration"
    init {
        subcommands(ConfigAddCommand(), ConfigRemoveCommand(), ConfigListCommand(), ConfigEditCommand())
    }

    override fun run() {}
}

class ConfigAddCommand : CliktCommand(name = "add") {
    override fun help(context: Context) = "Add a new MCP server"

    private val name by argument(help = "Server name")
    private val endpoint by argument(help = "Server endpoint URL")
    private val username by option("--username", help = "Basic auth username")
    private val password by option("--password", help = "Basic auth password")
    private val token by option("--token", help = "Bearer token")
    private val timeout by option("--timeout", help = "Request timeout in milliseconds").long().default(30000)
    private val poolSize by option("--pool-size", help = "Connection pool size").int().default(10)

    override fun run() {
        logger.info { "Adding new MCP server: $name with endpoint: $endpoint" }
        val configManager = ConfigManager()
        val validator = ConfigValidator()

        var config = configManager.readConfig() ?: Configuration()

        // Check if server already exists
        if (config.servers.any { it.name == name }) {
            echo("Error: Server '$name' already exists", err = true)
            throw com.github.ajalt.clikt.core.CliktError("Server already exists")
        }

        // Create auth config
        val auth = when {
            username != null && password != null -> AuthConfig.Basic(username!!, password!!)
            token != null -> AuthConfig.Bearer(token!!)
            else -> null
        }

        val newServer = ServerConfig(
            name = name,
            endpoint = endpoint,
            auth = auth,
            timeout = timeout,
            poolSize = poolSize
        )

        config = config.copy(servers = config.servers + newServer)

        // Validate
        val errors = validator.validate(config)
        if (errors.isNotEmpty()) {
            echo("Configuration errors:", err = true)
            errors.forEach { echo("  - $it", err = true) }
            throw com.github.ajalt.clikt.core.CliktError("Invalid configuration")
        }

        configManager.writeConfig(config)
        echo("Server '$name' added successfully")
    }
}

class ConfigRemoveCommand : CliktCommand(name = "remove") {
    override fun help(context: Context) = "Remove an MCP server"

    private val name by argument(help = "Server name to remove")

    override fun run() {
        logger.info { "Removing MCP server: $name" }
        val configManager = ConfigManager()
        var config = configManager.readConfig() ?: Configuration()

        if (!config.servers.any { it.name == name }) {
            echo("Error: Server '$name' not found", err = true)
            throw com.github.ajalt.clikt.core.CliktError("Server not found")
        }

        config = config.copy(servers = config.servers.filter { it.name != name })
        configManager.writeConfig(config)
        echo("Server '$name' removed successfully")
    }
}

class ConfigListCommand : CliktCommand(name = "list") {
    override fun help(context: Context) = "List configured MCP servers"

    override fun run() {
        logger.info { "Listing configured MCP servers" }
        val configManager = ConfigManager()
        val config = configManager.readConfig()

        if (config == null || config.servers.isEmpty()) {
            echo("No servers configured")
            return
        }

        echo("Configured servers:")
        config.servers.forEach { server ->
            val defaultMark = if (server.name == config.defaultServer) " (default)" else ""
            echo("  ${server.name}$defaultMark")
            echo("    Endpoint: ${server.endpoint}")
            echo("    Timeout: ${server.timeout}ms")
            echo("    Pool size: ${server.poolSize}")
            if (server.auth != null) {
                val authType = when (server.auth) {
                    is AuthConfig.Basic -> "Basic"
                    is AuthConfig.Bearer -> "Bearer"
                }
                echo("    Auth: $authType")
            }
        }
    }
}

class ConfigEditCommand : CliktCommand(name = "edit") {
    override fun help(context: Context) = "Edit server configuration (opens JSON file)"

    override fun run() {
        val configManager = ConfigManager()
        val configPath = configManager.getConfigPath()
        echo("Configuration file: $configPath")
        echo("Edit the file manually and use 'config list' to verify changes")
    }
}
