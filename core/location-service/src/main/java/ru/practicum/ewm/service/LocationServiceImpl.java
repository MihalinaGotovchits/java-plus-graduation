package ru.practicum.ewm.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.EventClient;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.error.exception.ConflictDataException;
import ru.practicum.ewm.error.exception.NotFoundException;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.dto.location.NewLocationDto;
import ru.practicum.ewm.dto.location.UpdateLocationAdminRequestDto;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.repository.LocationRepository;
import ru.practicum.ewm.util.PagingUtil;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {
    LocationRepository locationRepository;
    LocationMapper locationMapper;
    EventClient eventClient;

    @Override
    public List<LocationDto> getLocations(Integer from, Integer size) {
        log.info("start getLocations by from {} size {}", from, size);
        return locationRepository.findAll(PagingUtil.pageOf(from, size)).stream()
                .map(locationMapper::toDto).toList();
    }

    @Override
    public LocationDto getById(Long locationId) {
        log.info("getById params: id = {}", locationId);
        Location location = locationRepository.findById(locationId).orElseThrow(() -> new NotFoundException(
                String.format("Локация с ид %s не найдена", locationId))
        );
        log.info("getById result location = {}", location);
        return locationMapper.toDto(location);
    }

    @Override
    @Transactional
    public LocationDto addLocation(NewLocationDto newLocationDto) {
        Location location = locationRepository.save(locationMapper.toLocation(newLocationDto));
        log.info("Location is created: {}", location);
        return locationMapper.toDto(location);
    }

    @Override
    @Transactional
    public LocationDto updateLocation(Long locationId, UpdateLocationAdminRequestDto updateLocationAdminRequestDto) {
        log.info("start updateLocation");
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location with id " + locationId + " not found"));
        location = locationRepository.save(locationMapper.update(location, updateLocationAdminRequestDto));
        log.info("Location is updated: {}", location);
        return locationMapper.toDto(location);
    }

    @Override
    @Transactional
    public LocationDto addOrGetLocation(NewLocationDto newLocationDto) {
        Location location = newLocationDto == null ? null : locationRepository.findByLatAndLon(newLocationDto.getLat(), newLocationDto.getLon())
                .orElseGet(() -> locationRepository.save(locationMapper.toLocation(newLocationDto)));

        return locationMapper.toDto(location);
    }

    @Override
    public List<LocationDto> getByRadius(Double lat, Double lon, Double radius) {
        log.info("call getByRadius for lat = {}, lon = {}, radius = {}", lat, lon, radius);
        List<LocationDto> locationDtos = locationRepository.findAllByRadius(lat, lon, radius)
                .stream()
                .map(locationMapper::toDto)
                .toList();
        log.info("result locations: {}", locationDtos);
        return locationDtos;
    }

    @Override
    @Transactional
    public void delete(Long locationId) {
        List<EventFullDto> events = eventClient.getByLocation(locationId);
        if (!events.isEmpty()) {
            throw new ConflictDataException("location is used in events");
        }
        locationRepository.deleteById(locationId);
        log.info("Location deleted with id: {}", locationId);
    }
}
