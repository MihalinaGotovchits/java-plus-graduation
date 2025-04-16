package ru.practicum.ewm.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.util.DateTimeUtil;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ParticipationRequestDto {
    private Long id;
    private Long requester;
    private Long event;
    private ParticipationRequestStatus status;
    @JsonFormat(pattern = DateTimeUtil.DATE_TIME_FORMAT)
    private LocalDateTime created;
}