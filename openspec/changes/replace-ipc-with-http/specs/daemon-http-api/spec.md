## ADDED Requirements

### Requirement: Local HTTP Server
The daemon SHALL run a local HTTP server listening on a loopback interface (localhost/127.0.0.1) only.

#### Scenario: Daemon server only on localhost
- **WHEN** the daemon starts
- **THEN** it SHALL only accept connections from 127.0.0.1 on the configured port (default 5030)

### Requirement: Health and Status API
The daemon SHALL provide endpoints for health checks and daemon status.

#### Scenario: Checking daemon status
- **WHEN** the CLI sends a GET request to `/health`
- **THEN** the daemon SHALL respond with a successful status and its current state

### Requirement: Server-Specific Tool API
The daemon SHALL expose endpoints for listing tools and calling tools per server.

#### Scenario: Listing tools for a server
- **WHEN** the CLI sends a GET request to `/servers/{name}/tools`
- **THEN** the daemon SHALL respond with the list of tools for that server

#### Scenario: Calling a tool for a server
- **WHEN** the CLI sends a POST request to `/servers/{name}/tools/call` with the tool name and arguments
- **THEN** the daemon SHALL execute the tool and return the result via HTTP
