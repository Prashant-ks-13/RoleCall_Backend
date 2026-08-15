package com.rolecall.user.dto;

import com.rolecall.user.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * File upload/object storage is out of scope for this service; the client is
 * expected to upload the resume to blob storage itself (e.g. via a
 * presigned URL) and hand this service the resulting URL to persist.
 */
public record UpdateResumeRequest(
        @NotBlank @Size(max = 500) @HttpUrl String resumeUrl
) {
}
