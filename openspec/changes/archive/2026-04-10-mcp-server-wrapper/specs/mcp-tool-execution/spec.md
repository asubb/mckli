## ADDED Requirements

### Requirement: CLI tool invocation command
The system SHALL provide `mckli tools call [server] <tool-name> --json <args>` command for invoking MCP tools.

#### Scenario: Call tool with JSON arguments
- **WHEN** user runs `mckli tools call myserver read-file --json '{"path": "/foo/bar.txt"}'`
- **THEN** CLI parses JSON, sends request to daemon, and displays tool result

#### Scenario: Call tool using default server
- **WHEN** user runs `mckli tools call read-file --json '{"path": "/foo/bar.txt"}'` and a default server is configured
- **THEN** CLI sends request to the default server's daemon

#### Scenario: Call tool without arguments
- **WHEN** user runs `mckli tools call myserver list-users` for tool requiring no parameters
- **THEN** CLI sends request to daemon and displays result

#### Scenario: Call non-existent tool
- **WHEN** user calls tool that doesn't exist in cache
- **THEN** CLI returns error with list of available tools

### Requirement: Argument parsing from CLI flags
The CLI SHALL parse tool arguments from command-line flags based on cached parameter schema.

#### Scenario: Parse string argument
- **WHEN** tool expects string parameter "path" and user provides `--path /foo/bar`
- **THEN** CLI constructs JSON object `{"path": "/foo/bar"}` for MCP request

#### Scenario: Parse numeric argument
- **WHEN** tool expects integer parameter "count" and user provides `--count 42`
- **THEN** CLI converts to integer and constructs JSON object `{"count": 42}`

#### Scenario: Parse boolean argument
- **WHEN** tool expects boolean parameter "recursive" and user provides `--recursive true`
- **THEN** CLI converts to boolean and constructs JSON object `{"recursive": true}`

#### Scenario: Parse array argument
- **WHEN** tool expects array parameter "tags" and user provides `--tags="tag1,tag2,tag3"`
- **THEN** CLI splits string and constructs JSON array `{"tags": ["tag1", "tag2", "tag3"]}`

### Requirement: Required argument validation
The CLI SHALL validate required arguments are provided before sending request to daemon.

#### Scenario: Missing required argument
- **WHEN** tool requires "path" parameter but user omits it
- **THEN** CLI returns validation error indicating required parameter

#### Scenario: All required arguments provided
- **WHEN** user provides all required parameters
- **THEN** CLI proceeds with request to daemon

### Requirement: Argument type validation
The CLI SHALL validate argument types match parameter schema before sending request.

#### Scenario: Invalid numeric value
- **WHEN** tool expects integer but user provides non-numeric string
- **THEN** CLI returns type error without sending request

#### Scenario: Valid typed arguments
- **WHEN** all arguments match expected types
- **THEN** CLI sends request to daemon

### Requirement: Tool execution via daemon
The CLI SHALL send tool invocation requests to daemon via HTTP with tool name and arguments.

#### Scenario: Successful tool execution
- **WHEN** daemon receives valid tool request via HTTP
- **THEN** daemon forwards to MCP server, receives result, and returns to CLI

#### Scenario: Tool execution error
- **WHEN** MCP server returns error for tool invocation
- **THEN** daemon forwards error to CLI with details

#### Scenario: Tool execution timeout
- **WHEN** tool execution exceeds configured timeout (default 120 seconds)
- **THEN** daemon cancels request and returns timeout error to CLI

### Requirement: Result formatting
The CLI SHALL format tool results for display, supporting JSON and plain text output modes.

#### Scenario: JSON output mode
- **WHEN** user runs tool call with `--output json` flag
- **THEN** CLI displays raw JSON result from MCP server

#### Scenario: Plain text output mode
- **WHEN** user runs tool call with `--output text` flag (default)
- **THEN** CLI formats result as human-readable text

#### Scenario: Binary result handling
- **WHEN** tool returns binary data
- **THEN** CLI detects content type and offers to save to file or display base64

### Requirement: JSON argument fallback
The CLI SHALL support `--json` flag for providing complex arguments as JSON object.

#### Scenario: Complex nested arguments via JSON
- **WHEN** tool requires nested object structure and user provides `--json '{"config": {"nested": "value"}}'`
- **THEN** CLI parses JSON and sends to daemon

#### Scenario: JSON overrides individual flags
- **WHEN** user provides both `--json` and individual flags
- **THEN** CLI returns error indicating conflicting input methods

### Requirement: Tool help generation
The CLI SHALL generate `--help` output for tools based on cached parameter schema.

#### Scenario: Display tool help
- **WHEN** user runs `mckli tools call myserver read-file --help`
- **THEN** CLI displays tool description and parameter list with types and descriptions

#### Scenario: Help for tool without parameters
- **WHEN** user requests help for parameterless tool
- **THEN** CLI displays tool description and indicates no parameters required

### Requirement: Streaming result support
The CLI SHALL support streaming results for long-running tools that provide progressive output.

#### Scenario: Streaming text output
- **WHEN** MCP tool returns streaming text response
- **THEN** CLI displays output progressively as it arrives

#### Scenario: Non-streaming result
- **WHEN** MCP tool returns complete result
- **THEN** CLI displays result after full response received
