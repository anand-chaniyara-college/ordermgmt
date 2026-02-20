# API Reference — User-grouped

This document lists all REST endpoints grouped by user role (Public / Customer / Admin) with accepted parameters and request payloads. Optional parameters/fields are marked as `(optional)`.

---

**Public / Authentication**

- POST /api/auth/register
  - Description: Register a new user (role: ADMIN or CUSTOMER). Rate-limited.
  - Request Body (JSON): `RegistrationRequestDTO`
    - `email` (string) — required, valid email
    - `password` (string) — required, min 6 chars
    - `roleName` (string) — required, must be `ADMIN` or `CUSTOMER`
  - Response: `RegistrationResponseDTO` { `message` }

- POST /api/auth/login
  - Description: Sign in and receive access + refresh tokens.
  - Request Body (JSON): `LoginRequestDTO`
    - `email` (string) — required, valid email
    - `password` (string) — required
  - Response: `LoginResponseDTO` { `accessToken`, `refreshToken`, `tokenType`, `role`, `message` }

- POST /api/auth/refresh
  - Description: Refresh access token using refresh token.
  - Request Header: `Authorization: Bearer <accessToken>` — required
  - Request Body (JSON): `RefreshTokenRequestDTO`
    - `refreshToken` (string) — required
  - Response: `RefreshTokenResponseDTO` { `accessToken`, `refreshToken`, `tokenType`, `message` }

- POST /api/auth/logout
  - Description: Invalidate current session/token.
  - Request Header: `Authorization: Bearer <accessToken>` — required
  - Request Body (JSON): `RefreshTokenRequestDTO`
    - `refreshToken` (string) — required
  - Response: plain text message

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
    - `email` (string) — (optional)

- PUT /api/customer/profile
  - Description: Update the logged-in customer's profile.
  - Authorization: `Authorization: Bearer <accessToken>` — required
  - Request Body (JSON): `CustomerProfileDTO` (same fields as above; validation applies)
  - Response: plain text message

---

**Product Catalog (public / customer view)**
- GET /api/customer/products
  - Description: List all available products.
  - Authorization: (optional) — endpoint is read-only; use token if available
  - Response Body: array of `ProductDTO`
    - `itemId` (string)
    - `itemName` (string)
    - `unitPrice` (decimal)
    - `availableStock` (integer)

---

**Customer Orders (requires authentication)**
- POST /api/customer/orders
  - Description: Place a new order.
  - Authorization: `Authorization: Bearer <accessToken>` — required
  - Request Body (JSON): `OrderDTO`(Omited non used fileds)
    - `orderId` (string) — (created by server)
    - `customerId` (string) — (server uses authenticated user)
    - `items` (array of `OrderItemDTO`) — required, at least one
      - `itemId` (string) — required
      - `quantity` (integer) — required, positive
  - Response: created `OrderDTO`

- GET /api/customer/orders
  - Description: View customer's orders (merged behavior):
    - If `orderId` query provided → returns a list containing the single order.
    - If `page` and `size` provided → returns paginated `Page<OrderDTO>`.
    - Otherwise → returns full list of `OrderDTO`.
  - Query Parameters:
    - `orderId` (string) — (optional)
    - `page` (integer) — (optional)
    - `size` (integer) — (optional)
  - Authorization: `Authorization: Bearer <accessToken>` — required
  - Response: list or page of `OrderDTO` as described above

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
  - Response: list or `Page<OrderDTO>` as applicable

- PUT /api/admin/orders/status
  - Description: Bulk update status for multiple orders. Each update is independent.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body (JSON): array of `BulkOrderStatusUpdateDTO`
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
  - Response: `AdminPricingDTO` or list/page
    - `itemId` (string) — required
    - `unitPrice` (decimal) — required, non-negative
    - `effectiveFrom` (datetime string `yyyy-MM-dd HH:mm:ss`) — (optional)

- POST /api/admin/prices
  - Description: Create price records in bulk.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body: array of `AdminPricingDTO` (see fields above)
  - Response: plain text message (201 Created)

- PUT /api/admin/prices
  - Description: Update prices in bulk.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body: array of `AdminPricingDTO`
  - Response: plain text message

---

**Admin — Inventory / Stock (requires ADMIN role)**
- GET /api/admin/inventory
  - Description: Get inventory items. Behavior:
    - `itemId` → returns single `InventoryItemDTO`.
    - `page` & `size` → returns paginated `Page<InventoryItemDTO>`.
    - otherwise → returns full list.
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
  - Request Body: array of `InventoryItemDTO` (fields above)
  - Response: list of created item IDs (201 Created)

- POST /api/admin/inventory/addstock
  - Description: Add stock amounts to existing inventory items.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body: array of `AddStockRequestDTO`
    - `itemId` (string) — required
    - `addStock` (integer) — required, > 0
  - Response: list of results

- PUT /api/admin/inventory
  - Description: Update multiple inventory items in bulk.
  - Authorization: `Authorization: Bearer <accessToken>` — required (ADMIN)
  - Request Body: array of `InventoryItemDTO`
  - Response: list of results

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
  - Response: plain text message

---

If you want, I can also:
- add example JSON payloads for each request,
- generate a Postman collection, or
- produce an OpenAPI spec for automatic docs.

File: [docs/API_BY_USER.md](docs/API_BY_USER.md)

---

## Example JSON payloads

Below are compact, practical examples you can use directly when calling the API. These examples are intentionally minimal (only required fields) unless noted.

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
[
  { "orderId": "ORD-1001", "newStatus": "SHIPPED" },
  { "orderId": "ORD-1002", "newStatus": "CANCELLED" }
]
```

- Admin: Create/update prices (POST/PUT /api/admin/prices)
```json
[
  { "itemId": "SKU-1234", "unitPrice": 19.99 },
  { "itemId": "SKU-5678", "unitPrice": 9.5 }
]
```

- Admin: Add inventory items (POST /api/admin/inventory)
```json
[
  { "itemId": "SKU-1234", "itemName": "Widget A", "availableStock": 100 }
]
```

- Admin: Add stock (POST /api/admin/inventory/addstock)
```json
[
  { "itemId": "SKU-1234", "addStock": 50 },
  { "itemId": "SKU-5678", "addStock": 20 }
]
```

- Analytics: Request monthly report (POST /api/admin/analytics/sendreportemail)
```json
{
  "month": "January",
  "year": 2026
}
```

---

File: [docs/API_BY_USER.md](docs/API_BY_USER.md)
