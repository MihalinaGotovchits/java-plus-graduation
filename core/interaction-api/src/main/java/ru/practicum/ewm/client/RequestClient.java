package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.ParticipationRequestCountDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.ParticipationRequestStatus;

import java.util.List;

@FeignClient(name = "request-service", path = "/internal/api/requests")
public interface RequestClient {
    @GetMapping("/search")
    List<ParticipationRequestDto> getByStatus(@RequestParam(name = "eventId") Long eventId,
                                              @RequestParam(name = "status") ParticipationRequestStatus status);

    @GetMapping("/search/all")
    List<ParticipationRequestDto> getByIds(@RequestParam(name = "id") List<Long> ids);

    @GetMapping
    List<ParticipationRequestCountDto> getConfirmedCount(@RequestParam(name = "eventId") List<Long> ids);

    @PostMapping
    List<ParticipationRequestDto> updateStatus(
            @RequestParam(name = "status") ParticipationRequestStatus status,
            @RequestBody List<Long> ids);
}
