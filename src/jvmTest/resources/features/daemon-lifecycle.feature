Feature: Daemon Lifecycle Management
  As a user
  I want to manage daemon processes
  So that I can maintain persistent connections to MCP servers

  Background:
    Given a clean daemon directory
    And a server "testserver" exists with endpoint "http://localhost:8080/api"

  @requires-mock-server
  Scenario: Basic daemon lifecycle
    When I start the daemon for server "testserver"
    Then the daemon for "testserver" should be running
    When I restart the daemon for "testserver"
    Then the daemon for "testserver" should be running
    When I stop the daemon for "testserver"
    Then the daemon for "testserver" should not be running

  @requires-mock-server
  Scenario: Check daemon status
    Given the daemon for server "testserver" is running
    When I check daemon status
    Then I should see "testserver" is RUNNING

  @requires-mock-server
  Scenario: Auto-start daemon on first request
    When I send a tools list request to "testserver"
    Then the daemon for "testserver" should be running
    And the request should complete successfully

  @requires-mock-server
  Scenario: Running multiple daemons
    Given a server "server1" exists with endpoint "http://localhost:8081/api"
    And a server "server2" exists with endpoint "http://localhost:8082/api"
    When I start the daemon for server "server1"
    And I start the daemon for server "server2"
    Then both daemons for "server1" and "server2" should be running
