## Why

Currently, the `tools list` command prints the full description of every tool by default. When a server has many tools or tools have long descriptions, this output becomes cluttered and hard to scan. Users often just want a quick overview of available tools.

## What Changes

- Modified `tools list` command to show a compact view by default.
- Added a `--full` flag (or `-l` for long) to `tools list` to show the complete, human-readable tool descriptions.
- Compact view format: `tool-name  tool-short-description` (truncated to 200 characters, first line only).
- Improved readability of the full output format.

## Capabilities

### New Capabilities
- `compact-tool-listing`: Defines the requirements for compact and full tool listing formats, including truncation rules and display flags.

### Modified Capabilities
<!-- No existing formal specs found in openspec/specs/ to modify -->

## Impact

- `ToolListCommand` in `ToolCommands.kt`: Updated to handle the new default compact output and the `--full` flag.
- User experience: Improved readability and scannability of the `tools list` command output.
