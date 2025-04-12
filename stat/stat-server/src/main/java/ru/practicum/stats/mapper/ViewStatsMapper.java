package ru.practicum.stats.mapper;

import org.mapstruct.Mapper;
import ru.practicum.stats.dto.StatsDto;
import ru.practicum.stats.projection.ViewStatsProjection;

@Mapper(componentModel = "spring")
public interface ViewStatsMapper {

    StatsDto toDto(ViewStatsProjection projection);
}
