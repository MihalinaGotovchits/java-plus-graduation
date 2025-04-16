package ru.practicum.ewm.facade;

import ru.practicum.ewm.dto.request.ParticipationRequestCountDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.ParticipationRequestStatus;

import java.util.List;

public interface ParticipationRequestFacade {
    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> get(Long userId);

    ParticipationRequestDto cancel(Long userId, Long requestId);

    List<ParticipationRequestDto> findAllByEventIdAndStatus(Long eventId, ParticipationRequestStatus status);

    List<ParticipationRequestDto> getByIds(List<Long> ids);

    List<ParticipationRequestDto> updateStatus(ParticipationRequestStatus status, List<Long> ids);

    List<ParticipationRequestCountDto> getConfirmedCount(List<Long> ids);
}
