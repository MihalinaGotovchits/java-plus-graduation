package ru.practicum.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.SearchEventParamAdmin;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EventAdminController {
    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> searchEvents(@Valid @ModelAttribute SearchEventParamAdmin searchEventParamAdmin) {
        log.info("/admin/events/GET/searchEvents");
        return eventService.getAllEventFromAdmin(searchEventParamAdmin);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByAdmin(@PathVariable(value = "eventId") @Min(1) Long eventId,
                                           @RequestBody @Valid UpdateEventAdminRequest inputEventUpdate) {
        log.info("/admin/events/PATCH/updateEventByAdmin");
        return eventService.updateEventFromAdmin(eventId, inputEventUpdate);
    }
}
