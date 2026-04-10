## ADDED Requirements

### Requirement: Help flag support
The system SHALL display help information when the `--help` or `-h` flag is provided.

#### Scenario: Help flag is used
- **WHEN** the user runs the CLI with `--help` flag
- **THEN** the system displays usage information and available commands

#### Scenario: Short help flag is used
- **WHEN** the user runs the CLI with `-h` flag
- **THEN** the system displays usage information and available commands

### Requirement: Help content
The system SHALL display clear and concise help information including program usage and available commands.

#### Scenario: Help displays program name
- **WHEN** help is displayed
- **THEN** the output includes the program name

#### Scenario: Help lists available commands
- **WHEN** help is displayed
- **THEN** the output lists all available commands with brief descriptions

### Requirement: Exit after help
The system SHALL exit successfully after displaying help information.

#### Scenario: Help is displayed
- **WHEN** help information is shown
- **THEN** the program exits with status code 0
