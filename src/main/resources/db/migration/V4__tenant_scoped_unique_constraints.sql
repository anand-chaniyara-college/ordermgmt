-- =============================================================================
-- TENANT-SCOPED UNIQUE CONSTRAINTS
-- - Replace global uniqueness with tenant-level uniqueness
-- - APP_USER email:      UNIQUE(org_id, lower(email))
-- - CUSTOMER contactno:  UNIQUE(org_id, contactno) WHERE contactno IS NOT NULL
-- =============================================================================

SET search_path TO ordermgmt;

-- 1) Safety checks: org_id must be present for tenant-owned identity keys
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM APP_USER WHERE org_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot apply tenant-scoped email uniqueness: APP_USER has rows with NULL org_id';
    END IF;

    IF EXISTS (SELECT 1 FROM CUSTOMER WHERE contactno IS NOT NULL AND org_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot apply tenant-scoped contact uniqueness: CUSTOMER has contactno rows with NULL org_id';
    END IF;
END $$;

-- 2) Safety checks: data must already satisfy the new tenant-scoped uniqueness
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM APP_USER
        GROUP BY org_id, lower(email)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot apply tenant-scoped email uniqueness: duplicate (org_id, lower(email)) exists';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM CUSTOMER
        WHERE contactno IS NOT NULL
        GROUP BY org_id, contactno
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot apply tenant-scoped contact uniqueness: duplicate (org_id, contactno) exists';
    END IF;
END $$;

-- 3) Drop global unique constraints on APP_USER(email) and CUSTOMER(contactno)
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
        EXECUTE format('ALTER TABLE APP_USER DROP CONSTRAINT IF EXISTS %I', rec.conname);
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
        EXECUTE format('ALTER TABLE CUSTOMER DROP CONSTRAINT IF EXISTS %I', rec.conname);
    END LOOP;
END $$;

-- 4) Drop legacy named unique indexes if present
DROP INDEX IF EXISTS uq_app_user_email;
DROP INDEX IF EXISTS uq_customer_contactno;

-- 5) Create tenant-scoped unique indexes
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_org_email_ci
    ON APP_USER (org_id, lower(email));

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_org_contactno
    ON CUSTOMER (org_id, contactno)
    WHERE contactno IS NOT NULL;
