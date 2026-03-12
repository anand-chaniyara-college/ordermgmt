-- =============================================================================
-- ORDERMGMT CLEAN SCHEMA SETUP (DE-DUPLICATED)
-- - Single implementation for each constraint/index
-- - Consistent constraint naming
-- - Consistent audit timestamp columns
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ordermgmt;
SET search_path TO ordermgmt;


-- =============================================================================
-- 1) FUNCTIONS
-- =============================================================================

CREATE OR REPLACE FUNCTION ordermgmt.prevent_pricing_history_modification()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Modifications to pricing_history are not allowed. This table is append-only.';
    RETURN NULL;
END;
$$;


-- =============================================================================
-- 2) TABLE SKELETON
-- =============================================================================

CREATE TABLE IF NOT EXISTS ordermgmt.ORGANIZATION (
    org_id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) NOT NULL,
    subdomain character varying(100) NOT NULL,
    isactive boolean DEFAULT true NOT NULL,
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedtimestamp timestamp(6) without time zone,
    createdby character varying(255),
    updatedby character varying(255),
    description character varying(1000)
);

CREATE TABLE IF NOT EXISTS ordermgmt.USER_ROLE (
    roleid integer NOT NULL,
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    rolename character varying(50) NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone
);

CREATE TABLE IF NOT EXISTS ordermgmt.ORDER_STATUS_LOOKUP (
    statusid integer NOT NULL,
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    statusname character varying(50) NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone
);

CREATE TABLE IF NOT EXISTS ordermgmt.APP_USER (
    userid uuid NOT NULL,
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    email character varying(255) NOT NULL,
    isactive boolean,
    ispasswordchanged boolean,
    passwordhash character varying(255) NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone,
    roleid integer NOT NULL,
    org_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS ordermgmt.CUSTOMER (
    customerid uuid NOT NULL,
    address character varying(255),
    contactno character varying(20),
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    firstname character varying(100),
    lastname character varying(100),
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone,
    userid uuid NOT NULL,
    org_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS ordermgmt.INVENTORY_ITEM (
    itemid uuid NOT NULL,
    availablestock integer NOT NULL,
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    itemname character varying(100),
    reservedstock integer NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone,
    version bigint,
    org_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS ordermgmt.PRICING_CATALOG (
    itemid uuid NOT NULL,
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    unitprice numeric(19,4) NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone
);

CREATE TABLE IF NOT EXISTS ordermgmt.PRICING_HISTORY (
    historyid uuid NOT NULL,
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    newprice numeric(19,4) NOT NULL,
    oldprice numeric(19,4),
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone,
    itemid uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS ordermgmt.ORDERS (
    orderid uuid NOT NULL,
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone,
    customerid uuid NOT NULL,
    statusid integer NOT NULL,
    org_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS ordermgmt.ORDER_ITEM (
    itemid uuid NOT NULL,
    createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    quantity integer NOT NULL,
    unitprice numeric(19,4) NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone,
    orderid uuid NOT NULL,
    org_id uuid NOT NULL
);

CREATE TABLE IF NOT EXISTS ordermgmt.EMAIL_LOG (
    id uuid NOT NULL,
    createdby character varying(255),
    errormessage character varying(1000),
    recipient character varying(255) NOT NULL,
    sentat timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL,
    subject character varying(255),
    org_id uuid NOT NULL
);
-- =============================================================================
-- a)Lookup data
-- =============================================================================
-- Insert order status lookup data
INSERT INTO ordermgmt.ORDER_STATUS_LOOKUP (statusid, createdby, createdtimestamp, statusname, updatedby, updatedtimestamp)
VALUES
    (1, 'SYSTEM', CURRENT_TIMESTAMP, 'PENDING', 'SYSTEM', CURRENT_TIMESTAMP),
    (2, 'SYSTEM', CURRENT_TIMESTAMP, 'CONFIRMED', 'SYSTEM', CURRENT_TIMESTAMP),
    (3, 'SYSTEM', CURRENT_TIMESTAMP, 'PROCESSING', 'SYSTEM', CURRENT_TIMESTAMP),
    (4, 'SYSTEM', CURRENT_TIMESTAMP, 'SHIPPED', 'SYSTEM', CURRENT_TIMESTAMP),
    (5, 'SYSTEM', CURRENT_TIMESTAMP, 'DELIVERED', 'SYSTEM', CURRENT_TIMESTAMP),
    (6, 'SYSTEM', CURRENT_TIMESTAMP, 'CANCELLED', 'SYSTEM', CURRENT_TIMESTAMP)
ON CONFLICT (statusid) DO NOTHING;

-- Insert user role data
INSERT INTO ordermgmt.USER_ROLE (roleid, createdby, createdtimestamp, rolename, updatedby, updatedtimestamp)
VALUES
    (1, 'SYSTEM', CURRENT_TIMESTAMP, 'ADMIN', 'SYSTEM', CURRENT_TIMESTAMP),
    (2, 'SYSTEM', CURRENT_TIMESTAMP, 'CUSTOMER', 'SYSTEM', CURRENT_TIMESTAMP),
    (3, 'SYSTEM', CURRENT_TIMESTAMP, 'SUPER_ADMIN', 'SYSTEM', CURRENT_TIMESTAMP),
    (4, 'SYSTEM', CURRENT_TIMESTAMP, 'ORG_ADMIN', 'SYSTEM', CURRENT_TIMESTAMP)
ON CONFLICT (roleid) DO NOTHING;

-- Note: Add ON CONFLICT clauses if you have unique constraints on these tables

INSERT INTO ordermgmt.ORGANIZATION
(org_id, name, subdomain, isactive, createdtimestamp, updatedtimestamp, createdby, updatedby, description)
VALUES ('00000000-0000-0000-0000-000000000001', 'system', 'system', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', 'system')
ON CONFLICT (org_id) DO NOTHING;
INSERT INTO ordermgmt.APP_USER (
    userid, createdby, createdtimestamp, email, isactive, ispasswordchanged, passwordhash, updatedby, updatedtimestamp, roleid, org_id
) VALUES ('00000000-0000-0000-0000-000000000002','SYSTEM',CURRENT_TIMESTAMP,'superadmin@superemail.com',
     true,true,'$2y$10$xqB/IbPkfb6uulzzoBJENeaiLxJ.iHE7S0zSVyUvPK8FiqtWDSXP.','SYSTEM',
     CURRENT_TIMESTAMP,3,'00000000-0000-0000-0000-000000000001')
ON CONFLICT (userid) DO NOTHING;
-- =============================================================================
-- 3) CONSTRAINTS (ADDED ONCE, WITH CONSISTENT NAMES)
-- =============================================================================

-- Primary keys
ALTER TABLE ONLY ordermgmt.ORGANIZATION
    ADD CONSTRAINT pk_organization PRIMARY KEY (org_id);

ALTER TABLE ONLY ordermgmt.USER_ROLE
    ADD CONSTRAINT pk_user_role PRIMARY KEY (roleid);

ALTER TABLE ONLY ordermgmt.ORDER_STATUS_LOOKUP
    ADD CONSTRAINT pk_order_status_lookup PRIMARY KEY (statusid);

ALTER TABLE ONLY ordermgmt.APP_USER
    ADD CONSTRAINT pk_app_user PRIMARY KEY (userid);

ALTER TABLE ONLY ordermgmt.CUSTOMER
    ADD CONSTRAINT pk_customer PRIMARY KEY (customerid);

ALTER TABLE ONLY ordermgmt.INVENTORY_ITEM
    ADD CONSTRAINT pk_inventory_item PRIMARY KEY (itemid);

ALTER TABLE ONLY ordermgmt.PRICING_CATALOG
    ADD CONSTRAINT pk_pricing_catalog PRIMARY KEY (itemid);

ALTER TABLE ONLY ordermgmt.PRICING_HISTORY
    ADD CONSTRAINT pk_pricing_history PRIMARY KEY (historyid);

ALTER TABLE ONLY ordermgmt.ORDERS
    ADD CONSTRAINT pk_orders PRIMARY KEY (orderid);

ALTER TABLE ONLY ordermgmt.ORDER_ITEM
    ADD CONSTRAINT pk_order_item PRIMARY KEY (itemid, orderid);

ALTER TABLE ONLY ordermgmt.EMAIL_LOG
    ADD CONSTRAINT pk_email_log PRIMARY KEY (id);

-- Unique constraints
ALTER TABLE ONLY ordermgmt.USER_ROLE
    ADD CONSTRAINT uq_user_role_rolename UNIQUE (rolename);

ALTER TABLE ONLY ordermgmt.ORDER_STATUS_LOOKUP
    ADD CONSTRAINT uq_order_status_lookup_statusname UNIQUE (statusname);

ALTER TABLE ONLY ordermgmt.ORGANIZATION
    ADD CONSTRAINT uq_organization_subdomain UNIQUE (subdomain);

-- Check constraints
ALTER TABLE ONLY ordermgmt.INVENTORY_ITEM
    ADD CONSTRAINT ck_inventory_item_availablestock_nonnegative CHECK (availablestock >= 0);

ALTER TABLE ONLY ordermgmt.INVENTORY_ITEM
    ADD CONSTRAINT ck_inventory_item_reservedstock_nonnegative CHECK (reservedstock >= 0);

-- Foreign keys
ALTER TABLE ONLY ordermgmt.APP_USER
    ADD CONSTRAINT fk_app_user_role
    FOREIGN KEY (roleid) REFERENCES ordermgmt.USER_ROLE(roleid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.APP_USER
    ADD CONSTRAINT fk_app_user_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.ORGANIZATION(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.CUSTOMER
    ADD CONSTRAINT fk_customer_user
    FOREIGN KEY (userid) REFERENCES ordermgmt.APP_USER(userid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.CUSTOMER
    ADD CONSTRAINT fk_customer_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.ORGANIZATION(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.EMAIL_LOG
    ADD CONSTRAINT fk_email_log_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.ORGANIZATION(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.INVENTORY_ITEM
    ADD CONSTRAINT fk_inventory_item_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.ORGANIZATION(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.ORDERS
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customerid) REFERENCES ordermgmt.CUSTOMER(customerid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.ORDERS
    ADD CONSTRAINT fk_orders_status
    FOREIGN KEY (statusid) REFERENCES ordermgmt.ORDER_STATUS_LOOKUP(statusid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.ORDERS
    ADD CONSTRAINT fk_orders_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.ORGANIZATION(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.ORDER_ITEM
    ADD CONSTRAINT fk_order_item_order
    FOREIGN KEY (orderid) REFERENCES ordermgmt.ORDERS(orderid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.ORDER_ITEM
    ADD CONSTRAINT fk_order_item_inventory
    FOREIGN KEY (itemid) REFERENCES ordermgmt.INVENTORY_ITEM(itemid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.ORDER_ITEM
    ADD CONSTRAINT fk_order_item_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.ORGANIZATION(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.PRICING_CATALOG
    ADD CONSTRAINT fk_pricing_catalog_item
    FOREIGN KEY (itemid) REFERENCES ordermgmt.INVENTORY_ITEM(itemid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

-- =============================================================================
-- 4) INDEXES (ADDED ONCE)
-- =============================================================================

CREATE INDEX idx_app_user_org_id ON ordermgmt.APP_USER USING btree (org_id);
CREATE INDEX idx_app_user_roleid ON ordermgmt.APP_USER USING btree (roleid);

CREATE INDEX idx_customer_org_id ON ordermgmt.CUSTOMER USING btree (org_id);
CREATE INDEX idx_customer_userid ON ordermgmt.CUSTOMER USING btree (userid);

CREATE INDEX idx_email_log_org_id ON ordermgmt.EMAIL_LOG USING btree (org_id);
CREATE INDEX idx_email_log_sentat ON ordermgmt.EMAIL_LOG USING btree (sentat);
CREATE INDEX idx_email_log_status ON ordermgmt.EMAIL_LOG USING btree (status);

CREATE INDEX idx_inventory_item_org_id ON ordermgmt.INVENTORY_ITEM USING btree (org_id);

CREATE INDEX idx_order_item_orderid ON ordermgmt.ORDER_ITEM USING btree (orderid);
CREATE INDEX idx_order_item_org_id ON ordermgmt.ORDER_ITEM USING btree (org_id);

CREATE INDEX idx_orders_created ON ordermgmt.ORDERS USING btree (createdtimestamp);
CREATE INDEX idx_orders_customerid ON ordermgmt.ORDERS USING btree (customerid);
CREATE INDEX idx_orders_org_id ON ordermgmt.ORDERS USING btree (org_id);
CREATE INDEX idx_orders_status_created ON ordermgmt.ORDERS USING btree (statusid, createdtimestamp);
CREATE INDEX idx_orders_statusid ON ordermgmt.ORDERS USING btree (statusid);

CREATE INDEX idx_pricing_history_created ON ordermgmt.PRICING_HISTORY USING btree (createdtimestamp);
CREATE INDEX idx_pricing_history_item_created ON ordermgmt.PRICING_HISTORY USING btree (itemid, createdtimestamp DESC);
CREATE INDEX idx_pricing_history_itemid ON ordermgmt.PRICING_HISTORY USING btree (itemid);

-- Tenant-scoped uniqueness
CREATE UNIQUE INDEX uq_app_user_email_org
    ON ordermgmt.APP_USER USING btree (lower((email)::text), org_id);

CREATE UNIQUE INDEX uq_customer_contactno_org
    ON ordermgmt.CUSTOMER USING btree (contactno, org_id)
    WHERE (contactno IS NOT NULL);


-- =============================================================================
-- 5) TRIGGERS
-- =============================================================================

CREATE TRIGGER trg_pricing_history_no_update
    BEFORE UPDATE ON ordermgmt.PRICING_HISTORY
    FOR EACH ROW
    EXECUTE FUNCTION ordermgmt.prevent_pricing_history_modification();

CREATE TRIGGER trg_pricing_history_no_delete
    BEFORE DELETE ON ordermgmt.PRICING_HISTORY
    FOR EACH ROW
    EXECUTE FUNCTION ordermgmt.prevent_pricing_history_modification();
