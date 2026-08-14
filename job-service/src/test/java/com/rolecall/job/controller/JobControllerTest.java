package com.rolecall.job.controller;

import com.rolecall.job.dto.JobResponse;
import com.rolecall.job.dto.PageResponse;
import com.rolecall.job.entity.JobArrangement;
import com.rolecall.job.entity.JobStatus;
import com.rolecall.job.entity.JobType;
import com.rolecall.job.security.SecurityConfig;
import com.rolecall.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import(SecurityConfig.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void browsingJobsRequiresNoAuthentication() throws Exception {
        when(jobService.searchJobs(any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 9, 0, 0));

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void gettingASingleJobRequiresNoAuthentication() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobService.getJob(id)).thenReturn(sampleJob(id, UUID.randomUUID()));

        mockMvc.perform(get("/api/jobs/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void creatingAJobWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(post("/api/jobs").contentType(MediaType.APPLICATION_JSON).content(validCreateBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingAJobAsCandidateIsForbidden() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.claim("role", "CANDIDATE"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingAJobAsEmployerSucceeds() throws Exception {
        UUID employerId = UUID.randomUUID();
        when(jobService.createJob(any(), any())).thenReturn(sampleJob(UUID.randomUUID(), employerId));

        mockMvc.perform(post("/api/jobs")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject(employerId.toString()).claim("role", "EMPLOYER"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated());
    }

    @Test
    void myJobsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/jobs/mine"))
                .andExpect(status().isUnauthorized());
    }

    private JobResponse sampleJob(UUID id, UUID employerId) {
        return new JobResponse(id, employerId, "Backend Engineer", "Description", "Acme", "Remote",
                JobType.FULL_TIME, JobArrangement.REMOTE, "2-4 years", 80000, 120000,
                JobStatus.ACTIVE, null, 0, Instant.now(), Instant.now());
    }

    private String validCreateBody() {
        return """
                {
                  "title": "Backend Engineer",
                  "description": "Build things",
                  "company": "Acme",
                  "location": "Remote",
                  "type": "FULL_TIME",
                  "arrangement": "REMOTE",
                  "experienceLevel": "2-4 years",
                  "salaryMin": 80000,
                  "salaryMax": 120000
                }
                """;
    }
}
