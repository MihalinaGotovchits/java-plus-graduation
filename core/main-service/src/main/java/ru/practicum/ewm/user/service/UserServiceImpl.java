package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.core.error.exception.NotFoundException;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.dto.UserRequestDto;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserShortDto getById(Long userId) {
        log.info("getById params: id = {}", userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException(
                String.format("Пользователь с ид %s не найден", userId))
        );
        log.info("getById result user = {}", user);
        return userMapper.toShortDto(user);
    }

    @Override
    public List<UserShortDto> getUsers(List<Long> ids) {
        log.info("getUsers params: ids = {}", ids);
        return userRepository.findAllById(ids)
                .stream()
                .map(userMapper::toShortDto)
                .toList();
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        log.info("getUsers params: ids = {}, from = {}, size = {}", ids, from, size);
        PageRequest page = PageRequest.of(from > 0 ? from / size : 0, size);

        if (ids == null || ids.isEmpty()) {
            log.info("getUsers call: findAll");
            return userRepository.findAll(page)
                    .map(userMapper::toDto)
                    .getContent();
        }
        log.info("getUsers call: findAllByIdIn");
        return userRepository.findAllByIdIn(ids, page)
                .map(userMapper::toDto)
                .getContent();
    }

    @Override
    @Transactional
    public UserDto registerUser(UserRequestDto userRequestDto) {
        log.info("registerUser params: userRequestDto = {}", userRequestDto);
        User user = userRepository.save(userMapper.toEntity(userRequestDto));
        log.info("registerUser result user = {}", user);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        log.info("delete params: userId = {}", userId);
        userRepository.deleteById(userId);
    }
}
