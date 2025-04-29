package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.config.KafkaConfig;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityServiceImpl implements SimilarityService {
    private final Producer<Long, SpecificRecordBase> producer;
    private final KafkaConfig kafkaConfig;

    // Хранит максимальные веса действий пользователей для каждого мероприятия
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();

    // Хранит суммарные веса для каждого мероприятия (S_i)
    private final Map<Long, Double> eventTotalWeights = new HashMap<>();

    // Хранит суммы минимальных весов для пар мероприятий (S_min(i,j))
    private final Map<Long, Map<Long, Double>> pairMinWeights = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> updateSimilarity(UserActionAvro userAction) {
        log.info("Processing action for user {} and event {}",
                userAction.getUserId(), userAction.getEventId());

        List<EventSimilarityAvro> results = new ArrayList<>();
        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();
        double newWeight = getWeightByActionType(userAction.getActionType());

        // Инициализация структур данных при первом обращении
        eventUserWeights.putIfAbsent(eventId, new HashMap<>());

        // Получаем текущий максимальный вес пользователя для этого мероприятия
        double currentWeight = eventUserWeights.get(eventId).getOrDefault(userId, 0.0);

        // Если новый вес не больше текущего - пропускаем обработку
        if (newWeight <= currentWeight) {
            return results;
        }

        // Обновляем максимальный вес пользователя для мероприятия
        eventUserWeights.get(eventId).put(userId, newWeight);

        // Обновляем общий вес мероприятия (S_i)
        double deltaWeight = newWeight - currentWeight;
        eventTotalWeights.merge(eventId, deltaWeight, Double::sum);

        // Пересчитываем схожесть с другими мероприятиями
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            Long otherEventId = entry.getKey();

            if (otherEventId.equals(eventId)) continue; // Пропускаем текущее мероприятие

            // Проверяем, взаимодействовал ли пользователь с другим мероприятием
            if (entry.getValue().containsKey(userId)) {
                double otherWeight = entry.getValue().get(userId);

                // Упорядочиваем ID мероприятий
                long firstEvent = Math.min(eventId, otherEventId);
                long secondEvent = Math.max(eventId, otherEventId);

                // Вычисляем изменение минимальных весов
                double oldMin = Math.min(currentWeight, otherWeight);
                double newMin = Math.min(newWeight, otherWeight);
                double deltaMin = newMin - oldMin;

                // Обновляем сумму минимальных весов для пары
                pairMinWeights.computeIfAbsent(firstEvent, k -> new HashMap<>())
                        .merge(secondEvent, deltaMin, Double::sum);

                // Вычисляем новый коэффициент схожести
                double sumMin = pairMinWeights.get(firstEvent).get(secondEvent);
                double sumA = eventTotalWeights.get(firstEvent);
                double sumB = eventTotalWeights.get(secondEvent);

                double score = calculateCosineSimilarity(sumA, sumB, sumMin);

                if (score > 0) {
                    results.add(createSimilarityAvro(firstEvent, secondEvent, score));
                }
            }
        }

        return results;
    }

    private double calculateCosineSimilarity(double sumA, double sumB, double sumMin) {
        if (sumA <= 0 || sumB <= 0 || sumMin <= 0) return 0;

        double denominator = Math.sqrt(sumA) * Math.sqrt(sumB);
        if (denominator == 0) return 0;

        double score = sumMin / denominator;
        return Math.round(score * 100000.0) / 100000.0; // Округление до 5 знаков
    }

    private EventSimilarityAvro createSimilarityAvro(long eventA, long eventB, double score) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore((float) score)
                .setTimestamp(Instant.now())
                .build();
    }

    private double getWeightByActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    @Override
    public void collectEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        try {
            ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                    kafkaConfig.getKafkaProperties().getEventsSimilarityTopic(),
                    eventSimilarityAvro.getEventA(),
                    eventSimilarityAvro);
            producer.send(record);
        } catch (Exception e) {
            log.error("Error sending to Kafka: {}", e.getMessage());
        }
    }

    public void resetState() {
        eventUserWeights.clear();
        eventTotalWeights.clear();
        pairMinWeights.clear();
    }
}