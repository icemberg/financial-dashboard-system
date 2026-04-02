# 📋 Error Code Reference - Finance Dashboard System

## Overview

The Finance Dashboard system uses a comprehensive error code system with structured prefixes for different modules. This guide documents all error codes and their meanings.

---

## Error Code Structure

```
XXXX = PMMM
│     │└─ Specific error number (001-999)
│     └─ Module prefix (1-4, 9)
└─ Prefix digit
```

### Module Categories

| Prefix | Module | Range | HTTP Status Range |
|--------|--------|-------|-------------------|
| **1xxxx** | Dashboard | 10001-10999 | 200-500 |
| **2xxxx** | User Management | 20001-20999 | 400-500 |
| **3xxxx** | Financial Records | 30001-30999 | 400-500 |
| **4xxxx** | Authentication | 40001-40999 | 401-403 |
| **99xxx** | General Errors | 99001-99999 | 400-500 |

---

## 1. Dashboard Errors (1xxxx)

### 10001 - DASHBOARD_NOT_FOUND
**HTTP Status**: 404 NOT_FOUND
**Message**: "Dashboard configuration not found. Please check the dashboard ID and try again."
**Cause**: The requested dashboard does not exist
**Action**: Verify the dashboard ID and retry

### 10002 - DASHBOARD_FETCH_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to retrieve dashboard data. Please try again later or contact support."
**Cause**: Database or service error while fetching dashboard
**Action**: Retry after some time or contact support

### 10003 - DASHBOARD_EMPTY
**HTTP Status**: 200 OK
**Message**: "Dashboard contains no data. Start by adding financial records."
**Cause**: Dashboard exists but has no financial records
**Action**: Add financial records to populate dashboard

### 10004 - DASHBOARD_CALCULATION_ERROR
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Error calculating dashboard metrics. Invalid data detected. Please verify your financial records."
**Cause**: Invalid data detected during metric calculation
**Action**: Verify and fix financial records

### 10005 - DASHBOARD_SUMMARY_ERROR
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Unable to generate dashboard summary. Please refresh and try again."
**Cause**: Error generating dashboard summary
**Action**: Refresh and retry

### 10006 - DASHBOARD_PERMISSION_DENIED
**HTTP Status**: 403 FORBIDDEN
**Message**: "You do not have permission to access this dashboard. Contact your administrator."
**Cause**: User lacks permission to view dashboard
**Action**: Request access from administrator

### 10007 - DASHBOARD_INVALID_DATE_RANGE
**HTTP Status**: 400 BAD_REQUEST
**Message**: "Invalid date range provided for dashboard. Start date must be before end date."
**Cause**: Start date is after end date
**Action**: Provide valid date range

---

## 2. User Management Errors (2xxxx)

### 20001 - USER_NOT_FOUND
**HTTP Status**: 404 NOT_FOUND
**Message**: "The requested user does not exist. Please verify the user ID and try again."
**Cause**: User ID doesn't exist in database
**Action**: Verify user ID and retry

**Example Response**:
```json
{
  "timestamp": "2026-04-02T15:30:45",
  "status": 404,
  "error": "Not Found",
  "message": "The requested user does not exist. Please verify the user ID and try again.",
  "code": "20001"
}
```

### 20002 - USER_ALREADY_EXISTS
**HTTP Status**: 409 CONFLICT
**Message**: "A user with this email address already exists in the system. Please use a different email."
**Cause**: Email is already registered
**Action**: Use a different email address

**Example Response**:
```json
{
  "timestamp": "2026-04-02T15:31:20",
  "status": 409,
  "error": "Conflict",
  "message": "A user with this email address already exists in the system. Please use a different email.",
  "code": "20002"
}
```

### 20003 - INVALID_USER_INPUT
**HTTP Status**: 400 BAD_REQUEST
**Message**: "One or more user input fields are invalid. Please check your input and try again. Common issues: invalid email format, password too short, name too long."
**Cause**: Validation failed on user input
**Action**: Check field constraints and retry

### 20004 - USER_INACTIVE
**HTTP Status**: 403 FORBIDDEN
**Message**: "This user account is inactive and cannot access the system. Please contact your administrator to reactivate."
**Cause**: User account is deactivated
**Action**: Contact administrator for reactivation

### 20005 - USER_CREATION_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to create new user. Please try again or contact support if the issue persists."
**Cause**: Database or system error during user creation
**Action**: Retry or contact support

### 20006 - USER_UPDATE_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to update user information. Please try again or contact support."
**Cause**: Database or system error during user update
**Action**: Retry or contact support

### 20007 - USER_DELETION_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to delete user. The user may be associated with important records. Contact support for assistance."
**Cause**: User cannot be deleted (dependencies or permissions)
**Action**: Contact support

### 20008 - USER_BATCH_OPERATION_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Batch operation on users failed partially. Some users were processed successfully, others failed."
**Cause**: Batch operation partially failed
**Action**: Review failed items and retry

### 20009 - DUPLICATE_EMAIL
**HTTP Status**: 409 CONFLICT
**Message**: "The provided email is already in use by another user. Please use a unique email address."
**Cause**: Email already exists
**Action**: Use unique email

### 20010 - INVALID_EMAIL_FORMAT
**HTTP Status**: 400 BAD_REQUEST
**Message**: "The email address provided is invalid. Please provide a valid email format (e.g., user@example.com)."
**Cause**: Email format invalid
**Action**: Provide valid email (user@domain.com)

### 20011 - PASSWORD_TOO_WEAK
**HTTP Status**: 400 BAD_REQUEST
**Message**: "Password does not meet security requirements. Password must be 8-50 characters with uppercase, lowercase, and numbers."
**Cause**: Password doesn't meet security requirements
**Action**: Use stronger password (8-50 chars, mixed case, numbers)

### 20012 - USER_PROFILE_INCOMPLETE
**HTTP Status**: 400 BAD_REQUEST
**Message**: "User profile is incomplete. Please complete all required fields."
**Cause**: Required fields missing
**Action**: Fill all required fields

---

## 3. Financial Record Errors (3xxxx)

### 30001 - FINANCIAL_RECORD_NOT_FOUND
**HTTP Status**: 404 NOT_FOUND
**Message**: "The requested financial record does not exist. Please verify the record ID and try again."
**Cause**: Record ID doesn't exist
**Action**: Verify record ID

### 30002 - FINANCIAL_RECORD_ALREADY_EXISTS
**HTTP Status**: 409 CONFLICT
**Message**: "A financial record with the same details already exists. Please check for duplicates."
**Cause**: Duplicate record exists
**Action**: Check for duplicate records

### 30003 - INVALID_FINANCIAL_RECORD_INPUT
**HTTP Status**: 400 BAD_REQUEST
**Message**: "One or more financial record fields are invalid. Please check amount, date, category, and other fields."
**Cause**: Invalid field values
**Action**: Verify all fields (amount, date, category)

### 30004 - INVALID_RECORD_AMOUNT
**HTTP Status**: 400 BAD_REQUEST
**Message**: "Invalid amount provided. Amount must be greater than zero."
**Cause**: Amount is zero or negative
**Action**: Provide positive amount

### 30005 - INVALID_RECORD_DATE
**HTTP Status**: 400 BAD_REQUEST
**Message**: "Invalid date provided. Date cannot be in the future or before system start date."
**Cause**: Date out of valid range
**Action**: Provide date within valid range

### 30006 - INVALID_RECORD_CATEGORY
**HTTP Status**: 400 BAD_REQUEST
**Message**: "The provided category is not valid. Please select from available categories."
**Cause**: Invalid category
**Action**: Select valid category

### 30007 - FINANCIAL_RECORD_CREATION_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to create new financial record. Please try again or contact support."
**Cause**: Database error during creation
**Action**: Retry or contact support

### 30008 - FINANCIAL_RECORD_UPDATE_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to update financial record. Please verify your changes and try again."
**Cause**: Database error during update
**Action**: Verify changes and retry

### 30009 - FINANCIAL_RECORD_DELETION_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to delete financial record. Record may be locked or referenced by other records."
**Cause**: Record cannot be deleted
**Action**: Contact support

### 30010 - FINANCIAL_RECORD_FETCH_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to retrieve financial records. Please try again later."
**Cause**: Database error
**Action**: Retry later

### 30011 - RECORD_AMOUNT_EXCEEDS_LIMIT
**HTTP Status**: 400 BAD_REQUEST
**Message**: "The record amount exceeds the allowed limit. Please reduce the amount and try again."
**Cause**: Amount exceeds limit
**Action**: Reduce amount

### 30012 - DUPLICATE_RECORD_DETECTED
**HTTP Status**: 409 CONFLICT
**Message**: "A similar financial record already exists on the same date with the same amount. Check for duplicates."
**Cause**: Similar record exists
**Action**: Check for duplicates

### 30013 - RECORD_TYPE_MISMATCH
**HTTP Status**: 400 BAD_REQUEST
**Message**: "The record type does not match the selected category. Please verify and try again."
**Cause**: Record type doesn't match category
**Action**: Verify record type matches category

### 30014 - FINANCIAL_RECORD_BATCH_FAILED
**HTTP Status**: 400 BAD_REQUEST
**Message**: "Batch import of financial records failed. Please check the file format and try again."
**Cause**: Invalid batch file format
**Action**: Check file format and retry

---

## 4. Authentication & Authorization Errors (4xxxx)

### 40001 - UNAUTHORIZED
**HTTP Status**: 401 UNAUTHORIZED
**Message**: "Authentication required. Please log in to access this resource."
**Cause**: No valid authentication token
**Action**: Login to get token

### 40002 - INVALID_CREDENTIALS
**HTTP Status**: 401 UNAUTHORIZED
**Message**: "Invalid username or password. Please check your credentials and try again."
**Cause**: Wrong username or password
**Action**: Verify credentials and retry

### 40003 - TOKEN_EXPIRED
**HTTP Status**: 401 UNAUTHORIZED
**Message**: "Your authentication token has expired. Please log in again."
**Cause**: Token expired
**Action**: Login to get new token

### 40004 - INVALID_TOKEN
**HTTP Status**: 401 UNAUTHORIZED
**Message**: "The provided authentication token is invalid or malformed. Please log in again."
**Cause**: Token is invalid or corrupted
**Action**: Login to get valid token

### 40005 - TOKEN_REFRESH_FAILED
**HTTP Status**: 401 UNAUTHORIZED
**Message**: "Failed to refresh authentication token. Please log in again."
**Cause**: Token refresh failed
**Action**: Login again

### 40006 - UNAUTHORIZED_ACCESS
**HTTP Status**: 403 FORBIDDEN
**Message**: "You do not have permission to access this resource. Your role does not grant sufficient privileges."
**Cause**: Insufficient role permissions
**Action**: Contact admin for permissions

### 40007 - FORBIDDEN
**HTTP Status**: 403 FORBIDDEN
**Message**: "Access denied. This operation is not allowed for your account."
**Cause**: Operation not allowed
**Action**: Contact admin

### 40008 - ROLE_MODIFICATION_NOT_ALLOWED
**HTTP Status**: 403 FORBIDDEN
**Message**: "You do not have permission to modify user roles. Only administrators can perform this action."
**Cause**: Only admins can modify roles
**Action**: Contact admin

### 40009 - INSUFFICIENT_PERMISSIONS
**HTTP Status**: 403 FORBIDDEN
**Message**: "Insufficient permissions to perform this action. Please contact your administrator."
**Cause**: Insufficient permissions
**Action**: Contact admin

### 40010 - SESSION_EXPIRED
**HTTP Status**: 401 UNAUTHORIZED
**Message**: "Your session has expired. Please log in again."
**Cause**: Session timeout
**Action**: Login again

### 40011 - ACCOUNT_LOCKED
**HTTP Status**: 403 FORBIDDEN
**Message**: "Your account has been locked due to multiple failed login attempts. Please contact support."
**Cause**: Account locked
**Action**: Contact support

### 40012 - ACCOUNT_SUSPENDED
**HTTP Status**: 403 FORBIDDEN
**Message**: "Your account has been suspended. Please contact your administrator for assistance."
**Cause**: Account suspended
**Action**: Contact admin

### 40013 - LOGIN_FAILED
**HTTP Status**: 401 UNAUTHORIZED
**Message**: "Login failed. Please try again or reset your password."
**Cause**: Login error
**Action**: Retry or reset password

### 40014 - PASSWORD_RESET_TOKEN_INVALID
**HTTP Status**: 400 BAD_REQUEST
**Message**: "The password reset token is invalid or has expired. Please request a new password reset."
**Cause**: Token invalid or expired
**Action**: Request new password reset

### 40015 - PASSWORD_RESET_FAILED
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Failed to reset password. Please try again or contact support."
**Cause**: System error during reset
**Action**: Retry or contact support

### 40016 - TWO_FACTOR_REQUIRED
**HTTP Status**: 403 FORBIDDEN
**Message**: "Two-factor authentication is required. Please complete the verification."
**Cause**: 2FA required
**Action**: Complete 2FA verification

### 40017 - TWO_FACTOR_FAILED
**HTTP Status**: 401 UNAUTHORIZED
**Message**: "Two-factor authentication failed. Please try again or contact support."
**Cause**: 2FA verification failed
**Action**: Retry 2FA or contact support

---

## 5. General Errors (99xxx)

### 99001 - VALIDATION_ERROR
**HTTP Status**: 400 BAD_REQUEST
**Message**: "Validation failed. Please check your input and try again."
**Cause**: Input validation failed
**Action**: Check input fields

### 99002 - BAD_REQUEST
**HTTP Status**: 400 BAD_REQUEST
**Message**: "Bad request. The request format or parameters are invalid."
**Cause**: Invalid request format
**Action**: Check request format

### 99003 - DATA_ACCESS_ERROR
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "Database operation failed. Please try again later or contact support."
**Cause**: Database error
**Action**: Retry or contact support

### 99999 - INTERNAL_ERROR
**HTTP Status**: 500 INTERNAL_SERVER_ERROR
**Message**: "An unexpected internal server error occurred. Please try again later or contact support."
**Cause**: Unexpected system error
**Action**: Retry or contact support

---

## Usage Examples

### In UserException
```java
throw new UserException(
    "User with email already exists",
    ErrorCodeEnum.USER_ALREADY_EXISTS
);
```

### In GlobalExceptionHandler
```java
ErrorResponse body = ErrorResponse.builder()
    .timestamp(LocalDateTime.now())
    .status(status.value())
    .error(status.getReasonPhrase())
    .message(ex.getMessage())
    .code(ex.getCode())
    .build();
```

### HTTP Response
```json
{
  "timestamp": "2026-04-02T15:35:20",
  "status": 404,
  "error": "Not Found",
  "message": "The requested user does not exist. Please verify the user ID and try again.",
  "code": "20001"
}
```

---

## Best Practices

1. ✅ Use appropriate error code prefix (1xxxx, 2xxxx, etc.)
2. ✅ Use correct HTTP status code
3. ✅ Provide detailed, user-friendly message
4. ✅ Include suggested action in message
5. ✅ Log error with appropriate level
6. ✅ Don't expose sensitive data in error message
7. ✅ Use ErrorCodeEnum directly in exceptions

---

## Error Handling Flow

```
User Request
    ↓
Validation (99001, 20003, 30003, etc.)
    ↓
Authentication (40001-40017)
    ↓
Authorization (40006-40009)
    ↓
Business Logic (20001-20012, 30001-30014, 10001-10007)
    ↓
Database (99003)
    ↓
Response with ErrorCodeEnum & HTTP Status
```

---

**Last Updated**: April 2, 2026  
**Version**: 1.0.0
