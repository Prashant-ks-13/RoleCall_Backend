package com.rolecall.user.event;

import com.rolecall.common.event.KafkaTopics;
import com.rolecall.common.event.UserRegisteredEvent;
import com.rolecall.user.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserRegisteredEventConsumer {

    private final UserProfileService userProfileService;

    public UserRegisteredEventConsumer(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "user-service")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Creating profile shell for newly registered user {}", event.userId());
        userProfileService.createProfileShell(event);
    }
}
