package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventAdminFilterParamsDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequestDto;
import ru.practicum.ewm.event.facade.EventFacade;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/events")
public class AdminEventController {
    private final EventFacade eventFacade;

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PathVariable Long eventId,
                               @Valid @RequestBody UpdateEventAdminRequestDto updateEventAdminRequestDto) {
        return eventFacade.update(eventId, updateEventAdminRequestDto);
    }

    @GetMapping
    public List<EventFullDto> get(@Valid EventAdminFilterParamsDto filters,
                                  @PositiveOrZero @RequestParam(defaultValue = "0") int from,
                                  @Positive @RequestParam(defaultValue = "10") int size) {
        return eventFacade.get(filters, from, size);
    }
}