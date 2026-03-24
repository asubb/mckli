## 1. Project Setup and Dependencies

- [ ] 1.1 Add Ktor client dependency to build.gradle.kts for HTTP communication
- [ ] 1.2 Add kotlinx.serialization dependency for JSON handling
- [ ] 1.3 Add kotlinx.coroutines dependency for async operations
- [ ] 1.4 Configure Ktor CIO engine for multiplatform support
- [ ] 1.5 Create module structure for daemon, client, and config components

## 2. Configuration System

- [ ] 2.1 Define ServerConfig data class with name, endpoint, auth, timeout, pool size
- [ ] 2.2 Implement configuration file reader for ~/.config/mckli/servers.json
- [ ] 2.3 Add configuration validation logic for URL format, timeout values
- [ ] 2.4 Create configuration file writer with pretty-printed JSON
- [ ] 2.5 Implement `config add` command for adding new servers
- [ ] 2.6 Implement `config remove` command for removing servers
- [ ] 2.7 Implement `config list` command for listing configured servers
- [ ] 2.8 Implement `config edit` command for editing server configuration
- [ ] 2.9 Add default configuration creation when servers.json is missing

## 3. HTTP Client Implementation

- [ ] 3.1 Create HttpMcpClient class with Ktor HttpClient instance
- [ ] 3.2 Implement client initialization with timeout, pool size, and retry settings
- [ ] 3.3 Add HTTP POST request method for MCP protocol forwarding
- [ ] 3.4 Implement Basic authentication support in request headers
- [ ] 3.5 Implement Bearer token authentication support in request headers
- [ ] 3.6 Add JSON request serialization using kotlinx.serialization
- [ ] 3.7 Add JSON response parsing with error handling
- [ ] 3.8 Implement error handling for network failures
- [ ] 3.9 Implement error handling for HTTP 4xx and 5xx responses
- [ ] 3.10 Implement timeout handling with cancellation
- [ ] 3.11 Add connection reuse through Ktor connection pool

## 4. Connection Pooling

- [ ] 4.1 Create ConnectionPool class for managing HTTP connections per daemon
- [ ] 4.2 Implement connection acquisition logic with availability check
- [ ] 4.3 Add connection creation when pool is empty
- [ ] 4.4 Implement waiting/timeout when pool is at max capacity
- [ ] 4.5 Add idle connection timeout mechanism (default 5 minutes)
- [ ] 4.6 Implement connection validation before reuse
- [ ] 4.7 Add maximum connection lifetime enforcement (default 30 minutes)
- [ ] 4.8 Implement connection pool shutdown logic
- [ ] 4.9 Add pool metrics tracking (active, idle, total created)
- [ ] 4.10 Implement connection leak detection warnings

## 5. Daemon Process Management

- [ ] 5.1 Create DaemonProcess class for daemon lifecycle management
- [ ] 5.2 Implement daemon process spawning with server configuration
- [ ] 5.3 Add PID file creation in ~/.config/mckli/daemons/ directory
- [ ] 5.4 Implement PID file reading and process validation
- [ ] 5.5 Add stale PID file cleanup on CLI startup
- [ ] 5.6 Implement `daemon start` command with server name parameter
- [ ] 5.7 Implement `daemon stop` command with graceful SIGTERM
- [ ] 5.8 Implement `daemon status` command listing all daemons
- [ ] 5.9 Implement `daemon restart` command (stop + start)
- [ ] 5.10 Add forced termination with SIGKILL after 10 second timeout
- [ ] 5.11 Implement daemon auto-start on first request
- [ ] 5.12 Add multi-daemon support for concurrent server instances

## 6. Daemon Process Implementation

- [ ] 6.1 Create daemon main entry point separate from CLI
- [ ] 6.2 Initialize HTTP client and connection pool in daemon process
- [ ] 6.3 Implement Unix domain socket server for IPC (or named pipes on Windows)
- [ ] 6.4 Add signal handler for graceful SIGTERM shutdown
- [ ] 6.5 Implement request receiving from IPC socket
- [ ] 6.6 Add request deserialization from JSON
- [ ] 6.7 Implement request forwarding to MCP server via HTTP client
- [ ] 6.8 Add response serialization to JSON
- [ ] 6.9 Implement response sending via IPC socket
- [ ] 6.10 Add concurrent request handling with coroutines
- [ ] 6.11 Implement request correlation with unique IDs
- [ ] 6.12 Add error propagation from MCP server to CLI
- [ ] 6.13 Implement daemon logging for debugging

## 7. Request Routing (CLI to Daemon)

- [ ] 7.1 Create RequestRouter class for CLI-to-daemon communication
- [ ] 7.2 Implement Unix domain socket client connection
- [ ] 7.3 Add connection timeout handling (5 seconds)
- [ ] 7.4 Implement request serialization to JSON with request ID
- [ ] 7.5 Add server selection logic based on CLI parameters
- [ ] 7.6 Implement default server selection from configuration
- [ ] 7.7 Add request sending via IPC socket
- [ ] 7.8 Implement response receiving and deserialization
- [ ] 7.9 Add response timeout handling (default 60 seconds)
- [ ] 7.10 Implement request-response correlation by request ID
- [ ] 7.11 Add error handling for daemon not running (trigger auto-start)
- [ ] 7.12 Implement error message formatting for user display

## 8. Tool Discovery and Caching

- [ ] 8.1 Define ToolMetadata data class for tool name, description, parameter schema
- [ ] 8.2 Implement MCP tools/list endpoint call in HTTP client
- [ ] 8.3 Add tool metadata fetching on daemon startup
- [ ] 8.4 Create in-memory tool cache in daemon process
- [ ] 8.5 Implement tool cache refresh logic triggered by CLI
- [ ] 8.6 Add tool cache query interface via IPC
- [ ] 8.7 Implement `tools list` CLI command with daemon query
- [ ] 8.8 Implement `tools describe` CLI command for detailed tool info
- [ ] 8.9 Implement `tools refresh` CLI command to update cache
- [ ] 8.10 Add tool filtering by name pattern in list command
- [ ] 8.11 Handle empty tool cache gracefully (show message to user)

## 9. Tool Invocation

- [ ] 9.1 Implement `tools call` CLI command with server and tool name
- [ ] 9.2 Add CLI flag parser for tool arguments based on cached schema
- [ ] 9.3 Implement type conversion for string, integer, boolean, array arguments
- [ ] 9.4 Add required argument validation before request
- [ ] 9.5 Add argument type validation against parameter schema
- [ ] 9.6 Implement --json flag for complex nested arguments
- [ ] 9.7 Create tool invocation request format for daemon
- [ ] 9.8 Add tool execution handler in daemon (forward to MCP server)
- [ ] 9.9 Implement tool result formatting (JSON and plain text modes)
- [ ] 9.10 Add --output flag for selecting output format
- [ ] 9.11 Generate tool-specific --help based on parameter schema
- [ ] 9.12 Implement timeout handling for long-running tools (default 120s)
- [ ] 9.13 Add streaming result support for progressive output
- [ ] 9.14 Handle binary results (save to file or display base64)

## 10. CLI Commands Integration

- [ ] 10.1 Update main CLI command structure with Clikt
- [ ] 10.2 Add `config` subcommand group
- [ ] 10.3 Add `daemon` subcommand group
- [ ] 10.4 Add `tools` subcommand group
- [ ] 10.5 Add server name parameter with default selection for tool commands
- [ ] 10.6 Implement help text and usage examples for all commands

## 11. Testing and Validation

- [ ] 11.1 Create unit tests for configuration loading and validation
- [ ] 11.2 Add unit tests for HTTP client request/response handling
- [ ] 11.3 Create unit tests for connection pool lifecycle
- [ ] 11.4 Add unit tests for PID file management
- [ ] 11.5 Create unit tests for tool cache management
- [ ] 11.6 Add unit tests for argument parsing and validation
- [ ] 11.7 Create integration test for daemon start/stop lifecycle
- [ ] 11.8 Add integration test for CLI-to-daemon request routing
- [ ] 11.9 Create integration test for tool discovery and invocation
- [ ] 11.10 Create end-to-end test with mock MCP server
- [ ] 11.11 Test error scenarios (network failures, timeouts, invalid config)
- [ ] 11.12 Validate multiplatform compatibility (JVM and Native targets)

## 12. Documentation and Polish

- [ ] 12.1 Update README with new architecture overview
- [ ] 12.2 Add configuration file format documentation
- [ ] 12.3 Document daemon management commands with examples
- [ ] 12.4 Document tool discovery and invocation workflow
- [ ] 12.5 Add examples of LLM integration with tool commands
- [ ] 12.6 Add troubleshooting guide for common issues
- [ ] 12.7 Create example configuration for common MCP servers
- [ ] 12.8 Add logging configuration documentation
- [ ] 12.9 Document migration from old CLI to daemon-based architecture
