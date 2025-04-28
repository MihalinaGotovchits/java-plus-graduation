package ru.practicum.stats.client;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stat.action.ActionTypeProto;
import ru.practicum.grpc.stat.action.UserActionProto;
import ru.practicum.grpc.stat.dashboard.RecommendationsControllerGrpc;
import ru.practicum.grpc.stat.collector.UserActionControllerGrpc;
import ru.practicum.grpc.stat.request.InteractionsCountRequestProto;
import ru.practicum.grpc.stat.request.RecommendedEventProto;
import ru.practicum.grpc.stat.request.SimilarEventsRequestProto;
import ru.practicum.grpc.stat.request.UserPredictionsRequestProto;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClientImpl implements StatClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub userClient;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerClient;

    @Override
    public void registerUserAction(long eventId, long userId, ActionTypeProto actionType, Instant instant) {
        log.info("statsClientImpl registerUserAction for eventId = {}, userId = {}, actionType = {}, time = {}",
                eventId, userId, actionType, instant);

        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
        UserActionProto request = UserActionProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setActionType(actionType)
                .setTimestamp(timestamp)
                .build();
        log.info("statsClientImpl registerUserAction request = {}", request);
        userClient.collectUserAction(request);
    }

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        log.info("statsClientImpl getSimilarEvents for eventId = {}, userId = {}, maxResults = {}",
                eventId, userId, maxResults);
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        Iterator<RecommendedEventProto> iterator = analyzerClient.getSimilarEvents(request);

        return asStream(iterator);
    }

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.info("statsClientImpl getRecommendationsForUser for userId = {}, maxResults = {}", userId, maxResults);

        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = analyzerClient.getRecommendationsForUser(request);

        return asStream(iterator);
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("statsClientImpl getInteractionsCount for event list = {}", eventIds);

        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
        Iterator<RecommendedEventProto> iterator = analyzerClient.getInteractionsCount(request);

        return asStream(iterator);
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}