package ru.practicum.ewm.compilation.service;

import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.CompilationRequestDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequestDto;

import java.util.List;

public interface CompilationService {

    List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size);

    CompilationDto getById(Long compilationId);

    CompilationDto addCompilation(CompilationRequestDto compilationRequestDto);

    CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequestDto compilationRequestDto);

    void delete(Long compilationId);
}
