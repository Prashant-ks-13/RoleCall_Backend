package com.rolecall.application.dto;

import com.rolecall.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
        @NotNull ApplicationStatus status,
        @Size(max = 1000) String note
) {
}
