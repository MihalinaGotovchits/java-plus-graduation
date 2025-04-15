package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.StatsDto;
import ru.practicum.stats.error.ValidationException;
import ru.practicum.stats.mapper.EndpointHitMapper;
import ru.practicum.stats.mapper.ViewStatsMapper;
import ru.practicum.stats.model.Stats;
import ru.practicum.stats.projection.ViewStatsProjection;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final EndpointHitMapper endpointHitMapper;
    private final ViewStatsMapper viewStatsMapper;

    @Override
    public void saveHit(HitDto hitDto) {
        Stats stats = endpointHitMapper.toEntity(hitDto);
        statsRepository.save(stats);
    }

    @Override
    public List<StatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        List<ViewStatsProjection> projections;

        if (end.isBefore(start)) {
            throw new ValidationException(String.format("End date %s is before start date %s", end, start));
        }

        if (uris != null && !uris.isEmpty()) {
            if (unique) {
                projections = statsRepository.getUniqueStatsWithHitsAndUris(start, end, uris);
            } else {
                projections = statsRepository.getStatsWithHitsAndUris(start, end, uris);
            }
        } else {
            if (unique) {
                projections = statsRepository.getUniqueStatsWithHits(start, end);
            } else {
                projections = statsRepository.getStatsWithHits(start, end);
            }
        }

        return projections.stream()
                .map(viewStatsMapper::toDto)
                .collect(Collectors.toList());
    }
}
