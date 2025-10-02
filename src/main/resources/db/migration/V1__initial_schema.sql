CREATE TABLE accounts
(
    id             VARCHAR                     NOT NULL,
    user_id        UUID                        NOT NULL,
    account_number VARCHAR(34)                 NOT NULL,
    balance        NUMERIC(19, 4)              NOT NULL DEFAULT 0 CHECK (balance >= 0),
    currency       VARCHAR(3)                  NOT NULL,
    status         VARCHAR(20)                 NOT NULL,
    version        BIGINT,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE cards
(
    id            UUID                        NOT NULL,
    card_type     VARCHAR(31)                 NOT NULL,
    account_id    UUID                        NOT NULL,
    card_number   VARCHAR(16)                 NOT NULL,
    expiry_date   date                        NOT NULL,
    ccv_encrypted VARCHAR(100)                NOT NULL,
    status        VARCHAR(20)                 NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_cards PRIMARY KEY (id)
);

CREATE TABLE transactions
(
    id                     VARCHAR                     NOT NULL,
    account_id             UUID                        NOT NULL,
    card_id                UUID                        NOT NULL,
    transaction_type       VARCHAR(20)                 NOT NULL,
    amount                 DECIMAL(19, 4)              NOT NULL,
    fee                    DECIMAL(19, 4)              NOT NULL,
    related_account_id     UUID,
    related_transaction_id UUID,
    balance_after          DECIMAL(19, 4)              NOT NULL,
    description            VARCHAR(255),
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);

CREATE TABLE users
(
    id         UUID PRIMARY KEY                     DEFAULT gen_random_uuid(),
    email      VARCHAR(255)                NOT NULL UNIQUE,
    full_name  VARCHAR(255)                NOT NULL,
    bsn_id     VARCHAR(9)                  NOT NULL UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_bsn_id CHECK (bsn_id ~ '^[0-9]{9}$' OR bsn_id IS NULL),
    CONSTRAINT chk_email_format CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_account_number UNIQUE (account_number);

ALTER TABLE cards
    ADD CONSTRAINT uc_cards_account UNIQUE (account_id);

ALTER TABLE cards
    ADD CONSTRAINT uc_cards_card_number UNIQUE (card_number);

ALTER TABLE users
    ADD CONSTRAINT uc_users_bsn UNIQUE (bsn_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

CREATE INDEX idx_account_created ON transactions (account_id, created_at);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_bsn ON users (bsn_id) WHERE bsn_id IS NOT NULL;

ALTER TABLE accounts
    ADD CONSTRAINT FK_ACCOUNTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE cards
    ADD CONSTRAINT FK_CARDS_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_CARD FOREIGN KEY (card_id) REFERENCES cards (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_RELATED_ACCOUNT FOREIGN KEY (related_account_id) REFERENCES accounts (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_RELATED_TRANSACTION FOREIGN KEY (related_transaction_id) REFERENCES transactions (id);