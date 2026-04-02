# User Management API - Quick Reference

## Base URL
```
/v1/users
```

## Endpoints Overview

### 1. Get All Users
```
GET /v1/users
Authorization: Bearer ADMIN_TOKEN
Security: hasRole('ADMIN')
Status: 200 OK
Response: List<UserResponse>
```

**Success Response (200)**:
```json
[
  {
    "id": 1,
    "name": "Admin User",
    "email": "admin@example.com",
    "role": "ADMIN",
    "status": "ACTIVE",
    "createdAt": "2026-04-01T08:00:00",
    "updatedAt": "2026-04-01T10:30:00"
  },
  {
    "id": 2,
    "name": "Analyst User",
    "email": "analyst@example.com",
    "role": "ANALYST",
    "status": "ACTIVE",
    "createdAt": "2026-04-02T09:15:00",
    "updatedAt": null
  }
]
```

**Error Response (403)**:
```json
{
  "timestamp": "2026-04-02T15:30:45",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource",
  "code": "UNAUTHORIZED_ACCESS"
}
```

---

### 2. Get User by ID
```
GET /v1/users/{id}
Authorization: Bearer TOKEN
Security: hasRole('ADMIN')
Status: 200 OK | 404 NOT_FOUND | 403 FORBIDDEN
Response: UserResponse
```

**Path Parameters**:
- `id` (Long, required): User ID

**Success Response (200)**:
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "ANALYST",
  "status": "ACTIVE",
  "createdAt": "2026-04-02T10:30:00",
  "updatedAt": null
}
```

**Error Response (404)**:
```json
{
  "timestamp": "2026-04-02T15:35:20",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 999",
  "code": "USER_NOT_FOUND"
}
```

---

### 3. Create User
```
POST /v1/users
Authorization: Bearer ADMIN_TOKEN
Content-Type: application/json
Security: hasRole('ADMIN')
Status: 201 CREATED | 400 BAD_REQUEST | 409 CONFLICT
Request: UserRequest
Response: UserResponse
```

**Request Body**:
```json
{
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "password": "SecurePass@123",
  "role": "VIEWER",
  "status": "ACTIVE"
}
```

**Success Response (201)**:
```json
{
  "id": 3,
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "role": "VIEWER",
  "status": "ACTIVE",
  "createdAt": "2026-04-02T16:45:30",
  "updatedAt": null
}
```

**Validation Error (400)**:
```json
{
  "timestamp": "2026-04-02T16:46:00",
  "status": 400,
  "error": "Bad Request",
  "message": "email: Email should be valid, password: Password must be between 8 and 50 characters",
  "code": "VALIDATION_ERROR"
}
```

**Conflict Error (409)**:
```json
{
  "timestamp": "2026-04-02T16:47:15",
  "status": 409,
  "error": "Conflict",
  "message": "User with email jane.smith@example.com already exists",
  "code": "USER_ALREADY_EXISTS"
}
```

---

### 4. Update User
```
PATCH /v1/users/{id}
Authorization: Bearer ADMIN_TOKEN
Content-Type: application/json
Security: hasRole('ADMIN')
Status: 200 OK | 400 BAD_REQUEST | 404 NOT_FOUND | 409 CONFLICT
Request: UserRequest (partial)
Response: UserResponse
```

**Path Parameters**:
- `id` (Long, required): User ID

**Request Body (Partial Update)**:
```json
{
  "name": "Jane Doe",
  "email": "jane.doe@example.com"
}
```

**Success Response (200)**:
```json
{
  "id": 3,
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "role": "VIEWER",
  "status": "ACTIVE",
  "createdAt": "2026-04-02T16:45:30",
  "updatedAt": "2026-04-02T17:00:45"
}
```

**Conflict Error (409)**:
```json
{
  "timestamp": "2026-04-02T17:01:30",
  "status": 409,
  "error": "Conflict",
  "message": "Email jane.doe@example.com is already in use",
  "code": "USER_ALREADY_EXISTS"
}
```

---

### 5. Update User Status
```
PATCH /v1/users/{id}/status
Authorization: Bearer ADMIN_TOKEN
Content-Type: application/json
Security: hasRole('ADMIN')
Status: 200 OK | 400 BAD_REQUEST | 404 NOT_FOUND
Request: UserRequest
Response: UserResponse
```

**Path Parameters**:
- `id` (Long, required): User ID

**Request Body**:
```json
{
  "status": "INACTIVE"
}
```

**Valid Status Values**:
- `ACTIVE`: User can access the system
- `INACTIVE`: User is locked out

**Success Response (200)**:
```json
{
  "id": 3,
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "role": "VIEWER",
  "status": "INACTIVE",
  "createdAt": "2026-04-02T16:45:30",
  "updatedAt": "2026-04-02T17:05:00"
}
```

**Validation Error (400)**:
```json
{
  "timestamp": "2026-04-02T17:06:15",
  "status": 400,
  "error": "Bad Request",
  "message": "Status must be provided",
  "code": "INVALID_USER_INPUT"
}
```

---

### 6. Delete User
```
DELETE /v1/users/{id}
Authorization: Bearer ADMIN_TOKEN
Security: hasRole('ADMIN')
Status: 204 NO_CONTENT | 404 NOT_FOUND | 403 FORBIDDEN
Response: Empty body
```

**Path Parameters**:
- `id` (Long, required): User ID

**Success Response (204)**:
```
No Content
```

**Error Response (404)**:
```json
{
  "timestamp": "2026-04-02T17:10:30",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 999",
  "code": "USER_NOT_FOUND"
}
```

---

## Request/Response DTOs

### UserRequest
Used for creation and updates.

```json
{
  "name": "string (2-100 chars)",
  "email": "string (valid email format)",
  "password": "string (8-50 chars, for creation only)",
  "role": "VIEWER | ANALYST | ADMIN",
  "status": "ACTIVE | INACTIVE"
}
```

**Field Requirements**:
- `name`: Required, 2-100 characters
- `email`: Required, valid email format, must be unique
- `password`: Required for creation (8-50 chars), optional for updates
- `role`: Required, one of: VIEWER, ANALYST, ADMIN
- `status`: Required, one of: ACTIVE, INACTIVE

### UserResponse
Returned in all responses (password excluded).

```json
{
  "id": "number",
  "name": "string",
  "email": "string",
  "role": "VIEWER | ANALYST | ADMIN",
  "status": "ACTIVE | INACTIVE",
  "createdAt": "2026-04-02T10:30:00",
  "updatedAt": "2026-04-02T14:22:00 or null"
}
```

---

## HTTP Status Codes

| Code | Meaning | Scenario |
|------|---------|----------|
| 200 | OK | Successful read/update operation |
| 201 | Created | User successfully created |
| 204 | No Content | Successful delete operation |
| 400 | Bad Request | Validation failed, invalid input |
| 403 | Forbidden | Unauthorized access, not ADMIN role |
| 404 | Not Found | User ID does not exist |
| 409 | Conflict | Email already exists (duplicate) |
| 500 | Server Error | Unexpected server error |

---

## Error Response Format

All errors follow this format:

```json
{
  "timestamp": "2026-04-02T15:30:45",
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message",
  "code": "ERROR_CODE_ENUM"
}
```

**Error Codes**:
- `USER_NOT_FOUND`: User ID doesn't exist
- `USER_ALREADY_EXISTS`: Email already registered
- `UNAUTHORIZED_ACCESS`: Access denied
- `INVALID_USER_INPUT`: Validation failed
- `USER_INACTIVE`: User account is deactivated
- `ROLE_MODIFICATION_NOT_ALLOWED`: Cannot change role
- `VALIDATION_ERROR`: @Valid failed
- `DATA_ACCESS_ERROR`: Database error
- `INTERNAL_ERROR`: Server error

---

## Role-Based Access Control

| Endpoint | Role Required |
|----------|---------------|
| GET /v1/users | ADMIN |
| GET /v1/users/{id} | ADMIN |
| POST /v1/users | ADMIN |
| PATCH /v1/users/{id} | ADMIN |
| PATCH /v1/users/{id}/status | ADMIN |
| DELETE /v1/users/{id} | ADMIN |

**Roles Available**:
- `ADMIN`: Full access to user management
- `ANALYST`: Limited access (defined elsewhere)
- `VIEWER`: Read-only access (defined elsewhere)

---

## Curl Examples

### Get All Users
```bash
curl -X GET http://localhost:8080/v1/users \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

### Create User
```bash
curl -X POST http://localhost:8080/v1/users \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "SecurePass@123",
    "role": "ANALYST",
    "status": "ACTIVE"
  }'
```

### Get User by ID
```bash
curl -X GET http://localhost:8080/v1/users/1 \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

### Update User Status
```bash
curl -X PATCH http://localhost:8080/v1/users/1/status \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "INACTIVE"}'
```

### Delete User
```bash
curl -X DELETE http://localhost:8080/v1/users/1 \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

---

## Validation Rules

### Email
- Format: Valid email (RFC 5322)
- Unique: No duplicate emails in system
- Example: `user@example.com`

### Password
- Length: 8-50 characters
- Recommend: Mix of uppercase, lowercase, digits, special characters
- Example: `SecurePass@123!`

### Name
- Length: 2-100 characters
- Required: Cannot be empty
- Example: `John Doe`

### Status
- Values: `ACTIVE` or `INACTIVE`
- ACTIVE users: Can access the system
- INACTIVE users: Locked out

### Role
- Values: `VIEWER`, `ANALYST`, `ADMIN`
- ADMIN: Full system access
- ANALYST: Limited dashboard access
- VIEWER: Read-only access

---

## Notes

1. **Password Security**: Passwords are encrypted using BCrypt. They are never returned in responses.
2. **Timestamps**: All timestamps are in ISO 8601 format (YYYY-MM-DDTHH:mm:ss).
3. **Partial Updates**: PATCH endpoints support partial updates (only changed fields).
4. **Authorization**: All endpoints require Bearer token in Authorization header.
5. **ADMIN Only**: User management is restricted to ADMIN role.
6. **Email Uniqueness**: Each email can only be registered once.

---

## Troubleshooting

### 403 Forbidden
- Ensure your token has ADMIN role
- Check Authorization header is present
- Verify Bearer token format

### 409 Conflict (Email Already Exists)
- Email is already registered
- Use different email or update existing user

### 404 Not Found
- User ID doesn't exist
- Check the ID is correct
- Verify user hasn't been deleted

### 400 Bad Request (Validation Error)
- Check required fields are provided
- Verify email format is valid
- Ensure password is 8-50 characters
- Confirm status is ACTIVE or INACTIVE

---

**Last Updated**: April 2, 2026
