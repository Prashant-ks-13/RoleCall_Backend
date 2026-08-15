package com.rolecall.job.service;

import com.rolecall.job.dto.CreateJobRequest;
import com.rolecall.job.dto.JobResponse;
import com.rolecall.job.dto.PageResponse;
import com.rolecall.job.dto.UpdateJobRequest;
import com.rolecall.job.entity.JobArrangement;
import com.rolecall.job.entity.JobType;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface JobService {

    JobResponse createJob(UUID employerId, CreateJobRequest request);

    JobResponse getJob(UUID id);

    JobResponse updateJob(UUID id, UUID requesterId, boolean requesterIsAdmin, UpdateJobRequest request);

    void closeJob(UUID id, UUID requesterId, boolean requesterIsAdmin);

    PageResponse<JobResponse> searchJobs(String search, JobType type, JobArrangement arrangement,
                                          String experienceLevel, Pageable pageable);

    PageResponse<JobResponse> getJobsForEmployer(UUID employerId, Pageable pageable);

    void incrementApplicantCount(UUID jobId);

    void markFeatured(UUID jobId, Instant featuredUntil);

    void revertFeatured(UUID jobId);
}
