package ru.practicum.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatClient;
import ru.practicum.dto.StatResponseDto;
import ru.practicum.dto.comment.CountCommentsByEventDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.UpdatedRequestsDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.UncorrectedParametersException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.*;
import ru.practicum.repository.*;
import ru.practicum.service.EventService;
import ru.practicum.status.event.AdminEventStatus;
import ru.practicum.status.event.State;
import ru.practicum.status.event.UserEventStatus;
import ru.practicum.status.request.RequestStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    private final CategoryRepository categoryRepository;

    private final RequestRepository requestRepository;

    private final UserRepository userRepository;

    private final LocationRepository locationRepository;

    private final StatClient statClient;

    private final CommentRepository commentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<EventFullDto> getAllEventPublic(SearchEventParamPublic searchEventParamPublic) {

        LocalDateTime rangeEnd = searchEventParamPublic.getRangeEnd();
        LocalDateTime rangeStart = searchEventParamPublic.getRangeStart();

        if (rangeEnd != null && rangeStart != null && rangeEnd.isBefore(rangeStart)) {
            throw new UncorrectedParametersException("rangeEnd is before rangeStart");
        }

        PageRequest pageable = PageRequest.of(searchEventParamPublic.getFrom() / searchEventParamPublic.getSize(),
                searchEventParamPublic.getSize());
        Specification<Event> specification = Specification.where((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        String text = searchEventParamPublic.getText();
        List<Long> categories = searchEventParamPublic.getCategories();
        Boolean paid = searchEventParamPublic.getPaid();
        Boolean onlyAvailable = searchEventParamPublic.getOnlyAvailable();
        Sort sort = searchEventParamPublic.getSort();

        if (text != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("description")),
                                    "%" + text.toLowerCase() + "%"
                            ),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("annotation")),
                                    "%" + text.toLowerCase() + "%"
                            )
                    )
            );
        }

        if (categories != null && !categories.isEmpty()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("category").get("id").in(categories));
        }
        if (paid != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("paid"), paid));
        }
        if (rangeEnd != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }
        if (rangeStart != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }

        Page<Event> events = eventRepository.findAll(specification, pageable);

        List<Event> eventsResponse = events.toList();

        if (onlyAvailable != null && onlyAvailable) {
            eventsResponse = getAvailableOnly(eventsResponse);
        } else {
            Map<Long, List<Request>> confirmedRequestsCountMap = getConfirmedRequestsCount(events.toList());
            for (Event event : eventsResponse) {
                List<Request> requests = confirmedRequestsCountMap.getOrDefault(event.getId(), List.of());
                event.setConfirmedRequests(requests.size());
            }
        }

        List<CountCommentsByEventDto> commentsCount = commentRepository.countCommentByEvent(eventsResponse.stream()
                .map(Event::getId).collect(Collectors.toList()));
        Map<Long, Long> commentsCountToEventIdMap = commentsCount.stream().collect(Collectors.toMap(
                CountCommentsByEventDto::getEventId, CountCommentsByEventDto::getCountComments));

        List<EventFullDto> eventFullDtos = eventsResponse.stream().map(event -> {
            EventFullDto eventFullDto = EventMapper.toEventFullDto(event);
            Long commentCount = commentsCountToEventIdMap.getOrDefault(event.getId(), 0L);
            eventFullDto.setComments(commentCount);
            return eventFullDto;
        }).toList();

        setViewsCount(eventsResponse);

        if (sort == Sort.VIEWS) {
            return eventFullDtos.stream()
                    .sorted(Comparator.comparing(EventFullDto::getViews))
                    .collect(Collectors.toList());
        } else {
            return eventFullDtos.stream()
                    .sorted(Comparator.comparing(EventFullDto::getEventDate))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<EventFullDto> getAllEventFromAdmin(SearchEventParamAdmin searchEventParamsAdmin) {
        PageRequest pageable = PageRequest.of(searchEventParamsAdmin.getFrom() / searchEventParamsAdmin.getSize(),
                searchEventParamsAdmin.getSize());
        Specification<Event> specification = Specification.where(null);

        List<Long> users = searchEventParamsAdmin.getUsers();
        List<String> states = searchEventParamsAdmin.getStates();
        List<Long> categories = searchEventParamsAdmin.getCategories();
        LocalDateTime rangeEnd = searchEventParamsAdmin.getRangeEnd();
        LocalDateTime rangeStart = searchEventParamsAdmin.getRangeStart();

        if (users != null && !users.isEmpty()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("initiator").get("id").in(users));
        }
        if (states != null && !states.isEmpty()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("state").as(String.class).in(states));
        }
        if (categories != null && !categories.isEmpty()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("category").get("id").in(categories));
        }
        if (rangeEnd != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }
        if (rangeStart != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }
        Page<Event> events = eventRepository.findAll(specification, pageable);

        List<EventFullDto> result = events.getContent()
                .stream().map(EventMapper::toEventFullDto).collect(Collectors.toList());

        Map<Long, List<Request>> confirmedRequestsCountMap = getConfirmedRequestsCount(events.toList());
        for (EventFullDto event : result) {
            List<Request> requests = confirmedRequestsCountMap.getOrDefault(event.getId(), List.of());
            event.setConfirmedRequests(requests.size());
        }
        return result;
    }

    public UpdatedRequestsDto confirmRequestsPrivate(Long userId, Long eventId, EventRequestStatusUpdateRequest updatedRequests) {

        User user = checkUser(userId);

        Event event = checkEvent(eventId);

        checkEventInitiator(event, user);

        List<Long> ids = updatedRequests.getRequestIds().stream().toList();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            return RequestMapper.toUpdatedRequestsDto(RequestMapper.toListRequestDto(requestRepository.findAllByIdIn(ids)), List.of());
        }

        List<Request> requests = requestRepository.findAllByEventIdAndStatusAndIdIn(eventId, RequestStatus.PENDING, ids);

        if (requests.size() != updatedRequests.getRequestIds().size()) {
            throw new ConflictException("Изменить можно только заявки в статусе ожидания");
        }

        List<Request> confirmedRequests = getConfirmedRequests(eventId);

        if (confirmedRequests.size() + updatedRequests.getRequestIds().size() > event.getParticipantLimit()) {
            throw new ConflictException("Нельзя выходить за лимиты заявок");
        }

        requestRepository.updateRequestsStatusByIds(updatedRequests.getStatus(), ids);

        if (confirmedRequests.size() + updatedRequests.getRequestIds().size() == event.getParticipantLimit()) {
            requestRepository.rejectRequestsPending();
        }

        entityManager.clear();

        if (updatedRequests.getStatus().equals(RequestStatus.CONFIRMED)) {
            return RequestMapper.toUpdatedRequestsDto(RequestMapper.toListRequestDto(requestRepository.findAllByIdIn(ids)), List.of());
        } else {
            return RequestMapper.toUpdatedRequestsDto(List.of(), RequestMapper.toListRequestDto(requestRepository.findAllByIdIn(ids)));
        }
    }


    @Override
    public EventFullDto getEventPrivate(Long userId, Long eventId) {

        User user = checkUser(userId);

        Event event = checkEvent(eventId);

        checkEventInitiator(event, user);

        return EventMapper.toEventFullDto(event, getConfirmedRequests(event.getId()));

    }

    @Override
    public EventFullDto getEvent(Long id) {

        Event event = eventRepository.findByIdAndState(id, State.PUBLISHED);

        if (event == null) {
            throw new NotFoundException("Не найдено опубликованного события по заданному id");
        }

        List<String> params = new ArrayList<>();
        params.add("/events/" + event.getId());

        List<StatResponseDto> statResponseDtos = statClient.readStatEvent(null, null, params, true);
        if (statResponseDtos.size() > 0) {
            event.setViews(((long) statResponseDtos.size()));
        } else {
            event.setViews(0L);
        }

        return EventMapper.toEventFullDto(event, getConfirmedRequests(event.getId()));

    }

    @Override
    public EventFullDto updateEventFromAdmin(Long eventId, UpdateEventAdminRequest updateEvent) {
        Event oldEvent = checkEvent(eventId);
        if (oldEvent.getState().equals(State.PUBLISHED) || oldEvent.getState().equals(State.CANCELED)) {
            throw new ConflictException("Имзменить можно только неподтвержденное событие");
        }
        boolean hasChanges = false;
        Event eventForUpdate = universalUpdate(oldEvent, EventMapper.toNewEventDto(updateEvent));
        if (eventForUpdate == null) {
            eventForUpdate = oldEvent;
        } else {
            hasChanges = true;
        }
        LocalDateTime eventDate = updateEvent.getEventDate();
        if (eventDate != null) {
            if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new UncorrectedParametersException("Некорректные параметры даты. Дата начала изменяемого" +
                        "события должна быть не ранне, чем за час от даты его публикации.");
            }
            eventForUpdate.setEventDate(eventDate);
            hasChanges = true;
        }
        LocalDateTime publishedOn = updateEvent.getPublishedOn();
        if (publishedOn != null) {
            if (publishedOn.isAfter(LocalDateTime.now())) {
                throw new UncorrectedParametersException("Некорректные параметры даты. Дата публикации " +
                        "события должна быть не позже, чем нынешние дата и время.");
            }
            eventForUpdate.setPublishedOn(publishedOn);
            hasChanges = true;
        }

        AdminEventStatus action = updateEvent.getStateAction();
        if (action != null) {
            if (AdminEventStatus.PUBLISH_EVENT.equals(action)) {
                eventForUpdate.setState(State.PUBLISHED);
                eventForUpdate.setPublishedOn(LocalDateTime.now());
                hasChanges = true;
            } else if (AdminEventStatus.REJECT_EVENT.equals(action)) {
                eventForUpdate.setState(State.CANCELED);
                hasChanges = true;
            }
        }
        Event eventAfterUpdate = null;
        if (hasChanges) {
            eventAfterUpdate = eventRepository.save(eventForUpdate);
        }
        return eventAfterUpdate != null ? EventMapper.toEventFullDto(eventAfterUpdate) : null;
    }

    @Override
    public EventFullDto updateEventFromUser(Long userId, Long eventId, UpdateEventUserRequest updateEvent) {

        User user = checkUser(userId);

        Event oldEvent = checkEvent(eventId);

        checkEventInitiator(oldEvent, user);

        if (!oldEvent.getState().equals(State.PENDING) && !oldEvent.getState().equals(State.CANCELED)) {
            throw new ConflictException("Изменить можно только неподтвержденное событие");
        }
        boolean hasChanges = false;
        Event eventForUpdate = universalUpdate(oldEvent, EventMapper.toNewEventDto(updateEvent));
        if (eventForUpdate == null) {
            eventForUpdate = oldEvent;
        } else {
            hasChanges = true;
        }
        LocalDateTime eventDate = updateEvent.getEventDate();
        if (eventDate != null) {
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new UncorrectedParametersException("Некорректные параметры даты. Дата начала изменяемого" +
                        "события должна быть не ранне, чем за 2 часа от даты его публикации.");
            }
            eventForUpdate.setEventDate(updateEvent.getEventDate());
            hasChanges = true;
        }
        UserEventStatus action = updateEvent.getStateAction();
        if (action != null) {
            if (UserEventStatus.SEND_TO_REVIEW.equals(action)) {
                eventForUpdate.setState(State.PENDING);
                hasChanges = true;
            } else if (UserEventStatus.CANCEL_REVIEW.equals(action)) {
                eventForUpdate.setState(State.CANCELED);
                hasChanges = true;
            }
        }
        Event eventAfterUpdate = null;
        if (hasChanges) {
            eventAfterUpdate = eventRepository.save(eventForUpdate);
        }
        return eventAfterUpdate != null ? EventMapper.toEventFullDto(eventAfterUpdate) : null;
    }

    @Override
    public List<EventShortDto> getEventsOfUser(Long userId, SearchEventParamPrivate searchEventParamPrivate) {

        User user = checkUser(userId);

        PageRequest page = PageRequest.of(searchEventParamPrivate.getFrom() / searchEventParamPrivate.getSize(),
                searchEventParamPrivate.getSize());
        Specification<Event> specification = Specification.where(null);

        specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("initiator"), user));

        Page<Event> events = eventRepository.findAll(specification, page);

        List<EventShortDto> result = events.getContent().stream()
                .map(EventMapper::toEventShortDto).collect(Collectors.toList());
        Map<Long, List<Request>> confirmedRequestsCount = getConfirmedRequestsCount(events.toList());
        for (EventShortDto event : result) {
            List<Request> requests = confirmedRequestsCount.getOrDefault(event.getId(), List.of());
            event.setConfirmedRequest(requests.size());
        }

        return result;
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto newEvent) {

        User user = checkUser(userId);

        checkEventDate(newEvent.getEventDate());

        Location location = locationRepository.save(LocationMapper.toLocation(newEvent.getLocation()));

        Event event = EventMapper.toEvent(newEvent, user, location, checkCategory(newEvent.getCategory()));
        event.setState(State.PENDING);

        if (event.getPaid() == null) {
            event.setPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0);
        }
        if (event.getRequestModeration() == null) {
            event.setRequestModeration(true);
        }

        event.setCreateOn(LocalDateTime.now());
        return EventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsPrivate(Long userId, Long eventId) {

        User user = checkUser(userId);

        Event event = checkEvent(eventId);

        checkEventInitiator(event, user);

        List<Request> requests = requestRepository.findAllByEventId(eventId);

        return RequestMapper.toListRequestDto(requests);
    }

    private void checkEventInitiator(Event event, User user) {
        if (!event.getInitiator().equals(user)) {
            throw new ConflictException("Событие не создано текущим пользователем");
        }
    }

    private Event checkEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие с Id: " + eventId + " не найдено")
        );
    }

    private Category checkCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(
                () -> new NotFoundException("Категория с Id: " + categoryId + " не найдена")
        );
    }

    private User checkUser(Long userId) {

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new NotFoundException("Пользователь с Id: " + userId + " не найден");
        } else {
            return user.get();
        }
    }

    private void checkEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now())) {
            throw new UncorrectedParametersException("Дата события не может быть раньше текущей даты");
        }
    }

    private Map<Long, List<Request>> getConfirmedRequestsCount(List<Event> events) {
        List<Request> requests = requestRepository.findAllByEventIdInAndStatus(events.stream()
                .map(Event::getId).collect(Collectors.toList()), RequestStatus.CONFIRMED);
        return requests.stream().collect(Collectors.groupingBy(r -> r.getEvent().getId()));
    }

    private List<Event> getAvailableOnly(List<Event> events) {
        List<Request> requests = requestRepository.findAllByEventIdInAndStatus(events.stream()
                .map(Event::getId).collect(Collectors.toList()), RequestStatus.CONFIRMED);
        final Map<Long, List<Request>> requestsByEvent = requests.stream().collect(Collectors.groupingBy(r -> r.getEvent().getId()));

        List<Event> events1 = events.stream().filter(event -> requestsByEvent.containsKey(event.getId())
                && (event.getParticipantLimit() == null || event.getParticipantLimit() > requestsByEvent.get(event.getId()).size())).collect(Collectors.toList());

        for (Event event : events1) {
            event.setConfirmedRequests(requestsByEvent.get(event.getId()).size());
        }
        return events1;
    }

    private List<Request> getConfirmedRequests(Long eventId) {
        return requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private void setViewsCount(List<Event> events) {

        List<String> params = new ArrayList<>();
        Map<String, Event> eventMap = new HashMap<>();

        for (Event event : events) {
            String key = "/events/" + event.getId();
            eventMap.put(key, event);
            params.add(key);
        }

        List<StatResponseDto> responseDtos = statClient.readStatEvent(null, null, params, true);

        for (StatResponseDto responseDto : responseDtos) {
            Event event = eventMap.get(responseDto.getUri());
            if (event != null) {
                event.setViews((responseDto.getHits() == null) ? 0L : responseDto.getHits());
            }
        }
    }

    private Event universalUpdate(Event oldEvent, NewEventDto updateEvent) {
        boolean hasChanges = false;
        String annotation = updateEvent.getAnnotation();
        if (annotation != null && !annotation.isBlank()) {
            oldEvent.setAnnotation(annotation);
            hasChanges = true;
        }
        Long category = updateEvent.getCategory();
        if (category != null) {
            Category category1 = checkCategory(category);
            oldEvent.setCategory(category1);
            hasChanges = true;
        }
        String description = updateEvent.getDescription();
        if (description != null && !description.isBlank()) {
            oldEvent.setDescription(description);
            hasChanges = true;
        }
        if (updateEvent.getLocation() != null) {
            Location location = LocationMapper.toLocation(updateEvent.getLocation());
            oldEvent.setLocation(locationRepository.save(location));
            hasChanges = true;
        }
        Integer participantLimit = updateEvent.getParticipantLimit();
        if (participantLimit != null) {
            oldEvent.setParticipantLimit(participantLimit);
            hasChanges = true;
        }
        if (updateEvent.getPaid() != null) {
            oldEvent.setPaid(updateEvent.getPaid());
            hasChanges = true;
        }
        if (updateEvent.getRequestModeration() != null) {
            oldEvent.setRequestModeration(updateEvent.getRequestModeration());
            hasChanges = true;
        }
        String title = updateEvent.getTitle();
        if (title != null && !title.isBlank()) {
            oldEvent.setTitle(title);
            hasChanges = true;
        }
        if (!hasChanges) {
            oldEvent = null;
        }
        return oldEvent;
    }
}
