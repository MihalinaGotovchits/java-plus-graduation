package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.user.UserRequestDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.dto.user.UserDto;

import java.util.List;

public interface UserService {

    UserShortDto getById(Long userId);

    List<UserShortDto> getUsers(List<Long> ids);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    UserDto registerUser(UserRequestDto userRequestDto);

    void delete(Long userId);
}