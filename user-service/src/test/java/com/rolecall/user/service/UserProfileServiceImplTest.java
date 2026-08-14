package com.rolecall.user.service;

import com.rolecall.common.event.UserRegisteredEvent;
import com.rolecall.user.dto.UpdateProfileRequest;
import com.rolecall.user.dto.UserProfileResponse;
import com.rolecall.user.entity.UserProfile;
import com.rolecall.user.exception.ProfileNotFoundException;
import com.rolecall.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository repository;

    private UserProfileServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(repository);
    }

    @Test
    void createProfileShellPersistsANewProfileFromTheEvent() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "new@example.com", "CANDIDATE", Instant.now());
        when(repository.existsById(userId)).thenReturn(false);

        service.createProfileShell(event);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(userId);
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getRole()).isEqualTo("CANDIDATE");
    }

    @Test
    void createProfileShellIsIdempotentForDuplicateEventDelivery() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "new@example.com", "CANDIDATE", Instant.now());
        when(repository.existsById(userId)).thenReturn(true);

        service.createProfileShell(event);

        verify(repository, never()).save(any());
    }

    @Test
    void getMyProfileThrowsWhenProfileShellHasNotArrivedYet() {
        UUID userId = UUID.randomUUID();
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile(userId))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void updateMyProfileAppliesAllEditableFields() {
        UUID userId = UUID.randomUUID();
        UserProfile existing = UserProfile.builder()
                .id(userId).email("user@example.com").role("CANDIDATE").build();
        when(repository.findById(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane Doe", "Senior Engineer", "Bio text", "555-1234", "Remote", null, null);
        UserProfileResponse response = service.updateMyProfile(userId, request);

        assertThat(response.fullName()).isEqualTo("Jane Doe");
        assertThat(response.headline()).isEqualTo("Senior Engineer");
        assertThat(response.location()).isEqualTo("Remote");
    }
}
