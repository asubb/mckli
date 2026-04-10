## Context

The current architecture of `mckli` uses one daemon process per configured MCP server. Communication between the CLI and these daemons happens over Unix Domain Sockets (IPC). This design is resource-intensive and complex to maintain. We aim to consolidate into a single daemon process that handles all MCP servers and communicates with the CLI over HTTP.

## Goals / Non-Goals

**Goals:**
- Consolidate all MCP server management into a single `mckli-daemon` process.
- Replace Unix Domain Sockets with a local HTTP server for CLI-daemon communication.
- Support multiplexing multiple MCP servers within the same daemon.
- Automate daemon lifecycle management (start on demand).
- Maintain existing functionality (tool listing, calling, caching).

**Non-Goals:**
- Supporting remote daemons (localhost only for now).
- Changing the underlying MCP transport (HTTP/SSE to actual MCP servers remains).
- Implementing complex authentication for the local daemon API.

## Decisions

### 1. HTTP over Unix Sockets
- **Decision**: Use a local HTTP server (Ktor) running on a fixed or configurable port (default 5030).
- **Rationale**: HTTP is more standard, easier to debug, and has better cross-platform support in Kotlin Multiplatform.
- **Alternatives**: Keeping Unix sockets but multiplexing them. This wouldn't solve the portability and debugging issues as effectively as HTTP.

### 2. Single Daemon Process
- **Decision**: A single daemon process will manage a `Map<String, ServerConnection>` where each entry corresponds to an MCP server configuration.
- **Rationale**: Drastically reduces resource usage and simplifies process management for the user.
- **Alternatives**: Keeping multiple daemons. Rejected due to overhead.

### 3. API Routing
- **Decision**: Use URL path parameters to specify the target server, e.g., `POST /servers/{serverName}/tools/call`.
- **Rationale**: Clear, RESTful, and easy to route within the daemon.
- **Alternatives**: Custom headers or including server name in the request body. Path parameters are more standard for resource identification.

### 4. Daemon Lifecycle Management
- **Decision**: The CLI will check for daemon availability (via a `GET /health` or similar) and spawn it if missing. A PID file will be used to track the process.
- **Rationale**: Provides a seamless user experience where the daemon "just works" without manual startup.
- **Alternatives**: Requiring the user to start the daemon manually. Rejected for poor UX.

## Risks / Trade-offs

- **[Risk] Port Conflict** → [Mitigation] Allow configuring the daemon port in `config.yaml`, and provide clear error messages if the port is taken.
- **[Risk] Security** → [Mitigation] Bind the daemon HTTP server to `localhost` (127.0.0.1) ONLY.
- **[Risk] State Synchronization** → [Mitigation] Ensure the daemon reloads configuration if it changes, or provides an endpoint to trigger a reload.
- **[Risk] Shutdown Race Conditions** → [Mitigation] Use robust PID file management and SIGTERM handling.
