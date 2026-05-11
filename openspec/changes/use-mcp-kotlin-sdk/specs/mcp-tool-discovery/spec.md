## MODIFIED Requirements

### Requirement: Tool metadata fetching on daemon startup
Daemons SHALL fetch tool metadata from their assigned MCP server during initialization using the official MCP Kotlin SDK's `McpClient.listTools` method.

#### Scenario: Successful tool discovery on startup
- **WHEN** daemon starts and connects to MCP server
- **THEN** daemon calls `McpClient.listTools` to fetch complete tool list

### Requirement: Tool cache refresh
The system SHALL provide `mckli tools refresh <server>` command to re-fetch tool metadata from MCP server using the SDK's `listTools`.

#### Scenario: Manual cache refresh
- **WHEN** user runs `mckli tools refresh myserver`
- **THEN** daemon calls `McpClient.listTools` from MCP server and updates local cache
