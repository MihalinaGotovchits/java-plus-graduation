package ru.practicum.service;

import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequests(Long requestId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}
