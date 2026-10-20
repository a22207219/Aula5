CREATE TABLE orders (id UUID PRIMARY KEY, product_id UUID NOT NULL, quantity INTEGER NOT NULL CHECK (quantity > 0), status VARCHAR(24) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL);
CREATE INDEX ix_orders_product_id ON orders(product_id);
