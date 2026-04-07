## 1. Tool Search Capability

- [ ] 1.1 Implement `ToolsSearchCommand` in `ToolCommands.kt`
- [ ] 1.2 Implement search logic across all configured servers (in `ToolCommands` or `RequestRouter`)
- [ ] 1.3 Implement search result formatting with server prefix and text preview
- [ ] 1.4 Add `--json` flag to `ToolsSearchCommand` and implement JSON output format

## 2. Updated Tool Listing and Description

- [ ] 2.1 Modify `ToolsListCommand` to support listing all tools from all servers
- [ ] 2.2 Update `ToolsDescribeCommand` to correctly handle server name and tool name arguments
- [ ] 2.3 Ensure `ToolsListCommand` handles grouping or prefixing tools by server
- [ ] 2.4 Add `--json` flag to `ToolsListCommand` for machine-readable output

## 3. Testing and Verification

- [ ] 3.1 Update integration tests in `ToolSteps.kt` and add scenarios to `tool-discovery.feature`
- [ ] 3.2 Add unit tests for search result formatting and filtering logic
- [ ] 3.3 Verify `tools search` displays correct previews for matching tools
- [ ] 3.4 Verify `--json` output for both `tools search` and `tools list`
