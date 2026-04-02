package com.financedashboard.zorvyn.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financedashboard.zorvyn.dto.RecordRequest;
import com.financedashboard.zorvyn.dto.RecordResponse;
import com.financedashboard.zorvyn.entity.FinancialRecord;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.enums.RecordTypeEnum;
import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.repository.interfaces.FinancialRecordRepository;
import com.financedashboard.zorvyn.service.interfaces.FinancialRecordService;
import com.financedashboard.zorvyn.service.util.UserResolutionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Full implementation of financial record CRUD with role-based access control.
 *
 * Data-level access rules (enforced here, not just at the endpoint level):
 * - ADMIN: can create/view/update/delete ANY record
 * - ANALYST: can create/view/update/delete only THEIR OWN records
 * - VIEWER: can view only their own records (write operations blocked at controller via @PreAuthorize)
 *
 * Authenticated user identity is always resolved from the JWT-extracted email,
 * never from client-supplied values.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FinancialRecordServiceImpl implements FinancialRecordService {

    private final FinancialRecordRepository financialRecordRepository;
    private final UserResolutionUtil userResolutionUtil;

    /**
     * Creates a record owned by the authenticated user.
     * The createdBy field is set server-side from the JWT — the client cannot forge ownership.
     */
    @Override
    public RecordResponse createRecord(RecordRequest request, String userEmail) {
        log.info("Creating financial record for user={}", userEmail);

        User owner = userResolutionUtil.getUserOrThrow(userEmail);

        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .transactionDate(request.getTransactionDate())
                .notes(request.getNotes())
                .createdBy(owner)
                .createdAt(LocalDateTime.now())
                .build();

        FinancialRecord saved = financialRecordRepository.save(record);
        log.info("Financial record created: id={}, user={}", saved.getId(), userEmail);
        return RecordResponse.fromEntity(saved);
    }

    /**
     * Returns all non-deleted records visible to the user.
     * ADMIN gets all records (userId filter = null); others get only their own.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RecordResponse> getAllRecords(
            String userEmail,
            String category,
            RecordTypeEnum type,
            LocalDate from,
            LocalDate to) {

        log.debug("Fetching records for user={}, category={}, type={}, from={}, to={}",
                userEmail, category, type, from, to);

        User user = userResolutionUtil.getUserOrThrow(userEmail);
        Long userIdFilter = userResolutionUtil.resolveUserIdFilter(user); // null for ADMIN

        List<FinancialRecord> records = financialRecordRepository
                .findAllByFilters(userIdFilter, category, type, from, to);

        log.debug("Found {} records for user={}", records.size(), userEmail);
        return records.stream().map(RecordResponse::fromEntity).toList();
    }

    /**
     * Fetches a single record by ID.
     * Applies ownership check: non-ADMIN users can only see their own records.
     */
    @Override
    @Transactional(readOnly = true)
    public RecordResponse getRecordById(Long id, String userEmail) {
        log.debug("Fetching record id={} for user={}", id, userEmail);

        FinancialRecord record = findActiveRecordOrThrow(id);
        User requester = userResolutionUtil.getUserOrThrow(userEmail);

        enforceOwnership(record, requester, "view");

        return RecordResponse.fromEntity(record);
    }

    /**
     * Updates fields of an existing record.
     * Ownership check runs before any modification — non-ADMIN cannot update others' records.
     */
    @Override
    public RecordResponse updateRecord(Long id, RecordRequest request, String userEmail) {
        log.info("Updating record id={} for user={}", id, userEmail);

        FinancialRecord record = findActiveRecordOrThrow(id);
        User requester = userResolutionUtil.getUserOrThrow(userEmail);

        enforceOwnership(record, requester, "update");

        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setTransactionDate(request.getTransactionDate());
        record.setNotes(request.getNotes());
        record.setUpdatedAt(LocalDateTime.now());

        FinancialRecord updated = financialRecordRepository.save(record);
        log.info("Record id={} updated successfully by user={}", id, userEmail);
        return RecordResponse.fromEntity(updated);
    }

    /**
     * Soft-deletes a record by setting deleted=true.
     * The record remains in the database for audit purposes but is excluded from all queries.
     * Ownership check prevents non-ADMIN users from deleting others' records.
     */
    @Override
    public void softDeleteRecord(Long id, String userEmail) {
        log.info("Soft-deleting record id={} for user={}", id, userEmail);

        FinancialRecord record = findActiveRecordOrThrow(id);
        User requester = userResolutionUtil.getUserOrThrow(userEmail);

        enforceOwnership(record, requester, "delete");

        record.setDeleted(true);
        record.setUpdatedAt(LocalDateTime.now());
        financialRecordRepository.save(record);
        log.info("Record id={} soft-deleted by user={}", id, userEmail);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /** Fetches a non-deleted record or throws 404. */
    private FinancialRecord findActiveRecordOrThrow(Long id) {
        return financialRecordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Record not found or deleted: id={}", id);
                    return new FinancialDashboardException(
                            ErrorCodeEnum.FINANCIAL_RECORD_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.FINANCIAL_RECORD_NOT_FOUND.getErrorMessage() + ": id=" + id,
                            ErrorCodeEnum.FINANCIAL_RECORD_NOT_FOUND.getHttpStatus()
                    );
                });
    }

    /**
     * Enforces data-level ownership.
     * ADMIN bypasses ownership checks and can act on any record.
     * ANALYST/VIEWER can only act on records they created.
     */
    private void enforceOwnership(FinancialRecord record, User requester, String action) {
        if (requester.getRole() == RolesEnum.ADMIN) {
            return; // ADMIN has full access to all records
        }
        if (!record.getCreatedBy().getId().equals(requester.getId())) {
            log.warn("Ownership check failed: user={} attempted to {} record id={}",
                    requester.getEmail(), action, record.getId());
            throw new FinancialDashboardException(
                    ErrorCodeEnum.UNAUTHORIZED_ACCESS.getErrorCode(),
                    "You do not have permission to " + action + " this record.",
                    ErrorCodeEnum.UNAUTHORIZED_ACCESS.getHttpStatus()
            );
        }
    }
}
