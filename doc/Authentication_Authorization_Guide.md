# Spring Boot Authentication & Authorization - Complete Teaching Guide

> [!IMPORTANT]
> This guide is designed for **complete beginners** learning Spring Boot for the first time. We'll build **backend REST APIs only** with a focus on **real-world enterprise practices**.

---

## 📚 Table of Contents
1. [Foundational Concepts](#1-foundational-concepts)
2. [Authentication Flow](#2-authentication-flow)
3. [Token-Based Authentication](#3-token-based-authentication)
4. [Module Structure & Packages](#4-module-structure--packages)
5. [Layer Responsibilities](#5-layer-responsibilities)
6. [Database Tables Usage](#6-database-tables-usage)
7. [API Contract & JSON Responses](#7-api-contract--json-responses)
8. [Minimal Code Examples](#8-minimal-code-examples)

---

## 1. Foundational Concepts

### 1.1 Authentication vs Authorization

**Authentication** = "Who are you?"
- Verifying the identity of a user
- Example: Login with email and password
- Question: Is this really John Doe?

**Authorization** = "What can you do?"
- Verifying what a user is allowed to access
- Example: Only admins can delete orders
- Question: Does John Doe have permission to perform this action?

**Real-World Analogy:**
- **Authentication**: Showing your ID card to enter a building
- **Authorization**: Your ID card determines which floors you can access

---

### 1.2 Stateless vs Stateful Authentication

#### Stateful (Traditional - Session-based)
```
User logs in → Server creates session → Stores session in memory/database
Every request → Server checks session → Validates user
```
**Problem**: Doesn't scale well for distributed systems (multiple servers)

#### Stateless (Modern - Token-based)
```
User logs in → Server creates JWT token → Returns token to client
Every request → Client sends token → Server validates token (no DB lookup needed)
```
**Benefit**: Scalable, works perfectly with REST APIs and microservices

> [!NOTE]
> **We'll use STATELESS token-based authentication** because:
> - REST APIs should be stateless (industry standard)
> - Better for microservices architecture
> - Scales horizontally across multiple servers
> - JWT (JSON Web Token) is the industry standard

---

### 1.3 What is JWT (JSON Web Token)?

A JWT is a **self-contained token** that carries information about the user.

**Structure**: `header.payload.signature`

Example:
```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxMjM0Iiwicm9sZSI6IkNVU1RPTUVSIn0.SflKxwRJSMeKKF2QT4fwpM
```

**3 Parts:**

1. **Header** (algorithm used)
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

2. **Payload** (user data - NOT sensitive!)
```json
{
  "userId": "abc-123-xyz",
  "email": "john@example.com",
  "role": "ROLE_CUSTOMER",
  "exp": 1735123456
}
```

3. **Signature** (verifies token wasn't tampered with)
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret-key
)
```

> [!CAUTION]
> **Never store sensitive data (passwords, credit cards) in JWT payload!**
> The payload is only **encoded** (Base64), NOT **encrypted**. Anyone can decode and read it.

---

### 1.4 Access Token vs Refresh Token

#### Access Token (Short-lived)
- **Purpose**: Used to access protected resources
- **Lifespan**: Short (15 minutes to 1 hour)
- **Storage**: Client stores it (memory, localStorage)
- **Usage**: Sent with every API request in `Authorization` header

**Why short-lived?**
- If stolen, attacker has limited time to misuse it
- Reduces security risk

#### Refresh Token (Long-lived)
- **Purpose**: Used to get a new access token when it expires
- **Lifespan**: Long (days, weeks, or months)
- **Storage**: Stored in database (APP_USER table) AND client
- **Usage**: Only sent to `/refresh-token` endpoint

**Why needed?**
- User doesn't have to login repeatedly
- Better user experience
- Can be revoked by server (logout, security breach)

**Real-World Analogy:**
- **Access Token**: Your office key card (expires daily)
- **Refresh Token**: Your permanent employee ID (can renew your key card)

---

### 1.5 Role-Based Access Control (RBAC)

**Concept**: Different users have different roles, and roles determine permissions.

In your system:
- **ROLE_ADMIN**: Can manage inventory, view all orders, manage users
- **ROLE_CUSTOMER**: Can create orders, view their own orders only

**How it works:**

```
Request → Check Access Token → Extract role from JWT → 
Verify role has permission → Allow/Deny access
```

**Example Scenarios:**

| Endpoint | Admin | Customer |
|----------|-------|----------|
| `POST /api/orders` | ✅ | ✅ |
| `GET /api/orders/{orderId}` | ✅ (all orders) | ✅ (own orders only) |
| `PUT /api/inventory/{itemId}` | ✅ | ❌ |
| `GET /api/users` | ✅ | ❌ |

---

## 2. Authentication Flow

### 2.1 Complete Flow Diagram

```
┌─────────────┐                                    ┌─────────────┐
│   Client    │                                    │   Server    │
│ (Frontend/  │                                    │ (Spring     │
│   Postman)  │                                    │   Boot)     │
└──────┬──────┘                                    └──────┬──────┘
       │                                                  │
       │ 1. POST /api/auth/login                         │
       │    { email, password }                          │
       ├────────────────────────────────────────────────►│
       │                                                  │
       │                              2. Validate email  │
       │                              Query: APP_USER    │
       │                              table by email     │
       │                                                  │
       │                              3. Check password  │
       │                              using BCrypt       │
       │                                                  │
       │                              4. Get role from   │
       │                              USER_ROLE table    │
       │                                                  │
       │                              5. Generate JWT    │
       │                              Access Token       │
       │                              (15 min expiry)    │
       │                                                  │
       │                              6. Generate JWT    │
       │                              Refresh Token      │
       │                              (30 days expiry)   │
       │                                                  │
       │                              7. Save refresh    │
       │                              token in           │
       │                              REFRESH_TOKEN table│
       │                                                  │
       │ 8. Response: 200 OK                             │
       │    {                                            │
       │      "accessToken": "eyJhbG...",                │
       │      "refreshToken": "eyJhbG...",               │
       │      "tokenType": "Bearer",                     │
       │      "expiresIn": 900                           │
       │    }                                            │
       │◄────────────────────────────────────────────────┤
       │                                                  │
       │ 9. Store tokens in client                       │
       │                                                  │
       │ 10. Make authenticated request                  │
       │     GET /api/orders                             │
       │     Authorization: Bearer eyJhbG...             │
       ├────────────────────────────────────────────────►│
       │                                                  │
       │                              11. Extract token  │
       │                              from header        │
       │                                                  │
       │                              12. Validate JWT   │
       │                              signature          │
       │                                                  │
       │                              13. Extract userId │
       │                              and role from      │
       │                              token payload      │
       │                                                  │
       │                              14. Check role     │
       │                              authorization      │
       │                                                  │
       │                              15. Process request│
       │                              (fetch orders)     │
       │                                                  │
       │ 16. Response: 200 OK                            │
       │     { orders: [...] }                           │
       │◄────────────────────────────────────────────────┤
       │                                                  │
```

---

### 2.2 Step-by-Step Login Flow

#### Step 1: User Initiates Login
- Client sends `POST /api/auth/login`
- Request body: `{ "email": "john@example.com", "password": "secret123" }`

#### Step 2: Validate Email (Database Lookup)
- Query: `SELECT * FROM APP_USER WHERE email = 'john@example.com'`
- If not found → Return `401 Unauthorized: Invalid credentials`
- If found → Proceed to Step 3

#### Step 3: Verify Password
- Retrieve `passwordHash` from APP_USER table
- Use **BCrypt** to compare:
  ```
  BCrypt.checkpw(plainPassword, passwordHash)
  ```
- Why BCrypt? Because passwords should **NEVER** be stored in plain text
- If password doesn't match → Return `401 Unauthorized: Invalid credentials`
- If matches → Proceed to Step 4

> [!WARNING]
> **Never compare passwords using `==` or `.equals()`**
> Always use BCrypt or similar hashing algorithms.
> 
> **Why?** Raw password comparison is vulnerable to timing attacks and requires storing passwords in plain text (huge security risk).

#### Step 4: Fetch User Role
- From APP_USER, we have `roleId`
- Query: `SELECT roleName FROM USER_ROLE WHERE roleId = ?`
- Result: `ROLE_ADMIN` or `ROLE_CUSTOMER`

#### Step 5: Generate Access Token (JWT)
- Create JWT with payload:
```json
{
  "userId": "abc-123-xyz",
  "email": "john@example.com",
  "role": "ROLE_CUSTOMER",
  "type": "access",
  "iat": 1735000000,
  "exp": 1735000900
}
```
- Sign with secret key
- Set expiry: **15 minutes** (900 seconds)

#### Step 6: Generate Refresh Token (JWT)
- Create JWT with payload:
```json
{
  "userId": "abc-123-xyz",
  "type": "refresh",
  "tokenId": "token-uuid-456",
  "iat": 1735000000,
  "exp": 1737592000
}
```
- Sign with different/same secret key
- Set expiry: **30 days** (2,592,000 seconds)

#### Step 7: Save Refresh Token to Database
- Insert into REFRESH_TOKEN table:
```sql
INSERT INTO REFRESH_TOKEN (tokenId, userId, token, expiryDate, revoked)
VALUES ('token-uuid-456', 'abc-123-xyz', 'eyJhbG...', '2026-03-07 12:00:00', false);
```

**Why save to database?**
- Can revoke tokens during logout
- Can invalidate all tokens if account is compromised
- Can implement "logout from all devices"

#### Step 8: Return Response to Client
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "userId": "abc-123-xyz",
  "email": "john@example.com",
  "role": "ROLE_CUSTOMER"
}
```

---

### 2.3 Logout Flow

```
Client                                         Server
   │                                              │
   │ POST /api/auth/logout                        │
   │ Authorization: Bearer {refreshToken}         │
   ├─────────────────────────────────────────────►│
   │                                              │
   │                          Extract refresh     │
   │                          token from header   │
   │                                              │
   │                          Validate token      │
   │                          signature           │
   │                                              │
   │                          Extract tokenId     │
   │                          from payload        │
   │                                              │
   │                          Mark token as       │
   │                          revoked in DB:      │
   │                          UPDATE REFRESH_TOKEN│
   │                          SET revoked = true  │
   │                          WHERE tokenId = ?   │
   │                                              │
   │ 200 OK                                       │
   │ { "message": "Logged out successfully" }     │
   │◄─────────────────────────────────────────────┤
   │                                              │
   │ Clear tokens from client storage             │
   │                                              │
```

**Key Points:**
- Logout marks refresh token as `revoked = true` in database
- Access tokens **cannot** be invalidated (they expire naturally)
- This is why access tokens have short lifespans

---

### 2.4 Refresh Token Flow

When access token expires, client uses refresh token to get a new one:

```
Client                                         Server
   │                                              │
   │ POST /api/auth/refresh                       │
   │ { "refreshToken": "eyJhbG..." }              │
   ├─────────────────────────────────────────────►│
   │                                              │
   │                          Validate JWT        │
   │                          signature           │
   │                                              │
   │                          Extract tokenId     │
   │                                              │
   │                          Check in DB:        │
   │                          SELECT * FROM       │
   │                          REFRESH_TOKEN       │
   │                          WHERE tokenId = ?   │
   │                          AND revoked = false │
   │                          AND expiryDate > now│
   │                                              │
   │                          If not found/       │
   │                          revoked/expired →   │
   │                          Return 401          │
   │                                              │
   │                          Generate new        │
   │                          Access Token        │
   │                                              │
   │                          (Optional) Rotate:  │
   │                          Generate new        │
   │                          Refresh Token,      │
   │                          revoke old one      │
   │                                              │
   │ 200 OK                                       │
   │ {                                            │
   │   "accessToken": "new-token...",             │
   │   "refreshToken": "new-refresh..." (optional)│
   │ }                                            │
   │◄─────────────────────────────────────────────┤
   │                                              │
```

> [!TIP]
> **Refresh Token Rotation** (Advanced Security Practice):
> - When client uses a refresh token, server generates a NEW refresh token
> - Old refresh token is revoked
> - This limits the window of attack if a refresh token is stolen

---

## 3. Token-Based Authentication

### 3.1 How JWT Validation Works

Every protected endpoint follows this pattern:

```
Request arrives with header:
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

↓

1. Extract token from header
   - Remove "Bearer " prefix
   - Get: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

↓

2. Validate signature
   - Decode header + payload
   - Recompute signature using secret key
   - Compare with token's signature
   - If doesn't match → 401 Unauthorized (token tampered)

↓

3. Check expiration
   - Read "exp" claim from payload
   - Compare with current timestamp
   - If expired → 401 Unauthorized (token expired)

↓

4. Extract user information
   - userId: "abc-123-xyz"
   - email: "john@example.com"
   - role: "ROLE_CUSTOMER"

↓

5. Check authorization (role-based)
   - Does this role have permission for this endpoint?
   - If no → 403 Forbidden (insufficient permissions)

↓

6. Proceed with business logic
   - User is authenticated and authorized
```

---

### 3.2 Where Tokens Are Used

| Token Type | When Used | Where Sent | Purpose |
|------------|-----------|------------|---------|
| Access Token | Every API request | `Authorization: Bearer {token}` header | Authenticate user |
| Refresh Token | When access token expires | Request body or header to `/refresh` endpoint | Get new access token |

---

## 4. Module Structure & Packages

### 4.1 Project Package Organization

```
com.ordermgmt
│
├── config/                          # Configuration classes
│   ├── SecurityConfig.java          # Spring Security configuration
│   └── JwtConfig.java               # JWT settings (secret, expiry)
│
├── security/                        # Security-related components
│   ├── jwt/
│   │   ├── JwtTokenProvider.java    # Generate & validate JWT
│   │   ├── JwtAuthenticationFilter.java  # Intercept requests, extract token
│   │   └── JwtTokenValidator.java   # Token validation logic
│   │
│   └── CustomUserDetailsService.java # Load user from database
│
├── controller/                      # REST API endpoints
│   ├── AuthController.java          # /api/auth/* endpoints
│   ├── OrderController.java         # /api/orders/* endpoints
│   └── InventoryController.java     # /api/inventory/* endpoints
│
├── service/                         # Business logic
│   ├── AuthService.java             # Login, logout, refresh logic
│   ├── OrderService.java            # Order business logic
│   └── UserService.java             # User management logic
│
├── repository/                      # Database access (Spring Data JPA)
│   ├── AppUserRepository.java       # APP_USER table
│   ├── UserRoleRepository.java      # USER_ROLE table
│   ├── RefreshTokenRepository.java  # REFRESH_TOKEN table
│   ├── CustomerRepository.java      # CUSTOMER table
│   └── OrderRepository.java         # ORDERS table
│
├── entity/                          # JPA entities (map to DB tables)
│   ├── AppUser.java                 # APP_USER table
│   ├── UserRole.java                # USER_ROLE table
│   ├── RefreshToken.java            # REFRESH_TOKEN table
│   ├── Customer.java                # CUSTOMER table
│   └── Order.java                   # ORDERS table
│
├── dto/                             # Data Transfer Objects (API request/response)
│   ├── request/
│   │   ├── LoginRequest.java        # { email, password }
│   │   ├── RefreshTokenRequest.java # { refreshToken }
│   │   └── CreateOrderRequest.java  # Order creation payload
│   │
│   └── response/
│       ├── AuthResponse.java        # { accessToken, refreshToken, ... }
│       ├── ErrorResponse.java       # { error, message, timestamp }
│       └── OrderResponse.java       # Order details
│
└── exception/                       # Custom exceptions
    ├── InvalidCredentialsException.java
    ├── TokenExpiredException.java
    └── UnauthorizedException.java
```

---

### 4.2 Why This Structure?

#### Separation of Concerns
- **Each package has ONE responsibility**
- `controller` = handle HTTP requests/responses
- `service` = business logic
- `repository` = database operations
- `security` = authentication/authorization logic

#### Testability
- Each layer can be tested independently
- Mock dependencies easily

#### Maintainability
- Easy to locate code
- Clear boundaries between layers

#### Industry Standard
- This is how real-world Spring Boot apps are structured
- Easier for other developers to understand your code

---

## 5. Layer Responsibilities

### 5.1 Controller Layer (REST API Endpoints)

**Responsibility**: Handle HTTP requests and responses

**What it does:**
1. Receive HTTP request
2. Validate input (basic validation)
3. Call service layer
4. Return HTTP response (JSON)

**What it does NOT do:**
- No business logic
- No database access
- No password hashing
- No token generation

**Example endpoints:**

```
POST   /api/auth/login          → Login
POST   /api/auth/logout         → Logout
POST   /api/auth/refresh        → Refresh access token
POST   /api/auth/register       → Register new customer
```

**Conceptual Flow:**
```
AuthController receives POST /api/auth/login
    ↓
Extract { email, password } from request body
    ↓
Call authService.login(email, password)
    ↓
Receive AuthResponse from service
    ↓
Return 200 OK with JSON response
```

---

### 5.2 Service Layer (Business Logic)

**Responsibility**: Implement business rules

**What it does:**
1. Validate business rules
2. Orchestrate multiple repository calls
3. Hash passwords
4. Generate tokens
5. Handle transactions

**What it does NOT do:**
- No HTTP request/response handling
- No direct SQL queries (uses repositories)

**Example methods in AuthService:**

```
AuthResponse login(String email, String password)
    → Validate credentials
    → Generate tokens
    → Save refresh token
    → Return response

void logout(String refreshToken)
    → Validate token
    → Mark as revoked in database

AuthResponse refreshAccessToken(String refreshToken)
    → Validate refresh token
    → Generate new access token
    → (Optional) Rotate refresh token
```

**Conceptual Flow for Login:**
```
AuthService.login(email, password)
    ↓
1. Call appUserRepository.findByEmail(email)
   → Get user from APP_USER table
    ↓
2. If user not found → Throw InvalidCredentialsException
    ↓
3. Check passwordEncoder.matches(password, user.passwordHash)
   → Verify password using BCrypt
    ↓
4. If password wrong → Throw InvalidCredentialsException
    ↓
5. Call userRoleRepository.findById(user.roleId)
   → Get role name from USER_ROLE table
    ↓
6. Call jwtTokenProvider.generateAccessToken(user, role)
   → Generate JWT access token
    ↓
7. Call jwtTokenProvider.generateRefreshToken(user)
   → Generate JWT refresh token
    ↓
8. Create RefreshToken entity
    ↓
9. Call refreshTokenRepository.save(refreshTokenEntity)
   → Save to REFRESH_TOKEN table
    ↓
10. Build and return AuthResponse
    → { accessToken, refreshToken, expiresIn, ... }
```

---

### 5.3 Repository Layer (Database Access)

**Responsibility**: Execute database queries

**What it does:**
1. CRUD operations (Create, Read, Update, Delete)
2. Custom queries (if needed)

**What it does NOT do:**
- No business logic
- No password hashing
- No token generation

**Spring Data JPA Magic:**
- You define interfaces, Spring implements them automatically
- No need to write SQL for basic operations

**Example methods:**

```
AppUserRepository:
    findByEmail(String email) → Optional<AppUser>
    findById(String userId) → Optional<AppUser>
    save(AppUser user) → AppUser

RefreshTokenRepository:
    findByToken(String token) → Optional<RefreshToken>
    findByUserId(String userId) → List<RefreshToken>
    save(RefreshToken token) → RefreshToken
    deleteByTokenId(String tokenId) → void

UserRoleRepository:
    findById(Integer roleId) → Optional<UserRole>
    findByRoleName(String roleName) → Optional<UserRole>
```

> [!TIP]
> Spring Data JPA automatically implements methods based on their names:
> - `findByEmail` → `SELECT * FROM app_user WHERE email = ?`
> - `findByUserId` → `SELECT * FROM refresh_token WHERE user_id = ?`
> - `deleteByTokenId` → `DELETE FROM refresh_token WHERE token_id = ?`

---

### 5.4 Security Layer (JWT & Filters)

**Responsibility**: Handle JWT operations and request filtering

#### JwtTokenProvider
- Generate access tokens
- Generate refresh tokens
- Validate tokens
- Extract userId, email, role from tokens

#### JwtAuthenticationFilter
- Intercepts EVERY request
- Extracts token from `Authorization` header
- Validates token
- Sets authentication in Spring Security context

**Flow:**
```
Every HTTP request
    ↓
JwtAuthenticationFilter intercepts
    ↓
Extract token from header: "Authorization: Bearer {token}"
    ↓
Call jwtTokenProvider.validateToken(token)
    ↓
If valid → Extract user info → Set in SecurityContext
    ↓
If invalid → Clear SecurityContext → Request will fail with 401
    ↓
Request proceeds to controller
```

---

## 6. Database Tables Usage

### 6.1 APP_USER Table

**Purpose**: Store user accounts (both admin and customers)

**When Used:**
- **Login**: Query by email to validate credentials
- **Registration**: Insert new user
- **Token Generation**: Fetch userId and roleId for JWT payload

**Columns:**

| Column | Type | Purpose |
|--------|------|---------|
| userId | String (UUID) | Unique identifier |
| email | String (UNIQUE) | Login credential |
| passwordHash | String | BCrypt hashed password |
| roleId | int (FK) | Links to USER_ROLE table |
| isActive | boolean | Account status (can disable users) |
| createdTimestamp | Instant | Audit trail |

**Sample Data:**

| userId | email | passwordHash | roleId | isActive |
|--------|-------|-------------|--------|----------|
| admin-001 | admin@ordermgmt.com | $2a$10$... | 1 | true |
| cust-001 | john@example.com | $2a$10$... | 2 | true |

---

### 6.2 USER_ROLE Table (Lookup)

**Purpose**: Define available roles in the system

**When Used:**
- **Login**: Fetch role name for JWT payload
- **Authorization**: Check if role has permission

**Columns:**

| Column | Type | Purpose |
|--------|------|---------|
| roleId | int (PK) | Unique identifier |
| roleName | String (UNIQUE) | Role name (must start with ROLE_) |

**Pre-populated Data:**

| roleId | roleName |
|--------|----------|
| 1 | ROLE_ADMIN |
| 2 | ROLE_CUSTOMER |

> [!NOTE]
> **Why "ROLE_" prefix?**
> Spring Security convention. When checking authorization, you can use:
> - `@PreAuthorize("hasRole('ADMIN')")` → Automatically adds "ROLE_" prefix
> - `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` → Use full name

---

### 6.3 REFRESH_TOKEN Table

**Purpose**: Store active refresh tokens for logout and security

**When Used:**
- **Login**: Insert new refresh token
- **Logout**: Mark token as revoked
- **Refresh**: Validate token is not revoked and not expired
- **Security**: Revoke all tokens for a user (account compromise)

**Columns:**

| Column | Type | Purpose |
|--------|------|---------|
| tokenId | String (UUID, PK) | Unique identifier |
| userId | String (FK) | Links to APP_USER |
| token | String (UNIQUE) | The actual JWT refresh token |
| expiryDate | Instant | When token expires |
| revoked | boolean | Logout flag |

**Sample Data:**

| tokenId | userId | token | expiryDate | revoked |
|---------|--------|-------|------------|---------|
| token-001 | cust-001 | eyJhbG... | 2026-03-07 | false |
| token-002 | admin-001 | eyJhbG... | 2026-03-05 | true |

**Queries:**

```sql
-- On login: Save refresh token
INSERT INTO refresh_token (token_id, user_id, token, expiry_date, revoked)
VALUES (?, ?, ?, ?, false);

-- On logout: Revoke token
UPDATE refresh_token
SET revoked = true
WHERE token = ? AND user_id = ?;

-- On refresh: Validate token
SELECT * FROM refresh_token
WHERE token = ?
  AND revoked = false
  AND expiry_date > CURRENT_TIMESTAMP;

-- Security: Revoke all user's tokens (logout from all devices)
UPDATE refresh_token
SET revoked = true
WHERE user_id = ?;
```

---

### 6.4 CUSTOMER Table

**Purpose**: Store customer profile information

**Relationship**: One-to-one with APP_USER

**When Used:**
- **Registration**: Insert customer profile after creating APP_USER
- **Order Creation**: Link orders to customer
- **Profile Management**: Update customer details

**Important:**
- Only users with `ROLE_CUSTOMER` should have a CUSTOMER record
- Admin users do NOT have CUSTOMER records

**Flow:**
```
Register Customer:
    1. Create APP_USER (userId, email, password, roleId=2)
    2. Create CUSTOMER (customerId, userId, firstName, lastName, contactNo)
```

---

### 6.5 Table Relationships Summary

```
USER_ROLE (1) ─────► (M) APP_USER
                         │
                         ├──► (1) CUSTOMER
                         │
                         └──► (M) REFRESH_TOKEN
```

---

## 7. API Contract & JSON Responses

### 7.1 Login API

**Endpoint**: `POST /api/auth/login`

**Request:**
```json
{
  "email": "john@example.com",
  "password": "MySecurePassword123!"
}
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "userId": "cust-001",
  "email": "john@example.com",
  "role": "ROLE_CUSTOMER"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid email or password",
  "timestamp": "2026-02-05T17:57:49Z"
}
```

---

### 7.2 Logout API

**Endpoint**: `POST /api/auth/logout`

**Request Header:**
```
Authorization: Bearer {refreshToken}
```

**OR Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

---

### 7.3 Refresh Token API

**Endpoint**: `POST /api/auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "INVALID_REFRESH_TOKEN",
  "message": "Refresh token is invalid, expired, or revoked",
  "timestamp": "2026-02-05T17:57:49Z"
}
```

---

### 7.4 Register Customer API

**Endpoint**: `POST /api/auth/register`

**Request:**
```json
{
  "email": "jane@example.com",
  "password": "SecurePass456!",
  "firstName": "Jane",
  "lastName": "Doe",
  "contactNo": "+1234567890"
}
```

**Success Response (201 Created):**
```json
{
  "userId": "cust-002",
  "email": "jane@example.com",
  "customerId": "cust-002",
  "firstName": "Jane",
  "lastName": "Doe",
  "message": "Registration successful. Please login."
}
```

**Error Response (409 Conflict):**
```json
{
  "error": "EMAIL_ALREADY_EXISTS",
  "message": "An account with this email already exists",
  "timestamp": "2026-02-05T17:57:49Z"
}
```

---

### 7.5 Protected Endpoint Example (Get Orders)

**Endpoint**: `GET /api/orders`

**Request Header:**
```
Authorization: Bearer {accessToken}
```

**Success Response (200 OK) - For CUSTOMER:**
```json
{
  "orders": [
    {
      "orderId": "order-001",
      "customerId": "cust-001",
      "status": "PENDING",
      "createdAt": "2026-02-05T10:30:00Z",
      "items": [
        {
          "itemId": "ITEM-001",
          "quantity": 2,
          "unitPrice": 50.00
        }
      ],
      "totalAmount": 100.00
    }
  ]
}
```

**Success Response (200 OK) - For ADMIN:**
```json
{
  "orders": [
    // All orders in system, not just customer's orders
  ]
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "UNAUTHORIZED",
  "message": "Access token is missing or invalid",
  "timestamp": "2026-02-05T17:57:49Z"
}
```

**Error Response (403 Forbidden):**
```json
{
  "error": "FORBIDDEN",
  "message": "You do not have permission to access this resource",
  "timestamp": "2026-02-05T17:57:49Z"
}
```

---

## 8. Minimal Code Examples

> [!NOTE]
> These are **simplified examples** to understand the structure and flow.
> Production code needs error handling, validation, logging, etc.

### 8.1 AuthService - Login Method

```java
@Service
public class AuthService {
    
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;  // BCrypt
    private final JwtTokenProvider jwtTokenProvider;

    // Constructor injection (Spring Boot best practice)
    public AuthService(AppUserRepository appUserRepository,
                       UserRoleRepository userRoleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Login method - Authenticates user and generates tokens
     * 
     * @param email User's email
     * @param password User's plain password
     * @return AuthResponse containing tokens
     * @throws InvalidCredentialsException if email/password is wrong
     */
    public AuthResponse login(String email, String password) {
        
        // Step 1: Find user by email
        AppUser user = appUserRepository.findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        
        // Step 2: Verify password using BCrypt
        // Why BCrypt? It's designed to be slow, preventing brute-force attacks
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        // Step 3: Check if account is active
        if (!user.isActive()) {
            throw new AccountDisabledException("Your account has been disabled");
        }
        
        // Step 4: Get user's role name
        UserRole role = userRoleRepository.findById(user.getRoleId())
            .orElseThrow(() -> new DataIntegrityException("User role not found"));
        
        // Step 5: Generate Access Token (short-lived, 15 minutes)
        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getUserId(),
            user.getEmail(),
            role.getRoleName()
        );
        
        // Step 6: Generate Refresh Token (long-lived, 30 days)
        String tokenId = UUID.randomUUID().toString();
        String refreshTokenJwt = jwtTokenProvider.generateRefreshToken(
            user.getUserId(),
            tokenId
        );
        
        // Step 7: Save Refresh Token to database
        // Why? So we can revoke it during logout or security breach
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(tokenId);
        refreshToken.setUserId(user.getUserId());
        refreshToken.setToken(refreshTokenJwt);
        refreshToken.setExpiryDate(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);
        
        refreshTokenRepository.save(refreshToken);
        
        // Step 8: Build and return response
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshTokenJwt)
            .tokenType("Bearer")
            .expiresIn(900)  // 15 minutes in seconds
            .userId(user.getUserId())
            .email(user.getEmail())
            .role(role.getRoleName())
            .build();
    }
}
```

**Why this approach?**
- **Separation of concerns**: Service handles business logic, not HTTP
- **Dependency injection**: Spring provides dependencies automatically
- **Exception handling**: Throw custom exceptions, controller handles HTTP status
- **Security**: Passwords verified with BCrypt (never plain text comparison)
- **Stateless**: No session storage, just tokens

---

### 8.2 AuthService - Logout Method

```java
@Service
public class AuthService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Logout method - Revokes refresh token
     * 
     * Why revoke? So token cannot be used again to get new access tokens
     * 
     * @param refreshTokenJwt The refresh token to revoke
     * @throws InvalidTokenException if token is invalid
     */
    public void logout(String refreshTokenJwt) {
        
        // Step 1: Validate token signature and expiry
        if (!jwtTokenProvider.validateToken(refreshTokenJwt)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        // Step 2: Extract tokenId from JWT payload
        String tokenId = jwtTokenProvider.getTokenIdFromJwt(refreshTokenJwt);
        
        // Step 3: Find token in database
        RefreshToken token = refreshTokenRepository.findByTokenId(tokenId)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        
        // Step 4: Mark as revoked
        // Why not delete? Audit trail - we want to know when/if token was revoked
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        
        // Note: Access tokens cannot be revoked (they expire naturally)
        // This is why access tokens have short lifespans
    }
```

**Why this approach?**
- **Token validation first**: Ensure token is genuine before DB lookup
- **Revoke, don't delete**: Keeps audit trail for security analysis
- **Access tokens**: Cannot be revoked (expire on their own - security tradeoff)

---

### 8.3 AuthService - Refresh Token Method

```java
@Service
public class AuthService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh access token using a valid refresh token
     * 
     * @param refreshTokenJwt The refresh token
     * @return New access token
     * @throws InvalidTokenException if refresh token is invalid/expired/revoked
     */
    public AuthResponse refreshAccessToken(String refreshTokenJwt) {
        
        // Step 1: Validate token signature
        if (!jwtTokenProvider.validateToken(refreshTokenJwt)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        // Step 2: Extract data from token
        String tokenId = jwtTokenProvider.getTokenIdFromJwt(refreshTokenJwt);
        String userId = jwtTokenProvider.getUserIdFromJwt(refreshTokenJwt);
        
        // Step 3: Check token in database
        // Must be: not revoked AND not expired
        RefreshToken token = refreshTokenRepository.findByTokenId(tokenId)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        
        if (token.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        
        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }
        
        // Step 4: Get user details for new access token
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        UserRole role = userRoleRepository.findById(user.getRoleId())
            .orElseThrow(() -> new DataIntegrityException("User role not found"));
        
        // Step 5: Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(
            user.getUserId(),
            user.getEmail(),
            role.getRoleName()
        );
        
        // Step 6: (Optional) Refresh Token Rotation
        // Advanced security: Generate new refresh token, revoke old one
        // Uncomment for production-grade security
        /*
        String newTokenId = UUID.randomUUID().toString();
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, newTokenId);
        
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        
        RefreshToken newToken = new RefreshToken();
        newToken.setTokenId(newTokenId);
        newToken.setUserId(userId);
        newToken.setToken(newRefreshToken);
        newToken.setExpiryDate(Instant.now().plus(30, ChronoUnit.DAYS));
        newToken.setRevoked(false);
        refreshTokenRepository.save(newToken);
        */
        
        // Step 7: Return new access token
        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshTokenJwt)  // Or newRefreshToken if rotating
            .tokenType("Bearer")
            .expiresIn(900)
            .build();
    }
}
```

**Why this approach?**
- **Validate before DB lookup**: Catch tampered tokens early
- **Check revoked status**: Ensures logout actually works
- **Check expiry in DB**: Double validation (JWT + database)
- **Token rotation (commented)**: Advanced security practice for production

---

### 8.4 JwtTokenProvider - Generate & Validate Tokens

```java
@Component
public class JwtTokenProvider {
    
    // Secret key for signing JWT (should be in environment variables, not hardcoded)
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    // Access token expiry: 15 minutes (in milliseconds)
    @Value("${jwt.access.expiration}")
    private long accessTokenExpiry = 900000;  // 15 * 60 * 1000
    
    // Refresh token expiry: 30 days (in milliseconds)
    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiry = 2592000000L;  // 30 * 24 * 60 * 60 * 1000

    /**
     * Generate Access Token (short-lived)
     * 
     * Contains: userId, email, role
     * Used for: Every API request authentication
     */
    public String generateAccessToken(String userId, String email, String role) {
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiry);
        
        return Jwts.builder()
            .setSubject(userId)  // "sub" claim - who the token is about
            .claim("email", email)
            .claim("role", role)
            .claim("type", "access")  // Distinguish from refresh token
            .setIssuedAt(now)  // "iat" claim - when token was created
            .setExpiration(expiryDate)  // "exp" claim - when token expires
            .signWith(SignatureAlgorithm.HS256, jwtSecret)  // Sign with secret key
            .compact();
    }

    /**
     * Generate Refresh Token (long-lived)
     * 
     * Contains: userId, tokenId (to identify in database)
     * Used for: Getting new access token when it expires
     */
    public String generateRefreshToken(String userId, String tokenId) {
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiry);
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("tokenId", tokenId)  // Link to database record
            .claim("type", "refresh")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }

    /**
     * Validate JWT Token
     * 
     * Checks:
     * 1. Signature is valid (token not tampered)
     * 2. Token not expired
     * 
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)  // Use same secret to verify
                .parseClaimsJws(token);    // Will throw exception if invalid
            return true;
        } catch (SignatureException ex) {
            // Token signature doesn't match - token was tampered
            return false;
        } catch (ExpiredJwtException ex) {
            // Token has expired
            return false;
        } catch (Exception ex) {
            // Other errors (malformed token, etc.)
            return false;
        }
    }

    /**
     * Extract userId from token
     */
    public String getUserIdFromJwt(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getSubject();  // "sub" claim contains userId
    }

    /**
     * Extract email from token
     */
    public String getEmailFromJwt(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.get("email", String.class);
    }

    /**
     * Extract role from token
     */
    public String getRoleFromJwt(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.get("role", String.class);
    }

    /**
     * Extract tokenId from refresh token
     */
    public String getTokenIdFromJwt(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return claims.get("tokenId", String.class);
    }
}
```

**Why this approach?**
- **HMAC SHA-256**: Industry-standard signing algorithm (secure and fast)
- **Claims**: Store user data in token payload (no DB lookup on every request)
- **Expiry handling**: Automatic validation by JWT library
- **Secret key**: Should be in environment variables for security

---

### 8.5 JwtAuthenticationFilter - Intercept Requests

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * This filter runs BEFORE every controller method
     * 
     * Flow:
     * 1. Extract JWT from Authorization header
     * 2. Validate token
     * 3. Set authentication in Spring Security context
     * 4. Pass request to next filter/controller
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Step 1: Get JWT from Authorization header
            String jwt = getJwtFromRequest(request);
            
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                
                // Step 2: Extract user details from token
                String userId = jwtTokenProvider.getUserIdFromJwt(jwt);
                String email = jwtTokenProvider.getEmailFromJwt(jwt);
                String role = jwtTokenProvider.getRoleFromJwt(jwt);
                
                // Step 3: Create Spring Security authorities
                // "Authority" = permission/role in Spring Security
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority(role));
                
                // Step 4: Create Authentication object
                // This tells Spring Security "this user is authenticated"
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Step 5: Set in Security Context
                // Now Spring Security knows: this request is from an authenticated user
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // If token extraction/validation fails, just continue without authentication
            // The controller will return 401 if authentication is required
        }
        
        // Step 6: Continue with request
        // Pass to next filter or to controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT from Authorization header
     * 
     * Header format: "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * 
     * @return JWT string (without "Bearer " prefix), or null if not present
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // Check if Authorization header exists and starts with "Bearer "
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // Remove "Bearer " prefix
        }
        
        return null;
    }
}
```

**Why this approach?**
- **OncePerRequestFilter**: Ensures filter runs exactly once per request
- **SecurityContext**: Spring Security's way of storing authenticated user
- **Graceful failure**: If token is invalid, request continues (controller will reject)
- **No database lookup**: All user info comes from JWT (stateless!)

---

## 9. Summary & Key Takeaways

### 9.1 Authentication Flow Summary

```
Login → Validate credentials → Generate tokens → Return to client
                                     ↓
                          Save refresh token in DB

Every Request → Extract JWT → Validate signature → Check expiry → 
Extract user info → Check role → Allow/Deny

Logout → Find refresh token in DB → Mark as revoked

Refresh → Validate refresh token → Check not revoked → Generate new access token
```

---

### 9.2 Why This Architecture?

| Decision | Reason |
|----------|--------|
| **JWT over Sessions** | Stateless, scalable, microservice-friendly |
| **Short-lived access tokens** | Limits damage if stolen |
| **Long-lived refresh tokens** | Good UX (don't login repeatedly) |
| **Refresh tokens in DB** | Can revoke (logout, security) |
| **BCrypt for passwords** | Slow by design (prevents brute-force) |
| **Role-based authorization** | Simple, scalable permission model |
| **Layered architecture** | Separation of concerns, testability |

---

### 9.3 Security Best Practices

> [!CAUTION]
> **Critical Security Rules:**
> 1. **NEVER** store passwords in plain text
> 2. **NEVER** put sensitive data in JWT payload
> 3. **NEVER** hardcode JWT secret (use environment variables)
> 4. **NEVER** return different error messages for "email not found" vs "wrong password" (information leakage)
> 5. **ALWAYS** use HTTPS in production (prevents token stealing)
> 6. **ALWAYS** validate tokens on every request
> 7. **ALWAYS** check token expiry

---

### 9.4 What's Next?

After understanding this guide, you should be able to:
1. Explain authentication vs authorization
2. Describe JWT structure and purpose
3. Map the login/logout/refresh flows
4. Understand Spring Boot layered architecture
5. Know which table is used at which step

**When you're ready for code:**
- I can help you implement each component step-by-step
- We'll start with entities, then repositories, then services, then controllers
- Each step will build on the structure explained here

---

## 10. Quick Reference

### Database Tables

| Table | Purpose | Used In |
|-------|---------|---------|
| APP_USER | User accounts | Login, Registration |
| USER_ROLE | Role definitions | Login (get role name) |
| REFRESH_TOKEN | Active tokens | Login, Logout, Refresh |
| CUSTOMER | Customer profiles | Registration, Order creation |

### Tokens

| Token | Lifespan | Storage | Purpose |
|-------|----------|---------|---------|
| Access | 15 min | Client only | Authenticate API requests |
| Refresh | 30 days | Client + DB | Get new access token |

### Layers

| Layer | Responsibility | Example |
|-------|---------------|---------|
| Controller | HTTP handling | Receive POST /login |
| Service | Business logic | Validate credentials, generate tokens |
| Repository | Database | Find user by email |
| Security | Auth/Auth | Validate JWT, check roles |

---

> [!TIP]
> **Learning Path:**
> 1. Read this guide thoroughly
> 2. Draw the flows on paper
> 3. Understand each layer's responsibility
> 4. Ask questions about concepts you don't understand
> 5. Then we'll implement code step-by-step

This guide gives you the **complete mental model** you need before writing a single line of code.

---

**Questions to validate your understanding:**
1. What's the difference between access token and refresh token?
2. Why do we save refresh tokens in the database but not access tokens?
3. What happens to access tokens when a user logs out?
4. Which table do we query first during login?
5. What information goes into the JWT payload?
6. Why do we use BCrypt instead of SHA-256 for passwords?
7. What's the role of JwtAuthenticationFilter?

Once you can answer these, you're ready to implement! 🚀
