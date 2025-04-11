package ru.practicum.service;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest user);

    List<UserDto> getListUsers(List<Long> userIds, Integer from, Integer size);

    void deleteUserById(Long userId);
}