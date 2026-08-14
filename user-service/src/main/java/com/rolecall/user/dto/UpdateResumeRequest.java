package com.rolecall.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * File upload/object storage is out of scope for this service; the client is
 * expected to upload the resume to blob storage itself (e.g. via a
 * presigned URL) and hand this service the resulting URL to persist.
 */
public record UpdateResumeRequest(
        @NotBlank String resumeUrl
) {
}
