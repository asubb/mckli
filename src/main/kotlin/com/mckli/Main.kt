package com.mckli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.mckli.config.ConfigCommand
import com.mckli.daemon.DaemonCommand
import com.mckli.tools.ToolsCommand

/**
 * MCP CLI Wrapper
 *
 * A wrapper around MCP (Model Context Protocol) servers that:
 * - Maintains persistent HTTP connections through daemon processes
 * - Caches tool metadata to reduce token usage
 * - Exposes MCP tools as simple CLI commands for LLM integration
 *
 * Commands:
 * - config: Manage MCP server configuration
 * - daemon: Manage daemon processes
 * - tools: Discover and invoke MCP tools
 */
fun main(args: Array<String>) = MckliCommand()
    .subcommands(
        ConfigCommand(),
        DaemonCommand(),
        ToolsCommand()
    )
    .main(args)
