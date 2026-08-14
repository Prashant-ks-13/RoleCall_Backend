package com.rolecall.user.controller;

import com.rolecall.user.dto.UserProfileResponse;
import com.rolecall.user.exception.ProfileNotFoundException;
import com.rolecall.user.security.SecurityConfig;
import com.rolecall.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import(SecurityConfig.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    // Required only so the resource-server filter chain has a bean to wire up;
    // SecurityMockMvcRequestPostProcessors.jwt() injects a pre-built Authentication
    // directly, so decode() is never actually invoked by these tests.
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void getMyProfileRejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyProfileReturnsTheAuthenticatedUsersOwnProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = new UserProfileResponse(
                userId, "user@example.com", "CANDIDATE", "Jane Doe", null, null, null, null, null, null, null, null);
        when(userProfileService.getMyProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"));
    }

    @Test
    void getMyProfileReturnsNotFoundWhenProfileShellHasNotArrivedYet() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userProfileService.getMyProfile(userId)).thenThrow(new ProfileNotFoundException(userId));

        mockMvc.perform(get("/api/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void getPublicProfileIsReachableForAnyAuthenticatedUser() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(userProfileService.getPublicProfile(any())).thenReturn(
                new com.rolecall.user.dto.PublicUserProfileResponse(
                        targetId, "EMPLOYER", "Acme Recruiter", null, null, null, null, "Acme Inc", null));

        mockMvc.perform(get("/api/users/" + targetId)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Inc"));
    }
}
