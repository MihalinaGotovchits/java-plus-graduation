package ru.practicum.ewm.compilation.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.CompilationRequestDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequestDto;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.error.exception.NotFoundException;
import ru.practicum.ewm.util.PagingUtil;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    CompilationRepository compilationRepository;
    EventRepository eventRepository;
    CompilationMapper compilationMapper;
    EventMapper eventMapper;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        log.info("getCompilations params: pinned = {}, from = {}, size = {}", pinned, from, size);
        PageRequest page = PagingUtil.pageOf(from, size);

        return compilationRepository.findAllByPinned(pinned, page)
                .map(compilation -> compilationMapper.toDto(compilation, eventMapper.toEventShortDtoList(compilation.getEvents())))
                .getContent();
    }

    @Override
    public CompilationDto getById(Long compilationId) {
        log.info("getById params: id = {}", compilationId);
        Compilation compilation = compilationRepository.findById(compilationId).orElseThrow(() -> new NotFoundException(
                String.format("Подборка с ид %s не найдена", compilationId))
        );
        log.info("getById result compilation = {}", compilation);
        return compilationMapper.toDto(compilation, eventMapper.toEventShortDtoList(compilation.getEvents()));
    }

    @Override
    @Transactional
    public CompilationDto addCompilation(CompilationRequestDto compilationRequestDto) {
        log.info("addCompilation params: compilationRequestDto = {}", compilationRequestDto);

        List<Event> events = getAndCheckEventList(compilationRequestDto.getEvents());
        Compilation compilation = compilationRepository.save(compilationMapper.toEntity(compilationRequestDto, events));
        log.info("addCompilation result compilation = {}", compilation);
        return compilationMapper.toDto(compilation, eventMapper.toEventShortDtoList(compilation.getEvents()));
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequestDto compilationRequestDto) {
        log.info("update params: compilationId = {}, compilationRequestDto = {}", compilationId, compilationRequestDto);
        Compilation compilation = compilationRepository.findById(compilationId).orElseThrow(() -> new NotFoundException(
                String.format("Подборка с ид %s не найдена", compilationId))
        );
        List<Event> events = getAndCheckEventList(compilationRequestDto.getEvents());
        compilationMapper.update(compilationRequestDto, compilationId, events, compilation);
        compilation = compilationRepository.save(compilation);
        log.info("addCompilation result compilation = {}", compilation);

        return compilationMapper.toDto(compilation, eventMapper.toEventShortDtoList(compilation.getEvents()));
    }

    @Override
    @Transactional
    public void delete(Long compilationId) {
        log.info("delete params: compilationId = {}", compilationId);
        compilationRepository.deleteById(compilationId);
    }

    private List<Event> getAndCheckEventList(List<Long> eventIds) {
        log.info("getAndCheckEventList params: eventIds = {}", eventIds);
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<Event> events = eventRepository.findAllById(eventIds);
            log.info("getAndCheckEventList result: events = {}", events);
            if (events.size() != eventIds.size()) {
                throw new NotFoundException("Некорректный список событий");
            }

            return events;
        }
    }
}
