## MODIFIED Requirements

### Requirement: Tool execution via daemon
The CLI SHALL send tool invocation requests to daemon via HTTP, which the daemon forwards to the MCP server using the official MCP Kotlin SDK's `McpClient`.

#### Scenario: Successful tool execution
- **WHEN** daemon receives valid tool request via HTTP
- **THEN** daemon invokes the tool via `McpClient.callTool`, receives result, and returns to CLI

#### Scenario: Tool execution error
- **WHEN** MCP server returns error for tool invocation via SDK
- **THEN** daemon forwards error to CLI with details including SDK-provided error codes
