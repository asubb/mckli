## Context

The `mckli` project is a Kotlin Multiplatform CLI tool using Gradle. Currently, there is no automated CI/CD pipeline. All builds and releases are handled manually by developers. This design outlines the implementation of GitHub Actions to automate these processes and simplify user installation.

## Goals / Non-Goals

**Goals:**
- Automate JVM builds and tests on every Pull Request.
- Automate the creation of GitHub Releases and artifact uploading when a version tag is pushed.
- Provide clear test reporting on Pull Requests.
- Implement a one-liner installation script for users.

**Non-Goals:**
- Automated deployment to package managers (e.g., Maven Central, Homebrew) - initially, only GitHub Releases will be used.
- Multi-architecture native builds (e.g., ARM64 vs X64) - initial focus is on standard runners provided by GitHub (Linux x64).

## Decisions

### 1. Workflow Structure
Split into two primary workflows:
- `ci.yml`: Triggers on `push` to `main` and all `pull_request`. Runs `./gradlew build`.
- `release.yml`: Triggers on `push` with tags `v*`. Runs `./gradlew distZip`, then creates a GitHub Release.

### 2. Test Reporting
**Decision**: Use `action-junit-report` or similar to publish test results.
**Rationale**: Provides better visibility for PR reviewers without needing to dig into raw logs.

### 3. Native Build (Deferred)
**Decision**: Native builds are not yet automated. Initial focus is JVM-only.
**Rationale**: Postponing native builds allows for faster initial CI/CD setup.

### 4. Installation Script
**Decision**: A shell script hosted in the repository (e.g., `scripts/install.sh`) and triggered by a `curl | sh` one-liner.
**Rationale**: Allows for logic to detect OS (Linux/macOS) and choose the correct binary from the latest release.

### 5. Artifact Naming
**Decision**: Use default `distribution` plugin naming: `mckli-<version>.zip`.
**Rationale**: Standard and consistent with Gradle conventions.

## Risks / Trade-offs

- **Risk**: Native compilation can be slow and resource-intensive on GitHub runners.
  - **Mitigation**: Use Gradle caching to speed up subsequent builds.
- **Risk**: `curl | sh` is often criticized for security.
  - **Mitigation**: Ensure the script is simple, transparent, and hosted directly in the official repository. Provide the manual installation steps as an alternative.
- **Risk**: GitHub API rate limits for the installation script.
  - **Mitigation**: Use the `/releases/latest/download/` redirect URL which doesn't count against API limits as heavily as the API itself.
