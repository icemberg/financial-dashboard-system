# Zorvyn Finance Dashboard — Complete API Design Reference

> **Base URL (local):** `http://localhost:8080`
> **Spring Boot Version:** 3.x / Spring Security 6
> **Database:** MySQL (`finance_dashboard`)
> **Auth Scheme:** JWT Bearer tokens (HS256, 256-bit minimum key)

---

## Table of Contents

1. [System Architecture Overview](#1-system-architecture-overview)
2. [Security Architecture](#2-security-architecture)
3. [Roles & Access Control](#3-roles--access-control)
4. [Database Schema](#4-database-schema)
5. [Error Handling & Error Codes](#5-error-handling--error-codes)
6. [Auth APIs — `/v1/auth`](#6-auth-apis--v1auth)
   - [POST /v1/auth/register](#61-post-v1authregister)
   - [POST /v1/auth/login](#62-post-v1authlogin)
   - [GET /oauth2/authorization/google](#63-get-oauth2authorizationgoogle--google-oauth2-flow)
7. [Financial Record APIs — `/v1/records`](#7-financial-record-apis--v1records)
   - [POST /v1/records](#71-post-v1records)
   - [GET /v1/records](#72-get-v1records)
   - [GET /v1/records/{id}](#73-get-v1recordsid)
   - [PATCH /v1/records/{id}](#74-patch-v1recordsid)
   - [DELETE /v1/records/{id}](#75-delete-v1recordsid)
8. [Dashboard APIs — `/v1/dashboard`](#8-dashboard-apis--v1dashboard)
   - [GET /v1/dashboard/summary](#81-get-v1dashboardsummary)
   - [GET /v1/dashboard/category-totals](#82-get-v1dashboardcategory-totals)
   - [GET /v1/dashboard/monthly-trends](#83-get-v1dashboardmonthly-trends)
   - [GET /v1/dashboard/recent-activity](#84-get-v1dashboardrecent-activity)
9. [User Management APIs — `/v1/users`](#9-user-management-apis--v1users)
   - [GET /v1/users](#91-get-v1users)
   - [GET /v1/users/{id}](#92-get-v1usersid)
   - [POST /v1/users](#93-post-v1users)
   - [PATCH /v1/users/{id}](#94-patch-v1usersid)
   - [PATCH /v1/users/{id}/status](#95-patch-v1usersidstatus)
   - [DELETE /v1/users/{id}](#96-delete-v1usersid)
10. [Enum Reference](#10-enum-reference)
11. [Application Error Code Catalogue](#11-application-error-code-catalogue)
12. [Quick Reference — All Endpoints](#12-quick-reference--all-endpoints)

---

## 1. System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HTTP Clients                                │
│              (Browser / Frontend / Postman / cURL)                  │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                 ┌──────────▼──────────┐
                 │  JwtAuthFilter      │  ← Runs on every request
                 │  (OncePerRequest)   │    Validates Bearer token
                 └──────────┬──────────┘    Sets SecurityContext
                            │
          ┌─────────────────┼───────────────────────┐
          │                 │                       │
   ┌──────▼──────┐  ┌───────▼────────┐  ┌──────────▼──────────┐
   │AuthController│  │FinancialRecord │  │UserController       │
   │/v1/auth      │  │Controller      │  │/v1/users            │
   │              │  │/v1/records     │  │                     │
   └──────┬──────┘  └───────┬────────┘  └──────────┬──────────┘
          │                 │                       │
   ┌──────▼──────┐  ┌───────▼────────┐  ┌──────────▼──────────┐
   │AuthService  │  │FinancialRecord │  │UserService          │
   │             │  │ServiceImpl     │  │                     │
   └──────┬──────┘  └───────┬────────┘  └──────────┬──────────┘
          │                 │                       │
          └─────────────────┼───────────────────────┘
                            │
                 ┌──────────▼──────────┐
                 │FinancialDashboard   │
                 │Controller           │
                 │/v1/dashboard        │
                 └──────────┬──────────┘
                            │
                 ┌──────────▼──────────┐
                 │FinancialDashboard   │
                 │ServiceImpl          │
                 └──────────┬──────────┘
                            │
          ┌─────────────────┼───────────────────────┐
          │                 │                       │
   ┌──────▼──────┐  ┌───────▼────────┐  ┌──────────▼──────────┐
   │UserRepository│ │FinancialRecord │  │UserResolutionUtil   │
   │             │  │Repository      │  │DataMapperUtil       │
   └──────┬──────┘  └───────┬────────┘  └─────────────────────┘
          │                 │
          └────────┬────────┘
                   │
          ┌────────▼────────┐
          │  MySQL Database  │
          │ finance_dashboard│
          └─────────────────┘
```

### Key Design Decisions

| Decision | Choice | Reason |
|---|---|---|
| Auth mechanism | Stateless JWT (HS256) | Scalable, no server-side session state |
| Session policy | `IF_REQUIRED` | Needed only for OAuth2 state parameter during redirect |
| Password storage | BCrypt | Industry-standard adaptive hashing |
| Record deletion | Soft-delete (`deleted = true`) | Preserves audit trail; record never purged |
| User identity | JWT-extracted email | Client can never forge ownership |
| Role resolution | `UserResolutionUtil` | Single component centralises ADMIN vs non-ADMIN filter |
| ADMIN data scope | `userId = null` in JPQL | One query handles both scoped and global access |

---

## 2. Security Architecture

### JWT Auth Filter (`JwtAuthFilter`)

Every HTTP request passes through `JwtAuthFilter` before reaching any controller.

```
Request arrives
      │
      ▼
Authorization header present AND starts with "Bearer "?
      │
  NO  │  YES
      │    │
      │    ├──► Extract JWT, extract email
      │    │
      │    ├──► SecurityContext already set?
      │    │       YES ──► skip (already authenticated)
      │    │       NO  ──► loadUserByUsername(email)
      │    │
      │    ├──► isTokenValid(jwt, userDetails)?
      │    │       YES ──► Set UsernamePasswordAuthenticationToken in SecurityContext
      │    │       NO  ──► Log warning, continue chain (Spring returns 401 for protected routes)
      │    │
      ▼    ▼
    Continue filter chain
```

### Public (No JWT Required) Endpoints

Configured in `SecurityConfig`:

| Pattern | Reason |
|---|---|
| `/v1/auth/**` | Login and register endpoints |
| `/oauth2/**` | Google OAuth2 initiation |
| `/login/oauth2/**` | Google OAuth2 callback |
| `/v3/api-docs/**` | Swagger/OpenAPI docs |
| `/swagger-ui/**` | Swagger UI |
| `/swagger-ui.html` | Swagger UI HTML |

All other endpoints require a valid JWT.

### Password Rules (Service-Layer Validation)

Beyond `@Size(min=8, max=50)` in the DTO, `AuthServiceImpl` enforces:

```
Password regex: ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$
```

Meaning: at least one **lowercase**, one **uppercase**, one **digit**.  
Violation → HTTP 400 with error code `20011` (`PASSWORD_TOO_WEAK`).

### Google OAuth2 Users

- Google users have `password = null` in the database.
- Attempting `POST /v1/auth/login` with a Google-only account returns `401 INVALID_CREDENTIALS`.
- Google users are auto-provisioned on first login with role `VIEWER` and status `ACTIVE`.
- If name changes on Google's side, it is synced on every subsequent login.

---

## 3. Roles & Access Control

Roles are stored as strings in the `users.role` column and embedded as a `role` claim inside every JWT.

| Role | Description | Privileges |
|---|---|---|
| `VIEWER` | Read-only observer | `GET` on their own records and dashboard data |
| `ANALYST` | Power user | All of VIEWER's access + `POST`, `PATCH`, `DELETE` on **own** records |
| `ADMIN` | System administrator | Full CRUD on **all** records and **all** users |

### How Ownership Is Enforced

Ownership enforcement happens at **two layers** for defence in depth:

1. **Controller layer** — `@PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")` blocks `VIEWER` from write endpoints before any business logic executes.
2. **Service layer** — `FinancialRecordServiceImpl.enforceOwnership()` checks that `record.createdBy.id == requester.id`. A non-ADMIN accessing someone else's record gets `403 UNAUTHORIZED_ACCESS`.

The ADMIN bypass in `enforceOwnership`:

```java
if (requester.getRole() == RolesEnum.ADMIN) {
    return; // ADMIN has full access to all records
}
```

---

## 4. Database Schema

### Table: `users`

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `id` | BIGINT (PK, AUTO) | No | Primary key |
| `name` | VARCHAR | No | Display name |
| `email` | VARCHAR (UNIQUE) | No | Login identifier, also stored in JWT subject |
| `password` | VARCHAR | Yes | BCrypt hash. `null` for Google OAuth2 users |
| `role` | ENUM STRING | No | `VIEWER`, `ANALYST`, `ADMIN` |
| `status` | ENUM STRING | No | `ACTIVE`, `INACTIVE` |
| `created_at` | DATETIME | No | Set at registration / provisioning |
| `updated_at` | DATETIME | Yes | `null` if never updated |

### Table: `financial_records`

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `id` | BIGINT (PK, AUTO) | No | Primary key |
| `amount` | DECIMAL(19,2) | No | Monetary value, `> 0.01` |
| `type` | ENUM STRING | No | `INCOME` or `EXPENSE` |
| `category` | VARCHAR | No | Free-text label, max 100 chars |
| `transaction_date` | DATE | No | Must not be in the future |
| `notes` | VARCHAR | Yes | Optional memo, max 500 chars |
| `created_at` | DATETIME | No | Server-set timestamp |
| `updated_at` | DATETIME | Yes | Set on PATCH; `null` if never updated |
| `created_by` | BIGINT (FK → users.id) | No | Owner's user ID |
| `deleted` | BOOLEAN | No | Soft-delete flag; `false` by default |

> All queries filter `WHERE deleted = false` so soft-deleted records are invisible to all API responses.

---

## 5. Error Handling & Error Codes

### Standard Error Response Body

Every error from any endpoint returns this structure (handled by `GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-04-02T17:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "The requested user does not exist. Please verify the user ID and try again.: 99",
  "code": "20001"
}
```

| Field | Type | Description |
|---|---|---|
| `timestamp` | datetime | ISO-8601 datetime when the error occurred |
| `status` | integer | HTTP status code mirroring the response code |
| `error` | string | HTTP reason phrase (e.g., `"Not Found"`, `"Bad Request"`) |
| `message` | string | Human-readable description including suggested action |
| `code` | string | Application error code (see catalogue in §11) |

### Validation Errors (`@Valid` failures)

When request body validation fails, the error message concatenates all failing fields:

```json
{
  "timestamp": "2026-04-02T17:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "email: Email must be a valid email address, password: Password must be between 8 and 50 characters",
  "code": "99001"
}
```

### Exception Hierarchy

```
Exception
└── FinancialDashboardException      (domain base, carries errorCode + HttpStatus)
    └── UserException                (user management errors)
        └── UserNotFoundException    (legacy; resolved to USER_NOT_FOUND)
```

---

## 6. Auth APIs — `/v1/auth`

All endpoints under `/v1/auth` are **public** — no JWT is required.

---

### 6.1 `POST /v1/auth/register`

Registers a new user via email and password. All self-registered users get role `VIEWER`. To create users with `ANALYST` or `ADMIN` roles, use [`POST /v1/users`](#93-post-v1users) (ADMIN only).

#### Service-Layer Logic

1. Validate password strength: must contain uppercase, lowercase, and digit.
2. Check for duplicate email → `409 CONFLICT` if found.
3. BCrypt-encode the password.
4. Save user with `role = VIEWER`, `status = ACTIVE`, `createdAt = now()`.
5. Generate and return a JWT.

#### Request

```
POST http://localhost:8080/v1/auth/register
Content-Type: application/json
```

```json
{
  "name": "Abhiram Reddy",
  "email": "abhiram@example.com",
  "password": "SecurePass1"
}
```

**Request Fields — `RegisterRequest`:**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | string | ✅ | 2–100 characters |
| `email` | string | ✅ | Valid email format (`@Email`) |
| `password` | string | ✅ | 8–50 chars + uppercase + lowercase + digit |

#### Response — `201 Created`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "email": "abhiram@example.com",
  "name": "Abhiram Reddy",
  "role": "VIEWER"
}
```

**Response Fields — `AuthResponse`:**

| Field | Type | Description |
|---|---|---|
| `token` | string | JWT for subsequent authenticated requests |
| `tokenType` | string | Always `"Bearer"` |
| `expiresIn` | long | Token validity in **seconds** (e.g., `86400` = 24 h). Derived from `app.jwt.expiration-ms / 1000` |
| `userId` | long | Newly assigned database ID |
| `email` | string | Registered email |
| `name` | string | Registered display name |
| `role` | string | Always `"VIEWER"` for self-registration |

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `400` | `20011` | Password too weak (missing uppercase/lowercase/digit) |
| `400` | `99001` | DTO validation failed (blank name, invalid email format, etc.) |
| `409` | `20002` | Email already registered |

---

### 6.2 `POST /v1/auth/login`

Authenticates an existing email-and-password user. Returns a fresh JWT.

#### Service-Layer Logic

1. Find user by email → generic `401 INVALID_CREDENTIALS` if not found (prevents user enumeration).
2. Reject if account `status != ACTIVE` → `403 USER_INACTIVE`.
3. If user has `password = null` (Google-only) → `401 INVALID_CREDENTIALS`.
4. Verify BCrypt match → `401 INVALID_CREDENTIALS` on mismatch.
5. Generate and return JWT.

#### Request

```
POST http://localhost:8080/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "abhiram@example.com",
  "password": "SecurePass1"
}
```

**Request Fields — `LoginRequest`:**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `email` | string | ✅ | Valid email format |
| `password` | string | ✅ | Not blank |

#### Response — `200 OK`

Same shape as the register response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "email": "abhiram@example.com",
  "name": "Abhiram Reddy",
  "role": "VIEWER"
}
```

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `400` | `99001` | Blank email or blank password |
| `401` | `40002` | Email not found or wrong password or Google-only account |
| `403` | `20004` | Account `status = INACTIVE` |

---

### 6.3 `GET /oauth2/authorization/google` — Google OAuth2 Flow

This is a **browser-redirect flow**, not a JSON endpoint. Navigate the browser to this URL to begin Google login.

```
GET http://localhost:8080/oauth2/authorization/google
```

#### Full Flow Diagram

```
Browser                    Zorvyn Backend                  Google
  │                              │                             │
  ├──GET /oauth2/authorization/google──►│                      │
  │                              ├──302 Redirect to Google────►│
  │                              │                             │
  │◄─────────────────── Google consent page ─────────────────►│
  │ (user clicks "Allow")        │                             │
  │                              │◄────── Auth code ───────────│
  │                              │  GET /login/oauth2/code/google
  │                              │                             │
  │                              │ 1. Exchange code for token  │
  │                              │ 2. Fetch email + name       │
  │                              │ 3. Verify email_verified=true
  │                              │ 4. Provision or load User   │
  │                              │ 5. Sync name if changed     │
  │                              │ 6. Block if INACTIVE        │
  │                              │ 7. Generate Zorvyn JWT      │
  │◄──── 302 Redirect to frontend?token=<JWT> ───────────────│
  │  http://localhost:3000/oauth2/callback?token=eyJ...        │
```

#### Backend Logic (OAuth2LoginSuccessHandler)

| Step | Action |
|---|---|
| Extract attributes | Reads `email` and `name` from Google's `OAuth2User` |
| Verify email | Checks `email_verified = true`; rejects unverified with `401` |
| Provision or load | If first login → creates user with `role=VIEWER`, `status=ACTIVE`, `password=null` |
| Sync name | If Google name has changed → updates `name` and `updatedAt` in DB |
| Block inactive | If existing user has `status=INACTIVE` → returns `403` |
| Issue JWT | Generates a Zorvyn JWT via `JwtService.generateToken()` |
| Redirect | Redirects browser to `${app.oauth2.redirect-uri}?token=<JWT>` |

The configured redirect URI (from `application-local.properties`):
```
app.oauth2.redirect-uri=http://localhost:3000/oauth2/callback
```

The frontend reads `?token=<JWT>` from the URL and stores it for API calls.

> **Google-only users** have `password = null`. They cannot use `POST /v1/auth/login` — doing so returns `401 INVALID_CREDENTIALS`.

---

## 7. Financial Record APIs — `/v1/records`

All endpoints require a valid JWT.

```
Authorization: Bearer <your-jwt-token>
```

> **Key rule:** The client **never** supplies `userId`. Ownership is always resolved server-side from the JWT-extracted email via `UserResolutionUtil`.

---

### 7.1 `POST /v1/records`

Creates a new financial record. Ownership is set server-side from the authenticated user's JWT.

**Required roles:** `ANALYST`, `ADMIN`

#### Service-Layer Logic

1. Extract user email from JWT, load `User` entity.
2. Build `FinancialRecord` with `createdBy = user`, `createdAt = now()`, `deleted = false`.
3. Persist and return `RecordResponse`.

#### Request

```
POST http://localhost:8080/v1/records
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "amount": 4500.00,
  "type": "INCOME",
  "category": "Freelance",
  "transactionDate": "2026-04-01",
  "notes": "Payment received for design project"
}
```

**Request Fields — `RecordRequest`:**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `amount` | BigDecimal | ✅ | `> 0.01`. Stored as `DECIMAL(19,2)` for precision |
| `type` | string | ✅ | `"INCOME"` or `"EXPENSE"` (maps to `RecordTypeEnum`) |
| `category` | string | ✅ | Not blank, max 100 characters |
| `transactionDate` | date | ✅ | ISO date `YYYY-MM-DD`, must be past or present (`@PastOrPresent`) |
| `notes` | string | ❌ | Optional memo, max 500 characters |

#### Response — `201 Created`

```json
{
  "id": 12,
  "amount": 4500.00,
  "type": "INCOME",
  "category": "Freelance",
  "transactionDate": "2026-04-01",
  "notes": "Payment received for design project",
  "createdBy": 1,
  "createdAt": "2026-04-02T15:30:00"
}
```

**Response Fields — `RecordResponse`:**

| Field | Type | Description |
|---|---|---|
| `id` | long | Database primary key of the record |
| `amount` | decimal | Transaction amount, 2 decimal places |
| `type` | string | `"INCOME"` or `"EXPENSE"` |
| `category` | string | Category label |
| `transactionDate` | date | `YYYY-MM-DD` |
| `notes` | string | Optional memo (may be `null`) |
| `createdBy` | long | `userId` of the record owner |
| `createdAt` | datetime | ISO-8601 server timestamp |

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `400` | `99001` | Validation failed (negative amount, future date, blank category, etc.) |
| `401` | `40001` | No JWT or token expired |
| `403` | `40006` | Authenticated as `VIEWER` |

---

### 7.2 `GET /v1/records`

Returns all non-deleted records visible to the authenticated user with optional filters.

**Required roles:** `VIEWER`, `ANALYST`, `ADMIN`

- **VIEWER / ANALYST** → own records only (JPQL filter: `createdBy.id = userId`)
- **ADMIN** → all records (JPQL filter: `userId = null`, bypassing the filter clause)

#### Service-Layer Logic

1. Resolve `User` from email and determine `userIdFilter` (`null` if ADMIN, own ID otherwise).
2. Call `findAllByFilters(userIdFilter, category, type, from, to)`.
3. All soft-deleted records excluded (`WHERE deleted = false`).
4. Results ordered: most recent `transactionDate` and `createdAt` first.

#### Request

```
GET http://localhost:8080/v1/records
Authorization: Bearer <token>
```

**Optional Query Parameters:**

| Parameter | Type | Description | Example |
|---|---|---|---|
| `category` | string | Exact category match (case-sensitive) | `?category=Food` |
| `type` | string | `INCOME` or `EXPENSE` | `?type=EXPENSE` |
| `from` | date | Start date inclusive (`YYYY-MM-DD`) | `?from=2026-01-01` |
| `to` | date | End date inclusive (`YYYY-MM-DD`) | `?to=2026-03-31` |

All are optional and fully combinable:

```
GET http://localhost:8080/v1/records?type=EXPENSE&category=Food&from=2026-01-01&to=2026-03-31
Authorization: Bearer <token>
```

#### Response — `200 OK`

Array of `RecordResponse`. Empty array `[]` if no records match.

```json
[
  {
    "id": 7,
    "amount": 320.50,
    "type": "EXPENSE",
    "category": "Food",
    "transactionDate": "2026-02-14",
    "notes": "Valentine's dinner",
    "createdBy": 1,
    "createdAt": "2026-02-14T20:00:00"
  },
  {
    "id": 11,
    "amount": 85.00,
    "type": "EXPENSE",
    "category": "Food",
    "transactionDate": "2026-03-20",
    "notes": null,
    "createdBy": 1,
    "createdAt": "2026-03-20T10:15:00"
  }
]
```

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |

---

### 7.3 `GET /v1/records/{id}`

Returns a single non-deleted record by its ID. Ownership enforced at the service layer.

**Required roles:** `VIEWER`, `ANALYST`, `ADMIN`

#### Service-Layer Logic

1. `findByIdAndDeletedFalse(id)` → `404 FINANCIAL_RECORD_NOT_FOUND` if not found or soft-deleted.
2. `enforceOwnership(record, requester)` → ADMIN bypasses; non-ADMIN checks `record.createdBy.id == requester.id`.

#### Request

```
GET http://localhost:8080/v1/records/12
Authorization: Bearer <token>
```

| Path Variable | Type | Description |
|---|---|---|
| `id` | long | Database ID of the record |

#### Response — `200 OK`

```json
{
  "id": 12,
  "amount": 4500.00,
  "type": "INCOME",
  "category": "Freelance",
  "transactionDate": "2026-04-01",
  "notes": "Payment received for design project",
  "createdBy": 1,
  "createdAt": "2026-04-02T15:30:00"
}
```

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |
| `403` | `40006` | Record belongs to another user and requester is not ADMIN |
| `404` | `30001` | Record not found or soft-deleted |

---

### 7.4 `PATCH /v1/records/{id}`

Fully replaces all fields of an existing record (all `RecordRequest` fields required). Updates `updatedAt = now()`.

**Required roles:** `ANALYST` (own records), `ADMIN` (any record)

#### Service-Layer Logic

1. `findActiveRecordOrThrow(id)` → 404 if not found or deleted.
2. `enforceOwnership()` → 403 if ANALYST tries to update someone else's record.
3. Update all 5 fields from request body + set `updatedAt = now()`.
4. Save and return updated `RecordResponse`.

#### Request

```
PATCH http://localhost:8080/v1/records/12
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Freelance",
  "transactionDate": "2026-04-01",
  "notes": "Final payment — milestone 2 complete"
}
```

Same field rules as [POST /v1/records](#71-post-v1records).

#### Response — `200 OK`

Updated `RecordResponse`:

```json
{
  "id": 12,
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Freelance",
  "transactionDate": "2026-04-01",
  "notes": "Final payment — milestone 2 complete",
  "createdBy": 1,
  "createdAt": "2026-04-02T15:30:00"
}
```

> Note: `createdAt` never changes on update. `updatedAt` is stored in the database but not currently returned in `RecordResponse`.

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `400` | `99001` | Validation failed |
| `401` | `40001` | No JWT or expired |
| `403` | `40007` | `VIEWER` role, or `ANALYST` editing another user's record |
| `404` | `30001` | Record not found or already soft-deleted |

---

### 7.5 `DELETE /v1/records/{id}`

**Soft-deletes** a record — sets `deleted = true` in the database. The record is never physically removed, preserving the audit trail. All subsequent queries filter `WHERE deleted = false`.

**Required roles:** `ANALYST` (own records), `ADMIN` (any record)

#### Service-Layer Logic

1. `findActiveRecordOrThrow(id)` → 404 if already deleted or not found.
2. `enforceOwnership()` → 403 if non-ADMIN tries to delete someone else's record.
3. Set `record.deleted = true`, `updatedAt = now()`, save.

#### Request

```
DELETE http://localhost:8080/v1/records/12
Authorization: Bearer <token>
```

| Path Variable | Type | Description |
|---|---|---|
| `id` | long | Database ID of the record to soft-delete |

No request body.

#### Response — `204 No Content`

Empty body. Confirms soft-deletion succeeded.

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |
| `403` | `40007` | `VIEWER` role, or `ANALYST` deleting someone else's record |
| `404` | `30001` | Record not found or already soft-deleted |

---

## 8. Dashboard APIs — `/v1/dashboard`

All endpoints require a valid JWT. Dashboard data is always scoped to the authenticated user:
- **Non-ADMIN** users see their own data.
- **ADMIN** sees aggregated data for all users.

This scoping is handled by `UserResolutionUtil.resolveUserIdFilter()`:  
→ returns `null` for ADMIN (JPQL treats `null` as "no filter"), returns the user's own ID for everyone else.

---

### 8.1 `GET /v1/dashboard/summary`

Returns a complete one-shot dashboard payload: income total, expense total, net balance, category breakdown, monthly trends, and latest 5 transactions. Use this endpoint to populate the entire dashboard view in a single request.

**Required roles:** All authenticated roles

#### Service-Layer Logic

1. Resolve `User` from email; compute `userIdFilter`.
2. `sumIncome(userIdFilter)` — JPQL aggregation on non-deleted `INCOME` records.
3. `sumExpense(userIdFilter)` — JPQL aggregation on non-deleted `EXPENSE` records.
4. Validate: throws `DASHBOARD_CALCULATION_ERROR` if either sum is `null`.
5. `netBalance = totalIncome - totalExpense`.
6. `calculateCategoryTotals(userIdFilter)` — group-by category.
7. `calculateMonthlyTrends(userIdFilter)` — last 12 calendar months.
8. `calculateRecentActivity(userIdFilter)` — top 5 most recent records.
9. Build and return `DashboardSummaryResponse`.

#### Request

```
GET http://localhost:8080/v1/dashboard/summary
Authorization: Bearer <token>
```

No query parameters. No request body.

#### Response — `200 OK`

```json
{
  "totalIncome": 25000.00,
  "totalExpense": 12500.00,
  "netBalance": 12500.00,
  "categoryTotals": {
    "Freelance": 20000.00,
    "Salary": 5000.00,
    "Food": 3000.00,
    "Transport": 2500.00,
    "Utilities": 7000.00
  },
  "recentActivity": [
    {
      "id": 42,
      "amount": 5000.00,
      "type": "INCOME",
      "category": "Salary",
      "transactionDate": "2026-04-01",
      "notes": "April salary",
      "createdBy": 1,
      "createdAt": "2026-04-01T09:00:00"
    }
  ],
  "monthlyTrends": [
    { "year": 2026, "month": 4, "total": 5000.00 },
    { "year": 2026, "month": 3, "total": 7200.00 },
    { "year": 2026, "month": 2, "total": 4800.00 }
  ]
}
```

**Response Fields — `DashboardSummaryResponse`:**

| Field | Type | Description |
|---|---|---|
| `totalIncome` | BigDecimal | Sum of all non-deleted `INCOME` records in scope |
| `totalExpense` | BigDecimal | Sum of all non-deleted `EXPENSE` records in scope |
| `netBalance` | BigDecimal | `totalIncome - totalExpense` |
| `categoryTotals` | `Map<String, BigDecimal>` | Map of category label → total amount. See §8.2 |
| `recentActivity` | `List<RecordResponse>` | Latest 5 transactions. See §8.4 |
| `monthlyTrends` | `List<MonthlyTrendResponse>` | Last 12 months of data. See §8.3 |

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |
| `500` | `10004` | Income or expense sum resolved to `null` (indicates data integrity issue) |
| `500` | `10005` | Unexpected error during summary construction |

---

### 8.2 `GET /v1/dashboard/category-totals`

Returns a flat map of category → total amount across all record types (both `INCOME` and `EXPENSE`). Useful for rendering pie/bar charts.

**Required roles:** All authenticated roles

#### Service-Layer Logic

1. Resolve `userIdFilter` from JWT email.
2. Call `financialRecordRepository.categoryTotals(userId)` — JPQL `GROUP BY fr.category`.
3. `DataMapperUtil.mapCategoryTotals()` converts `Object[]` rows to `Map<String, BigDecimal>`.
4. Returns empty map `{}` if no records exist.

#### Request

```
GET http://localhost:8080/v1/dashboard/category-totals
Authorization: Bearer <token>
```

No query parameters. No request body.

#### Response — `200 OK`

```json
{
  "Freelance": 20000.00,
  "Salary": 5000.00,
  "Food": 3000.00,
  "Transport": 2500.00,
  "Utilities": 7000.00
}
```

A `Map<String, BigDecimal>` where:
- **key** = category string (as stored in `financial_records.category`)
- **value** = total `COALESCE(SUM(amount), 0)` for that category

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |
| `500` | `10004` | Calculation error |

---

### 8.3 `GET /v1/dashboard/monthly-trends`

Returns monthly aggregated totals for the **last 12 calendar months** (from the 1st of the month 11 months ago up to today).

**Required roles:** All authenticated roles

#### Service-Layer Logic

1. Resolve `userIdFilter` from JWT email.
2. Compute: `startDate = today.minusMonths(11).withDayOfMonth(1)`, `endDate = today`.
3. Call `monthlyTrends(startDate, endDate, userId)` — JPQL `GROUP BY YEAR, MONTH ORDER BY DESC`.
4. `DataMapperUtil.mapMonthlyTrends()` converts `Object[]` rows to `List<MonthlyTrendResponse>`.

#### Request

```
GET http://localhost:8080/v1/dashboard/monthly-trends
Authorization: Bearer <token>
```

No query parameters. No request body.

#### Response — `200 OK`

```json
[
  { "year": 2026, "month": 4, "total": 5000.00 },
  { "year": 2026, "month": 3, "total": 7200.00 },
  { "year": 2026, "month": 2, "total": 4800.00 },
  { "year": 2026, "month": 1, "total": 3100.00 },
  { "year": 2025, "month": 12, "total": 6400.00 }
]
```

**Response Fields — `MonthlyTrendResponse`:**

| Field | Type | Description |
|---|---|---|
| `year` | int | Calendar year (e.g., `2026`) |
| `month` | int | Calendar month, 1–12 (e.g., `4` = April) |
| `total` | BigDecimal | Sum of all record amounts for that month |

Ordered from most recent month to oldest. Months with no transactions are **omitted** (not zero-filled).

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |
| `400` | `10007` | Date range invalid (should not occur in normal operation) |
| `500` | `10004` | Calculation error |

---

### 8.4 `GET /v1/dashboard/recent-activity`

Returns the **5 most recent** non-deleted transactions for the authenticated user, ordered by `transactionDate DESC, createdAt DESC`.

**Required roles:** All authenticated roles

#### Service-Layer Logic

1. Resolve `userIdFilter` from JWT email.
2. Call `findRecentActivity(userId, PageRequest.of(0, 5))` — hard-coded page size of 5.
3. Map `FinancialRecord` → `RecordResponse` via `RecordResponse.fromEntity()`.

#### Request

```
GET http://localhost:8080/v1/dashboard/recent-activity
Authorization: Bearer <token>
```

No query parameters. No request body.

#### Response — `200 OK`

Array of up to 5 `RecordResponse` objects, newest first:

```json
[
  {
    "id": 42,
    "amount": 5000.00,
    "type": "INCOME",
    "category": "Salary",
    "transactionDate": "2026-04-01",
    "notes": "April salary",
    "createdBy": 1,
    "createdAt": "2026-04-01T09:00:00"
  },
  {
    "id": 41,
    "amount": 150.00,
    "type": "EXPENSE",
    "category": "Transport",
    "transactionDate": "2026-03-29",
    "notes": null,
    "createdBy": 1,
    "createdAt": "2026-03-29T18:30:00"
  }
]
```

Returns an empty array `[]` if no records exist. Always at most 5 items.

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |
| `500` | `10002` | Unexpected fetch failure |

---

## 9. User Management APIs — `/v1/users`

All endpoints require a valid JWT.  All endpoints currently require `ADMIN` role.

> **Important:** The `updateUser` and `getUserById` endpoints have Javadoc indicating  
> *"ADMIN or own user"* access, but in the current implementation, both `@PreAuthorize` annotations  
> use `hasRole('ADMIN')` only. Non-ADMIN users cannot call these endpoints regardless of whether  
> the ID is their own.

---

### 9.1 `GET /v1/users`

Retrieves all users in the system. Passwords are never included in any response.

**Required roles:** `ADMIN`

#### Request

```
GET http://localhost:8080/v1/users
Authorization: Bearer <ADMIN-token>
```

No parameters. No request body.

#### Response — `200 OK`

```json
[
  {
    "id": 1,
    "name": "Abhiram Reddy",
    "email": "abhiram@example.com",
    "role": "ADMIN",
    "status": "ACTIVE",
    "createdAt": "2026-01-15T10:00:00",
    "updatedAt": null
  },
  {
    "id": 2,
    "name": "Jane Doe",
    "email": "jane@example.com",
    "role": "VIEWER",
    "status": "ACTIVE",
    "createdAt": "2026-02-20T14:30:00",
    "updatedAt": "2026-03-10T09:00:00"
  }
]
```

**Response Fields — `UserResponse`:**

| Field | Type | Description |
|---|---|---|
| `id` | long | Database primary key |
| `name` | string | Display name |
| `email` | string | Login email. `password` is **never** returned |
| `role` | string | `VIEWER`, `ANALYST`, or `ADMIN` |
| `status` | string | `ACTIVE` or `INACTIVE` |
| `createdAt` | datetime | ISO-8601 formatted as `yyyy-MM-dd'T'HH:mm:ss` |
| `updatedAt` | datetime | `null` if the user has never been updated after creation |

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |
| `403` | `40006` | Not ADMIN |

---

### 9.2 `GET /v1/users/{id}`

Retrieves a single user by their database ID.

**Required roles:** `ADMIN`

#### Request

```
GET http://localhost:8080/v1/users/2
Authorization: Bearer <ADMIN-token>
```

| Path Variable | Type | Description |
|---|---|---|
| `id` | long | Database ID of the user |

No request body.

#### Response — `200 OK`

```json
{
  "id": 2,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "VIEWER",
  "status": "ACTIVE",
  "createdAt": "2026-02-20T14:30:00",
  "updatedAt": null
}
```

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `400` | `20003` | `id` is null or `<= 0` (validated in service) |
| `401` | `40001` | No JWT or expired |
| `403` | `40006` | Not ADMIN |
| `404` | `20001` | No user found for the given ID |

---

### 9.3 `POST /v1/users`

Creates a new user with a specified role and status. This is the only way to create `ANALYST` or `ADMIN` accounts. Unlike `/v1/auth/register`, this endpoint allows setting any `role` and `status`.

**Required roles:** `ADMIN`

#### Service-Layer Logic

1. Check for duplicate email → `409 USER_ALREADY_EXISTS`.
2. BCrypt-encode the password.
3. Save user with `createdAt = now()`.
4. Return `UserResponse` (no password).

#### Request

```
POST http://localhost:8080/v1/users
Content-Type: application/json
Authorization: Bearer <ADMIN-token>
```

```json
{
  "name": "John Smith",
  "email": "john.smith@example.com",
  "password": "SecurePass1",
  "role": "ANALYST",
  "status": "ACTIVE"
}
```

**Request Fields — `UserRequest`:**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | string | ✅ | 2–100 characters |
| `email` | string | ✅ | Valid email format, must be unique |
| `password` | string | ✅ | 8–50 characters |
| `role` | string | ✅ | `VIEWER`, `ANALYST`, or `ADMIN` |
| `status` | string | ✅ | `ACTIVE` or `INACTIVE` |

#### Response — `201 Created`

```json
{
  "id": 5,
  "name": "John Smith",
  "email": "john.smith@example.com",
  "role": "ANALYST",
  "status": "ACTIVE",
  "createdAt": "2026-04-02T17:00:00",
  "updatedAt": null
}
```

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `400` | `99001` | Validation failed (blank name, invalid email, short password, null role, etc.) |
| `401` | `40001` | No JWT or expired |
| `403` | `40006` | Not ADMIN |
| `409` | `20002` | Email already registered |

---

### 9.4 `PATCH /v1/users/{id}`

Updates one or more fields of an existing user. This is a **partial update** — only non-null/non-blank fields in the request body are applied.

**Required roles:** `ADMIN`

> **Note:** Password is **not** updated through this endpoint. A dedicated password-change endpoint would be needed for that (not yet implemented).

#### Service-Layer Logic

1. Fetch user by ID → `404` if not found.
2. Apply provided fields only:
   - `name` → if not blank
   - `email` → if not blank and different from current; check for duplicate
   - `role` → if not null
   - `status` → if not null
3. Set `updatedAt = now()`. Save. Return `UserResponse`.

#### Request

```
PATCH http://localhost:8080/v1/users/5
Content-Type: application/json
Authorization: Bearer <ADMIN-token>
```

All fields are technically sent as `UserRequest`, but you only need to include what you want to change. **However**, because `@Valid` is applied, you must still satisfy all `@NotBlank` / `@NotNull` constraints for the fields you include. To change only `name`, for example, you would still need to include all required fields:

```json
{
  "name": "John A. Smith",
  "email": "john.smith@example.com",
  "password": "SecurePass1",
  "role": "ANALYST",
  "status": "ACTIVE"
}
```

#### Response — `200 OK`

```json
{
  "id": 5,
  "name": "John A. Smith",
  "email": "john.smith@example.com",
  "role": "ANALYST",
  "status": "ACTIVE",
  "createdAt": "2026-04-02T17:00:00",
  "updatedAt": "2026-04-02T18:00:00"
}
```

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `400` | `99001` | Validation failed |
| `401` | `40001` | No JWT or expired |
| `403` | `40006` | Not ADMIN |
| `404` | `20001` | User not found |
| `409` | `20002` | New email is already in use by another user |

---

### 9.5 `PATCH /v1/users/{id}/status`

Updates **only the status** field of a user. Used to activate or deactivate accounts without touching any other fields.

**Required roles:** `ADMIN`

#### Service-Layer Logic

1. Validate `status` field is not null (explicit null check in controller + service).
2. Fetch user by ID → `404` if not found.
3. Set `status`, `updatedAt = now()`. Save. Return `UserResponse`.

#### Request

```
PATCH http://localhost:8080/v1/users/5/status
Content-Type: application/json
Authorization: Bearer <ADMIN-token>
```

```json
{
  "name": "John A. Smith",
  "email": "john.smith@example.com",
  "password": "SecurePass1",
  "role": "ANALYST",
  "status": "INACTIVE"
}
```

> The endpoint only uses `status` from the request body. All other fields are ignored in the service's `updateUserStatus()`. However, because `UserRequest` is used with `@Valid`, all required fields must still be present.

#### Response — `200 OK`

```json
{
  "id": 5,
  "name": "John A. Smith",
  "email": "john.smith@example.com",
  "role": "ANALYST",
  "status": "INACTIVE",
  "createdAt": "2026-04-02T17:00:00",
  "updatedAt": "2026-04-02T19:00:00"
}
```

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `400` | `20003` | `status` field is null in the request body |
| `401` | `40001` | No JWT or expired |
| `403` | `40006` | Not ADMIN |
| `404` | `20001` | User not found |

---

### 9.6 `DELETE /v1/users/{id}`

Hard-deletes a user from the database.

> **Production note:** The service Javadoc explicitly recommends converting this to a soft-delete (adding an `isDeleted` flag) in production to prevent referential integrity issues with existing `financial_records`.

**Required roles:** `ADMIN`

#### Service-Layer Logic

1. Check existence via `existsById(id)` → throw `404 USER_NOT_FOUND` if not found.
2. Call `deleteById(id)` — permanent deletion.

#### Request

```
DELETE http://localhost:8080/v1/users/5
Authorization: Bearer <ADMIN-token>
```

| Path Variable | Type | Description |
|---|---|---|
| `id` | long | Database ID of the user to delete |

No request body.

#### Response — `204 No Content`

Empty body. Confirms deletion.

#### Error Cases

| HTTP | Code | Scenario |
|---|---|---|
| `401` | `40001` | No JWT or expired |
| `403` | `40006` | Not ADMIN |
| `404` | `20001` | User not found |

---

## 10. Enum Reference

### `RecordTypeEnum`

Used in `RecordRequest.type` (input) and `RecordResponse.type` (output).

| Value | Meaning |
|---|---|
| `INCOME` | Money received (salary, freelance, interest, etc.) |
| `EXPENSE` | Money spent (food, transport, utilities, etc.) |

### `RolesEnum`

Controls access level. Stored as a string in DB (`VIEWER`/`ANALYST`/`ADMIN`). Also embedded as `role` claim in JWT.

| Value | Who gets it | Access summary |
|---|---|---|
| `VIEWER` | Self-registered users, Google OAuth2 auto-provisioned users | GET own records + dashboard data |
| `ANALYST` | Created by ADMIN via `POST /v1/users` | All VIEWER access + POST/PATCH/DELETE own records |
| `ADMIN` | Created by ADMIN via `POST /v1/users` | All operations on all records and all users |

### `UserStatusEnum`

Controls whether a user can access the system at all.

| Value | Effect |
|---|---|
| `ACTIVE` | User can log in and call all APIs within their role |
| `INACTIVE` | Login rejected at `POST /v1/auth/login` (403). Google OAuth2 login redirected to 403 |

---

## 11. Application Error Code Catalogue

Error codes follow this namespace scheme:

| Range | Domain |
|---|---|
| `1xxxx` | Dashboard errors |
| `2xxxx` | User management errors |
| `3xxxx` | Financial record errors |
| `4xxxx` | Authentication & authorisation errors |
| `9xxxx` | General / validation errors |

### Dashboard Errors (10xxx)

| Code | Name | HTTP | Message |
|---|---|---|---|
| `10001` | `DASHBOARD_NOT_FOUND` | 404 | Dashboard configuration not found |
| `10002` | `DASHBOARD_FETCH_FAILED` | 500 | Failed to retrieve dashboard data |
| `10003` | `DASHBOARD_EMPTY` | 200 | Dashboard contains no data |
| `10004` | `DASHBOARD_CALCULATION_ERROR` | 500 | Error calculating dashboard metrics |
| `10005` | `DASHBOARD_SUMMARY_ERROR` | 500 | Unable to generate dashboard summary |
| `10006` | `DASHBOARD_PERMISSION_DENIED` | 403 | No permission to access this dashboard |
| `10007` | `DASHBOARD_INVALID_DATE_RANGE` | 400 | Invalid date range (start must be before end) |

### User Management Errors (20xxx)

| Code | Name | HTTP | Message |
|---|---|---|---|
| `20001` | `USER_NOT_FOUND` | 404 | User does not exist |
| `20002` | `USER_ALREADY_EXISTS` | 409 | Email already registered |
| `20003` | `INVALID_USER_INPUT` | 400 | Invalid input fields |
| `20004` | `USER_INACTIVE` | 403 | Account is inactive |
| `20005` | `USER_CREATION_FAILED` | 500 | Failed to create user |
| `20006` | `USER_UPDATE_FAILED` | 500 | Failed to update user |
| `20007` | `USER_DELETION_FAILED` | 500 | Failed to delete user |
| `20008` | `USER_BATCH_OPERATION_FAILED` | 500 | Batch operation partially failed |
| `20009` | `DUPLICATE_EMAIL` | 409 | Email already in use |
| `20010` | `INVALID_EMAIL_FORMAT` | 400 | Invalid email format |
| `20011` | `PASSWORD_TOO_WEAK` | 400 | Password doesn't meet strength requirements |
| `20012` | `USER_PROFILE_INCOMPLETE` | 400 | Required profile fields missing |

### Financial Record Errors (30xxx)

| Code | Name | HTTP | Message |
|---|---|---|---|
| `30001` | `FINANCIAL_RECORD_NOT_FOUND` | 404 | Record does not exist or is soft-deleted |
| `30002` | `FINANCIAL_RECORD_ALREADY_EXISTS` | 409 | Duplicate record detected |
| `30003` | `INVALID_FINANCIAL_RECORD_INPUT` | 400 | Invalid record fields |
| `30004` | `INVALID_RECORD_AMOUNT` | 400 | Amount must be > 0 |
| `30005` | `INVALID_RECORD_DATE` | 400 | Date is in the future or invalid |
| `30006` | `INVALID_RECORD_CATEGORY` | 400 | Category not valid |
| `30007` | `FINANCIAL_RECORD_CREATION_FAILED` | 500 | Record creation failed |
| `30008` | `FINANCIAL_RECORD_UPDATE_FAILED` | 500 | Record update failed |
| `30009` | `FINANCIAL_RECORD_DELETION_FAILED` | 500 | Record deletion failed |
| `30010` | `FINANCIAL_RECORD_FETCH_FAILED` | 500 | Record fetch failed |
| `30011` | `RECORD_AMOUNT_EXCEEDS_LIMIT` | 400 | Amount exceeds allowed limit |
| `30012` | `DUPLICATE_RECORD_DETECTED` | 409 | Similar record exists on same date/amount |
| `30013` | `RECORD_TYPE_MISMATCH` | 400 | Type doesn't match category |
| `30014` | `FINANCIAL_RECORD_BATCH_FAILED` | 400 | Batch import failed |

### Auth & Authorisation Errors (40xxx)

| Code | Name | HTTP | Message |
|---|---|---|---|
| `40001` | `UNAUTHORIZED` | 401 | Authentication required |
| `40002` | `INVALID_CREDENTIALS` | 401 | Wrong email/password or Google-only account |
| `40003` | `TOKEN_EXPIRED` | 401 | JWT has expired |
| `40004` | `INVALID_TOKEN` | 401 | JWT is malformed |
| `40005` | `TOKEN_REFRESH_FAILED` | 401 | Token refresh failed |
| `40006` | `UNAUTHORIZED_ACCESS` | 403 | Role insufficient for this resource |
| `40007` | `FORBIDDEN` | 403 | Operation not allowed for this account |
| `40008` | `ROLE_MODIFICATION_NOT_ALLOWED` | 403 | Cannot modify role (non-ADMIN) |
| `40009` | `INSUFFICIENT_PERMISSIONS` | 403 | Not enough permissions |
| `40010` | `SESSION_EXPIRED` | 401 | Session expired |
| `40011` | `ACCOUNT_LOCKED` | 403 | Account locked after failed attempts |
| `40012` | `ACCOUNT_SUSPENDED` | 403 | Account suspended |
| `40013` | `LOGIN_FAILED` | 401 | General login failure |
| `40014` | `PASSWORD_RESET_TOKEN_INVALID` | 400 | Reset token invalid or expired |
| `40015` | `PASSWORD_RESET_FAILED` | 500 | Password reset failed |
| `40016` | `TWO_FACTOR_REQUIRED` | 403 | 2FA required |
| `40017` | `TWO_FACTOR_FAILED` | 401 | 2FA verification failed |

### General Errors (99xxx)

| Code | Name | HTTP | Message |
|---|---|---|---|
| `99001` | `VALIDATION_ERROR` | 400 | DTO `@Valid` constraint failed |
| `99002` | `BAD_REQUEST` | 400 | Invalid request format or parameters |
| `99003` | `DATA_ACCESS_ERROR` | 500 | Database operation failed |
| `99999` | `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## 12. Quick Reference — All Endpoints

| Method | Endpoint | Auth | Min Role | Description |
|---|---|---|---|---|
| `POST` | `/v1/auth/register` | ❌ | Public | Register new VIEWER account |
| `POST` | `/v1/auth/login` | ❌ | Public | Login, returns JWT |
| `GET` | `/oauth2/authorization/google` | ❌ | Public | Initiate Google OAuth2 browser flow |
| | | | | |
| `POST` | `/v1/records` | ✅ JWT | `ANALYST` | Create a financial record |
| `GET` | `/v1/records` | ✅ JWT | `VIEWER` | List records (filterable, scoped by role) |
| `GET` | `/v1/records/{id}` | ✅ JWT | `VIEWER` | Get single record by ID |
| `PATCH` | `/v1/records/{id}` | ✅ JWT | `ANALYST` | Full-replace update a record |
| `DELETE` | `/v1/records/{id}` | ✅ JWT | `ANALYST` | Soft-delete a record |
| | | | | |
| `GET` | `/v1/dashboard/summary` | ✅ JWT | `VIEWER` | Full dashboard payload (all aggregates) |
| `GET` | `/v1/dashboard/category-totals` | ✅ JWT | `VIEWER` | Category → amount breakdown map |
| `GET` | `/v1/dashboard/monthly-trends` | ✅ JWT | `VIEWER` | Last 12 months totals |
| `GET` | `/v1/dashboard/recent-activity` | ✅ JWT | `VIEWER` | Latest 5 transactions |
| | | | | |
| `GET` | `/v1/users` | ✅ JWT | `ADMIN` | List all users |
| `GET` | `/v1/users/{id}` | ✅ JWT | `ADMIN` | Get user by ID |
| `POST` | `/v1/users` | ✅ JWT | `ADMIN` | Create user with any role |
| `PATCH` | `/v1/users/{id}` | ✅ JWT | `ADMIN` | Update user fields |
| `PATCH` | `/v1/users/{id}/status` | ✅ JWT | `ADMIN` | Activate or deactivate user |
| `DELETE` | `/v1/users/{id}` | ✅ JWT | `ADMIN` | Hard-delete a user |
