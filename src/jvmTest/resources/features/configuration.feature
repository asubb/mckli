Feature: Configuration Management
  As a user
  I want to manage MCP server configurations
  So that I can connect to multiple servers

  Background:
    Given a clean configuration directory

  Scenario: Manage server configurations
    When I add a server with name "testserver" and endpoint "https://example.com/api"
    And I add a server with name "authserver" and endpoint "https://secure.example.com/api" and bearer token "token123"
    Then the configuration should contain servers "testserver" and "authserver"
    When I remove the server "testserver"
    Then the configuration should only contain server "authserver"

  Scenario: List configured servers
    Given a server "server1" exists with endpoint "https://api1.example.com"
    And a server "server2" exists with endpoint "https://api2.example.com"
    When I list all servers
    Then I should see both "server1" and "server2" in the list

  Scenario: Set default server
    Given a server "default-srv" exists with endpoint "https://default.example.com"
    When I set "default-srv" as the default server
    Then the default server should be "default-srv"
