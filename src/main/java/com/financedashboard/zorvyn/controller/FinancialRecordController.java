package com.financedashboard.zorvyn.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.financedashboard.zorvyn.dto.ErrorResponse;
import com.financedashboard.zorvyn.dto.RecordRequest;
import com.financedashboard.zorvyn.dto.RecordResponse;
import com.financedashboard.zorvyn.enums.RecordTypeEnum;
import com.financedashboard.zorvyn.service.interfaces.FinancialRecordService;
import com.financedashboard.zorvyn.service.util.AuthenticationHelper;

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
 * REST controller for financial record CRUD — /v1/records
 *
 * Role-Based Access Control (Principle of Least Privilege):
 * VIEWER: No access to raw records (dashboard only)
 * ANALYST: GET own records only (read-only, row-level security)
 * ADMIN: Full CRUD on all records
 *
 * GET /v1/records supports pagination:
 * page — 0-based page index (default: 0)
 * size — records per page, max 100 (default: 20)
 * sortBy — field name to sort by (default: transactionDate)
 * sortDir — asc or desc (default: desc)
 */
@Slf4j
@RestController
@RequestMapping("/v1/records")
@RequiredArgsConstructor
@Tag(name = "Financial Records", description = "CRUD operations for income/expense records with pagination and filtering")
public class FinancialRecordController {

        private final FinancialRecordService financialRecordService;
        private final AuthenticationHelper authenticationHelper;

        @Operation(summary = "Create a financial record", description = "Creates a new income or expense record. ADMIN only. "
                        + "Ownership is set server-side from the JWT — the client cannot supply a userId. "
                        + "Amount must be positive, transaction date cannot be in the future.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Record created", content = @Content(schema = @Schema(implementation = RecordResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Caller is not ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<RecordResponse> createRecord(
                        @Valid @RequestBody RecordRequest request,
                        Authentication authentication) {

                String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
                log.info("POST /v1/records — user={}", userEmail);

                RecordResponse response = financialRecordService.createRecord(request, userEmail);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @Operation(summary = "List financial records (paginated)", description = "Returns a paginated, filtered list of records. "
                        + "ADMIN sees all records; ANALYST sees only their own. VIEWER cannot access this endpoint. "
                        + "Soft-deleted records are excluded. All filter parameters are optional and combinable.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Records retrieved"),
                        @ApiResponse(responseCode = "403", description = "Caller is VIEWER", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping
        @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
        public ResponseEntity<Page<RecordResponse>> getAllRecords(
                        @Parameter(description = "Filter by exact category match", example = "Food") @RequestParam(required = false) String category,

                        @Parameter(description = "Filter by record type", example = "EXPENSE") @RequestParam(required = false) RecordTypeEnum type,

                        @Parameter(description = "Start date inclusive (YYYY-MM-DD)", example = "2024-01-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

                        @Parameter(description = "End date inclusive (YYYY-MM-DD)", example = "2024-12-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

                        @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,

                        @Parameter(description = "Page size (max 100)", example = "20") @RequestParam(defaultValue = "20") int size,

                        @Parameter(description = "Field to sort by", example = "transactionDate") @RequestParam(defaultValue = "transactionDate") String sortBy,

                        @Parameter(description = "Sort direction: asc or desc", example = "desc") @RequestParam(defaultValue = "desc") String sortDir,

                        Authentication authentication) {

                String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);

                // Cap page size at 100 to prevent accidental full-table fetches
                int effectiveSize = Math.min(size, 100);
                Sort sort = sortDir.equalsIgnoreCase("asc")
                                ? Sort.by(sortBy).ascending()
                                : Sort.by(sortBy).descending();
                PageRequest pageable = PageRequest.of(page, effectiveSize, sort);

                log.debug(
                                "GET /v1/records — user={}, page={}, size={}, sortBy={} {}, filters: category={}, type={}, from={}, to={}",
                                userEmail, page, effectiveSize, sortBy, sortDir, category, type, from, to);

                Page<RecordResponse> result = financialRecordService.getAllRecords(
                                userEmail, category, type, from, to, pageable);

                return ResponseEntity.ok(result);
        }

        @Operation(summary = "Get a record by ID", description = "Returns a single non-deleted record. "
                        + "ANALYST can only view their own records (row-level security). ADMIN can view any record. VIEWER cannot access.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Record found", content = @Content(schema = @Schema(implementation = RecordResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Caller is VIEWER, or ANALYST accessing another user's record", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Record not found or soft-deleted", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
        public ResponseEntity<RecordResponse> getRecordById(
                        @Parameter(description = "Record ID", example = "1") @PathVariable Long id,
                        Authentication authentication) {

                String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
                log.debug("GET /v1/records/{} — user={}", id, userEmail);

                RecordResponse response = financialRecordService.getRecordById(id, userEmail);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Update a financial record", description = "Replaces all fields of a record. ADMIN only.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Record updated", content = @Content(schema = @Schema(implementation = RecordResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Caller is not ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Record not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PatchMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<RecordResponse> updateRecord(
                        @Parameter(description = "Record ID", example = "1") @PathVariable Long id,
                        @Valid @RequestBody RecordRequest request,
                        Authentication authentication) {

                String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
                log.info("PATCH /v1/records/{} — user={}", id, userEmail);

                RecordResponse response = financialRecordService.updateRecord(id, request, userEmail);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Soft-delete a financial record", description = "Sets deleted=true on the record. The record is never physically removed, "
                        + "preserving the financial audit trail. ADMIN only.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Record soft-deleted"),
                        @ApiResponse(responseCode = "403", description = "Caller is not ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Record not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Void> deleteRecord(
                        @Parameter(description = "Record ID", example = "1") @PathVariable Long id,
                        Authentication authentication) {

                String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
                log.info("DELETE /v1/records/{} — user={}", id, userEmail);

                financialRecordService.softDeleteRecord(id, userEmail);
                return ResponseEntity.noContent().build();
        }
}
