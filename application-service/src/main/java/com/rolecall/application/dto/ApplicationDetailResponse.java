package com.rolecall.application.dto;

import java.util.List;

public record ApplicationDetailResponse(
        ApplicationResponse application,
        List<StatusHistoryResponse> history
) {
}
