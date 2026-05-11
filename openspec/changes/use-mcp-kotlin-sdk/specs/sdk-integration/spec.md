## ADDED Requirements

### Requirement: SDK Dependency
The project SHALL include the official Model Context Protocol Kotlin SDK as a core dependency.

#### Scenario: Add SDK dependency
- **WHEN** build.gradle.kts is updated with the SDK dependency
- **THEN** the project compiles successfully and SDK classes are available

### Requirement: SDK Client Management
The system SHALL use the `McpClient` from the SDK to manage connections and requests to MCP servers.

#### Scenario: Initialize SDK client
- **WHEN** a connection to an MCP server is requested
- **THEN** an `McpClient` instance is created with appropriate configuration
