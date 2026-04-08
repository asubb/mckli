# Testing Guide

Documentation for running and writing tests for mckli.

## Test Structure

```
src/commonTest/kotlin/com/mckli/
├── config/
│   ├── ConfigValidatorTest.kt       # Configuration validation
│   └── ServerConfigTest.kt          # Configuration serialization
├── http/
│   ├── McpRequestTest.kt            # MCP protocol messages
│   └── ConnectionPoolTest.kt        # Connection pool lifecycle
├── ipc/
│   └── IpcMessageTest.kt            # IPC protocol messages
└── tools/
    └── ToolMetadataTest.kt          # Tool metadata handling
```

## Running Tests

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Suites

```bash
# Configuration tests
./gradlew test --tests "com.mckli.config.*"

# HTTP client tests
./gradlew test --tests "com.mckli.http.*"

# IPC tests
./gradlew test --tests "com.mckli.ipc.*"

# Tool tests
./gradlew test --tests "com.mckli.tools.*"
```

### Integration Tests (Cucumber)

```bash
# Run all integration tests
./gradlew test --tests "com.mckli.integration.CucumberTestRunner"

# Run specific features by tag
./gradlew test --tests "com.mckli.integration.CucumberTestRunner" -Dcucumber.filter.tags="@daemon"
```

### Run Single Test Class

```bash
./gradlew test --tests "com.mckli.config.ConfigValidatorTest"
```

### Run with Reports

```bash
./gradlew test --info
# HTML report at: build/reports/tests/test/index.html
```

## Test Coverage

### Unit Tests (Completed)

#### Configuration Tests
- ✅ **ConfigValidatorTest** - Validation logic
  - Valid configuration acceptance
  - Invalid URL scheme rejection
  - Negative timeout rejection
  - Zero pool size rejection
  - Duplicate server name detection
  - Invalid default server detection
  - Multiple error collection

- ✅ **ServerConfigTest** - Serialization/deserialization
  - JSON serialization
  - JSON deserialization
  - Basic auth handling
  - Bearer auth handling
  - Multiple servers in configuration
  - Default value application

#### HTTP Client Tests
- ✅ **McpRequestTest** - MCP protocol
  - Request serialization
  - Request with parameters
  - Response with result parsing
  - Response with error parsing
  - Error serialization

- ✅ **ConnectionPoolTest** - Pool lifecycle
  - Initialization with config
  - Metrics tracking
  - Shutdown cleanup
  - Force shutdown
  - Metrics data class operations

#### IPC Tests
- ✅ **IpcMessageTest** - IPC protocol
  - McpRequest serialization
  - ListTools serialization
  - DescribeTool serialization
  - CallTool with arguments
  - RefreshTools serialization
  - Success response handling
  - Error response handling
  - Complex roundtrip preservation

#### Tool Tests
- ✅ **ToolMetadataTest** - Tool metadata
  - Metadata serialization
  - Metadata without schema
  - ToolList deserialization from MCP
  - Complex schema handling
  - Null description handling

### Integration Tests (Completed)

Integration tests use Cucumber to verify high-level user functionality and end-to-end flows.

- ✅ **Configuration Feature** - Management of MCP server configurations
- ✅ **Daemon Lifecycle Feature** - Process management (start/stop/restart/status)
- ✅ **Tool Discovery Feature** - Fetching, describing, and caching tools
- ✅ **Tool Invocation Feature** - Executing tools with arguments and handling results
- ✅ **SSE Transport Feature** - Real-time streaming communication (JVM only)

See [INTEGRATION-TESTING.md](INTEGRATION-TESTING.md) for more details.

## Writing Tests

### Test Conventions

```kotlin
class MyComponentTest {
    // Use descriptive test names with backticks
    @Test
    fun `component handles valid input correctly`() {
        // Arrange
        val input = "test input"

        // Act
        val result = myComponent.process(input)

        // Assert
        assertEquals("expected", result)
    }
}
```

### Testing with Coroutines

```kotlin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AsyncComponentTest {
    @Test
    fun `async operation completes successfully`() = runTest {
        val result = suspendingFunction()
        assertEquals("expected", result)
    }
}
```

### Testing Serialization

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Test
fun `data class serializes to JSON`() {
    val json = Json { prettyPrint = true }
    val data = MyData(field = "value")

    val jsonString = json.encodeToString(data)
    val decoded = json.decodeFromString<MyData>(jsonString)

    assertEquals(data, decoded)
}
```

### Testing Error Cases

```kotlin
@Test
fun `function throws exception on invalid input`() {
    assertFailsWith<MyException> {
        myFunction(invalidInput)
    }
}

@Test
fun `function returns error result on failure`() {
    val result = myFunction(invalidInput)

    assertTrue(result.isFailure)
    assert(result.exceptionOrNull() is MyException)
}
```

## Test Dependencies

From `build.gradle.kts`:

```kotlin
val commonTest by getting {
    dependencies {
        implementation(kotlin("test"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    }
}
```

## Manual Testing

### Testing Configuration Commands

```bash
# Add server
./gradlew jvmRun --args="config add testserver https://example.com/api --token test123"

# List servers
./gradlew jvmRun --args="config list"

# Remove server
./gradlew jvmRun --args="config remove testserver"
```

### Testing Daemon Commands

```bash
# Start daemon
./gradlew jvmRun --args="daemon start testserver"

# Check status
./gradlew jvmRun --args="daemon status"

# Stop daemon
./gradlew jvmRun --args="daemon stop testserver"
```

### Testing Tool Commands

```bash
# List tools (requires running daemon + MCP server)
./gradlew jvmRun --args="tools list testserver"

# Describe tool
./gradlew jvmRun --args="tools describe testserver read-file"

# Call tool
./gradlew jvmRun --args='tools call testserver read-file --json "{\"path\":\"/tmp/test.txt\"}"'
```

## Continuous Integration

For CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
- name: Run Tests
  run: ./gradlew test

- name: Generate Test Report
  if: always()
  run: |
    ./gradlew test --info
    cat build/reports/tests/test/index.html

- name: Upload Test Results
  if: always()
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: build/reports/tests/
```

## Future Testing Improvements

1. **Mock MCP Server** - Create test fixtures
2. **Integration Tests** - End-to-end flows
3. **Performance Tests** - Connection pool efficiency
4. **Stress Tests** - Multiple concurrent daemons
5. **Security Tests** - Authentication handling
6. **Platform Tests** - JVM vs Native compatibility

## Troubleshooting Tests

### Tests Fail to Compile

```bash
# Clean and rebuild
./gradlew clean test
```

### Coroutine Tests Hang

```bash
# Check for missing runTest wrapper
# All suspend functions should be tested with:
@Test
fun `my test`() = runTest {
    // test code
}
```

### Serialization Tests Fail

```bash
# Verify JSON format matches expected structure
# Use prettyPrint for debugging:
val json = Json { prettyPrint = true }
println(json.encodeToString(data))
```

## Next Steps

- Add performance benchmarks
- Set up CI/CD pipeline with test reporting
- Implement visual regression tests for CLI output
