package com.rolecall.application.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Only the fields application-service needs from user-service's full UserProfileResponse. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CandidateProfile(UUID id, String resumeUrl) {
}
