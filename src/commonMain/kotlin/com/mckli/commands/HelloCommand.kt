package com.mckli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional

/**
 * Hello command - displays a greeting message
 *
 * Prints "Hello, World!" by default, or "Hello, <name>!" if a name is provided.
 */
class HelloCommand : CliktCommand(name = "hello") {
    private val name by argument(help = "Name to greet").optional()

    override fun run() {
        val greeting = if (name != null) {
            "Hello, $name!"
        } else {
            "Hello, World!"
        }
        echo(greeting)
    }
}
