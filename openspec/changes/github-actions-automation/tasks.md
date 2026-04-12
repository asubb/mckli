## 1. CI Workflow Setup

- [x] 1.1 Create `.github/workflows/ci.yml` file.
- [x] 1.2 Define `ci` workflow to run on PRs and push to `main`.
- [x] 1.3 Add job for JVM build and test (run `./gradlew jvmTest`).
- [ ] 1.4 Add job for Native build and test (Deferred).
- [x] 1.5 Configure test result publishing (e.g., using `action-junit-report`).

## 2. CD Workflow Setup

- [x] 2.1 Create `.github/workflows/release.yml` file.
- [x] 2.2 Define `release` workflow to run on tag push (`v*`).
- [x] 2.3 Add job to build JVM distribution (`./gradlew distZip distTar`).
- [ ] 2.4 Add job to build Native executables (Deferred).
- [x] 2.5 Configure GitHub Release creation and artifact upload.

## 3. Automated Installation

- [x] 3.1 Create `scripts/install.sh` to detect OS and download the latest release binary.
- [x] 3.2 Update `README.md` to include the one-liner `curl` installation command in the "Quick Start" section.
- [x] 3.3 Ensure the installation script correctly handles the latest release redirect.

## 4. Verification

- [x] 4.1 Verify workflows syntax using a linter (if available) or by inspection.
- [x] 4.2 Verify the installation command points to the correct (planned) repository location.
