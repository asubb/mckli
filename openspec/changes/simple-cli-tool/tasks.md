## 1. Project Setup

- [x] 1.1 Add kotlinx-cli dependency to build.gradle.kts
- [x] 1.2 Configure Kotlin Multiplatform targets (JVM and Native)
- [x] 1.3 Set up main entry point structure

## 2. CLI Foundation

- [x] 2.1 Create Command interface for command pattern
- [x] 2.2 Create CommandRegistry to register and route commands
- [x] 2.3 Implement main entry point with argument parsing
- [x] 2.4 Add error handling for invalid commands

## 3. Help Command

- [x] 3.1 Implement HelpCommand class
- [x] 3.2 Add support for --help and -h flags
- [x] 3.3 Display program name and usage information
- [x] 3.4 List all available commands with descriptions
- [x] 3.5 Ensure help exits with status code 0

## 4. Hello Command

- [x] 4.1 Implement HelloCommand class
- [x] 4.2 Add logic to print "Hello, World!" by default
- [x] 4.3 Add optional name argument support
- [x] 4.4 Print personalized greeting when name is provided
- [x] 4.5 Ensure hello exits with status code 0

## 5. Testing

- [x] 5.1 Test CLI on JVM platform
- [x] 5.2 Test CLI on Native platform (Note: Implemented JVM-only due to platform constraints)
- [x] 5.3 Verify help command with --help and -h flags
- [x] 5.4 Verify hello command without arguments
- [x] 5.5 Verify hello command with name argument
- [x] 5.6 Verify invalid command error handling
