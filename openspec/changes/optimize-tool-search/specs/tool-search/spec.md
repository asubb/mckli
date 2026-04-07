## ADDED Requirements

### Requirement: Full-text search across all tools
The system SHALL provide a `tools search` command that performs a case-insensitive search for a given query across all tool names and descriptions from all configured MCP servers.

#### Scenario: Search for a keyword
- **WHEN** user runs `mckli tools search search_term`
- **THEN** the system SHALL display matches from all servers

### Requirement: Search result format
The system SHALL display each search result in the format `<server>:<tool-name> <preview>` by default, where `<preview>` is a snippet of the tool's description or name containing the search query.
The system SHALL also support a `--json` flag to output results as a JSON array of objects, where each object contains `server`, `name`, and `description`.

#### Scenario: Displaying search results as text (default)
- **WHEN** user runs `mckli tools search search_term`
- **THEN** each match SHALL be shown on a new line with its server prefix and a text preview

#### Scenario: Displaying search results as JSON
- **WHEN** user runs `mckli tools search search_term --json`
- **THEN** the system SHALL output a JSON array of matching tool metadata

### Requirement: List all tools across all servers
The `tools list` command SHALL support listing all available tools from all configured MCP servers when no specific server name is provided.

#### Scenario: List all tools
- **WHEN** user runs `mckli tools list` (without a server argument)
- **THEN** the system SHALL list all tools from all active servers grouped or prefixed by server name

### Requirement: Tool description detail
The `tools describe` command SHALL show the tool's name, description, and input schema.

#### Scenario: Describe a tool
- **WHEN** user runs `mckli tools describe <server> <tool-name>`
- **THEN** the system SHALL display the detailed metadata for that tool
