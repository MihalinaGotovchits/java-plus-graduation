package ru.practicum.controller.privates;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.UpdatedRequestsDto;
import ru.practicum.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventPrivateController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getPrivateEvents(@PathVariable Long userId, @Valid SearchEventParamPrivate searchEventParam) {
        log.info("Get request by id = {}", userId);
        return eventService.getEventsOfUser(userId, searchEventParam);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventFullDto createEvent(@PathVariable Long userId, @RequestBody @Valid NewEventDto newEventDto) {
        log.info("Post request with body = {}", newEventDto);
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getPrivateEvent(@PathVariable Long userId, @PathVariable(value = "eventId") @Min(1) Long eventId) {
        log.info("Get request by id = {}", userId);
        return eventService.getEventPrivate(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByUser(@PathVariable Long userId, @PathVariable(value = "eventId") @Min(1) Long eventId,
                                          @RequestBody @Valid UpdateEventUserRequest inputEventUpdate) {
        log.info("Patch request with body = {}", inputEventUpdate);
        return eventService.updateEventFromUser(userId, eventId, inputEventUpdate);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsPrivate(@PathVariable Long userId, @PathVariable(value = "eventId") @Min(1) Long eventId) {
        log.info("Get request by id = {}", userId);
        return eventService.getRequestsPrivate(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public UpdatedRequestsDto confirmRequestsPrivate(@PathVariable Long userId, @PathVariable(value = "eventId") @Min(1) Long eventId,
                                                                @RequestBody @Valid EventRequestStatusUpdateRequest updatedRequests) {
        log.info("Patch request with body = {}", updatedRequests);
        return eventService.confirmRequestsPrivate(userId, eventId, updatedRequests);
    }
}