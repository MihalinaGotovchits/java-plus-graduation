package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.dto.event.EventFullDto;

import java.util.List;

@FeignClient(name = "event-service", path = "/internal/api/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventFullDto getById(@PathVariable Long eventId);

    @GetMapping("/location/{locationId}")
    List<EventFullDto> getByLocation(@PathVariable Long locationId);
}
