package ru.practicum.controller.pub;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.StatClient;
import ru.practicum.dto.StatDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.SearchEventParamPublic;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EventPublicController {

    private final EventService eventService;
    private final StatClient statClient;

    @Value("${server.application.name:ewm-service}")
    private String applicationName;

    @GetMapping
    public List<EventFullDto> searchEvents(@Valid @ModelAttribute SearchEventParamPublic searchEventParamPublic, HttpServletRequest request) {
        log.info("/events/GET/searchEvents");
        List<EventFullDto> events = eventService.getAllEventPublic(searchEventParamPublic);

        StatDto statDto = new StatDto(applicationName, null, new HashSet<>(), request.getRemoteAddr(), LocalDateTime.now());

        for (EventFullDto event : events) {
            statDto.getUris().add("/events/" + event.getId());
        }

        statClient.addStatEvent(statDto);

        return events;
    }

    @GetMapping("/{id}")
    public EventFullDto getPrivateEvent(@PathVariable @Min(1) Long id, HttpServletRequest request) throws JsonProcessingException {
        log.info("Get request by id = {}", id);

        EventFullDto event = eventService.getEvent(id);

        statClient.addStatEvent(new StatDto(applicationName, "/events/" + id, null, request.getRemoteAddr(), LocalDateTime.now()));

        return event;
    }
}
