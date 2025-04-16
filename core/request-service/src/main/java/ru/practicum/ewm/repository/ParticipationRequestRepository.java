package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.dto.request.ParticipationRequestStatus;
import ru.practicum.ewm.model.ParticipationRequestCount;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long>,
        QuerydslPredicateExecutor<ParticipationRequest> {

    @Query("SELECT new ru.practicum.ewm.model.ParticipationRequestCount(pr.eventId, count(pr.id)) " +
            "FROM ParticipationRequest pr WHERE pr.eventId in :ids and status = 'CONFIRMED' GROUP BY pr.eventId")
    List<ParticipationRequestCount> getParticipationRequestCountConfirmed(@Param("ids") List<Long> ids);

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    long countByEventIdAndStatusIn(Long eventId, List<ParticipationRequestStatus> status);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, ParticipationRequestStatus status);


}