package com.rolecall.application.dto;

import com.rolecall.application.entity.ApplicationStatus;
import com.rolecall.application.entity.ApplicationStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
        UUID id,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        UUID changedBy,
        Instant changedAt,
        String note
) {
    public static StatusHistoryResponse from(ApplicationStatusHistory history) {
        return new StatusHistoryResponse(
                history.getId(), history.getFromStatus(), history.getToStatus(),
                history.getChangedBy(), history.getChangedAt(), history.getNote());
    }
}
