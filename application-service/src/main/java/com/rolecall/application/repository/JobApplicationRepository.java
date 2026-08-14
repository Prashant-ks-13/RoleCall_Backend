package com.rolecall.application.repository;

import com.rolecall.application.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    Page<JobApplication> findByCandidateId(UUID candidateId, Pageable pageable);

    Page<JobApplication> findByJobId(UUID jobId, Pageable pageable);

    Optional<JobApplication> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);
}
