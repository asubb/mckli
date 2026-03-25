Feature: SSE Transport
  As a user
  I want to use SSE transport for MCP servers
  So that I can benefit from real-time streaming communication

  @requires-sse-server
  Scenario: Configure server with SSE transport
    Given a server "sseserver" with SSE transport
    When I check the server configuration
    Then the transport should be "SSE"

  @requires-sse-server
  Scenario: Start daemon with SSE transport
    Given a server "sseserver" with SSE transport configured
    When I start the daemon for SSE server "sseserver"
    Then the SSE daemon for "sseserver" should be running
    And SSE connection should be established

  @requires-sse-server
  Scenario: SSE connection handles reconnection
    Given the daemon for SSE server "sseserver" is running
    When the SSE connection is interrupted
    Then the daemon should attempt reconnection
    And the connection should be reestablished within 5 seconds

  @requires-sse-server
  Scenario: Stop daemon gracefully closes SSE connection
    Given the daemon for SSE server "sseserver" is running
    When I stop the daemon for SSE server "sseserver"
    Then the SSE connection should be closed gracefully
    And the SSE daemon for "sseserver" should not be running

  @requires-sse-server
  Scenario: Tool discovery works over SSE transport
    Given a server "sseserver" with SSE transport
    And the SSE server provides a dynamic POST endpoint "/messages/?session_id=123"
    And the SSE server has tools:
      | name      | description    |
      | sse-tool  | SSE tool       |
    When I start the daemon for SSE server "sseserver"
    And I list tools from SSE server "sseserver"
    Then I should see tool "sse-tool" from SSE server
    And the request should have been sent to the dynamic endpoint "/messages/?session_id=123"
