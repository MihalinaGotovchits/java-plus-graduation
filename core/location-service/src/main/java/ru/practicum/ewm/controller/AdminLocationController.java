package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.dto.location.NewLocationDto;
import ru.practicum.ewm.dto.location.UpdateLocationAdminRequestDto;
import ru.practicum.ewm.service.LocationService;

@RestController
@RequestMapping(path = "/admin/locations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminLocationController {
    private final LocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto addLocation(@RequestBody @Valid NewLocationDto newLocationDto) {
        log.info("POST /admin/locations with body({})", newLocationDto);
        return locationService.addLocation(newLocationDto);
    }

    @PatchMapping("/{locationId}")
    public LocationDto updateLocation(@PathVariable(name = "locationId") Long locationId,
                                      @RequestBody @Valid UpdateLocationAdminRequestDto updateLocationAdminRequestDto) {
        log.info("PATCH /admin/locations with body({})", updateLocationAdminRequestDto);
        return locationService.updateLocation(locationId, updateLocationAdminRequestDto);
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "locationId") Long locationId) {
        log.info("DELETE /admin/locations/{locationId} locationId = {})", locationId);
        locationService.delete(locationId);
    }
}
