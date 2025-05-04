package ru.practicum.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.grpc.stat.dashboard.RecommendationsControllerGrpc;
import ru.practicum.grpc.stat.request.InteractionsCountRequestProto;
import ru.practicum.grpc.stat.request.RecommendedEventProto;
import ru.practicum.grpc.stat.request.SimilarEventsRequestProto;
import ru.practicum.grpc.stat.request.UserPredictionsRequestProto;
import ru.practicum.service.RecommendationService;

import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос на рекомендации для пользователя: {}", request);
        try {
            List<RecommendedEventProto> recommendedEvents = recommendationService.generateRecommendationsForUser(request);
            recommendedEvents.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
            log.info("Успешно сформированы рекомендации для пользователя");
        } catch (Exception e) {
            log.error("Ошибка при формировании рекомендаций для пользователя: {}", request, e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Ошибка сервера при получении рекомендаций: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос на поиск похожих событий: {}", request);
        try {
            List<RecommendedEventProto> similarEvents = recommendationService.getSimilarEvents(request);
            similarEvents.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
            log.info("Успешно найдены похожие события");
        } catch (Exception e) {
            log.error("Ошибка при поиске похожих событий: {}", request, e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Ошибка сервера при поиске похожих событий: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Получен запрос на получение количества взаимодействий: {}", request);
        try {
            List<RecommendedEventProto> interactions = recommendationService.getInteractionsCount(request);
            interactions.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
            log.info("Успешно получено количество взаимодействий");
        } catch (Exception e) {
            log.error("Ошибка при получении количества взаимодействий: {}", request, e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Ошибка сервера при получении количества взаимодействий: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}