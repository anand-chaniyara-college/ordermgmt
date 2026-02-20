# API Reference — User-grouped

This document lists all REST endpoints grouped by user role (Public / Customer / Admin) with accepted parameters, request payloads, and response structures. Optional parameters/fields are marked as `(optional)`.

> **Response convention:** All responses are JSON objects. String messages are wrapped as `{"message": "..."}`. Lists are wrapped under a named key (e.g. `{"orders": [...]}`). Paginated responses use Spring's `Page` object format.

---

**Public / Authentication**

- POST /api/auth/register
  - Description: Register a new user (role: ADMIN or CUSTOMER). Rate-limited.
  - Request Body (JSON): `RegistrationRequestDTO`
    - `email` (string) — required, valid email
    - `password` (string) — required, min 6 chars
    - `roleName` (string) — required, must be `ADMIN` or `CUSTOMER`
  - Response (200): `RegistrationResponseDTO` `{ "message" }`
  - Response (429 — rate limited): `{ "message": "..." }`

- POST /api/auth/login
  - Description: Sign in and receive access + refresh tokens.
  - Request Body (JSON): `LoginRequestDTO`
    - `email` (string) — required, valid email
    - `password` (string) — required
  - Response: `LoginResponseDTO` `{ "accessToken", "refreshToken", "tokenType", "role", "message" }`

- POST /api/auth/refresh
  - Description: Refresh access token using refresh token.
  - Request Header: `Authorization: Bearer <accessToken>` — required
  - Request Body (JSON): `RefreshTokenRequestDTO`
    - `refreshToken` (string) — required
  - Response: `RefreshTokenResponseDTO` `{ "accessToken", "refreshToken", "tokenType", "message" }`

- POST /api/auth/logout
  - Description: Invalidate current session/token.
  - Request Header: `Authorization: Bearer <accessToken>` — required
  - Request Body (JSON): `RefreshTokenRequestDTO`
    - `refreshToken` (string) — required
  - Response: `{ "message": "Logged out successfully" }`

---

**Customer (requires authentication)**
- GET /api/customer/profile
  - Description: Retrieve the logged-in customer's profile.
  - Authorization: `Authorization: Bearer <accessToken>` — required
  - Response Body: `CustomerProfileDTO`
    - `firstName` (string) — required
    - `lastName` (string) — required
    - `contactNo` (string) — (optional) digits only, 10–20 chars
    - `address` (string) — (optional)
    - `email` (string) — read-only, returned in response

- PUT /api/customer/profile
  - Description: Update the logged-in customer's profile.
  - Authorization: `Authorization: Bearer <accessToken>` — required
  - Request Body (JSON): `CustomerProfileDTO`
    - `firstName` (string) — required, alphabets only, max 100 chars
    - `lastName` (string) — required, alphabets only, max 100 chars
    - `contactNo` (string) — (optional) digits only, 10–20 chars
    - `address` (string) — (optional) max 255 chars
    - `email` — **not updatable**; if a different email is provided, the request is rejected with error `"Email cannot be updated"`
  - Response: `{ "message": "Profile updated successfully" }`

---

**Product Catalog (requires authentication)**
- GET /api/customer/products
  - Description: List all available products. Supports optional pagination.
  - Authorization: `Authorization: Bearer <accessToken>` — required
  - Query Parameters:
    - `page` (integer) — (optional) 0-indexed page number
    - `size` (integer) — (optional) page size
  - Response (without pagination): `{ "products": [ ProductDTO, ... ] }`
  - Response (with pagination): Spring `Page<ProductDTO>` object `{ "content": [...], "totalElements", "totalPages", ... }`
  - `ProductDTO` fields:
    - `itemId` (string)
    - `itemName` (string)
    - `unitPrice` (decimal)
    - `availableStock` (integer)

---

**Customer Orders (requires authentication)**
- POST /api/customer/orders
  - Description: Place a new order.
  - Authorization: `Authorization: Bearer <accessToken>` — required
  - Request Body (JSON): `OrderDTO` (client sends only `items`)
    - `items` (array of `OrderItemDTO`) — required, at least one
      - `itemId` (string) — required
      - `quantity` (integer) — required, positive
  - Response (201 Created): `OrderDTO`
    - `orderId` (string) — server-generated
    - `status` (string) — e.g. `"PENDING"`
    - `createdTimestamp` (datetime) — server-generated
    - `updatedTimestamp` (datetime) — server-generated
    - `items` (array of `OrderItemDTO`)
      - `itemId`, `itemName`, `quantity`, `unitPrice`, `subTotal`
    - `totalAmount` (decimal)
    - Note: `customerId` is **not** included in the response

- GET /api/customer/orders
  - Description: View customer's orders (merged behavior):
    - If `orderId` query provided → returns `{ "orders": [ OrderDTO ] }`.
    - If `page` and `size` provided → returns paginated `Page<OrderDTO>`.
    - Otherwise → returns `{ "orders": [ OrderDTO, ... ] }`.
  - Query Parameters:
    - `orderId` (string) — (optional)
    - `page` (integer) — (optional)
    - `size` (integer) — (optional)
  - Authorization: `Authorization: Bearer <accessToken>` — required

- PUT /api/customer/orders/{orderId}/cancel
  - Description: Cancel a specific order belonging to the authenticated customer.
  - Path Parameter: `orderId` (string) — required
  - Authorization: `Authorization: Bearer <accessToken>` — required
  - Response: updated `OrderDTO`

---

**Admin — Order Management (requires ADMIN role)**
- GET /api/admin/orders
  - Description: View all orders (merged behavior): specific, paginated, or full list.
  - Query Parameters:
    - `orderId` (string) — (optional)
    - `page` (integer) — (optional)
    - `size` (integer) — (optional)
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Response (specific orderId): `{ "orders": [ OrderDTO ] }`
  - Response (paginated): `Page<OrderDTO>`
  - Response (full list): `{ "orders": [ OrderDTO, ... ] }`

- PUT /api/admin/orders/status
  - Description: Bulk update status for multiple orders. Each update is independent.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body (JSON): `BulkOrderStatusUpdateWrapperDTO`
    - `orders` (array of `BulkOrderStatusUpdateDTO`) — required
      - `orderId` (string) — required
      - `newStatus` (string) — required
  - Response Body: `BulkOrderUpdateResultDTO`
    - `successes` (array of `OrderDTO`)
    - `failures` (array of `BulkOrderFailureDTO`)
      - `orderId` (string)
      - `error` (string)

---

**Admin — Pricing Control (requires ADMIN role)**
- GET /api/admin/prices
  - Description: Get pricing records (specific item / paginated / full list).
  - Query Parameters:
    - `itemId` (string) — (optional)
    - `page` (integer) — (optional)
    - `size` (integer) — (optional)
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Response (specific itemId): `AdminPricingDTO`
  - Response (paginated): `Page<AdminPricingDTO>`
  - Response (full list): `{ "prices": [ AdminPricingDTO, ... ] }`
  - `AdminPricingDTO` fields:
    - `itemId` (string) — required
    - `unitPrice` (decimal) — required, non-negative
    - `effectiveFrom` (datetime string `yyyy-MM-dd HH:mm:ss`) — (optional)

- POST /api/admin/prices
  - Description: Create price records in bulk.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body (JSON): `AdminPricingWrapperDTO`
    - `price` (array of `AdminPricingDTO`) — required
  - Response (201 Created): `{ "message": "..." }`

- PUT /api/admin/prices
  - Description: Update prices in bulk.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body (JSON): `AdminPricingWrapperDTO`
    - `price` (array of `AdminPricingDTO`) — required
  - Response: `{ "message": "..." }`

---

**Admin — Inventory / Stock (requires ADMIN role)**
- GET /api/admin/inventory
  - Description: Get inventory items. Behavior:
    - `itemId` → returns single `InventoryItemDTO`.
    - `page` & `size` → returns paginated `Page<InventoryItemDTO>`.
    - otherwise → returns `{ "inventory": [ InventoryItemDTO, ... ] }`.
  - Query Parameters:
    - `itemId` (string) — (optional)
    - `page` (integer) — (optional)
    - `size` (integer) — (optional)
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - `InventoryItemDTO` fields:
    - `itemId` (string) — required
    - `itemName` (string) — (optional for update)
    - `availableStock` (integer) — required, >= 0
    - `reservedStock` (integer) — (optional), >= 0

- POST /api/admin/inventory
  - Description: Add multiple inventory items in bulk.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body (JSON): `InventoryItemWrapperDTO`
    - `inventory` (array of `InventoryItemDTO`) — required
  - Response (201 Created): `{ "items": [ ... ] }`

- POST /api/admin/inventory/addstock
  - Description: Add stock amounts to existing inventory items.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body (JSON): `AddStockWrapperDTO`
    - `addstock` (array of `AddStockRequestDTO`) — required
      - `itemId` (string) — required
      - `addStock` (integer) — required, > 0
  - Response: `{ "items": [ ... ] }`

- PUT /api/admin/inventory
  - Description: Update multiple inventory items in bulk.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body (JSON): `InventoryItemWrapperDTO`
    - `inventory` (array of `InventoryItemDTO`) — required
  - Response: `{ "items": [ ... ] }`

- DELETE /api/admin/inventory/{ids}
  - Description: Delete multiple inventory items by comma-separated IDs.
  - Path Parameter: `ids` (list of strings in path) — required
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Response: 204 No Content

---

**Admin — Analytics & Reports (requires ADMIN role)**
- GET /api/admin/analytics/monthlyreport
  - Description: Retrieve monthly sales report for a given month and year.
  - Query Parameters:
    - `month` (string) — required (alpha-only)
    - `year` (integer) — required, >= 2000
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Response Body: `MonthlySalesLogDTO`
    - `totalSoldItems` (long)
    - `totalRevenue` (decimal)
    - `items` (array of `ItemSalesReportDTO`) — (optional)

- POST /api/admin/analytics/sendreportemail
  - Description: Generate monthly report and email it to the admin.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body: `MonthlyReportRequestDTO`
    - `month` (string) — required
    - `year` (integer) — required
  - Response: `{ "message": "..." }`

---

## Example JSON payloads

Below are compact, practical examples you can use directly when calling the API.

- Register (POST /api/auth/register)
```json
{
  "email": "user@example.com",
  "password": "s3cret!",
  "roleName": "CUSTOMER"
}
```

- Login (POST /api/auth/login)
```json
{
  "email": "user@example.com",
  "password": "s3cret!"
}
```

- Refresh token (POST /api/auth/refresh)
Request header: `Authorization: Bearer <accessToken>`
```json
{
  "refreshToken": "<refresh-token>"
}
```

- Logout (POST /api/auth/logout)
Request header: `Authorization: Bearer <accessToken>`
```json
{
  "refreshToken": "<refresh-token>"
}
```

- Update profile (PUT /api/customer/profile)
```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "contactNo": "1234567890",
  "address": "123 Main St"
}
```

- Place order (POST /api/customer/orders) — minimal client payload
```json
{
  "items": [
    { "itemId": "SKU-1234", "quantity": 2 },
    { "itemId": "SKU-5678", "quantity": 1 }
  ]
}
```

- Admin: Bulk update order statuses (PUT /api/admin/orders/status)
```json
{
  "orders": [
    { "orderId": "ORD-1001", "newStatus": "SHIPPED" },
    { "orderId": "ORD-1002", "newStatus": "CANCELLED" }
  ]
}
```

- Admin: Create/update prices (POST/PUT /api/admin/prices)
```json
{
  "price": [
    { "itemId": "SKU-1234", "unitPrice": 19.99 },
    { "itemId": "SKU-5678", "unitPrice": 9.5 }
  ]
}
```

- Admin: Add inventory items (POST /api/admin/inventory)
```json
{
  "inventory": [
    { "itemId": "SKU-1234", "itemName": "Widget A", "availableStock": 100 }
  ]
}
```

- Admin: Add stock (POST /api/admin/inventory/addstock)
```json
{
  "addstock": [
    { "itemId": "SKU-1234", "addStock": 50 },
    { "itemId": "SKU-5678", "addStock": 20 }
  ]
}
```

- Admin: Update inventory (PUT /api/admin/inventory)
```json
{
  "inventory": [
    { "itemId": "SKU-1234", "itemName": "Widget A Updated", "availableStock": 150 }
  ]
}
```

- Analytics: Request monthly report (POST /api/admin/analytics/sendreportemail)
```json
{
  "month": "January",
  "year": 2026
}
```
