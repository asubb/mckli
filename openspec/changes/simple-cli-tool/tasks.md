## 1. Project Setup

- [ ] 1.1 Add kotlinx-cli dependency to build.gradle.kts
- [ ] 1.2 Configure Kotlin Multiplatform targets (JVM and Native)
- [ ] 1.3 Set up main entry point structure

## 2. CLI Foundation

- [ ] 2.1 Create Command interface for command pattern
- [ ] 2.2 Create CommandRegistry to register and route commands
- [ ] 2.3 Implement main entry point with argument parsing
- [ ] 2.4 Add error handling for invalid commands

## 3. Help Command

- [ ] 3.1 Implement HelpCommand class
- [ ] 3.2 Add support for --help and -h flags
- [ ] 3.3 Display program name and usage information
- [ ] 3.4 List all available commands with descriptions
- [ ] 3.5 Ensure help exits with status code 0

## 4. Hello Command

- [ ] 4.1 Implement HelloCommand class
- [ ] 4.2 Add logic to print "Hello, World!" by default
- [ ] 4.3 Add optional name argument support
- [ ] 4.4 Print personalized greeting when name is provided
- [ ] 4.5 Ensure hello exits with status code 0

## 5. Testing

- [ ] 5.1 Test CLI on JVM platform
- [ ] 5.2 Test CLI on Native platform
- [ ] 5.3 Verify help command with --help and -h flags
- [ ] 5.4 Verify hello command without arguments
- [ ] 5.5 Verify hello command with name argument
- [ ] 5.6 Verify invalid command error handling
