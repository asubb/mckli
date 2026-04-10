# mckli - MCP CLI Wrapper

A Kotlin Multiplatform CLI tool that acts as a bridge between LLMs and MCP (Model Context Protocol) servers. It optimizes performance and reduces token usage by maintaining persistent connections through background daemon processes and intelligent metadata caching.

## Core Intentions

- 🔄 **Performance Optimization**: Maintains persistent HTTP/SSE connections to eliminate repeated handshake overhead.
- 💾 **Efficiency**: Caches tool metadata to avoid redundant discovery calls, significantly reducing token consumption for LLMs.
- 🚀 **Developer Experience**: Exposes complex MCP tools as simple, discoverable CLI commands.
- 🌐 **Robustness**: Provides automatic reconnection, connection pooling, and multi-server management.

## Architecture at a Glance

```
┌─────────────────┐
│   LLM / User    │
└────────┬────────┘
         │ Simple CLI commands
    ┌────▼─────┐
    │   mckli  │ (Stateless CLI)
    └────┬─────┘
         │ IPC (Unix Sockets / Named Pipes)
    ┌────▼───────────────┐
    │    Daemon          │
    │  - Connection Pool │
    │  - Tool Cache      │
    └────┬───────────────┘
         │ Persistent HTTP / SSE
    ┌────▼────────────┐
    │   MCP Server    │
    └─────────────────┘
```

## Quick Start

### 1. Installation

Build the application using the included Gradle wrapper (requires Java 21+):

```bash
# Build self-contained JAR
./gradlew fatJar
# Artifact: build/libs/mckli-all.jar

# Or build native binary (Linux/macOS/Windows)
./gradlew linkReleaseExecutableNative
# Artifact: build/bin/native/releaseExecutable/mckli.kexe
```

### 2. Basic Usage

```bash
# 1. Add an MCP server
mckli config add myserver https://mcp.example.com/api

# 2. Search for tools across all servers
mckli tools search "read"

# 3. List available tools for a specific server (daemons start automatically)
mckli tools list myserver

# 4. Call a tool
mckli tools call myserver read-file --json '{"path": "config.json"}'

# 5. Get help for any command
mckli --help
mckli tools --help
mckli tools call --help
```

### Example: Claude Subagent Skill Integration

When defining a skill for a Claude subagent, provide clear explanations of how to use `mckli` commands. This helps the subagent understand how to interact with MCP servers through the wrapper.

**Example `SKILL.md` snippet:**

```markdown
### MCP Tool Interaction

To interact with the connected MCP servers, use the `mckli` CLI tool. It manages persistent connections via a background daemon.

**Command Structure:**
`mckli tools call <server-name> <tool-name> --json '<arguments>'`

**Key Commands:**
- `mckli tools search <query>`: **CRITICAL**: Use this first to find which server provides the tool you need. It searches across ALL configured servers.
- `mckli tools list <server-name>`: Use this to discover all available tools and their required JSON schemas for a specific server.
- `mckli tools call <server-name> <tool-name> --json '<json-args>'`: Use this to execute a tool. Always ensure the `--json` argument is a valid, single-quoted JSON string.
- `mckli <command> --help`: Use this for any command to see available subcommands and flags.

**Example Usage:**
To find a tool that can read files:
`mckli tools search "read"`

To get more help on the `tools` command:
`mckli tools --help`

To read a file using the `myserver` MCP server's `read-file` tool:
`mckli tools call myserver read-file --json '{"path": "src/main.kt"}'`
```

## Documentation

- 📘 **[User Guide](docs/user/README.md)**: Detailed installation, configuration, and command usage.
- 🛠️ **[Developer Guide](docs/dev/README.md)**: Architecture details, code organization, and contribution workflow.
- 🧪 **[Testing Guide](docs/dev/TESTING.md)**: Running unit and integration tests.

## Requirements

- **Java 21 or higher** (for building and JVM execution)
- **Unix-like OS** (Linux, macOS) recommended; Windows supported via WSL or limited native testing.

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) file for details.
