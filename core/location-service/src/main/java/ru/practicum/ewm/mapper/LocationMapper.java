package ru.practicum.ewm.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.dto.location.NewLocationDto;
import ru.practicum.ewm.dto.location.UpdateLocationAdminRequestDto;
import ru.practicum.ewm.model.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    LocationDto toDto(Location location);

    @Mapping(target = "id", ignore = true)
    Location toLocation(NewLocationDto newLocationDto);

    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Location update(@MappingTarget Location location, UpdateLocationAdminRequestDto updateLocationAdminRequestDto);
}