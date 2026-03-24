package com.mckli

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Main CLI application command
 *
 * This is the root command that coordinates all subcommands.
 * If no subcommand is provided, it displays help information.
 */
class MckliCommand : CliktCommand(name = "mckli") {
    override fun run() {
        // If no subcommand was invoked, show help
        if (currentContext.invokedSubcommand == null) {
            echo(getFormattedHelp())
        }
    }
}
