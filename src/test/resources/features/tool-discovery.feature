Feature: Tool Discovery and Caching
  As a user
  I want to discover available tools from MCP servers
  So that I can invoke them via the CLI

  Background:
    Given a clean daemon directory
    And a mock MCP server is running on port 8080
    And a server "testserver" exists with endpoint "http://localhost:8080/api"
    And the daemon for server "testserver" is running

  @requires-mock-server
  Scenario: Discover and describe tools
    Given the MCP server has tools:
      | name        | description           |
      | read-file   | Read a file           |
      | write-file  | Write to a file       |
    When I list tools from "testserver"
    Then I should see tools "read-file" and "write-file"
    When I describe tool "read-file" from "testserver"
    Then I should see the tool name "read-file" and its description

  @requires-mock-server
  Scenario: Search and filter tools
    Given the MCP server has tools:
      | name         | description           |
      | read-file    | Read a file           |
      | write-file   | Write to a file       |
      | read-config  | Read configuration    |
    When I list tools from "testserver" with filter "read"
    Then I should see tools "read-file" and "read-config" but not "write-file"
    When I search for "file"
    Then I should see tools with names containing "file"

  @requires-mock-server
  Scenario: Tool caching and refresh
    Given the MCP server has tools:
      | name       | description    |
      | test-tool  | A test tool    |
    And I have listed tools from "testserver"
    When the MCP server stops responding
    And I list tools from "testserver" again
    Then I should still see tool "test-tool" from cache
    When the MCP server updates its tools to:
      | name      | description    |
      | new-tool  | New tool       |
    And I refresh tools for "testserver"
    Then I should see tool "new-tool" and not "test-tool"
