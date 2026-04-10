## ADDED Requirements

### Requirement: CI Workflow for PRs and Main Branch
The system SHALL have a GitHub Actions workflow that automatically triggers on every pull request to the `main` branch and every push to the `main` branch.

#### Scenario: Workflow Triggers on PR
- **WHEN** a developer creates a new pull request targeting the `main` branch
- **THEN** the CI workflow starts automatically

#### Scenario: Workflow Triggers on Push to Main
- **WHEN** a change is merged into the `main` branch
- **THEN** the CI workflow starts automatically

### Requirement: Automated Build and Test
The CI workflow SHALL compile the project and run all tests for the JVM target.

#### Scenario: JVM Build and Test
- **WHEN** the CI workflow runs
- **THEN** it executes `./gradlew build` to compile and test the JVM target

### Requirement: Publish Test Results
The CI workflow SHALL publish test results and code coverage (if applicable) to the GitHub pull request interface.

#### Scenario: Test Results Visibility
- **WHEN** the tests finish execution in a PR
- **THEN** the test summary is posted as a comment or integrated into the PR check summary
