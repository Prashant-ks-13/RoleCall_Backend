package com.rolecall.job.dto;

import com.rolecall.job.entity.Job;
import com.rolecall.job.entity.JobArrangement;
import com.rolecall.job.entity.JobStatus;
import com.rolecall.job.entity.JobType;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID employerId,
        String title,
        String description,
        String company,
        String location,
        JobType type,
        JobArrangement arrangement,
        String experienceLevel,
        Integer salaryMin,
        Integer salaryMax,
        JobStatus status,
        Instant featuredUntil,
        int applicantCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(), job.getEmployerId(), job.getTitle(), job.getDescription(), job.getCompany(),
                job.getLocation(), job.getType(), job.getArrangement(), job.getExperienceLevel(),
                job.getSalaryMin(), job.getSalaryMax(), job.getStatus(), job.getFeaturedUntil(),
                job.getApplicantCount(), job.getCreatedAt(), job.getUpdatedAt());
    }
}
