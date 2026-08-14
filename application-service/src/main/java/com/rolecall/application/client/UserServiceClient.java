package com.rolecall.application.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Deliberately calls /me rather than /{id}: this is invoked on behalf of
     * the candidate applying, using their own forwarded JWT (see
     * FeignAuthForwardingInterceptor), and /{id} only returns a public-safe
     * subset that excludes the resume URL.
     */
    @GetMapping("/api/users/me")
    CandidateProfile getMyProfile();
}
