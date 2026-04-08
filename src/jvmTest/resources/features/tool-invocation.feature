Feature: Tool Invocation
  As a user
  I want to invoke MCP tools via the CLI
  So that LLMs can execute tools through simple commands

  Background:
    Given a clean daemon directory
    And a mock MCP server is running on port 8080
    And a server "testserver" exists with endpoint "http://localhost:8080/api"
    And the daemon for server "testserver" is running

  @requires-mock-server
  Scenario: Call a tool with arguments
    Given the MCP server has a tool "read-file" that accepts:
      | parameter | type   | required |
      | path      | string | true     |
    And the tool "read-file" returns:
      """
      {"content": "Hello, World!", "size": 13}
      """
    When I call tool "read-file" with arguments:
      """
      {"path": "/tmp/test.txt"}
      """
    Then the tool execution should succeed
    And the result should contain "content" and "size" values

  @requires-mock-server
  Scenario: Handle tool execution failures
    Given the MCP server has a tool "failing-tool"
    And the tool "failing-tool" returns an error "Tool execution failed"
    When I call tool "failing-tool" without arguments
    Then the tool execution should fail with error "Tool execution failed"
    When I try to call tool "nonexistent-tool" without arguments
    Then the tool execution should fail with error "Tool 'nonexistent-tool' not found"

  @requires-mock-server
  Scenario: Support various output formats
    Given the MCP server has a tool "get-data"
    And the tool "get-data" returns:
      """
      {"name": "test", "value": 42}
      """
    When I call tool "get-data" with JSON output format
    Then the output should be formatted as valid JSON
