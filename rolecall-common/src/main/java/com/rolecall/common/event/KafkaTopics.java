package com.rolecall.common.event;

/**
 * Registry of Kafka topic names shared by producers and consumers.
 * See /docs/kafka-topics.md for the full topic contract (payload, producer, consumers).
 */
public final class KafkaTopics {

    public static final String USER_REGISTERED = "rolecall.user.registered";
    public static final String APPLICATION_SUBMITTED = "rolecall.application.submitted";
    public static final String APPLICATION_STATUS_CHANGED = "rolecall.application.status-changed";
    public static final String PAYMENT_COMPLETED = "rolecall.payment.completed";
    public static final String PAYMENT_FAILED = "rolecall.payment.failed";

    private KafkaTopics() {
    }
}
