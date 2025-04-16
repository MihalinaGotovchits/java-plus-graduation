package ru.practicum.ewm.event.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.LocationClient;
import ru.practicum.ewm.client.RequestClient;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.dto.location.NewLocationDto;
import ru.practicum.ewm.dto.request.ParticipationRequestCountDto;
import ru.practicum.ewm.dto.request.ParticipationRequestStatus;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.error.exception.ConflictDataException;
import ru.practicum.ewm.error.exception.NotFoundException;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.util.DateTimeUtil;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.StatsDto;
import ru.practicum.stats.dto.StatsRequestParamsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventFacadeImpl implements EventFacade {
    private final UserClient userClient;
    private final StatClient statClient;
    private final LocationClient locationClient;
    private final RequestClient requestClient;

    private final EventService eventService;
    private final EventMapper eventMapper;

    private static final String appNameForStat = "ewm-main-service";

    @Override
    public EventFullDto addEvent(Long id, NewEventDto newEventDto) {
        UserShortDto user = getUserById(id);
        LocationDto location = locationClient.addOrGetLocation(getLocation(newEventDto.getLocation()));
        Event event = eventService.addEvent(user.getId(), newEventDto, location.getId());

        return eventMapper.toFullDto(event, location, user);
    }

    @Override
    public List<EventShortDto> getEventsByUserId(Long id, int from, int size) {
        UserShortDto user = getUserById(id);
        List<Event> events = eventService.getEventsByUserId(id, from, size);
        List<EventShortDto> eventsDto = events.stream()
                .map(event -> eventMapper.toShortDto(event, user))
                .toList();
        populateWithConfirmedRequests(events, eventsDto);
        populateWithStats(eventsDto);
        return eventsDto;
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        UserShortDto user = getUserById(userId);
        Event event = eventService.getEventById(userId, eventId);
        LocationDto location = locationClient.getById(event.getLocationId());

        EventFullDto eventDto = eventMapper.toFullDto(event, location, user);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        return eventDto;
    }

    @Override
    public EventFullDto getEventById(Long eventId) {
        Event event = eventService.getEventById(eventId);
        UserShortDto user = getUserById(event.getInitiatorId());
        LocationDto location = locationClient.getById(event.getLocationId());

        EventFullDto eventDto = eventMapper.toFullDto(event, location, user);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        return eventDto;
    }

    @Override
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequestDto eventUpdateDto) {
        log.info("updateEvent: {}", eventUpdateDto);
        LocationDto location = eventUpdateDto.getLocation() == null ? null :
                locationClient.addOrGetLocation(getLocation(eventUpdateDto.getLocation()));
        log.info("updateEvent new location : {}", location);
        UserShortDto user = getUserById(userId);

        Event event = eventService.updateEvent(userId, eventId, location, eventUpdateDto);
        EventFullDto eventDto = eventMapper.toFullDto(event, location, user);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        return eventDto;
    }

    @Override
    public EventFullDto update(Long eventId, UpdateEventAdminRequestDto updateEventAdminRequestDto) {
        log.info("update: {}", updateEventAdminRequestDto);
        LocationDto location = updateEventAdminRequestDto.getLocation() == null ? null :
                locationClient.addOrGetLocation(getLocation(updateEventAdminRequestDto.getLocation()));
        log.info("update new location : {}", location);
        Event event = eventService.update(eventId, location, updateEventAdminRequestDto);
        UserShortDto user = getUserById(event.getInitiatorId());
        EventFullDto eventDto = eventMapper.toFullDto(event, location, user);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        return eventDto;
    }

    @Override
    public EventFullDto get(Long eventId, HttpServletRequest request) {
        Event event = eventService.get(eventId, request);
        UserShortDto user = getUserById(event.getInitiatorId());
        LocationDto location = locationClient.getById(event.getLocationId());

        EventFullDto eventDto = eventMapper.toFullDto(event, location, user);
        populateWithConfirmedRequests(List.of(event), List.of(eventDto));
        populateWithStats(List.of(eventDto));

        hitStat(request);
        return eventDto;
    }

    @Override
    public List<EventFullDto> get(EventAdminFilterParamsDto filters, int from, int size) {
        List<Event> events = eventService.get(filters, from, size);

        List<EventFullDto> eventsDto =  new ArrayList<>(eventMapper.toFullDto(events));

        populateWithConfirmedRequests(events, eventsDto);
        populateWithStats(eventsDto);

        return eventsDto;
    }

    @Override
    public List<EventShortDto> get(EventPublicFilterParamsDto filters, int from, int size, HttpServletRequest request) {
        List<LocationDto> locations = getLocationsByRadius(filters.getLat(), filters.getLon(), filters.getRadius());
        List<Event> events = eventService.get(filters, from, size, locations, request);

        List<EventShortDto> eventsDto2 = events.stream()
                .map(event -> eventMapper.toShortDto(event, null))
                .toList();

        List<EventShortDto> eventsDto = new ArrayList<>(eventsDto2);
        populateWithConfirmedRequests(events, eventsDto, true);
        populateWithStats(eventsDto);

        if (filters.getSort() != null && filters.getSort() == EventPublicFilterParamsDto.EventSort.VIEWS)
            eventsDto.sort(Comparator.comparing(EventShortDto::getViews).reversed());

        hitStat(request);
        return eventsDto;
    }

    @Override
    public List<ParticipationRequestDto> getEventAllParticipationRequests(Long eventId, Long userId) {
        Event event = eventService.checkAndGetEventByIdAndInitiatorId(eventId, userId);
        return requestClient.getByStatus(event.getId(), ParticipationRequestStatus.PENDING);
    }

    @Override
    public EventRequestStatusUpdateResultDto changeEventState(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequestDto requestStatusUpdateRequest) {
        Event event = eventService.checkAndGetEventByIdAndInitiatorId(eventId, userId);
        int participantsLimit = event.getParticipantLimit();

        List<ParticipationRequestDto> confirmedRequests = requestClient.getByStatus(eventId,
                ParticipationRequestStatus.CONFIRMED);
        log.info("confirmedRequests: {}", confirmedRequests);

        List<ParticipationRequestDto> requestToChangeStatus = requestClient.getByIds(requestStatusUpdateRequest.getRequestIds());
        List<Long> idsToChangeStatus = requestToChangeStatus.stream()
                .map(ParticipationRequestDto::getId)
                .toList();
        log.info("idsToChangeStatus: {}", idsToChangeStatus);
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            log.info("Заявки подтверждать не требуется");
            return null;
        }

        log.info("Заявки:  Лимит: {}, подтвержденных заявок {}, запрошенных заявок {}, разница между ними: {}", participantsLimit,
                confirmedRequests.size(), requestStatusUpdateRequest.getRequestIds().size(), (participantsLimit
                        - confirmedRequests.size() - requestStatusUpdateRequest.getRequestIds().size()));

        if (requestStatusUpdateRequest.getStatus().equals(ParticipationRequestStatus.CONFIRMED)) {
            log.info("меняем статус заявок для статуса: {}", ParticipationRequestStatus.CONFIRMED);
            if ((participantsLimit - (confirmedRequests.size()) - requestStatusUpdateRequest.getRequestIds().size()) >= 0) {
                List<ParticipationRequestDto> requestUpdated = requestClient.updateStatus(
                        ParticipationRequestStatus.CONFIRMED, idsToChangeStatus);
                return new EventRequestStatusUpdateResultDto(requestUpdated, null);
            } else {
                throw new ConflictDataException("слишком много участников. Лимит: " + participantsLimit +
                        ", уже подтвержденных заявок: " + confirmedRequests.size() + ", а заявок на одобрение: " +
                        idsToChangeStatus.size() +
                        ". Разница между ними: " + (participantsLimit - confirmedRequests.size() -
                        idsToChangeStatus.size()));
            }
        } else if (requestStatusUpdateRequest.getStatus().equals(ParticipationRequestStatus.REJECTED)) {
            log.info("меняем статус заявок для статуса: {}", ParticipationRequestStatus.REJECTED);

            for (ParticipationRequestDto request : requestToChangeStatus) {
                if (request.getStatus() == ParticipationRequestStatus.CONFIRMED) {
                    throw new ConflictDataException("Заявка" + request.getStatus() + "уже подтверждена.");
                }
            }

            List<ParticipationRequestDto> requestUpdated = requestClient.updateStatus(
                    ParticipationRequestStatus.REJECTED, idsToChangeStatus);
            return new EventRequestStatusUpdateResultDto(null, requestUpdated);
        }
        return null;
    }

    @Override
    public List<EventFullDto> getByLocation(Long locationId) {
        return eventService.getByLocation(locationId)
                .stream()
                .map(eventMapper::toFullDto)
                .toList();
    }

    private UserShortDto getUserById(Long userId) {
        UserShortDto user = userClient.getById(userId);
        if (user == null) {
            throw new NotFoundException("Такого пользователя не существует: " + userId);
        }

        return user;
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

    private List<LocationDto> getLocationsByRadius(Double lat, Double lon, Double radius) {
        if (lat == null || lon == null) {
            return Collections.emptyList();
        }

        return locationClient.getByRadius(lat, lon, radius);
    }

    private NewLocationDto getLocation(NewLocationDto newLocationDto) {
        if (newLocationDto != null) {
            return newLocationDto;
        }
        return NewLocationDto.builder()
                .lat(0D)
                .lon(0D)
                .build();
    }


    private void populateWithConfirmedRequests(List<Event> events, List<? extends EventShortDto> eventsDto) {
        populateWithConfirmedRequests(events, eventsDto, null);
    }

    private void populateWithConfirmedRequests(List<Event> events, List<? extends EventShortDto> eventsDto, Boolean filterOnlyAvailable) {
        List<Long> ids = eventsDto.stream()
                .map(EventShortDto::getId)
                .toList();
        Map<Long, ParticipationRequestCountDto> confirmedRequests =  requestClient.getConfirmedCount(ids)
                .stream()
                .collect(Collectors.toMap(ParticipationRequestCountDto::getEventId, Function.identity()));
        ParticipationRequestCountDto zeroCount = new ParticipationRequestCountDto(0L, 0L);
        eventsDto.forEach(event ->
                event.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), zeroCount).getQuantity()));

        if (filterOnlyAvailable != null && filterOnlyAvailable) {
            eventsDto.removeIf(event -> eventService.getEventById(event.getId()).getParticipantLimit() -
                    event.getConfirmedRequests() <= 0);
        }
    }
}
