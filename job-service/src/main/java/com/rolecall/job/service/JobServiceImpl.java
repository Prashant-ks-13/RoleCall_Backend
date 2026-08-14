package com.rolecall.job.service;

import com.rolecall.job.dto.CreateJobRequest;
import com.rolecall.job.dto.JobResponse;
import com.rolecall.job.dto.PageResponse;
import com.rolecall.job.dto.UpdateJobRequest;
import com.rolecall.job.entity.Job;
import com.rolecall.job.entity.JobArrangement;
import com.rolecall.job.entity.JobStatus;
import com.rolecall.job.entity.JobType;
import com.rolecall.job.exception.JobNotFoundException;
import com.rolecall.job.exception.NotJobOwnerException;
import com.rolecall.job.repository.JobRepository;
import com.rolecall.job.repository.JobSpecifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class JobServiceImpl implements JobService {

    private static final List<JobStatus> PUBLICLY_VISIBLE_STATUSES = List.of(JobStatus.ACTIVE, JobStatus.FEATURED);

    private final JobRepository jobRepository;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    @Transactional
    public JobResponse createJob(UUID employerId, CreateJobRequest request) {
        Job job = Job.builder()
                .employerId(employerId)
                .title(request.title())
                .description(request.description())
                .company(request.company())
                .location(request.location())
                .type(request.type())
                .arrangement(request.arrangement())
                .experienceLevel(request.experienceLevel())
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .status(JobStatus.ACTIVE)
                .applicantCount(0)
                .build();
        return JobResponse.from(jobRepository.save(job));
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID id) {
        return JobResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional
    public JobResponse updateJob(UUID id, UUID requesterId, boolean requesterIsAdmin, UpdateJobRequest request) {
        Job job = findOrThrow(id);
        assertOwnerOrAdmin(job, requesterId, requesterIsAdmin);

        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setCompany(request.company());
        job.setLocation(request.location());
        job.setType(request.type());
        job.setArrangement(request.arrangement());
        job.setExperienceLevel(request.experienceLevel());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        return JobResponse.from(jobRepository.save(job));
    }

    @Override
    @Transactional
    public void closeJob(UUID id, UUID requesterId, boolean requesterIsAdmin) {
        Job job = findOrThrow(id);
        assertOwnerOrAdmin(job, requesterId, requesterIsAdmin);
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> searchJobs(String search, JobType type, JobArrangement arrangement,
                                                 String experienceLevel, Pageable pageable) {
        Specification<Job> spec = Specification.where(JobSpecifications.statusIn(PUBLICLY_VISIBLE_STATUSES))
                .and(JobSpecifications.searchTextMatches(search))
                .and(JobSpecifications.hasType(type))
                .and(JobSpecifications.hasArrangement(arrangement))
                .and(JobSpecifications.hasExperienceLevel(experienceLevel));

        Page<Job> page = jobRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(JobResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> getJobsForEmployer(UUID employerId, Pageable pageable) {
        Page<Job> page = jobRepository.findByEmployerId(employerId, pageable);
        return PageResponse.from(page.map(JobResponse::from));
    }

    @Override
    @Transactional
    public void incrementApplicantCount(UUID jobId) {
        jobRepository.findById(jobId).ifPresentOrElse(job -> {
            job.setApplicantCount(job.getApplicantCount() + 1);
            jobRepository.save(job);
        }, () -> log.warn("Received applicant-count increment for unknown job {}", jobId));
    }

    @Override
    @Transactional
    public void markFeatured(UUID jobId, Instant featuredUntil) {
        jobRepository.findById(jobId).ifPresentOrElse(job -> {
            job.setStatus(JobStatus.FEATURED);
            job.setFeaturedUntil(featuredUntil);
            jobRepository.save(job);
        }, () -> log.warn("Received payment-completed event for unknown job {}", jobId));
    }

    @Override
    @Transactional
    public void revertFeatured(UUID jobId) {
        jobRepository.findById(jobId).ifPresentOrElse(job -> {
            if (job.getStatus() == JobStatus.FEATURED) {
                job.setStatus(JobStatus.ACTIVE);
                job.setFeaturedUntil(null);
                jobRepository.save(job);
            }
        }, () -> log.warn("Received payment-failed event for unknown job {}", jobId));
    }

    private Job findOrThrow(UUID id) {
        return jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
    }

    private void assertOwnerOrAdmin(Job job, UUID requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !job.isOwnedBy(requesterId)) {
            throw new NotJobOwnerException();
        }
    }
}
