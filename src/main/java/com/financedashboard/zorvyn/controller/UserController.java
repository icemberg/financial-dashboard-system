package com.financedashboard.zorvyn.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financedashboard.zorvyn.dto.ErrorResponse;
import com.financedashboard.zorvyn.dto.StatusUpdateRequest;
import com.financedashboard.zorvyn.dto.UserRequest;
import com.financedashboard.zorvyn.dto.UserResponse;
import com.financedashboard.zorvyn.dto.UserUpdateRequest;
import com.financedashboard.zorvyn.service.interfaces.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for User Management — /v1/users
 *
 * Access control per endpoint:
 *   GET    /v1/users           → ADMIN only
 *   GET    /v1/users/{id}      → ADMIN or own profile
 *   POST   /v1/users           → ADMIN only
 *   PATCH  /v1/users/{id}      → ADMIN or own profile
 *   PATCH  /v1/users/{id}/status → ADMIN only
 *   DELETE /v1/users/{id}      → ADMIN only
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "CRUD operations for user accounts. Most endpoints require ADMIN role.")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "List all users",
            description = "Returns all users in the system. Password is never included in the response."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Caller is not ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /v1/users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(
            summary = "Get user by ID",
            description = "Returns a single user. ADMIN can fetch any user; non-ADMIN users can only fetch their own profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not ADMIN and not own profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID", example = "1") @PathVariable Long id) {
        log.info("GET /v1/users/{}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(
            summary = "Create a new user (ADMIN only)",
            description = "Creates a user with any role (VIEWER, ANALYST, ADMIN) and status. "
                    + "This is the only way to create ANALYST or ADMIN accounts. "
                    + "Self-registration via /v1/auth/register always assigns VIEWER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or password too weak",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        log.info("POST /v1/users — email={}", userRequest.getEmail());
        UserResponse created = userService.createUser(userRequest);
        log.info("User created: id={}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Partially update a user",
            description = "Updates only the non-null, non-blank fields provided in the request. "
                    + "Password is NOT updated here — use PATCH /v1/auth/change-password. "
                    + "ADMIN can update any user; non-ADMIN can only update their own profile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not ADMIN and not own profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "New email already taken",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.userId")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest updateRequest) {
        log.info("PATCH /v1/users/{}", id);
        UserResponse updated = userService.updateUser(id, updateRequest);
        log.info("User updated: id={}", id);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Update user status (ADMIN only)",
            description = "Changes the user's status to ACTIVE or INACTIVE. Inactive users cannot log in."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @Parameter(description = "User ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        log.info("PATCH /v1/users/{}/status → {}", id, request.getStatus());
        UserResponse updated = userService.updateUserStatus(id, request.getStatus());
        log.info("User status updated: id={}", id);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete a user (ADMIN only)",
            description = "Permanently removes a user from the database. This is a hard delete and cannot be undone."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", example = "1") @PathVariable Long id) {
        log.info("DELETE /v1/users/{}", id);
        userService.deleteUser(id);
        log.info("User deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
