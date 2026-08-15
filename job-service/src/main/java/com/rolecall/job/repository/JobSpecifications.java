package com.rolecall.job.repository;

import com.rolecall.job.entity.Job;
import com.rolecall.job.entity.JobArrangement;
import com.rolecall.job.entity.JobStatus;
import com.rolecall.job.entity.JobType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class JobSpecifications {

    private JobSpecifications() {
    }

    public static Specification<Job> statusIn(List<JobStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Job> searchTextMatches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("company")), like));
    }

    public static Specification<Job> hasType(JobType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Job> hasArrangement(JobArrangement arrangement) {
        if (arrangement == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("arrangement"), arrangement);
    }

    public static Specification<Job> hasExperienceLevel(String experienceLevel) {
        if (experienceLevel == null || experienceLevel.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("experienceLevel"), experienceLevel);
    }
}
