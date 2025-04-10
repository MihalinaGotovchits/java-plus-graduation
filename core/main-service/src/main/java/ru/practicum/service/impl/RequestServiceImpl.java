package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.DataIntegrityViolationException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.Request;
import ru.practicum.model.User;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.RequestService;
import ru.practicum.status.event.State;
import ru.practicum.status.request.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final UserRepository userRepository;

    private final RequestRepository requestRepository;

    private final EventRepository eventRepository;

    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%d not found", userId)));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));

        requestRepository.getRequestByRequesterIdAndEventId(userId, eventId)
                .ifPresent(result -> {
                    throw new ConflictException(String.format("Request with userId=%d and eventId=%d", userId, eventId));
                });

        if (event.getInitiator().getId().equals(userId)) {
            throw new DataIntegrityViolationException(
                    "The event initiator cannot add a request to participate in his event.");
        }

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new DataIntegrityViolationException(
                    "You can't participate in an unpublished event.");
        }

        if (event.getParticipantLimit() <= requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.CONFIRMED).size()
            && event.getParticipantLimit() != 0) {
            throw new DataIntegrityViolationException(
                    "The event has reached its participation request limit.");
        }

        Request request = Request.builder()
                .requester(user)
                .event(event)
                .created(LocalDateTime.now())
                .status((event.getParticipantLimit() == 0) ? RequestStatus.CONFIRMED : RequestStatus.PENDING)
                .build();

        if (Boolean.FALSE.equals(event.getRequestModeration())) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        ParticipationRequestDto requestDto = RequestMapper.toParticipationRequestDto(requestRepository.save(request));
        log.info("createRequest - request save successfully - {}", request);
        return requestDto;
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long requestId) {
        List<ParticipationRequestDto> dtos = requestRepository.getRequestsByRequesterId(requestId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
        log.info("getRequests - successfully");
        return dtos;
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Request with id=%d not found", requestId)));

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequestDto requestDto = RequestMapper.toParticipationRequestDto(requestRepository.save(request));
        log.info("cancelRequest - successfully - {}", request);
        return requestDto;
    }
}
