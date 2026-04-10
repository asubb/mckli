## Why

Currently, the project uses a custom `fatJar` task to package the JVM application. While this works for simple cases, it lacks standard features like start scripts, structured distribution (lib/bin folders), and proper handling of JVM arguments. Adopting the official Gradle `application` plugin provides a standardized way to package and distribute the JVM part of the Kotlin Multiplatform application, ensuring compatibility with common deployment patterns.

## What Changes

- Add the `application` plugin to the Gradle build.
- Configure the `application` plugin with the main class and executable name.
- Remove the custom `fatJar` task in favor of the standard `distZip` and `distTar` tasks provided by the plugin.
- Ensure the JVM distribution is easily accessible and follows standard directory structures.

## Capabilities

### New Capabilities
- `jvm-distribution`: Standardized JVM application packaging including start scripts and dependency management.

### Modified Capabilities
- None: This change is focused on implementation details of distribution and doesn't change functional requirements.

## Impact

- **Build System**: `build.gradle.kts` will be modified to include and configure the `application` plugin.
- **Artifacts**: Instead of `mckli-all.jar`, the build will produce `mckli-<version>.zip` and `.tar` containing the full distribution.
- **CI/CD**: Any scripts relying on `fatJar` task or the resulting JAR file will need to be updated.
