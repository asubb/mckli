## Context

The `tools list` command is part of the CLI utility that interacts with MCP (Model Context Protocol) servers. Currently, it retrieves tool metadata and prints it to the console. The current implementation lacks a way to filter or compress the output, leading to large amounts of text being printed when multiple tools are available.

## Goals / Non-Goals

**Goals:**
- Implement a compact default output for `tools list`.
- Add a `--full` (or `-l`) flag for detailed output.
- Ensure the full output is more readable for humans.
- Limit description truncation to the first line and max 200 characters in compact mode.

**Non-Goals:**
- Changing the JSON output format (already exists and should remain unchanged).
- Modifying other tool commands (like `search` or `describe`), unless they benefit from the same utility functions.

## Decisions

- **CLI Parameter Library**: Continue using `Clikt` as it is already the project's choice.
- **Truncation Logic**:
  - Use `take(200)` for length limiting.
  - Use `substringBefore('\n')` or `lineSequence().first()` to get the first line.
  - Append an ellipsis (`...`) if the description was truncated.
- **Output Formatting**:
  - Compact: `  tool-name  truncated-description`
  - Full:
    ```
    Tool: tool-name
    Description: full-description (possibly multi-line)
    Input Schema: <json-schema> (if available)
    ----------------------------------------
    ```

## Risks / Trade-offs

- **[Risk]** Ellipsis may make the total length slightly exceed 200 characters.
  - **Mitigation**: Truncate to 197 characters and append `...`.
- **[Risk]** Some descriptions might not have a clear "first line" that makes sense.
  - **Mitigation**: This is an inherent limitation of automatic truncation. Users can always use `--full` to see more.
