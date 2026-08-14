package com.rolecall.application.dto;

import com.rolecall.application.entity.ApplicationStatus;
import com.rolecall.application.entity.JobApplication;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID jobId,
        UUID candidateId,
        ApplicationStatus status,
        String resumeSnapshotUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(JobApplication application) {
        return new ApplicationResponse(
                application.getId(), application.getJobId(), application.getCandidateId(),
                application.getStatus(), application.getResumeSnapshotUrl(),
                application.getCreatedAt(), application.getUpdatedAt());
    }
}
