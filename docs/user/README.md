# mckli User Manual

Complete guide to using mckli for wrapping MCP servers with persistent daemon connections.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Configuration](#configuration)
3. [Daemon Management](#daemon-management)
4. [Working with Tools](#working-with-tools)
5. [Advanced Usage](#advanced-usage)
6. [Troubleshooting](#troubleshooting)
7. [Examples](#examples)

---

## Getting Started

### Installation

1. **Prerequisites**
   - Java 21 or higher
   - Unix-like OS (Linux, macOS, or Windows WSL)

2. **Download or Build**

   **Option A: Build from source**
   ```bash
   git clone <repository-url>
   cd mckli

   # Build and install to local directory
   ./gradlew installDist
   
   # Add to PATH (example for bash)
   export PATH="$PATH:$(pwd)/build/install/mckli/bin"
   ```

   **Option B: Download release**
   ```bash
   # Use the installation script
   curl -sL https://raw.githubusercontent.com/asubb/mckli/refs/heads/main/scripts/install.sh | bash
   ```

3. **Verify installation**
   ```bash
   mckli --help
   ```

### First Steps

1. **Add your first MCP server**
   ```bash
   mckli config add myserver https://mcp.example.com/sse
   ```

2. **Check daemon status (starts automatically on first request)**
   ```bash
   mckli daemon status
   ```

3. **Search for tools**
   ```bash
   mckli tools search "read"
   ```

4. **List available tools**
   ```bash
   mckli tools list myserver
   ```

5. **Get more help**
   ```bash
   # Show global help
   mckli --help

   # Show help for a specific command group
   mckli tools --help

   # Show help for a specific command
   mckli tools call --help
   ```

---

## Configuration

### Adding Servers

#### Basic Configuration (SSE)
```bash
mckli config add myserver https://mcp.example.com/sse
```

#### Explicit Transport Type
```bash
# Force HTTP instead of default SSE
mckli config add legacy-server https://mcp.example.com/api --transport HTTP
```

### Authentication

#### With Bearer Token Authentication
```bash
mckli config add myserver https://mcp.example.com/sse \
  --token "your-bearer-token-here"
```

#### With Basic Authentication
```bash
mckli config add myserver https://mcp.example.com/sse \
  --username "myuser" \
  --password "mypassword"
```

#### With Custom Settings
```bash
mckli config add myserver https://mcp.example.com/api \
  --token "token" \
  --timeout 60000 \
  --pool-size 20
```

### Managing Configuration

#### List Servers
```bash
mckli config list
```

Output:
```
Configured servers:
  myserver (default)
    Endpoint: https://mcp.example.com/api
    Timeout: 30000ms
    Pool size: 10
    Auth: Bearer
```

#### Remove a Server
```bash
mckli config remove myserver
```

#### Edit Configuration Manually
```bash
mckli config edit
```

This displays the config file path: `~/.mckli/servers.json`

You can then edit it manually:
```json
{
  "servers": [
    {
      "name": "myserver",
      "endpoint": "https://mcp.example.com/api",
      "auth": {
        "type": "Bearer",
        "token": "..."
      },
      "timeout": 30000,
      "poolSize": 10
    }
  ],
  "defaultServer": "myserver"
}
```

---

## Daemon Management

### Unified Daemon

The `mckli` tool uses a single background "Unified Daemon" to manage all configured MCP servers. This improves performance and simplifies resource management.

### Starting the Daemon

The daemon **starts automatically** the first time you run a `tools` command (like `list` or `search`). You don't usually need to start it manually.

```bash
# Explicitly start the daemon
mckli daemon start

# Check status
mckli daemon status
```

### Checking Status

```bash
mckli daemon status
```

Output:
```
Daemon: RUNNING (PID: 12345)
Port: 5030

Managed Servers:
  myserver: Connected
  otherserver: Connected
```

### Stopping the Daemon

```bash
# Graceful shutdown (stops all managed server connections)
mckli daemon stop

# Force kill immediately
mckli daemon stop --force
```

### Understanding Daemon Logs

Logs are stored in `~/.mckli/daemons/`:
- `daemon.log` - Unified daemon standard output
- `daemon.err` - Unified daemon error output
- `daemon.pid` - Process ID file

**View logs:**
```bash
# Standard output
cat ~/.mckli/daemons/daemon.log

# Errors
cat ~/.mckli/daemons/daemon.err

# Tail live logs
tail -f ~/.mckli/daemons/daemon.log
```

### SSE Transport

The `mckli` tool supports both standard HTTP and SSE (Server-Sent Events) transports for MCP.

```bash
# Add an SSE server (default)
mckli config add streaming-server https://mcp.example.com/sse

# Force HTTP for older servers
mckli config add legacy-server https://mcp.example.com/api --transport HTTP
```

SSE features:
- **Persistent connection**: The daemon maintains a long-lived SSE stream, avoiding the overhead of repeated connections.
- **Bi-directional events**: Efficiently handles server-initiated events.
- **Auto-reconnect**: Automatically reconnects with exponential backoff if the stream is interrupted.

---

## Working with Tools

### Searching for Tools

**CRITICAL**: The `tools search` command is the most important tool for discovery across multiple MCP servers. It allows you to find which server provides a specific capability without needing to list each server individually.

**Why use Search?**
- **Token Efficiency**: Instead of an LLM listing all tools from all servers (which can consume thousands of tokens), it can search for a specific keyword.
- **Discovery**: Quickly find tools when you don't remember which server they belong to.
- **Cross-Server Search**: Results include the server name for every match.

```bash
# Search across ALL configured servers
mckli tools search "read"

# Example Output:
# myserver:read-file Read contents of a file from the filesystem...
# secondserver:read-db Read data from the database...
```

To get the output in JSON format for scripting:
```bash
mckli tools search "read" --json
```

### Listing Tools

The `tools list` command displays available tools from MCP servers. By default, it uses a **compact view** (name and first line of description) for readability.

```bash
# List all tools from a server (compact view)
mckli tools list myserver

# Show full descriptions and schemas
mckli tools list myserver --full

# Filter tools by name/description
mckli tools list myserver --filter "file"
```

Compact Output:
```
Server: myserver
  read-file             Read contents of a file from the filesystem...
  write-file            Write content to a file
  list-files            List files in a directory
  delete-file           Delete a file
  search-files          Search for files matching a pattern...
```

Full Output (`--full` or `-l`):
```
Server: myserver
Tool: read-file
Description: Read contents of a file from the filesystem
Multi-line description continues here...
Input Schema: {"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}
----------------------------------------
...
```

### Describing Tools

```bash
mckli tools describe myserver read-file
```

Output:
```
Tool: read-file
Description: Read contents of a file from the filesystem

Input Schema:
{
  "type": "object",
  "properties": {
    "path": {
      "type": "string",
      "description": "Path to the file to read"
    }
  },
  "required": ["path"]
}
```

### Calling Tools

#### With JSON Arguments
```bash
mckli tools call myserver read-file --json '{"path": "/tmp/file.txt"}'
```

#### For Tools Without Arguments
```bash
mckli tools call myserver list-users
```

#### Complex Nested Arguments
```bash
mckli tools call myserver search-files --json '{
  "directory": "/home/user",
  "pattern": "*.txt",
  "recursive": true,
  "options": {
    "maxDepth": 5,
    "followSymlinks": false
  }
}'
```

### Refreshing Tool Cache

```bash
# Refresh tools for a specific server
mckli tools refresh myserver

# Tools are automatically cached on daemon start
# Only refresh if MCP server adds/removes tools
```

---

## Advanced Usage

### Using Default Server

If you have a default server configured, you can omit the server name:

```bash
# Set default in config
# Edit ~/.mckli/servers.json and set "defaultServer": "myserver"

# Then use commands without server name
mckli tools list
mckli tools call read-file --json '{"path": "/tmp/file.txt"}'
```

### Working with Multiple Servers

```bash
# Configure multiple servers
mckli config add dev-server https://dev-mcp.example.com/api
mckli config add prod-server https://prod-mcp.example.com/api

# Each gets its own daemon
mckli daemon start dev-server
mckli daemon start prod-server

# Use tools from different servers
mckli tools list dev-server
mckli tools list prod-server
```

### Output Formatting

Tool results are returned as JSON by default:

```bash
# Default JSON output
mckli tools call myserver read-file --json '{"path": "/tmp/file.txt"}'

# Output:
# {
#   "content": "file contents here",
#   "size": 1234,
#   "encoding": "utf-8"
# }
```

For scripts, parse with `jq`:
```bash
content=$(mckli tools call myserver read-file --json '{"path": "/tmp/file.txt"}' | jq -r '.content')
echo "$content"
```

### Connection Management

Daemons automatically manage connections:

- **Connection pooling**: Up to `poolSize` concurrent connections (default: 10)
- **Idle timeout**: Connections close after 5 minutes of inactivity
- **Max lifetime**: Connections recreated after 30 minutes
- **Auto-reconnect**: Failed connections automatically retry with exponential backoff

---

## Troubleshooting

### Daemon Won't Start

**Check if port/socket is in use:**
```bash
ls -la ~/.mckli/daemons/
# Look for .sock files

# Clean up stale sockets
rm ~/.mckli/daemons/myserver.sock
mckli daemon start myserver
```

**Check logs:**
```bash
cat ~/.mckli/daemons/myserver.err
```

**Force cleanup:**
```bash
mckli daemon stop myserver --force
mckli daemon start myserver
```

### Connection Errors

**Test server connectivity:**
```bash
# Check server endpoint is reachable
curl https://mcp.example.com/api

# Verify authentication token
mckli config list
```

**Restart daemon:**
```bash
mckli daemon restart myserver
```

### Tool Not Found

**Refresh tool cache:**
```bash
mckli tools refresh myserver
```

**Check tool name:**
```bash
mckli tools list myserver
```

### Slow Tool Responses

**Check daemon status:**
```bash
mckli daemon status
```

**Check logs for errors:**
```bash
tail -f ~/.mckli/daemons/myserver.log
```

**Restart daemon to clear connection pool:**
```bash
mckli daemon restart myserver
```

### JSON Parsing Errors

**Validate JSON syntax:**
```bash
# Use a JSON validator
echo '{"path": "/tmp/file.txt"}' | jq .

# Escape special characters in shell
mckli tools call myserver read-file --json "{\"path\": \"/tmp/file.txt\"}"
```

---

## Examples

### Example 1: File Operations

```bash
# Read a file
mckli tools call myserver read-file --json '{"path": "/etc/hosts"}'

# Write a file
mckli tools call myserver write-file --json '{
  "path": "/tmp/test.txt",
  "content": "Hello, World!"
}'

# List directory
mckli tools call myserver list-files --json '{"directory": "/tmp"}'
```

### Example 2: LLM Integration

Use mckli in LLM tool definitions:

**Claude tool definition:**
```json
{
  "name": "read_file",
  "description": "Read contents of a file",
  "input_schema": {
    "type": "object",
    "properties": {
      "path": {
        "type": "string",
        "description": "File path"
      }
    },
    "required": ["path"]
  }
}
```

**Tool execution:**
```bash
# LLM generates this command
mckli tools call myserver read-file --json '{"path": "/path/to/file"}'
```

### Example 3: Scripting with mckli

```bash
#!/bin/bash
# backup-files.sh - Backup files using MCP server

SERVER="myserver"
SOURCE_DIR="/important/data"
BACKUP_DIR="/backups/$(date +%Y%m%d)"

# List files to backup
files=$(mckli tools call "$SERVER" list-files --json "{\"directory\": \"$SOURCE_DIR\"}" | jq -r '.files[]')

# Read and backup each file
for file in $files; do
  echo "Backing up: $file"
  content=$(mckli tools call "$SERVER" read-file --json "{\"path\": \"$file\"}" | jq -r '.content')

  # Write to backup location
  backup_path="$BACKUP_DIR/$(basename $file)"
  mckli tools call "$SERVER" write-file --json "{\"path\": \"$backup_path\", \"content\": \"$content\"}"
done

echo "Backup complete!"
```

### Example 4: Monitoring Daemons

```bash
#!/bin/bash
# monitor-daemons.sh - Keep daemons running

while true; do
  # Check all configured servers
  servers=$(mckli config list | grep "^  " | awk '{print $1}')

  for server in $servers; do
    if ! mckli daemon status | grep "$server: RUNNING" > /dev/null; then
      echo "$(date): Daemon $server is down, restarting..."
      mckli daemon start "$server"
    fi
  done

  sleep 60
done
```

---

## Configuration Details

Configuration is stored in `~/.mckli/servers.json`.

### Config File Format

```json
{
  "servers": [
    {
      "name": "myserver",
      "endpoint": "https://mcp.example.com/api",
      "transport": "HTTP",
      "auth": {
        "type": "Bearer",
        "token": "your-token"
      },
      "timeout": 30000,
      "poolSize": 10
    },
    {
      "name": "streaming-server",
      "endpoint": "https://mcp.example.com/sse",
      "transport": "SSE",
      "timeout": 30000,
      "poolSize": 10
    }
  ],
  "defaultServer": "myserver"
}
```

### Transport Types

**HTTP (default)**
Standard request/response communication. Best for most MCP servers with lower overhead and predictable behavior.

**SSE (Server-Sent Events)**
Streaming communication for real-time updates.
- Persistent connection with server-initiated messages.
- Automatic reconnection with exponential backoff.
- Supports real-time notifications from the server.
- Note: JVM platform only (not available on Native builds).

### Daemon State

Daemon-related files are stored in `~/.mckli/daemons/`:
- `<server-name>.pid`: Process ID files.
- `<server-name>.sock`: Unix domain sockets (or named pipes on Windows).
- `<server-name>.log`: Daemon standard output logs.
- `<server-name>.err`: Daemon error logs.

---

## Troubleshooting

### Daemon won't start

1. **Check logs**:
   ```bash
   cat ~/.mckli/daemons/<server>.log
   cat ~/.mckli/daemons/<server>.err
   ```
2. **Clean up stale processes**:
   ```bash
   mckli daemon status
   mckli daemon stop <server> --force
   ```

### Connection issues

1. **Verify server configuration**:
   ```bash
   mckli config list
   ```
2. **Test daemon connectivity**:
   ```bash
   mckli tools list <server>
   ```
3. **Restart daemon**:
   ```bash
   mckli daemon restart <server>
   ```

### SSE Transport Issues

- **Connection not establishing**: Check daemon logs for SSE connection errors and verify the server endpoint supports SSE (typically `/sse` or `/stream`).
- **Frequent reconnections**: Server may be unstable or overloaded. Check network stability and consider increasing the timeout in configuration.
- **"Max reconnection attempts exceeded"**: Server is down or unreachable. Fixing the server issue and restarting the daemon is required.
- **SSE only available on JVM**: Native builds do not support SSE transport yet. Use the JVM version (`fatJar`) for SSE support.

### Tool cache issues

If tools have changed on the server but are not showing up in `mckli`:
```bash
mckli tools refresh <server>
```

---

## Best Practices

1. **Use default server** for convenience in single-server setups
2. **Monitor daemon logs** regularly for errors
3. **Restart daemons** after configuration changes
4. **Validate JSON** before calling tools
5. **Use --filter** to quickly find tools
6. **Check tool schemas** with `describe` before calling
7. **Handle errors** in scripts (check exit codes)
8. **Keep daemons running** for best performance

---

## Next Steps

- See [Developer Guide](../dev/README.md) for architecture details
- Read [OpenSpec changes](../../openspec/changes/mcp-server-wrapper/) for implementation details
- Report issues on GitHub
