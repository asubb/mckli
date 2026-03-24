## 1. Project Setup and Dependencies

- [x] 1.1 Add Ktor client dependency to build.gradle.kts for HTTP communication
- [x] 1.2 Add kotlinx.serialization dependency for JSON handling
- [x] 1.3 Add kotlinx.coroutines dependency for async operations
- [x] 1.4 Configure Ktor CIO engine for multiplatform support
- [x] 1.5 Create module structure for daemon, client, and config components

## 2. Configuration System

- [x] 2.1 Define ServerConfig data class with name, endpoint, auth, timeout, pool size
- [x] 2.2 Implement configuration file reader for ~/.config/mckli/servers.json
- [x] 2.3 Add configuration validation logic for URL format, timeout values
- [x] 2.4 Create configuration file writer with pretty-printed JSON
- [x] 2.5 Implement `config add` command for adding new servers
- [x] 2.6 Implement `config remove` command for removing servers
- [x] 2.7 Implement `config list` command for listing configured servers
- [x] 2.8 Implement `config edit` command for editing server configuration
- [x] 2.9 Add default configuration creation when servers.json is missing

## 3. HTTP Client Implementation

- [x] 3.1 Create HttpMcpClient class with Ktor HttpClient instance
- [x] 3.2 Implement client initialization with timeout, pool size, and retry settings
- [x] 3.3 Add HTTP POST request method for MCP protocol forwarding
- [x] 3.4 Implement Basic authentication support in request headers
- [x] 3.5 Implement Bearer token authentication support in request headers
- [x] 3.6 Add JSON request serialization using kotlinx.serialization
- [x] 3.7 Add JSON response parsing with error handling
- [x] 3.8 Implement error handling for network failures
- [x] 3.9 Implement error handling for HTTP 4xx and 5xx responses
- [x] 3.10 Implement timeout handling with cancellation
- [x] 3.11 Add connection reuse through Ktor connection pool

## 4. Connection Pooling

- [x] 4.1 Create ConnectionPool class for managing HTTP connections per daemon
- [x] 4.2 Implement connection acquisition logic with availability check
- [x] 4.3 Add connection creation when pool is empty
- [x] 4.4 Implement waiting/timeout when pool is at max capacity
- [x] 4.5 Add idle connection timeout mechanism (default 5 minutes)
- [x] 4.6 Implement connection validation before reuse
- [x] 4.7 Add maximum connection lifetime enforcement (default 30 minutes)
- [x] 4.8 Implement connection pool shutdown logic
- [x] 4.9 Add pool metrics tracking (active, idle, total created)
- [x] 4.10 Implement connection leak detection warnings

## 5. Daemon Process Management

- [x] 5.1 Create DaemonProcess class for daemon lifecycle management
- [x] 5.2 Implement daemon process spawning with server configuration
- [x] 5.3 Add PID file creation in ~/.config/mckli/daemons/ directory
- [x] 5.4 Implement PID file reading and process validation
- [x] 5.5 Add stale PID file cleanup on CLI startup
- [x] 5.6 Implement `daemon start` command with server name parameter
- [x] 5.7 Implement `daemon stop` command with graceful SIGTERM
- [x] 5.8 Implement `daemon status` command listing all daemons
- [x] 5.9 Implement `daemon restart` command (stop + start)
- [x] 5.10 Add forced termination with SIGKILL after 10 second timeout
- [x] 5.11 Implement daemon auto-start on first request
- [x] 5.12 Add multi-daemon support for concurrent server instances

## 6. Daemon Process Implementation

- [x] 6.1 Create daemon main entry point separate from CLI
- [x] 6.2 Initialize HTTP client and connection pool in daemon process
- [x] 6.3 Implement Unix domain socket server for IPC (or named pipes on Windows)
- [x] 6.4 Add signal handler for graceful SIGTERM shutdown
- [x] 6.5 Implement request receiving from IPC socket
- [x] 6.6 Add request deserialization from JSON
- [x] 6.7 Implement request forwarding to MCP server via HTTP client
- [x] 6.8 Add response serialization to JSON
- [x] 6.9 Implement response sending via IPC socket
- [x] 6.10 Add concurrent request handling with coroutines
- [x] 6.11 Implement request correlation with unique IDs
- [x] 6.12 Add error propagation from MCP server to CLI
- [x] 6.13 Implement daemon logging for debugging

## 7. Request Routing (CLI to Daemon)

- [x] 7.1 Create RequestRouter class for CLI-to-daemon communication
- [x] 7.2 Implement Unix domain socket client connection
- [x] 7.3 Add connection timeout handling (5 seconds)
- [x] 7.4 Implement request serialization to JSON with request ID
- [x] 7.5 Add server selection logic based on CLI parameters
- [x] 7.6 Implement default server selection from configuration
- [x] 7.7 Add request sending via IPC socket
- [x] 7.8 Implement response receiving and deserialization
- [x] 7.9 Add response timeout handling (default 60 seconds)
- [x] 7.10 Implement request-response correlation by request ID
- [x] 7.11 Add error handling for daemon not running (trigger auto-start)
- [x] 7.12 Implement error message formatting for user display

## 8. Tool Discovery and Caching

- [x] 8.1 Define ToolMetadata data class for tool name, description, parameter schema
- [x] 8.2 Implement MCP tools/list endpoint call in HTTP client
- [x] 8.3 Add tool metadata fetching on daemon startup
- [x] 8.4 Create in-memory tool cache in daemon process
- [x] 8.5 Implement tool cache refresh logic triggered by CLI
- [x] 8.6 Add tool cache query interface via IPC
- [x] 8.7 Implement `tools list` CLI command with daemon query
- [x] 8.8 Implement `tools describe` CLI command for detailed tool info
- [x] 8.9 Implement `tools refresh` CLI command to update cache
- [x] 8.10 Add tool filtering by name pattern in list command
- [x] 8.11 Handle empty tool cache gracefully (show message to user)

## 9. Tool Invocation

- [x] 9.1 Implement `tools call` CLI command with server and tool name
- [x] 9.2 Add CLI flag parser for tool arguments based on cached schema
- [x] 9.3 Implement type conversion for string, integer, boolean, array arguments
- [x] 9.4 Add required argument validation before request
- [x] 9.5 Add argument type validation against parameter schema
- [x] 9.6 Implement --json flag for complex nested arguments
- [x] 9.7 Create tool invocation request format for daemon
- [x] 9.8 Add tool execution handler in daemon (forward to MCP server)
- [x] 9.9 Implement tool result formatting (JSON and plain text modes)
- [x] 9.10 Add --output flag for selecting output format
- [x] 9.11 Generate tool-specific --help based on parameter schema
- [x] 9.12 Implement timeout handling for long-running tools (default 120s)
- [x] 9.13 Add streaming result support for progressive output
- [x] 9.14 Handle binary results (save to file or display base64)

## 10. CLI Commands Integration

- [x] 10.1 Update main CLI command structure with Clikt
- [x] 10.2 Add `config` subcommand group
- [x] 10.3 Add `daemon` subcommand group
- [x] 10.4 Add `tools` subcommand group
- [x] 10.5 Add server name parameter with default selection for tool commands
- [x] 10.6 Implement help text and usage examples for all commands

## 11. Testing and Validation

- [x] 11.1 Create unit tests for configuration loading and validation
- [x] 11.2 Add unit tests for HTTP client request/response handling
- [x] 11.3 Create unit tests for connection pool lifecycle
- [x] 11.4 Add unit tests for PID file management
- [x] 11.5 Create unit tests for tool cache management
- [x] 11.6 Add unit tests for argument parsing and validation
- [x] 11.7 Create integration test for daemon start/stop lifecycle
- [x] 11.8 Add integration test for CLI-to-daemon request routing
- [x] 11.9 Create integration test for tool discovery and invocation
- [x] 11.10 Create end-to-end test with mock MCP server
- [x] 11.11 Test error scenarios (network failures, timeouts, invalid config)
- [ ] 11.12 Validate multiplatform compatibility (JVM and Native targets)

## 12. Documentation and Polish

- [x] 12.1 Update README with new architecture overview
- [x] 12.2 Add configuration file format documentation
- [x] 12.3 Document daemon management commands with examples
- [x] 12.4 Document tool discovery and invocation workflow
- [x] 12.5 Add examples of LLM integration with tool commands
- [x] 12.6 Add troubleshooting guide for common issues
- [x] 12.7 Create example configuration for common MCP servers
- [x] 12.8 Add logging configuration documentation
- [x] 12.9 Document migration from old CLI to daemon-based architecture
