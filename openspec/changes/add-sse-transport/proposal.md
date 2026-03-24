## Why

The MCP CLI currently only supports HTTP POST transport for communicating with MCP servers. Adding Server-Sent Events (
SSE) transport enables real-time streaming capabilities, supports the full MCP protocol specification, and allows for
bidirectional communication patterns required by many MCP server implementations.

## What Changes

- Add SSE transport type alongside existing HTTP POST transport
- Extend `ServerConfig` to support transport type configuration (`http` or `sse`)
- Implement SSE client with connection management, event parsing, and reconnection logic
- Update daemon processes to handle SSE connections with persistent streaming
- Modify request routing to select appropriate transport based on server configuration
- Add error handling and timeout management specific to SSE streams

## Capabilities

### New Capabilities

- `sse-transport`: SSE-based transport implementation for MCP protocol communication, including connection
  establishment, event stream parsing, and automatic reconnection handling

### Modified Capabilities

<!-- Existing capabilities whose REQUIREMENTS are changing (not just implementation).
     Only list here if spec-level behavior changes. Each needs a delta spec file.
     Use existing spec names from openspec/specs/. Leave empty if no requirement changes. -->

## Impact

- `ServerConfig.kt`: Add transport type field to configuration model
- `HttpMcpClient.kt`: Refactor to support transport abstraction
- New SSE client implementation with Ktor SSE support
- `DaemonProcess`: Update to manage long-lived SSE connections
- `RequestRouter`: Add transport selection logic
- Configuration file format: Add optional `transport` field to server definitions
- Dependencies: Verify Ktor SSE plugin availability for Kotlin Multiplatform
