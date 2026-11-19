package org.conrad.maintenanceservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates the event topic with several partitions. Within a consumer group, each partition is read by
 * one instance, so a topic with one partition would keep every resident-service instance but one idle.
 * Messages are keyed by request id, so all events for one request stay in order on one partition.
 * The producer owns the topic, which is why it is declared here.
 */
@Configuration
class KafkaTopicConfig {

    @Bean
    NewTopic maintenanceCompletedTopic(
            @Value("${app.kafka.topics.maintenance-completed:maintenance.completed}") String name,
            @Value("${app.kafka.topics.partitions:3}") int partitions) {
        return TopicBuilder.name(name).partitions(partitions).replicas(1).build();
    }
}
