## Why

The current implementation of `mckli` uses a separate daemon process for each MCP server, and communicates with these processes via Unix domain sockets (IPC). This architecture has several drawbacks:
- Resource overhead: Each daemon process consumes its own memory and CPU.
- Management complexity: Users have to manage and troubleshoot multiple background processes.
- Portability: Unix domain sockets are less portable than standard HTTP.
- Complexity: IPC protocol requires custom serialization and socket management.

By consolidating into a single daemon and using HTTP for all communication, we simplify the architecture, reduce resource consumption, and make the tool more robust and easier to manage.

## What Changes

- **Daemon Consolidation**: A single background daemon process will now manage connections to ALL configured MCP servers.
- **Protocol Shift**: Replace Unix domain sockets (IPC) with standard HTTP (REST/JSON) for communication between the CLI and the daemon.
- **Daemon Lifecycle**: The daemon will be started on the first CLI command if it's not running and will listen on a fixed (or configurable) local port.
- **Server Multiplexing**: The daemon will handle requests for different servers by including the server name in the HTTP request (e.g., as a header or path parameter).
- **Tool Management**: The unified daemon will maintain caches and connections for all active MCP servers.
- **BREAKING**: IPC-related code (UnixSocketServer, UnixSocketClient) will be removed or replaced.
- **BREAKING**: `DaemonProcess` and related CLI commands will be updated to handle a single daemon instead of per-server daemons.

## Capabilities

### New Capabilities
- `unified-daemon`: A single background process that manages multiple MCP server connections.
- `daemon-http-api`: A RESTful API for the CLI to interact with the daemon.

### Modified Capabilities
- `daemon-process-manager`: Updated to manage only one global daemon process.
- `mcp-server-wrapper`: Updated to use the unified daemon and HTTP transport.
- `request-routing`: Updated to route requests through the single daemon using the HTTP API.

## Impact

- **CLI-Daemon Communication**: Transition from Unix sockets to localhost HTTP.
- **Configuration**: Potential addition of daemon port/host settings.
- **Bootstrap**: CLI will need to check if the global daemon is running and start it if needed.
- **Maintenance**: Simplified troubleshooting as there's only one daemon log and process to monitor.
- **Portability**: Improved compatibility on non-Unix systems (though primarily JVM/Native focused here).
