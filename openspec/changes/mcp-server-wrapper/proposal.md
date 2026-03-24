## Why

LLM calls to MCP (Model Context Protocol) servers incur high token costs due to repeated connection overhead and context
transmission. Converting the CLI tool to an MCP server wrapper with persistent HTTP daemon processes will significantly
reduce token usage by maintaining long-lived connections and reusing context across requests.

## What Changes

- Transform CLI from a simple command-line tool to an MCP server wrapper that proxies requests to remote MCP servers
  over HTTP
- Implement daemon process management to maintain persistent connections to each MCP server instance
- Add HTTP client functionality for communicating with remote MCP servers
- Introduce connection pooling and lifecycle management for daemon processes
- Add configuration system for managing multiple MCP server endpoints
- Implement request routing and multiplexing across daemon processes
- Expose MCP tools as CLI commands that LLMs can invoke directly (e.g., `mckli tool <server> <tool-name> <args>`)
- Daemons cache tool metadata on startup and CLI fetches this for tool discovery and execution

## Capabilities

### New Capabilities

- `mcp-http-client`: HTTP client implementation for communicating with remote MCP servers, including request/response
  handling, authentication, and error management
- `daemon-process-manager`: Background daemon process lifecycle management for maintaining persistent connections to MCP
  server instances
- `connection-pooling`: Connection pool management for efficient reuse of HTTP connections and MCP sessions across
  requests
- `mcp-server-config`: Configuration system for defining and managing multiple MCP server endpoints with their
  connection parameters
- `request-routing`: Request routing and multiplexing logic to distribute LLM requests across appropriate daemon
  processes
- `mcp-tool-discovery`: Tool metadata discovery and caching in daemons, with CLI commands to list and inspect available
  tools
- `mcp-tool-execution`: CLI command interface for invoking MCP tools, allowing LLMs to execute tools via simple
  command-line calls

### Modified Capabilities

<!-- No existing capabilities are being modified - this is a complete architectural transformation -->

## Impact

- **Architecture**: Complete redesign from simple CLI to client-server architecture with daemon processes
- **Dependencies**: New HTTP client libraries (e.g., ktor-client for Kotlin Multiplatform), coroutines for async
  operations, serialization libraries for MCP protocol
- **Build configuration**: Updated gradle dependencies for networking and async capabilities
- **Deployment**: New daemon process lifecycle management, potentially system service integration
- **User interaction**: New commands for daemon management (start, stop, status), server configuration, and direct tool
  invocation (list, describe, call)
- **Performance**: Reduced token usage through connection reuse, but increased memory footprint for daemon processes
