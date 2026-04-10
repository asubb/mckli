## 1. Prepare CLI Parameters

- [x] 1.1 Add `--full` and `-l` option to `ToolsListCommand` in `ToolCommands.kt`.

## 2. Implement Truncation Utility

- [x] 2.1 Create a utility function for truncating descriptions (first line, max 200 chars).
- [x] 2.2 Add tests for the truncation utility.

## 3. Update Output Logic

- [x] 3.1 Update `ToolsListCommand.run()` to use the compact output format by default.
- [x] 3.2 Implement the full output format when the `--full` flag is provided.
- [x] 3.3 Ensure the JSON output remains unaffected by these changes.

## 4. Verification

- [x] 4.1 Verify compact output with long and multi-line descriptions.
- [x] 4.2 Verify full output format readability.
- [x] 4.3 Ensure all existing tests pass.
