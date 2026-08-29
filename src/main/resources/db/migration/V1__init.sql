CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    age           INTEGER,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_age CHECK (age IS NULL OR (age >= 0 AND age <= 150)),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_name_lower ON users (lower(name));

CREATE TABLE orders (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status     VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    amount     NUMERIC(12, 2) NOT NULL,
    currency   VARCHAR(3)    NOT NULL,
    comment    VARCHAR(500),
    version    BIGINT        NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_orders_amount CHECK (amount > 0),
    CONSTRAINT ck_orders_status CHECK (status IN ('NEW', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);

-- Remembers the answer already given for an Idempotency-Key.
CREATE TABLE idempotency_keys (
    idempotency_key     VARCHAR(255) PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    request_fingerprint VARCHAR(64)  NOT NULL,
    order_id            BIGINT       NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- The consumer's dedupe log, which makes at-least-once delivery safe to re-process.
CREATE TABLE processed_events (
    event_id    VARCHAR(64) PRIMARY KEY,
    event_type  VARCHAR(64) NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE webhook_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    target_url VARCHAR(500) NOT NULL,
    event_type VARCHAR(64)  NOT NULL,
    secret     VARCHAR(255) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
