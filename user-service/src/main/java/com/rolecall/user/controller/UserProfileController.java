package com.rolecall.user.controller;

import com.rolecall.user.dto.PublicUserProfileResponse;
import com.rolecall.user.dto.UpdateProfileRequest;
import com.rolecall.user.dto.UpdateResumeRequest;
import com.rolecall.user.dto.UserProfileResponse;
import com.rolecall.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile management")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's own profile")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getMyProfile(currentUserId(jwt));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's own profile")
    public UserProfileResponse updateMyProfile(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateMyProfile(currentUserId(jwt), request);
    }

    @PostMapping("/me/resume")
    @Operation(summary = "Attach a resume URL to the authenticated user's profile")
    public UserProfileResponse updateMyResume(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody UpdateResumeRequest request) {
        return userProfileService.updateMyResume(currentUserId(jwt), request.resumeUrl());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get another user's public-safe profile subset")
    public PublicUserProfileResponse getPublicProfile(@PathVariable UUID id) {
        return userProfileService.getPublicProfile(id);
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
