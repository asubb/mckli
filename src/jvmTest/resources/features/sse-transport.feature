Feature: SSE Transport
  As a user
  I want to use SSE transport for MCP servers
  So that I can benefit from real-time streaming communication

  @requires-sse-server
  Scenario: Basic SSE transport lifecycle
    Given a server "sseserver" with SSE transport
    When I start the daemon for SSE server "sseserver"
    Then the SSE daemon for "sseserver" should be running
    Then logs should not have any errors
    When I list tools from SSE server "sseserver"
    Then I should see tools from the SSE server
    When I stop the daemon for SSE server "sseserver"
    Then the SSE daemon for "sseserver" should not be running
