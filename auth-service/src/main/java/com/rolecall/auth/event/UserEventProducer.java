package com.rolecall.auth.event;

import com.rolecall.auth.entity.User;
import com.rolecall.common.event.KafkaTopics;
import com.rolecall.common.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getRole().name(), user.getCreatedAt());
        kafkaTemplate.send(KafkaTopics.USER_REGISTERED, user.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserRegisteredEvent for user {}", user.getId(), ex);
                    }
                });
    }
}
