Feature: Configuration Management
  As a user
  I want to manage MCP server configurations
  So that I can connect to multiple servers

  Background:
    Given a clean configuration directory

  Scenario: Add a new server configuration
    When I add a server with name "testserver" and endpoint "https://example.com/api"
    Then the configuration should contain server "testserver"
    And the server "testserver" should have endpoint "https://example.com/api"

  Scenario: Add server with authentication
    When I add a server with name "authserver" and endpoint "https://secure.example.com/api" and bearer token "token123"
    Then the configuration should contain server "authserver"
    And the server "authserver" should have bearer authentication

  Scenario: Remove a server configuration
    Given a server "oldserver" exists with endpoint "https://old.example.com/api"
    When I remove the server "oldserver"
    Then the configuration should not contain server "oldserver"

  Scenario: List all configured servers
    Given a server "server1" exists with endpoint "https://api1.example.com"
    And a server "server2" exists with endpoint "https://api2.example.com"
    When I list all servers
    Then I should see 2 servers
    And I should see server "server1"
    And I should see server "server2"

  Scenario: Validate server configuration
    When I try to add a server with name "bad" and endpoint "ftp://invalid.com"
    Then the configuration validation should fail
    And I should see an error about invalid URL scheme

  Scenario: Set default server
    Given a server "default-srv" exists with endpoint "https://default.example.com"
    When I set "default-srv" as the default server
    Then the default server should be "default-srv"
