# Zorvyn — Finance Dashboard Backend

A production-grade backend system for a finance dashboard where users interact with financial records based on their role. Built with **Java 17**, **Spring Boot 4**, **Spring Security**, **MySQL**, and **JWT authentication**.

---

## Table of Contents

- [Overview](#overview)
- [Live Deployment](#live-deployment)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Role-Based Access Control](#role-based-access-control)
- [Data Model](#data-model)
- [Error Handling](#error-handling)
- [Security Design](#security-design)
- [Design Decisions & Assumptions](#design-decisions--assumptions)

---

## Overview

Zorvyn is a finance dashboard backend that provides:

- **User & Role Management** — Create, update, and manage users with role-based access (VIEWER, ANALYST, ADMIN)
- **Financial Records CRUD** — Full lifecycle management of income/expense entries with multi-criteria filtering
- **Dashboard Analytics** — Aggregated summaries including totals, category breakdowns, monthly trends, and recent activity
- **Two-Layer Access Control** — Controller-level `@PreAuthorize` annotations combined with service-level ownership enforcement
- **JWT + OAuth2 Authentication** — Stateless API authentication with Google OAuth2 integration

### Key Features Beyond Basic CRUD

| Feature | Details |
|---|---|
| **Pagination** | Configurable page size (max 100), sort field/direction on record listings |
| **Soft Delete** | Financial records are never physically removed — `deleted` flag preserves data integrity |
| **Partial Updates** | User profile updates accept only changed fields (no full-payload requirement) |
| **Password Reset Flow** | Forgot → token (15-min TTL, single-use) → reset — with user enumeration prevention |
| **JWT Refresh** | Extend sessions without re-entering credentials |
| **Google OAuth2** | Auto-registration on first Google login, JWT issued on callback |
| **Structured Error Codes** | 40+ categorized error codes with consistent JSON error responses |
| **Profile-Based Config** | Separate `local` and `prod` property files for environment isolation |

---

## Live Deployment

The API is deployed and live on **Render**. You can explore and test the API directly via the Swagger UI:

- **Swagger UI (Interactive Docs)**: [https://financial-dashboard-system.onrender.com/swagger-ui/index.html](https://financial-dashboard-system.onrender.com/swagger-ui/index.html)
- **Base API URL**: `https://financial-dashboard-system.onrender.com/v1`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Security | Spring Security 7 + JWT (jjwt 0.12.6) + Google OAuth2 |
| Database | MySQL 8+ with Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation (`@NotNull`, `@Size`, `@Email`, etc.) |
| API Docs | SpringDoc OpenAPI 3.0.2 (Swagger UI) |
| Build | Maven with Lombok annotation processing |
| Monitoring | Spring Boot Actuator (`/actuator/health`) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT / FRONTEND                          │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTP (REST)
┌──────────────────────────────▼──────────────────────────────────────┐
│                        SECURITY FILTER CHAIN                        │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────────┐   │
│  │ CORS Filter │→ │ JwtAuthFilter│→ │ @PreAuthorize (SpEL)     │   │
│  └─────────────┘  └──────────────┘  └──────────────────────────┘   │
│                   OAuth2LoginSuccessHandler (Google callback)        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                        CONTROLLER LAYER                             │
│  AuthController        UserController                               │
│  /v1/auth/*             /v1/users/*                                 │
│                                                                     │
│  FinancialRecordController    FinancialDashboardController           │
│  /v1/records/*                /v1/dashboard/*                       │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                         SERVICE LAYER                               │
│  AuthServiceImpl          UserServiceImpl                           │
│  FinancialRecordServiceImpl   FinancialDashboardServiceImpl         │
│                                                                     │
│  ┌─────────────────────┐  ┌──────────────────────┐                 │
│  │ AuthenticationHelper│  │ UserResolutionUtil    │                 │
│  │ (extract JWT email) │  │ (role-based filtering)│                 │
│  └─────────────────────┘  └──────────────────────┘                 │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                       REPOSITORY LAYER                              │
│  UserRepository      FinancialRecordRepository                      │
│  PasswordResetTokenRepository                                       │
│  (Spring Data JPA + custom @Query JPQL)                             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                          MySQL DATABASE                             │
│  users │ financial_records │ password_reset_tokens                   │
└─────────────────────────────────────────────────────────────────────┘
```

### Request Flow

1. **Client** sends HTTP request with `Authorization: Bearer <JWT>` header
2. **JwtAuthFilter** extracts and validates the JWT, sets the `SecurityContext`
3. **`@PreAuthorize`** checks role-level access at the controller method
4. **Controller** delegates to the service layer (no business logic in controllers)
5. **Service layer** enforces data-level ownership rules (defence in depth)
6. **Repository** executes JPA/JPQL queries against MySQL
7. **GlobalExceptionHandler** catches any exception and returns a structured `ErrorResponse`

### Detailed Architecture — All Components & Dependencies

The diagram below maps **every Java file** in the project and how they interact:

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   CLIENT / FRONTEND                                  │
│                        (Browser, Postman, or any HTTP client)                         │
└──────────────────────────────────────┬──────────────────────────────────────────────┘
                                       │
                                       │  HTTP Request
                                       │  Authorization: Bearer <JWT>
                                       ▼
╔═════════════════════════════════════════════════════════════════════════════════════╗
║                              SECURITY LAYER                                        ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  SecurityConfig.java                                                         │  ║
║  │  ─────────────────                                                          │  ║
║  │  • securityFilterChain()     → defines URL access rules                     │  ║
║  │  • corsConfigurationSource() → injects app.cors.allowed-origins             │  ║
║  │  • passwordEncoder()         → BCryptPasswordEncoder bean                   │  ║
║  │  • authenticationProvider()  → links CustomUserDetailsService + BCrypt      │  ║
║  │                                                                              │  ║
║  │  URL Rules:                                                                  │  ║
║  │    /v1/auth/register, /login, /forgot-password, /reset-password → permitAll │  ║
║  │    /oauth2/**, /login/oauth2/**                                 → permitAll │  ║
║  │    /swagger-ui/**, /v3/api-docs/**                              → permitAll │  ║
║  │    /actuator/health                                             → permitAll │  ║
║  │    ALL OTHER ENDPOINTS                                          → JWT req'd │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                          │                                                         ║
║              ┌───────────▼────────────┐      ┌──────────────────────────────────┐  ║
║              │  JwtAuthFilter.java     │      │  OAuth2LoginSuccessHandler.java  │  ║
║              │  ─────────────────────  │      │  ──────────────────────────────  │  ║
║              │  extends                │      │  • onAuthenticationSuccess()     │  ║
║              │  OncePerRequestFilter   │      │    ├─ extract Google email+name  │  ║
║              │                         │      │    ├─ find or create User        │  ║
║              │  • doFilterInternal()   │      │    ├─ call JwtService.generate() │  ║
║              │    ├─ extract Bearer    │      │    └─ redirect to frontend with  │  ║
║              │    │  token from header │      │       ?token=<JWT>               │  ║
║              │    ├─ call JwtService   │      └──────────────┬───────────────────┘  ║
║              │    │  .extractEmail()   │                     │                      ║
║              │    ├─ load UserDetails  │                     │                      ║
║              │    │  from DB           │      ┌──────────────▼───────────────────┐  ║
║              │    ├─ validate token    │      │  JwtService.java                 │  ║
║              │    └─ set SecurityCtx   │      │  ────────────────                │  ║
║              └───────────┬────────────┘      │  • generateToken(UserDetails)    │  ║
║                          │                    │    → claims: sub, role, userId   │  ║
║                          │ uses               │    → algorithm: HS256            │  ║
║                          └────────────────────│    → expiry: app.jwt.expiration  │  ║
║                                               │  • extractEmail(token)           │  ║
║                                               │  • isTokenValid(token, user)     │  ║
║                                               │  • getExpirationMs()             │  ║
║                                               └─────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  CustomUserDetailsService.java                                               │  ║
║  │  ─────────────────────────────                                              │  ║
║  │  implements UserDetailsService                                               │  ║
║  │  • loadUserByUsername(email) → fetches User from DB → returns CustomUserDtls │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  CustomUserDetails.java                                                      │  ║
║  │  ──────────────────────                                                     │  ║
║  │  implements UserDetails                                                      │  ║
║  │  • getAuthorities() → ROLE_{user.role}                                      │  ║
║  │  • getUserId()       → user.id (used in @PreAuthorize SpEL)                 │  ║
║  │  • getUsername()     → user.email                                           │  ║
║  │  • isEnabled()       → user.status == ACTIVE                                │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════╤══════════════════════════════════════════════╝
                                       │
                          ┌────────────▼────────────┐
                          │  @PreAuthorize (SpEL)    │
                          │  Method-level security   │
                          │  evaluated per-endpoint  │
                          └────────────┬────────────┘
                                       │
╔══════════════════════════════════════▼══════════════════════════════════════════════╗
║                              CONTROLLER LAYER                                      ║
║                    (Request validation, delegation, HTTP status)                    ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  AuthController.java — /v1/auth                                              │  ║
║  │  ──────────────────────────────────                                         │  ║
║  │  POST /register          → authService.register(req)          → 201 Created │  ║
║  │  POST /login             → authService.login(req)             → 200 OK      │  ║
║  │  POST /refresh           → authService.refresh(email)         → 200 OK      │  ║
║  │  PATCH /change-password  → authService.changePassword(e,req)  → 204         │  ║
║  │  POST /forgot-password   → authService.initPasswordReset(req) → 200 OK      │  ║
║  │  POST /reset-password    → authService.resetPassword(req)     → 204         │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  UserController.java — /v1/users                                             │  ║
║  │  ───────────────────────────────                                            │  ║
║  │  @PreAuthorize:                                                              │  ║
║  │  GET    /              ADMIN only      → userService.getAllUsers()       200 │  ║
║  │  GET    /{id}          ADMIN or self   → userService.getUserById(id)    200 │  ║
║  │  POST   /              ADMIN only      → userService.createUser(req)    201 │  ║
║  │  PATCH  /{id}          ADMIN or self   → userService.updateUser(id,req) 200 │  ║
║  │  PATCH  /{id}/status   ADMIN only      → userService.updateStatus(i,r) 200 │  ║
║  │  DELETE /{id}          ADMIN only      → userService.deleteUser(id)     204 │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  FinancialRecordController.java — /v1/records                                │  ║
║  │  ────────────────────────────────────────────                               │  ║
║  │  @PreAuthorize:                                                              │  ║
║  │  POST   /          ANALYST|ADMIN → recordService.createRecord(req,email) 201│  ║
║  │  GET    /          authenticated → recordService.getAllRecords(...)      200 │  ║
║  │  GET    /{id}      authenticated → recordService.getRecordById(id,email)200 │  ║
║  │  PATCH  /{id}      ANALYST|ADMIN → recordService.updateRecord(id,r,e)   200 │  ║
║  │  DELETE /{id}      ANALYST|ADMIN → recordService.softDeleteRecord(id,e) 204 │  ║
║  │                                                                              │  ║
║  │  Query params: page, size (max 100), sortBy, sortDir, category, type,       │  ║
║  │                from, to — all optional, all combinable                       │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  FinancialDashboardController.java — /v1/dashboard                           │  ║
║  │  ─────────────────────────────────────────────                              │  ║
║  │  GET /summary          → dashboardService.getSummary(email)        → 200 OK │  ║
║  │  GET /category-totals  → dashboardService.getCategoryTotals(email) → 200 OK │  ║
║  │  GET /monthly-trends   → dashboardService.getMonthlyTrends(email)  → 200 OK │  ║
║  │  GET /recent-activity  → dashboardService.getRecentActivity(email) → 200 OK │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════╤══════════════════════════════════════════════╝
                                       │
╔══════════════════════════════════════▼══════════════════════════════════════════════╗
║                               SERVICE LAYER                                        ║
║                (Business logic, validation, ownership, error wrapping)              ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  AuthServiceImpl.java  (implements AuthService interface)                    │  ║
║  │  ────────────────────                                                       │  ║
║  │  Dependencies: UserRepository, PasswordResetTokenRepository,                 │  ║
║  │                PasswordEncoder (BCrypt), JwtService                          │  ║
║  │                                                                              │  ║
║  │  • register(RegisterRequest)                                                 │  ║
║  │    ├─ validatePasswordStrength() — regex: uppercase + lowercase + digit      │  ║
║  │    ├─ check email uniqueness    — 409 if exists                             │  ║
║  │    ├─ BCrypt encode password                                                │  ║
║  │    ├─ save User (role=VIEWER, status=ACTIVE)                                │  ║
║  │    └─ buildAuthResponse() → JWT + user profile                              │  ║
║  │                                                                              │  ║
║  │  • login(LoginRequest)                                                       │  ║
║  │    ├─ find user by email       — 401 if not found (generic msg)             │  ║
║  │    ├─ check status == ACTIVE   — 403 if inactive                            │  ║
║  │    ├─ BCrypt verify password   — 401 if mismatch                            │  ║
║  │    └─ buildAuthResponse()                                                   │  ║
║  │                                                                              │  ║
║  │  • refresh(email)              — re-verify active, issue fresh JWT           │  ║
║  │  • changePassword(email, req)  — verify current pwd, validate new, save     │  ║
║  │  • initPasswordReset(req)      — generate UUID token (15min TTL)            │  ║
║  │  • resetPassword(req)          — validate token/expiry, encode new pwd      │  ║
║  │                                                                              │  ║
║  │  Private: validatePasswordStrength(), buildAuthResponse()                    │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  UserServiceImpl.java  (implements UserService interface)                    │  ║
║  │  ────────────────────                                                       │  ║
║  │  Dependencies: UserRepository, PasswordEncoder (BCrypt)                      │  ║
║  │                                                                              │  ║
║  │  • getAllUsers()           — returns List<UserResponse>                      │  ║
║  │  • getUserById(id)        — validates id > 0, returns UserResponse or 404   │  ║
║  │  • getUserByEmail(email)  — validates non-blank, returns UserResponse       │  ║
║  │  • createUser(UserRequest)                                                   │  ║
║  │    ├─ check email uniqueness — 409 if exists                                │  ║
║  │    ├─ validatePasswordStrength()                                            │  ║
║  │    ├─ BCrypt encode password                                                │  ║
║  │    └─ save with requested role + status                                     │  ║
║  │  • updateUser(id, UserUpdateRequest)                                        │  ║
║  │    ├─ partial update — only non-null, non-blank fields applied              │  ║
║  │    ├─ email uniqueness check if email changed                               │  ║
║  │    └─ password excluded — use /auth/change-password                         │  ║
║  │  • updateUserStatus(id, StatusUpdateRequest) — ACTIVE or INACTIVE           │  ║
║  │  • deleteUser(id)         — hard delete                                     │  ║
║  │                                                                              │  ║
║  │  Private: convertToResponse(User) → UserResponse (excludes password)        │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  FinancialRecordServiceImpl.java  (implements FinancialRecordService)        │  ║
║  │  ───────────────────────────────                                            │  ║
║  │  Dependencies: FinancialRecordRepository, UserResolutionUtil                 │  ║
║  │                                                                              │  ║
║  │  • createRecord(RecordRequest, email)                                        │  ║
║  │    ├─ resolve user from email                                               │  ║
║  │    ├─ build FinancialRecord entity (owner = authenticated user)             │  ║
║  │    └─ save → return RecordResponse                                          │  ║
║  │                                                                              │  ║
║  │  • getAllRecords(email, category, type, from, to, pageable)                  │  ║
║  │    ├─ resolve userId filter (null for ADMIN, userId for others)              │  ║
║  │    └─ call findAllByFilters() → Page<RecordResponse>                        │  ║
║  │                                                                              │  ║
║  │  • getRecordById(id, email)  — find + enforceOwnership + return             │  ║
║  │  • updateRecord(id, req, email) — find + enforceOwnership + update + save   │  ║
║  │  • softDeleteRecord(id, email)  — find + enforceOwnership + set deleted=true│  ║
║  │                                                                              │  ║
║  │  Private:                                                                    │  ║
║  │    findActiveRecordOrThrow(id) — findByIdAndDeletedFalse() or 404           │  ║
║  │    enforceOwnership(record, user, action) — 403 if not owner & not ADMIN    │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  FinancialDashboardServiceImpl.java  (implements FinancialDashboardService)  │  ║
║  │  ──────────────────────────────────                                         │  ║
║  │  Dependencies: FinancialRecordRepository, UserResolutionUtil,                │  ║
║  │                AuthenticationHelper, DataMapperUtil                          │  ║
║  │                                                                              │  ║
║  │  • getSummary(email) → DashboardSummaryResponse                             │  ║
║  │    ├─ resolve userId filter                                                 │  ║
║  │    ├─ sumIncome(userId)  — JPQL: COALESCE(SUM(amount), 0) where INCOME      │  ║
║  │    ├─ sumExpense(userId) — JPQL: COALESCE(SUM(amount), 0) where EXPENSE     │  ║
║  │    ├─ netBalance = income - expense                                         │  ║
║  │    ├─ categoryTotals(userId) — JPQL: GROUP BY category                      │  ║
║  │    ├─ monthlyTrends(12 months, userId) — JPQL: GROUP BY year, month         │  ║
║  │    └─ recentActivity(userId, top 5) — ordered by transactionDate DESC       │  ║
║  │                                                                              │  ║
║  │  • getCategoryTotals(email) → Map<String, BigDecimal>                       │  ║
║  │  • getMonthlyTrends(email)  → List<MonthlyTrendResponse>                    │  ║
║  │  • getRecentActivity(email) → List<RecordResponse>                          │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌─────────────────────────────────┐  ┌─────────────────────────────────────────┐  ║
║  │  AuthenticationHelper.java      │  │  UserResolutionUtil.java                │  ║
║  │  ─────────────────────────      │  │  ──────────────────────                │  ║
║  │  @Component (utility)           │  │  @Component (utility)                  │  ║
║  │                                 │  │                                         │  ║
║  │  • extractUserEmailOrThrow      │  │  • getUserOrThrow(email)               │  ║
║  │    (Authentication)             │  │    → User or 404                        │  ║
║  │    → extracts email from        │  │                                         │  ║
║  │      SecurityContext principal   │  │  • resolveUserIdFilter(user)           │  ║
║  │    → throws 401 if missing      │  │    → ADMIN: null (see all)             │  ║
║  │                                 │  │    → Other: user.id (own only)          │  ║
║  │  Used by:                       │  │                                         │  ║
║  │    All controllers to get       │  │  • resolveUserIdFilterByEmail(email)    │  ║
║  │    authenticated user's email   │  │    → convenience: lookup + filter       │  ║
║  └─────────────────────────────────┘  └─────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  DataMapperUtil.java                                                         │  ║
║  │  ───────────────────                                                        │  ║
║  │  @Component (utility)                                                        │  ║
║  │  • mapCategoryTotals(List<Object[]>)   → Map<String, BigDecimal>            │  ║
║  │  • mapMonthlyTrends(List<Object[]>)    → List<MonthlyTrendResponse>         │  ║
║  │  • mapRecentActivity(List<Object[]>)   → List<RecordResponse>               │  ║
║  │                                                                              │  ║
║  │  Transforms raw JPQL Object[] results into typed DTOs                        │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════╤══════════════════════════════════════════════╝
                                       │
╔══════════════════════════════════════▼══════════════════════════════════════════════╗
║                             REPOSITORY LAYER                                       ║
║                    (Spring Data JPA + custom @Query JPQL)                           ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  UserRepository.java  (extends JpaRepository<User, Long>)                    │  ║
║  │  ────────────────────                                                       │  ║
║  │  • findByEmail(String email)  → Optional<User>                              │  ║
║  │  • (inherited) findById, save, deleteById, findAll                          │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  FinancialRecordRepository.java  (extends JpaRepository)                     │  ║
║  │  ──────────────────────────────                                             │  ║
║  │  Custom @Query JPQL:                                                         │  ║
║  │  • findByIdAndDeletedFalse(id)          → Optional<FinancialRecord>         │  ║
║  │  • findAllByFilters(userId, category,   → Page<FinancialRecord>             │  ║
║  │      type, from, to, pageable)            multi-criteria with pagination    │  ║
║  │  • sumByType(userId, RecordTypeEnum)    → BigDecimal (COALESCE + SUM)       │  ║
║  │  • findCategoryTotals(userId)           → List<Object[]> (category, sum)    │  ║
║  │  • findMonthlyTrends(start, end, userId)→ List<Object[]> (yr, mo, sum)      │  ║
║  │  • findRecentActivity(userId, pageable) → List<Object[]> (top N records)    │  ║
║  │                                                                              │  ║
║  │  All queries include: deleted = false AND (:userId IS NULL OR                │  ║
║  │                        createdBy.id = :userId)                               │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  PasswordResetTokenRepository.java  (extends JpaRepository)                  │  ║
║  │  ─────────────────────────────────                                          │  ║
║  │  • findByTokenAndUsedFalse(token) → Optional<PasswordResetToken>            │  ║
║  │  • deleteByUserAndUsedFalse(user) → void (cleanup old tokens)               │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════╤══════════════════════════════════════════════╝
                                       │
╔══════════════════════════════════════▼══════════════════════════════════════════════╗
║                             ENTITY / MODEL LAYER                                   ║
║                                                                                    ║
║  ┌────────────────────────┐ ┌──────────────────────┐ ┌──────────────────────────┐  ║
║  │  User.java             │ │ FinancialRecord.java  │ │ PasswordResetToken.java  │  ║
║  │  ─────────             │ │ ────────────────────  │ │ ────────────────────────  │  ║
║  │  id       (Long, PK)   │ │ id       (Long, PK)   │ │ id        (Long, PK)     │  ║
║  │  name     (String)     │ │ amount   (BigDecimal)  │ │ token     (String, UQ)   │  ║
║  │  email    (String, UQ) │ │ type     (RecordType)  │ │ user      (FK → User)    │  ║
║  │  password (String,null)│ │ category (String)      │ │ expiresAt (LocalDateTime)│  ║
║  │  role     (RolesEnum)  │ │ txnDate  (LocalDate)   │ │ used      (Boolean)      │  ║
║  │  status   (StatusEnum) │ │ notes    (String,null)  │ │ createdAt (LocalDateTime)│  ║
║  │  createdAt(LocalDT)    │ │ createdBy(FK → User)   │ └──────────────────────────┘  ║
║  │  updatedAt(LocalDT)    │ │ deleted  (Boolean)     │                               ║
║  └────────────────────────┘ │ createdAt(LocalDT)     │                               ║
║                              │ updatedAt(LocalDT)     │                               ║
║                              └──────────────────────┘                               ║
╚══════════════════════════════════════╤══════════════════════════════════════════════╝
                                       │
╔══════════════════════════════════════▼══════════════════════════════════════════════╗
║                             ENUM DEFINITIONS                                       ║
║                                                                                    ║
║  ┌──────────────────┐  ┌───────────────────┐  ┌─────────────────────────────────┐  ║
║  │  RolesEnum        │  │  RecordTypeEnum   │  │  UserStatusEnum                 │  ║
║  │  ─────────        │  │  ──────────────   │  │  ──────────────                 │  ║
║  │  • VIEWER         │  │  • INCOME         │  │  • ACTIVE                       │  ║
║  │  • ANALYST        │  │  • EXPENSE        │  │  • INACTIVE                     │  ║
║  │  • ADMIN          │  │                   │  │                                 │  ║
║  └──────────────────┘  └───────────────────┘  └─────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  ErrorCodeEnum.java  (40+ error codes)                                       │  ║
║  │  ──────────────────                                                         │  ║
║  │  1xxxx → Dashboard errors  (10001–10007)                                    │  ║
║  │  2xxxx → User errors       (20001–20012)                                    │  ║
║  │  3xxxx → Record errors     (30001–30014)                                    │  ║
║  │  4xxxx → Auth errors       (40001–40017)                                    │  ║
║  │  99xxx → System errors     (99001–99999)                                    │  ║
║  │                                                                              │  ║
║  │  Each entry: errorCode (String) + errorMessage (String) + httpStatus         │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════╤══════════════════════════════════════════════╝
                                       │
╔══════════════════════════════════════▼══════════════════════════════════════════════╗
║                          EXCEPTION HANDLING LAYER                                  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  FinancialDashboardException.java (base)                                     │  ║
║  │  ──────────────────────────────────────                                     │  ║
║  │  extends RuntimeException                                                    │  ║
║  │  Fields: errorCode (String), code (ErrorCodeEnum), status (HttpStatus)       │  ║
║  │  3 constructors for flexible creation                                        │  ║
║  │                                                                              │  ║
║  │  └── UserException.java (extends FinancialDashboardException)                │  ║
║  │      ──────────────────                                                     │  ║
║  │      3 constructors — convenience for user-related errors                    │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                    ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐  ║
║  │  GlobalExceptionHandler.java  (@RestControllerAdvice)                        │  ║
║  │  ───────────────────────────                                                │  ║
║  │  Catches ALL exceptions → converts to ErrorResponse JSON                    │  ║
║  │                                                                              │  ║
║  │  @ExceptionHandler mappings:                                                 │  ║
║  │  ┌──────────────────────────────────┬────────┬──────────────────────────┐    │  ║
║  │  │ Exception Type                   │ Status │ When                     │    │  ║
║  │  ├──────────────────────────────────┼────────┼──────────────────────────┤    │  ║
║  │  │ FinancialDashboardException      │ varies │ Domain business errors   │    │  ║
║  │  │ MethodArgumentNotValidException  │ 400    │ @Valid failures          │    │  ║
║  │  │ HttpMessageNotReadableException  │ 400    │ Malformed JSON body      │    │  ║
║  │  │ MethodArgumentTypeMismatchEx.    │ 400    │ Bad param types          │    │  ║
║  │  │ MissingServletRequestParamEx.    │ 400    │ Missing required param   │    │  ║
║  │  │ AccessDeniedException            │ 403    │ @PreAuthorize failure    │    │  ║
║  │  │ HttpRequestMethodNotSupportedEx. │ 405    │ Wrong HTTP verb          │    │  ║
║  │  │ Exception (fallback)             │ 500    │ Unexpected errors        │    │  ║
║  │  └──────────────────────────────────┴────────┴──────────────────────────┘    │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════╤══════════════════════════════════════════════╝
                                       │
╔══════════════════════════════════════▼══════════════════════════════════════════════╗
║                               DTO LAYER                                            ║
║                                                                                    ║
║  Request DTOs (with Jakarta Bean Validation)                                       ║
║  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐                   ║
║  │ RegisterRequest   │ │ LoginRequest     │ │ RecordRequest    │                   ║
║  │ • name  @NotBlank │ │ • email @NotBlank│ │ • amount @NotNull│                   ║
║  │ • email @Email    │ │ • password       │ │ • type   @NotNull│                   ║
║  │ • password @Size  │ │   @NotBlank      │ │ • category       │                   ║
║  │   (min=8,max=50)  │ │                  │ │ • txnDate        │                   ║
║  └──────────────────┘ └──────────────────┘ │   @PastOrPresent  │                   ║
║  ┌──────────────────┐ ┌──────────────────┐ │ • notes (optional)│                   ║
║  │ UserRequest       │ │ UserUpdateRequest│ └──────────────────┘                   ║
║  │ • name, email,    │ │ (all optional)   │ ┌──────────────────┐                   ║
║  │   password, role, │ │ • name, email,   │ │ ChangePasswordReq│                   ║
║  │   status          │ │   role, status   │ │ • currentPassword│                   ║
║  └──────────────────┘ └──────────────────┘ │ • newPassword     │                   ║
║  ┌──────────────────┐ ┌──────────────────┐ └──────────────────┘                   ║
║  │ ForgotPasswordReq│ │ ResetPasswordReq │ ┌──────────────────┐                   ║
║  │ • email @Email    │ │ • token @NotBlank│ │ StatusUpdateReq   │                   ║
║  └──────────────────┘ │ • newPassword     │ │ • status @NotNull│                   ║
║                       └──────────────────┘ └──────────────────┘                   ║
║                                                                                    ║
║  Response DTOs                                                                     ║
║  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐                   ║
║  │ AuthResponse      │ │ UserResponse     │ │ RecordResponse   │                   ║
║  │ • token           │ │ • id, name       │ │ • id, amount     │                   ║
║  │ • tokenType       │ │ • email          │ │ • type, category │                   ║
║  │ • expiresIn       │ │ • role, status   │ │ • txnDate, notes │                   ║
║  │ • userId, email   │ │ • createdAt      │ │ • createdBy      │                   ║
║  │ • name, role      │ │ • updatedAt      │ │ • createdAt      │                   ║
║  └──────────────────┘ └──────────────────┘ └──────────────────┘                   ║
║  ┌──────────────────────────┐ ┌──────────────────┐ ┌──────────────────┐           ║
║  │ DashboardSummaryResponse │ │ MonthlyTrendResp │ │ ErrorResponse    │           ║
║  │ • totalIncome             │ │ • year           │ │ • timestamp      │           ║
║  │ • totalExpense            │ │ • month          │ │ • status         │           ║
║  │ • netBalance              │ │ • total          │ │ • error          │           ║
║  │ • categoryTotals (Map)    │ │                  │ │ • message        │           ║
║  │ • recentActivity (List)   │ └──────────────────┘ │ • code           │           ║
║  │ • monthlyTrends (List)    │                       └──────────────────┘           ║
║  └──────────────────────────┘                                                      ║
╚══════════════════════════════════════╤══════════════════════════════════════════════╝
                                       │
                                       ▼
                     ┌──────────────────────────────────┐
                     │          MySQL DATABASE            │
                     │                                    │
                     │  ┌────────────┐ ┌───────────────┐ │
                     │  │   users     │ │  financial_   │ │
                     │  │            │ │  records      │ │
                     │  └──────┬─────┘ └───────────────┘ │
                     │         │                          │
                     │  ┌──────▼──────────────────────┐  │
                     │  │  password_reset_tokens       │  │
                     │  └─────────────────────────────┘  │
                     └──────────────────────────────────┘
```

### Component Dependency Graph

```
AuthController ──────────────────→ AuthServiceImpl
                                     ├─→ UserRepository
                                     ├─→ PasswordResetTokenRepository
                                     ├─→ PasswordEncoder (BCrypt)
                                     └─→ JwtService

UserController ──────────────────→ UserServiceImpl
                                     ├─→ UserRepository
                                     └─→ PasswordEncoder (BCrypt)

FinancialRecordController ───────→ FinancialRecordServiceImpl
   │                                 ├─→ FinancialRecordRepository
   ├─→ AuthenticationHelper          └─→ UserResolutionUtil
   │                                       └─→ UserRepository
   │
   └─→ FinancialDashboardController → FinancialDashboardServiceImpl
                                        ├─→ FinancialRecordRepository
                                        ├─→ UserResolutionUtil
                                        ├─→ AuthenticationHelper
                                        └─→ DataMapperUtil

SecurityFilterChain
   ├─→ JwtAuthFilter ──→ JwtService + CustomUserDetailsService
   └─→ OAuth2LoginSuccessHandler ──→ JwtService + UserRepository
```

---

## Project Structure

```
src/main/java/com/financedashboard/zorvyn/
├── ZorvynApplication.java              # Spring Boot entry point
├── config/
│   └── SecurityConfig.java             # Security filter chain, CORS, auth providers
├── controller/
│   ├── AuthController.java             # /v1/auth/* — register, login, refresh, password reset
│   ├── UserController.java             # /v1/users/* — user CRUD (admin)
│   ├── FinancialRecordController.java  # /v1/records/* — record CRUD with pagination
│   └── FinancialDashboardController.java # /v1/dashboard/* — analytics endpoints
├── dto/
│   ├── RegisterRequest.java            # Registration input
│   ├── LoginRequest.java               # Login input
│   ├── AuthResponse.java               # JWT + user profile response
│   ├── RecordRequest.java              # Financial record input (validated)
│   ├── RecordResponse.java             # Financial record output
│   ├── UserRequest.java                # Admin user creation input
│   ├── UserUpdateRequest.java          # Partial update input (all fields optional)
│   ├── UserResponse.java               # User output (password excluded)
│   ├── StatusUpdateRequest.java        # Status-only update
│   ├── DashboardSummaryResponse.java   # Composite dashboard data
│   ├── MonthlyTrendResponse.java       # Year/month/total trend data
│   ├── ChangePasswordRequest.java      # Password change input
│   ├── ForgotPasswordRequest.java      # Password reset initiation
│   ├── ResetPasswordRequest.java       # Password reset execution
│   ├── ErrorResponse.java              # Structured error output
│   └── HttpError.java                  # Lightweight error DTO
├── entity/
│   ├── User.java                       # JPA entity — users table
│   ├── FinancialRecord.java            # JPA entity — financial_records table
│   └── PasswordResetToken.java         # JPA entity — password reset tokens
├── enums/
│   ├── RolesEnum.java                  # VIEWER, ANALYST, ADMIN
│   ├── RecordTypeEnum.java             # INCOME, EXPENSE
│   ├── UserStatusEnum.java             # ACTIVE, INACTIVE
│   └── ErrorCodeEnum.java              # 40+ categorized error codes
├── exception/
│   ├── FinancialDashboardException.java # Base domain exception
│   ├── UserException.java              # User-specific exception (extends base)
│   └── GlobalExceptionHandler.java     # @RestControllerAdvice — 8 handlers
├── repository/interfaces/
│   ├── UserRepository.java
│   ├── FinancialRecordRepository.java  # Custom JPQL for aggregation + filtering
│   └── PasswordResetTokenRepository.java
├── security/
│   ├── JwtService.java                 # Token generation, validation, claim extraction
│   ├── JwtAuthFilter.java              # OncePerRequestFilter — JWT validation
│   ├── CustomUserDetails.java          # UserDetails implementation with userId
│   ├── CustomUserDetailsService.java   # Loads user from DB for Spring Security
│   └── OAuth2LoginSuccessHandler.java  # Google OAuth2 → JWT issuance
└── service/
    ├── interfaces/                     # Service contracts
    ├── impl/                           # Service implementations
    └── util/
        ├── AuthenticationHelper.java   # Extract email from SecurityContext
        ├── UserResolutionUtil.java     # Role-based user ID filter resolution
        └── DataMapperUtil.java         # Object[] → DTO mapping for aggregation queries
```

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **MySQL 8+**

### 1. Create the Database

```sql
CREATE DATABASE finance_dashboard;
```

### 2. Configure Local Properties

Edit `src/main/resources/application-local.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finance_dashboard
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

Hibernate will auto-create tables on first run (`ddl-auto=update`).

### 3. Build & Run

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run with local profile
mvn spring-boot:run "-Dspring-boot.run.profiles=local"

# Or run the JAR directly
java -jar target/zorvyn-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

The server starts on **http://localhost:8080**.

### 4. Access API Documentation

Open **http://localhost:8080/swagger-ui.html** in your browser for the interactive Swagger UI.

### 5. Bootstrap an Admin User

Since self-registration creates VIEWER accounts only, you need to seed an admin. Insert directly into MySQL:

```sql
-- Password: Admin@123 (BCrypt encoded)
INSERT INTO users (name, email, password, role, status, created_at)
VALUES (
    'Admin',
    'admin@zorvyn.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoHKeuonEuPCDVhUzXLH0.E4eUj5L9JImhge',
    'ADMIN',
    'ACTIVE',
    NOW()
);
```

Then log in via `POST /v1/auth/login` to get your JWT.

---

## API Reference

### Authentication — `/v1/auth`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/v1/auth/register` | Public | Register as VIEWER, returns JWT |
| `POST` | `/v1/auth/login` | Public | Email/password login, returns JWT |
| `POST` | `/v1/auth/forgot-password` | Public | Request password reset token |
| `POST` | `/v1/auth/reset-password` | Public | Reset password using token |
| `POST` | `/v1/auth/refresh` | JWT | Get a fresh JWT |
| `PATCH` | `/v1/auth/change-password` | JWT | Change own password |

**Register Example:**
```bash
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "MyPass123"
  }'
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "role": "VIEWER"
}
```

---

### User Management — `/v1/users`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/v1/users` | ADMIN | List all users |
| `GET` | `/v1/users/{id}` | ADMIN or own profile | Get user by ID |
| `POST` | `/v1/users` | ADMIN | Create user with any role |
| `PATCH` | `/v1/users/{id}` | ADMIN or own profile | Partial update user |
| `PATCH` | `/v1/users/{id}/status` | ADMIN | Update user status (ACTIVE/INACTIVE) |
| `DELETE` | `/v1/users/{id}` | ADMIN | Delete user |

---

### Financial Records — `/v1/records`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/v1/records` | ADMIN | Create a record (with optional userId assignment) |
| `GET` | `/v1/records` | ANALYST \| ADMIN | List records (paginated, filtered) |
| `GET` | `/v1/records/{id}` | ANALYST \| ADMIN | Get single record |
| `PATCH` | `/v1/records/{id}` | ADMIN | Update a record (ALL fields replaced) |
| `DELETE` | `/v1/records/{id}` | ADMIN | Soft-delete a record |

**Pagination & Filtering (GET /v1/records):**

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | 0-based page index |
| `size` | `20` | Items per page (max 100) |
| `sortBy` | `transactionDate` | Field to sort by |
| `sortDir` | `desc` | Sort direction (`asc` / `desc`) |
| `category` | — | Filter by exact category |
| `type` | — | Filter by `INCOME` or `EXPENSE` |
| `from` | — | Start date inclusive (`YYYY-MM-DD`) |
| `to` | — | End date inclusive (`YYYY-MM-DD`) |

**Example:**
```bash
curl "http://localhost:8080/v1/records?type=EXPENSE&category=Food&from=2024-01-01&page=0&size=10" \
  -H "Authorization: Bearer <JWT>"
```

---

### Dashboard Analytics — `/v1/dashboard`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/v1/dashboard/summary` | All roles | Full dashboard (income, expenses, net balance, category totals, trends, recent activity) |
| `GET` | `/v1/dashboard/category-totals` | All roles | Category-wise breakdown |
| `GET` | `/v1/dashboard/monthly-trends` | All roles | Last 12 months income/expense trends |
| `GET` | `/v1/dashboard/recent-activity` | All roles | 5 most recent transactions |

**Summary Response:**
```json
{
  "totalIncome": 15000.00,
  "totalExpense": 8500.50,
  "netBalance": 6499.50,
  "categoryTotals": {
    "Salary": 15000.00,
    "Food": 3200.50,
    "Transport": 1800.00,
    "Entertainment": 3500.00
  },
  "recentActivity": [ ... ],
  "monthlyTrends": [
    { "year": 2024, "month": 12, "total": 5200.00 },
    { "year": 2024, "month": 11, "total": 4800.50 }
  ]
}
```

---

## Role-Based Access Control

### Role Definitions

| Role | Description |
|---|---|
| **VIEWER** | Read-only access to own records and dashboard data |
| **ANALYST** | Full CRUD on own records + read dashboard data |
| **ADMIN** | Full access to all records and all users |

### Access Control Matrix

| Operation | VIEWER | ANALYST | ADMIN |
|---|---|---|---|
| View own records | ✅ | ✅ | ✅ |
| View all records | ❌ | ❌ | ✅ |
| Create records | ❌ | ✅ | ✅ |
| Update own records | ❌ | ✅ | ✅ |
| Update any record | ❌ | ❌ | ✅ |
| Delete own records | ❌ | ✅ | ✅ |
| Delete any record | ❌ | ❌ | ✅ |
| View dashboard | ✅ | ✅ | ✅ |
| View own profile | ✅ | ✅ | ✅ |
| Update own profile | ✅ | ✅ | ✅ |
| Manage all users | ❌ | ❌ | ✅ |

### Enforcement Layers

1. **Controller layer** — `@PreAuthorize("hasRole('ADMIN')")` blocks unauthorized roles before any logic runs
2. **Service layer** — `enforceOwnership()` verifies the authenticated user owns the record they're accessing
3. **Repository layer** — `resolveUserIdFilter()` returns `null` for ADMIN (sees all) or the user's ID (sees only own)

This **defence-in-depth** approach ensures access control is not bypassed even if one layer is misconfigured.

---

## Data Model

### Entity Relationship

```
┌─────────────────────┐       ┌──────────────────────────┐
│       users          │       │    financial_records      │
├─────────────────────┤       ├──────────────────────────┤
│ id (PK, BIGINT)     │──┐    │ id (PK, BIGINT)          │
│ name (VARCHAR)       │  │    │ amount (DECIMAL 19,2)    │
│ email (VARCHAR, UQ)  │  │    │ type (ENUM: INCOME/      │
│ password (VARCHAR,   │  │    │       EXPENSE)           │
│          nullable)   │  │    │ category (VARCHAR)       │
│ role (ENUM: VIEWER/  │  │    │ transaction_date (DATE)  │
│       ANALYST/ADMIN) │  │    │ notes (VARCHAR, nullable)│
│ status (ENUM: ACTIVE/│  ├───→│ created_by (FK → users)  │
│        INACTIVE)     │  │    │ deleted (BOOLEAN)        │
│ created_at (DATETIME)│  │    │ created_at (DATETIME)    │
│ updated_at (DATETIME)│  │    │ updated_at (DATETIME)    │
└─────────────────────┘  │    └──────────────────────────┘
                          │
                          │    ┌──────────────────────────┐
                          │    │  password_reset_tokens    │
                          │    ├──────────────────────────┤
                          │    │ id (PK, BIGINT)          │
                          └───→│ user_id (FK → users)     │
                               │ token (VARCHAR, UQ)      │
                               │ expires_at (DATETIME)    │
                               │ used (BOOLEAN)           │
                               │ created_at (DATETIME)    │
                               └──────────────────────────┘
```

### Key Design Choices

- **`BigDecimal`** for monetary amounts — avoids floating-point precision errors
- **`password` is nullable** — Google OAuth2 users have no local password
- **Soft delete** on financial records — `deleted=true` flag, never physically removed
- **`created_by` FK** — links every financial record to its owner for row-level security

---

## Error Handling

### Structured Error Response

Every error returns a consistent JSON structure:

```json
{
  "timestamp": "2024-12-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "The requested financial record does not exist.",
  "code": "30001"
}
```

### Error Code Categories

| Range | Category | Examples |
|---|---|---|
| `1xxxx` | Dashboard | `10001` Dashboard not found, `10004` Calculation error |
| `2xxxx` | User Management | `20001` User not found, `20002` Email already exists, `20011` Password too weak |
| `3xxxx` | Financial Records | `30001` Record not found, `30007` Creation failed |
| `4xxxx` | Authentication | `40001` Unauthorized, `40002` Invalid credentials, `40006` Access denied |
| `99xxx` | System | `99001` Validation error, `99003` Database error, `99999` Internal error |

### Exception Handlers

The `GlobalExceptionHandler` (`@RestControllerAdvice`) handles:

| Exception Type | HTTP Status | When Triggered |
|---|---|---|
| `FinancialDashboardException` | Per-exception | Domain business logic errors |
| `MethodArgumentNotValidException` | 400 | `@Valid` annotation failures |
| `HttpMessageNotReadableException` | 400 | Malformed JSON body |
| `MethodArgumentTypeMismatchException` | 400 | Bad path/query parameter types |
| `MissingServletRequestParameterException` | 400 | Required parameter missing |
| `AccessDeniedException` | 403 | `@PreAuthorize` role check failures |
| `HttpRequestMethodNotSupportedException` | 405 | Wrong HTTP method |
| `Exception` (fallback) | 500 | Unexpected errors (no details leaked) |

---

## Security Design

### Authentication Flow

```
                    ┌──────────────┐
                    │   Register   │──→ BCrypt encode → save → issue JWT
                    └──────────────┘

                    ┌──────────────┐
                    │    Login     │──→ verify BCrypt → check active → issue JWT
                    └──────────────┘

┌──────────────┐    ┌──────────────┐
│ Google OAuth2 │──→│ Success      │──→ find-or-create user → issue JWT
│ Consent Page  │   │ Handler      │    → redirect to frontend with ?token=<JWT>
└──────────────┘    └──────────────┘
```

### JWT Token Structure

```json
{
  "sub": "user@example.com",
  "role": "ANALYST",
  "userId": 42,
  "iat": 1702641600,
  "exp": 1702728000
}
```

- **Algorithm**: HS256
- **Expiry**: 24 hours (configurable via `app.jwt.expiration-ms`)
- **Custom claims**: `role` and `userId` embedded to avoid extra DB lookups

### Password Security

- Passwords are **BCrypt-encoded** before storage
- Passwords are **never returned** in any API response (`UserResponse` excludes them)
- Password strength enforced: minimum 8 chars, requires uppercase + lowercase + digit
- Google OAuth2 users have `null` password — prevented from using password-based endpoints

### Password Reset Flow

1. `POST /v1/auth/forgot-password` → generates UUID token (15-min TTL, single-use)
2. Token logged to console in local dev (email in production)
3. `POST /v1/auth/reset-password` → validates token → BCrypt encodes new password → marks token as used
4. Always returns 200 on forgot-password (prevents user enumeration)

---

## Design Decisions & Assumptions

### Assumptions Made

1. **Single role per user** — Users have exactly one role (not a set of roles). This simplifies the model while meeting the stated requirements.
2. **Self-registration = VIEWER** — The lowest privilege is assigned by default. Only ADMIN can create ANALYST/ADMIN accounts.
3. **Record ownership** — Financial records belong to the user who created them. Non-ADMIN users cannot see other users' records.
4. **Soft delete for records, hard delete for users** — Records preserve financial audit trails; user deletion is admin-only and permanent.
5. **Dashboard data is user-scoped** — ADMIN sees aggregates over all records; other roles see only their own data.

### Architecture Decisions

| Decision | Rationale |
|---|---|
| **Interface-driven services** | `UserService` interface → `UserServiceImpl` enables testing with mocks and respects dependency inversion |
| **DTOs separate from entities** | Prevents exposing internal fields (e.g., password hash), allows request validation without polluting entities |
| **Repository-level aggregation** | Dashboard totals computed via JPQL `SUM`/`GROUP BY` rather than loading all records into memory |
| **Utility classes for cross-cutting concerns** | `AuthenticationHelper` and `UserResolutionUtil` avoid duplicating auth/resolution logic across services |
| **COALESCE in JPQL** | `SUM` returns `null` for empty result sets — `COALESCE(SUM(...), 0)` ensures the API never returns `null` for totals |
| **Profile-based configuration** | `application-local.properties` and `application-prod.properties` keep environment config isolated from code |

### What This Project Does NOT Include (Out of Scope)

- Frontend application
- Email service integration (password reset tokens are logged to console)
- Rate limiting
- Caching layer
- File upload/import
- Deployment configuration (Docker, CI/CD)

---

## Configuration Reference

### Environment Profiles

| Profile | Activation | Purpose |
|---|---|---|
| `local` | `--spring.profiles.active=local` | Local development with MySQL on localhost |
| `prod` | `--spring.profiles.active=prod` | Production with externalized configs |

### Key Properties

| Property | Default | Description |
|---|---|---|
| `app.jwt.secret` | (in local config) | Base64-encoded HS256 key (min 256 bits) |
| `app.jwt.expiration-ms` | `86400000` | JWT lifetime (24 hours) |
| `app.cors.allowed-origins` | `localhost:3000,5173` | Comma-separated CORS origins |
| `app.oauth2.redirect-uri` | `localhost:3000/oauth2/callback` | Frontend OAuth2 callback URL |

---

## Health Check

```bash
curl http://localhost:8080/actuator/health
# {"status": "UP"}
```

Publicly accessible for load balancer probes without authentication.

---

## License

This project was built as a backend development assignment submission.
