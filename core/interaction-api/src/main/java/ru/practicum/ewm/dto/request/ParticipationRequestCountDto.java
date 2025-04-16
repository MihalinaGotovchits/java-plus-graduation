package ru.practicum.ewm.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParticipationRequestCountDto {
    Long eventId;
    Long quantity;
}
