package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/internal/api/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserShortDto getById(@PathVariable  Long userId);

    @GetMapping
    List<UserShortDto> getByIds(@RequestParam(name = "id") List<Long> ids);
}
