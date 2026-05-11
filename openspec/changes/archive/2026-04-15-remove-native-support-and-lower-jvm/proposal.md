## Why

The current project supports both JVM and Native targets using Kotlin Multiplatform, which increases build complexity and maintenance overhead. Given the nature of this CLI tool, focusing on a single robust platform (JVM) while lowering the minimum required version (to JVM 17) and removing Kotlin Multiplatform (KMP) support entirely will simplify the project structure and improve developer experience.

## What Changes

- **REMOVAL**: Drop support for Native targets (Linux, macOS, Windows).
- **REMOVAL**: Remove Kotlin Multiplatform (KMP) support.
- **MODIFICATION**: Convert the project to a standard Kotlin JVM project.
- **MODIFICATION**: Lower the JVM target version from 21 to 17.
- **MODIFICATION**: Update installation scripts and CI/CD workflows to reflect these changes.
- **MODIFICATION**: Merge all source sets into a single `src/main/kotlin` and `src/test/kotlin` structure.

## Capabilities

### Modified Capabilities
- `cli-foundation`: Update to support JVM 17 as the minimum requirement and transition from KMP to standard JVM.

## Impact

- **Build System**: `build.gradle.kts` will be significantly simplified by removing KMP and native target configurations.
- **CI/CD**: GitHub Actions workflows (`ci.yml`, `release.yml`) need to be updated to remove native build jobs and use standard JVM build tasks.
- **Installation**: `scripts/install.sh` must be updated to check for Java 17 instead of Java 21.
- **Codebase**: All source code will be moved from `src/commonMain`, `src/jvmMain`, and `src/nativeMain` into standard `src/main` and `src/test` directories. Platform-specific `expect`/`actual` declarations will be replaced with JVM implementations.
