## ADDED Requirements

### Requirement: Configuration file location
The system SHALL store MCP server configuration in `~/.config/mckli/servers.json` with JSON format.

#### Scenario: Default config location
- **WHEN** CLI reads configuration without custom path
- **THEN** system loads from `~/.config/mckli/servers.json`

#### Scenario: Custom config path
- **WHEN** user specifies custom config path via environment variable or flag
- **THEN** system loads configuration from the specified path

#### Scenario: Missing config file
- **WHEN** configuration file does not exist
- **THEN** system creates empty configuration with default structure

### Requirement: Server configuration schema
Each MCP server configuration SHALL include name, endpoint URL, transport type, authentication settings, and connection parameters.

#### Scenario: Complete server config
- **WHEN** user defines server with all parameters
- **THEN** config includes name, endpoint, transport (HTTP or SSE), auth type, credentials, timeout, and pool size

#### Scenario: Minimal server config
- **WHEN** user defines server with only name and endpoint
- **THEN** system applies default values for optional parameters (transport = HTTP, timeout = 30000ms, poolSize = 10)

#### Scenario: Invalid configuration
- **WHEN** configuration file contains invalid JSON or missing required fields
- **THEN** system returns validation error with details

### Requirement: Multiple server support
The configuration SHALL support defining multiple MCP server instances with unique names and optionally specifying a default server.

#### Scenario: Multiple servers configured
- **WHEN** config file defines multiple servers
- **THEN** system loads all server configurations and allows selection by name

#### Scenario: Default server configuration
- **WHEN** config file defines a `defaultServer` name
- **THEN** system uses that server for tool requests if no server is explicitly specified

#### Scenario: Duplicate server names
- **WHEN** configuration contains duplicate server names
- **THEN** system returns validation error

### Requirement: Configuration commands
The system SHALL provide CLI commands for managing server configuration: add, remove, list, and edit.

#### Scenario: Add server
- **WHEN** user runs `mckli config add <name> <endpoint>`
- **THEN** system adds server to configuration and saves file

#### Scenario: Remove server
- **WHEN** user runs `mckli config remove <name>`
- **THEN** system removes server from configuration and stops associated daemon

#### Scenario: List servers
- **WHEN** user runs `mckli config list`
- **THEN** system displays all configured servers with their endpoints

#### Scenario: Edit server
- **WHEN** user runs `mckli config edit <name>`
- **THEN** system opens editor with server configuration

### Requirement: Authentication configuration
The configuration SHALL support storing credentials for HTTP Basic auth and Bearer tokens.

#### Scenario: Basic auth credentials
- **WHEN** server config includes username and password
- **THEN** system stores credentials and uses for HTTP authentication

#### Scenario: Bearer token
- **WHEN** server config includes bearer token
- **THEN** system stores token and includes in Authorization header

#### Scenario: No authentication
- **WHEN** server config omits authentication fields
- **THEN** system sends requests without credentials

### Requirement: Configuration validation
The system SHALL validate configuration on load and before saving changes.

#### Scenario: Valid configuration
- **WHEN** configuration file is loaded with valid syntax and schema
- **THEN** system successfully parses and applies configuration

#### Scenario: Invalid URL format
- **WHEN** server endpoint is not a valid HTTP/HTTPS URL
- **THEN** system returns validation error for that server

#### Scenario: Invalid timeout value
- **WHEN** timeout is negative or zero
- **THEN** system returns validation error

### Requirement: Configuration reload
The system SHALL reload configuration without restarting daemons when config file changes.

#### Scenario: Config file modified
- **WHEN** user updates configuration file
- **THEN** system detects changes and reloads on next CLI invocation

#### Scenario: Running daemon config changed
- **WHEN** configuration for running daemon is updated
- **THEN** system prompts user to restart daemon to apply changes
