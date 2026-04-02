package com.financedashboard.zorvyn.controller;

import java.time.LocalDate;
import java.util.List;

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

import com.financedashboard.zorvyn.dto.RecordRequest;
import com.financedashboard.zorvyn.dto.RecordResponse;
import com.financedashboard.zorvyn.enums.RecordTypeEnum;
import com.financedashboard.zorvyn.service.interfaces.FinancialRecordService;
import com.financedashboard.zorvyn.service.util.AuthenticationHelper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for financial record CRUD operations.
 *
 * Access control:
 *  - VIEWER:  GET only (own records — enforced at service/data level)
 *  - ANALYST: GET + POST + PATCH + DELETE (own records)
 *  - ADMIN:   Full access to all records
 *
 * The authenticated user's email is extracted from the JWT via Authentication object.
 * The client never supplies a userId — ownership is always resolved server-side.
 *
 * Base path: /v1/records
 */
@Slf4j
@RestController
@RequestMapping("/v1/records")
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService financialRecordService;
    private final AuthenticationHelper authenticationHelper;

    /**
     * POST /v1/records
     * Creates a new financial record owned by the authenticated user.
     * Access: ANALYST, ADMIN
     */
    @PostMapping
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<RecordResponse> createRecord(
            @Valid @RequestBody RecordRequest request,
            Authentication authentication) {

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        log.info("POST /v1/records — user={}", userEmail);

        RecordResponse response = financialRecordService.createRecord(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /v1/records
     * Returns records visible to the authenticated user.
     * ADMIN sees all; ANALYST/VIEWER see only their own.
     * Supports optional filters: category, type, from (date), to (date).
     * Access: All authenticated roles
     */
    @GetMapping
    public ResponseEntity<List<RecordResponse>> getAllRecords(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) RecordTypeEnum type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        log.debug("GET /v1/records — user={}, category={}, type={}, from={}, to={}",
                userEmail, category, type, from, to);

        List<RecordResponse> records = financialRecordService.getAllRecords(userEmail, category, type, from, to);
        return ResponseEntity.ok(records);
    }

    /**
     * GET /v1/records/{id}
     * Returns a single record.
     * Access: All authenticated roles (data-level ownership enforced in service)
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecordResponse> getRecordById(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        log.debug("GET /v1/records/{} — user={}", id, userEmail);

        RecordResponse response = financialRecordService.getRecordById(id, userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /v1/records/{id}
     * Updates a financial record. Full update (all fields required).
     * Access: ANALYST (own), ADMIN (any)
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<RecordResponse> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody RecordRequest request,
            Authentication authentication) {

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        log.info("PATCH /v1/records/{} — user={}", id, userEmail);

        RecordResponse response = financialRecordService.updateRecord(id, request, userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /v1/records/{id}
     * Soft-deletes a record (sets deleted=true, record is never purged from DB).
     * Access: ANALYST (own), ADMIN (any)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        log.info("DELETE /v1/records/{} — user={}", id, userEmail);

        financialRecordService.softDeleteRecord(id, userEmail);
        return ResponseEntity.noContent().build();
    }
}
