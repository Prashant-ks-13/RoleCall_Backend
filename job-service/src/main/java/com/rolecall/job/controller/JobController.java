package com.rolecall.job.controller;

import com.rolecall.job.dto.CreateJobRequest;
import com.rolecall.job.dto.JobResponse;
import com.rolecall.job.dto.PageResponse;
import com.rolecall.job.dto.UpdateJobRequest;
import com.rolecall.job.entity.JobArrangement;
import com.rolecall.job.entity.JobType;
import com.rolecall.job.service.JobService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Job posting CRUD and search")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Create a job posting")
    public ResponseEntity<JobResponse> createJob(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreateJobRequest request) {
        JobResponse created = jobService.createJob(userId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Browse/search active job postings")
    public PageResponse<JobResponse> searchJobs(@RequestParam(required = false) String search,
                                                 @RequestParam(required = false) JobType type,
                                                 @RequestParam(required = false) JobArrangement arrangement,
                                                 @RequestParam(required = false) String experienceLevel,
                                                 @PageableDefault(size = 9) Pageable pageable) {
        return jobService.searchJobs(search, type, arrangement, experienceLevel, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "List the authenticated employer's own job postings")
    public PageResponse<JobResponse> getMyJobs(@AuthenticationPrincipal Jwt jwt,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return jobService.getJobsForEmployer(userId(jwt), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single job posting")
    public JobResponse getJob(@PathVariable UUID id) {
        return jobService.getJob(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job posting (owner or admin only)")
    public JobResponse updateJob(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody UpdateJobRequest request) {
        return jobService.updateJob(id, userId(jwt), isAdmin(jwt), request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Close a job posting (soft delete; owner or admin only)")
    public ResponseEntity<Void> closeJob(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        jobService.closeJob(id, userId(jwt), isAdmin(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }
}
