## Context

The `mckli` CLI currently uses Kotlin Multiplatform (KMP) to target both JVM and Native (Linux, macOS, Windows). This setup requires complex build scripts and CI/CD pipelines. Native compilation is slow and KMP adds unnecessary complexity for a project that will only target the JVM going forward. The current Java 21 requirement is also restrictive for some environments.

## Goals / Non-Goals

**Goals:**
- **Remove Kotlin Multiplatform (KMP)**: Convert the project to a standard Kotlin JVM project.
- **Simplify build and CI/CD**: Reduce complexity by targeting only JVM 17.
- **Widen compatibility**: Lower the minimum Java requirement to version 17.

**Non-Goals:**
- **Rewrite in another language**: The project remains in Kotlin.
- **Support Native**: All native-specific code and configurations will be removed.

## Decisions

- **Decision 1: Standard Kotlin JVM**: Instead of KMP with only JVM target, completely remove KMP and use the standard `kotlin("jvm")` plugin. This is the simplest structure for a JVM-only project.
- **Decision 2: Simplify Source Sets**: Move all code from `src/commonMain`, `src/jvmMain`, and `src/nativeMain` into `src/main/kotlin`. Replace all `expect`/`actual` declarations with the actual JVM implementations.
- **Decision 3: Remove Native-specific code**: Delete any code that was specifically added to support native targets and is no longer needed (e.g., `ConfigManager.native.kt`).

## Risks / Trade-offs

- **Risk: Performance**: Native binary was faster to start. Mitigation: JVM startup time for this CLI is acceptable (often under 200ms with modern JVMs).
- **Trade-off: Project Restructuring**: Moving files from KMP structure to standard JVM structure requires care to avoid breaking package structure or missing files. This will be handled systematically in the implementation tasks.
