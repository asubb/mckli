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

Use `kotlinx-cli` for argument parsing. It's official, multiplatform-compatible, and lightweight.

**Alternatives considered:**

- `clikt`: More feature-rich but adds complexity we don't need yet
- Manual parsing: Error-prone and time-consuming

### Command Structure

Implement a simple command pattern with a `Command` interface. Each command (help, hello) implements this interface.

**Rationale:** Makes it easy to add new commands later without modifying core CLI logic.

### Entry Point

Create a `Main.kt` with platform-specific entry points:

- JVM: Standard `main(args: Array<String>)`
- Native: `fun main(args: Array<String>)`

Use expect/actual for any platform-specific code if needed.

## Risks / Trade-offs

**[Risk]** `kotlinx-cli` might not cover future complex CLI needs → **Mitigation:** Start simple, refactor to `clikt` if
requirements grow

**[Trade-off]** Command pattern adds abstraction overhead for just 2 commands → Acceptable for future extensibility
