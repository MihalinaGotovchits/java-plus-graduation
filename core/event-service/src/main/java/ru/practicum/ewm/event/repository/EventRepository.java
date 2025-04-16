package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.ewm.event.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    List<Event> findAllByInitiatorId(Long initiatorId, Pageable page);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    List<Event> findAllByLocationId(Long locationId);
}