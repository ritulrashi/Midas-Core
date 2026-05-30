-- midas_auth is created via POSTGRES_DB env var; create the rest here

CREATE DATABASE midas_transactions;
CREATE DATABASE midas_reconciliation;
CREATE DATABASE midas_ledger;
CREATE DATABASE midas_notifications;

GRANT ALL PRIVILEGES ON DATABASE midas_transactions   TO midas;
GRANT ALL PRIVILEGES ON DATABASE midas_reconciliation TO midas;
GRANT ALL PRIVILEGES ON DATABASE midas_ledger         TO midas;
GRANT ALL PRIVILEGES ON DATABASE midas_notifications  TO midas;
