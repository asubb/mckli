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

# 2. List available tools (daemons start automatically)
mckli tools list myserver

# 3. Call a tool
mckli tools call myserver read-file --json '{"path": "config.json"}'
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
