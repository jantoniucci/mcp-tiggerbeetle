package tb.agent.mcp.server;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tigerbeetle.AccountBalanceBatch;
import com.tigerbeetle.AccountBatch;
import com.tigerbeetle.AccountFilter;
import com.tigerbeetle.AccountFlags;
import com.tigerbeetle.CreateAccountResultBatch;
import com.tigerbeetle.CreateTransferResultBatch;
import com.tigerbeetle.TransferBatch;
import com.tigerbeetle.Client;

@Service
public class TiggerBeetleToolsService {

    private static final String JSON_ARRAY_START = "[";
    private static final String JSON_ARRAY_END = "]";

    private final Client client;

    @Autowired
    public TiggerBeetleToolsService(Client client) {
        this.client = client;
    }

    @Tool(description = TiggerBeetleDocs.TOOL_CREATE_ACCOUNT_DESCRIPTION)
    public String createAccount(
        @ToolParam(required = true, description = "This is the high part of the unique account identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long idHigh,
        @ToolParam(required = true, description = "This is the low part of the unique account identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long idLow,
        @ToolParam(description = "This is the high part of the optional 128‑bit secondary identifier as a long; use 0 if the user does not assigned a value.") Long userData128High,
        @ToolParam(description = "This is the low part of the optional 128‑bit secondary identifier as a long; use 0 if the user does not assigned a value.") Long userData128Low,
        @ToolParam(description = "This is the optional 64‑bit secondary identifier; use 0 if the user does not assigned a value.") Long userData64,
        @ToolParam(description = "This is the optional 32‑bit secondary identifier; use 0 if the user does not assigned a value.") Integer userData32,
        @ToolParam(required = true, description = "Ledger identifier (32‑bit unsigned) grouping accounts that can transact with each other; must be non‑zero.") Integer ledger,
        @ToolParam(required = true, description = "User‑defined account category code (16‑bit unsigned); must be non‑zero.") Integer code,
        @ToolParam(description = "If true, links this account creation to the next in the same batch so they succeed or fail together; use false if the user does not assigned a value.") Boolean linked,
        @ToolParam(description = "If true, rejects any transfer that would cause the account’s debits (pending + posted) to exceed its posted credits; use false if the user does not assigned a value.") Boolean debitsMustNotExceedCredits,
        @ToolParam(description = "If true, rejects any transfer that would cause the account’s credits (pending + posted) to exceed its posted debits; use false if the user does not assigned a value.") Boolean creditsMustNotExceedDebits,
        @ToolParam(description = "If true, retains the history of balance changes, enabling the get_account_balances operation for this account; use false if the user does not assigned a value.") Boolean history,
        @ToolParam(description = "If true, allows importing a historical account with a user‑defined timestamp instead of using the cluster clock; use false if the user does not assigned a value.") Boolean imported,
        @ToolParam(description = "If true, prevents further transfers on the account (except voiding pending two‑phase transfers); use false if the user does not assigned a value.") Boolean closed,
        @ToolParam(description = "Creation timestamp in nanoseconds since the UNIX epoch; must be 0 if the imported flag is not set.") Long timestamp
        ) {
        AccountBatch accounts = new AccountBatch(1);
        accounts.add();
        accounts.setId(idHigh != null ? idHigh : 0, idLow != null ? idLow : 0);
        accounts.setUserData128(userData128High != null ? userData128High : 0, userData128Low != null ? userData128Low : 0);
        accounts.setUserData64(userData64 != null ? userData64 : 0);
        accounts.setUserData32(userData32 != null ? userData32 : 0);
        accounts.setLedger(ledger != null ? ledger : 0);
        accounts.setCode(code != null ? code : 0);
        accounts.setFlags(calcAccountFlags(linked != null && linked, debitsMustNotExceedCredits != null && debitsMustNotExceedCredits, creditsMustNotExceedDebits != null && creditsMustNotExceedDebits, history != null && history, imported != null && imported, closed != null && closed));
        accounts.setTimestamp(timestamp != null ? timestamp : 0);
    
        CreateAccountResultBatch accountErrors = client.createAccounts(accounts);
        String result = JSON_ARRAY_START;
        while (accountErrors.next()) {
            result = result.equals(JSON_ARRAY_START)? "": result + ",";
            switch (accountErrors.getResult()) {
                case Exists:
                    result += "{\"success\": false, \"value\": \"Batch account at " + accountErrors.getIndex() + " already exists.\"}";
                    break;
        
                default:
                    result += "{\"success\": false, \"value\": \"Batch account at " + accountErrors.getIndex() + " failed to create " + accountErrors.getResult() + ".\"}";
                    break;
            }
        }
        result = result.equals(JSON_ARRAY_START)? "{\"success\": true, \"value\": \"Account created successfully\"}": result + JSON_ARRAY_END;
        return result;
    }

    /**
     * Computes a bitmask of AccountFlags by OR‑ing together each flag enabled by the given boolean parameters.
     *
     * @param linked                        If true, links this account creation to the next in the same batch so they succeed or fail together.
     * @param debitsMustNotExceedCredits    If true, rejects any transfer that would cause the account’s debits (pending + posted) to exceed its posted credits.
     * @param creditsMustNotExceedDebits    If true, rejects any transfer that would cause the account’s credits (pending + posted) to exceed its posted debits.
     * @param history                       If true, retains the history of balance changes, enabling the get_account_balances operation for this account.
     * @param imported                      If true, allows importing a historical account with a user‑defined timestamp instead of using the cluster clock.
     * @param closed                        If true, prevents further transfers on the account (except voiding pending two‑phase transfers).
     * @return                               An integer bitmask representing the combined AccountFlags.
     */
    public static int calcAccountFlags(
            Boolean linked,
            Boolean debitsMustNotExceedCredits,
            Boolean creditsMustNotExceedDebits,
            Boolean history,
            Boolean imported,
            Boolean closed
    ) {
        int flags = AccountFlags.NONE;
        if (linked) flags |= AccountFlags.LINKED;
        if (debitsMustNotExceedCredits) flags |= AccountFlags.DEBITS_MUST_NOT_EXCEED_CREDITS;
        if (creditsMustNotExceedDebits) flags |= AccountFlags.CREDITS_MUST_NOT_EXCEED_DEBITS;
        if (history) flags |= AccountFlags.HISTORY;
        if (imported) flags |= AccountFlags.IMPORTED;
        if (closed) flags |= AccountFlags.CLOSED;
        return flags;
    }

    @Tool(description = TiggerBeetleDocs.TOOL_TRANSFER_DESCRIPTION)
    public String createTransfer(
        @ToolParam(required = true, description = "This is the highest significant part of the unique transfer identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long idHigh,
        @ToolParam(required = true, description = "This is the lowest significant part of the unique transfer identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long idLow,
        @ToolParam(required = true, description = "This is the highest significant part of the debit account identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long debitAccountIdHigh,
        @ToolParam(required = true, description = "This is the lowest significant part of the debit account identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long debitAccountIdLow,
        @ToolParam(required = true, description = "This is the highest significant part of the credit account identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long creditAccountIdHigh,
        @ToolParam(required = true, description = "This is the lowest significant part of the credit account identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long creditAccountIdLow,
        @ToolParam(required = true, description = "This is the highest significant part of the ammount identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long amountHigh,
        @ToolParam(required = true, description = "This is the lowest significant part of the ammount identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long amountLow,
        @ToolParam(description = "an optional 128‑bit secondary identifier as a long; use 0 if the user does not assigned a value.") Long userData128High,
        @ToolParam(description = "an optional 128‑bit secondary identifier as a long; use 0 if the user does not assigned a value.") Long userData128Low,
        @ToolParam(description = "Optional 64‑bit secondary identifier; use 0 if the user does not assigned a value.") Long userData64,
        @ToolParam(description = "Optional 32‑bit secondary identifier; use 0 if the user does not assigned a value.") Integer userData32,
        @ToolParam(required = true, description = "Ledger identifier (32‑bit unsigned) grouping accounts that can transact with each other; must be non‑zero.") Integer ledger,
        @ToolParam(required = true, description = "User‑defined account category code (16‑bit unsigned); must be non‑zero.") Integer code,
        @ToolParam(description = "an optional timeout; use 0 if the user does not assigned a value.") Integer timeout,

        @ToolParam(description = "If true, links this transfer with the next in the batch to succeed or fail together.") Boolean linked,
        @ToolParam(description = "If true, marks this as a pending transfer reserving the funds until later posting or voiding.") Boolean pending,
        @ToolParam(description = "If true, posts a previously pending transfer to finalize it.") Boolean postPendingTransfer,
        @ToolParam(description = "If true, voids a previously pending transfer to cancel it.") Boolean voidPendingTransfer,
        @ToolParam(description = "If true, debits up to the specified amount or less to avoid exceeding account credit limits.") Boolean balancingDebit,
        @ToolParam(description = "If true, credits up to the specified amount or less to avoid exceeding account debit limits.") Boolean balancingCredit,
        @ToolParam(description = "If true, marks the debit account as closed upon successful transfer; requires a pending transfer.") Boolean closingDebit,
        @ToolParam(description = "If true, marks the credit account as closed upon successful transfer; requires a pending transfer.") Boolean closingCredit,
        @ToolParam(description = "If true, allows importing a historical transfer with a custom timestamp; must be used in isolated batches.") Boolean imported
        ) {
            TransferBatch transfers = new TransferBatch(1);

            transfers.add();
            transfers.setId(idLow, idHigh);

            transfers.setDebitAccountId(debitAccountIdLow, debitAccountIdHigh);
            transfers.setCreditAccountId(creditAccountIdLow, creditAccountIdHigh);
            transfers.setAmount(amountLow, amountHigh);
            transfers.setUserData128(userData128Low, userData128High);
            transfers.setUserData64(userData64);
            transfers.setUserData32(userData32);
            transfers.setTimeout(timeout);
            transfers.setLedger(ledger);
            transfers.setCode(code);
            transfers.setFlags(calcTransferFlags(linked, pending, postPendingTransfer, voidPendingTransfer, balancingDebit, balancingCredit, closingDebit, closingCredit, imported));
            
            CreateTransferResultBatch transferErrors = client.createTransfers(transfers);

            String result = JSON_ARRAY_START;
        while (transferErrors.next()) {
            result = result.equals(JSON_ARRAY_START)? "": result + ",";
            switch (transferErrors.getResult()) {
                case Exists:
                    result += "{\"success\": false, \"value\": \"Batch transfer at " + transferErrors.getIndex() + " already exists.\"}";
                    break;
        
                default:
                    result += "{\"success\": false, \"value\": \"Batch transfer at " + transferErrors.getIndex() + " failed to create " + transferErrors.getResult() + ".\"}";
                    break;
            }
        }
        result = result.equals(JSON_ARRAY_START)? "{\"success\": true, \"value\": \"Transfer created successfully\"}": result + JSON_ARRAY_END;
        return result;
    }

    /**
     * Computes a bitmask of transfer flags by OR‑ing together each flag enabled by the given boolean parameters.
     *
     * @param linked                        If true, links this transfer with the next in the batch to succeed or fail together.
     * @param pending                       If true, marks this as a pending transfer reserving the funds until later posting or voiding.
     * @param postPendingTransfer           If true, posts a previously pending transfer to finalize it.
     * @param voidPendingTransfer           If true, voids a previously pending transfer to cancel it.
     * @param balancingDebit                If true, debits up to the specified amount or less to avoid exceeding account credit limits.
     * @param balancingCredit               If true, credits up to the specified amount or less to avoid exceeding account debit limits.
     * @param closingDebit                  If true, marks the debit account as closed upon successful transfer; requires a pending transfer.
     * @param closingCredit                 If true, marks the credit account as closed upon successful transfer; requires a pending transfer.
     * @param imported                      If true, allows importing a historical transfer with a custom timestamp; must be used in isolated batches.
     * @return                               An integer bitmask representing the combined transfer flags.
     */
    public static int calcTransferFlags(
            Boolean linked,
            Boolean pending,
            Boolean postPendingTransfer,
            Boolean voidPendingTransfer,
            Boolean balancingDebit,
            Boolean balancingCredit,
            Boolean closingDebit,
            Boolean closingCredit,
            Boolean imported
    ) {
        int flags = 0;
        if (linked) flags |= 1; // LINKED
        if (pending) flags |= 2; // PENDING
        if (postPendingTransfer) flags |= 4; // POST_PENDING_TRANSFER
        if (voidPendingTransfer) flags |= 8; // VOID_PENDING_TRANSFER
        if (balancingDebit) flags |= 16; // BALANCING_DEBIT
        if (balancingCredit) flags |= 32; // BALANCING_CREDIT
        if (closingDebit) flags |= 64; // CLOSING_DEBIT
        if (closingCredit) flags |= 128; // CLOSING_CREDIT
        if (imported) flags |= 256; // IMPORTED
        return flags;
    }

    @Tool(description = TiggerBeetleDocs.TOOL_GET_ACCOUNT_BALANCES_DESCRIPTION)
    public String getAccountBalances(
        @ToolParam(required = true, description = "This is the high part of the unique account identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long idHigh,
        @ToolParam(required = true, description = "This is the low part of the unique account identifier as a long. Must not be 0 or 2^128‑1, and must be unique within the cluster.") Long idLow
    ) {
        AccountFilter filter = new AccountFilter();
        filter.setAccountId(idLow, idHigh);
        filter.setUserData128(0); // No filter by UserData.
        filter.setUserData64(0);// No filter by UserData.
        filter.setUserData32(0);// No filter by UserData.
        filter.setCode(0); // No filter by Code.
        filter.setTimestampMin(0); // No filter by Timestamp.
        filter.setTimestampMax(0); // No filter by Timestamp.
        filter.setLimit(10); // Limit to ten balances at most.
        filter.setDebits(true); // Include transfer from the debit side.
        filter.setCredits(true); // Include transfer from the credit side.
        filter.setReversed(true); // Sort by timestamp in reverse-chronological order.
        
        AccountBalanceBatch account_balances = client.getAccountBalances(filter);
        return String.format(
            "{\"credits_pending\":%d,\"credits_posted\":%d,\"debits_pending\":%d,\"debits_posted\":%d}",
            account_balances.getCreditsPending(),
            account_balances.getCreditsPosted(),
            account_balances.getDebitsPending(),
            account_balances.getDebitsPosted()
        );
    }

}
