package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.UncorrectedParametersException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.service.CompilationService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto compilationDto) {
        Compilation compilation = CompilationMapper.toCompilation(compilationDto);
        compilation.setPinned(Optional.ofNullable(compilation.getPinned()).orElse(false));

        Set<Long> compEventsId = (compilationDto.getEvents() != null) ? compilationDto.getEvents() : Collections.emptySet();
        List<Long> eventIds = new ArrayList<>(compEventsId);
        List<Event> events = eventRepository.findAllByIdIn(eventIds);
        Set<Event> eventSet = new HashSet<>(events);
        compilation.setEvents(eventSet);

        Compilation compilationAfterSave = compilationRepository.save(compilation);
        return CompilationMapper.toCompilationDto(compilationAfterSave);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateComp) {
        Compilation compilation = checkCompilation(compId);

        List<Long> eventIds = updateComp.getEvents();

        if (eventIds != null) {
            List<Event> events = eventRepository.findAllByIdIn(new ArrayList<>(eventIds));
            Set<Event> eventSet = new HashSet<>(events);
            compilation.setEvents(eventSet);
        }

        compilation.setPinned(Optional.ofNullable(updateComp.getPinned()).orElse(compilation.getPinned()));
        if (compilation.getTitle().isBlank()) {
            throw new UncorrectedParametersException("Заголовок не может быть пустым");
        }
        compilation.setTitle(Optional.ofNullable(updateComp.getTitle()).orElse(compilation.getTitle()));
        return CompilationMapper.toCompilationDto(compilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        checkCompilation(compId);
        compilationRepository.deleteById(compId);
    }

    private Compilation checkCompilation(Long compId) {
        return compilationRepository.findById(compId).orElseThrow(
                () -> new NotFoundException("Compilation с Id: " + compId + " не найден")
        );
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {

        PageRequest pageRequest = PageRequest.of(from, size);
        List<Compilation> compilations;
        if (pinned == null) {
            compilations = compilationRepository.findAll(pageRequest).getContent();
        } else {
            compilations = compilationRepository.findAllByPinned(pinned, pageRequest);
        }

        return compilations.stream()
                .map(CompilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto findByIdCompilation(Long compId) {
        return CompilationMapper.toCompilationDto(checkCompilation(compId));
    }
}
