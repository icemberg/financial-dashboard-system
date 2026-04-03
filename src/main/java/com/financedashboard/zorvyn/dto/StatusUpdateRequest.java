package com.financedashboard.zorvyn.dto;

import com.financedashboard.zorvyn.enums.UserStatusEnum;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal DTO for PATCH /v1/users/{id}/status.
 *
 * Fixes Bug #2 — previously the endpoint required a full UserRequest body
 * (name, email, password, role, status) when it only ever used the status field.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "Status is required (ACTIVE or INACTIVE)")
    private UserStatusEnum status;
}
