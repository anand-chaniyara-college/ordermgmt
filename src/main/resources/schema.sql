-- =============================================================================
--  ORDER MANAGEMENT SYSTEM — PRODUCTION SCHEMA
--  Schema: ordermgmt
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ordermgmt;
SET search_path TO ordermgmt;


/* =====================================================
   SECTION 1: BASE TABLES (Create Order respects FK deps)
===================================================== */

-- 1.1  Reference / Lookup Tables (no FK dependencies)
-- ---------------------------------------------------

CREATE TABLE IF NOT EXISTS USER_ROLE (
    roleid          INTEGER         PRIMARY KEY,
    rolename        VARCHAR(50)     NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ORDER_STATUS_LOOKUP (
    statusid        INTEGER         PRIMARY KEY,
    statusname      VARCHAR(50)     NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);


-- 1.2  Core Identity
-- ---------------------------------------------------

CREATE TABLE IF NOT EXISTS APP_USER (
    userid          UUID     PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL,
    passwordhash    VARCHAR(255)    NOT NULL,
    roleid          INTEGER         NOT NULL,
    isactive        BOOLEAN         DEFAULT TRUE,
    isPasswordChanged BOOLEAN         DEFAULT FALSE,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS CUSTOMER (
    customerid      UUID     PRIMARY KEY,
    userid          UUID     NOT NULL,
    firstname       VARCHAR(100),
    lastname        VARCHAR(100),
    contactno       VARCHAR(20),
    address         VARCHAR(255),
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

-- CREATE TABLE IF NOT EXISTS refresh_token (
--     tokenid         VARCHAR(36)     PRIMARY KEY,
--     userid          VARCHAR(36)     NOT NULL,
--     token           VARCHAR(512)    NOT NULL,
--     expirydate      TIMESTAMP       NOT NULL,
--     revoked         BOOLEAN         DEFAULT FALSE,
--     createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updatedtimestamp TIMESTAMP,
--     createdby       VARCHAR(255),
--     updatedby       VARCHAR(255)
-- );


-- 1.3  Inventory & Pricing
-- ---------------------------------------------------

CREATE TABLE IF NOT EXISTS INVENTORY_ITEM (
    itemid          UUID     PRIMARY KEY,
    itemname        VARCHAR(100),
    availablestock  INTEGER         NOT NULL DEFAULT 0,
    reservedstock   INTEGER         NOT NULL DEFAULT 0,
    version         BIGINT          DEFAULT 0,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS PRICING_CATALOG (
    itemid          UUID     PRIMARY KEY,
    unitprice       DECIMAL(19,4)   NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

-- IMMUTABLE AUDIT TRAIL — INSERT ONLY, NO UPDATE / DELETE
CREATE TABLE IF NOT EXISTS PRICING_HISTORY (
    historyid       UUID     PRIMARY KEY,
    itemid          UUID     NOT NULL,
    oldprice        DECIMAL(19,4),
    newprice        DECIMAL(19,4)   NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);


-- 1.4  Orders
-- ---------------------------------------------------

CREATE TABLE IF NOT EXISTS ORDERS (
    orderid         UUID    PRIMARY KEY,
    customerid      UUID     NOT NULL,
    statusid        INTEGER         NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ORDER_ITEM (
    orderid         UUID     NOT NULL,
    itemid          UUID     NOT NULL,
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

CREATE TABLE IF NOT EXISTS EMAIL_LOG (
    id              UUID       PRIMARY KEY,
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

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_app_user_email') THEN
        ALTER TABLE APP_USER ADD CONSTRAINT uq_app_user_email UNIQUE (email);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_customer_contactno') THEN
        ALTER TABLE CUSTOMER ADD CONSTRAINT uq_customer_contactno UNIQUE (contactno);
    END IF;
END $$;

-- DO $$ BEGIN
--     IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_refresh_token_token') THEN
--         ALTER TABLE refresh_token ADD CONSTRAINT uq_refresh_token_token UNIQUE (token);
--     END IF;
-- END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_user_role_rolename') THEN
        ALTER TABLE USER_ROLE ADD CONSTRAINT uq_user_role_rolename UNIQUE (rolename);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_order_status_statusname') THEN
        ALTER TABLE ORDER_STATUS_LOOKUP ADD CONSTRAINT uq_order_status_statusname UNIQUE (statusname);
    END IF;
END $$;


/* =====================================================
   SECTION 3: CHECK CONSTRAINTS (BUSINESS RULES)
===================================================== */

-- Inventory: stock counts must be non-negative
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_inventory_available_non_negative') THEN
        ALTER TABLE INVENTORY_ITEM ADD CONSTRAINT chk_inventory_available_non_negative CHECK (availablestock >= 0);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_inventory_reserved_non_negative') THEN
        ALTER TABLE INVENTORY_ITEM ADD CONSTRAINT chk_inventory_reserved_non_negative CHECK (reservedstock >= 0);
    END IF;
END $$;

-- OrderItem: quantity must be positive, price non-negative
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_order_item_quantity_positive') THEN
        ALTER TABLE ORDER_ITEM ADD CONSTRAINT chk_order_item_quantity_positive CHECK (quantity > 0);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_order_item_price_non_negative') THEN
        ALTER TABLE ORDER_ITEM ADD CONSTRAINT chk_order_item_price_non_negative CHECK (unitprice >= 0);
    END IF;
END $$;

-- PricingCatalog: price non-negative
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_pricing_catalog_price_non_negative') THEN
        ALTER TABLE PRICING_CATALOG ADD CONSTRAINT chk_pricing_catalog_price_non_negative CHECK (unitprice >= 0);
    END IF;
END $$;

-- PricingHistory: new price non-negative (immutable audit)
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_pricing_history_newprice_non_negative') THEN
        ALTER TABLE PRICING_HISTORY ADD CONSTRAINT chk_pricing_history_newprice_non_negative CHECK (newprice >= 0);
    END IF;
END $$;


/* =====================================================
   SECTION 4: FOREIGN KEY CONSTRAINTS
   All use ON UPDATE CASCADE ON DELETE CASCADE
   EXCEPT pricing_history (immutable — RESTRICT)
===================================================== */

-- AppUser → UserRole
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_app_user_role') THEN
        ALTER TABLE APP_USER ADD CONSTRAINT fk_app_user_role FOREIGN KEY (roleid) REFERENCES USER_ROLE (roleid) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END $$;

-- Customer → AppUser
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_customer_user') THEN
        ALTER TABLE CUSTOMER ADD CONSTRAINT fk_customer_user FOREIGN KEY (userid) REFERENCES APP_USER (userid) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END $$;

-- PricingCatalog → InventoryItem (shared PK)
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pricing_catalog_item') THEN
        ALTER TABLE PRICING_CATALOG ADD CONSTRAINT fk_pricing_catalog_item FOREIGN KEY (itemid) REFERENCES INVENTORY_ITEM (itemid) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END $$;

-- PricingHistory → InventoryItem (IMMUTABLE: RESTRICT delete/update)
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pricing_history_item') THEN
        ALTER TABLE PRICING_HISTORY ADD CONSTRAINT fk_pricing_history_item FOREIGN KEY (itemid) REFERENCES INVENTORY_ITEM (itemid) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;
END $$;

-- Orders → Customer
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_customer') THEN
        ALTER TABLE ORDERS ADD CONSTRAINT fk_orders_customer FOREIGN KEY (customerid) REFERENCES CUSTOMER (customerid) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END $$;

-- Orders → OrderStatusLookup
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_status') THEN
        ALTER TABLE ORDERS ADD CONSTRAINT fk_orders_status FOREIGN KEY (statusid) REFERENCES ORDER_STATUS_LOOKUP (statusid) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END $$;

-- OrderItem → Orders
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_item_order') THEN
        ALTER TABLE ORDER_ITEM ADD CONSTRAINT fk_order_item_order FOREIGN KEY (orderid) REFERENCES ORDERS (orderid) ON UPDATE CASCADE ON DELETE CASCADE;
    END IF;
END $$;

-- OrderItem → InventoryItem
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_item_inventory') THEN
        ALTER TABLE ORDER_ITEM ADD CONSTRAINT fk_order_item_inventory FOREIGN KEY (itemid) REFERENCES INVENTORY_ITEM (itemid) ON UPDATE CASCADE;
    END IF;
END $$;


/* =====================================================
   SECTION 5: IMMUTABILITY RULES FOR PRICING_HISTORY
   Prevent UPDATE and DELETE via trigger (PostgreSQL)
===================================================== */

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON p.pronamespace = n.oid WHERE n.nspname = 'ordermgmt' AND p.proname = 'prevent_pricing_history_modification') THEN
        CREATE FUNCTION prevent_pricing_history_modification()
        RETURNS TRIGGER AS $func$
        BEGIN
            RAISE EXCEPTION 'PRICING_HISTORY is immutable. UPDATE and DELETE operations are not permitted.';
            RETURN NULL;
        END;
        $func$ LANGUAGE plpgsql;
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_pricing_history_no_update') THEN
        CREATE TRIGGER trg_pricing_history_no_update
            BEFORE UPDATE ON PRICING_HISTORY
            FOR EACH ROW
            EXECUTE FUNCTION prevent_pricing_history_modification();
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_pricing_history_no_delete') THEN
        CREATE TRIGGER trg_pricing_history_no_delete
            BEFORE DELETE ON PRICING_HISTORY
            FOR EACH ROW
            EXECUTE FUNCTION prevent_pricing_history_modification();
    END IF;
END $$;


/* =====================================================
   SECTION 6: PERFORMANCE INDEXES
===================================================== */

-- Auth & Identity
CREATE INDEX IF NOT EXISTS idx_app_user_roleid        ON APP_USER (roleid);
CREATE INDEX IF NOT EXISTS idx_customer_userid        ON CUSTOMER (userid);

-- Orders (hot path: status filtering, date-range queries, customer lookup)
CREATE INDEX IF NOT EXISTS idx_orders_statusid        ON ORDERS (statusid);
CREATE INDEX IF NOT EXISTS idx_orders_customerid      ON ORDERS (customerid);
CREATE INDEX IF NOT EXISTS idx_orders_created         ON ORDERS (createdtimestamp);

-- Composite: Admin queries (status + created date for scheduler / reports)
CREATE INDEX IF NOT EXISTS idx_orders_status_created  ON ORDERS (statusid, createdtimestamp);

-- OrderItem (join path: order lookup)
CREATE INDEX IF NOT EXISTS idx_order_item_orderid     ON ORDER_ITEM (orderid);

-- Pricing (hot path: latest price lookup, history timeline)
CREATE INDEX IF NOT EXISTS idx_pricing_history_itemid          ON PRICING_HISTORY (itemid);
CREATE INDEX IF NOT EXISTS idx_pricing_history_created         ON PRICING_HISTORY (createdtimestamp);

-- Composite: resolveUnitPrice() — latest history per item
CREATE INDEX IF NOT EXISTS idx_pricing_history_item_created    ON PRICING_HISTORY (itemid, createdtimestamp DESC);

-- Email Log (admin monitoring)
CREATE INDEX IF NOT EXISTS idx_email_log_status       ON EMAIL_LOG (status);
CREATE INDEX IF NOT EXISTS idx_email_log_sentat       ON EMAIL_LOG (sentat);


/* =====================================================
   SECTION 7: SEED DATA (Order Status Lookup)
===================================================== */

INSERT INTO ORDER_STATUS_LOOKUP (statusid, statusname) VALUES
    (1, 'PENDING'),
    (2, 'CONFIRMED'),
    (3, 'PROCESSING'),
    (4, 'SHIPPED'),
    (5, 'DELIVERED'),
    (6, 'CANCELLED')
ON CONFLICT (statusid) DO NOTHING;

INSERT INTO USER_ROLE (roleid, rolename) VALUES
    (1, 'ADMIN'),
    (2, 'CUSTOMER')
ON CONFLICT (roleid) DO NOTHING;
