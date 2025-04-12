package ru.practicum.stats.client;

import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.StatsDto;
import ru.practicum.stats.dto.StatsRequestParamsDto;

import java.util.List;

public interface StatClient {
    void hit(HitDto hitDto);

    List<StatsDto> get(StatsRequestParamsDto statsRequestParamsDto);
}