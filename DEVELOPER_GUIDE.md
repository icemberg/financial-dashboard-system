# Developer Guide - User Management Module

## Overview

This guide provides information for developers working with the User Management module. It covers architecture, key concepts, and common development tasks.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Module Structure](#module-structure)
3. [Key Classes](#key-classes)
4. [Data Flow](#data-flow)
5. [Security Concepts](#security-concepts)
6. [Common Tasks](#common-tasks)
7. [Debugging Tips](#debugging-tips)
8. [Testing Guide](#testing-guide)
9. [Best Practices](#best-practices)
10. [Extending the Module](#extending-the-module)

---

## Architecture Overview

### Layered Architecture

```
┌─────────────────────────────────────────┐
│          REST API Layer                  │
│        UserController                    │
│  (HTTP Request/Response Handling)        │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Business Logic Layer               │
│   UserService (Interface)                │
│   UserServiceImpl (Implementation)        │
│  (Validation, Security, Business Rules) │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│       Data Access Layer                  │
│      UserRepository (JPA)                │
│   (Database Operations)                  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│     Database Layer                       │
│   MySQL Database                         │
│   (users table)                          │
└──────────────────────────────────────────┘
```

### Supporting Layers

**DTOs** (Data Transfer Objects):
- `UserRequest`: API input validation
- `UserResponse`: API response format

**Exceptions**:
- `UserException`: Business logic errors
- `GlobalExceptionHandler`: Error centralization

**Enums**:
- `ErrorCodeEnum`: Standardized error codes
- `RolesEnum`: User roles
- `UserStatusEnum`: User status values

---

## Module Structure

```
src/main/java/com/financedashboard/zorvyn/
├── controller/
│   └── UserController.java          ← REST endpoints
├── service/
│   ├── interfaces/
│   │   └── UserService.java         ← Service contract
│   └── impl/
│       └── UserServiceImpl.java      ← Service implementation
├── repository/
│   └── interfaces/
│       └── UserRepository.java      ← Data access
├── entity/
│   └── User.java                    ← JPA entity (modified)
├── dto/
│   ├── UserRequest.java             ← Request DTO (new)
│   ├── UserResponse.java            ← Response DTO (new)
│   └── ErrorResponse.java           ← Error format
├── exception/
│   ├── UserException.java           ← Custom exception (updated)
│   ├── GlobalExceptionHandler.java  ← Handler (updated)
│   └── FinancialDashboardException.java
├── enums/
│   ├── ErrorCodeEnum.java           ← Updated with user codes
│   ├── RolesEnum.java
│   └── UserStatusEnum.java
```

---

## Key Classes

### UserController
**Location**: `controller/UserController.java`
**Responsibility**: Handle HTTP requests/responses
**Key Aspects**:
- REST endpoint mapping
- Request validation with @Valid
- Security annotations (@PreAuthorize)
- Logging entry points

**Important Methods**:
```java
// GET all users
GET /v1/users

// GET user by ID
GET /v1/users/{id}

// POST create user
POST /v1/users

// PATCH update user
PATCH /v1/users/{id}

// PATCH update status
PATCH /v1/users/{id}/status

// DELETE user
DELETE /v1/users/{id}
```

### UserService / UserServiceImpl
**Location**: `service/interfaces/UserService.java` + `service/impl/UserServiceImpl.java`
**Responsibility**: Business logic and validation
**Key Aspects**:
- User CRUD operations
- Email duplicate checks
- Password encryption
- Transaction management
- Error handling

**Important Methods**:
```java
getAllUsers()              // Fetch all users
getUserById(Long id)       // Fetch by ID
getUserByEmail(String email) // Fetch by email
createUser(UserRequest)    // Create with validation
updateUser(Long id, UserRequest)  // Update details
updateUserStatus(Long id, UserStatusEnum) // Update status
deleteUser(Long id)        // Delete user
emailExists(String email)  // Check email duplicate
isUserActive(Long id)      // Check active status
```

### User Entity
**Location**: `entity/User.java`
**Responsibility**: Database representation
**Columns**:
- `id`: Primary key (auto-generated)
- `name`: User's name
- `email`: User's email (unique)
- `password`: Encrypted password
- `role`: User's role (enum)
- `status`: User's status (enum)
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp

### UserRepository
**Location**: `repository/interfaces/UserRepository.java`
**Responsibility**: Database access
**Methods**:
```java
// Inherited from JpaRepository
findById(Long id)
findAll()
save(User user)
deleteById(Long id)

// Custom query
findByEmail(String email)
```

---

## Data Flow

### Creating a User (POST /v1/users)

```
1. Client sends HTTP POST request
   ↓
2. UserController.createUser() receives request
   ↓
3. @Valid annotation triggers validation
   ↓
4. Validation fails? → MethodArgumentNotValidException → GlobalExceptionHandler (400)
   ↓
5. UserServiceImpl.createUser() called
   ↓
6. Check if email exists → emailExists() → UserRepository.findByEmail()
   ↓
7. Email exists? → UserException → GlobalExceptionHandler (409)
   ↓
8. Encrypt password using PasswordEncoder.encode()
   ↓
9. Create User entity with UserBuilder
   ↓
10. Save to database → UserRepository.save()
    ↓
11. Convert to UserResponse DTO (without password)
    ↓
12. Return ResponseEntity(201 CREATED, userResponse)
    ↓
13. Client receives HTTP 201 with user data
```

### Retrieving a User (GET /v1/users/{id})

```
1. Client sends HTTP GET request
   ↓
2. @PreAuthorize checks ADMIN role
   ↓
3. Role missing? → Spring Security denies (403)
   ↓
4. UserController.getUserById(id) called
   ↓
5. UserServiceImpl.getUserById(id) called
   ↓
6. UserRepository.findById(id) queries database
   ↓
7. User found? Convert to UserResponse
   ↓
8. User not found? → UserException → GlobalExceptionHandler (404)
   ↓
9. Return ResponseEntity(200 OK, userResponse)
    ↓
10. Client receives HTTP 200 with user data
```

---

## Security Concepts

### Role-Based Access Control (RBAC)

**Roles Available**:
- `ADMIN`: Full access to user management
- `ANALYST`: Limited access
- `VIEWER`: Read-only access

**Implementation**:
```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
    // Only ADMIN role can execute
}
```

**How It Works**:
1. Spring Security intercepts request
2. Checks authentication principal
3. Validates role from authorities
4. Denies access if role not present → 403 FORBIDDEN

### Password Security

**BCrypt Encryption**:
```java
// In UserServiceImpl.createUser()
String encryptedPassword = passwordEncoder.encode(userRequest.getPassword());

// Never store plain text!
user.setPassword(encryptedPassword);
```

**Why BCrypt?**
- Slow algorithm (prevents brute force)
- Adaptive (can increase rounds over time)
- Salted (prevents rainbow table attacks)
- Industry standard

### Input Validation

**Layers of Validation**:

1. **DTO Level**:
```java
@NotBlank(message = "Email is required")
@Email(message = "Email should be valid")
private String email;
```

2. **Service Level**:
```java
if (emailExists(userRequest.getEmail())) {
    throw new UserException("Email already exists", USER_ALREADY_EXISTS);
}
```

3. **Database Level**:
```java
@Column(nullable = false, unique = true)
private String email;
```

---

## Common Tasks

### Task 1: Add New User Field

**Goal**: Add a `phone` field to users

**Steps**:

1. **Update Entity**:
```java
@Column(nullable = false)
private String phone;
```

2. **Update DTOs**:
```java
// UserRequest.java
@NotBlank
@Pattern(regexp = "\\d{10}") // Example: US phone
private String phone;

// UserResponse.java
private String phone;
```

3. **Update Database**:
```sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20) NOT NULL;
```

4. **Update Service** (if needed):
```java
public UserResponse createUser(UserRequest userRequest) {
    // ...
    user.setPhone(userRequest.getPhone());
    // ...
}
```

### Task 2: Add Email Verification

**Goal**: Verify email before user can login

**Implementation**:

1. Add field to User entity:
```java
private boolean emailVerified = false;
```

2. Add to UserResponse:
```java
private boolean emailVerified;
```

3. Update service:
```java
public void sendVerificationEmail(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException("User not found", USER_NOT_FOUND));
    // Send email with verification token
}

public void verifyEmail(Long userId, String token) {
    // Validate token
    User user = userRepository.findById(userId).orElseThrow();
    user.setEmailVerified(true);
    userRepository.save(user);
}
```

4. Add controller endpoints:
```java
@PostMapping("/{id}/verify-email")
public ResponseEntity<Void> verifyEmail(@PathVariable Long id, @RequestParam String token) {
    userService.verifyEmail(id, token);
    return ResponseEntity.ok().build();
}
```

### Task 3: Add User Search/Filter

**Goal**: Search users by name or email

**Implementation**:

1. Update repository:
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByNameContainingIgnoreCase(String name);
    List<User> findByRole(RolesEnum role);
    List<User> findByStatus(UserStatusEnum status);
}
```

2. Update service:
```java
public List<UserResponse> searchByName(String name) {
    return userRepository.findByNameContainingIgnoreCase(name)
        .stream()
        .map(this::convertToResponse)
        .toList();
}
```

3. Add controller:
```java
@GetMapping("/search")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String query) {
    List<UserResponse> results = userService.searchByName(query);
    return ResponseEntity.ok(results);
}
```

---

## Debugging Tips

### Enable Debug Logging

**Application Properties**:
```properties
logging.level.com.financedashboard.zorvyn=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

### Common Issues

**Issue 1: 403 Forbidden on all endpoints**
```
Solution:
1. Check user has ADMIN role in authentication
2. Verify @PreAuthorize annotation syntax
3. Ensure Spring Security is configured
4. Check Authorization header is present
```

**Issue 2: 409 Conflict - Email already exists**
```
Solution:
1. Database has duplicate emails (run: SELECT * FROM users WHERE email = 'test@test.com')
2. Check emailExists() logic
3. Verify unique constraint on database
```

**Issue 3: Password encoding issues**
```
Solution:
1. Ensure PasswordEncoder bean is configured
2. Check passwordEncoder.encode() is called
3. Never store plain passwords directly
```

**Issue 4: Validation errors not showing**
```
Solution:
1. Add @Valid to @RequestBody
2. Add spring-boot-starter-validation dependency
3. Check MethodArgumentNotValidException handler
4. Verify error messages in annotations
```

### Debug Queries

```sql
-- Check all users
SELECT * FROM users;

-- Find user by email
SELECT * FROM users WHERE email = 'test@example.com';

-- Check user status
SELECT id, name, status FROM users WHERE status = 'INACTIVE';

-- Count users by role
SELECT role, COUNT(*) FROM users GROUP BY role;

-- Find recently created users
SELECT * FROM users WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 DAY);
```

---

## Testing Guide

### Unit Tests Example

```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testCreateUserSuccess() {
        // Given
        UserRequest request = UserRequest.builder()
            .name("John Doe")
            .email("john@example.com")
            .password("SecurePass123!")
            .role(RolesEnum.ANALYST)
            .status(UserStatusEnum.ACTIVE)
            .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertNotNull(response);
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        verify(userRepository).save(any());
    }

    @Test
    void testCreateUserDuplicateEmail() {
        // Given
        UserRequest request = UserRequest.builder()
            .name("Jane Doe")
            .email("existing@example.com")
            .password("SecurePass123!")
            .role(RolesEnum.VIEWER)
            .status(UserStatusEnum.ACTIVE)
            .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        // When & Then
        assertThrows(UserException.class, () -> userService.createUser(request));
    }

    @Test
    void testGetUserNotFound() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> userService.getUserById(999L));
    }
}
```

### Integration Tests Example

```java
@SpringBootTest
@Sql("/db/test-data.sql")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateUserIntegration() throws Exception {
        // Given
        String requestBody = """
            {
                "name": "Integration Test User",
                "email": "integration@test.com",
                "password": "TestPass123!",
                "role": "ANALYST",
                "status": "ACTIVE"
            }
            """;

        // When & Then
        mockMvc.perform(post("/v1/users")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("Integration Test User"));

        // Verify database
        User savedUser = userRepository.findByEmail("integration@test.com").orElseThrow();
        assertNotNull(savedUser.getId());
    }
}
```

---

## Best Practices

### 1. Always Use DTOs

❌ **Don't**:
```java
@PostMapping
public User createUser(@RequestBody User user) {
    return userRepository.save(user);
}
```

✅ **Do**:
```java
@PostMapping
public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
    return ResponseEntity.status(201).body(userService.createUser(request));
}
```

### 2. Never Expose Passwords

❌ **Don't**:
```java
private UserResponse convertToResponse(User user) {
    return UserResponse.builder()
        .password(user.getPassword())  // NEVER!
        .build();
}
```

✅ **Do**:
```java
private UserResponse convertToResponse(User user) {
    return UserResponse.builder()
        // password is intentionally NOT included
        .build();
}
```

### 3. Use Proper Exception Handling

❌ **Don't**:
```java
try {
    User user = userRepository.findById(id).get(); // throws exception
} catch (Exception e) {
    // swallow exception
}
```

✅ **Do**:
```java
User user = userRepository.findById(id)
    .orElseThrow(() -> new UserException("User not found", USER_NOT_FOUND));
```

### 4. Log Appropriately

❌ **Don't**:
```java
log.info("User password: {}", user.getPassword()); // Never log passwords!
```

✅ **Do**:
```java
log.info("User created with email={}", user.getEmail());
log.debug("User details: id={}, role={}", user.getId(), user.getRole());
```

### 5. Use Transactions

❌ **Don't**:
```java
public UserResponse createUser(UserRequest request) {
    // No transaction management
    User user = new User();
    return convertToResponse(userRepository.save(user));
}
```

✅ **Do**:
```java
@Transactional
public UserResponse createUser(UserRequest request) {
    // Automatic transaction management
    User user = User.builder().build();
    return convertToResponse(userRepository.save(user));
}
```

### 6. Validate Early

❌ **Don't**:
```java
public void updateUser(Long id, UserRequest request) {
    User user = userRepository.findById(id); // May be null
    user.setName(request.getName()); // NPE!
}
```

✅ **Do**:
```java
public void updateUser(Long id, UserRequest request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new UserException("Not found", USER_NOT_FOUND));
    user.setName(request.getName());
}
```

---

## Extending the Module

### Adding a New Role

**Step 1**: Update enum
```java
public enum RolesEnum {
    VIEWER("VIEWER"),
    ANALYST("ANALYST"),
    ADMIN("ADMIN"),
    MANAGER("MANAGER");  // New role
    // ...
}
```

**Step 2**: Use in endpoints
```java
@PreAuthorize("hasRole('MANAGER')")
public ResponseEntity<Void> approveUser(@PathVariable Long id) {
    // ...
}
```

### Adding Audit Trail

**Step 1**: Extend User entity
```java
@Column(name = "created_by")
private String createdBy;

@Column(name = "updated_by")
private String updatedBy;
```

**Step 2**: Set in service
```java
String currentUser = SecurityContextHolder.getContext()
    .getAuthentication().getName();

user.setCreatedBy(currentUser);
user.setUpdatedBy(currentUser);
```

### Adding Soft Delete

**Step 1**: Add field to entity
```java
@Column(name = "is_deleted")
private boolean isDeleted = false;
```

**Step 2**: Update queries
```java
@Query("SELECT u FROM User u WHERE u.isDeleted = false")
List<User> findAllActive();
```

**Step 3**: Update delete method
```java
public void deleteUser(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.setIsDeleted(true);
    userRepository.save(user);
}
```

---

## Performance Optimization

### 1. Enable Query Caching
```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
```

### 2. Use Pagination for Lists
```java
public Page<UserResponse> getAllUsers(Pageable pageable) {
    return userRepository.findAll(pageable)
        .map(this::convertToResponse);
}
```

### 3. Add Database Indexes
```sql
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_status ON users(status);
CREATE INDEX idx_user_role ON users(role);
```

---

## Conclusion

This User Management module is designed for:
- **Maintainability**: Clean, well-structured code
- **Security**: Role-based access, password encryption
- **Scalability**: Ready for extensions and customizations
- **Reliability**: Comprehensive error handling
- **Observability**: Detailed logging

For questions or issues, refer to the implementation documentation or contact the team.

---

**Last Updated**: April 2, 2026
**Version**: 1.0.0
