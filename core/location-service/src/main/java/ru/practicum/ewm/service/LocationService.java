package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.dto.location.NewLocationDto;
import ru.practicum.ewm.dto.location.UpdateLocationAdminRequestDto;

import java.util.List;

public interface LocationService {

    List<LocationDto> getLocations(Integer from, Integer size);

    LocationDto getById(Long locationId);

    LocationDto addLocation(NewLocationDto newLocationDto);

    LocationDto updateLocation(Long locationId, UpdateLocationAdminRequestDto updateLocationAdminRequestDto);

    LocationDto addOrGetLocation(NewLocationDto newLocationDto);

    List<LocationDto> getByRadius(Double lat, Double lon, Double radius);

    void delete(Long locationId);
}
