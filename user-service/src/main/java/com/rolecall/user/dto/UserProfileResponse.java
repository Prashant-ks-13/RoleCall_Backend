package com.rolecall.user.dto;

import com.rolecall.user.entity.UserProfile;

import java.util.UUID;

/** Full self-view, returned only from /api/users/me. */
public record UserProfileResponse(
        UUID id,
        String email,
        String role,
        String fullName,
        String headline,
        String bio,
        String phone,
        String location,
        String resumeUrl,
        String avatarUrl,
        String companyName,
        String companyWebsite
) {
    public static UserProfileResponse from(UserProfile p) {
        return new UserProfileResponse(
                p.getId(), p.getEmail(), p.getRole(), p.getFullName(), p.getHeadline(), p.getBio(),
                p.getPhone(), p.getLocation(), p.getResumeUrl(), p.getAvatarUrl(),
                p.getCompanyName(), p.getCompanyWebsite());
    }
}
