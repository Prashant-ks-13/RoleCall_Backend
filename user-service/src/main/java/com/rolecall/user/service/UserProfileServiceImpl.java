package com.rolecall.user.service;

import com.rolecall.common.event.UserRegisteredEvent;
import com.rolecall.user.dto.PublicUserProfileResponse;
import com.rolecall.user.dto.UpdateProfileRequest;
import com.rolecall.user.dto.UserProfileResponse;
import com.rolecall.user.entity.UserProfile;
import com.rolecall.user.exception.ProfileNotFoundException;
import com.rolecall.user.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileServiceImpl(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void createProfileShell(UserRegisteredEvent event) {
        if (repository.existsById(event.userId())) {
            log.debug("Profile shell for user {} already exists, skipping (duplicate delivery)", event.userId());
            return;
        }
        UserProfile profile = UserProfile.builder()
                .id(event.userId())
                .email(event.email())
                .role(event.role())
                .build();
        repository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UUID userId) {
        return UserProfileResponse.from(findOrThrow(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = findOrThrow(userId);
        profile.setFullName(request.fullName());
        profile.setHeadline(request.headline());
        profile.setBio(request.bio());
        profile.setPhone(request.phone());
        profile.setLocation(request.location());
        profile.setCompanyName(request.companyName());
        profile.setCompanyWebsite(request.companyWebsite());
        return UserProfileResponse.from(repository.save(profile));
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyResume(UUID userId, String resumeUrl) {
        UserProfile profile = findOrThrow(userId);
        profile.setResumeUrl(resumeUrl);
        return UserProfileResponse.from(repository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicUserProfileResponse getPublicProfile(UUID userId) {
        return PublicUserProfileResponse.from(findOrThrow(userId));
    }

    private UserProfile findOrThrow(UUID userId) {
        return repository.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}
