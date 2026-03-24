## ADDED Requirements

### Requirement: CLI entry point
The system SHALL provide a main entry point that accepts command-line arguments.

#### Scenario: Program is invoked with arguments
- **WHEN** the CLI is executed with any arguments
- **THEN** the system parses and processes the arguments

#### Scenario: Program is invoked without arguments
- **WHEN** the CLI is executed without arguments
- **THEN** the system displays a default message or help information

### Requirement: Command routing
The system SHALL route arguments to the appropriate command handler.

#### Scenario: Valid command is provided
- **WHEN** a recognized command is specified
- **THEN** the system executes the corresponding command handler

#### Scenario: Invalid command is provided
- **WHEN** an unrecognized command is specified
- **THEN** the system displays an error message and suggests valid commands

### Requirement: Cross-platform support
The system SHALL run on both JVM and Native platforms.

#### Scenario: Execution on JVM
- **WHEN** the CLI is compiled for JVM and executed
- **THEN** all commands work correctly

#### Scenario: Execution on Native
- **WHEN** the CLI is compiled for Native and executed
- **THEN** all commands work correctly
