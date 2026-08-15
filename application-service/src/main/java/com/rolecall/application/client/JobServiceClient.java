package com.rolecall.application.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "job-service")
public interface JobServiceClient {

    @GetMapping("/api/jobs/{id}")
    JobSummary getJob(@PathVariable("id") UUID id);
}
