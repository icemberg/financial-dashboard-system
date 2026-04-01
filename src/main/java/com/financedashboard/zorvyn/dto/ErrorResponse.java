package com.financedashboard.zorvyn.dto;

import java.time.LocalDateTime;

import com.financedashboard.zorvyn.enums.ErrorCodeEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private ErrorCodeEnum code;
}
