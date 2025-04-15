package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.core.util.DateTimeUtil;
import ru.practicum.ewm.core.validation.NullOrNotBlank;
import ru.practicum.ewm.event.model.EventStateActionPrivate;
import ru.practicum.ewm.location.dto.NewLocationDto;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventUserRequestDto {
    @NullOrNotBlank
    @Size(min = 20, max = 2000)
    private String annotation;
    private Long category;
    @NullOrNotBlank
    @Size(min = 20, max = 7000)
    private String description;
    @JsonFormat(pattern = DateTimeUtil.DATE_TIME_FORMAT)
    private LocalDateTime eventDate;
    private NewLocationDto location;
    private Boolean paid;
    @PositiveOrZero
    private Integer participantLimit;
    private Boolean requestModeration;
    @NullOrNotBlank
    @Size(min = 3, max = 120)
    private String title;
    private EventStateActionPrivate stateAction;
}