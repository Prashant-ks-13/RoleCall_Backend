package com.rolecall.job.service;

import com.rolecall.job.dto.CreateJobRequest;
import com.rolecall.job.dto.JobResponse;
import com.rolecall.job.dto.UpdateJobRequest;
import com.rolecall.job.entity.Job;
import com.rolecall.job.entity.JobArrangement;
import com.rolecall.job.entity.JobStatus;
import com.rolecall.job.entity.JobType;
import com.rolecall.job.exception.JobNotFoundException;
import com.rolecall.job.exception.NotJobOwnerException;
import com.rolecall.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    private JobServiceImpl jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobServiceImpl(jobRepository);
    }

    @Test
    void createJobDefaultsToActiveStatusWithZeroApplicants() {
        UUID employerId = UUID.randomUUID();
        CreateJobRequest request = new CreateJobRequest(
                "Backend Engineer", "Description", "Acme", "Remote",
                JobType.FULL_TIME, JobArrangement.REMOTE, "2-4 years", 80000, 120000);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = jobService.createJob(employerId, request);

        assertThat(response.status()).isEqualTo(JobStatus.ACTIVE);
        assertThat(response.applicantCount()).isZero();
        assertThat(response.employerId()).isEqualTo(employerId);
    }

    @Test
    void updateJobRejectsNonOwnerNonAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Job job = Job.builder().id(UUID.randomUUID()).employerId(ownerId).status(JobStatus.ACTIVE).build();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        UpdateJobRequest request = new UpdateJobRequest(
                "Title", "Desc", "Acme", "Remote", JobType.FULL_TIME, JobArrangement.REMOTE, "2-4 years", null, null);

        assertThatThrownBy(() -> jobService.updateJob(job.getId(), otherUserId, false, request))
                .isInstanceOf(NotJobOwnerException.class);
    }

    @Test
    void updateJobAllowsAdminRegardlessOfOwnership() {
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Job job = Job.builder().id(UUID.randomUUID()).employerId(ownerId).status(JobStatus.ACTIVE).build();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateJobRequest request = new UpdateJobRequest(
                "New Title", "Desc", "Acme", "Remote", JobType.FULL_TIME, JobArrangement.REMOTE, "2-4 years", null, null);
        JobResponse response = jobService.updateJob(job.getId(), adminId, true, request);

        assertThat(response.title()).isEqualTo("New Title");
    }

    @Test
    void getJobThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJob(id)).isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void incrementApplicantCountIncreasesByOne() {
        Job job = Job.builder().id(UUID.randomUUID()).applicantCount(2).build();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        jobService.incrementApplicantCount(job.getId());

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicantCount()).isEqualTo(3);
    }

    @Test
    void markFeaturedSetsStatusAndFeaturedUntil() {
        Job job = Job.builder().id(UUID.randomUUID()).status(JobStatus.ACTIVE).build();
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        var until = java.time.Instant.now().plusSeconds(3600);
        jobService.markFeatured(job.getId(), until);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FEATURED);
        assertThat(job.getFeaturedUntil()).isEqualTo(until);
    }

    @Test
    void revertFeaturedOnlyAppliesWhenCurrentlyFeatured() {
        Job activeJob = Job.builder().id(UUID.randomUUID()).status(JobStatus.ACTIVE).build();
        when(jobRepository.findById(activeJob.getId())).thenReturn(Optional.of(activeJob));

        jobService.revertFeatured(activeJob.getId());

        verify(jobRepository, org.mockito.Mockito.never()).save(any());
    }
}
