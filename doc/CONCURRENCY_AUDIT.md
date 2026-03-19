# Concurrency Audit — Order Management System

> Author: Antigravity (AI Pair Programmer)
> Date: 2026-03-19
> Scope: Full audit of all concurrency scenarios in the current codebase —
>        what is handled, how it is handled, and what is still a gap.

---

## Table of Contents

1. [Concurrency Weapons (Mechanisms in Use)](#1-concurrency-weapons-mechanisms-in-use)
2. [All Race Condition Scenarios](#2-all-race-condition-scenarios)
   - [① Customer vs Customer — Stock Exhaustion](#-customer-vs-customer--stock-exhaustion)
   - [② Multi-Item Order Deadlock](#-multi-item-order-deadlock)
   - [③ Customer Cancel vs Admin Confirm — Same Order](#-customer-cancel-vs-admin-confirm--same-order--critical-bug)
   - [④ Scheduler vs Admin Confirm — Same Order](#-scheduler-vs-admin-confirm--same-order)
   - [⑤ Admin Price Update vs Order Creation](#-admin-price-update-vs-order-creation)
   - [⑥ Concurrent Bulk Status Update](#-concurrent-bulk-status-update)
   - [⑦ Admin Inventory Update vs Order Creation](#-admin-inventory-update-vs-order-creation)
   - [⑧ Admin Add Stock vs Order Creation](#-admin-add-stock-vs-order-creation)
   - [⑨ Admin Delete Item with Active Reservations](#-admin-delete-item-with-active-reservations)
   - [⑩ Two Admins Update Same Price Simultaneously](#-two-admins-update-same-price-simultaneously--not-handled)
   - [⑪ Duplicate Customer Registration](#-duplicate-customer-registration)
   - [⑫ Duplicate Organization Subdomain Creation](#-duplicate-organization-subdomain-creation)
   - [⑬ Token Blacklist Race](#-token-blacklist-race)
   - [⑭ Customer Profile Update from Two Devices](#-customer-profile-update-from-two-devices--not-handled)
   - [⑮ Scheduler List → Order Deleted Mid-Loop](#-scheduler-list--order-deleted-mid-loop)
3. [Master Summary Table](#3-master-summary-table)
4. [What Needs to Be Fixed (Priority Order)](#4-what-needs-to-be-fixed-priority-order)
   - [🔴 P1 — cancelOrder() Missing Lock](#-p1--cancelorder-missing-lock)
   - [🔴 P2 — No Retry Mechanism](#-p2--no-retry-mechanism-anywhere)
   - [🟡 P3 — Price Update Lock Gap](#-p3--pricingcatalog-write-not-locked)
   - [🟡 P4 — Duplicate Registration 500 vs 409](#-p4--duplicate-registrationsubdomain--500-instead-of-409)
   - [🟢 P5 — @Version on Orders Entity](#-p5--version-on-orders-entity)
5. [Evidence from Integration Tests](#5-evidence-from-integration-tests)

---

## 1. Concurrency Weapons (Mechanisms in Use)

Before scanning individual scenarios, here is every concurrency protection mechanism
that actually exists in this codebase and where it lives.

| # | Mechanism | Class / Query | What It Does |
|---|---|---|---|
| A | `SELECT … FOR UPDATE` (Pessimistic Write) | `InventoryItemRepository.findAllByItemIdInForUpdate()` | Exclusive row lock on all requested inventory items |
| B | `SELECT … FOR UPDATE` (Pessimistic Write) | `InventoryItemRepository.findByIdForUpdate()` | Exclusive row lock on a single inventory item |
| C | `SELECT … FOR UPDATE` (Pessimistic Write) | `OrdersRepository.findByIdWithLock()` | Exclusive row lock on a single order row |
| D | `SELECT … FOR UPDATE SKIP LOCKED` | `OrdersRepository.findStalePendingOrdersPriority()` | Scheduler silently skips any order locked by another transaction |
| E | `@Version` (Optimistic Locking) | `InventoryItem.version` | Database-level version stamp; stale write throws `OptimisticLockException` |
| F | Sorted Lock Acquisition | `OrderInventoryManagerImpl`, `InventoryServiceImpl` | itemIds always sorted ascending before locking → no circular wait → no deadlock |
| G | `@Transactional(propagation = REQUIRES_NEW)` | `OrderTransitionHelper.updateOrderInternal()`, `cancelStalePendingOrder()` | Each order transition runs in its own isolated transaction; one failure cannot roll back another |
| H | Status Re-check After Lock | `OrderTransitionHelper.cancelStalePendingOrder()` | After acquiring the lock, re-reads current status; exits without action if order is no longer PENDING |
| I | Immutable Price Snapshot | `PricingHistory` table (append-only) | Price captured at order-creation time from history, not from the mutable catalog |

---

## 2. All Race Condition Scenarios

---

### ① Customer vs Customer — Stock Exhaustion

```
Race: Two customers both order the last available unit of the same item simultaneously.
```

**Code Path:**
```
POST /customer/orders
  → OrderServiceImpl.createOrder()
    → OrderInventoryManagerImpl.processAndSaveOrderItems()
      → inventoryRepository.findAllByItemIdInForUpdate(itemIds)  [Mechanism A]
        → check effectiveAvailable (availableStock - reservedStock)
          → reservedStock++ → saveAndFlush()
```

**Status: ✅ FULLY HANDLED**

**How it works step by step:**

1. All requested item rows are locked with `SELECT … FOR UPDATE` in a single sorted batch
   query before any reads or writes occur.
2. The effective availability check (`availableStock - reservedStock`) happens **inside the
   lock**, so no other transaction can intervene between the check and the update.
3. `reservedStock` is incremented and flushed (`saveAndFlush`) before the lock is released.
4. The second customer's thread will block at the `FOR UPDATE` statement until the first
   transaction commits, then re-reads the now-updated `reservedStock` and will correctly
   receive an `InsufficientStockException` if stock is exhausted.
5. `@Version` on `InventoryItem` acts as a second layer — even if a pessimistic lock gap
   somehow occurred, a stale-read write would be rejected at commit time.

**Test Coverage:** `StockExhaustionIntegrationTest` (sequential), `AdminPriceUpdateVsCreationIntegrationTest`
(156 concurrent orders with zero stock integrity violations).

---

### ② Multi-Item Order Deadlock

```
Race: Customer A orders [Item-1, Item-2]. Customer B orders [Item-2, Item-1].
      Classic AB / BA lock-ordering circular wait → deadlock.
```

**Code Path:**
```
OrderInventoryManagerImpl.processAndSaveOrderItems()
  → sortedItems = items sorted by itemId ascending      [Mechanism F]
  → itemIds = sortedItems.stream().map(itemId).collect()
  → inventoryRepository.findAllByItemIdInForUpdate(itemIds)  // ORDER BY i.itemId in JPQL
```

```
InventoryServiceImpl.updateInventoryItems()
  → itemIds = sorted ascending                          [Mechanism F]
  → inventoryRepository.findAllByItemIdInForUpdate(itemIds)

InventoryServiceImpl.addStock()
  → itemIds = sorted ascending                          [Mechanism F]
  → inventoryRepository.findAllByItemIdInForUpdate(itemIds)
```

**Status: ✅ FULLY HANDLED**

**How it works:**

- Every code path that locks multiple inventory rows first sorts the IDs in ascending UUID
  order before acquiring the lock.
- The JPQL query itself also enforces `ORDER BY i.itemId`, so database rows are always
  locked in the same global order regardless of what order the caller passed items.
- Because every thread acquires lock A before lock B, two threads can never be in the state
  where Thread-1 holds A and waits for B while Thread-2 holds B and waits for A — the
  circular wait condition is structurally impossible.

**No deadlock has ever been observed in any integration test.**

---

### ③ Customer Cancel vs Admin Confirm — Same Order — ⚠️ CRITICAL BUG

```
Race: Order is PENDING.
      Customer hits PUT /customer/orders/{id}/cancel.
      Admin simultaneously hits PUT /admin/orders/status (PENDING → CONFIRMED).
```

**Admin Code Path (Correct):**
```
OrderServiceImpl.updateOrderStatus()
  → OrderTransitionHelper.updateOrderInternal()          [REQUIRES_NEW]
    → ordersRepository.findByIdWithLock(orderId)         [Mechanism C — FOR UPDATE]
      → validateAdminTransition(PENDING, CONFIRMED)
        → handleInventoryUpdate()
          → order.setStatus(CONFIRMED) → save()
```

**Customer Code Path (Broken):**
```
OrderServiceImpl.cancelOrder()
  → getOrderOrThrow(orderId)
    → ordersRepository.findById(orderId)                 ❌ NO LOCK
      → validateOrderCancellation()
        → handleInventoryUpdate()
          → order.setStatus(CANCELLED) → save()
```

**Status: ⚠️ PARTIALLY HANDLED — Customer path has no lock**

**How the bug manifests:**

```
Timeline:

T0: Admin thread calls findByIdWithLock(orderId) → acquires FOR UPDATE lock.
T1: Customer thread calls findById(orderId) → reads PENDING state (no lock needed).
T2: Admin commits: status = CONFIRMED, reservedStock unchanged.
    Lock is released.
T3: Customer (unaware of T2 commit) writes: status = CANCELLED, reservedStock--.
    Result: CONFIRMED order is now CANCELLED. ReservedStock was decremented for
            a confirmed order → stock leak.
```

**Observed in Test:**
`OrderStatusRaceIntegrationTest` (30 orders):
- 21 CANCELLED (customer won)
- 8 CONFIRMED (admin won)
- **1 stuck in PENDING** (both threads collided at the DB lock manager — one rolled back, no retry → orphan with leaked stock)

**The Fix (one line change):**

```java
// OrderServiceImpl.java — cancelOrder() method
// ─────────────────────────────────────────────────────────
// CURRENT (BROKEN):
Orders order = getOrderOrThrow(orderId);

// FIX:
Orders order = ordersRepository.findByIdWithLock(orderId)
        .orElseThrow(() -> {
            logger.warn("Order not found: {}", orderId);
            return new OrderNotFoundException("Order not found: " + orderId);
        });
```

This one change makes the customer path use the same `SELECT … FOR UPDATE` as the admin path,
ensuring both actors respect the same lock and the winner is whoever gets the DB lock first,
with the loser receiving a clean transition error rather than silently corrupting state.

---

### ④ Scheduler vs Admin Confirm — Same Order

```
Race: OrderAutoCancelScheduler identifies a stale PENDING order → calls cancelStalePendingOrder().
      Simultaneously, Admin is clicking "Confirm" on the same order via the Admin UI.
```

**Scheduler Code Path:**
```
OrderAutoCancelScheduler.cancelStalePendingOrders()
  → ordersRepository.findStalePendingOrdersPriority(PENDING, cutoff)
    [PESSIMISTIC_WRITE + SKIP LOCKED — lock.timeout = -2]          [Mechanism D]
  → for each orderId:
      transitionHelper.cancelStalePendingOrder(orderId)             [REQUIRES_NEW]
        → ordersRepository.findByIdWithLock(orderId)                [Mechanism C]
          → re-check: if status != PENDING → return early           [Mechanism H]
            → handleInventoryUpdate(PENDING → CANCELLED)
              → order.setStatus(CANCELLED) → save()
```

**Admin Code Path:**
```
OrderTransitionHelper.updateOrderInternal()                         [REQUIRES_NEW]
  → ordersRepository.findByIdWithLock(orderId)                      [Mechanism C]
    → validateAdminTransition(PENDING, CONFIRMED)
      → handleInventoryUpdate()
        → order.setStatus(CONFIRMED) → save()
```

**Status: ✅ HANDLED (with a documented residual nanosecond race)**

**How it works — three layers of protection:**

**Layer 1 — SKIP LOCKED on the list query:**
`findStalePendingOrdersPriority` uses `lock.timeout = -2` which translates to `SKIP LOCKED`
in PostgreSQL. If the admin transaction is **already holding a lock** on the order row, the
scheduler's list query will simply not include that order in its result set. The order is
effectively invisible to the scheduler while the admin holds the lock.

**Layer 2 — Per-order pessimistic lock:**
Both `cancelStalePendingOrder()` and `updateOrderInternal()` call `findByIdWithLock()`.
Whichever thread gets the `SELECT … FOR UPDATE` lock first wins. The other waits.

**Layer 3 — Status re-check after lock acquisition:**
Inside `cancelStalePendingOrder()`:
```java
if (!OrderStatus.PENDING.name().equals(order.getStatus().getStatusName())) {
    logger.warn("Skipping cancelStalePendingOrder for Order: {} - no longer PENDING", orderId);
    return;   // ← idempotent exit
}
```
Even if the scheduler beat the admin to the lock and then the admin committed first (impossible
with pessimistic lock but covered for safety), this check ensures no double-action occurs.

**Residual Risk (documented in test, not yet mitigated):**
SKIP LOCKED only helps if the admin has **already acquired** the row lock. If the scheduler
thread reaches the DB lock manager a nanosecond before the admin thread, the scheduler gets
the lock, cancels the order, and the admin sees "order is no longer PENDING". At the database
level there is no built-in way to assign higher priority to the admin's lock request over the
scheduler's lock request.

**Proposed (not yet implemented) mitigations:**
- Soft-lock flag (`is_admin_active` column): Scheduler ignores orders with this flag.
- Grace-period filter: Scheduler skips orders with `updatedTimestamp > now - 5 min`.
- Admin Grace-window: Admin sets the flag when the order detail page is opened.

**Test Coverage:** `CronVsHumanRaceIntegrationTest` — 30 orders: admin won 6, cron won 24.
Inventory perfectly matched the 6 confirmed orders (zero stock leaks).

---

### ⑤ Admin Price Update vs Order Creation

```
Race: Admin calls PUT /admin/prices to change Item X price from ₹100 to ₹500.
      At the same instant, a Customer is creating an order for Item X.
      Risk: Customer might capture a phantom price — neither ₹100 nor ₹500 but some
            torn intermediate value, or an inconsistent total.
```

**Admin Code Path:**
```
AdminPriceServiceImpl.updatePrices()   [@Transactional]
  → pricingCatalogRepository.findById() → setCatalogUnitPrice(₹500) → save()
  → savePricingHistory(item, ₹100, ₹500)  ← immutable record appended
  Both writes committed atomically in the same transaction.
```

**Customer Code Path:**
```
OrderInventoryManagerImpl.processAndSaveOrderItems()
  → inventoryRepository.findAllByItemIdInForUpdate(itemIds)  [FOR UPDATE lock]
    → resolveUnitPrice(inventoryItem)
      → pricingHistoryRepository
          .findFirstByInventoryItemItemIdOrderByCreatedTimestampDesc(itemId)
        ← reads the LATEST committed PricingHistory entry (immutable)
      → captures that BigDecimal as unitPrice in OrderItem
```

**Status: ✅ FULLY HANDLED**

**Why this works — the PricingHistory design:**

The key architectural decision is that order creation reads from `PricingHistory`
(append-only, immutable) rather than from `PricingCatalog` (mutable). Because:

1. Each admin price update atomically appends a new `PricingHistory` row AND updates
   the catalog — both in the same `@Transactional` block.
2. `PricingHistory` rows are never updated or deleted — they are immutable snapshots.
3. The customer's transaction reads the **latest committed** snapshot at the moment
   the inventory lock is held. Any admin commit that happened before the customer's
   lock time is visible; any commit after is not — classic snapshot isolation semantics.
4. Even if two admins race to update the catalog (see scenario ⑩), the `PricingHistory`
   will have both entries and the "latest" one (by `createdTimestamp`) is unambiguously
   determinable.
5. Once the `unitPrice` is stored in `OrderItem` at creation time, it is immutable.
   Future admin price changes never retrospectively alter placed orders.

**Test Coverage:** `AdminPriceUpdateVsCreationIntegrationTest`:
- 156 orders created concurrently with 24 unique price points pushed by 2 admin threads.
- **Zero mismatches** between any order's `unitPrice` and any row in `PricingHistory`.
- **Zero cases** where `totalAmount ≠ unitPrice × quantity`.
- Reserved stock perfectly matched `156 orders × 2 items = 312`.

---

### ⑥ Concurrent Bulk Status Update

```
Race: Two admin threads simultaneously submit bulk status update requests that contain
      overlapping order IDs.
```

**Code Path:**
```
OrderServiceImpl.updateOrdersStatus()   [No @Transactional on outer loop — by design]
  → for each BulkOrderStatusUpdateDTO:
      transitionHelper.updateOrderInternal(orderId, newStatus)   [REQUIRES_NEW]
        → findByIdWithLock(orderId)                              [FOR UPDATE]
          → validateAdminTransition(current, next)
            → handleInventoryUpdate()
              → order.setStatus() → save()
```

**Status: ✅ HANDLED**

**How it works:**

1. Every order transition runs inside its own `REQUIRES_NEW` transaction — fully isolated.
2. `findByIdWithLock()` ensures only one thread operates on a given order at a time.
3. If Thread-1 already processed Order-X (e.g., PENDING → CONFIRMED) and Thread-2 also
   tries PENDING → CONFIRMED for the same Order-X: Thread-2 blocks on the lock, reads the
   CONFIRMED status, and `validateAdminTransition(CONFIRMED, CONFIRMED)` throws
   `InvalidOrderTransitionException` — this is caught and returned as a failure entry in
   the bulk result. **No corruption occurs.**
4. One order's failure never rolls back another order's success (REQUIRES_NEW isolation).

**Design Note:**
`updateOrdersStatus()` has no `@Transactional` on the outer loop — this is intentional.
If it were transactional and one order failed, all previously succeeded updates within
the same outer transaction could be rolled back. The current design is correct for a
"best-effort bulk" operation.

---

### ⑦ Admin Inventory Update vs Order Creation

```
Race: Admin calls PUT /admin/inventory to reduce Item X's availableStock from 50 to 10.
      Customer is simultaneously placing an order for 20 units of Item X.
```

**Admin Code Path:**
```
InventoryServiceImpl.updateInventoryItems()
  → itemIds sorted ascending                                         [Mechanism F]
  → inventoryRepository.findAllByItemIdInForUpdate(itemIds)         [Mechanism A]
    → validate: newAvailableStock >= currentReservedStock
      → existingItem.setAvailableStock(10) → saveAll()
```

**Customer Code Path:**
```
OrderInventoryManagerImpl.processAndSaveOrderItems()
  → inventoryRepository.findAllByItemIdInForUpdate(itemIds)         [Mechanism A]
    → check effectiveAvailable = availableStock - reservedStock
      → reservedStock += 20 → saveAndFlush()
```

**Status: ✅ HANDLED**

Both paths contend for the exact same `SELECT … FOR UPDATE` lock on the same item row.
They serialize. If the admin goes first (availableStock = 10) and the customer requests
20 units: `effectiveAvailable = 10 - 0 = 10 < 20` → `InsufficientStockException`. Correct.
If the customer goes first: reservedStock = 20. Admin then tries to set availableStock = 10
but `10 < 20 (reservedStock)` → `InvalidOperationException`. Correct.

---

### ⑧ Admin Add Stock vs Order Creation

```
Race: Admin adds +100 units to Item X while Customer is simultaneously ordering.
```

**Code Path:**
```
InventoryServiceImpl.addStock()
  → itemIds sorted ascending                                         [Mechanism F]
  → inventoryRepository.findAllByItemIdInForUpdate(itemIds)         [Mechanism A]
    → existingItem.availableStock += addStock → saveAll()
```

**Status: ✅ HANDLED**

Both paths use the same `FOR UPDATE` lock — they serialize. The increment is a locked
read-modify-write. No lost-update is possible.

---

### ⑨ Admin Delete Item with Active Reservations

```
Race: Admin deletes Item X while a customer's PENDING order has units of Item X reserved.
```

**Code Path:**
```
InventoryServiceImpl.deleteInventoryItems()
  → sortedIds sorted ascending                                      [Mechanism F]
  → inventoryRepository.findAllByItemIdInForUpdate(sortedIds)      [Mechanism A]
    → for each item: if reservedStock > 0 → throw InvalidOperationException
      → inventoryRepository.deleteAll(itemsToDelete)
```

**Status: ✅ HANDLED**

The deletion path acquires the same `FOR UPDATE` lock used by order creation. This means:
- If delete gets the lock first: `reservedStock > 0` check passes (order not yet placed),
  deletion succeeds. Subsequent order creation calls `findAllByItemIdInForUpdate()` and gets
  nothing → `InvalidOperationException("Item not found")`. Correct.
- If order creation gets the lock first: reservedStock is incremented and committed.
  Delete then reads the committed `reservedStock > 0` → throws. Deletion blocked. Correct.

---

### ⑩ Two Admins Update Same Price Simultaneously — ❌ NOT HANDLED

```
Race: Admin A updates Item X price to ₹500.
      Admin B simultaneously updates Item X price to ₹750.
      Both read the PricingCatalog row, both write different values.
```

**Code Path:**
```
AdminPriceServiceImpl.updatePrices()   [@Transactional]
  → pricingCatalogRepository.findById(itemId)   ❌ PLAIN READ — NO LOCK
    → target.setUnitPrice(newPrice)
      → pricingCatalogRepository.save(target)
  → savePricingHistory(...)
```

**Status: ❌ NOT HANDLED**

**How the bug manifests:**

```
Timeline:

T0: Admin A reads PricingCatalog: oldPrice = ₹100
T1: Admin B reads PricingCatalog: oldPrice = ₹100
T2: Admin A writes: PricingCatalog.unitPrice = ₹500, PricingHistory(₹100 → ₹500) added.
T3: Admin B writes: PricingCatalog.unitPrice = ₹750, PricingHistory(₹100 → ₹750) added.

Final PricingCatalog state: ₹750 (last writer wins — non-deterministic).
PricingHistory has both entries — correct in isolation, but the catalog is inconsistent.
```

The impact on **orders is limited** because order creation reads `PricingHistory` (see ⑤),
not the catalog. However, `PricingCatalog` is the mutable "current price" reference used
for display, reporting, and any features that read the catalog directly.

**The Fix:**

```java
// AdminPriceServiceImpl.updatePrices()
// Acquire a pessimistic lock on the InventoryItem before touching PricingCatalog.
// Both the catalog update and the history write happen under this lock.

@Override
@Transactional
public void updatePrices(List<AdminPricingDTO> prices) {
    for (AdminPricingDTO pricingDTO : prices) {
        // ✅ Lock the inventory item row first
        InventoryItem lockedItem = inventoryItemRepository
                .findByIdForUpdate(pricingDTO.getItemId())          // [Mechanism B]
                .orElseThrow(() -> new ResourceNotFoundException(...));

        PricingCatalog target = pricingCatalogRepository
                .findById(pricingDTO.getItemId())
                .orElseThrow(...);

        BigDecimal oldPrice = target.getUnitPrice();
        target.setUnitPrice(pricingDTO.getUnitPrice());
        pricingCatalogRepository.save(target);
        savePricingHistory(lockedItem, oldPrice, pricingDTO.getUnitPrice(), ...);
    }
}
```

---

### ⑪ Duplicate Customer Registration

```
Race: Two requests to register the same email in the same org arrive simultaneously.
```

**Code Path (inferred from OrgAdminServiceImpl/AuthServiceImpl pattern):**
```
appUserRepository.existsByOrgIdAndEmailIgnoreCase(orgId, email)   ← check (no lock)
  → if false → appUserRepository.save(newUser)                     ← act
```

**Status: ⚠️ PARTIALLY HANDLED**

**What works:** The database has a `UNIQUE` constraint on `(org_id, email)`. The second
concurrent insert will be rejected at the DB level with a constraint violation.

**What doesn't work:** The application layer does not explicitly catch
`DataIntegrityViolationException` from the DB and translate it into a user-friendly
response. Depending on the global exception handler configuration, this may surface as an
unhandled 500 Internal Server Error instead of a 409 Conflict.

The `existsByEmail` check followed by `save()` is a classic **check-then-act** race
(TOCTOU — Time-of-Check-Time-of-Use). The check and the save are not atomic.

**The Fix:**

```java
// In GlobalExceptionHandler (or equivalent):
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    // Detect unique constraint violations specifically
    if (ex.getMessage().contains("unique") || ex.getMessage().contains("duplicate")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_ENTRY", "Resource already exists."));
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("DATA_ERROR", "A data integrity error occurred."));
}
```

---

### ⑫ Duplicate Organization Subdomain Creation

```
Race: Two SuperAdmin requests to create an org with the same subdomain arrive simultaneously.
```

**Code Path:**
```
SuperAdminServiceImpl.createOrganization()
  → organizationRepository.findBySubdomainIgnoreCase(subdomain).isPresent()  ← check
    → if absent → organizationRepository.save(newOrg)                        ← act
```

**Status: ⚠️ PARTIALLY HANDLED — identical pattern to ⑪**

Same TOCTOU gap. DB UNIQUE constraint saves data integrity but may surface as 500. Same fix
applies — catch `DataIntegrityViolationException` at the global handler level.

---

### ⑬ Token Blacklist Race

```
Race: User's valid JWT is being blacklisted (logout). A concurrent API call using the same
      token arrives at the filter before the blacklist write is committed.
```

**Code Path:**
```
TokenBlacklistServiceImpl → (implementation detail, in-memory or DB backed)
JwtAuthFilter → checks blacklist → allows/denies request
```

**Status: ❓ REQUIRES REVIEW**

The safety of this scenario depends entirely on the `TokenBlacklistServiceImpl`
implementation:

- **If DB-backed with a UNIQUE constraint on the token:** The insert is atomic. The filter
  reads from the same DB, so once committed, subsequent requests are blocked. Safe, with
  a potential tiny window on the same JVM thread-local cache.
- **If in-memory (`Set<String>`):** Thread safety depends on whether a `ConcurrentHashSet`
  or synchronized collection is used. A plain `HashSet` would be a data race.
- **If Redis-backed:** Atomic `SET NX EX` operations make this safe.

**Action:** Review `TokenBlacklistServiceImpl` to confirm thread-safe collection or
DB-level atomicity.

---

### ⑭ Customer Profile Update from Two Devices — ❌ NOT HANDLED

```
Race: Same customer submits profile update from their phone and laptop simultaneously.
      Both updates contain different data.
```

**Code Path:**
```
CustomerServiceImpl.updateProfile()
  → customerRepository.findByAppUserEmail(email)   ← plain read, no lock
    → customer.setFirstName() / setAddress() etc.
      → customerRepository.save(customer)
```

**Status: ❌ NOT HANDLED**

**How the bug manifests:**

```
Timeline:

T0: Request A reads Customer: address = "Old Address"
T1: Request B reads Customer: address = "Old Address"
T2: Request A writes: firstName = "NewFirst", address = "Old Address"
T3: Request B writes: firstName = "OldFirst", address = "New Address"

Final: firstName is "OldFirst" (A's update silently lost).
```

Classic **Lost Update** problem. Last writer wins. No `@Version` on `Customer` entity.

**Practical Risk:** Low — the same user updating from two devices simultaneously is rare.
But it is a correctness gap.

**The Fix (Option A — Optimistic Lock):**
```java
// Customer.java
@Version
@Column(name = "version")
private Long version;
// Requires: ALTER TABLE customer ADD COLUMN version BIGINT DEFAULT 0;
// Client must pass the version it read; mismatch throws OptimisticLockException → 409.
```

**The Fix (Option B — Last-Write-Wins with acknowledgment):**
Accept that last-writer-wins is acceptable for profile data (common in most consumer apps),
but document it explicitly and ensure it does NOT apply to financial data.

---

### ⑮ Scheduler List → Order Deleted Mid-Loop

```
Race: Scheduler fetches a list of stale PENDING orders.
      Between the list fetch and the per-order cancel call, one of those orders is deleted
      (or its inventory item is deleted by an admin).
```

**Code Path:**
```
OrderAutoCancelScheduler.cancelStalePendingOrders()
  → ordersRepository.findStalePendingOrdersPriority(PENDING, cutoff) ← snapshot list
    → for each order:
        transitionHelper.cancelStalePendingOrder(order.getOrderId())
          → ordersRepository.findByIdWithLock(orderId)
            ← Returns Optional.empty() if order was deleted in the gap
              → throws OrderNotFoundException
                ← caught in scheduler loop:
                     catch (OrderNotFoundException e) { logger.error(...) }
```

**Status: ✅ FULLY HANDLED**

`findByIdWithLock()` returns an `Optional`. If the order was deleted between the list
snapshot and the per-order lock call, the Optional is empty, an `OrderNotFoundException`
is thrown, and the scheduler's `try/catch` block logs the error and moves on to the next
order. The scheduler loop never crashes.

---

## 3. Master Summary Table

| # | Race Scenario | Status | Mechanisms Used | Gap |
|---|---|---|---|---|
| ① | Customer vs Customer — stock exhaustion | ✅ **Fully Handled** | FOR UPDATE + @Version + sorted locks | None |
| ② | Multi-item deadlock (AB/BA ordering) | ✅ **Fully Handled** | Sorted lock acquisition (itemId asc) | None |
| ③ | Customer Cancel vs Admin Confirm — same order | ⚠️ **Partial** | Admin: FOR UPDATE ✅ / Customer: plain findById ❌ | `cancelOrder()` uses unlocked `findById()` |
| ④ | Scheduler Cancel vs Admin Confirm — same order | ✅ **Handled** | SKIP LOCKED + FOR UPDATE + status re-check | Nanosecond priority race (accepted, unmitigated) |
| ⑤ | Admin Price Update vs Order Creation | ✅ **Fully Handled** | Immutable PricingHistory + inventory lock | None |
| ⑥ | Concurrent Bulk Status Update | ✅ **Handled** | REQUIRES_NEW + per-order FOR UPDATE | Outer loop non-transactional (by design) |
| ⑦ | Admin Inventory Update vs Order Creation | ✅ **Fully Handled** | Both use FOR UPDATE on same row | None |
| ⑧ | Admin Add Stock vs Order Creation | ✅ **Fully Handled** | FOR UPDATE | None |
| ⑨ | Admin Delete Item with active reservations | ✅ **Fully Handled** | FOR UPDATE + reservedStock guard | None |
| ⑩ | Two Admins update same price simultaneously | ❌ **Not Handled** | None | PricingCatalog.unitPrice is last-write-wins |
| ⑪ | Duplicate customer registration (same email) | ⚠️ **Partial** | DB UNIQUE constraint | App may throw 500 instead of 409 |
| ⑫ | Duplicate org subdomain creation | ⚠️ **Partial** | DB UNIQUE constraint | Same as ⑪ |
| ⑬ | Token blacklist race (logout vs reuse) | ❓ **Unknown** | Depends on impl | Needs code review |
| ⑭ | Customer profile update from two devices | ❌ **Not Handled** | None | Lost-update (low practical risk) |
| ⑮ | Scheduler list → order deleted mid-loop | ✅ **Fully Handled** | Optional + OrderNotFoundException catch | None |

---

## 4. What Needs to Be Fixed (Priority Order)

---

### 🔴 P1 — `cancelOrder()` Missing Lock

**File:** `OrderServiceImpl.java`
**Method:** `cancelOrder(UUID orderId, String email)`
**Risk:** Stock leaks, orphaned orders in PENDING state, overwriting admin transitions.

```java
// BEFORE (broken — line 125 in OrderServiceImpl.java):
Orders order = getOrderOrThrow(orderId);

// AFTER (fix):
Orders order = ordersRepository.findByIdWithLock(orderId)
        .orElseThrow(() -> {
            logger.warn("Order not found: {}", orderId);
            return new OrderNotFoundException("Order not found: " + orderId);
        });
```

This single change makes the customer cancel path symmetric with the admin confirm path —
both actors will then compete for the same `SELECT … FOR UPDATE` row lock, and whichever
wins will atomically read the current state and act on it. The loser will retry (once P2
is implemented) or receive a clean conflict error.

---

### 🔴 P2 — No Retry Mechanism Anywhere

**File:** `pom.xml`, `OrderServiceImpl.java`
**Risk:** Even with the P1 fix, a nanosecond-level collision between two threads at the DB
lock manager can cause one transaction to receive a `PessimisticLockingFailureException` or
`CannotAcquireLockException`. Without retry, that request permanently fails and — if it is
the only actor — can leave an order in PENDING indefinitely.

**Step 1 — Add dependencies to `pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

**Step 2 — Enable retry in the main config:**
```java
@SpringBootApplication
@EnableRetry
public class OrdermgmtApplication { ... }
```

**Step 3 — Annotate the critical methods:**
```java
// OrderServiceImpl.java
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;

@Override
@Transactional
@Retryable(
    value  = { PessimisticLockingFailureException.class, CannotAcquireLockException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public OrderDTO cancelOrder(UUID orderId, String email) { ... }

@Override
// Note: updateOrdersStatus() has no @Transactional (outer loop). That is intentional.
// Retry should be on the inner transitionHelper calls — add @Retryable there.
public BulkOrderUpdateResultDTO updateOrdersStatus(List<BulkOrderStatusUpdateDTO> updates) { ... }
```

**Step 4 — Annotate `OrderTransitionHelper.updateOrderInternal()`:**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Retryable(
    value  = { PessimisticLockingFailureException.class, CannotAcquireLockException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public OrderDTO updateOrderInternal(UUID orderId, String newStatusString) { ... }
```

---

### 🟡 P3 — `PricingCatalog` Write Not Locked

**File:** `AdminPriceServiceImpl.java`
**Method:** `updatePrices(List<AdminPricingDTO>)`
**Risk:** Two simultaneous admin price updates produce non-deterministic `PricingCatalog`
state (last-writer-wins). `PricingHistory` remains correct but the live catalog is unreliable.

```java
@Override
@Transactional
public void updatePrices(List<AdminPricingDTO> prices) {
    for (AdminPricingDTO pricingDTO : prices) {

        // ✅ FIX: Acquire lock on InventoryItem row first.
        // Since PricingCatalog is 1:1 with InventoryItem, locking the item
        // serializes all price updates for that item.
        inventoryItemRepository.findByIdForUpdate(pricingDTO.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item not found: " + pricingDTO.getItemId()));

        PricingCatalog target = pricingCatalogRepository.findById(pricingDTO.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existing price record found for item: " + pricingDTO.getItemId()));

        BigDecimal oldPrice = target.getUnitPrice();
        // ... rest of the method unchanged
    }
}
```

---

### 🟡 P4 — Duplicate Registration/Subdomain → 500 Instead of 409

**File:** Global exception handler (e.g., `GlobalExceptionHandler.java`)
**Risk:** Two concurrent registration requests both pass the `exists()` check. The DB
UNIQUE constraint catches the second. Without catching `DataIntegrityViolationException`,
this becomes an unhandled 500.

```java
// GlobalExceptionHandler.java
@ExceptionHandler(DataIntegrityViolationException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
    String msg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
    if (msg != null && (msg.toLowerCase().contains("unique") ||
                        msg.toLowerCase().contains("duplicate"))) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_ENTRY",
                        "Resource already exists. Please use a different value."));
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("DATA_ERROR", "A data integrity error occurred."));
}
```

---

### 🟢 P5 — `@Version` on `Orders` Entity

**File:** `Orders.java`, database migration
**Risk:** Currently `Orders` has NO optimistic lock. If a pessimistic lock collision occurs
without retry (until P2 is implemented), there is no second-layer net. Adding `@Version`
provides optimistic locking as a safety net.

**Step 1 — Database migration:**
```sql
ALTER TABLE itest_ordermgmt.orders ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE test_ordermgmt.orders ADD COLUMN version BIGINT DEFAULT 0;
-- (and prod tables)
```

**Step 2 — Entity update:**
```java
// Orders.java — add this field:
@Version
@Column(name = "version")
private Long version;
```

With `@Version`, if two transactions both read version=5 and one commits first (now version=6),
the second commit throws `OptimisticLockingFailureException`. With the `@Retryable` from P2,
this will trigger a retry that re-reads version=6 and proceeds safely.

---

## 5. Evidence from Integration Tests

The following concurrency integration tests provide empirical evidence of the handled and
unhandled scenarios above.

### `OrderStatusRaceIntegrationTest`

- **What it tests:** 30 PENDING orders hit simultaneously by Customer Cancel + Admin Confirm.
- **Result (before fix):**
  - 21 CANCELLED (customer wins)
  - 8 CONFIRMED (admin wins)
  - **1 stuck in PENDING** (collision with no retry → orphaned order)
  - Inventory: reservedStock = 9 but should = 8 (1-unit stock leak from the orphan)
- **Root cause confirmed:** `cancelOrder()` uses unlocked `findById()` → Scenario ③.

### `CronVsHumanRaceIntegrationTest`

- **What it tests:** 30 orders hit simultaneously by Scheduler Cancel + Admin Confirm.
- **Result:**
  - Admin wins: 6 / Cron wins: 24
  - Reserved stock: exactly 6 (matching 6 confirmed orders)
  - **Zero stock leaks** (the SKIP LOCKED + status re-check mechanism is sound)
- **Residual finding:** Scheduler has no priority disadvantage — it can beat the admin
  (nanosecond race). This is the accepted residual risk of Scenario ④.

### `AdminPriceUpdateVsCreationIntegrationTest`

- **What it tests:** 2 admin threads × 10 price updates, 10 customer threads × 15 orders
  each — all concurrent.
- **Result:**
  - 156 orders created
  - 24 unique price points in PricingHistory
  - **0 phantom prices** (every order's unitPrice matched a PricingHistory entry)
  - **0 total-amount mismatches** (totalAmount = unitPrice × quantity for all 156 orders)
  - Reserved stock: exactly 312 (156 orders × 2 items each)
- **Confirms:** Scenario ⑤ (price snapshot) is correctly handled.

---

*End of Concurrency Audit*
