-- =============================================================================
--  MULTI-TENANCY BOOTSTRAP (ADDITIVE / NON-BREAKING)
--  - Adds organization model and org discriminator columns
--  - Seeds SUPER_ADMIN and ORG_ADMIN roles/users
--  - Links all existing data to organization: test
-- =============================================================================

SET search_path TO ordermgmt;

-- 1) Organization master table (new)
CREATE TABLE IF NOT EXISTS ORGANIZATION (
    org_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    isactive BOOLEAN NOT NULL DEFAULT TRUE,
    createdtimestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedtimestamp TIMESTAMP,
    createdby VARCHAR(255),
    updatedby VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_organization_subdomain ON ORGANIZATION (subdomain);

-- 2) Add new roles required for tenant administration
INSERT INTO USER_ROLE (roleid, rolename, createdtimestamp, createdby)
VALUES
    (3, 'SUPER_ADMIN', CURRENT_TIMESTAMP, 'SYSTEM'),
    (4, 'ORG_ADMIN', CURRENT_TIMESTAMP, 'SYSTEM')
ON CONFLICT (roleid) DO NOTHING;

-- 3) Add org_id discriminator columns to existing tables (nullable for backward compatibility)
ALTER TABLE APP_USER ADD COLUMN IF NOT EXISTS org_id UUID;
ALTER TABLE CUSTOMER ADD COLUMN IF NOT EXISTS org_id UUID;
ALTER TABLE INVENTORY_ITEM ADD COLUMN IF NOT EXISTS org_id UUID;
ALTER TABLE ORDERS ADD COLUMN IF NOT EXISTS org_id UUID;
ALTER TABLE ORDER_ITEM ADD COLUMN IF NOT EXISTS org_id UUID;
ALTER TABLE EMAIL_LOG ADD COLUMN IF NOT EXISTS org_id UUID;

-- 4) Seed SUPER_ADMIN user first
INSERT INTO APP_USER (
    userid,
    email,
    passwordhash,
    roleid,
    org_id,
    isactive,
    ispasswordchanged,
    createdtimestamp,
    createdby
)
SELECT
    '00000000-0000-0000-0000-000000000002',
    'superadmin@superemail.com',
    '$2y$10$xqB/IbPkfb6uulzzoBJENeaiLxJ.iHE7S0zSVyUvPK8FiqtWDSXP.',
    ur.roleid,
    NULL,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    'SYSTEM'
FROM USER_ROLE ur
WHERE ur.rolename = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM APP_USER au WHERE au.email = 'superadmin@superemail.com'
  );

-- 5) Create bootstrap organization 'test'
INSERT INTO ORGANIZATION (
    org_id,
    name,
    subdomain,
    description,
    isactive,
    createdtimestamp,
    createdby
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'test',
    'test',
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit.',
    TRUE,
    CURRENT_TIMESTAMP,
    'SYSTEM'
)
ON CONFLICT (org_id) DO NOTHING;

-- Link SUPER_ADMIN to test organization
UPDATE APP_USER
SET org_id = '00000000-0000-0000-0000-000000000001'
WHERE email = 'superadmin@superemail.com'
  AND org_id IS NULL;

-- 6) Seed ORG_ADMIN user for test organization
INSERT INTO APP_USER (
    userid,
    email,
    passwordhash,
    roleid,
    org_id,
    isactive,
    ispasswordchanged,
    createdtimestamp,
    createdby
)
SELECT
    '00000000-0000-0000-0000-000000000003',
    'orgadmin@test.com',
    '$2y$10$Oa6dKJKBwRoBaklZZAlzieIxVTQtU/gZPxAhlz7IxI9uECXeUH2Nm',
    ur.roleid,
    '00000000-0000-0000-0000-000000000001',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    'SYSTEM'
FROM USER_ROLE ur
WHERE ur.rolename = 'ORG_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM APP_USER au WHERE au.email = 'orgadmin@test.com'
  );

-- 7) Link all existing data to organization 'test'
UPDATE APP_USER
SET org_id = '00000000-0000-0000-0000-000000000001'
WHERE org_id IS NULL;

UPDATE CUSTOMER c
SET org_id = u.org_id
FROM APP_USER u
WHERE c.userid = u.userid
  AND c.org_id IS NULL;

UPDATE INVENTORY_ITEM
SET org_id = '00000000-0000-0000-0000-000000000001'
WHERE org_id IS NULL;

UPDATE ORDERS o
SET org_id = c.org_id
FROM CUSTOMER c
WHERE o.customerid = c.customerid
  AND o.org_id IS NULL;

UPDATE ORDER_ITEM oi
SET org_id = o.org_id
FROM ORDERS o
WHERE oi.orderid = o.orderid
  AND oi.org_id IS NULL;

UPDATE EMAIL_LOG
SET org_id = '00000000-0000-0000-0000-000000000001'
WHERE org_id IS NULL;

-- 8) Foreign keys + indexes (kept non-breaking with nullable org_id)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_app_user_org') THEN
        ALTER TABLE APP_USER
            ADD CONSTRAINT fk_app_user_org
                FOREIGN KEY (org_id) REFERENCES ORGANIZATION (org_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_customer_org') THEN
        ALTER TABLE CUSTOMER
            ADD CONSTRAINT fk_customer_org
                FOREIGN KEY (org_id) REFERENCES ORGANIZATION (org_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_item_org') THEN
        ALTER TABLE INVENTORY_ITEM
            ADD CONSTRAINT fk_inventory_item_org
                FOREIGN KEY (org_id) REFERENCES ORGANIZATION (org_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_org') THEN
        ALTER TABLE ORDERS
            ADD CONSTRAINT fk_orders_org
                FOREIGN KEY (org_id) REFERENCES ORGANIZATION (org_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_item_org') THEN
        ALTER TABLE ORDER_ITEM
            ADD CONSTRAINT fk_order_item_org
                FOREIGN KEY (org_id) REFERENCES ORGANIZATION (org_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_email_log_org') THEN
        ALTER TABLE EMAIL_LOG
            ADD CONSTRAINT fk_email_log_org
                FOREIGN KEY (org_id) REFERENCES ORGANIZATION (org_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_app_user_org_id ON APP_USER (org_id);
CREATE INDEX IF NOT EXISTS idx_customer_org_id ON CUSTOMER (org_id);
CREATE INDEX IF NOT EXISTS idx_inventory_item_org_id ON INVENTORY_ITEM (org_id);
CREATE INDEX IF NOT EXISTS idx_orders_org_id ON ORDERS (org_id);
CREATE INDEX IF NOT EXISTS idx_order_item_org_id ON ORDER_ITEM (org_id);
CREATE INDEX IF NOT EXISTS idx_email_log_org_id ON EMAIL_LOG (org_id);
