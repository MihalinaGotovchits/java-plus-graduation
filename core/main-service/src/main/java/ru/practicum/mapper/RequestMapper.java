package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.UpdatedRequestsDto;
import ru.practicum.model.Request;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class RequestMapper {

    public ParticipationRequestDto toParticipationRequestDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .requester(request.getRequester().getId())
                .event(request.getEvent().getId())
                .status(request.getStatus())
                .build();
    }

    public List<ParticipationRequestDto> toListRequestDto(List<Request> requests) {
        return requests.stream().map(RequestMapper::toParticipationRequestDto).collect(Collectors.toList());
    }

    public UpdatedRequestsDto toUpdatedRequestsDto(List<ParticipationRequestDto> confirmedRequests,
                                                   List<ParticipationRequestDto> rejectedRequests) {
        return UpdatedRequestsDto.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }
}
