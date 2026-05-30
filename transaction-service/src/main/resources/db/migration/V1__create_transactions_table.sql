CREATE TABLE IF NOT EXISTS transactions (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    type           VARCHAR(20)  NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    currency       CHAR(3)      NOT NULL,
    from_account   VARCHAR(255) NOT NULL,
    to_account     VARCHAR(255) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    initiated_by   VARCHAR(255),
    failure_reason TEXT,
    description    VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_txn_from_account ON transactions (from_account);
CREATE INDEX idx_txn_to_account   ON transactions (to_account);
CREATE INDEX idx_txn_status        ON transactions (status);
CREATE INDEX idx_txn_created_at    ON transactions (created_at DESC);
CREATE INDEX idx_txn_initiated_by  ON transactions (initiated_by);
