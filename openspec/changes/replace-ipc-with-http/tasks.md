## 1. Unified Daemon Foundation

- [ ] 1.1 Update `DaemonStatus` and `ConnectionState` in `DaemonProcess.kt` to support multi-server reporting
- [ ] 1.2 Implement `DaemonManager` to handle multiple `ConnectionPool` and `ToolCache` instances
- [ ] 1.3 Refactor `Daemon.kt` (or create `UnifiedDaemon.kt`) to use `DaemonManager`

## 2. HTTP API Implementation

- [ ] 2.1 Add Ktor Server dependencies to `build.gradle.kts`
- [ ] 2.2 Implement the Ktor-based HTTP server in the daemon
- [ ] 2.3 Define and implement routes for `/health`, `/servers/{name}/tools`, and `/servers/{name}/tools/call`
- [ ] 2.4 Replace `UnixSocketServer` usage with the new HTTP server in `DaemonMain.kt`

## 3. CLI & Request Routing

- [ ] 3.1 Implement an HTTP client in the CLI for daemon communication (replacing `UnixSocketClient`)
- [ ] 3.2 Update `RequestRouter.kt` to use the new HTTP client and server-specific URL paths
- [ ] 3.3 Update `DaemonProcess` (JVM/Native) to handle a single global PID file and status check via HTTP

## 4. Cleanup & Migration

- [ ] 4.1 Remove `UnixSocketServer.kt`, `UnixSocketClient.kt`, and `IpcMessage.kt`
- [ ] 4.2 Update integration tests to use HTTP for daemon communication
- [ ] 4.3 Update `DaemonCommands.kt` to reflect single-daemon lifecycle management (start/stop/status)
- [ ] 4.4 Verify all existing features (tool discovery, execution) work through the unified daemon
