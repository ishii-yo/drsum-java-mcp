# DrSum Java MCP Server Example Configuration

## MCP Client Configuration Example

If you're using this server with an MCP client, here's an example configuration:

### For Claude Desktop or similar MCP clients

Add this to your MCP configuration file:

```json
{
  "mcpServers": {
    "drsum": {
      "command": "java",
      "args": [
        "-jar", 
        "/path/to/drsum-java-mcp-1.0.0-SNAPSHOT.jar"
      ],
      "env": {
        "LOG_LEVEL": "INFO"
      }
    }
  }
}
```

### Using with scripts

```json
{
  "mcpServers": {
    "drsum": {
      "command": "/path/to/drsum-java-mcp/scripts/start-server.sh",
      "args": [],
      "env": {
        "JAVA_HOME": "/path/to/java17"
      }
    }
  }
}
```

### Windows Configuration

```json
{
  "mcpServers": {
    "drsum": {
      "command": "C:\\path\\to\\drsum-java-mcp\\scripts\\start-server.bat",
      "args": []
    }
  }
}
```

## Environment Variables

The following environment variables can be used to configure the server:

- `LOG_LEVEL`: Set logging level (DEBUG, INFO, WARN, ERROR)
- `JAVA_OPTS`: Additional JVM options
- `MCP_SERVER_PORT`: Port for HTTP transport (if using HTTP instead of STDIO)

## Tool Usage Examples

### Basic Text Summarization

```json
{
  "method": "tools/call",
  "params": {
    "name": "summarize",
    "arguments": {
      "text": "Artificial intelligence (AI) is intelligence demonstrated by machines, in contrast to the natural intelligence displayed by humans and animals. Leading AI textbooks define the field as the study of 'intelligent agents': any device that perceives its environment and takes actions that maximize its chance of successfully achieving its goals. Colloquially, the term 'artificial intelligence' is often used to describe machines (or computers) that mimic 'cognitive' functions that humans associate with the human mind, such as 'learning' and 'problem solving'.",
      "max_sentences": 2
    }
  }
}
```

Expected response:
```json
{
  "content": [
    {
      "type": "text",
      "text": "Artificial intelligence (AI) is intelligence demonstrated by machines, in contrast to the natural intelligence displayed by humans and animals. Leading AI textbooks define the field as the study of 'intelligent agents': any device that perceives its environment and takes actions that maximize its chance of successfully achieving its goals."
    }
  ],
  "isError": false
}
```

### Summarization with Default Settings

```json
{
  "method": "tools/call",
  "params": {
    "name": "summarize",
    "arguments": {
      "text": "Your long text here..."
    }
  }
}
```

This will use the default `max_sentences` value of 3.