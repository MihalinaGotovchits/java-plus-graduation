package ru.practicum.ewm.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.ewm.core.util.DateTimeUtil;
import ru.practicum.ewm.core.validation.DateTimeRange;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@DateTimeRange(before = "rangeStart", after = "rangeEnd")
public class EventPublicFilterParamsDto {
    String text;
    List<Long> categories;
    Boolean paid;
    @DateTimeFormat(pattern = DateTimeUtil.DATE_TIME_FORMAT)
    LocalDateTime rangeStart;
    @DateTimeFormat(pattern = DateTimeUtil.DATE_TIME_FORMAT)
    LocalDateTime rangeEnd;
    @Builder.Default
    Boolean onlyAvailable = false;
    Double lat;
    Double lon;
    @Builder.Default
    Double radius = 0D;
    EventSort sort;

    public enum EventSort {
        EVENT_DATE,
        VIEWS
    }
}