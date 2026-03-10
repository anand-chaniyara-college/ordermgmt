# Order Management System — Multi-Tenancy Plan (Current State)
> Last Updated: 2026-03-03
> Scope: Shared-schema SaaS multi-tenancy with `org_id` discriminator and strict role boundaries.

## 1) Final Scope

### In Scope
- Shared DB + shared schema tenant model using `org_id`
- Runtime tenant isolation using Hibernate `@TenantId`
- Global platform role: `SUPER_ADMIN`
- Tenant management role: `ORG_ADMIN`
- Existing `ADMIN` and `CUSTOMER` flows kept tenant-aware

### Out of Scope
- Subscription plans
- Billing and metering

## 2) Architecture Decisions (Final)
- SUPER_ADMIN uses a runtime-only root tenant marker (not a DB organization row)
- JWT filter behavior:
  - role `SUPER_ADMIN` -> set root tenant context
  - all other roles -> require `org_id` claim
- `TenantIdentifierResolver`:
  - returns current tenant context
  - implements `isRoot(...)` for the root marker
- Root marker is never persisted into business tables (`org_id` must always be a real org UUID)
- ORG_ADMIN cross-organization actions are blocked in service layer

## 3) Database Status
- [x] Flyway integrated (`V1`, `V2`, `V3`)
- [x] Tenant bootstrap complete
- [x] `test` organization created and active
- [x] Legacy data mapped to `test`
- [x] Roles seeded: `SUPER_ADMIN`, `ORG_ADMIN`
- [x] Seed users:
  - `superadmin@superemail.com` (SUPER_ADMIN)
  - `orgadmin@test.com` (ORG_ADMIN)
- [x] Tenant-owned tables have non-null `org_id`

## 4) Code Status

### Tenant Infrastructure
- [x] `TenantContextHolder`
- [x] `TenantIdentifierResolver` with root handling
- [x] Hibernate tenant resolver wiring
- [x] JWT handling updated for super-admin/global context

### Domain & APIs
- [x] `Organization` entity + repository
- [x] `@TenantId` added to tenant-owned entities
- [x] SUPER_ADMIN APIs implemented:
  - create/list organizations
  - create/list org-admins
  - org-admin status update
- [x] ORG_ADMIN APIs implemented:
  - create/list admins
  - admin status update
  - send monthly report email

### Functional Hardening (Completed)
- [x] Custom `@Query` audit done for tenant safety
- [x] Unsafe native query removed (`InventoryItemRepository.updateItemId`)
- [x] Duplicate org subdomain / admin-email conflict now mapped to HTTP `409`
- [x] `/api/admin/analytics/sendreportemail` removed
- [x] Report email flow standardized to org-admin endpoint only:
  - `POST /api/org-admin/analytics/sendreportemail`

## 5) Endpoint Matrix (Current)

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### SUPER_ADMIN
- `POST /api/super-admin/organizations`
- `GET /api/super-admin/organizations`
- `POST /api/super-admin/org-admins`
- `GET /api/super-admin/org-admins`
- `PATCH /api/super-admin/org-admins/{id}/status`

### ORG_ADMIN
- `POST /api/org-admin/admins`
- `GET /api/org-admin/admins`
- `PATCH /api/org-admin/admins/{id}/status`
- `POST /api/org-admin/analytics/sendreportemail`

### ADMIN
- Existing `/api/admin/**` flows remain, except removed endpoint:
  - removed: `POST /api/admin/analytics/sendreportemail`

## 6) What Is Left
- [ ] Integration tests for SUPER_ADMIN org/org-admin flows
- [ ] Integration tests for ORG_ADMIN admin-management flows
- [ ] Register endpoint matrix tests (`403`, `404`, `409`, success)
- [ ] Cross-tenant leakage tests
- [ ] Role-access `403` tests
- [ ] Keep API docs/examples synchronized with runtime behavior

## 7) Acceptance Criteria
- No cross-tenant leakage in reads/writes for org-scoped users
- SUPER_ADMIN can manage organizations without tenant mismatch errors
- `org_id` claim is required for non-super-admin tokens
- Root marker remains runtime-only and never persisted into tenant business data
- Flyway `info` / `validate` / `migrate` pass cleanly
