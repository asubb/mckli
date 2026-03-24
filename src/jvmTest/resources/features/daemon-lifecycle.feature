Feature: Daemon Lifecycle Management
  As a user
  I want to manage daemon processes
  So that I can maintain persistent connections to MCP servers

  Background:
    Given a clean daemon directory
    And a server "testserver" exists with endpoint "http://localhost:8080/api"

  @requires-mock-server
  Scenario: Start a daemon process
    When I start the daemon for server "testserver"
    Then the daemon for "testserver" should be running
    And a PID file should exist for "testserver"
    And a socket file should exist for "testserver"

  @requires-mock-server
  Scenario: Stop a running daemon
    Given the daemon for server "testserver" is running
    When I stop the daemon for "testserver"
    Then the daemon for "testserver" should not be running
    And the PID file for "testserver" should not exist
    And the socket file for "testserver" should not exist

  @requires-mock-server
  Scenario: Check daemon status
    Given the daemon for server "testserver" is running
    When I check daemon status
    Then I should see "testserver" is RUNNING
    And I should see the PID for "testserver"

  @requires-mock-server
  Scenario: Restart a daemon
    Given the daemon for server "testserver" is running
    When I restart the daemon for "testserver"
    Then the daemon for "testserver" should be running
    And the PID should be different from before

  @requires-mock-server
  Scenario: Auto-start daemon on first request
    When I send a tools list request to "testserver"
    Then the daemon for "testserver" should be running
    And the request should complete successfully

  Scenario: Handle daemon start failure for non-existent server
    When I try to start the daemon for server "nonexistent"
    Then the daemon start should fail
    And I should see an error about server not found

  @requires-mock-server
  Scenario: Multiple daemons running concurrently
    Given a server "server1" exists with endpoint "http://localhost:8081/api"
    And a server "server2" exists with endpoint "http://localhost:8082/api"
    When I start the daemon for server "server1"
    And I start the daemon for server "server2"
    Then the daemon for "server1" should be running
    And the daemon for "server2" should be running
    And each daemon should have its own PID file
    And each daemon should have its own socket file
