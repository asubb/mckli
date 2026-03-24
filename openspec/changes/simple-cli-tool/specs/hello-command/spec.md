## ADDED Requirements

### Requirement: Hello command
The system SHALL provide a `hello` command that displays a greeting message.

#### Scenario: Hello command is invoked
- **WHEN** the user runs the CLI with `hello` command
- **THEN** the system prints "Hello, World!" to standard output

#### Scenario: Hello command exits successfully
- **WHEN** the hello command completes
- **THEN** the program exits with status code 0

### Requirement: Hello with name argument
The system SHALL accept an optional name argument to personalize the greeting.

#### Scenario: Hello with name provided
- **WHEN** the user runs `hello <name>`
- **THEN** the system prints "Hello, <name>!" to standard output

#### Scenario: Hello without name
- **WHEN** the user runs `hello` without a name
- **THEN** the system prints "Hello, World!" to standard output
