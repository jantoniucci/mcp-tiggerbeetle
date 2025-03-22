package tb.agent.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.tigerbeetle.Client;
import com.tigerbeetle.UInt128;

@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider tiggerBeetleTools(TiggerBeetleToolsService tiggerBeetleToolsService) {
        return MethodToolCallbackProvider.builder().toolObjects(tiggerBeetleToolsService).build();
    }

    @Bean
    public Client tigerBeetleClient() {
        String replicaAddress = System.getenv("TB_ADDRESS");
        byte[] clusterID = UInt128.asBytes(0);
        String[] replicaAddresses = new String[] {replicaAddress == null ? "3001" : replicaAddress};
        return new Client(clusterID, replicaAddresses);
    }
}
