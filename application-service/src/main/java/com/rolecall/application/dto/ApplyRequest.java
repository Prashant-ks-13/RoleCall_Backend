package com.rolecall.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplyRequest(
        @NotNull UUID jobId
) {
}
