## Context

The `mckli` project is a Kotlin Multiplatform CLI tool. Currently, the JVM target is distributed using a custom `fatJar` task in `build.gradle.kts`. While this produces a single runnable JAR, it's not a standard practice for complex Java/Kotlin applications which often require start scripts, separate library directories, and configurable JVM options.

## Goals / Non-Goals

**Goals:**
- Replace the custom `fatJar` task with the standard Gradle `application` plugin.
- Configure the application with `mainClass` and `applicationName`.
- Generate standard ZIP/TAR distributions.
- Maintain existing functionality (running via Gradle and as a standalone application).

**Non-Goals:**
- Changing the native target distribution.
- Implementing advanced installer generation (e.g., DEB, RPM, MSI).
- Changing the application's internal architecture.

## Decisions

- **Decision 1: Use the `distribution` plugin instead of `application`.**
  - **Rationale**: The `application` plugin is incompatible with `kotlin("multiplatform")` plugin in a single project. The `distribution` plugin provides the core distribution features (`distZip`, `distTar`, `installDist`) and is compatible. We'll manually configure the JAR packaging and start scripts to achieve the same result.
  - **Alternatives**: Using a separate subproject for the JVM application (more complex restructuring) or sticking with the `fatJar` task (less standardized).

- **Decision 2: Use the standard `Jar` task for `jvmMain` instead of `fatJar`.**
  - **Rationale**: Standard distribution typically includes a main application JAR and its dependencies as separate files in a `lib` directory.
  - **Details**: Configure the default `jvmJar` task.

- **Decision 3: Use the `CreateStartScripts` task to generate scripts.**
  - **Rationale**: To provide standard Unix/Windows entry points as the `application` plugin does.

## Risks / Trade-offs

- **[Risk]** → Users relying on a single fat JAR might find the ZIP distribution more cumbersome to use as it requires extraction.
  - **Mitigation** → The ZIP distribution is more standard for full applications. For single-file execution, the `installDist` task can be used during development, and native binaries are still available for other targets.
- **[Risk]** → KMP interaction with the `application` plugin.
  - **Mitigation** → The `application` plugin is primarily designed for the `java` plugin, but it works with `kotlin("multiplatform")` as long as the JVM target is properly configured and the `application` plugin is applied to the project.
