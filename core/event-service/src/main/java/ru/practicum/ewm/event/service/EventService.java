package ru.practicum.ewm.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.event.model.Event;

import java.util.List;

public interface EventService {
    Event addEvent(Long id, NewEventDto newEventDto, Long locationId);

    List<Event> getEventsByUserId(Long id, int from, int size);

    Event getEventById(Long userId, Long eventId);

    Event getEventById(Long eventId);

    Event updateEvent(Long userId, Long eventId, LocationDto location, UpdateEventUserRequestDto eventUpdateDto);

    Event update(Long eventId, LocationDto location, UpdateEventAdminRequestDto updateEventAdminRequestDto);

    Event get(Long eventId, HttpServletRequest request);

    List<Event> get(EventAdminFilterParamsDto filters, int from, int size);

    List<Event> get(EventPublicFilterParamsDto filters, int from, int size, List<LocationDto> locations,
                    HttpServletRequest request);

    Event checkAndGetEventByIdAndInitiatorId(Long eventId, Long initiatorId);

    List<Event> getByLocation(Long locationId);
}