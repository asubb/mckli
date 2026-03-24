package com.mckli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.mckli.commands.HelloCommand

/**
 * Application entry point
 *
 * Initializes the CLI application with all available commands and
 * delegates argument parsing to Clikt.
 *
 * Clikt automatically provides --help/-h flag for all commands.
 * No need for a custom help command!
 *
 * To add a new command:
 * 1. Create a new class extending CliktCommand in the commands package
 * 2. Add it to the subcommands list below
 */
fun main(args: Array<String>) = MckliCommand()
    .subcommands(
        HelloCommand()
        // Add new commands here
    )
    .main(args)
