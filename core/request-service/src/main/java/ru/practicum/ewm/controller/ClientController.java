package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.client.RequestClient;
import ru.practicum.ewm.dto.request.ParticipationRequestCountDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.ParticipationRequestStatus;
import ru.practicum.ewm.facade.ParticipationRequestFacade;

import java.util.List;

@RestController
@RequestMapping(path = "/internal/api/requests")
@RequiredArgsConstructor
public class ClientController implements RequestClient {
    private final ParticipationRequestFacade requestFacade;

    @Override
    public List<ParticipationRequestDto> getByStatus(Long eventId, ParticipationRequestStatus status) {
        return requestFacade.findAllByEventIdAndStatus(eventId, status);
    }

    @Override
    public List<ParticipationRequestDto> getByIds(List<Long> ids) {
        return requestFacade.getByIds(ids);
    }

    @Override
    public List<ParticipationRequestCountDto> getConfirmedCount(List<Long> ids) {
        return requestFacade.getConfirmedCount(ids);
    }

    @Override
    public List<ParticipationRequestDto> updateStatus(ParticipationRequestStatus status, List<Long> ids) {
        return requestFacade.updateStatus(status, ids);
    }
}
