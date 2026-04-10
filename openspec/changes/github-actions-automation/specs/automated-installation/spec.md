## ADDED Requirements

### Requirement: Single-Line Curl Installation
The documentation SHALL provide a one-liner `curl` command to download and install the latest `mckli` binary for the user's platform.

#### Scenario: One-liner Availability
- **WHEN** a user visits the "Quick Start" or "Installation" section in the README
- **THEN** they find a `curl` command that downloads the latest `mckli` executable

### Requirement: Installation Path Preference
The installation script SHALL follow standard Unix patterns, suggesting `/usr/local/bin` (with `sudo`) or `$HOME/.local/bin` as the installation directory.

#### Scenario: User Access Permissions
- **WHEN** the installation command is executed
- **THEN** it downloads the appropriate binary for the user's platform (e.g., Linux, macOS), makes it executable, and moves it to a folder in the user's PATH

### Requirement: Use of Latest Published Artifact
The installation command SHALL dynamically resolve and download the latest published artifact from the GitHub Releases.

#### Scenario: Fetching Latest Release
- **WHEN** the `curl` command is run
- **THEN** it uses the GitHub API or redirect URLs (e.g., `https://github.com/mckli/mckli/releases/latest/download/mckli`) to ensure the most recent version is downloaded
