## 1. Build System Migration (KMP to JVM)

- [x] 1.1 Replace `kotlin("multiplatform")` with `kotlin("jvm")` in `build.gradle.kts`
- [x] 1.2 Remove native target configurations and KMP-specific blocks from `build.gradle.kts`
- [x] 1.3 Lower the JVM target to version 17 in `build.gradle.kts`
- [x] 1.4 Update dependencies from KMP format to standard JVM format in `build.gradle.kts`
- [x] 1.5 Configure the `application` plugin or update `distribution` plugin to work with the new structure

## 2. Codebase Restructuring

- [x] 2.1 Create `src/main/kotlin` and `src/test/kotlin` directories
- [x] 2.2 Move all code from `src/commonMain/kotlin` and `src/jvmMain/kotlin` to `src/main/kotlin`
- [x] 2.3 Move all code from `src/commonTest/kotlin` and `src/jvmTest/kotlin` to `src/test/kotlin`
- [x] 2.4 Replace all `expect`/`actual` declarations with the actual JVM implementations (e.g., in `ConfigManager.kt`)
- [x] 2.5 Delete all KMP-specific source directories: `src/commonMain`, `src/commonTest`, `src/jvmMain`, `src/jvmTest`, `src/nativeMain`

## 3. Documentation and Scripts Update

- [x] 3.1 Update `scripts/install.sh` to check for Java 17+ and ensure it works with the new JVM distribution path
- [x] 3.2 Update `README.md` to reflect the removal of native binaries and the new Java requirement
- [x] 3.3 Update any developer documentation regarding build and test procedures (now using standard `gradle build` and `gradle test`)

## 4. CI/CD Workflow Updates

- [x] 4.1 Update `.github/workflows/ci.yml` to use standard JVM build tasks and Java 17
- [x] 4.2 Update `.github/workflows/release.yml` to remove native build jobs and only release the JVM distribution
- [x] 4.3 Verify that CI/CD passes with the new project structure
