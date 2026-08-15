package com.rolecall.application.service;

import com.rolecall.application.client.CandidateProfile;
import com.rolecall.application.client.JobServiceClient;
import com.rolecall.application.client.JobSummary;
import com.rolecall.application.client.UserServiceClient;
import com.rolecall.application.dto.ApplicationDetailResponse;
import com.rolecall.application.dto.ApplicationResponse;
import com.rolecall.application.dto.ApplyRequest;
import com.rolecall.application.dto.PageResponse;
import com.rolecall.application.dto.StatusHistoryResponse;
import com.rolecall.application.dto.UpdateStatusRequest;
import com.rolecall.application.entity.ApplicationStatus;
import com.rolecall.application.entity.ApplicationStatusHistory;
import com.rolecall.application.entity.JobApplication;
import com.rolecall.application.event.ApplicationEventProducer;
import com.rolecall.application.exception.ApplicationNotFoundException;
import com.rolecall.application.exception.DuplicateApplicationException;
import com.rolecall.application.exception.JobNotAvailableException;
import com.rolecall.application.exception.NotAuthorizedForApplicationException;
import com.rolecall.application.repository.ApplicationStatusHistoryRepository;
import com.rolecall.application.repository.JobApplicationRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private static final Set<String> AVAILABLE_JOB_STATUSES = Set.of("ACTIVE", "FEATURED");

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final JobServiceClient jobServiceClient;
    private final UserServiceClient userServiceClient;
    private final ApplicationEventProducer eventProducer;

    public ApplicationServiceImpl(JobApplicationRepository applicationRepository,
                                   ApplicationStatusHistoryRepository historyRepository,
                                   JobServiceClient jobServiceClient,
                                   UserServiceClient userServiceClient,
                                   ApplicationEventProducer eventProducer) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.jobServiceClient = jobServiceClient;
        this.userServiceClient = userServiceClient;
        this.eventProducer = eventProducer;
    }

    @Override
    @Transactional
    public ApplicationResponse apply(UUID candidateId, ApplyRequest request) {
        JobSummary job = fetchJob(request.jobId());
        if (!AVAILABLE_JOB_STATUSES.contains(job.status())) {
            throw new JobNotAvailableException("This job is not currently accepting applications");
        }
        if (applicationRepository.existsByJobIdAndCandidateId(request.jobId(), candidateId)) {
            throw new DuplicateApplicationException();
        }

        String resumeUrl = fetchResumeUrl();

        JobApplication application = JobApplication.builder()
                .jobId(request.jobId())
                .candidateId(candidateId)
                .status(ApplicationStatus.APPLIED)
                .resumeSnapshotUrl(resumeUrl)
                .build();
        application = applicationRepository.save(application);

        historyRepository.save(ApplicationStatusHistory.builder()
                .applicationId(application.getId())
                .fromStatus(null)
                .toStatus(ApplicationStatus.APPLIED)
                .changedBy(candidateId)
                .build());

        eventProducer.publishSubmitted(application);

        return ApplicationResponse.from(application);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getMine(UUID candidateId, Pageable pageable) {
        Page<JobApplication> page = applicationRepository.findByCandidateId(candidateId, pageable);
        return PageResponse.from(page.map(ApplicationResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getForJob(UUID jobId, UUID requesterId, boolean requesterIsAdmin,
                                                         Pageable pageable) {
        assertOwnerOrAdmin(fetchJob(jobId), requesterId, requesterIsAdmin);
        Page<JobApplication> page = applicationRepository.findByJobId(jobId, pageable);
        return PageResponse.from(page.map(ApplicationResponse::from));
    }

    @Override
    @Transactional
    public ApplicationDetailResponse updateStatus(UUID applicationId, UUID requesterId, boolean requesterIsAdmin,
                                                   UpdateStatusRequest request) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        assertOwnerOrAdmin(fetchJob(application.getJobId()), requesterId, requesterIsAdmin);

        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(request.status());
        application = applicationRepository.save(application);

        historyRepository.save(ApplicationStatusHistory.builder()
                .applicationId(application.getId())
                .fromStatus(previousStatus)
                .toStatus(request.status())
                .changedBy(requesterId)
                .note(request.note())
                .build());

        eventProducer.publishStatusChanged(application, previousStatus, requesterId);

        List<StatusHistoryResponse> history = historyRepository.findByApplicationIdOrderByChangedAtAsc(applicationId)
                .stream().map(StatusHistoryResponse::from).toList();
        return new ApplicationDetailResponse(ApplicationResponse.from(application), history);
    }

    private JobSummary fetchJob(UUID jobId) {
        try {
            return jobServiceClient.getJob(jobId);
        } catch (FeignException.NotFound e) {
            throw new JobNotAvailableException("Job '%s' does not exist".formatted(jobId));
        }
    }

    private String fetchResumeUrl() {
        try {
            CandidateProfile profile = userServiceClient.getMyProfile();
            return profile.resumeUrl();
        } catch (FeignException e) {
            log.warn("Could not fetch candidate resume at time of application: {}", e.getMessage());
            return null;
        }
    }

    private void assertOwnerOrAdmin(JobSummary job, UUID requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !job.employerId().equals(requesterId)) {
            throw new NotAuthorizedForApplicationException();
        }
    }
}
