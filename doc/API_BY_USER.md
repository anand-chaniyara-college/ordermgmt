# API Reference — User Grouped (Current)

All endpoints return JSON.

## 1) Public Authentication

### POST `/api/auth/register`
Creates a new CUSTOMER account for a tenant.

Request body:
```json
{
  "email": "customer1@test.com",
  "password": "customerpassword",
  "roleName": "CUSTOMER",
  "orgSubdomain": "test"
}
```

Responses:
- `201` `{ "message": "Registration successful" }`
- `403` `{ "error": "Forbidden", "message": "Only CUSTOMER registration is allowed on this endpoint" }`
- `404` `{ "error": "Resource Not Found", "message": "Organization not found: <subdomain>" }`
- `409` `{ "message": "Email already exists: <email>" }` or `{ "error": "Conflict", "message": "Organization is inactive: <subdomain>" }`
- `429` `{ "message": "Too many registration attempts. Please try again later." }`

### POST `/api/auth/login`
Request body:
```json
{
  "email": "superadmin@superemail.com",
  "password": "superadminpassword"
}
```

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

Response (`200`):
```json
{
  "message": "Logged out successfully"
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

### POST `/api/super-admin/org-admins`
Request body:
```json
{
  "email": "orgadmin@acme.com",
  "password": "orgadminpassword",
  "orgId": "00000000-0000-0000-0000-000000000001"
}
```

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

Response (`200`):
```json
{
  "message": "Admin status updated successfully."
}
```

### GET `/api/org-admin/analytics/monthlyreport?month=february&year=2026`
Response (`200`):
```json
{
  "totalSoldItems": 10,
  "totalRevenue": 500.00,
  "items": [
    {
      "itemId": "...",
      "itemName": "Item 1",
      "totalSoldItems": 5,
      "totalRevenue": 250.00
    }
  ]
}
```

No data response (`404`):
```json
{
  "error": "Resource Not Found",
  "message": "No records found for FEBRUARY 2026"
}
```

### POST `/api/org-admin/analytics/sendreportemail`
Request body:
```json
{
  "month": "FEBRUARY",
  "year": 2026
}
```

Response (`200`):
```json
{
  "message": "Report email request submitted for orgadmin@test.com"
}
```

## 4) ADMIN Endpoints

Header: `Authorization: Bearer <ADMIN token>`

- `GET /api/admin/orders`
- `PUT /api/admin/orders/status`
- `GET /api/admin/prices`
- `POST /api/admin/prices`
- `PUT /api/admin/prices`
- `GET /api/admin/inventory`
- `POST /api/admin/inventory`
- `POST /api/admin/inventory/addstock`
- `PUT /api/admin/inventory`
- `DELETE /api/admin/inventory/{ids}`
- `GET /api/admin/analytics/monthlyreport`

Note: `POST /api/admin/analytics/sendreportemail` is intentionally removed. Report email is org-admin-only.

## 5) CUSTOMER Endpoints

Header: `Authorization: Bearer <CUSTOMER token>`

- `GET /api/customer/profile`
- `PUT /api/customer/profile`
- `GET /api/customer/products`
- `POST /api/customer/orders`
- `GET /api/customer/orders`
- `PUT /api/customer/orders/{orderId}/cancel`

## 6) Role Access Rules

- `/api/super-admin/**` -> `SUPER_ADMIN`
- `/api/org-admin/**` -> `ORG_ADMIN`
- `/api/admin/**` -> `ADMIN`
- `/api/customer/**` -> `CUSTOMER`
