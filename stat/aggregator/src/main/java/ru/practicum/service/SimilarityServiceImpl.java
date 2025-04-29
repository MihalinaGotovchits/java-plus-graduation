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

        log.debug("Received weight: {} for event: {}, user: {}", newWeight, eventId, userId);

        // Инициализация структур данных
        eventUserWeights.putIfAbsent(eventId, new HashMap<>());
        double currentWeight = eventUserWeights.get(eventId).getOrDefault(userId, 0.0);
        log.debug("Current weight: {} for event: {}, user: {}", currentWeight, eventId, userId);

        // Если новый вес не больше текущего - пропускаем обработку
        if (newWeight <= currentWeight) {
            log.debug("Weight not increased, skipping processing");
            return results;
        }

        // Обновляем максимальный вес пользователя для мероприятия
        eventUserWeights.get(eventId).put(userId, newWeight);
        log.debug("Updated user weight to: {} for event: {}, user: {}", newWeight, eventId, userId);

        // Обновляем общий вес мероприятия (S_i)
        double deltaWeight = newWeight - currentWeight;
        double newTotalWeight = eventTotalWeights.merge(eventId, deltaWeight, Double::sum);
        log.debug("Updated total weight for event {}: {}", eventId, newTotalWeight);

        // Пересчитываем схожесть с другими мероприятиями
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            Long otherEventId = entry.getKey();

            if (otherEventId.equals(eventId)) {
                log.debug("Skipping same event: {}", eventId);
                continue;
            }

            if (entry.getValue().containsKey(userId)) {
                double otherWeight = entry.getValue().get(userId);
                log.debug("Found interaction with event: {}, weight: {}", otherEventId, otherWeight);

                // Упорядочиваем ID мероприятий
                long firstEvent = Math.min(eventId, otherEventId);
                long secondEvent = Math.max(eventId, otherEventId);
                log.debug("Processing pair: {} and {}", firstEvent, secondEvent);

                // Вычисляем изменение минимальных весов
                double oldMin = Math.min(currentWeight, otherWeight);
                double newMin = Math.min(newWeight, otherWeight);
                double deltaMin = newMin - oldMin;
                log.debug("Min weights - old: {}, new: {}, delta: {}", oldMin, newMin, deltaMin);

                // Обновляем сумму минимальных весов для пары
                Map<Long, Double> secondLevelMap = pairMinWeights.computeIfAbsent(firstEvent, k -> new HashMap<>());
                double currentSum = secondLevelMap.getOrDefault(secondEvent, 0.0);
                double updatedSum = currentSum + deltaMin;
                secondLevelMap.put(secondEvent, updatedSum);

                log.debug("Updated min weights sum for pair ({}, {}): was {}, now {}",
                        firstEvent, secondEvent, currentSum, updatedSum);

                // Вычисляем новый коэффициент схожести
                double sumA = eventTotalWeights.get(firstEvent);
                double sumB = eventTotalWeights.get(secondEvent);
                log.debug("Total weights - sumA: {}, sumB: {}", sumA, sumB);

                double score = calculateCosineSimilarity(sumA, sumB, updatedSum);
                log.info("Calculated similarity score for events {} and {}: {}",
                        firstEvent, secondEvent, score);

                if (score > 0) {
                    EventSimilarityAvro similarity = createSimilarityAvro(firstEvent, secondEvent, score);
                    results.add(similarity);
                    log.debug("Created similarity record: {}", similarity);
                }
            }
        }

        return results;
    }

    private double calculateCosineSimilarity(double sumA, double sumB, double sumMin) {
        if (sumA <= 0 || sumB <= 0 || sumMin <= 0) {
            log.debug("Invalid input for similarity calculation - sumA: {}, sumB: {}, sumMin: {}",
                    sumA, sumB, sumMin);
            return 0;
        }

        double sqrtA = Math.sqrt(sumA);
        double sqrtB = Math.sqrt(sumB);
        double denominator = sqrtA * sqrtB;

        if (denominator == 0) {
            log.debug("Denominator is zero - sumA: {}, sumB: {}", sumA, sumB);
            return 0;
        }

        double score = sumMin / denominator;
        double roundedScore = Math.round(score * 100000.0) / 100000.0;

        log.debug("Similarity calculation details:");
        log.debug("  sumMin: {}", sumMin);
        log.debug("  sqrt(sumA): {}", sqrtA);
        log.debug("  sqrt(sumB): {}", sqrtB);
        log.debug("  denominator: {}", denominator);
        log.debug("  raw score: {}", score);
        log.debug("  rounded score: {}", roundedScore);

        return roundedScore;
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