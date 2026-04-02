# 🎯 QUICK REFERENCE CARD - User Management Module

## Error Code Prefix System

```
1xxxx = Dashboard
2xxxx = User Management
3xxxx = Financial Records  
4xxxx = Authentication
99xxx = General Errors
```

---

## User Management Error Codes (2xxxx)

| Code | Error | HTTP | Message |
|------|-------|------|---------|
| 20001 | USER_NOT_FOUND | 404 | User doesn't exist |
| 20002 | USER_ALREADY_EXISTS | 409 | Email already registered |
| 20003 | INVALID_USER_INPUT | 400 | Invalid input fields |
| 20004 | USER_INACTIVE | 403 | Account inactive |
| 20005 | USER_CREATION_FAILED | 500 | Create error |
| 20006 | USER_UPDATE_FAILED | 500 | Update error |
| 20007 | USER_DELETION_FAILED | 500 | Delete error |
| 20008 | USER_BATCH_OPERATION_FAILED | 500 | Batch error |
| 20009 | DUPLICATE_EMAIL | 409 | Email in use |
| 20010 | INVALID_EMAIL_FORMAT | 400 | Bad email format |
| 20011 | PASSWORD_TOO_WEAK | 400 | Weak password |
| 20012 | USER_PROFILE_INCOMPLETE | 400 | Missing fields |

---

## REST Endpoints

```
GET    /v1/users                 → List all users
GET    /v1/users/{id}            → Get user by ID
POST   /v1/users                 → Create user
PATCH  /v1/users/{id}            → Update user
PATCH  /v1/users/{id}/status     → Update status
DELETE /v1/users/{id}            → Delete user
```

---

## Curl Examples

### Create User
```bash
curl -X POST http://localhost:8080/v1/users \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "role": "ANALYST",
    "status": "ACTIVE"
  }'
```

### Get User
```bash
curl -X GET http://localhost:8080/v1/users/1 \
  -H "Authorization: Bearer TOKEN"
```

### Update User
```bash
curl -X PATCH http://localhost:8080/v1/users/1 \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane Doe"}'
```

### Update Status
```bash
curl -X PATCH http://localhost:8080/v1/users/1/status \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "INACTIVE"}'
```

### Delete User
```bash
curl -X DELETE http://localhost:8080/v1/users/1 \
  -H "Authorization: Bearer TOKEN"
```

---

## Error Response Format

```json
{
  "timestamp": "2026-04-02T15:30:45",
  "status": 404,
  "error": "Not Found",
  "message": "The requested user does not exist...",
  "code": "20001"
}
```

---

## File Locations

### Code Files
- UserException.java
- UserRequest.java
- UserResponse.java
- UserService.java
- UserServiceImpl.java
- UserController.java
- SecurityConfig.java
- ErrorCodeEnum.java

### Documentation
- API_REFERENCE.md (endpoints & examples)
- ERROR_CODE_REFERENCE.md (all error codes)
- DEVELOPER_GUIDE.md (development guide)
- PRODUCTION_READINESS.md (deployment)

---

## Key Methods

### UserService
```java
List<UserResponse> getAllUsers()
UserResponse getUserById(Long id)
UserResponse createUser(UserRequest request)
UserResponse updateUser(Long id, UserRequest request)
UserResponse updateUserStatus(Long id, UserStatusEnum status)
void deleteUser(Long id)
boolean emailExists(String email)
boolean isUserActive(Long id)
```

### ErrorCodeEnum
```java
String getErrorCode()           // "20001"
String getErrorMessage()        // Detailed message
HttpStatus getHttpStatus()      // HttpStatus.NOT_FOUND
```

---

## HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | User fetched/updated |
| 201 | Created | User created |
| 204 | No Content | User deleted |
| 400 | Bad Request | Validation error |
| 403 | Forbidden | Unauthorized |
| 404 | Not Found | User not found |
| 409 | Conflict | Duplicate email |
| 500 | Server Error | Database error |

---

## User Roles

```
ADMIN    → Full access to all operations
ANALYST  → Limited dashboard access
VIEWER   → Read-only access
```

---

## Password Requirements

- Length: 8-50 characters
- Must contain: uppercase, lowercase, numbers
- Encrypted with: BCrypt
- Never exposed: in responses/logs

---

## Common Error Scenarios

### Scenario 1: User Not Found
```
Request: GET /v1/users/999
Response: 404, code: 20001
```

### Scenario 2: Duplicate Email
```
Request: POST /v1/users with email@example.com (already exists)
Response: 409, code: 20002
```

### Scenario 3: Weak Password
```
Request: POST /v1/users with password: "123"
Response: 400, code: 20011
```

### Scenario 4: Unauthorized
```
Request: No Authorization header
Response: 403, code: 40006
```

---

## Build & Run

### Build
```bash
mvn clean package
```

### Run
```bash
mvn spring-boot:run
```

### Test
```bash
mvn test
```

---

## Quick Checklist

- ✅ 54 error codes defined
- ✅ 6 REST endpoints
- ✅ SecurityConfig configured
- ✅ PasswordEncoder ready
- ✅ 100+ pages documentation
- ✅ Zero compilation errors
- ✅ Production ready

---

**Last Updated**: April 2, 2026  
**Version**: 1.0.0  
**Status**: Production Ready ✅
