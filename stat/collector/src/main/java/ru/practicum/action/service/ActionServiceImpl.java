package ru.practicum.action.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.action.mapper.UserActionMapper;
import ru.practicum.action.model.UserAction;
import ru.practicum.config.KafkaConfig;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionServiceImpl implements ActionService {
    private final Producer<String, SpecificRecordBase> producer;
    private final KafkaConfig kafkaConfig;

    @Override
    public void collectUserAction(UserAction userAction) {
        log.info("ActionService: call collectUserAction for UserAction = {}", userAction);
        send(kafkaConfig.getKafkaProperties().getUserActionTopic(),
                userAction.getEventId().toString(),
                userAction.getTimestamp().toEpochMilli(),
                UserActionMapper.toUserActionAvro(userAction));
    }

    private void send(String topic, String key, Long timestamp, SpecificRecordBase specificRecordBase) {
        ProducerRecord<String, SpecificRecordBase> rec = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                specificRecordBase);
        producer.send(rec, (metadata, exception) -> {
            if (exception != null) {
                log.error("Kafka: сообщение НЕ ОТПРАВЛЕНО, topic: {}", topic, exception);
            } else {
                log.info("Kafka: сообщение УСПЕШНО отправлено, topic: {}, partition: {}, offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @PreDestroy
    private void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }
}
