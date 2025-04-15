package ru.practicum.ewm.compilation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.CompilationRequestDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequestDto;
import ru.practicum.ewm.compilation.service.CompilationService;

@RestController
@RequestMapping(path = "/admin/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCompilationController {
    private final CompilationService compilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto addCompilation(@RequestBody @Valid CompilationRequestDto compilationRequestDto) {
        log.info("POST /admin/compilations with body({})", compilationRequestDto);
        return compilationService.addCompilation(compilationRequestDto);
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(@PathVariable(name = "compId") Long compilationId,
                                            @RequestBody @Valid UpdateCompilationRequestDto compilationRequestDto) {
        log.info("PATCH /admin/compilations with body({})", compilationRequestDto);
        return compilationService.updateCompilation(compilationId, compilationRequestDto);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "compId") Long compilationId) {
        log.info("DELETE /admin/compilations/{compId} compilationId = {})", compilationId);
        compilationService.delete(compilationId);
    }
}