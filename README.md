# MCP TiggerBeetle Server

A Model Context Protocol Server providing TigerBeetle account management.

## Features

- Create TigerBeetle accounts with detailed configuration
- Manage account flags and properties
- JSON-based response format
- Integration with Claude desktop

## Backlog for Future Development

- Get Account by ID
- List Accounts by using filters
- Create transfer
- Get transfer by ID
- List transfers by using filters

## Prerequisites

- Java 17 or higher
- Maven 3.8.1 or higher
- TigerBeetle server 
- Claude desktop or any other MCP client

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/mcp-tiggerbeetle.git
   cd mcp-tiggerbeetle
   ```

2. Build the project:
   ```bash
   mvn clean package
   ```

3. The built JAR will be located in the `target` directory.

## Configuration

### Environment Variables

Set the following environment variables before running the application:

```bash
export TB_ADDRESS=127.0.0.1:3001  # TigerBeetle server address
```

## Integration with Claude Desktop

1. Open Claude Desktop and navigate to Claude / Settings / Developer and click on Edit Config
2. Open the file `claude_desktop_config.json` by using your favourite text editor
3. Add a new MCP Server configuration to the file in the `mcpServers` section as follows:
```json
{
  "mcpServers": {
    "mcp-tiggerbeetle": {
      "command": "[ABSOLUTE PATH TO]/java/current/bin/java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-jar",
        "[ABSOLUTE PATH TO]/mcp-tiggerbeetle/target/mcp-tiggerbeetle-0.0.1-SNAPSHOT.jar"
      ],
      "autoApprove": ["mcp-tiggerbeetle"]
    }
  }
}
```

## Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

Distributed under the MIT License. See `LICENSE` for more information.

## Contact

Javier Antoniucci - javier.antoniucci@gmail.com

Project Link: [https://github.com/jantoniucci/mcp-tiggerbeetle](https://github.com/jantoniucci/mcp-tiggerbeetle)
