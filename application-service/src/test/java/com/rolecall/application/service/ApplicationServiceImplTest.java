package com.rolecall.application.service;

import com.rolecall.application.client.CandidateProfile;
import com.rolecall.application.client.JobServiceClient;
import com.rolecall.application.client.JobSummary;
import com.rolecall.application.client.UserServiceClient;
import com.rolecall.application.dto.ApplicationDetailResponse;
import com.rolecall.application.dto.ApplicationResponse;
import com.rolecall.application.dto.ApplyRequest;
import com.rolecall.application.dto.UpdateStatusRequest;
import com.rolecall.application.entity.ApplicationStatus;
import com.rolecall.application.entity.JobApplication;
import com.rolecall.application.event.ApplicationEventProducer;
import com.rolecall.application.exception.DuplicateApplicationException;
import com.rolecall.application.exception.JobNotAvailableException;
import com.rolecall.application.exception.NotAuthorizedForApplicationException;
import com.rolecall.application.repository.ApplicationStatusHistoryRepository;
import com.rolecall.application.repository.JobApplicationRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private ApplicationStatusHistoryRepository historyRepository;
    @Mock
    private JobServiceClient jobServiceClient;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private ApplicationEventProducer eventProducer;

    private ApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApplicationServiceImpl(
                applicationRepository, historyRepository, jobServiceClient, userServiceClient, eventProducer);
    }

    @Test
    void applyRejectsInactiveJobs() {
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(jobServiceClient.getJob(jobId)).thenReturn(new JobSummary(jobId, UUID.randomUUID(), "CLOSED"));

        assertThatThrownBy(() -> service.apply(candidateId, new ApplyRequest(jobId)))
                .isInstanceOf(JobNotAvailableException.class);
    }

    @Test
    void applyRejectsAJobThatDoesNotExistUpstream() {
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        Request request = Request.create(Request.HttpMethod.GET, "/api/jobs/" + jobId,
                java.util.Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(jobServiceClient.getJob(jobId)).thenThrow(new FeignException.NotFound("not found", request, null, null));

        assertThatThrownBy(() -> service.apply(candidateId, new ApplyRequest(jobId)))
                .isInstanceOf(JobNotAvailableException.class);
    }

    @Test
    void applyRejectsDuplicateApplications() {
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(jobServiceClient.getJob(jobId)).thenReturn(new JobSummary(jobId, UUID.randomUUID(), "ACTIVE"));
        when(applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(candidateId, new ApplyRequest(jobId)))
                .isInstanceOf(DuplicateApplicationException.class);
    }

    @Test
    void applySucceedsAndSnapshotsResumeAndPublishesEvent() {
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(jobServiceClient.getJob(jobId)).thenReturn(new JobSummary(jobId, UUID.randomUUID(), "FEATURED"));
        when(applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)).thenReturn(false);
        when(userServiceClient.getMyProfile()).thenReturn(new CandidateProfile(candidateId, "https://resumes/mine.pdf"));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = service.apply(candidateId, new ApplyRequest(jobId));

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.resumeSnapshotUrl()).isEqualTo("https://resumes/mine.pdf");
        org.mockito.Mockito.verify(eventProducer).publishSubmitted(any(JobApplication.class));
        org.mockito.Mockito.verify(historyRepository).save(any());
    }

    @Test
    void updateStatusRejectsNonOwnerNonAdmin() {
        UUID jobId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        JobApplication application = JobApplication.builder()
                .id(UUID.randomUUID()).jobId(jobId).candidateId(UUID.randomUUID()).status(ApplicationStatus.APPLIED).build();
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(jobServiceClient.getJob(jobId)).thenReturn(new JobSummary(jobId, ownerId, "ACTIVE"));

        UpdateStatusRequest request = new UpdateStatusRequest(ApplicationStatus.UNDER_REVIEW, null);

        assertThatThrownBy(() -> service.updateStatus(application.getId(), otherUser, false, request))
                .isInstanceOf(NotAuthorizedForApplicationException.class);
    }

    @Test
    void updateStatusRecordsHistoryAndPublishesEvent() {
        UUID jobId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        JobApplication application = JobApplication.builder()
                .id(UUID.randomUUID()).jobId(jobId).candidateId(UUID.randomUUID()).status(ApplicationStatus.APPLIED).build();
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(jobServiceClient.getJob(jobId)).thenReturn(new JobSummary(jobId, ownerId, "ACTIVE"));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.findByApplicationIdOrderByChangedAtAsc(application.getId())).thenReturn(List.of());

        UpdateStatusRequest request = new UpdateStatusRequest(ApplicationStatus.INTERVIEW, "Great candidate");
        ApplicationDetailResponse response = service.updateStatus(application.getId(), ownerId, false, request);

        assertThat(response.application().status()).isEqualTo(ApplicationStatus.INTERVIEW);
        org.mockito.Mockito.verify(historyRepository).save(any());
        org.mockito.Mockito.verify(eventProducer).publishStatusChanged(any(), org.mockito.Mockito.eq(ApplicationStatus.APPLIED), org.mockito.Mockito.eq(ownerId));
    }
}
