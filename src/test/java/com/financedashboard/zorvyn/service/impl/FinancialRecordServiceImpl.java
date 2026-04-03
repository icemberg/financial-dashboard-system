package com.financedashboard.zorvyn.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.financedashboard.zorvyn.dto.RecordRequest;
import com.financedashboard.zorvyn.dto.RecordResponse;
import com.financedashboard.zorvyn.entity.FinancialRecord;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.RecordTypeEnum;
import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.repository.interfaces.FinancialRecordRepository;
import com.financedashboard.zorvyn.service.util.UserResolutionUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class FinancialRecordServiceImplTest {

    @Mock
    private FinancialRecordRepository repository;

    @Mock
    private UserResolutionUtil userResolutionUtil;

    @InjectMocks
    private FinancialRecordServiceImpl service;

    private User analyst;
    private User admin;
    private FinancialRecord record;

    @BeforeEach
    void setup() {
        analyst = User.builder()
                .id(1L)
                .email("analyst@test.com")
                .role(RolesEnum.ANALYST)
                .build();

        admin = User.builder()
                .id(2L)
                .email("admin@test.com")
                .role(RolesEnum.ADMIN)
                .build();

        record = FinancialRecord.builder()
                .id(100L)
                .amount(BigDecimal.valueOf(500.0))
                .type(RecordTypeEnum.EXPENSE)
                .category("Food")
                .transactionDate(LocalDate.now())
                .createdBy(analyst)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ================= CREATE =================

    @Test
    void createRecord_success() {
        RecordRequest request = RecordRequest.builder()
                .amount(BigDecimal.valueOf(500.0))
                .type(RecordTypeEnum.EXPENSE)
                .category("Food")
                .transactionDate(LocalDate.now())
                .notes("Lunch")
                .build();

        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);
        when(repository.save(any())).thenReturn(record);

        RecordResponse response = service.createRecord(request, analyst.getEmail());

        assertNotNull(response);
        verify(repository).save(any());
    }

    @Test
    void createRecord_rethrowBusinessException() {
        when(userResolutionUtil.getUserOrThrow(anyString()))
                .thenThrow(new FinancialDashboardException("ERR", "msg", null));

        assertThrows(FinancialDashboardException.class,
                () -> service.createRecord(new RecordRequest(), "x"));
    }

    @Test
    void createRecord_wrapGenericException() {
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);
        when(repository.save(any())).thenThrow(RuntimeException.class);

        assertThrows(FinancialDashboardException.class,
                () -> service.createRecord(new RecordRequest(), analyst.getEmail()));
    }

    // ================= GET ALL =================

    @Test
    void getAllRecords_analystFlow() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<FinancialRecord> page = new PageImpl<>(java.util.List.of(record));

        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);
        when(userResolutionUtil.resolveUserIdFilter(analyst)).thenReturn(1L);
        when(repository.findAllByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<RecordResponse> result = service.getAllRecords(
                analyst.getEmail(), null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllRecords_adminFlow() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(admin);
        when(userResolutionUtil.resolveUserIdFilter(admin)).thenReturn(null);
        when(repository.findAllByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Page<RecordResponse> result = service.getAllRecords(
                admin.getEmail(), null, null, null, null, pageable);

        assertNotNull(result);
    }

    @Test
    void getAllRecords_wrapException() {
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);
        when(userResolutionUtil.resolveUserIdFilter(any())).thenReturn(1L);
        when(repository.findAllByFilters(any(), any(), any(), any(), any(), any()))
                .thenThrow(RuntimeException.class);

        assertThrows(FinancialDashboardException.class,
                () -> service.getAllRecords(
                        analyst.getEmail(), null, null, null, null, PageRequest.of(0, 10)));
    }

    // ================= GET BY ID =================

    @Test
    void getRecordById_success_owner() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);

        assertNotNull(service.getRecordById(100L, analyst.getEmail()));
    }

    @Test
    void getRecordById_adminBypass() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(admin);

        assertNotNull(service.getRecordById(100L, admin.getEmail()));
    }

    @Test
    void getRecordById_notOwner() {
        User other = User.builder().id(99L).role(RolesEnum.ANALYST).build();

        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(other);

        assertThrows(FinancialDashboardException.class,
                () -> service.getRecordById(100L, "x"));
    }

    @Test
    void getRecordById_notFound() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        assertThrows(FinancialDashboardException.class,
                () -> service.getRecordById(100L, "x"));
    }

    // ================= UPDATE =================

    @Test
    void updateRecord_success() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);
        when(repository.save(any())).thenReturn(record);

        RecordRequest request = RecordRequest.builder()
                .amount(BigDecimal.valueOf(999.0))
                .type(RecordTypeEnum.INCOME)
                .category("Salary")
                .transactionDate(LocalDate.now())
                .build();

        assertNotNull(service.updateRecord(100L, request, analyst.getEmail()));
    }

    @Test
    void updateRecord_notOwner() {
        User other = User.builder().id(2L).role(RolesEnum.ANALYST).build();

        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(other);

        assertThrows(FinancialDashboardException.class,
                () -> service.updateRecord(100L, new RecordRequest(), "x"));
    }

    @Test
    void updateRecord_notFound() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        assertThrows(FinancialDashboardException.class,
                () -> service.updateRecord(100L, new RecordRequest(), "x"));
    }

    @Test
    void updateRecord_wrapException() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);
        when(repository.save(any())).thenThrow(RuntimeException.class);

        assertThrows(FinancialDashboardException.class,
                () -> service.updateRecord(100L, new RecordRequest(), analyst.getEmail()));
    }

    // ================= DELETE =================

    @Test
    void delete_success() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);

        service.softDeleteRecord(100L, analyst.getEmail());

        assertTrue(record.isDeleted());
        verify(repository).save(record);
    }

    @Test
    void delete_notOwner() {
        User other = User.builder().id(2L).role(RolesEnum.ANALYST).build();

        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(other);

        assertThrows(FinancialDashboardException.class,
                () -> service.softDeleteRecord(100L, "x"));
    }

    @Test
    void delete_notFound() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        assertThrows(FinancialDashboardException.class,
                () -> service.softDeleteRecord(100L, "x"));
    }

    @Test
    void delete_wrapException() {
        when(repository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(record));
        when(userResolutionUtil.getUserOrThrow(anyString())).thenReturn(analyst);
        when(repository.save(any())).thenThrow(RuntimeException.class);

        assertThrows(FinancialDashboardException.class,
                () -> service.softDeleteRecord(100L, analyst.getEmail()));
    }
}