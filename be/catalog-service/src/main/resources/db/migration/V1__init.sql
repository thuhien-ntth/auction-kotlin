CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    start_time   TIMESTAMPTZ  NOT NULL,
    end_time     TIMESTAMPTZ  NOT NULL,
    created_by   UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (end_time > start_time)
);

CREATE TABLE products (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id        UUID         NOT NULL,
    event_id         UUID         NULL REFERENCES events(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT         NOT NULL DEFAULT '',
    category         VARCHAR(100) NOT NULL DEFAULT 'OTHER',
    start_price      NUMERIC(14,2) NOT NULL CHECK (start_price > 0),
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    rejection_reason VARCHAR(500) NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_seller ON products(seller_id);
CREATE INDEX idx_products_event ON products(event_id);
CREATE INDEX idx_products_status ON products(status);
-- phục vụ USTPS/USEDS Search & Filter Product (tìm theo tên + trạng thái)
CREATE INDEX idx_products_title_trgm ON products USING gin (title gin_trgm_ops);
