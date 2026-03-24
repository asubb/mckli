# mckli - MCP CLI Wrapper

A Kotlin Multiplatform CLI wrapper for MCP (Model Context Protocol) servers that reduces token usage by maintaining
persistent HTTP connections through daemon processes and caching tool metadata.

## Overview

**mckli** acts as a bridge between LLMs and MCP servers, solving the token cost problem of repeated connection overhead.
It:

- 🔄 **Maintains persistent connections** - Daemon processes keep HTTP connections alive to MCP servers
- 💾 **Caches tool metadata** - Tools are discovered once and cached, eliminating repeated discovery calls
- 🚀 **Exposes tools as CLI commands** - LLMs can invoke MCP tools via simple shell commands
- ⚡ **Auto-starts daemons** - Daemons launch automatically on first request
- 🌐 **Supports multiple servers** - Manage and connect to multiple MCP servers simultaneously

## Architecture

```
┌─────────────────┐
│   LLM / User    │
└────────┬────────┘
         │ Simple CLI commands
         │
    ┌────▼─────┐
    │   mckli  │  (stateless CLI)
    └────┬─────┘
         │ Unix socket IPC
         │
    ┌────▼──────────┐
    │    Daemon     │  (per MCP server)
    │  - HTTP Pool  │
    │  - Tool Cache │
    └────┬──────────┘
         │ Persistent HTTP
         │
    ┌────▼────────────┐
    │   MCP Server    │
    └─────────────────┘
```

## Quick Start

### 1. Build the Application

```bash
# Build self-contained JAR with all dependencies
./gradlew fatJar

# Or build native binary (Linux x64, macOS, Windows)
./gradlew linkReleaseExecutableNative
```

**Artifacts:**
- JAR: `build/libs/mckli-all.jar`
- Native: `build/bin/native/releaseExecutable/mckli.kexe`

For convenience, create an alias:
```bash
# For JAR (Java 21+)
alias mckli='java --enable-native-access=ALL-UNNAMED -jar /path/to/build/libs/mckli-all.jar'

# For native binary
alias mckli='/path/to/build/bin/native/releaseExecutable/mckli.kexe'
```

### 2. Configure an MCP Server

```bash
# Add your first MCP server
mckli config add myserver https://mcp.example.com/api

# With authentication
mckli config add myserver https://mcp.example.com/api --token YOUR_TOKEN

# List configured servers
mckli config list
```

### 3. Start the Daemon

```bash
# Start daemon for your server (happens automatically on first use)
mckli daemon start myserver

# Check daemon status
mckli daemon status
```

### 4. Use MCP Tools

```bash
# List available tools
mckli tools list myserver

# Get tool details
mckli tools describe myserver read-file

# Call a tool
mckli tools call myserver read-file --json '{"path": "/tmp/file.txt"}'
```

## Key Features

### Persistent Connections

- One daemon per MCP server maintains long-lived HTTP connections
- Connection pool with configurable size (default: 10)
- Automatic connection lifecycle management (idle timeout, max lifetime)

### Tool Caching

- Tools fetched once on daemon startup
- Cached in memory for instant access
- Manual refresh available via `tools refresh`

### LLM Integration

- Simple CLI interface: `mckli tools call <server> <tool> --json <args>`
- JSON output for easy parsing
- Error messages include context for debugging

### Multi-Server Support

- Configure multiple MCP servers
- Each gets its own daemon process
- Default server selection for convenience

## Commands

### Configuration Management

```bash
# Add a server
mckli config add <name> <endpoint> [--username USER --password PASS | --token TOKEN]

# Remove a server
mckli config remove <name>

# List servers
mckli config list

# Edit configuration (opens config file path)
mckli config edit
```

### Daemon Management

```bash
# Start a daemon
mckli daemon start <server>

# Stop a daemon
mckli daemon stop <server> [--force]

# Check status
mckli daemon status

# Restart
mckli daemon restart <server>
```

### Tool Operations

```bash
# List tools
mckli tools list [server] [--filter PATTERN]

# Describe a tool
mckli tools describe [server] <tool-name>

# Call a tool
mckli tools call [server] <tool-name> --json '{"arg": "value"}'

# Refresh tool cache
mckli tools refresh [server]
```

## Documentation

- **[User Manual](docs/user/README.md)** - Complete usage guide with examples
- **[Developer Guide](docs/dev/README.md)** - Architecture and development documentation

## Requirements

- **Java 21 or higher**
- **Gradle 9.4.1+** (included via wrapper)
- **Unix-like OS** (Linux, macOS) for full functionality
    - Windows support via named pipes (limited testing)

## Building

### For Users (Distribution)

**Fat JAR (recommended - works everywhere with Java):**
```bash
./gradlew fatJar
# Output: build/libs/mckli-all.jar
# Run: java --enable-native-access=ALL-UNNAMED -jar build/libs/mckli-all.jar
```

**Native Binary (faster startup, platform-specific):**
```bash
./gradlew linkReleaseExecutableNative
# Output: build/bin/native/releaseExecutable/mckli.kexe
```

**Note:** Native compilation not available on Linux ARM64 hosts. Use JVM target on ARM64.

### For Developers

**Run without building:**
```bash
./gradlew run --args="daemon status"
```

**Run tests:**
```bash
./gradlew test       # All tests
./gradlew jvmTest    # JVM tests only
```

**Build and test everything:**
```bash
./gradlew build
```

## Technology Stack

| Component                 | Version | Purpose                |
|---------------------------|---------|------------------------|
| **Kotlin**                | 2.3.20  | Multiplatform language |
| **Clikt**                 | 5.1.0   | CLI framework          |
| **Ktor**                  | 2.3.12  | HTTP client            |
| **kotlinx.serialization** | 1.7.3   | JSON handling          |
| **kotlinx.coroutines**    | 1.9.0   | Async operations       |

## Configuration

Configuration stored in `~/.config/mckli/servers.json`:

```json
{
  "servers": [
    {
      "name": "myserver",
      "endpoint": "https://mcp.example.com/api",
      "transport": "HTTP",
      "auth": {
        "type": "Bearer",
        "token": "..."
      },
      "timeout": 30000,
      "poolSize": 10
    },
    {
      "name": "streaming-server",
      "endpoint": "https://mcp.example.com/sse",
      "transport": "SSE",
      "timeout": 30000,
      "poolSize": 10
    }
  ],
  "defaultServer": "myserver"
}
```

### Transport Types

**HTTP (default)** - Standard request/response communication:
- Simple POST requests with immediate responses
- Best for most MCP servers
- Lower overhead, predictable behavior

**SSE (Server-Sent Events)** - Streaming communication:
- Persistent connection with server-initiated messages
- Automatic reconnection with exponential backoff
- Supports real-time notifications from server
- Client requests via POST, responses via SSE stream
- Note: JVM platform only (not available on Native builds)

Daemon state in `~/.config/mckli/daemons/`:

- `<server-name>.pid` - Process ID files
- `<server-name>.sock` - Unix domain sockets
- `<server-name>.log` - Daemon logs

## Development

See [Developer Guide](docs/dev/README.md) for:

- Architecture details
- Code organization
- Testing guidelines
- Contributing workflow

## Project Structure

```
mckli/
├── src/
│   ├── commonMain/kotlin/com/mckli/
│   │   ├── config/          # Configuration management
│   │   ├── daemon/          # Daemon lifecycle
│   │   ├── http/            # HTTP client & connection pool
│   │   ├── ipc/             # Unix socket IPC
│   │   ├── client/          # Request routing
│   │   ├── tools/           # Tool discovery & invocation
│   │   └── Main.kt
│   ├── jvmMain/kotlin/      # JVM-specific implementations
│   └── nativeMain/kotlin/   # Native-specific implementations
├── docs/
│   ├── user/                # User documentation
│   └── dev/                 # Developer documentation
├── openspec/                # OpenSpec change documentation
└── build.gradle.kts
```

## Troubleshooting

### Daemon won't start

```bash
# Check logs
cat ~/.config/mckli/daemons/<server>.log
cat ~/.config/mckli/daemons/<server>.err

# Clean up stale processes
mckli daemon status
mckli daemon stop <server> --force
```

### Connection issues

```bash
# Verify server configuration
mckli config list

# Test daemon connectivity
mckli tools list <server>

# Restart daemon
mckli daemon restart <server>
```

### SSE Transport Issues

**Connection not establishing:**
- Check daemon logs for SSE connection errors
- Verify server endpoint supports SSE (typically `/sse` or `/stream`)
- Confirm server is accessible and responding
- Check authentication credentials

**Frequent reconnections:**
- Server may be unstable or overloaded
- Network issues between client and server
- Check daemon logs for error patterns
- Consider increasing timeout in configuration

**"Max reconnection attempts exceeded":**
- Server is down or unreachable
- Configuration error (wrong endpoint, auth failure)
- Stop and restart daemon after fixing server issues
- Default: 10 retries with exponential backoff (1s to 30s)

**SSE only available on JVM:**
- Native builds don't support SSE transport yet
- Use HTTP transport for native builds
- Or use JVM version (`fatJar`) for SSE support

### Tool cache issues

```bash
# Refresh tool cache
mckli tools refresh <server>
```

## License

See [LICENSE](LICENSE) file for details.

## Contributing

This project follows the OpenSpec workflow for managing changes. See `openspec/changes/mcp-server-wrapper/` for the
current implementation documentation.
