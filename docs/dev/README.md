# mckli Developer Guide

Technical documentation for developing and contributing to mckli.

## Table of Contents

1. [Architecture](#architecture)
2. [Code Organization](#code-organization)
3. [Key Components](#key-components)
4. [Development Setup](#development-setup)
5. [Building and Testing](#building-and-testing)
6. [Contributing](#contributing)
7. [Design Decisions](#design-decisions)

---

## Architecture

### High-Level Design

```
┌────────────────────────────────────────────┐
│              CLI Process                   │
│  (stateless, invoked per command)          │
│                                            │
│  ┌──────────┐  ┌──────────┐  ┌─────────┐   │
│  │  Config  │  │  Daemon  │  │  Tools  │   │
│  │ Commands │  │ Commands │  │Commands │   │
│  └─────┬────┘  └─────┬────┘  └────┬────┘   │
│        └──────────────┼────────────┘       │
│                   ┌───▼────┐               │
│                   │Request │               │
│                   │Router  │               │
│                   └───┬────┘               │
└───────────────────────┼────────────────────┘
                        │ HTTP API (Port 5030)
                        │
┌───────────────────────▼────────────────────┐
│         Unified Daemon Process             │
│      (long-lived, handles ALL servers)     │
│                                            │
│  ┌──────────────┐  ┌─────────────────┐     │
│  │ HTTP Server  │  │   Tool Cache    │     │
│  │ (Ktor/Netty) │  │   (per server)  │     │
│  └──────┬───────┘  └────────┬────────┘     │
│         │                    │             │
│    ┌────▼────────────────────▼───────┐     │
│    │    Connection Pool              │     │
│    │  (HTTP/SSE lifecycle mgmt)      │     │
│    └────────────┬────────────────────┘     │
└─────────────────┼──────────────────────────┘
                  │ HTTP/HTTPS or SSE
                  │ (persistent connections)
                  │
          ┌───────▼────────┐
          │   MCP Server   │
          └────────────────┘
```

### Component Interaction Flow

**Tool Invocation Flow:**

```
1. User: mckli tools call myserver read-file --json {...}
2. CLI: Parse arguments, create request
3. RequestRouter: Check if unified daemon running → auto-start if needed
4. RequestRouter: Send HTTP POST to localhost:5030/servers/myserver/tools/call
5. Daemon HTTP Server: Receive request
6. Daemon: Route to appropriate ServerContext
7. ServerContext: Route to ToolCache
8. ToolCache: Execute call via ConnectionPool
9. ConnectionPool: Execute via Ktor HTTP client (SSE or HTTP)
10. MCP Server: Process and respond
11. Ktor Client: Parse response
12. ToolCache: Format result
13. HTTP Server: Send HTTP response
14. RequestRouter: Receive response
15. CLI: Display formatted output to user
```

---

## Code Organization

### Source Structure

```
src/
├── main/kotlin/com/mckli/
│   ├── Main.kt                    # Entry point (CLI)
│   ├── MckliCommand.kt            # Root CLI command
│   │
│   ├── config/                    # Configuration management
│   │   ├── ServerConfig.kt        # Data models
│   │   ├── ConfigManager.kt       # Config I/O
│   │   └── ConfigCommands.kt      # CLI commands
│   │
│   ├── daemon/                    # Daemon lifecycle
│   │   ├── UnifiedDaemon.kt       # Unified daemon logic
│   │   ├── UnifiedDaemonServer.kt # Ktor HTTP server
│   │   ├── DaemonProcess.kt       # Daemon process control
│   │   └── DaemonCommands.kt      # CLI commands
│   │
│   ├── http/                      # Protocol layer
│   │   ├── McpRequest.kt          # MCP protocol types
│   │   └── DaemonHttpClient.kt    # Client for daemon API
│   │
│   ├── tools/                     # Tool management
│   │   ├── ToolCache.kt           # Metadata and execution
│   │   └── ToolSearchService.kt   # Cross-server search
│   │
│   ├── transport/                 # MCP Transport
│   │   ├── TransportFactory.kt    # SSE/HTTP factory
│   │   └── ReconnectionStrategy.kt
│   ├── client/                    # CLI-side request routing
│   │   └── RequestRouter.kt       # CLI-to-daemon routing
│   │
│   └── tools/                     # Tool discovery & invocation
│       ├── ToolMetadata.kt        # Tool data models
│       ├── ToolCache.kt           # In-memory cache
│       ├── ToolSearchService.kt   # Cross-server search
│       └── ToolCommands.kt        # CLI commands
│
└── resources/
    └── logback.xml                # Logging configuration
```

### Module Responsibilities

| Module     | Responsibility                                            |
|------------|-----------------------------------------------------------|
| **config** | Server configuration CRUD, validation, persistence        |
| **daemon** | Unified daemon logic, Ktor server, process management     |
| **http**   | Protocol types and CLI HTTP client (to daemon)            |
| **transport**| MCP Transport implementation (SSE/HTTP)                  |
| **client** | CLI-to-daemon routing, auto-start, error handling         |
| **tools**  | Tool discovery, caching, invocation, search               |

---

## Key Components

### 1. Configuration Management

**Files:** `config/ConfigManager.kt`, `config/ServerConfig.kt`

**Responsibilities:**

- Store/load server configurations from `~/.mckli/servers.json`
- Validate URLs, timeouts, pool sizes
- Handle authentication (Basic, Bearer)

**Key Classes:**

```kotlin
data class ServerConfig(
    val name: String,
    val endpoint: String,
    val auth: AuthConfig? = null,
    val timeout: Long = 30000,
    val poolSize: Int = 10
)

sealed class AuthConfig {
    data class Basic(username: String, password: String)
    data class Bearer(token: String)
}
```

**Platform-specific:**

- JVM: Uses `java.io.File` for JSON I/O
- Native: Uses C interop with `fopen`/`fgets`

### 2. HTTP Transport (to MCP Servers)

**Files:** `transport/TransportFactory.kt`, `transport/ReconnectionStrategy.kt`

**Responsibilities:**

- Implement MCP transport protocols (SSE, HTTP)
- Manage persistent connections to MCP servers
- Handle automatic reconnection with backoff
- Manage connection lifecycle (idle timeout, pool size)

**Key Classes:**

```kotlin
interface McpTransport {
    suspend fun start()
    suspend fun stop()
    suspend fun sendRequest(request: McpRequest): Result<McpResponse>
}

class SseTransport(config: ServerConfig) : McpTransport { ... }
class HttpTransport(config: ServerConfig) : McpTransport { ... }
```

**Features:**

- Default SSE support for modern MCP servers
- Fallback to HTTP for legacy servers
- Persistent connection pooling via Ktor client
- Retry logic with exponential backoff
- Connection validation before reuse

### 3. Daemon Process

**Files:** `daemon/DaemonProcess.kt`, `daemon/DaemonMain.kt`, `daemon/UnifiedDaemon.kt`, `daemon/UnifiedDaemonServer.kt`

**Responsibilities:**

- Spawn the unified daemon as a separate background process
- Manage the PID file in `~/.mckli/daemons/daemon.pid`
- Handle SIGTERM for graceful shutdown
- Initialize all configured MCP servers and their tool caches
- Expose a REST API via Ktor/Netty on port 5030

**Unified Daemon Lifecycle:**

```kotlin
class UnifiedDaemon {
    suspend fun start()    // Initializes servers from config
    suspend fun shutdown() // Graceful cleanup of all connections
}

class UnifiedDaemonServer(daemonManager: DaemonManager) {
    fun start() // Starts Ktor server on port 5030
    fun stop()
}
```

### 4. Daemon API (formerly IPC)

**Files:** `daemon/UnifiedDaemonServer.kt`, `http/DaemonHttpClient.kt`

**Responsibilities:**

- Provide a stable HTTP interface between CLI and Daemon
- Standardize request/response formats using JSON

**Key Endpoints:**

- `GET /health`: Unified status of all managed servers
- `GET /servers/{name}/tools`: List tools for a specific server
- `POST /servers/{name}/tools/call`: Execute a tool on a specific server
- `POST /servers/{name}/refresh`: Force metadata refresh

### 5. Tool Cache

**Files:** `tools/ToolCache.kt`, `tools/ToolMetadata.kt`, `tools/ToolSearchService.kt`

**Responsibilities:**

- Fetch tool lists on server initialization
- Maintain in-memory cache of tool schemas and metadata
- Provide cross-server tool searching
- Forward tool calls to MCP transport layer

**Key Operations:**

```kotlin
class ToolCache(transport: McpTransport) {
    suspend fun refresh()
    fun listTools(filter: String?): List<ToolMetadata>
    suspend fun callTool(name: String, arguments: JsonObject?): Result<JsonElement>
}
```

### 6. Request Router

**Files:** `client/RequestRouter.kt`

**Responsibilities:**

- Route CLI commands to the Unified Daemon via HTTP
- Automatically start the daemon if it's not running
- Handle connection timeouts and daemon failures
- Gracefully handle server-specific errors

**Flow:**

1. CLI command invoked (e.g., `tools call`)
2. `RequestRouter` checks if daemon is alive (`GET /health`)
3. If not alive, `DaemonProcess` spawns a new background JVM process
4. `RequestRouter` waits for daemon to become ready
5. Request is forwarded to Daemon API
6. Daemon processes request and returns JSON
7. CLI formats and displays result

---

## Development Setup

### Prerequisites

```bash
# Install Java 21
# macOS
brew install openjdk@21

# Linux
sudo apt-get install openjdk-21-jdk

# Verify
java -version
```

### Clone and Build

```bash
git clone <repository>
cd mckli
./gradlew build
```

### Run in Development

```bash
# Run JVM version with arguments
./gradlew jvmRun --args="config list"

# Run with debugging
./gradlew jvmRun --args="--help" --debug-jvm
```

### IDE Setup

**IntelliJ IDEA:**

1. Open project
2. Gradle auto-import
3. Mark `src/commonMain/kotlin` as sources root
4. Set SDK to Java 21

**VS Code:**

1. Install Kotlin extension
2. Open project folder
3. Use Gradle tasks panel

---

## Building and Testing

### Build Commands

```bash
# Clean build
./gradlew clean build

# Build JVM only
./gradlew jvmJar

# Build native binary
./gradlew linkReleaseExecutableNative

# Build debug native
./gradlew linkDebugExecutableNative
```

### Run Tests

```bash
# Run all tests
./gradlew test

# Run common tests only
./gradlew commonTest

# Run JVM tests
./gradlew jvmTest

# Run with reports
./gradlew test --info
```

### Code Quality

```bash
# Check formatting
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat

# Full check
./gradlew check
```

---

## Contributing

### Workflow

1. **Create a feature branch**
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Make changes**
    - Follow existing code style
    - Add tests for new functionality
    - Update documentation

3. **Test locally**
   ```bash
   ./gradlew build test
   ```

4. **Commit with clear message**
   ```bash
   git commit -m "Add feature: description"
   ```

5. **Push and create PR**

### Code Style

- **Kotlin conventions:** Follow [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html)
- **Naming:**
    - Classes: PascalCase
    - Functions: camelCase
    - Constants: UPPER_SNAKE_CASE
- **Documentation:** KDoc for public APIs
- **Tests:** Use descriptive test names

### Adding New Features

#### Adding a New CLI Command

1. **Create command class:**
   ```kotlin
   // src/commonMain/kotlin/com/mckli/myfeature/MyCommands.kt
   class MyCommand : CliktCommand(name = "mycommand") {
       override fun run() {
           echo("My feature!")
       }
   }
   ```

2. **Register in Main.kt:**
   ```kotlin
   fun main(args: Array<String>) = MckliCommand()
       .subcommands(
           ConfigCommand(),
           DaemonCommand(),
           ToolsCommand(),
           MyCommand()  // Add here
       )
       .main(args)
   ```

#### Adding a New IPC Message Type

1. **Add to IpcMessage.kt:**
   ```kotlin
   sealed class IpcRequest {
       // ...
       data class MyRequest(
           override val requestId: String,
           val myParam: String
       ) : IpcRequest()
   }
   ```

2. **Handle in UnixSocketServer:**
   ```kotlin
   private suspend fun processRequest(request: IpcRequest): IpcResponse {
       return when (request) {
           // ...
           is IpcRequest.MyRequest -> {
               // Handle request
               IpcResponse.Success(request.requestId, result)
           }
       }
   }
   ```

3. **Add to RequestRouter:**
   ```kotlin
   fun myFeature(param: String): Result<JsonElement> {
       return executeRequest { _, requestId ->
           IpcRequest.MyRequest(requestId, param)
       }
   }
   ```

---

## Design Decisions

### Why Unix Domain Sockets?

**Alternatives considered:**

- HTTP on localhost → More overhead, port conflicts
- Shared memory → Complex, platform-specific
- Named pipes → Windows-only

**Chosen:** Unix domain sockets

- Lower latency than TCP
- Filesystem permissions for security
- Cross-platform (with named pipes fallback on Windows)

### Why Separate Daemon Processes?

**Alternatives considered:**

- In-process threading → CLI would need to stay running
- Single daemon for all servers → One failure affects all

**Chosen:** One daemon per server

- Isolation: failures don't cascade
- Resource management: individual control
- Simplicity: separate PID files, logs

### Why Ktor for HTTP?

**Alternatives considered:**

- OkHttp (JVM only) → Breaks Native support
- java.net.http → JVM only
- Platform-specific libs → Duplicate code

**Chosen:** Ktor with CIO engine

- Multiplatform support (JVM + Native)
- Built-in connection pooling
- Coroutine-native async
- Mature and well-documented

### Why JSON for Configuration?

**Alternatives considered:**

- YAML → Limited Kotlin multiplatform support
- TOML → Less common in Kotlin ecosystem

**Chosen:** JSON with kotlinx.serialization

- Multiplatform support
- Type-safe serialization
- Easy to validate

---

## Technology Stack

| Component                 | Version | Purpose                |
|---------------------------|---------|------------------------|
| **Kotlin**                | 2.3.20  | Multiplatform language |
| **Clikt**                 | 5.1.0   | CLI framework          |
| **Ktor**                  | 2.3.12  | HTTP client            |
| **kotlinx.serialization** | 1.7.3   | JSON handling          |
| **kotlinx.coroutines**    | 1.9.0   | Async operations       |

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

## Troubleshooting Development Issues

### Build Fails

```bash
# Clean and rebuild
./gradlew clean build --stacktrace

# Check Java version
java -version  # Must be 21+

# Update Gradle wrapper
./gradlew wrapper --gradle-version 9.4.1
```

### Native Build Fails

```bash
# Check platform support
uname -m  # Linux ARM64 not supported

# Use JVM target instead
./gradlew jvmJar
```

### Tests Fail

```bash
# Run with detailed output
./gradlew test --info --stacktrace

# Run specific test
./gradlew test --tests "com.mckli.config.ConfigManagerTest"
```

---

## Next Steps

- Read [OpenSpec change documentation](../../openspec/changes/mcp-server-wrapper/)
- Check [User Manual](../user/README.md) for usage examples
- Review [Architecture Decision Records](../../openspec/changes/mcp-server-wrapper/design.md)
