# Zorvyn — API Design Document

> Comprehensive API architecture, endpoint specification, and design rationale for the Zorvyn Finance Dashboard Backend.

---

## Table of Contents

- [System Architecture](#system-architecture)
- [API Design Principles](#api-design-principles)
- [Authentication Architecture](#authentication-architecture)
- [Endpoint Specification](#endpoint-specification)
  - [Authentication Endpoints](#1-authentication---v1auth)
  - [User Management Endpoints](#2-user-management---v1users)
  - [Financial Record Endpoints](#3-financial-records---v1records)
  - [Dashboard Analytics Endpoints](#4-dashboard-analytics---v1dashboard)
- [Request / Response Schemas](#request--response-schemas)
- [Access Control Architecture](#access-control-architecture)
- [Error Handling Architecture](#error-handling-architecture)
- [Data Flow Diagrams](#data-flow-diagrams)
- [Database Schema Design](#database-schema-design)
- [Security Architecture](#security-architecture)
- [Pagination & Filtering Design](#pagination--filtering-design)

---

## System Architecture

### High-Level Architecture

```
                              ┌────────────────────┐
                              │   Client / Browser  │
                              │   (Frontend App)    │
                              └─────────┬──────────┘
                                        │
                                        │  HTTPS / REST
                                        │  Authorization: Bearer <JWT>
                                        ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                        SPRING BOOT APPLICATION                           │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                    SECURITY FILTER CHAIN                            │ │
│  │                                                                     │ │
│  │  ┌──────────┐   ┌───────────────┐   ┌───────────────────────────┐  │ │
│  │  │   CORS   │──▶│ JwtAuthFilter │──▶│  SecurityFilterChain      │  │ │
│  │  │  Filter  │   │ (extract JWT, │   │  (.authorizeHttpRequests  │  │ │
│  │  │          │   │  set context)  │   │   .permitAll / .authenticated)│ │
│  │  └──────────┘   └───────────────┘   └───────────────────────────┘  │ │
│  │                                                                     │ │
│  │  ┌──────────────────────────────────────────────────────────────┐   │ │
│  │  │  OAuth2LoginSuccessHandler (Google OAuth2 callback → JWT)    │   │ │
│  │  └──────────────────────────────────────────────────────────────┘   │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│                        ┌───────────▼────────────┐                       │
│                        │  @PreAuthorize (SpEL)   │                       │
│                        │  Method-level security  │                       │
│                        └───────────┬────────────┘                       │
│                                    │                                     │
│  ┌─────────────────────────────────▼───────────────────────────────────┐ │
│  │                      CONTROLLER LAYER                               │ │
│  │                                                                     │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌──────────────────────┐  │ │
│  │  │ AuthController  │  │ UserController  │  │FinancialRecord      │  │ │
│  │  │ /v1/auth/*      │  │ /v1/users/*     │  │Controller           │  │ │
│  │  │                 │  │                 │  │/v1/records/*        │  │ │
│  │  │ • register      │  │ • getAllUsers    │  │                     │  │ │
│  │  │ • login         │  │ • getUserById   │  │ • createRecord      │  │ │
│  │  │ • refresh       │  │ • createUser    │  │ • getAllRecords      │  │ │
│  │  │ • changePassword│  │ • updateUser    │  │ • getRecordById     │  │ │
│  │  │ • forgotPassword│  │ • updateStatus  │  │ • updateRecord      │  │ │
│  │  │ • resetPassword │  │ • deleteUser    │  │ • deleteRecord      │  │ │
│  │  └────────────────┘  └────────────────┘  └──────────────────────┘  │ │
│  │                                                                     │ │
│  │  ┌──────────────────────────────────────────────────────────────┐   │ │
│  │  │ FinancialDashboardController                                 │   │ │
│  │  │ /v1/dashboard/*                                              │   │ │
│  │  │ • summary  • categoryTotals  • monthlyTrends  • recentActivity│  │ │
│  │  └──────────────────────────────────────────────────────────────┘   │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│  ┌─────────────────────────────────▼───────────────────────────────────┐ │
│  │                       SERVICE LAYER                                 │ │
│  │                                                                     │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌──────────────────────┐  │ │
│  │  │AuthServiceImpl  │  │UserServiceImpl  │  │FinancialRecord      │  │ │
│  │  │                 │  │                 │  │ServiceImpl          │  │ │
│  │  │ • BCrypt encode │  │ • partial update│  │                     │  │ │
│  │  │ • JWT issuance  │  │ • email unique  │  │ • ownership check   │  │ │
│  │  │ • token mgmt    │  │ • status mgmt   │  │ • role-based filter │  │ │
│  │  └────────────────┘  └────────────────┘  │ • soft delete        │  │ │
│  │                                           └──────────────────────┘  │ │
│  │  ┌────────────────────────────────────────────────────────────────┐ │ │
│  │  │ FinancialDashboardServiceImpl                                  │ │ │
│  │  │ • sumIncome/sumExpense  • categoryTotals  • monthlyTrends     │ │ │
│  │  └────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                     │ │
│  │  ┌─────────────────────┐  ┌──────────────────────┐                 │ │
│  │  │ AuthenticationHelper│  │ UserResolutionUtil    │                 │ │
│  │  │ (JWT → email)       │  │ (role → userId filter)│                 │ │
│  │  └─────────────────────┘  └──────────────────────┘                 │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│  ┌─────────────────────────────────▼───────────────────────────────────┐ │
│  │                     REPOSITORY LAYER                                │ │
│  │  (Spring Data JPA + custom @Query JPQL)                             │ │
│  │                                                                     │ │
│  │  FinancialRecordController.java — /v1/records                       │ │
│  │  ────────────────────────────────────────────                       │ │
│  │  @PreAuthorize:                                                     │ │
│  │  POST   /          ADMIN         → recordService.createRecord(req,email) 201│ │
│  │  GET    /          ANALYST|ADMIN → recordService.getAllRecords(...)      200 │ │
│  │  GET    /{id}      ANALYST|ADMIN → recordService.getRecordById(id,email) 200 │ │
│  │  PATCH  /{id}      ADMIN         → recordService.updateRecord(id,r,e)    200 │ │
│  │  DELETE /{id}      ADMIN         → recordService.softDeleteRecord(id,e)  204 │ │
│  │                                                                     │ │
│  │  ┌────────────────┐  ┌──────────────────────────┐                  │ │
│  │  │ UserRepository  │  │ FinancialRecordRepository │                  │ │
│  │  │ • findByEmail   │  │ • findAllByFilters        │                  │ │
│  │  │                 │  │ • sumIncome / sumExpense   │                  │ │
│  │  └────────────────┘  │ • categoryTotals           │                  │ │
│  │                       │ • monthlyTrends            │                  │ │
│  │  ┌────────────────────│ • findRecentActivity       │                  │ │
│  │  │ PasswordResetToken │ • findByIdAndDeletedFalse  │                  │ │
│  │  │ Repository         └──────────────────────────┘                  │ │
│  │  └────────────────┘                                                 │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│  ┌─────────────────────────────────▼───────────────────────────────────┐ │
│  │                  EXCEPTION HANDLING LAYER                           │ │
│  │  GlobalExceptionHandler (@RestControllerAdvice)                     │ │
│  │                                                                     │ │
│  │  FinancialDashboardException → domain errors (any HTTP status)      │ │
│  │  MethodArgumentNotValidException → 400 (validation failures)        │ │
│  │  HttpMessageNotReadableException → 400 (malformed JSON)             │ │
│  │  MethodArgumentTypeMismatchException → 400 (bad param types)        │ │
│  │  AccessDeniedException → 403 (@PreAuthorize failures)               │ │
│  │  HttpRequestMethodNotSupportedException → 405 (wrong HTTP verb)     │ │
│  │  Exception (fallback) → 500 (generic, no details leaked)            │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────┬────────────────────────────────────────┘
                                   │
                          ┌────────▼────────┐
                          │   MySQL 8+       │
                          │                  │
                          │  ┌────────────┐  │
                          │  │   users     │  │
                          │  └──────┬─────┘  │
                          │         │ FK     │
                          │  ┌──────▼──────┐ │
                          │  │ financial_  │ │
                          │  │ records     │ │
                          │  └─────────────┘ │
                          │  ┌─────────────┐ │
                          │  │ password_   │ │
                          │  │ reset_tokens│ │
                          │  └─────────────┘ │
                          └─────────────────┘
```

---

## API Design Principles

| Principle | Implementation |
|---|---|
| **RESTful conventions** | Resources as nouns (`/users`, `/records`), HTTP verbs for actions |
| **Versioned API** | All endpoints under `/v1/` for future backward-compatible evolution |
| **Stateless authentication** | JWT in `Authorization: Bearer` header — no server-side sessions for API calls |
| **Consistent response shapes** | Success → resource DTO; Error → `ErrorResponse` with code/message/timestamp |
| **HATEOAS-ready IDs** | All responses include `id` fields for resource linkability |
| **Pagination by default** | List endpoints return `Page<T>` with metadata (totalElements, totalPages, etc.) |
| **Idempotent operations** | `PATCH` and `DELETE` are safe to retry |
| **Defence in depth** | Access control at filter, controller, and service layers |

---

## Authentication Architecture

### Auth Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                     AUTHENTICATION FLOWS                                 │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─── EMAIL/PASSWORD ────────────────────────────────────────────────┐  │
│  │                                                                    │  │
│  │  POST /v1/auth/register                                           │  │
│  │    ▼                                                               │  │
│  │  Validate password strength (uppercase + lowercase + digit)        │  │
│  │    ▼                                                               │  │
│  │  Check email uniqueness → 409 if exists                           │  │
│  │    ▼                                                               │  │
│  │  BCrypt encode password → save user (role=VIEWER, status=ACTIVE)  │  │
│  │    ▼                                                               │  │
│  │  Generate JWT (email, role, userId) → return AuthResponse          │  │
│  │                                                                    │  │
│  │  POST /v1/auth/login                                              │  │
│  │    ▼                                                               │  │
│  │  Find user by email → 401 if not found (generic msg)              │  │
│  │    ▼                                                               │  │
│  │  Check status == ACTIVE → 403 if inactive                         │  │
│  │    ▼                                                               │  │
│  │  BCrypt verify password → 401 if mismatch                         │  │
│  │    ▼                                                               │  │
│  │  Generate JWT → return AuthResponse                                │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌─── GOOGLE OAUTH2 ─────────────────────────────────────────────────┐  │
│  │                                                                    │  │
│  │  GET /oauth2/authorization/google                                  │  │
│  │    ▼                                                               │  │
│  │  Redirect to Google Consent Page                                   │  │
│  │    ▼                                                               │  │
│  │  Google callback → /login/oauth2/code/google                      │  │
│  │    ▼                                                               │  │
│  │  OAuth2LoginSuccessHandler:                                        │  │
│  │    ├─ Extract email + name from Google profile                    │  │
│  │    ├─ Find user by email                                          │  │
│  │    │   ├─ EXISTS → use existing user                              │  │
│  │    │   └─ NOT FOUND → create new (role=VIEWER, password=null)     │  │
│  │    ├─ Generate JWT                                                │  │
│  │    └─ Redirect to frontend: {redirect-uri}?token={JWT}            │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌─── PASSWORD RESET ─────────────────────────────────────────────────┐ │
│  │                                                                    │  │
│  │  POST /v1/auth/forgot-password                                    │  │
│  │    ▼                                                               │  │
│  │  Find user by email                                                │  │
│  │    ├─ NOT FOUND → return 200 silently (prevent enumeration)       │  │
│  │    └─ FOUND:                                                      │  │
│  │        ├─ Delete any existing unused tokens for this user         │  │
│  │        ├─ Generate UUID token (15-min TTL, single-use)            │  │
│  │        ├─ Save to password_reset_tokens table                     │  │
│  │        └─ Log token to console (dev) / send email (prod)          │  │
│  │                                                                    │  │
│  │  POST /v1/auth/reset-password                                     │  │
│  │    ▼                                                               │  │
│  │  Find token (must be unused) → 400 if invalid                     │  │
│  │    ▼                                                               │  │
│  │  Check expiry → 400 if expired                                    │  │
│  │    ▼                                                               │  │
│  │  Validate new password strength                                    │  │
│  │    ▼                                                               │  │
│  │  BCrypt encode → update user → mark token as used                  │  │
│  │    ▼                                                               │  │
│  │  Return 204 No Content                                             │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌─── TOKEN LIFECYCLE ────────────────────────────────────────────────┐  │
│  │                                                                    │  │
│  │  POST /v1/auth/refresh (requires valid JWT)                       │  │
│  │    ▼                                                               │  │
│  │  JwtAuthFilter validates existing token                            │  │
│  │    ▼                                                               │  │
│  │  Reload user from DB → check still active                         │  │
│  │    ▼                                                               │  │
│  │  Issue fresh JWT → return AuthResponse                             │  │
│  │                                                                    │  │
│  │  PATCH /v1/auth/change-password (requires valid JWT)              │  │
│  │    ▼                                                               │  │
│  │  Reject Google-only accounts (null password)                      │  │
│  │    ▼                                                               │  │
│  │  Verify current password via BCrypt                                │  │
│  │    ▼                                                               │  │
│  │  Validate new password strength → BCrypt encode → save             │  │
│  │    ▼                                                               │  │
│  │  Return 204 No Content                                             │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

### JWT Token Anatomy

```
Header:     { "alg": "HS256", "typ": "JWT" }
Payload:    {
              "sub": "user@example.com",    ← email as subject
              "role": "ANALYST",            ← custom claim
              "userId": 42,                 ← custom claim
              "iat": 1702641600,            ← issued at
              "exp": 1702728000             ← expires (24h later)
            }
Signature:  HMACSHA256(base64(header) + "." + base64(payload), secret)
```

---

## Endpoint Specification

### 1. Authentication — `/v1/auth`

---

#### `POST /v1/auth/register`

**Access:** Public

**Description:** Creates a new user account with VIEWER role and returns JWT.

**Request Body:**
```json
{
  "name": "John Doe",               // required, 2-100 chars
  "email": "john@example.com",      // required, valid email format
  "password": "MySecure1"           // required, 8-50 chars, must contain uppercase + lowercase + digit
}
```

**Success Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "role": "VIEWER"
}
```

**Error Responses:**

| Status | Code | Condition |
|---|---|---|
| 400 | `99001` | Validation failed (missing/invalid fields) |
| 400 | `20011` | Password too weak |
| 409 | `20002` | Email already exists |

---

#### `POST /v1/auth/login`

**Access:** Public

**Description:** Authenticates user with email and password, returns JWT.

**Request Body:**
```json
{
  "email": "john@example.com",      // required
  "password": "MySecure1"           // required
}
```

**Success Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "role": "VIEWER"
}
```

**Error Responses:**

| Status | Code | Condition |
|---|---|---|
| 401 | `40002` | Invalid credentials (wrong email or password) |
| 403 | `20004` | Account is inactive |

> **Note:** Login uses the same error message for both "email not found" and "wrong password" to prevent user enumeration.

---

#### `POST /v1/auth/refresh`

**Access:** Authenticated (valid JWT required)

**Description:** Issues a fresh JWT for the currently authenticated user.

**Request:** No body required. JWT passed via `Authorization: Bearer <token>` header.

**Success Response (200 OK):** Same shape as login/register `AuthResponse`.

**Error Responses:**

| Status | Code | Condition |
|---|---|---|
| 401 | `40001` | Missing or invalid JWT |
| 403 | `20004` | Account became inactive since last token |

---

#### `PATCH /v1/auth/change-password`

**Access:** Authenticated (valid JWT required)

**Description:** Changes the authenticated user's password. Requires current password for verification.

**Request Body:**
```json
{
  "currentPassword": "OldPass123",  // required
  "newPassword": "NewPass456"       // required, 8-50 chars, strength rules apply
}
```

**Success Response:** `204 No Content`

**Error Responses:**

| Status | Code | Condition |
|---|---|---|
| 401 | `40002` | Current password incorrect, or Google-only account |
| 400 | `20011` | New password too weak |

---

#### `POST /v1/auth/forgot-password`

**Access:** Public

**Description:** Initiates password reset. Generates a single-use, 15-minute UUID token.

**Request Body:**
```json
{
  "email": "john@example.com"       // required
}
```

**Success Response:** `200 OK` (always — even if email doesn't exist, to prevent enumeration)

> **Local dev:** Token is printed to the server console log.
> **Production:** Token would be emailed to the user.

---

#### `POST /v1/auth/reset-password`

**Access:** Public

**Description:** Completes password reset using the token from forgot-password.

**Request Body:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",   // required, UUID from forgot-password
  "newPassword": "NewSecure1"                          // required, strength rules apply
}
```

**Success Response:** `204 No Content`

**Error Responses:**

| Status | Code | Condition |
|---|---|---|
| 400 | `40014` | Token invalid, expired, or already used |
| 400 | `20011` | New password too weak |

---

### 2. User Management — `/v1/users`

---

#### `GET /v1/users`

**Access:** ADMIN only

**Description:** Returns all users in the system.

**Success Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Admin User",
    "email": "admin@zorvyn.com",
    "role": "ADMIN",
    "status": "ACTIVE",
    "createdAt": "2024-12-01T10:00:00",
    "updatedAt": null
  }
]
```

> **Note:** Password is never included in any user response.

---

#### `GET /v1/users/{id}`

**Access:** ADMIN or the user themselves (SpEL: `#id == authentication.principal.userId`)

**Success Response (200 OK):** Single `UserResponse` object.

**Error Responses:**

| Status | Code | Condition |
|---|---|---|
| 400 | `20003` | Invalid user ID (null or ≤ 0) |
| 403 | `40006` | Not ADMIN and not own profile |
| 404 | `20001` | User not found |

---

#### `POST /v1/users`

**Access:** ADMIN only

**Description:** Creates a user with any role and status. This is the only way to create ANALYST or ADMIN accounts (self-registration always assigns VIEWER).

**Request Body:**
```json
{
  "name": "Jane Analyst",           // required, 2-100 chars
  "email": "jane@example.com",      // required, valid email format, unique
  "password": "Analyst123",          // required, 8-50 chars, strength rules apply
  "role": "ANALYST",                 // required: VIEWER, ANALYST, or ADMIN
  "status": "ACTIVE"                 // required: ACTIVE or INACTIVE
}
```

**Success Response (201 Created):** `UserResponse` object.

---

#### `PATCH /v1/users/{id}`

**Access:** ADMIN or the user themselves

**Description:** Partially updates a user. Only non-null, non-blank fields are applied. Password is NOT updated here — use `PATCH /v1/auth/change-password`.

**Request Body (all fields optional):**
```json
{
  "name": "Updated Name",           // optional
  "email": "newemail@example.com",  // optional, must be unique
  "role": "ANALYST",                 // optional (only ADMIN can change roles)
  "status": "INACTIVE"              // optional (only ADMIN can change status)
}
```

**Success Response (200 OK):** Updated `UserResponse`.

---

#### `PATCH /v1/users/{id}/status`

**Access:** ADMIN only

**Description:** Updates only the user's status.

**Request Body:**
```json
{
  "status": "INACTIVE"              // required: ACTIVE or INACTIVE
}
```

**Success Response (200 OK):** Updated `UserResponse`.

---

#### `DELETE /v1/users/{id}`

**Access:** ADMIN only

**Description:** Hard-deletes a user from the database.

**Success Response:** `204 No Content`

**Error Responses:**

| Status | Code | Condition |
|---|---|---|
| 404 | `20001` | User not found |

---

### 3. Financial Records — `/v1/records`

---

#### `POST /v1/records`

**Access:** ADMIN

**Description:** Creates a new financial record. By default, ownership is set server-side from the JWT of the ADMIN creating the record. An ADMIN may optionally specify `userId` to bind the record to another user (e.g., an ANALYST).

**Request Body:**
```json
{
  "amount": 1500.50,                // required, > 0.00, BigDecimal
  "type": "EXPENSE",                // required: INCOME or EXPENSE
  "category": "Food",               // required, max 100 chars
  "transactionDate": "2024-12-15",  // required, YYYY-MM-DD, must be ≤ today
  "notes": "Dinner with team",      // optional, max 500 chars
  "userId": 2                       // optional, assigns ownership to specific target user
}
```

**Success Response (201 Created):**
```json
{
  "id": 42,
  "amount": 1500.50,
  "type": "EXPENSE",
  "category": "Food",
  "transactionDate": "2024-12-15",
  "notes": "Dinner with team",
  "createdBy": 2,
  "createdAt": "2024-12-15T18:30:00"
}
```

---

#### `GET /v1/records`

**Access:** All authenticated roles

**Description:** Returns a paginated, filtered list of records. ADMIN sees all records; other roles see only their own. Soft-deleted records are excluded.

**Query Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | int | `0` | 0-based page index |
| `size` | int | `20` | Items per page (max 100, enforced server-side) |
| `sortBy` | string | `transactionDate` | Field to sort by |
| `sortDir` | string | `desc` | `asc` or `desc` |
| `category` | string | — | Exact category match |
| `type` | enum | — | `INCOME` or `EXPENSE` |
| `from` | date | — | Start date inclusive (`YYYY-MM-DD`) |
| `to` | date | — | End date inclusive (`YYYY-MM-DD`) |

**Success Response (200 OK):**
```json
{
  "content": [
    {
      "id": 42,
      "amount": 1500.50,
      "type": "EXPENSE",
      "category": "Food",
      "transactionDate": "2024-12-15",
      "notes": "Dinner with team",
      "createdBy": 5,
      "createdAt": "2024-12-15T18:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "direction": "DESC" }
  },
  "totalElements": 47,
  "totalPages": 3,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

**Example requests:**
```bash
# All expenses in Food category, sorted by amount descending
GET /v1/records?type=EXPENSE&category=Food&sortBy=amount&sortDir=desc

# Records from January 2024, page 2 with 10 items
GET /v1/records?from=2024-01-01&to=2024-01-31&page=1&size=10

# All income records, latest first (default sort)
GET /v1/records?type=INCOME
```

---

#### `GET /v1/records/{id}`

**Access:** All authenticated roles (ownership enforced — non-ADMIN sees only own records)

**Success Response (200 OK):** Single `RecordResponse` object.

---

#### `PATCH /v1/records/{id}`

**Access:** ADMIN

**Request Body:** Same shape as `POST /v1/records` (full replacement of all fields).

**Success Response (200 OK):** Updated `RecordResponse`.

---

#### `DELETE /v1/records/{id}`

**Access:** ADMIN

**Description:** Soft-deletes the record (`deleted = true`). The record is never physically removed.

**Success Response:** `204 No Content`

---

### 4. Dashboard Analytics — `/v1/dashboard`

---

#### `GET /v1/dashboard/summary`

**Access:** All authenticated roles

**Description:** Returns a composite dashboard summary. ADMIN sees aggregates over all records; other roles see only their own data.

**Success Response (200 OK):**
```json
{
  "totalIncome": 45000.00,
  "totalExpense": 28500.50,
  "netBalance": 16499.50,
  "categoryTotals": {
    "Salary": 45000.00,
    "Food": 8200.50,
    "Transport": 5800.00,
    "Entertainment": 7500.00,
    "Utilities": 7000.00
  },
  "recentActivity": [
    {
      "id": 100,
      "amount": 450.00,
      "type": "EXPENSE",
      "category": "Food",
      "transactionDate": "2024-12-15",
      "notes": "Groceries",
      "createdBy": 5,
      "createdAt": "2024-12-15T10:00:00"
    }
  ],
  "monthlyTrends": [
    { "year": 2024, "month": 12, "total": 12500.00 },
    { "year": 2024, "month": 11, "total": 9800.50 },
    { "year": 2024, "month": 10, "total": 11200.00 }
  ]
}
```

---

#### `GET /v1/dashboard/category-totals`

**Access:** All authenticated roles

**Success Response (200 OK):**
```json
{
  "Salary": 45000.00,
  "Food": 8200.50,
  "Transport": 5800.00
}
```

---

#### `GET /v1/dashboard/monthly-trends`

**Access:** All authenticated roles

**Description:** Returns monthly totals for the last 12 months, sorted newest first.

**Success Response (200 OK):**
```json
[
  { "year": 2024, "month": 12, "total": 12500.00 },
  { "year": 2024, "month": 11, "total": 9800.50 }
]
```

---

#### `GET /v1/dashboard/recent-activity`

**Access:** All authenticated roles

**Description:** Returns the 5 most recent transactions.

**Success Response (200 OK):** Array of `RecordResponse` objects.

---

## Request / Response Schemas

### Request DTOs

| DTO | Used By | Validation Rules |
|---|---|---|
| `RegisterRequest` | `POST /v1/auth/register` | name: 2-100, email: valid format, password: 8-50 |
| `LoginRequest` | `POST /v1/auth/login` | email: required, password: required |
| `ChangePasswordRequest` | `PATCH /v1/auth/change-password` | currentPassword: required, newPassword: 8-50 |
| `ForgotPasswordRequest` | `POST /v1/auth/forgot-password` | email: required, valid format |
| `ResetPasswordRequest` | `POST /v1/auth/reset-password` | token: required, newPassword: 8-50 |
| `UserRequest` | `POST /v1/users` | all fields required, email unique |
| `UserUpdateRequest` | `PATCH /v1/users/{id}` | all fields optional, only non-null applied |
| `StatusUpdateRequest` | `PATCH /v1/users/{id}/status` | status: ACTIVE or INACTIVE |
| `RecordRequest` | `POST/PATCH /v1/records` | amount > 0, type required, category required, date ≤ today |

### Response DTOs

| DTO | Fields | Notes |
|---|---|---|
| `AuthResponse` | token, tokenType, expiresIn, userId, email, name, role | Returned on register/login/refresh |
| `UserResponse` | id, name, email, role, status, createdAt, updatedAt | Password never included |
| `RecordResponse` | id, amount, type, category, transactionDate, notes, createdBy, createdAt | `createdBy` = userId |
| `DashboardSummaryResponse` | totalIncome, totalExpense, netBalance, categoryTotals, recentActivity, monthlyTrends | Composite |
| `MonthlyTrendResponse` | year, month, total | Used in trends array |
| `ErrorResponse` | timestamp, status, error, message, code | Every error uses this |

---

## Access Control Architecture

### Three-Layer Enforcement

```
┌──────────────────────────────────────────────────────────────────┐
│                    LAYER 1: SECURITY FILTER CHAIN                │
│                                                                  │
│  SecurityConfig.authorizeHttpRequests():                         │
│    /v1/auth/register, /v1/auth/login     → permitAll()          │
│    /v1/auth/forgot-password, /reset      → permitAll()          │
│    /oauth2/**, /login/oauth2/**          → permitAll()          │
│    /swagger-ui/**, /v3/api-docs/**       → permitAll()          │
│    /actuator/health                      → permitAll()          │
│    everything else                       → authenticated()      │
│                                                                  │
│  Result: Unauthenticated requests are blocked before hitting     │
│          any controller (returns 401)                            │
└──────────────────────────────┬───────────────────────────────────┘
                               │ (authenticated requests only)
┌──────────────────────────────▼───────────────────────────────────┐
│                    LAYER 2: @PreAuthorize (SpEL)                  │
│                                                                  │
│  UserController:                                                 │
│    GET /users           → hasRole('ADMIN')                       │
│    GET /users/{id}      → hasRole('ADMIN') or own profile        │
│    POST /users          → hasRole('ADMIN')                       │
│    PATCH /users/{id}    → hasRole('ADMIN') or own profile        │
│    PATCH /users/{id}/status → hasRole('ADMIN')                   │
│    DELETE /users/{id}   → hasRole('ADMIN')                       │
│                                                                  │
│  FinancialRecordController:                                      │
│    POST /records        → hasRole('ADMIN')                       │
│    GET /records         → hasRole('ANALYST') or hasRole('ADMIN') │
│    PATCH /records/{id}  → hasRole('ADMIN')                       │
│    DELETE /records/{id} → hasRole('ADMIN')                       │
│                                                                  │
│  Result: Wrong role → 403 Forbidden (structured ErrorResponse)   │
└──────────────────────────────┬───────────────────────────────────┘
                               │ (correct role)
┌──────────────────────────────▼───────────────────────────────────┐
│                    LAYER 3: SERVICE-LEVEL OWNERSHIP               │
│                                                                  │
│  FinancialRecordServiceImpl.enforceOwnership():                  │
│    if (requester.role != ADMIN && record.createdBy != requester) │
│        → throw 403 "You do not have permission"                  │
│                                                                  │
│  UserResolutionUtil.resolveUserIdFilter():                       │
│    ADMIN  → null  (all records visible in queries)               │
│    Other  → userId (only own records in query WHERE clause)      │
│                                                                  │
│  Result: Even if a user has the right role, they can only act    │
│          on their own data (unless they are ADMIN)               │
└──────────────────────────────────────────────────────────────────┘
```

### Complete Access Control Matrix

| Endpoint | VIEWER | ANALYST | ADMIN |
|---|---|---|---|
| `POST /v1/auth/register` | ✅ Public | ✅ Public | ✅ Public |
| `POST /v1/auth/login` | ✅ Public | ✅ Public | ✅ Public |
| `POST /v1/auth/refresh` | ✅ | ✅ | ✅ |
| `PATCH /v1/auth/change-password` | ✅ | ✅ | ✅ |
| `GET /v1/users` | ❌ 403 | ❌ 403 | ✅ |
| `GET /v1/users/{id}` | ✅ own only | ✅ own only | ✅ any |
| `POST /v1/users` | ❌ 403 | ❌ 403 | ✅ |
| `PATCH /v1/users/{id}` | ✅ own only | ✅ own only | ✅ any |
| `PATCH /v1/users/{id}/status` | ❌ 403 | ❌ 403 | ✅ |
| `DELETE /v1/users/{id}` | ❌ 403 | ❌ 403 | ✅ |
| `POST /v1/records` | ❌ 403 | ❌ 403 | ✅ |
| `GET /v1/records` | ✅ own only | ✅ own only | ✅ all |
| `GET /v1/records/{id}` | ✅ own only | ✅ own only | ✅ any |
| `PATCH /v1/records/{id}` | ❌ 403 | ❌ 403 | ✅ any |
| `DELETE /v1/records/{id}` | ❌ 403 | ❌ 403 | ✅ any |
| `GET /v1/dashboard/*` | ✅ own data | ✅ own data | ✅ all data |

---

## Error Handling Architecture

### Error Response Flow

```
  Controller method throws exception (or service/repo bubbles one up)
                           │
                           ▼
              ┌─── GlobalExceptionHandler ───┐
              │  @RestControllerAdvice       │
              │                              │
              │  Match exception type:       │
              │  ├─ FinancialDashboardEx. ──→│──→ { status, code, message } from exception
              │  ├─ ValidationException  ──→ │──→ 400 + concatenated field errors
              │  ├─ Unreadable JSON     ──→  │──→ 400 + "Malformed or missing request body"
              │  ├─ Type Mismatch       ──→  │──→ 400 + "Invalid value for parameter"
              │  ├─ Missing Param       ──→  │──→ 400 + "Required parameter missing"
              │  ├─ Access Denied       ──→  │──→ 403 + "You do not have permission"
              │  ├─ Method Not Allowed  ──→  │──→ 405 + "HTTP method not supported"
              │  └─ Exception (fallback)──→  │──→ 500 + "An unexpected error occurred"
              │                              │
              └──────────────────────────────┘
                           │
                           ▼
                   ErrorResponse JSON
```

### Error Code Numbering Scheme

```
┌─────────┬──────────────────────┬─────────────────────────────────────┐
│  Range  │  Category            │  Examples                           │
├─────────┼──────────────────────┼─────────────────────────────────────┤
│ 10001-  │ Dashboard errors     │ 10001 Not found                    │
│ 10007   │                      │ 10004 Calculation error            │
│         │                      │ 10007 Invalid date range           │
├─────────┼──────────────────────┼─────────────────────────────────────┤
│ 20001-  │ User management      │ 20001 User not found               │
│ 20012   │                      │ 20002 Email already exists         │
│         │                      │ 20004 User inactive                │
│         │                      │ 20011 Password too weak            │
├─────────┼──────────────────────┼─────────────────────────────────────┤
│ 30001-  │ Financial records    │ 30001 Record not found             │
│ 30014   │                      │ 30007 Creation failed              │
│         │                      │ 30010 Fetch failed                 │
├─────────┼──────────────────────┼─────────────────────────────────────┤
│ 40001-  │ Auth & authorization │ 40001 Authentication required      │
│ 40017   │                      │ 40002 Invalid credentials          │
│         │                      │ 40006 Unauthorized access          │
│         │                      │ 40014 Reset token invalid          │
├─────────┼──────────────────────┼─────────────────────────────────────┤
│ 99001-  │ System / generic     │ 99001 Validation error             │
│ 99999   │                      │ 99003 Database error               │
│         │                      │ 99999 Internal server error        │
└─────────┴──────────────────────┴─────────────────────────────────────┘
```

---

## Database Schema Design

```
┌─────────────────────────────────────────────────────────────────┐
│                        users                                     │
├──────────────────┬──────────────────┬───────────────────────────┤
│ Column           │ Type             │ Constraints               │
├──────────────────┼──────────────────┼───────────────────────────┤
│ id               │ BIGINT           │ PK, AUTO_INCREMENT        │
│ name             │ VARCHAR(100)     │ NOT NULL                  │
│ email            │ VARCHAR(255)     │ NOT NULL, UNIQUE          │
│ password         │ VARCHAR(255)     │ NULLABLE (Google OAuth2)  │
│ role             │ ENUM             │ NOT NULL: VIEWER/ANALYST/ │
│                  │                  │ ADMIN                     │
│ status           │ ENUM             │ NOT NULL: ACTIVE/INACTIVE │
│ created_at       │ DATETIME         │ NOT NULL                  │
│ updated_at       │ DATETIME         │ NULLABLE                  │
└──────────────────┴──────────────────┴───────────────────────────┘
                            │
                            │ 1:N (created_by FK)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    financial_records                              │
├──────────────────┬──────────────────┬───────────────────────────┤
│ Column           │ Type             │ Constraints               │
├──────────────────┼──────────────────┼───────────────────────────┤
│ id               │ BIGINT           │ PK, AUTO_INCREMENT        │
│ amount           │ DECIMAL(19,2)    │ NOT NULL                  │
│ type             │ ENUM             │ NOT NULL: INCOME/EXPENSE  │
│ category         │ VARCHAR(100)     │ NOT NULL                  │
│ transaction_date │ DATE             │ NOT NULL                  │
│ notes            │ VARCHAR(500)     │ NULLABLE                  │
│ created_by       │ BIGINT           │ NOT NULL, FK → users.id   │
│ deleted          │ BOOLEAN          │ DEFAULT false             │
│ created_at       │ DATETIME         │ NOT NULL                  │
│ updated_at       │ DATETIME         │ NULLABLE                  │
└──────────────────┴──────────────────┴───────────────────────────┘

                            │
                            │ 1:N (user_id FK)
┌───────────────────────────┴─────────────────────────────────────┐
│                    password_reset_tokens                          │
├──────────────────┬──────────────────┬───────────────────────────┤
│ Column           │ Type             │ Constraints               │
├──────────────────┼──────────────────┼───────────────────────────┤
│ id               │ BIGINT           │ PK, AUTO_INCREMENT        │
│ token            │ VARCHAR(255)     │ NOT NULL, UNIQUE          │
│ user_id          │ BIGINT           │ NOT NULL, FK → users.id   │
│ expires_at       │ DATETIME         │ NOT NULL                  │
│ used             │ BOOLEAN          │ DEFAULT false             │
│ created_at       │ DATETIME         │ NOT NULL                  │
└──────────────────┴──────────────────┴───────────────────────────┘
```

### Key JPQL Queries

| Query | Purpose | Caller |
|---|---|---|
| `sumIncome(:userId)` | `COALESCE(SUM(amount), 0)` where type=INCOME, deleted=false | Dashboard summary |
| `sumExpense(:userId)` | `COALESCE(SUM(amount), 0)` where type=EXPENSE, deleted=false | Dashboard summary |
| `categoryTotals(:userId)` | `GROUP BY category` with SUM | Category breakdown |
| `monthlyTrends(:start, :end, :userId)` | `GROUP BY YEAR, MONTH` with SUM | Monthly trends |
| `findRecentActivity(:userId, pageable)` | Top 5 by date DESC | Recent activity |
| `findAllByFilters(:userId, :category, :type, :from, :to, pageable)` | Multi-criteria paginated query | Record listing |

> All queries filter by `deleted = false` and apply `(:userId IS NULL OR createdBy.id = :userId)` for role-based row-level security.

---

## Security Architecture

### Security Headers & Configuration

| Setting | Value | Rationale |
|---|---|---|
| CSRF | Disabled | Stateless JWT-based API — no browser cookies/sessions |
| Session policy | `IF_REQUIRED` | API is stateless; sessions only created for OAuth2 redirect state |
| CORS origins | Configurable per profile | `localhost:3000,5173` for local dev |
| CORS methods | `GET, POST, PATCH, DELETE, OPTIONS` | OPTIONS required for preflight |
| BCrypt rounds | Default (10) | Industry standard for password hashing |
| JWT algorithm | HS256 | Symmetric — adequate for single-service deployments |
| JWT expiry | 24 hours | Balanced between security and user convenience |

### Password Policy

| Rule | Enforcement |
|---|---|
| Minimum 8 characters | `@Size(min = 8)` on DTO |
| Maximum 50 characters | `@Size(max = 50)` on DTO |
| At least 1 uppercase letter | Service-layer regex: `(?=.*[A-Z])` |
| At least 1 lowercase letter | Service-layer regex: `(?=.*[a-z])` |
| At least 1 digit | Service-layer regex: `(?=.*\\d)` |

---

## Pagination & Filtering Design

### Pagination Strategy

```
Client request:  GET /v1/records?page=0&size=20&sortBy=transactionDate&sortDir=desc
                                    │       │            │                    │
                                    ▼       ▼            ▼                    ▼
Controller:              PageRequest.of(page, min(size,100), Sort.by(sortBy).desc())
                                    │
                                    ▼
Repository:     @Query with Pageable → SQL LIMIT/OFFSET
                                    │
                                    ▼
Response:       Page<RecordResponse> with metadata:
                  content[]         — the actual records
                  totalElements     — total matching records
                  totalPages        — ceil(totalElements / size)
                  number            — current page index
                  first / last      — boolean convenience flags
                  numberOfElements  — records on this page
```

### Filtering Strategy

All filter parameters are optional and combinable. The JPQL query uses conditional `IS NULL` checks:

```sql
WHERE deleted = false
  AND (:userId IS NULL OR created_by = :userId)        -- role-based scoping
  AND (:category IS NULL OR category = :category)      -- optional category filter
  AND (:type IS NULL OR type = :type)                  -- optional type filter
  AND (:startDate IS NULL OR transaction_date >= :startDate)  -- optional date range
  AND (:endDate IS NULL OR transaction_date <= :endDate)
```

This approach avoids dynamic query building while supporting all filter combinations efficiently.

---

*Document generated for the Zorvyn Finance Dashboard Backend — a backend development assignment submission.*
