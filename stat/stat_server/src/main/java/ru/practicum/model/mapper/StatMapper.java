package ru.practicum.model.mapper;

import ru.practicum.dto.StatDto;
import ru.practicum.model.Stat;

import java.util.ArrayList;
import java.util.List;

public class StatMapper {
    public static Stat toStat(StatDto statDto) {
        return Stat.builder()
                .ip(statDto.getIp())
                .app(statDto.getApp())
                .uri(statDto.getUri())
                .timestamp(statDto.getTimestamp())
                .build();
    }

    public static StatDto toStatDto(Stat stat) {
        return StatDto.builder()
                .app(stat.getApp())
                .ip(stat.getIp())
                .uri(stat.getUri())
                .timestamp(stat.getTimestamp())
                .build();
    }

    public static List<Stat> toStats(StatDto statDto) {
        List<Stat> stats = new ArrayList<>();

        for (String uri : statDto.getUris()) {
            Stat stat = new Stat();
            stat.setUri(uri);
            stat.setApp(statDto.getApp());
            stat.setIp(statDto.getIp());
            stat.setTimestamp(statDto.getTimestamp());
            stats.add(stat);
        }

        return stats;
    }
}
