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
    // Формат: eventId -> (userId -> maxWeight)
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();

    // Хранит суммарные веса для каждого мероприятия
    // Формат: eventId -> totalWeight (S_i)
    private final Map<Long, Double> eventTotalWeights = new HashMap<>();

    // Хранит суммы минимальных весов для пар мероприятий
    // Формат: firstEventId -> (secondEventId -> sumOfMinWeights), где firstEventId < secondEventId
    private final Map<Long, Map<Long, Double>> pairMinWeights = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> updateSimilarity(UserActionAvro userAction) {
        log.info("Обработка действия пользователя {} для мероприятия {}",
                userAction.getUserId(), userAction.getEventId());

        List<EventSimilarityAvro> results = new ArrayList<>();
        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();

        // Получаем вес действия в зависимости от типа
        double newWeight = getWeightByActionType(userAction.getActionType());

        // Инициализируем структуры данных при первом обращении
        eventUserWeights.putIfAbsent(eventId, new HashMap<>());

        // Получаем текущий вес пользователя для этого мероприятия
        double oldWeight = eventUserWeights.get(eventId).getOrDefault(userId, 0.0);

        // Если вес не изменился - пропускаем пересчет
        if (Math.abs(newWeight - oldWeight) < 0.00001) {
            return results;
        }

        // Обновляем вес пользователя для мероприятия
        eventUserWeights.get(eventId).put(userId, newWeight);

        // Обновляем общий вес для мероприятия
        double eventTotalWeight = eventTotalWeights.getOrDefault(eventId, 0.0) + (newWeight - oldWeight);
        eventTotalWeights.put(eventId, eventTotalWeight);

        // Пересчитываем схожесть с другими мероприятиями
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            Long otherEventId = entry.getKey();

            // Пропускаем текущее мероприятие
            if (otherEventId.equals(eventId)) continue;

            // Учитываем только мероприятия, с которыми пользователь взаимодействовал
            if (entry.getValue().containsKey(userId)) {
                double otherWeight = entry.getValue().get(userId);

                // Упорядочиваем ID мероприятий для избежания дублирования
                long firstEvent = Math.min(eventId, otherEventId);
                long secondEvent = Math.max(eventId, otherEventId);

                // Вычисляем изменение минимальных весов
                double oldMin = Math.min(oldWeight, otherWeight);
                double newMin = Math.min(newWeight, otherWeight);
                double delta = newMin - oldMin;

                // Обновляем сумму минимальных весов для пары мероприятий
                pairMinWeights.putIfAbsent(firstEvent, new HashMap<>());
                double currentSum = pairMinWeights.get(firstEvent).getOrDefault(secondEvent, 0.0);
                double updatedSum = currentSum + delta;
                pairMinWeights.get(firstEvent).put(secondEvent, updatedSum);

                // Вычисляем новый коэффициент схожести
                double otherTotalWeight = eventTotalWeights.get(otherEventId);
                double score = calculateCosineSimilarity(
                        eventTotalWeight,
                        otherTotalWeight,
                        updatedSum
                );

                // Добавляем результат, если коэффициент положительный
                if (score > 0) {
                    results.add(createSimilarityAvro(firstEvent, secondEvent, score));
                }
            }
        }

        return results;
    }

    /**
     * Вычисляет косинусное сходство между двумя мероприятиями
     * @param sumA Сумма весов для мероприятия A (S_i)
     * @param sumB Сумма весов для мероприятия B (S_j)
     * @param sumMin Сумма минимальных весов для пары (S_min(i,j))
     * @return Значение косинусного сходства, округленное до 5 знаков
     */
    private double calculateCosineSimilarity(double sumA, double sumB, double sumMin) {
        // Проверка на нулевые значения
        if (sumA <= 0 || sumB <= 0 || sumMin <= 0) return 0;

        // Вычисляем знаменатель - произведение корней из сумм весов
        double denominator = Math.sqrt(sumA) * Math.sqrt(sumB);
        if (denominator == 0) return 0;

        // Основная формула косинусного сходства
        double score = sumMin / denominator;

        // Округление до 5 знаков после запятой согласно требованиям
        return Math.round(score * 100000.0) / 100000.0;
    }

    /**
     * Создает объект EventSimilarityAvro для отправки в Kafka
     */
    private EventSimilarityAvro createSimilarityAvro(long eventA, long eventB, double score) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore((float) score)
                .setTimestamp(Instant.now())
                .build();
    }

    /**
     * Возвращает вес действия в зависимости от его типа
     */
    private double getWeightByActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;      // Просмотр
            case REGISTER -> 0.8;  // Регистрация
            case LIKE -> 1.0;      // Лайк
            default -> 0;          // Неизвестный тип
        };
    }

    @Override
    public void collectEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        try {
            // Создаем запись для отправки в Kafka
            ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                    kafkaConfig.getKafkaProperties().getEventsSimilarityTopic(),
                    eventSimilarityAvro.getEventA(),
                    eventSimilarityAvro
            );

            // Отправляем сообщение
            producer.send(record);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения в Kafka: {}", e.getMessage());
        }
    }

    /**
     * Очищает состояние сервиса (используется между тестами)
     */
    public void resetState() {
        eventUserWeights.clear();
        eventTotalWeights.clear();
        pairMinWeights.clear();
        log.info("Состояние сервиса было сброшено");
    }
}