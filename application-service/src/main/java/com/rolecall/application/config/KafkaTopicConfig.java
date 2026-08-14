package com.rolecall.application.config;

import com.rolecall.common.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic applicationSubmittedTopic() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_SUBMITTED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic applicationStatusChangedTopic() {
        return TopicBuilder.name(KafkaTopics.APPLICATION_STATUS_CHANGED).partitions(3).replicas(1).build();
    }
}
