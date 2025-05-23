package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventPublicFilterParamsDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.RecommendedEventDto;
import ru.practicum.ewm.event.facade.EventFacade;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events")
public class PublicEventController {
    private final EventFacade eventFacade;

    @GetMapping("/{id}")
    public EventFullDto get(@PathVariable("id") Long eventId,
                            @RequestHeader("X-EWM-USER-ID") Long userId,
                            HttpServletRequest request) {
        return eventFacade.get(eventId, userId, request);
    }

    @GetMapping
    public List<EventShortDto> get(@Valid EventPublicFilterParamsDto filters,
                                   @PositiveOrZero @RequestParam(defaultValue = "0") int from,
                                   @Positive @RequestParam(defaultValue = "10") int size,
                                   HttpServletRequest request) {
        return eventFacade.get(filters, from, size, request);
    }

    @GetMapping("/recommendations")
    public Stream<RecommendedEventDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") Long userId,
                                                          @PositiveOrZero @RequestParam(defaultValue = "10") int limit) {
        return eventFacade.getRecommendations(userId, limit);
    }

    @PutMapping("/{eventId}/like")
    public void addLike(@RequestHeader("X-EWM-USER-ID") Long userId,
                        @PathVariable("eventId") Long eventId) {
        eventFacade.addLike(userId, eventId);
    }
}
