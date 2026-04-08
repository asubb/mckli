## MODIFIED Requirements

### Requirement: Daemon process spawning
The system SHALL spawn a single global background daemon process to manage all configured MCP server connections.

#### Scenario: Start global daemon
- **WHEN** user starts the global daemon
- **THEN** system spawns a new process and records its PID in the config directory

#### Scenario: Daemon already running
- **WHEN** user attempts to start the daemon that is already running
- **THEN** system returns an error indicating the daemon is already active

#### Scenario: Daemon process fails to start
- **WHEN** daemon spawn fails due to system error
- **THEN** system returns a detailed error message and cleans up any partial state

### Requirement: PID file management
The system SHALL maintain a single PID file in `~/.config/mckli/daemon.pid` for tracking the global daemon process.

#### Scenario: Create PID file on daemon start
- **WHEN** the global daemon process is successfully spawned
- **THEN** system writes the PID file with the process ID

#### Scenario: Remove PID file on daemon stop
- **WHEN** the daemon is stopped gracefully
- **THEN** system removes the global PID file

#### Scenario: Stale PID file cleanup
- **WHEN** the PID file exists but the process is not running
- **THEN** system removes the stale PID file on the next CLI invocation

### Requirement: Daemon lifecycle commands
The system SHALL provide CLI commands for managing the global daemon: start, stop, status, and restart.

#### Scenario: Start command
- **WHEN** user runs `mckli daemon start`
- **THEN** system spawns the global daemon and confirms success

#### Scenario: Stop command
- **WHEN** user runs `mckli daemon stop`
- **THEN** system sends a termination signal to the daemon and waits for graceful shutdown

#### Scenario: Status command
- **WHEN** user runs `mckli daemon status`
- **THEN** system reports whether the global daemon is running and lists the MCP servers it is managing

#### Scenario: Restart command
- **WHEN** user runs `mckli daemon restart`
- **THEN** system stops the existing daemon and starts a new one

### Requirement: Process health monitoring
The system SHALL verify the global daemon process health by checking the PID existence and responding to a health check endpoint.

#### Scenario: Check process is running
- **WHEN** system checks daemon status using the PID file
- **THEN** system verifies the process exists in the OS process table

#### Scenario: Daemon process crashed
- **WHEN** the PID file exists but the process is not running
- **THEN** system reports the daemon as crashed and offers a restart option

### Requirement: Daemon auto-start
The system SHALL automatically start the global daemon on the first request if it is not already running.

#### Scenario: Request to inactive daemon
- **WHEN** the CLI sends a request to any server with no running daemon
- **THEN** system starts the global daemon automatically and retries the request

#### Scenario: Auto-start failure
- **WHEN** auto-start fails due to configuration or system error
- **THEN** system returns an error and does not retry the request

## REMOVED Requirements

### Requirement: Multi-daemon support
**Reason**: Replaced by a single unified daemon architecture.
**Migration**: Use the single global daemon process for all servers.
