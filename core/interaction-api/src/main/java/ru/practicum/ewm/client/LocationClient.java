package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.dto.location.NewLocationDto;

import java.util.List;

@FeignClient(name = "location-service", path = "/internal/api/locations")
public interface LocationClient {

    @PostMapping
    LocationDto addOrGetLocation(@RequestBody NewLocationDto newLocationDto);

    @GetMapping
    List<LocationDto> getByRadius(@RequestParam(name = "latitude") Double lat,
                                  @RequestParam(name = "longitude") Double lon,
                                  @RequestParam(name = "radius") Double radius);

    @GetMapping("/{locationId}")
    LocationDto getById(@PathVariable Long locationId);
}
