package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.*;
import ru.practicum.model.*;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class EventMapper {

    public Event toEvent(NewEventDto eventDto, User user, Location location, Category category) {
        return Event.builder()
                .eventDate(eventDto.getEventDate())
                .annotation(eventDto.getAnnotation())
                .category(category)
                .paid(eventDto.getPaid())
                .location(location)
                .participantLimit(eventDto.getParticipantLimit())
                .description(eventDto.getDescription())
                .title(eventDto.getTitle())
                .createOn(LocalDateTime.now())
                .initiator(user)
                .requestModeration(eventDto.getRequestModeration())
                .build();
    }

    public EventFullDto toEventFullDto(Event event) {
        EventFullDto build = EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .createdOn(event.getCreateOn())
                .initiator(UserMapper.userShortDto(event.getInitiator()))
                .confirmedRequests(event.getConfirmedRequests())
                .views((event.getViews() != null) ? event.getViews() : 0)
                .state(event.getState())
                .annotation(event.getAnnotation())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .paid(event.getPaid())
                .category((event.getCategory() == null) ? new CategoryDto() : CategoryMapper.toCategoryDto(event.getCategory()))
                .eventDate(event.getEventDate())
                .location(LocationMapper.toLocationDto(event.getLocation()))
                .build();

        if (event.getPublishedOn() != null) {
            build.setPublishedOn(event.getPublishedOn());
        }
        return build;
    }

    public EventFullDto toEventFullDto(Event event, List<Request> requests) {
        EventFullDto build = EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .createdOn(event.getCreateOn())
                .initiator(UserMapper.userShortDto(event.getInitiator()))
                .confirmedRequests(event.getConfirmedRequests())
                .views((event.getViews() != null) ? event.getViews() : 0)
                .state(event.getState())
                .annotation(event.getAnnotation())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .paid(event.getPaid())
                .category((event.getCategory() == null) ? new CategoryDto() : CategoryMapper.toCategoryDto(event.getCategory()))
                .eventDate(event.getEventDate())
                .location(LocationMapper.toLocationDto(event.getLocation()))
                .confirmedRequests(requests.size())
                .build();

        if (event.getPublishedOn() != null) {
            build.setPublishedOn(event.getPublishedOn());
        }
        return build;
    }

    public EventShortDto toEventShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category((event.getCategory() == null) ? new CategoryDto() : CategoryMapper.toCategoryDto(event.getCategory()))
                .eventDate(event.getEventDate())
                .initiator(UserMapper.userShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .build();
    }

    public NewEventDto toNewEventDto(UpdateEventAdminRequest adminRequest) {
        return NewEventDto.builder()
                .annotation(adminRequest.getAnnotation())
                .category(adminRequest.getCategory())
                .description(adminRequest.getDescription())
                .eventDate(adminRequest.getEventDate())
                .location((adminRequest.getLocation() == null) ? null : LocationMapper.toLocationDto(adminRequest.getLocation()))
                .paid(adminRequest.getPaid())
                .participantLimit(adminRequest.getParticipantLimit())
                .requestModeration(adminRequest.getRequestModeration())
                .title(adminRequest.getTitle())
                .build();
    }

    public NewEventDto toNewEventDto(UpdateEventUserRequest userRequest) {
        return NewEventDto.builder()
                .annotation(userRequest.getAnnotation())
                .category(userRequest.getCategory())
                .description(userRequest.getDescription())
                .eventDate(userRequest.getEventDate())
                .location((userRequest.getLocation() == null) ? null : LocationMapper.toLocationDto(userRequest.getLocation()))
                .paid(userRequest.getPaid())
                .participantLimit(userRequest.getParticipantLimit())
                .requestModeration(userRequest.getRequestModeration())
                .title(userRequest.getTitle())
                .build();
    }
}
