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

   # Build self-contained JAR
   ./gradlew fatJar

   # Or build native binary (faster startup)
   ./gradlew linkReleaseExecutableNative
   ```

   **Option B: Download release**
   ```bash
   # Download from releases page
   # Extract and verify
   ```

3. **Install the binary**
   ```bash
   # For JAR
   sudo cp build/libs/mckli-all.jar /usr/local/bin/
   echo 'alias mckli="java --enable-native-access=ALL-UNNAMED -jar /usr/local/bin/mckli-all.jar"' >> ~/.bashrc
   source ~/.bashrc

   # For native binary
   sudo cp build/bin/native/releaseExecutable/mckli.kexe /usr/local/bin/mckli
   sudo chmod +x /usr/local/bin/mckli
   ```

4. **Verify installation**
   ```bash
   mckli --help
   ```

### First Steps

1. **Add your first MCP server**
   ```bash
   mckli config add myserver https://mcp.example.com/api
   ```

2. **Start the daemon**
   ```bash
   mckli daemon start myserver
   ```

3. **List available tools**
   ```bash
   mckli tools list myserver
   ```

---

## Configuration

### Adding Servers

#### Basic Configuration
```bash
mckli config add myserver https://mcp.example.com/api
```

#### With Bearer Token Authentication
```bash
mckli config add myserver https://mcp.example.com/api \
  --token "your-bearer-token-here"
```

#### With Basic Authentication
```bash
mckli config add myserver https://mcp.example.com/api \
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

This displays the config file path: `~/.config/mckli/servers.json`

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

### Starting Daemons

```bash
# Start a specific daemon
mckli daemon start myserver

# Daemons auto-start on first tool use
mckli tools list myserver  # Starts daemon automatically if not running
```

### Checking Status

```bash
mckli daemon status
```

Output:
```
Daemon status:
  myserver: RUNNING (PID: 12345)
    Socket: /home/user/.config/mckli/daemons/myserver.sock
  otherserver: STOPPED
```

### Stopping Daemons

```bash
# Graceful shutdown (waits up to 10 seconds)
mckli daemon stop myserver

# Force kill immediately
mckli daemon stop myserver --force
```

### Restarting Daemons

```bash
mckli daemon restart myserver
```

### Understanding Daemon Logs

Logs are stored in `~/.config/mckli/daemons/`:
- `<server>.log` - Standard output
- `<server>.err` - Error output
- `<server>.pid` - Process ID file
- `<server>.sock` - Unix domain socket

**View logs:**
```bash
# Standard output
cat ~/.config/mckli/daemons/myserver.log

# Errors
cat ~/.config/mckli/daemons/myserver.err

# Tail live logs
tail -f ~/.config/mckli/daemons/myserver.log
```

---

## Working with Tools

### Listing Tools

```bash
# List all tools from a server
mckli tools list myserver

# Filter tools by name/description
mckli tools list myserver --filter "file"
```

Output:
```
Available tools (5):
  read-file
    Read contents of a file from the filesystem
  write-file
    Write content to a file
  list-files
    List files in a directory
  delete-file
    Delete a file
  search-files
    Search for files matching a pattern
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
# Edit ~/.config/mckli/servers.json and set "defaultServer": "myserver"

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
ls -la ~/.config/mckli/daemons/
# Look for .sock files

# Clean up stale sockets
rm ~/.config/mckli/daemons/myserver.sock
mckli daemon start myserver
```

**Check logs:**
```bash
cat ~/.config/mckli/daemons/myserver.err
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
tail -f ~/.config/mckli/daemons/myserver.log
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
