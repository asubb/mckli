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
  Scenario: List all available tools
    Given the MCP server has tools:
      | name        | description           |
      | read-file   | Read a file           |
      | write-file  | Write to a file       |
      | list-files  | List files in a dir   |
    When I list tools from "testserver"
    Then I should see 3 tools
    And I should see tool "read-file"
    And I should see tool "write-file"
    And I should see tool "list-files"

  @requires-mock-server
  Scenario: Describe a specific tool
    Given the MCP server has a tool "read-file" with schema:
      """
      {
        "type": "object",
        "properties": {
          "path": {"type": "string", "description": "File path"}
        },
        "required": ["path"]
      }
      """
    When I describe tool "read-file" from "testserver"
    Then I should see the tool name "read-file"
    And I should see the tool description
    And I should see the input schema with property "path"

  @requires-mock-server
  Scenario: Filter tools by name
    Given the MCP server has tools:
      | name         | description           |
      | read-file    | Read a file           |
      | write-file   | Write to a file       |
      | read-config  | Read configuration    |
    When I list tools from "testserver" with filter "read"
    Then I should see 2 tools
    And I should see tool "read-file"
    And I should see tool "read-config"
    But I should not see tool "write-file"

  @requires-mock-server
  Scenario: Tool cache persists across requests
    Given the MCP server has tools:
      | name       | description    |
      | test-tool  | A test tool    |
    And I have listed tools from "testserver"
    When the MCP server stops responding
    And I list tools from "testserver" again
    Then I should still see tool "test-tool"
    And the request should complete without contacting the MCP server

  @requires-mock-server
  Scenario: Refresh tool cache
    Given the MCP server has tools:
      | name      | description    |
      | old-tool  | Old tool       |
    And I have listed tools from "testserver"
    When the MCP server updates its tools to:
      | name      | description    |
      | new-tool  | New tool       |
    And I refresh tools for "testserver"
    Then I should see tool "new-tool"
    But I should not see tool "old-tool"

  @requires-mock-server
  Scenario: Handle empty tool list
    Given the MCP server has no tools
    When I list tools from "testserver"
    Then I should see a message "No tools available"
    And the request should complete successfully

  @requires-mock-server
  Scenario: Search for tools across all servers
    Given a mock MCP server is running on port 8081
    And a server "server2" exists with endpoint "http://localhost:8081/api"
    And the MCP server on port 8080 has tools:
      | name        | description           |
      | read-file   | Read a file           |
      | write-file  | Write to a file       |
    And the MCP server on port 8081 has tools:
      | name        | description           |
      | search-text | Search text in files  |
      | list-files  | List files            |
    When I search for "file"
    Then I should see search result "testserver:read-file"
    And I should see search result "testserver:write-file"
    And I should see search result "server2:search-text"
    And I should see search result "server2:list-files"

  @requires-mock-server
  Scenario: Search results in JSON format
    Given the MCP server has tools:
      | name        | description           |
      | read-file   | Read a file           |
    When I search for "read" with JSON format
    Then the output should be a JSON array
    And the JSON should contain a tool "read-file" from "testserver"
