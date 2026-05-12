package com.mckli
 
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.github.oshai.kotlinlogging.KotlinLogging
 
class MckliCommand : CliktCommand(
    name = "mckli"
) {
    private val logger = KotlinLogging.logger {}

    override fun help(context: Context) = "MCP CLI Wrapper - Manage MCP servers and invoke tools via persistent daemons"
 
    override fun run() {
        logger.info { "Starting mckli command" }
        // If no subcommand was invoked, show help
        if (currentContext.invokedSubcommand == null) {
            echo(getFormattedHelp())
        }
    }
}
