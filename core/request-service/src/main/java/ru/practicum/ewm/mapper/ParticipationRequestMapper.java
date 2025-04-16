package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.ewm.dto.request.ParticipationRequestCountDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.ParticipationRequestCount;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ParticipationRequestMapper {
    @Mapping(target = "requester", source = "requesterId")
    @Mapping(target = "event", source = "eventId")
    ParticipationRequestDto toDto(ParticipationRequest participationRequest);

    List<ParticipationRequestDto> toDto(List<ParticipationRequest> participationRequest);

    ParticipationRequestCountDto toDto(ParticipationRequestCount requestCount);
}