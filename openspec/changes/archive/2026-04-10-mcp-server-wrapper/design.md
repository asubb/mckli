## Context

The current CLI tool is a basic Kotlin Multiplatform application using Clikt for command-line parsing, targeting both
JVM and native platforms. It needs to evolve into an MCP server wrapper that maintains persistent HTTP connections to
remote MCP servers through daemon processes, minimizing token usage from repeated connection overhead.

**Current State:**

- Simple CLI with Clikt command structure
- Kotlin Multiplatform (JVM + Native targets)
- No networking or daemon capabilities
- Stateless execution model

**Constraints:**

- Must maintain Kotlin Multiplatform compatibility (JVM + Native)
- Native targets have limited HTTP client options (ktor-client supports both)
- Daemon processes need to survive across CLI invocations
- CLI must remain responsive (async operations required)

## Goals / Non-Goals

**Goals:**

- Reduce token costs by maintaining persistent connections to MCP servers
- Support multiple concurrent MCP server instances with dedicated daemons
- Provide seamless CLI experience for daemon lifecycle management
- Enable HTTP-only access to remote MCP servers
- Maintain Kotlin Multiplatform compatibility (JVM + Native)
- Expose MCP tools as stateless CLI commands that LLMs can invoke directly
- Cache tool metadata in daemons to eliminate discovery overhead on each invocation

**Non-Goals:**

- Direct stdio/socket-based MCP server communication (HTTP only)
- Built-in MCP server implementation (wrapper only)
- GUI interface for daemon management
- Automatic MCP server discovery or service registry
- Authentication/authorization beyond basic HTTP auth

## Decisions

### 1. Daemon Architecture: Separate Process vs In-Process

**Decision:** Use separate daemon processes (one per MCP server instance) that communicate with the CLI via local IPC (
Unix sockets or named pipes).

**Rationale:**

- **Isolation:** Each MCP server connection runs in its own process, preventing cascading failures
- **Persistence:** Daemons survive CLI invocations, maintaining long-lived connections
- **Resource Management:** Can individually stop/restart problematic daemons
- **Process Monitoring:** Easier to track memory/CPU usage per MCP server

**Alternatives Considered:**

- In-process threading: Would require CLI to stay running, defeating purpose of reducing overhead
- Single daemon for all servers: One failure would affect all connections, harder to manage

### 2. HTTP Client: Ktor vs Platform-Specific

**Decision:** Use Ktor Client with CIO engine for multiplatform HTTP support.

**Rationale:**

- **Multiplatform:** Works on both JVM and Native targets
- **Coroutines:** Native async/await support with Kotlin coroutines
- **Mature:** Production-ready with extensive MCP protocol support (JSON, SSE for streaming)
- **Configurable:** Easy connection pooling, timeout, retry logic

**Alternatives Considered:**

- Platform-specific clients (OkHttp for JVM, curl for Native): Would require duplicate code, harder maintenance
- java.net.http: JVM-only, breaks Native target compatibility

### 3. IPC Mechanism: Unix Sockets vs HTTP

**Decision:** Use Unix domain sockets for CLI-to-daemon communication (with named pipes fallback for Windows).

**Rationale:**

- **Performance:** Lower latency than TCP/HTTP localhost
- **Security:** Filesystem permissions control access, no network exposure
- **Simplicity:** Request-response pattern fits well with socket communication
- **Native Support:** Both JVM and Native Kotlin have socket support

**Alternatives Considered:**

- HTTP on localhost: More overhead, requires port management, potential conflicts
- Shared memory: Complex to implement safely across process boundaries, platform-specific

### 4. Configuration Format: YAML vs TOML vs JSON

**Decision:** Use JSON for configuration with kotlinx.serialization.

**Rationale:**

- **Multiplatform:** kotlinx.serialization works on all targets
- **Simple:** Adequate for MCP server endpoint definitions
- **Tooling:** Easy to validate and generate

**Alternatives Considered:**

- YAML: More human-friendly but limited multiplatform library support
- TOML: Better for configuration but less common in Kotlin ecosystem

### 5. Daemon Lifecycle: PID Files vs System Service

**Decision:** Use PID files stored in user config directory (`~/.config/mckli/daemons/`) for daemon tracking.

**Rationale:**

- **Portability:** Works across all platforms (Linux, macOS, Windows)
- **Simplicity:** No system service registration required, easier for users
- **User-scoped:** Each user manages their own daemons, no root access needed

**Alternatives Considered:**

- systemd/launchd services: Platform-specific, requires elevated permissions, overkill for user tool
- In-memory registry: Doesn't survive system reboots or crashes

### 6. MCP Protocol Handling: Full Implementation vs Proxy

**Decision:** Implement a thin proxy layer that forwards requests to remote MCP servers, caching connection state and
tool metadata.

**Rationale:**

- **Simplicity:** Don't need to understand full MCP protocol semantics
- **Flexibility:** Works with any MCP server version/extensions
- **Maintenance:** MCP protocol changes don't require wrapper updates
- **Caching:** Tool metadata cached in daemon eliminates repeated discovery calls

**Alternatives Considered:**

- Full MCP client implementation: Complex, brittle to protocol changes, unnecessary for proxy use case

### 7. Tool Discovery: On-Demand vs Cached

**Decision:** Daemons fetch and cache tool metadata on startup, refreshable via CLI command.

**Rationale:**

- **Performance:** Eliminates repeated tool discovery calls that consume tokens
- **Responsiveness:** CLI instantly returns available tools without network latency
- **Consistency:** All CLI invocations see same tool list until explicit refresh
- **LLM-friendly:** Simple CLI commands like `mckli tools list <server>` and `mckli tools call <server> <tool> <args>`

**Alternatives Considered:**

- On-demand discovery: Every CLI call would need to query MCP server, defeating token reduction goal
- Periodic auto-refresh: Adds complexity, unclear when changes occur, prefer explicit refresh

### 8. Tool Invocation: Direct CLI vs JSON Request

**Decision:** Provide direct CLI commands for tool invocation with argument parsing (e.g.,
`mckli tools call myserver read-file --path=/foo/bar`).

**Rationale:**

- **LLM Integration:** LLMs can generate simple shell commands without JSON formatting
- **User Experience:** Humans can also invoke tools naturally from shell
- **Type Safety:** CLI can validate arguments before sending to daemon
- **Discoverability:** Standard `--help` shows tool parameters

**Alternatives Considered:**

- JSON-only interface: Requires LLMs to format JSON, more error-prone, less human-friendly
- REPL mode: Adds complexity, doesn't fit stateless execution model

## Risks / Trade-offs

**[Risk]** Daemon processes orphaned after crash → **Mitigation:** CLI startup checks for stale PID files and cleans
them up, validates processes are actually running

**[Risk]** Network failures between daemon and remote MCP server → **Mitigation:** Implement exponential backoff retry
with circuit breaker pattern, surface errors to CLI

**[Risk]** Memory leaks in long-running daemon processes → **Mitigation:** Implement periodic health checks, provide
`mckli daemon restart` command, monitor connection pool sizes

**[Risk]** Concurrent access to same daemon from multiple CLI invocations → **Mitigation:** Daemon accepts multiple IPC
connections, uses coroutine-based request handling for concurrency

**[Risk]** Native target may have limitations with coroutines/networking → **Mitigation:** Prioritize JVM target
initially, validate Native capabilities early in implementation

**[Trade-off]** Increased complexity for users (daemon management) vs token cost savings → Provide sane defaults,
auto-start daemons on first use, clear status commands

**[Trade-off]** Memory overhead of persistent daemons vs connection reuse benefits → Configure max connection lifetime,
idle timeout for unused daemons

## Migration Plan

1. **Phase 1 - Core Daemon Framework:**
    - Implement daemon process spawning and PID file management
    - Create IPC communication layer (Unix sockets)
    - Build basic CLI commands: `daemon start`, `daemon stop`, `daemon status`

2. **Phase 2 - HTTP Client Integration:**
    - Add Ktor client with connection pooling
    - Implement MCP protocol proxy (request forwarding)
    - Add configuration loading for MCP server endpoints

3. **Phase 3 - Tool Discovery and Caching:**
    - Implement tool metadata fetching on daemon startup
    - Add tool cache management in daemon
    - Create CLI commands for tool listing and inspection

4. **Phase 4 - Tool Invocation:**
    - Implement CLI-to-daemon request routing for tool calls
    - Add argument parsing and validation
    - Create response formatting for tool results

5. **Phase 5 - Polish:**
    - Add auto-start for daemons on first request
    - Implement cleanup for stale daemons
    - Add comprehensive logging and debugging support

**Rollback Strategy:**

- Old CLI remains functional during development
- Feature flag to enable/disable daemon mode
- Clear migration guide for users switching from direct MCP access

## Open Questions

- Should we support WebSocket connections to MCP servers in addition to HTTP? (Defer to v2 unless requirement emerges)
- What's the ideal idle timeout for unused daemon processes? (Start with 30 minutes, make configurable)
- Should daemons support reconnection to MCP servers on network failure, or fail fast? (Reconnect with exponential
  backoff up to 3 attempts, then fail and require manual restart)
- How often should tool metadata be refreshed? (Manual refresh via `mckli tools refresh <server>` to keep control
  explicit)
- Should tool arguments support both flag-style and JSON input? (Start with flags, add JSON fallback for complex nested
  structures)
