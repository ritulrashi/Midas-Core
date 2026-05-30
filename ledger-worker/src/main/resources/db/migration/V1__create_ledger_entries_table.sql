CREATE TABLE IF NOT EXISTS ledger_entries (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id VARCHAR(255)  NOT NULL,
    account_id     VARCHAR(255)  NOT NULL,
    entry_type     VARCHAR(10)   NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    currency       CHAR(3)       NOT NULL,
    event_type     VARCHAR(50),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_txn_id     ON ledger_entries (transaction_id);
CREATE INDEX idx_ledger_account_id  ON ledger_entries (account_id);
CREATE INDEX idx_ledger_created_at  ON ledger_entries (created_at DESC);
