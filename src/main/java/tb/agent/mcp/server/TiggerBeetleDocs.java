package tb.agent.mcp.server;

public class TiggerBeetleDocs {

    public static final String TOOL_CREATE_ACCOUNT_DESCRIPTION = """
        The MCP tool to create an account in TigerBeetle enables clients to define and register a new immutable `Account` record within the system. Once created, the account's structure cannot be modified, though balance-related fields (such as debits and credits) will automatically update as transfers are processed. The tool returns a JSON object containing the result of the operation with a `success` flag and a `value` message for error tracking or confirmation.

        Each TigerBeetle account contains several fields designed for consistency and auditability. These include a unique `id`, balance indicators (`debits_pending`, `debits_posted`, `credits_pending`, `credits_posted`), and optional metadata fields (`user_data_128`, `user_data_64`, `user_data_32`) for client-specific use cases. Accounts also include a `ledger` identifier for grouping transactable accounts and a `code` field for categorizing the account type (e.g., asset types). Notably, all balance-related fields must be initialized to zero upon creation, and accounts cannot be deleted.

        The system enforces strong guarantees, including global uniqueness of account IDs and internal consistency between posted and pending debit and credit totals. These properties ensure traceability, prevent inconsistencies, and support robust financial reconciliation. Additionally, the `timestamp` field reflects the precise moment of account creation and plays a crucial role when importing historical data via the `imported` flag.

        The tool also supports a `flags` bitfield that enables additional behaviors. For instance, accounts can be linked to succeed or fail as a batch (`linked`), enforce balance constraints (`debits_must_not_exceed_credits` or vice versa), retain transfer history (`history`), import with user-defined timestamps (`imported`), or be marked as closed to prevent further transactions (`closed`). Some flags are mutually exclusive, and careful use is necessary to maintain integrity during advanced operations such as historical imports or account lifecycle management.

        **Flags Summary**:
        - `linked`: Groups multiple account creations into a single atomic operation.
        - `debits_must_not_exceed_credits` / `credits_must_not_exceed_debits`: Enforce balance direction constraints.
        - `history`: Enables storing a full balance history at each transfer.
        - `imported`: Allows creating historical accounts with custom timestamps under strict rules.
        - `closed`: Prevents any new transactions, except voiding pending two-phase transfers.

        """;
    public static final String TOOL_TRANSFER_DESCRIPTION = """
        The **Transfer Tool** in TigerBeetle enables the creation of financial transfers between two accounts on the same ledger. A transfer is a fundamental and immutable operation that debits one account and credits another by a specified amount. Upon execution, the tool returns a JSON response indicating the success of the operation and providing a value field with either a result or an error message. Once created, a transfer cannot be modified or deleted — any correction must be made by issuing a new transfer that reverses the original one.

        TigerBeetle supports two modes of transfers: **Single-Phase** (executed immediately) and **Two-Phase** (involving pending, post-pending, or void-pending states). Two-phase transfers allow for more control by enabling reservation of funds and conditional finalization. Each mode has specific required fields, including account IDs, amount, ledger, code, and optionally, user-defined metadata. The system ensures strict immutability, uniqueness, and deterministic timeouts, based on the cluster’s internal clock.

        To configure a transfer, various fields are available: `id`, `debit_account_id`, `credit_account_id`, `amount`, `pending_id`, metadata fields (`user_data_*`), `timeout`, `ledger`, and `code`. Constraints are rigorously enforced, such as uniqueness of IDs, timestamp validity, and matching ledgers. Transfers involving historical data can be imported using the `imported` flag, which requires unique, user-defined timestamps. These imported events must be submitted in dedicated batches and cannot include timeouts.

        The tool provides robust consistency and safety mechanisms through immutable data, strong constraints, and the use of linking for transactional atomicity across multiple transfers. This makes it well-suited for complex financial workflows such as currency exchanges, multi-party settlements, or ledger reconciliations. TigerBeetle’s transfer model is designed to ensure auditability and correctness, even under high concurrency and failure conditions.

        ### Flags Overview

        Flags in the transfer structure specify behavior such as:
        - `linked`: Binds transfers to succeed/fail as a group.
        - `pending`, `post_pending_transfer`, `void_pending_transfer`: Enable two-phase operations for conditional execution.
        - `balancing_debit`, `balancing_credit`: Allow transferring *up to* the specified amount based on account limits.
        - `closing_debit`, `closing_credit`: Mark accounts as closed upon successful completion of a pending transfer.
        - `imported`: Indicates historical data import with manual timestamps, disabling timeout and requiring strict sequencing.

        Each flag alters the transfer’s semantics, allowing for advanced workflows while ensuring consistency and adherence to system rules.

    """;

    public static final String TOOL_GET_ACCOUNT_BALANCES_DESCRIPTION = """
        The **Get Account Balances** tool provides clients with real-time and accurate balance information for accounts registered in **TigerBeetle**, a high-performance financial ledger. By specifying the necessary parameters, clients can retrieve account balances in a structured JSON format. Each response includes key financial metrics such as pending and posted credits, as well as pending and posted debits, providing a comprehensive view of the account's financial state.

        Each balance returned is encapsulated in an `AccountBalance` object, representing the financial snapshot of an account at a specific point in time. This is especially useful for clients needing historical data for auditing or analysis, as only accounts marked with the `history` flag retain historical balance records. This allows for granular tracking of account changes over time, driven by transfers and transactions recorded in the system.

        The core fields in an `AccountBalance` include a `timestamp` in nanoseconds (indicating when the balance was updated), and four key monetary values: `debits_pending`, `debits_posted`, `credits_pending`, and `credits_posted`. These values are expressed as 128-bit unsigned integers, ensuring precision and scalability for high-volume financial applications. Additionally, a `reserved` field of 56 bytes is included for future-proofing and must currently be set to zero.

        All balance updates are directly tied to transfer operations, with the `timestamp` reflecting the moment of the most recent transaction affecting the account. The balance values represent the account’s status *after* the transfer was applied, ensuring a consistent and traceable financial history. This structure guarantees data integrity and accuracy, critical for financial service providers operating at scale.

        **Filters** can be applied when invoking the tool to refine the balance queries. These may include account identifiers, specific timestamps, or the presence of historical data flags. By leveraging filters, clients can narrow down the scope of balance retrievals, making the tool highly efficient and customizable to meet the operational needs of various financial systems.        
    """;
}
