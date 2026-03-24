## Context

Building a Kotlin Multiplatform CLI tool targeting JVM and Native platforms. Currently no CLI infrastructure exists.
Need a minimal foundation for command-line interactions that works across both platforms with argument parsing and basic
commands.

## Goals / Non-Goals

**Goals:**

- Create a simple, extensible CLI architecture
- Support basic argument parsing with `--help` flag
- Implement hello world command as proof-of-concept
- Ensure cross-platform compatibility (JVM and Native)

**Non-Goals:**

- Advanced CLI features (subcommands, config files, plugins)
- Complex command chaining or piping
- Interactive mode or REPL
- Color output or advanced formatting

## Decisions

### CLI Library Choice

Use **Clikt 5.1.0** for argument parsing and command structure.

**Rationale:**
- Actively maintained (unlike kotlinx-cli which is deprecated)
- Full multiplatform support including linuxArm64
- Rich feature set with excellent API design
- Built-in subcommand support eliminates need for custom command pattern

**Alternatives considered:**
- `kotlinx-cli`: Deprecated, limited platform support (no linuxArm64)
- Manual parsing: Error-prone and time-consuming

### Architecture

Use Clikt's `CliktCommand` base class directly for all commands. Each command (help, hello) extends `CliktCommand`.

**Rationale:**
- Clikt provides built-in command routing and help generation
- No need for custom `Command` interface or `CommandRegistry`
- Cleaner, more maintainable code with less boilerplate

### Kotlin Multiplatform Configuration

**Targets:**
- JVM (primary development target)
- Native: macosArm64, macosX64, linuxX64, mingwX64

**Platform Limitations:**
- Linux ARM64 cannot compile native targets (Kotlin Native limitation - no prebuilt compiler)
- Native binaries can be cross-compiled from other supported platforms

### Entry Point

Single unified entry point using Clikt's extension function:
```kotlin
fun main(args: Array<String>) = MckliCommand()
    .subcommands(HelpCommand(), HelloCommand())
    .main(args)
```

## Risks / Trade-offs

**[Trade-off]** Cannot build native binaries directly on Linux ARM64 → Acceptable: JVM target works on all platforms, native can be cross-compiled

**[Trade-off]** Clikt is a larger dependency than kotlinx-cli → Acceptable: Better maintained, more features, saves custom code
