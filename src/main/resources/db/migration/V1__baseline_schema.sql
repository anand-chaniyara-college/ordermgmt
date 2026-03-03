-- =============================================================================
--  ORDER MANAGEMENT SYSTEM — PRODUCTION SCHEMA
--  Schema: ordermgmt
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ordermgmt;
SET search_path TO ordermgmt;


/* =====================================================
   SECTION 1: BASE TABLES
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
    userid          UUID            PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL,
    passwordhash    VARCHAR(255)    NOT NULL,
    roleid          INTEGER         NOT NULL,
    isactive        BOOLEAN         DEFAULT TRUE,
    ispasswordchanged BOOLEAN       DEFAULT FALSE,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS CUSTOMER (
    customerid      UUID            PRIMARY KEY,
    userid          UUID            NOT NULL,
    firstname       VARCHAR(100),
    lastname        VARCHAR(100),
    contactno       VARCHAR(20),
    address         VARCHAR(255),
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);


-- 1.3  Inventory & Pricing
-- ---------------------------------------------------

CREATE TABLE IF NOT EXISTS INVENTORY_ITEM (
    itemid          UUID            PRIMARY KEY,
    itemname        VARCHAR(100),
    availablestock  INTEGER         NOT NULL DEFAULT 0 CHECK (availablestock >= 0),
    reservedstock   INTEGER         NOT NULL DEFAULT 0 CHECK (reservedstock >= 0),
    version         BIGINT          DEFAULT 0,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS PRICING_CATALOG (
    itemid          UUID            PRIMARY KEY,
    unitprice       DECIMAL(19,4)   NOT NULL CHECK (unitprice >= 0),
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

-- IMMUTABLE AUDIT TRAIL — INSERT ONLY, NO UPDATE / DELETE
CREATE TABLE IF NOT EXISTS PRICING_HISTORY (
    historyid       UUID            PRIMARY KEY,
    itemid          UUID            NOT NULL,
    oldprice        DECIMAL(19,4),
    newprice        DECIMAL(19,4)   NOT NULL CHECK (newprice >= 0),
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);


-- 1.4  Orders
-- ---------------------------------------------------

CREATE TABLE IF NOT EXISTS ORDERS (
    orderid         UUID            PRIMARY KEY,
    customerid      UUID            NOT NULL,
    statusid        INTEGER         NOT NULL,
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ORDER_ITEM (
    orderid         UUID            NOT NULL,
    itemid          UUID            NOT NULL,
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    unitprice       DECIMAL(19,4)   NOT NULL CHECK (unitprice >= 0),
    createdtimestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby       VARCHAR(255),
    updatedby       VARCHAR(255),
    PRIMARY KEY (orderid, itemid)
);


-- 1.5  System Logging
-- ---------------------------------------------------

CREATE TABLE IF NOT EXISTS EMAIL_LOG (
    id              UUID            PRIMARY KEY,
    recipient       VARCHAR(255)    NOT NULL,
    subject         VARCHAR(255),
    status          VARCHAR(255)    NOT NULL,
    createdby       VARCHAR(255),
    sentat          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    errormessage    VARCHAR(1000)
);


/* =====================================================
   SECTION 2: UNIQUE CONSTRAINTS
   Using CREATE UNIQUE INDEX IF NOT EXISTS — fully supports
   IF NOT EXISTS without DO $$ blocks.
===================================================== */

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_email        ON APP_USER (email);
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_contactno    ON CUSTOMER (contactno);
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_role_rolename    ON USER_ROLE (rolename);
CREATE UNIQUE INDEX IF NOT EXISTS uq_order_status_name     ON ORDER_STATUS_LOOKUP (statusname);


/* =====================================================
   SECTION 3: FOREIGN KEY CONSTRAINTS
   Direct ALTER TABLE — safe on first run.
   spring.sql.init.continue-on-error=true handles re-runs
   where constraints already exist.
===================================================== */

-- AppUser → UserRole
ALTER TABLE APP_USER ADD CONSTRAINT fk_app_user_role
    FOREIGN KEY (roleid) REFERENCES USER_ROLE (roleid) ON UPDATE CASCADE ON DELETE CASCADE;

-- Customer → AppUser
ALTER TABLE CUSTOMER ADD CONSTRAINT fk_customer_user
    FOREIGN KEY (userid) REFERENCES APP_USER (userid) ON UPDATE CASCADE ON DELETE CASCADE;

-- PricingCatalog → InventoryItem (shared PK)
ALTER TABLE PRICING_CATALOG ADD CONSTRAINT fk_pricing_catalog_item
    FOREIGN KEY (itemid) REFERENCES INVENTORY_ITEM (itemid) ON UPDATE CASCADE ON DELETE CASCADE;

-- PricingHistory → InventoryItem (IMMUTABLE: RESTRICT delete/update)
ALTER TABLE PRICING_HISTORY ADD CONSTRAINT fk_pricing_history_item
    FOREIGN KEY (itemid) REFERENCES INVENTORY_ITEM (itemid) ON UPDATE RESTRICT ON DELETE RESTRICT;

-- Orders → Customer
ALTER TABLE ORDERS ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customerid) REFERENCES CUSTOMER (customerid) ON UPDATE CASCADE ON DELETE CASCADE;

-- Orders → OrderStatusLookup
ALTER TABLE ORDERS ADD CONSTRAINT fk_orders_status
    FOREIGN KEY (statusid) REFERENCES ORDER_STATUS_LOOKUP (statusid) ON UPDATE CASCADE ON DELETE CASCADE;

-- OrderItem → Orders
ALTER TABLE ORDER_ITEM ADD CONSTRAINT fk_order_item_order
    FOREIGN KEY (orderid) REFERENCES ORDERS (orderid) ON UPDATE CASCADE ON DELETE CASCADE;

-- OrderItem → InventoryItem
ALTER TABLE ORDER_ITEM ADD CONSTRAINT fk_order_item_inventory
    FOREIGN KEY (itemid) REFERENCES INVENTORY_ITEM (itemid) ON UPDATE CASCADE;


/* =====================================================
   SECTION 4: IMMUTABILITY RULES FOR PRICING_HISTORY
   CREATE OR REPLACE FUNCTION/TRIGGER
===================================================== */

CREATE OR REPLACE FUNCTION ordermgmt.prevent_pricing_history_modification()
RETURNS TRIGGER LANGUAGE plpgsql AS
'BEGIN RAISE EXCEPTION ''PRICING_HISTORY is immutable. UPDATE and DELETE operations are not permitted.''; RETURN NULL; END';

DROP TRIGGER IF EXISTS trg_pricing_history_no_update ON PRICING_HISTORY;
CREATE TRIGGER trg_pricing_history_no_update
    BEFORE UPDATE ON PRICING_HISTORY
    FOR EACH ROW
    EXECUTE FUNCTION ordermgmt.prevent_pricing_history_modification();

DROP TRIGGER IF EXISTS trg_pricing_history_no_delete ON PRICING_HISTORY;
CREATE TRIGGER trg_pricing_history_no_delete
    BEFORE DELETE ON PRICING_HISTORY
    FOR EACH ROW
    EXECUTE FUNCTION ordermgmt.prevent_pricing_history_modification();


/* =====================================================
   SECTION 5: PERFORMANCE INDEXES
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
   SECTION 6: SEED DATA (Order Status Lookup)
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
