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
│  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │  Config  │  │  Daemon  │  │  Tools  │ │
│  │ Commands │  │ Commands │  │Commands │ │
│  └─────┬────┘  └─────┬────┘  └────┬────┘ │
│        └──────────────┼────────────┘      │
│                   ┌───▼────┐              │
│                   │Request │              │
│                   │Router  │              │
│                   └───┬────┘              │
└───────────────────────┼────────────────────┘
                        │ IPC (Unix sockets)
                        │
┌───────────────────────▼────────────────────┐
│         Daemon Process (per server)        │
│      (long-lived, one per MCP server)      │
│                                            │
│  ┌──────────────┐  ┌─────────────────┐   │
│  │ IPC Server   │  │   Tool Cache    │   │
│  │ (Unix Socket)│  │   (in-memory)   │   │
│  └──────┬───────┘  └────────┬────────┘   │
│         │                    │            │
│    ┌────▼────────────────────▼───────┐   │
│    │    Connection Pool              │   │
│    │  (HTTP client lifecycle mgmt)   │   │
│    └────────────┬────────────────────┘   │
└─────────────────┼──────────────────────────┘
                  │ HTTP/HTTPS
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
2. CLI: Parse arguments, create IpcRequest
3. RequestRouter: Check if daemon running → auto-start if needed
4. RequestRouter: Connect to Unix socket
5. Daemon IPC Server: Receive request
6. Daemon: Route to ToolCache
7. ToolCache: Build MCP request
8. ConnectionPool: Execute via HTTP client
9. HTTP Client: POST to MCP server
10. MCP Server: Process and respond
11. HTTP Client: Parse response
12. ConnectionPool: Return to ToolCache
13. ToolCache: Format result
14. IPC Server: Send IpcResponse
15. RequestRouter: Receive response
16. CLI: Display formatted output to user
```

---

## Code Organization

### Source Structure

```
src/
├── commonMain/kotlin/com/mckli/
│   ├── Main.kt                    # Entry point
│   ├── MckliCommand.kt            # Root CLI command
│   │
│   ├── config/                    # Configuration management
│   │   ├── ServerConfig.kt        # Data models
│   │   ├── ConfigManager.kt       # Abstract config I/O
│   │   └── ConfigCommands.kt      # CLI commands
│   │
│   ├── daemon/                    # Daemon lifecycle
│   │   ├── DaemonProcess.kt       # Abstract daemon control
│   │   └── DaemonCommands.kt      # CLI commands
│   │
│   ├── http/                      # HTTP client layer
│   │   ├── McpRequest.kt          # MCP protocol types
│   │   ├── HttpMcpClient.kt       # Ktor HTTP client
│   │   └── ConnectionPool.kt      # Connection lifecycle
│   │
│   ├── ipc/                       # Inter-process communication
│   │   └── IpcMessage.kt          # IPC protocol types
│   │
│   ├── client/                    # CLI-side request routing
│   │   └── RequestRouter.kt       # Abstract IPC client
│   │
│   └── tools/                     # Tool discovery & invocation
│       ├── ToolMetadata.kt        # Tool data models
│       ├── ToolCache.kt           # In-memory cache
│       └── ToolCommands.kt        # CLI commands
│
├── jvmMain/kotlin/com/mckli/
│   ├── config/
│   │   └── ConfigManager.jvm.kt   # File I/O implementation
│   │
│   ├── daemon/
│   │   ├── DaemonProcess.jvm.kt   # Process spawning
│   │   └── DaemonMain.kt          # Daemon entry point
│   │
│   ├── ipc/
│   │   ├── UnixSocketServer.kt    # IPC server
│   │   └── UnixSocketClient.kt    # IPC client
│   │
│   └── client/
│       └── RequestRouter.jvm.kt   # IPC implementation
│
└── nativeMain/kotlin/com/mckli/
    └── config/
        └── ConfigManager.native.kt # Native file I/O
```

### Module Responsibilities

| Module     | Responsibility                                            |
|------------|-----------------------------------------------------------|
| **config** | Server configuration CRUD, validation, persistence        |
| **daemon** | Daemon process lifecycle, PID management, process control |
| **http**   | HTTP client, MCP protocol, connection pooling             |
| **ipc**    | Unix socket communication, message serialization          |
| **client** | CLI-to-daemon routing, auto-start, error handling         |
| **tools**  | Tool discovery, caching, invocation                       |

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

### 2. HTTP Client

**Files:** `http/HttpMcpClient.kt`, `http/ConnectionPool.kt`

**Responsibilities:**

- Send MCP requests to remote servers via HTTP POST
- Handle authentication (headers)
- Manage connection lifecycle (idle timeout, max lifetime)
- Error handling (network, HTTP errors, timeouts)

**Key Classes:**

```kotlin
class HttpMcpClient(config: ServerConfig) {
    private val client: HttpClient  // Ktor CIO engine

    suspend fun sendRequest(request: McpRequest): Result<McpResponse>
}

class ConnectionPool(config: ServerConfig) {
    suspend fun <T> executeRequest(block: suspend (HttpMcpClient) -> T): T
    suspend fun shutdown()
}
```

**Features:**

- Ktor CIO engine for connection pooling
- Automatic timeout handling
- Retry logic with exponential backoff
- Connection validation before reuse

### 3. Daemon Process

**Files:** `daemon/DaemonProcess.kt`, `daemon/DaemonMain.kt`

**Responsibilities:**

- Spawn daemon as separate process
- Manage PID files in `~/.mckli/daemons/`
- Handle SIGTERM for graceful shutdown
- Initialize HTTP client and tool cache on startup

**JVM Implementation:**

```kotlin
class DaemonProcess(config: ServerConfig) {
    fun start(): Result<Unit>  // Spawn with ProcessBuilder
    fun stop(force: Boolean): Result<Unit>  // SIGTERM or SIGKILL
    fun isRunning(): Boolean  // Check PID validity
}
```

**Daemon Main:**

```kotlin
// DaemonMain.kt
fun main(args: Array<String>) {
    val serverName = args[0]
    val daemon = Daemon(serverName)
    daemon.start()  // Blocks forever until SIGTERM
}
```

### 4. IPC Layer

**Files:** `ipc/IpcMessage.kt`, `ipc/UnixSocketServer.kt`, `ipc/UnixSocketClient.kt`

**Responsibilities:**

- Define request/response protocol
- Unix domain socket server (daemon side)
- Unix domain socket client (CLI side)

**Protocol:**

```kotlin
sealed class IpcRequest {
    data class McpRequest(id, method, params)
    data class ListTools(id, filter)
    data class CallTool(id, toolName, arguments)
    // ...
}

sealed class IpcResponse {
    data class Success(id, result)
    data class Error(id, error, details)
}
```

**Socket Communication:**

- One request per connection
- Line-based protocol (JSON + newline)
- Concurrent connections handled with coroutines

### 5. Tool Cache

**Files:** `tools/ToolCache.kt`, `tools/ToolMetadata.kt`

**Responsibilities:**

- Fetch tool list on daemon startup
- Cache in-memory for fast access
- Provide query interface (list, describe)
- Forward tool calls to MCP server

**Key Operations:**

```kotlin
class ToolCache(connectionPool: ConnectionPool) {
    suspend fun refresh()  // Fetch from MCP server
    suspend fun listTools(filter: String?): List<ToolMetadata>
    suspend fun getTool(name: String): ToolMetadata?
    suspend fun callTool(name, args, pool): Result<JsonElement>
}
```

### 6. Request Router

**Files:** `client/RequestRouter.kt`

**Responsibilities:**

- Route CLI requests to appropriate daemon
- Auto-start daemons if not running
- Handle IPC connection errors
- Format responses for display

**Flow:**

```kotlin
class RequestRouter(serverName: String?) {
    fun callTool(toolName, arguments): Result<JsonElement> {
        // 1. Get server config
        // 2. Check daemon status
        // 3. Auto-start if needed
        // 4. Connect to Unix socket
        // 5. Send IPC request
        // 6. Return response
    }
}
```

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
