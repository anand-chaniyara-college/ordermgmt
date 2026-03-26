# API Reference — User Grouped (Current Implementation)

All endpoints return JSON. Authenticated routes require the `Authorization: Bearer <accessToken>` header.
Optional query parameters and optional request-body fields are explicitly marked as optional below.

## 1) Public Authentication

### POST `/api/auth/register`
Creates a new CUSTOMER account for a tenant. Public registration is CUSTOMER-only (any other role is rejected).

Request body:
```json
{
  "email": "customer1@test.com",
  "password": "customerpassword",
  "roleName": "CUSTOMER",
  "orgSubdomain": "test"
}
```

Field notes:
- `email` required.
- `password` required (min 6 chars).
- `roleName` required but must be `CUSTOMER` for this public endpoint.
- `orgSubdomain` required.

Responses:
- `201` `{ "message": "Registration successful" }`
- `403` `{ "error": "Forbidden", "message": "Only CUSTOMER registration is allowed on this endpoint" }`
- `404` `{ "error": "Resource Not Found", "message": "Organization not found: <subdomain>" }`
- `409` `{ "message": "Email already exists: <email>" }` or `{ "error": "Conflict", "message": "Organization is inactive: <subdomain>" }`
- `429` `{ "message": "Too many registration attempts. Please try again later." }`

Rate limiting: enforced per IP (configured via `app.rate-limit.auth.*`).

### POST `/api/auth/login`
Request body:
```json
{
  "orgSubdomain": "test",
  "email": "superadmin@superemail.com",
  "password": "superadminpassword"
}
```

Field notes:
- `orgSubdomain` required.
- `email` required.
- `password` required.

Response (`200`):
```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "role": "SUPER_ADMIN",
  "message": "Login successful",
  "refreshToken": "..."
}
```

### POST `/api/auth/refresh`
Header: `Authorization: Bearer <accessToken>`

Request body:
```json
{
  "refreshToken": "..."
}
```

Field notes:
- `refreshToken` required.
- `Authorization` header required.

Response (`200`):
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "message": "Token refreshed successfully"
}
```

### POST `/api/auth/logout`
Header: `Authorization: Bearer <accessToken>`

Request body:
```json
{
  "refreshToken": "..."
}
```

Field notes:
- `refreshToken` required.
- `Authorization` header required.

Response (`200`):
```json
{
  "message": "Logged out successfully"
}
```

### POST `/api/auth/forgot-password`
Rate-limited per IP (configured via `app.rate-limit.auth.*`).

Request body:
```json
{
  "email": "user@test.com",
  "orgSubdomain": "test"
}
```

Field notes:
- `email` required.
- `orgSubdomain` required.

Response (`200`):
```json
{
  "message": "If an account matches, a temporary password has been sent to your email."
}
```

### PATCH `/api/auth/reset-password`
Request body:
```json
{
  "email": "user@test.com",
  "orgSubdomain": "test",
  "temporaryPassword": "TEMP1234",
  "newPassword": "newpassword123"
}
```

Field notes:
- `email` required.
- `orgSubdomain` required.
- `temporaryPassword` required.
- `newPassword` required (min 8 chars).

Response (`200`):
```json
{
  "message": "Password has been successfully reset."
}
```

## 2) SUPER_ADMIN Endpoints

Header: `Authorization: Bearer <SUPER_ADMIN token>`

### POST `/api/super-admin/organizations`
Request body:
```json
{
  "name": "acme",
  "subdomain": "acme",
  "description": "Lorem ipsum dolor sit amet"
}
```

Field notes:
- `name` required.
- `subdomain` required.
- `description` optional.

Response (`201`):
```json
{
  "orgId": "...",
  "name": "acme",
  "subdomain": "acme",
  "isActive": true,
  "description": "Lorem ipsum dolor sit amet",
  "createdTimestamp": "2026-03-03T07:10:00.123"
}
```

Duplicate subdomain response (`409`):
```json
{
  "message": "Organization subdomain already exists: acme"
}
```

### GET `/api/super-admin/organizations`
Response (`200`):
```json
{
  "organizations": [
    {
      "orgId": "00000000-0000-0000-0000-000000000001",
      "name": "test",
      "subdomain": "test",
      "isActive": true,
      "description": "Lorem ipsum dolor sit amet",
      "createdTimestamp": "2026-03-02T18:34:01.123"
    }
  ]
}
```

### PATCH `/api/super-admin/organizations/{id}/status`
Request body:
```json
{
  "isActive": false
}
```

Field notes:
- `isActive` required.

Response (`200`):
```json
{
  "message": "Organization status updated successfully."
}
```

### POST `/api/super-admin/org-admins`
Request body:
```json
{
  "email": "orgadmin@acme.com",
  "password": "orgadminpassword",
  "orgId": "00000000-0000-0000-0000-000000000001"
}
```

Field notes:
- `email` required.
- `password` required (min 6 chars).
- `orgId` required (validated in service).

Response (`201`):
```json
{
  "userId": "...",
  "email": "orgadmin@acme.com",
  "role": "ORG_ADMIN",
  "orgId": "00000000-0000-0000-0000-000000000001",
  "isActive": true,
  "createdTimestamp": "2026-03-03T07:12:00.321",
  "message": "Org Admin created successfully."
}
```

Duplicate email response (`409`):
```json
{
  "message": "Email already exists: orgadmin@acme.com"
}
```

### GET `/api/super-admin/org-admins`
Response (`200`):
```json
{
  "orgAdmins": [
    {
      "userId": "...",
      "email": "orgadmin@test.com",
      "role": "ORG_ADMIN",
      "orgId": "00000000-0000-0000-0000-000000000001",
      "isActive": true,
      "createdTimestamp": "2026-03-02T18:34:01.30992",
      "message": null
    }
  ]
}
```

### PATCH `/api/super-admin/org-admins/{id}/status`
Request body:
```json
{
  "isActive": false
}
```

Field notes:
- `isActive` required.

Response (`200`):
```json
{
  "message": "Org Admin status updated successfully."
}
```

## 3) ORG_ADMIN Endpoints

Header: `Authorization: Bearer <ORG_ADMIN token>`

### POST `/api/org-admin/admins`
Request body:
```json
{
  "email": "admin@acme.com",
  "password": "adminpassword"
}
```

Field notes:
- `email` required.
- `password` required (min 6 chars).

Response (`201`):
```json
{
  "userId": "...",
  "email": "admin@acme.com",
  "role": "ADMIN",
  "orgId": "00000000-0000-0000-0000-000000000001",
  "isActive": true,
  "createdTimestamp": "2026-03-03T06:12:42.071637",
  "message": "Admin created successfully."
}
```

Duplicate email response (`409`):
```json
{
  "message": "Email already exists: admin@acme.com"
}
```

### GET `/api/org-admin/admins`
Response (`200`):
```json
{
  "admins": [
    {
      "userId": "...",
      "email": "admin@example.com",
      "role": "ADMIN",
      "orgId": "00000000-0000-0000-0000-000000000001",
      "isActive": true,
      "createdTimestamp": "2026-02-26T11:33:23.038744",
      "message": null
    }
  ]
}
```

### PATCH `/api/org-admin/admins/{id}/status`
Request body:
```json
{
  "isActive": false
}
```

Field notes:
- `isActive` required.

Response (`200`):
```json
{
  "message": "Admin status updated successfully."
}
```

### GET `/api/org-admin/analytics/revenue-report`
Query params:
- `startdate` required, `YYYY-MM-DD`
- `enddate` required, `YYYY-MM-DD`
- `itemname` optional, comma-separated list allowed, partial match (case-insensitive)
- `page` optional, 0-indexed
- `size` optional (pagination only when both `page` and `size` are provided)
- `sendEmail` optional, `true|false`, default `false`
- `emailTo` optional (used only when `sendEmail=true`; defaults to logged-in user)

Validation notes:
- `startdate` must be before `enddate`.

Response (`200`):
```json
{
  "startDate": "2026-03-01",
  "endDate": "2026-03-10",
  "totalSoldItems": 2,
  "totalSoldQty": 5,
  "totalRevenue": 500.00,
  "items": [
    {
      "itemId": "00000000-0000-0000-0000-000000000010",
      "itemName": "Laptop",
      "totalRevenue": 500.00,
      "sales": [
        { "soldQty": 2, "soldOn": "2026-03-05T10:15:00" }
      ]
    }
  ]
}
```

### GET `/api/org-admin/analytics/order-analytics`
Query params:
- `startdate` optional, `YYYY-MM-DD`
- `enddate` optional, `YYYY-MM-DD`
- `itemname` optional, comma-separated list allowed, partial match (case-insensitive)
- `orderstatus` optional, comma-separated list allowed, values: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED` (alias `CANCELED` accepted)
- `page` optional, 0-indexed
- `size` optional (pagination only when both `page` and `size` are provided)
- `sendEmail` optional, `true|false`, default `false`
- `emailTo` optional (used only when `sendEmail=true`; defaults to logged-in user)

Validation notes:
- `startdate` must be before `enddate`.

Response (`200`):
```json
{
  "startDate": "2026-03-01",
  "endDate": "2026-03-10",
  "totalSoldItems": 2,
  "totalSoldQty": 5,
  "items": [
    {
      "itemId": "00000000-0000-0000-0000-000000000010",
      "itemName": "Laptop",
      "sales": [
        { "orderStatus": "DELIVERED", "soldQty": 2, "soldOn": "2026-03-05T10:15:00" }
      ]
    }
  ]
}
```

## 4) ADMIN Endpoints

Header: `Authorization: Bearer <ADMIN token>`

### GET `/api/admin/orders`
Query params:
- `orderId` optional UUID (if provided, `page`/`size` are ignored)
- `page` optional, 0-indexed
- `size` optional

Behavior:
- If `orderId` is provided, returns that specific order in `{ "orders": [...] }`
- If both `page` and `size` are provided, returns a paged response
- If `page` and `size` are omitted, the endpoint applies a default page of `0` and size of `50`, and returns up to 50 orders in `{ "orders": [...] }`

Responses:
- If `orderId` is provided:
```json
{
  "orders": [
    {
      "orderId": "00000000-0000-0000-0000-000000000201",
      "status": "PENDING",
      "createdTimestamp": "2026-03-10T10:00:00",
      "updatedTimestamp": "2026-03-10T10:00:00",
      "items": [
        { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
      ],
      "totalAmount": 500.00
    }
  ]
}
```
- If `page` and `size` are provided (paged response):
```json
{
  "content": [
    {
      "orderId": "00000000-0000-0000-0000-000000000201",
      "status": "PENDING",
      "createdTimestamp": "2026-03-10T10:00:00",
      "updatedTimestamp": "2026-03-10T10:00:00",
      "items": [
        { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
      ],
      "totalAmount": 500.00
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
```
- If `page` and `size` are omitted (default first 50 orders):
```json
{
  "orders": [
    {
      "orderId": "00000000-0000-0000-0000-000000000201",
      "status": "PENDING",
      "createdTimestamp": "2026-03-10T10:00:00",
      "updatedTimestamp": "2026-03-10T10:00:00",
      "items": [
        { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
      ],
      "totalAmount": 500.00
    }
  ]
}
```

### PUT `/api/admin/orders/status`
Bulk update order status.

Request body:
```json
{
  "orders": [
    { "orderId": "00000000-0000-0000-0000-000000000101", "newStatus": "SHIPPED" },
    { "orderId": "00000000-0000-0000-0000-000000000102", "newStatus": "DELIVERED" }
  ]
}
```

Field notes:
- `orders` required, non-empty.
- `orderId` required for each entry.
- `newStatus` required and case-insensitive. Allowed values: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
- Allowed admin transitions: `PENDING` -> `CONFIRMED` or `CANCELLED`.
- Allowed admin transitions: `CONFIRMED` -> `PROCESSING` or `CANCELLED`.
- Allowed admin transitions: `PROCESSING` -> `SHIPPED` or `CANCELLED`.
- Allowed admin transitions: `SHIPPED` -> `DELIVERED`.

Response (`200`):
```json
{
  "successes": [
    {
      "orderId": "00000000-0000-0000-0000-000000000201",
      "status": "SHIPPED",
      "createdTimestamp": "2026-03-10T10:00:00",
      "updatedTimestamp": "2026-03-11T09:00:00",
      "items": [
        { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
      ],
      "totalAmount": 500.00
    }
  ],
  "failures": [
    { "orderId": "00000000-0000-0000-0000-000000000103", "error": "Order not found" }
  ]
}
```

### GET `/api/admin/prices`
Query params:
- `itemId` optional UUID (if provided, `page`/`size` are ignored)
- `page` optional, 0-indexed
- `size` optional (pagination only when both `page` and `size` are provided)

Responses:
- If `itemId` is provided:
```json
{
  "itemId": "00000000-0000-0000-0000-000000000010",
  "unitPrice": 250.00,
  "effectiveFrom": "2026-03-01 09:00:00"
}
```
- If `page` and `size` are provided (paged response):
```json
{
  "content": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "unitPrice": 250.00, "effectiveFrom": "2026-03-01 09:00:00" }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
```
- Otherwise:
```json
{
  "prices": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "unitPrice": 250.00, "effectiveFrom": "2026-03-01 09:00:00" }
  ]
}
```

Notes:
- If a product has no price yet, `unitPrice` and `effectiveFrom` can be `null` in responses.

### POST `/api/admin/prices`
Create price records in bulk.

Request body:
```json
{
  "price": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "unitPrice": 250.00 }
  ]
}
```

Field notes:
- `itemId` required.
- `unitPrice` required (non-negative).
- `effectiveFrom` optional (if omitted, current timestamp is used).
- `price` list must be non-empty.

Notes:
- Fails with `400` if a price already exists for the item.

Response (`201`):
```json
{
  "message": "Price records added successfully."
}
```

### PUT `/api/admin/prices`
Update price records in bulk.

Request body:
```json
{
  "price": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "unitPrice": 275.00 }
  ]
}
```

Field notes:
- `itemId` required.
- `unitPrice` required (non-negative).
- `effectiveFrom` optional (if omitted, current timestamp is used).
- `price` list must be non-empty.

Notes:
- Fails with `404` if no existing price record exists for the item.

Response (`200`):
```json
{
  "message": "Prices updated successfully."
}
```

### GET `/api/admin/inventory`
Query params:
- `itemId` optional UUID (if provided, `page`/`size` are ignored)
- `page` optional, 0-indexed
- `size` optional (pagination only when both `page` and `size` are provided)

Responses:
- If `itemId` is provided:
```json
{
  "itemId": "00000000-0000-0000-0000-000000000010",
  "itemName": "Laptop",
  "availableStock": 15,
  "reservedStock": 0
}
```
- If `page` and `size` are provided (paged response):
```json
{
  "content": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "availableStock": 15, "reservedStock": 0 }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
```
- Otherwise:
```json
{
  "inventory": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "availableStock": 15, "reservedStock": 0 }
  ]
}
```

### POST `/api/admin/inventory`
Add inventory items in bulk.

Request body:
```json
{
  "inventory": [
    { "itemName": "Laptop", "availableStock": 10, "reservedStock": 0 }
  ]
}
```

Field notes:
- `itemName` required for creation.
- `availableStock` required (>= 0).
- `reservedStock` optional (defaults to `0` if omitted).
- `inventory` list must be non-empty.

Response (`201`):
```json
{
  "items": [ "00000000-0000-0000-0000-000000000010" ]
}
```

### POST `/api/admin/inventory/addstock`
Add stock to existing items.

Request body:
```json
{
  "addstock": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "addStock": 5 }
  ]
}
```

Field notes:
- `itemId` required.
- `addStock` required (> 0).
- `addstock` list must be non-empty.

Response (`200`):
```json
{
  "items": [ "00000000-0000-0000-0000-000000000010" ]
}
```

### PUT `/api/admin/inventory`
Update inventory items in bulk.

Request body:
```json
{
  "inventory": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "availableStock": 15, "reservedStock": 0 }
  ]
}
```

Field notes:
- `itemId` required.
- `availableStock` required (>= 0).
- `itemName` optional (only updated when provided and non-blank).
- `reservedStock` is ignored on update (not persisted by the current implementation).
- `inventory` list must be non-empty.

Response (`200`):
```json
{
  "items": [ "00000000-0000-0000-0000-000000000010" ]
}
```

### DELETE `/api/admin/inventory/{ids}`
Delete inventory items by comma-separated UUIDs in the path.

Field notes:
- `ids` required, comma-separated UUIDs.

Example: `DELETE /api/admin/inventory/00000000-0000-0000-0000-000000000010,00000000-0000-0000-0000-000000000011`

Response: `204 No Content`

## 5) CUSTOMER Endpoints

Header: `Authorization: Bearer <CUSTOMER token>`

### GET `/api/customer/profile`
Response (`200`):
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "contactNo": "1234567890",
  "address": "123 Main St",
  "email": "john.doe@test.com"
}
```

Notes:
- If the profile does not exist yet, the response may contain only `email` and other fields as `null`.

### PUT `/api/customer/profile`
Email is read-only; changing it returns a `400`.

Request body:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "contactNo": "1234567890",
  "address": "123 Main St",
  "email": "john.doe@test.com"
}
```

Field notes:
- `firstName` required.
- `lastName` required.
- `contactNo` optional.
- `address` optional (but required to place an order).
- `email` optional; if provided, must match the logged-in user (email cannot be changed).

Response (`200`):
```json
{
  "message": "Profile updated successfully"
}
```

### GET `/api/customer/products`
Query params:
- `page` optional, 0-indexed
- `size` optional (pagination only when both `page` and `size` are provided)

Responses:
- If `page` and `size` are provided (paged response):
```json
{
  "content": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "unitPrice": 250.00, "availableStock": 15 }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
```
- Otherwise:
```json
{
  "products": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "unitPrice": 250.00, "availableStock": 15 }
  ]
}
```

### POST `/api/customer/orders`
Request body:
```json
{
  "items": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "quantity": 2 }
  ]
}
```

Field notes:
- `items` required, non-empty.
- `itemId` required for each item.
- `quantity` required and must be > 0.
- The customer profile must have `firstName`, `lastName`, and `address` set before ordering.

Notes:
- If stock is insufficient or item IDs are invalid, the request fails with `400`.

Response (`201`):
```json
{
  "orderId": "00000000-0000-0000-0000-000000000201",
  "status": "PENDING",
  "createdTimestamp": "2026-03-10T10:00:00",
  "updatedTimestamp": "2026-03-10T10:00:00",
  "items": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
  ],
  "totalAmount": 500.00
}
```

### GET `/api/customer/orders`
Query params:
- `orderId` optional UUID (if provided, `page`/`size` are ignored)
- `page` optional, 0-indexed
- `size` optional (pagination only when both `page` and `size` are provided)

Responses:
- If `orderId` is provided:
```json
{
  "orders": [
    {
      "orderId": "00000000-0000-0000-0000-000000000201",
      "status": "PENDING",
      "createdTimestamp": "2026-03-10T10:00:00",
      "updatedTimestamp": "2026-03-10T10:00:00",
      "items": [
        { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
      ],
      "totalAmount": 500.00
    }
  ]
}
```
- If `page` and `size` are provided (paged response):
```json
{
  "content": [
    {
      "orderId": "00000000-0000-0000-0000-000000000201",
      "status": "PENDING",
      "createdTimestamp": "2026-03-10T10:00:00",
      "updatedTimestamp": "2026-03-10T10:00:00",
      "items": [
        { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
      ],
      "totalAmount": 500.00
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "numberOfElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
```
- Otherwise:
```json
{
  "orders": [
    {
      "orderId": "00000000-0000-0000-0000-000000000201",
      "status": "PENDING",
      "createdTimestamp": "2026-03-10T10:00:00",
      "updatedTimestamp": "2026-03-10T10:00:00",
      "items": [
        { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
      ],
      "totalAmount": 500.00
    }
  ]
}
```

### PUT `/api/customer/orders/{orderId}/cancel`
Response (`200`):
```json
{
  "orderId": "00000000-0000-0000-0000-000000000201",
  "status": "CANCELLED",
  "createdTimestamp": "2026-03-10T10:00:00",
  "updatedTimestamp": "2026-03-10T11:00:00",
  "items": [
    { "itemId": "00000000-0000-0000-0000-000000000010", "itemName": "Laptop", "quantity": 2, "unitPrice": 250.00, "subTotal": 500.00 }
  ],
  "totalAmount": 500.00
}
```

Notes:
- Only the order owner can cancel.
- Only orders in `PENDING` status can be cancelled.

## 6) Role Access Rules

- `/api/super-admin/**` -> `SUPER_ADMIN`
- `/api/org-admin/**` -> `ORG_ADMIN`
- `/api/admin/**` -> `ADMIN`
- `/api/customer/**` -> `CUSTOMER`
