# Integration Testing with Cucumber

Guide to running and writing Cucumber-based integration tests for mckli.

## Overview

mckli uses Cucumber (BDD framework) for integration testing, allowing us to write tests in natural language (Gherkin) that verify end-to-end functionality.

## Test Structure

```
src/jvmTest/
├── kotlin/com/mckli/integration/
│   ├── CucumberTestRunner.kt          # JUnit test runner
│   ├── steps/                         # Step definitions
│   │   ├── ConfigurationSteps.kt      # Config management steps
│   │   ├── DaemonSteps.kt             # Daemon lifecycle steps
│   │   └── ToolSteps.kt               # Tool discovery/invocation steps
│   └── support/
│       └── MockMcpServer.kt           # Mock MCP server for testing
└── resources/features/                # Gherkin feature files
    ├── configuration.feature          # Configuration scenarios
    ├── daemon-lifecycle.feature       # Daemon management scenarios
    ├── tool-discovery.feature         # Tool discovery scenarios
    └── tool-invocation.feature        # Tool execution scenarios
```

## Running Integration Tests

### Run All Integration Tests

```bash
./gradlew test --tests "com.mckli.integration.CucumberTestRunner"
```

### Run Specific Features

```bash
# Run only configuration tests
./gradlew test --tests "com.mckli.integration.CucumberTestRunner" \
  -Dcucumber.filter.tags="@configuration"

# Run only daemon tests
./gradlew test --tests "com.mckli.integration.CucumberTestRunner" \
  -Dcucumber.filter.tags="@daemon"
```

### Skip Tests Requiring Mock Server

```bash
./gradlew test --tests "com.mckli.integration.CucumberTestRunner" \
  -Dcucumber.filter.tags="not @requires-mock-server"
```

### View Test Reports

After running tests:
```bash
# HTML report
open build/reports/cucumber.html

# JSON report (for CI)
cat build/reports/cucumber.json
```

## Feature Files

### Configuration Feature

Tests configuration management:
- Adding/removing servers
- Authentication setup
- Validation
- Default server selection

**Example scenario:**
```gherkin
Scenario: Add a new server configuration
  When I add a server with name "testserver" and endpoint "https://example.com/api"
  Then the configuration should contain server "testserver"
  And the server "testserver" should have endpoint "https://example.com/api"
```

### Daemon Lifecycle Feature

Tests daemon process management:
- Starting/stopping daemons
- Status checking
- Auto-start behavior
- Multiple concurrent daemons

**Example scenario:**
```gherkin
Scenario: Start a daemon process
  When I start the daemon for server "testserver"
  Then the daemon for "testserver" should be running
  And a PID file should exist for "testserver"
  And a socket file should exist for "testserver"
```

### Tool Discovery Feature

Tests tool caching and discovery:
- Listing tools
- Describing tools
- Filtering
- Cache refresh

**Example scenario:**
```gherkin
Scenario: List all available tools
  Given the MCP server has tools:
    | name        | description           |
    | read-file   | Read a file           |
    | write-file  | Write to a file       |
  When I list tools from "testserver"
  Then I should see 2 tools
  And I should see tool "read-file"
```

### Tool Invocation Feature

Tests tool execution:
- Calling tools with arguments
- Error handling
- Timeout behavior
- Output formatting

**Example scenario:**
```gherkin
Scenario: Call a tool with arguments
  Given the MCP server has a tool "read-file"
  And the tool "read-file" returns:
    """
    {"content": "Hello, World!", "size": 13}
    """
  When I call tool "read-file" with arguments:
    """
    {"path": "/tmp/test.txt"}
    """
  Then the tool execution should succeed
  And the result should contain "content" with value "Hello, World!"
```

## Mock MCP Server

The `MockMcpServer` provides a test double for integration testing without requiring a real MCP server.

### Features

- Configurable tool responses
- Error simulation
- Delay simulation
- Server unavailability simulation

### Usage in Tests

```kotlin
// In step definitions
val mockServer = MockMcpServer(port = 8080)
mockServer.start()

// Add tools
mockServer.addTool(
    name = "read-file",
    description = "Read a file",
    response = Json.parseToJsonElement("""{"content": "test"}""")
)

// Simulate errors
mockServer.addTool(
    name = "failing-tool",
    error = "Tool execution failed"
)

// Simulate delays
mockServer.addTool(
    name = "slow-tool",
    delayMs = 5000
)

// Stop responding
mockServer.setResponding(false)

// Clean up
mockServer.stop()
```

## Writing New Tests

### 1. Create Feature File

Create a new `.feature` file in `src/jvmTest/resources/features/`:

```gherkin
Feature: My New Feature
  As a user
  I want to do something
  So that I can achieve a goal

  Scenario: My test scenario
    Given some precondition
    When I perform an action
    Then I should see the expected result
```

### 2. Implement Step Definitions

Create or extend step definition file in `src/jvmTest/kotlin/com/mckli/integration/steps/`:

```kotlin
package com.mckli.integration.steps

import io.cucumber.java8.En

class MyFeatureSteps : En {
    init {
        Given("some precondition") {
            // Setup code
        }

        When("I perform an action") {
            // Action code
        }

        Then("I should see the expected result") {
            // Assertion code
        }
    }
}
```

### 3. Use Data Tables

For multiple similar items:

```gherkin
Given the MCP server has tools:
  | name       | description    |
  | tool1      | First tool     |
  | tool2      | Second tool    |
```

```kotlin
Given("the MCP server has tools:") { dataTable: DataTable ->
    val tools = dataTable.asMaps()
    tools.forEach { row ->
        mockServer?.addTool(
            name = row["name"]!!,
            description = row["description"]
        )
    }
}
```

### 4. Use Doc Strings

For JSON or multi-line content:

```gherkin
When I call tool "read-file" with arguments:
  """
  {
    "path": "/tmp/file.txt",
    "encoding": "utf-8"
  }
  """
```

```kotlin
When("I call tool {string} with arguments:") { toolName: String, argsJson: String ->
    val args = Json.parseToJsonElement(argsJson)
    // Use args
}
```

## Test Tags

Use tags to organize and filter tests:

### Available Tags

- `@configuration` - Configuration tests
- `@daemon` - Daemon lifecycle tests
- `@tools` - Tool-related tests
- `@requires-mock-server` - Needs MockMcpServer
- `@skip` - Skip this scenario

### Usage

```gherkin
@requires-mock-server
Scenario: Test with mock server
  Given a mock MCP server is running
  ...

@skip
Scenario: Not implemented yet
  ...
```

## Hooks

### Before/After Hooks

Defined in step definition files:

```kotlin
Before { ->
    // Setup before each scenario
}

After { ->
    // Cleanup after each scenario
}

Before("@requires-mock-server") { ->
    // Setup only for scenarios with this tag
    mockServer = MockMcpServer()
    mockServer?.start()
}

After("@requires-mock-server") { ->
    mockServer?.stop()
}
```

## Best Practices

### 1. Keep Scenarios Independent

Each scenario should:
- Set up its own preconditions
- Clean up after itself
- Not depend on other scenarios

### 2. Use Background for Common Setup

```gherkin
Feature: My Feature

  Background:
    Given a clean configuration directory
    And a server "testserver" exists

  Scenario: First test
    ...

  Scenario: Second test
    ...
```

### 3. Write Declarative Steps

**Good:**
```gherkin
When I add a server "myserver"
Then the server should be configured
```

**Bad:**
```gherkin
When I click the add button
And I type "myserver" in the name field
And I type "https://..." in the endpoint field
And I click save
```

### 4. Reuse Step Definitions

Write generic steps that can be reused:

```kotlin
Then("the {string} should be {string}") { field: String, value: String ->
    // Generic assertion
}
```

### 5. Handle Async Operations

```kotlin
When("I start the daemon") {
    daemon.start()
    Thread.sleep(1000) // Give time to initialize
}
```

## Troubleshooting

### Tests Fail to Find Steps

```
Undefined step: When I add a server
```

**Solution:** Ensure step definitions match exactly:
- Check for typos
- Verify parameter placeholders `{string}`, `{int}`
- Make sure glue path is correct in `CucumberTestRunner`

### Mock Server Port Conflicts

```
Address already in use: 8080
```

**Solution:**
- Ensure After hooks clean up properly
- Use random ports: `MockMcpServer(port = Random.nextInt(8000, 9000))`
- Check for leaked processes

### Daemon Won't Stop

**Solution:**
- Use `daemon.stop(force = true)` in After hooks
- Check daemon logs for errors
- Manually clean up: `killall mckli-daemon`

## Continuous Integration

### GitHub Actions Example

```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  integration-tests:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run Integration Tests
        run: ./gradlew test --tests "com.mckli.integration.*"

      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: cucumber-reports
          path: build/reports/cucumber.html
```

## Future Improvements

- [ ] Add performance/stress test scenarios
- [ ] Test multiplatform compatibility (Native)
- [ ] Add visual regression tests for CLI output
- [ ] Implement parallel scenario execution
- [ ] Add code coverage reporting
- [ ] Test security scenarios (auth failures, etc.)

## Resources

- [Cucumber Documentation](https://cucumber.io/docs/cucumber/)
- [Cucumber JVM](https://github.com/cucumber/cucumber-jvm)
- [Gherkin Reference](https://cucumber.io/docs/gherkin/reference/)
- [mckli Testing Guide](./TESTING.md)
