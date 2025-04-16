package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventStates;
import ru.practicum.ewm.dto.request.ParticipationRequestCountDto;
import ru.practicum.ewm.error.exception.ConflictDataException;
import ru.practicum.ewm.error.exception.NotFoundException;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.dto.request.ParticipationRequestStatus;
import ru.practicum.ewm.repository.ParticipationRequestRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository participationRequestRepository;
    private final ParticipationRequestMapper participationRequestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, EventFullDto event) {
        if (!event.getState().equals(EventStates.PUBLISHED))
            throw new ConflictDataException("On part. request create - " +
                    "Event isn't published with id: " + event.getId());


        if (event.getInitiator().getId().equals(userId))
            throw new ConflictDataException(
                    String.format("On part. request create - " +
                            "Event with id %s has Requester with id %s as an initiator: ", event.getId(), userId));

        if (participationRequestRepository.existsByRequesterIdAndEventId(userId, event.getId()))
            throw new ConflictDataException(
                    String.format("On part. request create - " +
                            "Request by Requester with id %s and Event with id %s already exists: ", event.getId(), userId));

        if (event.getParticipantLimit() != 0) {
            long requestsCount = participationRequestRepository.countByEventIdAndStatusIn(event.getId(),
                    List.of(ParticipationRequestStatus.CONFIRMED));
            if (requestsCount >= event.getParticipantLimit())
                throw new ConflictDataException(
                        String.format("On part. request create - " +
                                        "Event with id %s reached the limit of participants and User with id %s can't apply: ",
                                event.getId(), userId));
        }

        ParticipationRequest createdParticipationRequest = participationRequestRepository.save(
                ParticipationRequest.builder()
                        .requesterId(userId)
                        .eventId(event.getId())
                        .status(event.getParticipantLimit() != 0 && event.getRequestModeration() ?
                                ParticipationRequestStatus.PENDING : ParticipationRequestStatus.CONFIRMED)
                        .build()
        );
        log.info("Participation request is created: {}", createdParticipationRequest);
        return participationRequestMapper.toDto(createdParticipationRequest);
    }

    @Override
    public List<ParticipationRequestDto> get(Long userId) {
        List<ParticipationRequest> participationRequests = participationRequestRepository.findByRequesterId(userId);
        log.trace("Participation requests are requested by user with id {}", userId);
        return participationRequestMapper.toDto(participationRequests);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest participationRequest = participationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("On part. request cancel - Request doesn't exist with id: " + requestId));

        if (!participationRequest.getRequesterId().equals(userId))
            throw new NotFoundException(String.format("On part. request cancel - " +
                    "Request with id %s can't be canceled by not owner with id %s: ", requestId, userId));

        participationRequest.setStatus(ParticipationRequestStatus.CANCELED);
        participationRequest = participationRequestRepository.save(participationRequest);
        log.info("Participation request is canceled: {}", participationRequest);
        return participationRequestMapper.toDto(participationRequest);
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventIdAndStatus(Long eventId, ParticipationRequestStatus status) {
        return participationRequestRepository.findAllByEventIdAndStatus(eventId, status)
                .stream()
                .map(participationRequestMapper::toDto)
                .toList();
    }

    @Override
    public List<ParticipationRequestDto> getByIds(List<Long> ids) {
        return participationRequestRepository.findAllById(ids)
                .stream()
                .map(participationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public List<ParticipationRequestDto> updateStatus(ParticipationRequestStatus status, List<Long> ids) {
        List<ParticipationRequest> requests = participationRequestRepository.findAllById(ids);

        if (status == ParticipationRequestStatus.REJECTED &&
                requests.stream().anyMatch(request -> request.getStatus() == ParticipationRequestStatus.CONFIRMED)) {
            throw new ConflictDataException("Среди заявок уже есть подтвержденные");
        }

        requests.forEach(request -> request.setStatus(status));
        List<ParticipationRequest> updatedRequests = participationRequestRepository.saveAll(requests);
        return updatedRequests
                .stream()
                .map(participationRequestMapper::toDto)
                .toList();
    }

    @Override
    public List<ParticipationRequestCountDto> getConfirmedCount(List<Long> ids) {
        return participationRequestRepository.getParticipationRequestCountConfirmed(ids)
                .stream()
                .map(participationRequestMapper::toDto)
                .toList();
    }

}