package com.financedashboard.zorvyn.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * Data-level access rules (enforced here — defence in depth beyond @PreAuthorize):
 * - ADMIN:   can create/view/update/delete ANY record
 * - ANALYST: can create/view/update/delete only THEIR OWN records
 * - VIEWER:  can view only their own records (write ops blocked at controller)
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FinancialRecordServiceImpl implements FinancialRecordService {

    private final FinancialRecordRepository financialRecordRepository;
    private final UserResolutionUtil userResolutionUtil;

    @Override
    public RecordResponse createRecord(RecordRequest request, String userEmail) {
        log.info("Creating financial record for user={}", userEmail);

        try {
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
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating financial record for user={}", userEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.FINANCIAL_RECORD_CREATION_FAILED.getErrorCode(),
                    ErrorCodeEnum.FINANCIAL_RECORD_CREATION_FAILED.getErrorMessage(),
                    ErrorCodeEnum.FINANCIAL_RECORD_CREATION_FAILED.getHttpStatus()
            );
        }
    }

    /**
     * Returns a paginated list of non-deleted records visible to the user.
     * ADMIN gets all records (userId filter = null); others get only their own.
     * Default sort: transactionDate DESC, createdAt DESC (applied via Pageable).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecordResponse> getAllRecords(
            String userEmail,
            String category,
            RecordTypeEnum type,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        log.debug("Fetching records for user={}, category={}, type={}, from={}, to={}, page={}",
                userEmail, category, type, from, to, pageable.getPageNumber());

        try {
            User user = userResolutionUtil.getUserOrThrow(userEmail);
            Long userIdFilter = userResolutionUtil.resolveUserIdFilter(user); // null for ADMIN

            Page<FinancialRecord> page = financialRecordRepository
                    .findAllByFilters(userIdFilter, category, type, from, to, pageable);

            log.debug("Found {} records (page {}/{}) for user={}",
                    page.getNumberOfElements(), page.getNumber(), page.getTotalPages(), userEmail);

            return page.map(RecordResponse::fromEntity);
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error fetching records for user={}", userEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.FINANCIAL_RECORD_FETCH_FAILED.getErrorCode(),
                    ErrorCodeEnum.FINANCIAL_RECORD_FETCH_FAILED.getErrorMessage(),
                    ErrorCodeEnum.FINANCIAL_RECORD_FETCH_FAILED.getHttpStatus()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RecordResponse getRecordById(Long id, String userEmail) {
        log.debug("Fetching record id={} for user={}", id, userEmail);

        try {
            FinancialRecord record = findActiveRecordOrThrow(id);
            User requester = userResolutionUtil.getUserOrThrow(userEmail);
            enforceOwnership(record, requester, "view");

            return RecordResponse.fromEntity(record);
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error fetching record id={} for user={}", id, userEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.FINANCIAL_RECORD_FETCH_FAILED.getErrorCode(),
                    ErrorCodeEnum.FINANCIAL_RECORD_FETCH_FAILED.getErrorMessage(),
                    ErrorCodeEnum.FINANCIAL_RECORD_FETCH_FAILED.getHttpStatus()
            );
        }
    }

    @Override
    public RecordResponse updateRecord(Long id, RecordRequest request, String userEmail) {
        log.info("Updating record id={} for user={}", id, userEmail);

        try {
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
            log.info("Record id={} updated by user={}", id, userEmail);
            return RecordResponse.fromEntity(updated);
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error updating record id={} for user={}", id, userEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.FINANCIAL_RECORD_UPDATE_FAILED.getErrorCode(),
                    ErrorCodeEnum.FINANCIAL_RECORD_UPDATE_FAILED.getErrorMessage(),
                    ErrorCodeEnum.FINANCIAL_RECORD_UPDATE_FAILED.getHttpStatus()
            );
        }
    }

    @Override
    public void softDeleteRecord(Long id, String userEmail) {
        log.info("Soft-deleting record id={} for user={}", id, userEmail);

        try {
            FinancialRecord record = findActiveRecordOrThrow(id);
            User requester = userResolutionUtil.getUserOrThrow(userEmail);
            enforceOwnership(record, requester, "delete");

            record.setDeleted(true);
            record.setUpdatedAt(LocalDateTime.now());
            financialRecordRepository.save(record);
            log.info("Record id={} soft-deleted by user={}", id, userEmail);
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error soft-deleting record id={} for user={}", id, userEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.FINANCIAL_RECORD_DELETION_FAILED.getErrorCode(),
                    ErrorCodeEnum.FINANCIAL_RECORD_DELETION_FAILED.getErrorMessage(),
                    ErrorCodeEnum.FINANCIAL_RECORD_DELETION_FAILED.getHttpStatus()
            );
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private FinancialRecord findActiveRecordOrThrow(Long id) {
        return financialRecordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> {
                    log.warn("Record not found or soft-deleted: id={}", id);
                    return new FinancialDashboardException(
                            ErrorCodeEnum.FINANCIAL_RECORD_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.FINANCIAL_RECORD_NOT_FOUND.getErrorMessage() + ": id=" + id,
                            ErrorCodeEnum.FINANCIAL_RECORD_NOT_FOUND.getHttpStatus()
                    );
                });
    }

    private void enforceOwnership(FinancialRecord record, User requester, String action) {
        if (requester.getRole() == RolesEnum.ADMIN) {
            return;
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
