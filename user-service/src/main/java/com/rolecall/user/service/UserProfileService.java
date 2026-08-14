package com.rolecall.user.service;

import com.rolecall.common.event.UserRegisteredEvent;
import com.rolecall.user.dto.PublicUserProfileResponse;
import com.rolecall.user.dto.UpdateProfileRequest;
import com.rolecall.user.dto.UserProfileResponse;

import java.util.UUID;

public interface UserProfileService {

    void createProfileShell(UserRegisteredEvent event);

    UserProfileResponse getMyProfile(UUID userId);

    UserProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request);

    UserProfileResponse updateMyResume(UUID userId, String resumeUrl);

    PublicUserProfileResponse getPublicProfile(UUID userId);
}
