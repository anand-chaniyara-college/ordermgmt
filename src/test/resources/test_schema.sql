-- =============================================================================
-- test_ordermgmt CLEAN SCHEMA SETUP (DE-DUPLICATED)
-- - Single implementation for each constraint/index
-- - Consistent constraint naming
-- - Consistent audit timestamp columns
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS test_ordermgmt;
SET search_path TO test_ordermgmt;


-- =============================================================================
-- 1) FUNCTIONS
-- =============================================================================

CREATE OR REPLACE FUNCTION test_ordermgmt.prevent_pricing_history_modification()
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

CREATE TABLE IF NOT EXISTS test_ordermgmt.organization (
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

CREATE TABLE IF NOT EXISTS test_ordermgmt.user_role (
                                                   roleid integer NOT NULL,
                                                   createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    rolename character varying(50) NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone
    );

CREATE TABLE IF NOT EXISTS test_ordermgmt.order_status_lookup (
                                                             statusid integer NOT NULL,
                                                             createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    statusname character varying(50) NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone
    );

CREATE TABLE IF NOT EXISTS test_ordermgmt.app_user (
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

CREATE TABLE IF NOT EXISTS test_ordermgmt.customer (
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

CREATE TABLE IF NOT EXISTS test_ordermgmt.inventory_item (
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

CREATE TABLE IF NOT EXISTS test_ordermgmt.pricing_catalog (
                                                         itemid uuid NOT NULL,
                                                         createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    unitprice numeric(19,4) NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone
    );

CREATE TABLE IF NOT EXISTS test_ordermgmt.pricing_history (
                                                         historyid uuid NOT NULL,
                                                         createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    newprice numeric(19,4) NOT NULL,
    oldprice numeric(19,4),
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone,
    itemid uuid NOT NULL
    );

CREATE TABLE IF NOT EXISTS test_ordermgmt.orders (
                                                orderid uuid NOT NULL,
                                                createdby character varying(255),
    createdtimestamp timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updatedby character varying(255),
    updatedtimestamp timestamp(6) without time zone,
    customerid uuid NOT NULL,
    statusid integer NOT NULL,
    org_id uuid NOT NULL
    );

CREATE TABLE IF NOT EXISTS test_ordermgmt.order_item (
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

CREATE TABLE IF NOT EXISTS test_ordermgmt.email_log (
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
INSERT INTO test_ordermgmt.order_status_lookup (statusid, createdby, createdtimestamp, statusname, updatedby, updatedtimestamp)
VALUES
    (1, 'SYSTEM', CURRENT_TIMESTAMP, 'PENDING', 'SYSTEM', CURRENT_TIMESTAMP),
    (2, 'SYSTEM', CURRENT_TIMESTAMP, 'CONFIRMED', 'SYSTEM', CURRENT_TIMESTAMP),
    (3, 'SYSTEM', CURRENT_TIMESTAMP, 'PROCESSING', 'SYSTEM', CURRENT_TIMESTAMP),
    (4, 'SYSTEM', CURRENT_TIMESTAMP, 'SHIPPED', 'SYSTEM', CURRENT_TIMESTAMP),
    (5, 'SYSTEM', CURRENT_TIMESTAMP, 'DELIVERED', 'SYSTEM', CURRENT_TIMESTAMP),
    (6, 'SYSTEM', CURRENT_TIMESTAMP, 'CANCELLED', 'SYSTEM', CURRENT_TIMESTAMP)
    ON CONFLICT (statusid) DO NOTHING;

-- Insert user role data
INSERT INTO test_ordermgmt.user_role (roleid, createdby, createdtimestamp, rolename, updatedby, updatedtimestamp)
VALUES
    (1, 'SYSTEM', CURRENT_TIMESTAMP, 'ADMIN', 'SYSTEM', CURRENT_TIMESTAMP),
    (2, 'SYSTEM', CURRENT_TIMESTAMP, 'CUSTOMER', 'SYSTEM', CURRENT_TIMESTAMP),
    (3, 'SYSTEM', CURRENT_TIMESTAMP, 'SUPER_ADMIN', 'SYSTEM', CURRENT_TIMESTAMP),
    (4, 'SYSTEM', CURRENT_TIMESTAMP, 'ORG_ADMIN', 'SYSTEM', CURRENT_TIMESTAMP)
    ON CONFLICT (roleid) DO NOTHING;

-- Note: Add ON CONFLICT clauses if you have unique constraints on these tables

INSERT INTO test_ordermgmt.organization (org_id, name, subdomain, isactive, createdtimestamp, updatedtimestamp, createdby, updatedby,description) VALUES
    ('11111111-1111-1111-1111-111111111111','SYSTEM','systesting',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'SYSTEM','SYSTEM','SYSTEM GENERATED FOR SUPER ADMIN' );

INSERT INTO test_ordermgmt.app_user ( userid, createdby, createdtimestamp, email, isactive, ispasswordchanged, passwordhash, updatedby, updatedtimestamp, roleid, org_id) VALUES
    ('22222222-2222-2222-2222-222222222222', 'SYSTEM', CURRENT_TIMESTAMP, 'superadmin@super.com', true, true, 'hashedpassword123', 'SYSTEM', CURRENT_TIMESTAMP, 3, '11111111-1111-1111-1111-111111111111');
-- =============================================================================
-- 3) CONSTRAINTS (ADDED ONCE, WITH CONSISTENT NAMES)
-- =============================================================================

-- Primary keys
ALTER TABLE ONLY test_ordermgmt.organization
    ADD CONSTRAINT pk_organization PRIMARY KEY (org_id);

ALTER TABLE ONLY test_ordermgmt.user_role
    ADD CONSTRAINT pk_user_role PRIMARY KEY (roleid);

ALTER TABLE ONLY test_ordermgmt.order_status_lookup
    ADD CONSTRAINT pk_order_status_lookup PRIMARY KEY (statusid);

ALTER TABLE ONLY test_ordermgmt.app_user
    ADD CONSTRAINT pk_app_user PRIMARY KEY (userid);

ALTER TABLE ONLY test_ordermgmt.customer
    ADD CONSTRAINT pk_customer PRIMARY KEY (customerid);

ALTER TABLE ONLY test_ordermgmt.inventory_item
    ADD CONSTRAINT pk_inventory_item PRIMARY KEY (itemid);

ALTER TABLE ONLY test_ordermgmt.pricing_catalog
    ADD CONSTRAINT pk_pricing_catalog PRIMARY KEY (itemid);

ALTER TABLE ONLY test_ordermgmt.pricing_history
    ADD CONSTRAINT pk_pricing_history PRIMARY KEY (historyid);

ALTER TABLE ONLY test_ordermgmt.orders
    ADD CONSTRAINT pk_orders PRIMARY KEY (orderid);

ALTER TABLE ONLY test_ordermgmt.order_item
    ADD CONSTRAINT pk_order_item PRIMARY KEY (itemid, orderid);

ALTER TABLE ONLY test_ordermgmt.email_log
    ADD CONSTRAINT pk_email_log PRIMARY KEY (id);

-- Unique constraints
ALTER TABLE ONLY test_ordermgmt.user_role
    ADD CONSTRAINT uq_user_role_rolename UNIQUE (rolename);

ALTER TABLE ONLY test_ordermgmt.order_status_lookup
    ADD CONSTRAINT uq_order_status_lookup_statusname UNIQUE (statusname);

ALTER TABLE ONLY test_ordermgmt.organization
    ADD CONSTRAINT uq_organization_subdomain UNIQUE (subdomain);

-- Check constraints
ALTER TABLE ONLY test_ordermgmt.inventory_item
    ADD CONSTRAINT ck_inventory_item_availablestock_nonnegative CHECK (availablestock >= 0);

ALTER TABLE ONLY test_ordermgmt.inventory_item
    ADD CONSTRAINT ck_inventory_item_reservedstock_nonnegative CHECK (reservedstock >= 0);

-- Foreign keys
ALTER TABLE ONLY test_ordermgmt.app_user
    ADD CONSTRAINT fk_app_user_role
    FOREIGN KEY (roleid) REFERENCES test_ordermgmt.user_role(roleid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.app_user
    ADD CONSTRAINT fk_app_user_org
    FOREIGN KEY (org_id) REFERENCES test_ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.customer
    ADD CONSTRAINT fk_customer_user
    FOREIGN KEY (userid) REFERENCES test_ordermgmt.app_user(userid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.customer
    ADD CONSTRAINT fk_customer_org
    FOREIGN KEY (org_id) REFERENCES test_ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.email_log
    ADD CONSTRAINT fk_email_log_org
    FOREIGN KEY (org_id) REFERENCES test_ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.inventory_item
    ADD CONSTRAINT fk_inventory_item_org
    FOREIGN KEY (org_id) REFERENCES test_ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.orders
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customerid) REFERENCES test_ordermgmt.customer(customerid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.orders
    ADD CONSTRAINT fk_orders_status
    FOREIGN KEY (statusid) REFERENCES test_ordermgmt.order_status_lookup(statusid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.orders
    ADD CONSTRAINT fk_orders_org
    FOREIGN KEY (org_id) REFERENCES test_ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.order_item
    ADD CONSTRAINT fk_order_item_order
    FOREIGN KEY (orderid) REFERENCES test_ordermgmt.orders(orderid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.order_item
    ADD CONSTRAINT fk_order_item_inventory
    FOREIGN KEY (itemid) REFERENCES test_ordermgmt.inventory_item(itemid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.order_item
    ADD CONSTRAINT fk_order_item_org
    FOREIGN KEY (org_id) REFERENCES test_ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY test_ordermgmt.pricing_catalog
    ADD CONSTRAINT fk_pricing_catalog_item
    FOREIGN KEY (itemid) REFERENCES test_ordermgmt.inventory_item(itemid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

-- =============================================================================
-- 4) INDEXES (ADDED ONCE)
-- =============================================================================

CREATE INDEX idx_app_user_org_id ON test_ordermgmt.app_user USING btree (org_id);
CREATE INDEX idx_app_user_roleid ON test_ordermgmt.app_user USING btree (roleid);

CREATE INDEX idx_customer_org_id ON test_ordermgmt.customer USING btree (org_id);
CREATE INDEX idx_customer_userid ON test_ordermgmt.customer USING btree (userid);

CREATE INDEX idx_email_log_org_id ON test_ordermgmt.email_log USING btree (org_id);
CREATE INDEX idx_email_log_sentat ON test_ordermgmt.email_log USING btree (sentat);
CREATE INDEX idx_email_log_status ON test_ordermgmt.email_log USING btree (status);

CREATE INDEX idx_inventory_item_org_id ON test_ordermgmt.inventory_item USING btree (org_id);

CREATE INDEX idx_order_item_orderid ON test_ordermgmt.order_item USING btree (orderid);
CREATE INDEX idx_order_item_org_id ON test_ordermgmt.order_item USING btree (org_id);

CREATE INDEX idx_orders_created ON test_ordermgmt.orders USING btree (createdtimestamp);
CREATE INDEX idx_orders_customerid ON test_ordermgmt.orders USING btree (customerid);
CREATE INDEX idx_orders_org_id ON test_ordermgmt.orders USING btree (org_id);
CREATE INDEX idx_orders_status_created ON test_ordermgmt.orders USING btree (statusid, createdtimestamp);
CREATE INDEX idx_orders_statusid ON test_ordermgmt.orders USING btree (statusid);

CREATE INDEX idx_pricing_history_created ON test_ordermgmt.pricing_history USING btree (createdtimestamp);
CREATE INDEX idx_pricing_history_item_created ON test_ordermgmt.pricing_history USING btree (itemid, createdtimestamp DESC);
CREATE INDEX idx_pricing_history_itemid ON test_ordermgmt.pricing_history USING btree (itemid);

-- Tenant-scoped uniqueness
CREATE UNIQUE INDEX uq_app_user_email_org
    ON test_ordermgmt.app_user USING btree (lower((email)::text), org_id);

CREATE UNIQUE INDEX uq_customer_contactno_org
    ON test_ordermgmt.customer USING btree (contactno, org_id)
    WHERE (contactno IS NOT NULL);


-- =============================================================================
-- 5) TRIGGERS
-- =============================================================================

CREATE TRIGGER trg_pricing_history_no_update
    BEFORE UPDATE ON test_ordermgmt.pricing_history
    FOR EACH ROW
    EXECUTE FUNCTION test_ordermgmt.prevent_pricing_history_modification();

CREATE TRIGGER trg_pricing_history_no_delete
    BEFORE DELETE ON test_ordermgmt.pricing_history
    FOR EACH ROW
    EXECUTE FUNCTION test_ordermgmt.prevent_pricing_history_modification();
