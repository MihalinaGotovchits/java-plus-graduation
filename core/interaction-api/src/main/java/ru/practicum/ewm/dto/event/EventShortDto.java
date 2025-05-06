package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.practicum.ewm.dto.categories.CategoryDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.util.DateTimeUtil;

import java.time.LocalDateTime;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class EventShortDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private UserShortDto initiator;
    @JsonFormat(pattern = DateTimeUtil.DATE_TIME_FORMAT)
    private LocalDateTime eventDate;
    private Boolean paid;
    private String title;
    private long confirmedRequests;
    private double rating;
}