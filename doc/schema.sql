-- =============================================================================
--  ORDER MANAGEMENT SYSTEM — PRODUCTION SCHEMA
--  Generated from JPA Entities (2026-02-20)
--  Schema: ordermgmt
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ordermgmt;
SET search_path TO ordermgmt;


/* =====================================================
   SECTION 1: BASE TABLES (Create Order respects FK deps)
===================================================== */

-- 1.1  Reference / Lookup Tables (no FK dependencies)
-- ---------------------------------------------------

CREATE TABLE user_role (
    roleid          INTEGER         PRIMARY KEY,
    rolename        VARCHAR(50)     NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE order_status_lookup (
    statusid        INTEGER         PRIMARY KEY,
    statusname      VARCHAR(50)     NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);


-- 1.2  Core Identity
-- ---------------------------------------------------

CREATE TABLE app_user (
    userid          VARCHAR(36)     PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL,
    passwordhash    VARCHAR(255)    NOT NULL,
    roleid          INTEGER         NOT NULL,
    isactive        BOOLEAN         DEFAULT TRUE,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE customer (
    customerid      VARCHAR(36)     PRIMARY KEY,
    userid          VARCHAR(36)     NOT NULL,
    firstname       VARCHAR(100),
    lastname        VARCHAR(100),
    contactno       VARCHAR(20),
    address         VARCHAR(255),
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE refresh_token (
    tokenid         VARCHAR(36)     PRIMARY KEY,
    userid          VARCHAR(36)     NOT NULL,
    token           VARCHAR(512)    NOT NULL,
    expirydate      TIMESTAMP       NOT NULL,
    revoked         BOOLEAN         DEFAULT FALSE,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);


-- 1.3  Inventory & Pricing
-- ---------------------------------------------------

CREATE TABLE inventory_item (
    itemid          VARCHAR(50)     PRIMARY KEY,
    itemname        VARCHAR(100),
    availablestock  INTEGER         NOT NULL DEFAULT 0,
    reservedstock   INTEGER         NOT NULL DEFAULT 0,
    version         BIGINT          DEFAULT 0,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE pricing_catalog (
    itemid          VARCHAR(50)     PRIMARY KEY,
    unitprice       DECIMAL(19,4)   NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

-- IMMUTABLE AUDIT TRAIL — INSERT ONLY, NO UPDATE / DELETE
CREATE TABLE pricing_history (
    historyid       VARCHAR(36)     PRIMARY KEY,
    itemid          VARCHAR(50)     NOT NULL,
    oldprice        DECIMAL(19,4),
    newprice        DECIMAL(19,4)   NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);


-- 1.4  Orders
-- ---------------------------------------------------

CREATE TABLE orders (
    orderid         VARCHAR(36)     PRIMARY KEY,
    customerid      VARCHAR(36)     NOT NULL,
    statusid        INTEGER         NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE order_item (
    orderid         VARCHAR(36)     NOT NULL,
    itemid          VARCHAR(50)     NOT NULL,
    quantity        INTEGER         NOT NULL,
    unitprice       DECIMAL(19,4)   NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255),
    PRIMARY KEY (orderid, itemid)
);


-- 1.5  System Logging
-- ---------------------------------------------------

CREATE TABLE email_log (
    id              BIGSERIAL       PRIMARY KEY,
    recipient       VARCHAR(255)    NOT NULL,
    subject         VARCHAR(255),
    status          VARCHAR(255)    NOT NULL,
    createdby       VARCHAR(255),
    sentat          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    errormessage    VARCHAR(1000)
);


/* =====================================================
   SECTION 2: UNIQUENESS CONSTRAINTS
===================================================== */

ALTER TABLE app_user
    ADD CONSTRAINT uq_app_user_email UNIQUE (email);

ALTER TABLE customer
    ADD CONSTRAINT uq_customer_contactno UNIQUE (contactno);

ALTER TABLE refresh_token
    ADD CONSTRAINT uq_refresh_token_token UNIQUE (token);

ALTER TABLE user_role
    ADD CONSTRAINT uq_user_role_rolename UNIQUE (rolename);

ALTER TABLE order_status_lookup
    ADD CONSTRAINT uq_order_status_statusname UNIQUE (statusname);


/* =====================================================
   SECTION 3: CHECK CONSTRAINTS (BUSINESS RULES)
===================================================== */

-- Inventory: stock counts must be non-negative
-- NOTE: Do NOT add CHECK (availablestock >= reservedstock).
--       Business logic subtracts available stock at reservation time.
--       Overselling prevention is handled via PESSIMISTIC_WRITE locking
--       and @Transactional atomic update logic.
ALTER TABLE inventory_item
    ADD CONSTRAINT chk_inventory_available_non_negative
    CHECK (availablestock >= 0);

ALTER TABLE inventory_item
    ADD CONSTRAINT chk_inventory_reserved_non_negative
    CHECK (reservedstock >= 0);

-- OrderItem: quantity must be positive, price non-negative
ALTER TABLE order_item
    ADD CONSTRAINT chk_order_item_quantity_positive
    CHECK (quantity > 0);

ALTER TABLE order_item
    ADD CONSTRAINT chk_order_item_price_non_negative
    CHECK (unitprice >= 0);

-- PricingCatalog: price non-negative
ALTER TABLE pricing_catalog
    ADD CONSTRAINT chk_pricing_catalog_price_non_negative
    CHECK (unitprice >= 0);

-- PricingHistory: new price non-negative (immutable audit)
ALTER TABLE pricing_history
    ADD CONSTRAINT chk_pricing_history_newprice_non_negative
    CHECK (newprice >= 0);


/* =====================================================
   SECTION 4: FOREIGN KEY CONSTRAINTS
   All use ON UPDATE CASCADE ON DELETE CASCADE
   EXCEPT pricing_history (immutable — RESTRICT)
===================================================== */

-- AppUser → UserRole
ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_role
    FOREIGN KEY (roleid)
    REFERENCES user_role (roleid)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- Customer → AppUser
ALTER TABLE customer
    ADD CONSTRAINT fk_customer_user
    FOREIGN KEY (userid)
    REFERENCES app_user (userid)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- RefreshToken → AppUser
ALTER TABLE refresh_token
    ADD CONSTRAINT fk_refresh_token_user
    FOREIGN KEY (userid)
    REFERENCES app_user (userid)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- PricingCatalog → InventoryItem (shared PK)
ALTER TABLE pricing_catalog
    ADD CONSTRAINT fk_pricing_catalog_item
    FOREIGN KEY (itemid)
    REFERENCES inventory_item (itemid)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- PricingHistory → InventoryItem (IMMUTABLE: RESTRICT delete/update)
ALTER TABLE pricing_history
    ADD CONSTRAINT fk_pricing_history_item
    FOREIGN KEY (itemid)
    REFERENCES inventory_item (itemid)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

-- Orders → Customer
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customerid)
    REFERENCES customer (customerid)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- Orders → OrderStatusLookup
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_status
    FOREIGN KEY (statusid)
    REFERENCES order_status_lookup (statusid)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- OrderItem → Orders
ALTER TABLE order_item
    ADD CONSTRAINT fk_order_item_order
    FOREIGN KEY (orderid)
    REFERENCES orders (orderid)
    ON UPDATE CASCADE ON DELETE CASCADE;

-- OrderItem → InventoryItem
ALTER TABLE order_item
    ADD CONSTRAINT fk_order_item_inventory
    FOREIGN KEY (itemid)
    REFERENCES inventory_item (itemid)
    ON UPDATE CASCADE;


/* =====================================================
   SECTION 5: IMMUTABILITY RULES FOR pricing_history
   Prevent UPDATE and DELETE via trigger (PostgreSQL)
===================================================== */

CREATE OR REPLACE FUNCTION prevent_pricing_history_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'pricing_history is immutable. UPDATE and DELETE operations are not permitted.';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pricing_history_no_update
    BEFORE UPDATE ON pricing_history
    FOR EACH ROW
    EXECUTE FUNCTION prevent_pricing_history_modification();

CREATE TRIGGER trg_pricing_history_no_delete
    BEFORE DELETE ON pricing_history
    FOR EACH ROW
    EXECUTE FUNCTION prevent_pricing_history_modification();


/* =====================================================
   SECTION 6: PERFORMANCE INDEXES
===================================================== */

-- Auth & Identity
CREATE INDEX idx_app_user_roleid        ON app_user (roleid);
CREATE INDEX idx_customer_userid        ON customer (userid);
CREATE INDEX idx_refresh_token_userid   ON refresh_token (userid);
CREATE INDEX idx_refresh_token_expiry   ON refresh_token (expirydate);

-- Orders (hot path: status filtering, date-range queries, customer lookup)
CREATE INDEX idx_orders_statusid        ON orders (statusid);
CREATE INDEX idx_orders_customerid      ON orders (customerid);
CREATE INDEX idx_orders_created         ON orders (createdtimestamp);

-- Composite: Admin queries (status + created date for scheduler / reports)
CREATE INDEX idx_orders_status_created  ON orders (statusid, createdtimestamp);

-- OrderItem (join path: order lookup)
CREATE INDEX idx_order_item_orderid     ON order_item (orderid);

-- Pricing (hot path: latest price lookup, history timeline)
CREATE INDEX idx_pricing_history_itemid          ON pricing_history (itemid);
CREATE INDEX idx_pricing_history_created         ON pricing_history (createdtimestamp);

-- Composite: resolveUnitPrice() — latest history per item
CREATE INDEX idx_pricing_history_item_created    ON pricing_history (itemid, createdtimestamp DESC);

-- Email Log (admin monitoring)
CREATE INDEX idx_email_log_status       ON email_log (`status`);
CREATE INDEX idx_email_log_sentat       ON email_log (sentat);


/* =====================================================
   SECTION 7: SEED DATA (Order Status Lookup)
===================================================== */

INSERT INTO order_status_lookup (statusid, statusname) VALUES
    (1, 'PENDING'),
    (2, 'CONFIRMED'),
    (3, 'PROCESSING'),
    (4, 'SHIPPED'),
    (5, 'DELIVERED'),
    (6, 'CANCELLED');

INSERT INTO user_role (roleid, rolename) VALUES
    (1, 'ADMIN'),
    (2, 'CUSTOMER');
