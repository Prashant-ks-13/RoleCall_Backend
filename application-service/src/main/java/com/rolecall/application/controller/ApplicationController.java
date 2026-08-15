package com.rolecall.application.controller;

import com.rolecall.application.dto.ApplicationDetailResponse;
import com.rolecall.application.dto.ApplicationResponse;
import com.rolecall.application.dto.ApplyRequest;
import com.rolecall.application.dto.PageResponse;
import com.rolecall.application.dto.UpdateStatusRequest;
import com.rolecall.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Job applications and status tracking")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Apply to a job")
    public ResponseEntity<ApplicationResponse> apply(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody ApplyRequest request) {
        ApplicationResponse response = applicationService.apply(userId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    @Operation(summary = "List the authenticated candidate's own applications")
    public PageResponse<ApplicationResponse> getMine(@AuthenticationPrincipal Jwt jwt,
                                                       @PageableDefault(size = 20) Pageable pageable) {
        return applicationService.getMine(userId(jwt), pageable);
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "List applicants for a job (job owner or admin only)")
    public PageResponse<ApplicationResponse> getForJob(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable UUID jobId,
                                                         @PageableDefault(size = 20) Pageable pageable) {
        return applicationService.getForJob(jobId, userId(jwt), isAdmin(jwt), pageable);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update an application's status (job owner or admin only)")
    public ApplicationDetailResponse updateStatus(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody UpdateStatusRequest request) {
        return applicationService.updateStatus(id, userId(jwt), isAdmin(jwt), request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }
}
