package ru.practicum.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.UpdatedRequestsDto;

import java.util.List;

public interface EventService {

    List<EventFullDto> getAllEventFromAdmin(SearchEventParamAdmin searchEventParamAdmin);

    List<EventFullDto> getAllEventPublic(SearchEventParamPublic searchEventParamPublic);

    EventFullDto updateEventFromAdmin(Long userId, UpdateEventAdminRequest updateEventAdminRequest);

    List<EventShortDto> getEventsOfUser(Long userId, SearchEventParamPrivate searchEventParamPrivate);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getEventPrivate(Long userId, Long eventId);

    EventFullDto getEvent(Long id) throws JsonProcessingException;

    EventFullDto updateEventFromUser(Long userId, Long eventId, UpdateEventUserRequest inputEventUpdate);

    List<ParticipationRequestDto> getRequestsPrivate(Long userId, Long eventId);

    UpdatedRequestsDto confirmRequestsPrivate(Long userId, Long eventId, EventRequestStatusUpdateRequest updatedRequests);

}
