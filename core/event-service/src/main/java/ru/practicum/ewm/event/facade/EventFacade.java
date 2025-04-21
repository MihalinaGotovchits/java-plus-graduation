package ru.practicum.ewm.event.facade;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;

import java.util.List;

public interface EventFacade {
    EventFullDto addEvent(Long id, NewEventDto newEventDto);

    List<EventShortDto> getEventsByUserId(Long id, int from, int size);

    EventFullDto getEventById(Long userId, Long eventId);

    EventFullDto getEventById(Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequestDto eventUpdateDto);

    EventFullDto update(Long eventId, UpdateEventAdminRequestDto updateEventAdminRequestDto);

    EventFullDto get(Long eventId, HttpServletRequest request);

    List<EventFullDto> get(EventAdminFilterParamsDto filters, int from, int size);

    List<EventShortDto> get(EventPublicFilterParamsDto filters, int from, int size, HttpServletRequest request);

    List<ParticipationRequestDto> getEventAllParticipationRequests(Long eventId, Long userId);

    EventRequestStatusUpdateResultDto changeEventState(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequestDto requestStatusUpdateRequest);

    List<EventFullDto> getByLocation(Long locationId);
}
