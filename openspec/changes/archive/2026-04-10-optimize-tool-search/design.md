## Context

The `mckli` tool manages multiple MCP servers. Currently, tool discovery (list, describe) is mostly server-specific. To improve usability, we need to allow users to search and discover tools across all configured servers.

## Goals / Non-Goals

**Goals:**
- Implement `tools search <query>` to find tools across all servers.
- Add `--json` flag to `tools search` and `tools list` for machine-readable output.
- Update `tools list` to support multi-server listing.
- Enhance `tools describe` to handle `<server> <tool>` syntax and maintain its JSON output for schema.
- Provide a consistent result format: `<server>:<tool-name> <preview>`.

**Non-Goals:**
- Implementing a persistent search index (search will be done on live/cached data).
- Advanced fuzzy matching (initially simple case-insensitive substring match).

## Decisions

### 1. Cross-Server Tool Discovery
- **Decision**: Update `RequestRouter` or create a `MultiServerRouter` to aggregate results from all configured servers.
- **Rationale**: The current `RequestRouter` is designed for a single server. Aggregating results in the CLI layer is easier initially but might belong in a router abstraction if we add more cross-server features.

### 2. Search Result Preview
- **Decision**: The `<preview>` will be a short snippet from the tool's description. If the description is missing, it will just show the name.
- **Rationale**: Matches the user requirement for a "preview of found string".

### 3. CLI Command Structure
- **Decision**: 
    - `tools list [server]` (server remains optional; if omitted, list all).
    - `tools search <query>` (always searches all servers).
    - `tools describe <server> <tool>` (make server mandatory or use default if omitted).
- **Rationale**: Consistent with existing Clikt command structure.

## Risks / Trade-offs

- **[Risk] Performance** → [Mitigation] Searching across many servers might be slow if daemons need to start. We will rely on `ToolCache` where possible.
- **[Risk] Output Verbosity** → [Mitigation] Limit the number of results or provide a concise one-line format per match.
