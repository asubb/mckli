Feature: Unified Daemon Lifecycle Management
  As a user
  I want to manage the unified daemon process
  So that I can maintain persistent connections to all MCP servers

  Background:
    Given a clean daemon directory
    And a server "testserver" exists with endpoint "http://localhost:8080/api"

  @requires-mock-server
  Scenario: Basic daemon lifecycle
    When I start the unified daemon
    Then the unified daemon should be running
    And logs should not have any errors
    When I restart the unified daemon
    Then the unified daemon should be running
    And logs should not have any errors
    When I stop the unified daemon
    Then the unified daemon should not be running

  @requires-mock-server
  Scenario: Check daemon status
    Given the unified daemon is running
    When I check daemon status
    Then I should see the unified daemon is RUNNING

  @requires-mock-server
  Scenario: Auto-start daemon on first request
    When I list tools for server "testserver"
    Then the unified daemon should be running
    And the request should complete successfully
