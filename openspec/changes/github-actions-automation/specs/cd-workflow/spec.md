## ADDED Requirements

### Requirement: CD Workflow Triggered on Tags
The system SHALL have a GitHub Actions workflow that triggers when a new Git tag (following semver pattern, e.g., `v1.2.3`) is pushed to the repository.

#### Scenario: Workflow Triggers on Tag Push
- **WHEN** a user pushes a tag matching `v*` (e.g., `v0.1.0`)
- **THEN** the CD workflow starts automatically

### Requirement: Artifact Generation for Release
The CD workflow SHALL generate distribution-ready artifacts for the JVM platform.

#### Scenario: JVM Distribution Generation
- **WHEN** the CD workflow runs
- **THEN** it executes `./gradlew distZip` to produce the JVM distribution ZIP file

### Requirement: Automated GitHub Release Creation
The CD workflow SHALL create a new GitHub Release corresponding to the pushed tag and upload the generated artifacts to it.

#### Scenario: Release Creation with Artifacts
- **WHEN** the build and artifact generation are successful
- **THEN** a new GitHub Release is created, and the JVM distribution ZIP is attached as a downloadable asset
