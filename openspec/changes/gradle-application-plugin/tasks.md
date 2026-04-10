## 1. Configure Distribution Plugin

- [x] 1.1 Add `distribution` plugin to the `plugins` block in `build.gradle.kts`.
- [x] 1.2 Configure JVM target with `withJava()` and `mainRun` block.
- [x] 1.3 Configure `jvmJar` task with manifest for standard JAR usage.
- [x] 1.4 Add and configure `CreateStartScripts` task named `startScripts`.
- [x] 1.5 Configure `distributions` block to include `startScripts` and all runtime dependencies.

## 3. Verification

- [x] 3.1 Run `./gradlew installDist` to verify local distribution creation.
- [x] 3.2 Execute the generated start script at `build/install/mckli/bin/mckli` to ensure the application starts correctly.
- [x] 3.3 Run `./gradlew distZip` to verify ZIP distribution creation.
