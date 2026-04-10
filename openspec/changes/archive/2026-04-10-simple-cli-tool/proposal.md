## Why

The project needs a simple CLI tool to provide a foundation for command-line interactions, starting with basic help
documentation and a hello world command to verify the tooling works.

## What Changes

- Create a new CLI entry point with command parsing
- Add `--help` flag to display usage information
- Add `hello` command that prints a greeting message
- Set up the basic CLI structure for future command additions

## Capabilities

### New Capabilities

- `cli-foundation`: Core CLI infrastructure with argument parsing and command routing
- `help-command`: Display help text and usage information
- `hello-command`: Simple hello world command for demonstration and testing

### Modified Capabilities

<!-- No existing capabilities are being modified -->

## Impact

- New main entry point for CLI operations
- Dependencies: Clikt 5.1.0 for CLI framework
- Kotlin Multiplatform setup targeting JVM and Native platforms
- Build system: Gradle 9.4.1 with Kotlin 2.3.0
- Foundation for future CLI commands with extensible architecture
