## Why

Currently, finding and discovering tools across multiple MCP servers is limited. Users need more powerful search
capabilities to find the right tool for their task, especially when dealing with many tools and servers.

## What Changes

- **New Search Capability**: Add a `tools search` command that performs a full-text search across tool names and
  descriptions across all configured MCP servers.
- **Search Result Format**: The search results will be formatted as `<server>:<tool-name> <preview of found string>` by default. A `--json` flag will be added to output results in a machine-readable JSON format.
- **Enhanced Listing**: Improve `tools list` to support listing all tools across all servers by default (or filtered).
- **Tool Description**: Ensure `tools describe` provides comprehensive information about a specific tool to help users understand its usage.

## Capabilities

### New Capabilities
- `tool-search`: Implements full-text search across all tools in all connected servers. Output formats: Text (default) and JSON.

### Modified Capabilities

- `mcp-tool-discovery`: Update requirements for tool listing to support cross-server listing and more granular
  filtering.

## Impact

- `ToolCommands`: New `ToolsSearchCommand` and updates to `ToolsListCommand` and `ToolsDescribeCommand`.
- `ToolCache` / `RequestRouter`: May need updates to support cross-server tool discovery if not already present.
- CLI Interface: New subcommand `tools search`.
