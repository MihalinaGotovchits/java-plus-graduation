package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest user) {
        log.info("createUser - invoked");
        if (userRepository.existsByEmail(user.getEmail())) {

            throw new ConflictException("Email уже занят");

        }
        User user1 = userRepository.save(UserMapper.toUser(user));
        log.info("createUser - user save successfully - {}", user);
        return UserMapper.userDto(user1);
    }

    @Override
    public List<UserDto> getListUsers(List<Long> userIds, Integer from, Integer size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable = PageRequest.of(from, size, sort);
        log.info("getAllUsers - successfully");
        return (userIds != null) ? userRepository.findByIdIn(userIds, pageable).stream()
                .map(UserMapper::userDto).collect(Collectors.toList()) : userRepository.findAll(pageable)
                .stream().map(UserMapper::userDto).collect(Collectors.toList());
    }

    @Override
    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id: " + userId + " не найден");
        }
        log.info("deleteUserById - successfully - {}", userId);
        userRepository.deleteById(userId);
    }
}