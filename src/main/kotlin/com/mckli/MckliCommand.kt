package com.mckli
 
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.github.oshai.kotlinlogging.KotlinLogging
 
private val logger = KotlinLogging.logger {}
 
/**
 * Main CLI application command for MCP CLI Wrapper
 *
 * This CLI wraps MCP (Model Context Protocol) servers with persistent
 * daemon processes to reduce token usage and expose tools as simple commands.
 *
 * If no subcommand is provided, displays help information.
 */
class MckliCommand : CliktCommand(
    name = "mckli"
) {
    override fun help(context: Context) = "MCP CLI Wrapper - Manage MCP servers and invoke tools via persistent daemons"
 
    override fun run() {
        logger.info { "Starting mckli command" }
        // If no subcommand was invoked, show help
        if (currentContext.invokedSubcommand == null) {
            echo(getFormattedHelp())
        }
    }
}
