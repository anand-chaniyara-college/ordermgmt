# Integration Test Execution Plan

> Run files **in sequence** — each file depends on data/users created by previous files.

---

## File 1 — `AuthIntegrationTest.java`

| Order | Test Category         | Scenario                                                       |
|------:|-----------------------|----------------------------------------------------------------|
|   1–2 | Super Admin Login     | Negative (Wrong Pass / Wrong Subdomain) & Success              |
|   3–4 | Org Provisioning      | Negative (No Token) & Success                                 |
|     5 | Org Admin Setup       | Success Creation                                               |
|     6 | Security / Logout     | Token Blacklisting Verification                                |
|     7 | Org Admin Login       | Success & Forbidden Access (Super Admin API)                   |
|     8 | Admin Provisioning    | Success Subgroup Admin Creation                                |
|     9 | Public Register       | Success, Conflict (Duplicate), and Forbidden (Role)            |
|    10 | Logout                | Org Admin Session Termination                                  |
|    11 | Org Lifecycle         | Deactivate → Login Fail → Reactivate → Login Success           |
|    12 | Token Refresh         | Negative (Invalid / Revoked Token)                             |
|    13 | Account Lifecycle     | Deactivate User → Login Fail → Refresh Fail                   |

---

## File 2 — `OrderLifecycleIntegrationTest.java`

| Order | Test Category               | Scenario                                                                  |
|------:|-----------------------------|---------------------------------------------------------------------------|
|   1–3 | Setup (Admin)               | Login ADMIN → Create Inventory Item A (stock=50) → Set Price (25.00)      |
|   4–5 | Setup (Customer)            | Login CUSTOMER, complete profile → Place order 5× Item A (PENDING)        |
|   6–7 | Order & Stock Verification  | Admin GET confirms PENDING order → Verify reservedStock increased by 5    |
|     8 | Bulk Transition (Happy)     | PENDING → CONFIRMED (success)                                            |
|     9 | Bulk Transition (Negative)  | CONFIRMED → DELIVERED — skip steps → failure in bulk result               |
|    10 | Bulk Transition (Happy)     | CONFIRMED → PROCESSING (success)                                         |
|    11 | Bulk Transition (Negative)  | PROCESSING → PENDING — backward → failure in bulk result                  |
|    12 | Bulk Transition (Happy)     | PROCESSING → SHIPPED (success)                                           |
|    13 | Bulk Transition (Negative)  | SHIPPED → CANCELLED → failure in bulk result                              |
|    14 | Bulk Transition (Happy)     | SHIPPED → DELIVERED (success — lifecycle complete)                        |
|    15 | Bulk Transition (Negative)  | DELIVERED → CONFIRMED — terminal state → failure in bulk result           |
|    16 | Timestamp Verification      | Confirm createdTimestamp & updatedTimestamp on DELIVERED order             |
|    17 | Stock Finalization          | availableStock remains reduced from placement, reservedStock released after DELIVERED |
| 18–19 | Logout                      | Logout CUSTOMER → Logout ADMIN                                           |

---

## File 3 — `OrderCancellationIntegrationTest.java`

| Order | Test Category                    | Scenario                                                              |
|------:|----------------------------------|-----------------------------------------------------------------------|
|     1 | Setup (Admin)                    | Login ADMIN, create Item B (stock=30), set price (15.00)              |
|     2 | Setup (Customer + Delivered)     | Login CUSTOMER, place order 3× Item B, drive PENDING→DELIVERED        |
|     3 | Wrong-State Cancel (Negative)    | Customer cancel DELIVERED order → 400 "Invalid Status Transition"     |
|     4 | Place New Order                  | Customer places new order 4× Item B → PENDING                        |
|     5 | Cancel Pending (Happy)           | Customer cancels PENDING order → 200 CANCELLED                       |
|     6 | Stock Release Verification       | Verify reservedStock returned to pre-order level                      |
|     7 | Logout                           | Logout CUSTOMER                                                       |
|     8 | Admin Cancel Delivered (Negative) | Admin bulk cancel DELIVERED order → failure in bulk result            |
|     9 | Logout                           | Logout ADMIN                                                          |

---

## File 4 — `StockExhaustionIntegrationTest.java`

| Order | Test Category              | Scenario                                                             |
|------:|----------------------------|----------------------------------------------------------------------|
|     1 | Setup (Admin)              | Login ADMIN, create Item C (stock=10), set price (100.00)            |
|     2 | Setup (Customer)           | Login CUSTOMER, complete profile                                      |
|     3 | Stock Exhaustion           | Customer orders 10× Item C → all stock reserved (PENDING)            |
|     4 | Oversell (Negative)        | Customer orders 1× Item C → 400 "Insufficient Stock"                 |
|     5 | Logout                     | Logout CUSTOMER                                                       |
|     6 | Admin Restock              | Admin adds 20 units to Item C → verify new stock level               |
|     7 | Logout                     | Logout ADMIN                                                          |
|     8 | Recovery (Happy)           | Customer logs in again → orders 2× Item C → 201 success              |
|     9 | Logout                     | Logout CUSTOMER                                                       |

---

## File 5 — `SecurityAccessControlIntegrationTest.java`

| Order | Test Category                | Scenario                                                             |
|------:|------------------------------|----------------------------------------------------------------------|
|   1–4 | No-Token Access (401)        | GET admin/orders, customer/orders, analytics, POST customer/orders   |
|     5 | Setup                        | Login CUSTOMER                                                        |
|   6–9 | Customer → Admin APIs (403)  | GET admin/orders, PUT admin/orders/status, GET admin/inventory, GET analytics |
|    10 | Setup                        | Login ADMIN                                                           |
| 11–12 | Admin → Super-Admin APIs (403) | GET super-admin/organizations, POST super-admin/org-admins          |
| 13–14 | Invalid UUID (400)           | Invalid UUID in inventory lookup, invalid UUID in order lookup        |
|    15 | Non-Existent UUID (404)      | Random valid UUID in order lookup → "Order Not Found"                |
| 16–17 | Blacklisted Token (401)      | Logout CUSTOMER → reuse token → 401                                  |
| 18–19 | Blacklisted Token (401)      | Logout ADMIN → reuse token → 401                                     |
|    20 | Fabricated Token (401)       | Fake JWT on customer and admin endpoints → 401                        |

---

## File 6 — `AnalyticsIntegrationTest.java`

| Order | Test Category                 | Scenario                                                              |
|------:|-------------------------------|-----------------------------------------------------------------------|
|     1 | Setup (Admin)                 | Login ADMIN, create Item D (stock=100), set price (50.00)             |
|     2 | Setup (Data)                  | Customer places 3× Item D, Admin drives to DELIVERED, both logout     |
|     3 | Login ORG_ADMIN               | Login with ORG_ADMIN credentials                                      |
|     4 | Revenue Report (Happy)        | GET revenue-report with 30-day range → verify aggregates              |
|     5 | Order Analytics (Filter)      | GET order-analytics with itemName="Analytics Item D"                  |
|     6 | Order Analytics (Filter)      | GET order-analytics with orderStatus="DELIVERED"                      |
|     7 | Order Analytics (Pagination)  | GET order-analytics with page=0, size=5                               |
|     8 | Email Dispatch                | GET revenue-report with sendEmail=true, emailTo=test address          |
|     9 | Invalid Date Range (Negative) | GET revenue-report with startDate > endDate → capture system response |
|    10 | Logout                        | Logout ORG_ADMIN                                                       |

---

## Execution Sequence Summary

```
AuthIntegrationTest          → creates org, users (SUPER_ADMIN, ORG_ADMIN, ADMIN, CUSTOMER)
    ↓
OrderLifecycleIntegrationTest    → tests full status lifecycle + illegal transitions
    ↓
OrderCancellationIntegrationTest → tests cancel flows + stock release
    ↓
StockExhaustionIntegrationTest   → tests stock limits + restock recovery
    ↓
SecurityAccessControlIntegrationTest → tests 401/403/400/404 access controls
    ↓
AnalyticsIntegrationTest         → tests ORG_ADMIN analytics + email + validation
```

> **Total: 6 files · 80 test methods · Covers happy paths, negative paths, and edge cases**
