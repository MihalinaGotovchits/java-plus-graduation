package ru.practicum.ewm.event.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QSort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.categories.repository.CategoriesRepository;
import ru.practicum.ewm.core.error.exception.ConflictDataException;
import ru.practicum.ewm.core.error.exception.NotFoundException;
import ru.practicum.ewm.core.error.exception.ValidationException;
import ru.practicum.ewm.core.util.DateTimeUtil;
import ru.practicum.ewm.core.util.PagingUtil;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.location.dto.NewLocationDto;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.event.model.*;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.location.repository.LocationRepository;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.participationrequest.dto.ParticipationRequestDto;
import ru.practicum.ewm.participationrequest.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.participationrequest.model.ParticipationRequest;
import ru.practicum.ewm.participationrequest.model.ParticipationRequestStatus;
import ru.practicum.ewm.participationrequest.model.QParticipationRequest;
import ru.practicum.ewm.participationrequest.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.StatsDto;
import ru.practicum.stats.dto.StatsRequestParamsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.querydsl.core.group.GroupBy.groupBy;


@Slf4j
@Service
@AllArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoriesRepository categoriesRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final ParticipationRequestRepository participationRequestRepository;
    private final ParticipationRequestMapper participationRequestMapper;
    private final EntityManager entityManager;
    private final StatClient statClient;

    private static final String appNameForStat = "ewm-main-service";

    private Event checkAndGetEventByIdAndInitiatorId(Long eventId, Long initiatorId) {
        return eventRepository.findByIdAndInitiator_Id(eventId, initiatorId)
                .orElseThrow(() -> new NotFoundException(String.format("On event operations - " +
                        "Event doesn't exist with id %s or not available for User with id %s: ", eventId, initiatorId)));
    }

    @Override
    public EventFullDto addEvent(Long id, NewEventDto newEventDto) {
        checkEventTime(newEventDto.getEventDate());
        Category category = categoriesRepository.findById(newEventDto.getCategory()).get();
        User user = userRepository.findById(id).get();
        Location location = getOrCreateLocation(newEventDto.getLocation());

        Event event = eventRepository.save(eventMapper.toEvent(newEventDto, category, user, location));
        return eventMapper.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getEventsByUserId(Long id, int from, int size) {
        PageRequest page = PagingUtil.pageOf(from, size).withSort(Sort.by(Sort.Order.desc("eventDate")));
        List<Event> events = eventRepository.findAllByInitiator_Id(id, page);

        List<EventShortDto> eventsDto = eventMapper.toShortDto(events);
        populateWithConfirmedRequests(events, eventsDto);
        populateWithStats(eventsDto);

        return eventsDto;
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        Event event = checkAndGetEventByIdAndInitiatorId(eventId, userId);

        EventFullDto eventDto = eventMapper.toFullDto(event);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        return eventDto;
    }

    @Override
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequestDto eventUpdateDto) {
        Event event = checkAndGetEventByIdAndInitiatorId(eventId, userId);

        if (event.getState() == EventStates.PUBLISHED)
            throw new ConflictDataException(
                    String.format("On Event private update - " +
                                    "Event with id %s can't be changed because it is published.", event.getId()));
        checkEventTime(eventUpdateDto.getEventDate());

        eventMapper.update(event, eventUpdateDto, getOrCreateLocation(eventUpdateDto.getLocation()));
        if (eventUpdateDto.getStateAction() != null) {
            setStateToEvent(eventUpdateDto, event);
        }
        event.setId(eventId);

        event = eventRepository.save(event);

        EventFullDto eventDto = eventMapper.toFullDto(event);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        return eventDto;
    }

    @Override
    public EventFullDto update(Long eventId, UpdateEventAdminRequestDto updateEventAdminRequestDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("On Event admin update - Event doesn't exist with id: " + eventId));

        Category category = null;
        if (updateEventAdminRequestDto.getCategory() != null)
            category = categoriesRepository.findById(updateEventAdminRequestDto.getCategory())
                    .orElseThrow(() -> new NotFoundException("On Event admin update - Category doesn't exist with id: " +
                            updateEventAdminRequestDto.getCategory()));

        event = eventMapper.update(event, updateEventAdminRequestDto, category,
                getOrCreateLocation(updateEventAdminRequestDto.getLocation()));
        calculateNewEventState(event, updateEventAdminRequestDto.getStateAction());

        event = eventRepository.save(event);
        EventFullDto eventDto = eventMapper.toFullDto(event);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        log.info("Event is updated by admin: {}", event);
        return eventDto;
    }

    @Override
    public EventFullDto get(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("On Event public get - Event doesn't exist with id: " + eventId));

        if (event.getState() != EventStates.PUBLISHED)
            throw new NotFoundException("On Event public get - Event isn't published with id: " + eventId);

        EventFullDto eventDto = eventMapper.toFullDto(event);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        hitStat(request);
        return eventDto;
    }

    @Override
    public List<EventFullDto> get(EventAdminFilterParamsDto filters, int from, int size) {
        QEvent event = QEvent.event;

        BooleanBuilder builder = new BooleanBuilder();

        if (filters.getUsers() != null && !filters.getUsers().isEmpty())
            builder.and(event.initiator.id.in(filters.getUsers()));

        if (filters.getStates() != null && !filters.getStates().isEmpty())
            builder.and(event.state.in(filters.getStates()));

        if (filters.getCategories() != null && !filters.getCategories().isEmpty())
            builder.and(event.category.id.in(filters.getCategories()));

        if (filters.getRangeStart() != null)
            builder.and(event.eventDate.goe(filters.getRangeStart()));

        if (filters.getRangeEnd() != null)
            builder.and(event.eventDate.loe(filters.getRangeEnd()));

        List<Event> events = eventRepository.findAll(builder,
                PagingUtil.pageOf(from, size).withSort(new QSort(event.createdOn.desc()))).toList();

        List<EventFullDto> eventsDto = eventMapper.toFullDto(events);
        populateWithConfirmedRequests(events, eventsDto);
        populateWithStats(eventsDto);

        return eventsDto;
    }

    @Override
    public List<EventShortDto> get(EventPublicFilterParamsDto filters, int from, int size, HttpServletRequest request) {
        QEvent qEvent = QEvent.event;

        BooleanBuilder builder = new BooleanBuilder();

        builder.and(qEvent.state.eq(EventStates.PUBLISHED));

        if (filters.getText() != null)
            builder.and(qEvent.annotation.containsIgnoreCase(filters.getText())
                    .or(qEvent.description.containsIgnoreCase(filters.getText())));

        if (filters.getCategories() != null && !filters.getCategories().isEmpty())
            builder.and(qEvent.category.id.in(filters.getCategories()));

        if (filters.getPaid() != null)
            builder.and(qEvent.paid.eq(filters.getPaid()));

        if (filters.getRangeStart() == null && filters.getRangeEnd() == null)
            builder.and(qEvent.eventDate.goe(DateTimeUtil.currentDateTime()));
        else {
            if (filters.getRangeStart() != null)
                builder.and(qEvent.eventDate.goe(filters.getRangeStart()));

            if (filters.getRangeEnd() != null)
                builder.and(qEvent.eventDate.loe(filters.getRangeEnd()));
        }

        if (filters.getLon() != null && filters.getLat() != null)
            builder.and(Expressions.booleanTemplate("distance({0}, {1}, {2}, {3}) <= {4}",
                    qEvent.location.lat,
                    qEvent.location.lon,
                    filters.getLat(),
                    filters.getLon(),
                    filters.getRadius()));

        PageRequest page = PagingUtil.pageOf(from, size);
        if (filters.getSort() != null && filters.getSort() == EventPublicFilterParamsDto.EventSort.EVENT_DATE)
            page.withSort(new QSort(qEvent.eventDate.desc()));

        List<Event> events = eventRepository.findAll(builder, page).toList();

        List<EventShortDto> eventsDto = eventMapper.toShortDto(events);
        populateWithConfirmedRequests(events, eventsDto, true);
        populateWithStats(eventsDto);

        if (filters.getSort() != null && filters.getSort() == EventPublicFilterParamsDto.EventSort.VIEWS)
            eventsDto.sort(Comparator.comparing(EventShortDto::getViews).reversed());

        hitStat(request);
        return eventsDto;
    }

    @Override
    public List<ParticipationRequestDto> getEventAllParticipationRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Такого события не существует: " + eventId));
        checkEventOwner(event, userId);
        return participationRequestRepository.findAllByEvent_IdAndStatus(eventId, ParticipationRequestStatus.PENDING)
                .stream()
                .map(participationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResultDto changeEventState(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequestDto statusUpdateRequest) {
        log.info("Change event state by user: {} and event id: {}", userId, eventId);

        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("Такого события не существует: " + eventId));
        userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("Такого пользователя не существует: " + userId));
        checkEventOwner(event, userId);
        int participantsLimit = event.getParticipantLimit();

        List<ParticipationRequest> confirmedRequests = participationRequestRepository.findAllByEvent_IdAndStatus(eventId,
                ParticipationRequestStatus.CONFIRMED);
        List<ParticipationRequest> requestToChangeStatus = statusUpdateRequest.getRequestIds()
                .stream()
                .map(participationRequestRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        //Не очень понял, как обрабатывать это условие:
        // "если для события лимит заявок равен 0 или отключена пре-модерация заявок, то подтверждение заявок не требуется"
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            log.info("Заявки подтверждать не требуется");
            return null;
        }

        log.info("Заявки:  Лимит: {}, а заявок {}, разница между ними: {}", participantsLimit,
                statusUpdateRequest.getRequestIds().size(), (participantsLimit
                        - statusUpdateRequest.getRequestIds().size()));

        if (statusUpdateRequest.getStatus().equals(ParticipationRequestStatus.CONFIRMED)) {
            log.info("меняем статус заявок для статуса: {}", ParticipationRequestStatus.CONFIRMED);
            if ((participantsLimit - (confirmedRequests.size()) - statusUpdateRequest.getRequestIds().size()) >= 0) {
                for (ParticipationRequest request : requestToChangeStatus) {
                    request.setStatus(ParticipationRequestStatus.CONFIRMED);
                    participationRequestRepository.save(request);
                }
                return new EventRequestStatusUpdateResultDto(requestToChangeStatus
                        .stream().map(participationRequestMapper::toDto)
                        .toList(), null);
            } else {
                throw new ConflictDataException("слишком много участников. Лимит: " + participantsLimit +
                        ", уже подтвержденных заявок: " + confirmedRequests.size() + ", а заявок на одобрение: " +
                        statusUpdateRequest.getRequestIds().size() +
                        ". Разница между ними: " + (participantsLimit - confirmedRequests.size() -
                        statusUpdateRequest.getRequestIds().size()));
            }
        } else if (statusUpdateRequest.getStatus().equals(ParticipationRequestStatus.REJECTED)) {
            log.info("меняем статус заявок для статуса: {}", ParticipationRequestStatus.REJECTED);
            for (ParticipationRequest request : requestToChangeStatus) {
                if (request.getStatus() == ParticipationRequestStatus.CONFIRMED) {
                    throw new ConflictDataException("Заявка" + request.getStatus() + "уже подтверждена.");
                }
                request.setStatus(ParticipationRequestStatus.REJECTED);
                participationRequestRepository.save(request);
            }
            return new EventRequestStatusUpdateResultDto(null, requestToChangeStatus
                    .stream().map(participationRequestMapper::toDto)
                    .toList());
        }
        return null;
    }

    private Location getOrCreateLocation(NewLocationDto newLocationDto) {
        return newLocationDto == null ? null : locationRepository.findByLatAndLon(newLocationDto.getLat(), newLocationDto.getLon())
                .orElseGet(() -> locationRepository.save(locationMapper.toLocation(newLocationDto)));
    }

    private void calculateNewEventState(Event event, EventStateActionAdmin stateAction) {
        if (stateAction == EventStateActionAdmin.PUBLISH_EVENT) {
            if (event.getState() != EventStates.PENDING) {
                throw new ConflictDataException(
                        String.format("On Event admin update - " +
                                        "Event with id %s can't be published from the state %s: ",
                                event.getId(), event.getState()));
            }

            LocalDateTime currentDateTime = DateTimeUtil.currentDateTime();
            if (currentDateTime.plusHours(1).isAfter(event.getEventDate()))
                throw new ConflictDataException(
                        String.format("On Event admin update - " +
                                        "Event with id %s can't be published because the event date is to close %s: ",
                                event.getId(), event.getEventDate()));

            event.setPublishedOn(currentDateTime);
            event.setState(EventStates.PUBLISHED);
        } else if (stateAction == EventStateActionAdmin.REJECT_EVENT) {
            if (event.getState() == EventStates.PUBLISHED) {
                throw new ConflictDataException(
                        String.format("On Event admin update - " +
                                        "Event with id %s can't be canceled because it is already published: ",
                                event.getState()));
            }

            event.setState(EventStates.CANCELED);
        }
    }

    private void setStateToEvent(UpdateEventUserRequestDto eventUpdateDto, Event event) {
        if (eventUpdateDto.getStateAction().toString()
                .equalsIgnoreCase(EventStateActionPrivate.CANCEL_REVIEW.toString())) {
            event.setState(EventStates.CANCELED);
        } else if (eventUpdateDto.getStateAction().toString()
                .equalsIgnoreCase(EventStateActionPrivate.SEND_TO_REVIEW.toString())) {
            event.setState(EventStates.PENDING);
        }
    }

    private void checkEventTime(LocalDateTime eventDate) {
        if (eventDate == null) return;
        log.info("Проверяем дату события на корректность: {}", eventDate);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime correctEventTime = eventDate.plusHours(2);
        if (correctEventTime.isBefore(now)) {
            log.info("дата не корректна");
            throw new ValidationException("Дата события должна быть +2 часа вперед");
        }
    }

    private void checkEventOwner(Event event, Long userId) {
        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ValidationException("Событие создал другой пользователь");
        }
    }

    private void populateWithConfirmedRequests(List<Event> events, List<? extends EventShortDto> eventsDto) {
        populateWithConfirmedRequests(events, eventsDto, null);
    }

    private void populateWithConfirmedRequests(List<Event> events, List<?  extends EventShortDto>  eventsDto, Boolean filterOnlyAvailable) {
        QParticipationRequest qRequest = QParticipationRequest.participationRequest;
        BooleanBuilder requestBuilder = new BooleanBuilder();
        requestBuilder.and(qRequest.status.eq(ParticipationRequestStatus.CONFIRMED)).and(qRequest.event.in(events));

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager);

        Expression<Long> countExpression = qRequest.count();

        if (filterOnlyAvailable != null && filterOnlyAvailable) {
            countExpression = new CaseBuilder()
                    .when(qRequest.event.participantLimit.eq(0)
                            .or(qRequest.event.participantLimit.gt(qRequest.count()))).then(qRequest.count())
                    .otherwise(-1L);
        }

        JPAQuery<Tuple> query = jpaQueryFactory.selectFrom(qRequest)
                .select(qRequest.event.id, countExpression)
                .where(requestBuilder)
                .groupBy(qRequest.event.id, qRequest.event.participantLimit);

        Map<Long, Long> confirmedRequests = query
                .transform(groupBy(qRequest.event.id).as(countExpression));

        eventsDto
                .forEach(event -> event.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L)));

        if (filterOnlyAvailable != null && filterOnlyAvailable) {
            eventsDto.removeIf(event -> event.getConfirmedRequests() < 0);
        }
    }

    private void populateWithStats(List<? extends EventShortDto> eventsDto) {
        if (eventsDto.isEmpty()) return;

        Map<String, EventShortDto> uris = eventsDto.stream()
                .collect(Collectors.toMap(e -> String.format("/events/%s", e.getId()), e -> e));

        LocalDateTime currentDateTime = DateTimeUtil.currentDateTime();
        List<StatsDto> stats = statClient.get(StatsRequestParamsDto.builder()
                .start(currentDateTime.minusDays(1))
                .end(currentDateTime)
                .uris(uris.keySet().stream().toList())
                .unique(true)
                .build());

        stats.forEach(stat -> Optional.ofNullable(uris.get(stat.getUri()))
                .ifPresent(e -> e.setViews(stat.getHits())));
    }

    private void hitStat(HttpServletRequest request) {
        statClient.hit(HitDto.builder()
                .app(appNameForStat)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(DateTimeUtil.currentDateTime())
                .build());
    }
}
