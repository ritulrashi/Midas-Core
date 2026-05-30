CREATE TABLE IF NOT EXISTS reconciliation_records (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      VARCHAR(255) NOT NULL UNIQUE,
    event_type          VARCHAR(50)  NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            CHAR(3)      NOT NULL,
    from_account        VARCHAR(255),
    to_account          VARCHAR(255),
    status              VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    notes               TEXT,
    event_occurred_at   TIMESTAMPTZ,
    processed_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_recon_txn_id    ON reconciliation_records (transaction_id);
CREATE INDEX idx_recon_status     ON reconciliation_records (status);
CREATE INDEX idx_recon_event_type ON reconciliation_records (event_type);
