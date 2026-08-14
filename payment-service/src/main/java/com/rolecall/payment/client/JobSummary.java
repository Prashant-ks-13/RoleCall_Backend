package com.rolecall.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Only the fields payment-service needs from job-service's full JobResponse. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobSummary(UUID id, UUID employerId, String status) {
}
