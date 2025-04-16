package ru.practicum.ewm.event.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.categories.model.Category;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.event.model.Event;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Named(value = "EventShortDto")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "id", source = "event.id")
    EventShortDto toShortDto(Event event, UserShortDto initiator);

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "initiator", source = "initiator")
    EventFullDto toFullDto(Event event, LocationDto location, UserShortDto initiator);

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    EventFullDto toFullDto(Event event);

    List<EventFullDto> toFullDto(Iterable<Event> event);

    List<EventShortDto> toEventShortDtoList(Iterable<Event> events);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiatorId", source = "userId")
    @Mapping(target = "locationId", source = "locationId")
    @Mapping(target = "participantLimit", defaultValue = "0")
    @Mapping(target = "paid", defaultValue = "false")
    @Mapping(target = "requestModeration", defaultValue = "true")
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    Event toEvent(NewEventDto newEventDto, Category category, Long userId, Long locationId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "locationId", source = "locationId")
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Event update(@MappingTarget Event event, UpdateEventUserRequestDto eventUpdateDto, Long locationId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "locationId", source = "locationId")
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Event update(@MappingTarget Event event, UpdateEventAdminRequestDto eventUpdateDto, Category category, Long locationId);
}