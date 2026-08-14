package com.rolecall.user.dto;

import com.rolecall.user.entity.UserProfile;

import java.util.UUID;

/** Public-safe subset returned from GET /api/users/{id} — no phone number. */
public record PublicUserProfileResponse(
        UUID id,
        String role,
        String fullName,
        String headline,
        String bio,
        String location,
        String avatarUrl,
        String companyName,
        String companyWebsite
) {
    public static PublicUserProfileResponse from(UserProfile p) {
        return new PublicUserProfileResponse(
                p.getId(), p.getRole(), p.getFullName(), p.getHeadline(), p.getBio(),
                p.getLocation(), p.getAvatarUrl(), p.getCompanyName(), p.getCompanyWebsite());
    }
}
