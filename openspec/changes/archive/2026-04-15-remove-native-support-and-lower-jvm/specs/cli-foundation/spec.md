## MODIFIED Requirements

### Requirement: Cross-platform support
The system SHALL target the JVM platform (version 17 or higher) for all supported operating systems (Linux, macOS, Windows).

#### Scenario: Execution on JVM 17
- **WHEN** the CLI is compiled for JVM and executed on a JVM version 17 or higher
- **THEN** all commands MUST work correctly

## REMOVED Requirements

### Requirement: Native platform support
**Reason**: Replaced by unified JVM distribution to reduce build complexity and maintenance overhead.
**Migration**: Use the provided JVM-based `mckli` script with a compatible Java runtime (17+).

### Requirement: Kotlin Multiplatform (KMP) support
**Reason**: Removed to simplify the project structure for a JVM-only tool.
**Migration**: Transition to standard Kotlin JVM project structure and dependencies.
