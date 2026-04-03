package com.financedashboard.zorvyn.service.interfaces;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.financedashboard.zorvyn.dto.RecordRequest;
import com.financedashboard.zorvyn.dto.RecordResponse;
import com.financedashboard.zorvyn.enums.RecordTypeEnum;

/**
 * Contract for financial record CRUD operations.
 * The authenticated user's email is passed explicitly — never trust a client-supplied userId.
 * Role-based data filtering (ADMIN sees all, others see own) is enforced at service layer.
 */
public interface FinancialRecordService {

    /** Creates a new financial record owned by the authenticated user. ANALYST and ADMIN only. */
    RecordResponse createRecord(RecordRequest request, String userEmail);

    /**
     * Retrieves a paginated list of non-deleted records visible to the authenticated user.
     * ADMIN sees all records; ANALYST/VIEWER see only their own.
     * Supports optional filters: category, type, date range.
     *
     * @param pageable page number, page size, and sort order from the request
     */
    Page<RecordResponse> getAllRecords(
            String userEmail,
            String category,
            RecordTypeEnum type,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    /** Retrieves a single record by ID. ADMIN can fetch any; ANALYST/VIEWER can only fetch their own. */
    RecordResponse getRecordById(Long id, String userEmail);

    /** Updates an existing record. ADMIN can update any; ANALYST can only update their own. */
    RecordResponse updateRecord(Long id, RecordRequest request, String userEmail);

    /** Soft-deletes a record (sets deleted=true). ADMIN can delete any; ANALYST can only delete their own. */
    void softDeleteRecord(Long id, String userEmail);
}
