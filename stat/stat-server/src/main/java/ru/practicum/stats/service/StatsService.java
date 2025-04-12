package ru.practicum.stats.service;

import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.StatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    void saveHit(HitDto hitDTO);

    List<StatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
