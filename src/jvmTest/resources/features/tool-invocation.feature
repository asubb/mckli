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
    And the result should contain "content" with value "Hello, World!"
    And the result should contain "size" with value 13

  @requires-mock-server
  Scenario: Call a tool without arguments
    Given the MCP server has a tool "list-users" that accepts no parameters
    And the tool "list-users" returns:
      """
      {"users": ["alice", "bob", "charlie"]}
      """
    When I call tool "list-users" without arguments
    Then the tool execution should succeed
    And the result should contain 3 users

  @requires-mock-server
  Scenario: Handle tool execution error
    Given the MCP server has a tool "failing-tool"
    And the tool "failing-tool" returns an error "Tool execution failed"
    When I call tool "failing-tool" without arguments
    Then the tool execution should fail
    And I should see error message "Tool execution failed"

  @requires-mock-server
  Scenario: Call non-existent tool
    When I try to call tool "nonexistent-tool" without arguments
    Then the tool execution should fail
    And I should see error message "Tool 'nonexistent-tool' not found"

  @requires-mock-server
  Scenario: Tool with complex nested arguments
    Given the MCP server has a tool "search" that accepts:
      | parameter | type   | required |
      | query     | string | true     |
      | options   | object | false    |
    And the tool "search" returns:
      """
      {"results": ["file1.txt", "file2.txt"], "count": 2}
      """
    When I call tool "search" with arguments:
      """
      {
        "query": "*.txt",
        "options": {
          "caseSensitive": false,
          "maxResults": 10
        }
      }
      """
    Then the tool execution should succeed
    And the result should contain "count" with value 2

  @requires-mock-server
  Scenario: Tool execution timeout
    Given the MCP server has a tool "slow-tool"
    And the tool "slow-tool" takes 150 seconds to respond
    And the tool timeout is set to 5 seconds
    When I call tool "slow-tool" without arguments
    Then the tool execution should fail
    And I should see error message about timeout

  @requires-mock-server
  Scenario: JSON output formatting
    Given the MCP server has a tool "get-data"
    And the tool "get-data" returns:
      """
      {"name": "test", "value": 42, "nested": {"key": "value"}}
      """
    When I call tool "get-data" with JSON output format
    Then the output should be valid JSON
    And the output should be pretty-printed
