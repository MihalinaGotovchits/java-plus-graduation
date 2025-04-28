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

    private final Map<Long, Map<Long, Double>> eventWeights = new HashMap<>();
    private final Map<Long, Double> eventSummaryWeights = new HashMap<>();
    private final Map<Long, Map<Long, Double>> eventMinSummaryWeights = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> updateSimilarity(UserActionAvro userAction) {
        log.info("updateSimilarity for userAction = {}", userAction);
        List<EventSimilarityAvro> result = new ArrayList<>();

        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();
        double receivedWeight = getWeightByActionType(userAction.getActionType());
        double oldWeight = addOrUpdateEventWeightForUser(eventId, userId, receivedWeight);
        double newWeight = Math.max(oldWeight, receivedWeight);

        if (oldWeight != newWeight) {
            eventSummaryWeights.put(eventId, eventSummaryWeights.getOrDefault(eventId, 0.0) + newWeight - oldWeight);
            reCalcEventMinSummaryWeights(eventId, userId);

            for (Long secondEvent : eventWeights.keySet()) {
                if (!eventId.equals(secondEvent)) {
                    long eventA = Math.min(eventId, secondEvent);
                    long eventB = Math.max(eventId, secondEvent);
                    double sumWeightA = eventSummaryWeights.getOrDefault(eventA, 0.0);
                    double sumWeightB = eventSummaryWeights.getOrDefault(eventB, 0.0);

                    double score = 0.0;
                    if (sumWeightA > 0 && sumWeightB > 0) {
                        score = getEventMinSummaryWeights(eventA, eventB) /
                                (Math.sqrt(sumWeightA) * Math.sqrt(sumWeightB));
                        score = Math.round(score * 100.0) / 100.0;
                    }

                    if (score > 0) {
                        EventSimilarityAvro eventSimilarity = EventSimilarityAvro.newBuilder()
                                .setEventA(eventA)
                                .setEventB(eventB)
                                .setScore(score)
                                .setTimestamp(Instant.now())
                                .build();
                        result.add(eventSimilarity);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void collectEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        ProducerRecord<Long, SpecificRecordBase> rec = new ProducerRecord<>(
                kafkaConfig.getKafkaProperties().getEventsSimilarityTopic(),
                null,
                eventSimilarityAvro.getTimestamp().toEpochMilli(),
                eventSimilarityAvro.getEventA(),
                eventSimilarityAvro);
        producer.send(rec);
    }

    @Override
    public void close() {
        SimilarityService.super.close();
        if (producer != null) {
            producer.close();
        }
    }

    private void putEventMinSummaryWeights(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        eventMinSummaryWeights
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    private double getEventMinSummaryWeights(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return eventMinSummaryWeights
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    private double getWeightByActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    private double addOrUpdateEventWeightForUser(Long eventId, Long userId, double weight) {
        double oldWeight = 0.0;
        eventWeights.computeIfAbsent(eventId, e -> new HashMap<>()).putIfAbsent(userId, 0.0);
        if (eventWeights.containsKey(eventId)) {
            oldWeight = eventWeights.get(eventId).get(userId);
            double maxWeight = Math.max(oldWeight, weight);
            if (oldWeight != maxWeight) {
                eventWeights.get(eventId).put(userId, maxWeight);
                log.info("eventWeights updated for eventId = {}: new weight = {}", eventId, maxWeight);
            }
        }
        return oldWeight;
    }

    private void reCalcEventMinSummaryWeights(Long eventId, Long userId) {
        double weightForThisEvent = eventWeights.get(eventId).getOrDefault(userId, 0.0);
        for (Long otherEvent : eventWeights.keySet()) {
            if (!Objects.equals(otherEvent, eventId)) {
                double weightForOtherEvent = eventWeights.get(otherEvent).getOrDefault(userId, 0.0);
                double newMinWeight = Math.min(weightForThisEvent, weightForOtherEvent);
                double oldMinWeight = getEventMinSummaryWeights(eventId, otherEvent);
                putEventMinSummaryWeights(eventId, otherEvent, newMinWeight - oldMinWeight);
            }
        }
    }
}
