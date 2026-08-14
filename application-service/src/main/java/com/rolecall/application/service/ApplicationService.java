package com.rolecall.application.service;

import com.rolecall.application.dto.ApplicationDetailResponse;
import com.rolecall.application.dto.ApplicationResponse;
import com.rolecall.application.dto.ApplyRequest;
import com.rolecall.application.dto.PageResponse;
import com.rolecall.application.dto.UpdateStatusRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse apply(UUID candidateId, ApplyRequest request);

    PageResponse<ApplicationResponse> getMine(UUID candidateId, Pageable pageable);

    PageResponse<ApplicationResponse> getForJob(UUID jobId, UUID requesterId, boolean requesterIsAdmin, Pageable pageable);

    ApplicationDetailResponse updateStatus(UUID applicationId, UUID requesterId, boolean requesterIsAdmin,
                                            UpdateStatusRequest request);
}
