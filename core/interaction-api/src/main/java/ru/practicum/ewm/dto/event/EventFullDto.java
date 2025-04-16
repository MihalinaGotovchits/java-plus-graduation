package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.practicum.ewm.dto.location.LocationDto;
import ru.practicum.ewm.util.DateTimeUtil;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class EventFullDto extends EventShortDto {
    private String description;
    private LocationDto location;
    private Integer participantLimit;
    private Boolean requestModeration;
    private EventStates state;
    @JsonFormat(pattern = DateTimeUtil.DATE_TIME_FORMAT)
    private LocalDateTime createdOn;
    @JsonFormat(pattern = DateTimeUtil.DATE_TIME_FORMAT)
    private LocalDateTime publishedOn;
}