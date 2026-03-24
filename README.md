# mckli - Simple CLI Tool

A Kotlin Multiplatform command-line interface tool demonstrating modern CLI development with Clikt.

## Quick Start

```bash
# Build the project
./gradlew build

# Run with help
./gradlew jvmRun --args="--help"

# Run hello command
./gradlew jvmRun --args="hello"
./gradlew jvmRun --args="hello World"
```

## Features

- **Cross-platform**: Targets JVM and Native platforms (macOS, Linux, Windows)
- **Modern CLI framework**: Built with [Clikt 5.1.0](https://github.com/ajalt/clikt)
- **Extensible architecture**: Easy to add new commands
- **Type-safe**: Leverages Kotlin's type system for safer CLI development

## Requirements

- Java 21 or higher
- Gradle 9.4.1+ (included via wrapper)

## Building

### Build All Targets (Recommended)

```bash
./gradlew build
```

This builds the JVM target and creates a distributable JAR that works on all platforms.

### Build Native Binary (Platform-specific)

**macOS:**
```bash
./gradlew linkReleaseExecutableNative
```

**Linux x64:**
```bash
./gradlew linkReleaseExecutableNative
```

**Windows:**
```bash
gradlew.bat linkReleaseExecutableNative
```

**Note:** Native compilation is not available on Linux ARM64 hosts due to Kotlin Native limitations (no prebuilt compiler available). Use the JVM target on ARM64, or cross-compile from another platform.

## Running

### Run with Gradle (JVM)

```bash
# Show help
./gradlew jvmRun --args="--help"

# Run hello command
./gradlew jvmRun --args="hello"

# Run hello with name
./gradlew jvmRun --args="hello World"
```

### Run Native Binary

After building with `linkReleaseExecutableNative`, the native binary will be at:
```
build/bin/native/releaseExecutable/mckli
```

Run it directly:
```bash
./build/bin/native/releaseExecutable/mckli --help
./build/bin/native/releaseExecutable/mckli hello
./build/bin/native/releaseExecutable/mckli hello World
```

## Commands

### Built-in Help
Clikt automatically provides `--help` (or `-h`) for all commands:

```bash
mckli --help          # Show all commands
mckli hello --help    # Show help for hello command
```

### `hello [NAME]`
Display a greeting message. Optionally provide a name to personalize the greeting.

```bash
mckli hello           # Prints: Hello, World!
mckli hello Alice     # Prints: Hello, Alice!
```

## Project Structure

```
mckli/
├── src/
│   └── commonMain/kotlin/com/mckli/
│       ├── Main.kt                      # Application entry point
│       ├── MckliCommand.kt              # Root CLI command
│       └── commands/
│           └── HelloCommand.kt          # Hello command implementation
├── build.gradle.kts                     # Build configuration
├── settings.gradle.kts                  # Project settings
├── gradle.properties                    # Gradle properties
└── openspec/                            # OpenSpec change documentation
    └── changes/simple-cli-tool/
        ├── proposal.md                  # Why and what
        ├── design.md                    # Technical decisions
        ├── specs/                       # Requirements specifications
        └── tasks.md                     # Implementation checklist
```

## Adding New Commands

To add a new command:

1. Create a new file in `src/commonMain/kotlin/com/mckli/commands/`:

```kotlin
// MyCommand.kt
package com.mckli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional

class MyCommand : CliktCommand(name = "mycommand") {
    private val myArg by argument(help = "My argument description").optional()

    override fun run() {
        echo("My command executed with: ${myArg ?: "no argument"}")
    }
}
```

2. Register it in `Main.kt`:

```kotlin
fun main(args: Array<String>) = MckliCommand()
    .subcommands(
        HelloCommand(),
        MyCommand()  // Add your command here
    )
    .main(args)
```

3. Clikt automatically provides help for your command:
```bash
./gradlew jvmRun --args="mycommand --help"
```

## Technology Stack

| Component | Version |
|-----------|---------|
| **Kotlin** | 2.3.0 |
| **Clikt** | 5.1.0 |
| **Gradle** | 9.4.1 |
| **Java** | 21+ |

### Supported Platforms

| Platform | JVM | Native Binary |
|----------|-----|---------------|
| macOS (Intel) | ✅ | ✅ |
| macOS (Apple Silicon) | ✅ | ✅ |
| Linux x64 | ✅ | ✅ |
| Linux ARM64 | ✅ | ❌ (Kotlin limitation) |
| Windows x64 | ✅ | ✅ |

## Development

### Available Gradle Tasks

```bash
# Build all targets
./gradlew build

# Run JVM version
./gradlew jvmRun --args="[command]"

# Build native binary (on supported platforms)
./gradlew linkReleaseExecutableNative

# Run tests
./gradlew test

# Check code quality
./gradlew check

# Clean build artifacts
./gradlew clean

# List all available tasks
./gradlew tasks
```

## License

See [LICENSE](LICENSE) file for details.

## Contributing

This project follows the OpenSpec workflow for managing changes. See `openspec/changes/` for documentation on completed and in-progress changes.
