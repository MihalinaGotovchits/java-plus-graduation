package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.event.facade.EventFacade;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;

import java.util.List;


@RequiredArgsConstructor
@Validated
@RestController
@Slf4j
public class PrivateEventController {
    private final EventFacade eventFacade;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = "/users/{userId}/events")
    public EventFullDto createEvent(@PathVariable("userId") Long userId,
                                    @Valid @RequestBody NewEventDto newEventDto) {
        return eventFacade.addEvent(userId, newEventDto);
    }

    @GetMapping(path = "/users/{userId}/events")
    public List<EventShortDto> getEvent(@PathVariable("userId") Long userId,
                                        @PositiveOrZero @RequestParam(defaultValue = "0") int from,
                                        @Positive @RequestParam(defaultValue = "10") int size) {
        return eventFacade.getEventsByUserId(userId, from, size);
    }

    @GetMapping(path = "/users/{userId}/events/{eventId}")
    public EventFullDto getEvent(@PathVariable("userId") Long userId, @PathVariable("eventId") Long eventId) {
        return eventFacade.getEventById(userId, eventId);
    }

    @PatchMapping(path = "/users/{userId}/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable("userId") Long userId,
                                    @PathVariable("eventId") Long eventId,
                                    @Valid @RequestBody UpdateEventUserRequestDto eventUpdateDto) {
        return eventFacade.updateEvent(userId, eventId, eventUpdateDto);
    }

    @GetMapping(path = "/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getParticipationRequests(@PathVariable("userId") Long userId,
                                                                  @PathVariable("eventId") Long eventId) {
        return eventFacade.getEventAllParticipationRequests(eventId, userId);
    }

    @PatchMapping(path = "/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResultDto updatedEventRequestStatus(@PathVariable("userId") Long userId,
                                                                       @PathVariable("eventId") Long eventId,
                                                                       @RequestBody EventRequestStatusUpdateRequestDto request) {

        log.info("EventRequestStatusUpdateRequestDto: {}", request);
        return eventFacade.changeEventState(userId, eventId, request);
    }
}