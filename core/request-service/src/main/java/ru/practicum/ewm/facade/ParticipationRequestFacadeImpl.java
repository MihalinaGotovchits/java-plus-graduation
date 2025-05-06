package ru.practicum.ewm.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.EventClient;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.request.ParticipationRequestCountDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.ParticipationRequestStatus;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.error.exception.NotFoundException;
import ru.practicum.ewm.service.ParticipationRequestService;
import ru.practicum.grpc.stat.action.ActionTypeProto;
import ru.practicum.stats.client.StatClient;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipationRequestFacadeImpl implements ParticipationRequestFacade {
    private final EventClient eventClient;
    private final UserClient userClient;
    private final StatClient statClient;
    private final ParticipationRequestService requestService;

    @Override
    public ParticipationRequestDto create(Long userId, Long eventId) {
        checkAndGetUserById(userId);
        EventFullDto eventFullDto = checkAndGetEventById(eventId);

        ParticipationRequestDto participationRequestDto = requestService.create(userId, eventFullDto);
        statClient.registerUserAction(eventId, userId, ActionTypeProto.ACTION_REGISTER, Instant.now());

        return participationRequestDto;
    }

    @Override
    public List<ParticipationRequestDto> get(Long userId) {
        checkAndGetUserById(userId);
        return requestService.get(userId);
    }

    @Override
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        checkAndGetUserById(userId);
        return requestService.cancel(userId, requestId);
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventIdAndStatus(Long eventId, ParticipationRequestStatus status) {
        return requestService.findAllByEventIdAndStatus(eventId, status);
    }

    @Override
    public List<ParticipationRequestDto> getByIds(List<Long> ids) {
        return requestService.getByIds(ids);
    }

    @Override
    public List<ParticipationRequestDto> updateStatus(ParticipationRequestStatus status, List<Long> ids) {
        return requestService.updateStatus(status, ids);
    }

    @Override
    public List<ParticipationRequestCountDto> getConfirmedCount(List<Long> ids) {
        return requestService.getConfirmedCount(ids);
    }

    private UserShortDto checkAndGetUserById(Long userId) {
        UserShortDto user = userClient.getById(userId);
        if (user == null) {
            throw new NotFoundException("Такого пользователя не существует: " + userId);
        }

        return user;
    }

    private EventFullDto checkAndGetEventById(Long eventId) {
        EventFullDto event = eventClient.getById(eventId);
        if (event == null) {
            throw new NotFoundException("Такого события не существует: " + eventId);
        }

        return event;
    }
}
