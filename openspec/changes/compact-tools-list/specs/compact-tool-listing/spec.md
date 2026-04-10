## ADDED Requirements

### Requirement: Default Compact Tool List Output
The `tools list` command SHALL default to a compact output format that displays one tool per line.

#### Scenario: Listing tools without flags
- **WHEN** user executes `tools list`
- **THEN** system prints a list where each line contains only the tool name and a truncated description

### Requirement: Truncated Description Formatting
The compact description SHALL only include the first line of the tool description and be truncated to a maximum of 200 characters.

#### Scenario: Long tool description
- **WHEN** tool has a description with multiple lines or more than 200 characters
- **THEN** system only displays the first line and cuts off at character 200

### Requirement: Full Description Output Flag
The `tools list` command SHALL provide a `--full` flag (short `-l`) to display the complete tool description.

#### Scenario: Requesting full output
- **WHEN** user executes `tools list --full`
- **THEN** system prints tools with their full, multi-line descriptions

### Requirement: Human-Readable Full Output
The full output format SHALL use a readable layout that clearly separates tools and their attributes.

#### Scenario: Full output formatting
- **WHEN** `--full` flag is used
- **THEN** system displays tool name, full description, and input schema (if available) in a clearly indented or structured manner
