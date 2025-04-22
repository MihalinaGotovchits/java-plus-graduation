package ru.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aggregator.kafka")
public class KafkaConfigProperties {
    private String bootstrapServers;

    private String producerClientIdConfig;
    private String producerKeySerializer;
    private String producerValueSerializer;

    private String consumerGroupId;
    private String consumerClientIdConfig;
    private String consumerKeyDeserializer;
    private String consumerValueDeserializer;
    private long consumerAttemptTimeout;
    private String consumerEnableAutoCommit;

    private String userActionTopic;
    private String eventsSimilarityTopic;
}