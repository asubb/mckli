## ADDED Requirements

### Requirement: JVM Application Distribution
The build system SHALL use the Gradle `application` plugin to package the JVM target of the application.

#### Scenario: Distribution artifacts are generated
- **WHEN** the `distZip` or `distTar` task is executed
- **THEN** a ZIP or TAR archive is created in `build/distributions/` containing the application and its dependencies.

### Requirement: Start Scripts
The distribution SHALL include standard start scripts for Unix-like systems and Windows.

#### Scenario: Executing the start script
- **WHEN** the generated start script in the `bin/` directory is executed
- **THEN** the application main class `com.mckli.MainKt` is started with the JVM.

### Requirement: Application Configuration
The `application` plugin SHALL be configured with the application name `mckli`.

#### Scenario: Verify application name in distribution
- **WHEN** the distribution is extracted
- **THEN** the root directory and start scripts are named `mckli`.

## REMOVED Requirements

### Requirement: Custom Fat JAR Task
**Reason**: Replaced by standard Gradle `application` plugin distributions which provide better structure and start scripts.
**Migration**: Use `installDist` for local testing or `distZip`/`distTar` for distribution.
