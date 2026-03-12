-- =============================================================================
-- V5: Consolidate organization data and align live schema with cleanschema.sql
-- - Move tenant data from old org to new org
-- - Remove redundant constraints/indexes
-- - Normalize FK actions, naming, tenant uniqueness, timestamps
-- - Preserve business data (no table/data drops)
-- =============================================================================

SET search_path TO ordermgmt;

-- -----------------------------------------------------------------------------
-- 1) Validate merge preconditions
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    v_old CONSTANT uuid := '00000000-0000-0000-0000-000000000001';
    v_new CONSTANT uuid := 'a7e1b9d2-88fe-4d03-9bfd-120f39c48214';
BEGIN
    IF v_old = v_new THEN
        RAISE EXCEPTION 'Source and target org_id cannot be the same (%)', v_old;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM ordermgmt.organization WHERE org_id = v_new) THEN
        RAISE EXCEPTION 'Target org_id % does not exist in ordermgmt.organization', v_new;
    END IF;

    -- Prevent tenant-unique email conflicts after merge.
    -- (Rows from v_old and NULL org_id are moved to v_new.)
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT lower(email) AS email_key,
                   COUNT(*) FILTER (WHERE org_id = v_old OR org_id IS NULL) AS move_cnt,
                   COUNT(*) FILTER (WHERE org_id = v_new) AS new_cnt
            FROM ordermgmt.app_user
            WHERE org_id IN (v_old, v_new) OR org_id IS NULL
            GROUP BY lower(email)
        ) t
        WHERE t.move_cnt > 0 AND t.new_cnt > 0
    ) THEN
        RAISE EXCEPTION 'Org merge blocked: duplicate lower(email) would exist in target org after move';
    END IF;

    -- Prevent tenant-unique contact conflicts after merge.
    -- (Rows from v_old and NULL org_id are moved to v_new.)
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT contactno,
                   COUNT(*) FILTER (WHERE org_id = v_old OR org_id IS NULL) AS move_cnt,
                   COUNT(*) FILTER (WHERE org_id = v_new) AS new_cnt
            FROM ordermgmt.customer
            WHERE contactno IS NOT NULL
              AND (org_id IN (v_old, v_new) OR org_id IS NULL)
            GROUP BY contactno
        ) t
        WHERE t.move_cnt > 0 AND t.new_cnt > 0
    ) THEN
        RAISE EXCEPTION 'Org merge blocked: duplicate contactno would exist in target org after move';
    END IF;

    -- Fail fast if lookup/master data would violate canonical unique constraints.
    IF EXISTS (
        SELECT 1
        FROM ordermgmt.user_role
        GROUP BY rolename
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Schema align blocked: duplicate user_role.rolename found';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ordermgmt.order_status_lookup
        GROUP BY statusname
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Schema align blocked: duplicate order_status_lookup.statusname found';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ordermgmt.organization
        GROUP BY subdomain
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Schema align blocked: duplicate organization.subdomain found';
    END IF;
END $$;


-- -----------------------------------------------------------------------------
-- 2) Move data to target org and backfill NULL org_id (required for NOT NULL)
-- -----------------------------------------------------------------------------
UPDATE ordermgmt.app_user
SET org_id = 'a7e1b9d2-88fe-4d03-9bfd-120f39c48214'
WHERE org_id = '00000000-0000-0000-0000-000000000001'
   OR org_id IS NULL;

UPDATE ordermgmt.customer
SET org_id = 'a7e1b9d2-88fe-4d03-9bfd-120f39c48214'
WHERE org_id = '00000000-0000-0000-0000-000000000001'
   OR org_id IS NULL;

UPDATE ordermgmt.inventory_item
SET org_id = 'a7e1b9d2-88fe-4d03-9bfd-120f39c48214'
WHERE org_id = '00000000-0000-0000-0000-000000000001'
   OR org_id IS NULL;

UPDATE ordermgmt.orders
SET org_id = 'a7e1b9d2-88fe-4d03-9bfd-120f39c48214'
WHERE org_id = '00000000-0000-0000-0000-000000000001'
   OR org_id IS NULL;

UPDATE ordermgmt.order_item
SET org_id = 'a7e1b9d2-88fe-4d03-9bfd-120f39c48214'
WHERE org_id = '00000000-0000-0000-0000-000000000001'
   OR org_id IS NULL;

UPDATE ordermgmt.email_log
SET org_id = 'a7e1b9d2-88fe-4d03-9bfd-120f39c48214'
WHERE org_id = '00000000-0000-0000-0000-000000000001'
   OR org_id IS NULL;

-- Keep source org as inactive archive (no data-loss of organization row itself)
UPDATE ordermgmt.organization
SET isactive = FALSE,
    updatedtimestamp = CURRENT_TIMESTAMP,
    updatedby = 'SYSTEM',
    description = COALESCE(description, '')
WHERE org_id = '00000000-0000-0000-0000-000000000001';


-- -----------------------------------------------------------------------------
-- 3) Timestamp normalization + tenant org_id hardening
-- -----------------------------------------------------------------------------
ALTER TABLE ordermgmt.organization
    ALTER COLUMN org_id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone;

ALTER TABLE ordermgmt.user_role
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone;

ALTER TABLE ordermgmt.order_status_lookup
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone;

ALTER TABLE ordermgmt.app_user
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE ordermgmt.customer
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE ordermgmt.inventory_item
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE ordermgmt.pricing_catalog
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone;

ALTER TABLE ordermgmt.pricing_history
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone;

ALTER TABLE ordermgmt.orders
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE ordermgmt.order_item
    ALTER COLUMN createdtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN createdtimestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN createdtimestamp SET NOT NULL,
    ALTER COLUMN updatedtimestamp TYPE timestamp(6) without time zone,
    ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE ordermgmt.email_log
    ALTER COLUMN sentat TYPE timestamp(6) without time zone,
    ALTER COLUMN org_id SET NOT NULL;

-- -----------------------------------------------------------------------------
-- 4) Normalize PK names to clean-schema convention
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
    v_current text;
BEGIN
    FOR r IN
        SELECT *
        FROM (VALUES
            ('organization', 'pk_organization'),
            ('user_role', 'pk_user_role'),
            ('order_status_lookup', 'pk_order_status_lookup'),
            ('app_user', 'pk_app_user'),
            ('customer', 'pk_customer'),
            ('inventory_item', 'pk_inventory_item'),
            ('pricing_catalog', 'pk_pricing_catalog'),
            ('pricing_history', 'pk_pricing_history'),
            ('orders', 'pk_orders'),
            ('order_item', 'pk_order_item'),
            ('email_log', 'pk_email_log')
        ) AS t(table_name, target_name)
    LOOP
        SELECT c.conname
        INTO v_current
        FROM pg_constraint c
        JOIN pg_class cl ON cl.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = cl.relnamespace
        WHERE n.nspname = 'ordermgmt'
          AND cl.relname = r.table_name
          AND c.contype = 'p';

        IF v_current IS NOT NULL AND v_current <> r.target_name THEN
            EXECUTE format(
                'ALTER TABLE ordermgmt.%I RENAME CONSTRAINT %I TO %I',
                r.table_name,
                v_current,
                r.target_name
            );
        END IF;
    END LOOP;
END $$;


-- -----------------------------------------------------------------------------
-- 5) Drop redundant FK constraints and recreate once with canonical names/actions
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT cl.relname AS table_name, c.conname
        FROM pg_constraint c
        JOIN pg_class cl ON cl.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = cl.relnamespace
        WHERE n.nspname = 'ordermgmt'
          AND c.contype = 'f'
          AND cl.relname IN (
                'app_user',
                'customer',
                'email_log',
                'inventory_item',
                'orders',
                'order_item',
                'pricing_catalog',
                'pricing_history'
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE ordermgmt.%I DROP CONSTRAINT IF EXISTS %I',
            r.table_name,
            r.conname
        );
    END LOOP;
END $$;

ALTER TABLE ONLY ordermgmt.app_user
    ADD CONSTRAINT fk_app_user_role
    FOREIGN KEY (roleid) REFERENCES ordermgmt.user_role(roleid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.app_user
    ADD CONSTRAINT fk_app_user_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.customer
    ADD CONSTRAINT fk_customer_user
    FOREIGN KEY (userid) REFERENCES ordermgmt.app_user(userid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.customer
    ADD CONSTRAINT fk_customer_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.email_log
    ADD CONSTRAINT fk_email_log_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.inventory_item
    ADD CONSTRAINT fk_inventory_item_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.orders
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customerid) REFERENCES ordermgmt.customer(customerid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.orders
    ADD CONSTRAINT fk_orders_status
    FOREIGN KEY (statusid) REFERENCES ordermgmt.order_status_lookup(statusid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.orders
    ADD CONSTRAINT fk_orders_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.order_item
    ADD CONSTRAINT fk_order_item_order
    FOREIGN KEY (orderid) REFERENCES ordermgmt.orders(orderid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.order_item
    ADD CONSTRAINT fk_order_item_inventory
    FOREIGN KEY (itemid) REFERENCES ordermgmt.inventory_item(itemid)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.order_item
    ADD CONSTRAINT fk_order_item_org
    FOREIGN KEY (org_id) REFERENCES ordermgmt.organization(org_id)
    ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY ordermgmt.pricing_catalog
    ADD CONSTRAINT fk_pricing_catalog_item
    FOREIGN KEY (itemid) REFERENCES ordermgmt.inventory_item(itemid)
    ON UPDATE CASCADE ON DELETE RESTRICT;
-- -----------------------------------------------------------------------------
-- 6) Unique constraints/indexes de-duplication and normalization
-- -----------------------------------------------------------------------------
-- 6.1 Drop global unique constraints from APP_USER(email) and CUSTOMER(contactno)
DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'ordermgmt'
          AND t.relname = 'app_user'
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) = 'UNIQUE (email)'
    LOOP
        EXECUTE format('ALTER TABLE ordermgmt.app_user DROP CONSTRAINT IF EXISTS %I', rec.conname);
    END LOOP;

    FOR rec IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'ordermgmt'
          AND t.relname = 'customer'
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) = 'UNIQUE (contactno)'
    LOOP
        EXECUTE format('ALTER TABLE ordermgmt.customer DROP CONSTRAINT IF EXISTS %I', rec.conname);
    END LOOP;
END $$;

-- 6.2 Drop all unique constraints on lookup/master tables and add canonical names once.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT cl.relname AS table_name, c.conname
        FROM pg_constraint c
        JOIN pg_class cl ON cl.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = cl.relnamespace
        WHERE n.nspname = 'ordermgmt'
          AND c.contype = 'u'
          AND cl.relname IN ('user_role', 'order_status_lookup', 'organization')
    LOOP
        EXECUTE format(
            'ALTER TABLE ordermgmt.%I DROP CONSTRAINT IF EXISTS %I',
            r.table_name,
            r.conname
        );
    END LOOP;
END $$;

-- 6.3 Drop non-constraint unique indexes (legacy/redundant) on affected tables.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT idx.relname AS index_name
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_class tbl ON tbl.oid = i.indrelid
        JOIN pg_namespace n ON n.oid = tbl.relnamespace
        LEFT JOIN pg_constraint c ON c.conindid = i.indexrelid
        WHERE n.nspname = 'ordermgmt'
          AND tbl.relname IN ('user_role', 'order_status_lookup', 'organization', 'app_user', 'customer')
          AND i.indisunique
          AND c.oid IS NULL
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS ordermgmt.%I', r.index_name);
    END LOOP;
END $$;

ALTER TABLE ONLY ordermgmt.user_role
    ADD CONSTRAINT uq_user_role_rolename UNIQUE (rolename);

ALTER TABLE ONLY ordermgmt.order_status_lookup
    ADD CONSTRAINT uq_order_status_lookup_statusname UNIQUE (statusname);

ALTER TABLE ONLY ordermgmt.organization
    ADD CONSTRAINT uq_organization_subdomain UNIQUE (subdomain);

CREATE UNIQUE INDEX uq_app_user_email_org
    ON ordermgmt.app_user USING btree (lower((email)::text), org_id);

CREATE UNIQUE INDEX uq_customer_contactno_org
    ON ordermgmt.customer USING btree (contactno, org_id)
    WHERE (contactno IS NOT NULL);


-- -----------------------------------------------------------------------------
-- 7) Check constraint normalization
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class cl ON cl.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = cl.relnamespace
        WHERE n.nspname = 'ordermgmt'
          AND cl.relname = 'inventory_item'
          AND c.contype = 'c'
          AND (
              pg_get_constraintdef(c.oid) ILIKE '%availablestock%'
              OR pg_get_constraintdef(c.oid) ILIKE '%reservedstock%'
          )
    LOOP
        EXECUTE format('ALTER TABLE ordermgmt.inventory_item DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE ONLY ordermgmt.inventory_item
    ADD CONSTRAINT ck_inventory_item_availablestock_nonnegative CHECK (availablestock >= 0);

ALTER TABLE ONLY ordermgmt.inventory_item
    ADD CONSTRAINT ck_inventory_item_reservedstock_nonnegative CHECK (reservedstock >= 0);


-- -----------------------------------------------------------------------------
-- 8) Function + trigger normalization for pricing_history immutability
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ordermgmt.prevent_pricing_history_modification()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'PRICING_HISTORY is immutable. UPDATE and DELETE operations are not permitted.';
    RETURN NULL;
END;
$$;

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT tgname
        FROM pg_trigger
        WHERE tgrelid = 'ordermgmt.pricing_history'::regclass
          AND NOT tgisinternal
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS %I ON ordermgmt.pricing_history', r.tgname);
    END LOOP;
END $$;

CREATE TRIGGER trg_pricing_history_no_update
    BEFORE UPDATE ON ordermgmt.pricing_history
    FOR EACH ROW
    EXECUTE FUNCTION ordermgmt.prevent_pricing_history_modification();

CREATE TRIGGER trg_pricing_history_no_delete
    BEFORE DELETE ON ordermgmt.pricing_history
    FOR EACH ROW
    EXECUTE FUNCTION ordermgmt.prevent_pricing_history_modification();


-- -----------------------------------------------------------------------------
-- 9) Ensure clean-schema index set exists
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_app_user_org_id ON ordermgmt.app_user USING btree (org_id);
CREATE INDEX IF NOT EXISTS idx_app_user_roleid ON ordermgmt.app_user USING btree (roleid);

CREATE INDEX IF NOT EXISTS idx_customer_org_id ON ordermgmt.customer USING btree (org_id);
CREATE INDEX IF NOT EXISTS idx_customer_userid ON ordermgmt.customer USING btree (userid);

CREATE INDEX IF NOT EXISTS idx_email_log_org_id ON ordermgmt.email_log USING btree (org_id);
CREATE INDEX IF NOT EXISTS idx_email_log_sentat ON ordermgmt.email_log USING btree (sentat);
CREATE INDEX IF NOT EXISTS idx_email_log_status ON ordermgmt.email_log USING btree (status);

CREATE INDEX IF NOT EXISTS idx_inventory_item_org_id ON ordermgmt.inventory_item USING btree (org_id);

CREATE INDEX IF NOT EXISTS idx_order_item_orderid ON ordermgmt.order_item USING btree (orderid);
CREATE INDEX IF NOT EXISTS idx_order_item_org_id ON ordermgmt.order_item USING btree (org_id);

CREATE INDEX IF NOT EXISTS idx_orders_created ON ordermgmt.orders USING btree (createdtimestamp);
CREATE INDEX IF NOT EXISTS idx_orders_customerid ON ordermgmt.orders USING btree (customerid);
CREATE INDEX IF NOT EXISTS idx_orders_org_id ON ordermgmt.orders USING btree (org_id);
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON ordermgmt.orders USING btree (statusid, createdtimestamp);
CREATE INDEX IF NOT EXISTS idx_orders_statusid ON ordermgmt.orders USING btree (statusid);

CREATE INDEX IF NOT EXISTS idx_pricing_history_created ON ordermgmt.pricing_history USING btree (createdtimestamp);
CREATE INDEX IF NOT EXISTS idx_pricing_history_item_created ON ordermgmt.pricing_history USING btree (itemid, createdtimestamp DESC);
CREATE INDEX IF NOT EXISTS idx_pricing_history_itemid ON ordermgmt.pricing_history USING btree (itemid);


-- -----------------------------------------------------------------------------
-- 10) Post-migration safety checks
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    v_old CONSTANT uuid := '00000000-0000-0000-0000-000000000001';
BEGIN
    IF EXISTS (SELECT 1 FROM ordermgmt.app_user WHERE org_id = v_old)
       OR EXISTS (SELECT 1 FROM ordermgmt.customer WHERE org_id = v_old)
       OR EXISTS (SELECT 1 FROM ordermgmt.inventory_item WHERE org_id = v_old)
       OR EXISTS (SELECT 1 FROM ordermgmt.orders WHERE org_id = v_old)
       OR EXISTS (SELECT 1 FROM ordermgmt.order_item WHERE org_id = v_old)
       OR EXISTS (SELECT 1 FROM ordermgmt.email_log WHERE org_id = v_old) THEN
        RAISE EXCEPTION 'Post-check failed: source org_id still referenced in tenant tables';
    END IF;

    IF EXISTS (SELECT 1 FROM ordermgmt.app_user WHERE org_id IS NULL)
       OR EXISTS (SELECT 1 FROM ordermgmt.customer WHERE org_id IS NULL)
       OR EXISTS (SELECT 1 FROM ordermgmt.inventory_item WHERE org_id IS NULL)
       OR EXISTS (SELECT 1 FROM ordermgmt.orders WHERE org_id IS NULL)
       OR EXISTS (SELECT 1 FROM ordermgmt.order_item WHERE org_id IS NULL)
       OR EXISTS (SELECT 1 FROM ordermgmt.email_log WHERE org_id IS NULL) THEN
        RAISE EXCEPTION 'Post-check failed: NULL org_id remains in tenant tables';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ordermgmt.app_user
        GROUP BY lower(email), org_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Post-check failed: duplicate (lower(email), org_id) exists';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ordermgmt.customer
        WHERE contactno IS NOT NULL
        GROUP BY contactno, org_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Post-check failed: duplicate (contactno, org_id) exists';
    END IF;

    IF (
        SELECT COUNT(*)
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        WHERE n.nspname = 'ordermgmt'
          AND c.contype = 'f'
          AND c.conname IN (
              'fk_app_user_role',
              'fk_app_user_org',
              'fk_customer_user',
              'fk_customer_org',
              'fk_email_log_org',
              'fk_inventory_item_org',
              'fk_orders_customer',
              'fk_orders_status',
              'fk_orders_org',
              'fk_order_item_order',
              'fk_order_item_inventory',
              'fk_order_item_org',
              'fk_pricing_catalog_item',
              'fk_pricing_history_item'
          )
    ) <> 14 THEN
        RAISE EXCEPTION 'Post-check failed: canonical FK set is incomplete';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_namespace n ON n.oid = c.connamespace
        WHERE n.nspname = 'ordermgmt'
          AND c.contype = 'f'
          AND c.conname IN (
              'fk_app_user_role',
              'fk_app_user_org',
              'fk_customer_user',
              'fk_customer_org',
              'fk_email_log_org',
              'fk_inventory_item_org',
              'fk_orders_customer',
              'fk_orders_status',
              'fk_orders_org',
              'fk_order_item_order',
              'fk_order_item_inventory',
              'fk_order_item_org',
              'fk_pricing_catalog_item',
              'fk_pricing_history_item'
          )
          AND c.confdeltype <> 'r' -- RESTRICT
    ) THEN
        RAISE EXCEPTION 'Post-check failed: one or more canonical FKs are not ON DELETE RESTRICT';
    END IF;
END $$;
