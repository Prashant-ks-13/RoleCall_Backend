package com.rolecall.application.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Only the fields application-service needs from job-service's full JobResponse. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobSummary(UUID id, UUID employerId, String status) {
}
