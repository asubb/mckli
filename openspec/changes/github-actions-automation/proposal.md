## Why

The project currently lacks automated CI/CD processes. Manually running builds, tests, and publishing releases is error-prone and inefficient. Implementing GitHub Actions will ensure code quality through automated testing on PRs and automate the delivery of artifacts (JVM distributions) to users.

## What Changes

- Add a GitHub Actions workflow for Continuous Integration (CI) to run on every Pull Request and push to the `main` branch.
- Add a GitHub Actions workflow for Continuous Delivery (CD) to run when a new Git tag is created.
- Automated building and testing of JVM targets.
- Automated publication of test results to Pull Requests for better visibility.
- Automated creation of GitHub Releases with attached artifacts (JVM distribution) when a tag is pushed.
- Update the documentation (Quick Start) with a one-liner `curl` command to install the latest release.

## Capabilities

### New Capabilities
- `ci-workflow`: Defines the automated build and test process for pull requests and main branch updates.
- `cd-workflow`: Defines the automated release process, including artifact generation and GitHub Release creation upon tagging.
- `automated-installation`: Provides a simplified, one-liner installation method for users to fetch the latest binary.

### Modified Capabilities
- None: No existing core functional requirements are changing; this is an infrastructure and developer experience enhancement.

## Impact

- **CI/CD Infrastructure**: New `.github/workflows` directory.
- **Documentation**: `README.md` and potentially `docs/user/README.md` will be updated with installation instructions.
- **Development Workflow**: Developers will receive immediate feedback on PRs via GitHub Actions.
- **User Experience**: Users will have access to pre-built binaries and a simpler installation process.
