package tb.agent.mcp.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.tigerbeetle.Client;
import com.tigerbeetle.CreateAccountResultBatch;

@SpringBootTest
public class TiggerBeetleToolsServiceTest {

    @Autowired
    private TiggerBeetleToolsService tiggerBeetleToolsService;

    @MockBean
    private Client tigerBeetleClient;

    @Test
    public void testCreateAccount() throws Exception {
        // Configure mock
        when(tigerBeetleClient.createAccounts(any())).thenReturn(new CreateAccountResultBatch(1));

        String result = tiggerBeetleToolsService.createAccount(
            17L, // idHigh
            17L, // idLow
            0L,  // userData128High
            0L,  // userData128Low
            0L,  // userData64
            0,   // userData32
            17,  // ledger
            17,  // code
            false, // linked
            false, // debitsMustNotExceedCredits
            false, // creditsMustNotExceedDebits
            false, // history
            false, // imported
            false, // closed
            0L     // timestamp
        );
        assertNotNull(result);
        assertTrue(result.contains("success"));
    }
}
