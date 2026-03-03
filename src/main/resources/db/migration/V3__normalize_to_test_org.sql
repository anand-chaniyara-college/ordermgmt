-- =============================================================================
--  NORMALIZE TENANT DATA TO TEST ORG
--  Purpose:
--  - Keep migration-only flow
--  - Reconcile previously applied V2 states
--  - Ensure all existing data/users are linked to org 'test'
-- =============================================================================

SET search_path TO ordermgmt;

-- 1) Ensure ORGANIZATION has description column
ALTER TABLE ORGANIZATION ADD COLUMN IF NOT EXISTS description VARCHAR(1000);

-- 2) Ensure required roles exist
INSERT INTO USER_ROLE (roleid, rolename, createdtimestamp, createdby)
VALUES
    (3, 'SUPER_ADMIN', CURRENT_TIMESTAMP, 'SYSTEM'),
    (4, 'ORG_ADMIN', CURRENT_TIMESTAMP, 'SYSTEM')
ON CONFLICT (roleid) DO NOTHING;

-- 3) Ensure bootstrap organization 'test' exists
-- Support both shapes:
--   A) ORGANIZATION has plan_id NOT NULL (older applied V2)
--   B) ORGANIZATION has no plan_id column
DO $$
DECLARE
    has_plan_id BOOLEAN;
    resolved_plan_id UUID;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'ordermgmt'
          AND table_name = 'organization'
          AND column_name = 'plan_id'
    ) INTO has_plan_id;

    IF has_plan_id THEN
        SELECT o.plan_id
        INTO resolved_plan_id
        FROM ORGANIZATION o
        WHERE o.plan_id IS NOT NULL
        ORDER BY o.createdtimestamp
        LIMIT 1;

        INSERT INTO ORGANIZATION (
            org_id,
            name,
            subdomain,
            description,
            isactive,
            plan_id,
            createdtimestamp,
            createdby
        )
        VALUES (
            '00000000-0000-0000-0000-000000000001',
            'test',
            'test',
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit.',
            TRUE,
            resolved_plan_id,
            CURRENT_TIMESTAMP,
            'SYSTEM'
        )
        ON CONFLICT (org_id) DO UPDATE
        SET
            name = EXCLUDED.name,
            subdomain = EXCLUDED.subdomain,
            description = EXCLUDED.description,
            isactive = TRUE,
            plan_id = COALESCE(ORGANIZATION.plan_id, EXCLUDED.plan_id),
            updatedtimestamp = CURRENT_TIMESTAMP,
            updatedby = 'SYSTEM';
    ELSE
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
        ON CONFLICT (org_id) DO UPDATE
        SET
            name = EXCLUDED.name,
            subdomain = EXCLUDED.subdomain,
            description = EXCLUDED.description,
            isactive = TRUE,
            updatedtimestamp = CURRENT_TIMESTAMP,
            updatedby = 'SYSTEM';
    END IF;
END $$;

-- 4) Ensure org_id exists on ORDER_ITEM (was missing in previously applied V2)
ALTER TABLE ORDER_ITEM ADD COLUMN IF NOT EXISTS org_id UUID;

-- 5) Seed SUPER_ADMIN user (requested credentials)
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
    '00000000-0000-0000-0000-000000000001',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    'SYSTEM'
FROM USER_ROLE ur
WHERE ur.rolename = 'SUPER_ADMIN'
ON CONFLICT (email) DO NOTHING;

-- 6) Seed ORG_ADMIN for test org
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
ON CONFLICT (email) DO NOTHING;

-- 7) Enforce requested data normalization: all existing data -> org 'test'
UPDATE APP_USER
SET org_id = '00000000-0000-0000-0000-000000000001';

UPDATE CUSTOMER
SET org_id = '00000000-0000-0000-0000-000000000001';

UPDATE INVENTORY_ITEM
SET org_id = '00000000-0000-0000-0000-000000000001';

UPDATE ORDERS
SET org_id = '00000000-0000-0000-0000-000000000001';

UPDATE ORDER_ITEM
SET org_id = '00000000-0000-0000-0000-000000000001';

UPDATE EMAIL_LOG
SET org_id = '00000000-0000-0000-0000-000000000001';

-- 8) Add FK + indexes safely
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_item_org') THEN
        ALTER TABLE ORDER_ITEM
            ADD CONSTRAINT fk_order_item_org
                FOREIGN KEY (org_id) REFERENCES ORGANIZATION (org_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_order_item_org_id ON ORDER_ITEM (org_id);

-- 9) Keep test org active and make platform org inactive if present
UPDATE ORGANIZATION
SET isactive = FALSE,
    updatedtimestamp = CURRENT_TIMESTAMP,
    updatedby = 'SYSTEM'
WHERE lower(subdomain) = 'platform';

UPDATE ORGANIZATION
SET isactive = TRUE,
    description = COALESCE(description, 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.'),
    updatedtimestamp = CURRENT_TIMESTAMP,
    updatedby = 'SYSTEM'
WHERE org_id = '00000000-0000-0000-0000-000000000001';
