## ADDED Requirements

### Requirement: Tool metadata fetching on daemon startup
Daemons SHALL fetch tool metadata from their assigned MCP server during initialization and cache it in memory.

#### Scenario: Successful tool discovery on startup
- **WHEN** daemon starts and connects to MCP server
- **THEN** daemon fetches complete tool list with names, descriptions, and parameter schemas

#### Scenario: Tool discovery failure
- **WHEN** daemon cannot fetch tools due to network or server error
- **THEN** daemon logs error and starts with empty tool cache, allowing retry via refresh command

#### Scenario: MCP server with no tools
- **WHEN** MCP server returns empty tool list
- **THEN** daemon caches empty list and reports zero tools available

### Requirement: Tool cache structure
The tool cache SHALL store tool name, description, input schema (JSON Schema format), and any metadata from MCP server.

#### Scenario: Cache tool with parameters
- **WHEN** MCP server returns tool with parameter schema
- **THEN** daemon stores tool name, description, and full JSON Schema for parameters

#### Scenario: Cache tool without parameters
- **WHEN** MCP server returns tool with no parameters
- **THEN** daemon stores tool name and description with null parameter schema

### Requirement: CLI command to list tools
The system SHALL provide `mckli tools list [server]` command to display available tools.

#### Scenario: List all tools
- **WHEN** user runs `mckli tools list` without a server name
- **THEN** CLI displays tools from all configured servers, grouped by server

#### Scenario: List tools for a specific server
- **WHEN** user runs `mckli tools list myserver`
- **THEN** CLI connects to daemon and displays tool names from that server only

#### Scenario: JSON output for tool listing
- **WHEN** user runs `mckli tools list --json`
- **THEN** CLI returns tools in structured JSON format

#### Scenario: No tools available
- **WHEN** server has no tools cached
- **THEN** CLI displays message indicating no tools available

### Requirement: CLI command to describe tool
The system SHALL provide `mckli tools describe <server> <tool-name>` command to show detailed tool information.

#### Scenario: Describe existing tool
- **WHEN** user runs `mckli tools describe myserver read-file`
- **THEN** CLI displays tool description and parameter schema with types and requirements

#### Scenario: Describe non-existent tool
- **WHEN** user requests description for tool that doesn't exist
- **THEN** CLI returns error listing available tools

### Requirement: Tool cache refresh
The system SHALL provide `mckli tools refresh <server>` command to re-fetch tool metadata from MCP server.

#### Scenario: Manual cache refresh
- **WHEN** user runs `mckli tools refresh myserver`
- **THEN** daemon re-fetches tools from MCP server and updates cache

#### Scenario: Refresh with server changes
- **WHEN** MCP server has added or removed tools since startup
- **THEN** refresh command updates cache to reflect current server state

#### Scenario: Refresh failure
- **WHEN** refresh fails due to network error
- **THEN** daemon retains existing cache and returns error to CLI

### Requirement: Tool filtering and search
The system SHALL support searching and filtering tools across all servers.

#### Scenario: Search for a keyword
- **WHEN** user runs `mckli tools search <query>`
- **THEN** CLI displays matching tools from all servers with previews

#### Scenario: JSON output for tool search
- **WHEN** user runs `mckli tools search <query> --json`
- **THEN** CLI returns search results in structured JSON format

#### Scenario: Filter tools by name or description
- **WHEN** user runs `mckli tools list --filter "keyword"`
- **THEN** CLI displays only tools where name or description matches the keyword

### Requirement: Tool cache persistence
Tool cache SHALL be memory-only and not persisted to disk between daemon restarts.

#### Scenario: Daemon restart clears cache
- **WHEN** daemon is restarted
- **THEN** tool cache is re-fetched from MCP server on startup

#### Scenario: Cache available across CLI invocations
- **WHEN** multiple CLI invocations access tool cache while daemon is running
- **THEN** all invocations see consistent cached tool metadata without re-fetching
