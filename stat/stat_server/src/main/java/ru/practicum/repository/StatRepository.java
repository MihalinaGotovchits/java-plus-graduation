package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.dto.StatResponseDto;
import ru.practicum.model.Stat;

import java.time.LocalDateTime;
import java.util.List;

public interface StatRepository extends JpaRepository<Stat, Long> {
    @Query("SELECT new ru.practicum.dto.StatResponseDto(stat.app, stat.uri, count(distinct stat.ip)) " +
            "FROM Stat AS stat " +
            "WHERE stat.timestamp BETWEEN ?1 AND ?2 " +
            "GROUP BY stat.app, stat.uri " +
            "ORDER BY count(distinct stat.ip) desc")
    List<StatResponseDto> findAllByTimestampBetweenStartAndEndWithUniqueIp(LocalDateTime start, LocalDateTime end);

    @Query("SELECT new ru.practicum.dto.StatResponseDto(stat.app, stat.uri, count(stat.ip)) " +
            "FROM Stat AS stat " +
            "WHERE stat.timestamp BETWEEN ?1 AND ?2 " +
            "GROUP BY stat.app, stat.uri " +
            "ORDER BY count(stat.ip) desc ")
    List<StatResponseDto> findAllByTimestampBetweenStartAndEndWhereIpNotUnique(LocalDateTime start, LocalDateTime end);

    @Query("SELECT new ru.practicum.dto.StatResponseDto(stat.app, stat.uri, count(distinct stat.ip)) " +
            "FROM Stat AS stat " +
            "WHERE stat.timestamp BETWEEN ?1 AND ?2 AND stat.uri in ?3 " +
            "GROUP BY stat.app, stat.uri " +
            "ORDER BY count(distinct stat.ip) desc")
    List<StatResponseDto> findAllByTimestampBetweenStartAndEndWithUrisUniqueIp(LocalDateTime start, LocalDateTime end,
                                                                               List<String> uris);

    @Query("SELECT new ru.practicum.dto.StatResponseDto(stat.app, stat.uri, count(distinct stat.ip)) " +
            "FROM Stat AS stat " +
            "WHERE stat.timestamp BETWEEN ?1 AND ?2 AND stat.uri ilike %?3% " +
            "GROUP BY stat.app, stat.uri " +
            "ORDER BY count(distinct stat.ip) desc")
    List<StatResponseDto> findAllByTimestampBetweenStartAndEndWithUrisUniqueIpContainsUri(LocalDateTime start, LocalDateTime end,
                                                                               String uri);

    @Query("SELECT new ru.practicum.dto.StatResponseDto(stat.app, stat.uri, count(distinct stat.ip)) " +
            "FROM Stat AS stat " +
            "Where stat.uri in ?1 " +
            "GROUP BY stat.app, stat.uri " +
            "ORDER BY count(distinct stat.ip) desc")
    List<StatResponseDto> findAllUniqueIp(List<String> uris);

    @Query("SELECT new ru.practicum.dto.StatResponseDto(stat.app, stat.uri, count(stat.ip)) " +
            "FROM Stat AS stat " +
            "WHERE stat.timestamp BETWEEN ?1 AND ?2 AND stat.uri in ?3 " +
            "GROUP BY stat.app, stat.uri " +
            "ORDER BY count(stat.ip) desc")
    List<StatResponseDto> findAllByTimestampBetweenStartAndEndWithUrisIpNotUnique(LocalDateTime start, LocalDateTime end,
                                                                                  List<String> uris);

    @Query("SELECT new ru.practicum.dto.StatResponseDto(stat.app, stat.uri, count(stat.ip)) " +
            "FROM Stat AS stat " +
            "WHERE stat.timestamp BETWEEN ?1 AND ?2 AND stat.uri ILIKE %?3% " +
            "GROUP BY stat.app, stat.uri " +
            "ORDER BY count(stat.ip) desc")
    List<StatResponseDto> findAllByTimestampBetweenStartAndEndContainsUriIpNotUnique(LocalDateTime start, LocalDateTime end,
                                                                                  String uri);


}
