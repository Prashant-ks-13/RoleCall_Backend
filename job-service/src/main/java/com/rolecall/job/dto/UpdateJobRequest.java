package com.rolecall.job.dto;

import com.rolecall.job.entity.JobArrangement;
import com.rolecall.job.entity.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateJobRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 8000) String description,
        @NotBlank @Size(max = 150) String company,
        @NotBlank @Size(max = 150) String location,
        @NotNull JobType type,
        @NotNull JobArrangement arrangement,
        @NotBlank @Size(max = 50) String experienceLevel,
        @Positive Integer salaryMin,
        @Positive Integer salaryMax
) {
}
