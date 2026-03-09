CREATE TABLE holdings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    avg_buy_price DECIMAL(19, 2) NOT NULL,
    evaluated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_holdings PRIMARY KEY (id),
    CONSTRAINT uk_holdings_user_stock UNIQUE (user_id, stock_id),
    CONSTRAINT fk_holdings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_holdings_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE INDEX idx_holdings_user_id ON holdings (user_id);

CREATE TABLE trade_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    trade_type VARCHAR(20) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    quantity BIGINT NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_trade_orders PRIMARY KEY (id),
    CONSTRAINT fk_trade_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_trade_orders_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE INDEX idx_trade_orders_user_created_at ON trade_orders (user_id, created_at, id);
